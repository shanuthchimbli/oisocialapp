import 'dart:async';

import 'package:flutter/material.dart';

import 'app_config.dart';
import 'auth/auth_page.dart';
import 'auth/oi_api.dart';
import 'auth/session_store.dart';
import 'account_pages.dart';
import 'welcome_page.dart';

class OiApp extends StatefulWidget {
  OiApp({super.key, AppConfig? config, this.api, SessionStore? sessionStore})
    : config = config ?? AppConfig.fromEnvironment(),
      sessionStore = sessionStore ?? SecureSessionStore();

  final AppConfig config;
  final OiApi? api;
  final SessionStore sessionStore;

  @override
  State<OiApp> createState() => _OiAppState();
}

class _OiAppState extends State<OiApp> with WidgetsBindingObserver {
  final _scaffoldMessengerKey = GlobalKey<ScaffoldMessengerState>();
  late final OiApi? _api;
  bool _isRestoringSession = false;
  bool _isSigningOut = false;
  String? _restoreError;
  String? _accessToken;
  SessionResult? _session;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _api =
        widget.api ??
        (widget.config.isBackendConfigured
            ? OiApi(config: widget.config)
            : null);
    if (_api != null) {
      unawaited(_restoreSession());
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    if (widget.api == null) {
      _api?.close();
    }
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed &&
        _api != null &&
        _accessToken != null &&
        !_isRestoringSession) {
      unawaited(_restoreSession());
    }
  }

  Future<void> _restoreSession() async {
    if (_isRestoringSession) return;
    setState(() {
      _isRestoringSession = true;
      _restoreError = null;
    });

    try {
      final token = await widget.sessionStore.readAccessToken();
      if (token == null || token.isEmpty) {
        if (!mounted) return;
        setState(() {
          _accessToken = null;
          _session = null;
          _restoreError = null;
          _isRestoringSession = false;
        });
        return;
      }

      final restored = await _api!.getSession(token);
      if (!mounted) return;
      setState(() {
        _accessToken = token;
        _session = restored;
        _isRestoringSession = false;
      });
    } on OiApiException catch (error) {
      if (error.statusCode == 401) {
        try {
          await widget.sessionStore.clear();
        } catch (_) {
          _setRestoreError(
            'The service ended this session, but the saved credential could not be cleared. Try again.',
          );
          return;
        }
        if (!mounted) return;
        setState(() {
          _accessToken = null;
          _session = null;
          _isRestoringSession = false;
        });
      } else {
        _setRestoreError(error.message);
      }
    } on ApiConnectionException catch (error) {
      _setRestoreError(error.message);
    } on ApiProtocolException catch (error) {
      _setRestoreError(error.message);
    } catch (_) {
      _setRestoreError(
        'Could not securely restore the saved sign-in. Please try again.',
      );
    }
  }

  void _setRestoreError(String message) {
    if (!mounted) return;
    setState(() {
      _restoreError = message;
      _isRestoringSession = false;
    });
  }

  Future<void> _onAuthenticated(AuthResult auth) async {
    try {
      await widget.sessionStore.writeAccessToken(auth.accessToken);
    } catch (_) {
      try {
        await _api!.endSession(auth.accessToken);
      } catch (_) {
        // The user is told sign-in could not be completed; no token is kept locally.
      }
      throw StateError(
        'Could not securely save the sign-in on this device. Please try again.',
      );
    }
    if (!mounted) return;
    setState(() {
      _accessToken = auth.accessToken;
      _session = SessionResult(account: auth.account, nextStep: auth.nextStep);
    });
  }

  Future<SessionResult> _onProfileSubmitted({
    required String displayName,
    required String username,
  }) async {
    final result = await _api!.updateProfile(
      accessToken: _accessToken!,
      displayName: displayName,
      username: username,
    );
    if (!mounted) return result;
    setState(() => _session = result);
    return result;
  }

  Future<bool> _onSignOut() async {
    if (_isSigningOut) return true;
    setState(() => _isSigningOut = true);
    final token = _accessToken;
    try {
      await widget.sessionStore.clear();
    } catch (_) {
      if (mounted) setState(() => _isSigningOut = false);
      throw StateError(
        'Could not securely clear the saved sign-in. You are still signed in on this device.',
      );
    }

    var remoteSignOutConfirmed = true;
    try {
      if (token != null) {
        await _api!.endSession(token);
      }
    } on OiApiException catch (error) {
      remoteSignOutConfirmed = error.statusCode == 401;
    } on ApiConnectionException {
      remoteSignOutConfirmed = false;
    } on ApiProtocolException {
      remoteSignOutConfirmed = false;
    } catch (_) {
      remoteSignOutConfirmed = false;
    }

    if (mounted) {
      setState(() {
        _accessToken = null;
        _session = null;
        _isSigningOut = false;
      });
      if (!remoteSignOutConfirmed) {
        _scaffoldMessengerKey.currentState?.showSnackBar(
          const SnackBar(
            content: Text(
              'Signed out on this device. The service could not confirm that the remote session ended.',
            ),
          ),
        );
      }
    }
    return remoteSignOutConfirmed;
  }

  @override
  Widget build(BuildContext context) {
    const seedColor = Color(0xFF4757D6);

    return MaterialApp(
      scaffoldMessengerKey: _scaffoldMessengerKey,
      title: 'Oi',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: seedColor),
        scaffoldBackgroundColor: const Color(0xFFF7F7FA),
        useMaterial3: true,
      ),
      home: _buildHome(),
    );
  }

  Widget _buildHome() {
    if (!widget.config.isBackendConfigured || _api == null) {
      return WelcomePage(config: widget.config);
    }
    if (_isRestoringSession) {
      return const _StatusPage(
        title: 'Checking your account',
        message: 'Connecting to the Oi service to restore your session.',
        isLoading: true,
      );
    }
    if (_restoreError case final error?) {
      return _StatusPage(
        title: 'Could not restore your account',
        message: error,
        actionLabel: 'Try again',
        onAction: _restoreSession,
      );
    }
    if (_session == null) {
      return AuthPage(api: _api, onAuthenticated: _onAuthenticated);
    }

    return switch (_session!.nextStep) {
      AccountStep.profileSetup => ProfileSetupPage(
        account: _session!.account,
        isSigningOut: _isSigningOut,
        onSubmit: _onProfileSubmitted,
        onSignOut: _onSignOut,
      ),
      AccountStep.livenessRequired => LivenessRequiredPage(
        isSigningOut: _isSigningOut,
        onSignOut: _onSignOut,
      ),
      AccountStep.ready => AccountHomePage(
        account: _session!.account,
        isSigningOut: _isSigningOut,
        onSignOut: _onSignOut,
      ),
    };
  }
}

class _StatusPage extends StatelessWidget {
  const _StatusPage({
    required this.title,
    required this.message,
    this.isLoading = false,
    this.actionLabel,
    this.onAction,
  });

  final String title;
  final String message;
  final bool isLoading;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (isLoading) ...[
                    const CircularProgressIndicator(),
                    const SizedBox(height: 24),
                  ],
                  Text(
                    title,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    message,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      color: colors.onSurfaceVariant,
                      height: 1.5,
                    ),
                  ),
                  if (actionLabel != null && onAction != null) ...[
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: onAction,
                      child: Text(actionLabel!),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
