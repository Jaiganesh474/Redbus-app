import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../models/booking_model.dart';
import '../../providers/auth_provider.dart';
import '../../providers/booking_provider.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/custom_text_field.dart';
import 'checkout_screen.dart';

class PassengerDetailsScreen extends StatefulWidget {
  const PassengerDetailsScreen({super.key});

  @override
  State<PassengerDetailsScreen> createState() => _PassengerDetailsScreenState();
}

class _PassengerDetailsScreenState extends State<PassengerDetailsScreen> {
  final _formKey = GlobalKey<FormState>();
  final List<TextEditingController> _nameControllers = [];
  final List<TextEditingController> _ageControllers = [];
  final List<String> _genders = [];

  late final TextEditingController _phoneController;
  late final TextEditingController _emailController;
  final TextEditingController _gstNumberController = TextEditingController();
  final TextEditingController _gstCompanyNameController = TextEditingController();

  bool _sendWhatsAppUpdates = true;
  bool _hasGst = false;

  @override
  void initState() {
    super.initState();
    final bookingProvider = Provider.of<BookingProvider>(context, listen: false);
    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    _phoneController = TextEditingController(text: authProvider.user?.phone ?? '');
    _emailController = TextEditingController(text: authProvider.user?.email ?? '');

    for (int i = 0; i < bookingProvider.selectedSeats.length; i++) {
      String initialName = '';
      String initialAge = '';
      String initialGender = 'Male';

      if (i == 0 && authProvider.user != null) {
        initialName = authProvider.user!.name;
        if (authProvider.user!.gender != null && authProvider.user!.gender!.isNotEmpty) {
          initialGender = authProvider.user!.gender!;
        }
      } else if (authProvider.user != null && i < authProvider.user!.savedTravellers.length) {
        final st = authProvider.user!.savedTravellers[i];
        initialName = st.name;
        initialAge = st.age > 0 ? st.age.toString() : '';
        initialGender = st.gender;
      }

      _nameControllers.add(TextEditingController(text: initialName));
      _ageControllers.add(TextEditingController(text: initialAge));
      _genders.add(initialGender);
    }
  }

  @override
  void dispose() {
    for (var c in _nameControllers) {
      c.dispose();
    }
    for (var c in _ageControllers) {
      c.dispose();
    }
    _phoneController.dispose();
    _emailController.dispose();
    _gstNumberController.dispose();
    _gstCompanyNameController.dispose();
    super.dispose();
  }

  void _onProceedToPayment() {
    if (!_formKey.currentState!.validate()) return;

    final bookingProvider = Provider.of<BookingProvider>(context, listen: false);

    // Save passengers
    for (int i = 0; i < bookingProvider.selectedSeats.length; i++) {
      final p = PassengerInfo(
        name: _nameControllers[i].text.trim(),
        age: int.tryParse(_ageControllers[i].text.trim()) ?? 25,
        gender: _genders[i],
        seatNumber: bookingProvider.selectedSeats[i].seatNumber,
        seatPrice: bookingProvider.selectedSeats[i].price,
      );
      bookingProvider.updatePassenger(i, p);
    }

    bookingProvider.setContactInfo(_phoneController.text.trim(), _emailController.text.trim());

    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const CheckoutScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bookingProvider = Provider.of<BookingProvider>(context);
    final authProvider = Provider.of<AuthProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Passenger Details'),
        elevation: 0,
      ),
      body: Form(
        key: _formKey,
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Contact Details Card
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
                          const Row(
                            children: [
                              Icon(Icons.contact_phone_outlined, color: AppColors.primary, size: 20),
                              SizedBox(width: 8),
                              Text('Contact Information', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                            ],
                          ),
                          const SizedBox(height: 4),
                          const Text(
                            'Your e-ticket and bus live tracking link will be sent here',
                            style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
                          ),
                          const SizedBox(height: 14),

                          CustomTextField(
                            label: 'Mobile Number',
                            hintText: 'Enter 10-digit mobile number',
                            controller: _phoneController,
                            prefixIcon: Icons.phone_android_rounded,
                            keyboardType: TextInputType.phone,
                            validator: (val) {
                              if (val == null || val.trim().length < 10) {
                                return 'Please enter a valid 10-digit mobile number';
                              }
                              return null;
                            },
                          ),

                          const SizedBox(height: 12),

                          CustomTextField(
                            label: 'Email ID',
                            hintText: 'Enter email address for e-ticket',
                            controller: _emailController,
                            prefixIcon: Icons.email_outlined,
                            keyboardType: TextInputType.emailAddress,
                            validator: (val) {
                              if (val == null || !val.contains('@') || !val.contains('.')) {
                                return 'Please enter a valid email address';
                              }
                              return null;
                            },
                          ),

                          const SizedBox(height: 12),

                          // WhatsApp updates toggle
                          InkWell(
                            onTap: () => setState(() => _sendWhatsAppUpdates = !_sendWhatsAppUpdates),
                            borderRadius: BorderRadius.circular(8),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(vertical: 4),
                              child: Row(
                                children: [
                                  Icon(
                                    _sendWhatsAppUpdates ? Icons.check_box_rounded : Icons.check_box_outline_blank_rounded,
                                    color: _sendWhatsAppUpdates ? AppColors.success : Colors.grey,
                                    size: 20,
                                  ),
                                  const SizedBox(width: 8),
                                  const Icon(Icons.chat, color: Color(0xFF25D366), size: 16),
                                  const SizedBox(width: 6),
                                  const Expanded(
                                    child: Text(
                                      'Send booking details & updates on WhatsApp',
                                      style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),

                    const SizedBox(height: 16),

                    // Saved Travellers Quick Selection
                    if (authProvider.user?.savedTravellers.isNotEmpty ?? false) ...[
                      Row(
                        children: [
                          const Icon(Icons.people_alt_rounded, size: 18, color: AppColors.primary),
                          const SizedBox(width: 8),
                          const Text(
                            'Saved Co-Travellers',
                            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: Row(
                          children: authProvider.user!.savedTravellers.map((st) {
                            return Padding(
                              padding: const EdgeInsets.only(right: 8),
                              child: ActionChip(
                                avatar: const Icon(Icons.person_add_alt_1_rounded, size: 16, color: AppColors.primary),
                                label: Text('${st.name} (${st.age}y, ${st.gender[0]})'),
                                backgroundColor: isDark ? AppColors.darkCard : const Color(0xFFF3F4F6),
                                onPressed: () {
                                  for (int i = 0; i < _nameControllers.length; i++) {
                                    if (_nameControllers[i].text.isEmpty || i == 0) {
                                      setState(() {
                                        _nameControllers[i].text = st.name;
                                        _ageControllers[i].text = st.age.toString();
                                        _genders[i] = st.gender;
                                      });
                                      break;
                                    }
                                  }
                                },
                              ),
                            );
                          }).toList(),
                        ),
                      ),
                      const SizedBox(height: 16),
                    ],

                    // Passenger Info Cards for each selected seat
                    ...List.generate(bookingProvider.selectedSeats.length, (index) {
                      final seat = bookingProvider.selectedSeats[index];
                      return Container(
                        margin: const EdgeInsets.only(bottom: 16),
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
                                Row(
                                  children: [
                                    CircleAvatar(
                                      radius: 12,
                                      backgroundColor: AppColors.primaryLight,
                                      child: Text(
                                        '${index + 1}',
                                        style: const TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.bold,
                                          color: AppColors.primary,
                                        ),
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Text(
                                      'Passenger ${index + 1}',
                                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                                    ),
                                  ],
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: AppColors.primaryLight,
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Text(
                                    'Seat ${seat.seatNumber} • ₹${seat.price.toInt()}',
                                    style: const TextStyle(
                                      fontSize: 11,
                                      fontWeight: FontWeight.bold,
                                      color: AppColors.primary,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 14),

                            // Full Name
                            CustomTextField(
                              label: 'Full Name',
                              hintText: 'Enter passenger full name',
                              controller: _nameControllers[index],
                              prefixIcon: Icons.person_outline_rounded,
                              validator: (val) {
                                if (val == null || val.trim().isEmpty) return 'Please enter passenger name';
                                return null;
                              },
                            ),

                            const SizedBox(height: 12),

                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                // Age
                                Expanded(
                                  flex: 1,
                                  child: CustomTextField(
                                    label: 'Age',
                                    hintText: 'Age',
                                    controller: _ageControllers[index],
                                    keyboardType: TextInputType.number,
                                    validator: (val) {
                                      if (val == null || val.trim().isEmpty) return 'Required';
                                      final age = int.tryParse(val);
                                      if (age == null || age <= 0 || age > 120) return 'Valid age';
                                      return null;
                                    },
                                  ),
                                ),
                                const SizedBox(width: 12),

                                // Gender Selector
                                Expanded(
                                  flex: 2,
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      const Text(
                                        'Gender',
                                        style: TextStyle(
                                          fontSize: 13,
                                          fontWeight: FontWeight.w600,
                                          color: AppColors.textPrimary,
                                        ),
                                      ),
                                      const SizedBox(height: 6),
                                      Row(
                                        children: ['Male', 'Female', 'Other'].map((g) {
                                          final isSelected = _genders[index] == g;
                                          return Expanded(
                                            child: Padding(
                                              padding: const EdgeInsets.symmetric(horizontal: 2),
                                              child: InkWell(
                                                onTap: () => setState(() => _genders[index] = g),
                                                borderRadius: BorderRadius.circular(8),
                                                child: Container(
                                                  padding: const EdgeInsets.symmetric(vertical: 12),
                                                  decoration: BoxDecoration(
                                                    color: isSelected
                                                        ? AppColors.primary
                                                        : (isDark ? AppColors.darkSurface : const Color(0xFFF3F4F6)),
                                                    borderRadius: BorderRadius.circular(8),
                                                    border: Border.all(
                                                      color: isSelected
                                                          ? AppColors.primary
                                                          : (isDark ? const Color(0xFF374151) : AppColors.border),
                                                    ),
                                                  ),
                                                  child: Center(
                                                    child: Text(
                                                      g,
                                                      style: TextStyle(
                                                        fontSize: 11,
                                                        fontWeight: FontWeight.bold,
                                                        color: isSelected
                                                            ? Colors.white
                                                            : (isDark ? Colors.white : AppColors.textPrimary),
                                                      ),
                                                    ),
                                                  ),
                                                ),
                                              ),
                                            ),
                                          );
                                        }).toList(),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      );
                    }),

                    // ACKO Travel Insurance Card
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: isDark ? AppColors.darkCard : Colors.white,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: bookingProvider.optedInsurance ? AppColors.success : (isDark ? const Color(0xFF374151) : AppColors.border),
                        ),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.all(6),
                                decoration: BoxDecoration(
                                  color: AppColors.successLight,
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: const Icon(Icons.shield_rounded, color: AppColors.success, size: 20),
                              ),
                              const SizedBox(width: 10),
                              const Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      children: [
                                        Text('ACKO Travel Insurance', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                                        SizedBox(width: 6),
                                        Text('₹15/person', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.success, fontSize: 12)),
                                      ],
                                    ),
                                    Text('Accident & hospitalization cover up to ₹5,00,000', style: TextStyle(fontSize: 11, color: AppColors.textSecondary)),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          RadioListTile<bool>(
                            contentPadding: EdgeInsets.zero,
                            title: const Text('Yes, protect my trip (Recommended)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                            subtitle: const Text('Covers personal accident, medical emergency & baggage loss', style: TextStyle(fontSize: 11, color: AppColors.textSecondary)),
                            value: true,
                            groupValue: bookingProvider.optedInsurance,
                            activeColor: AppColors.success,
                            onChanged: (val) => bookingProvider.toggleInsurance(val ?? true),
                          ),
                          RadioListTile<bool>(
                            contentPadding: EdgeInsets.zero,
                            title: const Text('No, I will risk my travel', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
                            value: false,
                            groupValue: bookingProvider.optedInsurance,
                            activeColor: AppColors.primary,
                            onChanged: (val) => bookingProvider.toggleInsurance(val ?? false),
                          ),
                        ],
                      ),
                    ),

                    const SizedBox(height: 16),

                    // GST Checkbox
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: isDark ? AppColors.darkCard : Colors.white,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          InkWell(
                            onTap: () => setState(() => _hasGst = !_hasGst),
                            child: Row(
                              children: [
                                Icon(
                                  _hasGst ? Icons.check_box_rounded : Icons.check_box_outline_blank_rounded,
                                  color: _hasGst ? AppColors.primary : Colors.grey,
                                  size: 20,
                                ),
                                const SizedBox(width: 10),
                                const Text(
                                  'I have a GST number (Optional)',
                                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                                ),
                              ],
                            ),
                          ),
                          if (_hasGst) ...[
                            const SizedBox(height: 12),
                            CustomTextField(
                              label: 'GST Number',
                              hintText: 'e.g. 29ABCDE1234F1Z5',
                              controller: _gstNumberController,
                            ),
                            const SizedBox(height: 8),
                            CustomTextField(
                              label: 'Company Name',
                              hintText: 'Registered business name',
                              controller: _gstCompanyNameController,
                            ),
                          ],
                        ],
                      ),
                    ),

                    const SizedBox(height: 20),
                  ],
                ),
              ),
            ),

            // Bottom Sticky Bar
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
                            'Total Amount',
                            style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
                          ),
                          Text(
                            '₹${bookingProvider.totalPayableAmount.toStringAsFixed(1)}',
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w900,
                              color: AppColors.primary,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Expanded(
                      child: CustomButton(
                        text: 'PROCEED TO PAY',
                        onPressed: _onProceedToPayment,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
