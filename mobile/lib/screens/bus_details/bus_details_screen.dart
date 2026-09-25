import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../models/bus_model.dart';
import '../../providers/booking_provider.dart';
import '../../widgets/rating_badge.dart';
import '../../widgets/custom_button.dart';
import 'seat_selection_screen.dart';

class BusDetailsScreen extends StatelessWidget {
  final Bus bus;

  const BusDetailsScreen({super.key, required this.bus});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return DefaultTabController(
      length: 4,
      child: Scaffold(
        appBar: AppBar(
          title: Text(bus.operatorName),
          bottom: const TabBar(
            isScrollable: true,
            indicatorColor: AppColors.primary,
            labelColor: AppColors.primary,
            unselectedLabelColor: AppColors.textSecondary,
            tabs: [
              Tab(text: 'Overview'),
              Tab(text: 'Boarding / Dropping'),
              Tab(text: 'Amenities'),
              Tab(text: 'Policies & Reviews'),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            // Tab 1: Overview
            _buildOverviewTab(context, isDark),
            // Tab 2: Boarding & Dropping
            _buildBoardingDroppingTab(context, isDark),
            // Tab 3: Amenities
            _buildAmenitiesTab(context, isDark),
            // Tab 4: Policies & Reviews
            _buildPoliciesReviewsTab(context, isDark),
          ],
        ),
        bottomNavigationBar: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: isDark ? AppColors.darkCard : Colors.white,
            border: Border(top: BorderSide(color: isDark ? const Color(0xFF374151) : AppColors.border)),
          ),
          child: Row(
            children: [
              Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Starting from', style: TextStyle(fontSize: 11, color: AppColors.textSecondary)),
                  Text(
                    '₹${bus.basePrice.toInt()}',
                    style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: AppColors.primary),
                  ),
                ],
              ),
              const Spacer(),
              CustomButton(
                width: 170,
                text: 'SELECT SEATS',
                onPressed: () {
                  final bookingProvider = Provider.of<BookingProvider>(context, listen: false);
                  bookingProvider.selectBus(bus);
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const SeatSelectionScreen()),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildOverviewTab(BuildContext context, bool isDark) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Operator Header Card
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkCard : Colors.white,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(bus.operatorName, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                          const SizedBox(height: 2),
                          Text(bus.busType, style: const TextStyle(fontSize: 12, color: AppColors.textSecondary)),
                        ],
                      ),
                    ),
                    RatingBadge(rating: bus.rating, totalRatings: bus.totalRatings),
                  ],
                ),
                const SizedBox(height: 16),
                const Divider(),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    _InfoItem(label: 'Departure', value: bus.departureTime),
                    _InfoItem(label: 'Duration', value: bus.duration),
                    _InfoItem(label: 'Arrival', value: bus.arrivalTime),
                  ],
                ),
              ],
            ),
          ),

          const SizedBox(height: 16),

          // Bus Number & Safety Features
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkCard : Colors.white,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Bus Information & Safety', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                const SizedBox(height: 12),
                _RowFeature(icon: Icons.confirmation_number_outlined, title: 'Bus Reg Number', subtitle: bus.busNumber),
                _RowFeature(icon: Icons.event_seat_rounded, title: 'Total Seats', subtitle: '${bus.totalSeats} (Sleeper & Seater available)'),
                _RowFeature(icon: Icons.sanitizer_outlined, title: 'Clean & Sanitized', subtitle: 'Deep cleaned before every departure'),
                _RowFeature(icon: Icons.gps_fixed, title: 'Live GPS Tracking', subtitle: 'Real-time location sharing enabled'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBoardingDroppingTab(BuildContext context, bool isDark) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Boarding Points (Pickup)', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          ...bus.boardingPoints.map((bp) => _PointTile(point: bp, isBoarding: true, isDark: isDark)),

          const SizedBox(height: 24),

          const Text('Dropping Points', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          ...bus.droppingPoints.map((dp) => _PointTile(point: dp, isBoarding: false, isDark: isDark)),
        ],
      ),
    );
  }

  Widget _buildAmenitiesTab(BuildContext context, bool isDark) {
    return GridView.builder(
      padding: const EdgeInsets.all(16),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        childAspectRatio: 2.4,
        crossAxisSpacing: 12,
        mainAxisSpacing: 12,
      ),
      itemCount: bus.amenities.length,
      itemBuilder: (context, index) {
        final amenity = bus.amenities[index];
        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color: isDark ? AppColors.darkCard : Colors.white,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: AppColors.primaryLight,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(Icons.check_circle_rounded, color: AppColors.primary, size: 16),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  amenity,
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildPoliciesReviewsTab(BuildContext context, bool isDark) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Cancellation & Refund Policy', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          Container(
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkCard : Colors.white,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
            ),
            child: Column(
              children: bus.cancellationPolicies.map((cp) {
                return ListTile(
                  dense: true,
                  title: Text(cp.timeFrame, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
                  trailing: Text(
                    cp.refundPercent,
                    style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.success),
                  ),
                );
              }).toList(),
            ),
          ),

          const SizedBox(height: 24),

          const Text('Passenger Reviews & Ratings', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          if (bus.reviews.isEmpty)
            const Text('No reviews yet. Be the first to review!', style: TextStyle(color: AppColors.textSecondary))
          else
            ...bus.reviews.map((r) => _ReviewCard(review: r, isDark: isDark)),
        ],
      ),
    );
  }
}

class _InfoItem extends StatelessWidget {
  final String label;
  final String value;
  const _InfoItem({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(label, style: const TextStyle(fontSize: 11, color: AppColors.textSecondary)),
        const SizedBox(height: 4),
        Text(value, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
      ],
    );
  }
}

class _RowFeature extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  const _RowFeature({required this.icon, required this.title, required this.subtitle});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Icon(icon, size: 18, color: AppColors.primary),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
              Text(subtitle, style: const TextStyle(fontSize: 11, color: AppColors.textSecondary)),
            ],
          ),
        ],
      ),
    );
  }
}

class _PointTile extends StatelessWidget {
  final BoardingDroppingPoint point;
  final bool isBoarding;
  final bool isDark;

  const _PointTile({required this.point, required this.isBoarding, required this.isDark});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkCard : Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: isBoarding ? AppColors.primaryLight : AppColors.successLight,
              borderRadius: BorderRadius.circular(6),
            ),
            child: Text(
              point.time,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.bold,
                color: isBoarding ? AppColors.primary : AppColors.success,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(point.name, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
                if (point.landmark.isNotEmpty)
                  Text(point.landmark, style: const TextStyle(fontSize: 11, color: AppColors.textSecondary)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ReviewCard extends StatelessWidget {
  final BusReview review;
  final bool isDark;
  const _ReviewCard({required this.review, required this.isDark});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkCard : Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(review.userName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              const Spacer(),
              RatingBadge(rating: review.rating, showStar: true),
            ],
          ),
          const SizedBox(height: 6),
          Text(review.comment, style: const TextStyle(fontSize: 12)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 6,
            children: review.tags.map((tag) {
              return Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: isDark ? const Color(0xFF374151) : const Color(0xFFF3F4F6),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(tag, style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w500)),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}
