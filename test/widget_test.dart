import 'package:flutter_test/flutter_test.dart';
import 'package:oi/app.dart';
import 'package:oi/app_config.dart';

void main() {
  testWidgets('explains that backend features are not active', (tester) async {
    await tester.pumpWidget(OiApp(config: const AppConfig(apiBaseUri: null)));

    expect(find.text('Oi'), findsOneWidget);
    expect(find.text('Backend not configured'), findsOneWidget);
    expect(
      find.text('Sign-in and messaging are not active in this build.'),
      findsOneWidget,
    );
  });

  testWidgets('does not imply an endpoint is a working backend', (
    tester,
  ) async {
    await tester.pumpWidget(
      OiApp(
        config: AppConfig.fromEnvironment(
          apiBaseUrl: 'https://api.example.test/v1',
        ),
      ),
    );

    expect(find.text('Backend endpoint configured'), findsOneWidget);
    expect(
      find.text(
        'An API address is set, but this build does not connect or authenticate yet.',
      ),
      findsOneWidget,
    );
  });
}
