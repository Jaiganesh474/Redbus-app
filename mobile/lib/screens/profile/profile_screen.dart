import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../providers/auth_provider.dart';
import '../../providers/theme_provider.dart';
import 'saved_travellers_screen.dart';
import '../auth/login_screen.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    final themeProvider = Provider.of<ThemeProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final user = authProvider.user;

    return Scaffold(
      appBar: AppBar(
        title: const Text('My Account'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Column(
          children: [
            // User Header Card
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: isDark ? AppColors.darkCard : Colors.white,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
              ),
              child: Row(
                children: [
                  Container(
                    width: 58,
                    height: 58,
                    decoration: const BoxDecoration(
                      gradient: AppColors.primaryGradient,
                      shape: BoxShape.circle,
                    ),
                    child: Center(
                      child: Text(
                        (user?.name.isNotEmpty ?? false) ? user!.name[0].toUpperCase() : 'U',
                        style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user?.name ?? 'RedBus Traveller',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          user?.email ?? 'traveler@redbus.in',
                          style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
                        ),
                        Text(
                          user?.phone ?? '+91 98765 43210',
                          style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // RedBus Wallet Card
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF1E293B), Color(0xFF0F172A)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.1),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.account_balance_wallet_rounded, color: Colors.amber, size: 24),
                  ),
                  const SizedBox(width: 14),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'RedBus Wallet Balance',
                        style: TextStyle(color: Colors.white70, fontSize: 12),
                      ),
                      Text(
                        '₹${user?.walletBalance.toInt() ?? 250}',
                        style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.w900),
                      ),
                    ],
                  ),
                  const Spacer(),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.primary,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Text(
                      '100% Usable',
                      style: TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 20),

            // Settings & Preferences List
            Container(
              decoration: BoxDecoration(
                color: isDark ? AppColors.darkCard : Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
              ),
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.people_alt_outlined, color: AppColors.primary),
                    title: const Text('Saved Co-Travellers', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    subtitle: Text('${user?.savedTravellers.length ?? 0} saved passenger(s)', style: const TextStyle(fontSize: 12)),
                    trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 14),
                    onTap: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(builder: (_) => const SavedTravellersScreen()),
                      );
                    },
                  ),
                  const Divider(height: 1),
                  SwitchListTile(
                    secondary: const Icon(Icons.dark_mode_outlined, color: AppColors.primary),
                    title: const Text('Dark Mode', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    subtitle: const Text('Toggle app appearance', style: TextStyle(fontSize: 12)),
                    value: themeProvider.isDarkMode,
                    activeColor: AppColors.primary,
                    onChanged: (_) => themeProvider.toggleTheme(),
                  ),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.notifications_none_rounded, color: AppColors.primary),
                    title: const Text('Push Notifications', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    subtitle: const Text('Trip alerts, offers & updates', style: TextStyle(fontSize: 12)),
                    trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 14),
                    onTap: () {},
                  ),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.translate_rounded, color: AppColors.primary),
                    title: const Text('App Language', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    trailing: const Text('English', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppColors.primary)),
                    onTap: () {},
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // Support & Legal
            Container(
              decoration: BoxDecoration(
                color: isDark ? AppColors.darkCard : Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
              ),
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.help_outline_rounded, color: AppColors.primary),
                    title: const Text('24/7 Customer Support', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    subtitle: const Text('Help center, FAQs, Chat', style: TextStyle(fontSize: 12)),
                    trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 14),
                    onTap: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Support line: 1800-210-9999 (Toll Free)')),
                      );
                    },
                  ),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.policy_outlined, color: AppColors.primary),
                    title: const Text('Terms & Privacy Policy', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 14),
                    onTap: () {},
                  ),
                ],
              ),
            ),

            const SizedBox(height: 20),

            // Logout / Switch Account Button
            ListTile(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: const BorderSide(color: Colors.redAccent, width: 1),
              ),
              leading: const Icon(Icons.logout_rounded, color: Colors.redAccent),
              title: const Text(
                'Logout / Switch Account',
                style: TextStyle(color: Colors.redAccent, fontWeight: FontWeight.bold, fontSize: 14),
              ),
              onTap: () {
                authProvider.logout();
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                  (route) => false,
                );
              },
            ),

            const SizedBox(height: 24),
            const Text(
              'redBus Mobile v1.0.0 • Made with ❤️ in India',
              style: TextStyle(fontSize: 11, color: AppColors.textMuted),
            ),
            const SizedBox(height: 30),
          ],
        ),
      ),
    );
  }
}
