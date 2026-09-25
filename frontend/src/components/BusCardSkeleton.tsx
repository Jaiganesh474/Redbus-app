"use client";

import React from "react";

export default function BusCardSkeleton() {
  return (
    <div className="bg-white dark:bg-[#0f172a] rounded-2xl border border-gray-200 dark:border-slate-800 shadow-xs mb-4 overflow-hidden relative">
      {/* Shimmer sweep line */}
      <div className="absolute inset-0 -translate-x-full animate-[shimmer_1.8s_infinite] bg-gradient-to-r from-transparent via-white/40 dark:via-slate-700/30 to-transparent pointer-events-none" />

      <div className="p-4 sm:p-5">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-center">
          {/* Operator & Bus Info */}
          <div className="md:col-span-4 space-y-2.5">
            <div className="flex items-start space-x-3">
              <div className="w-14 h-14 rounded-xl bg-gray-100 dark:bg-slate-800 shrink-0 border border-gray-100 dark:border-slate-700/60 animate-pulse" />
              <div className="flex-1 min-w-0 space-y-2">
                <div className="flex items-center space-x-2">
                  <div className="h-5 w-32 bg-gray-200 dark:bg-slate-700 rounded-md animate-pulse" />
                  <div className="h-4 w-12 bg-emerald-100 dark:bg-emerald-950/60 rounded-md animate-pulse" />
                </div>
                <div className="h-3.5 w-24 bg-gray-100 dark:bg-slate-800 rounded animate-pulse" />
              </div>
            </div>

            {/* Amenities pills */}
            <div className="flex flex-wrap gap-1.5 pt-1">
              <div className="h-5 w-16 bg-gray-100 dark:bg-slate-800 rounded-lg animate-pulse" />
              <div className="h-5 w-20 bg-gray-100 dark:bg-slate-800 rounded-lg animate-pulse" />
              <div className="h-5 w-14 bg-gray-100 dark:bg-slate-800 rounded-lg animate-pulse" />
            </div>
          </div>

          {/* Schedule & Duration */}
          <div className="md:col-span-5">
            <div className="flex items-center justify-between sm:justify-start sm:space-x-6">
              <div className="space-y-1.5">
                <div className="h-6 w-16 bg-gray-200 dark:bg-slate-700 rounded-md animate-pulse" />
                <div className="h-3 w-14 bg-gray-100 dark:bg-slate-800 rounded animate-pulse" />
              </div>
              <div className="flex flex-col items-center px-2 space-y-1.5">
                <div className="h-3 w-12 bg-gray-200 dark:bg-slate-700 rounded animate-pulse" />
                <div className="w-20 sm:w-28 h-1 bg-gray-200 dark:bg-slate-700 rounded-full animate-pulse" />
                <div className="h-2.5 w-16 bg-gray-100 dark:bg-slate-800 rounded animate-pulse" />
              </div>
              <div className="space-y-1.5">
                <div className="h-6 w-16 bg-gray-200 dark:bg-slate-700 rounded-md animate-pulse" />
                <div className="h-3 w-14 bg-gray-100 dark:bg-slate-800 rounded animate-pulse" />
              </div>
            </div>
          </div>

          {/* Price & Action */}
          <div className="md:col-span-3 flex flex-row md:flex-col items-center md:items-end justify-between border-t md:border-t-0 pt-3 md:pt-0 border-gray-100 dark:border-slate-800">
            <div className="space-y-1.5 text-left md:text-right">
              <div className="h-6 w-20 bg-gray-200 dark:bg-slate-700 rounded-md ml-auto animate-pulse" />
              <div className="h-3 w-16 bg-gray-100 dark:bg-slate-800 rounded ml-auto animate-pulse" />
            </div>
            <div className="h-10 w-28 bg-red-100 dark:bg-red-950/60 rounded-xl mt-3 animate-pulse border border-red-200 dark:border-red-900/50" />
          </div>
        </div>
      </div>

      {/* Bottom quick tabs bar */}
      <div className="px-5 py-2.5 bg-gray-50/80 dark:bg-slate-800/60 border-t border-gray-100 dark:border-slate-800 flex items-center space-x-6">
        <div className="h-3 w-16 bg-gray-200 dark:bg-slate-700 rounded animate-pulse" />
        <div className="h-3 w-16 bg-gray-200 dark:bg-slate-700 rounded animate-pulse" />
        <div className="h-3 w-20 bg-gray-200 dark:bg-slate-700 rounded animate-pulse" />
        <div className="h-3 w-18 bg-gray-200 dark:bg-slate-700 rounded animate-pulse" />
      </div>
    </div>
  );
}
