# Oi

Oi is a Flutter app for private communication. Android is the first target;
the same Dart application is structured to support iOS.

## Current status

The Flutter app now has a backend-facing phone OTP sign-in and session/profile
flow. It does not include a backend server, SMS delivery provider, or face
liveness integration. Those steps are blocked visibly; the app never treats a
local or sample response as a verified production account. Private messaging,
groups, status, and retention features are not implemented yet.

## Requirements

- Flutter stable (CI currently pins Flutter 3.44.1)
- Android SDK and Java 17 for Android builds
- macOS with Xcode for iOS builds

## Run and verify

```sh
flutter pub get
flutter run
flutter analyze
flutter test
flutter build apk --debug
```

An API base URL can be supplied with
`--dart-define=OI_API_BASE_URL=https://api.example.com`. The client uses the
versioned API in [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md). HTTPS is
required except for loopback development URLs. Without an available backend,
network actions fail explicitly and do not claim success.

The app stores the opaque session bearer only with the platform secure-storage
plugin. The service must still implement server-authoritative single-device
sessions, OTP validation/rate limits, account states, and token revocation.
Client-side OTP countdowns do not replace backend enforcement. Android app
backup is disabled so secure-storage keys cannot be restored without their
device-bound key material; chat backup behavior remains undecided and no chat
history is implemented.

## Release setup

Flutter generated the temporary Android application ID and iOS bundle ID as
`com.example.oi`. Replace them with the approved Oi identifiers before release.
Production releases also require the provider, hosting, signing, privacy, and
store prerequisites recorded in `HUMAN_PLAN.md`.
