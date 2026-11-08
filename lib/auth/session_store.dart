import 'package:flutter_secure_storage/flutter_secure_storage.dart';

abstract interface class SessionStore {
  Future<String?> readAccessToken();

  Future<void> writeAccessToken(String accessToken);

  Future<void> clear();
}

class SecureSessionStore implements SessionStore {
  SecureSessionStore({FlutterSecureStorage? storage})
    : _storage = storage ?? const FlutterSecureStorage();

  static const _tokenKey = 'oi.access_token';

  final FlutterSecureStorage _storage;

  @override
  Future<String?> readAccessToken() => _storage.read(key: _tokenKey);

  @override
  Future<void> writeAccessToken(String accessToken) =>
      _storage.write(key: _tokenKey, value: accessToken);

  @override
  Future<void> clear() => _storage.delete(key: _tokenKey);
}
