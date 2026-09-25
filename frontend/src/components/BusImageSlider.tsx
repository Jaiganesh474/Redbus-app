"use client";

import React, { useState } from "react";
import Image from "next/image";
import { ChevronLeft, ChevronRight, Maximize2, X, Sparkles, Camera, ShieldCheck } from "lucide-react";

interface BusImageSliderProps {
  photoUrls?: string | string[];
  busName?: string;
  busType?: string;
  aspectRatio?: "video" | "wide" | "square";
  showThumbnails?: boolean;
  enableLightbox?: boolean;
  className?: string;
}

export default function BusImageSlider({
  photoUrls,
  busName = "Express Coach",
  busType = "AC Sleeper",
  aspectRatio = "video",
  showThumbnails = true,
  enableLightbox = false,
  className = "",
}: BusImageSliderProps) {
  // Parse images from comma-separated string or array
  const rawImages: string[] = React.useMemo(() => {
    if (!photoUrls) return [];
    if (Array.isArray(photoUrls)) return photoUrls.filter(Boolean);
    return photoUrls
      .split(",")
      .map((url) => url.trim())
      .filter((url) => url.length > 0);
  }, [photoUrls]);

  // Default fallback realistic fleet images if operator hasn't uploaded yet
  const defaultImages = React.useMemo(() => {
    const isSleeper = busType.toLowerCase().includes("sleeper");
    if (isSleeper) {
      return [
        "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1200&q=80",
      ];
    }
    return [
      "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?auto=format&fit=crop&w=1200&q=80",
    ];
  }, [busType]);

  const images = rawImages.length > 0 ? rawImages : defaultImages;
  const isAiEnhanced = rawImages.length > 0;

  const [currentIndex, setCurrentIndex] = useState(0);
  const [isLightboxOpen, setIsLightboxOpen] = useState(false);

  const prevSlide = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setCurrentIndex((prev) => (prev === 0 ? images.length - 1 : prev - 1));
  };

  const nextSlide = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setCurrentIndex((prev) => (prev === images.length - 1 ? 0 : prev + 1));
  };

  const aspectClass =
    aspectRatio === "square"
      ? "aspect-square"
      : aspectRatio === "wide"
      ? "aspect-[21/9]"
      : "aspect-[16/9]";

  return (
    <div className={`relative group select-none rounded-2xl overflow-hidden bg-slate-900 ${className}`}>
      {/* Main Slide Container */}
      <div
        onClick={(e) => {
          if (enableLightbox) {
            e.preventDefault();
            e.stopPropagation();
            setIsLightboxOpen(true);
          }
        }}
        className={`relative w-full ${aspectClass} overflow-hidden ${enableLightbox ? "cursor-pointer" : ""}`}
      >
        <img
          src={images[currentIndex]}
          alt={`${busName} - Image ${currentIndex + 1}`}
          onError={(e) => {
            (e.target as HTMLImageElement).src = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80";
          }}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
        />

        {/* Gradient Overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent pointer-events-none" />

        {/* Top Badges */}
        <div className="absolute top-3 left-3 flex items-center gap-1.5 z-10 pointer-events-none">
          {isAiEnhanced ? (
            <span className="px-2.5 py-1 bg-black/60 backdrop-blur-md text-amber-300 border border-amber-500/30 rounded-full text-[10px] font-bold flex items-center gap-1 shadow-sm">
              <Sparkles className="w-3 h-3 text-amber-400" />
              <span>Operator Verified Studio</span>
            </span>
          ) : (
            <span className="px-2.5 py-1 bg-black/60 backdrop-blur-md text-slate-200 border border-white/20 rounded-full text-[10px] font-semibold flex items-center gap-1 shadow-sm">
              <Camera className="w-3 h-3 text-red-400" />
              <span>Fleet Preview</span>
            </span>
          )}
        </div>

        {/* Top Right Counter & Zoom */}
        <div className="absolute top-3 right-3 flex items-center gap-2 z-20">
          <span className="px-2.5 py-1 bg-black/60 backdrop-blur-md text-white rounded-full text-[10px] font-mono font-bold shadow-sm">
            {currentIndex + 1} / {images.length}
          </span>
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              setIsLightboxOpen(true);
            }}
            className="p-1.5 bg-black/60 hover:bg-black/80 backdrop-blur-md text-white rounded-full transition-colors cursor-pointer"
            title="Expand Fullscreen View"
          >
            <Maximize2 className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Bottom Label */}
        <div className="absolute bottom-3 left-3 right-3 flex items-center justify-between text-white pointer-events-none z-10">
          <div>
            <p className="text-xs font-bold truncate drop-shadow-md">{busName}</p>
            <p className="text-[10px] text-slate-300 truncate drop-shadow-sm">{busType}</p>
          </div>
        </div>

        {/* Navigation Arrows */}
        {images.length > 1 && (
          <div className="absolute inset-x-2 top-1/2 -translate-y-1/2 flex items-center justify-between pointer-events-none z-30">
            <button
              type="button"
              onClick={prevSlide}
              className="pointer-events-auto p-2.5 rounded-full bg-black/70 hover:bg-black/95 text-white backdrop-blur-md shadow-lg border border-white/20 transition-all active:scale-90 cursor-pointer"
              title="Previous photo"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={nextSlide}
              className="pointer-events-auto p-2.5 rounded-full bg-black/70 hover:bg-black/95 text-white backdrop-blur-md shadow-lg border border-white/20 transition-all active:scale-90 cursor-pointer"
              title="Next photo"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>

      {/* Thumbnail Bar */}
      {showThumbnails && images.length > 1 && (
        <div className="flex items-center gap-1.5 p-2 bg-slate-950/80 backdrop-blur-sm overflow-x-auto scrollbar-none z-20">
          {images.map((img, idx) => (
            <button
              key={idx}
              type="button"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setCurrentIndex(idx);
              }}
              className={`relative shrink-0 w-12 h-9 rounded-lg overflow-hidden border-2 transition-all cursor-pointer ${
                idx === currentIndex
                  ? "border-[#d84e55] scale-105 shadow-md shadow-red-500/20"
                  : "border-transparent opacity-60 hover:opacity-100"
              }`}
            >
              <img
                src={img}
                alt={`Thumb ${idx + 1}`}
                onError={(e) => {
                  (e.target as HTMLImageElement).src = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80";
                }}
                className="w-full h-full object-cover"
              />
            </button>
          ))}
        </div>
      )}

      {/* Fullscreen Lightbox Modal */}
      {isLightboxOpen && (
        <div
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            setIsLightboxOpen(false);
          }}
          className="fixed inset-0 z-70 bg-black/95 backdrop-blur-xl flex flex-col items-center justify-center p-4 animate-in fade-in duration-200"
        >
          {/* Close Button */}
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              setIsLightboxOpen(false);
            }}
            className="absolute top-6 right-6 p-3 rounded-full bg-white/10 hover:bg-white/20 text-white transition-colors cursor-pointer z-50"
          >
            <X className="w-6 h-6" />
          </button>

          {/* Bus Title */}
          <div className="absolute top-6 left-6 text-white space-y-0.5">
            <h3 className="text-lg font-black">{busName}</h3>
            <p className="text-xs text-slate-400">{busType} • Photo {currentIndex + 1} of {images.length}</p>
          </div>

          {/* Fullscreen Image with Controls */}
          <div
            onClick={(e) => e.stopPropagation()}
            className="relative max-w-5xl max-h-[75vh] w-full flex items-center justify-center"
          >
            <img
              src={images[currentIndex]}
              alt={`${busName} Fullscreen`}
              className="max-h-[75vh] max-w-full object-contain rounded-2xl shadow-2xl border border-white/10"
            />

            {images.length > 1 && (
              <>
                <button
                  type="button"
                  onClick={prevSlide}
                  className="absolute -left-4 sm:left-4 p-3 rounded-full bg-white/10 hover:bg-white/30 text-white backdrop-blur-md transition-all cursor-pointer"
                >
                  <ChevronLeft className="w-6 h-6" />
                </button>
                <button
                  type="button"
                  onClick={nextSlide}
                  className="absolute -right-4 sm:right-4 p-3 rounded-full bg-white/10 hover:bg-white/30 text-white backdrop-blur-md transition-all cursor-pointer"
                >
                  <ChevronRight className="w-6 h-6" />
                </button>
              </>
            )}
          </div>

          {/* Bottom Thumbnails inside Lightbox */}
          {images.length > 1 && (
            <div
              onClick={(e) => e.stopPropagation()}
              className="mt-6 flex items-center gap-2 max-w-xl overflow-x-auto p-2"
            >
              {images.map((img, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    setCurrentIndex(idx);
                  }}
                  className={`w-16 h-12 rounded-xl overflow-hidden border-2 transition-all cursor-pointer ${
                    idx === currentIndex
                      ? "border-red-500 scale-110 shadow-lg"
                      : "border-transparent opacity-50 hover:opacity-90"
                  }`}
                >
                  <img src={img} alt="Thumb" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
