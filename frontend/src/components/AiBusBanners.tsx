"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Sparkles, ArrowRight, Zap, ChevronLeft, ChevronRight, Tag } from "lucide-react";
import { useGetBannersQuery } from "@/store/apiSlice";
import { Banner } from "@/types";

const FALLBACK_BANNERS: Banner[] = [
  {
    id: 1,
    tag: "AI Curated Fleet",
    title: "Volvo 9600 Multi-Axle Luxury Sleeper",
    subtitle: "Memory foam berths, personal charging hubs & panoramic sunset windows.",
    ctaText: "Explore Fleet",
    ctaLink: "/bus-tickets/bangalore-to-chennai",
    bgGradient: "from-slate-950 via-red-950/80 to-slate-900",
    badgeColor: "bg-red-500/20 border-red-500/30 text-red-300",
    imageUrl: "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80",
    promoCode: "₹699 ONWARDS",
    routeInfo: "Bangalore ⇄ Chennai",
  },
  {
    id: 2,
    tag: "Festive Offer • AI Dynamic Price",
    title: "Scenic Hill Station Holiday Express",
    subtitle: "Direct sleeper coaches to Ooty, Munnar, Coorg & Kodaikanal.",
    ctaText: "Book Getaway",
    ctaLink: "/bus-tickets/bangalore-to-ooty",
    bgGradient: "from-slate-950 via-emerald-950/80 to-slate-900",
    badgeColor: "bg-emerald-500/20 border-emerald-500/30 text-emerald-300",
    imageUrl: "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?auto=format&fit=crop&w=1200&q=80",
    promoCode: "FLAT 20% OFF",
    discountPercentage: 20,
    routeInfo: "Bangalore ⇄ Ooty",
  },
  {
    id: 3,
    tag: "Passenger Safety First",
    title: "AI Smart Safe Seating For Solo Women",
    subtitle: "Dedicated safe zones, 24/7 live GPS telemetry & verified drivers.",
    ctaText: "Safe Routes",
    ctaLink: "/bus-tickets/chennai-to-coimbatore",
    bgGradient: "from-slate-950 via-purple-950/80 to-slate-900",
    badgeColor: "bg-purple-500/20 border-purple-500/30 text-purple-300",
    imageUrl: "https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1200&q=80",
    promoCode: "100% VERIFIED",
    routeInfo: "Chennai ⇄ Coimbatore",
  },
];

export default function AiBusBanners() {
  const { data: remoteBanners, isLoading } = useGetBannersQuery();
  const [currentIndex, setCurrentIndex] = useState(0);

  const banners = (remoteBanners && remoteBanners.length > 0) ? remoteBanners : FALLBACK_BANNERS;

  useEffect(() => {
    if (banners.length <= 1) return;
    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % banners.length);
    }, 6000);
    return () => clearInterval(timer);
  }, [banners.length]);

  // Safeguard index if banners array changes
  const activeIdx = currentIndex >= banners.length ? 0 : currentIndex;
  const banner = banners[activeIdx] || FALLBACK_BANNERS[0];

  const prevBanner = () => {
    setCurrentIndex((prev) => (prev === 0 ? banners.length - 1 : prev - 1));
  };

  const nextBanner = () => {
    setCurrentIndex((prev) => (prev + 1) % banners.length);
  };

  const bgGradient = banner.bgGradient || "from-slate-950 via-red-950/80 to-slate-900";
  const badgeColor = banner.badgeColor || "bg-red-500/20 border-red-500/30 text-red-300";
  const imageUrl = banner.imageUrl || "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80";

  return (
    <div className="relative rounded-xl sm:rounded-3xl overflow-hidden shadow-lg sm:shadow-2xl border border-slate-800/80 bg-slate-950 text-white my-3 sm:my-8 group">
      {/* Background Image with Dynamic Overlay */}
      <div className="absolute inset-0 z-0">
        <img
          src={imageUrl}
          alt={banner.title}
          className="w-full h-full object-cover object-center opacity-25 sm:opacity-30 transition-all duration-1000 group-hover:scale-105"
        />
        <div className={`absolute inset-0 bg-gradient-to-r ${bgGradient} opacity-95 backdrop-blur-xs`} />
      </div>

      {/* Content Container - Compact on Mobile */}
      <div className="relative z-10 p-3.5 sm:p-7 lg:p-9 pb-6 sm:pb-7 flex flex-col md:flex-row md:items-center justify-between gap-2.5 sm:gap-6 min-h-[160px] sm:min-h-[220px]">
        <div className="max-w-xl space-y-1.5 sm:space-y-2.5">
          {/* Top Tag & Route Badges */}
          <div className="flex flex-wrap items-center gap-1.5 sm:gap-2">
            <span
              className={`inline-flex items-center gap-1 px-2 sm:px-3 py-0.5 rounded-full text-[9px] sm:text-xs font-bold uppercase tracking-wider border backdrop-blur-md ${badgeColor}`}
            >
              <Sparkles className="w-2.5 h-2.5 sm:w-3.5 sm:h-3.5" />
              <span>{banner.tag || "EXCLUSIVE OFFER"}</span>
            </span>
            {banner.routeInfo && (
              <span className="px-2 py-0.5 bg-white/10 rounded-full text-[9px] sm:text-[11px] font-semibold text-slate-300">
                {banner.routeInfo}
              </span>
            )}
            {banner.isAiGenerated && (
              <span className="px-1.5 py-0.5 bg-purple-500/20 border border-purple-500/30 rounded-md text-[8px] sm:text-[10px] font-bold text-purple-300">
                AI Powered
              </span>
            )}
          </div>

          {/* Banner Title */}
          <h2 className="text-sm sm:text-xl lg:text-3xl font-black text-white tracking-tight leading-tight drop-shadow-md">
            {banner.title}
          </h2>

          {/* Subtitle */}
          {banner.subtitle && (
            <p className="text-[10.5px] sm:text-sm text-slate-300 max-w-lg leading-snug drop-shadow-sm line-clamp-2 sm:line-clamp-none">
              {banner.subtitle}
            </p>
          )}

          {/* CTA & Accent */}
          <div className="flex flex-wrap items-center gap-2 sm:gap-3.5 pt-0.5 sm:pt-1">
            <Link
              href={banner.ctaLink || "/search"}
              className="inline-flex items-center gap-1 sm:gap-2 px-3 sm:px-5 py-1.5 sm:py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-lg sm:rounded-xl text-[11px] sm:text-xs font-bold shadow-md shadow-red-600/30 transition-all transform hover:-translate-y-0.5"
            >
              <span>{banner.ctaText || "Claim Offer"}</span>
              <ArrowRight className="w-3 h-3 sm:w-4 sm:h-4" />
            </Link>
            {banner.promoCode && (
              <div className="flex items-center gap-1 px-2 sm:px-3 py-1 sm:py-1.5 bg-white/10 backdrop-blur-md rounded-lg sm:rounded-xl border border-white/15 text-[9px] sm:text-[11px] font-black text-amber-300">
                <Tag className="w-2.5 h-2.5 sm:w-3 sm:h-3 text-amber-400" />
                <span>{banner.promoCode}</span>
              </div>
            )}
          </div>
        </div>

        {/* Right Feature Card for Desktop */}
        <div className="hidden lg:flex flex-col items-end gap-2 text-right">
          <div className="p-3.5 bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 max-w-xs space-y-1">
            <div className="flex items-center justify-end gap-1.5 text-xs font-bold text-emerald-400">
              <Zap className="w-3.5 h-3.5" />
              <span>AI Dynamic Real-Time Sync</span>
            </div>
            <p className="text-[11px] text-slate-300">
              Personalized fares, instant discount claims & 24/7 verified fleet tracking across all national routes.
            </p>
          </div>
        </div>
      </div>

      {/* Navigation Arrows */}
      {banners.length > 1 && (
        <>
          <button
            type="button"
            onClick={prevBanner}
            className="absolute left-1 sm:left-2.5 top-1/2 -translate-y-1/2 p-1 sm:p-2 rounded-full bg-black/50 hover:bg-black/80 text-white backdrop-blur-md opacity-70 sm:opacity-0 sm:group-hover:opacity-100 transition-all cursor-pointer z-20"
            title="Previous banner"
            aria-label="Previous banner"
          >
            <ChevronLeft className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
          </button>
          <button
            type="button"
            onClick={nextBanner}
            className="absolute right-1 sm:right-2.5 top-1/2 -translate-y-1/2 p-1 sm:p-2 rounded-full bg-black/50 hover:bg-black/80 text-white backdrop-blur-md opacity-70 sm:opacity-0 sm:group-hover:opacity-100 transition-all cursor-pointer z-20"
            title="Next banner"
            aria-label="Next banner"
          >
            <ChevronRight className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
          </button>

          {/* Slide Indicator Dots - Compact & Clean */}
          <div className="absolute bottom-1.5 sm:bottom-2.5 left-1/2 -translate-x-1/2 flex items-center justify-center gap-1 sm:gap-1.5 z-20 pointer-events-auto">
            {banners.map((_, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => setCurrentIndex(idx)}
                className={`p-0 m-0 border-0 outline-none block shrink-0 rounded-full transition-all duration-300 cursor-pointer ${
                  idx === activeIdx
                    ? "w-4 sm:w-6 h-1 sm:h-1.5 bg-[#d84e55] shadow-xs shadow-red-500/50"
                    : "w-1 sm:w-1.5 h-1 sm:h-1.5 bg-white/40 hover:bg-white/70"
                }`}
                aria-label={`Go to slide ${idx + 1}`}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
