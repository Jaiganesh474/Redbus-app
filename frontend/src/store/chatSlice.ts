import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import type { ChatMessageItem } from "@/types";

interface ChatState {
  isOpen: boolean;
  sessionId: string;
  messages: ChatMessageItem[];
  isTyping: boolean;
}

const initialWelcomeMessage: ChatMessageItem = {
  id: "welcome-1",
  role: "assistant",
  content:
    "👋 Hello! I am your RedBus AI Assistant. I can help you search for buses, check your PNR booking status, explain cancellation and baggage rules, or find recommendations! How may I assist you today?",
  suggestedPrompts: [
    "Find AC Sleeper from Bangalore to Chennai",
    "What is the cancellation policy?",
    "Check status of my PNR",
    "How much luggage is allowed?",
  ],
  timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
};

const initialState: ChatState = {
  isOpen: false,
  sessionId: "session-" + Math.random().toString(36).substring(2, 10),
  messages: [initialWelcomeMessage],
  isTyping: false,
};

export const chatSlice = createSlice({
  name: "chat",
  initialState,
  reducers: {
    toggleChat: (state, action: PayloadAction<boolean | undefined>) => {
      state.isOpen = action.payload !== undefined ? action.payload : !state.isOpen;
    },
    addMessage: (state, action: PayloadAction<ChatMessageItem>) => {
      state.messages.push(action.payload);
    },
    setTyping: (state, action: PayloadAction<boolean>) => {
      state.isTyping = action.payload;
    },
    clearChat: (state) => {
      state.messages = [initialWelcomeMessage];
      state.sessionId = "session-" + Math.random().toString(36).substring(2, 10);
    },
  },
});

export const { toggleChat, addMessage, setTyping, clearChat } = chatSlice.actions;
export default chatSlice.reducer;
