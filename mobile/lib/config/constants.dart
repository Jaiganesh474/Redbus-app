class AppConstants {
  static const String appName = 'redBus';
  static const String appTagline = 'India\'s No. 1 Online Bus Ticket Booking Site';
  
  // API Config
  static const String baseUrl = 'http://localhost:8080/api/v1';
  static const int connectTimeout = 10000;
  static const int receiveTimeout = 10000;
  
  // Popular Indian Cities for Bus Travel
  static const List<String> popularCities = [
    'Bangalore',
    'Hyderabad',
    'Chennai',
    'Mumbai',
    'Pune',
    'Delhi',
    'Goa',
    'Coimbatore',
    'Ahmedabad',
    'Jaipur',
    'Kochi',
    'Vijayawada',
    'Visakhapatnam',
    'Mysore',
    'Tirupati',
  ];

  // Bus Types
  static const List<String> busTypes = [
    'AC Sleeper',
    'Non-AC Sleeper',
    'AC Seater',
    'Non-AC Seater',
    'Volvo Multi-Axle',
    'Scania AC',
    'Electric AC',
  ];

  // Amenities
  static const List<Map<String, dynamic>> amenitiesList = [
    {'id': 'wifi', 'name': 'Free Wi-Fi', 'icon': 'wifi'},
    {'id': 'charging', 'name': 'Charging Point', 'icon': 'battery_charging_full'},
    {'id': 'water', 'name': 'Water Bottle', 'icon': 'local_drink'},
    {'id': 'blanket', 'name': 'Blanket & Pillow', 'icon': 'bed'},
    {'id': 'reading_light', 'name': 'Reading Light', 'icon': 'lightbulb'},
    {'id': 'tracking', 'name': 'Live Tracking', 'icon': 'location_on'},
    {'id': 'emergency_exit', 'name': 'Emergency Exit', 'icon': 'security'},
    {'id': 'cctv', 'name': 'CCTV Surveillance', 'icon': 'videocam'},
  ];

  // Promo Offers
  static const List<Map<String, dynamic>> promoOffers = [
    {
      'code': 'FIRST50',
      'title': 'Save up to ₹250 on first booking',
      'description': 'Use code FIRST50 & get 15% instant discount up to ₹250',
      'discount': 15,
      'maxDiscount': 250,
      'minFare': 400,
      'validTill': '31 Dec 2026',
      'badge': 'NEW USER',
    },
    {
      'code': 'SUPERBUS',
      'title': 'Flat ₹150 OFF on Primo Buses',
      'description': 'Enjoy top-rated buses with guaranteed punctuality & hygiene',
      'discount': 20,
      'maxDiscount': 150,
      'minFare': 500,
      'validTill': '30 Nov 2026',
      'badge': 'PRIMO EXCLUSIVE',
    },
    {
      'code': 'FESTIVE100',
      'title': 'Save ₹100 on Weekend Routes',
      'description': 'Special discount for inter-city express buses',
      'discount': 10,
      'maxDiscount': 100,
      'minFare': 350,
      'validTill': '15 Oct 2026',
      'badge': 'LIMITED TIME',
    },
  ];

  // Stats
  static const String statBuses = '10,000+';
  static const String statRoutes = '100,000+';
  static const String statUsers = '36 Million+';
  static const String statRating = '4.6 ★';
}
