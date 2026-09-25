import '../models/chat_model.dart';
import '../models/bus_model.dart';
import 'mock_data_service.dart';

class AiChatService {
  Future<ChatMessage> processUserQuery(String query) async {
    // Simulate AI processing delay
    await Future.delayed(const Duration(milliseconds: 700));

    final lower = query.toLowerCase();

    if (lower.contains('bangalore') || lower.contains('hyderabad') || lower.contains('chennai') || lower.contains('bus') || lower.contains('sleeper') || lower.contains('price')) {
      final buses = MockDataService.getBusesForRoute('Bangalore', 'Hyderabad', '2026-09-22');
      
      List<Bus> filtered = buses;
      String reply = "I found great options for your trip! Here are top rated buses with live tracking and flexible cancellation:";

      if (lower.contains('cheapest') || lower.contains('cheap')) {
        filtered = List.from(buses)..sort((a, b) => a.basePrice.compareTo(b.basePrice));
        reply = "Here are the most affordable buses available, starting at ₹${filtered.first.basePrice.toInt()}:";
      } else if (lower.contains('primo') || lower.contains('best') || lower.contains('rating')) {
        filtered = buses.where((b) => b.isPrimo || b.rating >= 4.5).toList();
        reply = "Here are our highest-rated Primo buses with guaranteed on-time departure & sanitized berths:";
      }

      return ChatMessage(
        id: 'msg_${DateTime.now().millisecondsSinceEpoch}',
        text: reply,
        role: MessageRole.assistant,
        timestamp: DateTime.now(),
        recommendedBuses: filtered.take(3).toList(),
        quickSuggestions: [
          'Filter by Sleeper',
          'Show Primo Only',
          'Departure after 9 PM',
        ],
      );
    } else if (lower.contains('offer') || lower.contains('discount') || lower.contains('coupon')) {
      return ChatMessage(
        id: 'msg_${DateTime.now().millisecondsSinceEpoch}',
        text: "You have 3 active coupons available right now! 🎉\n\n• **FIRST50** - 15% OFF up to ₹250\n• **SUPERBUS** - Flat ₹150 OFF on Primo\n• **REDBUS100** - Flat ₹100 OFF on ₹500+",
        role: MessageRole.assistant,
        timestamp: DateTime.now(),
        quickSuggestions: ['Apply FIRST50', 'Search Bangalore to Goa'],
      );
    } else if (lower.contains('refund') || lower.contains('cancel')) {
      return ChatMessage(
        id: 'msg_${DateTime.now().millisecondsSinceEpoch}',
        text: "You get instant refunds to your original payment mode or RedBus Wallet when cancelling qualifying bookings! You can cancel any booking under **My Bookings** tab with a single tap.",
        role: MessageRole.assistant,
        timestamp: DateTime.now(),
        quickSuggestions: ['Go to My Bookings', 'Check Cancellation Policy'],
      );
    }

    return ChatMessage(
      id: 'msg_${DateTime.now().millisecondsSinceEpoch}',
      text: "Hello! I am your RedBus AI travel companion 🚌. I can help you find buses, compare prices, check seat availability, find promo codes, or track your live bus.",
      role: MessageRole.assistant,
      timestamp: DateTime.now(),
      quickSuggestions: [
        'Buses from Bangalore to Hyderabad',
        'Top rated Sleeper buses',
        'Available promo discounts',
      ],
    );
  }
}
