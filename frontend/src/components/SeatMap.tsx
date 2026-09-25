"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAppDispatch, useAppSelector } from "@/store";
import {
  toggleSeatSelection,
  setLockDetails,
  setBoardingPoint,
  setDroppingPoint,
  clearSeatLocks,
} from "@/store/bookingSlice";
import {
  useGetRouteSeatsQuery,
  useLockSeatsMutation,
  useUnlockSeatsMutation,
} from "@/store/apiSlice";
import { useSeatLockTimer } from "@/hooks/useSeatLockTimer";
import type { RouteItem, SeatItem } from "@/types";
import {
  Clock,
  CheckCircle2,
  AlertCircle,
  MapPin,
  ShieldAlert,
  ArrowRight,
  Info,
  X,
  User as UserIcon,
} from "lucide-react";

interface SeatMapProps {
  route: RouteItem;
  onClose?: () => void;
}

export default function SeatMap({ route, onClose }: SeatMapProps) {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);
  const { selectedSeats, boardingPoint, droppingPoint } = useAppSelector(
    (state) => state.booking
  );

  const [errorMessage, setErrorMessage] = useState("");
  const [femaleRestrictionModal, setFemaleRestrictionModal] = useState<SeatItem | null>(null);

  const [selectedBoarding, setSelectedBoarding] = useState(
    boardingPoint || route.boardingPoints?.[0] || "Main Boarding Point"
  );
  const [selectedDropping, setSelectedDropping] = useState(
    droppingPoint || route.droppingPoints?.[0] || "Main Dropping Point"
  );

  const { data: layoutData, isLoading, refetch } = useGetRouteSeatsQuery(route.id, {
    refetchOnMountOrArgChange: true,
    pollingInterval: 10000,
  });

  // Realistic 1-second seat loading timer
  const [isMinSeatLoading, setIsMinSeatLoading] = useState(true);
  useEffect(() => {
    setIsMinSeatLoading(true);
    const timer = setTimeout(() => {
      setIsMinSeatLoading(false);
    }, 1000); // 1-second delay
    return () => clearTimeout(timer);
  }, [route.id]);

  const showSeatsLoader = isLoading || isMinSeatLoading;
  const [lockSeatsMutation, { isLoading: isLocking }] = useLockSeatsMutation();
  const [unlockSeatsMutation] = useUnlockSeatsMutation();
  const { formattedTime, isLocked } = useSeatLockTimer();

  const handleClose = async () => {
    if (selectedSeats.length > 0) {
      try {
        await unlockSeatsMutation({
          routeId: route.id,
          seatIds: selectedSeats.map((s) => s.seatId),
          userId: user?.id,
        }).unwrap();
      } catch {}
    }
    dispatch(clearSeatLocks());
    if (onClose) onClose();
  };

  const seats = layoutData?.seats || [];
  const lowerDeckSeats = seats.filter((s) => s.deck === "LOWER");
  const upperDeckSeats = seats.filter((s) => s.deck === "UPPER");
  const hasUpperDeck = upperDeckSeats.length > 0;

  // Helper to check if a seat is adjacent to a female-booked seat
  const isAdjacentToFemale = (seat: SeatItem, allDeckSeats: SeatItem[]) => {
    if (seat.genderRestriction === "FEMALE") return true;
    const sameRowSeats = allDeckSeats.filter(
      (s) => s.rowNum === seat.rowNum && s.id !== seat.id
    );
    return sameRowSeats.some((other) => {
      const isPair =
        (seat.colNum === 2 && other.colNum === 3) ||
        (seat.colNum === 3 && other.colNum === 2) ||
        (seat.colNum === 1 && other.colNum === 2) ||
        (seat.colNum === 3 && other.colNum === 4);
      return (
        isPair &&
        (other.status === "BOOKED" && other.bookedGender === "FEMALE")
      );
    });
  };

  const handleSeatClick = (seat: SeatItem, deckSeats: SeatItem[]) => {
    setErrorMessage("");

    if (seat.status === "BOOKED") {
      setErrorMessage(`Seat ${seat.seatNumber} is already booked.`);
      return;
    }

    const isAlreadySelected = selectedSeats.some((s) => s.seatId === seat.seatId);
    const isLockedBySomeoneElse =
      seat.status === "LOCKED" &&
      !isAlreadySelected &&
      Boolean(seat.lockedByUserId && user?.id ? seat.lockedByUserId !== user.id : true);

    if (isLockedBySomeoneElse) {
      setErrorMessage(`Seat ${seat.seatNumber} is currently locked by another passenger.`);
      return;
    }

    // If deselecting, immediately unlock on server
    if (isAlreadySelected) {
      unlockSeatsMutation({
        routeId: route.id,
        seatIds: [seat.seatId],
        userId: user?.id,
      });
    }

    // Female adjacent seat safety check
    const isRestrictedForLadies = isAdjacentToFemale(seat, deckSeats);
    if (isRestrictedForLadies && !isAlreadySelected) {
      if (user?.gender === "MALE") {
        setErrorMessage(
          `Seat ${seat.seatNumber} is reserved next to a female traveler. Male passengers cannot select this seat.`
        );
        return;
      }
      if (!user?.gender) {
        // Prompt confirmation for female traveler
        setFemaleRestrictionModal(seat);
        return;
      }
    }

    dispatch(toggleSeatSelection(seat));
  };

  const handleProceedToBook = async () => {
    if (selectedSeats.length === 0) {
      setErrorMessage("Please select at least 1 seat to proceed.");
      return;
    }

    setErrorMessage("");
    try {
      const seatIds = selectedSeats.map((s) => s.seatId);
      const res = await lockSeatsMutation({
        routeId: route.id,
        seatIds,
        userId: user?.id,
      }).unwrap();

      dispatch(
        setLockDetails({
          lockedSeatIds: res.lockedSeatIds,
          lockExpiry: res.lockExpiry,
          remainingSeconds: res.remainingSeconds,
        })
      );
      dispatch(setBoardingPoint(selectedBoarding));
      dispatch(setDroppingPoint(selectedDropping));

      router.push(`/checkout?routeId=${route.id}`);
    } catch (err: any) {
      setErrorMessage(
        err?.data?.message || err?.message || "Failed to lock seats. Someone may have just reserved them."
      );
      refetch();
    }
  };

  const calculateTotal = () => {
    return selectedSeats.reduce((sum, s) => sum + (s.price || route.basePrice), 0);
  };

  // Group seats by row
  const getDeckRows = (deckSeats: SeatItem[]) => {
    const rowsMap = new Map<number, SeatItem[]>();
    deckSeats.forEach((s) => {
      const r = s.rowNum || 1;
      if (!rowsMap.has(r)) rowsMap.set(r, []);
      rowsMap.get(r)!.push(s);
    });
    return Array.from(rowsMap.keys()).sort((a, b) => a - b);
  };

  // Render authentic sleeper berth matching screenshots
  const renderSleeperBerth = (seat: SeatItem, deckSeats: SeatItem[]) => {
    const isSelected = selectedSeats.some((s) => s.seatId === seat.seatId);
    const isBooked = seat.status === "BOOKED";
    const isLockedByOther =
      seat.status === "LOCKED" &&
      !isSelected &&
      Boolean(seat.lockedByUserId && user?.id ? seat.lockedByUserId !== user.id : true);
    const isFemaleBooked = isBooked && seat.bookedGender === "FEMALE";
    const isFemaleOnly = !isBooked && isAdjacentToFemale(seat, deckSeats);

    let containerStyle =
      "relative w-11 sm:w-12 h-22 rounded-xl flex flex-col items-center justify-between py-2 px-1 transition-all select-none ";
    let pillowStyle = "w-7 h-1.5 rounded-full ";
    let priceOrLabel = `₹${Math.round(seat.price || route.basePrice)}`;
    let priceColor = "text-gray-900 font-bold";

    if (isSelected) {
      containerStyle += "border-2 border-blue-600 bg-blue-50/60 shadow-md scale-105";
      pillowStyle += "bg-blue-300";
      priceColor = "text-blue-600 font-extrabold";
    } else if (isBooked) {
      if (isFemaleBooked) {
        containerStyle += "border border-pink-200 bg-[#fdf2f4] cursor-not-allowed";
        pillowStyle += "bg-pink-200";
      } else {
        containerStyle += "border border-gray-200 bg-[#f0f4f9] cursor-not-allowed";
        pillowStyle += "bg-blue-100";
      }
      priceOrLabel = "Sold";
      priceColor = "text-gray-400 font-medium";
    } else if (isLockedByOther) {
      containerStyle += "border border-amber-300 bg-amber-50/60 cursor-not-allowed";
      pillowStyle += "bg-amber-200";
      priceOrLabel = "Locked";
      priceColor = "text-amber-600 font-medium";
    } else if (isFemaleOnly) {
      containerStyle += "border-2 border-pink-500 bg-white hover:bg-pink-50 cursor-pointer hover:shadow-md";
      pillowStyle += "bg-pink-200";
      priceColor = "text-pink-600 font-bold";
    } else {
      // Default Available Sleeper
      containerStyle += "border-2 border-emerald-600 bg-white hover:bg-emerald-50/50 cursor-pointer hover:shadow-md";
      pillowStyle += "bg-emerald-200";
      priceColor = "text-gray-800 font-bold";
    }

    return (
      <div key={seat.id} className="flex flex-col items-center">
        <button
          type="button"
          disabled={isBooked || isLockedByOther}
          onClick={() => handleSeatClick(seat, deckSeats)}
          className={containerStyle}
          title={`Seat ${seat.seatNumber} (${seat.deck} Deck Sleeper) - ${priceOrLabel}`}
        >
          {/* Top Pill / Person Icon */}
          <div className="flex items-center justify-center h-full">
            {isBooked ? (
              isFemaleBooked ? (
                // Soft pink female icon
                <svg className="w-3.5 h-3.5 text-pink-400" viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="12" cy="7" r="4" />
                  <path d="M5.5 21v-3a4.5 4.5 0 0 1 9 0v3h-9z" />
                </svg>
              ) : (
                // Soft slate male icon
                <svg className="w-3.5 h-3.5 text-slate-400" viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="12" cy="7" r="4" />
                  <path d="M5.5 21v-3a4.5 4.5 0 0 1 9 0v3h-9z" />
                </svg>
              )
            ) : isFemaleOnly ? (
              <svg className="w-3.5 h-3.5 text-pink-500 animate-pulse" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="7" r="4" />
                <path d="M5.5 21v-3a4.5 4.5 0 0 1 9 0v3h-9z" />
              </svg>
            ) : isSelected ? (
              <svg className="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="7" r="4" />
                <path d="M6 21v-2a6 6 0 0 1 12 0v2H6z" />
              </svg>
            ) : null}
          </div>

          {/* Pillow Headrest Indicator */}
          <div className={pillowStyle} />
        </button>

        {/* Price / Sold label below berth */}
        <span className={`text-[11px] mt-0.5 tracking-tight ${priceColor}`}>
          {priceOrLabel}
        </span>
      </div>
    );
  };

  // Render authentic seater chair matching screenshot 2
  const renderSeaterChair = (seat: SeatItem, deckSeats: SeatItem[]) => {
    const isSelected = selectedSeats.some((s) => s.seatId === seat.seatId);
    const isBooked = seat.status === "BOOKED";
    const isLockedByOther =
      seat.status === "LOCKED" &&
      !isSelected &&
      Boolean(seat.lockedByUserId && user?.id ? seat.lockedByUserId !== user.id : true);
    const isFemaleBooked = isBooked && seat.bookedGender === "FEMALE";
    const isFemaleOnly = !isBooked && isAdjacentToFemale(seat, deckSeats);

    let strokeColor = "#16a34a"; // green
    let fillColor = "#ffffff";
    let priceOrLabel = `₹${Math.round(seat.price || route.basePrice)}`;
    let priceColor = "text-gray-800 font-bold";

    if (isSelected) {
      strokeColor = "#2563eb"; // blue
      fillColor = "#eff6ff";
      priceColor = "text-blue-600 font-extrabold";
    } else if (isBooked) {
      if (isFemaleBooked) {
        strokeColor = "#f472b6";
        fillColor = "#fdf2f4";
      } else {
        strokeColor = "#cbd5e1";
        fillColor = "#f0f4f9";
      }
      priceOrLabel = "Sold";
      priceColor = "text-gray-400 font-medium";
    } else if (isLockedByOther) {
      strokeColor = "#f59e0b";
      fillColor = "#fffbeb";
      priceOrLabel = "Locked";
      priceColor = "text-amber-600 font-medium";
    } else if (isFemaleOnly) {
      strokeColor = "#ec4899";
      fillColor = "#ffffff";
      priceColor = "text-pink-600 font-bold";
    }

    return (
      <div key={seat.id} className="flex flex-col items-center">
        <button
          type="button"
          disabled={isBooked || isLockedByOther}
          onClick={() => handleSeatClick(seat, deckSeats)}
          className="p-0.5 rounded-lg hover:scale-105 transition-transform cursor-pointer disabled:cursor-not-allowed"
          title={`Seat ${seat.seatNumber} (${seat.deck} Deck Seater) - ${priceOrLabel}`}
        >
          {/* Authentic redBus Chair Silhouette SVG */}
          <svg className="w-10 h-11" viewBox="0 0 44 48" fill="none">
            {/* Outer Chair Shell */}
            <rect
              x="6"
              y="6"
              width="32"
              height="34"
              rx="8"
              fill={fillColor}
              stroke={strokeColor}
              strokeWidth="2.5"
            />
            {/* Cushion Line */}
            <path
              d="M11 26h22"
              stroke={strokeColor}
              strokeWidth="2"
              strokeLinecap="round"
            />
            {/* Headrest Pill */}
            <rect
              x="13"
              y="11"
              width="18"
              height="8"
              rx="4"
              fill={isSelected ? "#93c5fd" : isBooked ? (isFemaleBooked ? "#fbcfe8" : "#cbd5e1") : "#bbf7d0"}
            />
          </svg>
        </button>

        {/* Price / Sold label below seat */}
        <span className={`text-[11px] mt-0.5 tracking-tight ${priceColor}`}>
          {priceOrLabel}
        </span>
      </div>
    );
  };

  // Deck Column Container
  const renderDeckChassis = (title: string, deckSeats: SeatItem[], isLowerDeck: boolean) => {
    const rows = getDeckRows(deckSeats);

    return (
      <div className="flex-1 max-w-[280px] sm:max-w-[320px]">
        {/* Deck Header */}
        <div className="flex items-center justify-between px-2 mb-3">
          <h4 className="text-base font-bold text-gray-900">{title}</h4>
          {isLowerDeck && (
            <div className="w-8 h-8 rounded-full border border-gray-300 bg-gray-50 flex items-center justify-center text-gray-500 shadow-xs" title="Driver Cabin">
              {/* Steering Wheel SVG */}
              <svg className="w-5 h-5 text-gray-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="9" />
                <circle cx="12" cy="12" r="3" />
                <line x1="12" y1="3" x2="12" y2="9" />
                <line x1="3" y1="12" x2="9" y2="12" />
                <line x1="15" y1="12" x2="21" y2="12" />
                <line x1="12" y1="15" x2="12" y2="21" />
              </svg>
            </div>
          )}
        </div>

        {/* Chassis Body */}
        <div className="rounded-3xl border-2 border-gray-200 bg-white p-4 shadow-sm relative min-h-[460px] flex flex-col justify-between">
          <div className="space-y-4">
            {rows.map((rowNum) => {
              const rowSeats = deckSeats.filter((s) => (s.rowNum || 1) === rowNum);
              const leftSeat = rowSeats.find((s) => s.colNum === 1);
              const rightSeat1 = rowSeats.find((s) => s.colNum === 2);
              const rightSeat2 = rowSeats.find((s) => s.colNum === 3);

              return (
                <div key={rowNum} className="flex items-center justify-between">
                  {/* Left Column (Single Berth or Chair) */}
                  <div className="w-12 flex justify-center">
                    {leftSeat ? (
                      leftSeat.seatType === "SEATER"
                        ? renderSeaterChair(leftSeat, deckSeats)
                        : renderSleeperBerth(leftSeat, deckSeats)
                    ) : (
                      <div className="w-11 h-20" />
                    )}
                  </div>

                  {/* Aisle Walking Space */}
                  <div className="w-6 sm:w-8" />

                  {/* Right Column (2 Berths / Chairs Side by Side) */}
                  <div className="flex items-center space-x-2 sm:space-x-2.5">
                    {rightSeat1 ? (
                      rightSeat1.seatType === "SEATER"
                        ? renderSeaterChair(rightSeat1, deckSeats)
                        : renderSleeperBerth(rightSeat1, deckSeats)
                    ) : (
                      <div className="w-11 h-20" />
                    )}
                    {rightSeat2 ? (
                      rightSeat2.seatType === "SEATER"
                        ? renderSeaterChair(rightSeat2, deckSeats)
                        : renderSleeperBerth(rightSeat2, deckSeats)
                    ) : (
                      <div className="w-11 h-20" />
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Emergency Exit Indicator at the Bottom */}
          <div className="pt-6 pb-1 text-center border-t border-dashed border-gray-200 mt-4">
            <span className="inline-flex items-center text-[10px] font-semibold text-gray-400 uppercase tracking-wider">
              ▼ Emergency Exit
            </span>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="bg-gray-50/80 border-t border-gray-200 p-4 sm:p-6 rounded-b-2xl animate-in slide-in-from-top-2 duration-300">
      {/* Top Banner: Seat Lock Timer */}
      {isLocked && (
        <div className="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-2xl flex items-center justify-between text-xs text-amber-800">
          <div className="flex items-center space-x-2">
            <Clock className="w-4 h-4 text-amber-600 animate-pulse" />
            <span className="font-semibold">Seats Locked for You:</span>
            <span>Complete checkout before time runs out.</span>
          </div>
          <span className="font-mono font-bold text-sm bg-amber-200 px-2.5 py-0.5 rounded-lg text-amber-900">
            {formattedTime}
          </span>
        </div>
      )}

      {errorMessage && (
        <div className="mb-4 p-3.5 bg-red-50 border border-red-200 rounded-2xl flex items-center space-x-2 text-xs text-red-700">
          <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
          <span>{errorMessage}</span>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Main Seat Layout matching user's images */}
        <div className="lg:col-span-8 bg-white p-6 rounded-3xl border border-gray-100 shadow-sm">
          {/* Legend */}
          <div className="flex flex-wrap items-center justify-center gap-4 pb-6 mb-6 border-b border-gray-100 text-xs">
            <div className="flex items-center space-x-1.5">
              <div className="w-3.5 h-3.5 rounded border-2 border-emerald-600 bg-white" />
              <span className="text-gray-600 font-medium">Available</span>
            </div>
            <div className="flex items-center space-x-1.5">
              <div className="w-3.5 h-3.5 rounded border border-gray-300 bg-[#f0f4f9]" />
              <span className="text-gray-500 font-medium">Sold (Male)</span>
            </div>
            <div className="flex items-center space-x-1.5">
              <div className="w-3.5 h-3.5 rounded border border-pink-300 bg-[#fdf2f4]" />
              <span className="text-pink-700 font-medium">Sold (Female)</span>
            </div>
            <div className="flex items-center space-x-1.5">
              <div className="w-3.5 h-3.5 rounded border-2 border-pink-500 bg-white" />
              <span className="text-pink-600 font-bold">Ladies Reserved</span>
            </div>
            <div className="flex items-center space-x-1.5">
              <div className="w-3.5 h-3.5 rounded border-2 border-blue-600 bg-blue-50" />
              <span className="text-blue-700 font-medium">Selected</span>
            </div>
          </div>

          {/* Side-by-Side Decks Chassis (matching images 1 & 2) */}
          {showSeatsLoader ? (
            <div className="flex flex-col md:flex-row items-center justify-center gap-8 py-2 animate-pulse">
              {/* Lower Deck Skeleton */}
              <div className="flex-1 max-w-[280px] sm:max-w-[320px] w-full">
                <div className="flex items-center justify-between px-2 mb-3">
                  <div className="h-5 w-24 bg-gray-200 rounded" />
                  <div className="w-8 h-8 rounded-full bg-gray-200" />
                </div>
                <div className="rounded-3xl border-2 border-gray-200 bg-white p-4 shadow-sm min-h-[460px] flex flex-col justify-between">
                  <div className="space-y-4">
                    {[1, 2, 3, 4, 5].map((r) => (
                      <div key={r} className="flex items-center justify-between">
                        <div className="w-11 sm:w-12 h-20 rounded-xl bg-gray-100 border border-gray-200 flex flex-col items-center justify-between p-2">
                          <div className="w-7 h-1.5 rounded-full bg-gray-200" />
                          <div className="w-4 h-4 rounded-full bg-gray-200" />
                          <div className="w-7 h-2 rounded bg-gray-200" />
                        </div>
                        <div className="w-6 sm:w-8" />
                        <div className="flex items-center space-x-2">
                          <div className="w-11 sm:w-12 h-20 rounded-xl bg-gray-100 border border-gray-200 flex flex-col items-center justify-between p-2">
                            <div className="w-7 h-1.5 rounded-full bg-gray-200" />
                            <div className="w-4 h-4 rounded-full bg-gray-200" />
                            <div className="w-7 h-2 rounded bg-gray-200" />
                          </div>
                          <div className="w-11 sm:w-12 h-20 rounded-xl bg-gray-100 border border-gray-200 flex flex-col items-center justify-between p-2">
                            <div className="w-7 h-1.5 rounded-full bg-gray-200" />
                            <div className="w-4 h-4 rounded-full bg-gray-200" />
                            <div className="w-7 h-2 rounded bg-gray-200" />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                  <div className="pt-6 pb-1 text-center border-t border-dashed border-gray-200 mt-4">
                    <div className="h-3 w-28 bg-gray-200 rounded mx-auto" />
                  </div>
                </div>
              </div>

              {/* Upper Deck Skeleton */}
              {hasUpperDeck && (
                <div className="flex-1 max-w-[280px] sm:max-w-[320px] w-full">
                  <div className="flex items-center justify-between px-2 mb-3">
                    <div className="h-5 w-24 bg-gray-200 rounded" />
                    <div className="w-8 h-8 rounded-full bg-gray-200" />
                  </div>
                  <div className="rounded-3xl border-2 border-gray-200 bg-white p-4 shadow-sm min-h-[460px] flex flex-col justify-between">
                    <div className="space-y-4">
                      {[1, 2, 3, 4, 5].map((r) => (
                        <div key={r} className="flex items-center justify-between">
                          <div className="w-11 sm:w-12 h-20 rounded-xl bg-gray-100 border border-gray-200 flex flex-col items-center justify-between p-2">
                            <div className="w-7 h-1.5 rounded-full bg-gray-200" />
                            <div className="w-4 h-4 rounded-full bg-gray-200" />
                            <div className="w-7 h-2 rounded bg-gray-200" />
                          </div>
                          <div className="w-6 sm:w-8" />
                          <div className="flex items-center space-x-2">
                            <div className="w-11 sm:w-12 h-20 rounded-xl bg-gray-100 border border-gray-200 flex flex-col items-center justify-between p-2">
                              <div className="w-7 h-1.5 rounded-full bg-gray-200" />
                              <div className="w-4 h-4 rounded-full bg-gray-200" />
                              <div className="w-7 h-2 rounded bg-gray-200" />
                            </div>
                            <div className="w-11 sm:w-12 h-20 rounded-xl bg-gray-100 border border-gray-200 flex flex-col items-center justify-between p-2">
                              <div className="w-7 h-1.5 rounded-full bg-gray-200" />
                              <div className="w-4 h-4 rounded-full bg-gray-200" />
                              <div className="w-7 h-2 rounded bg-gray-200" />
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                    <div className="pt-6 pb-1 text-center border-t border-dashed border-gray-200 mt-4">
                      <div className="h-3 w-28 bg-gray-200 rounded mx-auto" />
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="flex flex-col md:flex-row items-center justify-center gap-8 py-2">
              {/* Lower Deck */}
              {lowerDeckSeats.length > 0 && renderDeckChassis("Lower deck", lowerDeckSeats, true)}

              {/* Upper Deck */}
              {hasUpperDeck && renderDeckChassis("Upper deck", upperDeckSeats, false)}
            </div>
          )}
        </div>

        {/* Right Sidebar: Booking Summary & Points */}
        <div className="lg:col-span-4 space-y-4">
          <div className="bg-white p-5 sm:p-6 rounded-3xl border border-gray-100 shadow-sm space-y-5">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h4 className="text-base font-bold text-gray-900">Booking Summary</h4>
              <span className="text-xs bg-red-50 text-[#d84e55] font-bold px-2 py-0.5 rounded-full">
                {selectedSeats.length} {selectedSeats.length === 1 ? "seat" : "seats"}
              </span>
            </div>

            {/* Selected Seats List */}
            {selectedSeats.length > 0 ? (
              <div className="space-y-2">
                <div className="flex flex-wrap gap-2">
                  {selectedSeats.map((s) => (
                    <div
                      key={s.id}
                      className="px-3 py-1.5 bg-blue-50 border border-blue-200 text-blue-800 rounded-xl text-xs font-bold flex items-center space-x-1"
                    >
                      <span>Seat {s.seatNumber}</span>
                      <span className="text-blue-500">({s.deck[0]}D)</span>
                    </div>
                  ))}
                </div>

                <div className="pt-3 border-t border-gray-100 flex items-center justify-between">
                  <span className="text-xs text-gray-500">Base Fare:</span>
                  <span className="text-lg font-black text-gray-900">₹{calculateTotal()}</span>
                </div>
              </div>
            ) : (
              <div className="py-4 text-center text-xs text-gray-400">
                Click any available green seat to begin selection
              </div>
            )}

            {/* Boarding and Dropping Points */}
            <div className="space-y-3 pt-2 border-t border-gray-100">
              <div>
                <label className="block text-[11px] font-semibold text-gray-500 mb-1 uppercase tracking-wider">
                  Boarding Point
                </label>
                <select
                  value={selectedBoarding}
                  onChange={(e) => setSelectedBoarding(e.target.value)}
                  className="w-full text-xs font-medium py-2.5 px-3 rounded-xl border border-gray-200 bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                >
                  {route.boardingPoints?.map((bp) => (
                    <option key={bp} value={bp}>
                      {bp}
                    </option>
                  )) || <option value={selectedBoarding}>{selectedBoarding}</option>}
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-gray-500 mb-1 uppercase tracking-wider">
                  Dropping Point
                </label>
                <select
                  value={selectedDropping}
                  onChange={(e) => setSelectedDropping(e.target.value)}
                  className="w-full text-xs font-medium py-2.5 px-3 rounded-xl border border-gray-200 bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                >
                  {route.droppingPoints?.map((dp) => (
                    <option key={dp} value={dp}>
                      {dp}
                    </option>
                  )) || <option value={selectedDropping}>{selectedDropping}</option>}
                </select>
              </div>
            </div>

            {/* Proceed to Book Button */}
            <button
              type="button"
              disabled={selectedSeats.length === 0 || isLocking}
              onClick={handleProceedToBook}
              className="w-full py-3.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-2xl font-bold text-sm shadow-md shadow-red-500/20 transition-all flex items-center justify-center space-x-2 disabled:opacity-50 cursor-pointer"
            >
              {isLocking ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>Locking Seats...</span>
                </>
              ) : (
                <>
                  <span>Continue to Passenger Details</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Female Seat Adjacent Modal Confirmation */}
      {femaleRestrictionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-gray-100 text-center relative">
            <button
              onClick={() => setFemaleRestrictionModal(null)}
              className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 p-1"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="w-14 h-14 rounded-2xl bg-pink-50 border border-pink-100 flex items-center justify-center text-pink-600 mx-auto mb-3">
              <UserIcon className="w-7 h-7" />
            </div>

            <h3 className="text-lg font-bold text-gray-900 mb-1">Ladies Reserved Seat</h3>
            <p className="text-xs text-gray-500 leading-relaxed mb-5">
              Seat <strong>{femaleRestrictionModal.seatNumber}</strong> is adjacent to a female passenger. As per safety rules, this seat is reserved exclusively for female travelers.
            </p>

            <div className="space-y-2">
              <button
                type="button"
                onClick={() => {
                  dispatch(toggleSeatSelection(femaleRestrictionModal));
                  setFemaleRestrictionModal(null);
                }}
                className="w-full py-2.5 px-4 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
              >
                Yes, I am a Female Traveler
              </button>
              <button
                type="button"
                onClick={() => setFemaleRestrictionModal(null)}
                className="w-full py-2 px-4 border border-gray-200 text-gray-600 rounded-xl text-xs font-semibold hover:bg-gray-50 transition-colors"
              >
                Select Another Seat
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
