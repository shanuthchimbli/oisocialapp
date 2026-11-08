# Oi

Oi is a Flutter app for private communication. Android is the first target;
the same Dart application is structured to support iOS.

## Current status

This repository currently contains the mobile app foundation only. It does not
provide account creation, OTP delivery, face liveness, messaging, or a backend.
The app states this clearly rather than treating a configured API address as
an available service.

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
`--dart-define=OI_API_BASE_URL=https://api.example.com`.
It is configuration only; no API requests or authentication are implemented
yet. HTTPS is required except for loopback development URLs.

## Release setup

Flutter generated the temporary Android application ID and iOS bundle ID as
`com.example.oi`. Replace them with the approved Oi identifiers before release.
Production releases also require the provider, hosting, signing, privacy, and
store prerequisites recorded in `HUMAN_PLAN.md`.
