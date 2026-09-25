import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:redbus_app/main.dart';
import 'package:redbus_app/providers/theme_provider.dart';
import 'package:redbus_app/providers/auth_provider.dart';
import 'package:redbus_app/providers/search_provider.dart';
import 'package:redbus_app/providers/booking_provider.dart';
import 'package:redbus_app/providers/chat_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  testWidgets('RedBusApp smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider(create: (_) => ThemeProvider()),
          ChangeNotifierProvider(create: (_) => AuthProvider()),
          ChangeNotifierProvider(create: (_) => SearchProvider()),
          ChangeNotifierProvider(create: (_) => BookingProvider()),
          ChangeNotifierProvider(create: (_) => ChatProvider()),
        ],
        child: const RedBusApp(),
      ),
    );

    // Initial pump and advance past splash screen timer
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 3000));
    await tester.pumpAndSettle();
  });
}
