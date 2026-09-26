"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useAppDispatch, useAppSelector } from "@/store";
import { setSearchParams, swapCities } from "@/store/searchSlice";
import { useParseNlpQueryMutation, useGetAvailableCitiesQuery } from "@/store/apiSlice";
import AiCityDropdown from "./AiCityDropdown";
import {
  MapPin,
  ArrowRightLeft,
  Calendar,
  Search,
  Sparkles,
  ArrowRight,
  Bus,
} from "lucide-react";

export default function HeroSearch() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const searchState = useAppSelector((state) => state.search);

  const [source, setSource] = useState(searchState.sourceCity || "");
  const [destination, setDestination] = useState(searchState.destinationCity || "");
  const [date, setDate] = useState(searchState.travelDate);
  const [nlpQuery, setNlpQuery] = useState("");
  const [isNlpLoading, setIsNlpLoading] = useState(false);

  const { data: availableCities = ["Bangalore", "Chennai", "Mumbai", "Pune", "Delhi", "Jaipur", "Hyderabad", "Coimbatore"] } = useGetAvailableCitiesQuery();
  const [parseNlpMutation] = useParseNlpQueryMutation();

  const handleSwap = () => {
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
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    const dateStr = d.toISOString().split("T")[0];
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

        {/* Standard Search Card */}
        <div className="bg-white dark:bg-slate-900 rounded-xl sm:rounded-2xl p-3 sm:p-5 shadow-xl sm:shadow-2xl text-gray-800 dark:text-slate-100 border border-gray-100 dark:border-slate-800 max-w-4xl mx-auto">
          <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-12 gap-2 sm:gap-3 items-center">
            {/* From City */}
            <div className="md:col-span-4 relative">
              <AiCityDropdown
                label="From"
                value={source}
                onChange={setSource}
                excludeCity={destination}
                placeholder="Select departure city"
                required
              />
            </div>

            {/* Desktop Swap Button */}
            <div className="hidden md:flex md:col-span-1 justify-center pt-5">
              <button
                type="button"
                onClick={handleSwap}
                title="Swap Cities"
                className="w-8.5 h-8.5 sm:w-9 sm:h-9 rounded-full bg-gray-100 dark:bg-slate-800 hover:bg-red-50 dark:hover:bg-slate-700 hover:text-[#d84e55] dark:hover:text-red-400 border border-gray-200 dark:border-slate-700 flex items-center justify-center text-gray-600 dark:text-slate-300 transition-all hover:scale-110 active:scale-95 cursor-pointer shadow-xs"
              >
                <ArrowRightLeft className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              </button>
            </div>

            {/* Mobile Swap Button */}
            <div className="flex md:hidden justify-center -my-1 relative z-10">
              <button
                type="button"
                onClick={handleSwap}
                title="Swap Cities"
                className="px-2.5 py-0.5 bg-gray-50 dark:bg-slate-800 hover:bg-red-50 dark:hover:bg-slate-700 text-[#d84e55] dark:text-red-400 border border-gray-200 dark:border-slate-700 rounded-full text-[10px] font-bold shadow-2xs flex items-center space-x-1 transition-all active:scale-95 cursor-pointer"
              >
                <ArrowRightLeft className="w-3 h-3" />
                <span>Swap Cities</span>
              </button>
            </div>

            {/* To City */}
            <div className="md:col-span-4 relative">
              <AiCityDropdown
                label="To"
                value={destination}
                onChange={setDestination}
                excludeCity={source}
                placeholder="Select destination city"
                required
              />
            </div>

            {/* Travel Date */}
            <div className="md:col-span-3 relative group">
              <div className="flex items-center justify-between mb-1">
                <label className="block text-[10px] sm:text-[11px] font-bold text-gray-500 dark:text-slate-400 uppercase tracking-wider flex items-center space-x-1">
                  <Calendar className="w-3 sm:w-3.5 h-3 sm:h-3.5 text-gray-600 dark:text-slate-400" />
                  <span>Date</span>
                </label>
                <div className="flex space-x-1.5 text-[9px] sm:text-[10px]">
                  <button
                    type="button"
                    onClick={() => setQuickDate(0)}
                    className="text-gray-500 dark:text-slate-400 hover:text-[#d84e55] dark:hover:text-red-400 font-semibold cursor-pointer"
                  >
                    Today
                  </button>
                  <span className="text-gray-300 dark:text-slate-600">|</span>
                  <button
                    type="button"
                    onClick={() => setQuickDate(1)}
                    className="text-gray-500 dark:text-slate-400 hover:text-[#d84e55] dark:hover:text-red-400 font-semibold cursor-pointer"
                  >
                    Tomorrow
                  </button>
                </div>
              </div>
              <input
                type="date"
                required
                value={date}
                min={new Date().toISOString().split("T")[0]}
                onChange={(e) => setDate(e.target.value)}
                className="w-full px-3 sm:px-3.5 py-2 sm:py-2.5 bg-gray-50 dark:bg-slate-800 hover:bg-gray-100/80 dark:hover:bg-slate-700/80 focus:bg-white dark:focus:bg-slate-800 rounded-xl border border-gray-200 dark:border-slate-700 text-xs sm:text-sm font-semibold text-gray-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:border-transparent transition-all cursor-pointer"
              />
            </div>

            {/* Search Button */}
            <div className="md:col-span-12 mt-1 sm:mt-2">
              <button
                type="submit"
                className="w-full py-2.5 sm:py-3.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl font-black text-xs sm:text-base shadow-md sm:shadow-lg shadow-red-500/25 transition-all flex items-center justify-center space-x-2 active:scale-[0.99] cursor-pointer"
              >
                <Search className="w-3.5 h-3.5 sm:w-5 sm:h-5" />
                <span>SEARCH BUSES</span>
              </button>
            </div>
          </form>
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
    </div>
  );
}
