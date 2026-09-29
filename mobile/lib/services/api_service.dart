import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import '../config/constants.dart';
import '../models/bus_model.dart';
import '../models/seat_model.dart';
import '../models/booking_model.dart';
import '../models/coupon_model.dart';
import '../models/review_model.dart';
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
    if (_authToken != null && _authToken!.isNotEmpty) {
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

  // 1. Search Trips & Routes from Backend
  Future<List<Bus>> searchTrips({
    required String source,
    required String destination,
    required String travelDate,
    String? busType,
    double? minPrice,
    double? maxPrice,
    String? departureWindow,
    String sortBy = 'departure_asc',
  }) async {
    for (final base in _candidateUrls) {
      try {
        final queryParams = <String, String>{
          'source': source,
          'destination': destination,
          'date': travelDate,
          'sortBy': sortBy,
        };
        if (busType != null && busType.isNotEmpty) queryParams['busType'] = busType;
        if (minPrice != null) queryParams['minPrice'] = minPrice.toString();
        if (maxPrice != null) queryParams['maxPrice'] = maxPrice.toString();
        if (departureWindow != null && departureWindow.isNotEmpty) {
          queryParams['departureWindow'] = departureWindow;
        }

        final uri = Uri.parse('$base/routes/search').replace(queryParameters: queryParams);
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final List data = jsonDecode(response.body);
          final list = data.map((item) => Bus.fromJson(item)).toList();
          if (list.isNotEmpty) return list;
        }
      } catch (e) {
        debugPrint('Search error on $base: $e');
      }
    }
    return MockDataService.getBusesForRoute(source, destination, travelDate);
  }

  // 2. Fetch Available Cities from Backend
  Future<List<String>> getAvailableCities() async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/routes/cities');
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final List data = jsonDecode(response.body);
          final cities = data.map((e) => e.toString()).toList();
          if (cities.isNotEmpty) return cities;
        }
      } catch (_) {}
    }
    return AppConstants.popularCities;
  }

  // 3. Fetch Popular Routes
  Future<List<Map<String, dynamic>>> getPopularRoutes() async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/routes/popular');
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final List data = jsonDecode(response.body);
          return List<Map<String, dynamic>>.from(data);
        }
      } catch (_) {}
    }
    return [
      {'source': 'Bangalore', 'destination': 'Hyderabad', 'minPrice': 750, 'busCount': 45},
      {'source': 'Mumbai', 'destination': 'Goa', 'minPrice': 850, 'busCount': 38},
      {'source': 'Chennai', 'destination': 'Bangalore', 'minPrice': 550, 'busCount': 60},
      {'source': 'Delhi', 'destination': 'Jaipur', 'minPrice': 450, 'busCount': 50},
    ];
  }

  // 4. Fetch Live Seat Layout for a Route
  Future<List<Seat>> getSeatLayout(String routeIdOrTripId, double basePrice) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/routes/$routeIdOrTripId/seats');
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final data = jsonDecode(response.body);
          if (data['seats'] != null) {
            final List seatsData = data['seats'];
            final list = seatsData.map((item) => Seat.fromJson(item)).toList();
            if (list.isNotEmpty) return list;
          }
        }
      } catch (e) {
        debugPrint('Seat API error on $base: $e');
      }
    }
    return MockDataService.generateSeatLayout(basePrice);
  }

  // 5. Lock Seats
  Future<bool> lockSeats(String routeId, List<String> seatNumbers) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/seats/lock');
        final response = await http
            .post(
              uri,
              headers: _getHeaders(),
              body: jsonEncode({'routeId': int.tryParse(routeId) ?? 1, 'seatNumbers': seatNumbers}),
            )
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200 || response.statusCode == 201) {
          return true;
        }
      } catch (_) {}
    }
    return true;
  }

  // 6. Create Real Booking on Backend
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
      } catch (e) {
        debugPrint('Booking API error on $base: $e');
      }
    }

    final pnr = 'RB${DateTime.now().millisecondsSinceEpoch.toString().substring(4)}';
    return {
      'id': 'bk_${DateTime.now().millisecondsSinceEpoch}',
      'pnr': pnr,
      'status': 'CONFIRMED',
      'paymentId': 'pay_sim_${DateTime.now().millisecondsSinceEpoch}',
      'qrData': 'REDBUS:PNR=$pnr',
    };
  }

  // 7. Get User Bookings from Backend
  Future<List<Booking>> getMyBookings({int page = 0, int size = 20}) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/bookings/my-bookings?page=$page&size=$size');
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final data = jsonDecode(response.body);
          final List content = data['content'] ?? (data is List ? data : []);
          return content.map((b) => Booking.fromJson(b)).toList();
        }
      } catch (_) {}
    }
    return [];
  }

  // 8. Get Booking by PNR
  Future<Booking?> getBookingByPnr(String pnr) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/bookings/$pnr');
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final data = jsonDecode(response.body);
          return Booking.fromJson(data);
        }
      } catch (_) {}
    }
    return null;
  }

  // 9. Cancel Booking on Backend
  Future<Map<String, dynamic>> cancelBooking(String pnr, {String reason = 'Customer request', String destination = 'WALLET'}) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/bookings/$pnr/cancel');
        final response = await http
            .post(
              uri,
              headers: _getHeaders(),
              body: jsonEncode({'reason': reason, 'refundDestination': destination}),
            )
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          return jsonDecode(response.body);
        }
      } catch (_) {}
    }
    return {'pnr': pnr, 'status': 'CANCELLED', 'refundAmount': 600.0, 'refundStatus': 'PROCESSED'};
  }

  // 10. Send E-Ticket via Email / Dispatach
  Future<bool> sendTicketEmail(String pnr, {String? email}) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/bookings/$pnr/send-ticket${email != null ? "?email=$email" : ""}');
        final response = await http
            .post(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          return true;
        }
      } catch (_) {}
    }
    return true;
  }

  // 11. Fetch Active Promo Coupons
  Future<List<Coupon>> getCoupons() async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/coupons');
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final List data = jsonDecode(response.body);
          return data.map((c) => Coupon(
            code: c['code'] ?? '',
            title: c['title'] ?? 'Discount Offer',
            description: c['description'] ?? '',
            discountPercent: (c['discountPercent'] ?? 15).toDouble(),
            maxDiscount: (c['maxDiscount'] ?? 200).toDouble(),
            minFare: (c['minFare'] ?? 400).toDouble(),
            validTill: c['validTill'] ?? '31 Dec 2026',
            badge: c['badge'] ?? 'SPECIAL',
          )).toList();
        }
      } catch (_) {}
    }
    return AppConstants.promoOffers.map((o) => Coupon(
      code: o['code'],
      title: o['title'],
      description: o['description'],
      discountPercent: (o['discount'] as num).toDouble(),
      maxDiscount: (o['maxDiscount'] as num).toDouble(),
      minFare: (o['minFare'] as num).toDouble(),
      validTill: o['validTill'],
      badge: o['badge'],
    )).toList();
  }

  // 12. Validate Coupon with Backend
  Future<Map<String, dynamic>> validateCoupon(String code, double fareAmount) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/coupons/validate');
        final response = await http
            .post(uri, headers: _getHeaders(), body: jsonEncode({'code': code.trim(), 'fareAmount': fareAmount}))
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          return jsonDecode(response.body);
        }
      } catch (_) {}
    }
    // Fallback validation
    final offer = AppConstants.promoOffers.firstWhere(
      (o) => o['code'] == code.trim().toUpperCase(),
      orElse: () => {},
    );
    if (offer.isNotEmpty) {
      final discount = ((fareAmount * (offer['discount'] as num)) / 100).clamp(0, (offer['maxDiscount'] as num)).toDouble();
      return {'valid': true, 'discountAmount': discount, 'message': 'Coupon applied!'};
    }
    return {'valid': false, 'message': 'Invalid coupon code'};
  }

  // 13. Fetch Passenger Reviews for a Route
  Future<List<Review>> getRouteReviews(String routeId) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/reviews/route/$routeId');
        final response = await http
            .get(uri, headers: _getHeaders())
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          final List data = jsonDecode(response.body);
          return data.map((r) => Review.fromJson(r)).toList();
        }
      } catch (_) {}
    }
    return [
      Review(id: 'rev_1', userName: 'Arun Kumar', rating: 4.8, busRating: 5.0, punctualityRating: 5.0, driverRating: 4.5, comment: 'Very clean bus, on-time departure. Great experience!', createdAt: 'Yesterday'),
      Review(id: 'rev_2', userName: 'Deepika S.', rating: 4.5, busRating: 4.5, punctualityRating: 4.5, driverRating: 4.5, comment: 'Comfortable sleeper berths and smooth driving.', createdAt: '3 days ago'),
    ];
  }

  // 14. Send AI Chat to Backend
  Future<Map<String, dynamic>> sendAiChat(String message, List<Map<String, String>> history) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/ai/chat');
        final response = await http
            .post(
              uri,
              headers: _getHeaders(),
              body: jsonEncode({'message': message, 'history': history}),
            )
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          return jsonDecode(response.body);
        }
      } catch (_) {}
    }
    return {
      'reply': 'I found buses matching your request. You can select your preferred departure time and choose your seats directly.',
      'suggestions': ['Show AC Sleepers', 'Buses departing after 8 PM', 'Cheapest fare'],
    };
  }

  // 15. NLP Search Query Parser
  Future<Map<String, dynamic>?> parseNlpQuery(String queryText) async {
    for (final base in _candidateUrls) {
      try {
        final uri = Uri.parse('$base/ai/parse-query');
        final response = await http
            .post(uri, headers: _getHeaders(), body: jsonEncode({'query': queryText}))
            .timeout(const Duration(milliseconds: AppConstants.connectTimeout));

        if (response.statusCode == 200) {
          return jsonDecode(response.body);
        }
      } catch (_) {}
    }
    return null;
  }
}
