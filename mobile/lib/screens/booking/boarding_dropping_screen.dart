import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../models/bus_model.dart';
import '../../providers/booking_provider.dart';
import '../../widgets/custom_button.dart';
import 'passenger_details_screen.dart';

class BoardingDroppingScreen extends StatefulWidget {
  const BoardingDroppingScreen({super.key});

  @override
  State<BoardingDroppingScreen> createState() => _BoardingDroppingScreenState();
}

class _BoardingDroppingScreenState extends State<BoardingDroppingScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
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
        title: const Text('Select Pickup & Drop Points'),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppColors.primary,
          labelColor: AppColors.primary,
          unselectedLabelColor: AppColors.textSecondary,
          labelStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
          tabs: [
            Tab(
              text: '1. BOARDING (${bookingProvider.selectedBoardingPoint?.name ?? "Select"})',
            ),
            Tab(
              text: '2. DROPPING (${bookingProvider.selectedDroppingPoint?.name ?? "Select"})',
            ),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // Boarding Points Tab
          ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: bus.boardingPoints.length,
            itemBuilder: (context, index) {
              final point = bus.boardingPoints[index];
              final isSelected = bookingProvider.selectedBoardingPoint?.id == point.id;
              return _PointSelectionCard(
                point: point,
                isSelected: isSelected,
                isDark: isDark,
                onTap: () {
                  bookingProvider.setBoardingPoint(point);
                  // Automatically advance to dropping tab
                  _tabController.animateTo(1);
                },
              );
            },
          ),

          // Dropping Points Tab
          ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: bus.droppingPoints.length,
            itemBuilder: (context, index) {
              final point = bus.droppingPoints[index];
              final isSelected = bookingProvider.selectedDroppingPoint?.id == point.id;
              return _PointSelectionCard(
                point: point,
                isSelected: isSelected,
                isDark: isDark,
                onTap: () {
                  bookingProvider.setDroppingPoint(point);
                },
              );
            },
          ),
        ],
      ),
      bottomNavigationBar: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isDark ? AppColors.darkCard : Colors.white,
          border: Border(top: BorderSide(color: isDark ? const Color(0xFF374151) : AppColors.border)),
        ),
        child: SafeArea(
          child: Row(
            children: [
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${bookingProvider.selectedBoardingPoint?.name ?? "Pick stop"} → ${bookingProvider.selectedDroppingPoint?.name ?? "Drop stop"}',
                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    Text(
                      '${bookingProvider.selectedSeats.length} Seat(s) Selected',
                      style: const TextStyle(fontSize: 11, color: AppColors.textSecondary),
                    ),
                  ],
                ),
              ),
              CustomButton(
                width: 150,
                text: 'CONTINUE',
                onPressed: (bookingProvider.selectedBoardingPoint != null &&
                        bookingProvider.selectedDroppingPoint != null)
                    ? () {
                        Navigator.of(context).push(
                          MaterialPageRoute(builder: (_) => const PassengerDetailsScreen()),
                        );
                      }
                    : null,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PointSelectionCard extends StatelessWidget {
  final BoardingDroppingPoint point;
  final bool isSelected;
  final bool isDark;
  final VoidCallback onTap;

  const _PointSelectionCard({
    required this.point,
    required this.isSelected,
    required this.isDark,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: isSelected
            ? AppColors.primaryLight.withOpacity(0.5)
            : (isDark ? AppColors.darkCard : Colors.white),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: isSelected ? AppColors.primary : (isDark ? const Color(0xFF374151) : AppColors.border),
          width: isSelected ? 1.8 : 1,
        ),
      ),
      child: ListTile(
        onTap: onTap,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        leading: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: isSelected ? AppColors.primary : (isDark ? const Color(0xFF374151) : AppColors.background),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Text(
            point.time,
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 13,
              color: isSelected ? Colors.white : AppColors.primary,
            ),
          ),
        ),
        title: Text(
          point.name,
          style: TextStyle(
            fontWeight: isSelected ? FontWeight.bold : FontWeight.w600,
            fontSize: 15,
          ),
        ),
        subtitle: point.landmark.isNotEmpty
            ? Text(
                'Landmark: ${point.landmark}',
                style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
              )
            : null,
        trailing: Icon(
          isSelected ? Icons.radio_button_checked_rounded : Icons.radio_button_off_rounded,
          color: isSelected ? AppColors.primary : AppColors.textMuted,
        ),
      ),
    );
  }
}
