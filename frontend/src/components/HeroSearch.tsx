"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useAppDispatch, useAppSelector } from "@/store";
import { setSearchParams, swapCities } from "@/store/searchSlice";
import { useParseNlpQueryMutation, useGetAvailableCitiesQuery } from "@/store/apiSlice";
import AiCityDropdown from "./AiCityDropdown";
import SelectDateModal from "./SelectDateModal";
import { formatLocalDate, getTodayDateStr, getTomorrowDateStr, getOffsetDateStr, formatShortDateDisplay } from "@/lib/dateUtils";
import {
  MapPin,
  ArrowRightLeft,
  Calendar,
  Search,
  Sparkles,
  ArrowRight,
  Bus,
  User,
  X,
} from "lucide-react";

export default function HeroSearch() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const searchState = useAppSelector((state) => state.search);

  const [source, setSource] = useState(searchState.sourceCity || "");
  const [destination, setDestination] = useState(searchState.destinationCity || "");
  const [date, setDate] = useState(searchState.travelDate || getTodayDateStr());
  const [showDateModal, setShowDateModal] = useState(false);
  const [showWomenModal, setShowWomenModal] = useState(false);
  const [swapRotation, setSwapRotation] = useState(0);
  const [nlpQuery, setNlpQuery] = useState("");
  const [isNlpLoading, setIsNlpLoading] = useState(false);
  const [bookingForWomen, setBookingForWomen] = useState(false);
  const [womenToast, setWomenToast] = useState<string | null>(null);

  const toggleBookingForWomen = () => {
    const nextState = !bookingForWomen;
    setBookingForWomen(nextState);
    const msg = nextState ? "Booking for women enabled" : "Booking for women disabled";
    setWomenToast(msg);
    setTimeout(() => {
      setWomenToast(null);
    }, 2500);
  };

  const formatHeroDate = (dateStr: string) => {
    if (!dateStr) return { dateFormatted: "", dayTag: "" };
    const parts = dateStr.split("-");
    if (parts.length < 3) return { dateFormatted: dateStr, dayTag: "" };
    const [y, m, d] = parts.map(Number);
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const dayStr = String(d).padStart(2, "0");
    const monthStr = months[(m || 1) - 1] || "";
    const dateFormatted = `${dayStr} ${monthStr}, ${y}`;
    const dayTag = dateStr === getTodayDateStr() ? "(Today)" : dateStr === getTomorrowDateStr() ? "(Tomorrow)" : "";
    return { dateFormatted, dayTag };
  };

  const { data: availableCities = ["Bangalore", "Chennai", "Mumbai", "Pune", "Delhi", "Jaipur", "Hyderabad", "Coimbatore"] } = useGetAvailableCitiesQuery();
  const [parseNlpMutation] = useParseNlpQueryMutation();

  const handleSwap = () => {
    setSwapRotation((prev) => prev + 180);
    const temp = source;
    setSource(destination);
    setDestination(temp);
    dispatch(swapCities());
  };

  const handleSearch = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    dispatch(
      setSearchParams({
        sourceCity: source,
        destinationCity: destination,
        travelDate: date,
      })
    );
    router.push(`/search?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(destination)}&date=${encodeURIComponent(date)}`);
  };

  const handleNlpSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nlpQuery.trim()) return;

    setIsNlpLoading(true);
    try {
      const res = await parseNlpMutation({ query: nlpQuery }).unwrap();
      if (res.sourceCity) setSource(res.sourceCity);
      if (res.destinationCity) setDestination(res.destinationCity);
      if (res.travelDate) setDate(res.travelDate);

      dispatch(
        setSearchParams({
          sourceCity: res.sourceCity || source,
          destinationCity: res.destinationCity || destination,
          travelDate: res.travelDate || date,
          busType: res.busType,
          maxPrice: res.maxPrice,
          timePreference: res.timePreference,
        })
      );

      const src = res.sourceCity || source;
      const dst = res.destinationCity || destination;
      const dt = res.travelDate || date;

      router.push(`/search?source=${encodeURIComponent(src)}&destination=${encodeURIComponent(dst)}&date=${encodeURIComponent(dt)}`);
    } catch {
      // Fallback
    } finally {
      setIsNlpLoading(false);
    }
  };

  const setQuickDate = (offsetDays: number) => {
    const dateStr = getOffsetDateStr(offsetDays);
    setDate(dateStr);
  };

  return (
    <div className="relative bg-gradient-to-r from-[#d84e55] via-red-600 to-rose-600 overflow-hidden">
      {/* Decorative SVG background shapes */}
      <div className="absolute inset-0 opacity-10 pointer-events-none">
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/5 rounded-full blur-3xl transform translate-x-1/2 -translate-y-1/2" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-black/10 rounded-full blur-2xl transform -translate-x-1/3 translate-y-1/3" />
      </div>

      <div className="px-3.5 py-6 sm:px-6 sm:py-12 md:py-16 text-white max-w-5xl mx-auto">
        {/* Title */}
        <div className="text-center max-w-2xl mx-auto mb-4 sm:mb-8">
          <div className="inline-flex items-center space-x-1.5 sm:space-x-2 px-2.5 sm:px-3 py-0.5 sm:py-1 bg-white/10 backdrop-blur-md rounded-full text-[10px] sm:text-xs font-semibold tracking-wide uppercase mb-2 sm:mb-3 border border-white/15">
            <Sparkles className="w-3 h-3 sm:w-3.5 sm:h-3.5 text-amber-300" />
            <span>India&apos;s AI-Powered Bus Booking</span>
          </div>
          <h1 className="text-2xl sm:text-4xl md:text-5xl font-black tracking-tight text-white leading-tight">
            Book Bus Tickets with <span className="text-amber-300">Trust and Confidence</span>
          </h1>
          <p className="text-red-100 text-[11px] sm:text-base mt-1 sm:mt-2 font-normal">
            Choose from 30,000+ routes, live seat selection, and 24/7 AI trip assistance.
          </p>
        </div>

        {/* NLP Natural Language Search Pill */}
        <div className="max-w-3xl mx-auto mb-3.5 sm:mb-6">
          <form onSubmit={handleNlpSearch} className="relative">
            <div className="relative flex items-center">
              <div className="absolute left-3 sm:left-4 flex items-center pointer-events-none text-[#d84e55]">
                <Sparkles className="w-3.5 h-3.5 sm:w-5 sm:h-5 animate-pulse" />
              </div>
              <input
                type="text"
                value={nlpQuery}
                onChange={(e) => setNlpQuery(e.target.value)}
                placeholder="Ask AI: 'AC sleeper Bangalore to Chennai tomorrow'..."
                className="w-full pl-8 sm:pl-12 pr-24 sm:pr-36 py-2 sm:py-3.5 bg-white dark:bg-slate-900 text-gray-800 dark:text-white placeholder-gray-400 dark:placeholder-slate-400 rounded-xl sm:rounded-2xl text-[11px] sm:text-sm font-medium shadow-md sm:shadow-lg border border-red-100 dark:border-slate-800 focus:outline-hidden focus:ring-4 focus:ring-amber-300/50"
              />
              <button
                type="submit"
                disabled={isNlpLoading}
                className="absolute right-1 sm:right-2 px-2.5 sm:px-4 py-1.5 sm:py-2 bg-gradient-to-r from-[#d84e55] to-red-600 hover:from-[#b83e44] hover:to-red-700 text-white rounded-lg sm:rounded-xl text-[10px] sm:text-xs font-bold transition-all shadow-md flex items-center space-x-1 disabled:opacity-70 cursor-pointer"
              >
                {isNlpLoading ? (
                  <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    <span className="hidden sm:inline">Search with AI</span>
                    <span className="inline sm:hidden">AI Search</span>
                    <ArrowRight className="w-3 h-3 sm:w-3.5 sm:h-3.5" />
                  </>
                )}
              </button>
            </div>
          </form>
        </div>

        {/* Standard Search Card matching Image 1 */}
        <div className="relative bg-white dark:bg-slate-900 rounded-[28px] p-3 sm:p-4 pb-8 sm:pb-9 shadow-2xl border border-gray-100 dark:border-slate-800 max-w-5xl mx-auto mb-8 sm:mb-10 text-gray-800 dark:text-slate-100 transition-all">
          <form onSubmit={handleSearch} className="flex flex-col lg:flex-row items-stretch gap-2.5 sm:gap-3">
            {/* 1. Combined FROM & TO with Circular Swap Button in between */}
            <div className="flex-1 min-w-0 border border-gray-200 dark:border-slate-700 rounded-2xl bg-white dark:bg-slate-900 flex items-center divide-x divide-gray-200 dark:divide-slate-700 shadow-2xs relative group focus-within:border-gray-300 dark:focus-within:border-slate-600 transition-all">
              {/* From Input */}
              <div className="flex-1 min-w-0">
                <AiCityDropdown
                  label="From"
                  value={source}
                  onChange={setSource}
                  excludeCity={destination}
                  placeholder="Enter departure city"
                  variant="hero"
                  icon={
                    <svg className="w-5 h-5 text-gray-500 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="4" width="14" height="15" rx="2" />
                      <path d="M3 9h14" />
                      <circle cx="6.5" cy="16" r="1" fill="currentColor" />
                      <circle cx="13.5" cy="16" r="1" fill="currentColor" />
                      <path d="M19 12h3m-1.5-1.5l1.5 1.5-1.5 1.5" />
                    </svg>
                  }
                  required
                />
              </div>

              {/* Center Circular Swap Button */}
              <div className="relative flex items-center justify-center -mx-3.5 z-10 shrink-0">
                <button
                  type="button"
                  onClick={handleSwap}
                  title="Swap Cities"
                  className="w-7 h-7 sm:w-8 sm:h-8 rounded-full bg-[#3e3e3e] hover:bg-black text-white flex items-center justify-center cursor-pointer shadow-md transition-all duration-300 active:scale-90"
                  style={{ transform: `rotate(${swapRotation}deg)` }}
                >
                  <ArrowRightLeft className="w-3.5 h-3.5" />
                </button>
              </div>

              {/* To Input */}
              <div className="flex-1 min-w-0">
                <AiCityDropdown
                  label="To"
                  value={destination}
                  onChange={setDestination}
                  excludeCity={source}
                  placeholder="Enter destination city"
                  variant="hero"
                  icon={
                    <svg className="w-5 h-5 text-gray-500 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="7" y="4" width="14" height="15" rx="2" />
                      <path d="M7 9h14" />
                      <circle cx="10.5" cy="16" r="1" fill="currentColor" />
                      <circle cx="17.5" cy="16" r="1" fill="currentColor" />
                      <path d="M5 12H2m1.5-1.5L2 12l1.5 1.5" />
                    </svg>
                  }
                  required
                />
              </div>
            </div>

            {/* 2. Date of Journey Box with Today & Tomorrow Quick Pills */}
            <div
              onClick={() => setShowDateModal(true)}
              className="lg:w-[325px] border border-gray-200 dark:border-slate-700 rounded-2xl px-3.5 py-2 sm:py-2.5 flex items-center justify-between bg-white dark:bg-slate-900 cursor-pointer hover:border-gray-300 dark:hover:border-slate-600 transition-all shadow-2xs group"
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  setShowDateModal(true);
                }
              }}
            >
              <div className="flex items-center space-x-2.5 min-w-0">
                <Calendar className="w-5 h-5 text-gray-500 dark:text-gray-400 shrink-0" />
                <div className="flex flex-col min-w-0 leading-tight">
                  <span className="text-[10px] sm:text-[11px] text-gray-400 font-medium tracking-tight">
                    Date of Journey
                  </span>
                  <div className="flex items-center space-x-1 mt-0.5">
                    <span className="text-xs sm:text-sm font-black text-gray-900 dark:text-white truncate">
                      {formatHeroDate(date).dateFormatted}
                    </span>
                    {formatHeroDate(date).dayTag && (
                      <span className="text-[10px] sm:text-[11px] text-gray-400 font-normal shrink-0">
                        {formatHeroDate(date).dayTag}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex items-center space-x-1.5 shrink-0 pl-2" onClick={(e) => e.stopPropagation()}>
                <button
                  type="button"
                  onClick={() => setQuickDate(0)}
                  className={`px-3 py-1 rounded-full text-xs font-bold transition-all cursor-pointer ${
                    date === getTodayDateStr()
                      ? "bg-[#fee2e2] text-gray-900 font-black shadow-2xs"
                      : "bg-[#fee2e2]/70 hover:bg-[#fee2e2] text-gray-700"
                  }`}
                >
                  Today
                </button>
                <button
                  type="button"
                  onClick={() => setQuickDate(1)}
                  className={`px-3 py-1 rounded-full text-xs font-bold transition-all cursor-pointer ${
                    date === getTomorrowDateStr()
                      ? "bg-[#fee2e2] text-gray-900 font-black shadow-2xs"
                      : "bg-[#fee2e2]/70 hover:bg-[#fee2e2] text-gray-700"
                  }`}
                >
                  Tomorrow
                </button>
              </div>
            </div>

            {/* 3. Booking for Women Card */}
            <div className="lg:w-[235px] border border-gray-200 dark:border-slate-700 rounded-2xl px-3 sm:px-3.5 py-2 sm:py-2.5 flex items-center justify-between gap-2.5 bg-white dark:bg-slate-900 shadow-2xs">
              <div className="flex items-center space-x-2.5 min-w-0">
                {/* Female Avatar matching Image 1 */}
                <div className="w-8 h-8 rounded-full bg-pink-100 dark:bg-pink-950/60 border border-pink-200/80 flex items-center justify-center shrink-0 overflow-hidden shadow-2xs">
                  <svg className="w-6 h-6" viewBox="0 0 36 36" fill="none">
                    <circle cx="18" cy="13" r="6" fill="#F472B6" />
                    <path d="M12 14c0-4 2.8-7 6-7s6 3 6 7c0 2-1 4-2 5.5-1 1.5-2.5 3-4 3s-3-1.5-4-3c-1-1.5-2-3.5-2-5.5z" fill="#DB2777" />
                    <circle cx="18" cy="13" r="4.5" fill="#FDE047" />
                    <path d="M14 13c0 2.5 1.8 4 4 4s4-1.5 4-4" fill="#FBBF24" />
                    <path d="M11 29c0-4.5 3.1-8 7-8s7 3.5 7 8" fill="#F43F5E" />
                    <path d="M15 24l3 2.5 3-2.5" stroke="#FFF" strokeWidth="1.2" strokeLinecap="round" />
                  </svg>
                </div>

                <div className="flex flex-col min-w-0 leading-tight">
                  <span className="text-xs sm:text-[13px] font-black text-gray-900 dark:text-white whitespace-nowrap">
                    Booking for women
                  </span>
                  <button
                    type="button"
                    onClick={() => setShowWomenModal(true)}
                    className="text-[10px] sm:text-[11px] text-blue-600 dark:text-blue-400 font-semibold underline hover:text-blue-800 text-left cursor-pointer"
                  >
                    Know more
                  </button>
                </div>
              </div>

              {/* Pill Switch */}
              <button
                type="button"
                onClick={toggleBookingForWomen}
                role="switch"
                aria-checked={bookingForWomen}
                className={`w-11 h-6 flex items-center rounded-full p-0.5 transition-colors duration-200 ease-in-out cursor-pointer shrink-0 ${
                  bookingForWomen ? "bg-[#d84e55]" : "bg-gray-200 dark:bg-slate-700"
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full shadow-sm transform transition-transform duration-200 ease-in-out ${
                    bookingForWomen
                      ? "translate-x-5 bg-white"
                      : "translate-x-0 bg-[#4b5563]"
                  }`}
                />
              </button>
            </div>

            {/* Hanging Floating "Search buses" Button matching Image 1 */}
            <div className="absolute -bottom-5 sm:-bottom-6 left-1/2 -translate-x-1/2 z-20">
              <button
                type="submit"
                className="px-8 sm:px-12 py-3 sm:py-3.5 bg-[#d84e55] hover:bg-[#c23b42] text-white rounded-full font-bold text-sm sm:text-base shadow-xl shadow-red-600/35 transition-all hover:scale-105 active:scale-95 flex items-center space-x-2.5 cursor-pointer whitespace-nowrap"
              >
                <Search className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
                <span>Search buses</span>
              </button>
            </div>
          </form>

          {/* Floating Toast for Booking for women enabled/disabled */}
          {womenToast && (
            <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 px-5 py-2.5 bg-neutral-900/95 text-white text-xs sm:text-sm font-semibold rounded-xl shadow-2xl flex items-center space-x-2 animate-in fade-in slide-in-from-bottom-3 duration-200">
              <div className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
              <span>{womenToast}</span>
            </div>
          )}
        </div>

        {/* Popular Route Badges */}
        <div className="flex flex-wrap items-center justify-center gap-1.5 sm:gap-2 mt-3.5 sm:mt-6 text-[10px] sm:text-xs text-white/90">
          <span className="text-white/70 font-medium">Popular:</span>
          {[
            { from: "Bangalore", to: "Chennai" },
            { from: "Mumbai", to: "Pune" },
            { from: "Delhi", to: "Jaipur" },
            { from: "Hyderabad", to: "Bangalore" },
          ].map((pair) => (
            <button
              key={`${pair.from}-${pair.to}`}
              type="button"
              onClick={() => {
                setSource(pair.from);
                setDestination(pair.to);
              }}
              className="px-2 sm:px-2.5 py-0.5 sm:py-1 bg-white/10 hover:bg-white/20 border border-white/20 rounded-full transition-colors flex items-center space-x-1 cursor-pointer"
            >
              <span>{pair.from}</span>
              <span className="text-amber-300">➔</span>
              <span>{pair.to}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Select Date Modal Popup */}
      <SelectDateModal
        isOpen={showDateModal}
        onClose={() => setShowDateModal(false)}
        selectedDate={date}
        onSelectDate={(newDate) => setDate(newDate)}
      />

      {/* Booking for Women Info Modal */}
      {showWomenModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-6 shadow-2xl border border-gray-100 dark:border-slate-800 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100 dark:border-slate-800">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-full bg-pink-100 dark:bg-pink-950 flex items-center justify-center text-pink-600">
                  <User className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-base font-black text-gray-900 dark:text-white">
                    Booking for women
                  </h3>
                  <p className="text-[11px] text-gray-500">Dedicated safety features for solo female travelers</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowWomenModal(false)}
                className="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-all cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3 text-xs text-gray-600 dark:text-gray-300">
              <div className="p-3 bg-pink-50/70 dark:bg-pink-950/30 rounded-2xl border border-pink-100 dark:border-pink-900/40 space-y-1">
                <p className="font-bold text-pink-900 dark:text-pink-300 text-sm">
                  Priority Safe Travel
                </p>
                <p className="text-[11px] text-pink-700 dark:text-pink-400">
                  When enabled, search results prioritize buses with women-only reserved seats and verified safety protocols.
                </p>
              </div>

              <div className="space-y-2">
                <div className="flex items-start space-x-2">
                  <span className="text-pink-500 font-bold">•</span>
                  <p><span className="font-bold text-gray-800 dark:text-white">Adjacent Duo Seat Protection:</span> In sleeper and duo seats, male passengers cannot book adjacent to solo female travelers.</p>
                </div>
                <div className="flex items-start space-x-2">
                  <span className="text-pink-500 font-bold">•</span>
                  <p><span className="font-bold text-gray-800 dark:text-white">Primo Certified Partners:</span> Certified operators with background-verified staff, emergency exits, and GPS tracking.</p>
                </div>
                <div className="flex items-start space-x-2">
                  <span className="text-pink-500 font-bold">•</span>
                  <p><span className="font-bold text-gray-800 dark:text-white">24/7 Dedicated Support:</span> Priority assistance for female passengers before, during, and after travel.</p>
                </div>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setShowWomenModal(false)}
              className="w-full py-2.5 bg-[#d84e55] hover:bg-[#c23b42] text-white rounded-xl text-xs font-bold transition-all cursor-pointer shadow-md"
            >
              Got it
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
