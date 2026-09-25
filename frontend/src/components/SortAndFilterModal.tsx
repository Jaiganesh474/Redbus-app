"use client";

import React, { useState, useMemo, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  X,
  Search,
  Sparkles,
  Sun,
  Sunset,
  Moon,
  Sunrise,
  Check,
  Star,
  ShieldCheck,
  BedDouble,
  Armchair,
  Wifi,
  Zap,
  Coffee,
  Clock,
  SlidersHorizontal,
} from "lucide-react";
import type { RouteItem } from "@/types";

interface SortAndFilterModalProps {
  isOpen: boolean;
  onClose: () => void;
  routes: RouteItem[];
  // Active Filter states
  activeSort: "ratings" | "departure" | "price" | "price_desc" | "departure_desc";
  setActiveSort: (sort: "ratings" | "departure" | "price" | "price_desc" | "departure_desc") => void;
  selectedDepartureWindows: string[];
  setSelectedDepartureWindows: React.Dispatch<React.SetStateAction<string[]>>;
  selectedBusTypes: string[];
  setSelectedBusTypes: React.Dispatch<React.SetStateAction<string[]>>;
  selectedBoardingPoints: string[];
  setSelectedBoardingPoints: React.Dispatch<React.SetStateAction<string[]>>;
  selectedDroppingPoints: string[];
  setSelectedDroppingPoints: React.Dispatch<React.SetStateAction<string[]>>;
  selectedOperators: string[];
  setSelectedOperators: React.Dispatch<React.SetStateAction<string[]>>;
  selectedAmenities: string[];
  setSelectedAmenities: React.Dispatch<React.SetStateAction<string[]>>;
  isSingleSeatFilter: boolean;
  setIsSingleSeatFilter: (val: boolean) => void;
  isPrimoFilter: boolean;
  setIsPrimoFilter: (val: boolean) => void;
  isFreeCancellationFilter: boolean;
  setIsFreeCancellationFilter: (val: boolean) => void;
  isNonAcFilter: boolean;
  setIsNonAcFilter: (val: boolean) => void;
  aiFilterQuery: string;
  setAiFilterQuery: (val: string) => void;
  onClearAll: () => void;
}

type TabKey =
  | "ai_smart_filter"
  | "sort_by"
  | "departure_time"
  | "bus_type"
  | "single_window"
  | "boarding_points"
  | "dropping_points"
  | "bus_operator"
  | "amenities"
  | "bus_features"
  | "special_features"
  | "arrival_time"
  | "rtc_type";

const TABS: { id: TabKey; label: string }[] = [
  { id: "ai_smart_filter", label: "AI Smart Filter" },
  { id: "sort_by", label: "Sort by" },
  { id: "departure_time", label: "Departure time from source" },
  { id: "bus_type", label: "Bus type" },
  { id: "single_window", label: "Single window seater/sleeper" },
  { id: "boarding_points", label: "Boarding points" },
  { id: "dropping_points", label: "Dropping points" },
  { id: "bus_operator", label: "Bus operator" },
  { id: "amenities", label: "Amenities" },
  { id: "bus_features", label: "Bus features" },
  { id: "special_features", label: "Special bus features" },
  { id: "arrival_time", label: "Arrival time at destination" },
  { id: "rtc_type", label: "RTC bus type" },
];

export default function SortAndFilterModal({
  isOpen,
  onClose,
  routes,
  activeSort,
  setActiveSort,
  selectedDepartureWindows,
  setSelectedDepartureWindows,
  selectedBusTypes,
  setSelectedBusTypes,
  selectedBoardingPoints,
  setSelectedBoardingPoints,
  selectedDroppingPoints,
  setSelectedDroppingPoints,
  selectedOperators,
  setSelectedOperators,
  selectedAmenities,
  setSelectedAmenities,
  isSingleSeatFilter,
  setIsSingleSeatFilter,
  isPrimoFilter,
  setIsPrimoFilter,
  isFreeCancellationFilter,
  setIsFreeCancellationFilter,
  isNonAcFilter,
  setIsNonAcFilter,
  aiFilterQuery,
  setAiFilterQuery,
  onClearAll,
}: SortAndFilterModalProps) {
  const [activeTab, setActiveTab] = useState<TabKey>("departure_time");
  const [boardingSearch, setBoardingSearch] = useState("");
  const [droppingSearch, setDroppingSearch] = useState("");
  const [operatorSearch, setOperatorSearch] = useState("");

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

  // Calculate dynamic point lists & counts STRICTLY from database-fetched routes (no mocked data)
  const droppingPointsData = useMemo(() => {
    const map = new Map<string, number>();
    routes.forEach((r) => {
      (r.droppingPoints || []).forEach((pt) => {
        const clean = pt.replace(/\s*\([^)]*\)/g, "").trim();
        if (clean) map.set(clean, (map.get(clean) || 0) + 1);
      });
    });
    return Array.from(map.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
  }, [routes]);

  const boardingPointsData = useMemo(() => {
    const map = new Map<string, number>();
    routes.forEach((r) => {
      (r.boardingPoints || []).forEach((pt) => {
        const clean = pt.replace(/\s*\([^)]*\)/g, "").trim();
        if (clean) map.set(clean, (map.get(clean) || 0) + 1);
      });
    });
    return Array.from(map.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
  }, [routes]);

  const operatorsData = useMemo(() => {
    const map = new Map<string, number>();
    routes.forEach((r) => {
      const op = (r.operatorName || (r as any).bus?.operatorName || "").trim();
      if (op) {
        map.set(op, (map.get(op) || 0) + 1);
      }
    });
    return Array.from(map.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
  }, [routes]);

  // Departure windows counts strictly from real database routes
  const departureCounts = useMemo(() => {
    let morning = 0; // 06:00 - 12:00
    let afternoon = 0; // 12:00 - 18:00
    let evening = 0; // 18:00 - 24:00
    let night = 0; // 00:00 - 06:00

    routes.forEach((r) => {
      const hr = parseInt((r.departureTime || "00:00").split(":")[0], 10);
      if (hr >= 6 && hr < 12) morning++;
      else if (hr >= 12 && hr < 18) afternoon++;
      else if (hr >= 18 && hr < 24) evening++;
      else night++;
    });

    return { morning, afternoon, evening, night };
  }, [routes]);

  // Toggle helper for string arrays
  const toggleArrayItem = (list: string[], setList: React.Dispatch<React.SetStateAction<string[]>>, item: string) => {
    if (list.includes(item)) {
      setList(list.filter((x) => x !== item));
    } else {
      setList([...list, item]);
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-xs cursor-pointer"
          />

          {/* Main Modal Container matching Image 1 & Image 2 */}
          <motion.div
            initial={{ y: "100%", opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: "100%", opacity: 0 }}
            transition={{ type: "spring", damping: 28, stiffness: 300 }}
            className="relative z-10 w-full sm:max-w-2xl bg-white dark:bg-[#0f172a] rounded-t-3xl sm:rounded-3xl shadow-2xl overflow-hidden border border-gray-100 dark:border-slate-800 flex flex-col max-h-[90vh]"
          >
            {/* 1. Header: "Sort and filter buses" + Circular Close Button */}
            <div className="px-5 py-4 border-b border-gray-100 dark:border-slate-800 flex items-center justify-between bg-white dark:bg-[#0f172a] shrink-0">
              <h3 className="font-extrabold text-base sm:text-lg text-gray-900 dark:text-white">
                Sort and filter buses
              </h3>
              <button
                type="button"
                onClick={onClose}
                className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 dark:bg-slate-800 text-gray-700 dark:text-slate-300 flex items-center justify-center transition-colors cursor-pointer active:scale-95"
                title="Close"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* 2. Two-Column Body */}
            <div className="flex flex-1 min-h-0 h-[440px] sm:h-[480px]">
              {/* Left Column: Categories List */}
              <div className="w-[42%] sm:w-[38%] bg-gray-50 dark:bg-slate-900/60 border-r border-gray-100 dark:border-slate-800 overflow-y-auto py-1">
                {TABS.map((tab) => {
                  const isActive = activeTab === tab.id;
                  return (
                    <button
                      key={tab.id}
                      type="button"
                      onClick={() => setActiveTab(tab.id)}
                      className={`w-full text-left px-3 sm:px-4 py-3 text-xs leading-snug transition-all flex items-center justify-between cursor-pointer ${
                        isActive
                          ? "border-l-4 border-[#d84e55] bg-white dark:bg-[#0f172a] text-[#d84e55] font-bold shadow-2xs"
                          : "border-l-4 border-transparent text-gray-700 dark:text-slate-300 font-medium hover:bg-gray-100 dark:hover:bg-slate-800/50"
                      }`}
                    >
                      <span className="truncate">{tab.label}</span>
                    </button>
                  );
                })}
              </div>

              {/* Right Column: Dynamic Content based on selected category */}
              <div className="flex-1 bg-white dark:bg-[#0f172a] p-4 overflow-y-auto">
                <AnimatePresence mode="wait">
                  {/* Category 1: Dropping Points (Matching Image 1) */}
                  {activeTab === "dropping_points" && (
                    <motion.div
                      key="dropping"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-3"
                    >
                      {/* Search Bar matching Image 1 */}
                      <div className="relative flex items-center bg-gray-100 dark:bg-slate-800 rounded-xl px-3 py-2">
                        <Search className="w-3.5 h-3.5 text-gray-400 mr-2 shrink-0" />
                        <input
                          type="text"
                          value={droppingSearch}
                          onChange={(e) => setDroppingSearch(e.target.value)}
                          placeholder="Search dropping point"
                          className="w-full bg-transparent text-xs font-semibold text-gray-900 dark:text-white placeholder-gray-400 focus:outline-hidden"
                        />
                        {droppingSearch && (
                          <button
                            type="button"
                            onClick={() => setDroppingSearch("")}
                            className="text-gray-400 hover:text-gray-600 text-xs font-bold"
                          >
                            ×
                          </button>
                        )}
                      </div>

                      {/* Items List */}
                      <div className="space-y-1 divide-y divide-gray-50 dark:divide-slate-800/40">
                        {droppingPointsData.filter((p) => p.name.toLowerCase().includes(droppingSearch.toLowerCase())).length === 0 ? (
                          <div className="py-8 text-center text-xs text-gray-500 dark:text-slate-400">
                            {droppingSearch ? `No dropping points matching "${droppingSearch}"` : "No dropping points found for this route"}
                          </div>
                        ) : (
                          droppingPointsData
                            .filter((p) =>
                              p.name.toLowerCase().includes(droppingSearch.toLowerCase())
                            )
                            .map((p) => {
                              const checked = selectedDroppingPoints.includes(p.name);
                              return (
                                <label
                                  key={p.name}
                                  className="flex items-center justify-between py-2.5 px-1 cursor-pointer hover:bg-gray-50 dark:hover:bg-slate-800/30 rounded-lg select-none"
                                >
                                  <span className="text-xs font-semibold text-gray-900 dark:text-white">
                                    {p.name}
                                  </span>
                                  <div className="flex items-center space-x-3">
                                    <span className="text-[11px] text-gray-400 dark:text-slate-400 font-medium">
                                      {p.count}
                                    </span>
                                    <input
                                      type="checkbox"
                                      checked={checked}
                                      onChange={() =>
                                        toggleArrayItem(
                                          selectedDroppingPoints,
                                          setSelectedDroppingPoints,
                                          p.name
                                        )
                                      }
                                      className="w-4 h-4 rounded border-gray-300 dark:border-slate-600 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                                    />
                                  </div>
                                </label>
                              );
                            })
                        )}
                      </div>
                    </motion.div>
                  )}

                  {/* Category 2: Departure Time from Source (Matching Image 2) */}
                  {activeTab === "departure_time" && (
                    <motion.div
                      key="departure"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-2 divide-y divide-gray-50 dark:divide-slate-800/40"
                    >
                      {[
                        {
                          id: "12PM_6PM",
                          label: "12:00-18:00",
                          sub: "Afternoon",
                          icon: <Sun className="w-5 h-5 text-amber-500" />,
                          count: departureCounts.afternoon,
                        },
                        {
                          id: "18PM_24PM",
                          label: "18:00-24:00",
                          sub: "Evening",
                          icon: <Sunset className="w-5 h-5 text-orange-500" />,
                          count: departureCounts.evening,
                        },
                        {
                          id: "00AM_06AM",
                          label: "00:00-06:00",
                          sub: "Night",
                          icon: <Moon className="w-5 h-5 text-indigo-500" />,
                          count: departureCounts.night,
                        },
                        {
                          id: "06AM_12PM",
                          label: "06:00-12:00",
                          sub: "Morning",
                          icon: <Sunrise className="w-5 h-5 text-amber-400" />,
                          count: departureCounts.morning,
                        },
                      ].map((slot) => {
                        const checked = selectedDepartureWindows.includes(slot.id);
                        return (
                          <label
                            key={slot.id}
                            className="flex items-center justify-between py-3 px-1 cursor-pointer hover:bg-gray-50 dark:hover:bg-slate-800/30 rounded-lg select-none"
                          >
                            <div className="flex items-center space-x-3">
                              <div className="shrink-0">{slot.icon}</div>
                              <div>
                                <p className="text-xs font-bold text-gray-900 dark:text-white">
                                  {slot.label}
                                </p>
                                <p className="text-[10px] text-gray-500 dark:text-slate-400 font-medium">
                                  {slot.sub}
                                </p>
                              </div>
                            </div>
                            <div className="flex items-center space-x-3">
                              <span className="text-[11px] text-gray-400 dark:text-slate-400 font-medium">
                                {slot.count}
                              </span>
                              <input
                                type="checkbox"
                                checked={checked}
                                onChange={() =>
                                  toggleArrayItem(
                                    selectedDepartureWindows,
                                    setSelectedDepartureWindows,
                                    slot.id
                                  )
                                }
                                className="w-4 h-4 rounded border-gray-300 dark:border-slate-600 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                              />
                            </div>
                          </label>
                        );
                      })}
                    </motion.div>
                  )}

                  {/* Category 3: Boarding points */}
                  {activeTab === "boarding_points" && (
                    <motion.div
                      key="boarding"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-3"
                    >
                      <div className="relative flex items-center bg-gray-100 dark:bg-slate-800 rounded-xl px-3 py-2">
                        <Search className="w-3.5 h-3.5 text-gray-400 mr-2 shrink-0" />
                        <input
                          type="text"
                          value={boardingSearch}
                          onChange={(e) => setBoardingSearch(e.target.value)}
                          placeholder="Search boarding point"
                          className="w-full bg-transparent text-xs font-semibold text-gray-900 dark:text-white placeholder-gray-400 focus:outline-hidden"
                        />
                      </div>
                      <div className="space-y-1 divide-y divide-gray-50 dark:divide-slate-800/40">
                        {boardingPointsData.filter((p) => p.name.toLowerCase().includes(boardingSearch.toLowerCase())).length === 0 ? (
                          <div className="py-8 text-center text-xs text-gray-500 dark:text-slate-400">
                            {boardingSearch ? `No boarding points matching "${boardingSearch}"` : "No boarding points found for this route"}
                          </div>
                        ) : (
                          boardingPointsData
                            .filter((p) =>
                              p.name.toLowerCase().includes(boardingSearch.toLowerCase())
                            )
                            .map((p) => {
                              const checked = selectedBoardingPoints.includes(p.name);
                              return (
                                <label
                                  key={p.name}
                                  className="flex items-center justify-between py-2.5 px-1 cursor-pointer hover:bg-gray-50 dark:hover:bg-slate-800/30 rounded-lg select-none"
                                >
                                  <span className="text-xs font-semibold text-gray-900 dark:text-white">
                                    {p.name}
                                  </span>
                                  <div className="flex items-center space-x-3">
                                    <span className="text-[11px] text-gray-400 dark:text-slate-400 font-medium">
                                      {p.count}
                                    </span>
                                    <input
                                      type="checkbox"
                                      checked={checked}
                                      onChange={() =>
                                        toggleArrayItem(
                                          selectedBoardingPoints,
                                          setSelectedBoardingPoints,
                                          p.name
                                        )
                                      }
                                      className="w-4 h-4 rounded border-gray-300 dark:border-slate-600 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                                    />
                                  </div>
                                </label>
                              );
                            })
                        )}
                      </div>
                    </motion.div>
                  )}

                  {/* Category 4: Sort by */}
                  {activeTab === "sort_by" && (
                    <motion.div
                      key="sort"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-2"
                    >
                      {[
                        { id: "ratings", label: "Customer Ratings (High to Low)" },
                        { id: "departure", label: "Departure Time (Early First)" },
                        { id: "departure_desc", label: "Departure Time (Late First)" },
                        { id: "price", label: "Price (Low to High)" },
                        { id: "price_desc", label: "Price (High to Low)" },
                      ].map((s) => (
                        <button
                          key={s.id}
                          type="button"
                          onClick={() => setActiveSort(s.id as any)}
                          className={`w-full text-left p-3 rounded-xl border text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                            activeSort === s.id
                              ? "bg-red-50 dark:bg-red-950/40 text-[#d84e55] border-[#d84e55]"
                              : "bg-gray-50 dark:bg-slate-800/60 border-gray-200 dark:border-slate-700 text-gray-700 dark:text-slate-300"
                          }`}
                        >
                          <span>{s.label}</span>
                          {activeSort === s.id && <Check className="w-4 h-4 text-[#d84e55]" />}
                        </button>
                      ))}
                    </motion.div>
                  )}

                  {/* Category 5: Bus type */}
                  {activeTab === "bus_type" && (
                    <motion.div
                      key="bus_type"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-2"
                    >
                      {[
                        { id: "AC", label: "AC Buses" },
                        { id: "NON_AC", label: "Non-AC Buses" },
                        { id: "SLEEPER", label: "Sleeper" },
                        { id: "SEATER", label: "Seater" },
                      ].map((item) => {
                        const checked = selectedBusTypes.includes(item.id);
                        return (
                          <label
                            key={item.id}
                            className="flex items-center justify-between py-2.5 px-1 cursor-pointer hover:bg-gray-50 dark:hover:bg-slate-800/30 rounded-lg select-none"
                          >
                            <span className="text-xs font-semibold text-gray-900 dark:text-white">
                              {item.label}
                            </span>
                            <input
                              type="checkbox"
                              checked={checked}
                              onChange={() =>
                                toggleArrayItem(selectedBusTypes, setSelectedBusTypes, item.id)
                              }
                              className="w-4 h-4 rounded border-gray-300 dark:border-slate-600 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                            />
                          </label>
                        );
                      })}
                    </motion.div>
                  )}

                  {/* Category 6: Single window seater/sleeper */}
                  {activeTab === "single_window" && (
                    <motion.div
                      key="single"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-3"
                    >
                      <label className="flex items-center justify-between p-3 rounded-xl border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800/50 cursor-pointer">
                        <div>
                          <p className="text-xs font-bold text-gray-900 dark:text-white">
                            Single Window Seats Only
                          </p>
                          <p className="text-[10px] text-gray-500 dark:text-slate-400">
                            Solo seats without adjacent passenger
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={isSingleSeatFilter}
                          onChange={(e) => setIsSingleSeatFilter(e.target.checked)}
                          className="w-4 h-4 rounded border-gray-300 dark:border-slate-600 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                        />
                      </label>
                    </motion.div>
                  )}

                  {/* Category 7: Bus operator */}
                  {activeTab === "bus_operator" && (
                    <motion.div
                      key="operator"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-3"
                    >
                      <div className="relative flex items-center bg-gray-100 dark:bg-slate-800 rounded-xl px-3 py-2">
                        <Search className="w-3.5 h-3.5 text-gray-400 mr-2 shrink-0" />
                        <input
                          type="text"
                          value={operatorSearch}
                          onChange={(e) => setOperatorSearch(e.target.value)}
                          placeholder="Search bus operator"
                          className="w-full bg-transparent text-xs font-semibold text-gray-900 dark:text-white placeholder-gray-400 focus:outline-hidden"
                        />
                      </div>
                      <div className="space-y-1 divide-y divide-gray-50 dark:divide-slate-800/40">
                        {operatorsData.filter((p) => p.name.toLowerCase().includes(operatorSearch.toLowerCase())).length === 0 ? (
                          <div className="py-8 text-center text-xs text-gray-500 dark:text-slate-400">
                            {operatorSearch ? `No operators matching "${operatorSearch}"` : "No bus operators found for this route"}
                          </div>
                        ) : (
                          operatorsData
                            .filter((p) =>
                              p.name.toLowerCase().includes(operatorSearch.toLowerCase())
                            )
                            .map((p) => {
                              const checked = selectedOperators.includes(p.name);
                              return (
                                <label
                                  key={p.name}
                                  className="flex items-center justify-between py-2.5 px-1 cursor-pointer hover:bg-gray-50 dark:hover:bg-slate-800/30 rounded-lg select-none"
                                >
                                  <span className="text-xs font-semibold text-gray-900 dark:text-white">
                                    {p.name}
                                  </span>
                                  <div className="flex items-center space-x-3">
                                    <span className="text-[11px] text-gray-400 dark:text-slate-400 font-medium">
                                      {p.count}
                                    </span>
                                    <input
                                      type="checkbox"
                                      checked={checked}
                                      onChange={() =>
                                        toggleArrayItem(
                                          selectedOperators,
                                          setSelectedOperators,
                                          p.name
                                        )
                                      }
                                      className="w-4 h-4 rounded border-gray-300 dark:border-slate-600 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                                    />
                                  </div>
                                </label>
                              );
                            })
                        )}
                      </div>
                    </motion.div>
                  )}

                  {/* Category 8: Amenities */}
                  {activeTab === "amenities" && (
                    <motion.div
                      key="amenities"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-2"
                    >
                      {[
                        "WiFi",
                        "Charging Point",
                        "Water Bottle",
                        "Blanket",
                        "Live Tracking",
                        "Snacks",
                        "Emergency Exit",
                        "CCTV",
                      ].map((item) => {
                        const checked = selectedAmenities.includes(item);
                        return (
                          <label
                            key={item}
                            className="flex items-center justify-between py-2 px-1 cursor-pointer hover:bg-gray-50 dark:hover:bg-slate-800/30 rounded-lg select-none"
                          >
                            <span className="text-xs font-semibold text-gray-900 dark:text-white">
                              {item}
                            </span>
                            <input
                              type="checkbox"
                              checked={checked}
                              onChange={() =>
                                toggleArrayItem(selectedAmenities, setSelectedAmenities, item)
                              }
                              className="w-4 h-4 rounded border-gray-300 dark:border-slate-600 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                            />
                          </label>
                        );
                      })}
                    </motion.div>
                  )}

                  {/* Category 9: AI Smart Filter Tab */}
                  {activeTab === "ai_smart_filter" && (
                    <motion.div
                      key="ai"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-3"
                    >
                      <div className="p-3 bg-purple-50 dark:bg-purple-950/40 rounded-2xl border border-purple-200 dark:border-purple-800/60 space-y-2">
                        <div className="flex items-center space-x-1.5 text-purple-700 dark:text-purple-300 font-extrabold text-xs">
                          <Sparkles className="w-4 h-4" />
                          <span>Natural Language Filter</span>
                        </div>
                        <input
                          type="text"
                          value={aiFilterQuery}
                          onChange={(e) => setAiFilterQuery(e.target.value)}
                          placeholder="e.g. need bus between chennai and bengaluru under 1000"
                          className="w-full bg-white dark:bg-slate-900 border border-purple-300 dark:border-purple-700 rounded-xl px-3 py-2 text-xs text-gray-900 dark:text-white focus:outline-hidden"
                        />
                        <div className="flex flex-wrap gap-1.5 pt-1">
                          {[
                            "Morning bus",
                            "Sleeper under ₹1000",
                            "Primo bus",
                            "need bus between chennai and bengaluru",
                          ].map((chip) => (
                            <button
                              key={chip}
                              type="button"
                              onClick={() => setAiFilterQuery(chip)}
                              className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-white dark:bg-slate-900 border border-purple-200 dark:border-purple-700 text-purple-700 dark:text-purple-300 hover:bg-purple-100 cursor-pointer"
                            >
                              {chip}
                            </button>
                          ))}
                        </div>
                      </div>
                    </motion.div>
                  )}

                  {/* Other Categories fallback */}
                  {(activeTab === "bus_features" ||
                    activeTab === "special_features" ||
                    activeTab === "arrival_time" ||
                    activeTab === "rtc_type") && (
                    <motion.div
                      key="other"
                      initial={{ opacity: 0, x: 8 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -8 }}
                      transition={{ duration: 0.15 }}
                      className="space-y-2"
                    >
                      <label className="flex items-center justify-between py-2.5 px-1 cursor-pointer select-none">
                        <span className="text-xs font-semibold text-gray-900 dark:text-white">
                          Primo Verified Buses
                        </span>
                        <input
                          type="checkbox"
                          checked={isPrimoFilter}
                          onChange={(e) => setIsPrimoFilter(e.target.checked)}
                          className="w-4 h-4 rounded border-gray-300 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                        />
                      </label>
                      <label className="flex items-center justify-between py-2.5 px-1 cursor-pointer select-none">
                        <span className="text-xs font-semibold text-gray-900 dark:text-white">
                          Free Cancellation Policy
                        </span>
                        <input
                          type="checkbox"
                          checked={isFreeCancellationFilter}
                          onChange={(e) => setIsFreeCancellationFilter(e.target.checked)}
                          className="w-4 h-4 rounded border-gray-300 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                        />
                      </label>
                      <label className="flex items-center justify-between py-2.5 px-1 cursor-pointer select-none">
                        <span className="text-xs font-semibold text-gray-900 dark:text-white">
                          State RTC Government Buses
                        </span>
                        <input
                          type="checkbox"
                          checked={selectedOperators.includes("KSRTC")}
                          onChange={() =>
                            toggleArrayItem(selectedOperators, setSelectedOperators, "KSRTC")
                          }
                          className="w-4 h-4 rounded border-gray-300 text-[#d84e55] focus:ring-[#d84e55] cursor-pointer"
                        />
                      </label>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </div>

            {/* 3. Bottom Action Bar matching Image 1 & Image 2 */}
            <div className="p-4 border-t border-gray-100 dark:border-slate-800 bg-white dark:bg-[#0f172a] flex items-center space-x-3 shrink-0">
              <button
                type="button"
                onClick={onClearAll}
                className="flex-1 py-3 rounded-full border border-gray-400 dark:border-slate-600 text-xs sm:text-sm font-extrabold text-gray-800 dark:text-white hover:bg-gray-100 dark:hover:bg-slate-800 transition-all text-center cursor-pointer active:scale-95"
              >
                Clear all
              </button>
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-3 rounded-full bg-[#d84e55] hover:bg-red-600 text-xs sm:text-sm font-extrabold text-white shadow-md hover:shadow-lg transition-all text-center cursor-pointer active:scale-95"
              >
                Apply
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
