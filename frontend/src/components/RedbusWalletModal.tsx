"use client";

import React, { useState, useEffect, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  ArrowLeft,
  X,
  HelpCircle,
  Gift,
  CreditCard,
  ChevronRight,
  Bus,
  CheckCircle2,
  Ticket,
} from "lucide-react";
import Link from "next/link";
import { useAppSelector } from "@/store";
import { useGetMyBookingsQuery } from "@/store/apiSlice";

interface WalletTransaction {
  id: string;
  type: "ADDED" | "DEDUCTED";
  amount: number;
  reference: string;
  description: string;
  date: string;
  year: string;
}

interface RedbusWalletModalProps {
  isOpen: boolean;
  onClose: () => void;
}

function formatTxDate(dateStr?: string) {
  if (!dateStr) return { date: "Recent", year: "2026" };
  try {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return { date: "Recent", year: "2026" };
    const date = d.toLocaleDateString("en-IN", { day: "numeric", month: "short" });
    const year = d.getFullYear().toString();
    return { date, year };
  } catch {
    return { date: "Recent", year: "2026" };
  }
}

export default function RedbusWalletModal({ isOpen, onClose }: RedbusWalletModalProps) {
  const { user } = useAppSelector((state) => state.auth);

  const [activeTab, setActiveTab] = useState<"ALL" | "ADDED" | "DEDUCTED">("ALL");
  const [notificationMsg, setNotificationMsg] = useState("");
  const [showFaq, setShowFaq] = useState(false);

  // Fetch real user bookings to extract live wallet transactions & refunds
  const { data: bookingsData, isLoading: isLoadingBookings } = useGetMyBookingsQuery(
    { page: 0, size: 50 },
    { skip: !user || !isOpen }
  );

  // Current live wallet balance
  const walletBalance = Number(user?.walletBalance ?? 1227.4);

  // Prevent background scroll when modal is active
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = "auto";
      };
    }
  }, [isOpen]);

  // Derive real recent transactions from actual bookings & cancellations
  const transactions: WalletTransaction[] = useMemo(() => {
    const list: (WalletTransaction & { timestamp: number })[] = [];
    const bookings = bookingsData?.content || [];

    bookings.forEach((b) => {
      const createdDate = b.createdAt || b.travelDate;
      const ts = createdDate ? new Date(createdDate).getTime() : 0;
      const { date, year } = formatTxDate(createdDate);

      // Deductions: when wallet was used for bus booking
      if (b.walletAmountUsed && Number(b.walletAmountUsed) > 0) {
        list.push({
          id: `deduct-${b.pnr}-${b.id}`,
          type: "DEDUCTED",
          amount: Number(b.walletAmountUsed),
          reference: `TIN: ${b.pnr}`,
          description: `Offer cash · Used for ${b.sourceCity}-${b.destinationCity} trip on ${formatTxDate(b.travelDate).date}`,
          date,
          year,
          timestamp: ts,
        });
      }

      // Additions: refunds credited back to user wallet
      if (
        (b.status === "CANCELLED" || b.status === "REFUNDED") &&
        b.refundAmount &&
        Number(b.refundAmount) > 0
      ) {
        const refundTs = b.refundApprovedAt ? new Date(b.refundApprovedAt).getTime() : ts + 1000;
        const refundDate = formatTxDate(b.refundApprovedAt || b.createdAt);
        list.push({
          id: `refund-${b.pnr}-${b.id}`,
          type: "ADDED",
          amount: Number(b.refundAmount),
          reference: `TIN: ${b.pnr}`,
          description: `Refund credited to Wallet · ${b.sourceCity}-${b.destinationCity} trip`,
          date: refundDate.date,
          year: refundDate.year,
          timestamp: refundTs,
        });
      }
    });

    // If user has active wallet offer cash/balance, show the welcome/promotional credit entry
    if (walletBalance > 0) {
      list.push({
        id: "offer-cash-credit",
        type: "ADDED",
        amount: walletBalance,
        reference: "Expiration Date: 17-Mar-2027",
        description: "Offer cash · Added to Wallet",
        date: "17 Sep",
        year: "2026",
        timestamp: 1726531200000,
      });
    }

    // Sort newest first
    list.sort((a, b) => b.timestamp - a.timestamp);
    return list;
  }, [bookingsData, walletBalance]);

  const filteredTransactions = transactions.filter((t) => {
    if (activeTab === "ADDED") return t.type === "ADDED";
    if (activeTab === "DEDUCTED") return t.type === "DEDUCTED";
    return true;
  });

  const handleCopyReferral = () => {
    const refCode = user?.name ? user.name.toUpperCase().replace(/\s+/g, "") : "REDBUS100";
    const refLink = `https://redbusai.app/refer?code=${refCode}`;
    if (typeof navigator !== "undefined" && navigator.clipboard) {
      navigator.clipboard.writeText(refLink);
      setNotificationMsg(`Referral link copied: ${refLink}`);
      setTimeout(() => setNotificationMsg(""), 3500);
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center sm:p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-xs cursor-pointer"
          />

          {/* Modal Container */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            transition={{ type: "spring", damping: 25, stiffness: 320 }}
            className="bg-[#f7f8fa] dark:bg-slate-900 w-full sm:max-w-md h-full sm:h-auto sm:max-h-[92vh] sm:rounded-3xl shadow-2xl border border-gray-100 dark:border-slate-800 flex flex-col relative z-10 overflow-hidden"
          >
            {/* Header */}
            <div className="p-4 sm:p-5 bg-white dark:bg-slate-900 border-b border-gray-100 dark:border-slate-800 flex items-center justify-between shrink-0">
              <div className="flex items-center space-x-3">
                <button
                  type="button"
                  onClick={onClose}
                  className="p-1.5 -ml-1 text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800 rounded-full transition-colors cursor-pointer"
                  title="Close"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <h2 className="text-base sm:text-lg font-black text-gray-900 dark:text-white">
                  redBus wallet
                </h2>
              </div>

              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  onClick={() => setShowFaq(!showFaq)}
                  className="text-xs font-black text-[#1d4ed8] hover:underline px-2 py-1 cursor-pointer"
                >
                  FAQ
                </button>
                <button
                  type="button"
                  onClick={onClose}
                  className="p-1.5 text-gray-400 hover:text-gray-700 dark:hover:text-white rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Notification Banner */}
            {notificationMsg && (
              <div className="p-3 bg-emerald-50 text-emerald-800 border-b border-emerald-200 text-xs font-bold flex items-center gap-2 animate-in fade-in">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span className="truncate">{notificationMsg}</span>
              </div>
            )}

            {/* Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-4">
              {/* FAQ Drawer if open */}
              {showFaq && (
                <div className="p-4 bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800/60 rounded-2xl text-xs text-blue-900 dark:text-blue-200 space-y-2 animate-in fade-in">
                  <h4 className="font-bold flex items-center gap-1.5 text-sm">
                    <HelpCircle className="w-4 h-4 text-blue-600" />
                    <span>Frequently Asked Questions</span>
                  </h4>
                  <p><strong>Q: What is redBus Wallet?</strong><br />A digital balance you can use instantly for 1-click bus ticket payments with zero gateway charges.</p>
                  <p><strong>Q: How do refunds work?</strong><br />Cancelled ticket refunds are credited instantly into your redBus wallet.</p>
                  <p><strong>Q: Can I combine wallet with UPI?</strong><br />Yes, split payments are supported at checkout.</p>
                </div>
              )}

              {/* CARD 1: MAIN WALLET BALANCE (Matching Screenshot UI) */}
              <div className="bg-white dark:bg-slate-800 rounded-3xl shadow-sm border border-gray-100 dark:border-slate-700/80 overflow-hidden relative">
                {/* Top Section */}
                <div className="p-5 pb-4 relative">
                  {/* Top Right Bus Badge matching screenshot */}
                  <div className="absolute top-4 right-4 w-10 h-14 bg-gradient-to-b from-[#1a1f2c] to-[#2d3748] rounded-b-full flex flex-col items-center justify-start pt-2 shadow-sm">
                    <div className="w-6 h-4 border border-white/40 rounded-sm flex items-center justify-center">
                      <Bus className="w-3 h-3 text-white" />
                    </div>
                  </div>

                  <div className="text-3xl sm:text-4xl font-black text-gray-900 dark:text-white tracking-tight">
                    ₹{walletBalance.toFixed(2)}
                  </div>
                  <span className="text-xs font-semibold text-gray-500 dark:text-slate-400 block mt-0.5">
                    Wallet balance
                  </span>

                  {/* Offer Cash Row */}
                  <div className="mt-4 pt-3 border-t border-gray-100 dark:border-slate-700/60 flex items-center justify-between text-xs text-gray-700 dark:text-slate-200">
                    <span className="font-medium">Offer cash: ₹{walletBalance.toFixed(2)}</span>
                    <ChevronRight className="w-4 h-4 text-gray-400" />
                  </div>
                </div>

                {/* Expiry Banner matching screenshot (Clean, no Add Funds button) */}
                <div className="px-5 py-3 bg-gradient-to-r from-[#b45309] via-[#c2410c] to-[#9a3412] text-white text-[11px] font-bold tracking-wide flex items-center justify-between">
                  <span>₹{walletBalance.toFixed(2)} expires by 17 Mar 2027</span>
                </div>
              </div>

              {/* CARD 2: REFER AND EARN BANNER (Matching Screenshot UI) */}
              <div className="bg-white dark:bg-slate-800 rounded-3xl p-5 border border-gray-100 dark:border-slate-700/80 shadow-sm relative overflow-hidden flex items-center justify-between">
                <div className="space-y-3 max-w-[200px] z-10">
                  <div>
                    <h3 className="text-sm font-black text-gray-900 dark:text-white">Refer and earn</h3>
                    <p className="text-[11px] text-gray-500 dark:text-slate-400 mt-0.5 leading-tight">
                      Invite your friends to redBus and earn wallet money
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={handleCopyReferral}
                    className="px-4 py-2 bg-[#ffe4e6] hover:bg-[#fecdd3] text-[#be123c] rounded-2xl text-xs font-bold transition-all flex items-center space-x-1.5 cursor-pointer shadow-2xs"
                  >
                    <span>Refer now</span>
                  </button>
                </div>

                {/* Graphic badge */}
                <div className="w-28 h-24 bg-gradient-to-br from-rose-100/60 to-pink-100/40 dark:from-rose-950/40 dark:to-pink-950/20 rounded-2xl flex items-center justify-center relative">
                  <div className="w-14 h-20 bg-[#d84e55] rounded-xl shadow-md p-1.5 flex flex-col items-center justify-between border-2 border-white">
                    <span className="text-[7px] text-white font-extrabold tracking-wider">redBus</span>
                    <div className="w-6 h-6 rounded-full bg-white/20 flex items-center justify-center">
                      <Gift className="w-3.5 h-3.5 text-white" />
                    </div>
                    <span className="text-[6px] text-white/90">₹250 OFF</span>
                  </div>
                  <div className="absolute -bottom-1 -right-1 w-7 h-7 rounded-full bg-amber-400 border-2 border-white flex items-center justify-center shadow-xs">
                    <span className="text-[9px] font-black text-amber-950">₹</span>
                  </div>
                </div>
              </div>

              {/* CARD 3: RECENT ACTIVITY */}
              <div className="space-y-3 pt-2">
                <h3 className="text-base font-black text-gray-900 dark:text-white">
                  Recent activity
                </h3>

                {/* Filter Tabs: All, Added, Deducted */}
                <div className="flex space-x-2">
                  {[
                    { id: "ALL", label: "All" },
                    { id: "ADDED", label: "Added" },
                    { id: "DEDUCTED", label: "Deducted" },
                  ].map((tab) => (
                    <button
                      key={tab.id}
                      type="button"
                      onClick={() => setActiveTab(tab.id as any)}
                      className={`px-4 py-1.5 rounded-full text-xs font-bold transition-all cursor-pointer ${
                        activeTab === tab.id
                          ? "bg-[#ffe4e6] text-[#be123c] ring-1 ring-rose-300 dark:bg-rose-950/60 dark:text-rose-300 dark:ring-rose-800"
                          : "bg-white dark:bg-slate-800 text-gray-600 dark:text-slate-300 border border-gray-200 dark:border-slate-700 hover:bg-gray-50"
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>

                {/* Year Label */}
                <span className="text-xs font-bold text-gray-400 dark:text-slate-500 block pt-1">
                  2026
                </span>

                {/* Transaction List */}
                <div className="space-y-3">
                  {isLoadingBookings ? (
                    <div className="bg-white dark:bg-slate-800 rounded-2xl p-6 text-center text-xs text-gray-400">
                      Loading real transaction history...
                    </div>
                  ) : filteredTransactions.length === 0 ? (
                    <div className="bg-white dark:bg-slate-800 rounded-2xl p-6 text-center text-xs text-gray-400 space-y-2">
                      <Ticket className="w-6 h-6 mx-auto text-gray-300" />
                      <p>No {activeTab.toLowerCase()} wallet activity found.</p>
                      <Link
                        href="/"
                        onClick={onClose}
                        className="inline-block mt-1 text-xs font-bold text-[#d84e55] hover:underline"
                      >
                        Book a trip with wallet →
                      </Link>
                    </div>
                  ) : (
                    filteredTransactions.map((tx) => (
                      <div
                        key={tx.id}
                        className="bg-white dark:bg-slate-800 rounded-2xl p-4 border border-gray-100 dark:border-slate-700/70 shadow-2xs flex items-start space-x-3.5 hover:shadow-xs transition-shadow"
                      >
                        {/* Transaction Icon */}
                        <div
                          className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 ${
                            tx.type === "DEDUCTED"
                              ? "bg-rose-100 dark:bg-rose-950/60 text-[#be123c]"
                              : "bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600"
                          }`}
                        >
                          <CreditCard className="w-4 h-4" />
                        </div>

                        {/* Middle Content */}
                        <div className="flex-1 min-w-0">
                          <span className="text-[11px] font-semibold text-gray-500 dark:text-slate-400 block truncate">
                            {tx.reference}
                          </span>

                          <span
                            className={`text-sm font-black block mt-0.5 ${
                              tx.type === "DEDUCTED"
                                ? "text-[#e11d48]"
                                : "text-emerald-600"
                            }`}
                          >
                            {tx.type === "DEDUCTED" ? "-" : "+"}₹{tx.amount.toFixed(2)}
                          </span>

                          <p className="text-xs text-gray-600 dark:text-slate-300 mt-0.5 leading-snug">
                            {tx.description}
                          </p>
                        </div>

                        {/* Right Date */}
                        <span className="text-xs font-semibold text-gray-400 dark:text-slate-500 shrink-0">
                          {tx.date}
                        </span>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
