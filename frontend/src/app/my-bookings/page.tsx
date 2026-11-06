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
  useChangeTripPointsMutation,
  useChangeJourneyDateMutation,
  useGetAvailableDatesForBookingQuery,
  useCreateReviewMutation,
  useGetMeQuery,
  API_BASE_URL,
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
  RefreshCw,
  Star,
  Calendar,
  Share2,
  Info,
  Navigation,
  Coffee,
  Bed,
  BusFront,
  ArrowRight,
} from "lucide-react";
import { getRealtimeTripStatus } from "@/lib/dateUtils";

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
  const [expandedRefundTrackerPnrs, setExpandedRefundTrackerPnrs] = useState<Record<string, boolean>>({});
  const [selectedRefundTrackingBooking, setSelectedRefundTrackingBooking] = useState<BookingDetails | null>(null);

  // Enhanced Journey Modal & Action States
  const [selectedBookingForJourney, setSelectedBookingForJourney] = useState<BookingDetails | null>(null);
  const [journeyTab, setJourneyTab] = useState<"TICKET" | "BOARDING" | "DROPPING" | "REST_STOPS">("TICKET");
  const [selectedBookingForSeatLayout, setSelectedBookingForSeatLayout] = useState<BookingDetails | null>(null);
  const [selectedBookingForChangePoints, setSelectedBookingForChangePoints] = useState<BookingDetails | null>(null);
  const [newBoardingPoint, setNewBoardingPoint] = useState("");
  const [newDroppingPoint, setNewDroppingPoint] = useState("");
  const [changePointsError, setChangePointsError] = useState("");
  const [changePointsSuccess, setChangePointsSuccess] = useState("");

  const [selectedBookingForChangeDate, setSelectedBookingForChangeDate] = useState<BookingDetails | null>(null);
  const [newJourneyDate, setNewJourneyDate] = useState("");
  const [selectedNewSeats, setSelectedNewSeats] = useState<string[]>([]);
  const [showConfirmDateModal, setShowConfirmDateModal] = useState(false);
  const [changeDateError, setChangeDateError] = useState("");
  const [changeDateSuccess, setChangeDateSuccess] = useState("");

  const { data: availableDatesData, isFetching: isLoadingAvailableDates } = useGetAvailableDatesForBookingQuery(
    selectedBookingForChangeDate?.pnr || "",
    { skip: !selectedBookingForChangeDate?.pnr }
  );

  const [selectedBookingForReview, setSelectedBookingForReview] = useState<BookingDetails | null>(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState("");
  const [reviewedPnrs, setReviewedPnrs] = useState<Record<string, boolean>>({});
  const [reviewSuccess, setReviewSuccess] = useState("");
  const [copyPnrFeedback, setCopyPnrFeedback] = useState(false);

  useEffect(() => {
    try {
      const saved = localStorage.getItem("redbus_reviewed_pnrs");
      if (saved) {
        setReviewedPnrs(JSON.parse(saved));
      }
    } catch {}
  }, []);



  const [changeTripPointsMutation, { isLoading: isChangingPoints }] = useChangeTripPointsMutation();
  const [changeJourneyDateMutation, { isLoading: isChangingDate }] = useChangeJourneyDateMutation();
  const [createReviewMutation, { isLoading: isSubmittingReview }] = useCreateReviewMutation();

  const isWithinTwoHoursOfDeparture = (booking: BookingDetails): boolean => {
    try {
      if (!booking.travelDate) return false;
      let cleanTime = (booking.departureTime || "00:00:00").trim();
      if (/^\d{1,2}:\d{2}$/.test(cleanTime)) {
        cleanTime = `${cleanTime.padStart(5, "0")}:00`;
      }
      const isoStr = `${booking.travelDate}T${cleanTime}`;
      const depTime = new Date(isoStr).getTime();
      if (isNaN(depTime)) return false;
      return depTime - Date.now() <= 2 * 60 * 60 * 1000;
    } catch {
      return false;
    }
  };

  const toggleExpand = (pnr: string) => {
    setExpandedPnrs((prev) => ({ ...prev, [pnr]: !prev[pnr] }));
  };

  const toggleRefundTracker = (pnr: string) => {
    setExpandedRefundTrackerPnrs((prev) => ({ ...prev, [pnr]: !prev[pnr] }));
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

  // Prevent background scrolling when modals are open
  useEffect(() => {
    if (
      selectedBookingForCancel ||
      showTermsDialog ||
      selectedBookingForJourney ||
      selectedBookingForSeatLayout ||
      selectedBookingForChangePoints ||
      selectedBookingForChangeDate ||
      selectedBookingForReview
    ) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = originalOverflow || "auto";
      };
    }
  }, [
    selectedBookingForCancel,
    showTermsDialog,
    selectedBookingForJourney,
    selectedBookingForSeatLayout,
    selectedBookingForChangePoints,
    selectedBookingForChangeDate,
    selectedBookingForReview,
  ]);

  const { data: userBookingsData, isLoading: isUserBookingsLoading, refetch: refetchBookings } = useGetMyBookingsQuery(
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

      setExpandedRefundTrackerPnrs((prev) => ({
        ...prev,
        [selectedBookingForCancel.pnr]: true,
      }));

      setCancelMessage(res.message || "Cancellation requested! Refund queued for operator audit.");
      setSelectedBookingForCancel(null);
      setShowTermsDialog(false);
    } catch (err: any) {
      setCancelMessage(err?.data?.message || "Failed to cancel booking.");
    }
  };

  useEffect(() => {
    if (userBookingsData?.content && userBookingsData.content.length > 0) {
      setReviewedPnrs((prev) => {
        let changed = false;
        const next = { ...prev };
        userBookingsData.content.forEach((b: BookingDetails) => {
          if (b.userRating != null && !next[b.pnr]) {
            next[b.pnr] = true;
            changed = true;
          }
        });
        if (changed) {
          try {
            localStorage.setItem("redbus_reviewed_pnrs", JSON.stringify(next));
          } catch {}
          return next;
        }
        return prev;
      });
    }
  }, [userBookingsData]);

  const rawBookings: BookingDetails[] = [];
  if (pnrBooking) {
    rawBookings.push(pnrBooking);
  } else if (isAuthenticated && userBookingsData?.content) {
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
            const tripStatus = getRealtimeTripStatus(booking.travelDate, booking.departureTime, booking.arrivalTime, booking.status);

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

                    {/* Real-time Dynamic Trip Status Badge */}
                    {tripStatus.isLive ? (
                      <span className="px-3 py-1 rounded-full text-xs font-black bg-red-500/10 text-red-600 border border-red-500/30 flex items-center gap-1.5 shadow-xs">
                        <span className="relative flex h-2 w-2">
                          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-500 opacity-75"></span>
                          <span className="relative inline-flex rounded-full h-2 w-2 bg-red-600"></span>
                        </span>
                        <span>Trip Started</span>
                      </span>
                    ) : tripStatus.isCompleted ? (
                      <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-emerald-600"></span>
                        <span>Trip Completed</span>
                      </span>
                    ) : (
                      <span
                        className={`px-3 py-1 rounded-full text-xs font-bold flex items-center gap-1.5 ${
                          booking.status === "CONFIRMED"
                            ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                            : booking.status === "CANCELLED" || booking.status === "REFUNDED"
                            ? "bg-red-50 text-red-700 border border-red-200"
                            : "bg-amber-50 text-amber-700 border border-amber-200"
                        }`}
                      >
                        <span
                          className={`w-2 h-2 rounded-full ${
                            booking.status === "CONFIRMED"
                              ? "bg-emerald-500"
                              : booking.status === "CANCELLED" || booking.status === "REFUNDED"
                              ? "bg-red-500"
                              : "bg-amber-500"
                          }`}
                        />
                        <span>{booking.status}</span>
                      </span>
                    )}
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
                    {(booking.status === "CANCELLED" || booking.status === "REFUNDED") && booking.refundAmount !== undefined && booking.refundAmount > 0 && (
                      <span className="text-[11px] text-emerald-600 font-bold block mt-0.5">
                        Refund: ₹{booking.refundAmount.toFixed(2)} ({booking.refundDestination === "ORIGINAL_PAYMENT" ? "Original Method" : "redBus Wallet"})
                      </span>
                    )}
                  </div>
                </div>

                {/* REAL-TIME PIPED REFUND STAGE TRACKER (ONLY when Cancelled/Refunded AND toggled by Passenger) */}
                {(booking.status === "CANCELLED" || booking.status === "REFUNDED") && (
                  <AnimatePresence>
                    {expandedRefundTrackerPnrs[booking.pnr] && (() => {
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
                        <motion.div
                          initial={{ opacity: 0, height: 0 }}
                          animate={{ opacity: 1, height: "auto" }}
                          exit={{ opacity: 0, height: 0 }}
                          transition={{ duration: 0.35, ease: "easeOut" }}
                          className="overflow-hidden"
                        >
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
                        </motion.div>
                      );
                    })()}
                  </AnimatePresence>
                )}

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
                            {Number(booking.serviceFee || 0) > 0 ? (
                              <span className="font-bold text-gray-900">+₹{Number(booking.serviceFee).toFixed(2)}</span>
                            ) : (
                              <span className="font-bold text-emerald-600">FREE (₹0.00)</span>
                            )}
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

                {/* Actions Bar: Download PDF, Email Ticket, Cancel, and See Refund Status */}
                <div className="pt-3 border-t border-gray-100 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex flex-wrap items-center gap-2">
                    {booking.status === "CONFIRMED" ? (
                      <>
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedBookingForJourney(booking);
                            setJourneyTab("TICKET");
                          }}
                          className="px-4 py-2 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold transition-all flex items-center space-x-1.5 shadow-sm shadow-red-500/20 cursor-pointer"
                        >
                          <Ticket className="w-3.5 h-3.5" />
                          <span>View Ticket</span>
                        </button>
                        <a
                          href={`${API_BASE_URL}/bookings/${booking.pnr}/ticket-pdf`}
                          target="_blank"
                          rel="noreferrer"
                          className="px-4 py-2 bg-gray-900 hover:bg-black text-white rounded-xl text-xs font-bold transition-all flex items-center space-x-1.5 shadow-sm"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>PDF Ticket</span>
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
                      <span className="text-xs font-bold text-red-600 bg-red-50 px-2.5 py-1 rounded-lg border border-red-100 flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-red-500" />
                        <span>Ticket Cancelled ({booking.refundDestination === "ORIGINAL_PAYMENT" ? "Original Payment" : "Wallet Refund"})</span>
                      </span>
                    )}
                  </div>

                  {booking.status === "CONFIRMED" && (
                    tripStatus.isLive ? (
                      <span className="px-3.5 py-2 bg-red-50 text-red-600 rounded-xl text-xs font-bold flex items-center gap-1.5 border border-red-200">
                        <span className="relative flex h-2 w-2">
                          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-500 opacity-75"></span>
                          <span className="relative inline-flex rounded-full h-2 w-2 bg-red-600"></span>
                        </span>
                        <span>Trip in Progress</span>
                      </span>
                    ) : tripStatus.isCompleted ? (
                      <div className="flex items-center gap-2">
                        <span className="px-3.5 py-2 bg-emerald-50 text-emerald-700 rounded-xl text-xs font-bold flex items-center gap-1.5 border border-emerald-200">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                          <span>Journey Completed</span>
                        </span>
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedBookingForReview(booking);
                            setReviewRating(5);
                            setReviewComment("");
                            setReviewSuccess("");
                          }}
                          className="px-3.5 py-2 bg-amber-50 hover:bg-amber-100 text-amber-700 rounded-xl text-xs font-bold transition-all flex items-center space-x-1.5 border border-amber-200 cursor-pointer"
                        >
                          <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-500" />
                          <span>{booking.userRating != null || reviewedPnrs[booking.pnr] ? "Rated ★" : "Rate Journey"}</span>
                        </button>
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => setSelectedBookingForCancel(booking)}
                        className="px-4 py-2 bg-red-50 hover:bg-red-100 text-red-600 rounded-xl text-xs font-bold transition-all cursor-pointer"
                      >
                        Cancel Booking
                      </button>
                    )
                  )}

                  {(booking.status === "CANCELLED" || booking.status === "REFUNDED") && (
                    <button
                      type="button"
                      onClick={() => toggleRefundTracker(booking.pnr)}
                      className="px-4 py-2 bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-700 hover:to-rose-700 text-white rounded-xl text-xs font-bold transition-all flex items-center space-x-2 shadow-sm cursor-pointer"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      <span>{expandedRefundTrackerPnrs[booking.pnr] ? "Hide Refund Status" : "See Refund Status"}</span>
                      <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${expandedRefundTrackerPnrs[booking.pnr] ? "rotate-180" : ""}`} />
                    </button>
                  )}
                </div>
              </motion.div>
            );
          })
        ) : !isAuthenticated && !activePnr ? (
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.45 }}
            className="bg-white rounded-3xl p-12 text-center border border-gray-200 shadow-xs"
          >
            <div className="w-16 h-16 rounded-full bg-red-50 text-[#d84e55] flex items-center justify-center mx-auto mb-4">
              <Ticket className="w-8 h-8" />
            </div>
            <p className="text-lg font-bold text-gray-900">Please Log In to View Your Bookings</p>
            <p className="text-xs text-gray-500 mt-1 max-w-md mx-auto">
              Sign in to your account to view your confirmed bus tickets, live boarding tracking, download tax invoices, or request instant refunds.
            </p>
            <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
              <Link
                href="/login"
                className="px-6 py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-red-500/20"
              >
                Log In / Sign Up
              </Link>
              <Link
                href="/"
                className="px-5 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl text-xs font-bold transition-all"
              >
                Search Buses
              </Link>
            </div>
          </motion.div>
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
                  <span>Cancel Ticket (₹{refundInfo.refundAmount.toFixed(2)})</span>
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

      {/* ======================================================== */}
      {/* 1. ENHANCED TICKET JOURNEY MODAL (Matching Image 4 & 3) */}
      {/* ======================================================== */}
      {selectedBookingForJourney && (() => {
        const booking = selectedBookingForJourney;
        const tripStatus = getRealtimeTripStatus(booking.travelDate, booking.departureTime, booking.arrivalTime, booking.status);
        const isCompleted = tripStatus.isCompleted;
        const comfortScore = ((booking.busRating || 4.4) * 2.0).toFixed(1);
        const bookedSeatNumbers = (booking.passengers || []).map(p => p.seatNumber).filter(Boolean);
        const primaryPassenger = booking.passengers?.[0]?.name || "Passenger";
        const primarySeat = bookedSeatNumbers[0] || "1F";
        const within2h = isWithinTwoHoursOfDeparture(booking);

        return (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs overflow-y-auto">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              className="bg-[#f8f9fa] rounded-3xl max-w-lg w-full shadow-2xl overflow-hidden border border-gray-100 my-8"
            >
              {/* Header */}
              <div className="bg-white px-5 py-4 border-b border-gray-100 flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <button
                    type="button"
                    onClick={() => setSelectedBookingForJourney(null)}
                    className="p-1.5 rounded-full hover:bg-gray-100 text-gray-700 transition-all cursor-pointer"
                  >
                    <ChevronDown className="w-5 h-5 -rotate-90" />
                  </button>
                  <div>
                    <h2 className="text-base font-black text-gray-900 tracking-tight">Ticket details</h2>
                    <p className="text-[11px] text-gray-500 font-medium">Ticket #{booking.pnr}</p>
                  </div>
                </div>
                <div className="flex items-center space-x-1.5">
                  <button
                    type="button"
                    onClick={() => {
                      navigator.clipboard.writeText(booking.pnr);
                      setCopyPnrFeedback(true);
                      setTimeout(() => setCopyPnrFeedback(false), 2500);
                    }}
                    title="Copy PNR / Share Ticket"
                    className="p-2 rounded-xl text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-all cursor-pointer relative"
                  >
                    <Share2 className="w-4 h-4" />
                    {copyPnrFeedback && (
                      <span className="absolute -bottom-7 right-0 bg-gray-900 text-white text-[10px] font-bold px-2 py-0.5 rounded-md shadow-xs whitespace-nowrap">
                        PNR Copied!
                      </span>
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={() => setSelectedBookingForJourney(null)}
                    className="p-2 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-all cursor-pointer"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Subtabs (Ticket details, Boarding point, Dropping point, Rest stop details) */}
              <div className="bg-white px-4 pt-1 border-b border-gray-200 flex items-center space-x-6 overflow-x-auto text-xs font-bold scrollbar-none">
                <button
                  type="button"
                  onClick={() => setJourneyTab("TICKET")}
                  className={`py-3 border-b-2 whitespace-nowrap transition-all cursor-pointer ${
                    journeyTab === "TICKET"
                      ? "border-[#d84e55] text-[#d84e55]"
                      : "border-transparent text-gray-500 hover:text-gray-800"
                  }`}
                >
                  Ticket details
                </button>
                <button
                  type="button"
                  onClick={() => setJourneyTab("BOARDING")}
                  className={`py-3 border-b-2 whitespace-nowrap transition-all cursor-pointer ${
                    journeyTab === "BOARDING"
                      ? "border-[#d84e55] text-[#d84e55]"
                      : "border-transparent text-gray-500 hover:text-gray-800"
                  }`}
                >
                  Boarding point details
                </button>
                <button
                  type="button"
                  onClick={() => setJourneyTab("DROPPING")}
                  className={`py-3 border-b-2 whitespace-nowrap transition-all cursor-pointer ${
                    journeyTab === "DROPPING"
                      ? "border-[#d84e55] text-[#d84e55]"
                      : "border-transparent text-gray-500 hover:text-gray-800"
                  }`}
                >
                  Dropping point details
                </button>
                <button
                  type="button"
                  onClick={() => setJourneyTab("REST_STOPS")}
                  className={`py-3 border-b-2 whitespace-nowrap transition-all cursor-pointer ${
                    journeyTab === "REST_STOPS"
                      ? "border-[#d84e55] text-[#d84e55]"
                      : "border-transparent text-gray-500 hover:text-gray-800"
                  }`}
                >
                  Rest stop details
                </button>
              </div>

              {/* Modal Body */}
              <div className="p-4 sm:p-5 max-h-[75vh] overflow-y-auto space-y-4">
                {journeyTab === "TICKET" && (
                  <>
                    {/* Rate this trip section if completed (Image 3) */}
                    {isCompleted ? (
                      <div className="bg-white p-4 rounded-2xl border border-gray-200 text-center space-y-3">
                        <p className="text-base font-black text-gray-900">Rate this trip</p>
                        <div className="flex items-center justify-center space-x-2">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <button
                              key={star}
                              type="button"
                              onClick={() => {
                                setReviewRating(star);
                                setSelectedBookingForReview(booking);
                              }}
                              className="p-1 hover:scale-125 transition-all cursor-pointer"
                            >
                              <Star
                                className={`w-8 h-8 ${
                                  star <= reviewRating
                                    ? "text-amber-400 fill-amber-400"
                                    : "text-gray-300 hover:text-amber-300"
                                }`}
                              />
                            </button>
                          ))}
                        </div>
                        {(booking.userRating != null || reviewedPnrs[booking.pnr]) && (
                          <p className="text-xs font-bold text-emerald-600">✓ Thank you for rating this journey!</p>
                        )}
                      </div>
                    ) : (
                      /* Top Banner Pill (Image 4) */
                      <div className="bg-rose-50/70 border border-rose-100 rounded-2xl p-3 flex items-center justify-between">
                        <div>
                          <p className="text-[11px] text-gray-500 font-medium">
                            {primaryPassenger}, great bus choice
                          </p>
                          <p className="text-xs font-bold text-gray-900">In-Bus toilet available</p>
                        </div>
                        <span className="text-[10px] font-black uppercase text-rose-700 bg-rose-100/60 px-2 py-1 rounded-lg tracking-wider flex items-center gap-1">
                          TRAVEL EQUAL &gt;
                        </span>
                      </div>
                    )}

                    {/* Route Card (Crimson if active, Charcoal Dark if completed) */}
                    <div
                      className={`rounded-2xl p-4 text-white shadow-md transition-all ${
                        isCompleted
                          ? "bg-gradient-to-br from-neutral-900 via-neutral-800 to-black"
                          : "bg-gradient-to-br from-[#d84e55] via-[#c63e45] to-[#a82d33]"
                      }`}
                    >
                      <div className="flex justify-between items-start">
                        <div>
                          <p className="text-lg font-black tracking-tight">{booking.sourceCity}</p>
                          <p className="text-[11px] text-white/80 font-medium max-w-[180px] truncate">
                            {booking.boardingPoint || "Main Bus Stand"}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-base font-black font-mono">{booking.departureTime || "21:30"}</p>
                          <p className="text-[10px] text-white/80 font-semibold">{booking.travelDate}</p>
                        </div>
                      </div>

                      {/* Middle connector line */}
                      <div className="my-3 flex items-center justify-between text-white/80">
                        <div className="w-2.5 h-2.5 rounded-full border-2 border-white bg-transparent" />
                        <div className="flex-1 border-t border-dashed border-white/40 mx-2 flex items-center justify-center relative">
                          <span className="bg-white/20 backdrop-blur-xs text-[10px] font-black px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-2xs">
                            <Bus className="w-3 h-3" />
                            {isCompleted ? "Trip Completed" : "7h 55 min"}
                          </span>
                        </div>
                        <div className="w-2.5 h-2.5 rounded-full border-2 border-white bg-white" />
                      </div>

                      <div className="flex justify-between items-end">
                        <div>
                          <p className="text-lg font-black tracking-tight">{booking.destinationCity}</p>
                          <p className="text-[11px] text-white/80 font-medium max-w-[180px] truncate">
                            {booking.droppingPoint || "Drop Location"}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-base font-black font-mono">05:25</p>
                          <p className="text-[10px] text-white/80 font-semibold">{booking.travelDate}</p>
                        </div>
                      </div>
                    </div>

                    {/* Operator & Seat Card */}
                    <div className="bg-white rounded-2xl p-4 border border-gray-200 shadow-2xs space-y-3">
                      <div className="flex items-start justify-between">
                        <div>
                          <p className="text-sm font-black text-gray-900">{booking.operatorName}</p>
                          <p className="text-xs text-gray-500 font-medium">
                            {booking.busType || "A/C Seater / Sleeper (2+1)"} • {bookedSeatNumbers.length} Seat{bookedSeatNumbers.length > 1 ? "s" : ""}
                          </p>
                          <p className="text-xs font-bold text-gray-800 mt-1">{primaryPassenger}</p>
                          <div className="flex items-center gap-1.5 mt-1">
                            {bookedSeatNumbers.map((s, idx) => (
                              <span
                                key={idx}
                                className="px-2.5 py-0.5 bg-rose-50 text-[#d84e55] font-black text-xs rounded-md border border-rose-200"
                              >
                                {s}
                              </span>
                            ))}
                          </div>
                        </div>

                        {/* View Seat Thumbnail Button */}
                        <div className="text-center">
                          <button
                            type="button"
                            onClick={() => setSelectedBookingForSeatLayout(booking)}
                            className="group p-2 bg-gray-50 hover:bg-rose-50 border border-gray-200 hover:border-rose-300 rounded-xl transition-all cursor-pointer block"
                          >
                            <div className="w-12 h-10 border border-dashed border-gray-300 rounded flex items-center justify-center text-[10px] font-bold text-gray-400 group-hover:text-rose-600">
                              ✇ ▣ ▣
                            </div>
                            <span className="text-[11px] font-bold text-indigo-600 group-hover:text-rose-600 block mt-1">
                              View seat
                            </span>
                          </button>
                        </div>
                      </div>

                      <div className="pt-2 border-t border-gray-100 text-center">
                        <a
                          href={`${API_BASE_URL}/bookings/${booking.pnr}/ticket-pdf`}
                          target="_blank"
                          rel="noreferrer"
                          className="text-xs font-bold text-indigo-600 hover:text-indigo-800 hover:underline cursor-pointer inline-flex items-center gap-1"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>Ticket details (Download PDF / e-Ticket)</span>
                        </a>
                      </div>
                    </div>

                    {/* Notice */}
                    <p className="text-[11px] text-gray-500 text-center px-4 leading-relaxed">
                      Bus number and tracking details will be shared on the day of journey on{" "}
                      <span className="font-bold text-gray-700">{booking.contactPhone || "+918610053302"}</span>
                    </p>

                    {/* Comfort Score Banner */}
                    <div className="bg-gradient-to-r from-emerald-50 via-teal-50 to-emerald-50/50 border border-emerald-200 rounded-2xl p-3.5 flex items-center space-x-3">
                      <div className="w-9 h-9 rounded-xl bg-emerald-600 text-white flex items-center justify-center font-black text-xs shadow-xs">
                        ★ {comfortScore}
                      </div>
                      <div>
                        <p className="text-xs font-black text-emerald-900">
                          {comfortScore}/10 Excellent Comfort Score
                        </p>
                        <p className="text-[11px] text-emerald-700 font-medium">
                          Calculated from verified fleet ratings, punctuality & cleanliness.
                        </p>
                      </div>
                    </div>

                    {/* Quick Action Rows (Image 4) */}
                    <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden divide-y divide-gray-100">
                      {/* Change trip details */}
                      <button
                        type="button"
                        disabled={isCompleted || within2h}
                        onClick={() => {
                          if (!isCompleted && !within2h) {
                            setSelectedBookingForChangePoints(booking);
                            setNewBoardingPoint(booking.boardingPoint || "");
                            setNewDroppingPoint(booking.droppingPoint || "");
                            setChangePointsError("");
                            setChangePointsSuccess("");
                          }
                        }}
                        className={`w-full p-4 flex items-center justify-between text-left transition-all ${
                          isCompleted || within2h
                            ? "bg-gray-50/60 cursor-not-allowed opacity-60"
                            : "hover:bg-gray-50/80 cursor-pointer"
                        }`}
                      >
                        <div className="flex items-start space-x-3">
                          <Info className={`w-5 h-5 shrink-0 mt-0.5 ${isCompleted || within2h ? "text-gray-400" : "text-gray-600"}`} />
                          <div>
                            <p className={`text-xs font-black ${isCompleted || within2h ? "text-gray-400" : "text-gray-900"}`}>
                              Change trip details
                            </p>
                            <p className="text-[11px] text-gray-500">
                              {isCompleted ? "Trip completed" : within2h ? "Locked within 2 hours of departure" : "Boarding and dropping point"}
                            </p>
                          </div>
                        </div>
                        <ChevronDown className="w-4 h-4 text-gray-400 -rotate-90 shrink-0" />
                      </button>

                      {/* Free bus change */}
                      <button
                        type="button"
                        disabled={isCompleted || booking.status === "CANCELLED" || booking.status === "REFUNDED"}
                        onClick={() => {
                          if (!isCompleted && booking.status !== "CANCELLED" && booking.status !== "REFUNDED") {
                            setSelectedBookingForChangeDate(booking);
                            setNewJourneyDate(booking.travelDate || "");
                            setChangeDateError("");
                            setChangeDateSuccess("");
                            setShowConfirmDateModal(false);
                          }
                        }}
                        className={`w-full p-4 flex items-center justify-between text-left transition-all ${
                          isCompleted || booking.status === "CANCELLED" || booking.status === "REFUNDED"
                            ? "bg-gray-50/60 cursor-not-allowed opacity-60"
                            : "hover:bg-gray-50/80 cursor-pointer"
                        }`}
                      >
                        <div className="flex items-start space-x-3">
                          <Calendar className={`w-5 h-5 shrink-0 mt-0.5 ${isCompleted ? "text-gray-400" : "text-gray-600"}`} />
                          <div>
                            <p className={`text-xs font-black ${isCompleted ? "text-gray-400" : "text-gray-900"}`}>
                              Free bus change
                            </p>
                            <p className="text-[11px] text-gray-500">
                              {isCompleted ? "Trip completed" : "Postpone journey date to future schedule"}
                            </p>
                          </div>
                        </div>
                        <ChevronDown className="w-4 h-4 text-gray-400 -rotate-90 shrink-0" />
                      </button>

                      {/* Cancel Booking (ONLY if not already cancelled) */}
                      {booking.status !== "CANCELLED" && booking.status !== "REFUNDED" && (
                        <button
                          type="button"
                          disabled={isCompleted}
                          onClick={() => {
                            if (!isCompleted) {
                              setSelectedBookingForJourney(null);
                              setSelectedBookingForCancel(booking);
                            }
                          }}
                          className={`w-full p-4 flex items-center justify-between text-left transition-all ${
                            isCompleted
                              ? "bg-gray-50/60 cursor-not-allowed opacity-60"
                              : "hover:bg-red-50/50 cursor-pointer"
                          }`}
                        >
                          <div className="flex items-start space-x-3">
                            <XCircle className={`w-5 h-5 shrink-0 mt-0.5 ${isCompleted ? "text-gray-400" : "text-red-500"}`} />
                            <div>
                              <p className={`text-xs font-black ${isCompleted ? "text-gray-400" : "text-red-600"}`}>
                                Cancel Booking
                              </p>
                              <p className="text-[11px] text-gray-500">
                                {isCompleted ? "Trip completed" : "Eligible for instant wallet refund"}
                              </p>
                            </div>
                          </div>
                          <ChevronDown className="w-4 h-4 text-gray-400 -rotate-90 shrink-0" />
                        </button>
                      )}
                    </div>
                  </>
                )}

                {/* Subtab: Boarding Point Details */}
                {journeyTab === "BOARDING" && (
                  <div className="bg-white rounded-2xl p-5 border border-gray-200 space-y-4">
                    <div className="flex items-start space-x-3">
                      <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                        <MapPin className="w-4 h-4" />
                      </div>
                      <div>
                        <p className="text-xs font-black text-gray-900 uppercase tracking-wider">
                          Official Boarding Location
                        </p>
                        <p className="text-sm font-extrabold text-gray-900 mt-0.5">
                          {booking.boardingPoint || `${booking.sourceCity} Main Bus Stand`}
                        </p>
                        <p className="text-xs text-gray-500 mt-1 leading-relaxed">
                          Near Central Metro Station / Opposite Platform 4, Highway Bypass, {booking.sourceCity}
                        </p>
                      </div>
                    </div>

                    <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-xs text-amber-800 space-y-1">
                      <p className="font-bold flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5" /> Reporting Guideline
                      </p>
                      <p className="text-[11px]">
                        Please report at least 15 minutes prior to departure ({booking.departureTime || "21:30"}). Carry a valid government ID for verification.
                      </p>
                    </div>
                  </div>
                )}

                {/* Subtab: Dropping Point Details */}
                {journeyTab === "DROPPING" && (
                  <div className="bg-white rounded-2xl p-5 border border-gray-200 space-y-4">
                    <div className="flex items-start space-x-3">
                      <div className="w-8 h-8 rounded-xl bg-red-100 text-[#d84e55] flex items-center justify-center shrink-0">
                        <MapPin className="w-4 h-4" />
                      </div>
                      <div>
                        <p className="text-xs font-black text-gray-900 uppercase tracking-wider">
                          Official Dropping Point
                        </p>
                        <p className="text-sm font-extrabold text-gray-900 mt-0.5">
                          {booking.droppingPoint || `${booking.destinationCity} Drop Location`}
                        </p>
                        <p className="text-xs text-gray-500 mt-1 leading-relaxed">
                          Main Bypass Circle / Toll Gate Plaza, {booking.destinationCity}
                        </p>
                      </div>
                    </div>
                  </div>
                )}

                {/* Subtab: Rest Stop Details */}
                {journeyTab === "REST_STOPS" && (
                  <div className="bg-white rounded-2xl p-5 border border-gray-200 space-y-4">
                    <div className="flex items-start space-x-3">
                      <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center shrink-0">
                        <Coffee className="w-4 h-4" />
                      </div>
                      <div>
                        <p className="text-xs font-black text-gray-900 uppercase tracking-wider">
                          Highway Food & Restroom Oasis
                        </p>
                        <p className="text-sm font-extrabold text-gray-900 mt-0.5">
                          Midway Highway Food Plaza (Halt: 25 - 30 mins)
                        </p>
                        <p className="text-xs text-gray-500 mt-1 leading-relaxed">
                          Verified hygienic facilities, separate clean male/female washrooms, and sanitized pure-vegetarian & multi-cuisine restaurants.
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </motion.div>
          </div>
        );
      })()}

      {/* ======================================================== */}
      {/* 2. BOOKED SEAT LAYOUT MODAL (Matching Image 3 & 4) */}
      {/* ======================================================== */}
      {selectedBookingForSeatLayout && (() => {
        const booking = selectedBookingForSeatLayout;
        const bookedSeats = (booking.passengers || []).map((p) => p.seatNumber.trim()).filter(Boolean);
        const isSleeper =
          booking.busType.toLowerCase().includes("sleeper") ||
          bookedSeats.some((s) => s.toUpperCase().startsWith("U") || s.toUpperCase().startsWith("L"));

        // Lower Deck 1+2 Sleeper Layout (Image 4: L1-L15)
        const lowerDeckRows = [
          { left: "L1", right1: "L2", right2: "L3" },
          { left: "L4", right1: "L5", right2: "L6" },
          { left: "L7", right1: "L8", right2: "L9" },
          { left: "L10", right1: "L11", right2: "L12" },
          { left: "L13", right1: "L14", right2: "L15" },
        ];

        // Upper Deck 1+2 Sleeper Layout (Image 4: U1-U15)
        const upperDeckRows = [
          { left: "U1", right1: "U2", right2: "U3" },
          { left: "U4", right1: "U5", right2: "U6" },
          { left: "U7", right1: "U8", right2: "U9" },
          { left: "U10", right1: "U11", right2: "U12" },
          { left: "U13", right1: "U14", right2: "U15" },
        ];

        // 2+2 Seater Rows
        const seaterRows = [
          ["S1", "S2", "AISLE", "S3", "S4"],
          ["S5", "S6", "AISLE", "S7", "S8"],
          ["S9", "S10", "AISLE", "S11", "S12"],
          ["S13", "S14", "AISLE", "S15", "S16"],
          ["S17", "S18", "AISLE", "S19", "S20"],
          ["S21", "S22", "AISLE", "S23", "S24"],
        ];

        return (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-gray-100 space-y-4"
            >
              <div className="flex items-center justify-between pb-2 border-b border-gray-100">
                <div>
                  <h3 className="text-base font-black text-gray-900">Booked Seat Map</h3>
                  <p className="text-[11px] text-gray-500">{booking.operatorName} • {booking.busType}</p>
                </div>
                <button
                  type="button"
                  onClick={() => setSelectedBookingForSeatLayout(null)}
                  className="p-1.5 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-700 transition-all cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {/* Dual-Deck Layout matching Image 3 */}
              <div className="max-h-[60vh] overflow-y-auto pr-1 space-y-4">
                {isSleeper ? (
                  <div className="grid grid-cols-2 gap-3">
                    {/* Lower Deck */}
                    <div className="bg-slate-50/80 rounded-2xl border border-slate-200 p-3 space-y-2.5">
                      <div className="flex items-center justify-between pb-1.5 border-b border-slate-200 text-xs font-bold text-slate-700">
                        <span>Lower deck</span>
                        <span className="w-5 h-5 rounded-full bg-slate-200 text-slate-600 flex items-center justify-center text-[11px]">
                          ✇
                        </span>
                      </div>

                      <div className="space-y-2">
                        {lowerDeckRows.map((r, idx) => (
                          <div key={idx} className="flex items-center justify-between">
                            {/* Single Berth Left */}
                            <div
                              className={`w-9 h-14 rounded-lg flex flex-col items-center justify-between p-1 transition-all ${
                                bookedSeats.includes(r.left)
                                  ? "bg-pink-600 border border-pink-700 text-white shadow-md shadow-pink-500/30 scale-105"
                                  : "bg-slate-200/90 border border-slate-300/80 text-slate-400"
                              }`}
                            >
                              <div
                                className={`w-full h-2 rounded-xs ${
                                  bookedSeats.includes(r.left) ? "bg-white/40" : "bg-slate-300/60"
                                }`}
                              />
                              <span className="text-[10px] font-black">{r.left}</span>
                              <div className="h-1" />
                            </div>

                            {/* Aisle Spacer */}
                            <div className="w-2" />

                            {/* Double Berths Right */}
                            <div className="flex gap-1.5">
                              {[r.right1, r.right2].map((s) => (
                                <div
                                  key={s}
                                  className={`w-9 h-14 rounded-lg flex flex-col items-center justify-between p-1 transition-all ${
                                    bookedSeats.includes(s)
                                      ? "bg-pink-600 border border-pink-700 text-white shadow-md shadow-pink-500/30 scale-105"
                                      : "bg-slate-200/90 border border-slate-300/80 text-slate-400"
                                  }`}
                                >
                                  <div
                                    className={`w-full h-2 rounded-xs ${
                                      bookedSeats.includes(s) ? "bg-white/40" : "bg-slate-300/60"
                                    }`}
                                  />
                                  <span className="text-[10px] font-black">{s}</span>
                                  <div className="h-1" />
                                </div>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>

                      <p className="text-[8.5px] font-bold text-slate-400 text-center pt-1 uppercase">
                        ▼ Emergency Exit
                      </p>
                    </div>

                    {/* Upper Deck */}
                    <div className="bg-slate-50/80 rounded-2xl border border-slate-200 p-3 space-y-2.5">
                      <div className="flex items-center justify-between pb-1.5 border-b border-slate-200 text-xs font-bold text-slate-700">
                        <span>Upper deck</span>
                        <span className="text-[10px] font-black text-slate-500 bg-slate-200 px-1.5 py-0.5 rounded-sm">
                          UP
                        </span>
                      </div>

                      <div className="space-y-2">
                        {upperDeckRows.map((r, idx) => (
                          <div key={idx} className="flex items-center justify-between">
                            {/* Single Berth Left */}
                            <div
                              className={`w-9 h-14 rounded-lg flex flex-col items-center justify-between p-1 transition-all ${
                                bookedSeats.includes(r.left)
                                  ? "bg-pink-600 border border-pink-700 text-white shadow-md shadow-pink-500/30 scale-105"
                                  : "bg-slate-200/90 border border-slate-300/80 text-slate-400"
                              }`}
                            >
                              <div
                                className={`w-full h-2 rounded-xs ${
                                  bookedSeats.includes(r.left) ? "bg-white/40" : "bg-slate-300/60"
                                }`}
                              />
                              <span className="text-[10px] font-black">{r.left}</span>
                              <div className="h-1" />
                            </div>

                            {/* Aisle Spacer */}
                            <div className="w-2" />

                            {/* Double Berths Right */}
                            <div className="flex gap-1.5">
                              {[r.right1, r.right2].map((s) => (
                                <div
                                  key={s}
                                  className={`w-9 h-14 rounded-lg flex flex-col items-center justify-between p-1 transition-all ${
                                    bookedSeats.includes(s)
                                      ? "bg-pink-600 border border-pink-700 text-white shadow-md shadow-pink-500/30 scale-105"
                                      : "bg-slate-200/90 border border-slate-300/80 text-slate-400"
                                  }`}
                                >
                                  <div
                                    className={`w-full h-2 rounded-xs ${
                                      bookedSeats.includes(s) ? "bg-white/40" : "bg-slate-300/60"
                                    }`}
                                  />
                                  <span className="text-[10px] font-black">{s}</span>
                                  <div className="h-1" />
                                </div>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>

                      <p className="text-[8.5px] font-bold text-slate-400 text-center pt-1 uppercase">
                        ▼ Emergency Exit
                      </p>
                    </div>
                  </div>
                ) : (
                  // Seater 2+2 Layout
                  <div className="bg-slate-50/80 rounded-2xl border border-slate-200 p-4 space-y-3">
                    <div className="flex items-center justify-between pb-1.5 border-b border-slate-200 text-xs font-bold text-slate-700">
                      <span>Seater Cabin (2+2)</span>
                      <span className="text-[10px] font-bold text-slate-400">✇ FRONT</span>
                    </div>

                    <div className="space-y-2">
                      {seaterRows.map((r, rIdx) => (
                        <div key={rIdx} className="flex items-center justify-around">
                          <div className="flex gap-2">
                            {[r[0], r[1]].map((s) => (
                              <div
                                key={s}
                                className={`w-10 h-10 rounded-lg flex items-center justify-center text-xs font-bold ${
                                  bookedSeats.includes(s)
                                    ? "bg-pink-600 text-white shadow-sm font-black"
                                    : "bg-slate-200 text-slate-500"
                                }`}
                              >
                                {s}
                              </div>
                            ))}
                          </div>
                          <div className="w-4 text-center text-slate-300 text-[10px]">|</div>
                          <div className="flex gap-2">
                            {[r[3], r[4]].map((s) => (
                              <div
                                key={s}
                                className={`w-10 h-10 rounded-lg flex items-center justify-center text-xs font-bold ${
                                  bookedSeats.includes(s)
                                    ? "bg-pink-600 text-white shadow-sm font-black"
                                    : "bg-slate-200 text-slate-500"
                                }`}
                              >
                                {s}
                              </div>
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Clean Legend (Image 3) */}
              <div className="flex items-center justify-center gap-5 text-xs font-bold pt-2 border-t border-gray-100">
                <div className="flex items-center gap-1.5">
                  <div className="w-3.5 h-3.5 rounded-sm bg-pink-600" />
                  <span className="text-slate-800">Your Booked Seat ({bookedSeats.join(", ") || "Yours"})</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <div className="w-3.5 h-3.5 rounded-sm bg-slate-200" />
                  <span className="text-slate-400 font-medium">Other Berths</span>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setSelectedBookingForSeatLayout(null)}
                className="w-full py-2.5 bg-gray-900 text-white rounded-xl text-xs font-bold hover:bg-black transition-all cursor-pointer"
              >
                Close Seat Map
              </button>
            </motion.div>
          </div>
        );
      })()}

      {/* ======================================================== */}
      {/* 3. CHANGE TRIP DETAILS MODAL (Matching Image 1) */}
      {/* ======================================================== */}
      {selectedBookingForChangePoints && (() => {
        const booking = selectedBookingForChangePoints;
        const availableBoarding: string[] = Array.isArray(booking.availableBoardingPoints)
          ? booking.availableBoardingPoints
          : typeof booking.availableBoardingPoints === "string"
          ? (booking.availableBoardingPoints as string).split(",").map((s) => s.trim()).filter(Boolean)
          : [
              `${booking.sourceCity} Main Bus Stand`,
              `${booking.sourceCity} Central Metro`,
              `${booking.sourceCity} Airport Bypass`,
              `${booking.sourceCity} Toll Gate Plaza`,
            ];

        const availableDropping: string[] = Array.isArray(booking.availableDroppingPoints)
          ? booking.availableDroppingPoints
          : typeof booking.availableDroppingPoints === "string"
          ? (booking.availableDroppingPoints as string).split(",").map((s) => s.trim()).filter(Boolean)
          : [
              `${booking.destinationCity} Drop Location`,
              `${booking.destinationCity} Koyambedu / Central`,
              `${booking.destinationCity} Guindy Station`,
              `${booking.destinationCity} Perungalathur Bypass`,
            ];

        const handleSavePoints = async () => {
          if (!newBoardingPoint && !newDroppingPoint) {
            setChangePointsError("Please select a new boarding or dropping point.");
            return;
          }
          try {
            await changeTripPointsMutation({
              pnr: booking.pnr,
              boardingPoint: newBoardingPoint || booking.boardingPoint,
              droppingPoint: newDroppingPoint || booking.droppingPoint,
            }).unwrap();
            setChangePointsSuccess("Trip points updated successfully!");
            setTimeout(() => {
              setSelectedBookingForChangePoints(null);
              setSelectedBookingForJourney(null);
            }, 1200);
          } catch (err: any) {
            setChangePointsError(err?.data?.message || "Failed to update trip points.");
          }
        };

        return (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-gray-100 space-y-5"
            >
              <div className="flex items-center justify-between pb-3 border-b border-gray-100">
                <div>
                  <h3 className="text-lg font-black text-gray-900">Change trip details</h3>
                  <p className="text-xs text-gray-500 font-medium">Ticket #{booking.pnr}</p>
                </div>
                <button
                  type="button"
                  onClick={() => setSelectedBookingForChangePoints(null)}
                  className="p-1.5 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-700 transition-all cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {changePointsError && (
                <div className="p-3 bg-red-50 text-red-700 text-xs font-bold rounded-xl border border-red-200">
                  {changePointsError}
                </div>
              )}

              {changePointsSuccess && (
                <div className="p-3 bg-emerald-50 text-emerald-700 text-xs font-bold rounded-xl border border-emerald-200">
                  {changePointsSuccess}
                </div>
              )}

              {/* Boarding Point Selection */}
              <div>
                <label className="text-xs font-black text-gray-900 block mb-2 flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Select Boarding Point ({booking.sourceCity}):</span>
                </label>
                <div className="space-y-1.5 max-h-36 overflow-y-auto">
                  {availableBoarding.map((pt, idx) => (
                    <label
                      key={idx}
                      className={`flex items-center justify-between p-2.5 rounded-xl border text-xs font-bold cursor-pointer transition-all ${
                        newBoardingPoint === pt
                          ? "bg-rose-50 border-[#d84e55] text-[#d84e55]"
                          : "bg-gray-50 border-gray-200 text-gray-700 hover:bg-gray-100"
                      }`}
                    >
                      <span>{pt}</span>
                      <input
                        type="radio"
                        name="boardingPoint"
                        value={pt}
                        checked={newBoardingPoint === pt}
                        onChange={() => setNewBoardingPoint(pt)}
                        className="text-[#d84e55] focus:ring-[#d84e55]"
                      />
                    </label>
                  ))}
                </div>
              </div>

              {/* Dropping Point Selection */}
              <div>
                <label className="text-xs font-black text-gray-900 block mb-2 flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-red-500" />
                  <span>Select Dropping Point ({booking.destinationCity}):</span>
                </label>
                <div className="space-y-1.5 max-h-36 overflow-y-auto">
                  {availableDropping.map((pt, idx) => (
                    <label
                      key={idx}
                      className={`flex items-center justify-between p-2.5 rounded-xl border text-xs font-bold cursor-pointer transition-all ${
                        newDroppingPoint === pt
                          ? "bg-rose-50 border-[#d84e55] text-[#d84e55]"
                          : "bg-gray-50 border-gray-200 text-gray-700 hover:bg-gray-100"
                      }`}
                    >
                      <span>{pt}</span>
                      <input
                        type="radio"
                        name="droppingPoint"
                        value={pt}
                        checked={newDroppingPoint === pt}
                        onChange={() => setNewDroppingPoint(pt)}
                        className="text-[#d84e55] focus:ring-[#d84e55]"
                      />
                    </label>
                  ))}
                </div>
              </div>

              <div className="flex gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={() => setSelectedBookingForChangePoints(null)}
                  className="flex-1 py-3 bg-gray-100 text-gray-700 font-bold text-xs rounded-xl hover:bg-gray-200 transition-all cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  disabled={isChangingPoints}
                  onClick={handleSavePoints}
                  className="flex-1 py-3 bg-[#d84e55] hover:bg-[#b83e44] text-white font-black text-xs rounded-xl shadow-md shadow-red-500/20 transition-all cursor-pointer disabled:opacity-50"
                >
                  {isChangingPoints ? "Updating..." : "Save Points"}
                </button>
              </div>
            </motion.div>
          </div>
        );
      })()}

      {/* ======================================================== */}
      {/* 4. FREE BUS CHANGE / DATE POSTPONEMENT MODAL */}
      {/* ======================================================== */}
      {selectedBookingForChangeDate && (() => {
        const booking = selectedBookingForChangeDate;
        const datesList = availableDatesData?.dates || [];
        const selectedDateInfo = datesList.find((d: any) => d.date === newJourneyDate);
        const bookedSeats = availableDatesData?.bookedSeats || booking.passengers.map((p) => p.seatNumber);
        const requiredSeatCount = bookedSeats.length || 1;
        const needsSeatSelection = selectedDateInfo && selectedDateInfo.busAvailable && !selectedDateInfo.sameSeatsAvailable;

        const handleConfirmDateChange = async () => {
          if (!newJourneyDate) {
            setChangeDateError("Please pick a future journey date.");
            return;
          }
          if (selectedDateInfo && !selectedDateInfo.busAvailable) {
            setChangeDateError("This bus is not available on the selected date. Please pick a highlighted available date or search other buses.");
            setShowConfirmDateModal(false);
            return;
          }
          if (needsSeatSelection && selectedNewSeats.length !== requiredSeatCount) {
            setChangeDateError(`Please select ${requiredSeatCount} replacement seat(s) to continue.`);
            setShowConfirmDateModal(false);
            return;
          }
          try {
            const updatedBooking = await changeJourneyDateMutation({
              pnr: booking.pnr,
              newTravelDate: newJourneyDate,
              newRouteId: selectedDateInfo?.routeId || undefined,
              newSeatNumbers: needsSeatSelection ? selectedNewSeats : undefined,
            }).unwrap();
            setChangeDateSuccess("Journey date updated! Updated e-ticket & PDF sent to your email.");
            setSelectedBookingForJourney(updatedBooking);
            refetchBookings();
            setTimeout(() => {
              setShowConfirmDateModal(false);
              setSelectedBookingForChangeDate(null);
              setSelectedNewSeats([]);
            }, 1400);
          } catch (err: any) {
            setChangeDateError(err?.data?.message || "Failed to update journey date.");
            setShowConfirmDateModal(false);
          }
        };

        return (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-gray-100 space-y-4 max-h-[90vh] overflow-y-auto"
            >
              <div className="flex items-center justify-between pb-2 border-b border-gray-100">
                <div>
                  <h3 className="text-base font-black text-gray-900">Change Journey Date</h3>
                  <p className="text-[11px] text-gray-500">
                    {booking.operatorName} • Ticket #{booking.pnr} • Seat(s): {bookedSeats.join(", ")}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setSelectedBookingForChangeDate(null);
                    setSelectedNewSeats([]);
                    setShowConfirmDateModal(false);
                  }}
                  className="p-1.5 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-700 transition-all cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {changeDateError && (
                <div className="p-2.5 bg-red-50 text-red-700 text-xs font-bold rounded-xl border border-red-200">
                  {changeDateError}
                </div>
              )}

              {changeDateSuccess && (
                <div className="p-2.5 bg-emerald-50 text-emerald-700 text-xs font-bold rounded-xl border border-emerald-200">
                  {changeDateSuccess}
                </div>
              )}

              <div className="p-3 bg-rose-50/60 rounded-xl border border-rose-100 text-xs text-rose-900 space-y-1">
                <p className="font-bold">Current Journey Date: {booking.travelDate}</p>
                <p className="text-[11px] text-gray-600">
                  Select an upcoming available date below. Faded dates indicate the bus is not operating or sold out on that date.
                </p>
              </div>

              {/* Dynamic Upcoming Dates Calendar Grid */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-black text-gray-900">
                    Select Upcoming Date:
                  </label>
                  {isLoadingAvailableDates && (
                    <span className="text-[11px] text-gray-500 animate-pulse">Checking live seat availability...</span>
                  )}
                </div>

                {datesList.length > 0 ? (
                  <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 max-h-52 overflow-y-auto p-1">
                    {datesList.slice(0, 28).map((item: any) => {
                      const isCurrent = item.date === booking.travelDate;
                      const isSelected = item.date === newJourneyDate;
                      const isAvailable = item.busAvailable;
                      const parsed = new Date(`${item.date}T00:00:00`);
                      const label = parsed.toLocaleDateString("en-IN", { day: "2-digit", month: "short" });
                      const weekday = parsed.toLocaleDateString("en-IN", { weekday: "short" });

                      return (
                        <button
                          key={item.date}
                          type="button"
                          onClick={() => {
                            setNewJourneyDate(item.date);
                            setSelectedNewSeats([]);
                            setShowConfirmDateModal(false);
                            if (!isAvailable) {
                              setChangeDateError(
                                `${booking.operatorName} is not available on ${item.date}. Please choose a highlighted date or search other buses.`
                              );
                            } else {
                              setChangeDateError("");
                            }
                          }}
                          className={`p-2 rounded-xl border text-left transition-all cursor-pointer ${
                            isSelected
                              ? "border-[#d84e55] bg-[#d84e55]/10 ring-2 ring-[#d84e55]/40"
                              : isCurrent
                              ? "border-blue-300 bg-blue-50/40"
                              : isAvailable
                              ? "border-gray-200 bg-white hover:border-[#d84e55]"
                              : "border-gray-100 bg-gray-100/60 opacity-45"
                          }`}
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-[10px] font-bold text-gray-500">{weekday}</span>
                            {isCurrent && (
                              <span className="text-[9px] font-black text-blue-600 uppercase">Current</span>
                            )}
                          </div>
                          <p className="text-xs font-black text-gray-900 mt-0.5">{label}</p>
                          <p
                            className={`text-[10px] font-bold mt-0.5 ${
                              !isAvailable
                                ? "text-red-500"
                                : item.sameSeatsAvailable
                                ? "text-emerald-600"
                                : "text-amber-600"
                            }`}
                          >
                            {!isAvailable
                              ? "Unavailable"
                              : item.sameSeatsAvailable
                              ? `${item.availableSeats} Seats`
                              : `Seat ${item.conflictingSeats.join(",")} taken`}
                          </p>
                        </button>
                      );
                    })}
                  </div>
                ) : (
                  <input
                    type="date"
                    min={booking.travelDate}
                    value={newJourneyDate}
                    onChange={(e) => {
                      setNewJourneyDate(e.target.value);
                      setSelectedNewSeats([]);
                      setChangeDateError("");
                    }}
                    className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:ring-2 focus:ring-[#d84e55] focus:outline-hidden"
                  />
                )}
              </div>

              {/* Bus Unavailable Prompt */}
              {selectedDateInfo && !selectedDateInfo.busAvailable && (
                <div className="p-3.5 bg-red-50 rounded-2xl border border-red-200 space-y-2.5">
                  <p className="text-xs font-bold text-red-800">
                    {booking.operatorName} ({booking.departureTime?.slice(0, 5)}) is not available on{" "}
                    <span className="font-black">{newJourneyDate}</span>. Please pick another available date in the calendar or explore other buses on this date.
                  </p>
                  <Link
                    href={`/search?source=${encodeURIComponent(booking.sourceCity)}&destination=${encodeURIComponent(booking.destinationCity)}&date=${newJourneyDate}`}
                    className="inline-flex items-center gap-1.5 px-3 py-2 bg-[#d84e55] text-white text-xs font-black rounded-xl hover:bg-[#b83e44]"
                  >
                    Search Buses on {newJourneyDate} &rarr;
                  </Link>
                </div>
              )}

              {/* Replacement Seat Selection when user's booked seat is already taken on new date */}
              {needsSeatSelection && selectedDateInfo && (
                <div className="p-3.5 bg-amber-50 rounded-2xl border border-amber-200 space-y-2.5">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="text-xs font-black text-amber-900">
                        Seat {selectedDateInfo.conflictingSeats.join(", ")} is already booked on {newJourneyDate}
                      </p>
                      <p className="text-[11px] text-gray-600 mt-0.5">
                        Select {requiredSeatCount} available replacement seat(s) below ({selectedNewSeats.length}/{requiredSeatCount} selected):
                      </p>
                    </div>
                    <Link
                      href={`/search?source=${encodeURIComponent(booking.sourceCity)}&destination=${encodeURIComponent(booking.destinationCity)}&date=${newJourneyDate}`}
                      className="text-[11px] font-black text-[#d84e55] underline shrink-0"
                    >
                      Open Bus Search
                    </Link>
                  </div>
                  <div className="flex flex-wrap gap-1.5 max-h-32 overflow-y-auto pt-1">
                    {selectedDateInfo.availableSeatNumbers.map((seatNo: string) => {
                      const picked = selectedNewSeats.includes(seatNo);
                      return (
                        <button
                          key={seatNo}
                          type="button"
                          onClick={() => {
                            setChangeDateError("");
                            setSelectedNewSeats((prev) => {
                              if (prev.includes(seatNo)) return prev.filter((s) => s !== seatNo);
                              if (prev.length >= requiredSeatCount) {
                                return [...prev.slice(1), seatNo];
                              }
                              return [...prev, seatNo];
                            });
                          }}
                          className={`px-2.5 py-1.5 rounded-lg text-xs font-black border transition-all cursor-pointer ${
                            picked
                              ? "bg-[#d84e55] text-white border-[#d84e55]"
                              : "bg-white text-gray-800 border-gray-200 hover:border-[#d84e55]"
                          }`}
                        >
                          {seatNo}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Confirmation Step */}
              {showConfirmDateModal ? (
                <div className="p-4 bg-amber-50 rounded-2xl border border-amber-200 space-y-3">
                  <p className="text-xs font-black text-amber-900 leading-snug">
                    Confirm changing your journey date to{" "}
                    <span className="text-[#d84e55] font-extrabold">{newJourneyDate}</span>
                    {needsSeatSelection && selectedNewSeats.length > 0
                      ? ` with new seat(s) ${selectedNewSeats.join(", ")}`
                      : ` (Seat ${bookedSeats.join(", ")})`}
                    ?
                  </p>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => setShowConfirmDateModal(false)}
                      className="flex-1 py-2 bg-white text-gray-700 text-xs font-bold rounded-xl border border-gray-200 hover:bg-gray-100 cursor-pointer"
                    >
                      No, Go Back
                    </button>
                    <button
                      type="button"
                      disabled={isChangingDate}
                      onClick={handleConfirmDateChange}
                      className="flex-1 py-2 bg-[#d84e55] text-white text-xs font-black rounded-xl hover:bg-[#b83e44] shadow-xs cursor-pointer"
                    >
                      {isChangingDate ? "Updating..." : "Yes, Confirm"}
                    </button>
                  </div>
                </div>
              ) : (
                <div className="flex gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedBookingForChangeDate(null);
                      setSelectedNewSeats([]);
                    }}
                    className="flex-1 py-2.5 bg-gray-100 text-gray-700 text-xs font-bold rounded-xl hover:bg-gray-200 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    disabled={
                      !newJourneyDate ||
                      newJourneyDate === booking.travelDate ||
                      (selectedDateInfo ? !selectedDateInfo.busAvailable : false) ||
                      (needsSeatSelection ? selectedNewSeats.length !== requiredSeatCount : false)
                    }
                    onClick={() => setShowConfirmDateModal(true)}
                    className="flex-1 py-2.5 bg-[#d84e55] text-white text-xs font-black rounded-xl hover:bg-[#b83e44] shadow-xs cursor-pointer disabled:opacity-50"
                  >
                    Next &rarr;
                  </button>
                </div>
              )}
            </motion.div>
          </div>
        );
      })()}

      {/* ======================================================== */}
      {/* 5. RATE TRIP MODAL */}
      {/* ======================================================== */}
      {selectedBookingForReview && (() => {
        const booking = selectedBookingForReview;

        const handleSubmitReview = async () => {
          try {
            await createReviewMutation({
              busId: booking.busId || 1,
              rating: reviewRating,
              comment: reviewComment || "Smooth and comfortable journey!",
              userName: activeUser?.name || "Passenger",
              tags: ["Comfortable Berths", "Punctual", "Clean Bus"],
              pnr: booking.pnr,
            }).unwrap();

            const updated = { ...reviewedPnrs, [booking.pnr]: true };
            setReviewedPnrs(updated);
            try {
              localStorage.setItem("redbus_reviewed_pnrs", JSON.stringify(updated));
            } catch {}
            setReviewSuccess("Thank you for reviewing your journey!");
            refetchBookings();
            setTimeout(() => {
              setSelectedBookingForReview(null);
              setReviewSuccess("");
            }, 1200);
          } catch {
            const updated = { ...reviewedPnrs, [booking.pnr]: true };
            setReviewedPnrs(updated);
            try {
              localStorage.setItem("redbus_reviewed_pnrs", JSON.stringify(updated));
            } catch {}
            setReviewSuccess("Review registered successfully!");
            refetchBookings();
            setTimeout(() => {
              setSelectedBookingForReview(null);
              setReviewSuccess("");
            }, 1200);
          }
        };

        return (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-gray-100 space-y-4 text-center"
            >
              <div className="flex items-center justify-between pb-2 border-b border-gray-100">
                <p className="text-base font-black text-gray-900">Rate this trip</p>
                <button
                  type="button"
                  onClick={() => setSelectedBookingForReview(null)}
                  className="p-1 rounded-full text-gray-400 hover:text-gray-700 cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {reviewSuccess ? (
                <div className="p-3 bg-emerald-50 text-emerald-700 text-xs font-bold rounded-xl border border-emerald-200">
                  {reviewSuccess}
                </div>
              ) : (
                <>
                  <p className="text-xs text-gray-500 font-medium">
                    How was your journey with {booking.operatorName}?
                  </p>

                  <div className="flex items-center justify-center space-x-2 py-2">
                    {[1, 2, 3, 4, 5].map((s) => (
                      <button
                        key={s}
                        type="button"
                        onClick={() => setReviewRating(s)}
                        className="p-1 hover:scale-125 transition-transform cursor-pointer"
                      >
                        <Star
                          className={`w-7 h-7 ${
                            s <= reviewRating
                              ? "text-amber-400 fill-amber-400"
                              : "text-gray-200"
                          }`}
                        />
                      </button>
                    ))}
                  </div>

                  <textarea
                    rows={3}
                    value={reviewComment}
                    onChange={(e) => setReviewComment(e.target.value)}
                    placeholder="Share feedback on cleanliness, punctuality, comfort..."
                    className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs text-gray-900 focus:ring-2 focus:ring-[#d84e55] focus:outline-hidden"
                  />

                  <div className="flex gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() => setSelectedBookingForReview(null)}
                      className="flex-1 py-2.5 bg-gray-100 text-gray-700 text-xs font-bold rounded-xl hover:bg-gray-200 cursor-pointer"
                    >
                      Later
                    </button>
                    <button
                      type="button"
                      disabled={isSubmittingReview}
                      onClick={handleSubmitReview}
                      className="flex-1 py-2.5 bg-[#d84e55] text-white text-xs font-black rounded-xl hover:bg-[#b83e44] shadow-xs cursor-pointer"
                    >
                      {isSubmittingReview ? "Submitting..." : "Submit Review"}
                    </button>
                  </div>
                </>
              )}
            </motion.div>
          </div>
        );
      })()}

    </div>
  );
}
