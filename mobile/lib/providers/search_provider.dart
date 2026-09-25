import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/bus_model.dart';
import '../services/api_service.dart';

enum SortOption {
  cheapest,
  fastest,
  highestRated,
  earlyDeparture,
  lateDeparture,
}

class SearchProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  String _sourceCity = 'Bangalore';
  String _destinationCity = 'Hyderabad';
  DateTime _travelDate = DateTime.now().add(const Duration(days: 1));

  List<Bus> _allBuses = [];
  List<Bus> _filteredBuses = [];
  bool _isLoading = false;
  String? _error;

  // Filters
  bool _filterPrimoOnly = false;
  bool _filterAcOnly = false;
  bool _filterSleeperOnly = false;
  bool _filterSingleSeats = false;
  RangeValues _priceRange = const RangeValues(300, 2500);
  String? _selectedOperator;
  SortOption _currentSort = SortOption.highestRated;

  // Getters
  String get sourceCity => _sourceCity;
  String get destinationCity => _destinationCity;
  DateTime get travelDate => _travelDate;
  String get formattedTravelDate => DateFormat('EEE, d MMM').format(_travelDate);
  String get apiDateString => DateFormat('yyyy-MM-dd').format(_travelDate);
  List<Bus> get buses => _filteredBuses;
  int get totalBusesCount => _filteredBuses.length;
  bool get isLoading => _isLoading;
  String? get error => _error;

  bool get filterPrimoOnly => _filterPrimoOnly;
  bool get filterAcOnly => _filterAcOnly;
  bool get filterSleeperOnly => _filterSleeperOnly;
  bool get filterSingleSeats => _filterSingleSeats;
  RangeValues get priceRange => _priceRange;
  String? get selectedOperator => _selectedOperator;
  SortOption get currentSort => _currentSort;

  List<String> get uniqueOperators {
    return _allBuses.map((b) => b.operatorName).toSet().toList();
  }

  void setSourceCity(String city) {
    _sourceCity = city;
    notifyListeners();
  }

  void setDestinationCity(String city) {
    _destinationCity = city;
    notifyListeners();
  }

  void swapCities() {
    final temp = _sourceCity;
    _sourceCity = _destinationCity;
    _destinationCity = temp;
    notifyListeners();
  }

  void setTravelDate(DateTime date) {
    _travelDate = date;
    notifyListeners();
  }

  void setSortOption(SortOption option) {
    _currentSort = option;
    _applyFiltersAndSort();
  }

  void togglePrimoFilter() {
    _filterPrimoOnly = !_filterPrimoOnly;
    _applyFiltersAndSort();
  }

  void toggleAcFilter() {
    _filterAcOnly = !_filterAcOnly;
    _applyFiltersAndSort();
  }

  void toggleSleeperFilter() {
    _filterSleeperOnly = !_filterSleeperOnly;
    _applyFiltersAndSort();
  }

  void setPriceRange(RangeValues values) {
    _priceRange = values;
    _applyFiltersAndSort();
  }

  void setSelectedOperator(String? op) {
    _selectedOperator = op;
    _applyFiltersAndSort();
  }

  void resetFilters() {
    _filterPrimoOnly = false;
    _filterAcOnly = false;
    _filterSleeperOnly = false;
    _filterSingleSeats = false;
    _priceRange = const RangeValues(300, 2500);
    _selectedOperator = null;
    _currentSort = SortOption.highestRated;
    _applyFiltersAndSort();
  }

  Future<void> searchBuses() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _allBuses = await _apiService.searchTrips(
        source: _sourceCity,
        destination: _destinationCity,
        travelDate: apiDateString,
      );
      _applyFiltersAndSort();
    } catch (e) {
      _error = 'Failed to load buses. Please try again.';
      debugPrint('Search error: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void _applyFiltersAndSort() {
    List<Bus> list = List.from(_allBuses);

    if (_filterPrimoOnly) {
      list = list.where((b) => b.isPrimo).toList();
    }
    if (_filterAcOnly) {
      list = list.where((b) => b.busType.toLowerCase().contains('ac') && !b.busType.toLowerCase().contains('non-ac')).toList();
    }
    if (_filterSleeperOnly) {
      list = list.where((b) => b.busType.toLowerCase().contains('sleeper')).toList();
    }
    if (_selectedOperator != null && _selectedOperator!.isNotEmpty) {
      list = list.where((b) => b.operatorName == _selectedOperator).toList();
    }

    list = list.where((b) => b.basePrice >= _priceRange.start && b.basePrice <= _priceRange.end).toList();

    // Sort
    switch (_currentSort) {
      case SortOption.cheapest:
        list.sort((a, b) => a.basePrice.compareTo(b.basePrice));
        break;
      case SortOption.fastest:
        list.sort((a, b) => a.duration.compareTo(b.duration));
        break;
      case SortOption.highestRated:
        list.sort((a, b) => b.rating.compareTo(a.rating));
        break;
      case SortOption.earlyDeparture:
        list.sort((a, b) => a.departureTime.compareTo(b.departureTime));
        break;
      case SortOption.lateDeparture:
        list.sort((a, b) => b.departureTime.compareTo(a.departureTime));
        break;
    }

    _filteredBuses = list;
    notifyListeners();
  }
}
