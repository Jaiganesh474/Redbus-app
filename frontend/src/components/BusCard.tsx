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

  return (
    <>
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-gray-200 dark:border-slate-800 shadow-2xs hover:shadow-md transition-all mb-3.5 p-4 sm:p-5">
        {/* Direct Bus Pill Badge */}
        <div className="mb-2">
          <span className="inline-block px-2.5 py-0.5 rounded-md bg-[#eef2ff] dark:bg-indigo-950/60 text-[#4338ca] dark:text-indigo-300 text-xs font-semibold">
            Direct Bus
          </span>
        </div>

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
              {route.availableSeats > 0 ? (
                <span className="text-amber-700 dark:text-amber-500 font-medium"> (1 Single)</span>
              ) : null}
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
