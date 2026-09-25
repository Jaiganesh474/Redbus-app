import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../config/theme.dart';
import '../models/booking_model.dart';

class TicketCard extends StatelessWidget {
  final Booking booking;
  final VoidCallback? onLiveTrack;
  final VoidCallback? onCancel;
  final VoidCallback? onTap;

  const TicketCard({
    super.key,
    required this.booking,
    this.onLiveTrack,
    this.onCancel,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    Color getStatusColor() {
      switch (booking.status) {
        case BookingStatus.confirmed:
          return AppColors.success;
        case BookingStatus.cancelled:
          return Colors.redAccent;
        case BookingStatus.completed:
          return AppColors.info;
        default:
          return AppColors.warning;
      }
    }

    String getStatusText() {
      switch (booking.status) {
        case BookingStatus.confirmed:
          return 'CONFIRMED';
        case BookingStatus.cancelled:
          return 'CANCELLED';
        case BookingStatus.completed:
          return 'COMPLETED';
        default:
          return 'PENDING';
      }
    }

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkCard : Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isDark ? const Color(0xFF374151) : AppColors.border,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDark ? 0.2 : 0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: onTap,
          child: Column(
            children: [
              // Header: Operator & Status
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: isDark ? const Color(0xFF1E232B) : const Color(0xFFFAFAFA),
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            booking.operatorName,
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 15,
                            ),
                          ),
                          Text(
                            booking.busType,
                            style: TextStyle(
                              fontSize: 11,
                              color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: getStatusColor().withOpacity(0.12),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: getStatusColor().withOpacity(0.4)),
                      ),
                      child: Text(
                        getStatusText(),
                        style: TextStyle(
                          color: getStatusColor(),
                          fontWeight: FontWeight.bold,
                          fontSize: 11,
                          letterSpacing: 0.5,
                        ),
                      ),
                    ),
                  ],
                ),
              ),

              Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    // Route & Times
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              booking.departureTime,
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            Text(
                              booking.sourceCity,
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            Text(
                              booking.boardingPoint.name,
                              style: TextStyle(
                                fontSize: 11,
                                color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                        const Icon(Icons.arrow_forward_rounded, color: AppColors.primary, size: 20),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              booking.arrivalTime,
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            Text(
                              booking.destinationCity,
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            Text(
                              booking.droppingPoint.name,
                              style: TextStyle(
                                fontSize: 11,
                                color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),

                    const SizedBox(height: 14),

                    // Dashed Divider simulation
                    Row(
                      children: List.generate(
                        30,
                        (index) => Expanded(
                          child: Container(
                            height: 1,
                            color: index % 2 == 0
                                ? (isDark ? const Color(0xFF374151) : AppColors.border)
                                : Colors.transparent,
                          ),
                        ),
                      ),
                    ),

                    const SizedBox(height: 14),

                    // PNR & QR Code & Seats
                    Row(
                      children: [
                        // QR Code thumbnail
                        Container(
                          padding: const EdgeInsets.all(4),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: AppColors.border),
                          ),
                          child: QrImageView(
                            data: booking.qrData,
                            version: QrVersions.auto,
                            size: 55,
                            eyeStyle: const QrEyeStyle(
                              eyeShape: QrEyeShape.square,
                              color: AppColors.dark,
                            ),
                            dataModuleStyle: const QrDataModuleStyle(
                              dataModuleShape: QrDataModuleShape.square,
                              color: AppColors.dark,
                            ),
                          ),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  const Text(
                                    'PNR: ',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: AppColors.textSecondary,
                                    ),
                                  ),
                                  Text(
                                    booking.pnr,
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.bold,
                                      letterSpacing: 0.5,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 3),
                              Text(
                                'Seats: ${booking.passengers.map((p) => p.seatNumber).join(', ')}',
                                style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.primary,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Date: ${booking.travelDate}',
                                style: TextStyle(
                                  fontSize: 11,
                                  color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              '₹${booking.totalAmount.toInt()}',
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            Text(
                              '${booking.passengers.length} Passenger(s)',
                              style: TextStyle(
                                fontSize: 11,
                                color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),

                    if (booking.isUpcoming) ...[
                      const SizedBox(height: 14),
                      Row(
                        children: [
                          if (onLiveTrack != null)
                            Expanded(
                              child: OutlinedButton.icon(
                                onPressed: onLiveTrack,
                                icon: const Icon(Icons.navigation_outlined, size: 16),
                                label: const Text('Live Track', style: TextStyle(fontSize: 12)),
                                style: OutlinedButton.styleFrom(
                                  foregroundColor: AppColors.primary,
                                  side: const BorderSide(color: AppColors.primary),
                                  padding: const EdgeInsets.symmetric(vertical: 8),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                ),
                              ),
                            ),
                          if (onLiveTrack != null && onCancel != null)
                            const SizedBox(width: 10),
                          if (onCancel != null)
                            Expanded(
                              child: OutlinedButton.icon(
                                onPressed: onCancel,
                                icon: const Icon(Icons.cancel_outlined, size: 16, color: Colors.redAccent),
                                label: const Text('Cancel', style: TextStyle(fontSize: 12, color: Colors.redAccent)),
                                style: OutlinedButton.styleFrom(
                                  side: const BorderSide(color: Colors.redAccent),
                                  padding: const EdgeInsets.symmetric(vertical: 8),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                ),
                              ),
                            ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
