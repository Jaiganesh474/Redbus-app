import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../models/booking_model.dart';
import '../../providers/booking_provider.dart';
import '../../widgets/ticket_card.dart';
import 'live_tracker_screen.dart';

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
                const Text('90% (₹1,408.5)', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.success)),
              ],
            ),
            const SizedBox(height: 2),
            const Text(
              'Amount will be credited back in 2-4 hours to original payment mode.',
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
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () {
              bookingProvider.cancelBooking(booking.id);
              Navigator.pop(ctx);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Booking cancelled. Instant refund initiated!'),
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
        body: TabBarView(
          children: [
            // Upcoming
            _buildBookingsList(context, upcomingBookings, bookingProvider, 'No upcoming trips found'),
            // Completed
            _buildBookingsList(context, completedBookings, bookingProvider, 'No completed trips found'),
            // Cancelled
            _buildBookingsList(context, cancelledBookings, bookingProvider, 'No cancelled bookings'),
          ],
        ),
      ),
    );
  }

  Widget _buildBookingsList(
    BuildContext context,
    List<Booking> list,
    BookingProvider bookingProvider,
    String emptyMessage,
  ) {
    if (list.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.confirmation_number_outlined, size: 54, color: AppColors.textMuted),
            const SizedBox(height: 12),
            Text(
              emptyMessage,
              style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.textSecondary),
            ),
          ],
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
