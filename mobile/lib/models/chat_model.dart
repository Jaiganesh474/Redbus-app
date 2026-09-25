import 'bus_model.dart';

enum MessageRole {
  user,
  assistant,
  system,
}

class ChatMessage {
  final String id;
  final String text;
  final MessageRole role;
  final DateTime timestamp;
  final List<Bus>? recommendedBuses;
  final List<String>? quickSuggestions;

  ChatMessage({
    required this.id,
    required this.text,
    required this.role,
    required this.timestamp,
    this.recommendedBuses,
    this.quickSuggestions,
  });

  bool get isUser => role == MessageRole.user;
}
