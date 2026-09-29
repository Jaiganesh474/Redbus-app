import 'package:flutter/material.dart';
import '../config/theme.dart';

class ShimmerLoading extends StatefulWidget {
  final Widget child;
  final bool isLoading;

  const ShimmerLoading({
    super.key,
    required this.child,
    this.isLoading = true,
  });

  @override
  State<ShimmerLoading> createState() => _ShimmerLoadingState();
}

class _ShimmerLoadingState extends State<ShimmerLoading>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1400),
    )..repeat();

    _animation = Tween<double>(begin: -1.0, end: 2.0).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOutSine),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.isLoading) return widget.child;

    final isDark = Theme.of(context).brightness == Brightness.dark;
    final baseColor = isDark ? const Color(0xFF1F2937) : const Color(0xFFE5E7EB);
    final highlightColor = isDark ? const Color(0xFF374151) : const Color(0xFFF3F4F6);

    return AnimatedBuilder(
      animation: _animation,
      builder: (context, child) {
        return ShaderMask(
          blendMode: BlendMode.srcATop,
          shaderCallback: (bounds) {
            return LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                baseColor,
                highlightColor,
                baseColor,
              ],
              stops: const [0.0, 0.5, 1.0],
              transform: _SlidingGradientTransform(slidePercent: _animation.value),
            ).createShader(bounds);
          },
          child: widget.child,
        );
      },
    );
  }
}

class _SlidingGradientTransform extends GradientTransform {
  final double slidePercent;
  const _SlidingGradientTransform({required this.slidePercent});

  @override
  Matrix4? transform(Rect bounds, {TextDirection? textDirection}) {
    return Matrix4.translationValues(bounds.width * slidePercent, 0.0, 0.0);
  }
}

/// Shimmer Skeleton Box helper
class SkeletonBox extends StatelessWidget {
  final double width;
  final double height;
  final double borderRadius;

  const SkeletonBox({
    super.key,
    required this.width,
    required this.height,
    this.borderRadius = 8,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1F2937) : const Color(0xFFE5E7EB),
        borderRadius: BorderRadius.circular(borderRadius),
      ),
    );
  }
}

/// Bus Search Results Shimmer Skeleton Card
class BusCardSkeleton extends StatelessWidget {
  const BusCardSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkCard : Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isDark ? const Color(0xFF374151) : AppColors.border,
        ),
      ),
      child: const ShimmerLoading(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                SkeletonBox(width: 140, height: 18, borderRadius: 4),
                SkeletonBox(width: 70, height: 22, borderRadius: 6),
              ],
            ),
            SizedBox(height: 8),
            SkeletonBox(width: 180, height: 12, borderRadius: 4),
            SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SkeletonBox(width: 60, height: 18, borderRadius: 4),
                    SizedBox(height: 4),
                    SkeletonBox(width: 80, height: 12, borderRadius: 4),
                  ],
                ),
                Column(
                  children: [
                    SkeletonBox(width: 50, height: 10, borderRadius: 4),
                    SizedBox(height: 4),
                    SkeletonBox(width: 90, height: 2, borderRadius: 2),
                    SizedBox(height: 4),
                    SkeletonBox(width: 40, height: 10, borderRadius: 4),
                  ],
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    SkeletonBox(width: 60, height: 18, borderRadius: 4),
                    SizedBox(height: 4),
                    SkeletonBox(width: 80, height: 12, borderRadius: 4),
                  ],
                ),
              ],
            ),
            SizedBox(height: 16),
            Divider(height: 1),
            SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                SkeletonBox(width: 90, height: 14, borderRadius: 4),
                SkeletonBox(width: 110, height: 32, borderRadius: 8),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
