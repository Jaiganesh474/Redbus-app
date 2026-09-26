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
  const [touchStart, setTouchStart] = useState<number | null>(null);
  const [touchEnd, setTouchEnd] = useState<number | null>(null);

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

  // Mobile touch swipe handling
  const minSwipeDistance = 45;
  const onTouchStart = (e: React.TouchEvent) => {
    setTouchEnd(null);
    setTouchStart(e.targetTouches[0].clientX);
  };

  const onTouchMove = (e: React.TouchEvent) => {
    setTouchEnd(e.targetTouches[0].clientX);
  };

  const onTouchEnd = () => {
    if (!touchStart || !touchEnd) return;
    const distance = touchStart - touchEnd;
    const isLeftSwipe = distance > minSwipeDistance;
    const isRightSwipe = distance < -minSwipeDistance;
    if (isLeftSwipe) {
      nextBanner();
    } else if (isRightSwipe) {
      prevBanner();
    }
  };

  const bgGradient = banner.bgGradient || "from-slate-950 via-red-950/80 to-slate-900";
  const badgeColor = banner.badgeColor || "bg-red-500/20 border-red-500/30 text-red-300";
  const imageUrl = banner.imageUrl || "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80";

  return (
    <div
      className="relative rounded-2xl sm:rounded-3xl overflow-hidden shadow-xl border border-slate-800/80 bg-slate-950 text-white my-4 sm:my-8 group select-none"
      onTouchStart={onTouchStart}
      onTouchMove={onTouchMove}
      onTouchEnd={onTouchEnd}
    >
      {/* Background Image with Dynamic Overlay */}
      <div className="absolute inset-0 z-0 pointer-events-none">
        <img
          src={imageUrl}
          alt={banner.title}
          className="w-full h-full object-cover object-center opacity-25 sm:opacity-30 transition-all duration-1000 group-hover:scale-105"
        />
        <div className={`absolute inset-0 bg-gradient-to-r ${bgGradient} opacity-95 backdrop-blur-xs`} />
      </div>

      {/* Content Container - Spacious & Non-Overlapping on Mobile */}
      <div className="relative z-10 px-4 py-5 sm:p-7 lg:p-9 pb-10 sm:pb-8 flex flex-col md:flex-row md:items-center justify-between gap-4 sm:gap-6 min-h-[190px] sm:min-h-[220px]">
        <div className="max-w-2xl space-y-2 sm:space-y-3">
          {/* Top Tag & Route Badges */}
          <div className="flex flex-wrap items-center gap-1.5 sm:gap-2">
            <span
              className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] sm:text-xs font-bold uppercase tracking-wider border backdrop-blur-md ${badgeColor}`}
            >
              <Sparkles className="w-3 h-3" />
              <span>{banner.tag || "EXCLUSIVE OFFER"}</span>
            </span>
            {banner.routeInfo && (
              <span className="px-2.5 py-0.5 bg-white/10 border border-white/10 rounded-full text-[10px] sm:text-[11px] font-semibold text-slate-200">
                {banner.routeInfo}
              </span>
            )}
            {banner.isAiGenerated && (
              <span className="px-2 py-0.5 bg-purple-500/20 border border-purple-500/30 rounded-full text-[9px] sm:text-[10px] font-bold text-purple-300">
                AI Powered
              </span>
            )}
          </div>

          {/* Banner Title - Clean Typography */}
          <h2 className="text-base sm:text-2xl lg:text-3xl font-black text-white tracking-tight leading-snug drop-shadow-md">
            {banner.title}
          </h2>

          {/* Subtitle */}
          {banner.subtitle && (
            <p className="text-xs sm:text-sm text-slate-200/90 max-w-xl leading-relaxed drop-shadow-sm line-clamp-2 sm:line-clamp-none">
              {banner.subtitle}
            </p>
          )}

          {/* CTA & Promo Code - Well Spaced */}
          <div className="flex flex-wrap items-center gap-2.5 sm:gap-3.5 pt-1 sm:pt-2">
            <Link
              href={banner.ctaLink || "/search"}
              className="inline-flex items-center gap-1.5 px-4 py-2 sm:px-5 sm:py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs sm:text-sm font-bold shadow-md shadow-red-600/30 transition-all transform hover:-translate-y-0.5 active:scale-95"
            >
              <span>{banner.ctaText || "Claim Offer"}</span>
              <ArrowRight className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
            </Link>
            {banner.promoCode && (
              <div className="flex items-center gap-1.5 px-3 py-1.5 bg-black/40 backdrop-blur-md rounded-xl border border-white/15 text-[11px] sm:text-xs font-black text-amber-300 tracking-wide">
                <Tag className="w-3 h-3 text-amber-400" />
                <span>{banner.promoCode}</span>
              </div>
            )}
          </div>
        </div>

        {/* Right Feature Card for Large Screens */}
        <div className="hidden lg:flex flex-col items-end gap-2 text-right">
          <div className="p-4 bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 max-w-xs space-y-1.5">
            <div className="flex items-center justify-end gap-1.5 text-xs font-bold text-emerald-400">
              <Zap className="w-3.5 h-3.5" />
              <span>AI Dynamic Real-Time Sync</span>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Personalized fares, instant discount claims & 24/7 verified fleet tracking across all national routes.
            </p>
          </div>
        </div>
      </div>

      {/* Desktop Navigation Arrows */}
      {banners.length > 1 && (
        <>
          <button
            type="button"
            onClick={prevBanner}
            className="hidden sm:flex absolute left-3 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/60 hover:bg-black/85 text-white backdrop-blur-md opacity-0 group-hover:opacity-100 transition-all cursor-pointer z-20 shadow-md"
            title="Previous banner"
            aria-label="Previous banner"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <button
            type="button"
            onClick={nextBanner}
            className="hidden sm:flex absolute right-3 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/60 hover:bg-black/85 text-white backdrop-blur-md opacity-0 group-hover:opacity-100 transition-all cursor-pointer z-20 shadow-md"
            title="Next banner"
            aria-label="Next banner"
          >
            <ChevronRight className="w-4 h-4" />
          </button>

          {/* Slide Indicator Dots - Positioned safely with zero overlap */}
          <div className="absolute bottom-2.5 sm:bottom-3 left-1/2 -translate-x-1/2 flex items-center justify-center gap-1.5 z-20 pointer-events-auto">
            {banners.map((_, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => setCurrentIndex(idx)}
                className={`p-0 m-0 border-0 outline-none block shrink-0 rounded-full transition-all duration-300 cursor-pointer ${
                  idx === activeIdx
                    ? "w-6 h-1.5 bg-[#d84e55] shadow-xs shadow-red-500/50"
                    : "w-1.5 h-1.5 bg-white/40 hover:bg-white/70"
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
