"use client";

import React, { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { useAppDispatch, useAppSelector } from "@/store";
import {
  setSelectedRoute,
  prepareCheckout,
  toggleSeatSelection,
  setBoardingPoint,
  setDroppingPoint,
} from "@/store/bookingSlice";
import {
  useGetRouteSeatsQuery,
  useGetBusReviewsQuery,
  useGetBusDelayPredictionQuery,
  useGetDynamicPriceQuoteQuery,
} from "@/store/apiSlice";
import type { RouteItem, SeatItem } from "@/types";
import WriteReviewModal from "./WriteReviewModal";
import BusImageSlider from "./BusImageSlider";
import {
  X,
  Star,
  Clock,
  MapPin,
  Shield,
  Coffee,
  Wifi,
  Zap,
  Tv,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  ChevronRight,
  ChevronLeft,
  User as UserIcon,
  ShieldCheck,
  Award,
  CircleDot,
  Radio,
  Sparkles,
  Camera,
  TrendingUp,
} from "lucide-react";

interface BusDetailsModalProps {
  route: RouteItem;
  isOpen: boolean;
  onClose: () => void;
  initialTab?: string;
}

export default function BusDetailsModal({
  route,
  isOpen,
  onClose,
  initialTab = "cancellation",
}: BusDetailsModalProps) {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const tabsRef = useRef<HTMLDivElement>(null);
  const { user } = useAppSelector((state) => state.auth);
  const { selectedSeats, boardingPoint, droppingPoint } = useAppSelector(
    (state) => state.booking
  );

  const [activeStep, setActiveStep] = useState<"SEATS" | "BOARD_DROP" | "INFO">("SEATS");
  const [activeTab, setActiveTab] = useState<
    | "highlights"
    | "photos"
    | "ml_insights"
    | "cancellation"
    | "boarding"
    | "dropping"
    | "route"
    | "rest_stop"
    | "features"
    | "reviews"
    | "safety"
  >(
    (initialTab as any) || "highlights"
  );

  const [showWriteReview, setShowWriteReview] = useState(false);
  const [showAllBoarding, setShowAllBoarding] = useState(false);
  const [showAllDropping, setShowAllDropping] = useState(false);

  const { data: layoutData, isLoading: isSeatsLoading } = useGetRouteSeatsQuery(route.id, {
    refetchOnMountOrArgChange: true,
  });
  const { data: reviewData, refetch: refetchReviews } = useGetBusReviewsQuery(route.busId || route.id);
  const { data: delayPrediction } = useGetBusDelayPredictionQuery(
    {
      source: route.sourceCity,
      destination: route.destinationCity,
      departureTime: route.departureTime,
    },
    { skip: !isOpen }
  );
  const { data: dynamicPriceQuote } = useGetDynamicPriceQuoteQuery(
    {
      source: route.sourceCity,
      destination: route.destinationCity,
      basePrice: route.basePrice,
    },
    { skip: !isOpen }
  );

  // Realistic 1-second seat loading timer so seat layout loading looks authentic
  const [isMinSeatLoading, setIsMinSeatLoading] = useState(true);

  // Prevent background scrolling when modal is open
  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = originalOverflow || "auto";
      };
    }
  }, [isOpen]);

  useEffect(() => {
    if (isOpen) {
      setIsMinSeatLoading(true);
      const timer = setTimeout(() => {
        setIsMinSeatLoading(false);
      }, 1000); // exactly 1 second delay
      return () => clearTimeout(timer);
    }
  }, [isOpen, route.id]);

  const showSeatsLoader = isSeatsLoading || isMinSeatLoading;

  // Selected Boarding and Dropping points
  const currentBoarding = boardingPoint || route.boardingPoints?.[0] || "Majestic";
  const currentDropping = droppingPoint || route.droppingPoints?.[0] || "Sriperumbudur";

  const seats = layoutData?.seats || [];
  const lowerSeats = seats.filter((s) => s.deck === "LOWER");
  const upperSeats = seats.filter((s) => s.deck === "UPPER");
  const isSleeper =
    route.busType.toLowerCase().includes("sleeper") || upperSeats.length > 0;

  const totalPrice = selectedSeats.reduce((acc, s) => acc + (s.price || route.basePrice), 0);

  const handleSeatClick = (seat: SeatItem) => {
    if (seat.status !== "AVAILABLE") return;
    dispatch(setSelectedRoute(route));
    dispatch(toggleSeatSelection(seat));
  };

  // Group seats by rowNum
  const getDeckRows = (deckSeats: SeatItem[]) => {
    const rowsMap = new Map<number, SeatItem[]>();
    deckSeats.forEach((s, idx) => {
      const r = s.rowNum && s.rowNum > 0 ? s.rowNum : Math.floor(idx / 3) + 1;
      if (!rowsMap.has(r)) rowsMap.set(r, []);
      rowsMap.get(r)!.push(s);
    });
    return Array.from(rowsMap.keys()).sort((a, b) => a - b);
  };

  // Render authentic sleeper berth matching 2nd reference image (tall elongated berth with headrest pillow)
  const renderSleeperBerth = (seat: SeatItem) => {
    const isSelected = selectedSeats.some((s) => s.seatId === seat.seatId);
    const isSold = seat.status === "BOOKED" || seat.status === "LOCKED";
    const isFemale = seat.genderRestriction === "FEMALE" || seat.bookedGender === "FEMALE";

    let containerStyle =
      "relative w-11 sm:w-12 h-20 sm:h-22 rounded-xl flex flex-col items-center justify-between py-1.5 sm:py-2 px-1 transition-all select-none ";
    let pillowStyle = "w-6 sm:w-7 h-1.5 rounded-full transition-all ";
    let priceOrLabel = `₹${Math.round(seat.price || route.basePrice)}`;
    let priceColor = "text-gray-800 dark:text-slate-200 font-extrabold";

    if (isSelected) {
      containerStyle +=
        "bg-[#15803d] border-2 border-[#15803d] text-white shadow-md scale-105 ring-2 ring-emerald-400/40 cursor-pointer";
      pillowStyle += "bg-emerald-300";
      priceColor = "text-white font-black";
    } else if (isSold) {
      if (isFemale) {
        containerStyle +=
          "border border-pink-200 dark:border-pink-900/40 bg-pink-50/75 dark:bg-pink-950/25 cursor-not-allowed";
        pillowStyle += "bg-pink-200 dark:bg-pink-800/60";
      } else {
        containerStyle +=
          "border border-gray-200 dark:border-slate-700 bg-gray-50/90 dark:bg-slate-800/60 cursor-not-allowed";
        pillowStyle += "bg-gray-200 dark:bg-slate-600";
      }
      priceOrLabel = "Sold";
      priceColor = "text-gray-400 dark:text-slate-500 font-medium";
    } else {
      // Default Available Sleeper matching redBus authentic green outline
      containerStyle +=
        "bg-white dark:bg-slate-800 border-2 border-emerald-500 hover:border-emerald-600 hover:shadow-md hover:bg-emerald-50/40 dark:hover:bg-emerald-950/20 cursor-pointer active:scale-95";
      pillowStyle += "bg-emerald-100 dark:bg-emerald-800/50";
      priceColor = "text-gray-800 dark:text-slate-200 font-extrabold";
    }

    return (
      <button
        key={seat.seatId}
        type="button"
        disabled={isSold}
        onClick={() => handleSeatClick(seat)}
        className={containerStyle}
        title={`Seat ${seat.seatNumber} (${seat.deck} Deck) - ${priceOrLabel}`}
      >
        {/* Pillow Headrest Indicator at Top */}
        <div className={pillowStyle} />

        {/* Center Indicator (Silhouette for sold, Checkmark for selected) */}
        <div className="flex items-center justify-center my-auto">
          {isSold ? (
            isFemale ? (
              <UserIcon className="w-3.5 h-3.5 text-pink-400 dark:text-pink-300" />
            ) : (
              <UserIcon className="w-3.5 h-3.5 text-gray-400 dark:text-slate-500" />
            )
          ) : isSelected ? (
            <CheckCircle2 className="w-4 h-4 text-white" />
          ) : null}
        </div>

        {/* Bottom Price and Seat Number */}
        <div className="flex flex-col items-center leading-none">
          <span className={`text-[10px] sm:text-[11px] tracking-tight ${priceColor}`}>
            {priceOrLabel}
          </span>
          <span
            className={`text-[8px] mt-0.5 ${
              isSelected ? "text-emerald-100 font-semibold" : "text-gray-400 dark:text-slate-400 font-medium"
            }`}
          >
            {seat.seatNumber}
          </span>
        </div>
      </button>
    );
  };

  // Render authentic seater chair for seater buses
  const renderSeaterChair = (seat: SeatItem) => {
    const isSelected = selectedSeats.some((s) => s.seatId === seat.seatId);
    const isSold = seat.status === "BOOKED" || seat.status === "LOCKED";
    const isFemale = seat.genderRestriction === "FEMALE" || seat.bookedGender === "FEMALE";

    const strokeColor = isSelected ? "#15803d" : isSold ? (isFemale ? "#f472b6" : "#cbd5e1") : "#10b981";
    const fillColor = isSelected ? "#dcfce7" : isSold ? (isFemale ? "#fdf2f8" : "#f8fafc") : "#ffffff";
    const priceColor = isSelected
      ? "text-emerald-700 font-extrabold"
      : isSold
      ? "text-gray-400 font-medium"
      : "text-gray-800 dark:text-slate-200 font-bold";

    return (
      <div key={seat.seatId} className="flex flex-col items-center">
        <button
          type="button"
          disabled={isSold}
          onClick={() => handleSeatClick(seat)}
          className="relative w-10 sm:w-11 h-12 flex flex-col items-center justify-center transition-all cursor-pointer disabled:cursor-not-allowed hover:scale-105 active:scale-95"
          title={`Seat ${seat.seatNumber} - ₹${seat.price || route.basePrice}`}
        >
          <svg className="w-9 h-11" viewBox="0 0 44 48" fill="none">
            <rect x="6" y="8" width="32" height="34" rx="8" fill={fillColor} stroke={strokeColor} strokeWidth="2.5" />
            <path d="M11 26h22" stroke={strokeColor} strokeWidth="2" strokeLinecap="round" />
            <rect
              x="13"
              y="11"
              width="18"
              height="8"
              rx="4"
              fill={isSelected ? "#86efac" : isSold ? (isFemale ? "#fbcfe8" : "#cbd5e1") : "#bbf7d0"}
            />
          </svg>
        </button>
        <span className={`text-[10px] font-bold ${priceColor}`}>
          {isSold ? "Sold" : `₹${Math.round(seat.price || route.basePrice)}`}
        </span>
        <span className="text-[8px] text-gray-400 font-medium">{seat.seatNumber}</span>
      </div>
    );
  };

  // Render Deck Chassis matching RedBus desktop interface
  const renderDeckChassis = (title: string, deckSeats: SeatItem[], isLowerDeck: boolean) => {
    const rows = getDeckRows(deckSeats);

    return (
      <div className="flex-1 max-w-[260px] min-w-[190px]">
        {/* Deck Header */}
        <div className="flex items-center justify-between px-3 mb-2.5">
          <span className="text-xs font-black text-gray-800 dark:text-white uppercase tracking-wider">
            {title}
          </span>
          {isLowerDeck ? (
            <div
              className="w-7 h-7 rounded-full border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 flex items-center justify-center text-gray-500 shadow-2xs"
              title="Driver Steering Wheel"
            >
              <svg className="w-4 h-4 text-gray-600 dark:text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="9" />
                <circle cx="12" cy="12" r="3" />
                <line x1="12" y1="3" x2="12" y2="9" />
                <line x1="3" y1="12" x2="9" y2="12" />
                <line x1="15" y1="12" x2="21" y2="12" />
                <line x1="12" y1="15" x2="12" y2="21" />
              </svg>
            </div>
          ) : (
            <div
              className="w-7 h-7 rounded-full border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 flex items-center justify-center text-xs text-gray-400 font-bold"
              title="Upper Deck"
            >
              ⬆
            </div>
          )}
        </div>

        {/* Chassis Body */}
        <div className="rounded-t-[32px] sm:rounded-t-[36px] rounded-b-2xl border-2 border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800/90 p-3.5 sm:p-4 shadow-xs flex flex-col justify-between min-h-[460px]">
          <div className="space-y-3 sm:space-y-3.5">
            {rows.map((rowNum) => {
              const rowSeats = deckSeats.filter((s, idx) => {
                const r = s.rowNum && s.rowNum > 0 ? s.rowNum : Math.floor(idx / 3) + 1;
                return r === rowNum;
              });

              let leftSeat = rowSeats.find((s) => s.colNum === 1);
              let rightSeat1 = rowSeats.find((s) => s.colNum === 2);
              let rightSeat2 = rowSeats.find((s) => s.colNum === 3);

              if (!leftSeat && rowSeats.length > 0) leftSeat = rowSeats[0];
              if (!rightSeat1 && rowSeats.length > 1) rightSeat1 = rowSeats[1];
              if (!rightSeat2 && rowSeats.length > 2) rightSeat2 = rowSeats[2];

              return (
                <div key={rowNum} className="flex items-center justify-between">
                  {/* Left Column (Single Berth or Chair) */}
                  <div className="w-11 sm:w-12 flex justify-center">
                    {leftSeat ? (
                      leftSeat.seatType === "SEATER"
                        ? renderSeaterChair(leftSeat)
                        : renderSleeperBerth(leftSeat)
                    ) : (
                      <div className="w-11 sm:w-12 h-20 sm:h-22" />
                    )}
                  </div>

                  {/* Aisle Walking Space */}
                  <div className="w-5 sm:w-6" />

                  {/* Right Column (2 Berths / Chairs Side by Side) */}
                  <div className="flex items-center space-x-1.5 sm:space-x-2">
                    {rightSeat1 ? (
                      rightSeat1.seatType === "SEATER"
                        ? renderSeaterChair(rightSeat1)
                        : renderSleeperBerth(rightSeat1)
                    ) : (
                      <div className="w-11 sm:w-12 h-20 sm:h-22" />
                    )}
                    {rightSeat2 ? (
                      rightSeat2.seatType === "SEATER"
                        ? renderSeaterChair(rightSeat2)
                        : renderSleeperBerth(rightSeat2)
                    ) : (
                      <div className="w-11 sm:w-12 h-20 sm:h-22" />
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Emergency Exit Indicator at the Bottom */}
          <div className="pt-4 pb-1 text-center border-t border-dashed border-gray-200 dark:border-slate-700/60 mt-4">
            <span className="inline-flex items-center text-[9px] font-bold text-gray-400 dark:text-slate-500 uppercase tracking-widest">
              ▼ Emergency Exit
            </span>
          </div>
        </div>
      </div>
    );
  };

  const scrollTabs = (direction: "left" | "right") => {
    if (tabsRef.current) {
      const scrollAmount = direction === "left" ? -220 : 220;
      tabsRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
    }
  };

  const handleProceedToBook = () => {
    if (selectedSeats.length === 0) return;
    dispatch(
      prepareCheckout({
        route,
        seats: selectedSeats,
        boardingPoint: currentBoarding,
        droppingPoint: currentDropping,
      })
    );
    onClose();
    router.push(`/checkout?routeId=${route.id}`);
  };

  // Structured Boarding Points with timing and addresses (matching screenshot photo 4)
  const boardingStops = [
    {
      time: route.departureTime?.substring(0, 5) || "16:30",
      name: route.boardingPoints?.[0] || "Majestic",
      address: "Front of Bhagya Vinayaga Temple, Opp Amar Hotel Majestic",
    },
    {
      time: "16:45",
      name: route.boardingPoints?.[1] || "Santhi Nagar",
      address: "SETC Bus Stop Front of Relence Metro Mart, Near SRS Luggage Office",
    },
    {
      time: "16:50",
      name: route.boardingPoints?.[2] || "Nimhans Hospital",
      address: "Nimhans Hospital Opp, Bus Stop",
    },
    {
      time: "16:55",
      name: route.boardingPoints?.[3] || "Christ University",
      address: "Hosur Main Road Bus Stop",
    },
    {
      time: "17:15",
      name: "Silk Board",
      address: "Near Silk Board Junction, Hosur Main Road",
    },
    {
      time: "17:30",
      name: "Electronic City",
      address: "Toll Gate Entrance, Opp Infosys Gate",
    },
  ];

  // Structured Dropping Points with timing and addresses (matching screenshot photo 5)
  const droppingStops = [
    {
      time: "23:40",
      name: route.droppingPoints?.[0] || "Sriperumbudur",
      address: "Opp, Sriperambudur Arch",
    },
    {
      time: "23:45",
      name: route.droppingPoints?.[1] || "Sriperambadur Toll Gate",
      address: "Sriperambudur Toll Plaza",
    },
    {
      time: "00:10",
      name: route.droppingPoints?.[2] || "Poonamallee KFC",
      address: "Poonamallee High Road, Opp KFC",
    },
    {
      time: "00:15",
      name: route.droppingPoints?.[3] || "Poonamallee Bypass",
      address: "National Highway Bypass",
    },
    {
      time: "00:30",
      name: "Koyambedu",
      address: "Omni Bus Stand, Platform 4",
    },
  ];

  return (
    <>
      <AnimatePresence>
        {isOpen && (
          <div className="fixed inset-0 z-50 overflow-y-auto flex items-center justify-center p-2 sm:p-4">
          {/* Backdrop with fade */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/65 backdrop-blur-xs cursor-pointer"
          />

          {/* Modal Container with Spring Scale and Slide */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 15 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 15 }}
            transition={{ type: "spring", damping: 26, stiffness: 320 }}
            className="bg-white dark:bg-[#0b0f19] rounded-3xl w-full max-w-6xl xl:max-w-7xl shadow-2xl border border-gray-200 dark:border-slate-800 flex flex-col max-h-[92vh] overflow-hidden relative z-10"
          >
            {/* Top Header matching Photos 1-5 */}
            <div className="px-6 py-4 bg-white dark:bg-[#0f172a] border-b border-gray-200 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <button
              onClick={onClose}
              className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
            <div>
              <h2 className="text-base sm:text-lg font-black text-gray-900 dark:text-white flex items-center space-x-2">
                <span>{route.sourceCity}</span>
                <span className="text-[#d84e55]">➔</span>
                <span>{route.destinationCity}</span>
              </h2>
              <p className="text-[11px] text-gray-500 dark:text-slate-400">
                {route.operatorName} • {route.busType}
              </p>
            </div>
          </div>

          {/* Center Steps navigation matching Screenshots */}
          <div className="hidden md:flex items-center space-x-8 text-xs font-bold">
            <button
              onClick={() => setActiveStep("SEATS")}
              className={`pb-1 border-b-2 transition-all cursor-pointer ${
                activeStep === "SEATS"
                  ? "border-[#d84e55] text-[#d84e55]"
                  : "border-transparent text-gray-500 hover:text-gray-800 dark:text-slate-400"
              }`}
            >
              Select seats
            </button>
            <button
              onClick={() => {
                setActiveStep("BOARD_DROP");
                setActiveTab("boarding");
              }}
              className={`pb-1 border-b-2 transition-all cursor-pointer ${
                activeStep === "BOARD_DROP"
                  ? "border-[#d84e55] text-[#d84e55]"
                  : "border-transparent text-gray-500 hover:text-gray-800 dark:text-slate-400"
              }`}
            >
              Board/Drop point
            </button>
            <button
              onClick={handleProceedToBook}
              disabled={selectedSeats.length === 0}
              className={`pb-1 border-b-2 transition-all cursor-pointer ${
                activeStep === "INFO"
                  ? "border-[#d84e55] text-[#d84e55]"
                  : "border-transparent text-gray-400 dark:text-slate-500"
              }`}
            >
              Passenger Info
            </button>
          </div>

          {/* Quick Rating Badge */}
          <div className="flex items-center space-x-2">
            <div className="flex items-center space-x-1 px-2.5 py-1 bg-[#15803d] text-white rounded-lg text-xs font-bold shadow-2xs">
              <Star className="w-3.5 h-3.5 fill-white" />
              <span>{reviewData?.averageRating?.toFixed(1) || route.rating.toFixed(1)}</span>
            </div>
            <span className="text-xs text-gray-500 dark:text-slate-400 font-medium hidden sm:inline">
              {reviewData?.totalRatings || 427} ratings
            </span>
          </div>
        </div>

        {/* Main Body: Split View (Left: Authentic Redbus Seat Layout | Right: Tabbed Detail Panels) */}
        <div className="flex-1 overflow-y-auto grid grid-cols-1 lg:grid-cols-12 divide-y lg:divide-y-0 lg:divide-x divide-gray-200 dark:divide-slate-800">
          {/* Left Column: Redbus Dual Deck Seat Map (Spacious 6 cols) */}
          <div className="lg:col-span-6 p-4 sm:p-5 bg-gray-50/70 dark:bg-slate-900/60 overflow-y-auto flex flex-col items-center">
            <div className="w-full max-w-xl space-y-4">
              {/* Decks Header */}
              <div className="flex items-center justify-between px-2">
                <span className="text-xs font-black text-gray-800 dark:text-slate-200 uppercase tracking-wider">
                  Select Seats
                </span>
                <span className="text-[11px] text-[#d84e55] font-bold">
                  {route.availableSeats} Seats Available
                </span>
              </div>

              {showSeatsLoader ? (
                <div className="w-full space-y-3.5 animate-pulse">
                  {/* Status Banner */}
                  <div className="p-3 bg-white dark:bg-slate-800 rounded-2xl border border-gray-200 dark:border-slate-700 shadow-2xs flex items-center justify-between">
                    <div className="flex items-center space-x-2.5">
                      <div className="w-4 h-4 border-2 border-[#d84e55] border-t-transparent rounded-full animate-spin shrink-0" />
                      <span className="text-xs font-bold text-gray-800 dark:text-slate-200">
                        Connecting to {route.operatorName} live seat inventory...
                      </span>
                    </div>
                    <span className="text-[10px] text-[#d84e55] font-black uppercase tracking-wider bg-red-50 dark:bg-red-950/40 px-2 py-0.5 rounded">
                      Syncing
                    </span>
                  </div>

                  {/* Twin Chassis Skeletons */}
                  <div className="flex flex-row justify-center items-start gap-4 sm:gap-6">
                    {/* Lower Deck Skeleton */}
                    <div className="flex-1 max-w-[260px] min-w-[190px]">
                      <div className="flex items-center justify-between px-3 mb-2.5">
                        <div className="h-4 w-20 bg-gray-200 dark:bg-slate-700 rounded-sm" />
                        <div className="w-7 h-7 rounded-full bg-gray-200 dark:bg-slate-700" />
                      </div>
                      <div className="rounded-t-[32px] sm:rounded-t-[36px] rounded-b-2xl border-2 border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-3.5 sm:p-4 shadow-xs min-h-[460px] flex flex-col justify-between">
                        <div className="space-y-3 sm:space-y-3.5">
                          {[1, 2, 3, 4, 5].map((row) => (
                            <div key={row} className="flex items-center justify-between">
                              <div className="w-11 sm:w-12 h-20 sm:h-22 rounded-xl bg-gray-100 dark:bg-slate-700/60 border border-gray-200 dark:border-slate-600 flex flex-col items-center justify-between py-2 px-1">
                                <div className="w-6 h-1.5 rounded-full bg-gray-200 dark:bg-slate-600" />
                                <div className="w-4 h-4 rounded-full bg-gray-200 dark:bg-slate-600" />
                                <div className="w-7 h-2 rounded bg-gray-200 dark:bg-slate-600" />
                              </div>
                              <div className="w-5 sm:w-6" />
                              <div className="flex items-center space-x-1.5 sm:space-x-2">
                                <div className="w-11 sm:w-12 h-20 sm:h-22 rounded-xl bg-gray-100 dark:bg-slate-700/60 border border-gray-200 dark:border-slate-600 flex flex-col items-center justify-between py-2 px-1">
                                  <div className="w-6 h-1.5 rounded-full bg-gray-200 dark:bg-slate-600" />
                                  <div className="w-4 h-4 rounded-full bg-gray-200 dark:bg-slate-600" />
                                  <div className="w-7 h-2 rounded bg-gray-200 dark:bg-slate-600" />
                                </div>
                                <div className="w-11 sm:w-12 h-20 sm:h-22 rounded-xl bg-gray-100 dark:bg-slate-700/60 border border-gray-200 dark:border-slate-600 flex flex-col items-center justify-between py-2 px-1">
                                  <div className="w-6 h-1.5 rounded-full bg-gray-200 dark:bg-slate-600" />
                                  <div className="w-4 h-4 rounded-full bg-gray-200 dark:bg-slate-600" />
                                  <div className="w-7 h-2 rounded bg-gray-200 dark:bg-slate-600" />
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                        <div className="pt-4 pb-1 text-center border-t border-dashed border-gray-200 dark:border-slate-700/60 mt-4">
                          <div className="h-2.5 w-24 bg-gray-200 dark:bg-slate-700 rounded mx-auto" />
                        </div>
                      </div>
                    </div>

                    {/* Upper Deck Skeleton */}
                    {isSleeper && (
                      <div className="flex-1 max-w-[260px] min-w-[190px]">
                        <div className="flex items-center justify-between px-3 mb-2.5">
                          <div className="h-4 w-20 bg-gray-200 dark:bg-slate-700 rounded-sm" />
                          <div className="w-7 h-7 rounded-full bg-gray-200 dark:bg-slate-700" />
                        </div>
                        <div className="rounded-t-[32px] sm:rounded-t-[36px] rounded-b-2xl border-2 border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-3.5 sm:p-4 shadow-xs min-h-[460px] flex flex-col justify-between">
                          <div className="space-y-3 sm:space-y-3.5">
                            {[1, 2, 3, 4, 5].map((row) => (
                              <div key={row} className="flex items-center justify-between">
                                <div className="w-11 sm:w-12 h-20 sm:h-22 rounded-xl bg-gray-100 dark:bg-slate-700/60 border border-gray-200 dark:border-slate-600 flex flex-col items-center justify-between py-2 px-1">
                                  <div className="w-6 h-1.5 rounded-full bg-gray-200 dark:bg-slate-600" />
                                  <div className="w-4 h-4 rounded-full bg-gray-200 dark:bg-slate-600" />
                                  <div className="w-7 h-2 rounded bg-gray-200 dark:bg-slate-600" />
                                </div>
                                <div className="w-5 sm:w-6" />
                                <div className="flex items-center space-x-1.5 sm:space-x-2">
                                  <div className="w-11 sm:w-12 h-20 sm:h-22 rounded-xl bg-gray-100 dark:bg-slate-700/60 border border-gray-200 dark:border-slate-600 flex flex-col items-center justify-between py-2 px-1">
                                    <div className="w-6 h-1.5 rounded-full bg-gray-200 dark:bg-slate-600" />
                                    <div className="w-4 h-4 rounded-full bg-gray-200 dark:bg-slate-600" />
                                    <div className="w-7 h-2 rounded bg-gray-200 dark:bg-slate-600" />
                                  </div>
                                  <div className="w-11 sm:w-12 h-20 sm:h-22 rounded-xl bg-gray-100 dark:bg-slate-700/60 border border-gray-200 dark:border-slate-600 flex flex-col items-center justify-between py-2 px-1">
                                    <div className="w-6 h-1.5 rounded-full bg-gray-200 dark:bg-slate-600" />
                                    <div className="w-4 h-4 rounded-full bg-gray-200 dark:bg-slate-600" />
                                    <div className="w-7 h-2 rounded bg-gray-200 dark:bg-slate-600" />
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                          <div className="pt-4 pb-1 text-center border-t border-dashed border-gray-200 dark:border-slate-700/60 mt-4">
                            <div className="h-2.5 w-24 bg-gray-200 dark:bg-slate-700 rounded mx-auto" />
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="flex flex-row justify-center items-start gap-4 sm:gap-6">
                  {/* Lower Deck */}
                  {renderDeckChassis("Lower Deck", lowerSeats, true)}

                  {/* Upper Deck (if sleeper) */}
                  {upperSeats.length > 0 && renderDeckChassis("Upper Deck", upperSeats, false)}
                </div>
              )}

              {/* Seat Legend */}
              <div className="flex flex-wrap items-center justify-center gap-4 pt-3 text-[10px] text-gray-500 dark:text-slate-400 font-semibold border-t border-gray-200 dark:border-slate-700">
                <div className="flex items-center space-x-1.5">
                  <div className="w-4 h-4 rounded-sm border-2 border-emerald-500 bg-white" />
                  <span>Available</span>
                </div>
                <div className="flex items-center space-x-1.5">
                  <div className="w-4 h-4 rounded-sm bg-[#15803d]" />
                  <span>Selected</span>
                </div>
                <div className="flex items-center space-x-1.5">
                  <div className="w-4 h-4 rounded-sm bg-gray-200 dark:bg-slate-700 border border-gray-300 dark:border-slate-600" />
                  <span>Sold</span>
                </div>
                <div className="flex items-center space-x-1.5">
                  <div className="w-4 h-4 rounded-sm border border-pink-200 bg-pink-100 dark:bg-pink-950/40 flex items-center justify-center">
                    <div className="w-2 h-2 rounded-full bg-pink-500" />
                  </div>
                  <span>Female</span>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column: Tabbed Detail Panels & Proceed to Book Action Bar */}
          <div className="lg:col-span-6 flex flex-col justify-between bg-white dark:bg-[#0b0f19] overflow-hidden">
            {/* Scrollable Tabs Bar with Prominent Slide Arrows matching Image 1 */}
            <div className="relative flex items-center px-1.5 py-2 border-b border-gray-200 dark:border-slate-800 bg-white dark:bg-[#0f172a]">
              {/* Left Scroll Arrow Button */}
              <button
                type="button"
                onClick={() => scrollTabs("left")}
                className="w-7 h-7 rounded-full bg-white dark:bg-slate-800 border border-gray-300 dark:border-slate-600 shadow-md hover:shadow-lg hover:border-[#d84e55] hover:text-[#d84e55] dark:hover:text-red-400 text-gray-700 dark:text-slate-200 transition-all flex items-center justify-center shrink-0 z-10 mr-1.5 cursor-pointer active:scale-90"
                title="Slide tabs left"
                aria-label="Slide tabs left"
              >
                <ChevronLeft className="w-4 h-4 stroke-[2.5]" />
              </button>

              <div
                ref={tabsRef}
                className="flex-1 flex items-center space-x-1.5 overflow-x-auto whitespace-nowrap scrollbar-none scroll-smooth text-xs font-bold px-1"
              >
                {[
                  { id: "highlights", label: "Highlights" },
                  { id: "photos", label: "📸 Bus Photos" },
                  { id: "ml_insights", label: "⚡ AI Punctuality & Demand" },
                  { id: "cancellation", label: "Cancellation policy" },
                  { id: "boarding", label: "Boarding point" },
                  { id: "dropping", label: "Dropping point" },
                  { id: "route", label: "Bus route" },
                  { id: "rest_stop", label: "Rest stop" },
                  { id: "features", label: "Bus Features" },
                  { id: "reviews", label: "Rating and reviews" },
                  { id: "safety", label: "Bus Safety" },
                ].map((tab) => (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id as any)}
                    className={`px-3 py-1.5 rounded-lg transition-all shrink-0 cursor-pointer ${
                      activeTab === tab.id
                        ? "text-[#d84e55] border-b-2 border-[#d84e55] font-extrabold bg-red-50/50 dark:bg-red-950/40"
                        : "text-gray-500 dark:text-slate-400 hover:text-gray-800 dark:hover:text-slate-200"
                    }`}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>

              {/* Right Scroll Arrow Button matching Image 1 */}
              <button
                type="button"
                onClick={() => scrollTabs("right")}
                className="w-7 h-7 rounded-full bg-white dark:bg-slate-800 border border-gray-300 dark:border-slate-600 shadow-md hover:shadow-lg hover:border-[#d84e55] hover:text-[#d84e55] dark:hover:text-red-400 text-gray-700 dark:text-slate-200 transition-all flex items-center justify-center shrink-0 z-10 ml-1.5 cursor-pointer active:scale-90"
                title="Slide tabs right"
                aria-label="Slide tabs right"
              >
                <ChevronRight className="w-4 h-4 stroke-[2.5]" />
              </button>
            </div>

            {/* Tab Contents */}
            <div className="flex-1 p-5 overflow-y-auto space-y-6">
              <AnimatePresence mode="wait">
                <motion.div
                  key={activeTab}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  transition={{ duration: 0.18 }}
                  className="space-y-6"
                >
              {/* Bus Photos & Studio Carousel Tab */}
              {activeTab === "photos" && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-gray-900 dark:text-white flex items-center gap-1.5">
                        <Camera className="w-4 h-4 text-[#d84e55]" />
                        <span>Coach Imagery & Seat Layout</span>
                      </h3>
                      <p className="text-xs text-gray-500 dark:text-slate-400 mt-0.5">
                        Real-time photos uploaded by {route.operatorName} and verified by redBus Fleet Studio.
                      </p>
                    </div>
                  </div>

                  {/* Interactive Slider */}
                  <BusImageSlider
                    photoUrls={route.busPhotoUrl}
                    busName={route.operatorName}
                    busType={route.busType}
                    aspectRatio="video"
                    showThumbnails={true}
                  />

                  <div className="p-4 bg-gray-50 dark:bg-slate-800 rounded-2xl border border-gray-200 dark:border-slate-700 text-xs space-y-1.5">
                    <p className="font-bold text-gray-900 dark:text-white flex items-center gap-1">
                      <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                      <span>redBus Fleet Quality Guarantee</span>
                    </p>
                    <p className="text-gray-500 dark:text-slate-400 text-[11px] leading-relaxed">
                      All coaches are sanitized before every departure. High-speed USB charging, clean bedsheets, individual reading lights, and air suspension are verified for this fleet.
                    </p>
                  </div>
                </div>
              )}

              {/* AI Punctuality & Dynamic Surge Demand Tab */}
              {activeTab === "ml_insights" && (
                <div className="space-y-4">
                  <div>
                    <h3 className="text-sm font-bold text-gray-900 dark:text-white flex items-center gap-1.5">
                      <Sparkles className="w-4 h-4 text-amber-500" />
                      <span>Machine Learning Fleet Insights</span>
                    </h3>
                    <p className="text-xs text-gray-500 dark:text-slate-400 mt-0.5">
                      Real-time AI telemetry, ETA punctuality buffer, and dynamic market demand analytics.
                    </p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {/* On-Time Punctuality Card */}
                    <div className="p-4 bg-emerald-50/70 dark:bg-emerald-950/40 rounded-2xl border border-emerald-200 dark:border-emerald-800/80 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-emerald-800 dark:text-emerald-300">Punctuality Score</span>
                        <span className="px-2 py-0.5 bg-emerald-200 dark:bg-emerald-900 text-emerald-900 dark:text-emerald-200 rounded text-[10px] font-black">
                          {delayPrediction ? `${Math.round(delayPrediction.onTimeProbability * 100)}% ON-TIME` : "98% ON-TIME"}
                        </span>
                      </div>
                      <p className="text-xl font-black text-emerald-900 dark:text-emerald-100">
                        {delayPrediction?.punctualityGrade || "EXCELLENT"}
                      </p>
                      <p className="text-[11px] text-emerald-700 dark:text-emerald-300 leading-relaxed">
                        {delayPrediction?.aiExplanation || "Analyzed highway toll velocity and driver track record. Smooth transit expected."}
                      </p>
                    </div>

                    {/* Dynamic Pricing Surge Card */}
                    <div className="p-4 bg-amber-50/70 dark:bg-amber-950/40 rounded-2xl border border-amber-200 dark:border-amber-800/80 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-amber-800 dark:text-amber-300">Demand Level</span>
                        <span className="px-2 py-0.5 bg-amber-200 dark:bg-amber-900 text-amber-900 dark:text-amber-200 rounded text-[10px] font-black">
                          {dynamicPriceQuote?.demandLevel || "NORMAL"}
                        </span>
                      </div>
                      <p className="text-xl font-black text-amber-900 dark:text-amber-100">
                        ₹{dynamicPriceQuote?.currentDynamicPrice ? Number(dynamicPriceQuote.currentDynamicPrice).toFixed(0) : route.basePrice}
                        <span className="text-xs font-normal text-amber-700 dark:text-amber-400 ml-1.5">
                          ({dynamicPriceQuote?.surgeMultiplier ? `${dynamicPriceQuote.surgeMultiplier}x Surge` : "Standard Fare"})
                        </span>
                      </p>
                      <p className="text-[11px] text-amber-700 dark:text-amber-300 leading-relaxed">
                        {dynamicPriceQuote?.reason || "Prices may rise closer to departure as more seats fill up."}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* 1. Cancellation Policy Tab (Exact Match with Photo 2) */}
              {activeTab === "cancellation" && (
                <div className="space-y-4">
                  <div>
                    <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                      Cancellation policy
                    </h3>
                    <p className="text-xs text-gray-500 dark:text-slate-400">
                      Standard operator refund rules and redBus Free Cancellation coverage.
                    </p>
                  </div>

                  {/* Comparison Table */}
                  <div className="border border-gray-200 dark:border-slate-800 rounded-2xl overflow-hidden text-xs">
                    <table className="w-full text-left">
                      <thead className="bg-gray-50 dark:bg-slate-800/80 text-gray-700 dark:text-slate-300 font-bold border-b border-gray-200 dark:border-slate-800">
                        <tr>
                          <th className="p-3">Time before travel</th>
                          <th className="p-3 text-center">Without free cancellation</th>
                          <th className="p-3 text-center bg-emerald-50/70 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300">
                            With free cancellation
                          </th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100 dark:divide-slate-800 text-gray-600 dark:text-slate-300">
                        <tr>
                          <td className="p-3 font-medium">Before 19th Sep 05:30 PM</td>
                          <td className="p-3 text-center">85% Refund</td>
                          <td className="p-3 text-center font-bold text-emerald-600 bg-emerald-50/30 dark:bg-emerald-950/20">
                            ✓ 100% Refund
                          </td>
                        </tr>
                        <tr>
                          <td className="p-3 font-medium">From 19th Sep 05:30 PM Until 20th Sep 05:30 AM</td>
                          <td className="p-3 text-center">70% Refund</td>
                          <td className="p-3 text-center font-bold text-emerald-600 bg-emerald-50/30 dark:bg-emerald-950/20">
                            ✓ 100% Refund
                          </td>
                        </tr>
                        <tr>
                          <td className="p-3 font-medium">From 20th Sep 05:30 AM Until 20th Sep 09:30 AM</td>
                          <td className="p-3 text-center">50% Refund</td>
                          <td className="p-3 text-center font-bold text-emerald-600 bg-emerald-50/30 dark:bg-emerald-950/20">
                            ✓ 100% Refund
                          </td>
                        </tr>
                        <tr>
                          <td className="p-3 font-medium">From 20th Sep 09:30 AM Until 20th Sep 01:30 PM</td>
                          <td className="p-3 text-center">25% Refund</td>
                          <td className="p-3 text-center font-bold text-emerald-600 bg-emerald-50/30 dark:bg-emerald-950/20">
                            ✓ 100% Refund
                          </td>
                        </tr>
                        <tr>
                          <td className="p-3 font-medium">From 20th Sep 01:30 PM Until 20th Sep 05:30 PM</td>
                          <td className="p-3 text-center text-gray-400">0% refund</td>
                          <td className="p-3 text-center font-medium text-gray-400 bg-emerald-50/30 dark:bg-emerald-950/20">
                            0% refund
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  {/* Free Cancellation Banner */}
                  <div className="p-3.5 bg-red-50/70 dark:bg-red-950/40 rounded-2xl border border-red-200 dark:border-red-900/60 flex items-center space-x-3">
                    <div className="w-8 h-8 rounded-full bg-[#d84e55] text-white flex items-center justify-center font-bold text-sm shrink-0">
                      ₹
                    </div>
                    <p className="text-xs text-gray-800 dark:text-slate-200">
                      <span className="font-bold text-[#d84e55] dark:text-red-400">
                        Add Free Cancellation
                      </span>{" "}
                      while booking to get a 100% refund on cancellation.
                    </p>
                  </div>

                  <p className="text-[10px] text-gray-400 dark:text-slate-500 leading-relaxed">
                    * Cancellation charges are computed on a per seat basis. Above cancellation fare is calculated based on seat fare of ₹{route.basePrice}.
                  </p>
                </div>
              )}

              {/* 2. Boarding Point Tab (Exact Match with Photo 4) */}
              {activeTab === "boarding" && (
                <div className="space-y-4">
                  <div>
                    <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                      Boarding point
                    </h3>
                    <p className="text-xs text-gray-500 dark:text-slate-400 font-semibold mt-0.5">
                      {route.sourceCity}
                    </p>
                  </div>

                  <div className="space-y-3">
                    {boardingStops
                      .slice(0, showAllBoarding ? boardingStops.length : 4)
                      .map((stop, sIdx) => {
                        const isSelected = currentBoarding === stop.name;
                        return (
                          <div
                            key={sIdx}
                            onClick={() => dispatch(setBoardingPoint(stop.name))}
                            className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-start space-x-3.5 ${
                              isSelected
                                ? "border-[#d84e55] bg-red-50/40 dark:bg-red-950/30"
                                : "border-gray-200 dark:border-slate-800 hover:bg-gray-50 dark:hover:bg-slate-800/40"
                            }`}
                          >
                            <div className="pt-0.5">
                              <div
                                className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                                  isSelected
                                    ? "border-[#d84e55] bg-[#d84e55]"
                                    : "border-gray-300 dark:border-slate-600"
                                }`}
                              >
                                {isSelected && (
                                  <div className="w-1.5 h-1.5 rounded-full bg-white" />
                                )}
                              </div>
                            </div>

                            <div className="flex-1 min-w-0">
                              <div className="flex items-center space-x-2">
                                <span className="font-extrabold text-xs text-gray-900 dark:text-white">
                                  {stop.time}
                                </span>
                                <span className="text-gray-300">•</span>
                                <span className="font-bold text-xs text-gray-900 dark:text-white">
                                  {stop.name}
                                </span>
                              </div>
                              <p className="text-[11px] text-gray-500 dark:text-slate-400 mt-0.5">
                                {stop.address}
                              </p>
                            </div>
                          </div>
                        );
                      })}
                  </div>

                  <button
                    type="button"
                    onClick={() => setShowAllBoarding(!showAllBoarding)}
                    className="w-full py-2.5 bg-red-50 dark:bg-red-950/40 text-[#d84e55] dark:text-red-400 hover:bg-red-100 rounded-xl font-bold text-xs transition-colors cursor-pointer"
                  >
                    {showAllBoarding ? "Show less boarding points" : "View all boarding points"}
                  </button>
                </div>
              )}

              {/* 3. Dropping Point Tab (Exact Match with Photo 5) */}
              {activeTab === "dropping" && (
                <div className="space-y-4">
                  <div>
                    <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                      Dropping point
                    </h3>
                    <p className="text-xs text-gray-500 dark:text-slate-400 font-semibold mt-0.5">
                      {route.destinationCity}
                    </p>
                  </div>

                  <div className="space-y-3">
                    {droppingStops
                      .slice(0, showAllDropping ? droppingStops.length : 4)
                      .map((stop, sIdx) => {
                        const isSelected = currentDropping === stop.name;
                        return (
                          <div
                            key={sIdx}
                            onClick={() => dispatch(setDroppingPoint(stop.name))}
                            className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-start space-x-3.5 ${
                              isSelected
                                ? "border-[#d84e55] bg-red-50/40 dark:bg-red-950/30"
                                : "border-gray-200 dark:border-slate-800 hover:bg-gray-50 dark:hover:bg-slate-800/40"
                            }`}
                          >
                            <div className="pt-0.5">
                              <div
                                className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                                  isSelected
                                    ? "border-[#d84e55] bg-[#d84e55]"
                                    : "border-gray-300 dark:border-slate-600"
                                }`}
                              >
                                {isSelected && (
                                  <div className="w-1.5 h-1.5 rounded-full bg-white" />
                                )}
                              </div>
                            </div>

                            <div className="flex-1 min-w-0">
                              <div className="flex items-center space-x-2">
                                <span className="font-extrabold text-xs text-gray-900 dark:text-white">
                                  {stop.time}
                                </span>
                                <span className="text-gray-300">•</span>
                                <span className="font-bold text-xs text-gray-900 dark:text-white">
                                  {stop.name}
                                </span>
                              </div>
                              <p className="text-[11px] text-gray-500 dark:text-slate-400 mt-0.5">
                                {stop.address}
                              </p>
                            </div>
                          </div>
                        );
                      })}
                  </div>

                  <button
                    type="button"
                    onClick={() => setShowAllDropping(!showAllDropping)}
                    className="w-full py-2.5 bg-red-50 dark:bg-red-950/40 text-[#d84e55] dark:text-red-400 hover:bg-red-100 rounded-xl font-bold text-xs transition-colors cursor-pointer"
                  >
                    {showAllDropping ? "Show less dropping points" : "View all dropping points"}
                  </button>
                </div>
              )}

              {/* 4. Bus Route & Rest Stop Tab (Exact Match with Photo 3) */}
              {(activeTab === "route" || activeTab === "rest_stop") && (
                <div className="space-y-6">
                  {/* Bus Route */}
                  <div className="p-4 bg-gray-50 dark:bg-slate-800/60 rounded-2xl border border-gray-200 dark:border-slate-800 space-y-3">
                    <div className="flex items-center justify-between">
                      <h4 className="font-bold text-sm text-gray-900 dark:text-white">
                        Bus route
                      </h4>
                      <span className="text-xs text-gray-500 dark:text-slate-400 font-semibold">
                        375 km • 7 hr 30 min
                      </span>
                    </div>

                    <p className="text-xs font-medium text-gray-700 dark:text-slate-300 leading-relaxed">
                      <span className="font-bold bg-amber-100 dark:bg-amber-950 px-1 py-0.5 rounded text-amber-900 dark:text-amber-200">
                        {route.sourceCity}
                      </span>{" "}
                      ➔ Hosur ➔ Shoolagiri ➔ Krishnagiri ➔ Vaniyambadi ➔ Ambur ➔ Vellore ➔ Arcot ➔ Kanchipuram ➔{" "}
                      <span className="font-bold bg-amber-100 dark:bg-amber-950 px-1 py-0.5 rounded text-amber-900 dark:text-amber-200">
                        {route.destinationCity}
                      </span>
                    </p>
                  </div>

                  {/* Rest stop matching Photo 3 */}
                  <div className="space-y-3">
                    <h4 className="font-bold text-sm text-gray-900 dark:text-white">
                      Rest stop
                    </h4>
                    <div className="p-4 bg-white dark:bg-slate-800 rounded-2xl border border-gray-200 dark:border-slate-700 shadow-xs space-y-3">
                      <div>
                        <h5 className="font-bold text-xs text-gray-900 dark:text-white">
                          Shri Balaji Bhavan, Nellai Karupatti Coffee jinjupalli Krishnagiri
                        </h5>
                        <p className="text-[11px] text-gray-500 dark:text-slate-400 mt-0.5">
                          07:45 PM • <span className="text-[#d84e55] font-bold">15 Mins stop</span>
                        </p>
                      </div>

                      <div>
                        <span className="text-[11px] text-gray-400 block mb-1.5 font-medium">
                          Traveler experience:
                        </span>
                        <div className="flex flex-wrap gap-2">
                          <span className="px-2.5 py-1 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800 text-[11px] font-bold rounded-lg flex items-center gap-1">
                            👍 Washroom Hygiene
                          </span>
                          <span className="px-2.5 py-1 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800 text-[11px] font-bold rounded-lg flex items-center gap-1">
                            👍 Food Quality
                          </span>
                          <span className="px-2.5 py-1 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800 text-[11px] font-bold rounded-lg flex items-center gap-1">
                            👍 Safety
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* 5. Bus Features Tab (Exact Match with Photo 1) */}
              {(activeTab === "features" || activeTab === "highlights") && (
                <div className="space-y-4">
                  <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                    Bus Features
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {[
                      "Water Bottle",
                      "Blankets",
                      "Charging Point",
                      "Reading Light",
                      "Pillow",
                      "CCTV",
                      "Bed Sheet",
                      "Emergency Exit",
                      "Live GPS Tracking",
                      "Fire Extinguisher",
                    ].map((feature, fIdx) => (
                      <span
                        key={fIdx}
                        className="px-3.5 py-2 rounded-xl bg-gray-50 dark:bg-slate-800 border border-gray-200 dark:border-slate-700 text-xs font-semibold text-gray-700 dark:text-slate-200 flex items-center gap-2"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                        <span>{feature}</span>
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* 6. Ratings & Reviews Tab (Exact Match with Photo 1) */}
              {activeTab === "reviews" && (
                <div className="space-y-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                        Ratings & reviews
                      </h3>
                      <div className="flex items-center space-x-1.5 text-xs text-emerald-600 font-bold mt-0.5">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Real Feedback from verified travellers</span>
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={() => setShowWriteReview(true)}
                      className="px-4 py-2 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer"
                    >
                      Write a Review
                    </button>
                  </div>

                  {/* Rating score and star breakdown bars */}
                  <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-center p-4 bg-gray-50 dark:bg-slate-800/60 rounded-2xl border border-gray-200 dark:border-slate-800">
                    <div className="md:col-span-4 text-center md:text-left">
                      <div className="flex items-center justify-center md:justify-start space-x-1.5">
                        <Star className="w-6 h-6 fill-[#15803d] text-[#15803d]" />
                        <span className="text-3xl font-extrabold text-gray-900 dark:text-white">
                          {reviewData?.averageRating?.toFixed(1) || route.rating.toFixed(1)}
                        </span>
                      </div>
                      <p className="text-xs text-gray-500 dark:text-slate-400 mt-1 font-medium">
                        {reviewData?.totalRatings || 427} Ratings
                      </p>
                    </div>

                    {/* Progress bars matching Photo 1 */}
                    <div className="md:col-span-8 space-y-1.5">
                      {[
                        { star: 5, pct: reviewData?.starPercentages?.[5] || 71 },
                        { star: 4, pct: reviewData?.starPercentages?.[4] || 14 },
                        { star: 3, pct: reviewData?.starPercentages?.[3] || 3 },
                        { star: 2, pct: reviewData?.starPercentages?.[2] || 3 },
                        { star: 1, pct: reviewData?.starPercentages?.[1] || 8 },
                      ].map((item) => (
                        <div key={item.star} className="flex items-center space-x-2 text-[11px] font-semibold text-gray-600 dark:text-slate-300">
                          <span className="w-5">{item.star} ★</span>
                          <div className="flex-1 h-2 rounded-full bg-gray-200 dark:bg-slate-700 overflow-hidden">
                            <div
                              className="h-full bg-gray-800 dark:bg-slate-300 rounded-full"
                              style={{ width: `${item.pct}%` }}
                            />
                          </div>
                          <span className="w-8 text-right text-gray-500 dark:text-slate-400">
                            {item.pct}%
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Loved by travelers green pills matching Photo 1 */}
                  <div>
                    <h4 className="text-xs font-bold text-gray-700 dark:text-slate-300 mb-2">
                      Loved by travelers
                    </h4>
                    <div className="flex flex-wrap gap-1.5">
                      {[
                        { name: "Punctuality", count: 210 },
                        { name: "Staff behavior", count: 181 },
                        { name: "Seat / Sleep Comfort", count: 179 },
                        { name: "Driving", count: 178 },
                        { name: "Cleanliness", count: 174 },
                        { name: "Rest stop hygiene", count: 171 },
                        { name: "AC", count: 155 },
                        { name: "Live tracking", count: 151 },
                      ].map((tag, tIdx) => (
                        <span
                          key={tIdx}
                          className="px-2.5 py-1 bg-emerald-100 dark:bg-emerald-950/70 text-emerald-800 dark:text-emerald-300 rounded-md text-[11px] font-semibold"
                        >
                          {tag.name} ({tag.count})
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* Reviews List */}
                  <div className="space-y-3 pt-2">
                    <h4 className="text-xs font-bold text-gray-700 dark:text-slate-300">
                      Recent Passenger Reviews
                    </h4>
                    {reviewData?.reviews && reviewData.reviews.length > 0 ? (
                      reviewData.reviews.map((rev) => (
                        <div
                          key={rev.id}
                          className="p-3.5 bg-white dark:bg-slate-800 rounded-2xl border border-gray-100 dark:border-slate-700 shadow-2xs space-y-1.5"
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex items-center space-x-2">
                              <span className="font-bold text-xs text-gray-900 dark:text-white">
                                {rev.userName}
                              </span>
                              <span className="px-1.5 py-0.2 bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 rounded text-[9px] font-bold">
                                Verified
                              </span>
                            </div>
                            <div className="flex items-center space-x-0.5 text-amber-400">
                              {[...Array(rev.rating)].map((_, i) => (
                                <Star key={i} className="w-3 h-3 fill-amber-400" />
                              ))}
                            </div>
                          </div>

                          <p className="text-xs text-gray-600 dark:text-slate-300 leading-relaxed">
                            {rev.comment}
                          </p>

                          {rev.tags && rev.tags.length > 0 && (
                            <div className="flex flex-wrap gap-1 pt-1">
                              {rev.tags.map((t, idx) => (
                                <span
                                  key={idx}
                                  className="text-[9px] font-bold text-gray-500 bg-gray-100 dark:bg-slate-700 px-2 py-0.5 rounded-full"
                                >
                                  {t}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      ))
                    ) : (
                      <p className="text-xs text-gray-400">No reviews yet. Be the first to review!</p>
                    )}
                  </div>
                </div>
              )}

              {/* 7. Bus Safety Tab */}
              {activeTab === "safety" && (
                <div className="space-y-4">
                  <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                    redBus Certified Bus Safety
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {[
                      { title: "Sanitized Bus", desc: "Vehicle thoroughly disinfected prior to each departure." },
                      { title: "GPS Live Tracking", desc: "Share real-time location with family via WhatsApp." },
                      { title: "CCTV Surveillance", desc: "Installed in passenger cabin and luggage bays." },
                      { title: "Women Safe Seat Guarantee", desc: "Adjacent berth reserved solely for female passengers." },
                    ].map((s, idx) => (
                      <div
                        key={idx}
                        className="p-3.5 bg-gray-50 dark:bg-slate-800 rounded-2xl border border-gray-200 dark:border-slate-700 space-y-1"
                      >
                        <h5 className="font-bold text-xs text-gray-900 dark:text-white flex items-center gap-1.5">
                          <ShieldCheck className="w-4 h-4 text-emerald-600" />
                          <span>{s.title}</span>
                        </h5>
                        <p className="text-[11px] text-gray-500 dark:text-slate-400 leading-relaxed">
                          {s.desc}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
                </motion.div>
              </AnimatePresence>
            </div>

            {/* Bottom Action Bar: Selected seats summary + Proceed to Book button */}
            <div className="p-4 bg-white dark:bg-[#0f172a] border-t border-gray-200 dark:border-slate-800 flex items-center justify-between">
              <div>
                <div className="flex items-center space-x-2">
                  <span className="text-xs text-gray-500 dark:text-slate-400 font-medium">
                    Selected Seats:
                  </span>
                  {selectedSeats.length > 0 ? (
                    <span className="font-extrabold text-xs text-gray-900 dark:text-white">
                      {selectedSeats.map((s) => s.seatNumber).join(", ")} ({selectedSeats.length} seats)
                    </span>
                  ) : (
                    <span className="text-xs text-gray-400 italic">None selected</span>
                  )}
                </div>
                <div className="flex items-center space-x-1.5 text-[11px] text-gray-400 mt-0.5">
                  <span>Board: {currentBoarding}</span>
                  <span>•</span>
                  <span>Drop: {currentDropping}</span>
                </div>
              </div>

              <div className="flex items-center space-x-4">
                {selectedSeats.length > 0 && (
                  <div className="text-right">
                    <span className="text-[10px] text-gray-400 block font-medium">Total Fare</span>
                    <span className="text-lg font-black text-gray-900 dark:text-white">
                      ₹{totalPrice}
                    </span>
                  </div>
                )}

                <button
                  type="button"
                  disabled={selectedSeats.length === 0}
                  onClick={handleProceedToBook}
                  className="px-6 py-3 bg-[#d84e55] hover:bg-[#b83e44] disabled:bg-gray-200 disabled:text-gray-400 text-white font-extrabold text-xs rounded-xl shadow-md transition-all flex items-center space-x-1.5 cursor-pointer disabled:cursor-not-allowed active:scale-95"
                >
                  <span>Proceed to Book</span>
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
      )}
    </AnimatePresence>

      {/* Write Review Modal */}
      <WriteReviewModal
        busId={route.busId || route.id}
        busName={route.operatorName}
        isOpen={showWriteReview}
        onClose={() => setShowWriteReview(false)}
        onReviewSubmitted={() => refetchReviews()}
      />
    </>
  );
}
