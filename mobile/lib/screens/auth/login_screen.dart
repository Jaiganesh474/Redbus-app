import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/custom_text_field.dart';
import 'register_screen.dart';
import 'forgot_password_screen.dart';
import '../main_navigation.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _emailOrPhoneController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _mobileController = TextEditingController();
  final TextEditingController _otpController = TextEditingController();

  bool _isOtpMode = true;
  bool _otpSent = false;
  bool _obscurePassword = true;
  String? _previewOtp;

  void _onPasswordLogin() async {
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

  void _onSendOtp() async {
    final phone = _mobileController.text.trim();
    if (phone.isEmpty || phone.length < 7) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter a valid mobile number')),
      );
      return;
    }

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    try {
      final res = await authProvider.sendMobileOtp(phone, purpose: 'LOGIN');
      setState(() {
        _otpSent = true;
        _previewOtp = res['previewOtp'];
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(res['message'] ?? 'OTP sent to $phone'),
            backgroundColor: AppColors.success,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(authProvider.errorMessage ?? 'Failed to send OTP'),
            backgroundColor: AppColors.error,
          ),
        );
      }
    }
  }

  void _onVerifyOtp() async {
    final phone = _mobileController.text.trim();
    final otp = _otpController.text.trim();
    if (otp.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter 6-digit OTP code')),
      );
      return;
    }

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final success = await authProvider.loginWithMobileOtp(phone, otp);

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
    _mobileController.dispose();
    _otpController.dispose();
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
              padding: const EdgeInsets.fromLTRB(24, 60, 24, 30),
              decoration: const BoxDecoration(
                gradient: AppColors.heroGradient,
                borderRadius: BorderRadius.vertical(bottom: Radius.circular(32)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: const Icon(Icons.directions_bus_rounded, color: AppColors.primary, size: 32),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Welcome back!',
                    style: TextStyle(
                      fontFamily: 'Outfit',
                      fontSize: 26,
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
                  const SizedBox(height: 16),

                  // Mode Toggle
                  Container(
                    decoration: BoxDecoration(
                      color: Colors.black.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    padding: const EdgeInsets.all(4),
                    child: Row(
                      children: [
                        Expanded(
                          child: GestureDetector(
                            onTap: () => setState(() {
                              _isOtpMode = true;
                            }),
                            child: Container(
                              padding: const EdgeInsets.symmetric(vertical: 8),
                              decoration: BoxDecoration(
                                color: _isOtpMode ? Colors.white : Colors.transparent,
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(
                                    Icons.phone_android_rounded,
                                    size: 16,
                                    color: _isOtpMode ? AppColors.primary : Colors.white,
                                  ),
                                  const SizedBox(width: 6),
                                  Text(
                                    'Mobile OTP',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: _isOtpMode ? AppColors.primary : Colors.white,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                        Expanded(
                          child: GestureDetector(
                            onTap: () => setState(() {
                              _isOtpMode = false;
                            }),
                            child: Container(
                              padding: const EdgeInsets.symmetric(vertical: 8),
                              decoration: BoxDecoration(
                                color: !_isOtpMode ? Colors.white : Colors.transparent,
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(
                                    Icons.lock_outline_rounded,
                                    size: 16,
                                    color: !_isOtpMode ? AppColors.primary : Colors.white,
                                  ),
                                  const SizedBox(width: 6),
                                  Text(
                                    'Password',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: !_isOtpMode ? AppColors.primary : Colors.white,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            // Form Container
            Padding(
              padding: const EdgeInsets.all(24),
              child: _isOtpMode
                  ? Column(
                      children: [
                        CustomTextField(
                          label: 'Mobile Number',
                          hintText: 'e.g. 9876543210',
                          controller: _mobileController,
                          prefixIcon: Icons.phone_rounded,
                          keyboardType: TextInputType.phone,
                          enabled: !_otpSent,
                        ),

                        if (_otpSent) ...[
                          const SizedBox(height: 16),
                          CustomTextField(
                            label: '6-Digit OTP',
                            hintText: 'Enter 6-digit code',
                            controller: _otpController,
                            prefixIcon: Icons.key_rounded,
                            keyboardType: TextInputType.number,
                          ),
                          if (_previewOtp != null)
                            Padding(
                              padding: const EdgeInsets.only(top: 8),
                              child: Text(
                                'Preview OTP: $_previewOtp',
                                style: const TextStyle(
                                  color: AppColors.success,
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                        ],

                        const SizedBox(height: 20),

                        CustomButton(
                          text: _otpSent ? 'VERIFY & SIGN IN' : 'GET OTP',
                          isLoading: authProvider.isLoading,
                          onPressed: _otpSent ? _onVerifyOtp : _onSendOtp,
                        ),

                        if (_otpSent)
                          TextButton(
                            onPressed: () => setState(() {
                              _otpSent = false;
                              _otpController.clear();
                            }),
                            child: const Text('Change Number'),
                          ),
                      ],
                    )
                  : Form(
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
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(builder: (_) => const ForgotPasswordScreen()),
                                );
                              },
                              style: TextButton.styleFrom(
                                padding: EdgeInsets.zero,
                                minimumSize: const Size(50, 30),
                                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              ),
                              child: const Text(
                                'Forgot Password?',
                                style: TextStyle(
                                  color: AppColors.primary,
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ),
                          ),

                          const SizedBox(height: 24),

                          CustomButton(
                            text: 'LOG IN',
                            isLoading: authProvider.isLoading,
                            onPressed: _onPasswordLogin,
                          ),
                        ],
                      ),
                    ),
            ),

            // Sign Up link
            Padding(
              padding: const EdgeInsets.only(bottom: 24),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    "Don't have an account? ",
                    style: TextStyle(
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondary,
                      fontSize: 13,
                    ),
                  ),
                  GestureDetector(
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const RegisterScreen()),
                      );
                    },
                    child: const Text(
                      'Sign Up',
                      style: TextStyle(
                        color: AppColors.primary,
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
