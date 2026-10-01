"use client";

import React, { useEffect, useState, useRef } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAppDispatch } from "@/store";
import { markAsRead, removeNotification } from "@/store/notificationSlice";
import { useRealtimeNotifications } from "@/hooks/useRealtimeNotifications";
import type { AppNotification } from "@/types";
import {
  MapPin,
  Ticket,
  Tag,
  Wallet,
  AlertCircle,
  X,
  ChevronRight,
  Bus,
  Sparkles,
  Clock,
  ExternalLink,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface ToastItem extends AppNotification {
  toastId: string;
  autoDismissMs?: number;
}

export default function RealtimeNotificationToast() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { notifications, isAuthenticated } = useRealtimeNotifications();

  const [activeToasts, setActiveToasts] = useState<ToastItem[]>([]);
  const shownIdsRef = useRef<Set<string>>(new Set());

  // Track and pop new unread notifications as interactive toast cards
  useEffect(() => {
    if (!notifications || notifications.length === 0) return;

    // Filter unread notifications that haven't been shown in this session yet
    const newItems = notifications.filter(
      (n) => !n.read && !shownIdsRef.current.has(n.id)
    );

    if (newItems.length > 0) {
      // Mark as seen in this session to prevent repeated toast loops
      newItems.forEach((item) => shownIdsRef.current.add(item.id));

      // Prioritize live boarding alerts and recent bookings first
      const sorted = [...newItems].sort((a, b) => {
        if (a.id.startsWith("booking-live")) return -1;
        if (b.id.startsWith("booking-live")) return 1;
        return 0;
      });

      // Add up to 2 newest toasts to the stack
      const toastsToAdd: ToastItem[] = sorted.slice(0, 2).map((item) => ({
        ...item,
        toastId: `${item.id}-${Date.now()}`,
        autoDismissMs: item.id.startsWith("booking-live") ? 10000 : 7000,
      }));

      setActiveToasts((prev) => {
        const existingIds = new Set(prev.map((t) => t.id));
        const filteredNew = toastsToAdd.filter((t) => !existingIds.has(t.id));
        return [...prev, ...filteredNew].slice(-3); // Keep maximum 3 toasts on screen
      });
    }
  }, [notifications]);

  const handleDismiss = (toastId: string, notifId: string) => {
    setActiveToasts((prev) => prev.filter((t) => t.toastId !== toastId));
    dispatch(markAsRead(notifId));
  };

  const handleAction = (toast: ToastItem) => {
    handleDismiss(toast.toastId, toast.id);
    if (toast.actionUrl) {
      router.push(toast.actionUrl);
    }
  };

  const getToastBadge = (toast: ToastItem) => {
    if (toast.id.startsWith("booking-live")) {
      return {
        label: "LIVE BOARDING TODAY",
        bg: "bg-red-500",
        icon: <MapPin className="w-3.5 h-3.5 animate-bounce text-white" />,
        accent: "border-red-500/30 dark:border-red-500/40 shadow-red-500/10",
      };
    }
    switch (toast.type) {
      case "booking":
        return {
          label: "TICKET CONFIRMED",
          bg: "bg-blue-600",
          icon: <Bus className="w-3.5 h-3.5 text-white" />,
          accent: "border-blue-500/30 dark:border-blue-500/40 shadow-blue-500/10",
        };
      case "wallet":
        return {
          label: "WALLET UPDATE",
          bg: "bg-emerald-600",
          icon: <Wallet className="w-3.5 h-3.5 text-white" />,
          accent: "border-emerald-500/30 dark:border-emerald-500/40 shadow-emerald-500/10",
        };
      case "offer":
        return {
          label: "EXCLUSIVE OFFER",
          bg: "bg-amber-600",
          icon: <Tag className="w-3.5 h-3.5 text-white" />,
          accent: "border-amber-500/30 dark:border-amber-500/40 shadow-amber-500/10",
        };
      default:
        return {
          label: "TRIP ALERT",
          bg: "bg-[#d84e55]",
          icon: <AlertCircle className="w-3.5 h-3.5 text-white" />,
          accent: "border-rose-500/30 dark:border-rose-500/40 shadow-rose-500/10",
        };
    }
  };

  if (activeToasts.length === 0) return null;

  return (
    <div
      aria-live="polite"
      className="fixed top-20 right-4 sm:right-6 z-[9999] flex flex-col gap-3 max-w-sm sm:max-w-md w-full pointer-events-none"
    >
      <AnimatePresence mode="popLayout">
        {activeToasts.map((toast) => {
          const badge = getToastBadge(toast);

          return (
            <motion.div
              key={toast.toastId}
              layout
              initial={{ opacity: 0, y: -20, scale: 0.92, x: 20 }}
              animate={{ opacity: 1, y: 0, scale: 1, x: 0 }}
              exit={{ opacity: 0, scale: 0.9, x: 40, transition: { duration: 0.2 } }}
              transition={{ type: "spring", stiffness: 380, damping: 28 }}
              className={`pointer-events-auto relative overflow-hidden bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl border ${badge.accent} rounded-2xl shadow-2xl shadow-black/10 dark:shadow-black/40 p-4 transition-all hover:shadow-3xl`}
            >
              {/* Top Header Badge */}
              <div className="flex items-center justify-between gap-2 mb-2">
                <div className="flex items-center gap-2">
                  <span
                    className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold tracking-wider text-white ${badge.bg} shadow-sm`}
                  >
                    {badge.icon}
                    {badge.label}
                  </span>
                  {toast.timestamp && (
                    <span className="text-[11px] text-gray-500 dark:text-gray-400 flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {toast.timestamp}
                    </span>
                  )}
                </div>

                <button
                  onClick={() => handleDismiss(toast.toastId, toast.id)}
                  aria-label="Dismiss notification"
                  className="w-6 h-6 rounded-full flex items-center justify-center text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>

              {/* Title & Message */}
              <div className="space-y-1 pr-1">
                <h4 className="text-sm font-bold text-gray-900 dark:text-white leading-snug">
                  {toast.title}
                </h4>
                <p className="text-xs text-gray-600 dark:text-gray-300 line-clamp-3 leading-relaxed">
                  {toast.message}
                </p>
              </div>

              {/* Action Button & PNR info */}
              <div className="mt-3 pt-2.5 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between gap-2">
                {toast.pnr ? (
                  <span className="inline-flex items-center text-[11px] font-semibold text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-800 px-2 py-0.5 rounded-md">
                    PNR: <span className="text-[#d84e55] font-bold ml-1">{toast.pnr}</span>
                  </span>
                ) : (
                  <div />
                )}

                {toast.actionUrl && (
                  <button
                    onClick={() => handleAction(toast)}
                    className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-gradient-to-r from-[#d84e55] to-[#b8383f] hover:from-[#c23d44] hover:to-[#a12f36] text-white text-xs font-bold shadow-md shadow-red-500/20 transition-all active:scale-95"
                  >
                    <span>{toast.actionLabel || "View Details"}</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>

              {/* Animated Auto-dismiss Progress Bar */}
              <motion.div
                initial={{ width: "100%" }}
                animate={{ width: "0%" }}
                transition={{ duration: (toast.autoDismissMs || 7000) / 1000, ease: "linear" }}
                onAnimationComplete={() => handleDismiss(toast.toastId, toast.id)}
                className={`absolute bottom-0 left-0 h-[2.5px] ${badge.bg}`}
              />
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}
