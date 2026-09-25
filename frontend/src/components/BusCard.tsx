"use client";

import React, { useState, useEffect } from "react";
import { useAppDispatch } from "@/store";
import { setSelectedRoute } from "@/store/bookingSlice";
import type { RouteItem } from "@/types";
import {
  Star,
  Clock,
  Wifi,
  Zap,
  Coffee,
  Shield,
  ChevronRight,
  Tv,
  Eye,
} from "lucide-react";
import BusDetailsModal from "./BusDetailsModal";

interface BusCardProps {
  route: RouteItem;
  autoOpen?: boolean;
}

export default function BusCard({ route, autoOpen = false }: BusCardProps) {
  const dispatch = useAppDispatch();
  const [showModal, setShowModal] = useState(false);
  const [initialTab, setInitialTab] = useState<string>("cancellation");

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

  const getAmenityIcon = (name: string) => {
    const lower = name.toLowerCase();
    if (lower.includes("wifi")) return <Wifi className="w-3.5 h-3.5" />;
    if (lower.includes("charging")) return <Zap className="w-3.5 h-3.5" />;
    if (lower.includes("water") || lower.includes("snack")) return <Coffee className="w-3.5 h-3.5" />;
    if (lower.includes("tracking") || lower.includes("cctv")) return <Shield className="w-3.5 h-3.5" />;
    return <Tv className="w-3.5 h-3.5" />;
  };

  return (
    <>
      <div className="bg-white dark:bg-slate-900 rounded-xl sm:rounded-2xl border border-gray-200 dark:border-slate-800 shadow-2xs hover:shadow-md transition-all mb-3 sm:mb-4 overflow-hidden group">
        <div className="p-3.5 sm:p-5 sm:p-6">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-3 sm:gap-4 items-center">
            {/* Operator & Bus Info */}
            <div className="md:col-span-4">
              <div className="flex items-start space-x-2.5 sm:space-x-3">
                {route.busPhotoUrl ? (
                  <div className="w-12 h-12 sm:w-16 sm:h-16 rounded-lg sm:rounded-xl overflow-hidden shrink-0 border border-gray-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-800">
                    <img
                      src={route.busPhotoUrl.startsWith("http") ? route.busPhotoUrl : `http://localhost:8080${route.busPhotoUrl}`}
                      alt={route.operatorName}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        (e.target as HTMLElement).style.display = "none";
                      }}
                    />
                  </div>
                ) : null}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center space-x-1.5 sm:space-x-2">
                    <h3
                      onClick={() => openDetailsWithTab("highlights")}
                      className="font-bold text-gray-900 dark:text-white text-sm sm:text-lg tracking-tight hover:text-[#d84e55] dark:hover:text-red-400 cursor-pointer transition-colors truncate"
                      title="Click to view bus route and features"
                    >
                      {route.operatorName}
                    </h3>
                    <div
                      onClick={() => openDetailsWithTab("reviews")}
                      className="flex items-center space-x-1 px-1.5 sm:px-2 py-0.5 bg-[#15803d] text-white rounded text-[10px] sm:text-xs font-bold shadow-2xs shrink-0 cursor-pointer hover:opacity-90"
                      title="Click to view passenger ratings and reviews"
                    >
                      <Star className="w-2.5 h-2.5 sm:w-3 sm:h-3 fill-current" />
                      <span>{route.rating.toFixed(1)}</span>
                    </div>
                  </div>
                  <p className="text-[11px] sm:text-xs font-medium text-gray-500 dark:text-slate-400 mt-0.5 sm:mt-1 truncate">
                    {route.busType}
                  </p>
                </div>
              </div>

              {/* Amenities pills */}
              <div className="flex flex-wrap gap-1 sm:gap-1.5 mt-2 sm:mt-3">
                {route.amenities?.slice(0, 4).map((amenity) => (
                  <span
                    key={amenity}
                    className="px-1.5 sm:px-2 py-0.5 bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-slate-300 rounded text-[9px] sm:text-[10px] font-medium flex items-center space-x-1"
                  >
                    {getAmenityIcon(amenity)}
                    <span>{amenity}</span>
                  </span>
                ))}
                {route.amenities && route.amenities.length > 4 && (
                  <button
                    onClick={() => openDetailsWithTab("features")}
                    className="px-1.5 py-0.5 bg-gray-100 dark:bg-slate-800 text-gray-500 dark:text-slate-400 hover:text-[#d84e55] rounded text-[9px] sm:text-[10px] font-medium cursor-pointer"
                  >
                    +{route.amenities.length - 4} more
                  </button>
                )}
              </div>
            </div>

            {/* Schedule & Duration */}
            <div className="md:col-span-5">
              <div className="flex items-center justify-between sm:justify-start sm:space-x-6">
                {/* Departure */}
                <div>
                  <p className="text-base sm:text-xl font-black text-gray-900 dark:text-white leading-tight">
                    {route.departureTime}
                  </p>
                  <p className="text-[10px] sm:text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase mt-0.5 truncate max-w-[90px] sm:max-w-none">
                    {route.sourceCity}
                  </p>
                </div>

                {/* Journey Duration Line */}
                <div className="flex flex-col items-center px-1 sm:px-2">
                  <span className="text-[10px] sm:text-[11px] font-bold text-gray-400 dark:text-slate-500">
                    {route.durationHours} hrs
                  </span>
                  <div className="w-14 sm:w-24 h-0.5 bg-gray-300 dark:bg-slate-700 relative my-1">
                    <div className="w-1.5 h-1.5 rounded-full bg-gray-400 absolute left-0 -top-0.5" />
                    <div className="w-1.5 h-1.5 rounded-full bg-[#d84e55] absolute right-0 -top-0.5" />
                  </div>
                  <span className="text-[8px] sm:text-[9px] text-gray-400 dark:text-slate-500 uppercase tracking-wider">
                    Direct
                  </span>
                </div>

                {/* Arrival */}
                <div>
                  <p className="text-base sm:text-xl font-black text-gray-900 dark:text-white leading-tight">
                    {route.arrivalTime}
                  </p>
                  <p className="text-[10px] sm:text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase mt-0.5 truncate max-w-[90px] sm:max-w-none">
                    {route.destinationCity}
                  </p>
                </div>
              </div>

              {/* Quick links to details tabs */}
              <div className="flex items-center space-x-2 sm:space-x-3 mt-2 sm:mt-3 text-[10px] sm:text-[11px] text-gray-500 dark:text-slate-400 font-semibold">
                <button
                  onClick={() => openDetailsWithTab("boarding")}
                  className="hover:text-[#d84e55] transition-colors cursor-pointer"
                >
                  Boarding Points
                </button>
                <span>•</span>
                <button
                  onClick={() => openDetailsWithTab("dropping")}
                  className="hover:text-[#d84e55] transition-colors cursor-pointer"
                >
                  Dropping Points
                </button>
                <span>•</span>
                <button
                  onClick={() => openDetailsWithTab("cancellation")}
                  className="hover:text-[#d84e55] transition-colors cursor-pointer"
                >
                  Policy
                </button>
              </div>
            </div>

            {/* Price & Action */}
            <div className="md:col-span-3 flex md:flex-col items-center md:items-end justify-between pt-2.5 md:pt-0 border-t md:border-t-0 border-gray-100 dark:border-slate-800">
              <div className="text-left md:text-right">
                <span className="text-[10px] sm:text-[11px] text-gray-400 font-medium">Starts from</span>
                <p className="text-xl sm:text-2xl font-black text-gray-900 dark:text-white leading-none">
                  ₹{route.basePrice.toFixed(0)}
                </p>
                <p className="text-[10px] sm:text-xs text-emerald-600 dark:text-emerald-400 font-bold mt-0.5 sm:mt-1">
                  {route.availableSeats} seats left
                </p>
              </div>

              <button
                type="button"
                onClick={() => openDetailsWithTab("cancellation")}
                className="mt-1 sm:mt-2 px-4 sm:px-5 py-2 sm:py-2.5 rounded-xl text-xs font-bold transition-all flex items-center space-x-1.5 shadow-2xs bg-[#d84e55] text-white hover:bg-[#b83e44] shadow-red-500/20 active:scale-95 cursor-pointer"
              >
                <span>View Seats</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
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
