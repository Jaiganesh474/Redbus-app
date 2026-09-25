"use client";

import React, { useState, useEffect, useRef } from "react";
import { useSearchCitiesWithAiQuery } from "@/store/apiSlice";
import type { AiCityItem } from "@/types";
import { MapPin, Sparkles, Check, Search, Bus, ChevronDown, X } from "lucide-react";

interface AiCityDropdownProps {
  value: string;
  onChange: (city: string) => void;
  excludeCity?: string;
  placeholder?: string;
  label?: string;
  icon?: React.ReactNode;
  required?: boolean;
  className?: string;
}

export default function AiCityDropdown({
  value,
  onChange,
  excludeCity = "",
  placeholder = "Search city with AI...",
  label,
  icon,
  required = false,
  className = "",
}: AiCityDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const dropdownRef = useRef<HTMLDivElement>(null);

  // AI Live City Search with Debouncing
  const { data: cities = [], isFetching } = useSearchCitiesWithAiQuery(
    { query: searchTerm, exclude: excludeCity },
    { skip: !isOpen && !searchTerm }
  );

  // Close on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelect = (cityName: string) => {
    onChange(cityName);
    setSearchTerm("");
    setIsOpen(false);
  };

  return (
    <div ref={dropdownRef} className={`relative ${className}`}>
      {label && (
        <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1 flex items-center justify-between">
          <span className="flex items-center space-x-1">
            {icon || <MapPin className="w-3.5 h-3.5 text-[#d84e55]" />}
            <span>{label}</span>
          </span>
          <span className="text-[10px] text-red-600 dark:text-red-400 font-semibold flex items-center gap-0.5">
            <Sparkles className="w-3 h-3 text-amber-500 animate-pulse" />
            <span>AI Select</span>
          </span>
        </label>
      )}

      {/* Selected City Display Button */}
      <div
        onClick={() => setIsOpen(!isOpen)}
        className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-slate-800 hover:bg-gray-100/80 dark:hover:bg-slate-700/80 rounded-xl border border-gray-200 dark:border-slate-700 text-xs font-bold text-gray-900 dark:text-white flex items-center justify-between cursor-pointer transition-all shadow-2xs hover:border-[#d84e55]/50 focus-within:ring-2 focus-within:ring-[#d84e55]"
      >
        <div className="flex items-center space-x-2 truncate">
          {icon || <MapPin className="w-4 h-4 text-[#d84e55] shrink-0" />}
          <span className={value ? "text-gray-900 dark:text-white" : "text-gray-400 font-normal"}>
            {value || placeholder}
          </span>
        </div>

        <div className="flex items-center space-x-1 text-gray-400 shrink-0">
          {value && (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                onChange("");
                setSearchTerm("");
              }}
              className="p-1 hover:text-red-500 rounded-full hover:bg-gray-200 dark:hover:bg-slate-600"
            >
              <X className="w-3 h-3" />
            </button>
          )}
          <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
        </div>
      </div>

      {/* Hidden input for HTML form validation */}
      {required && (
        <input
          type="text"
          value={value}
          required={required}
          onChange={() => {}}
          className="sr-only"
          tabIndex={-1}
        />
      )}

      {/* AI Search Dropdown Popover */}
      {isOpen && (
        <div className="absolute left-0 right-0 top-full mt-2 z-50 bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-gray-100 dark:border-slate-800 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-150 max-h-80 flex flex-col">
          {/* AI Search Input Header */}
          <div className="p-3 border-b border-gray-100 dark:border-slate-800 bg-red-50/40 dark:bg-slate-800/60">
            <div className="relative flex items-center">
              <Sparkles className="w-3.5 h-3.5 text-amber-500 absolute left-3 pointer-events-none animate-pulse" />
              <input
                type="text"
                autoFocus
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    if (cities.length > 0) {
                      handleSelect(cities[0].name);
                    } else if (searchTerm.trim()) {
                      handleSelect(searchTerm.trim());
                    }
                  }
                }}
                placeholder="Type city or airport code (e.g. BLR, Dindigul)..."
                className="w-full pl-8 pr-8 py-2 bg-white dark:bg-slate-900 rounded-xl border border-red-200 dark:border-slate-700 text-xs font-semibold text-gray-900 dark:text-white placeholder-gray-400 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
              />
              {searchTerm && (
                <button
                  type="button"
                  onClick={() => setSearchTerm("")}
                  className="absolute right-2 text-gray-400 hover:text-gray-600 p-1"
                >
                  <X className="w-3 h-3" />
                </button>
              )}
            </div>

            <div className="flex items-center justify-between text-[10px] text-gray-500 dark:text-slate-400 mt-1.5 px-1 font-medium">
              <span>AI City Discovery</span>
              {excludeCity && (
                <span className="text-amber-600 dark:text-amber-400">
                  Excluding: <strong>{excludeCity}</strong>
                </span>
              )}
            </div>
          </div>

          {/* City Options List */}
          <div className="overflow-y-auto p-1.5 space-y-1 max-h-60 divide-y divide-gray-50 dark:divide-slate-800/40">
            {searchTerm.trim().length >= 2 && !cities.some((c: AiCityItem) => c.name.toLowerCase() === searchTerm.trim().toLowerCase()) && (
              <div
                onClick={() => handleSelect(searchTerm.trim())}
                className="p-2.5 rounded-xl bg-red-50/70 dark:bg-red-950/40 hover:bg-red-100/80 text-[#d84e55] font-bold border border-red-200 dark:border-red-900/60 flex items-center justify-between cursor-pointer transition-all"
              >
                <div className="flex items-center space-x-2 truncate">
                  <Sparkles className="w-3.5 h-3.5 text-amber-500 animate-pulse shrink-0" />
                  <div className="truncate">
                    <span className="text-xs font-bold text-gray-900 dark:text-white">
                      ✨ Select &quot;{searchTerm.trim()}&quot; with AI
                    </span>
                    <span className="text-[10px] text-gray-400 block font-normal">
                      Instant AI Transit Discovery
                    </span>
                  </div>
                </div>
                <span className="text-[9px] bg-[#d84e55] text-white px-2 py-0.5 rounded-full uppercase tracking-wider shrink-0 font-bold">
                  AI Pick
                </span>
              </div>
            )}

            {isFetching ? (
              <div className="p-6 text-center space-y-2">
                <div className="w-5 h-5 border-2 border-[#d84e55] border-t-transparent rounded-full animate-spin mx-auto" />
                <p className="text-xs font-bold text-gray-700 dark:text-slate-300">✨ AI Analyzing Transit Cities...</p>
                <p className="text-[10px] text-gray-400">Filtering operator schedules & routes</p>
              </div>
            ) : cities.length > 0 ? (
              cities.map((city: AiCityItem) => {
                const isSelected = value.toLowerCase() === city.name.toLowerCase();
                return (
                  <div
                    key={city.name}
                    onClick={() => handleSelect(city.name)}
                    className={`p-2.5 rounded-xl flex items-center justify-between cursor-pointer transition-all ${
                      isSelected
                        ? "bg-red-50 dark:bg-red-950/40 text-[#d84e55] font-bold border border-red-200 dark:border-red-900/60"
                        : "hover:bg-gray-50 dark:hover:bg-slate-800 text-gray-800 dark:text-slate-200"
                    }`}
                  >
                    <div className="flex items-center space-x-2.5 truncate">
                      <div
                        className={`p-1.5 rounded-lg shrink-0 ${
                          city.activeRoutesCount > 0
                            ? "bg-emerald-50 dark:bg-emerald-950 text-emerald-600"
                            : "bg-red-50 dark:bg-slate-800 text-[#d84e55]"
                        }`}
                      >
                        <Bus className="w-3.5 h-3.5" />
                      </div>

                      <div className="truncate">
                        <div className="flex items-center space-x-2">
                          <span className="text-xs font-bold text-gray-900 dark:text-white">
                            {city.name}
                          </span>
                          <span className="text-[10px] text-gray-400 dark:text-slate-400">
                            {city.state}
                          </span>
                        </div>

                        <div className="flex items-center gap-1.5 mt-0.5">
                          <span
                            className={`text-[9px] px-1.5 py-0.2 rounded-md font-semibold ${
                              city.activeRoutesCount > 0
                                ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300"
                                : "bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-slate-400"
                            }`}
                          >
                            {city.tag}
                          </span>

                          {city.aliases && city.aliases.length > 0 && (
                            <span className="text-[9px] text-gray-400 truncate">
                              • {city.aliases.slice(0, 2).join(", ")}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    {isSelected && <Check className="w-4 h-4 text-[#d84e55] shrink-0" />}
                  </div>
                );
              })
            ) : (
              <div className="p-6 text-center space-y-1">
                <p className="text-xs font-bold text-gray-700 dark:text-slate-300">
                  No cities found matching &quot;{searchTerm}&quot;
                </p>
                <p className="text-[10px] text-gray-400">
                  Try searching for another Indian city or transit code
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
