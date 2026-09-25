import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../config/theme.dart';
import '../../models/booking_model.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/ticket_card.dart';
import '../my_bookings/live_tracker_screen.dart';
import '../main_navigation.dart';

class BookingSuccessScreen extends StatelessWidget {
  final Booking booking;

  const BookingSuccessScreen({super.key, required this.booking});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return PopScope(
      canPop: false,
      onPopInvoked: (didPop) {
        if (!didPop) {
          Navigator.of(context).pushAndRemoveUntil(
            MaterialPageRoute(builder: (_) => const MainNavigationScreen()),
            (route) => false,
          );
        }
      },
      child: Scaffold(
        appBar: AppBar(
          automaticallyImplyLeading: false,
          title: const Text('Booking Confirmed'),
          actions: [
            IconButton(
              icon: const Icon(Icons.close_rounded),
              onPressed: () {
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const MainNavigationScreen()),
                  (route) => false,
                );
              },
            ),
          ],
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(vertical: 20),
          child: Column(
            children: [
              // Animated Success Tick Box
              Container(
                width: 80,
                height: 80,
                decoration: BoxDecoration(
                  color: AppColors.successLight,
                  shape: BoxShape.circle,
                  border: Border.all(color: AppColors.success, width: 2),
                ),
                child: const Center(
                  child: Icon(
                    Icons.check_circle_rounded,
                    size: 54,
                    color: AppColors.success,
                  ),
                ),
              ),

              const SizedBox(height: 16),

              const Text(
                'Ticket Confirmed!',
                style: TextStyle(
                  fontFamily: 'Outfit',
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),

              const SizedBox(height: 4),

              Text(
                'We have sent your e-ticket & SMS to your phone',
                style: TextStyle(
                  fontSize: 12,
                  color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                ),
              ),

              const SizedBox(height: 16),

              // PNR Copy Chip
              InkWell(
                onTap: () {
                  Clipboard.setData(ClipboardData(text: booking.pnr));
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('PNR copied to clipboard!'),
                      duration: Duration(seconds: 2),
                      backgroundColor: AppColors.dark,
                    ),
                  );
                },
                borderRadius: BorderRadius.circular(20),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                    color: isDark ? AppColors.darkCard : const Color(0xFFF3F4F6),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        'PNR: ${booking.pnr}',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, letterSpacing: 0.5),
                      ),
                      const SizedBox(width: 8),
                      const Icon(Icons.copy_rounded, size: 16, color: AppColors.primary),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 20),

              // Full Digital Ticket Card
              TicketCard(
                booking: booking,
                onLiveTrack: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => LiveTrackerScreen(booking: booking)),
                  );
                },
              ),

              const SizedBox(height: 24),

              // Actions
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  children: [
                    CustomButton(
                      text: 'TRACK LIVE BUS',
                      icon: Icons.navigation_rounded,
                      onPressed: () {
                        Navigator.of(context).push(
                          MaterialPageRoute(builder: (_) => LiveTrackerScreen(booking: booking)),
                        );
                      },
                    ),
                    const SizedBox(height: 10),
                    CustomButton(
                      text: 'GO TO HOME',
                      isOutlined: true,
                      onPressed: () {
                        Navigator.of(context).pushAndRemoveUntil(
                          MaterialPageRoute(builder: (_) => const MainNavigationScreen()),
                          (route) => false,
                        );
                      },
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 30),
            ],
          ),
        ),
      ),
    );
  }
}
