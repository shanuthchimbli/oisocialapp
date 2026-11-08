import 'package:flutter/material.dart';

import 'auth/oi_api.dart';

typedef ProfileSubmit =
    Future<SessionResult> Function({
      required String displayName,
      required String username,
    });

typedef SignOut = Future<bool> Function();

class ProfileSetupPage extends StatefulWidget {
  const ProfileSetupPage({
    super.key,
    required this.account,
    required this.isSigningOut,
    required this.onSubmit,
    required this.onSignOut,
  });

  final OiAccount account;
  final bool isSigningOut;
  final ProfileSubmit onSubmit;
  final SignOut onSignOut;

  @override
  State<ProfileSetupPage> createState() => _ProfileSetupPageState();
}

class _ProfileSetupPageState extends State<ProfileSetupPage> {
  late final TextEditingController _displayNameController;
  late final TextEditingController _usernameController;
  bool _isBusy = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _displayNameController = TextEditingController(
      text: widget.account.displayName ?? '',
    );
    _usernameController = TextEditingController(
      text: widget.account.username ?? '',
    );
  }

  @override
  void dispose() {
    _displayNameController.dispose();
    _usernameController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final displayName = _displayNameController.text.trim();
    final username = _usernameController.text.trim();
    if (displayName.isEmpty || username.isEmpty) {
      setState(() {
        _errorMessage = 'Enter a display name and a username to continue.';
      });
      return;
    }

    setState(() {
      _isBusy = true;
      _errorMessage = null;
    });
    try {
      await widget.onSubmit(displayName: displayName, username: username);
    } on OiApiException catch (error) {
      if (mounted) {
        setState(() => _errorMessage = error.message);
      }
    } on ApiConnectionException catch (error) {
      if (mounted) {
        setState(() => _errorMessage = error.message);
      }
    } on ApiProtocolException catch (error) {
      if (mounted) {
        setState(() => _errorMessage = error.message);
      }
    } catch (_) {
      if (mounted) {
        setState(
          () => _errorMessage = 'Could not save your profile. Try again.',
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isBusy = false);
      }
    }
  }

  Future<void> _signOut() async {
    try {
      await widget.onSignOut();
    } catch (error) {
      if (!mounted) return;
      _showSignOutFailure(context, error);
    }
  }

  @override
  Widget build(BuildContext context) {
    return _AccountFrame(
      title: 'Set up your profile',
      subtitle:
          'Your phone check is complete. Choose the name and username people will see.',
      onSignOut: _isBusy || widget.isSigningOut ? null : _signOut,
      children: [
        TextField(
          controller: _displayNameController,
          enabled: !_isBusy,
          textCapitalization: TextCapitalization.words,
          textInputAction: TextInputAction.next,
          autofillHints: const [AutofillHints.name],
          decoration: const InputDecoration(
            labelText: 'Display name',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _usernameController,
          enabled: !_isBusy,
          textInputAction: TextInputAction.done,
          autocorrect: false,
          enableSuggestions: false,
          onSubmitted: (_) => _submit(),
          decoration: const InputDecoration(
            labelText: 'Username',
            prefixText: '@',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'The Oi service checks username availability and its current rules.',
          style: Theme.of(context).textTheme.bodySmall,
        ),
        if (_errorMessage != null) ...[
          const SizedBox(height: 16),
          _InlineError(message: _errorMessage!),
        ],
        const SizedBox(height: 24),
        FilledButton(
          onPressed: _isBusy ? null : _submit,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 14),
            child: _isBusy
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Save profile'),
          ),
        ),
      ],
    );
  }
}

class LivenessRequiredPage extends StatelessWidget {
  const LivenessRequiredPage({
    super.key,
    required this.isSigningOut,
    required this.onSignOut,
  });

  final bool isSigningOut;
  final SignOut onSignOut;

  Future<void> _signOut(BuildContext context) async {
    try {
      await onSignOut();
    } catch (error) {
      if (!context.mounted) return;
      _showSignOutFailure(context, error);
    }
  }

  @override
  Widget build(BuildContext context) {
    return _AccountFrame(
      title: 'Face liveness check required',
      subtitle:
          'Your phone number is verified. Face liveness is a separate step, and an Oi UIN is not government identity verification.',
      onSignOut: isSigningOut ? null : () => _signOut(context),
      children: [
        const SizedBox(height: 8),
        const _NoticeCard(
          icon: Icons.face_retouching_natural_outlined,
          title: 'Verification is not available in this build',
          message:
              'No approved liveness provider or on-device method is configured. '
              'Oi will not mark this account as complete or open messaging until '
              'the real liveness flow is implemented.',
        ),
      ],
    );
  }
}

class AccountHomePage extends StatelessWidget {
  const AccountHomePage({
    super.key,
    required this.account,
    required this.isSigningOut,
    required this.onSignOut,
  });

  final OiAccount account;
  final bool isSigningOut;
  final SignOut onSignOut;

  Future<void> _signOut(BuildContext context) async {
    try {
      await onSignOut();
    } catch (error) {
      if (!context.mounted) return;
      _showSignOutFailure(context, error);
    }
  }

  @override
  Widget build(BuildContext context) {
    return _AccountFrame(
      title: 'Your Oi account',
      subtitle: account.displayName ?? 'Account verified',
      onSignOut: () => _signOut(context),
      children: [
        _NoticeCard(
          icon: Icons.verified_user_outlined,
          title: 'Verification status',
          message:
              'Phone: ${account.phoneVerified ? 'verified' : 'not verified'}\n'
              'Face liveness: ${account.livenessStatus}',
        ),
        if (account.username != null || account.uin != null) ...[
          const SizedBox(height: 16),
          _NoticeCard(
            icon: Icons.person_outline,
            title: 'Oi profile',
            message: [
              if (account.username != null) '@${account.username}',
              if (account.uin != null) ...[
                'Oi UIN: ${account.uin}',
                'This identifies your Oi account; it is not government ID or proof of real-world identity.',
              ],
            ].join('\n'),
          ),
        ],
        const SizedBox(height: 16),
        const _NoticeCard(
          icon: Icons.chat_bubble_outline,
          title: 'Messaging is not available yet',
          message:
              'This build does not yet include private conversations. No '
              'messages are sent, stored, or represented as delivered.',
        ),
      ],
    );
  }
}

class _AccountFrame extends StatelessWidget {
  const _AccountFrame({
    required this.title,
    required this.subtitle,
    required this.children,
    required this.onSignOut,
  });

  final String title;
  final String subtitle;
  final List<Widget> children;
  final VoidCallback? onSignOut;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Oi'),
        actions: [
          TextButton(onPressed: onSignOut, child: const Text('Sign out')),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 16, 24, 32),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    title,
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    subtitle,
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      color: colors.onSurfaceVariant,
                      height: 1.45,
                    ),
                  ),
                  const SizedBox(height: 28),
                  ...children,
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _NoticeCard extends StatelessWidget {
  const _NoticeCard({
    required this.icon,
    required this.title,
    required this.message,
  });

  final IconData icon;
  final String title;
  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: colors.primary),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  message,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: colors.onSurfaceVariant,
                    height: 1.45,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _InlineError extends StatelessWidget {
  const _InlineError({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Text(
      message,
      style: Theme.of(
        context,
      ).textTheme.bodyMedium?.copyWith(color: colors.error),
    );
  }
}

void _showSignOutFailure(BuildContext context, Object error) {
  ScaffoldMessenger.of(
    context,
  ).showSnackBar(SnackBar(content: Text(error.toString())));
}
