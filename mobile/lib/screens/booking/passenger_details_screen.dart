import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../models/booking_model.dart';
import '../../models/seat_model.dart';
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

  final TextEditingController _phoneController = TextEditingController(text: '9876543210');
  final TextEditingController _emailController = TextEditingController(text: 'traveler@redbus.in');

  @override
  void initState() {
    super.initState();
    final bookingProvider = Provider.of<BookingProvider>(context, listen: false);
    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    for (int i = 0; i < bookingProvider.selectedSeats.length; i++) {
      // Pre-fill first passenger from saved travellers or user if available
      String initialName = '';
      String initialAge = '26';
      String initialGender = 'Male';

      if (i == 0 && authProvider.user != null) {
        initialName = authProvider.user!.name;
        if (authProvider.user!.gender != null) {
          initialGender = authProvider.user!.gender!;
        }
      } else if (authProvider.user != null && i < authProvider.user!.savedTravellers.length) {
        final st = authProvider.user!.savedTravellers[i];
        initialName = st.name;
        initialAge = st.age.toString();
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
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Passenger Details'),
      ),
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Saved Travellers Quick Select Chips
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
                          avatar: const Icon(Icons.person_add_alt_1_rounded, size: 16),
                          label: Text('${st.name} (${st.age}, ${st.gender[0]})'),
                          backgroundColor: isDark ? AppColors.darkCard : const Color(0xFFF3F4F6),
                          onPressed: () {
                            // fill first empty passenger or passenger 0
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

              // Passenger Cards for each seat
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
                          Text(
                            'Passenger ${index + 1}',
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: AppColors.primaryLight,
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              'Seat ${seat.seatNumber} (${seat.deck == DeckType.upper ? "Upper" : "Lower"})',
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

                      // Name Field
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
                          // Age Field
                          Expanded(
                            flex: 1,
                            child: CustomTextField(
                              label: 'Age',
                              hintText: 'Age',
                              controller: _ageControllers[index],
                              keyboardType: TextInputType.number,
                              validator: (val) {
                                if (val == null || val.trim().isEmpty) return 'Enter age';
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
                                              color: isSelected ? AppColors.primary : (isDark ? AppColors.darkSurface : const Color(0xFFF3F4F6)),
                                              borderRadius: BorderRadius.circular(8),
                                              border: Border.all(
                                                color: isSelected ? AppColors.primary : (isDark ? const Color(0xFF374151) : AppColors.border),
                                              ),
                                            ),
                                            child: Center(
                                              child: Text(
                                                g,
                                                style: TextStyle(
                                                  fontSize: 11,
                                                  fontWeight: FontWeight.bold,
                                                  color: isSelected ? Colors.white : (isDark ? Colors.white : AppColors.textPrimary),
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

              // Contact Details Card
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
                    const Text('Contact Information', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                    const SizedBox(height: 4),
                    const Text('Your e-ticket & bus tracking link will be sent here', style: TextStyle(fontSize: 11, color: AppColors.textSecondary)),
                    const SizedBox(height: 14),

                    CustomTextField(
                      label: 'Mobile Number',
                      hintText: '10-digit mobile number',
                      controller: _phoneController,
                      prefixIcon: Icons.phone_android_rounded,
                      keyboardType: TextInputType.phone,
                      validator: (val) => (val == null || val.length < 10) ? 'Enter valid phone number' : null,
                    ),

                    const SizedBox(height: 12),

                    CustomTextField(
                      label: 'Email ID',
                      hintText: 'name@example.com',
                      controller: _emailController,
                      prefixIcon: Icons.email_outlined,
                      keyboardType: TextInputType.emailAddress,
                      validator: (val) => (val == null || !val.contains('@')) ? 'Enter valid email' : null,
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 16),

              // Travel Insurance Opt-In Card
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: bookingProvider.optedInsurance ? AppColors.successLight.withOpacity(0.5) : (isDark ? AppColors.darkCard : Colors.white),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: bookingProvider.optedInsurance ? AppColors.success : (isDark ? const Color(0xFF374151) : AppColors.border)),
                ),
                child: Row(
                  children: [
                    Checkbox(
                      value: bookingProvider.optedInsurance,
                      activeColor: AppColors.success,
                      onChanged: (val) => bookingProvider.toggleInsurance(val ?? true),
                    ),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            children: [
                              Text('Travel Insurance', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                              SizedBox(width: 6),
                              Text('₹15/person', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.success, fontSize: 12)),
                            ],
                          ),
                          const SizedBox(height: 2),
                          Text(
                            'Medical emergency & baggage loss coverage up to ₹5,00,000',
                            style: TextStyle(fontSize: 11, color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 24),

              // Continue Button
              CustomButton(
                text: 'PROCEED TO PAYMENT',
                onPressed: _onProceedToPayment,
              ),

              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }
}
