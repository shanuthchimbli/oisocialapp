import 'package:flutter_test/flutter_test.dart';
import 'package:oi/app_config.dart';

void main() {
  group('AppConfig.fromEnvironment', () {
    test('treats a missing API URL as unconfigured', () {
      final config = AppConfig.fromEnvironment(apiBaseUrl: '  ');

      expect(config.isBackendConfigured, isFalse);
      expect(() => config.endpoint('v1/auth'), throwsStateError);
    });

    test('accepts HTTPS URLs and preserves a base path', () {
      final config = AppConfig.fromEnvironment(
        apiBaseUrl: 'https://api.example.test/oi',
      );

      expect(config.isBackendConfigured, isTrue);
      expect(
        config.endpoint('v1/auth').toString(),
        'https://api.example.test/oi/v1/auth',
      );
    });

    test('allows HTTP only for loopback development hosts', () {
      expect(
        AppConfig.fromEnvironment(
          apiBaseUrl: 'http://localhost:8080',
        ).apiBaseUri,
        Uri.parse('http://localhost:8080/'),
      );
      expect(
        () => AppConfig.fromEnvironment(apiBaseUrl: 'http://api.example.test'),
        throwsFormatException,
      );
    });

    test('rejects credentials, query strings, and fragments', () {
      for (final value in [
        Uri(
          scheme: 'https',
          userInfo: 'test-user',
          host: 'api.example.test',
        ).toString(),
        'https://api.example.test?token=secret',
        'https://api.example.test#fragment',
      ]) {
        expect(
          () => AppConfig.fromEnvironment(apiBaseUrl: value),
          throwsFormatException,
        );
      }
    });

    test('rejects absolute and traversal API paths', () {
      final config = AppConfig.fromEnvironment(
        apiBaseUrl: 'https://api.example.test/v1',
      );

      expect(() => config.endpoint('/auth'), throwsArgumentError);
      expect(() => config.endpoint('../admin'), throwsArgumentError);
      expect(() => config.endpoint('  '), throwsArgumentError);
    });
  });
}
