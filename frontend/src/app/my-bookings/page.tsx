"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { motion, AnimatePresence } from "framer-motion";
import { useAppSelector, useAppDispatch } from "@/store";
import { updateUser } from "@/store/authSlice";
import { addNotification } from "@/store/notificationSlice";
import {
  useGetMyBookingsQuery,
  useGetBookingByPnrQuery,
  useCancelBookingMutation,
  useSendTicketEmailMutation,
  useGetMeQuery,
} from "@/store/apiSlice";
import type { BookingDetails } from "@/types";
import {
  Ticket,
  Search,
  Download,
  AlertCircle,
  Clock,
  CheckCircle2,
  XCircle,
  RotateCcw,
  Bus,
  Wallet,
  ShieldAlert,
  AlertTriangle,
  X,
  ShieldCheck,
  Mail,
  Tag,
  ChevronDown,
  ChevronUp,
  CreditCard,
  User as UserIcon,
  MapPin,
} from "lucide-react";

export default function MyBookingsPage() {
  const dispatch = useAppDispatch();
  const { isAuthenticated, user } = useAppSelector((state) => state.auth);
  const { data: latestUser } = useGetMeQuery(undefined, { skip: !isAuthenticated });
  const activeUser = latestUser || user;

  const [pnrSearch, setPnrSearch] = useState("");
  const [activePnr, setActivePnr] = useState("");

  const [selectedBookingForCancel, setSelectedBookingForCancel] = useState<BookingDetails | null>(null);
  const [showTermsDialog, setShowTermsDialog] = useState(false);
  const [agreeToTerms, setAgreeToTerms] = useState(false);
  const [cancelReason, setCancelReason] = useState("Change of plans");
  const [refundDestination, setRefundDestination] = useState<"WALLET" | "ORIGINAL_PAYMENT">("WALLET");
  const [cancelMessage, setCancelMessage] = useState("");
  const [expandedPnrs, setExpandedPnrs] = useState<Record<string, boolean>>({});
  const [emailStatusByPnr, setEmailStatusByPnr] = useState<Record<string, string>>({});

  const toggleExpand = (pnr: string) => {
    setExpandedPnrs((prev) => ({ ...prev, [pnr]: !prev[pnr] }));
  };

  const [sendTicketEmailMutation, { isLoading: isEmailSending }] = useSendTicketEmailMutation();

  const handleSendEmail = async (pnr: string) => {
    setEmailStatusByPnr((prev) => ({ ...prev, [pnr]: "Sending e-ticket..." }));
    try {
      const res = await sendTicketEmailMutation({ pnr }).unwrap();
      setEmailStatusByPnr((prev) => ({ ...prev, [pnr]: res.message || "E-ticket and invoice emailed successfully!" }));
      setTimeout(() => {
        setEmailStatusByPnr((prev) => {
          const copy = { ...prev };
          delete copy[pnr];
          return copy;
        });
      }, 5000);
    } catch {
      setEmailStatusByPnr((prev) => ({ ...prev, [pnr]: "Failed to send email. Please try again." }));
    }
  };

  // Prevent background scrolling when cancel or terms modal is open
  useEffect(() => {
    if (selectedBookingForCancel || showTermsDialog) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = originalOverflow || "auto";
      };
    }
  }, [selectedBookingForCancel, showTermsDialog]);

  const { data: userBookingsData, isLoading: isUserBookingsLoading } = useGetMyBookingsQuery(
    {},
    { skip: !isAuthenticated }
  );

  const { data: pnrBooking, isLoading: isPnrLoading } = useGetBookingByPnrQuery(activePnr, {
    skip: !activePnr,
  });

  const [cancelBookingMutation, { isLoading: isCancelling }] = useCancelBookingMutation();

  const handlePnrSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (pnrSearch.trim()) {
      setActivePnr(pnrSearch.trim().toUpperCase());
    }
  };

  const handleCancelBooking = async () => {
    if (!selectedBookingForCancel) return;
    setCancelMessage("");

    try {
      const res = await cancelBookingMutation({
        pnr: selectedBookingForCancel.pnr,
        reason: cancelReason,
        refundDestination,
      }).unwrap();

      if (res.walletBalance !== undefined && activeUser) {
        dispatch(updateUser({ ...activeUser, walletBalance: res.walletBalance }));
      }

      dispatch(
        addNotification({
          id: `cancellation-${selectedBookingForCancel.pnr}-${Date.now()}`,
          type: "booking",
          title: `Cancellation Initiated (PNR: ${selectedBookingForCancel.pnr}) 🔄`,
          message:
            res.message ||
            `Cancellation requested for ${selectedBookingForCancel.sourceCity} → ${selectedBookingForCancel.destinationCity}. Queued for operator audit.`,
          timestamp: "Just now",
          actionUrl: "/my-bookings",
          actionLabel: "View Status",
          read: false,
          pnr: selectedBookingForCancel.pnr,
        })
      );

      setCancelMessage(res.message || "Cancellation requested! Refund queued for operator audit.");
      setSelectedBookingForCancel(null);
      setShowTermsDialog(false);
    } catch (err: any) {
      setCancelMessage(err?.data?.message || "Failed to cancel booking.");
    }
  };

  const rawBookings: BookingDetails[] = [];
  if (pnrBooking) {
    rawBookings.push(pnrBooking);
  } else if (userBookingsData?.content) {
    rawBookings.push(...userBookingsData.content);
  }

  // Automatically filter out expired bookings and pending payment bookings older than 10 minutes
  const now = Date.now();
  const bookingsList = rawBookings.filter((b) => {
    if (b.status === "EXPIRED") return false;
    if (b.status === "PENDING_PAYMENT") {
      const createdTime = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      if (createdTime > 0 && now - createdTime > 10 * 60 * 1000) {
        return false; // Removed automatically if pending payment > 10 mins
      }
    }
    return true;
  });

  const getDynamicRefundInfo = (booking: BookingDetails) => {
    const isFreeCancellation = Boolean(
      booking.hasFreeCancellation === true || (booking.hasFreeCancellation as any) === "true"
    );

    if (isFreeCancellation) {
      const freeFee = Number(booking.freeCancellationFee) || 0;
      const refund = Math.max(0, booking.totalAmount - freeFee);
      return {
        percentage: 100,
        refundAmount: refund,
        deductionAmount: freeFee,
        isFreeCancellation: true,
        label: "Free Cancellation Protection Active (100% Base Fare Refund)",
        slabs: [
          { timeRange: "Anytime before departure", refundPct: 100, feePct: 0, isActive: true },
        ],
      };
    }

    // Parse departure time safely (handling "HH:mm:ss", "HH:mm", etc.)
    let diffHours = 24;
    try {
      if (booking.travelDate) {
        let cleanTime = (booking.departureTime || "00:00:00").trim();
        if (/^\d{1,2}:\d{2}$/.test(cleanTime)) {
          cleanTime = `${cleanTime.padStart(5, "0")}:00`;
        }
        const isoStr = `${booking.travelDate}T${cleanTime}`;
        const depDate = new Date(isoStr);
        if (!isNaN(depDate.getTime())) {
          diffHours = (depDate.getTime() - Date.now()) / (1000 * 60 * 60);
        }
      }
    } catch {
      diffHours = 24;
    }

    let percentage = 90;
    let label = "> 24 hrs before trip (90% Refund)";
    let activeSlabIndex = 0;

    if (diffHours >= 24) {
      percentage = 90;
      label = "More than 24 hrs before trip (90% Refund)";
      activeSlabIndex = 0;
    } else if (diffHours >= 12) {
      percentage = 75;
      label = "12 to 24 hrs before trip (75% Refund)";
      activeSlabIndex = 1;
    } else if (diffHours >= 2) {
      percentage = 50;
      label = "2 to 12 hrs before trip (50% Refund)";
      activeSlabIndex = 2;
    } else {
      percentage = 25;
      label = "Within 2 hrs of departure (25% Refund)";
      activeSlabIndex = 3;
    }

    const slabs = [
      { timeRange: "More than 24 hrs before departure", refundPct: 90, feePct: 10, isActive: activeSlabIndex === 0 },
      { timeRange: "Between 12 to 24 hrs before departure", refundPct: 75, feePct: 25, isActive: activeSlabIndex === 1 },
      { timeRange: "Between 2 to 12 hrs before departure", refundPct: 50, feePct: 50, isActive: activeSlabIndex === 2 },
      { timeRange: "Within 2 hrs of departure", refundPct: 25, feePct: 75, isActive: activeSlabIndex === 3 },
    ];

    const refund = (booking.totalAmount * percentage) / 100;
    const deduction = Math.max(0, booking.totalAmount - refund);

    return {
      percentage,
      refundAmount: refund,
      deductionAmount: deduction,
      isFreeCancellation: false,
      label,
      slabs,
    };
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Title */}
      <div className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight flex items-center space-x-2">
          <Ticket className="w-7 h-7 text-[#d84e55]" />
          <span>My Bookings & Tickets</span>
        </h1>
        <p className="text-xs sm:text-sm text-gray-500 mt-1">
          View your confirmed reservations, download official e-tickets, or request instant refund cancellations.
        </p>
      </div>

      {/* PNR Quick Search Card with bottom-to-top lazy loading */}
      <motion.div
        initial={{ opacity: 0, y: 25 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-30px" }}
        transition={{ duration: 0.45, ease: "easeOut" }}
        className="bg-white rounded-3xl p-5 border border-gray-200 shadow-xs mb-8"
      >
        <form onSubmit={handlePnrSearch} className="flex flex-col sm:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
            <input
              type="text"
              value={pnrSearch}
              onChange={(e) => setPnrSearch(e.target.value)}
              placeholder="Search by PNR Number (e.g. RB-2026-X9K2L1)..."
              className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold text-gray-900 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
            />
          </div>
          <button
            type="submit"
            className="w-full sm:w-auto px-5 py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold transition-all shadow-xs shrink-0 cursor-pointer"
          >
            Find Ticket
          </button>
        </form>
      </motion.div>

      {cancelMessage && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center space-x-2 text-xs text-emerald-800"
        >
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{cancelMessage}</span>
        </motion.div>
      )}

      {/* Bookings List with bottom-to-top scroll transition */}
      <div className="space-y-4">
        {bookingsList.length > 0 ? (
          bookingsList.map((booking, idx) => {
            const isExpanded = !!expandedPnrs[booking.pnr];
            const walletUsed = Number(booking.walletAmountUsed) || 0;
            const discount = Number(booking.discountAmount) || 0;
            const totalPaid = Number(booking.totalAmount) || 0;
            const passengerCount = booking.passengers?.length || 1;
            const cancelFee = booking.hasFreeCancellation ? (Number(booking.freeCancellationFee) || 21 * passengerCount) : 0;
            const approxBaseFare = totalPaid + walletUsed + discount - cancelFee;
            const isWalletFull = walletUsed > 0 && totalPaid === 0;
            const isSplitPay = walletUsed > 0 && totalPaid > 0;

            return (
              <motion.div
                key={booking.pnr}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-40px" }}
                transition={{ duration: 0.45, ease: "easeOut", delay: Math.min(idx * 0.06, 0.3) }}
                className="bg-white rounded-3xl border border-gray-200 p-6 shadow-xs hover:shadow-md transition-all space-y-4"
              >
                {/* Header with Route & Status Badges */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-gray-100">
                  <div className="flex items-center space-x-3">
                    <div className="w-11 h-11 rounded-2xl bg-red-50 text-[#d84e55] flex items-center justify-center shrink-0">
                      <Bus className="w-6 h-6" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-black text-gray-700 bg-gray-100 px-2 py-0.5 rounded-md">
                          PNR: {booking.pnr}
                        </span>
                        {isWalletFull && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 flex items-center gap-1">
                            <Wallet className="w-3 h-3" /> 100% Paid with Wallet
                          </span>
                        )}
                        {isSplitPay && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 flex items-center gap-1">
                            <CreditCard className="w-3 h-3" /> Split Payment
                          </span>
                        )}
                      </div>
                      <div className="flex items-center space-x-1.5 text-lg font-black text-gray-900 mt-1">
                        <span>{booking.sourceCity}</span>
                        <span className="text-[#d84e55]">➔</span>
                        <span>{booking.destinationCity}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    {booking.hasFreeCancellation && (
                      <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1">
                        <ShieldCheck className="w-3.5 h-3.5" /> Free Cancellation Active
                      </span>
                    )}
                    {booking.couponCode && (
                      <span className="px-2.5 py-1 rounded-full text-[11px] font-mono font-bold bg-red-50 text-red-700 border border-red-200 flex items-center gap-1">
                        <Tag className="w-3 h-3" /> {booking.couponCode}
                      </span>
                    )}
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-bold ${
                        booking.status === "CONFIRMED"
                          ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                          : booking.status === "CANCELLED" || booking.status === "REFUNDED"
                          ? "bg-red-50 text-red-700 border border-red-200"
                          : "bg-amber-50 text-amber-700 border border-amber-200"
                      }`}
                    >
                      {booking.status}
                    </span>
                  </div>
                </div>

                {/* Primary Journey & Fare Details Grid */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs text-gray-600 bg-gray-50/70 p-4 rounded-2xl">
                  <div>
                    <span className="text-gray-400 block font-medium">Operator & Bus:</span>
                    <span className="font-bold text-gray-900 block">{booking.operatorName}</span>
                    <span className="text-[11px] text-gray-500">{booking.busType || "AC Sleeper / Seater"}</span>
                  </div>
                  <div>
                    <span className="text-gray-400 block font-medium">Travel Date & Time:</span>
                    <span className="font-bold text-gray-900 block">{booking.travelDate}</span>
                    <span className="text-[11px] text-gray-500">{booking.departureTime} departure</span>
                  </div>
                  <div>
                    <span className="text-gray-400 block font-medium">Seat Number(s):</span>
                    <span className="font-black text-[#d84e55] text-sm block">
                      {booking.passengers?.map((p) => p.seatNumber).join(", ") || "Confirmed"}
                    </span>
                    <span className="text-[11px] text-gray-500">{passengerCount} Passenger{passengerCount > 1 ? "s" : ""}</span>
                  </div>
                  <div>
                    <span className="text-gray-400 block font-medium">Total Paid:</span>
                    <span className="font-black text-gray-900 text-sm block">
                      ₹{totalPaid.toFixed(2)}
                      {isWalletFull && <span className="text-[11px] text-emerald-600 font-bold ml-1.5">(Wallet ₹{walletUsed.toFixed(2)})</span>}
                    </span>
                    {booking.refundAmount !== undefined && booking.refundAmount > 0 && (
                      <span className="text-[11px] text-emerald-600 font-bold block mt-0.5">
                        Refund: ₹{booking.refundAmount.toFixed(2)} ({booking.refundDestination === "ORIGINAL_PAYMENT" ? "Original Method" : "redBus Wallet"})
                      </span>
                    )}
                  </div>
                </div>

                {/* REAL-TIME PIPED REFUND STAGE TRACKER (When Cancelled / In Refund Pipeline) */}
                {(booking.status === "CANCELLED" || booking.status === "REFUNDED" || booking.refundStatus) && (() => {
                  const stage = booking.refundStage || (booking.status === "REFUNDED" ? "COMPLETED" : "OPERATOR_AUDIT");
                  const destination = booking.refundDestination || "WALLET";
                  const stages = [
                    { key: "REQUESTED", label: "1. Refund Initiated", desc: "Cancellation registered", time: booking.refundRequestedAt ? new Date(booking.refundRequestedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "Done" },
                    { key: "OPERATOR_AUDIT", label: "2. Operator Audit", desc: stage === "REQUESTED" ? "Queued for audit" : stage === "OPERATOR_AUDIT" ? "Operator reviewing ticket" : "Approved by operator", time: booking.refundApprovedAt ? new Date(booking.refundApprovedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "In Review" },
                    { key: "REFUND_PROCESSING", label: "3. Disbursing Payout", desc: destination === "WALLET" ? "Direct redBus Wallet transfer" : "Payment gateway banking transfer", time: stage === "COMPLETED" ? "Success" : "Active" },
                    { key: "COMPLETED", label: destination === "WALLET" ? "4. Credited to Wallet" : "4. Refund Settled", desc: destination === "WALLET" ? "Instant balance added" : "3-5 business days to source", time: stage === "COMPLETED" ? "Credited 🎉" : "Pending" },
                  ];

                  const getStepIndex = (s: string) => {
                    if (s === "REQUESTED") return 0;
                    if (s === "OPERATOR_AUDIT") return 1;
                    if (s === "REFUND_PROCESSING") return 2;
                    if (s === "COMPLETED" || booking.status === "REFUNDED") return 3;
                    return 1;
                  };

                  const currentIdx = getStepIndex(stage);

                  return (
                    <div className="p-4 rounded-2xl bg-gradient-to-r from-slate-900 via-gray-900 to-slate-950 text-white border border-slate-700/70 shadow-md space-y-3">
                      <div className="flex items-center justify-between flex-wrap gap-2 pb-2 border-b border-slate-800">
                        <div className="flex items-center space-x-2">
                          <span className="w-2.5 h-2.5 rounded-full bg-red-500 animate-ping" />
                          <span className="text-xs font-black uppercase tracking-wider text-red-400">
                            Live Refund Pipeline Tracker
                          </span>
                        </div>
                        <div className="flex items-center space-x-2">
                          <span className="text-[11px] text-slate-300 font-medium">Payout Destination:</span>
                          <span className="text-[11px] font-bold px-2 py-0.5 rounded-md bg-red-500/20 text-red-300 border border-red-500/30 flex items-center gap-1">
                            {destination === "WALLET" ? <Wallet className="w-3 h-3" /> : <CreditCard className="w-3 h-3" />}
                            {destination === "WALLET" ? "redBus Wallet (Instant)" : "Original Payment Method"}
                          </span>
                        </div>
                      </div>

                      {/* Piped Stage Horizontal Flow */}
                      <div className="grid grid-cols-1 sm:grid-cols-4 gap-2 pt-1">
                        {stages.map((st, sIdx) => {
                          const isPast = sIdx < currentIdx;
                          const isCurrent = sIdx === currentIdx;
                          const isFuture = sIdx > currentIdx;

                          return (
                            <div
                              key={st.key}
                              className={`p-3 rounded-xl border transition-all relative ${
                                isCurrent
                                  ? "bg-red-950/60 border-red-500/80 shadow-md shadow-red-500/20"
                                  : isPast
                                  ? "bg-slate-800/80 border-emerald-500/50"
                                  : "bg-slate-900/40 border-slate-800 opacity-60"
                              }`}
                            >
                              <div className="flex items-center justify-between mb-1.5">
                                <span className={`text-[10px] font-black uppercase tracking-wider ${isCurrent ? "text-red-400" : isPast ? "text-emerald-400" : "text-slate-500"}`}>
                                  {isPast ? "✓ Verified" : isCurrent ? "⚡ Live Stage" : "Upcoming"}
                                </span>
                                <span className="text-[10px] font-mono text-slate-400">{st.time}</span>
                              </div>
                              <p className={`text-xs font-bold ${isCurrent ? "text-white" : isPast ? "text-slate-100" : "text-slate-400"}`}>
                                {st.label}
                              </p>
                              <p className="text-[10px] text-slate-400 mt-0.5 leading-tight">
                                {st.desc}
                              </p>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  );
                })()}

                {/* Expandable Invoice & Passenger Detailing Section */}
                <div>
                  <button
                    type="button"
                    onClick={() => toggleExpand(booking.pnr)}
                    className="w-full py-2 px-3 text-xs font-bold text-gray-600 hover:text-gray-900 bg-gray-100 hover:bg-gray-200/80 rounded-xl transition-all flex items-center justify-between cursor-pointer"
                  >
                    <span className="flex items-center gap-1.5">
                      <Ticket className="w-3.5 h-3.5 text-[#d84e55]" />
                      <span>{isExpanded ? "Hide Full Invoice & Passenger List" : "View Itemized Tax Invoice & Passenger List"}</span>
                    </span>
                    {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                  </button>

                  <AnimatePresence>
                    {isExpanded && (
                      <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: "auto" }}
                        exit={{ opacity: 0, height: 0 }}
                        transition={{ duration: 0.3 }}
                        className="overflow-hidden pt-3 space-y-4"
                      >
                        {/* Boarding & Dropping Point Box */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-3.5 bg-white border border-gray-200 rounded-2xl text-xs">
                          <div className="flex items-start space-x-2">
                            <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                            <div>
                              <span className="font-bold text-gray-900 block">Boarding Point</span>
                              <p className="text-gray-600 mt-0.5">{booking.boardingPoint || `${booking.sourceCity} Main Bus Stand`}</p>
                            </div>
                          </div>
                          <div className="flex items-start space-x-2">
                            <MapPin className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
                            <div>
                              <span className="font-bold text-gray-900 block">Dropping Point</span>
                              <p className="text-gray-600 mt-0.5">{booking.droppingPoint || `${booking.destinationCity} Drop Location`}</p>
                            </div>
                          </div>
                        </div>

                        {/* Passenger Table */}
                        <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white">
                          <div className="px-4 py-2 bg-gray-50 border-b border-gray-200 font-bold text-xs text-gray-700">
                            Passenger Details
                          </div>
                          <table className="w-full text-left text-xs">
                            <thead className="bg-gray-50/50 text-gray-400 font-semibold border-b border-gray-100">
                              <tr>
                                <th className="px-4 py-2">Seat</th>
                                <th className="px-4 py-2">Name</th>
                                <th className="px-4 py-2">Age / Gender</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                              {booking.passengers?.map((p, pIdx) => (
                                <tr key={pIdx} className="hover:bg-gray-50/50">
                                  <td className="px-4 py-2 font-black text-[#d84e55]">{p.seatNumber}</td>
                                  <td className="px-4 py-2 font-semibold text-gray-900">{p.name}</td>
                                  <td className="px-4 py-2 text-gray-600">{p.age} yrs • {p.gender}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>

                        {/* Itemized Financial & Tax Invoice Breakdown */}
                        <div className="p-4 bg-gray-50 rounded-2xl border border-gray-200 text-xs space-y-2">
                          <span className="font-black text-gray-900 block uppercase tracking-wider text-[11px]">
                            Tax Invoice & Payment Breakdown
                          </span>
                          <div className="flex justify-between text-gray-600">
                            <span>Base Ticket Price ({passengerCount} seat{passengerCount > 1 ? "s" : ""})</span>
                            <span className="font-semibold text-gray-900">₹{approxBaseFare.toFixed(2)}</span>
                          </div>
                          {booking.hasFreeCancellation && (
                            <div className="flex justify-between text-emerald-700">
                              <span className="flex items-center gap-1">
                                <ShieldCheck className="w-3.5 h-3.5" /> Free Cancellation Protection
                              </span>
                              <span className="font-bold">+₹{cancelFee.toFixed(2)}</span>
                            </div>
                          )}
                          {discount > 0 && (
                            <div className="flex justify-between text-emerald-600 font-bold">
                              <span className="flex items-center gap-1">
                                <Tag className="w-3.5 h-3.5" /> Coupon Discount ({booking.couponCode})
                              </span>
                              <span>-₹{discount.toFixed(2)}</span>
                            </div>
                          )}
                          {walletUsed > 0 && (
                            <div className="flex justify-between text-emerald-600 font-bold">
                              <span className="flex items-center gap-1">
                                <Wallet className="w-3.5 h-3.5" /> redBus Wallet Deducted
                              </span>
                              <span>-₹{walletUsed.toFixed(2)}</span>
                            </div>
                          )}
                          <div className="flex justify-between text-gray-600">
                            <span>Operator Service Fee & GST</span>
                            <span className="font-bold text-emerald-600">FREE (₹0.00)</span>
                          </div>
                          <div className="pt-2 border-t border-gray-200 flex justify-between items-center text-sm">
                            <div>
                              <span className="font-black text-gray-900 block">Final Gateway Paid</span>
                              <span className="text-[11px] text-gray-500 font-normal">
                                {isWalletFull
                                  ? "100% Paid via redBus Wallet Balance"
                                  : isSplitPay
                                  ? `Wallet: ₹${walletUsed.toFixed(2)} + Gateway: ₹${totalPaid.toFixed(2)}`
                                  : "Online Payment (Razorpay UPI / Cards)"}
                              </span>
                            </div>
                            <span className="font-black text-xl text-[#d84e55]">₹{totalPaid.toFixed(2)}</span>
                          </div>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>

                {/* Email feedback status */}
                {emailStatusByPnr[booking.pnr] && (
                  <div className="p-2.5 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-700 flex items-center space-x-2">
                    <CheckCircle2 className="w-4 h-4 text-blue-500 shrink-0" />
                    <span>{emailStatusByPnr[booking.pnr]}</span>
                  </div>
                )}

                {/* Actions Bar: Download PDF, Email Ticket & Cancel */}
                <div className="pt-3 border-t border-gray-100 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex flex-wrap items-center gap-2">
                    {booking.status === "CONFIRMED" ? (
                      <>
                        <a
                          href={`http://localhost:8080/api/bookings/${booking.pnr}/ticket-pdf`}
                          target="_blank"
                          rel="noreferrer"
                          className="px-4 py-2 bg-gray-900 hover:bg-black text-white rounded-xl text-xs font-bold transition-all flex items-center space-x-1.5 shadow-sm"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>Download PDF Ticket & Invoice</span>
                        </a>
                        <button
                          type="button"
                          disabled={isEmailSending}
                          onClick={() => handleSendEmail(booking.pnr)}
                          className="px-3.5 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 rounded-xl text-xs font-bold transition-all flex items-center space-x-1.5 cursor-pointer disabled:opacity-50"
                        >
                          <Mail className="w-3.5 h-3.5 text-gray-600" />
                          <span>Email Ticket</span>
                        </button>
                      </>
                    ) : booking.status === "PENDING_PAYMENT" ? (
                      <Link
                        href={`/checkout?routeId=${booking.routeId || 1}`}
                        className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-xs font-bold transition-all flex items-center space-x-1.5"
                      >
                        <Clock className="w-3.5 h-3.5" />
                        <span>Complete Payment</span>
                      </Link>
                    ) : (
                      <span className="text-xs text-gray-400 font-medium">Ticket {booking.status.toLowerCase()}</span>
                    )}
                  </div>

                  {booking.status === "CONFIRMED" && (
                    <button
                      type="button"
                      onClick={() => setSelectedBookingForCancel(booking)}
                      className="px-4 py-2 bg-red-50 hover:bg-red-100 text-red-600 rounded-xl text-xs font-bold transition-all cursor-pointer"
                    >
                      Cancel Booking & Refund
                    </button>
                  )}
                </div>
              </motion.div>
            );
          })
        ) : (
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.45 }}
            className="bg-white rounded-3xl p-12 text-center border border-gray-200"
          >
            <Ticket className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-base font-bold text-gray-800">No active bookings found</p>
            <p className="text-xs text-gray-500 mt-1">
              Enter your PNR above or book a new ticket to get started.
            </p>
            <Link
              href="/"
              className="mt-4 inline-block px-5 py-2.5 bg-[#d84e55] text-white rounded-xl text-xs font-bold"
            >
              Search Buses Now
            </Link>
          </motion.div>
        )}
      </div>

      {/* Dynamic Cancellation Confirmation Modal */}
      {selectedBookingForCancel && (() => {
        const refundInfo = getDynamicRefundInfo(selectedBookingForCancel);
        return (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setSelectedBookingForCancel(null)}
              className="fixed inset-0 bg-black/60 backdrop-blur-xs cursor-pointer"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.94, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.94, y: 20 }}
              transition={{ type: "spring", damping: 25, stiffness: 320 }}
              className="bg-white rounded-3xl max-w-lg w-full p-6 space-y-4 shadow-2xl border border-gray-100 max-h-[90vh] overflow-y-auto relative z-10"
            >
              <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                <div>
                  <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
                    <RotateCcw className="w-4 h-4 text-[#d84e55]" />
                    <span>Cancel Ticket ({selectedBookingForCancel.pnr})</span>
                  </h3>
                  <span className="text-xs text-gray-500 font-medium">
                    {selectedBookingForCancel.sourceCity} ➔ {selectedBookingForCancel.destinationCity} ({selectedBookingForCancel.operatorName})
                  </span>
                </div>
                <span className="text-xs font-mono font-bold px-2.5 py-1 bg-gray-100 rounded-lg text-gray-700 shrink-0">
                  {selectedBookingForCancel.travelDate}
                </span>
              </div>

              {/* Cancellation Policy Table & Slabs */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-gray-800 uppercase tracking-wider">
                    Official Cancellation Policy
                  </span>
                  {refundInfo.isFreeCancellation && (
                    <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                      🛡️ Free Cancellation Applied
                    </span>
                  )}
                </div>

                <div className="rounded-xl border border-gray-200 overflow-hidden text-xs">
                  <table className="w-full text-left">
                    <thead className="bg-gray-50 text-gray-500 font-semibold border-b border-gray-200">
                      <tr>
                        <th className="px-3 py-2">Time of Cancellation</th>
                        <th className="px-3 py-2 text-center">Refund %</th>
                        <th className="px-3 py-2 text-right">Fee %</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100 font-medium">
                      {refundInfo.slabs.map((slab, idx) => (
                        <tr
                          key={idx}
                          className={`${
                            slab.isActive
                              ? "bg-emerald-50/80 font-bold text-emerald-900"
                              : "text-gray-600 hover:bg-gray-50/50"
                          }`}
                        >
                          <td className="px-3 py-2 flex items-center gap-1.5">
                            {slab.isActive && (
                              <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 shrink-0" />
                            )}
                            <span>{slab.timeRange}</span>
                            {slab.isActive && (
                              <span className="ml-1 text-[10px] uppercase tracking-wide bg-emerald-600 text-white px-1.5 py-0.2 rounded font-bold">
                                Current
                              </span>
                            )}
                          </td>
                          <td className="px-3 py-2 text-center font-bold text-emerald-700">
                            {slab.refundPct}%
                          </td>
                          <td className="px-3 py-2 text-right text-red-600">
                            {slab.feePct}%
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Dynamic Refund Calculation Breakdown */}
              <div className="p-4 rounded-2xl bg-gray-50 border border-gray-200 space-y-2 text-xs">
                <div className="flex items-center justify-between pb-2 border-b border-gray-200">
                  <span className="font-semibold text-gray-700">Total Paid Fare:</span>
                  <span className="font-mono font-bold text-gray-900">
                    ₹{selectedBookingForCancel.totalAmount.toFixed(2)}
                  </span>
                </div>

                <div className="flex items-center justify-between text-red-600">
                  <span>
                    Cancellation Charges ({100 - refundInfo.percentage}%):
                  </span>
                  <span className="font-mono font-bold">
                    -₹{refundInfo.deductionAmount.toFixed(2)}
                  </span>
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-gray-200 text-emerald-700 font-bold text-sm">
                  <span className="flex items-center gap-1.5">
                    <Wallet className="w-4 h-4 text-emerald-600" />
                    <span>Estimated Refund Amount:</span>
                  </span>
                  <span className="font-mono text-base font-black">
                    ₹{refundInfo.refundAmount.toFixed(2)}
                  </span>
                </div>
              </div>

              {/* Choose Refund Destination */}
              <div>
                <label className="block text-xs font-bold text-gray-800 mb-1.5">
                  Where would you like to receive your refund? *
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRefundDestination("WALLET")}
                    className={`p-3 rounded-2xl border text-left transition-all cursor-pointer flex flex-col justify-between ${
                      refundDestination === "WALLET"
                        ? "border-emerald-500 bg-emerald-50/80 ring-2 ring-emerald-500/20 shadow-xs"
                        : "border-gray-200 bg-white hover:bg-gray-50"
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="flex items-center gap-1.5 text-xs font-black text-emerald-800">
                        <Wallet className="w-4 h-4 text-emerald-600" />
                        <span>redBus Wallet</span>
                      </span>
                      <span className="px-1.5 py-0.5 rounded text-[9px] font-black uppercase tracking-wider bg-emerald-100 text-emerald-800 border border-emerald-300">
                        Recommended (Instant)
                      </span>
                    </div>
                    <p className="text-[11px] text-gray-500 leading-tight">
                      Instant balance upon operator audit approval. 100% usable on any bus ticket.
                    </p>
                  </button>

                  <button
                    type="button"
                    onClick={() => setRefundDestination("ORIGINAL_PAYMENT")}
                    className={`p-3 rounded-2xl border text-left transition-all cursor-pointer flex flex-col justify-between ${
                      refundDestination === "ORIGINAL_PAYMENT"
                        ? "border-blue-500 bg-blue-50/80 ring-2 ring-blue-500/20 shadow-xs"
                        : "border-gray-200 bg-white hover:bg-gray-50"
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="flex items-center gap-1.5 text-xs font-black text-blue-900">
                        <CreditCard className="w-4 h-4 text-blue-600" />
                        <span>Original Method</span>
                      </span>
                      <span className="px-1.5 py-0.5 rounded text-[9px] font-black uppercase tracking-wider bg-blue-100 text-blue-800 border border-blue-300">
                        3-5 Days
                      </span>
                    </div>
                    <p className="text-[11px] text-gray-500 leading-tight">
                      Settlement returned to original bank/card/UPI account within 3-5 business days.
                    </p>
                  </button>
                </div>
              </div>

              {/* Reason Selector */}
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Reason for cancellation *
                </label>
                <select
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold text-gray-800 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                >
                  <option value="Change of plans">Change of plans</option>
                  <option value="Booked by mistake">Booked by mistake</option>
                  <option value="Found alternative transport">Found alternative transport</option>
                  <option value="Medical emergency">Medical emergency</option>
                  <option value="Trip postponed">Trip postponed</option>
                </select>
              </div>

              {/* Actions */}
              <div className="flex space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => {
                    setSelectedBookingForCancel(null);
                    setShowTermsDialog(false);
                  }}
                  className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl text-xs font-bold transition-all cursor-pointer"
                >
                  Keep Booking
                </button>
                <button
                  type="button"
                  disabled={isCancelling}
                  onClick={() => {
                    setShowTermsDialog(true);
                    setAgreeToTerms(false);
                  }}
                  className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold transition-all shadow-sm cursor-pointer disabled:opacity-50 flex items-center justify-center gap-1.5"
                >
                  <span>Review Terms & Cancel (₹{refundInfo.refundAmount.toFixed(2)})</span>
                </button>
              </div>
            </motion.div>
          </div>
        );
      })()}

      {/* ATTENTION-SEEKING TERMS AND CONDITIONS REVIEW DIALOG MODAL */}
      {showTermsDialog && selectedBookingForCancel && (() => {
        const refundInfo = getDynamicRefundInfo(selectedBookingForCancel);
        const seatNumbers =
          selectedBookingForCancel.passengers?.map((p) => p.seatNumber).join(", ") || "Selected Seats";

        return (
          <div className="fixed inset-0 z-60 flex items-center justify-center p-3 sm:p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowTermsDialog(false)}
              className="fixed inset-0 bg-black/80 backdrop-blur-md cursor-pointer"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.94, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.94, y: 20 }}
              transition={{ type: "spring", damping: 25, stiffness: 320 }}
              className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-2xl border-2 border-red-500/40 space-y-5 relative z-10 max-h-[90vh] overflow-y-auto"
            >
              {/* Header with High-Attention Pulsing Warning Icon */}
              <div className="flex items-start justify-between border-b border-gray-100 pb-4">
                <div className="flex items-center space-x-3.5">
                  <div className="w-12 h-12 rounded-2xl bg-red-100 border border-red-300 text-red-600 flex items-center justify-center shadow-lg shadow-red-500/20 shrink-0">
                    <ShieldAlert className="w-6 h-6 animate-pulse" />
                  </div>
                  <div>
                    <h3 className="font-black text-lg text-gray-900 tracking-tight flex items-center gap-1.5">
                      <span>Review Cancellation Terms</span>
                    </h3>
                    <p className="text-xs font-bold text-red-600">
                      ⚠️ Final confirmation required to release seats
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setShowTermsDialog(false)}
                  className="p-1.5 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-700 cursor-pointer transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Trip Capsule */}
              <div className="bg-gray-50 p-3.5 rounded-2xl border border-gray-200 flex items-center justify-between text-xs">
                <div>
                  <span className="font-extrabold text-sm text-gray-900 block">
                    {selectedBookingForCancel.sourceCity} ➔ {selectedBookingForCancel.destinationCity}
                  </span>
                  <span className="text-gray-500 text-[11px] font-medium">
                    {selectedBookingForCancel.operatorName} • PNR:{" "}
                    <strong className="text-gray-800">{selectedBookingForCancel.pnr}</strong>
                  </span>
                </div>
                <div className="text-right">
                  <span className="px-2 py-0.5 bg-red-100 text-red-700 font-extrabold text-[10px] rounded-md block">
                    Seat(s): {seatNumbers}
                  </span>
                  <span className="text-[10px] text-gray-400 mt-0.5 block">
                    {selectedBookingForCancel.travelDate}
                  </span>
                </div>
              </div>

              {/* High-Attention Warnings & Financial Breakdown */}
              <div className="space-y-3 text-xs">
                {/* 1. Irreversible Warning */}
                <div className="p-3 bg-red-50/90 border border-red-200 rounded-2xl flex items-start space-x-2.5 text-red-900 shadow-2xs">
                  <AlertTriangle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                  <div className="leading-relaxed">
                    <strong className="font-black text-red-700">Irreversible Action: </strong>
                    Your reserved seats (<strong>{seatNumbers}</strong>) will be instantly returned to the public booking network and can be reserved by other passengers immediately.
                  </div>
                </div>

                {/* 2. Refund Amount Card */}
                <div className="p-4 bg-gradient-to-br from-emerald-50 via-teal-50/40 to-white rounded-2xl border-2 border-emerald-300 space-y-2">
                  <div className="flex items-center justify-between text-gray-700">
                    <span className="font-semibold">Total Fare Paid:</span>
                    <span className="font-mono font-bold text-gray-900">
                      ₹{selectedBookingForCancel.totalAmount.toFixed(2)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-red-600 text-xs">
                    <span>Cancellation Fee / Service Deduction:</span>
                    <span className="font-mono font-bold">
                      -₹{refundInfo.deductionAmount.toFixed(2)}
                    </span>
                  </div>
                  <div className="pt-2 border-t border-emerald-200 flex items-center justify-between">
                    <span className="font-extrabold text-xs text-emerald-900 flex items-center gap-1.5">
                      <Wallet className="w-4 h-4 text-emerald-600" />
                      <span>Net Refund Credited to redBus Wallet:</span>
                    </span>
                    <span className="font-mono text-lg font-black text-emerald-700">
                      ₹{refundInfo.refundAmount.toFixed(2)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Terms and Conditions Scrollable Review Box */}
              <div className="p-3.5 bg-gray-50 rounded-2xl border border-gray-200 text-[11px] text-gray-600 space-y-2 max-h-36 overflow-y-auto">
                <p className="font-extrabold text-gray-900 text-xs flex items-center gap-1.5">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Cancellation Terms & Conditions:</span>
                </p>
                <div className="flex items-start gap-2">
                  <span className="text-[#d84e55] font-bold">1.</span>
                  <span>
                    <strong>Immediate Release:</strong> Seats are surrendered upon confirmation. Rebooking same seats is not guaranteed and requires a fresh transaction.
                  </span>
                </div>
                <div className="flex items-start gap-2">
                  <span className="text-[#d84e55] font-bold">2.</span>
                  <span>
                    <strong>Instant Wallet Credit:</strong> The net refund of <strong>₹{refundInfo.refundAmount.toFixed(2)}</strong> is deposited directly into your redBus Wallet without waiting for banking settlement.
                  </span>
                </div>
                <div className="flex items-start gap-2">
                  <span className="text-[#d84e55] font-bold">3.</span>
                  <span>
                    <strong>Confirmation Voucher:</strong> A digital cancellation receipt with cancelled status will be dispatched to <strong>{selectedBookingForCancel.contactEmail || "your email"}</strong>.
                  </span>
                </div>
                <div className="flex items-start gap-2">
                  <span className="text-[#d84e55] font-bold">4.</span>
                  <span>
                    <strong>Non-Refundable Add-ons:</strong> Free cancellation protection add-on fees and convenience charges are non-refundable.
                  </span>
                </div>
              </div>

              {/* Mandatory Agreement Checkbox */}
              <label className="flex items-start space-x-2.5 p-3 rounded-xl border border-amber-300 bg-amber-50/80 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={agreeToTerms}
                  onChange={(e) => setAgreeToTerms(e.target.checked)}
                  className="mt-0.5 w-4 h-4 text-[#d84e55] rounded border-gray-300 focus:ring-[#d84e55] cursor-pointer"
                />
                <span className="text-xs font-bold text-gray-800 leading-snug">
                  I have reviewed and agree to the Cancellation Terms & Conditions and authorize the immediate release of my seats.
                </span>
              </label>

              {/* Action Buttons */}
              <div className="flex space-x-2.5 pt-1">
                <button
                  type="button"
                  onClick={() => setShowTermsDialog(false)}
                  className="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl text-xs font-extrabold transition-all cursor-pointer"
                >
                  ← Keep Booking
                </button>
                <button
                  type="button"
                  disabled={!agreeToTerms || isCancelling}
                  onClick={handleCancelBooking}
                  className="flex-1 py-3 bg-red-600 hover:bg-red-700 disabled:bg-gray-200 disabled:text-gray-400 text-white rounded-xl text-xs font-black transition-all shadow-md shadow-red-500/20 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center gap-1.5 active:scale-95"
                >
                  {isCancelling ? (
                    <span>Processing Refund...</span>
                  ) : (
                    <span>Accept Terms & Cancel (₹{refundInfo.refundAmount.toFixed(2)})</span>
                  )}
                </button>
              </div>
            </motion.div>
          </div>
        );
      })()}
    </div>
  );
}
