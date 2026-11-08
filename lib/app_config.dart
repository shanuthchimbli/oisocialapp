class AppConfig {
  const AppConfig({required this.apiBaseUri});

  static const _apiBaseUrl = String.fromEnvironment('OI_API_BASE_URL');

  factory AppConfig.fromEnvironment({String? apiBaseUrl}) {
    final value = (apiBaseUrl ?? _apiBaseUrl).trim();
    if (value.isEmpty) {
      return const AppConfig(apiBaseUri: null);
    }

    final uri = Uri.tryParse(value);
    if (uri == null ||
        !uri.hasScheme ||
        uri.host.isEmpty ||
        uri.userInfo.isNotEmpty ||
        uri.hasQuery ||
        uri.hasFragment) {
      throw const FormatException(
        'OI_API_BASE_URL must be an absolute URL without credentials, query, '
        'or fragment.',
      );
    }

    final isLoopbackHost =
        uri.host == 'localhost' || uri.host == '127.0.0.1' || uri.host == '::1';
    if (uri.scheme != 'https' && !(uri.scheme == 'http' && isLoopbackHost)) {
      throw const FormatException(
        'OI_API_BASE_URL must use HTTPS. HTTP is allowed only for loopback '
        'development hosts.',
      );
    }

    final baseUri = uri.replace(
      path: uri.path.endsWith('/') ? uri.path : '${uri.path}/',
    );
    return AppConfig(apiBaseUri: baseUri);
  }

  final Uri? apiBaseUri;

  bool get isBackendConfigured => apiBaseUri != null;

  Uri endpoint(String relativePath) {
    final baseUri = apiBaseUri;
    if (baseUri == null) {
      throw StateError('OI_API_BASE_URL is not configured.');
    }

    final path = relativePath.trim();
    if (path.isEmpty ||
        path.startsWith('/') ||
        path.split('/').any((segment) => segment == '.' || segment == '..')) {
      throw ArgumentError.value(
        relativePath,
        'relativePath',
        'Provide a non-empty relative API path without traversal segments.',
      );
    }

    return baseUri.resolve(path);
  }
}
