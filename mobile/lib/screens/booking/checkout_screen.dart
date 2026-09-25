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

  void _onPayNow(BookingProvider bookingProvider) async {
    final booking = await bookingProvider.confirmAndPay();
    if (booking != null && mounted) {
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => BookingSuccessScreen(booking: booking)),
        (route) => route.isFirst,
      );
    }
  }

  @override
  void dispose() {
    _couponController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bookingProvider = Provider.of<BookingProvider>(context);
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final bus = bookingProvider.selectedBus!;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Checkout & Payment'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Journey Summary Card
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
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(bus.operatorName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                      Text(
                        '${bookingProvider.selectedSeats.length} Seat(s)',
                        style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.primary, fontSize: 13),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Text('${bus.sourceCity} (${bus.departureTime})', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                      const SizedBox(width: 8),
                      const Icon(Icons.arrow_forward_rounded, size: 14, color: AppColors.primary),
                      const SizedBox(width: 8),
                      Text('${bus.destinationCity} (${bus.arrivalTime})', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Seats: ${bookingProvider.selectedSeats.map((s) => s.seatNumber).join(', ')}',
                    style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
                  ),
                  const Divider(height: 20),
                  Row(
                    children: [
                      const Icon(Icons.pin_drop_outlined, size: 16, color: AppColors.primary),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          'Pickup: ${bookingProvider.selectedBoardingPoint?.name ?? ""}',
                          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // Promo Code & Offers Box
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
                  const Text('Offers & Promo Codes', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _couponController,
                          textCapitalization: TextCapitalization.characters,
                          decoration: InputDecoration(
                            hintText: 'Enter coupon code (e.g. FIRST50)',
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

                  // Quick coupons suggestions
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

            // Fare Breakdown Card
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
                  const Text('Fare Summary', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 12),
                  _FareRow(title: 'Base Ticket Fare', amount: '₹${bookingProvider.baseFare.toInt()}'),
                  _FareRow(title: 'GST & State Taxes (5%)', amount: '₹${bookingProvider.gstAndTaxes.toStringAsFixed(1)}'),
                  if (bookingProvider.optedInsurance)
                    _FareRow(title: 'Travel Insurance', amount: '₹${bookingProvider.insuranceFee.toInt()}'),
                  if (bookingProvider.discountAmount > 0)
                    _FareRow(
                      title: 'Coupon Discount',
                      amount: '-₹${bookingProvider.discountAmount.toInt()}',
                      isDiscount: true,
                    ),
                  const Divider(height: 20),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Total Payable Amount',
                        style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                      ),
                      Text(
                        '₹${bookingProvider.totalPayableAmount.toStringAsFixed(1)}',
                        style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: AppColors.primary),
                      ),
                    ],
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // Payment Methods
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
                  const Text('Select Payment Method', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 12),
                  _PaymentRadioTile(
                    title: 'Google Pay / PhonePe / UPI',
                    subtitle: 'Instant payment via any UPI app',
                    icon: Icons.account_balance_wallet_rounded,
                    value: 'UPI',
                    groupValue: bookingProvider.selectedPaymentMethod,
                    onChanged: (val) => bookingProvider.setPaymentMethod(val!),
                  ),
                  _PaymentRadioTile(
                    title: 'Razorpay Secure Checkout',
                    subtitle: 'Cards, NetBanking, Wallets',
                    icon: Icons.shield_rounded,
                    value: 'RAZORPAY',
                    groupValue: bookingProvider.selectedPaymentMethod,
                    onChanged: (val) => bookingProvider.setPaymentMethod(val!),
                  ),
                  _PaymentRadioTile(
                    title: 'Credit / Debit Card',
                    subtitle: 'Visa, Mastercard, RuPay',
                    icon: Icons.credit_card_rounded,
                    value: 'CARD',
                    groupValue: bookingProvider.selectedPaymentMethod,
                    onChanged: (val) => bookingProvider.setPaymentMethod(val!),
                  ),
                  _PaymentRadioTile(
                    title: 'RedBus Wallet',
                    subtitle: 'Available Balance: ₹250',
                    icon: Icons.account_balance_rounded,
                    value: 'WALLET',
                    groupValue: bookingProvider.selectedPaymentMethod,
                    onChanged: (val) => bookingProvider.setPaymentMethod(val!),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 24),

            // Pay Now Action
            CustomButton(
              text: 'PAY ₹${bookingProvider.totalPayableAmount.toStringAsFixed(1)} & BOOK',
              isLoading: bookingProvider.isLoading,
              onPressed: () => _onPayNow(bookingProvider),
            ),

            const SizedBox(height: 12),
            const Center(
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.lock_rounded, size: 14, color: AppColors.success),
                  SizedBox(width: 4),
                  Text(
                    '256-bit SSL Encrypted • 100% Safe Payments',
                    style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 30),
          ],
        ),
      ),
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
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(title, style: const TextStyle(fontSize: 13, color: AppColors.textSecondary)),
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

class _PaymentRadioTile extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final String value;
  final String groupValue;
  final ValueChanged<String?> onChanged;

  const _PaymentRadioTile({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.value,
    required this.groupValue,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final isSelected = value == groupValue;

    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(
        color: isSelected ? AppColors.primaryLight.withOpacity(0.3) : Colors.transparent,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: isSelected ? AppColors.primary : AppColors.border,
        ),
      ),
      child: RadioListTile<String>(
        value: value,
        groupValue: groupValue,
        activeColor: AppColors.primary,
        onChanged: onChanged,
        secondary: Icon(icon, color: isSelected ? AppColors.primary : AppColors.textSecondary),
        title: Text(title, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
        subtitle: Text(subtitle, style: const TextStyle(fontSize: 11, color: AppColors.textSecondary)),
      ),
    );
  }
}
