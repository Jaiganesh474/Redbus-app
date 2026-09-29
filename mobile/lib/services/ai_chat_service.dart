import '../models/chat_model.dart';
import '../models/bus_model.dart';
import 'api_service.dart';
import 'mock_data_service.dart';

class AiChatService {
  final ApiService _apiService = ApiService();

  Future<ChatMessage> processUserQuery(String query, [List<ChatMessage> history = const []]) async {
    try {
      final historyPayload = history.map((m) => {
        'role': m.isUser ? 'user' : 'assistant',
        'content': m.text,
      }).toList();

      final response = await _apiService.sendAiChat(query, historyPayload);
      final replyText = response['reply'] ?? response['message'] ?? response['content'];
      if (replyText != null && replyText.toString().isNotEmpty) {
        final List<String> suggestions = List<String>.from(response['suggestions'] ?? [
          'Filter by Sleeper',
          'Show Primo Only',
          'Available promo discounts',
        ]);

        List<Bus>? recommended;
        if (response['buses'] != null) {
          recommended = (response['buses'] as List).map((b) => Bus.fromJson(b)).toList();
        }

        return ChatMessage(
          id: 'ai_${DateTime.now().millisecondsSinceEpoch}',
          text: replyText.toString(),
          role: MessageRole.assistant,
          timestamp: DateTime.now(),
          recommendedBuses: recommended,
          quickSuggestions: suggestions,
        );
      }
    } catch (_) {}

    final lower = query.toLowerCase();
    if (lower.contains('bangalore') || lower.contains('hyderabad') || lower.contains('chennai') || lower.contains('bus') || lower.contains('sleeper') || lower.contains('price')) {
      final buses = MockDataService.getBusesForRoute('Bangalore', 'Hyderabad', DateTime.now().toString().split(' ').first);
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
        text: "You have active coupons available right now! 🎉\n\n• **FIRST50** - 15% OFF up to ₹250\n• **SUPERBUS** - Flat ₹150 OFF on Primo\n• **REDBUS100** - Flat ₹100 OFF on ₹500+",
        role: MessageRole.assistant,
        timestamp: DateTime.now(),
        quickSuggestions: ['Apply FIRST50', 'Search Bangalore to Goa'],
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
