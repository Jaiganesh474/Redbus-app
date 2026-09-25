import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../providers/search_provider.dart';
import '../../widgets/custom_button.dart';

class FilterSortBottomSheet extends StatefulWidget {
  const FilterSortBottomSheet({super.key});

  @override
  State<FilterSortBottomSheet> createState() => _FilterSortBottomSheetState();
}

class _FilterSortBottomSheetState extends State<FilterSortBottomSheet> {
  int _selectedTabIndex = 0;

  @override
  Widget build(BuildContext context) {
    final searchProvider = Provider.of<SearchProvider>(context);
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    final tabs = ['Sort', 'Bus Types', 'Price Range', 'Operators'];

    return Container(
      height: MediaQuery.of(context).size.height * 0.75,
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkSurface : Colors.white,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: Column(
        children: [
          // Drag handle
          Center(
            child: Container(
              margin: const EdgeInsets.only(top: 12, bottom: 8),
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: isDark ? const Color(0xFF4B5563) : AppColors.border,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),

          // Header
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
            child: Row(
              children: [
                const Text(
                  'Filters & Sorting',
                  style: TextStyle(
                    fontFamily: 'Outfit',
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                TextButton(
                  onPressed: () {
                    searchProvider.resetFilters();
                  },
                  child: const Text('Clear All', style: TextStyle(color: AppColors.primary)),
                ),
              ],
            ),
          ),

          const Divider(height: 1),

          // Side-by-side Tab layout (Category on Left, Options on Right)
          Expanded(
            child: Row(
              children: [
                // Left Tabs
                Container(
                  width: 120,
                  decoration: BoxDecoration(
                    color: isDark ? AppColors.darkCard : const Color(0xFFF9FAFB),
                    border: Border(
                      right: BorderSide(
                        color: isDark ? const Color(0xFF374151) : AppColors.border,
                      ),
                    ),
                  ),
                  child: ListView.builder(
                    itemCount: tabs.length,
                    itemBuilder: (context, index) {
                      final isSelected = _selectedTabIndex == index;
                      return InkWell(
                        onTap: () => setState(() => _selectedTabIndex = index),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 16),
                          decoration: BoxDecoration(
                            color: isSelected
                                ? (isDark ? AppColors.darkSurface : Colors.white)
                                : Colors.transparent,
                            border: Border(
                              left: BorderSide(
                                color: isSelected ? AppColors.primary : Colors.transparent,
                                width: 3,
                              ),
                            ),
                          ),
                          child: Text(
                            tabs[index],
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                              color: isSelected
                                  ? AppColors.primary
                                  : (isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),

                // Right Content
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: _buildTabContent(searchProvider, isDark),
                  ),
                ),
              ],
            ),
          ),

          // Bottom Apply Bar
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkCard : Colors.white,
              border: Border(top: BorderSide(color: isDark ? const Color(0xFF374151) : AppColors.border)),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    '${searchProvider.buses.length} Buses found',
                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                  ),
                ),
                CustomButton(
                  width: 140,
                  height: 44,
                  text: 'APPLY',
                  onPressed: () => Navigator.pop(context),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTabContent(SearchProvider searchProvider, bool isDark) {
    switch (_selectedTabIndex) {
      case 0: // Sort
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildRadioSort(searchProvider, SortOption.highestRated, 'Highest Rated', Icons.star_rounded),
            _buildRadioSort(searchProvider, SortOption.cheapest, 'Cheapest First', Icons.arrow_downward_rounded),
            _buildRadioSort(searchProvider, SortOption.fastest, 'Fastest Route', Icons.speed_rounded),
            _buildRadioSort(searchProvider, SortOption.earlyDeparture, 'Early Departure', Icons.wb_sunny_outlined),
            _buildRadioSort(searchProvider, SortOption.lateDeparture, 'Late Departure (Night)', Icons.nightlight_round),
          ],
        );

      case 1: // Bus Types
        return Column(
          children: [
            CheckboxListTile(
              title: const Text('Primo Verified Only', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
              subtitle: const Text('Top rated, punctual buses', style: TextStyle(fontSize: 11)),
              value: searchProvider.filterPrimoOnly,
              activeColor: AppColors.primary,
              onChanged: (_) => searchProvider.togglePrimoFilter(),
            ),
            CheckboxListTile(
              title: const Text('AC Buses Only', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
              value: searchProvider.filterAcOnly,
              activeColor: AppColors.primary,
              onChanged: (_) => searchProvider.toggleAcFilter(),
            ),
            CheckboxListTile(
              title: const Text('Sleeper Berths', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
              value: searchProvider.filterSleeperOnly,
              activeColor: AppColors.primary,
              onChanged: (_) => searchProvider.toggleSleeperFilter(),
            ),
          ],
        );

      case 2: // Price Range
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '₹${searchProvider.priceRange.start.toInt()} - ₹${searchProvider.priceRange.end.toInt()}',
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: AppColors.primary),
            ),
            const SizedBox(height: 16),
            RangeSlider(
              values: searchProvider.priceRange,
              min: 300,
              max: 2500,
              divisions: 22,
              activeColor: AppColors.primary,
              onChanged: (values) => searchProvider.setPriceRange(values),
            ),
            const Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('₹300', style: TextStyle(fontSize: 12, color: AppColors.textMuted)),
                Text('₹2500', style: TextStyle(fontSize: 12, color: AppColors.textMuted)),
              ],
            ),
          ],
        );

      case 3: // Operators
        final operators = searchProvider.uniqueOperators;
        return ListView.builder(
          itemCount: operators.length + 1,
          itemBuilder: (context, index) {
            if (index == 0) {
              return RadioListTile<String?>(
                title: const Text('All Operators', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                value: null,
                groupValue: searchProvider.selectedOperator,
                activeColor: AppColors.primary,
                onChanged: (val) => searchProvider.setSelectedOperator(val),
              );
            }
            final op = operators[index - 1];
            return RadioListTile<String?>(
              title: Text(op, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
              value: op,
              groupValue: searchProvider.selectedOperator,
              activeColor: AppColors.primary,
              onChanged: (val) => searchProvider.setSelectedOperator(val),
            );
          },
        );

      default:
        return const SizedBox();
    }
  }

  Widget _buildRadioSort(SearchProvider provider, SortOption option, String title, IconData icon) {
    return RadioListTile<SortOption>(
      title: Row(
        children: [
          Icon(icon, size: 18, color: provider.currentSort == option ? AppColors.primary : AppColors.textSecondary),
          const SizedBox(width: 8),
          Text(title, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
        ],
      ),
      value: option,
      groupValue: provider.currentSort,
      activeColor: AppColors.primary,
      onChanged: (val) {
        if (val != null) provider.setSortOption(val);
      },
    );
  }
}
