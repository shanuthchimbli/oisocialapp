import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'oi_api.dart';

final _e164Pattern = RegExp(r'^\+[1-9][0-9]{0,14}$');
final _otpPattern = RegExp(r'^[0-9]{6}$');

class AuthPage extends StatefulWidget {
  const AuthPage({super.key, required this.api, required this.onAuthenticated});

  final OiApi api;
  final Future<void> Function(AuthResult result) onAuthenticated;

  @override
  State<AuthPage> createState() => _AuthPageState();
}

class _AuthPageState extends State<AuthPage> {
  final _phoneController = TextEditingController();
  final _codeController = TextEditingController();
  OtpChallenge? _challenge;
  Timer? _timer;
  Duration _resendRemaining = Duration.zero;
  Duration _verificationLockRemaining = Duration.zero;
  Duration? _expiresRemaining;
  DateTime? _resendAvailableAt;
  DateTime? _verificationBlockedUntil;
  bool _isBusy = false;
  String? _errorMessage;
  String? _statusMessage;

  @override
  void dispose() {
    _timer?.cancel();
    _phoneController.dispose();
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _requestCode() async {
    final phone = _phoneController.text.trim();
    if (!_e164Pattern.hasMatch(phone)) {
      setState(() {
        _errorMessage =
            'Enter a complete phone number in international format, such as +14155552671.';
        _statusMessage = null;
      });
      return;
    }

    await _runRequest(() async {
      final challenge = await widget.api.requestOtp(phone);
      if (!mounted) return;
      setState(() {
        _challenge = challenge;
        _codeController.clear();
        _errorMessage = null;
        _expiresRemaining = challenge.expiresAt.difference(
          DateTime.now().toUtc(),
        );
        _verificationBlockedUntil = null;
        _statusMessage =
            'A code request was submitted. If this number can receive SMS, it should arrive shortly.';
      });
      _startResendCountdown(challenge.resendAvailableAt);
    });
  }

  Future<void> _verifyCode() async {
    final challenge = _challenge;
    final code = _codeController.text.trim();
    if (challenge == null) return;
    if (!_otpPattern.hasMatch(code)) {
      setState(() {
        _errorMessage = 'Enter the six-digit code.';
        _statusMessage = null;
      });
      return;
    }

    await _runRequest(() async {
      final result = await widget.api.verifyOtp(
        requestId: challenge.requestId,
        code: code,
      );
      await widget.onAuthenticated(result);
    });
  }

  Future<void> _runRequest(Future<void> Function() action) async {
    setState(() {
      _isBusy = true;
      _errorMessage = null;
    });
    try {
      await action();
    } on OiApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMessage = _humanizeApiError(error);
      });
      if (error.retryAfterSeconds != null && error.retryAfterSeconds! > 0) {
        final retryAt = DateTime.now().toUtc().add(
          Duration(seconds: error.retryAfterSeconds!),
        );
        if (error.code == 'otp_locked') {
          _startVerificationLock(retryAt);
        } else {
          _startResendCountdown(retryAt);
        }
      }
    } on ApiConnectionException catch (error) {
      if (!mounted) return;
      setState(() => _errorMessage = error.message);
    } on ApiProtocolException catch (error) {
      if (!mounted) return;
      setState(() => _errorMessage = error.message);
    } catch (_) {
      if (!mounted) return;
      setState(
        () => _errorMessage =
            'Could not safely finish sign-in. Please try again.',
      );
    } finally {
      if (mounted) {
        setState(() => _isBusy = false);
      }
    }
  }

  String _humanizeApiError(OiApiException error) {
    return switch (error.code) {
      'invalid_otp' => 'That code is not correct. Check it and try again.',
      'otp_expired' => 'That code has expired. Request a new code.',
      'otp_locked' =>
        'Too many incorrect attempts. Try again after the lockout.',
      'rate_limited' => 'Too many requests. Wait before trying again.',
      _ => error.message,
    };
  }

  void _startResendCountdown(DateTime availableAt) {
    _resendAvailableAt = availableAt;
    _refreshCountdowns();
    _ensureCountdownTimer();
  }

  void _startVerificationLock(DateTime availableAt) {
    _verificationBlockedUntil = availableAt;
    _refreshCountdowns();
    _ensureCountdownTimer();
  }

  void _ensureCountdownTimer() {
    _timer?.cancel();
    if (_resendRemaining == Duration.zero &&
        _verificationLockRemaining == Duration.zero &&
        (_expiresRemaining == null || _expiresRemaining == Duration.zero)) {
      return;
    }
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      _refreshCountdowns();
      if (_resendRemaining == Duration.zero &&
          _verificationLockRemaining == Duration.zero &&
          (_expiresRemaining == null || _expiresRemaining == Duration.zero)) {
        _timer?.cancel();
      }
    });
  }

  void _refreshCountdowns() {
    if (!mounted) return;
    final now = DateTime.now().toUtc();
    setState(() {
      final resend = _resendAvailableAt?.difference(now) ?? Duration.zero;
      _resendRemaining = resend.isNegative ? Duration.zero : resend;
      final lockout =
          _verificationBlockedUntil?.difference(now) ?? Duration.zero;
      _verificationLockRemaining = lockout.isNegative ? Duration.zero : lockout;
      final expiry = _challenge?.expiresAt.difference(now);
      _expiresRemaining = expiry == null || expiry.isNegative
          ? Duration.zero
          : expiry;
    });
  }

  void _backToPhone() {
    _timer?.cancel();
    setState(() {
      _challenge = null;
      _resendAvailableAt = null;
      _verificationBlockedUntil = null;
      _resendRemaining = Duration.zero;
      _verificationLockRemaining = Duration.zero;
      _expiresRemaining = null;
      _errorMessage = null;
      _statusMessage = null;
      _codeController.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final challenge = _challenge;

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Container(
                      width: 58,
                      height: 58,
                      decoration: BoxDecoration(
                        color: colors.primary,
                        borderRadius: BorderRadius.circular(18),
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        'Oi',
                        style: Theme.of(context).textTheme.headlineSmall
                            ?.copyWith(
                              color: colors.onPrimary,
                              fontWeight: FontWeight.w700,
                            ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),
                  Text(
                    challenge == null
                        ? 'Sign in with your phone'
                        : 'Enter your code',
                    style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    challenge == null
                        ? 'We will send a one-time verification code by SMS.'
                        : 'Enter the six-digit code sent to ${_phoneController.text.trim()}.',
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      color: colors.onSurfaceVariant,
                      height: 1.45,
                    ),
                  ),
                  const SizedBox(height: 28),
                  if (challenge == null) _buildPhoneField(),
                  if (challenge != null) _buildCodeField(),
                  if (_errorMessage != null) ...[
                    const SizedBox(height: 16),
                    _MessageBox(
                      message: _errorMessage!,
                      color: colors.errorContainer,
                      textColor: colors.onErrorContainer,
                      icon: Icons.error_outline,
                    ),
                  ],
                  if (_statusMessage != null) ...[
                    const SizedBox(height: 16),
                    _MessageBox(
                      message: _statusMessage!,
                      color: colors.secondaryContainer,
                      textColor: colors.onSecondaryContainer,
                      icon: Icons.info_outline,
                    ),
                  ],
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _isBusy
                        ? null
                        : challenge == null
                        ? _resendRemaining > Duration.zero
                              ? null
                              : _requestCode
                        : _verificationLockRemaining > Duration.zero
                        ? null
                        : _verifyCode,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      child: _isBusy
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(
                              challenge == null
                                  ? _resendRemaining > Duration.zero
                                        ? 'Try again in ${_resendRemaining.inSeconds}s'
                                        : 'Send verification code'
                                  : _verificationLockRemaining > Duration.zero
                                  ? 'Try again in ${_verificationLockRemaining.inSeconds}s'
                                  : 'Verify and continue',
                            ),
                    ),
                  ),
                  if (challenge != null) ...[
                    const SizedBox(height: 12),
                    TextButton(
                      onPressed: _isBusy ? null : _backToPhone,
                      child: const Text('Change phone number'),
                    ),
                    TextButton(
                      onPressed: _isBusy || _resendRemaining > Duration.zero
                          ? null
                          : _requestCode,
                      child: Text(
                        _resendRemaining > Duration.zero
                            ? 'Resend available in ${_resendRemaining.inSeconds}s'
                            : 'Resend code',
                      ),
                    ),
                    Text(
                      'Codes expire after five minutes. Verification and '
                      'request limits are enforced by the Oi service. '
                      '${_expiresRemaining == Duration.zero ? 'This code may have expired; request a new one.' : 'Code expires in ${_expiresRemaining?.inMinutes}m ${(_expiresRemaining?.inSeconds ?? 0) % 60}s.'}',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                  ],
                  const SizedBox(height: 28),
                  Text(
                    'Phone numbers are private account information and are '
                    'not shown on your public profile.',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: colors.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPhoneField() {
    return TextField(
      controller: _phoneController,
      enabled: !_isBusy,
      keyboardType: TextInputType.phone,
      textInputAction: TextInputAction.done,
      autofillHints: const [AutofillHints.telephoneNumber],
      inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[+0-9]'))],
      onSubmitted: (_) => _requestCode(),
      decoration: const InputDecoration(
        labelText: 'Phone number',
        hintText: '+14155552671',
        helperText: 'Use international format, including country code.',
        prefixIcon: Icon(Icons.phone_outlined),
        border: OutlineInputBorder(),
      ),
    );
  }

  Widget _buildCodeField() {
    return TextField(
      controller: _codeController,
      enabled: !_isBusy,
      autofocus: true,
      keyboardType: TextInputType.number,
      textInputAction: TextInputAction.done,
      autofillHints: const [AutofillHints.oneTimeCode],
      inputFormatters: [
        FilteringTextInputFormatter.digitsOnly,
        LengthLimitingTextInputFormatter(6),
      ],
      onSubmitted: (_) => _verifyCode(),
      decoration: const InputDecoration(
        labelText: 'Six-digit code',
        hintText: '000000',
        prefixIcon: Icon(Icons.lock_outline),
        border: OutlineInputBorder(),
      ),
    );
  }
}

class _MessageBox extends StatelessWidget {
  const _MessageBox({
    required this.message,
    required this.color,
    required this.textColor,
    required this.icon,
  });

  final String message;
  final Color color;
  final Color textColor;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: textColor, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(color: textColor),
            ),
          ),
        ],
      ),
    );
  }
}
