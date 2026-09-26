"use client";

import React, { useState, useEffect, useRef } from "react";
import { useSearchCitiesWithAiQuery } from "@/store/apiSlice";
import type { AiCityItem } from "@/types";
import { MapPin, Check, Bus, ChevronDown, X } from "lucide-react";

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
  placeholder = "Enter city name...",
  label,
  icon,
  required = false,
  className = "",
}: AiCityDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState(value || "");
  const dropdownRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Sync external value with local input
  useEffect(() => {
    setInputValue(value || "");
  }, [value]);

  // AI Live City Search with Debouncing
  const { data: cities = [], isFetching } = useSearchCitiesWithAiQuery(
    { query: inputValue, exclude: excludeCity },
    { skip: !isOpen }
  );

  // Close on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        // If user typed something and clicked away, keep it or revert to value
        if (inputValue.trim()) {
          onChange(inputValue.trim());
        }
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [inputValue, onChange]);

  const handleSelect = (cityName: string) => {
    setInputValue(cityName);
    onChange(cityName);
    setIsOpen(false);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newVal = e.target.value;
    setInputValue(newVal);
    onChange(newVal);
    if (!isOpen) {
      setIsOpen(true);
    }
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    setInputValue("");
    onChange("");
    setIsOpen(true);
    inputRef.current?.focus();
  };

  return (
    <div ref={dropdownRef} className={`relative ${className}`}>
      {label && (
        <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1 flex items-center space-x-1">
          {icon || <MapPin className="w-3.5 h-3.5 text-[#d84e55]" />}
          <span>{label}</span>
        </label>
      )}

      {/* Direct Searchable Input Box */}
      <div
        onClick={() => {
          setIsOpen(true);
          inputRef.current?.focus();
        }}
        className={`w-full px-3.5 py-2.5 bg-gray-50 dark:bg-slate-800 hover:bg-gray-100/80 dark:hover:bg-slate-700/80 rounded-xl border ${
          isOpen
            ? "border-[#d84e55] ring-2 ring-[#d84e55]/20 bg-white dark:bg-slate-900"
            : "border-gray-200 dark:border-slate-700"
        } text-xs font-bold text-gray-900 dark:text-white flex items-center justify-between cursor-text transition-all shadow-2xs`}
      >
        <div className="flex items-center space-x-2 flex-1 min-w-0 pr-2">
          {icon || <MapPin className="w-4 h-4 text-[#d84e55] shrink-0" />}
          <input
            ref={inputRef}
            type="text"
            required={required}
            value={inputValue}
            onChange={handleInputChange}
            onFocus={() => setIsOpen(true)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                if (cities.length > 0) {
                  handleSelect(cities[0].name);
                } else if (inputValue.trim()) {
                  handleSelect(inputValue.trim());
                }
              } else if (e.key === "Escape") {
                setIsOpen(false);
              }
            }}
            placeholder={placeholder}
            className="w-full bg-transparent border-none outline-hidden text-xs font-bold text-gray-900 dark:text-white placeholder-gray-400 focus:outline-hidden"
          />
        </div>

        <div className="flex items-center space-x-1 text-gray-400 shrink-0">
          {inputValue && (
            <button
              type="button"
              onClick={handleClear}
              className="p-1 hover:text-red-500 rounded-full hover:bg-gray-200 dark:hover:bg-slate-600 transition-colors cursor-pointer"
              title="Clear city"
            >
              <X className="w-3 h-3" />
            </button>
          )}
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setIsOpen(!isOpen);
            }}
            className="p-0.5 hover:text-gray-600 cursor-pointer"
          >
            <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
          </button>
        </div>
      </div>

      {/* Clean City Suggestions Dropdown */}
      {isOpen && (
        <div className="absolute left-0 right-0 top-full mt-1.5 z-50 bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-gray-100 dark:border-slate-800 overflow-hidden animate-in fade-in slide-in-from-top-1 duration-150 max-h-72 flex flex-col">
          {/* City Options List */}
          <div className="overflow-y-auto p-1.5 space-y-0.5 max-h-68 divide-y divide-gray-50 dark:divide-slate-800/40">
            {isFetching && cities.length === 0 ? (
              <div className="p-4 text-center space-y-1.5">
                <div className="w-4 h-4 border-2 border-[#d84e55] border-t-transparent rounded-full animate-spin mx-auto" />
                <p className="text-xs font-semibold text-gray-600 dark:text-slate-300">Searching routes & cities...</p>
              </div>
            ) : cities.length > 0 ? (
              cities.map((city: AiCityItem) => {
                const isSelected = value.toLowerCase() === city.name.toLowerCase();
                return (
                  <div
                    key={city.name}
                    onMouseDown={(e) => {
                      e.preventDefault(); // prevent input blur before select
                      handleSelect(city.name);
                    }}
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
              <div className="p-4 text-center space-y-1">
                <p className="text-xs font-semibold text-gray-700 dark:text-slate-300">
                  No cities found matching &quot;{inputValue}&quot;
                </p>
                <p className="text-[10px] text-gray-400">
                  Select or type any Indian city name
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
