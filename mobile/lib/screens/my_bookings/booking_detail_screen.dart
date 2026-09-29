import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../../config/theme.dart';
import '../../models/booking_model.dart';
import '../../providers/booking_provider.dart';
import '../../services/api_service.dart';
import '../../widgets/custom_button.dart';
import 'live_tracker_screen.dart';

class BookingDetailScreen extends StatefulWidget {
  final Booking booking;

  const BookingDetailScreen({super.key, required this.booking});

  @override
  State<BookingDetailScreen> createState() => _BookingDetailScreenState();
}

class _BookingDetailScreenState extends State<BookingDetailScreen> {
  final ApiService _apiService = ApiService();
  bool _isEmailSending = false;

  void _onEmailTicket() async {
    setState(() => _isEmailSending = true);
    final success = await _apiService.sendTicketEmail(widget.booking.pnr);
    setState(() => _isEmailSending = false);

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(success ? 'E-Ticket & Tax Invoice sent to your email! 📧' : 'Failed to send email. Please try again.'),
          backgroundColor: success ? AppColors.success : Colors.redAccent,
        ),
      );
    }
  }

  void _showCancelDialog(BuildContext context, BookingProvider bookingProvider) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cancel Booking?'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Are you sure you want to cancel booking for PNR: ${widget.booking.pnr}?'),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.amber.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                'Estimated Refund: ₹${(widget.booking.totalAmount * 0.9).toStringAsFixed(1)} (Instant credit to RedBus Wallet)',
                style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.deepOrange),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('KEEP BOOKING'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
            onPressed: () {
              Navigator.pop(ctx);
              bookingProvider.cancelBookingByPnr(widget.booking.pnr);
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Booking cancelled. Refund of 90% credited to your Wallet!'),
                  backgroundColor: AppColors.dark,
                ),
              );
            },
            child: const Text('CANCEL TICKET'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bookingProvider = Provider.of<BookingProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final b = widget.booking;

    return Scaffold(
      appBar: AppBar(
        title: Text('Ticket: ${b.pnr}'),
        actions: [
          IconButton(
            icon: const Icon(Icons.share_outlined),
            onPressed: () {
              Clipboard.setData(ClipboardData(text: 'My redBus Ticket: PNR ${b.pnr} from ${b.sourceCity} to ${b.destinationCity} on ${b.travelDate}'));
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Ticket details copied!')));
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            // Status & Operator Card
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: isDark ? AppColors.darkCard : Colors.white,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(b.operatorName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                          Text(b.busType, style: const TextStyle(fontSize: 12, color: AppColors.textSecondary)),
                        ],
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: (b.status == BookingStatus.confirmed ? AppColors.success : Colors.redAccent).withOpacity(0.12),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          b.status.name.toUpperCase(),
                          style: TextStyle(
                            color: b.status == BookingStatus.confirmed ? AppColors.success : Colors.redAccent,
                            fontWeight: FontWeight.bold,
                            fontSize: 11,
                          ),
                        ),
                      ),
                    ],
                  ),

                  const Divider(height: 24),

                  // Route & Times
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(b.departureTime, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                          Text(b.sourceCity, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                          Text(b.travelDate, style: const TextStyle(fontSize: 11, color: AppColors.textSecondary)),
                        ],
                      ),
                      const Icon(Icons.directions_bus_rounded, color: AppColors.primary, size: 28),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(b.arrivalTime, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                          Text(b.destinationCity, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                          const Text('Next Day', style: TextStyle(fontSize: 11, color: AppColors.textSecondary)),
                        ],
                      ),
                    ],
                  ),

                  const SizedBox(height: 16),

                  // Boarding & Dropping Locations
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: isDark ? AppColors.darkSurface : const Color(0xFFF9FAFB),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Column(
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.trip_origin, color: AppColors.primary, size: 16),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                'Pickup: ${b.boardingPoint.name} (${b.boardingPoint.time})',
                                style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            const Icon(Icons.location_on, color: AppColors.success, size: 16),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                'Drop: ${b.droppingPoint.name} (${b.droppingPoint.time})',
                                style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // Passengers & QR
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: isDark ? AppColors.darkCard : Colors.white,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Passenger & Seat Details', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 12),
                  ...b.passengers.map((p) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('${p.name} (${p.age}y, ${p.gender})', style: const TextStyle(fontSize: 13)),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: AppColors.primaryLight,
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            'Seat ${p.seatNumber}',
                            style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.primaryDark, fontSize: 12),
                          ),
                        ),
                      ],
                    ),
                  )),

                  const Divider(height: 24),

                  // QR Code
                  Center(
                    child: Column(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: AppColors.border),
                          ),
                          child: QrImageView(
                            data: b.qrData,
                            version: QrVersions.auto,
                            size: 130,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Show this QR code to the bus conductor while boarding',
                          style: TextStyle(fontSize: 11, color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 20),

            // Actions
            if (b.status == BookingStatus.confirmed) ...[
              CustomButton(
                text: 'TRACK LIVE BUS LOCATION',
                icon: Icons.navigation_rounded,
                onPressed: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => LiveTrackerScreen(booking: b)),
                  );
                },
              ),
              const SizedBox(height: 10),
              CustomButton(
                text: 'SEND E-TICKET TO EMAIL',
                icon: Icons.email_outlined,
                isOutlined: true,
                isLoading: _isEmailSending,
                onPressed: _onEmailTicket,
              ),
              const SizedBox(height: 10),
              OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.redAccent,
                  side: const BorderSide(color: Colors.redAccent),
                  minimumSize: const Size(double.infinity, 48),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                icon: const Icon(Icons.cancel_outlined, size: 18),
                label: const Text('CANCEL TICKET', style: TextStyle(fontWeight: FontWeight.bold)),
                onPressed: () => _showCancelDialog(context, bookingProvider),
              ),
            ],

            const SizedBox(height: 30),
          ],
        ),
      ),
    );
  }
}
