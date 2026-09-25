import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/custom_text_field.dart';
import 'register_screen.dart';
import '../main_navigation.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _emailOrPhoneController = TextEditingController(text: 'traveler@redbus.in');
  final TextEditingController _passwordController = TextEditingController(text: 'password123');
  bool _obscurePassword = true;

  void _onLogin() async {
    if (!_formKey.currentState!.validate()) return;

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final success = await authProvider.login(
      _emailOrPhoneController.text.trim(),
      _passwordController.text,
    );

    if (success && mounted) {
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const MainNavigationScreen()),
        (route) => false,
      );
    }
  }

  @override
  void dispose() {
    _emailOrPhoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      body: SingleChildScrollView(
        child: Column(
          children: [
            // Top Red Header
            Container(
              width: double.infinity,
              padding: const EdgeInsets.fromLTRB(24, 70, 24, 40),
              decoration: const BoxDecoration(
                gradient: AppColors.heroGradient,
                borderRadius: BorderRadius.vertical(bottom: Radius.circular(32)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: const Icon(Icons.directions_bus_rounded, color: AppColors.primary, size: 36),
                  ),
                  const SizedBox(height: 20),
                  const Text(
                    'Welcome back!',
                    style: TextStyle(
                      fontFamily: 'Outfit',
                      fontSize: 28,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Login to manage tickets & get exclusive discounts',
                    style: TextStyle(
                      fontSize: 13,
                      color: Colors.white.withOpacity(0.85),
                    ),
                  ),
                ],
              ),
            ),

            // Form Container
            Padding(
              padding: const EdgeInsets.all(24),
              child: Form(
                key: _formKey,
                child: Column(
                  children: [
                    CustomTextField(
                      label: 'Email or Mobile Number',
                      hintText: 'Enter email or 10-digit number',
                      controller: _emailOrPhoneController,
                      prefixIcon: Icons.person_outline_rounded,
                      validator: (val) {
                        if (val == null || val.trim().isEmpty) return 'Enter your email or phone';
                        return null;
                      },
                    ),

                    const SizedBox(height: 16),

                    CustomTextField(
                      label: 'Password',
                      hintText: 'Enter your password',
                      controller: _passwordController,
                      obscureText: _obscurePassword,
                      prefixIcon: Icons.lock_outline_rounded,
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword ? Icons.visibility_off_outlined : Icons.visibility_outlined,
                          size: 20,
                        ),
                        onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                      ),
                      validator: (val) {
                        if (val == null || val.length < 6) return 'Password must be at least 6 characters';
                        return null;
                      },
                    ),

                    const SizedBox(height: 8),

                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton(
                        onPressed: () {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Password reset link sent to your registered email!')),
                          );
                        },
                        child: const Text('Forgot Password?', style: TextStyle(fontSize: 12, color: AppColors.primary)),
                      ),
                    ),

                    if (authProvider.errorMessage != null) ...[
                      const SizedBox(height: 8),
                      Text(
                        authProvider.errorMessage!,
                        style: const TextStyle(color: Colors.redAccent, fontSize: 12),
                      ),
                    ],

                    const SizedBox(height: 16),

                    CustomButton(
                      text: 'LOGIN',
                      isLoading: authProvider.isLoading,
                      onPressed: _onLogin,
                    ),

                    const SizedBox(height: 20),

                    // Quick guest / explore mode
                    OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        side: const BorderSide(color: AppColors.border),
                        minimumSize: const Size(double.infinity, 48),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      onPressed: () {
                        Navigator.of(context).pushAndRemoveUntil(
                          MaterialPageRoute(builder: (_) => const MainNavigationScreen()),
                          (route) => false,
                        );
                      },
                      child: Text(
                        'Continue as Guest',
                        style: TextStyle(
                          color: isDark ? Colors.white70 : AppColors.textSecondary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),

                    const SizedBox(height: 30),

                    // Register Link
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'Don\'t have an account? ',
                          style: TextStyle(color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary),
                        ),
                        GestureDetector(
                          onTap: () {
                            Navigator.of(context).push(
                              MaterialPageRoute(builder: (_) => const RegisterScreen()),
                            );
                          },
                          child: const Text(
                            'Sign Up',
                            style: TextStyle(
                              color: AppColors.primary,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
