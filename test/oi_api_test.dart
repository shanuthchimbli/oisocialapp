import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:oi/app_config.dart';
import 'package:oi/auth/oi_api.dart';

void main() {
  final config = AppConfig.fromEnvironment(
    apiBaseUrl: 'https://api.example.test',
  );

  test('preserves server rate-limit details for resend UI', () async {
    final api = OiApi(
      config: config,
      client: MockClient(
        (_) async => http.Response(
          jsonEncode({
            'error': {
              'code': 'rate_limited',
              'message': 'Wait before requesting another code.',
              'retry_after_seconds': 73,
            },
          }),
          429,
          headers: const {'retry-after': '73'},
        ),
      ),
    );

    await expectLater(
      api.requestOtp('+14155552671'),
      throwsA(
        isA<OiApiException>()
            .having((error) => error.statusCode, 'status', 429)
            .having((error) => error.code, 'code', 'rate_limited')
            .having((error) => error.retryAfterSeconds, 'retry delay', 73),
      ),
    );
    api.close();
  });

  test(
    'rejects a server response that claims readiness without liveness',
    () async {
      final api = OiApi(
        config: config,
        client: MockClient(
          (_) async => http.Response(
            jsonEncode({
              'access_token': 'token',
              'account': {
                'id': 'account-1',
                'display_name': 'Ari',
                'username': 'ari',
                'uin': 'OI-1',
                'phone_verified': true,
                'liveness_status': 'not_started',
              },
              'next_step': 'ready',
            }),
            200,
          ),
        ),
      );

      await expectLater(
        api.verifyOtp(requestId: 'challenge', code: '123456'),
        throwsA(isA<ApiProtocolException>()),
      );
      api.close();
    },
  );

  test(
    'requires a valid protocol response for successful OTP requests',
    () async {
      final api = OiApi(
        config: config,
        client: MockClient((_) async => http.Response('not json', 202)),
      );

      await expectLater(
        api.requestOtp('+14155552671'),
        throwsA(isA<ApiProtocolException>()),
      );
      api.close();
    },
  );

  test('never sends bearer tokens on unauthenticated OTP requests', () async {
    http.Request? sentRequest;
    final api = OiApi(
      config: config,
      client: MockClient((request) async {
        sentRequest = request;
        return http.Response(
          jsonEncode({
            'request_id': 'challenge',
            'expires_at': '2026-11-08T11:00:00Z',
            'resend_available_at': '2026-11-08T10:56:00Z',
          }),
          202,
        );
      }),
    );

    await api.requestOtp('+14155552671');

    expect(sentRequest!.headers.containsKey('authorization'), isFalse);
    api.close();
  });

  test('rejects a 2xx response with the wrong endpoint status code', () async {
    final api = OiApi(
      config: config,
      client: MockClient(
        (_) async => http.Response(
          jsonEncode({
            'request_id': 'challenge',
            'expires_at': '2026-11-08T11:00:00Z',
            'resend_available_at': '2026-11-08T10:56:00Z',
          }),
          200,
        ),
      ),
    );

    await expectLater(
      api.requestOtp('+14155552671'),
      throwsA(isA<ApiProtocolException>()),
    );
    api.close();
  });

  test('requires exactly 204 when ending a session', () async {
    final api = OiApi(
      config: config,
      client: MockClient(
        (_) async => http.Response(jsonEncode({'ended': true}), 200),
      ),
    );

    await expectLater(
      api.endSession('opaque-session'),
      throwsA(isA<ApiProtocolException>()),
    );
    api.close();
  });
}
