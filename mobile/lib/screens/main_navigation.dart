import 'package:flutter/material.dart';
import '../config/theme.dart';
import 'home/home_screen.dart';
import 'my_bookings/my_bookings_screen.dart';
import 'offers/offers_screen.dart';
import 'profile/help_support_screen.dart';
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
      const OffersScreen(),
      const HelpSupportScreen(),
      const ProfileScreen(),
    ];

    return Scaffold(
      body: AnimatedSwitcher(
        duration: const Duration(milliseconds: 250),
        switchInCurve: Curves.easeOut,
        switchOutCurve: Curves.easeIn,
        child: KeyedSubtree(
          key: ValueKey<int>(_currentIndex),
          child: pages[_currentIndex],
        ),
      ),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: isDark ? AppColors.darkSurface : Colors.white,
          border: Border(
            top: BorderSide(
              color: isDark ? const Color(0xFF374151) : const Color(0xFFEEEEEE),
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
        child: BottomNavigationBar(
          currentIndex: _currentIndex,
          onTap: _onTabTapped,
          type: BottomNavigationBarType.fixed,
          backgroundColor: isDark ? AppColors.darkSurface : Colors.white,
          selectedItemColor: AppColors.primary,
          unselectedItemColor: isDark ? Colors.grey[400] : const Color(0xFF718096),
          selectedLabelStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 11),
          unselectedLabelStyle: const TextStyle(fontWeight: FontWeight.w500, fontSize: 11),
          elevation: 0,
          items: const [
            BottomNavigationBarItem(
              icon: Icon(Icons.home_outlined),
              activeIcon: Icon(Icons.home_rounded),
              label: 'Home',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.checklist_rounded),
              activeIcon: Icon(Icons.checklist_rtl_rounded),
              label: 'Bookings',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.local_offer_outlined),
              activeIcon: Icon(Icons.local_offer_rounded),
              label: 'Offers',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.chat_bubble_outline_rounded),
              activeIcon: Icon(Icons.chat_bubble_rounded),
              label: 'Help',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.person_outline_rounded),
              activeIcon: Icon(Icons.person_rounded),
              label: 'My Account',
            ),
          ],
        ),
      ),
    );
  }
}
