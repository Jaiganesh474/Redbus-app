import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../config/constants.dart';
import '../../models/coupon_model.dart';
import '../../providers/booking_provider.dart';
import '../../widgets/custom_button.dart';
import 'booking_success_screen.dart';

class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key});

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  final TextEditingController _couponController = TextEditingController();
  String? _couponMessage;
  bool _isCouponSuccess = false;

  late Timer _timer;
  int _secondsRemaining = 594; // 9 minutes 54 seconds

  @override
  void initState() {
    super.initState();
    // Default payment method set to RAZORPAY
    final bookingProvider = Provider.of<BookingProvider>(context, listen: false);
    bookingProvider.setPaymentMethod('RAZORPAY');

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsRemaining > 0) {
        setState(() => _secondsRemaining--);
      } else {
        _timer.cancel();
      }
    });
  }

  @override
  void dispose() {
    _timer.cancel();
    _couponController.dispose();
    super.dispose();
  }

  String _formatTimer(int totalSeconds) {
    final minutes = (totalSeconds ~/ 60).toString().padLeft(2, '0');
    final seconds = (totalSeconds % 60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
  }

  void _applyCouponCode(BookingProvider bookingProvider, String code) {
    final cleanCode = code.trim().toUpperCase();
    final offer = AppConstants.promoOffers.firstWhere(
      (o) => o['code'] == cleanCode,
      orElse: () => {},
    );

    if (offer.isNotEmpty) {
      final coupon = Coupon(
        code: offer['code'],
        title: offer['title'],
        description: offer['description'],
        discountPercent: (offer['discount'] as num).toDouble(),
        maxDiscount: (offer['maxDiscount'] as num).toDouble(),
        minFare: (offer['minFare'] as num).toDouble(),
        validTill: offer['validTill'],
        badge: offer['badge'],
      );

      if (bookingProvider.baseFare < coupon.minFare) {
        setState(() {
          _couponMessage = 'Minimum booking fare of ₹${coupon.minFare.toInt()} required for this coupon';
          _isCouponSuccess = false;
        });
      } else {
        bookingProvider.applyCoupon(coupon);
        setState(() {
          _couponMessage = 'Coupon applied! You saved ₹${coupon.calculateDiscount(bookingProvider.baseFare).toInt()} 🎉';
          _isCouponSuccess = true;
        });
      }
    } else {
      setState(() {
        _couponMessage = 'Invalid promo coupon code';
        _isCouponSuccess = false;
      });
    }
  }

  void _showFareBreakupSheet(BuildContext context, BookingProvider bookingProvider) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: isDark ? AppColors.darkCard : Colors.white,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.grey[300],
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Row(
                children: [
                  Icon(Icons.receipt_long_rounded, color: AppColors.primary, size: 22),
                  SizedBox(width: 8),
                  Text('Detailed Fare Breakup', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                ],
              ),
              const SizedBox(height: 16),
              _FareRow(
                title: 'Base Ticket Fare (${bookingProvider.selectedSeats.length} seats)',
                amount: '₹${bookingProvider.baseFare.toInt()}',
              ),
              _FareRow(
                title: 'GST & State Commercial Taxes (5%)',
                amount: '₹${bookingProvider.gstAndTaxes.toStringAsFixed(1)}',
              ),
              if (bookingProvider.optedInsurance)
                _FareRow(
                  title: 'ACKO Travel Insurance (${bookingProvider.selectedSeats.length} x ₹15)',
                  amount: '₹${bookingProvider.insuranceFee.toInt()}',
                ),
              if (bookingProvider.discountAmount > 0)
                _FareRow(
                  title: 'Coupon Discount (${bookingProvider.appliedCoupon?.code ?? ""})',
                  amount: '-₹${bookingProvider.discountAmount.toInt()}',
                  isDiscount: true,
                ),
              const Divider(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Total Final Payable', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  Text(
                    '₹${bookingProvider.totalPayableAmount.toStringAsFixed(1)}',
                    style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: AppColors.primary),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              CustomButton(
                text: 'CLOSE',
                onPressed: () => Navigator.of(ctx).pop(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _onPayWithRazorpay(BookingProvider bookingProvider) async {
    // Show Razorpay processing overlay
    final booking = await bookingProvider.confirmAndPay();
    if (booking != null && mounted) {
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => BookingSuccessScreen(booking: booking)),
        (route) => route.isFirst,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final bookingProvider = Provider.of<BookingProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final bus = bookingProvider.selectedBus!;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Payment & Checkout'),
        elevation: 0,
      ),
      body: Column(
        children: [
          // Countdown Timer Banner
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            color: const Color(0xFFFFFBEB),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.timer_outlined, size: 18, color: Color(0xFFD97706)),
                const SizedBox(width: 8),
                Text(
                  'Time remaining to complete booking: ',
                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: Colors.amber[900]),
                ),
                Text(
                  _formatTimer(_secondsRemaining),
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Colors.amber[900]),
                ),
              ],
            ),
          ),

          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Bus & Route Summary Card
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: isDark ? AppColors.darkCard : Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.02),
                          blurRadius: 8,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                bus.operatorName,
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                              decoration: BoxDecoration(
                                color: AppColors.primaryLight,
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text(
                                '${bookingProvider.selectedSeats.length} Seat(s)',
                                style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.primary, fontSize: 12),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Text(bus.sourceCity, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                            const SizedBox(width: 6),
                            const Icon(Icons.arrow_forward_rounded, size: 14, color: AppColors.primary),
                            const SizedBox(width: 6),
                            Text(bus.destinationCity, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Text(
                          'Seats: ${bookingProvider.selectedSeats.map((s) => s.seatNumber).join(', ')}',
                          style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
                        ),
                        const Divider(height: 20),
                        InkWell(
                          onTap: () => _showFareBreakupSheet(context, bookingProvider),
                          child: const Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Row(
                                children: [
                                  Icon(Icons.info_outline_rounded, size: 16, color: AppColors.primary),
                                  SizedBox(width: 6),
                                  Text(
                                    'View Detailed Fare Breakup',
                                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.primary),
                                  ),
                                ],
                              ),
                              Icon(Icons.chevron_right_rounded, size: 18, color: AppColors.primary),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 16),

                  // Coupon Accordion
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
                        const Row(
                          children: [
                            Icon(Icons.local_offer_rounded, color: AppColors.primary, size: 18),
                            SizedBox(width: 8),
                            Text('Offers & Promo Codes', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: TextField(
                                controller: _couponController,
                                textCapitalization: TextCapitalization.characters,
                                decoration: InputDecoration(
                                  hintText: 'Enter promo code',
                                  contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                                  border: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(10),
                                    borderSide: BorderSide(color: isDark ? const Color(0xFF4B5563) : AppColors.border),
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            ElevatedButton(
                              onPressed: () => _applyCouponCode(bookingProvider, _couponController.text),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: AppColors.primary,
                                foregroundColor: Colors.white,
                                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                              ),
                              child: const Text('APPLY'),
                            ),
                          ],
                        ),
                        if (_couponMessage != null) ...[
                          const SizedBox(height: 8),
                          Text(
                            _couponMessage!,
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                              color: _isCouponSuccess ? AppColors.success : Colors.redAccent,
                            ),
                          ),
                        ],
                        const SizedBox(height: 12),
                        Wrap(
                          spacing: 8,
                          children: ['FIRST50', 'SUPERBUS', 'REDBUS100'].map((code) {
                            return ActionChip(
                              label: Text(code),
                              labelStyle: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
                              backgroundColor: isDark ? AppColors.darkSurface : AppColors.primaryLight,
                              onPressed: () {
                                _couponController.text = code;
                                _applyCouponCode(bookingProvider, code);
                              },
                            );
                          }).toList(),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 16),

                  // Razorpay Payment Option (Exclusively Pay Using Razorpay)
                  Container(
                    padding: const EdgeInsets.all(18),
                    decoration: BoxDecoration(
                      color: isDark ? AppColors.darkCard : Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: const Color(0xFF0C2340), width: 1.5),
                      boxShadow: [
                        BoxShadow(
                          color: const Color(0xFF0C2340).withOpacity(0.06),
                          blurRadius: 10,
                          offset: const Offset(0, 3),
                        ),
                      ],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFF0C2340),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: const Icon(Icons.bolt_rounded, color: Colors.cyanAccent, size: 20),
                                ),
                                const SizedBox(width: 12),
                                const Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Pay using Razorpay',
                                      style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                                    ),
                                    Text(
                                      'UPI • Cards • NetBanking • Wallets',
                                      style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                              decoration: BoxDecoration(
                                color: AppColors.successLight,
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: const Row(
                                children: [
                                  Icon(Icons.verified_user_rounded, color: AppColors.success, size: 12),
                                  SizedBox(width: 4),
                                  Text(
                                    '100% SECURE',
                                    style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppColors.success),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),

                        const SizedBox(height: 16),
                        const Divider(height: 1),
                        const SizedBox(height: 14),

                        // Supported Gateways Inside Razorpay
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceAround,
                          children: [
                            _PaymentMethodBadge(label: 'Google Pay', icon: Icons.g_mobiledata_rounded),
                            _PaymentMethodBadge(label: 'PhonePe', icon: Icons.phone_android_rounded),
                            _PaymentMethodBadge(label: 'Paytm UPI', icon: Icons.account_balance_wallet_rounded),
                            _PaymentMethodBadge(label: 'Cards & EMI', icon: Icons.credit_card_rounded),
                          ],
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),

          // Bottom Sticky Pay Action Bar
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkSurface : Colors.white,
              border: Border(
                top: BorderSide(
                  color: isDark ? const Color(0xFF374151) : AppColors.border,
                ),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.06),
                  blurRadius: 10,
                  offset: const Offset(0, -3),
                ),
              ],
            ),
            child: SafeArea(
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Total Payable',
                          style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
                        ),
                        Text(
                          '₹${bookingProvider.totalPayableAmount.toStringAsFixed(1)}',
                          style: const TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.w900,
                            color: AppColors.primary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Expanded(
                    flex: 2,
                    child: CustomButton(
                      text: 'PAY VIA RAZORPAY',
                      isLoading: bookingProvider.isLoading,
                      onPressed: () => _onPayWithRazorpay(bookingProvider),
                    ),
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

class _PaymentMethodBadge extends StatelessWidget {
  final String label;
  final IconData icon;

  const _PaymentMethodBadge({required this.label, required this.icon});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: isDark ? AppColors.darkSurface : const Color(0xFFF3F4F6),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, size: 20, color: isDark ? Colors.white : const Color(0xFF374151)),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w500, color: AppColors.textSecondary),
        ),
      ],
    );
  }
}

class _FareRow extends StatelessWidget {
  final String title;
  final String amount;
  final bool isDiscount;

  const _FareRow({required this.title, required this.amount, this.isDiscount = false});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Text(title, style: const TextStyle(fontSize: 13, color: AppColors.textSecondary)),
          ),
          Text(
            amount,
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: isDiscount ? AppColors.success : null,
            ),
          ),
        ],
      ),
    );
  }
}
