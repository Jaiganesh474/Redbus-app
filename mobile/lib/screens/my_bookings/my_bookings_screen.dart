import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../models/booking_model.dart';
import '../../providers/booking_provider.dart';
import '../../widgets/ticket_card.dart';
import '../../widgets/custom_button.dart';
import 'live_tracker_screen.dart';
import 'booking_detail_screen.dart';
import '../main_navigation.dart';

class MyBookingsScreen extends StatelessWidget {
  const MyBookingsScreen({super.key});

  void _showCancelDialog(BuildContext context, Booking booking, BookingProvider bookingProvider) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cancel Booking?'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('PNR: ${booking.pnr}'),
            const SizedBox(height: 8),
            Text(
              'Route: ${booking.sourceCity} → ${booking.destinationCity}',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            const Text(
              'Refund Breakdown:',
              style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
            ),
            const SizedBox(height: 4),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Refund percentage:'),
                Text(
                  '90% (₹${(booking.totalAmount * 0.9).toStringAsFixed(1)})',
                  style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.success),
                ),
              ],
            ),
            const SizedBox(height: 2),
            const Text(
              'Amount will be credited back instantly to your RedBus Wallet.',
              style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Keep Ticket'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
            onPressed: () {
              bookingProvider.cancelBookingByPnr(booking.pnr);
              Navigator.pop(ctx);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Booking cancelled. Instant refund credited to Wallet!'),
                  backgroundColor: AppColors.dark,
                ),
              );
            },
            child: const Text('Confirm Cancel'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bookingProvider = Provider.of<BookingProvider>(context);
    final bookings = bookingProvider.myBookings;

    final upcomingBookings = bookings.where((b) => b.status == BookingStatus.confirmed).toList();
    final completedBookings = bookings.where((b) => b.status == BookingStatus.completed).toList();
    final cancelledBookings = bookings.where((b) => b.status == BookingStatus.cancelled).toList();

    return DefaultTabController(
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('My Bookings'),
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh_rounded),
              tooltip: 'Refresh',
              onPressed: () => bookingProvider.fetchMyBookings(),
            ),
          ],
          bottom: const TabBar(
            indicatorColor: AppColors.primary,
            labelColor: AppColors.primary,
            unselectedLabelColor: AppColors.textSecondary,
            labelStyle: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
            tabs: [
              Tab(text: 'Upcoming'),
              Tab(text: 'Completed'),
              Tab(text: 'Cancelled'),
            ],
          ),
        ),
        body: RefreshIndicator(
          onRefresh: () => bookingProvider.fetchMyBookings(),
          color: AppColors.primary,
          child: TabBarView(
            children: [
              _buildBookingsList(
                context,
                upcomingBookings,
                bookingProvider,
                'No upcoming trips',
                'Looks like you haven\'t booked any bus trips yet. Plan your next adventure now!',
                showBookNow: true,
              ),
              _buildBookingsList(
                context,
                completedBookings,
                bookingProvider,
                'No completed trips',
                'Your completed past journeys will be listed here.',
              ),
              _buildBookingsList(
                context,
                cancelledBookings,
                bookingProvider,
                'No cancelled trips',
                'Your cancelled bookings and instant refund history appear here.',
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBookingsList(
    BuildContext context,
    List<Booking> list,
    BookingProvider bookingProvider,
    String title,
    String subtitle, {
    bool showBookNow = false,
  }) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    if (list.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: 90,
                height: 90,
                decoration: BoxDecoration(
                  color: isDark ? const Color(0xFF1F2937) : const Color(0xFFFEE2E2),
                  shape: BoxShape.circle,
                ),
                child: const Center(
                  child: Icon(
                    Icons.luggage_rounded,
                    size: 46,
                    color: AppColors.primary,
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Text(
                title,
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                subtitle,
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppColors.textSecondary, fontSize: 13, height: 1.4),
              ),
              if (showBookNow) ...[
                const SizedBox(height: 24),
                CustomButton(
                  width: 160,
                  text: 'BOOK NOW',
                  onPressed: () {
                    Navigator.of(context).pushAndRemoveUntil(
                      MaterialPageRoute(builder: (_) => const MainNavigationScreen(initialIndex: 0)),
                      (route) => false,
                    );
                  },
                ),
              ],
            ],
          ),
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(vertical: 12),
      itemCount: list.length,
      itemBuilder: (context, index) {
        final b = list[index];
        return TicketCard(
          booking: b,
          onTap: () {
            Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => BookingDetailScreen(booking: b)),
            );
          },
          onLiveTrack: b.isUpcoming
              ? () {
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => LiveTrackerScreen(booking: b)),
                  );
                }
              : null,
          onCancel: b.isUpcoming ? () => _showCancelDialog(context, b, bookingProvider) : null,
        );
      },
    );
  }
}
