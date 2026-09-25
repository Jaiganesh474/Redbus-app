import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/custom_text_field.dart';
import '../main_navigation.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _obscurePassword = true;

  void _onRegister() async {
    if (!_formKey.currentState!.validate()) return;

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final success = await authProvider.register(
      name: _nameController.text.trim(),
      email: _emailController.text.trim(),
      phone: _phoneController.text.trim(),
      password: _passwordController.text,
    );

    if (success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Account created successfully! Welcome to redBus! 🎉'),
          backgroundColor: AppColors.success,
        ),
      );
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const MainNavigationScreen()),
        (route) => false,
      );
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Create Account'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Join redBus Family',
                style: TextStyle(
                  fontFamily: 'Outfit',
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'Earn ₹200 wallet reward on sign up',
                style: TextStyle(fontSize: 13, color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary),
              ),
              const SizedBox(height: 24),

              CustomTextField(
                label: 'Full Name',
                hintText: 'Enter your full name',
                controller: _nameController,
                prefixIcon: Icons.person_outline_rounded,
                validator: (val) => (val == null || val.trim().isEmpty) ? 'Enter your name' : null,
              ),

              const SizedBox(height: 16),

              CustomTextField(
                label: 'Email ID',
                hintText: 'name@example.com',
                controller: _emailController,
                prefixIcon: Icons.email_outlined,
                keyboardType: TextInputType.emailAddress,
                validator: (val) => (val == null || !val.contains('@')) ? 'Enter a valid email' : null,
              ),

              const SizedBox(height: 16),

              CustomTextField(
                label: 'Mobile Number',
                hintText: '10-digit mobile number',
                controller: _phoneController,
                prefixIcon: Icons.phone_android_rounded,
                keyboardType: TextInputType.phone,
                validator: (val) => (val == null || val.length < 10) ? 'Enter valid 10-digit phone' : null,
              ),

              const SizedBox(height: 16),

              CustomTextField(
                label: 'Password',
                hintText: 'Create a password',
                controller: _passwordController,
                obscureText: _obscurePassword,
                prefixIcon: Icons.lock_outline_rounded,
                suffixIcon: IconButton(
                  icon: Icon(_obscurePassword ? Icons.visibility_off_outlined : Icons.visibility_outlined, size: 20),
                  onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                ),
                validator: (val) => (val == null || val.length < 6) ? 'Password must be at least 6 characters' : null,
              ),

              const SizedBox(height: 24),

              CustomButton(
                text: 'CREATE ACCOUNT & EARN ₹200',
                isLoading: authProvider.isLoading,
                onPressed: _onRegister,
              ),

              const SizedBox(height: 20),

              Center(
                child: GestureDetector(
                  onTap: () => Navigator.pop(context),
                  child: const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text('Already have an account? '),
                      Text('Login', style: TextStyle(color: AppColors.primary, fontWeight: FontWeight.bold)),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
