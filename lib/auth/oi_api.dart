import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../app_config.dart';

enum AccountStep { profileSetup, livenessRequired, ready }

class OtpChallenge {
  const OtpChallenge({
    required this.requestId,
    required this.expiresAt,
    required this.resendAvailableAt,
  });

  final String requestId;
  final DateTime expiresAt;
  final DateTime resendAvailableAt;

  factory OtpChallenge.fromJson(Map<String, Object?> json) {
    return OtpChallenge(
      requestId: _requiredString(json, 'request_id'),
      expiresAt: _requiredDateTime(json, 'expires_at'),
      resendAvailableAt: _requiredDateTime(json, 'resend_available_at'),
    );
  }
}

class OiAccount {
  const OiAccount({
    required this.id,
    required this.phoneVerified,
    required this.livenessStatus,
    this.displayName,
    this.username,
    this.uin,
  });

  final String id;
  final bool phoneVerified;
  final String livenessStatus;
  final String? displayName;
  final String? username;
  final String? uin;

  factory OiAccount.fromJson(Map<String, Object?> json) {
    final phoneVerified = json['phone_verified'];
    final livenessStatus = json['liveness_status'];
    if (phoneVerified is! bool || livenessStatus is! String) {
      throw const ApiProtocolException(
        'The server returned an invalid account verification state.',
      );
    }

    return OiAccount(
      id: _requiredString(json, 'id'),
      displayName: _optionalString(json, 'display_name'),
      username: _optionalString(json, 'username'),
      uin: _optionalString(json, 'uin'),
      phoneVerified: phoneVerified,
      livenessStatus: livenessStatus,
    );
  }
}

class AuthResult {
  const AuthResult({
    required this.accessToken,
    required this.account,
    required this.nextStep,
  });

  final String accessToken;
  final OiAccount account;
  final AccountStep nextStep;

  factory AuthResult.fromJson(Map<String, Object?> json) {
    final account = _requiredObject(json, 'account', OiAccount.fromJson);
    final nextStep = _accountStep(json['next_step']);
    _validateAccountStep(account, nextStep);
    return AuthResult(
      accessToken: _requiredString(json, 'access_token'),
      account: account,
      nextStep: nextStep,
    );
  }
}

class SessionResult {
  const SessionResult({required this.account, required this.nextStep});

  final OiAccount account;
  final AccountStep nextStep;

  factory SessionResult.fromJson(Map<String, Object?> json) {
    final account = _requiredObject(json, 'account', OiAccount.fromJson);
    final nextStep = _accountStep(json['next_step']);
    _validateAccountStep(account, nextStep);
    return SessionResult(account: account, nextStep: nextStep);
  }
}

class OiApiException implements Exception {
  const OiApiException({
    required this.statusCode,
    required this.code,
    required this.message,
    this.retryAfterSeconds,
  });

  final int statusCode;
  final String code;
  final String message;
  final int? retryAfterSeconds;

  @override
  String toString() => 'OiApiException($statusCode, $code): $message';
}

class ApiConnectionException implements Exception {
  const ApiConnectionException(this.message);

  final String message;

  @override
  String toString() => message;
}

class ApiProtocolException implements Exception {
  const ApiProtocolException(this.message);

  final String message;

  @override
  String toString() => message;
}

class OiApi {
  OiApi({required this.config, http.Client? client})
    : _client = client ?? http.Client(),
      _ownsClient = client == null;

  final AppConfig config;
  final http.Client _client;
  final bool _ownsClient;

  Future<OtpChallenge> requestOtp(String phoneNumber) async {
    final json = await _send(
      'POST',
      'v1/otp/requests',
      body: {'phone_number': phoneNumber},
      expectedStatusCode: 202,
    );
    return OtpChallenge.fromJson(json);
  }

  Future<AuthResult> verifyOtp({
    required String requestId,
    required String code,
  }) async {
    final json = await _send(
      'POST',
      'v1/otp/verifications',
      body: {'request_id': requestId, 'code': code},
      expectedStatusCode: 200,
    );
    return AuthResult.fromJson(json);
  }

  Future<SessionResult> getSession(String accessToken) async {
    final json = await _send(
      'GET',
      'v1/session',
      accessToken: accessToken,
      expectedStatusCode: 200,
    );
    return SessionResult.fromJson(json);
  }

  Future<SessionResult> updateProfile({
    required String accessToken,
    required String displayName,
    required String username,
  }) async {
    final json = await _send(
      'PATCH',
      'v1/profile',
      accessToken: accessToken,
      body: {'display_name': displayName, 'username': username},
      expectedStatusCode: 200,
    );
    return SessionResult.fromJson(json);
  }

  Future<void> endSession(String accessToken) async {
    await _send(
      'DELETE',
      'v1/session',
      accessToken: accessToken,
      expectEmpty: true,
      expectedStatusCode: 204,
    );
  }

  void close() {
    if (_ownsClient) {
      _client.close();
    }
  }

  Future<Map<String, Object?>> _send(
    String method,
    String path, {
    String? accessToken,
    Map<String, Object?>? body,
    bool expectEmpty = false,
    required int expectedStatusCode,
  }) async {
    if (!config.isBackendConfigured) {
      throw const ApiConnectionException(
        'The Oi API endpoint has not been configured for this build.',
      );
    }

    final uri = config.endpoint(path);
    final headers = <String, String>{'Accept': 'application/json'};
    if (body != null) {
      headers['Content-Type'] = 'application/json; charset=utf-8';
    }
    if (accessToken != null) {
      headers['Authorization'] = 'Bearer $accessToken';
    }

    late final http.Response response;
    try {
      final request = http.Request(method, uri)..headers.addAll(headers);
      if (body != null) {
        request.body = jsonEncode(body);
      }
      response = await _sendAndRead(
        request,
      ).timeout(const Duration(seconds: 20));
    } on http.ClientException {
      throw const ApiConnectionException(
        'Could not reach the Oi service. Check your connection and try again.',
      );
    } on TimeoutException {
      throw const ApiConnectionException(
        'The Oi service took too long to respond. Try again.',
      );
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw _apiException(response);
    }
    if (response.statusCode != expectedStatusCode) {
      throw ApiProtocolException(
        'The Oi service returned HTTP ${response.statusCode}; expected $expectedStatusCode.',
      );
    }
    if (expectEmpty && response.bodyBytes.isEmpty) {
      return const {};
    }

    final Object? decoded;
    try {
      decoded = jsonDecode(utf8.decode(response.bodyBytes));
    } on FormatException {
      throw const ApiProtocolException(
        'The Oi service returned an unreadable response.',
      );
    }
    if (decoded is! Map<String, dynamic>) {
      throw const ApiProtocolException(
        'The Oi service returned an unexpected response.',
      );
    }
    return Map<String, Object?>.from(decoded);
  }

  Future<http.Response> _sendAndRead(http.Request request) async {
    final streamed = await _client.send(request);
    return http.Response.fromStream(streamed);
  }
}

OiApiException _apiException(http.Response response) {
  var code = 'request_failed';
  var message = 'The request could not be completed. Try again.';
  int? retryAfterSeconds = int.tryParse(response.headers['retry-after'] ?? '');

  try {
    final decoded = jsonDecode(utf8.decode(response.bodyBytes));
    if (decoded is Map<String, dynamic> &&
        decoded['error'] is Map<String, dynamic>) {
      final error = decoded['error'] as Map<String, dynamic>;
      if (error['code'] is String) {
        code = error['code'] as String;
      }
      if (error['message'] is String) {
        message = error['message'] as String;
      }
      if (error['retry_after_seconds'] is int) {
        retryAfterSeconds = error['retry_after_seconds'] as int;
      }
    }
  } on FormatException {
    // Keep the generic error when the server does not return the API envelope.
  }

  return OiApiException(
    statusCode: response.statusCode,
    code: code,
    message: message,
    retryAfterSeconds: retryAfterSeconds,
  );
}

AccountStep _accountStep(Object? value) => switch (value) {
  'profile_setup' => AccountStep.profileSetup,
  'liveness_required' => AccountStep.livenessRequired,
  'ready' => AccountStep.ready,
  _ => throw const ApiProtocolException(
    'The server returned an unsupported account setup state.',
  ),
};

void _validateAccountStep(OiAccount account, AccountStep step) {
  if (!account.phoneVerified) {
    throw const ApiProtocolException(
      'The server marked an account for setup before phone verification.',
    );
  }
  if (step == AccountStep.ready && account.livenessStatus != 'verified') {
    throw const ApiProtocolException(
      'The server marked an account ready before face liveness was verified.',
    );
  }
  if (step == AccountStep.livenessRequired &&
      account.livenessStatus == 'verified') {
    throw const ApiProtocolException(
      'The server returned conflicting face-liveness state.',
    );
  }
}

String _requiredString(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is! String || value.trim().isEmpty) {
    throw ApiProtocolException('The server response is missing $key.');
  }
  return value;
}

String? _optionalString(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value == null) {
    return null;
  }
  if (value is! String) {
    throw ApiProtocolException('The server returned an invalid $key.');
  }
  return value;
}

DateTime _requiredDateTime(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is! String) {
    throw ApiProtocolException('The server response is missing $key.');
  }
  final parsed = DateTime.tryParse(value);
  if (parsed == null) {
    throw ApiProtocolException('The server returned an invalid $key.');
  }
  return parsed.toUtc();
}

T _requiredObject<T>(
  Map<String, Object?> json,
  String key,
  T Function(Map<String, Object?>) parse,
) {
  final value = json[key];
  if (value is! Map<String, dynamic>) {
    throw ApiProtocolException('The server response is missing $key.');
  }
  return parse(Map<String, Object?>.from(value));
}
