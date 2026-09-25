"use client";

import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, ChevronLeft, ChevronRight } from "lucide-react";

interface SelectDateModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedDate: string; // YYYY-MM-DD
  onSelectDate: (dateStr: string) => void;
}

const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December"
];

const WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

export default function SelectDateModal({
  isOpen,
  onClose,
  selectedDate,
  onSelectDate,
}: SelectDateModalProps) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const initialDate = selectedDate ? new Date(selectedDate) : today;
  const [currentYear, setCurrentYear] = useState<number>(
    isNaN(initialDate.getTime()) ? today.getFullYear() : initialDate.getFullYear()
  );
  const [currentMonth, setCurrentMonth] = useState<number>(
    isNaN(initialDate.getTime()) ? today.getMonth() : initialDate.getMonth()
  );

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

  const handlePrevMonth = () => {
    if (currentMonth === 0) {
      setCurrentMonth(11);
      setCurrentYear((prev) => prev - 1);
    } else {
      setCurrentMonth((prev) => prev - 1);
    }
  };

  const handleNextMonth = () => {
    if (currentMonth === 11) {
      setCurrentMonth(0);
      setCurrentYear((prev) => prev + 1);
    } else {
      setCurrentMonth((prev) => prev + 1);
    }
  };

  // Days in current month
  const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();

  // First day of current month (0 = Sun, 1 = Mon ... 6 = Sat)
  const firstDayRaw = new Date(currentYear, currentMonth, 1).getDay();
  // Adjust so Monday = 0, Sunday = 6
  const firstDayMondayBased = (firstDayRaw + 6) % 7;

  // Selected date components
  const selDateObj = selectedDate ? new Date(selectedDate) : null;
  const isSelected = (day: number) => {
    if (!selDateObj || isNaN(selDateObj.getTime())) return false;
    return (
      selDateObj.getFullYear() === currentYear &&
      selDateObj.getMonth() === currentMonth &&
      selDateObj.getDate() === day
    );
  };

  const isPast = (day: number) => {
    const d = new Date(currentYear, currentMonth, day);
    d.setHours(0, 0, 0, 0);
    return d < today;
  };

  const handleDayClick = (day: number) => {
    if (isPast(day)) return;
    const mStr = String(currentMonth + 1).padStart(2, "0");
    const dStr = String(day).padStart(2, "0");
    const formatted = `${currentYear}-${mStr}-${dStr}`;
    onSelectDate(formatted);
    onClose();
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

          {/* Modal Container matching Image 4 */}
          <motion.div
            initial={{ y: "100%", opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: "100%", opacity: 0 }}
            transition={{ type: "spring", damping: 28, stiffness: 300 }}
            className="relative z-10 w-full sm:max-w-md bg-white dark:bg-[#0f172a] rounded-t-3xl sm:rounded-3xl shadow-2xl overflow-hidden border border-gray-100 dark:border-slate-800 p-5 space-y-4"
          >
            {/* Header: "Select date" + Circular Close Button */}
            <div className="flex items-center justify-between pb-2 border-b border-gray-100 dark:border-slate-800">
              <h3 className="font-extrabold text-base sm:text-lg text-gray-900 dark:text-white">
                Select date
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

            {/* Weekdays Strip */}
            <div className="grid grid-cols-7 text-center text-xs font-semibold text-gray-500 dark:text-slate-400 py-1">
              {WEEKDAYS.map((wd) => (
                <div key={wd} className="py-1">
                  {wd}
                </div>
              ))}
            </div>

            {/* Month & Year Navigation: ← September 2026 → */}
            <div className="flex items-center justify-between px-2 pt-1 pb-2">
              <button
                type="button"
                onClick={handlePrevMonth}
                className="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 text-gray-600 dark:text-slate-300 transition-colors cursor-pointer"
                title="Previous month"
              >
                <ChevronLeft className="w-5 h-5" />
              </button>
              <span className="font-black text-sm sm:text-base text-gray-900 dark:text-white">
                {MONTH_NAMES[currentMonth]} {currentYear}
              </span>
              <button
                type="button"
                onClick={handleNextMonth}
                className="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 text-gray-600 dark:text-slate-300 transition-colors cursor-pointer"
                title="Next month"
              >
                <ChevronRight className="w-5 h-5" />
              </button>
            </div>

            {/* Days Grid */}
            <div className="grid grid-cols-7 gap-y-2 gap-x-1 text-center py-2">
              {/* Empty leading days */}
              {Array.from({ length: firstDayMondayBased }).map((_, i) => (
                <div key={`empty-${i}`} className="w-8 h-8 sm:w-9 sm:h-9 mx-auto" />
              ))}

              {/* Days of Month */}
              {Array.from({ length: daysInMonth }).map((_, i) => {
                const day = i + 1;
                const past = isPast(day);
                const selected = isSelected(day);
                // Day of week (0 = Monday, 6 = Sunday)
                const dayOfWeek = (firstDayMondayBased + i) % 7;
                const isSunday = dayOfWeek === 6;

                return (
                  <button
                    key={day}
                    type="button"
                    disabled={past}
                    onClick={() => handleDayClick(day)}
                    className={`w-8 h-8 sm:w-9 sm:h-9 mx-auto rounded-full flex items-center justify-center text-xs sm:text-sm transition-all select-none ${
                      past
                        ? "text-gray-300 dark:text-slate-700 cursor-not-allowed"
                        : selected
                        ? "bg-[#1e293b] dark:bg-white text-white dark:text-gray-900 font-extrabold shadow-md scale-105"
                        : isSunday
                        ? "text-red-600 dark:text-red-400 font-bold hover:bg-red-50 dark:hover:bg-red-950/40 cursor-pointer"
                        : "text-gray-800 dark:text-slate-200 font-semibold hover:bg-gray-100 dark:hover:bg-slate-800 cursor-pointer"
                    }`}
                  >
                    {day}
                  </button>
                );
              })}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
