"use client";

import React, { useState, useEffect } from "react";
import { useAppDispatch, useAppSelector } from "@/store";
import { setSelectedRoute } from "@/store/bookingSlice";
import { useGetBusReviewsQuery, useGetMyBookingsQuery } from "@/store/apiSlice";
import type { RouteItem } from "@/types";
import { Star, ChevronRight } from "lucide-react";
import BusDetailsModal from "./BusDetailsModal";

interface BusCardProps {
  route: RouteItem;
  autoOpen?: boolean;
}

function formatTripDuration(dep?: string, arr?: string, fallbackHours?: number): string {
  if (dep && arr) {
    const [depH, depM] = dep.slice(0, 5).split(":").map(Number);
    const [arrH, arrM] = arr.slice(0, 5).split(":").map(Number);
    if (!isNaN(depH) && !isNaN(depM) && !isNaN(arrH) && !isNaN(arrM)) {
      let diff = (arrH * 60 + arrM) - (depH * 60 + depM);
      if (diff < 0) diff += 24 * 60;
      const h = Math.floor(diff / 60);
      const m = diff % 60;
      return m > 0 ? `${h}h ${String(m).padStart(2, "0")}m` : `${h}h 00m`;
    }
  }
  return fallbackHours ? `${fallbackHours} hrs` : "6.5 hrs";
}

export default function BusCard({ route, autoOpen = false }: BusCardProps) {
  const dispatch = useAppDispatch();
  const [showModal, setShowModal] = useState(false);
  const [initialTab, setInitialTab] = useState<string>("cancellation");

  const { user } = useAppSelector((state) => state.auth);
  const { data: bookingsData } = useGetMyBookingsQuery({}, { skip: !user });

  const isPreviouslyBooked = bookingsData?.content?.some(
    (b) =>
      b.status === "CONFIRMED" &&
      (b.busId === route.busId ||
        b.routeId === route.id ||
        (b.operatorName.toLowerCase() === route.operatorName.toLowerCase() &&
          b.departureTime?.slice(0, 5) === route.departureTime?.slice(0, 5)))
  );

  const { data: reviewData } = useGetBusReviewsQuery(route.busId || route.id);
  const displayRating = reviewData
    ? (reviewData.totalRatings > 0 ? reviewData.averageRating.toFixed(1) : (route.rating > 0 ? route.rating.toFixed(1) : "4.0"))
    : (route.rating > 0 ? route.rating.toFixed(1) : "4.0");

  const totalRatingsCount = reviewData?.totalRatings ?? route.totalRatings ?? 608;

  const primaryAmenity =
    route.amenities?.find((a) => a.toLowerCase().includes("toilet")) ||
    route.amenities?.find((a) => a.toLowerCase().includes("tracking")) ||
    route.amenities?.[0] ||
    "Toilet";

  useEffect(() => {
    if (autoOpen) {
      dispatch(setSelectedRoute(route));
      setShowModal(true);
    }
  }, [autoOpen, route, dispatch]);

  const openDetailsWithTab = (tab: string) => {
    dispatch(setSelectedRoute(route));
    setInitialTab(tab);
    setShowModal(true);
  };

  const isPrimo = Boolean(route.primoCertified);
  const licensePlate =
    route.busNumber && route.busNumber.trim().length > 0
      ? route.busNumber.trim().toUpperCase()
      : `TN${String(10 + ((route.busId || route.id || 1) % 75)).padStart(2, "0")}BB${String(1000 + ((route.id || 1) * 37) % 8999)}`;

  const cleanStops = (route.boardingPoints || [])
    .map((bp) => bp.replace(/\s*\([^)]*\)/g, "").trim())
    .filter(Boolean);
  const firstStop = cleanStops[0] || route.sourceCity || "Main Bus Stand";
  const secondStop = cleanStops[1] || `${route.sourceCity || "City"} Bypass`;
  const remainingStopsCount = cleanStops.length > 2 ? cleanStops.length - 2 : 4;

  return (
    <>
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-gray-200 dark:border-slate-800 shadow-2xs hover:shadow-md transition-all mb-4 p-3.5 sm:p-4">
        {/* Top Bus Number & Starting Stops Strip (Image 4) */}
        <div className="bg-[#f2f3f8] dark:bg-slate-800/80 rounded-xl px-3 py-2.5 mb-3">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-[5px] bg-[#f5cb39] text-slate-950 border border-slate-900 text-xs font-extrabold tracking-wide shadow-2xs">
              <span>•</span>
              <span>{licensePlate}</span>
              <span>•</span>
            </span>
            <span className="inline-block px-2 py-0.5 rounded-[5px] bg-[#e0e4fa] dark:bg-indigo-950/80 text-slate-900 dark:text-indigo-200 text-[11px] font-bold uppercase tracking-wide">
              STARTING
            </span>
          </div>
          <p className="text-xs text-gray-600 dark:text-slate-300 mt-1.5 truncate font-medium">
            <span>From: </span>
            <span className="text-gray-700 dark:text-slate-200">{firstStop}</span>
            <span className="mx-1 text-gray-500">▸</span>
            <span className="text-gray-700 dark:text-slate-200">{secondStop}</span>
            <span className="mx-1 text-gray-500">▸</span>
            <span className="text-gray-600 dark:text-slate-400">{remainingStopsCount} Other Stops</span>
          </p>
        </div>

        {/* Primo Certified Row (shown ONLY for Primo Certified Operators - Card 1 in Image 4) */}
        {isPrimo && (
          <div className="flex items-center justify-between gap-2 flex-wrap mb-3 px-0.5">
            {/* Left: Primo On Time Guarantee Logo */}
            <div className="inline-flex items-center gap-1.5">
              <div className="relative inline-flex items-center">
                <span className="text-base sm:text-lg font-black italic tracking-tight text-[#14265c] dark:text-amber-300">
                  Primo
                </span>
                <svg
                  className="w-5 h-5 text-[#e5b842] -ml-1 -mt-1"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                </svg>
              </div>
              <div className="leading-[1.05] text-[10px] font-bold text-[#14265c] dark:text-slate-200">
                <div>On Time</div>
                <div>Guarantee</div>
              </div>
            </div>

            {/* Right: Promo Badges */}
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-gradient-to-r from-[#fde8a8] to-[#f8d377] text-slate-900 text-[11px] font-medium shadow-2xs">
                Exclusive <strong className="font-extrabold ml-1">₹100 OFF</strong>
              </span>
              <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-[#efe8fc] dark:bg-purple-950/70 text-slate-900 dark:text-purple-200 text-[11px] font-medium shadow-2xs">
                Group <strong className="font-extrabold ml-1">15% OFF</strong>
              </span>
            </div>
          </div>
        )}

        {/* Main Content: Left & Right Columns */}
        <div className="flex items-start justify-between gap-4">
          {/* Left Column */}
          <div className="flex-1 min-w-0">
            {/* Departure — Arrival */}
            <div className="flex items-center space-x-2">
              <span className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
                {route.departureTime?.slice(0, 5)}
              </span>
              <span className="text-gray-400 font-bold">—</span>
              <span className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
                {route.arrivalTime?.slice(0, 5)}
              </span>
            </div>

            {/* Duration • Seats left */}
            <p className="text-xs sm:text-sm text-gray-500 dark:text-slate-400 mt-0.5">
              {formatTripDuration(route.departureTime, route.arrivalTime, route.durationHours)} • {route.availableSeats} Seats
              {(() => {
                const isSingleLayout = route.busType?.toLowerCase().includes("2+1") || route.busType?.toLowerCase().includes("1+2") || route.busType?.toLowerCase().includes("1+1");
                const singleCount = isSingleLayout ? ((route as any).availableSingleSeats ?? 0) : 0;
                return singleCount > 0 ? (
                  <span className="text-amber-700 dark:text-amber-500 font-medium"> ({singleCount} Single)</span>
                ) : null;
              })()}
            </p>

            {/* Operator Name */}
            <h3
              onClick={() => openDetailsWithTab("highlights")}
              className="font-bold text-gray-900 dark:text-white text-base sm:text-lg mt-2.5 hover:text-[#d84e55] cursor-pointer transition-colors truncate"
              title="Click to view bus route and features"
            >
              {route.operatorName}
            </h3>

            {/* Bus Type */}
            <p className="text-xs sm:text-sm text-gray-500 dark:text-slate-400 mt-0.5 truncate">
              {route.busType}
            </p>

            {/* Key Amenity with Checkmark */}
            <div className="flex items-center space-x-1.5 text-xs sm:text-sm text-gray-700 dark:text-slate-300 mt-1.5">
              <span className="text-blue-600 font-bold">✓</span>
              <span>{primaryAmenity}</span>
            </div>
          </div>

          {/* Right Column */}
          <div className="flex flex-col items-end shrink-0">
            {/* Price & Onwards */}
            <div className="text-right">
              <p className="text-2xl sm:text-3xl font-black text-gray-900 dark:text-white leading-none">
                ₹{route.basePrice.toFixed(0)}
              </p>
              <p className="text-xs text-gray-400 dark:text-slate-500 mt-1">
                Onwards
              </p>
            </div>

            {/* Two-tier Stacked Rating Card */}
            <div
              onClick={() => openDetailsWithTab("reviews")}
              className="mt-2.5 flex flex-col items-center justify-center rounded-lg overflow-hidden cursor-pointer shadow-2xs border border-emerald-600/30 w-14 shrink-0 transition-transform active:scale-95"
              title="Click to view passenger ratings and reviews"
            >
              <div className="bg-[#0f8a43] text-white w-full py-1 px-1.5 flex items-center justify-center space-x-1 text-xs font-black">
                <Star className="w-3 h-3 fill-current" />
                <span>{displayRating}</span>
              </div>
              <div className="bg-[#e8f5e9] dark:bg-emerald-950/60 text-[#0f8a43] dark:text-emerald-300 w-full py-0.5 px-1 flex items-center justify-center text-[11px] font-bold border-t border-[#c8e6c9] dark:border-emerald-800/40">
                {totalRatingsCount}
              </div>
            </div>

            {/* View Seats Button directly below Ratings */}
            <button
              type="button"
              onClick={() => openDetailsWithTab("cancellation")}
              className="mt-2.5 px-3.5 py-1.5 sm:px-4 sm:py-2 rounded-xl text-xs font-bold transition-all flex items-center space-x-1 shadow-2xs bg-[#d84e55] text-white hover:bg-[#b83e44] active:scale-95 cursor-pointer"
            >
              <span>View Seats</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Previously booked by you Pill Badge at bottom */}
        {isPreviouslyBooked && (
          <div className="mt-3 pt-2.5 border-t border-gray-100 dark:border-slate-800">
            <span className="inline-flex items-center space-x-1.5 px-2.5 py-1 rounded-md bg-[#e8f5e9] dark:bg-emerald-950/50 text-[#15803d] dark:text-emerald-300 text-xs font-bold">
              <Star className="w-3 h-3 fill-current" />
              <span>Previously booked by you</span>
            </span>
          </div>
        )}
      </div>

      {/* Official Redbus Seat Selection & Route Details Pop-Up Modal */}
      <BusDetailsModal
        route={route}
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        initialTab={initialTab}
      />
    </>
  );
}
