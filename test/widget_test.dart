import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:oi/app.dart';
import 'package:oi/app_config.dart';
import 'package:oi/auth/oi_api.dart';
import 'package:oi/auth/session_store.dart';

const _account = {
  'id': 'account-1',
  'display_name': 'Ari',
  'username': 'ari',
  'uin': 'OI-123',
  'phone_verified': true,
  'liveness_status': 'verified',
};

class _MemorySessionStore implements SessionStore {
  _MemorySessionStore([this.token, this.failWrites = false]);

  String? token;
  final bool failWrites;

  @override
  Future<String?> readAccessToken() async => token;

  @override
  Future<void> writeAccessToken(String accessToken) async {
    if (failWrites) {
      throw StateError('secure storage unavailable');
    }
    token = accessToken;
  }

  @override
  Future<void> clear() async {
    token = null;
  }
}

http.Response _jsonResponse(Map<String, Object?> body, {int statusCode = 200}) {
  return http.Response(
    jsonEncode(body),
    statusCode,
    headers: const {'content-type': 'application/json'},
  );
}

Map<String, Object?> _sessionResponse({
  String nextStep = 'ready',
  Map<String, Object?> account = _account,
}) => {'account': account, 'next_step': nextStep};

Future<void> _pumpConfiguredApp(
  WidgetTester tester, {
  required MockClient client,
  required _MemorySessionStore store,
}) async {
  await tester.pumpWidget(
    OiApp(
      config: AppConfig.fromEnvironment(apiBaseUrl: 'https://api.example.test'),
      api: OiApi(
        config: AppConfig.fromEnvironment(
          apiBaseUrl: 'https://api.example.test',
        ),
        client: client,
      ),
      sessionStore: store,
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('shows an explicit setup state when no API is configured', (
    tester,
  ) async {
    await tester.pumpWidget(OiApp(config: AppConfig.fromEnvironment()));

    expect(find.text('Backend not configured'), findsOneWidget);
    expect(
      find.text('Sign-in needs an Oi API endpoint and a running backend.'),
      findsOneWidget,
    );
  });

  testWidgets('validates international phone input before any API call', (
    tester,
  ) async {
    var requestCount = 0;
    final client = MockClient((_) async {
      requestCount++;
      return _jsonResponse({});
    });

    await _pumpConfiguredApp(
      tester,
      client: client,
      store: _MemorySessionStore(),
    );
    await tester.enterText(find.byType(TextField).first, '555-1234');
    await tester.tap(find.text('Send verification code'));
    await tester.pumpAndSettle();

    expect(
      find.text(
        'Enter a complete phone number in international format, such as +14155552671.',
      ),
      findsOneWidget,
    );
    expect(requestCount, 0);
  });

  testWidgets('requests and verifies a real API OTP then blocks at liveness', (
    tester,
  ) async {
    final store = _MemorySessionStore();
    final requests = <http.Request>[];
    final client = MockClient((request) async {
      requests.add(request);
      if (request.url.path == '/v1/otp/requests') {
        expect(jsonDecode(request.body), {'phone_number': '+14155552671'});
        final now = DateTime.now().toUtc();
        return _jsonResponse({
          'request_id': 'challenge-1',
          'expires_at': now.add(const Duration(minutes: 5)).toIso8601String(),
          'resend_available_at': now
              .add(const Duration(minutes: 1))
              .toIso8601String(),
        }, statusCode: 202);
      }
      if (request.url.path == '/v1/otp/verifications') {
        expect(jsonDecode(request.body), {
          'request_id': 'challenge-1',
          'code': '123456',
        });
        return _jsonResponse({
          'access_token': 'opaque-test-token',
          'account': {..._account, 'liveness_status': 'not_started'},
          'next_step': 'liveness_required',
        });
      }
      fail('Unexpected API request: ${request.method} ${request.url}');
    });

    await _pumpConfiguredApp(tester, client: client, store: store);
    await tester.enterText(find.byType(TextField).first, '+14155552671');
    await tester.tap(find.text('Send verification code'));
    await tester.pumpAndSettle();

    expect(find.text('Enter your code'), findsOneWidget);
    await tester.enterText(find.byType(TextField).first, '123456');
    await tester.tap(find.text('Verify and continue'));
    await tester.pumpAndSettle();

    expect(find.text('Face liveness check required'), findsOneWidget);
    expect(
      find.textContaining('not mark this account as complete'),
      findsOneWidget,
    );
    expect(store.token, 'opaque-test-token');
    expect(requests.map((request) => request.method), ['POST', 'POST']);
    expect(requests[1].headers['authorization'], isNull);
  });

  testWidgets('does not accept an invalid OTP or persist a session', (
    tester,
  ) async {
    final store = _MemorySessionStore();
    final client = MockClient((request) async {
      if (request.url.path == '/v1/otp/requests') {
        final now = DateTime.now().toUtc();
        return _jsonResponse({
          'request_id': 'challenge-2',
          'expires_at': now.add(const Duration(minutes: 5)).toIso8601String(),
          'resend_available_at': now
              .add(const Duration(minutes: 1))
              .toIso8601String(),
        }, statusCode: 202);
      }
      return _jsonResponse({
        'error': {'code': 'invalid_otp', 'message': 'Code rejected.'},
      }, statusCode: 400);
    });

    await _pumpConfiguredApp(tester, client: client, store: store);
    await tester.enterText(find.byType(TextField).first, '+14155552671');
    await tester.tap(find.text('Send verification code'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).first, '000000');
    await tester.tap(find.text('Verify and continue'));
    await tester.pumpAndSettle();

    expect(
      find.text('That code is not correct. Check it and try again.'),
      findsOneWidget,
    );
    expect(store.token, isNull);
    expect(find.text('Enter your code'), findsOneWidget);
  });

  testWidgets('revokes a new session if secure local persistence fails', (
    tester,
  ) async {
    final requests = <http.Request>[];
    final store = _MemorySessionStore(null, true);
    final client = MockClient((request) async {
      requests.add(request);
      if (request.url.path == '/v1/otp/requests') {
        final now = DateTime.now().toUtc();
        return _jsonResponse({
          'request_id': 'storage-challenge',
          'expires_at': now.add(const Duration(minutes: 5)).toIso8601String(),
          'resend_available_at': now
              .add(const Duration(minutes: 1))
              .toIso8601String(),
        }, statusCode: 202);
      }
      if (request.url.path == '/v1/otp/verifications') {
        return _jsonResponse({
          'access_token': 'token-not-saved',
          'account': {..._account, 'liveness_status': 'verified'},
          'next_step': 'ready',
        });
      }
      if (request.method == 'DELETE' && request.url.path == '/v1/session') {
        expect(request.headers['authorization'], 'Bearer token-not-saved');
        return http.Response('', 204);
      }
      fail('Unexpected API request: ${request.method} ${request.url}');
    });

    await _pumpConfiguredApp(tester, client: client, store: store);
    await tester.enterText(find.byType(TextField).first, '+14155552671');
    await tester.tap(find.text('Send verification code'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).first, '123456');
    await tester.tap(find.text('Verify and continue'));
    await tester.pumpAndSettle();

    expect(store.token, isNull);
    expect(find.text('Enter your code'), findsOneWidget);
    expect(requests.map((request) => request.method), [
      'POST',
      'POST',
      'DELETE',
    ]);
    expect(
      find.text('Could not safely finish sign-in. Please try again.'),
      findsOneWidget,
    );
  });

  testWidgets('honors server OTP lockout retry time in the UI', (tester) async {
    final client = MockClient((request) async {
      if (request.url.path == '/v1/otp/requests') {
        final now = DateTime.now().toUtc();
        return _jsonResponse({
          'request_id': 'locked-challenge',
          'expires_at': now.add(const Duration(minutes: 5)).toIso8601String(),
          'resend_available_at': now
              .add(const Duration(minutes: 1))
              .toIso8601String(),
        }, statusCode: 202);
      }
      return _jsonResponse({
        'error': {
          'code': 'otp_locked',
          'message': 'Too many attempts.',
          'retry_after_seconds': 900,
        },
      }, statusCode: 429);
    });

    await _pumpConfiguredApp(
      tester,
      client: client,
      store: _MemorySessionStore(),
    );
    await tester.enterText(find.byType(TextField).first, '+14155552671');
    await tester.tap(find.text('Send verification code'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).first, '123456');
    await tester.tap(find.text('Verify and continue'));
    await tester.pumpAndSettle();

    expect(
      find.text('Too many incorrect attempts. Try again after the lockout.'),
      findsOneWidget,
    );
    final verifyButton = tester.widget<FilledButton>(
      find.byType(FilledButton).first,
    );
    expect(verifyButton.onPressed, isNull);
  });

  testWidgets('restores a server-validated session and signs out remotely', (
    tester,
  ) async {
    final store = _MemorySessionStore('saved-session-token');
    final requests = <http.Request>[];
    final client = MockClient((request) async {
      requests.add(request);
      if (request.method == 'GET' && request.url.path == '/v1/session') {
        expect(request.headers['authorization'], 'Bearer saved-session-token');
        return _jsonResponse(_sessionResponse());
      }
      if (request.method == 'DELETE' && request.url.path == '/v1/session') {
        expect(request.headers['authorization'], 'Bearer saved-session-token');
        return http.Response('', 204);
      }
      fail('Unexpected API request: ${request.method} ${request.url}');
    });

    await _pumpConfiguredApp(tester, client: client, store: store);

    expect(find.text('Your Oi account'), findsOneWidget);
    expect(find.textContaining('OI-123'), findsOneWidget);
    await tester.tap(find.text('Sign out'));
    await tester.pumpAndSettle();

    expect(store.token, isNull);
    expect(find.text('Sign in with your phone'), findsOneWidget);
    expect(requests.map((request) => request.method), ['GET', 'DELETE']);
  });

  testWidgets(
    'reports when local sign-out succeeds but remote is unconfirmed',
    (tester) async {
      final store = _MemorySessionStore('saved-session-token');
      final client = MockClient((request) async {
        if (request.method == 'GET' && request.url.path == '/v1/session') {
          return _jsonResponse(_sessionResponse());
        }
        if (request.method == 'DELETE' && request.url.path == '/v1/session') {
          return _jsonResponse({
            'error': {
              'code': 'service_unavailable',
              'message': 'Please try later.',
            },
          }, statusCode: 503);
        }
        fail('Unexpected API request: ${request.method} ${request.url}');
      });

      await _pumpConfiguredApp(tester, client: client, store: store);
      await tester.tap(find.text('Sign out'));
      await tester.pumpAndSettle();

      expect(store.token, isNull);
      expect(find.text('Sign in with your phone'), findsOneWidget);
      expect(
        find.textContaining('could not confirm that the remote session ended'),
        findsOneWidget,
      );
    },
  );

  testWidgets('clears a session replaced by another device on resume', (
    tester,
  ) async {
    final store = _MemorySessionStore('replaced-session-token');
    var sessionChecks = 0;
    final client = MockClient((request) async {
      if (request.method == 'GET' && request.url.path == '/v1/session') {
        sessionChecks++;
        if (sessionChecks == 1) {
          return _jsonResponse(_sessionResponse());
        }
        return _jsonResponse({
          'error': {'code': 'session_replaced', 'message': 'Session replaced.'},
        }, statusCode: 401);
      }
      fail('Unexpected API request: ${request.method} ${request.url}');
    });

    await _pumpConfiguredApp(tester, client: client, store: store);
    expect(find.text('Your Oi account'), findsOneWidget);

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();

    expect(sessionChecks, 2);
    expect(store.token, isNull);
    expect(find.text('Sign in with your phone'), findsOneWidget);
  });

  testWidgets('updates profile only through the backend', (tester) async {
    final store = _MemorySessionStore();
    final client = MockClient((request) async {
      if (request.url.path == '/v1/otp/requests') {
        final now = DateTime.now().toUtc();
        return _jsonResponse({
          'request_id': 'profile-challenge',
          'expires_at': now.add(const Duration(minutes: 5)).toIso8601String(),
          'resend_available_at': now
              .add(const Duration(minutes: 1))
              .toIso8601String(),
        }, statusCode: 202);
      }
      if (request.url.path == '/v1/otp/verifications') {
        return _jsonResponse({
          'access_token': 'profile-token',
          'account': {
            'id': 'account-2',
            'display_name': null,
            'username': null,
            'uin': 'OI-456',
            'phone_verified': true,
            'liveness_status': 'verified',
          },
          'next_step': 'profile_setup',
        });
      }
      if (request.method == 'PATCH' && request.url.path == '/v1/profile') {
        expect(jsonDecode(request.body), {
          'display_name': 'Ari',
          'username': 'ari',
        });
        expect(request.headers['authorization'], 'Bearer profile-token');
        return _jsonResponse(_sessionResponse());
      }
      fail('Unexpected API request: ${request.method} ${request.url}');
    });

    await _pumpConfiguredApp(tester, client: client, store: store);
    await tester.enterText(find.byType(TextField).first, '+14155552671');
    await tester.tap(find.text('Send verification code'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).first, '123456');
    await tester.tap(find.text('Verify and continue'));
    await tester.pumpAndSettle();

    expect(find.text('Set up your profile'), findsOneWidget);
    await tester.enterText(find.byType(TextField).at(0), 'Ari');
    await tester.enterText(find.byType(TextField).at(1), 'ari');
    await tester.tap(find.text('Save profile'));
    await tester.pumpAndSettle();

    expect(find.text('Your Oi account'), findsOneWidget);
  });
}
