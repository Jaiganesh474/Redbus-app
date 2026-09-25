import 'package:flutter/material.dart';
import '../config/theme.dart';
import 'home/home_screen.dart';
import 'my_bookings/my_bookings_screen.dart';
import 'ai_assistant/redbus_ai_screen.dart';
import 'offers/offers_screen.dart';
import 'profile/profile_screen.dart';

class MainNavigationScreen extends StatefulWidget {
  final int initialIndex;

  const MainNavigationScreen({super.key, this.initialIndex = 0});

  @override
  State<MainNavigationScreen> createState() => _MainNavigationScreenState();
}

class _MainNavigationScreenState extends State<MainNavigationScreen> {
  late int _currentIndex;

  @override
  void initState() {
    super.initState();
    _currentIndex = widget.initialIndex;
  }

  void _onTabTapped(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final pages = [
      HomeScreen(onNavigateTab: _onTabTapped),
      const MyBookingsScreen(),
      const RedBusAiScreen(),
      const OffersScreen(),
      const ProfileScreen(),
    ];

    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: pages,
      ),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: isDark ? AppColors.darkSurface : Colors.white,
          border: Border(
            top: BorderSide(
              color: isDark ? const Color(0xFF374151) : AppColors.border,
              width: 1,
            ),
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.04),
              blurRadius: 10,
              offset: const Offset(0, -2),
            ),
          ],
        ),
        child: NavigationBar(
          selectedIndex: _currentIndex,
          onDestinationSelected: _onTabTapped,
          backgroundColor: isDark ? AppColors.darkSurface : Colors.white,
          indicatorColor: AppColors.primaryLight,
          surfaceTintColor: Colors.transparent,
          elevation: 0,
          destinations: const [
            NavigationDestination(
              icon: Icon(Icons.directions_bus_outlined),
              selectedIcon: Icon(Icons.directions_bus_rounded, color: AppColors.primary),
              label: 'Bus Tickets',
            ),
            NavigationDestination(
              icon: Icon(Icons.confirmation_number_outlined),
              selectedIcon: Icon(Icons.confirmation_number_rounded, color: AppColors.primary),
              label: 'Bookings',
            ),
            NavigationDestination(
              icon: Icon(Icons.auto_awesome_outlined, color: Colors.amber),
              selectedIcon: Icon(Icons.auto_awesome_rounded, color: AppColors.primary),
              label: 'redBus AI',
            ),
            NavigationDestination(
              icon: Icon(Icons.local_offer_outlined),
              selectedIcon: Icon(Icons.local_offer_rounded, color: AppColors.primary),
              label: 'Offers',
            ),
            NavigationDestination(
              icon: Icon(Icons.person_outline_rounded),
              selectedIcon: Icon(Icons.person_rounded, color: AppColors.primary),
              label: 'Account',
            ),
          ],
        ),
      ),
    );
  }
}
