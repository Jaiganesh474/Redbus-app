import 'package:flutter/material.dart';
import '../../config/theme.dart';
import '../../config/constants.dart';

class CitySearchModal extends StatefulWidget {
  final String title;
  final String initialCity;

  const CitySearchModal({
    super.key,
    required this.title,
    required this.initialCity,
  });

  @override
  State<CitySearchModal> createState() => _CitySearchModalState();
}

class _CitySearchModalState extends State<CitySearchModal> {
  final TextEditingController _searchController = TextEditingController();
  List<String> _filteredCities = [];

  final List<String> _recentSearches = [
    'Bangalore',
    'Hyderabad',
    'Chennai',
    'Goa',
  ];

  @override
  void initState() {
    super.initState();
    _filteredCities = List.from(AppConstants.popularCities);
    _searchController.addListener(_onSearchChanged);
  }

  void _onSearchChanged() {
    final query = _searchController.text.trim().toLowerCase();
    setState(() {
      if (query.isEmpty) {
        _filteredCities = List.from(AppConstants.popularCities);
      } else {
        _filteredCities = AppConstants.popularCities
            .where((city) => city.toLowerCase().contains(query))
            .toList();
      }
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Container(
      height: MediaQuery.of(context).size.height * 0.88,
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
                Text(
                  widget.title,
                  style: const TextStyle(
                    fontFamily: 'Outfit',
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.close_rounded),
                  onPressed: () => Navigator.pop(context),
                ),
              ],
            ),
          ),

          // Search Input
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 6),
            child: TextField(
              controller: _searchController,
              autofocus: true,
              decoration: InputDecoration(
                hintText: 'Search city or station (e.g. Bangalore, Hyderabad)',
                prefixIcon: const Icon(Icons.search_rounded, color: AppColors.primary),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear, size: 18),
                        onPressed: () => _searchController.clear(),
                      )
                    : null,
                filled: true,
                fillColor: isDark ? AppColors.darkCard : const Color(0xFFF3F4F6),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
          ),

          // Recent Searches Chips
          if (_searchController.text.isEmpty) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
              child: Row(
                children: [
                  const Icon(Icons.history_rounded, size: 18, color: AppColors.textSecondary),
                  const SizedBox(width: 8),
                  Text(
                    'RECENT SEARCHES',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary,
                      letterSpacing: 0.5,
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Wrap(
                spacing: 8,
                children: _recentSearches.map((city) {
                  return ActionChip(
                    label: Text(city),
                    backgroundColor: isDark ? AppColors.darkCard : const Color(0xFFF9FAFB),
                    side: BorderSide(
                      color: isDark ? const Color(0xFF374151) : AppColors.border,
                    ),
                    labelStyle: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                      color: isDark ? Colors.white : AppColors.textPrimary,
                    ),
                    onPressed: () => Navigator.pop(context, city),
                  );
                }).toList(),
              ),
            ),
            const SizedBox(height: 8),
            const Divider(height: 1),
          ],

          // City List
          Expanded(
            child: _filteredCities.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.location_off_outlined, size: 48, color: AppColors.textMuted),
                        const SizedBox(height: 12),
                        Text(
                          'No cities found for "${_searchController.text}"',
                          style: const TextStyle(color: AppColors.textSecondary),
                        ),
                      ],
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    itemCount: _filteredCities.length,
                    separatorBuilder: (_, __) => const Divider(height: 1, indent: 64),
                    itemBuilder: (context, index) {
                      final city = _filteredCities[index];
                      final isSelected = city.toLowerCase() == widget.initialCity.toLowerCase();

                      return ListTile(
                        leading: Container(
                          width: 38,
                          height: 38,
                          decoration: BoxDecoration(
                            color: isSelected
                                ? AppColors.primaryLight
                                : (isDark ? AppColors.darkCard : const Color(0xFFF3F4F6)),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Icon(
                            Icons.location_city_rounded,
                            size: 20,
                            color: isSelected ? AppColors.primary : AppColors.textSecondary,
                          ),
                        ),
                        title: Text(
                          city,
                          style: TextStyle(
                            fontWeight: isSelected ? FontWeight.bold : FontWeight.w600,
                            color: isSelected ? AppColors.primary : null,
                            fontSize: 15,
                          ),
                        ),
                        subtitle: const Text(
                          'Major Bus Hub',
                          style: TextStyle(fontSize: 12, color: AppColors.textMuted),
                        ),
                        trailing: isSelected
                            ? const Icon(Icons.check_circle_rounded, color: AppColors.primary, size: 20)
                            : const Icon(Icons.arrow_forward_ios_rounded, size: 14, color: AppColors.textMuted),
                        onTap: () => Navigator.pop(context, city),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
