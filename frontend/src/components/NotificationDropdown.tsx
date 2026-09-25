"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useAppDispatch } from "@/store";
import {
  markAsRead,
  markAllAsRead,
  removeNotification,
  clearAllNotifications,
  toggleNotificationDropdown,
} from "@/store/notificationSlice";
import { useRealtimeNotifications } from "@/hooks/useRealtimeNotifications";
import {
  Bell,
  Ticket,
  Tag,
  AlertCircle,
  Wallet,
  CheckCheck,
  Trash2,
  X,
  ChevronRight,
  Sparkles,
  MapPin,
  Clock,
  LogIn,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface NotificationDropdownProps {
  onOpenAuthModal?: () => void;
}

export default function NotificationDropdown({ onOpenAuthModal }: NotificationDropdownProps) {
  const dispatch = useAppDispatch();
  const { notifications, unreadCount, isOpen, isAuthenticated, activeUser } =
    useRealtimeNotifications();
  const [activeTab, setActiveTab] = useState<"ALL" | "BOOKING" | "OFFER" | "ALERT">("ALL");

  const filteredNotifications = notifications.filter((n) => {
    if (activeTab === "ALL") return true;
    if (activeTab === "BOOKING") return n.type === "booking";
    if (activeTab === "OFFER") return n.type === "offer";
    if (activeTab === "ALERT") return n.type === "alert" || n.type === "wallet";
    return true;
  });

  const getIcon = (type: string, id: string) => {
    if (id.startsWith("booking-live")) {
      return (
        <div className="w-8 h-8 rounded-full bg-red-100 dark:bg-red-950/70 text-[#d84e55] dark:text-red-400 flex items-center justify-center shrink-0">
          <MapPin className="w-4 h-4 animate-bounce" />
        </div>
      );
    }
    switch (type) {
      case "booking":
        return (
          <div className="w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-950/70 text-blue-600 dark:text-blue-400 flex items-center justify-center shrink-0">
            <Ticket className="w-4 h-4" />
          </div>
        );
      case "offer":
        return (
          <div className="w-8 h-8 rounded-full bg-amber-100 dark:bg-amber-950/70 text-amber-600 dark:text-amber-400 flex items-center justify-center shrink-0">
            <Tag className="w-4 h-4" />
          </div>
        );
      case "wallet":
        return (
          <div className="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-950/70 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
            <Wallet className="w-4 h-4" />
          </div>
        );
      case "alert":
      default:
        return (
          <div className="w-8 h-8 rounded-full bg-rose-100 dark:bg-rose-950/70 text-rose-600 dark:text-rose-400 flex items-center justify-center shrink-0">
            <AlertCircle className="w-4 h-4" />
          </div>
        );
    }
  };

  const handleActionClick = (notif: (typeof notifications)[0]) => {
    dispatch(markAsRead(notif.id));
    dispatch(toggleNotificationDropdown(false));

    if (notif.id === "guest-login-prompt" && onOpenAuthModal) {
      onOpenAuthModal();
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 8 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 8 }}
          transition={{ duration: 0.18, ease: "easeOut" }}
          className="absolute right-0 mt-2 w-80 sm:w-96 bg-white dark:bg-[#0f172a] rounded-2xl shadow-2xl border border-gray-200 dark:border-slate-800 z-50 overflow-hidden"
        >
          {/* Header */}
          <div className="p-4 bg-gray-50/80 dark:bg-slate-800/80 border-b border-gray-100 dark:border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <div className="p-1.5 rounded-lg bg-red-100 dark:bg-red-950/60 text-[#d84e55] dark:text-red-400">
                <Bell className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-gray-900 dark:text-white flex items-center gap-1.5">
                  <span>Notifications</span>
                  {unreadCount > 0 && (
                    <span className="px-1.5 py-0.2 bg-[#d84e55] text-white text-[10px] font-bold rounded-full">
                      {unreadCount} new
                    </span>
                  )}
                </h3>
                <p className="text-[10px] text-gray-400 dark:text-slate-400">
                  {isAuthenticated && activeUser
                    ? `Live trip updates for ${activeUser.name.split(" ")[0]}`
                    : "Real-time updates & verified deals"}
                </p>
              </div>
            </div>

            <div className="flex items-center space-x-1">
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={() => dispatch(markAllAsRead(notifications.map((n) => n.id)))}
                  className="px-2 py-1 text-[11px] font-medium text-[#d84e55] dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
                  title="Mark all as read"
                >
                  <CheckCheck className="w-3.5 h-3.5" />
                  <span>Mark all read</span>
                </button>
              )}
              <button
                type="button"
                onClick={() => dispatch(toggleNotificationDropdown(false))}
                className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-slate-300 rounded-lg hover:bg-gray-100 dark:hover:bg-slate-700/60 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Tabs */}
          <div className="flex items-center px-3 pt-2 bg-white dark:bg-[#0f172a] border-b border-gray-100 dark:border-slate-800 text-xs font-semibold gap-1">
            {(
              [
                { id: "ALL", label: "All" },
                { id: "BOOKING", label: "Bookings" },
                { id: "OFFER", label: "Offers" },
                { id: "ALERT", label: "Alerts" },
              ] as const
            ).map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={`px-3 py-1.5 rounded-lg transition-all cursor-pointer ${
                  activeTab === tab.id
                    ? "bg-red-50 dark:bg-red-950/50 text-[#d84e55] dark:text-red-400 border border-red-200/80 dark:border-red-900/60 font-bold"
                    : "text-gray-500 dark:text-slate-400 hover:text-gray-800 dark:hover:text-slate-200"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Real-time Notification List */}
          <div className="max-h-80 overflow-y-auto divide-y divide-gray-100 dark:divide-slate-800/80">
            {filteredNotifications.length > 0 ? (
              filteredNotifications.map((notif) => (
                <div
                  key={notif.id}
                  className={`p-3.5 flex items-start space-x-3 transition-colors hover:bg-gray-50 dark:hover:bg-slate-800/50 ${
                    !notif.read ? "bg-red-50/20 dark:bg-red-950/10" : ""
                  }`}
                >
                  {getIcon(notif.type, notif.id)}

                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-1">
                      <h4 className="text-xs font-bold text-gray-900 dark:text-white leading-snug truncate">
                        {notif.title}
                      </h4>
                      {!notif.read && (
                        <span className="w-2 h-2 rounded-full bg-[#d84e55] shrink-0 mt-1" />
                      )}
                    </div>

                    <p className="text-[11px] text-gray-600 dark:text-slate-300 mt-0.5 line-clamp-2 leading-relaxed">
                      {notif.message}
                    </p>

                    <div className="flex items-center justify-between mt-2 pt-1">
                      <span className="text-[10px] text-gray-400 dark:text-slate-500 font-medium">
                        {notif.timestamp}
                      </span>

                      <div className="flex items-center space-x-2">
                        {notif.id === "guest-login-prompt" ? (
                          <button
                            type="button"
                            onClick={() => handleActionClick(notif)}
                            className="text-[11px] font-bold text-[#d84e55] dark:text-red-400 hover:underline flex items-center gap-0.5 cursor-pointer"
                          >
                            <LogIn className="w-3 h-3" />
                            <span>{notif.actionLabel || "Log In"}</span>
                          </button>
                        ) : notif.actionUrl ? (
                          <Link
                            href={notif.actionUrl}
                            onClick={() => handleActionClick(notif)}
                            className="text-[11px] font-bold text-[#d84e55] dark:text-red-400 hover:underline flex items-center gap-0.5"
                          >
                            <span>{notif.actionLabel || "View"}</span>
                            <ChevronRight className="w-3 h-3" />
                          </Link>
                        ) : null}

                        <button
                          type="button"
                          onClick={() => dispatch(removeNotification(notif.id))}
                          className="p-1 text-gray-300 hover:text-gray-500 dark:hover:text-slate-400 transition-colors cursor-pointer"
                          title="Dismiss"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <div className="p-8 text-center text-gray-400 dark:text-slate-500">
                <Bell className="w-8 h-8 mx-auto mb-2 opacity-40 text-gray-400" />
                <p className="text-xs font-semibold">No notifications right now</p>
                <p className="text-[10px] mt-0.5">We will alert you about your real-time trips and offers here.</p>
              </div>
            )}
          </div>

          {/* Footer */}
          {notifications.length > 0 && (
            <div className="p-2.5 bg-gray-50 dark:bg-slate-800/80 border-t border-gray-100 dark:border-slate-800 flex items-center justify-between text-xs">
              <button
                type="button"
                onClick={() =>
                  dispatch(clearAllNotifications(notifications.map((n) => n.id)))
                }
                className="text-[11px] text-gray-500 dark:text-slate-400 hover:text-red-600 dark:hover:text-red-400 font-medium flex items-center gap-1 transition-colors px-2 py-1 rounded cursor-pointer"
              >
                <Trash2 className="w-3 h-3" />
                <span>Clear all</span>
              </button>
              {isAuthenticated ? (
                <Link
                  href="/my-bookings"
                  onClick={() => dispatch(toggleNotificationDropdown(false))}
                  className="text-[11px] font-bold text-[#d84e55] dark:text-red-400 hover:underline px-2 py-1"
                >
                  View My Bookings →
                </Link>
              ) : (
                <button
                  type="button"
                  onClick={() => {
                    dispatch(toggleNotificationDropdown(false));
                    if (onOpenAuthModal) onOpenAuthModal();
                  }}
                  className="text-[11px] font-bold text-[#d84e55] dark:text-red-400 hover:underline px-2 py-1 cursor-pointer"
                >
                  Log In to View Bookings →
                </button>
              )}
            </div>
          )}
        </motion.div>
      )}
    </AnimatePresence>
  );
}

