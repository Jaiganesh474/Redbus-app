import 'package:flutter/material.dart';
import '../../config/theme.dart';
import '../../models/bus_model.dart';
import '../../models/review_model.dart';
import '../../services/api_service.dart';

class BusReviewsScreen extends StatefulWidget {
  final Bus bus;

  const BusReviewsScreen({super.key, required this.bus});

  @override
  State<BusReviewsScreen> createState() => _BusReviewsScreenState();
}

class _BusReviewsScreenState extends State<BusReviewsScreen> {
  final ApiService _apiService = ApiService();
  List<Review> _reviews = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchReviews();
  }

  void _fetchReviews() async {
    try {
      final list = await _apiService.getRouteReviews(widget.bus.id);
      setState(() {
        _reviews = list;
        _isLoading = false;
      });
    } catch (_) {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.bus.operatorName} Reviews'),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Rating Overview Card
                  Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: isDark ? AppColors.darkCard : Colors.white,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
                    ),
                    child: Row(
                      children: [
                        Column(
                          children: [
                            Text(
                              widget.bus.rating.toStringAsFixed(1),
                              style: const TextStyle(
                                fontFamily: 'Outfit',
                                fontSize: 40,
                                fontWeight: FontWeight.w900,
                                color: AppColors.primary,
                              ),
                            ),
                            const Row(
                              children: [
                                Icon(Icons.star, color: Colors.amber, size: 16),
                                Icon(Icons.star, color: Colors.amber, size: 16),
                                Icon(Icons.star, color: Colors.amber, size: 16),
                                Icon(Icons.star, color: Colors.amber, size: 16),
                                Icon(Icons.star_half, color: Colors.amber, size: 16),
                              ],
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '${widget.bus.totalRatings} ratings',
                              style: const TextStyle(fontSize: 11, color: AppColors.textSecondary),
                            ),
                          ],
                        ),
                        const SizedBox(width: 24),
                        const Expanded(
                          child: Column(
                            children: [
                              _RatingBar(label: 'Cleanliness', score: '4.8 ★'),
                              SizedBox(height: 6),
                              _RatingBar(label: 'Punctuality', score: '4.6 ★'),
                              SizedBox(height: 6),
                              _RatingBar(label: 'Driver Behavior', score: '4.7 ★'),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 24),

                  const Text('Verified Passenger Reviews', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 12),

                  if (_reviews.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 30),
                      child: Center(child: Text('No reviews found for this bus yet.')),
                    )
                  else
                    ..._reviews.map((r) => Container(
                      margin: const EdgeInsets.only(bottom: 12),
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: isDark ? AppColors.darkCard : Colors.white,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Row(
                                children: [
                                  CircleAvatar(
                                    radius: 14,
                                    backgroundColor: AppColors.primaryLight,
                                    child: Text(r.userName[0], style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.primaryDark)),
                                  ),
                                  const SizedBox(width: 8),
                                  Text(r.userName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                                ],
                              ),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                decoration: BoxDecoration(
                                  color: AppColors.successLight,
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: Row(
                                  children: [
                                    Text('${r.rating}', style: const TextStyle(color: AppColors.success, fontWeight: FontWeight.bold, fontSize: 11)),
                                    const SizedBox(width: 2),
                                    const Icon(Icons.star, size: 12, color: AppColors.success),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Text(r.comment, style: const TextStyle(fontSize: 13, height: 1.3)),
                          const SizedBox(height: 8),
                          Text(r.createdAt, style: const TextStyle(fontSize: 10, color: AppColors.textMuted)),
                        ],
                      ),
                    )),
                ],
              ),
            ),
    );
  }
}

class _RatingBar extends StatelessWidget {
  final String label;
  final String score;

  const _RatingBar({required this.label, required this.score});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, color: AppColors.textSecondary)),
        Text(score, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.primary)),
      ],
    );
  }
}
