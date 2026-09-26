"use client";

import React, { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAppDispatch, useAppSelector } from "@/store";
import { toggleChat, addMessage, setTyping, clearChat } from "@/store/chatSlice";
import { useChatWithAiMutation } from "@/store/apiSlice";
import type { RouteItem } from "@/types";
import {
  Sparkles,
  X,
  Send,
  RefreshCw,
  FileText,
  CheckCircle2,
  Mic,
  MicOff,
  Star,
  Clock,
  ChevronRight,
  Bus,
  AudioLines,
  Mail,
} from "lucide-react";
import AutotypingPlaceholder from "./AutotypingPlaceholder";
import { motion, AnimatePresence } from "framer-motion";

function renderFormattedContent(text: string) {
  if (!text) return null;
  const paragraphs = text.split(/\n\n+/);

  return (
    <div className="space-y-2 leading-relaxed">
      {paragraphs.map((para, pIdx) => {
        const lines = para.split(/\n/);
        return (
          <div key={pIdx} className="space-y-1">
            {lines.map((line, lIdx) => {
              const trimmed = line.trim();
              const isBullet = trimmed.startsWith("•") || trimmed.startsWith("- ") || trimmed.startsWith("* ");
              const cleanLine = isBullet ? trimmed.replace(/^[•\-*]\s*/, "") : line;

              // Parse **bold**, *italic*, `code` without showing raw asterisks
              const parts = cleanLine.split(/(\*\*.*?\*\*|`.*?`|\*.*?\*)/g);

              const formattedParts = parts.map((part, partIdx) => {
                if (part.startsWith("**") && part.endsWith("**") && part.length >= 4) {
                  return (
                    <span key={partIdx} className="font-bold text-gray-900 dark:text-white">
                      {part.slice(2, -2)}
                    </span>
                  );
                }
                if (part.startsWith("*") && part.endsWith("*") && part.length >= 2) {
                  return (
                    <span key={partIdx} className="font-semibold text-gray-900 dark:text-white">
                      {part.slice(1, -1)}
                    </span>
                  );
                }
                if (part.startsWith("`") && part.endsWith("`") && part.length >= 2) {
                  return (
                    <code
                      key={partIdx}
                      className="px-1.5 py-0.5 bg-gray-100 dark:bg-slate-800 font-mono text-[11px] rounded text-[#d84e55] font-bold"
                    >
                      {part.slice(1, -1)}
                    </code>
                  );
                }
                return part;
              });

              if (isBullet) {
                return (
                  <div key={lIdx} className="flex items-start space-x-2 text-xs">
                    <span className="text-[#d84e55] font-bold text-sm leading-none mt-0.5">•</span>
                    <span className="flex-1 text-gray-800 dark:text-slate-200">{formattedParts}</span>
                  </div>
                );
              }

              return (
                <p key={lIdx} className="text-gray-800 dark:text-slate-200">
                  {formattedParts}
                </p>
              );
            })}
          </div>
        );
      })}
    </div>
  );
}

export default function AiChatWidget() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { isOpen, messages, isTyping, sessionId, isSeatSelectionOpen } = useAppSelector((state) => state.chat);
  const { user } = useAppSelector((state) => state.auth);
  const searchState = useAppSelector((state) => state.search);

  const [inputMessage, setInputMessage] = useState("");
  const [isListening, setIsListening] = useState(false);
  const [chatMutation] = useChatWithAiMutation();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Active context from search or default
  const sourceCity = searchState.sourceCity || "Madiwala";
  const destinationCity = searchState.destinationCity || "Chennai";
  const travelDate = searchState.travelDate || "20 Sep";

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isOpen, isTyping]);

  const handleSendMessage = async (textToSend?: string) => {
    const message = (textToSend || inputMessage).trim();
    if (!message) return;

    // Add user message
    dispatch(
      addMessage({
        id: "msg-" + Date.now(),
        role: "user",
        content: message,
        timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      })
    );
    setInputMessage("");
    dispatch(setTyping(true));

    try {
      const res = await chatMutation({
        sessionId,
        message,
        userId: user?.id,
        sourceCity,
        destinationCity,
        travelDate: searchState.travelDate,
      }).unwrap();

      dispatch(
        addMessage({
          id: "msg-" + Date.now(),
          role: "assistant",
          content: res.reply,
          toolExecuted: res.toolExecuted,
          toolData: res.toolData,
          suggestedPrompts: res.suggestedPrompts,
          timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        })
      );
    } catch (err: any) {
      dispatch(
        addMessage({
          id: "msg-" + Date.now(),
          role: "assistant",
          content: "Sorry, I ran into an issue processing your request. Please try again or reach our support team.",
          timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        })
      );
    } finally {
      dispatch(setTyping(false));
    }
  };

  // Web Speech API for voice speak feature
  const handleVoiceSpeak = () => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      alert("Voice recognition is not supported in this browser. Please use Google Chrome or Edge.");
      return;
    }

    if (isListening) {
      setIsListening(false);
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = false;
      recognition.lang = "en-IN";

      recognition.onstart = () => {
        setIsListening(true);
      };

      recognition.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        if (transcript) {
          setInputMessage(transcript);
          handleSendMessage(transcript);
        }
        setIsListening(false);
      };

      recognition.onerror = () => {
        setIsListening(false);
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognition.start();
    } catch (err) {
      console.error(err);
      setIsListening(false);
    }
  };

  const handleSelectRoute = (route: RouteItem) => {
    dispatch(toggleChat(false));
    const src = encodeURIComponent(route.sourceCity || sourceCity);
    const dst = encodeURIComponent(route.destinationCity || destinationCity);
    const dt = encodeURIComponent(route.travelDate || new Date().toISOString().split("T")[0]);
    router.push(`/search?source=${src}&destination=${dst}&date=${dt}&selectRoute=${route.id}`);
  };

  return (
    <AnimatePresence>
      {/* Floating Trigger Button */}
      {!isOpen && (
        <motion.button
          key="ask-ray-btn"
          initial={{ scale: 0, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0, opacity: 0 }}
          transition={{ type: "spring", damping: 20, stiffness: 300 }}
          type="button"
          onClick={() => dispatch(toggleChat(true))}
          className={`fixed bottom-4 left-1/2 -translate-x-1/2 sm:left-auto sm:translate-x-0 sm:bottom-6 sm:right-6 z-50 items-center space-x-2.5 px-5 py-2.5 sm:py-3 bg-gradient-to-r from-purple-600 via-[#d84e55] to-red-600 hover:from-purple-700 hover:to-red-700 text-white rounded-full shadow-2xl hover:scale-105 transition-all group active:scale-95 text-xs sm:text-sm font-extrabold tracking-wide cursor-pointer border border-white/20 whitespace-nowrap ${
            isSeatSelectionOpen ? "hidden sm:flex" : "flex"
          }`}
        >
          <div className="relative flex items-center justify-center">
            <Sparkles className="w-4 h-4 sm:w-5 sm:h-5 text-amber-300 animate-spin-slow" />
            <span className="absolute -top-1 -right-1 w-2 sm:w-2.5 h-2 sm:h-2.5 bg-emerald-400 rounded-full ring-2 ring-white animate-ping" />
          </div>
          <span>Ask RAY</span>
        </motion.button>
      )}

      {/* RAY beta Sliding Chat Window */}
      {isOpen && (
        <motion.div
          key="ask-ray-window"
          initial={{ opacity: 0, scale: 0.9, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.9, y: 20 }}
          transition={{ type: "spring", damping: 25, stiffness: 350 }}
          className="fixed bottom-3 sm:bottom-6 right-2 sm:right-6 left-2 sm:left-auto z-50 sm:w-[420px] max-w-[calc(100vw-1rem)] h-[580px] sm:h-[620px] max-h-[90vh] bg-white dark:bg-[#0f172a] rounded-3xl shadow-2xl border border-gray-200 dark:border-slate-800 flex flex-col overflow-hidden"
        >
          {/* Header matching Screenshot: RAY beta - redBus assistance for you */}
          <div className="px-5 py-4 bg-white dark:bg-[#0f172a] border-b border-gray-100 dark:border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-9 h-9 rounded-xl bg-purple-50 dark:bg-purple-950/60 flex items-center justify-center text-purple-600 dark:text-purple-400 shadow-2xs">
                <Sparkles className="w-5 h-5 fill-purple-600 dark:fill-purple-400 text-purple-600 dark:text-purple-400" />
              </div>
              <div>
                <div className="flex items-center space-x-1.5">
                  <h3 className="font-extrabold text-base text-gray-900 dark:text-white leading-none">
                    RAY <span className="font-medium text-xs text-purple-600 dark:text-purple-400">beta</span>
                  </h3>
                </div>
                <p className="text-[11px] text-gray-400 dark:text-slate-400 mt-0.5 font-medium">
                  redBus assistance for you
                </p>
              </div>
            </div>

            <div className="flex items-center space-x-1">
              <button
                onClick={() => dispatch(clearChat())}
                title="Reset conversation"
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors text-xs"
              >
                <RefreshCw className="w-4 h-4" />
              </button>
              <button
                onClick={() => dispatch(toggleChat(false))}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Context pill banner: "Helping you choose a bus from Madiwala to Chennai on 20 Sep" */}
          <div className="px-4 pt-3 pb-1 bg-white dark:bg-[#0f172a]">
            <div className="mx-auto text-center px-4 py-2 bg-slate-50 dark:bg-slate-800/70 border border-slate-200/70 dark:border-slate-700/60 rounded-xl text-xs font-medium text-gray-600 dark:text-slate-300 max-w-[90%] shadow-2xs">
              Helping you choose a bus from <span className="font-bold text-gray-900 dark:text-white">{sourceCity}</span> to <span className="font-bold text-gray-900 dark:text-white">{destinationCity}</span> on {travelDate}
            </div>
          </div>

          {/* Message History */}
          <div className="flex-1 p-4 overflow-y-auto space-y-4 bg-white dark:bg-[#0f172a]">
            {/* Introductory prompt section */}
            <div className="space-y-2">
              <p className="text-xs font-bold text-gray-700 dark:text-slate-200">
                Tell us how we can help you today!
              </p>
              <div className="flex flex-wrap gap-2">
                {[
                  "Show ac buses",
                  "Show buses with tracking link",
                  "Show sleeper buses",
                  "Buses under ₹1000",
                ].map((chip, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleSendMessage(chip)}
                    className="px-3.5 py-1.5 rounded-full border border-purple-200 dark:border-purple-900/60 text-xs font-semibold text-gray-800 dark:text-slate-200 hover:border-purple-600 hover:bg-purple-50 dark:hover:bg-purple-950/40 transition-all shadow-2xs cursor-pointer text-left active:scale-95"
                  >
                    {chip}
                  </button>
                ))}
              </div>
            </div>

            {/* Chat bubbles */}
            {messages.map((msg, idx) => (
              <div
                key={msg.id || idx}
                className={`flex flex-col ${
                  msg.role === "user" ? "items-end" : "items-start"
                }`}
              >
                {/* User Message: Dark rounded pill from screenshot */}
                {msg.role === "user" ? (
                  <div className="max-w-[85%] rounded-2xl px-4 py-2.5 text-xs font-medium bg-[#374151] dark:bg-slate-700 text-white shadow-xs">
                    {msg.content}
                  </div>
                ) : (
                  /* Bot Message */
                  <div className="max-w-full text-xs text-gray-800 dark:text-slate-200 space-y-2.5">
                    {renderFormattedContent(msg.content)}

                    {/* Rich Bus Cards inside chat (Exact replica of Redbus RAY beta card from Screenshot) */}
                    {msg.toolExecuted === "searchRoutes" &&
                      Array.isArray(msg.toolData) &&
                      msg.toolData.length > 0 && (
                        <div className="space-y-2.5 pt-1 w-full">
                          {msg.toolData.slice(0, 3).map((route: RouteItem) => {
                            const originalFare = Math.round(Number(route.basePrice || 999) * 1.11);
                            const finalFare = Math.round(Number(route.basePrice || 899));
                            const durationText = route.durationHours
                              ? `${route.durationHours}h`
                              : "8h";
                            const ratingScore = route.rating ? route.rating.toFixed(1) : "4.4";

                            return (
                              <div
                                key={route.id}
                                onClick={() => handleSelectRoute(route)}
                                className="bg-white dark:bg-slate-800/90 rounded-2xl border border-gray-200 dark:border-slate-700/80 p-4 shadow-sm hover:shadow-md transition-all relative cursor-pointer group hover:border-purple-400"
                              >
                                {/* Top right badge: "Try new 10% Off" */}
                                <div className="absolute top-3 right-3">
                                  <span className="px-2.5 py-0.5 rounded-full bg-amber-100 dark:bg-amber-950/80 text-amber-900 dark:text-amber-300 font-bold text-[10px] border border-amber-200/80 dark:border-amber-800/60 shadow-2xs">
                                    Try new <span className="font-extrabold">10% Off</span>
                                  </span>
                                </div>

                                <div className="grid grid-cols-12 gap-2 items-center">
                                  {/* Left: Operator and bus type */}
                                  <div className="col-span-5 pr-1">
                                    <h4 className="font-bold text-sm text-gray-900 dark:text-white truncate group-hover:text-[#d84e55] transition-colors">
                                      {route.operatorName}
                                    </h4>
                                    <p className="text-[11px] text-gray-400 dark:text-slate-400 truncate mt-0.5">
                                      {route.busType}
                                    </p>
                                  </div>

                                  {/* Rating Green Badge */}
                                  <div className="col-span-2 flex flex-col items-center justify-center">
                                    <div className="flex items-center space-x-0.5 px-2 py-0.5 bg-[#15803d] text-white rounded-md text-xs font-bold shadow-2xs">
                                      <Star className="w-3 h-3 fill-white text-white" />
                                      <span>{ratingScore}</span>
                                    </div>
                                    <span className="text-[10px] text-gray-400 dark:text-slate-400 mt-0.5">
                                      427
                                    </span>
                                  </div>

                                  {/* Timings & Duration */}
                                  <div className="col-span-2 text-center">
                                    <p className="font-bold text-xs text-gray-900 dark:text-white whitespace-nowrap">
                                      {route.departureTime?.substring(0, 5)} — {route.arrivalTime?.substring(0, 5)}
                                    </p>
                                    <p className="text-[10px] text-gray-400 dark:text-slate-400 mt-0.5 whitespace-nowrap">
                                      {durationText} • {route.availableSeats} Seats
                                    </p>
                                  </div>

                                  {/* Price */}
                                  <div className="col-span-3 text-right">
                                    <p className="text-[11px] text-gray-400 line-through">
                                      ₹{originalFare}
                                    </p>
                                    <p className="font-extrabold text-sm text-gray-900 dark:text-white">
                                      ₹{finalFare}
                                    </p>
                                    <p className="text-[9px] text-gray-400 dark:text-slate-400 font-medium">
                                      Onwards
                                    </p>
                                  </div>
                                </div>

                                {/* Quick CTA button */}
                                <div className="mt-3 pt-2.5 border-t border-gray-100 dark:border-slate-700/60 flex items-center justify-between">
                                  <span className="text-[10px] font-semibold text-emerald-600 dark:text-emerald-400">
                                    ✓ Live Tracking & Free Cancellation available
                                  </span>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleSelectRoute(route);
                                    }}
                                    className="px-3 py-1 bg-red-50 hover:bg-[#d84e55] text-[#d84e55] hover:text-white dark:bg-red-950/60 dark:text-red-300 dark:hover:bg-[#d84e55] rounded-lg font-bold text-[11px] transition-all flex items-center space-x-1 cursor-pointer"
                                  >
                                    <span>Select Seats</span>
                                    <ChevronRight className="w-3.5 h-3.5" />
                                  </button>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      )}

                    {/* Booking status action */}
                    {msg.toolExecuted === "getBookingStatus" && (
                      <div className="mt-2.5 pt-2 border-t border-gray-100 dark:border-slate-700/60 flex flex-wrap items-center gap-1.5">
                        {(() => {
                          const pnrMatch = msg.content.match(/RB-[0-9]{4}-[A-Z0-9]{6}/)?.[0] || "";
                          return (
                            <>
                              <button
                                type="button"
                                onClick={() => handleSendMessage(pnrMatch ? `Email ticket for ${pnrMatch}` : "Email me ticket")}
                                className="px-2.5 py-1 bg-purple-50 hover:bg-purple-100 dark:bg-purple-950/60 text-purple-700 dark:text-purple-300 rounded-md font-bold text-[10px] flex items-center space-x-1 cursor-pointer transition-colors shadow-2xs"
                              >
                                <Mail className="w-3 h-3 text-purple-600" />
                                <span>Email Me Ticket</span>
                              </button>
                              {pnrMatch && (
                                <a
                                  href={`http://localhost:8080/api/bookings/${pnrMatch}/ticket-pdf`}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="px-2.5 py-1 bg-red-50 hover:bg-red-100 text-[#d84e55] dark:bg-red-950/60 dark:text-red-300 rounded-md font-bold text-[10px] flex items-center space-x-1 transition-colors"
                                >
                                  <FileText className="w-3 h-3" />
                                  <span>Download PDF</span>
                                </a>
                              )}
                            </>
                          );
                        })()}
                      </div>
                    )}

                    {/* Email ticket action confirmation */}
                    {msg.toolExecuted === "emailTicket" && (
                      <div className="mt-2.5 pt-2 border-t border-gray-100 dark:border-slate-700/60 flex flex-wrap items-center gap-1.5">
                        <span className="px-2.5 py-1 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 rounded-md font-bold text-[10px] flex items-center space-x-1">
                          <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                          <span>E-Ticket Dispatched to Inbox</span>
                        </span>
                        {(() => {
                          const pnrMatch = msg.content.match(/RB-[0-9]{4}-[A-Z0-9]{6}/)?.[0] || "";
                          return pnrMatch ? (
                            <a
                              href={`http://localhost:8080/api/bookings/${pnrMatch}/ticket-pdf`}
                              target="_blank"
                              rel="noreferrer"
                              className="px-2.5 py-1 bg-red-50 hover:bg-red-100 text-[#d84e55] dark:bg-red-950/60 dark:text-red-300 rounded-md font-bold text-[10px] flex items-center space-x-1 transition-colors"
                            >
                              <FileText className="w-3 h-3" />
                              <span>Download PDF</span>
                            </a>
                          ) : null;
                        })()}
                      </div>
                    )}
                  </div>
                )}

                <span className="text-[9px] text-gray-400 dark:text-slate-500 mt-1 px-1">
                  {msg.timestamp}
                </span>
              </div>
            ))}

            {isTyping && (
              <div className="flex items-center space-x-1.5 p-2.5 bg-gray-100 dark:bg-slate-800 rounded-2xl max-w-[80px]">
                <div className="w-1.5 h-1.5 rounded-full bg-[#d84e55] animate-bounce" />
                <div className="w-1.5 h-1.5 rounded-full bg-[#d84e55] animate-bounce [animation-delay:0.2s]" />
                <div className="w-1.5 h-1.5 rounded-full bg-[#d84e55] animate-bounce [animation-delay:0.4s]" />
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Bottom Bar matching Screenshot: "Type here" + Mic icon + Purple "Speak" button */}
          <div className="p-3 bg-white dark:bg-[#0f172a] border-t border-gray-100 dark:border-slate-800">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSendMessage();
              }}
              className="flex items-center space-x-2"
            >
              <div className="relative flex-1">
                {!inputMessage && !isListening && (
                  <div className="absolute left-4 right-10 inset-y-0 flex items-center pointer-events-none">
                    <AutotypingPlaceholder
                      phrases={[
                        "Ask: Need bus between Chennai and Bengaluru",
                        "Ask: Email me ticket for PNR 784219",
                        "Ask: Primo buses with live tracking",
                        "Ask: Show AC sleeper under ₹1000",
                        "Ask: What is the cancellation policy?",
                      ]}
                      className="text-xs font-medium text-gray-400 dark:text-slate-400"
                      cursorClassName="bg-purple-600 dark:bg-purple-400"
                    />
                  </div>
                )}
                <input
                  type="text"
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  placeholder={isListening ? "Listening... speak now" : ""}
                  className="w-full pl-4 pr-10 py-2.5 bg-gray-50 dark:bg-slate-800/80 border border-gray-200 dark:border-slate-700 rounded-full text-xs text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-400 focus:outline-hidden focus:ring-2 focus:ring-purple-600 focus:bg-white dark:focus:bg-slate-800 relative z-10"
                />
                <button
                  type="button"
                  onClick={handleVoiceSpeak}
                  title={isListening ? "Stop listening" : "Click to speak"}
                  className={`absolute right-2.5 top-1/2 -translate-y-1/2 p-1 rounded-full text-gray-400 hover:text-purple-600 transition-colors ${
                    isListening ? "text-red-500 animate-pulse" : ""
                  }`}
                >
                  {isListening ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
                </button>
              </div>

              {/* Purple "Speak" Button with animated waveform from Screenshot */}
              <button
                type="button"
                onClick={handleVoiceSpeak}
                className={`px-4 py-2.5 rounded-full text-white font-bold text-xs flex items-center space-x-1.5 shadow-md active:scale-95 transition-all cursor-pointer ${
                  isListening
                    ? "bg-red-600 hover:bg-red-700 animate-pulse"
                    : "bg-gradient-to-r from-purple-600 via-indigo-600 to-purple-700 hover:from-purple-700 hover:to-indigo-700"
                }`}
              >
                <AudioLines className="w-4 h-4 animate-pulse" />
                <span>{isListening ? "Listening" : "Speak"}</span>
              </button>

              {/* Submit send button if typing */}
              {inputMessage.trim() && (
                <button
                  type="submit"
                  disabled={isTyping}
                  className="p-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-full transition-colors shadow-xs shrink-0 cursor-pointer"
                >
                  <Send className="w-3.5 h-3.5" />
                </button>
              )}
            </form>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
