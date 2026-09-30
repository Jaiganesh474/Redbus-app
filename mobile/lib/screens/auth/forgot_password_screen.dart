import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/custom_text_field.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _otpController = TextEditingController();
  final TextEditingController _newPasswordController = TextEditingController();

  bool _isMobileMode = true;
  bool _otpSent = false;
  bool _isLoading = false;
  bool _isResetSuccess = false;
  String? _previewOtp;

  void _onSendOtp() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    if (_isMobileMode) {
      final phone = _phoneController.text.trim();
      if (phone.isEmpty || phone.length < 7) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please enter a valid mobile number')),
        );
        return;
      }

      setState(() => _isLoading = true);
      try {
        final res = await authProvider.sendMobileOtp(phone, purpose: 'RESET_PASSWORD');
        setState(() {
          _isLoading = false;
          _otpSent = true;
          _previewOtp = res['previewOtp'];
        });
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(res['message'] ?? 'Reset OTP sent to $phone'),
              backgroundColor: AppColors.success,
            ),
          );
        }
      } catch (e) {
        setState(() => _isLoading = false);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(authProvider.errorMessage ?? 'Failed to send reset OTP'),
              backgroundColor: AppColors.error,
            ),
          );
        }
      }
    } else {
      final email = _emailController.text.trim();
      if (email.isEmpty || !email.contains('@')) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please enter a valid email address')),
        );
        return;
      }

      setState(() => _isLoading = true);
      await authProvider.forgotPassword(email);
      setState(() {
        _isLoading = false;
        _otpSent = true;
      });
    }
  }

  void _onResetPassword() async {
    final phone = _phoneController.text.trim();
    final otp = _otpController.text.trim();
    final newPassword = _newPasswordController.text;

    if (otp.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter the 6-digit OTP')),
      );
      return;
    }

    if (newPassword.length < 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Password must be at least 6 characters')),
      );
      return;
    }

    setState(() => _isLoading = true);
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final success = await authProvider.resetPasswordWithMobileOtp(phone, otp, newPassword);

    setState(() => _isLoading = false);
    if (success && mounted) {
      setState(() => _isResetSuccess = true);
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(authProvider.errorMessage ?? 'Failed to reset password'),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }

  @override
  void dispose() {
    _phoneController.dispose();
    _emailController.dispose();
    _otpController.dispose();
    _newPasswordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Reset Password'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.primaryLight,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.lock_reset_rounded, size: 40, color: AppColors.primary),
            ),
            const SizedBox(height: 20),
            const Text(
              'Forgot your password?',
              style: TextStyle(fontFamily: 'Outfit', fontSize: 24, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              'Receive a secure 6-digit verification code to reset your account password.',
              style: TextStyle(fontSize: 13, color: AppColors.textSecondary, height: 1.4),
            ),
            const SizedBox(height: 16),

            if (!_otpSent && !_isResetSuccess) ...[
              // Method toggle
              Row(
                children: [
                  ChoiceChip(
                    label: const Text('Mobile SMS OTP'),
                    selected: _isMobileMode,
                    onSelected: (val) => setState(() => _isMobileMode = true),
                  ),
                  const SizedBox(width: 8),
                  ChoiceChip(
                    label: const Text('Email OTP'),
                    selected: !_isMobileMode,
                    onSelected: (val) => setState(() => _isMobileMode = false),
                  ),
                ],
              ),
              const SizedBox(height: 20),

              if (_isMobileMode)
                CustomTextField(
                  label: 'Registered Mobile Number',
                  hintText: 'e.g. 9876543210',
                  controller: _phoneController,
                  prefixIcon: Icons.phone_rounded,
                  keyboardType: TextInputType.phone,
                )
              else
                CustomTextField(
                  label: 'Registered Email',
                  hintText: 'e.g. traveler@redbus.in',
                  controller: _emailController,
                  prefixIcon: Icons.email_outlined,
                  keyboardType: TextInputType.emailAddress,
                ),

              const SizedBox(height: 24),
              CustomButton(
                text: 'SEND RESET OTP',
                isLoading: _isLoading,
                onPressed: _onSendOtp,
              ),
            ] else if (_isMobileMode && !_isResetSuccess) ...[
              CustomTextField(
                label: '6-Digit OTP',
                hintText: 'Enter 6-digit code',
                controller: _otpController,
                prefixIcon: Icons.key_rounded,
                keyboardType: TextInputType.number,
              ),
              if (_previewOtp != null)
                Padding(
                  padding: const EdgeInsets.only(top: 6),
                  child: Text(
                    'Preview OTP: $_previewOtp',
                    style: const TextStyle(
                      color: AppColors.success,
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              const SizedBox(height: 16),
              CustomTextField(
                label: 'New Password',
                hintText: 'At least 6 characters',
                controller: _newPasswordController,
                obscureText: true,
                prefixIcon: Icons.lock_outline_rounded,
              ),
              const SizedBox(height: 24),
              CustomButton(
                text: 'RESET & SAVE PASSWORD',
                isLoading: _isLoading,
                onPressed: _onResetPassword,
              ),
            ] else ...[
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.successLight,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.success.withOpacity(0.4)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.check_circle_rounded, color: AppColors.success, size: 24),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        _isResetSuccess
                            ? 'Password reset successfully! You are now logged in.'
                            : 'Password reset code sent. Please check your messages.',
                        style: const TextStyle(fontSize: 13, color: AppColors.success, fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              CustomButton(
                text: 'BACK TO HOME',
                isOutlined: true,
                onPressed: () => Navigator.pop(context),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
