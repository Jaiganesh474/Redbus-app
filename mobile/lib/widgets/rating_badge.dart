import 'package:flutter/material.dart';
import '../config/theme.dart';

class RatingBadge extends StatelessWidget {
  final double rating;
  final int? totalRatings;
  final bool showStar;

  const RatingBadge({
    super.key,
    required this.rating,
    this.totalRatings,
    this.showStar = true,
  });

  @override
  Widget build(BuildContext context) {
    Color getRatingColor() {
      if (rating >= 4.5) return AppColors.success;
      if (rating >= 3.8) return const Color(0xFF388E3C);
      if (rating >= 3.0) return AppColors.warning;
      return Colors.grey;
    }

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
          decoration: BoxDecoration(
            color: getRatingColor(),
            borderRadius: BorderRadius.circular(6),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (showStar) ...[
                const Icon(Icons.star_rounded, size: 14, color: Colors.white),
                const SizedBox(width: 3),
              ],
              Text(
                rating.toStringAsFixed(1),
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
        if (totalRatings != null) ...[
          const SizedBox(width: 6),
          Text(
            '($totalRatings)',
            style: const TextStyle(
              fontSize: 12,
              color: AppColors.textSecondary,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ],
    );
  }
}

class PrimoBadge extends StatelessWidget {
  const PrimoBadge({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        gradient: AppColors.goldGradient,
        borderRadius: BorderRadius.circular(6),
        boxShadow: [
          BoxShadow(
            color: AppColors.warning.withOpacity(0.3),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.verified_rounded, size: 12, color: Colors.white),
          SizedBox(width: 4),
          Text(
            'PRIMO',
            style: TextStyle(
              color: Colors.white,
              fontSize: 10,
              fontWeight: FontWeight.w900,
              letterSpacing: 0.5,
            ),
          ),
        ],
      ),
    );
  }
}
