import 'package:flutter/material.dart';
import '../config/theme.dart';
import '../models/bus_model.dart';
import 'rating_badge.dart';

class BusCard extends StatelessWidget {
  final Bus bus;
  final VoidCallback onSelectSeats;
  final VoidCallback onViewDetails;

  const BusCard({
    super.key,
    required this.bus,
    required this.onSelectSeats,
    required this.onViewDetails,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkCard : Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: bus.isPrimo ? AppColors.warning.withOpacity(0.4) : (isDark ? const Color(0xFF374151) : AppColors.border),
          width: bus.isPrimo ? 1.5 : 1,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDark ? 0.2 : 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: onViewDetails,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Operator Name & Primo Badge & Price
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Flexible(
                                child: Text(
                                  bus.operatorName,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              if (bus.isPrimo) ...[
                                const SizedBox(width: 8),
                                const PrimoBadge(),
                              ],
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            bus.busType,
                            style: TextStyle(
                              fontSize: 12,
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
                          '₹${bus.basePrice.toInt()}',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w800,
                            color: isDark ? Colors.white : AppColors.textPrimary,
                          ),
                        ),
                        Text(
                          'onwards',
                          style: TextStyle(
                            fontSize: 11,
                            color: isDark ? const Color(0xFF9CA3AF) : AppColors.textMuted,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),

                const SizedBox(height: 16),

                // Departure - Duration - Arrival Timeline
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          bus.departureTime,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          bus.sourceCity,
                          style: TextStyle(
                            fontSize: 12,
                            color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                    Column(
                      children: [
                        Text(
                          bus.duration,
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            Container(
                              width: 6,
                              height: 6,
                              decoration: const BoxDecoration(
                                color: AppColors.primary,
                                shape: BoxShape.circle,
                              ),
                            ),
                            Container(
                              width: 60,
                              height: 1.5,
                              color: isDark ? const Color(0xFF4B5563) : AppColors.border,
                            ),
                            const Icon(Icons.directions_bus, size: 14, color: AppColors.primary),
                            Container(
                              width: 60,
                              height: 1.5,
                              color: isDark ? const Color(0xFF4B5563) : AppColors.border,
                            ),
                            Container(
                              width: 6,
                              height: 6,
                              decoration: const BoxDecoration(
                                color: AppColors.success,
                                shape: BoxShape.circle,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          bus.arrivalTime,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          bus.destinationCity,
                          style: TextStyle(
                            fontSize: 12,
                            color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),

                const SizedBox(height: 14),
                const Divider(height: 1),
                const SizedBox(height: 12),

                // Ratings, Seats Left & Action Button
                Row(
                  children: [
                    RatingBadge(
                      rating: bus.rating,
                      totalRatings: bus.totalRatings,
                    ),
                    const SizedBox(width: 12),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: bus.availableSeats <= 5
                            ? AppColors.primaryLight
                            : (isDark ? const Color(0xFF374151) : AppColors.background),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        '${bus.availableSeats} Seats left',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: bus.availableSeats <= 5
                              ? AppColors.primary
                              : (isDark ? Colors.white70 : AppColors.textSecondary),
                        ),
                      ),
                    ),
                    const Spacer(),
                    ElevatedButton(
                      onPressed: onSelectSeats,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                        elevation: 0,
                      ),
                      child: const Text(
                        'Select Seats',
                        style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
