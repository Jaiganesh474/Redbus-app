import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../models/chat_model.dart';
import '../../models/bus_model.dart';
import '../../providers/chat_provider.dart';
import '../../providers/booking_provider.dart';
import '../../widgets/bus_card.dart';
import '../bus_details/seat_selection_screen.dart';
import '../bus_details/bus_details_screen.dart';

class RedBusAiScreen extends StatefulWidget {
  const RedBusAiScreen({super.key});

  @override
  State<RedBusAiScreen> createState() => _RedBusAiScreenState();
}

class _RedBusAiScreenState extends State<RedBusAiScreen> {
  final TextEditingController _textController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _onSend(ChatProvider chatProvider) {
    final text = _textController.text.trim();
    if (text.isEmpty) return;
    _textController.clear();
    chatProvider.sendMessage(text);
    _scrollToBottom();
  }

  void _onBusSelected(BuildContext context, Bus bus, bool directToSeats) {
    final bookingProvider = Provider.of<BookingProvider>(context, listen: false);
    bookingProvider.selectBus(bus);

    if (directToSeats) {
      Navigator.of(context).push(
        MaterialPageRoute(builder: (_) => const SeatSelectionScreen()),
      );
    } else {
      Navigator.of(context).push(
        MaterialPageRoute(builder: (_) => BusDetailsScreen(bus: bus)),
      );
    }
  }

  @override
  void dispose() {
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final chatProvider = Provider.of<ChatProvider>(context);
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Row(
          children: [
            Icon(Icons.auto_awesome_rounded, color: Colors.amber, size: 22),
            SizedBox(width: 8),
            Text('redBus AI Companion'),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Clear Chat',
            onPressed: () => chatProvider.clearChat(),
          ),
        ],
      ),
      body: Column(
        children: [
          // Chat Messages List
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              itemCount: chatProvider.messages.length,
              itemBuilder: (context, index) {
                final msg = chatProvider.messages[index];
                return _buildMessageBubble(context, msg, isDark, chatProvider);
              },
            ),
          ),

          // Typing Indicator
          if (chatProvider.isTyping)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 6),
              child: Row(
                children: [
                  const SizedBox(
                    width: 14,
                    height: 14,
                    child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.primary),
                  ),
                  const SizedBox(width: 10),
                  Text(
                    'redBus AI is searching inventory...',
                    style: TextStyle(fontSize: 12, color: isDark ? const Color(0xFF9CA3AF) : AppColors.textSecondary),
                  ),
                ],
              ),
            ),

          // Input Bar
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkCard : Colors.white,
              border: Border(top: BorderSide(color: isDark ? const Color(0xFF374151) : AppColors.border)),
            ),
            child: SafeArea(
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _textController,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _onSend(chatProvider),
                      decoration: InputDecoration(
                        hintText: 'Ask anything (e.g. "Cheapest sleeper to Goa?")',
                        hintStyle: const TextStyle(fontSize: 13),
                        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        filled: true,
                        fillColor: isDark ? AppColors.darkSurface : const Color(0xFFF3F4F6),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: BorderSide.none,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filled(
                    onPressed: () => _onSend(chatProvider),
                    icon: const Icon(Icons.send_rounded, size: 18),
                    style: IconButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(
    BuildContext context,
    ChatMessage msg,
    bool isDark,
    ChatProvider chatProvider,
  ) {
    return Column(
      crossAxisAlignment: msg.isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: msg.isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (!msg.isUser) ...[
              Container(
                margin: const EdgeInsets.only(right: 8, top: 2),
                padding: const EdgeInsets.all(6),
                decoration: const BoxDecoration(
                  gradient: AppColors.primaryGradient,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.auto_awesome, size: 14, color: Colors.white),
              ),
            ],
            Flexible(
              child: Container(
                margin: const EdgeInsets.symmetric(vertical: 4),
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: msg.isUser
                      ? AppColors.primary
                      : (isDark ? AppColors.darkCard : const Color(0xFFF3F4F6)),
                  borderRadius: BorderRadius.only(
                    topLeft: const Radius.circular(16),
                    topRight: const Radius.circular(16),
                    bottomLeft: Radius.circular(msg.isUser ? 16 : 4),
                    bottomRight: Radius.circular(msg.isUser ? 4 : 16),
                  ),
                ),
                child: Text(
                  msg.text,
                  style: TextStyle(
                    fontSize: 14,
                    color: msg.isUser ? Colors.white : (isDark ? Colors.white : AppColors.textPrimary),
                    height: 1.4,
                  ),
                ),
              ),
            ),
          ],
        ),

        // Recommended Buses Cards (if any)
        if (msg.recommendedBuses != null && msg.recommendedBuses!.isNotEmpty) ...[
          const SizedBox(height: 8),
          ...msg.recommendedBuses!.map((bus) {
            return BusCard(
              bus: bus,
              onSelectSeats: () => _onBusSelected(context, bus, true),
              onViewDetails: () => _onBusSelected(context, bus, false),
            );
          }),
        ],

        // Quick Suggestion Chips
        if (msg.quickSuggestions != null && msg.quickSuggestions!.isNotEmpty) ...[
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.only(left: 36, bottom: 8),
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              children: msg.quickSuggestions!.map((sugg) {
                return ActionChip(
                  label: Text(sugg),
                  labelStyle: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppColors.primary),
                  backgroundColor: isDark ? AppColors.darkCard : AppColors.primaryLight,
                  side: const BorderSide(color: AppColors.primaryLight),
                  onPressed: () {
                    chatProvider.sendMessage(sugg);
                    _scrollToBottom();
                  },
                );
              }).toList(),
            ),
          ),
        ],

        const SizedBox(height: 6),
      ],
    );
  }
}
