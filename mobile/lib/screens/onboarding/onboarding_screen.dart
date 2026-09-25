import 'package:flutter/material.dart';
import '../../config/theme.dart';
import '../../widgets/custom_button.dart';
import '../main_navigation.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  final List<Map<String, dynamic>> _slides = [
    {
      'icon': Icons.location_searching_rounded,
      'title': '10,000+ Buses Across India',
      'subtitle': 'Search and book tickets instantly across 100,000+ verified routes with real-time seat availability.',
      'color': AppColors.primary,
    },
    {
      'icon': Icons.gps_fixed_rounded,
      'title': 'Live Bus GPS Tracking',
      'subtitle': 'Track your bus in real-time on live maps. Share your live journey with family for safety & peace of mind.',
      'color': const Color(0xFF2563EB),
    },
    {
      'icon': Icons.verified_user_rounded,
      'title': 'Primo Guaranteed & Instant Refunds',
      'subtitle': 'Experience top-rated buses with guaranteed punctuality, sanitized berths, and 100% instant refund on cancellation.',
      'color': const Color(0xFF059669),
    },
  ];

  void _onFinish() {
    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        pageBuilder: (_, __, ___) => const MainNavigationScreen(),
        transitionsBuilder: (_, animation, __, child) {
          return FadeTransition(opacity: animation, child: child);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          TextButton(
            onPressed: _onFinish,
            child: const Text(
              'SKIP',
              style: TextStyle(
                color: AppColors.textSecondary,
                fontWeight: FontWeight.bold,
                fontSize: 14,
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                onPageChanged: (index) => setState(() => _currentPage = index),
                itemCount: _slides.length,
                itemBuilder: (context, index) {
                  final slide = _slides[index];
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 32),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        // Icon Box with glowing ring
                        Container(
                          width: 130,
                          height: 130,
                          decoration: BoxDecoration(
                            color: (slide['color'] as Color).withOpacity(0.1),
                            shape: BoxShape.circle,
                          ),
                          child: Center(
                            child: Container(
                              width: 90,
                              height: 90,
                              decoration: BoxDecoration(
                                color: slide['color'] as Color,
                                shape: BoxShape.circle,
                                boxShadow: [
                                  BoxShadow(
                                    color: (slide['color'] as Color).withOpacity(0.4),
                                    blurRadius: 20,
                                    offset: const Offset(0, 8),
                                  ),
                                ],
                              ),
                              child: Icon(
                                slide['icon'] as IconData,
                                size: 44,
                                color: Colors.white,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 48),
                        Text(
                          slide['title'] as String,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontFamily: 'Outfit',
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                            color: AppColors.textPrimary,
                            height: 1.25,
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          slide['subtitle'] as String,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 14,
                            color: AppColors.textSecondary,
                            height: 1.5,
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),

            // Indicator Dots & Bottom Action
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(
                      _slides.length,
                      (index) => AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        width: _currentPage == index ? 26 : 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: _currentPage == index ? AppColors.primary : AppColors.border,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),
                  CustomButton(
                    text: _currentPage == _slides.length - 1 ? 'GET STARTED' : 'CONTINUE',
                    onPressed: () {
                      if (_currentPage == _slides.length - 1) {
                        _onFinish();
                      } else {
                        _pageController.nextPage(
                          duration: const Duration(milliseconds: 350),
                          curve: Curves.easeInOut,
                        );
                      }
                    },
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
