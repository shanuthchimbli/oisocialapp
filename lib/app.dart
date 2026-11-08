import 'package:flutter/material.dart';

import 'app_config.dart';
import 'welcome_page.dart';

class OiApp extends StatelessWidget {
  OiApp({super.key, AppConfig? config})
    : config = config ?? AppConfig.fromEnvironment();

  final AppConfig config;

  @override
  Widget build(BuildContext context) {
    const seedColor = Color(0xFF4757D6);

    return MaterialApp(
      title: 'Oi',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: seedColor),
        scaffoldBackgroundColor: const Color(0xFFF7F7FA),
        useMaterial3: true,
      ),
      home: WelcomePage(config: config),
    );
  }
}
