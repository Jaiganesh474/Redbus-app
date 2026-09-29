import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import '../config/constants.dart';
import '../models/bus_model.dart';
import '../models/seat_model.dart';
import 'mock_data_service.dart';

class ApiService {
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;
  ApiService._internal();

  String? _authToken;

  void setAuthToken(String? token) {
    _authToken = token;
  }

  Map<String, String> _getHeaders() {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (_authToken != null) {
      headers['Authorization'] = 'Bearer $_authToken';
    }
    return headers;
  }

  List<String> get _candidateUrls => [
    AppConstants.baseUrl,
    AppConstants.localAndroidUrl,
    AppConstants.localDesktopUrl,
    AppConstants.cloudBackendUrl,
  ];

  // Search trips from backend with fallback
  Future<List<Bus>> searchTrips({
    required String source,
    required String destination,
    required String travelDate,
  }) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse(
          '$base/trips/search?source=${Uri.encodeComponent(source)}&destination=${Uri.encodeComponent(destination)}&date=$travelDate',
        );
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final List data = jsonDecode(response.body);
          final list = data.map((item) => Bus.fromJson(item)).toList();
          if (list.isNotEmpty) return list;
        }
      } catch (_) {
        // Try next endpoint candidate
      }
    }
    return MockDataService.getBusesForRoute(source, destination, travelDate);
  }

  // Fetch seat layout for a trip
  Future<List<Seat>> getSeatLayout(String tripInstanceId, double basePrice) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/trips/$tripInstanceId/seats');
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final List data = jsonDecode(response.body);
          final list = data.map((item) => Seat.fromJson(item)).toList();
          if (list.isNotEmpty) return list;
        }
      } catch (_) {
        // Try next endpoint candidate
      }
    }
    return MockDataService.generateSeatLayout(basePrice);
  }

  // Create booking
  Future<Map<String, dynamic>> createBooking(Map<String, dynamic> bookingPayload) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/bookings');
        final response = await http
            .post(uri, headers: _getHeaders(), body: jsonEncode(bookingPayload))
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200 || response.statusCode == 201) {
          return jsonDecode(response.body);
        }
      } catch (_) {
        // Try next endpoint candidate
      }
    }
    
    // Simulate instant success
    final pnr = 'RB${DateTime.now().millisecondsSinceEpoch.toString().substring(4)}';
    return {
      'id': 'bk_${DateTime.now().millisecondsSinceEpoch}',
      'pnr': pnr,
      'status': 'CONFIRMED',
      'paymentId': 'pay_sim_${DateTime.now().millisecondsSinceEpoch}',
      'qrData': 'REDBUS:PNR=$pnr',
    };
  }
}
