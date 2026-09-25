import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../config/constants.dart';
import '../../providers/auth_provider.dart';
import '../../providers/search_provider.dart';
import '../../widgets/custom_button.dart';
import '../search/city_search_modal.dart';
import '../search/search_results_screen.dart';

class HomeScreen extends StatefulWidget {
  final Function(int)? onNavigateTab;

  const HomeScreen({super.key, this.onNavigateTab});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  void _openCitySelector(BuildContext context, bool isSource) async {
    final searchProvider = Provider.of<SearchProvider>(context, listen: false);
    final currentCity = isSource ? searchProvider.sourceCity : searchProvider.destinationCity;

    final selectedCity = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => CitySearchModal(
        title: isSource ? 'Select Departure City' : 'Select Destination City',
        initialCity: currentCity,
      ),
    );

    if (selectedCity != null) {
      if (isSource) {
        searchProvider.setSourceCity(selectedCity);
      } else {
        searchProvider.setDestinationCity(selectedCity);
      }
    }
  }

  void _selectDate(BuildContext context) async {
    final searchProvider = Provider.of<SearchProvider>(context, listen: false);
    final now = DateTime.now();
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: searchProvider.travelDate.isBefore(now) ? now : searchProvider.travelDate,
      firstDate: now,
      lastDate: now.add(const Duration(days: 90)),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: AppColors.primary,
              onPrimary: Colors.white,
              onSurface: AppColors.textPrimary,
            ),
          ),
          child: child!,
        );
      },
    );

    if (pickedDate != null) {
      searchProvider.setTravelDate(pickedDate);
    }
  }

  void _onSearchPressed() {
    final searchProvider = Provider.of<SearchProvider>(context, listen: false);
    searchProvider.searchBuses();
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const SearchResultsScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final searchProvider = Provider.of<SearchProvider>(context);
    final authProvider = Provider.of<AuthProvider>(context);
    final userName = authProvider.user?.name.split(' ').first ?? 'Traveler';

    return Scaffold(
      body: SingleChildScrollView(
        child: Column(
          children: [
            // Top Header & Hero Gradient
            Stack(
              clipBehavior: Clip.none,
              children: [
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.fromLTRB(20, 52, 20, 90),
                  decoration: const BoxDecoration(
                    gradient: AppColors.heroGradient,
                    borderRadius: BorderRadius.vertical(bottom: Radius.circular(32)),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Greeting & AI Quick Button
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Hi, $userName 👋',
                                style: const TextStyle(
                                  fontFamily: 'Outfit',
                                  fontSize: 22,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Where would you like to travel?',
                                style: TextStyle(
                                  fontSize: 13,
                                  color: Colors.white.withOpacity(0.85),
                                ),
                              ),
                            ],
                          ),
                          InkWell(
                            onTap: () {
                              if (widget.onNavigateTab != null) {
                                widget.onNavigateTab!(2); // Navigate to AI Tab
                              }
                            },
                            borderRadius: BorderRadius.circular(20),
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.2),
                                borderRadius: BorderRadius.circular(20),
                                border: Border.all(color: Colors.white.withOpacity(0.3)),
                              ),
                              child: const Row(
                                children: [
                                  Icon(Icons.auto_awesome_rounded, size: 16, color: Colors.amberAccent),
                                  SizedBox(width: 6),
                                  Text(
                                    'redBus AI',
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontWeight: FontWeight.bold,
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),

                // Floating Hero Search Card
                Positioned(
                  top: 120,
                  left: 16,
                  right: 16,
                  child: Container(
                    padding: const EdgeInsets.all(18),
                    decoration: BoxDecoration(
                      color: isDark ? AppColors.darkCard : Colors.white,
                      borderRadius: BorderRadius.circular(24),
                      border: Border.all(
                        color: isDark ? const Color(0xFF374151) : AppColors.border,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.12),
                          blurRadius: 20,
                          offset: const Offset(0, 8),
                        ),
                      ],
                    ),
                    child: Column(
                      children: [
                        // FROM & TO Rows with Swap Button
                        Stack(
                          alignment: Alignment.centerRight,
                          children: [
                            Column(
                              children: [
                                // FROM Field
                                InkWell(
                                  onTap: () => _openCitySelector(context, true),
                                  borderRadius: BorderRadius.circular(12),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                                    decoration: BoxDecoration(
                                      color: isDark ? AppColors.darkSurface : const Color(0xFFF9FAFB),
                                      borderRadius: BorderRadius.circular(12),
                                      border: Border.all(
                                        color: isDark ? const Color(0xFF374151) : AppColors.border,
                                      ),
                                    ),
                                    child: Row(
                                      children: [
                                        const Icon(Icons.trip_origin_rounded, color: AppColors.primary, size: 20),
                                        const SizedBox(width: 12),
                                        Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            const Text(
                                              'FROM',
                                              style: TextStyle(
                                                fontSize: 10,
                                                fontWeight: FontWeight.w700,
                                                color: AppColors.textMuted,
                                                letterSpacing: 0.5,
                                              ),
                                            ),
                                            Text(
                                              searchProvider.sourceCity,
                                              style: const TextStyle(
                                                fontSize: 16,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ),

                                const SizedBox(height: 10),

                                // TO Field
                                InkWell(
                                  onTap: () => _openCitySelector(context, false),
                                  borderRadius: BorderRadius.circular(12),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                                    decoration: BoxDecoration(
                                      color: isDark ? AppColors.darkSurface : const Color(0xFFF9FAFB),
                                      borderRadius: BorderRadius.circular(12),
                                      border: Border.all(
                                        color: isDark ? const Color(0xFF374151) : AppColors.border,
                                      ),
                                    ),
                                    child: Row(
                                      children: [
                                        const Icon(Icons.location_on_rounded, color: AppColors.success, size: 20),
                                        const SizedBox(width: 12),
                                        Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            const Text(
                                              'TO',
                                              style: TextStyle(
                                                fontSize: 10,
                                                fontWeight: FontWeight.w700,
                                                color: AppColors.textMuted,
                                                letterSpacing: 0.5,
                                              ),
                                            ),
                                            Text(
                                              searchProvider.destinationCity,
                                              style: const TextStyle(
                                                fontSize: 16,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ],
                            ),

                            // Swap Button in between
                            Positioned(
                              right: 18,
                              child: Material(
                                color: Colors.transparent,
                                child: InkWell(
                                  onTap: () => searchProvider.swapCities(),
                                  borderRadius: BorderRadius.circular(20),
                                  child: Container(
                                    padding: const EdgeInsets.all(8),
                                    decoration: BoxDecoration(
                                      color: AppColors.primary,
                                      shape: BoxShape.circle,
                                      boxShadow: [
                                        BoxShadow(
                                          color: AppColors.primary.withOpacity(0.4),
                                          blurRadius: 8,
                                          offset: const Offset(0, 2),
                                        ),
                                      ],
                                    ),
                                    child: const Icon(
                                      Icons.swap_vert_rounded,
                                      color: Colors.white,
                                      size: 20,
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),

                        const SizedBox(height: 12),

                        // Date Selector & Quick Chips
                        InkWell(
                          onTap: () => _selectDate(context),
                          borderRadius: BorderRadius.circular(12),
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                            decoration: BoxDecoration(
                              color: isDark ? AppColors.darkSurface : const Color(0xFFF9FAFB),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                color: isDark ? const Color(0xFF374151) : AppColors.border,
                              ),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.calendar_month_rounded, color: AppColors.info, size: 20),
                                const SizedBox(width: 12),
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Text(
                                      'DATE OF JOURNEY',
                                      style: TextStyle(
                                        fontSize: 10,
                                        fontWeight: FontWeight.w700,
                                        color: AppColors.textMuted,
                                        letterSpacing: 0.5,
                                      ),
                                    ),
                                    Text(
                                      searchProvider.formattedTravelDate,
                                      style: const TextStyle(
                                        fontSize: 15,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                                const Spacer(),
                                // Today & Tomorrow quick chips
                                Row(
                                  children: [
                                    _QuickDateChip(
                                      label: 'Today',
                                      isSelected: DateUtils.isSameDay(searchProvider.travelDate, DateTime.now()),
                                      onTap: () => searchProvider.setTravelDate(DateTime.now()),
                                    ),
                                    const SizedBox(width: 6),
                                    _QuickDateChip(
                                      label: 'Tomorrow',
                                      isSelected: DateUtils.isSameDay(searchProvider.travelDate, DateTime.now().add(const Duration(days: 1))),
                                      onTap: () => searchProvider.setTravelDate(DateTime.now().add(const Duration(days: 1))),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ),

                        const SizedBox(height: 18),

                        // Search Button
                        CustomButton(
                          text: 'SEARCH BUSES',
                          icon: Icons.search_rounded,
                          onPressed: _onSearchPressed,
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),

            // Spacer for floating card
            const SizedBox(height: 240),

            // Primo Assurance Banner
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF1E293B), Color(0xFF0F172A)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(18),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.15),
                      blurRadius: 10,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: Colors.amber.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Icon(Icons.verified_rounded, color: Colors.amber, size: 28),
                    ),
                    const SizedBox(width: 14),
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'redBus Primo Services',
                            style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 15,
                            ),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'On-Time Guarantee • Top Rated Drivers • Sanitized Fleet',
                            style: TextStyle(
                              color: Colors.white70,
                              fontSize: 11,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 24),

            // Offers & Promos Section
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Exclusive Offers',
                    style: TextStyle(
                      fontFamily: 'Outfit',
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  TextButton(
                    onPressed: () {
                      if (widget.onNavigateTab != null) widget.onNavigateTab!(3);
                    },
                    child: const Text('View All', style: TextStyle(color: AppColors.primary)),
                  ),
                ],
              ),
            ),

            // Offers horizontal carousel
            SizedBox(
              height: 145,
              child: ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                scrollDirection: Axis.horizontal,
                itemCount: AppConstants.promoOffers.length,
                itemBuilder: (context, index) {
                  final offer = AppConstants.promoOffers[index];
                  return Container(
                    width: 280,
                    margin: const EdgeInsets.symmetric(horizontal: 6),
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      gradient: index % 2 == 0 ? AppColors.promoCardGradient : const LinearGradient(
                        colors: [Color(0xFF831843), Color(0xFF500724)],
                      ),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.2),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: Text(
                                offer['badge'],
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 9,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                            const Spacer(),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text(
                                offer['code'],
                                style: const TextStyle(
                                  color: AppColors.primaryDark,
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        Text(
                          offer['title'],
                          style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          offer['description'],
                          style: const TextStyle(
                            color: Colors.white70,
                            fontSize: 11,
                          ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),

            const SizedBox(height: 24),

            // Trending Routes
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Align(
                alignment: Alignment.centerLeft,
                child: const Text(
                  'Popular Bus Routes',
                  style: TextStyle(
                    fontFamily: 'Outfit',
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),

            const SizedBox(height: 12),

            // Routes Grid
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Column(
                children: [
                  _TrendingRouteCard(
                    from: 'Bangalore',
                    to: 'Hyderabad',
                    fare: '₹850',
                    busesCount: '45+ Buses',
                    onTap: () {
                      searchProvider.setSourceCity('Bangalore');
                      searchProvider.setDestinationCity('Hyderabad');
                      _onSearchPressed();
                    },
                  ),
                  const SizedBox(height: 8),
                  _TrendingRouteCard(
                    from: 'Mumbai',
                    to: 'Goa',
                    fare: '₹950',
                    busesCount: '38+ Buses',
                    onTap: () {
                      searchProvider.setSourceCity('Mumbai');
                      searchProvider.setDestinationCity('Goa');
                      _onSearchPressed();
                    },
                  ),
                  const SizedBox(height: 8),
                  _TrendingRouteCard(
                    from: 'Chennai',
                    to: 'Bangalore',
                    fare: '₹600',
                    busesCount: '60+ Buses',
                    onTap: () {
                      searchProvider.setSourceCity('Chennai');
                      searchProvider.setDestinationCity('Bangalore');
                      _onSearchPressed();
                    },
                  ),
                ],
              ),
            ),

            const SizedBox(height: 24),

            // Live Platform Stats
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 16),
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: isDark ? AppColors.darkCard : const Color(0xFFF3F4F6),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Column(
                children: [
                  const Text(
                    'INDIA\'S NO. 1 BUS PLATFORM',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                      color: AppColors.primary,
                      letterSpacing: 1,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      _StatColumn(number: AppConstants.statBuses, label: 'Buses Available'),
                      Container(height: 30, width: 1, color: isDark ? const Color(0xFF4B5563) : AppColors.border),
                      _StatColumn(number: AppConstants.statRoutes, label: 'Live Routes'),
                      Container(height: 30, width: 1, color: isDark ? const Color(0xFF4B5563) : AppColors.border),
                      _StatColumn(number: AppConstants.statUsers, label: 'Happy Users'),
                    ],
                  ),
                ],
              ),
            ),

            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }
}

class _QuickDateChip extends StatelessWidget {
  final String label;
  final bool isSelected;
  final VoidCallback onTap;

  const _QuickDateChip({
    required this.label,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(6),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: isSelected ? AppColors.primary : Colors.transparent,
          borderRadius: BorderRadius.circular(6),
          border: Border.all(
            color: isSelected ? AppColors.primary : AppColors.border,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w600,
            color: isSelected ? Colors.white : AppColors.textSecondary,
          ),
        ),
      ),
    );
  }
}

class _TrendingRouteCard extends StatelessWidget {
  final String from;
  final String to;
  final String fare;
  final String busesCount;
  final VoidCallback onTap;

  const _TrendingRouteCard({
    required this.from,
    required this.to,
    required this.fare,
    required this.busesCount,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkCard : Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
      ),
      child: ListTile(
        onTap: onTap,
        dense: true,
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: AppColors.primaryLight,
            borderRadius: BorderRadius.circular(8),
          ),
          child: const Icon(Icons.directions_bus_filled, color: AppColors.primary, size: 20),
        ),
        title: Row(
          children: [
            Text(from, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
            const SizedBox(width: 6),
            const Icon(Icons.arrow_forward_rounded, size: 14, color: AppColors.textMuted),
            const SizedBox(width: 6),
            Text(to, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
          ],
        ),
        subtitle: Text(busesCount, style: const TextStyle(fontSize: 11, color: AppColors.textSecondary)),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(fare, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 14, color: AppColors.primary)),
            const Text('onwards', style: TextStyle(fontSize: 10, color: AppColors.textMuted)),
          ],
        ),
      ),
    );
  }
}

class _StatColumn extends StatelessWidget {
  final String number;
  final String label;

  const _StatColumn({required this.number, required this.label});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          number,
          style: const TextStyle(
            fontFamily: 'Outfit',
            fontSize: 18,
            fontWeight: FontWeight.w800,
            color: AppColors.primary,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: const TextStyle(
            fontSize: 11,
            color: AppColors.textSecondary,
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
    );
  }
}
