import 'package:flutter/material.dart';
import '../models/chat_model.dart';
import '../services/ai_chat_service.dart';

class ChatProvider with ChangeNotifier {
  final AiChatService _aiService = AiChatService();

  final List<ChatMessage> _messages = [];
  bool _isTyping = false;

  List<ChatMessage> get messages => _messages;
  bool get isTyping => _isTyping;

  ChatProvider() {
    _initWelcomeMessage();
  }

  void _initWelcomeMessage() {
    _messages.add(ChatMessage(
      id: 'welcome_1',
      text: 'Hi there! 👋 I am your RedBus AI Assistant. How can I help you plan your travel today?',
      role: MessageRole.assistant,
      timestamp: DateTime.now(),
      quickSuggestions: [
        'Buses from Bangalore to Hyderabad',
        'Show Primo verified buses',
        'Available promo discounts',
      ],
    ));
  }

  Future<void> sendMessage(String text) async {
    if (text.trim().isEmpty) return;

    final userMsg = ChatMessage(
      id: 'usr_${DateTime.now().millisecondsSinceEpoch}',
      text: text,
      role: MessageRole.user,
      timestamp: DateTime.now(),
    );

    _messages.add(userMsg);
    _isTyping = true;
    notifyListeners();

    try {
      final reply = await _aiService.processUserQuery(text);
      _messages.add(reply);
    } catch (e) {
      _messages.add(ChatMessage(
        id: 'err_${DateTime.now().millisecondsSinceEpoch}',
        text: 'Sorry, I encountered an issue. Please try again.',
        role: MessageRole.assistant,
        timestamp: DateTime.now(),
      ));
    } finally {
      _isTyping = false;
      notifyListeners();
    }
  }

  void clearChat() {
    _messages.clear();
    _initWelcomeMessage();
    notifyListeners();
  }
}
