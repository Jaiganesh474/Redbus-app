"use client";

import React, { useState, useEffect, useRef } from "react";
import { Bus, MapPin, Check, Sparkles, X, ChevronDown } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const POPULAR_INDIAN_CITIES = [
  "Bangalore",
  "Chennai",
  "Hyderabad",
  "Mumbai",
  "Pune",
  "Delhi",
  "Coimbatore",
  "Jaipur",
  "Madurai",
  "Tirupati",
  "Mysore",
  "Kochi",
  "Goa",
  "Vijayawada",
  "Ahmedabad",
];

interface CityAutocompleteInputProps {
  label: "From" | "To";
  value: string;
  onChange: (city: string) => void;
  placeholder?: string;
  excludeCity?: string;
  className?: string;
}

export default function CityAutocompleteInput({
  label,
  value,
  onChange,
  placeholder,
  excludeCity = "",
  className = "",
}: CityAutocompleteInputProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);

  // Filter cities based on user search and exclude the counterpart city
  const filteredCities = POPULAR_INDIAN_CITIES.filter((c) => {
    if (excludeCity && c.toLowerCase() === excludeCity.toLowerCase()) return false;
    if (!searchTerm.trim()) return true;
    return c.toLowerCase().includes(searchTerm.toLowerCase().trim());
  });

  // Close when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelectCity = (city: string) => {
    onChange(city);
    setSearchTerm("");
    setIsOpen(false);
  };

  return (
    <div ref={containerRef} className={`relative flex-1 min-w-0 ${className}`}>
      <div
        className="flex items-center space-x-2 w-full bg-white dark:bg-slate-800/90 border border-gray-200 dark:border-slate-700 hover:border-[#d84e55]/50 focus-within:border-[#d84e55] focus-within:ring-2 focus-within:ring-[#d84e55]/20 rounded-xl px-3 py-1.5 transition-all cursor-text"
        onClick={() => setIsOpen(true)}
      >
        <MapPin
          className={`w-3.5 h-3.5 shrink-0 ${
            label === "From" ? "text-emerald-500" : "text-[#d84e55]"
          }`}
        />
        <div className="flex-1 min-w-0">
          <span className="block text-[9px] uppercase font-extrabold text-gray-400 dark:text-slate-400 tracking-wider leading-none">
            {label}
          </span>
          <input
            type="text"
            value={isOpen ? searchTerm || value : value}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              onChange(e.target.value);
              if (!isOpen) setIsOpen(true);
            }}
            onFocus={() => setIsOpen(true)}
            placeholder={placeholder || (label === "From" ? "Source City" : "Destination City")}
            className="w-full bg-transparent font-black text-xs sm:text-sm text-gray-900 dark:text-white placeholder-gray-400 focus:outline-hidden truncate leading-tight"
          />
        </div>
        {value && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onChange("");
              setSearchTerm("");
            }}
            className="p-1 rounded-full text-gray-400 hover:text-gray-600 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-slate-700 shrink-0"
            title="Clear"
          >
            <X className="w-3 h-3" />
          </button>
        )}
      </div>

      {/* Dropdown with Popular Cities and Matching Results */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 4, scale: 0.98 }}
            transition={{ duration: 0.15 }}
            className="absolute left-0 right-0 top-full mt-1.5 z-50 bg-white dark:bg-[#0f172a] rounded-2xl shadow-xl border border-gray-200 dark:border-slate-800 p-3 max-h-72 overflow-y-auto"
          >
            <div className="flex items-center justify-between pb-2 border-b border-gray-100 dark:border-slate-800 text-[11px] font-bold text-gray-500 dark:text-slate-400">
              <span className="flex items-center space-x-1">
                <Sparkles className="w-3 h-3 text-[#d84e55]" />
                <span>Popular Cities</span>
              </span>
              <span className="text-[10px] text-gray-400">Select {label.toLowerCase()}</span>
            </div>

            {/* Quick Select City Chips */}
            <div className="flex flex-wrap gap-1.5 py-2">
              {POPULAR_INDIAN_CITIES.slice(0, 8).map((city) => {
                const isSelected = value.toLowerCase() === city.toLowerCase();
                return (
                  <button
                    key={city}
                    type="button"
                    onClick={() => handleSelectCity(city)}
                    className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                      isSelected
                        ? "bg-[#d84e55] text-white shadow-2xs"
                        : "bg-gray-100 dark:bg-slate-800 hover:bg-gray-200 dark:hover:bg-slate-700 text-gray-700 dark:text-slate-300"
                    }`}
                  >
                    {city}
                  </button>
                );
              })}
            </div>

            {/* Full List with Matching Highlights */}
            <div className="pt-1 space-y-0.5 border-t border-gray-100 dark:border-slate-800">
              {filteredCities.length === 0 ? (
                <div className="py-3 text-center text-xs text-gray-400">
                  No cities matching &quot;{searchTerm}&quot;
                </div>
              ) : (
                filteredCities.map((city) => {
                  const isSelected = value.toLowerCase() === city.toLowerCase();
                  return (
                    <button
                      key={city}
                      type="button"
                      onClick={() => handleSelectCity(city)}
                      className={`w-full text-left px-2.5 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                        isSelected
                          ? "bg-red-50 dark:bg-red-950/40 text-[#d84e55]"
                          : "hover:bg-gray-50 dark:hover:bg-slate-800/60 text-gray-800 dark:text-slate-200"
                      }`}
                    >
                      <div className="flex items-center space-x-2">
                        <Bus className="w-3.5 h-3.5 text-gray-400" />
                        <span>{city}</span>
                      </div>
                      {isSelected && <Check className="w-3.5 h-3.5 text-[#d84e55]" />}
                    </button>
                  );
                })
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
