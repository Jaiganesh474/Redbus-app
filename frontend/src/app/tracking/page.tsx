"use client";

import React, { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import { motion } from "framer-motion";
import {
  Navigation,
  Bus,
  Clock,
  MapPin,
  Phone,
  ShieldCheck,
  Share2,
  Download,
  ArrowLeft,
  RefreshCw,
  Zap,
} from "lucide-react";
import { useGetBookingByPnrQuery, API_BASE_URL } from "@/store/apiSlice";

function TrackingContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pnr = searchParams.get("pnr") || "";

  const { data: booking, isLoading, isError, refetch } = useGetBookingByPnrQuery(pnr, {
    skip: !pnr,
  });

  const [progress, setProgress] = useState(42);
  const [speed, setSpeed] = useState(64);
  const [nextStopEta, setNextStopEta] = useState("18 mins");
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Live simulation ticker for realism
  useEffect(() => {
    const interval = setInterval(() => {
      setSpeed(58 + Math.floor(Math.random() * 14));
      setProgress((prev) => (prev >= 95 ? 40 : prev + 0.2));
    }, 4000);
    return () => clearInterval(interval);
  }, []);

  const handleManualRefresh = () => {
    setIsRefreshing(true);
    refetch();
    setTimeout(() => setIsRefreshing(false), 800);
  };

  const pdfUrl = pnr ? `${API_BASE_URL}/bookings/${pnr}/ticket-pdf` : "#";

  if (!pnr) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-3xl p-8 max-w-md w-full text-center shadow-lg border border-gray-100">
          <Navigation className="w-12 h-12 text-[#d84e55] mx-auto mb-4 animate-bounce" />
          <h2 className="text-xl font-bold text-gray-900 mb-2">Live GPS Bus Tracking</h2>
          <p className="text-sm text-gray-500 mb-6">
            Please provide a valid Booking PNR to track your bus in real time.
          </p>
          <Link
            href="/my-bookings"
            className="inline-block px-6 py-3 bg-[#d84e55] text-white font-bold rounded-2xl shadow-md hover:bg-[#c13e45] transition-all"
          >
            View My Bookings
          </Link>
        </div>
      </div>
    );
  }

  const operatorName = booking?.operatorName || "redBus SmartBus";
  const busNumber = "TN-12AU-" + (pnr.slice(-4) || "1221");
  const source = booking?.sourceCity || "Origin";
  const destination = booking?.destinationCity || "Destination";
  const depTime = booking?.departureTime || "22:00";
  const arrTime = booking?.arrivalTime || "06:00";

  return (
    <div className="min-h-screen bg-gray-900 text-white pb-16">
      {/* Top Header */}
      <header className="bg-gray-800/80 backdrop-blur-md border-b border-gray-700/60 sticky top-0 z-40 px-4 py-3.5">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <button
              onClick={() => router.back()}
              className="p-2 bg-gray-700/60 hover:bg-gray-700 rounded-xl text-gray-300 transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <div className="flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
                <h1 className="text-base font-bold text-white tracking-wide">
                  Live Bus Tracking
                </h1>
                <span className="px-2 py-0.5 text-[10px] font-black uppercase tracking-wider bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-full">
                  GPS Active
                </span>
              </div>
              <p className="text-xs text-gray-400 font-mono">PNR: {pnr}</p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={handleManualRefresh}
              className={`p-2 bg-gray-700/60 hover:bg-gray-700 rounded-xl text-gray-300 transition-colors ${
                isRefreshing ? "animate-spin text-emerald-400" : ""
              }`}
              title="Refresh GPS location"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
            <a
              href={`/api/v1/bookings/${pnr}/ticket-pdf`}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center space-x-1.5 px-3 py-1.5 bg-[#d84e55] hover:bg-[#c13e45] text-white text-xs font-bold rounded-xl transition-all shadow-sm"
            >
              <Download className="w-3.5 h-3.5" />
              <span>E-Ticket</span>
            </a>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 pt-4 space-y-4">
        {/* Animated GPS Map Simulator Card */}
        <div className="relative h-64 sm:h-80 bg-gradient-to-br from-gray-950 via-gray-900 to-gray-950 rounded-3xl overflow-hidden border border-gray-700/80 shadow-2xl flex flex-col justify-between p-6">
          {/* Animated Map Grid Lines */}
          <div className="absolute inset-0 opacity-15 bg-[radial-gradient(#38bdf8_1px,transparent_1px)] [background-size:16px_16px]" />

          {/* Route Corridor Polyline */}
          <div className="absolute top-1/2 left-8 right-8 -translate-y-1/2 h-2 bg-gray-800 rounded-full overflow-hidden border border-gray-700">
            <motion.div
              className="h-full bg-gradient-to-r from-emerald-500 via-teal-400 to-[#d84e55] rounded-full"
              initial={{ width: "0%" }}
              animate={{ width: `${progress}%` }}
              transition={{ duration: 0.8, ease: "easeOut" }}
            />
          </div>

          {/* Bus Pin Marker on Path */}
          <motion.div
            className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 z-10 flex flex-col items-center"
            style={{ left: `${progress}%` }}
            animate={{ y: [-2, 2, -2] }}
            transition={{ repeat: Infinity, duration: 2, ease: "easeInOut" }}
          >
            <div className="px-2 py-0.5 bg-emerald-500 text-gray-950 text-[10px] font-black rounded-full shadow-lg mb-1 whitespace-nowrap">
              {speed} km/h • On Time
            </div>
            <div className="w-9 h-9 bg-emerald-500 text-white rounded-2xl flex items-center justify-center shadow-[0_0_20px_rgba(16,185,129,0.7)] border-2 border-white">
              <Bus className="w-5 h-5" />
            </div>
          </motion.div>

          {/* Top Live Status Bar */}
          <div className="relative z-10 flex items-center justify-between">
            <div className="bg-gray-800/90 backdrop-blur-md px-3.5 py-1.5 rounded-2xl border border-gray-700 flex items-center space-x-2">
              <Zap className="w-4 h-4 text-amber-400 animate-pulse" />
              <span className="text-xs font-semibold text-gray-200">
                Speed: <strong className="text-white font-mono">{speed} km/h</strong>
              </span>
            </div>
            <div className="bg-gray-800/90 backdrop-blur-md px-3.5 py-1.5 rounded-2xl border border-gray-700 flex items-center space-x-2">
              <Clock className="w-4 h-4 text-emerald-400" />
              <span className="text-xs font-semibold text-gray-200">
                Next Stop: <strong className="text-emerald-400">{nextStopEta}</strong>
              </span>
            </div>
          </div>

          {/* Bottom Route Cities */}
          <div className="relative z-10 flex items-center justify-between pt-12">
            <div className="text-left">
              <p className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider">Origin</p>
              <h3 className="text-base sm:text-lg font-bold text-white">{source}</h3>
              <p className="text-xs text-gray-400 font-mono">{depTime}</p>
            </div>
            <div className="text-right">
              <p className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider">Destination</p>
              <h3 className="text-base sm:text-lg font-bold text-white">{destination}</h3>
              <p className="text-xs text-gray-400 font-mono">{arrTime}</p>
            </div>
          </div>
        </div>

        {/* Bus & Journey Summary Card */}
        <div className="bg-gray-800/90 rounded-3xl p-5 border border-gray-700/80 shadow-lg space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-gray-700/60">
            <div>
              <h2 className="text-base font-bold text-white">{operatorName}</h2>
              <p className="text-xs text-gray-400 font-mono">Reg: {busNumber}</p>
            </div>
            <div className="flex items-center space-x-1.5 px-3 py-1 bg-emerald-500/10 border border-emerald-500/30 rounded-xl text-emerald-400 text-xs font-bold">
              <ShieldCheck className="w-4 h-4" />
              <span>Primo Certified</span>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
            <div className="bg-gray-900/60 p-3 rounded-2xl border border-gray-700/50">
              <p className="text-[10px] text-gray-400 uppercase font-semibold">Boarding Point</p>
              <p className="text-xs font-bold text-gray-200 truncate mt-0.5">
                {booking?.boardingPoint || "Main Terminus"}
              </p>
            </div>
            <div className="bg-gray-900/60 p-3 rounded-2xl border border-gray-700/50">
              <p className="text-[10px] text-gray-400 uppercase font-semibold">Dropping Point</p>
              <p className="text-xs font-bold text-gray-200 truncate mt-0.5">
                {booking?.droppingPoint || destination}
              </p>
            </div>
            <div className="bg-gray-900/60 p-3 rounded-2xl border border-gray-700/50">
              <p className="text-[10px] text-gray-400 uppercase font-semibold">Confirmed Seat(s)</p>
              <p className="text-xs font-bold text-emerald-400 font-mono mt-0.5">
                {booking?.passengers?.map((p: any) => p.seatNumber).join(", ") || "Confirmed"}
              </p>
            </div>
            <div className="bg-gray-900/60 p-3 rounded-2xl border border-gray-700/50">
              <p className="text-[10px] text-gray-400 uppercase font-semibold">Live GPS Frequency</p>
              <p className="text-xs font-bold text-sky-400 mt-0.5">Real-time (4s)</p>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
          <a
            href={`/api/v1/bookings/${pnr}/ticket-pdf`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center space-x-2 py-3.5 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-sm rounded-2xl shadow-lg transition-all"
          >
            <Download className="w-4 h-4" />
            <span>Download Official E-Ticket (PDF)</span>
          </a>

          <Link
            href="/my-bookings"
            className="flex items-center justify-center space-x-2 py-3.5 bg-gray-800 hover:bg-gray-700 text-gray-200 font-semibold text-sm rounded-2xl border border-gray-700 transition-all"
          >
            <span>Manage All Bookings</span>
          </Link>
        </div>
      </main>
    </div>
  );
}

export default function TrackingPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-gray-900" />}>
      <TrackingContent />
    </Suspense>
  );
}
