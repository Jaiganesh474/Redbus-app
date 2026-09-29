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
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _searchController.addListener(() {
      setState(() {
        _searchQuery = _searchController.text.trim().toLowerCase();
      });
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bookingProvider = Provider.of<BookingProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final bus = bookingProvider.selectedBus!;

    final filteredBoardingPoints = bus.boardingPoints.where((p) {
      if (_searchQuery.isEmpty) return true;
      return p.name.toLowerCase().contains(_searchQuery) || p.landmark.toLowerCase().contains(_searchQuery);
    }).toList();

    final filteredDroppingPoints = bus.droppingPoints.where((p) {
      if (_searchQuery.isEmpty) return true;
      return p.name.toLowerCase().contains(_searchQuery) || p.landmark.toLowerCase().contains(_searchQuery);
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Boarding & Dropping Points'),
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppColors.primary,
          labelColor: AppColors.primary,
          unselectedLabelColor: AppColors.textSecondary,
          labelStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
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
      body: Column(
        children: [
          // Search & GPS Bar
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            color: isDark ? AppColors.darkCard : Colors.white,
            child: Row(
              children: [
                Expanded(
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(
                      color: isDark ? AppColors.darkSurface : const Color(0xFFF3F4F6),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.search_rounded, size: 18, color: AppColors.textSecondary),
                        const SizedBox(width: 8),
                        Expanded(
                          child: TextField(
                            controller: _searchController,
                            decoration: const InputDecoration(
                              hintText: 'Search boarding / dropping location',
                              hintStyle: TextStyle(fontSize: 13, color: AppColors.textMuted),
                              border: InputBorder.none,
                              isDense: true,
                              contentPadding: EdgeInsets.symmetric(vertical: 10),
                            ),
                          ),
                        ),
                        if (_searchQuery.isNotEmpty)
                          IconButton(
                            icon: const Icon(Icons.clear_rounded, size: 16),
                            onPressed: () => _searchController.clear(),
                          ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                InkWell(
                  onTap: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Locating closest stops to your GPS location... 📍')),
                    );
                  },
                  borderRadius: BorderRadius.circular(12),
                  child: Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.primaryLight,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.my_location_rounded, color: AppColors.primary, size: 20),
                  ),
                ),
              ],
            ),
          ),

          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                // Boarding Points Tab
                filteredBoardingPoints.isEmpty
                    ? const Center(child: Text('No boarding stops match your search'))
                    : ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: filteredBoardingPoints.length,
                        itemBuilder: (context, index) {
                          final point = filteredBoardingPoints[index];
                          final isSelected = bookingProvider.selectedBoardingPoint?.id == point.id;
                          return _PointSelectionCard(
                            point: point,
                            isSelected: isSelected,
                            isDark: isDark,
                            onTap: () {
                              bookingProvider.setBoardingPoint(point);
                              // Automatically switch to dropping point tab
                              _tabController.animateTo(1);
                            },
                          );
                        },
                      ),

                // Dropping Points Tab
                filteredDroppingPoints.isEmpty
                    ? const Center(child: Text('No dropping stops match your search'))
                    : ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: filteredDroppingPoints.length,
                        itemBuilder: (context, index) {
                          final point = filteredDroppingPoints[index];
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
          ),
        ],
      ),
      bottomNavigationBar: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isDark ? AppColors.darkCard : Colors.white,
          border: Border(top: BorderSide(color: isDark ? const Color(0xFF374151) : AppColors.border)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.04),
              blurRadius: 8,
              offset: const Offset(0, -2),
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
            ? AppColors.primaryLight.withOpacity(0.4)
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
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (point.landmark.isNotEmpty) ...[
              const SizedBox(height: 2),
              Text(
                'Landmark: ${point.landmark}',
                style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
              ),
            ],
            const SizedBox(height: 4),
            Row(
              children: [
                const Icon(Icons.directions_subway_rounded, size: 12, color: Color(0xFF2563EB)),
                const SizedBox(width: 4),
                Text(
                  'Metro & Local Bus Hub Nearby',
                  style: TextStyle(fontSize: 10, color: Colors.blue[700], fontWeight: FontWeight.w500),
                ),
              ],
            ),
          ],
        ),
        trailing: Icon(
          isSelected ? Icons.radio_button_checked_rounded : Icons.radio_button_off_rounded,
          color: isSelected ? AppColors.primary : AppColors.textMuted,
        ),
      ),
    );
  }
}
