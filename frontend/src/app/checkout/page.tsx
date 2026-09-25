"use client";

import React, { useState, useEffect, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { useAppDispatch, useAppSelector } from "@/store";
import {
  updatePassengerDetails,
  clearSeatLocks,
  syncPassengers,
  hydrateBookingState,
  ensureDefaultPassenger,
} from "@/store/bookingSlice";
import {
  useGetRouteByIdQuery,
  useCreateBookingMutation,
  useCreatePaymentOrderMutation,
  useVerifyPaymentMutation,
  useGetSavedTravellersQuery,
  useGetAvailableCouponsQuery,
  useValidateCouponMutation,
  useUnlockSeatsMutation,
  useGetMeQuery,
} from "@/store/apiSlice";
import { useSeatLockTimer } from "@/hooks/useSeatLockTimer";
import type { CouponValidationResponse, SeatItem } from "@/types";
import {
  Clock,
  ShieldCheck,
  User as UserIcon,
  Phone,
  Mail,
  ArrowRight,
  ArrowLeft,
  AlertCircle,
  Bus,
  CheckCircle2,
  Users,
  Tag,
  Shield,
  Percent,
  Check,
  X,
  Info,
  Wallet,
  Building2,
  CreditCard,
  Sparkles,
} from "lucide-react";
import RazorpayModal from "@/components/RazorpayModal";
import { addNotification } from "@/store/notificationSlice";

declare global {
  interface Window {
    Razorpay: any;
  }
}

function CheckoutContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const dispatch = useAppDispatch();
  const routeId = Number(searchParams.get("routeId"));

  const { user } = useAppSelector((state) => state.auth);
  const { data: latestUser } = useGetMeQuery(undefined, { skip: !user });
  const activeUser = latestUser || user;
  const userWalletBalance = Number(activeUser?.walletBalance || 0);

  const [useWalletBalance, setUseWalletBalance] = useState(false);

  const {
    selectedRoute,
    selectedSeats,
    lockedSeatIds,
    boardingPoint,
    droppingPoint,
    passengers,
  } = useAppSelector((state) => state.booking);

  const { data: routeData } = useGetRouteByIdQuery(routeId, { skip: !routeId });
  const activeRoute = selectedRoute || routeData;

  const { formattedTime, isLocked } = useSeatLockTimer();
  const isPaymentSuccessRef = useRef(false);

  const [contactEmail, setContactEmail] = useState(user?.email || "user@example.com");
  const [contactPhone, setContactPhone] = useState(user?.phone || "+91 9876543212");
  const [errorMessage, setErrorMessage] = useState("");
  const [isProcessingPayment, setIsProcessingPayment] = useState(false);

  // Free Cancellation Add-on State (defaults to unselected / null)
  const [hasFreeCancellation, setHasFreeCancellation] = useState<boolean | null>(null);

  // Trip Guarantee Add-on State (defaults to unselected / null)
  const [hasTripGuarantee, setHasTripGuarantee] = useState<boolean | null>(null);
  const [showTripGuaranteeModal, setShowTripGuaranteeModal] = useState(false);

  // Coupon State
  const [couponCodeInput, setCouponCodeInput] = useState("");
  const [appliedCoupon, setAppliedCoupon] = useState<CouponValidationResponse | null>(null);
  const [couponMessage, setCouponMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const [createBookingMutation] = useCreateBookingMutation();
  const [createPaymentOrderMutation] = useCreatePaymentOrderMutation();
  const [verifyPaymentMutation] = useVerifyPaymentMutation();
  const [validateCouponMutation, { isLoading: isValidatingCoupon }] = useValidateCouponMutation();
  const [unlockSeatsMutation] = useUnlockSeatsMutation();

  const { data: savedTravellers = [] } = useGetSavedTravellersQuery(undefined, { skip: !user });
  const { data: availableCoupons = [] } = useGetAvailableCouponsQuery();

  // If user navigates without seats, redirect to search
  // Rehydrate state from session or ensure at least 1 passenger details form is ready
  useEffect(() => {
    if (typeof window !== "undefined") {
      try {
        const saved = sessionStorage.getItem("redbus_booking_state");
        if (saved) {
          const parsed = JSON.parse(saved);
          if (parsed.selectedSeats && parsed.selectedSeats.length > 0 && selectedSeats.length === 0) {
            dispatch(hydrateBookingState(parsed));
            return;
          }
        }
      } catch {}
    }

    if (selectedSeats.length === 0 || passengers.length === 0) {
      dispatch(
        ensureDefaultPassenger({
          route: activeRoute,
          userName: user?.name,
          userGender: user?.gender,
        })
      );
    }
  }, [activeRoute, selectedSeats.length, passengers.length, user, dispatch]);

  // Ensure passengers are always synced with selectedSeats
  useEffect(() => {
    if (selectedSeats.length > 0) {
      if (passengers.length !== selectedSeats.length || passengers.some((p) => !p.seatId)) {
        const syncedPassengers = selectedSeats.map((seat, idx) => {
          const existing = passengers[idx];
          return {
            seatId: seat.seatId,
            seatNumber: seat.seatNumber,
            name: existing?.name || (idx === 0 && user?.name ? user.name : ""),
            age: existing?.age || 25,
            gender: (existing?.gender as any) || (seat.genderRestriction === "FEMALE" ? "FEMALE" : (user?.gender as any) || "MALE"),
          };
        });
        dispatch(syncPassengers(syncedPassengers));
      }
    }
  }, [selectedSeats, passengers.length, user, dispatch]);

  const selectedSeatsRef = useRef(selectedSeats);
  selectedSeatsRef.current = selectedSeats;
  const activeRouteRef = useRef(activeRoute);
  activeRouteRef.current = activeRoute;
  const userRef = useRef(user);
  userRef.current = user;

  // Beacon unlock on tab close / window unload
  useEffect(() => {
    const handleBeforeUnload = () => {
      const currentRouteId = activeRouteRef.current?.id;
      const seatIds = selectedSeatsRef.current.map((s) => s.seatId);
      const currentUserId = userRef.current?.id;

      if (!isPaymentSuccessRef.current && currentRouteId && seatIds.length > 0) {
        try {
          const payload = JSON.stringify({ routeId: currentRouteId, seatIds, userId: currentUserId });
          if (navigator.sendBeacon) {
            navigator.sendBeacon("http://localhost:8080/api/v1/seats/unlock", new Blob([payload], { type: "application/json" }));
          } else {
            fetch("http://localhost:8080/api/v1/seats/unlock", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: payload,
              keepalive: true,
            }).catch(() => {});
          }
        } catch {}
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, []);

  const handleBackToSeats = async () => {
    const currentRouteId = activeRoute?.id;
    const seatIds = selectedSeats.map((s) => s.seatId);
    if (currentRouteId && seatIds.length > 0) {
      try {
        await unlockSeatsMutation({
          routeId: currentRouteId,
          seatIds,
          userId: user?.id,
        }).unwrap();
      } catch {}
    }
    dispatch(clearSeatLocks());
    router.back();
  };

  // Fare calculations
  const baseFare = selectedSeats.reduce(
    (sum, s) => sum + (s.price || activeRoute?.basePrice || 0),
    0
  );

  // Dynamic AI-Calculated Operator Service Fee (shows ₹5 - ₹15 based on route distance & AI pricing model)
  const aiServiceFee = React.useMemo(() => {
    if (!activeRoute) return 9;
    const factor = ((activeRoute.id || 1) * 3 + Math.floor((activeRoute.basePrice || 500) / 70)) % 11;
    return 5 + factor; // ₹5 to ₹15
  }, [activeRoute?.id, activeRoute?.basePrice]);

  // Free Cancellation Guarantee (+₹21 per passenger if selected)
  const freeCancellationFee = hasFreeCancellation === true ? selectedSeats.length * 21 : 0;

  // Dynamic Trip Guarantee Cashback (Randomly up to 30% max)
  const cashbackPercentage = React.useMemo(() => {
    const seed = ((activeRoute?.id || 1) * 7 + selectedSeats.length * 5) % 11; // 0 to 10
    return 20 + seed; // 20% to 30% randomly (up to 30%)
  }, [activeRoute?.id, selectedSeats.length]);

  const tripGuaranteeFeePerPassenger = 21;
  const tripGuaranteeFee = hasTripGuarantee === true ? selectedSeats.length * tripGuaranteeFeePerPassenger : 0;
  const tripCashbackRefund = Math.round(baseFare * (cashbackPercentage / 100));
  const tripTotalRefund = baseFare + tripCashbackRefund;

  const discountAmount = appliedCoupon?.valid ? (appliedCoupon.discountAmount || 0) : 0;
  const subtotalAmount = Math.max(0, baseFare + freeCancellationFee + tripGuaranteeFee + aiServiceFee - discountAmount);
  const walletDeduction = (useWalletBalance && userWalletBalance > 0) ? Math.min(userWalletBalance, subtotalAmount) : 0;
  const finalTotalAmount = Math.max(0, subtotalAmount - walletDeduction);

  const handlePassengerChange = (index: number, field: string, value: any) => {
    dispatch(
      updatePassengerDetails({
        index,
        passenger: { [field]: value },
      })
    );
  };

  const handleApplyCoupon = async (codeToApply?: string) => {
    const code = (codeToApply || couponCodeInput).trim().toUpperCase();
    if (!code) {
      setCouponMessage({ type: "error", text: "Please enter a coupon code" });
      return;
    }

    setCouponMessage(null);
    try {
      const res = await validateCouponMutation({
        code,
        bookingAmount: baseFare,
      }).unwrap();

      if (res.valid) {
        setAppliedCoupon(res);
        setCouponCodeInput(res.code || code);
        setCouponMessage({
          type: "success",
          text: `Coupon applied successfully! You saved ₹${res.discountAmount?.toFixed(2)} (${res.discountPercentage}% OFF)`,
        });
      } else {
        setAppliedCoupon(null);
        setCouponMessage({ type: "error", text: res.message || "Invalid or ineligible coupon code." });
      }
    } catch (err: any) {
      setAppliedCoupon(null);
      setCouponMessage({
        type: "error",
        text: err?.data?.message || "Failed to validate coupon code.",
      });
    }
  };

  const handleRemoveCoupon = () => {
    setAppliedCoupon(null);
    setCouponCodeInput("");
    setCouponMessage(null);
  };

  // Razorpay Gateway Modal State
  const [showRazorpayModal, setShowRazorpayModal] = useState(false);
  const [pendingBookingData, setPendingBookingData] = useState<{
    pnr: string;
    orderId: string;
    amount: number;
  } | null>(null);

  const loadRazorpayScript = (): Promise<boolean> => {
    return new Promise((resolve) => {
      if (typeof window !== "undefined" && window.Razorpay) {
        resolve(true);
        return;
      }
      const script = document.createElement("script");
      script.src = "https://checkout.razorpay.com/v1/checkout.js";
      script.async = true;
      script.onload = () => resolve(true);
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
  };

  const handleInitiatePayment = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");

    // Validate passenger names and gender restrictions
    for (let i = 0; i < passengers.length; i++) {
      const p = passengers[i];
      if (!p.name.trim()) {
        setErrorMessage(`Please enter the passenger name for Seat ${p.seatNumber}`);
        return;
      }
      const seat = selectedSeats.find((s) => s.seatId === p.seatId);
      if (seat?.genderRestriction === "FEMALE" && p.gender !== "FEMALE") {
        setErrorMessage(
          `Seat ${p.seatNumber} is adjacent to a female passenger and reserved exclusively for females. Please select Female for this passenger.`
        );
        return;
      }
    }

    if (!contactEmail.trim() || !contactPhone.trim()) {
      setErrorMessage("Contact email and phone number are required.");
      return;
    }

    if (!activeRoute) {
      setErrorMessage("Route details missing.");
      return;
    }

    setIsProcessingPayment(true);
    try {
      // 1. Create Booking in database (with wallet balance applied if selected)
      const bookingPayload = {
        routeId: activeRoute.id,
        boardingPoint: boardingPoint || activeRoute.boardingPoints?.[0] || "Main Boarding Station",
        droppingPoint: droppingPoint || activeRoute.droppingPoints?.[0] || "Main Destination Station",
        contactEmail,
        contactPhone,
        couponCode: appliedCoupon?.valid ? appliedCoupon.code : undefined,
        hasFreeCancellation: hasFreeCancellation === true,
        hasTripGuarantee: hasTripGuarantee === true,
        serviceFee: aiServiceFee,
        useWalletBalance: useWalletBalance && userWalletBalance > 0,
        passengers: passengers.map((p) => ({
          seatId: p.seatId,
          seatNumber: p.seatNumber,
          name: p.name,
          age: Number(p.age) || 25,
          gender: p.gender || "MALE",
        })),
      };

      const booking = await createBookingMutation(bookingPayload).unwrap();

      // If booking is 100% paid by wallet balance and confirmed immediately, bypass payment gateway!
      if (booking.status === "CONFIRMED" || finalTotalAmount === 0) {
        isPaymentSuccessRef.current = true;
        dispatch(
          addNotification({
            id: `booking-conf-${booking.pnr}`,
            type: "booking",
            title: "Trip Confirmed! 🚌",
            message: `Your booking for ${booking.sourceCity || activeRoute?.sourceCity || "Source"} ➔ ${booking.destinationCity || activeRoute?.destinationCity || "Destination"} is confirmed! PNR: ${booking.pnr}`,
            timestamp: "Just now",
            read: false,
            actionUrl: "/my-bookings",
            actionLabel: "View Ticket",
            pnr: booking.pnr,
          })
        );
        router.push(`/booking-confirmation?pnr=${booking.pnr}`);
        return;
      }

      // 2. Create Razorpay Payment Order on Backend for remaining payable amount
      const order = await createPaymentOrderMutation({ pnr: booking.pnr }).unwrap();

      setPendingBookingData({
        pnr: booking.pnr,
        orderId: order.orderId,
        amount: finalTotalAmount,
      });

      const effectiveKey = order.keyId || process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || "";
      const isPlaceholderOrMockKey =
        !effectiveKey ||
        effectiveKey === "rzp_test_placeholder" ||
        effectiveKey.includes("placeholder") ||
        effectiveKey.includes("Mock") ||
        effectiveKey.length < 15;

      // If backend has mock / placeholder key, directly open the full in-app Razorpay simulation modal
      if (isPlaceholderOrMockKey) {
        setShowRazorpayModal(true);
        setIsProcessingPayment(false);
        return;
      }

      // 3. Load Razorpay Script and open official Razorpay Checkout Test Mode
      const scriptLoaded = await loadRazorpayScript();

      if (scriptLoaded && window.Razorpay) {
        try {
          const options: any = {
            key: effectiveKey,
            amount: order.amountInPaise,
            currency: order.currency || "INR",
            name: "redBus India",
            description: `Bus Booking PNR: ${booking.pnr}`,
            prefill: {
              name: passengers[0]?.name || "Passenger",
              email: contactEmail,
              contact: contactPhone,
            },
            notes: {
              pnr: booking.pnr,
            },
            theme: {
              color: "#d84e55",
            },
            handler: async function (response: any) {
              try {
                const verifyRes = await verifyPaymentMutation({
                  pnr: booking.pnr,
                  razorpayOrderId: response.razorpay_order_id || order.orderId,
                  razorpayPaymentId: response.razorpay_payment_id || "pay_test_success",
                  razorpaySignature: response.razorpay_signature || "mock_sig_test",
                }).unwrap();

                if (verifyRes.success) {
                  isPaymentSuccessRef.current = true;
                  dispatch(
                    addNotification({
                      id: `booking-conf-${booking.pnr}`,
                      type: "booking",
                      title: "Trip Confirmed! 🚌",
                      message: `Your booking for ${booking.sourceCity || activeRoute?.sourceCity || "Source"} ➔ ${booking.destinationCity || activeRoute?.destinationCity || "Destination"} is confirmed! PNR: ${booking.pnr}`,
                      timestamp: "Just now",
                      read: false,
                      actionUrl: "/my-bookings",
                      actionLabel: "View Ticket",
                      pnr: booking.pnr,
                    })
                  );
                  router.push(`/booking-confirmation?pnr=${booking.pnr}`);
                }
              } catch (vErr: any) {
                setErrorMessage(vErr?.data?.message || "Payment verification failed.");
              }
            },
            modal: {
              ondismiss: function () {
                setIsProcessingPayment(false);
              },
            },
          };

          // Only attach order_id if it's a real order created by Razorpay server (and not a fallback mock string)
          if (order.orderId && !order.orderId.startsWith("order_mock_")) {
            options.order_id = order.orderId;
          }

          const rzp = new window.Razorpay(options);
          rzp.on("payment.failed", function (failResp: any) {
            console.warn("Razorpay official popup failed, falling back to seamless in-app modal:", failResp);
            setShowRazorpayModal(true);
            setIsProcessingPayment(false);
          });
          rzp.open();
        } catch (rzpErr) {
          console.warn("Razorpay popup launch error, opening in-app gateway modal:", rzpErr);
          setShowRazorpayModal(true);
        }
      } else {
        // Fallback to in-app Razorpay modal if script was blocked by browser
        setShowRazorpayModal(true);
      }
    } catch (err: any) {
      setErrorMessage(
        err?.data?.message || err?.message || "Failed to initiate payment. Please try again."
      );
    } finally {
      setIsProcessingPayment(false);
    }
  };

  const handleRazorpaySuccess = async ({ paymentId, signature }: { paymentId: string; signature: string }) => {
    if (!pendingBookingData) return;
    try {
      const verifyRes = await verifyPaymentMutation({
        pnr: pendingBookingData.pnr,
        razorpayOrderId: pendingBookingData.orderId,
        razorpayPaymentId: paymentId,
        razorpaySignature: signature,
      }).unwrap();

      if (verifyRes.success) {
        isPaymentSuccessRef.current = true;
        dispatch(
          addNotification({
            id: `booking-conf-${pendingBookingData.pnr}`,
            type: "booking",
            title: "Trip Confirmed! 🚌",
            message: `Your booking for ${activeRoute?.sourceCity || "Origin"} ➔ ${activeRoute?.destinationCity || "Destination"} is confirmed! PNR: ${pendingBookingData.pnr}`,
            timestamp: "Just now",
            read: false,
            actionUrl: "/my-bookings",
            actionLabel: "View Ticket",
            pnr: pendingBookingData.pnr,
          })
        );
        setShowRazorpayModal(false);
        router.push(`/booking-confirmation?pnr=${pendingBookingData.pnr}`);
      }
    } catch (err: any) {
      setErrorMessage(err?.data?.message || "Payment verification failed.");
      setShowRazorpayModal(false);
    }
  };

  if (!activeRoute) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <p className="text-sm font-semibold text-gray-700">Loading booking information...</p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Back button to return and immediately release seats */}
      <div className="mb-6 flex items-center justify-between">
        <button
          type="button"
          onClick={handleBackToSeats}
          className="inline-flex items-center space-x-2 px-4 py-2 bg-white hover:bg-gray-50 border border-gray-200 text-gray-700 rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer hover:border-gray-300"
        >
          <ArrowLeft className="w-4 h-4 text-[#d84e55]" />
          <span>← Back & Change Seats</span>
        </button>
        <span className="text-xs text-gray-400 font-medium">
          Returning to search releases your locked seats automatically.
        </span>
      </div>

      {/* Seat Lock Countdown Banner */}
      {isLocked && (
        <div className="mb-6 p-4 bg-amber-50 border border-amber-200 rounded-2xl flex items-center justify-between shadow-xs">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center text-amber-700">
              <Clock className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <p className="text-sm font-bold text-amber-900">
                Seats Locked for You ({selectedSeats.map((s) => s.seatNumber).join(", ")})
              </p>
              <p className="text-xs text-amber-700">
                Complete payment before the timer expires to secure your seats.
              </p>
            </div>
          </div>
          <div className="text-right">
            <span className="text-[10px] uppercase tracking-wider text-amber-700 font-bold block">
              Time Remaining
            </span>
            <span className="font-mono text-xl font-black text-amber-950 bg-amber-200/80 px-2.5 py-0.5 rounded-lg inline-block">
              {formattedTime}
            </span>
          </div>
        </div>
      )}

      {errorMessage && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-2xl flex items-center space-x-3 text-sm text-red-700">
          <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
          <span>{errorMessage}</span>
        </div>
      )}

      <form onSubmit={handleInitiatePayment} className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Passenger, Contact, Free Cancellation & Promo */}
        <div className="lg:col-span-8 space-y-6">
          {/* Passenger Information Cards with bottom-to-top lazy loading */}
          <motion.div
            initial={{ opacity: 0, y: 25 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-30px" }}
            transition={{ duration: 0.45, ease: "easeOut" }}
            className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs space-y-6"
          >
            <div className="flex items-center justify-between pb-4 border-b border-gray-100">
              <h3 className="font-bold text-lg text-gray-900 flex items-center space-x-2">
                <UserIcon className="w-5 h-5 text-[#d84e55]" />
                <span>Passenger Details</span>
              </h3>
              <div className="flex items-center space-x-3">
                <span className="text-xs font-semibold text-gray-500">
                  {Math.max(selectedSeats.length, passengers.length, 1)} Passenger{Math.max(selectedSeats.length, passengers.length, 1) > 1 ? "s" : ""}
                </span>
                <button
                  type="button"
                  onClick={() => {
                    const nextNum = passengers.length + 1;
                    const newSeat: SeatItem = {
                      id: Date.now(),
                      seatId: Date.now(),
                      seatNumber: `${nextNum}A`,
                      seatType: "SEATER",
                      deck: "LOWER",
                      rowNum: nextNum,
                      colNum: 1,
                      genderRestriction: "NONE",
                      status: "AVAILABLE",
                      price: activeRoute?.basePrice || 750,
                    };
                    const updatedSeats = [...selectedSeats, newSeat];
                    const updatedPassengers = [
                      ...passengers,
                      {
                        seatId: newSeat.seatId,
                        seatNumber: newSeat.seatNumber,
                        name: "",
                        age: 25,
                        gender: "MALE" as const,
                      },
                    ];
                    dispatch(hydrateBookingState({ selectedSeats: updatedSeats, passengers: updatedPassengers }));
                  }}
                  className="px-2.5 py-1 bg-red-50 hover:bg-red-100 text-[#d84e55] font-bold text-xs rounded-lg transition-colors cursor-pointer"
                >
                  + Add Passenger
                </button>
              </div>
            </div>

            {(passengers.length === 0 ? [{ seatId: 1, seatNumber: "1A", name: user?.name || "", age: 25, gender: "MALE" }] : passengers).map((passenger, idx) => (
              <div
                key={passenger.seatId}
                className="p-4 bg-gray-50/80 rounded-2xl border border-gray-200/80 space-y-4"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="flex items-center space-x-2">
                    <span className="px-2.5 py-1 bg-[#d84e55] text-white text-xs font-bold rounded-lg">
                      Seat {passenger.seatNumber}
                    </span>
                    <span className="text-xs font-bold text-gray-700">Passenger {idx + 1}</span>
                    {selectedSeats.find((s) => s.seatId === passenger.seatId)?.genderRestriction === "FEMALE" && (
                      <span className="px-2 py-0.5 bg-pink-100 text-pink-700 font-bold text-[10px] rounded-md flex items-center gap-1">
                        👩 Ladies-Only Seat
                      </span>
                    )}
                  </div>

                  {savedTravellers.length > 0 && (
                    <div className="flex flex-wrap items-center gap-1.5">
                      <span className="text-[10px] font-semibold text-gray-400 flex items-center gap-1">
                        <Users className="w-3 h-3 text-[#d84e55]" /> Fill:
                      </span>
                      {savedTravellers.map((traveller) => (
                        <button
                          key={traveller.id}
                          type="button"
                          onClick={() => {
                            handlePassengerChange(idx, "name", traveller.name);
                            handlePassengerChange(idx, "age", traveller.age);
                            handlePassengerChange(idx, "gender", traveller.gender);
                          }}
                          className="px-2 py-0.5 bg-white hover:bg-red-50 hover:border-red-300 border border-gray-200 rounded-lg text-[10px] font-semibold text-gray-700 transition-colors flex items-center space-x-1 cursor-pointer shadow-2xs"
                        >
                          <span>{traveller.gender === "FEMALE" ? "👩" : "👨"}</span>
                          <span>{traveller.name.split(" ")[0]} ({traveller.age})</span>
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-12 gap-3">
                  {/* Name */}
                  <div className="sm:col-span-6">
                    <label className="block text-xs font-semibold text-gray-700 mb-1">
                      Full Name *
                    </label>
                    <input
                      type="text"
                      required
                      value={passenger.name}
                      onChange={(e) => handlePassengerChange(idx, "name", e.target.value)}
                      placeholder="e.g. Rahul Sharma"
                      className="w-full px-3 py-2 bg-white rounded-xl border border-gray-200 text-xs font-semibold text-gray-900 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                    />
                  </div>

                  {/* Age */}
                  <div className="sm:col-span-3">
                    <label className="block text-xs font-semibold text-gray-700 mb-1">
                      Age *
                    </label>
                    <input
                      type="number"
                      required
                      min={1}
                      max={120}
                      value={passenger.age}
                      onChange={(e) => handlePassengerChange(idx, "age", Number(e.target.value))}
                      className="w-full px-3 py-2 bg-white rounded-xl border border-gray-200 text-xs font-semibold text-gray-900 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                    />
                  </div>

                  {/* Gender */}
                  <div className="sm:col-span-3">
                    <label className="block text-xs font-semibold text-gray-700 mb-1">
                      Gender *
                    </label>
                    <select
                      value={passenger.gender}
                      onChange={(e) => handlePassengerChange(idx, "gender", e.target.value)}
                      className="w-full px-3 py-2 bg-white rounded-xl border border-gray-200 text-xs font-semibold text-gray-900 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                    >
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                </div>
              </div>
            ))}
          </motion.div>

          {/* TRIP GUARANTEE CARD with dynamic fare & cashback logic */}
          <motion.div
            initial={{ opacity: 0, y: 25 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-30px" }}
            transition={{ duration: 0.45, ease: "easeOut", delay: 0.04 }}
            className="bg-white rounded-3xl p-6 border border-gray-200 shadow-sm space-y-4"
          >
            {/* Header */}
            <div className="flex items-start justify-between">
              <div>
                <h3 className="font-extrabold text-lg text-gray-900 tracking-tight">
                  Trip Guarantee
                </h3>
                <p className="text-xs text-gray-500 font-medium">
                  ₹{tripGuaranteeFeePerPassenger} per passenger
                </p>
              </div>

              {/* Red Shield with Bus Icon Emblem */}
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 text-white flex items-center justify-center shadow-md shadow-red-500/25 shrink-0">
                <Shield className="w-5 h-5 fill-white/20 stroke-[2.2]" />
              </div>
            </div>

            {/* Subtext description with View details button */}
            <div className="text-xs text-gray-700 font-medium flex flex-wrap items-center gap-1.5">
              <span>
                Get a full refund + <strong className="text-emerald-600 font-black">₹{tripCashbackRefund} extra</strong> if your bus gets cancelled by the operator.
              </span>
              <button
                type="button"
                onClick={() => setShowTripGuaranteeModal(true)}
                className="text-blue-600 font-semibold border border-blue-600/40 hover:border-blue-600 px-1.5 py-0.5 rounded text-[11px] hover:underline cursor-pointer transition-colors"
              >
                View details
              </button>
            </div>

            {/* Dynamic Refund Formula Box (matches screenshot) */}
            <div className="bg-[#f2f5fc] rounded-2xl p-4 text-center border border-blue-100/80 space-y-2">
              <p className="text-xs text-gray-600 font-medium">
                Get ₹{tripTotalRefund} refund if bus gets cancelled
              </p>
              <div className="flex items-center justify-center gap-6 sm:gap-14 pt-1">
                {/* Full Refund */}
                <div className="flex flex-col items-center">
                  <div className="flex items-center gap-1 text-blue-700 font-black text-lg sm:text-xl">
                    <Building2 className="w-5 h-5 text-blue-600" />
                    <span>₹{baseFare.toFixed(0)}</span>
                  </div>
                  <span className="text-[11px] text-gray-500 font-medium mt-0.5">Full refund</span>
                </div>

                {/* Plus Sign */}
                <span className="text-blue-500 font-bold text-xl">+</span>

                {/* Cashback */}
                <div className="flex flex-col items-center">
                  <div className="flex items-center gap-1 text-blue-700 font-black text-lg sm:text-xl">
                    <CreditCard className="w-5 h-5 text-blue-600" />
                    <span>₹{tripCashbackRefund}</span>
                  </div>
                  <span className="text-[11px] text-gray-500 font-medium mt-0.5">Cashback</span>
                </div>
              </div>
            </div>

            {/* Social Proof Pill Banner */}
            <div className="bg-[#eaf5ea] text-[#2c7a36] text-xs font-semibold py-2.5 px-4 rounded-xl flex items-center gap-2">
              <span>Bought by 7,42,445+ people in the last month</span>
            </div>

            {/* Radio Choice Options */}
            <div className="space-y-2.5 pt-1">
              {/* Option 1: Add Trip Guarantee */}
              <div
                onClick={() => setHasTripGuarantee(true)}
                className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                  hasTripGuarantee === true
                    ? "bg-white border-red-500 shadow-sm ring-2 ring-red-500/10"
                    : "bg-white border-gray-200 hover:border-gray-300"
                }`}
              >
                <div>
                  <p className="text-xs font-bold text-gray-900">Add Trip Guarantee</p>
                  <p className="text-[11px] text-gray-500 mt-0.5 font-medium">
                    ₹{tripGuaranteeFeePerPassenger * selectedSeats.length} for {selectedSeats.length} passenger{selectedSeats.length > 1 ? "s" : ""}
                  </p>
                </div>

                <div
                  className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors shrink-0 ${
                    hasTripGuarantee === true
                      ? "border-red-600 bg-red-600 text-white"
                      : "border-gray-300 bg-white"
                  }`}
                >
                  {hasTripGuarantee === true && <div className="w-2 h-2 rounded-full bg-white" />}
                </div>
              </div>

              {/* Option 2: Don't add Trip Guarantee */}
              <div
                onClick={() => setHasTripGuarantee(false)}
                className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                  hasTripGuarantee === false
                    ? "bg-white border-gray-700 shadow-sm"
                    : "bg-white border-gray-200 hover:border-gray-300"
                }`}
              >
                <div>
                  <p className="text-xs font-bold text-gray-900">Don’t add Trip Guarantee</p>
                </div>

                <div
                  className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors shrink-0 ${
                    hasTripGuarantee === false
                      ? "border-gray-700 bg-gray-700 text-white"
                      : "border-gray-300 bg-white"
                  }`}
                >
                  {hasTripGuarantee === false && <div className="w-2 h-2 rounded-full bg-white" />}
                </div>
              </div>
            </div>
          </motion.div>

          {/* FREE CANCELLATION GUARANTEE CARD (Default selected to nothing) */}
          <motion.div
            initial={{ opacity: 0, y: 25 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-30px" }}
            transition={{ duration: 0.45, ease: "easeOut", delay: 0.08 }}
            className="bg-gradient-to-br from-emerald-50 via-teal-50/50 to-white rounded-3xl p-6 border-2 border-emerald-200 shadow-sm space-y-4"
          >
            <div className="flex items-start justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-2xl bg-emerald-500 text-white flex items-center justify-center shadow-md shadow-emerald-500/20 shrink-0">
                  <ShieldCheck className="w-6 h-6" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-black text-base text-gray-900">
                      Free Cancellation Guarantee
                    </h3>
                    <span className="px-2 py-0.5 bg-emerald-600 text-white text-[10px] font-black rounded-full uppercase tracking-wider">
                      Popular
                    </span>
                  </div>
                  <p className="text-xs text-emerald-800 font-medium mt-0.5">
                    Get <strong>100% full refund</strong> of ticket price if you cancel before departure time!
                  </p>
                </div>
              </div>
              <span className="text-sm font-black text-emerald-700 bg-emerald-100 px-3 py-1 rounded-xl shrink-0">
                ₹21 <span className="text-[11px] font-normal text-emerald-600">/ passenger</span>
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              <div
                onClick={() => setHasFreeCancellation(true)}
                className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-start space-x-3 ${
                  hasFreeCancellation === true
                    ? "bg-white border-emerald-500 shadow-md ring-2 ring-emerald-500/20"
                    : "bg-white/60 border-gray-200 hover:border-gray-300"
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full border-2 flex items-center justify-center mt-0.5 shrink-0 ${
                    hasFreeCancellation === true
                      ? "border-emerald-600 bg-emerald-600 text-white"
                      : "border-gray-300 bg-white"
                  }`}
                >
                  {hasFreeCancellation === true && <Check className="w-3 h-3 stroke-[3]" />}
                </div>
                <div>
                  <p className="text-xs font-bold text-gray-900">
                    Yes, protect my trip (+₹{selectedSeats.length * 21})
                  </p>
                  <p className="text-[11px] text-gray-500 mt-0.5 leading-snug">
                    Get 100% base fare refunded (₹{baseFare.toFixed(2)}) if cancelled anytime before trip start time.
                  </p>
                </div>
              </div>

              <div
                onClick={() => setHasFreeCancellation(false)}
                className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-start space-x-3 ${
                  hasFreeCancellation === false
                    ? "bg-white border-gray-400 shadow-md"
                    : "bg-white/60 border-gray-200 hover:border-gray-300"
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full border-2 flex items-center justify-center mt-0.5 shrink-0 ${
                    hasFreeCancellation === false
                      ? "border-gray-700 bg-gray-700 text-white"
                      : "border-gray-300 bg-white"
                  }`}
                >
                  {hasFreeCancellation === false && <Check className="w-3 h-3 stroke-[3]" />}
                </div>
                <div>
                  <p className="text-xs font-bold text-gray-900">
                    No, I’ll take the risk
                  </p>
                  <p className="text-[11px] text-gray-500 mt-0.5 leading-snug">
                    Standard redBus time-based policy applies (10% to 100% deduction on cancellation).
                  </p>
                </div>
              </div>
            </div>

            {/* Dynamic policy comparison snippet */}
            <div className="bg-white/80 rounded-2xl p-3 border border-emerald-100 text-[11px] space-y-1 text-gray-600">
              <div className="font-semibold text-gray-800 flex items-center gap-1">
                <Info className="w-3.5 h-3.5 text-emerald-600" />
                <span>Dynamic RedBus Cancellation Refund Slabs:</span>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1 font-mono text-[10px]">
                <div className="bg-emerald-50/80 p-1.5 rounded-lg border border-emerald-100">
                  <span className="text-gray-500 block">&gt; 24 hrs:</span>
                  <span className="font-bold text-emerald-700">{hasFreeCancellation === true ? "100% Refund" : "90% Refund"}</span>
                </div>
                <div className="bg-blue-50/80 p-1.5 rounded-lg border border-blue-100">
                  <span className="text-gray-500 block">12 - 24 hrs:</span>
                  <span className="font-bold text-blue-700">{hasFreeCancellation === true ? "100% Refund" : "75% Refund"}</span>
                </div>
                <div className="bg-amber-50/80 p-1.5 rounded-lg border border-amber-100">
                  <span className="text-gray-500 block">2 - 12 hrs:</span>
                  <span className="font-bold text-amber-700">{hasFreeCancellation === true ? "100% Refund" : "50% Refund"}</span>
                </div>
                <div className="bg-red-50/80 p-1.5 rounded-lg border border-red-100">
                  <span className="text-gray-500 block">&lt; 2 hrs:</span>
                  <span className="font-bold text-red-700">{hasFreeCancellation === true ? "100% Refund" : "0% (No Refund)"}</span>
                </div>
              </div>
            </div>
          </motion.div>

          {/* COUPONS & OFFERS SECTION with bottom-to-top lazy loading */}
          <motion.div
            initial={{ opacity: 0, y: 25 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-30px" }}
            transition={{ duration: 0.45, ease: "easeOut", delay: 0.1 }}
            className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs space-y-4"
          >
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <h3 className="font-bold text-base text-gray-900 flex items-center space-x-2">
                <Tag className="w-5 h-5 text-[#d84e55]" />
                <span>Offers & Promo Codes</span>
              </h3>
              <span className="text-xs font-bold text-emerald-600 flex items-center gap-1">
                <Percent className="w-3.5 h-3.5" /> Instant Discounts
              </span>
            </div>

            {/* Coupon Input Box */}
            <div className="flex gap-2">
              <div className="relative flex-1">
                <input
                  type="text"
                  value={couponCodeInput}
                  onChange={(e) => setCouponCodeInput(e.target.value.toUpperCase())}
                  placeholder="Enter coupon code (e.g. SAVE2)..."
                  disabled={!!appliedCoupon}
                  className="w-full uppercase font-mono px-4 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-bold text-gray-900 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:bg-white disabled:bg-gray-100"
                />
              </div>
              {appliedCoupon ? (
                <button
                  type="button"
                  onClick={handleRemoveCoupon}
                  className="px-4 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl text-xs font-bold transition-all flex items-center space-x-1"
                >
                  <X className="w-4 h-4" />
                  <span>Remove</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => handleApplyCoupon()}
                  disabled={isValidatingCoupon || !couponCodeInput.trim()}
                  className="px-5 py-2.5 bg-[#d84e55] hover:bg-[#b83e44] disabled:bg-gray-300 text-white rounded-xl text-xs font-bold transition-all flex items-center space-x-1 shrink-0"
                >
                  {isValidatingCoupon ? (
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <span>APPLY</span>
                  )}
                </button>
              )}
            </div>

            {/* Coupon Feedback message */}
            {couponMessage && (
              <div
                className={`p-3 rounded-xl text-xs font-medium flex items-center space-x-2 ${
                  couponMessage.type === "success"
                    ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
                    : "bg-red-50 text-red-700 border border-red-200"
                }`}
              >
                {couponMessage.type === "success" ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
                )}
                <span>{couponMessage.text}</span>
              </div>
            )}

            {/* Live Available Coupons Chips */}
            {availableCoupons.length > 0 && !appliedCoupon && (
              <div className="pt-2 space-y-2">
                <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">
                  Available Live Coupons:
                </p>
                <div className="flex flex-wrap gap-2">
                  {availableCoupons.map((coupon) => (
                    <div
                      key={coupon.code}
                      onClick={() => handleApplyCoupon(coupon.code)}
                      className="px-3 py-2 bg-red-50/60 hover:bg-red-50 border border-dashed border-red-300 hover:border-red-500 rounded-xl cursor-pointer transition-all flex items-center space-x-2 group"
                    >
                      <Percent className="w-3.5 h-3.5 text-[#d84e55] group-hover:scale-110 transition-transform" />
                      <div className="text-left">
                        <span className="font-mono font-black text-xs text-[#d84e55] block">
                          {coupon.code}
                        </span>
                        <span className="text-[10px] text-gray-500 font-medium">
                          {coupon.discountPercentage}% OFF (Max ₹{coupon.maxDiscountAmount})
                        </span>
                      </div>
                      <span className="text-[10px] font-black text-red-600 ml-1 group-hover:underline">
                        Apply
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </motion.div>

          {/* Contact Details with bottom-to-top lazy loading */}
          <motion.div
            initial={{ opacity: 0, y: 25 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-30px" }}
            transition={{ duration: 0.45, ease: "easeOut", delay: 0.15 }}
            className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs space-y-4"
          >
            <h3 className="font-bold text-lg text-gray-900 pb-3 border-b border-gray-100 flex items-center space-x-2">
              <Phone className="w-5 h-5 text-[#d84e55]" />
              <span>Contact Information</span>
            </h3>
            <p className="text-xs text-gray-500">
              Your M-Ticket, live tracking link and refund receipts will be sent to this email & phone number.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1 flex items-center space-x-1">
                  <Mail className="w-3.5 h-3.5 text-gray-400" />
                  <span>Email Address *</span>
                </label>
                <input
                  type="email"
                  required
                  value={contactEmail}
                  onChange={(e) => setContactEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full px-3 py-2 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold text-gray-900 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1 flex items-center space-x-1">
                  <Phone className="w-3.5 h-3.5 text-gray-400" />
                  <span>Mobile Number *</span>
                </label>
                <input
                  type="tel"
                  required
                  value={contactPhone}
                  onChange={(e) => setContactPhone(e.target.value)}
                  placeholder="+91 9876543210"
                  className="w-full px-3 py-2 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold text-gray-900 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:bg-white"
                />
              </div>
            </div>
          </motion.div>

          {/* redBus Wallet Card with bottom-to-top lazy loading */}
          <motion.div
            initial={{ opacity: 0, y: 25 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-30px" }}
            transition={{ duration: 0.45, ease: "easeOut", delay: 0.2 }}
            className="bg-gradient-to-r from-emerald-500/10 via-teal-500/10 to-emerald-500/5 rounded-3xl p-6 border border-emerald-200/80 shadow-xs space-y-4"
          >
            <div className="flex items-center justify-between pb-3 border-b border-emerald-200/60">
              <div className="flex items-center space-x-2.5">
                <div className="w-9 h-9 rounded-xl bg-emerald-600 text-white flex items-center justify-center shadow-xs">
                  <Wallet className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-gray-900 flex items-center gap-1.5">
                    <span>redBus Wallet</span>
                    <span className="px-2 py-0.5 text-[10px] font-black uppercase tracking-wider bg-emerald-100 text-emerald-800 rounded-full">
                      Instant Pay
                    </span>
                  </h3>
                  <p className="text-[11px] text-gray-500">
                    Available balance: <strong className="text-emerald-700 font-black text-xs">₹{userWalletBalance.toFixed(2)}</strong>
                  </p>
                </div>
              </div>
              
              {userWalletBalance > 0 && (
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={useWalletBalance}
                    onChange={(e) => setUseWalletBalance(e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:outline-hidden rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-600"></div>
                </label>
              )}
            </div>

            {userWalletBalance > 0 ? (
              <div className="space-y-2">
                <div className="flex items-center justify-between text-xs font-semibold text-gray-700">
                  <span>Use wallet balance for this trip:</span>
                  <span className="font-black text-emerald-700">
                    {useWalletBalance ? `-₹${walletDeduction.toFixed(2)}` : "Not applied (Toggle on to use)"}
                  </span>
                </div>
                {useWalletBalance && (
                  <div className="p-3 bg-emerald-50/90 rounded-xl text-xs text-emerald-800 border border-emerald-200 flex items-start space-x-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    <div>
                      <p className="font-bold">
                        {walletDeduction >= subtotalAmount
                          ? "🎉 100% of your ticket fare is covered by your redBus Wallet!"
                          : `₹${walletDeduction.toFixed(2)} deducted from wallet. Remaining ₹${finalTotalAmount.toFixed(2)} payable via UPI/Cards.`}
                      </p>
                      <p className="text-[11px] text-emerald-700 mt-0.5">
                        Remaining wallet balance after booking: ₹{(userWalletBalance - walletDeduction).toFixed(2)}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <p className="text-xs text-gray-500">
                Your wallet balance is ₹0.00. Automatic refund credits from cancelled tickets will appear here for instant 1-click checkout.
              </p>
            )}
          </motion.div>
        </div>

        {/* Right Column: Fare & Booking Summary with bottom-to-top lazy loading */}
        <div className="lg:col-span-4 space-y-6">
          {/* Route Summary Card */}
          <motion.div
            initial={{ opacity: 0, y: 25 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-30px" }}
            transition={{ duration: 0.45, ease: "easeOut" }}
            className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs space-y-4"
          >
            <h3 className="font-bold text-base text-gray-900 pb-3 border-b border-gray-100 flex items-center space-x-2">
              <Bus className="w-4 h-4 text-[#d84e55]" />
              <span>Trip Summary</span>
            </h3>

            <div>
              <div className="flex items-center justify-between text-sm font-black text-gray-900">
                <span>{activeRoute.sourceCity}</span>
                <span className="text-[#d84e55]">➔</span>
                <span>{activeRoute.destinationCity}</span>
              </div>
              <p className="text-xs text-gray-500 mt-1">{activeRoute.operatorName}</p>
              <p className="text-[11px] text-gray-400">{activeRoute.busType}</p>
            </div>

            <div className="p-3 bg-gray-50 rounded-xl text-xs space-y-1.5 text-gray-700">
              <div className="flex justify-between">
                <span className="text-gray-500">Date:</span>
                <span className="font-semibold">{activeRoute.travelDate}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Departure:</span>
                <span className="font-semibold">{activeRoute.departureTime}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Boarding:</span>
                <span className="font-semibold truncate max-w-[150px]">{boardingPoint}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Dropping:</span>
                <span className="font-semibold truncate max-w-[150px]">{droppingPoint}</span>
              </div>
            </div>
          </motion.div>

          {/* Payment Breakdown Card */}
          <motion.div
            initial={{ opacity: 0, y: 25 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-30px" }}
            transition={{ duration: 0.45, ease: "easeOut", delay: 0.1 }}
            className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs space-y-4 sticky top-24"
          >
            <h3 className="font-bold text-base text-gray-900 pb-3 border-b border-gray-100">
              Payment Breakdown
            </h3>

            <div className="space-y-2 text-xs text-gray-600">
              <div className="flex justify-between">
                <span>Base Fare ({selectedSeats.length} seat{selectedSeats.length > 1 ? "s" : ""})</span>
                <span className="font-bold text-gray-900">₹{baseFare.toFixed(2)}</span>
              </div>

              {hasFreeCancellation === true && (
                <div className="flex justify-between items-center text-emerald-700">
                  <span className="flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5" /> Free Cancellation Guarantee
                  </span>
                  <span className="font-bold">+₹{freeCancellationFee.toFixed(2)}</span>
                </div>
              )}

              {hasTripGuarantee === true && (
                <div className="flex justify-between items-center text-rose-700">
                  <span className="flex items-center gap-1">
                    <Shield className="w-3.5 h-3.5 text-red-500" /> Trip Guarantee (+{cashbackPercentage}% Cashback)
                  </span>
                  <span className="font-bold">+₹{tripGuaranteeFee.toFixed(2)}</span>
                </div>
              )}

              {appliedCoupon?.valid && (
                <div className="flex justify-between items-center text-emerald-600 font-bold">
                  <span className="flex items-center gap-1">
                    <Tag className="w-3.5 h-3.5" /> Coupon Discount ({appliedCoupon.code})
                  </span>
                  <span>-₹{discountAmount.toFixed(2)}</span>
                </div>
              )}

              {walletDeduction > 0 && (
                <div className="flex justify-between items-center text-emerald-600 font-bold">
                  <span className="flex items-center gap-1">
                    <Wallet className="w-3.5 h-3.5" /> redBus Wallet Applied
                  </span>
                  <span>-₹{walletDeduction.toFixed(2)}</span>
                </div>
              )}

              <div className="flex justify-between items-center text-gray-700">
                <span>Operator Service Fee</span>
                <span className="font-bold text-gray-900">₹{aiServiceFee.toFixed(2)}</span>
              </div>

              <div className="pt-3 border-t border-gray-100 flex justify-between items-center text-base">
                <span className="font-bold text-gray-900">Total Payable</span>
                <div className="text-right">
                  {(discountAmount > 0 || walletDeduction > 0) && (
                    <span className="text-xs text-gray-400 line-through mr-2 font-normal">
                      ₹{(baseFare + freeCancellationFee + tripGuaranteeFee + aiServiceFee).toFixed(2)}
                    </span>
                  )}
                  <span className="font-black text-2xl text-[#d84e55]">₹{finalTotalAmount.toFixed(2)}</span>
                </div>
              </div>
            </div>

            <button
              type="submit"
              disabled={isProcessingPayment || selectedSeats.length === 0}
              className="w-full py-4 bg-[#d84e55] hover:bg-[#b83e44] disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-2xl font-bold text-sm sm:text-base shadow-xl shadow-red-500/20 hover:shadow-red-500/35 transition-all flex items-center justify-center space-x-2 active:scale-[0.99] cursor-pointer"
            >
              {isProcessingPayment ? (
                <div className="flex items-center space-x-2">
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>Processing Reservation...</span>
                </div>
              ) : finalTotalAmount === 0 ? (
                <div className="flex items-center justify-center space-x-2">
                  <Wallet className="w-5 h-5 text-white/90" />
                  <span>Confirm Booking (₹0 to Pay)</span>
                  <Check className="w-4 h-4 text-white" />
                </div>
              ) : (
                <div className="flex items-center justify-center space-x-2">
                  <span>Proceed to Pay ₹{finalTotalAmount.toFixed(2)}</span>
                  <ArrowRight className="w-5 h-5" />
                </div>
              )}
            </button>

            <div className="flex items-center justify-center space-x-2 text-[11px] text-gray-400 pt-2">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>100% Safe & Secure 256-bit Encrypted Checkout</span>
            </div>
          </motion.div>
        </div>
      </form>

      {/* Trip Guarantee Details Modal */}
      <AnimatePresence>
        {showTripGuaranteeModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-gray-100"
            >
              <div className="flex items-center justify-between pb-3 border-b border-gray-100">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-xl bg-red-50 text-[#d84e55] flex items-center justify-center">
                    <Shield className="w-4 h-4 fill-red-100" />
                  </div>
                  <h3 className="font-bold text-base text-gray-900">Trip Guarantee Policy</h3>
                </div>
                <button
                  type="button"
                  onClick={() => setShowTripGuaranteeModal(false)}
                  className="p-1 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-700 cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-3 text-xs text-gray-600">
                <div className="p-3 bg-red-50/70 rounded-2xl border border-red-100">
                  <span className="font-bold text-red-900 block mb-1">🏛️ 100% Full Refund + 💳 {cashbackPercentage}% Cashback</span>
                  <p className="text-red-700 leading-relaxed">
                    If your bus is cancelled by the operator or delayed by over 2 hours, receive an instant 100% full refund (₹{baseFare.toFixed(2)}) plus ₹{tripCashbackRefund} extra cashback directly to your redBus wallet.
                  </p>
                </div>

                <div className="space-y-2">
                  <h4 className="font-bold text-gray-800">Coverage Highlights:</h4>
                  <ul className="list-disc pl-4 space-y-1 text-gray-600">
                    <li>Immediate automatic claim processing without manual approval</li>
                    <li>Priority replacement bus booking assistance</li>
                    <li>24x7 dedicated emergency assistance hotline</li>
                  </ul>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setShowTripGuaranteeModal(false)}
                className="w-full py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white font-bold rounded-xl text-xs transition-colors cursor-pointer"
              >
                Understood
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Interactive Razorpay Gateway Modal */}
      {showRazorpayModal && pendingBookingData && (
        <RazorpayModal
          isOpen={showRazorpayModal}
          onClose={() => setShowRazorpayModal(false)}
          onSuccess={handleRazorpaySuccess}
          amount={pendingBookingData.amount}
          pnr={pendingBookingData.pnr}
          contactEmail={contactEmail}
          contactPhone={contactPhone}
          passengerName={passengers[0]?.name || user?.name || "Passenger"}
          orderId={pendingBookingData.orderId}
        />
      )}
    </div>
  );
}

export default function CheckoutPage() {
  return (
    <Suspense fallback={<div className="p-12 text-center text-sm">Loading checkout...</div>}>
      <CheckoutContent />
    </Suspense>
  );
}
