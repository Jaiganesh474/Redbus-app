import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../config/theme.dart';
import '../../config/constants.dart';

class OffersScreen extends StatelessWidget {
  const OffersScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Offers & Deals'),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: AppConstants.promoOffers.length,
        itemBuilder: (context, index) {
          final offer = AppConstants.promoOffers[index];
          return Container(
            margin: const EdgeInsets.only(bottom: 16),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkCard : Colors.white,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(isDark ? 0.2 : 0.04),
                  blurRadius: 10,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Banner Top
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    gradient: index == 0
                        ? AppColors.primaryGradient
                        : (index == 1 ? AppColors.goldGradient : AppColors.promoCardGradient),
                    borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.25),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          offer['badge'],
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 10,
                            fontWeight: FontWeight.w900,
                            letterSpacing: 0.5,
                          ),
                        ),
                      ),
                      const Spacer(),
                      Text(
                        'Valid till ${offer['validTill']}',
                        style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w500),
                      ),
                    ],
                  ),
                ),

                // Body & Copy Action
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        offer['title'],
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        offer['description'],
                        style: TextStyle(
                          fontSize: 13,
                          color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                          height: 1.4,
                        ),
                      ),
                      const SizedBox(height: 14),
                      const Divider(),
                      const SizedBox(height: 10),
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                            decoration: BoxDecoration(
                              color: isDark ? AppColors.darkSurface : AppColors.primaryLight,
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(color: AppColors.primary.withOpacity(0.3)),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.local_offer_rounded, size: 16, color: AppColors.primary),
                                const SizedBox(width: 6),
                                Text(
                                  offer['code'],
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 14,
                                    color: AppColors.primary,
                                    letterSpacing: 0.5,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const Spacer(),
                          ElevatedButton.icon(
                            onPressed: () {
                              Clipboard.setData(ClipboardData(text: offer['code']));
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text('Coupon code "${offer['code']}" copied to clipboard!'),
                                  backgroundColor: AppColors.dark,
                                ),
                              );
                            },
                            icon: const Icon(Icons.copy_rounded, size: 16),
                            label: const Text('COPY CODE'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.primary,
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
