import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../config/theme.dart';
import '../../models/bus_model.dart';
import '../../providers/search_provider.dart';
import '../../providers/booking_provider.dart';
import '../../widgets/bus_card.dart';
import 'filter_sort_bottom_sheet.dart';
import '../bus_details/bus_details_screen.dart';
import '../bus_details/seat_selection_screen.dart';

class SearchResultsScreen extends StatelessWidget {
  const SearchResultsScreen({super.key});

  void _openFilters(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const FilterSortBottomSheet(),
    );
  }

  void _onBusSelected(BuildContext context, Bus bus, bool directToSeats) {
    final bookingProvider = Provider.of<BookingProvider>(context, listen: false);
    bookingProvider.selectBus(bus);

    if (directToSeats) {
      Navigator.of(context).push(
        MaterialPageRoute(builder: (_) => const SeatSelectionScreen()),
      );
    } else {
      Navigator.of(context).push(
        MaterialPageRoute(builder: (_) => BusDetailsScreen(bus: bus)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final searchProvider = Provider.of<SearchProvider>(context);
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  searchProvider.sourceCity,
                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(width: 6),
                const Icon(Icons.arrow_forward_rounded, size: 14, color: AppColors.primary),
                const SizedBox(width: 6),
                Text(
                  searchProvider.destinationCity,
                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            Text(
              searchProvider.formattedTravelDate,
              style: TextStyle(
                fontSize: 12,
                color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                fontWeight: FontWeight.normal,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.tune_rounded),
            onPressed: () => _openFilters(context),
          ),
        ],
      ),
      body: Column(
        children: [
          // Date Switcher Ribbon
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkCard : Colors.white,
              border: Border(
                bottom: BorderSide(
                  color: isDark ? const Color(0xFF374151) : AppColors.border,
                ),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                InkWell(
                  onTap: () {
                    final prevDate = searchProvider.travelDate.subtract(const Duration(days: 1));
                    if (!prevDate.isBefore(DateTime.now().subtract(const Duration(hours: 12)))) {
                      searchProvider.setTravelDate(prevDate);
                      searchProvider.searchBuses();
                    }
                  },
                  child: Row(
                    children: [
                      Icon(
                        Icons.chevron_left_rounded,
                        size: 20,
                        color: isDark ? Colors.white70 : AppColors.textSecondary,
                      ),
                      const SizedBox(width: 2),
                      Text(
                        'Prev Day',
                        style: TextStyle(
                          fontSize: 12,
                          color: isDark ? Colors.white70 : AppColors.textSecondary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.primaryLight,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    DateFormat('EEEE, d MMM').format(searchProvider.travelDate),
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: AppColors.primary,
                    ),
                  ),
                ),
                InkWell(
                  onTap: () {
                    final nextDate = searchProvider.travelDate.add(const Duration(days: 1));
                    searchProvider.setTravelDate(nextDate);
                    searchProvider.searchBuses();
                  },
                  child: Row(
                    children: [
                      Text(
                        'Next Day',
                        style: TextStyle(
                          fontSize: 12,
                          color: isDark ? Colors.white70 : AppColors.textSecondary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(width: 2),
                      Icon(
                        Icons.chevron_right_rounded,
                        size: 20,
                        color: isDark ? Colors.white70 : AppColors.textSecondary,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          // Quick Filter Chips Bar
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: Row(
              children: [
                _FilterPill(
                  label: 'Filters',
                  icon: Icons.filter_list_rounded,
                  isActive: searchProvider.filterPrimoOnly ||
                      searchProvider.filterAcOnly ||
                      searchProvider.filterSleeperOnly ||
                      searchProvider.selectedOperator != null,
                  onTap: () => _openFilters(context),
                ),
                const SizedBox(width: 6),
                _FilterPill(
                  label: 'Primo Buses',
                  icon: Icons.verified_rounded,
                  isActive: searchProvider.filterPrimoOnly,
                  onTap: () => searchProvider.togglePrimoFilter(),
                ),
                const SizedBox(width: 6),
                _FilterPill(
                  label: 'AC Only',
                  isActive: searchProvider.filterAcOnly,
                  onTap: () => searchProvider.toggleAcFilter(),
                ),
                const SizedBox(width: 6),
                _FilterPill(
                  label: 'Sleeper',
                  isActive: searchProvider.filterSleeperOnly,
                  onTap: () => searchProvider.toggleSleeperFilter(),
                ),
              ],
            ),
          ),

          // Buses List or Empty State
          Expanded(
            child: searchProvider.isLoading
                ? const Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        CircularProgressIndicator(color: AppColors.primary),
                        SizedBox(height: 16),
                        Text(
                          'Finding best routes & prices...',
                          style: TextStyle(fontWeight: FontWeight.w500),
                        ),
                      ],
                    ),
                  )
                : searchProvider.buses.isEmpty
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              const Icon(Icons.directions_bus_filled_outlined, size: 60, color: AppColors.textMuted),
                              const SizedBox(height: 16),
                              const Text(
                                'No buses available for this filter',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 8),
                              const Text(
                                'Try clearing some filters or changing your travel date.',
                                textAlign: TextAlign.center,
                                style: TextStyle(color: AppColors.textSecondary, fontSize: 13),
                              ),
                              const SizedBox(height: 16),
                              ElevatedButton(
                                onPressed: () => searchProvider.resetFilters(),
                                child: const Text('Reset All Filters'),
                              ),
                            ],
                          ),
                        ),
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.only(top: 4, bottom: 20),
                        itemCount: searchProvider.buses.length,
                        itemBuilder: (context, index) {
                          final bus = searchProvider.buses[index];
                          return BusCard(
                            bus: bus,
                            onSelectSeats: () => _onBusSelected(context, bus, true),
                            onViewDetails: () => _onBusSelected(context, bus, false),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}

class _FilterPill extends StatelessWidget {
  final String label;
  final IconData? icon;
  final bool isActive;
  final VoidCallback onTap;

  const _FilterPill({
    required this.label,
    this.icon,
    required this.isActive,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isActive
              ? AppColors.primary
              : (isDark ? AppColors.darkCard : Colors.white),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isActive
                ? AppColors.primary
                : (isDark ? const Color(0xFF374151) : AppColors.border),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 14, color: isActive ? Colors.white : AppColors.textSecondary),
              const SizedBox(width: 4),
            ],
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                fontWeight: isActive ? FontWeight.bold : FontWeight.w500,
                color: isActive
                    ? Colors.white
                    : (isDark ? Colors.white : AppColors.textPrimary),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
