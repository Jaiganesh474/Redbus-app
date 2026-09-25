import 'package:flutter/material.dart';
import '../../config/theme.dart';
import '../../models/booking_model.dart';
import '../../widgets/custom_button.dart';

class LiveTrackerScreen extends StatefulWidget {
  final Booking booking;

  const LiveTrackerScreen({super.key, required this.booking});

  @override
  State<LiveTrackerScreen> createState() => _LiveTrackerScreenState();
}

class _LiveTrackerScreenState extends State<LiveTrackerScreen> {
  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Live Bus GPS Tracking'),
      ),
      body: Column(
        children: [
          // Simulated Map Container with Live Bus Marker & Pulse
          Container(
            height: 260,
            width: double.infinity,
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF1E2430) : const Color(0xFFE2E8F0),
            ),
            child: Stack(
              alignment: Alignment.center,
              children: [
                // Simulated Highway Roads Map Grid
                CustomPaint(
                  size: const Size(double.infinity, 260),
                  painter: _MapGridPainter(isDark: isDark),
                ),

                // Pulsing Live Bus Marker
                Positioned(
                  top: 100,
                  left: MediaQuery.of(context).size.width * 0.45,
                  child: Column(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppColors.dark,
                          borderRadius: BorderRadius.circular(8),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.3),
                              blurRadius: 6,
                            ),
                          ],
                        ),
                        child: Text(
                          widget.booking.busNumber,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: AppColors.primary,
                          shape: BoxShape.circle,
                          border: Border.all(color: Colors.white, width: 3),
                          boxShadow: [
                            BoxShadow(
                              color: AppColors.primary.withOpacity(0.5),
                              blurRadius: 15,
                              spreadRadius: 3,
                            ),
                          ],
                        ),
                        child: const Icon(
                          Icons.directions_bus_filled,
                          color: Colors.white,
                          size: 24,
                        ),
                      ),
                    ],
                  ),
                ),

                // Live Speed & Next Stop Overlay Chip
                Positioned(
                  bottom: 14,
                  left: 16,
                  right: 16,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                    decoration: BoxDecoration(
                      color: isDark ? AppColors.darkCard : Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.1),
                          blurRadius: 10,
                          offset: const Offset(0, 3),
                        ),
                      ],
                    ),
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            Icon(Icons.speed_rounded, size: 20, color: AppColors.primary),
                            SizedBox(width: 8),
                            Text(
                              'Speed: 68 km/h',
                              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                            ),
                          ],
                        ),
                        Row(
                          children: [
                            Icon(Icons.timer_outlined, size: 20, color: AppColors.success),
                            SizedBox(width: 8),
                            Text(
                              'ETA: In 22 Mins',
                              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppColors.success),
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

          // Route Timeline & Driver Info
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Driver & Vehicle Card
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: isDark ? AppColors.darkCard : Colors.white,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
                    ),
                    child: Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: AppColors.primaryLight,
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(Icons.person, color: AppColors.primary, size: 22),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Driver: Ramesh Gowda', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                              Text(widget.booking.operatorName, style: const TextStyle(fontSize: 12, color: AppColors.textSecondary)),
                            ],
                          ),
                        ),
                        IconButton.filled(
                          onPressed: () {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Calling Driver: +91 98450 12345')),
                            );
                          },
                          icon: const Icon(Icons.call, size: 18),
                          style: IconButton.styleFrom(backgroundColor: AppColors.success),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 20),

                  const Text('Live Journey Stops', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 14),

                  _StopTimelineTile(
                    title: widget.booking.boardingPoint.name,
                    subtitle: 'Departed at ${widget.booking.departureTime}',
                    isPassed: true,
                    isCurrent: false,
                    isLast: false,
                  ),
                  const _StopTimelineTile(
                    title: 'Highway Toll Plaza & Refreshment Hub',
                    subtitle: 'Current Location • Rest stop',
                    isPassed: true,
                    isCurrent: true,
                    isLast: false,
                  ),
                  _StopTimelineTile(
                    title: widget.booking.droppingPoint.name,
                    subtitle: 'Scheduled drop at ${widget.booking.arrivalTime}',
                    isPassed: false,
                    isCurrent: false,
                    isLast: true,
                  ),

                  const SizedBox(height: 20),

                  CustomButton(
                    text: 'SHARE LIVE TRACKING LINK',
                    icon: Icons.share_rounded,
                    isOutlined: true,
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('Live GPS tracking link copied to clipboard!'),
                          backgroundColor: AppColors.dark,
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _StopTimelineTile extends StatelessWidget {
  final String title;
  final String subtitle;
  final bool isPassed;
  final bool isCurrent;
  final bool isLast;

  const _StopTimelineTile({
    required this.title,
    required this.subtitle,
    required this.isPassed,
    required this.isCurrent,
    required this.isLast,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Column(
          children: [
            Container(
              width: 18,
              height: 18,
              decoration: BoxDecoration(
                color: isCurrent
                    ? AppColors.primary
                    : (isPassed ? AppColors.success : AppColors.seatBooked),
                shape: BoxShape.circle,
                border: Border.all(color: Colors.white, width: 2),
              ),
              child: isPassed && !isCurrent
                  ? const Icon(Icons.check, size: 10, color: Colors.white)
                  : null,
            ),
            if (!isLast)
              Container(
                width: 2,
                height: 44,
                color: isPassed ? AppColors.success : AppColors.seatBooked,
              ),
          ],
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyle(
                  fontWeight: isCurrent ? FontWeight.bold : FontWeight.w600,
                  fontSize: 14,
                  color: isCurrent ? AppColors.primary : null,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                subtitle,
                style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
              ),
              const SizedBox(height: 18),
            ],
          ),
        ),
      ],
    );
  }
}

class _MapGridPainter extends CustomPainter {
  final bool isDark;
  _MapGridPainter({required this.isDark});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = isDark ? Colors.white.withOpacity(0.04) : Colors.black.withOpacity(0.05)
      ..strokeWidth = 1.0;

    for (double i = 0; i < size.width; i += 30) {
      canvas.drawLine(Offset(i, 0), Offset(i, size.height), paint);
    }
    for (double i = 0; i < size.height; i += 30) {
      canvas.drawLine(Offset(0, i), Offset(size.width, i), paint);
    }

    // Road highway curved line
    final roadPaint = Paint()
      ..color = isDark ? const Color(0xFF3B82F6).withOpacity(0.6) : const Color(0xFF60A5FA)
      ..strokeWidth = 6.0
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    final path = Path();
    path.moveTo(20, size.height - 30);
    path.quadraticBezierTo(size.width * 0.4, size.height * 0.7, size.width * 0.5, 120);
    path.quadraticBezierTo(size.width * 0.6, 70, size.width - 30, 40);

    canvas.drawPath(path, roadPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
