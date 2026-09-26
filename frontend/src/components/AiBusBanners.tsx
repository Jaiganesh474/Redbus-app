"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Sparkles, ArrowRight, ShieldCheck, Zap, ChevronLeft, ChevronRight, Percent, Tag } from "lucide-react";

const BANNERS = [
  {
    id: 1,
    tag: "AI Curated Fleet",
    title: "Volvo 9600 Multi-Axle Luxury Sleeper",
    subtitle: "Memory foam berths, personal charging hubs & panoramic sunset windows.",
    ctaText: "Explore Luxury Fleet",
    ctaLink: "/bus-tickets/bangalore-to-chennai",
    bgGradient: "from-slate-950 via-red-950/80 to-slate-900",
    badgeColor: "bg-red-500/20 border-red-500/30 text-red-300",
    image: "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80",
    accent: "₹699 onwards",
    route: "Bangalore ⇄ Chennai",
  },
  {
    id: 2,
    tag: "Festive Offer • AI Dynamic Price",
    title: "Scenic Hill Station Holiday Express",
    subtitle: "Direct sleeper coaches to Ooty, Munnar, Coorg & Kodaikanal.",
    ctaText: "Book Weekend Getaway",
    ctaLink: "/bus-tickets/bangalore-to-ooty",
    bgGradient: "from-slate-950 via-emerald-950/80 to-slate-900",
    badgeColor: "bg-emerald-500/20 border-emerald-500/30 text-emerald-300",
    image: "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?auto=format&fit=crop&w=1200&q=80",
    accent: "Flat 20% OFF",
    route: "Bangalore ⇄ Ooty",
  },
  {
    id: 3,
    tag: "Passenger Safety First",
    title: "AI Smart Safe Seating For Solo Women",
    subtitle: "Dedicated safe zones, 24/7 live GPS telemetry & verified drivers.",
    ctaText: "Discover Safe Routes",
    ctaLink: "/bus-tickets/chennai-to-coimbatore",
    bgGradient: "from-slate-950 via-purple-950/80 to-slate-900",
    badgeColor: "bg-purple-500/20 border-purple-500/30 text-purple-300",
    image: "https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1200&q=80",
    accent: "100% Verified",
    route: "Chennai ⇄ Coimbatore",
  },
];

export default function AiBusBanners() {
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % BANNERS.length);
    }, 6000);
    return () => clearInterval(timer);
  }, []);

  const banner = BANNERS[currentIndex];

  const prevBanner = () => {
    setCurrentIndex((prev) => (prev === 0 ? BANNERS.length - 1 : prev - 1));
  };

  const nextBanner = () => {
    setCurrentIndex((prev) => (prev + 1) % BANNERS.length);
  };

  return (
    <div className="relative rounded-2xl sm:rounded-3xl overflow-hidden shadow-xl sm:shadow-2xl border border-slate-800/80 bg-slate-950 text-white my-4 sm:my-8 group">
      {/* Background Image with Dynamic Overlay */}
      <div className="absolute inset-0 z-0">
        <img
          src={banner.image}
          alt={banner.title}
          className="w-full h-full object-cover object-center opacity-30 transition-all duration-1000 group-hover:scale-105"
        />
        <div className={`absolute inset-0 bg-gradient-to-r ${banner.bgGradient} opacity-90 backdrop-blur-xs`} />
      </div>

      {/* Content Container */}
      <div className="relative z-10 p-4 sm:p-8 lg:p-10 pb-8 sm:pb-8 flex flex-col md:flex-row md:items-center justify-between gap-3 sm:gap-6 min-h-[190px] sm:min-h-[250px]">
        <div className="max-w-xl space-y-2 sm:space-y-3">
          {/* Top Tag */}
          <div className="flex flex-wrap items-center gap-1.5 sm:gap-2">
            <span
              className={`inline-flex items-center gap-1 sm:gap-1.5 px-2.5 sm:px-3 py-0.5 sm:py-1 rounded-full text-[10px] sm:text-xs font-bold uppercase tracking-wider border backdrop-blur-md ${banner.badgeColor}`}
            >
              <Sparkles className="w-3 h-3 sm:w-3.5 sm:h-3.5" />
              <span>{banner.tag}</span>
            </span>
            <span className="px-2 py-0.5 bg-white/10 rounded-full text-[10px] sm:text-[11px] font-semibold text-slate-300">
              {banner.route}
            </span>
          </div>

          {/* Banner Title */}
          <h2 className="text-base sm:text-2xl lg:text-4xl font-black text-white tracking-tight leading-snug sm:leading-tight drop-shadow-md">
            {banner.title}
          </h2>

          {/* Subtitle */}
          <p className="text-[11px] sm:text-sm text-slate-300 max-w-lg leading-relaxed drop-shadow-sm line-clamp-2 sm:line-clamp-none">
            {banner.subtitle}
          </p>

          {/* CTA & Accent */}
          <div className="flex flex-wrap items-center gap-2 sm:gap-4 pt-1 sm:pt-2 mb-2 sm:mb-0">
            <Link
              href={banner.ctaLink}
              className="inline-flex items-center gap-1.5 sm:gap-2 px-3.5 sm:px-6 py-2 sm:py-3 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl sm:rounded-2xl text-xs sm:text-sm font-bold shadow-md sm:shadow-lg shadow-red-600/30 transition-all transform hover:-translate-y-0.5"
            >
              <span>{banner.ctaText}</span>
              <ArrowRight className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
            </Link>
            <div className="px-2.5 sm:px-3.5 py-1.5 sm:py-2 bg-white/10 backdrop-blur-md rounded-xl sm:rounded-2xl border border-white/15 text-[10px] sm:text-xs font-black text-amber-300">
              {banner.accent}
            </div>
          </div>
        </div>

        {/* Right Feature Card */}
        <div className="hidden lg:flex flex-col items-end gap-3 text-right">
          <div className="p-4 bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 max-w-xs space-y-1.5">
            <div className="flex items-center justify-end gap-1.5 text-xs font-bold text-emerald-400">
              <Zap className="w-4 h-4" />
              <span>AI Dynamic Fleet Sync</span>
            </div>
            <p className="text-[11px] text-slate-300">
              Real-time image sliders, live delay predictions & smart seat safety algorithms enabled across all buses.
            </p>
          </div>
        </div>
      </div>

      {/* Navigation Arrows */}
      <button
        type="button"
        onClick={prevBanner}
        className="absolute left-1.5 sm:left-3 top-1/2 -translate-y-1/2 p-1.5 sm:p-2.5 rounded-full bg-black/40 hover:bg-black/70 text-white backdrop-blur-md opacity-60 sm:opacity-0 sm:group-hover:opacity-100 transition-all cursor-pointer z-20"
        title="Previous banner"
        aria-label="Previous banner"
      >
        <ChevronLeft className="w-4 h-4 sm:w-5 sm:h-5" />
      </button>
      <button
        type="button"
        onClick={nextBanner}
        className="absolute right-1.5 sm:right-3 top-1/2 -translate-y-1/2 p-1.5 sm:p-2.5 rounded-full bg-black/40 hover:bg-black/70 text-white backdrop-blur-md opacity-60 sm:opacity-0 sm:group-hover:opacity-100 transition-all cursor-pointer z-20"
        title="Next banner"
        aria-label="Next banner"
      >
        <ChevronRight className="w-4 h-4 sm:w-5 sm:h-5" />
      </button>

      {/* Slide Indicator Dots / Pills */}
      <div className="absolute bottom-2.5 sm:bottom-4 left-1/2 -translate-x-1/2 flex items-center justify-center gap-1.5 sm:gap-2 z-20 pointer-events-auto">
        {BANNERS.map((_, idx) => (
          <button
            key={idx}
            type="button"
            onClick={() => setCurrentIndex(idx)}
            className={`p-0 m-0 border-0 outline-none block shrink-0 rounded-full transition-all duration-300 cursor-pointer ${
              idx === currentIndex
                ? "w-6 sm:w-8 h-1.5 sm:h-2 bg-[#d84e55] shadow-xs shadow-red-500/50"
                : "w-1.5 sm:w-2 h-1.5 sm:h-2 bg-white/40 hover:bg-white/70"
            }`}
            aria-label={`Go to slide ${idx + 1}`}
          />
        ))}
      </div>
    </div>
  );
}
