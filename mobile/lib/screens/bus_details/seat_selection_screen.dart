import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../models/seat_model.dart';
import '../../providers/booking_provider.dart';
import '../../widgets/custom_button.dart';
import '../booking/boarding_dropping_screen.dart';

class SeatSelectionScreen extends StatelessWidget {
  const SeatSelectionScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final bookingProvider = Provider.of<BookingProvider>(context);
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final bus = bookingProvider.selectedBus;

    if (bus == null) {
      return const Scaffold(
        body: Center(child: Text('No bus selected')),
      );
    }

    final currentDeckSeats = bookingProvider.busSeats
        .where((s) => s.deck == bookingProvider.activeDeck)
        .toList();

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(bus.operatorName, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            Text(
              '${bus.sourceCity} → ${bus.destinationCity} • ${bus.departureTime}',
              style: TextStyle(
                fontSize: 12,
                color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                fontWeight: FontWeight.normal,
              ),
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          // Deck Switcher (Lower / Upper)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            color: isDark ? AppColors.darkCard : Colors.white,
            child: Row(
              children: [
                Expanded(
                  child: _DeckTabButton(
                    title: 'Lower Deck',
                    subtitle: 'Seater & Sleeper',
                    isSelected: bookingProvider.activeDeck == DeckType.lower,
                    onTap: () => bookingProvider.setActiveDeck(DeckType.lower),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _DeckTabButton(
                    title: 'Upper Deck',
                    subtitle: 'Sleeper Berths',
                    isSelected: bookingProvider.activeDeck == DeckType.upper,
                    onTap: () => bookingProvider.setActiveDeck(DeckType.upper),
                  ),
                ),
              ],
            ),
          ),

          // Legend Bar
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkSurface : const Color(0xFFF9FAFB),
              border: Border(
                bottom: BorderSide(
                  color: isDark ? const Color(0xFF374151) : AppColors.border,
                ),
              ),
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _LegendItem(color: Colors.white, borderColor: AppColors.seatBorder, label: 'Available'),
                _LegendItem(color: AppColors.seatSelected, label: 'Selected'),
                _LegendItem(color: AppColors.seatBooked, label: 'Booked'),
                _LegendItem(color: AppColors.seatFemaleLight, borderColor: AppColors.seatFemale, label: 'Female'),
              ],
            ),
          ),

          // Bus Bus Structure Graphic (Cabin Outline)
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Center(
                child: Container(
                  constraints: const BoxConstraints(maxWidth: 380),
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: isDark ? AppColors.darkCard : Colors.white,
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(
                      color: isDark ? const Color(0xFF374151) : AppColors.border,
                      width: 2,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.06),
                        blurRadius: 15,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Column(
                    children: [
                      // Driver Steering Wheel Row
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(
                              color: isDark ? const Color(0xFF374151) : AppColors.background,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              bookingProvider.activeDeck == DeckType.lower ? 'LOWER DECK' : 'UPPER DECK',
                              style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold, letterSpacing: 1),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.all(6),
                            decoration: BoxDecoration(
                              color: isDark ? const Color(0xFF374151) : const Color(0xFFE5E7EB),
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(Icons.drive_eta_rounded, size: 20, color: AppColors.textSecondary),
                          ),
                        ],
                      ),

                      const SizedBox(height: 16),
                      const Divider(thickness: 1.5),
                      const SizedBox(height: 16),

                      // Seat Grid: 5 Rows x 3 Cols (2 Left + Aisle + 1 Right)
                      Column(
                        children: List.generate(5, (rowIndex) {
                          final rowNum = rowIndex + 1;
                          final rowSeats = currentDeckSeats.where((s) => s.row == rowNum).toList();

                          final leftSeats = rowSeats.where((s) => s.column <= 2).toList();
                          final rightSeats = rowSeats.where((s) => s.column > 2).toList();

                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 8),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                // Left 2 Seats / Berth
                                Row(
                                  children: leftSeats.map((seat) {
                                    return Padding(
                                      padding: const EdgeInsets.only(right: 8),
                                      child: _SeatItem(
                                        seat: seat,
                                        onTap: () => bookingProvider.toggleSeatSelection(seat),
                                      ),
                                    );
                                  }).toList(),
                                ),

                                // Center Aisle Walkway
                                Container(
                                  width: 28,
                                  alignment: Alignment.center,
                                  child: Text(
                                    '|',
                                    style: TextStyle(
                                      color: isDark ? const Color(0xFF4B5563) : AppColors.border,
                                      fontSize: 18,
                                    ),
                                  ),
                                ),

                                // Right Single Seat / Berth
                                Row(
                                  children: rightSeats.map((seat) {
                                    return _SeatItem(
                                      seat: seat,
                                      onTap: () => bookingProvider.toggleSeatSelection(seat),
                                    );
                                  }).toList(),
                                ),
                              ],
                            ),
                          );
                        }),
                      ),

                      const SizedBox(height: 12),
                    ],
                  ),
                ),
              ),
            ),
          ),

          // Bottom Bar (Selected seats, price, Proceed button)
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkCard : Colors.white,
              border: Border(
                top: BorderSide(
                  color: isDark ? const Color(0xFF374151) : AppColors.border,
                ),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.08),
                  blurRadius: 10,
                  offset: const Offset(0, -4),
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
                        if (bookingProvider.selectedSeats.isEmpty)
                          const Text(
                            'Please select seat(s)',
                            style: TextStyle(fontSize: 13, color: AppColors.textSecondary, fontWeight: FontWeight.w500),
                          )
                        else ...[
                          Text(
                            'Seats: ${bookingProvider.selectedSeats.map((s) => s.seatNumber).join(', ')}',
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.bold,
                              color: AppColors.primary,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: 2),
                          Text(
                            '₹${bookingProvider.baseFare.toInt()}',
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  CustomButton(
                    width: 150,
                    text: 'CONTINUE',
                    onPressed: bookingProvider.selectedSeats.isEmpty
                        ? null
                        : () {
                            Navigator.of(context).push(
                              MaterialPageRoute(builder: (_) => const BoardingDroppingScreen()),
                            );
                          },
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

class _DeckTabButton extends StatelessWidget {
  final String title;
  final String subtitle;
  final bool isSelected;
  final VoidCallback onTap;

  const _DeckTabButton({
    required this.title,
    required this.subtitle,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 12),
        decoration: BoxDecoration(
          color: isSelected
              ? AppColors.primaryLight
              : (isDark ? AppColors.darkSurface : const Color(0xFFF9FAFB)),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isSelected ? AppColors.primary : (isDark ? const Color(0xFF374151) : AppColors.border),
            width: isSelected ? 1.5 : 1,
          ),
        ),
        child: Column(
          children: [
            Text(
              title,
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 13,
                color: isSelected ? AppColors.primary : (isDark ? Colors.white : AppColors.textPrimary),
              ),
            ),
            const SizedBox(height: 2),
            Text(
              subtitle,
              style: TextStyle(
                fontSize: 10,
                color: isSelected ? AppColors.primaryDark : AppColors.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LegendItem extends StatelessWidget {
  final Color color;
  final Color? borderColor;
  final String label;

  const _LegendItem({required this.color, this.borderColor, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 14,
          height: 14,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(3),
            border: Border.all(color: borderColor ?? Colors.transparent, width: 1.2),
          ),
        ),
        const SizedBox(width: 6),
        Text(label, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w500)),
      ],
    );
  }
}

class _SeatItem extends StatelessWidget {
  final Seat seat;
  final VoidCallback onTap;

  const _SeatItem({required this.seat, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final isSleeper = seat.type == SeatType.sleeper;
    final width = isSleeper ? 50.0 : 42.0;
    final height = isSleeper ? 72.0 : 44.0;

    Color getBgColor() {
      if (seat.isSelected) return AppColors.seatSelected;
      if (seat.isBooked) return AppColors.seatBooked;
      if (seat.isFemale) return AppColors.seatFemaleLight;
      return Colors.white;
    }

    Color getBorderColor() {
      if (seat.isSelected) return AppColors.seatSelected;
      if (seat.isBooked) return const Color(0xFF9CA3AF);
      if (seat.isFemale) return AppColors.seatFemale;
      return AppColors.seatBorder;
    }

    Color getTextColor() {
      if (seat.isSelected) return Colors.white;
      if (seat.isBooked) return const Color(0xFF6B7280);
      if (seat.isFemale) return AppColors.seatFemale;
      return AppColors.textPrimary;
    }

    return InkWell(
      onTap: seat.isBooked ? null : onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: getBgColor(),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: getBorderColor(), width: 1.5),
          boxShadow: seat.isSelected
              ? [
                  BoxShadow(
                    color: AppColors.seatSelected.withOpacity(0.4),
                    blurRadius: 6,
                    offset: const Offset(0, 2),
                  ),
                ]
              : [],
        ),
        child: Stack(
          alignment: Alignment.center,
          children: [
            // Headrest / Pillow indication
            Positioned(
              top: 3,
              child: Container(
                width: width * 0.7,
                height: 4,
                decoration: BoxDecoration(
                  color: getBorderColor().withOpacity(0.4),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),

            // Seat Number
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  seat.seatNumber,
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: getTextColor(),
                  ),
                ),
                if (isSleeper) ...[
                  const SizedBox(height: 2),
                  Text(
                    '₹${seat.price.toInt()}',
                    style: TextStyle(
                      fontSize: 9,
                      fontWeight: FontWeight.w600,
                      color: getTextColor(),
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }
}
