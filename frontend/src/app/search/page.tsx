"use client";

import React, { useState, useMemo, useEffect, useRef, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { useAppDispatch, useAppSelector } from "@/store";
import {
  setBusTypeFilter,
  setDepartureWindowFilter,
  setPriceRangeFilter,
  setSortBy,
  resetFilters,
  setSourceCity,
  setDestinationCity,
  setTravelDate,
} from "@/store/searchSlice";
import { toggleChat } from "@/store/chatSlice";
import { useSearchRoutesQuery } from "@/store/apiSlice";
import { motion, AnimatePresence } from "framer-motion";
import SelectDateModal from "@/components/SelectDateModal";
import SortAndFilterModal from "@/components/SortAndFilterModal";
import AutotypingPlaceholder from "@/components/AutotypingPlaceholder";
import CityAutocompleteInput from "@/components/CityAutocompleteInput";
import BusCard from "@/components/BusCard";
import BusCardSkeleton from "@/components/BusCardSkeleton";
import type { RouteItem } from "@/types";
import {
  ArrowLeft,
  ArrowLeftRight,
  Bus,
  Calendar,
  Search,
  Sparkles,
  Mic,
  MicOff,
  Star,
  ShieldCheck,
  BedDouble,
  Armchair,
  Check,
  X,
  RotateCcw,
  SlidersHorizontal,
  Sun,
  Sunset,
  Moon,
  Zap,
  Coffee,
  Clock,
  ChevronRight,
  Flame,
  Tag,
} from "lucide-react";

function SearchResultsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const dispatch = useAppDispatch();
  const filters = useAppSelector((state) => state.search);

  // Search parameters from URL or Redux
  const source = searchParams.get("source") || filters.sourceCity || "Bengaluru";
  const destination = searchParams.get("destination") || filters.destinationCity || "Chennai";
  const date = searchParams.get("date") || filters.travelDate || new Date().toISOString().split("T")[0];
  const busType = searchParams.get("busType") || filters.busType;
  const maxPriceParam = searchParams.get("maxPrice") ? Number(searchParams.get("maxPrice")) : filters.maxPrice;
  const selectRouteParam = searchParams.get("selectRoute");

  // Local state for editable top search bar
  const [editingSource, setEditingSource] = useState(source);
  const [editingDestination, setEditingDestination] = useState(destination);
  const [editingDate, setEditingDate] = useState(date);

  useEffect(() => {
    setEditingSource(source);
    setEditingDestination(destination);
    setEditingDate(date);
  }, [source, destination, date]);

  // AI Smart Filter States
  const [aiFilterQuery, setAiFilterQuery] = useState("");
  const [aiAppliedQuery, setAiAppliedQuery] = useState("");
  const [isListening, setIsListening] = useState(false);
  const [activeAiTag, setActiveAiTag] = useState<string | null>(null);

  const handleTriggerAiSearch = (customQuery?: string) => {
    const q = (typeof customQuery === "string" ? customQuery : aiFilterQuery).trim();
    if (!q) return;
    setAiAppliedQuery(q);
    setActiveAiTag(q);
    setIsMinLoading(true);
    setTimeout(() => {
      setIsMinLoading(false);
    }, 600);
  };

  const clearAiFilter = () => {
    setAiFilterQuery("");
    setAiAppliedQuery("");
    setActiveAiTag(null);
    setIsMinLoading(true);
    setTimeout(() => {
      setIsMinLoading(false);
    }, 400);
  };

  const aiFilterSummary = useMemo(() => {
    const q = (aiAppliedQuery || aiFilterQuery || "").toLowerCase().trim();
    if (!q) return "";

    // Time-based summaries
    if (q.includes("morning") || q.includes("early morning")) return "Showing Morning buses (5:00 AM - 12:00 PM)";
    if (q.includes("afternoon") || q.includes("noon")) return "Showing Afternoon buses (12:00 PM - 5:00 PM)";
    if (q.includes("evening")) return "Showing Evening buses (5:00 PM - 9:00 PM)";
    if (q.includes("night") || q.includes("overnight") || q.includes("late night")) return "Showing Night buses (8:00 PM - 5:00 AM)";

    // Time bounds
    const beforeMatch = q.match(/before\s*(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)/i);
    if (beforeMatch) return `Showing buses departing before ${beforeMatch[1].toUpperCase()}`;
    const afterMatch = q.match(/after\s*(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)/i);
    if (afterMatch) return `Showing buses departing after ${afterMatch[1].toUpperCase()}`;

    // Price bounds
    const priceMatch = q.match(/(?:under|below|less than|<|upto)\s*₹?\s*(\d+)/i);
    if (priceMatch) return `Showing buses under ₹${priceMatch[1]}`;

    // Bus types
    if (q.includes("sleeper")) return "Showing AC & Non-AC Sleeper buses";
    if (q.includes("seater")) return "Showing Seater buses";
    if (q.includes("volvo")) return "Showing Volvo Multi-Axle buses";
    if (q.includes("ac")) return "Showing AC buses";
    if (q.includes("primo") || q.includes("top rated")) return "Showing Primo Top-Rated buses (4.4★+)";

    return `Showing results for "${aiAppliedQuery || aiFilterQuery}"`;
  }, [aiAppliedQuery, aiFilterQuery]);

  // Quick Filter Pill States (matching Screenshot Photo 3)
  const [isPrimoFilter, setIsPrimoFilter] = useState(false);
  const [isFreeCancellationFilter, setIsFreeCancellationFilter] = useState(false);
  const [isAcFilter, setIsAcFilter] = useState(false);
  const [isSleeperFilter, setIsSleeperFilter] = useState(false);
  const [isSingleSeatFilter, setIsSingleSeatFilter] = useState(false);
  const [isSeaterFilter, setIsSeaterFilter] = useState(false);
  const [isNonAcFilter, setIsNonAcFilter] = useState(false);

  // Active sort tab
  const [activeSort, setActiveSort] = useState<"ratings" | "departure" | "price" | "price_desc" | "departure_desc">("ratings");

  // Modal Dialog states
  const [showFilterModal, setShowFilterModal] = useState(false);
  const [showDateModal, setShowDateModal] = useState(false);
  const [showMobileSearchEdit, setShowMobileSearchEdit] = useState(false);
  const [mobileVehicleTab, setMobileVehicleTab] = useState<"bus" | "train">("bus");

  // Filter Arrays for SortAndFilterModal
  const [selectedDepartureWindows, setSelectedDepartureWindows] = useState<string[]>([]);
  const [selectedBusTypes, setSelectedBusTypes] = useState<string[]>([]);
  const [selectedBoardingPoints, setSelectedBoardingPoints] = useState<string[]>([]);
  const [selectedDroppingPoints, setSelectedDroppingPoints] = useState<string[]>([]);
  const [selectedOperators, setSelectedOperators] = useState<string[]>([]);
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([]);


  // Mobile date display matching Image 3 (e.g. "20 Sep" / "Sun")
  const mobileDateInfo = useMemo(() => {
    try {
      const parts = editingDate.split("-");
      if (parts.length === 3) {
        const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        const day = d.getDate();
        const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        const dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
        return {
          dayMonth: `${day} ${monthNames[d.getMonth()]}`,
          dayOfWeek: dayNames[d.getDay()],
        };
      }
    } catch {}
    return { dayMonth: editingDate, dayOfWeek: "" };
  }, [editingDate]);

  // Fetch routes from backend
  const { data: rawRoutes = [], isLoading, isError, refetch } = useSearchRoutesQuery({
    source,
    destination,
    date,
    busType: busType || undefined,
    minPrice: filters.minPrice,
    maxPrice: maxPriceParam || undefined,
    departureWindow: filters.departureWindow !== "ALL" ? filters.departureWindow : undefined,
    sortBy: filters.sortBy,
  });

  // Realistic 1-second loading timer so search looks authentic
  const [isMinLoading, setIsMinLoading] = useState(true);
  useEffect(() => {
    setIsMinLoading(true);
    const timer = setTimeout(() => {
      setIsMinLoading(false);
    }, 1000); // 1-second delay
    return () => clearTimeout(timer);
  }, [source, destination, date, busType, filters.sortBy, activeSort]);

  const isSearching = isLoading || isMinLoading;

  // Calculate Today and Tomorrow dates for quick pills
  const todayStr = useMemo(() => new Date().toISOString().split("T")[0], []);
  const tomorrowStr = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().split("T")[0];
  }, []);

  const isToday = editingDate === todayStr;
  const isTomorrow = editingDate === tomorrowStr;

  // Format date display (e.g. "20 Sep, 2026 (Today)")
  const formattedJourneyDate = useMemo(() => {
    try {
      const parts = editingDate.split("-");
      if (parts.length === 3) {
        const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        const day = d.getDate();
        const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        const month = monthNames[d.getMonth()];
        const year = d.getFullYear();
        let suffix = "";
        if (isToday) suffix = " (Today)";
        else if (isTomorrow) suffix = " (Tomorrow)";
        return `${day} ${month}, ${year}${suffix}`;
      }
    } catch {}
    return editingDate;
  }, [editingDate, isToday, isTomorrow]);

  // Handle Search Submission from top bar
  const handleTopSearchSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    dispatch(setSourceCity(editingSource));
    dispatch(setDestinationCity(editingDestination));
    dispatch(setTravelDate(editingDate));
    router.push(
      `/search?source=${encodeURIComponent(editingSource)}&destination=${encodeURIComponent(
        editingDestination
      )}&date=${encodeURIComponent(editingDate)}`
    );
  };

  // Swap Source and Destination
  const handleSwapCities = () => {
    const temp = editingSource;
    setEditingSource(editingDestination);
    setEditingDestination(temp);
  };

  // Web Speech API for AI Smart Filter voice query
  const handleAiVoiceInput = () => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      alert("Voice recognition is not supported in this browser. Please use Chrome or Edge.");
      return;
    }

    if (isListening) {
      setIsListening(false);
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = false;
      recognition.lang = "en-IN";

      recognition.onstart = () => {
        setIsListening(true);
      };

      recognition.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        if (transcript) {
          setAiFilterQuery(transcript);
          setActiveAiTag(transcript);
        }
        setIsListening(false);
      };

      recognition.onerror = () => {
        setIsListening(false);
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognition.start();
    } catch (err) {
      console.error(err);
      setIsListening(false);
    }
  };

  // AI NLP keyword parser applied to routes
  const filteredAndSortedRoutes = useMemo(() => {
    let result = [...rawRoutes];

    // 1. AI Smart Filter text/voice processing
    const aiQuery = (aiAppliedQuery || aiFilterQuery || "").toLowerCase().trim();
    if (aiQuery) {
      // 1a. Operator matching: Check if user specifically requested an operator
      const matchedRouteOp = rawRoutes.find((r) =>
        aiQuery.includes(r.operatorName.toLowerCase())
      );
      if (matchedRouteOp) {
        result = result.filter((r) =>
          r.operatorName.toLowerCase().includes(matchedRouteOp.operatorName.toLowerCase())
        );
      } else {
        const knownOperators = ["intercity", "intrcity", "smartbus", "fresh", "zingbus", "zing", "ksrtc", "orange", "srs", "vrl", "jabbar", "kallada", "greenline", "parveen", "neeta", "chalo"];
        const foundKw = knownOperators.find((op) => aiQuery.includes(op));
        if (foundKw) {
          result = result.filter((r) => r.operatorName.toLowerCase().includes(foundKw));
        }
      }

      // 1b. Price intent
      const maxPriceMatch = aiQuery.match(/(?:under|below|less than|within|<|upto|up to)\s*₹?\s*(\d+)/i);
      if (maxPriceMatch) {
        const maxP = Number(maxPriceMatch[1]);
        result = result.filter((r) => r.basePrice <= maxP);
      }
      const minPriceMatch = aiQuery.match(/(?:above|more than|greater than|>)\s*₹?\s*(\d+)/i);
      if (minPriceMatch) {
        const minP = Number(minPriceMatch[1]);
        result = result.filter((r) => r.basePrice >= minP);
      }

      // 1c. Time of Day intent
      const beforeMatch = aiQuery.match(/before\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)?/i);
      const afterMatch = aiQuery.match(/after\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)?/i);

      if (beforeMatch) {
        let hour = parseInt(beforeMatch[1], 10);
        const meridiem = beforeMatch[3]?.toLowerCase();
        if (meridiem === "pm" && hour < 12) hour += 12;
        if (meridiem === "am" && hour === 12) hour = 0;
        result = result.filter((r) => {
          const busHour = parseInt(r.departureTime.split(":")[0], 10);
          return busHour < hour;
        });
      } else if (afterMatch) {
        let hour = parseInt(afterMatch[1], 10);
        const meridiem = afterMatch[3]?.toLowerCase();
        if (meridiem === "pm" && hour < 12) hour += 12;
        if (meridiem === "am" && hour === 12) hour = 0;
        result = result.filter((r) => {
          const busHour = parseInt(r.departureTime.split(":")[0], 10);
          return busHour >= hour;
        });
      } else if (aiQuery.includes("morning") || aiQuery.includes("early morning")) {
        result = result.filter((r) => {
          const hour = parseInt(r.departureTime.split(":")[0], 10);
          return hour >= 5 && hour < 12;
        });
      } else if (aiQuery.includes("afternoon") || aiQuery.includes("noon") || aiQuery.includes("day bus") || aiQuery.includes("daytime")) {
        result = result.filter((r) => {
          const hour = parseInt(r.departureTime.split(":")[0], 10);
          return hour >= 12 && hour < 17;
        });
      } else if (aiQuery.includes("evening")) {
        result = result.filter((r) => {
          const hour = parseInt(r.departureTime.split(":")[0], 10);
          return hour >= 17 && hour < 21;
        });
      } else if (aiQuery.includes("night") || aiQuery.includes("overnight") || aiQuery.includes("late night")) {
        result = result.filter((r) => {
          const hour = parseInt(r.departureTime.split(":")[0], 10);
          return hour >= 20 || hour < 5;
        });
      }

      // 1d. Bus Type & Amenities
      if (aiQuery.includes("sleeper")) {
        result = result.filter((r) => r.busType.toLowerCase().includes("sleeper"));
      } else if (aiQuery.includes("seater")) {
        result = result.filter((r) => r.busType.toLowerCase().includes("seater"));
      }

      if (aiQuery.includes("non ac") || aiQuery.includes("non-ac") || aiQuery.includes("nonac")) {
        result = result.filter((r) => r.busType.toLowerCase().includes("non ac") || r.busType.toLowerCase().includes("non-ac"));
      } else if (aiQuery.includes("ac") || aiQuery.includes("air condition") || aiQuery.includes("a/c")) {
        result = result.filter((r) => r.busType.toLowerCase().includes("ac") && !r.busType.toLowerCase().includes("non"));
      }

      if (aiQuery.includes("volvo")) {
        result = result.filter((r) => r.busType.toLowerCase().includes("volvo") || r.operatorName.toLowerCase().includes("volvo"));
      }

      if (aiQuery.includes("primo") || aiQuery.includes("top rated") || aiQuery.includes("best rating")) {
        result = result.filter((r) => r.rating >= 4.4);
      }

      if (aiQuery.includes("single seat") || aiQuery.includes("single")) {
        result = result.filter((r) => r.busType.toLowerCase().includes("2+1") || r.busType.toLowerCase().includes("sleeper"));
      }

      if (aiQuery.includes("wifi") || aiQuery.includes("wi-fi")) {
        result = result.filter((r) => (r.amenities || []).some((a) => a.toLowerCase().includes("wifi")));
      }

      if (aiQuery.includes("tracking") || aiQuery.includes("gps")) {
        result = result.filter((r) => (r.amenities || []).some((a) => a.toLowerCase().includes("tracking") || a.toLowerCase().includes("gps")));
      }
    }

    // 2. Quick Filter Pills
    if (isPrimoFilter) {
      result = result.filter((r) => r.rating >= 4.4);
    }
    if (isFreeCancellationFilter) {
      result = result.filter((r) => r.rating >= 4.0 || r.basePrice >= 600);
    }
    if (isAcFilter) {
      result = result.filter((r) => r.busType.toLowerCase().includes("ac") && !r.busType.toLowerCase().includes("non"));
    }
    if (isSleeperFilter) {
      result = result.filter((r) => r.busType.toLowerCase().includes("sleeper"));
    }
    if (isSingleSeatFilter) {
      result = result.filter((r) => r.busType.toLowerCase().includes("2+1") || r.busType.toLowerCase().includes("sleeper"));
    }
    if (isSeaterFilter) {
      result = result.filter((r) => r.busType.toLowerCase().includes("seater"));
    }
    if (isNonAcFilter) {
      result = result.filter((r) => r.busType.toLowerCase().includes("non"));
    }

    // 3. Modal Array Filters from SortAndFilterModal
    if (selectedDepartureWindows.length > 0) {
      result = result.filter((r) => {
        const hr = parseInt((r.departureTime || "00:00").split(":")[0], 10);
        return selectedDepartureWindows.some((win) => {
          if (win === "06AM_12PM") return hr >= 6 && hr < 12;
          if (win === "12PM_6PM") return hr >= 12 && hr < 18;
          if (win === "18PM_24PM") return hr >= 18 && hr < 24;
          if (win === "00AM_06AM") return hr >= 0 && hr < 6;
          return true;
        });
      });
    }

    if (selectedBusTypes.length > 0) {
      result = result.filter((r) => {
        const bt = r.busType.toUpperCase();
        return selectedBusTypes.some((t) => {
          if (t === "AC") return bt.includes("AC") && !bt.includes("NON");
          if (t === "NON_AC") return bt.includes("NON");
          if (t === "SLEEPER") return bt.includes("SLEEPER");
          if (t === "SEATER") return bt.includes("SEATER");
          return true;
        });
      });
    }

    if (selectedDroppingPoints.length > 0) {
      result = result.filter((r) =>
        (r.droppingPoints || []).some((pt) =>
          selectedDroppingPoints.some((sel) => pt.toLowerCase().includes(sel.toLowerCase()))
        )
      );
    }

    if (selectedBoardingPoints.length > 0) {
      result = result.filter((r) =>
        (r.boardingPoints || []).some((pt) =>
          selectedBoardingPoints.some((sel) => pt.toLowerCase().includes(sel.toLowerCase()))
        )
      );
    }

    if (selectedOperators.length > 0) {
      result = result.filter((r) =>
        selectedOperators.some((op) => r.operatorName.toLowerCase().includes(op.toLowerCase()))
      );
    }

    if (selectedAmenities.length > 0) {
      result = result.filter((r) =>
        selectedAmenities.every((amenity) =>
          (r.amenities || []).some((a) => a.toLowerCase().includes(amenity.toLowerCase()))
        )
      );
    }

    // 4. Sorting
    if (activeSort === "ratings") {
      result.sort((a, b) => b.rating - a.rating);
    } else if (activeSort === "departure") {
      result.sort((a, b) => a.departureTime.localeCompare(b.departureTime));
    } else if (activeSort === "departure_desc") {
      result.sort((a, b) => b.departureTime.localeCompare(a.departureTime));
    } else if (activeSort === "price") {
      result.sort((a, b) => a.basePrice - b.basePrice);
    } else if (activeSort === "price_desc") {
      result.sort((a, b) => b.basePrice - a.basePrice);
    }

    return result;
  }, [
    rawRoutes,
    aiFilterQuery,
    aiAppliedQuery,
    isPrimoFilter,
    isFreeCancellationFilter,
    isAcFilter,
    isSleeperFilter,
    isSingleSeatFilter,
    isSeaterFilter,
    isNonAcFilter,
    selectedDepartureWindows,
    selectedBusTypes,
    selectedDroppingPoints,
    selectedBoardingPoints,
    selectedOperators,
    selectedAmenities,
    activeSort,
  ]);

  // Counts for Quick Filter Pills strictly derived from real routes
  const filterCounts = useMemo(() => {
    const total = rawRoutes.length;
    const primo = rawRoutes.filter((r) => r.rating >= 4.4).length;
    const freeCancel = rawRoutes.filter((r) => r.rating >= 4.0 || r.basePrice >= 600).length;
    const ac = rawRoutes.filter((r) => r.busType.toLowerCase().includes("ac") && !r.busType.toLowerCase().includes("non")).length;
    const sleeper = rawRoutes.filter((r) => r.busType.toLowerCase().includes("sleeper")).length;
    const single = rawRoutes.filter((r) => r.busType.toLowerCase().includes("2+1") || r.busType.toLowerCase().includes("sleeper")).length;
    const seater = rawRoutes.filter((r) => r.busType.toLowerCase().includes("seater")).length;
    const nonac = rawRoutes.filter((r) => r.busType.toLowerCase().includes("non")).length;

    return {
      total,
      primo,
      freeCancel,
      ac,
      sleeper,
      single,
      seater,
      nonac,
    };
  }, [rawRoutes]);

  const clearAllFilters = () => {
    setAiFilterQuery("");
    setAiAppliedQuery("");
    setActiveAiTag(null);
    setIsPrimoFilter(false);
    setIsFreeCancellationFilter(false);
    setIsAcFilter(false);
    setIsSleeperFilter(false);
    setIsSingleSeatFilter(false);
    setIsSeaterFilter(false);
    setIsNonAcFilter(false);
    setSelectedDepartureWindows([]);
    setSelectedBusTypes([]);
    setSelectedBoardingPoints([]);
    setSelectedDroppingPoints([]);
    setSelectedOperators([]);
    setSelectedAmenities([]);
    dispatch(resetFilters());
  };

  const hasAnyFilterActive =
    !!aiFilterQuery ||
    !!aiAppliedQuery ||
    isPrimoFilter ||
    isFreeCancellationFilter ||
    isAcFilter ||
    isSleeperFilter ||
    isSingleSeatFilter ||
    isSeaterFilter ||
    isNonAcFilter ||
    filters.busType !== "" ||
    filters.departureWindow !== "ALL" ||
    filters.maxPrice !== undefined;

  return (
    <div className="min-h-screen bg-[#f7f8f9] dark:bg-[#090d16] text-gray-900 dark:text-gray-100 pb-20">
      {/* 1. Mobile Top Header matching Screenshot 3 */}
      <div className="lg:hidden bg-white dark:bg-[#0f172a] border-b border-gray-200 dark:border-slate-800 shadow-xs sticky top-0 z-30">
        <div className="px-4 py-2.5 flex items-center justify-between">
          <div className="flex items-center space-x-3 min-w-0">
            <button
              type="button"
              onClick={() => router.push("/")}
              className="p-1 -ml-1 text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800 rounded-full transition-colors cursor-pointer shrink-0"
              title="Back"
            >
              <ArrowLeft className="w-5 h-5 stroke-[2.5]" />
            </button>
            <div
              onClick={() => setShowMobileSearchEdit(!showMobileSearchEdit)}
              className="min-w-0 cursor-pointer"
            >
              <h1 className="text-sm font-black text-gray-900 dark:text-white truncate flex items-center space-x-1.5 leading-tight">
                <span>{source}</span>
                <span className="text-[#d84e55] font-bold">➔</span>
                <span>{destination}</span>
              </h1>
              <p className="text-[11px] text-gray-500 dark:text-slate-400 font-semibold">
                {filterCounts.total} buses
              </p>
            </div>
          </div>

          {/* Date Badge on Top Right */}
          <div
            onClick={() => setShowDateModal(true)}
            className="bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/50 text-[#d84e55] px-2.5 py-1 rounded-xl text-center cursor-pointer shadow-2xs leading-none shrink-0 hover:scale-105 active:scale-95 transition-all"
          >
            <div className="font-extrabold text-xs">{mobileDateInfo.dayMonth}</div>
            <div className="text-[9px] text-gray-500 dark:text-slate-400 font-semibold mt-0.5">{mobileDateInfo.dayOfWeek}</div>
          </div>
        </div>

        {/* Bus / Train Tab Toggle Bar matching Image 3 */}
        <div className="flex items-center border-t border-gray-100 dark:border-slate-800/80 text-xs font-black">
          <button
            type="button"
            onClick={() => setMobileVehicleTab("bus")}
            className={`flex-1 py-2 text-center flex items-center justify-center space-x-1.5 transition-all cursor-pointer ${
              mobileVehicleTab === "bus"
                ? "border-b-2 border-[#d84e55] text-[#d84e55]"
                : "border-b-2 border-transparent text-gray-500 dark:text-slate-400"
            }`}
          >
            <span>🚌</span>
            <span>Bus</span>
          </button>
          <button
            type="button"
            onClick={() => setMobileVehicleTab("train")}
            className={`flex-1 py-2 text-center flex items-center justify-center space-x-1.5 transition-all cursor-pointer ${
              mobileVehicleTab === "train"
                ? "border-b-2 border-[#d84e55] text-[#d84e55]"
                : "border-b-2 border-transparent text-gray-400 dark:text-slate-500"
            }`}
          >
            <span>🚆</span>
            <span>Train</span>
          </button>
        </div>

        {/* Expandable Mobile Search Editor with City Autocomplete & Media Queries */}
        {showMobileSearchEdit && (
          <form
            onSubmit={(e) => {
              handleTopSearchSubmit(e);
              setShowMobileSearchEdit(false);
            }}
            className="p-3.5 bg-white dark:bg-[#0f172a] border-t border-gray-200 dark:border-slate-800 space-y-3 shadow-xs animate-fadeIn"
          >
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 relative">
              <CityAutocompleteInput
                label="From"
                value={editingSource}
                onChange={setEditingSource}
                placeholder="Source City"
                excludeCity={editingDestination}
              />
              <div className="flex justify-center -my-1 sm:my-0 z-10">
                <button
                  type="button"
                  onClick={handleSwapCities}
                  className="w-8 h-8 rounded-full border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 hover:bg-red-50 hover:text-[#d84e55] dark:hover:bg-slate-700 text-gray-600 dark:text-slate-300 flex items-center justify-center transition-all shadow-xs shrink-0 cursor-pointer"
                  title="Swap cities"
                >
                  <ArrowLeftRight className="w-3.5 h-3.5" />
                </button>
              </div>
              <CityAutocompleteInput
                label="To"
                value={editingDestination}
                onChange={setEditingDestination}
                placeholder="Destination City"
                excludeCity={editingSource}
              />
            </div>

            <div className="flex items-center justify-between gap-2 pt-1">
              <div className="flex items-center space-x-1.5">
                <button
                  type="button"
                  onClick={() => {
                    setEditingDate(todayStr);
                    dispatch(setTravelDate(todayStr));
                  }}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                    isToday
                      ? "bg-[#d84e55] text-white shadow-2xs"
                      : "bg-gray-100 dark:bg-slate-800 text-gray-700 dark:text-slate-300 hover:bg-gray-200"
                  }`}
                >
                  Today
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setEditingDate(tomorrowStr);
                    dispatch(setTravelDate(tomorrowStr));
                  }}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                    isTomorrow
                      ? "bg-[#d84e55] text-white shadow-2xs"
                      : "bg-gray-100 dark:bg-slate-800 text-gray-700 dark:text-slate-300 hover:bg-gray-200"
                  }`}
                >
                  Tomorrow
                </button>
              </div>

              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  onClick={() => setShowDateModal(true)}
                  className="px-2.5 py-1.5 rounded-xl border border-gray-200 dark:border-slate-700 text-xs font-bold text-gray-700 dark:text-slate-300 flex items-center space-x-1 cursor-pointer hover:bg-gray-50 dark:hover:bg-slate-800"
                >
                  <Calendar className="w-3.5 h-3.5 text-[#d84e55]" />
                  <span>{mobileDateInfo.dayMonth}</span>
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-[#d84e55] hover:bg-[#c63f46] text-white rounded-xl text-xs font-black shadow-xs hover:shadow-md active:scale-95 transition-all cursor-pointer"
                >
                  Search
                </button>
              </div>
            </div>
          </form>
        )}
      </div>

      {/* 2. Desktop Top Search Header Capsule Bar */}
      <div className="hidden lg:block bg-white dark:bg-[#0f172a] border-b border-gray-200 dark:border-slate-800 shadow-xs sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5">
          {/* Back button & Route heading */}
          <div className="flex items-center space-x-3 mb-2.5">
            <button
              type="button"
              onClick={() => router.push("/")}
              className="p-1.5 hover:bg-gray-100 dark:hover:bg-slate-800 rounded-full transition-colors cursor-pointer"
              title="Back to home"
            >
              <ArrowLeft className="w-5 h-5 text-gray-700 dark:text-slate-300" />
            </button>
            <div className="flex items-baseline space-x-2">
              <h1 className="text-base sm:text-lg font-black text-gray-900 dark:text-white flex items-center space-x-2">
                <span>{source}</span>
                <span className="text-gray-400 font-normal">➔</span>
                <span>{destination}</span>
              </h1>
              <span className="text-xs font-semibold text-gray-500 dark:text-slate-400">
                {filterCounts.total} buses
              </span>
            </div>
          </div>

          {/* Floating Search Capsule Bar */}
          <form
            onSubmit={handleTopSearchSubmit}
            className="bg-white dark:bg-slate-900 border border-gray-300 dark:border-slate-700 rounded-2xl shadow-sm p-1.5 flex flex-wrap lg:flex-nowrap items-center gap-2"
          >
            {/* From City */}
            <CityAutocompleteInput
              label="From"
              value={editingSource}
              onChange={setEditingSource}
              placeholder="Source City"
              excludeCity={editingDestination}
              className="min-w-[180px]"
            />

            {/* Swap Button */}
            <button
              type="button"
              onClick={handleSwapCities}
              className="w-8 h-8 rounded-full border border-gray-200 dark:border-slate-700 hover:border-[#d84e55] hover:bg-red-50 dark:hover:bg-red-950/40 text-gray-500 hover:text-[#d84e55] flex items-center justify-center transition-all shrink-0 cursor-pointer shadow-2xs"
              title="Swap From and To cities"
            >
              <ArrowLeftRight className="w-3.5 h-3.5" />
            </button>

            {/* To City */}
            <CityAutocompleteInput
              label="To"
              value={editingDestination}
              onChange={setEditingDestination}
              placeholder="Destination City"
              excludeCity={editingSource}
              className="min-w-[180px]"
            />

            {/* Date of Journey */}
            <div
              onClick={() => setShowDateModal(true)}
              className="flex-1 min-w-[180px] px-3 py-1.5 flex items-center space-x-2.5 border-b lg:border-b-0 lg:border-r border-gray-200 dark:border-slate-800 cursor-pointer hover:bg-gray-50 dark:hover:bg-slate-800/60 rounded-xl transition-all"
            >
              <Calendar className="w-4 h-4 text-[#d84e55] shrink-0" />
              <div className="flex-1 min-w-0">
                <span className="block text-[10px] text-gray-400 uppercase font-bold tracking-wider leading-none">
                  Date of journey
                </span>
                <div className="font-bold text-xs sm:text-sm text-gray-900 dark:text-white truncate">
                  {formattedJourneyDate}
                </div>
              </div>
            </div>

            {/* Quick Date Buttons: Today & Tomorrow */}
            <div className="flex items-center space-x-1.5 px-2">
              <button
                type="button"
                onClick={() => {
                  setEditingDate(todayStr);
                  dispatch(setTravelDate(todayStr));
                  router.push(
                    `/search?source=${encodeURIComponent(editingSource)}&destination=${encodeURIComponent(
                      editingDestination
                    )}&date=${encodeURIComponent(todayStr)}`
                  );
                }}
                className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all cursor-pointer ${
                  isToday
                    ? "bg-gray-200 dark:bg-slate-700 text-gray-900 dark:text-white font-extrabold"
                    : "bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-slate-300 hover:bg-gray-200"
                }`}
              >
                Today
              </button>
              <button
                type="button"
                onClick={() => {
                  setEditingDate(tomorrowStr);
                  dispatch(setTravelDate(tomorrowStr));
                  router.push(
                    `/search?source=${encodeURIComponent(editingSource)}&destination=${encodeURIComponent(
                      editingDestination
                    )}&date=${encodeURIComponent(tomorrowStr)}`
                  );
                }}
                className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all cursor-pointer ${
                  isTomorrow
                    ? "bg-red-100 dark:bg-red-950/60 text-[#d84e55] border border-red-200 dark:border-red-800"
                    : "bg-red-50 dark:bg-red-950/30 text-rose-600 dark:text-rose-400 hover:bg-red-100"
                }`}
              >
                Tomorrow
              </button>
            </div>

            {/* Red Round Search Button */}
            <button
              type="submit"
              className="w-10 h-10 rounded-full bg-[#d84e55] hover:bg-[#b83e44] text-white flex items-center justify-center transition-all shadow-md shadow-red-500/20 active:scale-95 shrink-0 cursor-pointer ml-auto"
              title="Search buses"
            >
              <Search className="w-4 h-4 stroke-[2.5]" />
            </button>
          </form>
        </div>
      </div>

      {/* Main Content Area: Sidebar Filters (Left) + Promo Row & Bus List (Right) */}
      <div className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 pt-3 sm:pt-6">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 lg:gap-6">
          {/* ================= LEFT COLUMN: FILTERS (Desktop Only, hidden on Mobile) ================= */}
          <div className="hidden lg:block lg:col-span-3 space-y-4">
            <div className="bg-white dark:bg-[#0f172a] rounded-2xl p-4 sm:p-5 border border-gray-200 dark:border-slate-800 shadow-xs space-y-5">
              {/* Header */}
              <div className="flex items-center justify-between pb-3 border-b border-gray-100 dark:border-slate-800">
                <h2 className="font-extrabold text-base text-gray-900 dark:text-white">
                  Filter buses
                </h2>
                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={() => setShowFilterModal(true)}
                    className="text-xs text-[#d84e55] hover:underline font-bold flex items-center space-x-1 cursor-pointer"
                  >
                    <SlidersHorizontal className="w-3 h-3" />
                    <span>All Filters</span>
                  </button>
                  {hasAnyFilterActive && (
                    <button
                      type="button"
                      onClick={clearAllFilters}
                      className="text-xs text-gray-400 hover:text-gray-600 dark:hover:text-slate-300 font-bold flex items-center space-x-1 cursor-pointer"
                    >
                      <RotateCcw className="w-3 h-3" />
                      <span>Reset</span>
                    </button>
                  )}
                </div>
              </div>

              {/* 1. AI Smart Filter with Sparkles & Voice Mic */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-extrabold bg-gradient-to-r from-purple-600 to-indigo-600 bg-clip-text text-transparent uppercase tracking-wider flex items-center space-x-1">
                    <Sparkles className="w-3 h-3 text-purple-600 inline" />
                    <span>AI Smart Filter</span>
                  </span>
                  {(aiFilterQuery || aiAppliedQuery) && (
                    <button
                      type="button"
                      onClick={clearAiFilter}
                      className="text-[10px] text-gray-400 hover:text-gray-600 dark:hover:text-slate-300 font-bold cursor-pointer"
                    >
                      Clear
                    </button>
                  )}
                </div>

                <div
                  className={`relative flex items-center rounded-xl border-2 transition-all p-1.5 ${
                    isListening
                      ? "border-red-500 bg-red-50/40 dark:bg-red-950/20 ring-2 ring-red-400/30"
                      : aiFilterQuery
                      ? "border-purple-500 bg-purple-50/40 dark:bg-purple-950/30"
                      : "border-purple-200 dark:border-purple-800/80 bg-white dark:bg-[#0f172a] hover:border-purple-400"
                  }`}
                >
                  <Sparkles className="w-4 h-4 text-purple-600 dark:text-purple-400 ml-1 shrink-0 animate-pulse" />
                  {!aiFilterQuery && !isListening && (
                    <div className="absolute left-8 right-12 inset-y-0 flex items-center pointer-events-none">
                      <AutotypingPlaceholder
                        className="text-xs font-semibold text-gray-400 dark:text-slate-400"
                        cursorClassName="bg-purple-600 dark:bg-purple-400"
                      />
                    </div>
                  )}
                  <input
                    type="text"
                    value={aiFilterQuery}
                    onChange={(e) => setAiFilterQuery(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        handleTriggerAiSearch();
                      }
                    }}
                    placeholder={isListening ? "Listening... speak now" : ""}
                    className="w-full bg-transparent px-2 py-1 text-xs font-semibold text-gray-900 dark:text-white placeholder-gray-400 focus:outline-hidden relative z-10"
                  />
                  {aiFilterQuery ? (
                    <button
                      type="button"
                      onClick={clearAiFilter}
                      className="p-1 text-gray-400 hover:text-gray-700 dark:hover:text-white shrink-0 cursor-pointer relative z-20"
                      title="Clear search"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={handleAiVoiceInput}
                      className={`p-1.5 rounded-lg transition-all shrink-0 cursor-pointer relative z-20 ${
                        isListening
                          ? "bg-red-500 text-white animate-pulse"
                          : "text-purple-600 dark:text-purple-400 hover:bg-purple-100 dark:hover:bg-purple-900/40"
                      }`}
                      title={isListening ? "Click to stop listening" : "Speak to filter with AI"}
                    >
                      {isListening ? (
                        <MicOff className="w-3.5 h-3.5" />
                      ) : (
                        <Mic className="w-3.5 h-3.5" />
                      )}
                    </button>
                  )}
                </div>

                {/* Animated Search buses button */}
                <AnimatePresence>
                  {aiFilterQuery.trim() && (
                    <motion.div
                      initial={{ opacity: 0, y: -6, scale: 0.98 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: -6, scale: 0.98 }}
                      transition={{ duration: 0.18 }}
                      className="pt-1"
                    >
                      <button
                        type="button"
                        onClick={() => handleTriggerAiSearch()}
                        className="w-full py-2.5 px-3 rounded-xl bg-gradient-to-r from-purple-600 via-indigo-600 to-purple-700 hover:from-purple-700 hover:to-indigo-700 active:scale-98 text-white font-extrabold text-xs shadow-md shadow-purple-500/20 transition-all flex items-center justify-center space-x-1.5 cursor-pointer"
                      >
                        <Sparkles className="w-3.5 h-3.5" />
                        <span>Search buses with AI</span>
                      </button>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>

              {/* 2. Quick Filter Pills with Counts (Matching Screenshot 3) */}
              <div className="space-y-2 pt-1 border-t border-gray-100 dark:border-slate-800">
                {/* Primo Bus */}
                <button
                  type="button"
                  onClick={() => setIsPrimoFilter(!isPrimoFilter)}
                  className={`w-full px-3 py-2 rounded-xl border text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                    isPrimoFilter
                      ? "border-[#d84e55] bg-red-50 dark:bg-red-950/40 text-[#d84e55]"
                      : "border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800/60 text-gray-700 dark:text-slate-200"
                  }`}
                >
                  <div className="flex items-center space-x-2">
                    <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                    <span>Primo Bus</span>
                  </div>
                  <span className="text-[11px] text-gray-400 font-semibold">
                    ({filterCounts.primo})
                  </span>
                </button>

                {/* Free Cancellation */}
                <button
                  type="button"
                  onClick={() => setIsFreeCancellationFilter(!isFreeCancellationFilter)}
                  className={`w-full px-3 py-2 rounded-xl border text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                    isFreeCancellationFilter
                      ? "border-emerald-500 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300"
                      : "border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800/60 text-gray-700 dark:text-slate-200"
                  }`}
                >
                  <div className="flex items-center space-x-2">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                    <span>Free Cancellation</span>
                  </div>
                  <span className="text-[11px] text-gray-400 font-semibold">
                    ({filterCounts.freeCancel})
                  </span>
                </button>

                {/* AC */}
                <button
                  type="button"
                  onClick={() => setIsAcFilter(!isAcFilter)}
                  className={`w-full px-3 py-2 rounded-xl border text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                    isAcFilter
                      ? "border-[#d84e55] bg-red-50 dark:bg-red-950/40 text-[#d84e55]"
                      : "border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800/60 text-gray-700 dark:text-slate-200"
                  }`}
                >
                  <div className="flex items-center space-x-2">
                    <Bus className="w-3.5 h-3.5 text-blue-500" />
                    <span>AC</span>
                  </div>
                  <span className="text-[11px] text-gray-400 font-semibold">
                    ({filterCounts.ac})
                  </span>
                </button>

                {/* SLEEPER */}
                <button
                  type="button"
                  onClick={() => setIsSleeperFilter(!isSleeperFilter)}
                  className={`w-full px-3 py-2 rounded-xl border text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                    isSleeperFilter
                      ? "border-[#d84e55] bg-red-50 dark:bg-red-950/40 text-[#d84e55]"
                      : "border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800/60 text-gray-700 dark:text-slate-200"
                  }`}
                >
                  <div className="flex items-center space-x-2">
                    <BedDouble className="w-3.5 h-3.5 text-purple-500" />
                    <span>SLEEPER</span>
                  </div>
                  <span className="text-[11px] text-gray-400 font-semibold">
                    ({filterCounts.sleeper})
                  </span>
                </button>

                {/* Single Seats */}
                <button
                  type="button"
                  onClick={() => setIsSingleSeatFilter(!isSingleSeatFilter)}
                  className={`w-full px-3 py-2 rounded-xl border text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                    isSingleSeatFilter
                      ? "border-[#d84e55] bg-red-50 dark:bg-red-950/40 text-[#d84e55]"
                      : "border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800/60 text-gray-700 dark:text-slate-200"
                  }`}
                >
                  <div className="flex items-center space-x-2">
                    <Armchair className="w-3.5 h-3.5 text-teal-500" />
                    <span>Single Seats</span>
                  </div>
                  <span className="text-[11px] text-gray-400 font-semibold">
                    ({filterCounts.single})
                  </span>
                </button>

                {/* SEATER */}
                <button
                  type="button"
                  onClick={() => setIsSeaterFilter(!isSeaterFilter)}
                  className={`w-full px-3 py-2 rounded-xl border text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                    isSeaterFilter
                      ? "border-[#d84e55] bg-red-50 dark:bg-red-950/40 text-[#d84e55]"
                      : "border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800/60 text-gray-700 dark:text-slate-200"
                  }`}
                >
                  <div className="flex items-center space-x-2">
                    <Armchair className="w-3.5 h-3.5 text-orange-500" />
                    <span>SEATER</span>
                  </div>
                  <span className="text-[11px] text-gray-400 font-semibold">
                    ({filterCounts.seater})
                  </span>
                </button>

                {/* NONAC */}
                <button
                  type="button"
                  onClick={() => setIsNonAcFilter(!isNonAcFilter)}
                  className={`w-full px-3 py-2 rounded-xl border text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                    isNonAcFilter
                      ? "border-[#d84e55] bg-red-50 dark:bg-red-950/40 text-[#d84e55]"
                      : "border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800/60 text-gray-700 dark:text-slate-200"
                  }`}
                >
                  <div className="flex items-center space-x-2">
                    <span className="text-[11px] font-extrabold text-gray-500">NON</span>
                    <span>NONAC</span>
                  </div>
                  <span className="text-[11px] text-gray-400 font-semibold">
                    ({filterCounts.nonac})
                  </span>
                </button>
              </div>

              {/* 3. Departure Time section */}
              <div className="pt-2 border-t border-gray-100 dark:border-slate-800">
                <h3 className="text-xs font-bold text-gray-700 dark:text-slate-300 uppercase tracking-wider mb-2.5">
                  Departure Time
                </h3>
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { id: "MORNING", label: "Morning", sub: "6am - 12pm", icon: <Sun className="w-3.5 h-3.5 text-amber-500" /> },
                    { id: "AFTERNOON", label: "Afternoon", sub: "12pm - 6pm", icon: <Sun className="w-3.5 h-3.5 text-orange-500" /> },
                    { id: "EVENING", label: "Evening", sub: "6pm - 12am", icon: <Sunset className="w-3.5 h-3.5 text-rose-500" /> },
                    { id: "NIGHT", label: "Night", sub: "12am - 6am", icon: <Moon className="w-3.5 h-3.5 text-indigo-500" /> },
                  ].map((w) => (
                    <button
                      key={w.id}
                      type="button"
                      onClick={() =>
                        dispatch(
                          setDepartureWindowFilter(filters.departureWindow === w.id ? "ALL" : w.id)
                        )
                      }
                      className={`p-2 rounded-xl border text-left transition-all cursor-pointer ${
                        filters.departureWindow === w.id
                          ? "border-[#d84e55] bg-red-50 dark:bg-red-950/40 text-[#d84e55]"
                          : "border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800 text-gray-700 dark:text-slate-300"
                      }`}
                    >
                      <div className="flex items-center space-x-1">
                        {w.icon}
                        <span className="text-xs font-bold">{w.label}</span>
                      </div>
                      <span className="text-[10px] text-gray-400 block mt-0.5">{w.sub}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* 4. Price Presets */}
              <div className="pt-2 border-t border-gray-100 dark:border-slate-800">
                <h3 className="text-xs font-bold text-gray-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  Price Presets
                </h3>
                <div className="flex flex-wrap gap-1.5">
                  {[
                    { label: "Under ₹600", max: 600 },
                    { label: "Under ₹900", max: 900 },
                    { label: "Under ₹1200", max: 1200 },
                  ].map((preset) => (
                    <button
                      key={preset.label}
                      type="button"
                      onClick={() =>
                        dispatch(
                          setPriceRangeFilter({
                            max: filters.maxPrice === preset.max ? undefined : preset.max,
                          })
                        )
                      }
                      className={`px-2.5 py-1 rounded-lg text-xs font-bold border transition-all cursor-pointer ${
                        filters.maxPrice === preset.max
                          ? "bg-[#d84e55] text-white border-[#d84e55]"
                          : "bg-gray-50 dark:bg-slate-800 border-gray-200 dark:border-slate-700 text-gray-700 dark:text-slate-300 hover:bg-gray-100"
                      }`}
                    >
                      {preset.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* ================= RIGHT COLUMN: PROMOS + SORT + LISTING ================= */}
          <div className="lg:col-span-9 space-y-4">
            {/* 1. Desktop Service Highlight Cards Grid with Premium Dark Mode Colors */}
            <div className="hidden lg:grid lg:grid-cols-5 gap-2.5">
              {/* Card 1: Primo Top Rated */}
              <div
                onClick={() => setIsPrimoFilter(!isPrimoFilter)}
                className={`p-3.5 rounded-2xl shadow-xs border cursor-pointer hover:scale-[1.02] transition-all relative overflow-hidden group flex flex-col justify-between ${
                  isPrimoFilter
                    ? "bg-gradient-to-br from-amber-600 via-amber-700 to-yellow-700 border-amber-300 ring-2 ring-amber-400 text-white"
                    : "bg-gradient-to-br from-amber-500 via-amber-600 to-yellow-600 dark:from-[#251704] dark:via-[#191104] dark:to-[#0f172a] border-amber-400 dark:border-amber-600/40 text-white dark:text-amber-100"
                }`}
              >
                <div>
                  <div className="w-7 h-7 rounded-full bg-white/20 dark:bg-amber-500/20 flex items-center justify-center mb-1.5 text-white dark:text-amber-400 shadow-2xs">
                    <Star className="w-4 h-4 fill-current" />
                  </div>
                  <h4 className="text-xs font-black tracking-tight text-white dark:text-white flex items-center space-x-1">
                    <span>Primo Certified</span>
                  </h4>
                  <p className="text-[10px] text-yellow-100 dark:text-amber-200/80 font-semibold mt-0.5 leading-tight">
                    Top Rated 4.4★+ Fleet
                  </p>
                </div>
                <span className="mt-2 text-[10px] font-bold text-white dark:text-amber-300 group-hover:underline">
                  {isPrimoFilter ? "Applied ✓" : "Filter Primo →"}
                </span>
              </div>

              {/* Card 2: Free Cancellation */}
              <div
                onClick={() => setIsFreeCancellationFilter(!isFreeCancellationFilter)}
                className={`p-3.5 rounded-2xl shadow-xs border cursor-pointer hover:scale-[1.02] transition-all relative overflow-hidden group flex flex-col justify-between ${
                  isFreeCancellationFilter
                    ? "bg-rose-100 dark:bg-rose-950/70 border-[#d84e55] ring-2 ring-red-400 text-gray-900 dark:text-white"
                    : "bg-gradient-to-br from-pink-50 via-rose-50 to-red-50 dark:from-[#260a10] dark:via-[#18080c] dark:to-[#0f172a] text-gray-900 dark:text-white border-pink-200 dark:border-red-900/50"
                }`}
              >
                <div>
                  <div className="w-7 h-7 rounded-full bg-[#d84e55] text-white flex items-center justify-center mb-1.5 shadow-xs">
                    <ShieldCheck className="w-4 h-4" />
                  </div>
                  <h4 className="text-xs font-extrabold text-gray-900 dark:text-white">
                    Free Cancellation
                  </h4>
                  <p className="text-[10px] text-gray-600 dark:text-slate-300 font-medium mt-0.5 leading-tight">
                    100% Refund guarantee
                  </p>
                </div>
                <span className="mt-2 text-[10px] font-bold text-[#d84e55] dark:text-rose-400 group-hover:underline">
                  {isFreeCancellationFilter ? "Applied ✓" : "Learn more →"}
                </span>
              </div>

              {/* Card 3: Instant Refunds */}
              <div
                className="bg-gradient-to-br from-emerald-50 via-teal-50 to-cyan-50 dark:from-[#062419] dark:via-[#071913] dark:to-[#0f172a] text-gray-900 dark:text-white p-3.5 rounded-2xl shadow-xs border border-emerald-200 dark:border-emerald-800/50 relative overflow-hidden flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-[10px] font-black text-emerald-700 dark:text-emerald-400 uppercase tracking-tight">
                      ⚡ 15-Min Refund
                    </span>
                    <span className="text-[8px] bg-emerald-700 text-white font-bold px-1.5 py-0.2 rounded-md">
                      ASSURED
                    </span>
                  </div>
                  <h4 className="text-xs font-black tracking-tight leading-tight text-gray-900 dark:text-white">
                    Instant Bank Payout
                  </h4>
                  <p className="text-[10px] text-gray-600 dark:text-slate-300 font-medium mt-0.5 leading-tight">
                    Direct UPI & Card reversals
                  </p>
                </div>
                <span className="mt-2 text-[10px] font-bold text-emerald-700 dark:text-emerald-400">
                  Zero Hassle ✓
                </span>
              </div>

              {/* Card 4: Return Trip Deals */}
              <div
                className="bg-gradient-to-br from-blue-50 via-indigo-50 to-sky-50 dark:from-[#081b38] dark:via-[#081427] dark:to-[#0f172a] text-gray-900 dark:text-white p-3.5 rounded-2xl shadow-xs border border-blue-200 dark:border-blue-800/50 relative overflow-hidden flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center space-x-1 mb-1.5">
                    <span className="px-1.5 py-0.5 bg-blue-600 text-white font-black text-[9px] rounded-md uppercase">
                      OFFER
                    </span>
                  </div>
                  <h4 className="text-xs font-black tracking-tight leading-tight text-gray-900 dark:text-white">
                    Return Trip Savings
                  </h4>
                  <p className="text-[10px] text-gray-600 dark:text-slate-300 font-medium mt-0.5 leading-tight">
                    Flat 10% OFF on return bus
                  </p>
                </div>
                <span className="mt-2 text-[10px] font-bold text-blue-600 dark:text-blue-400">
                  Auto Applied at Checkout
                </span>
              </div>

              {/* Card 5: Last min Deals */}
              <div
                onClick={() => setActiveSort("price")}
                className={`p-3.5 rounded-2xl shadow-xs border cursor-pointer hover:scale-[1.02] transition-all relative overflow-hidden group flex flex-col justify-between ${
                  activeSort === "price"
                    ? "bg-amber-100 dark:bg-amber-950/70 border-orange-400 ring-2 ring-orange-400 text-gray-900 dark:text-white"
                    : "bg-gradient-to-br from-orange-50 via-amber-50 to-yellow-50 dark:from-[#261506] dark:via-[#190f05] dark:to-[#0f172a] text-gray-900 dark:text-white border-orange-200 dark:border-orange-800/50"
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-xs font-black text-orange-600 dark:text-orange-400 uppercase">
                      Last min.
                    </span>
                    <Clock className="w-3.5 h-3.5 text-orange-500" />
                  </div>
                  <h4 className="text-xs font-bold text-gray-800 dark:text-white leading-tight">
                    Limited time steal deals
                  </h4>
                  <p className="text-[10px] text-gray-500 dark:text-slate-300 mt-0.5">
                    Save up to ₹250 instant
                  </p>
                </div>
                <span className="mt-2 text-[10px] font-bold text-orange-600 dark:text-orange-400 group-hover:underline">
                  Lowest Fares →
                </span>
              </div>
            </div>

            {/* 1b. Mobile Horizontal Swipeable Service Cards */}
            <div className="flex lg:hidden overflow-x-auto gap-2.5 py-1 scrollbar-none snap-x px-1 -mx-1">
              {/* Mobile Card 1: Primo */}
              <div
                onClick={() => setIsPrimoFilter(!isPrimoFilter)}
                className={`min-w-[170px] snap-start p-3 rounded-2xl shadow-xs border cursor-pointer shrink-0 flex flex-col justify-between h-28 ${
                  isPrimoFilter
                    ? "bg-amber-600 border-amber-300 text-white"
                    : "bg-gradient-to-br from-amber-500 to-yellow-600 dark:from-[#251704] dark:to-[#0f172a] border-amber-400 dark:border-amber-600/40 text-white dark:text-amber-100"
                }`}
              >
                <div>
                  <div className="w-6 h-6 rounded-full bg-white/20 dark:bg-amber-500/20 flex items-center justify-center mb-1">
                    <Star className="w-3 h-3 fill-current" />
                  </div>
                  <h4 className="text-xs font-black text-white">Primo Certified</h4>
                  <p className="text-[9px] text-yellow-100 dark:text-amber-200 mt-0.5">Top Rated 4.4★+ Fleet</p>
                </div>
                <span className="text-[9px] font-bold text-white dark:text-amber-300">
                  {isPrimoFilter ? "Applied ✓" : "Top Rated →"}
                </span>
              </div>

              {/* Mobile Card 2: Free Cancellation */}
              <div
                onClick={() => setIsFreeCancellationFilter(!isFreeCancellationFilter)}
                className={`min-w-[180px] snap-start p-3 rounded-2xl shadow-xs border cursor-pointer shrink-0 flex flex-col justify-between h-28 ${
                  isFreeCancellationFilter
                    ? "bg-rose-100 dark:bg-rose-950/70 border-[#d84e55] text-gray-900 dark:text-white"
                    : "bg-gradient-to-br from-pink-50 to-red-50 dark:from-[#260a10] dark:to-[#0f172a] border-pink-200 dark:border-red-900/50 text-gray-900 dark:text-white"
                }`}
              >
                <div>
                  <div className="w-6 h-6 rounded-full bg-[#d84e55] text-white flex items-center justify-center mb-1">
                    <ShieldCheck className="w-3 h-3" />
                  </div>
                  <h4 className="text-xs font-extrabold text-gray-900 dark:text-white">Free Cancellation</h4>
                  <p className="text-[9px] text-gray-600 dark:text-slate-300 mt-0.5">100% Refund guarantee</p>
                </div>
                <span className="text-[9px] font-bold text-[#d84e55] dark:text-rose-400">
                  {isFreeCancellationFilter ? "Applied ✓" : "Learn more →"}
                </span>
              </div>

              {/* Mobile Card 3: Instant Refunds */}
              <div
                className="min-w-[170px] snap-start bg-gradient-to-br from-emerald-50 to-teal-50 dark:from-[#062419] dark:to-[#0f172a] text-gray-900 dark:text-white p-3 rounded-2xl shadow-xs border border-emerald-200 dark:border-emerald-800/50 shrink-0 flex flex-col justify-between h-28"
              >
                <div>
                  <span className="text-[10px] font-black text-emerald-700 dark:text-emerald-400 uppercase">
                    ⚡ Instant Refund
                  </span>
                  <h4 className="text-[11px] font-bold text-gray-800 dark:text-white leading-tight mt-0.5">
                    15-Min Reversal
                  </h4>
                  <p className="text-[9px] text-gray-500 dark:text-slate-300 mt-0.5">Direct UPI / Card</p>
                </div>
                <span className="text-[9px] font-bold text-emerald-700 dark:text-emerald-400">Assured ✓</span>
              </div>

              {/* Mobile Card 4: Return Deals */}
              <div
                className="min-w-[170px] snap-start bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-[#081b38] dark:to-[#0f172a] text-gray-900 dark:text-white p-3 rounded-2xl shadow-xs border border-blue-200 dark:border-blue-800/50 shrink-0 flex flex-col justify-between h-28"
              >
                <div>
                  <span className="text-[10px] font-black text-blue-700 dark:text-blue-400 uppercase">
                    🏷️ Return Deal
                  </span>
                  <h4 className="text-[11px] font-bold text-gray-800 dark:text-white leading-tight mt-0.5">
                    Flat 10% Discount
                  </h4>
                  <p className="text-[9px] text-gray-500 dark:text-slate-300 mt-0.5">On Return bookings</p>
                </div>
                <span className="text-[9px] font-bold text-blue-700 dark:text-blue-400">Auto Applied</span>
              </div>

              {/* Mobile Card 5: Last min Deals */}
              <div
                onClick={() => setActiveSort("price")}
                className="min-w-[170px] snap-start bg-gradient-to-br from-orange-50 to-amber-50 dark:from-[#261506] dark:to-[#0f172a] text-gray-900 dark:text-white p-3 rounded-2xl shadow-xs border border-orange-200 dark:border-orange-800/50 cursor-pointer shrink-0 flex flex-col justify-between h-28"
              >
                <div>
                  <span className="text-[10px] font-black text-orange-600 dark:text-orange-400 uppercase">
                    Last min.
                  </span>
                  <h4 className="text-[11px] font-bold text-gray-800 dark:text-white leading-tight mt-0.5">
                    Steal deals
                  </h4>
                  <p className="text-[9px] text-gray-500 dark:text-slate-300 mt-0.5">Save up to ₹250</p>
                </div>
                <span className="text-[9px] font-bold text-orange-600 dark:text-orange-400">Lowest Fares →</span>
              </div>
            </div>

            {/* 1c. Mobile Filter & Sort Pills Strip */}
            <div className="flex lg:hidden items-center space-x-2 overflow-x-auto scrollbar-none py-1 px-1 -mx-1">
              <button
                type="button"
                onClick={() => setShowFilterModal(true)}
                className="px-3 py-2 rounded-xl text-xs font-black border border-gray-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-800 dark:text-white flex items-center space-x-1.5 shadow-2xs shrink-0 cursor-pointer active:scale-95"
              >
                <SlidersHorizontal className="w-3.5 h-3.5 text-[#d84e55]" />
                <span>Filter & Sort</span>
                {hasAnyFilterActive && (
                  <span className="w-2 h-2 rounded-full bg-[#d84e55]" />
                )}
              </button>

              <button
                type="button"
                onClick={() => setIsPrimoFilter(!isPrimoFilter)}
                className={`px-3 py-2 rounded-xl text-xs font-bold border transition-all shrink-0 cursor-pointer ${
                  isPrimoFilter
                    ? "bg-amber-500 text-white border-amber-500"
                    : "bg-white dark:bg-slate-800 border-gray-200 dark:border-slate-700 text-gray-700 dark:text-slate-300"
                }`}
              >
                ⭐ Primo Bus ({filterCounts.primo})
              </button>

              <button
                type="button"
                onClick={() => setIsFreeCancellationFilter(!isFreeCancellationFilter)}
                className={`px-3 py-2 rounded-xl text-xs font-bold border transition-all shrink-0 cursor-pointer ${
                  isFreeCancellationFilter
                    ? "bg-[#d84e55] text-white border-[#d84e55]"
                    : "bg-white dark:bg-slate-800 border-gray-200 dark:border-slate-700 text-gray-700 dark:text-slate-300"
                }`}
              >
                🛡️ Free Cancellation ({filterCounts.freeCancel})
              </button>

              <button
                type="button"
                onClick={() => setIsAcFilter(!isAcFilter)}
                className={`px-3 py-2 rounded-xl text-xs font-bold border transition-all shrink-0 cursor-pointer ${
                  isAcFilter
                    ? "bg-[#d84e55] text-white border-[#d84e55]"
                    : "bg-white dark:bg-slate-800 border-gray-200 dark:border-slate-700 text-gray-700 dark:text-slate-300"
                }`}
              >
                🚌 AC ({filterCounts.ac})
              </button>

              <button
                type="button"
                onClick={() => setIsSleeperFilter(!isSleeperFilter)}
                className={`px-3 py-2 rounded-xl text-xs font-bold border transition-all shrink-0 cursor-pointer ${
                  isSleeperFilter
                    ? "bg-[#d84e55] text-white border-[#d84e55]"
                    : "bg-white dark:bg-slate-800 border-gray-200 dark:border-slate-700 text-gray-700 dark:text-slate-300"
                }`}
              >
                🛏️ SLEEPER ({filterCounts.sleeper})
              </button>
            </div>

            {/* 1d. Mobile AI Smart Filter Row */}
            <div className="lg:hidden space-y-1.5 px-1">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-extrabold text-purple-600 dark:text-purple-400 flex items-center space-x-1">
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>AI Smart filter</span>
                </span>
                {(aiFilterQuery || aiAppliedQuery) && (
                  <button
                    type="button"
                    onClick={clearAiFilter}
                    className="text-[10px] text-gray-400 hover:text-gray-600 dark:hover:text-slate-300 font-bold cursor-pointer"
                  >
                    Clear
                  </button>
                )}
              </div>
              <div
                className={`relative flex items-center rounded-xl border-2 transition-all p-1.5 ${
                  isListening
                    ? "border-red-500 bg-red-50/40 dark:bg-red-950/20 ring-2 ring-red-400/30"
                    : aiFilterQuery
                    ? "border-purple-500 bg-purple-50/40 dark:bg-purple-950/30"
                    : "border-purple-300 dark:border-purple-800/80 bg-white dark:bg-[#0f172a]"
                }`}
              >
                <Sparkles className="w-4 h-4 text-purple-600 dark:text-purple-400 ml-1 shrink-0" />
                {!aiFilterQuery && !isListening && (
                  <div className="absolute left-8 right-12 inset-y-0 flex items-center pointer-events-none">
                    <AutotypingPlaceholder
                      className="text-xs font-semibold text-gray-400 dark:text-slate-400"
                      cursorClassName="bg-purple-600 dark:bg-purple-400"
                    />
                  </div>
                )}
                <input
                  type="text"
                  value={aiFilterQuery}
                  onChange={(e) => setAiFilterQuery(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      handleTriggerAiSearch();
                    }
                  }}
                  placeholder={isListening ? "Listening... speak now" : ""}
                  className="w-full bg-transparent px-2 py-0.5 text-xs font-semibold text-gray-900 dark:text-white placeholder-gray-400 focus:outline-hidden relative z-10"
                />

                {aiFilterQuery ? (
                  <button
                    type="button"
                    onClick={clearAiFilter}
                    className="p-1 text-gray-400 hover:text-gray-700 dark:hover:text-white shrink-0 cursor-pointer relative z-20"
                    title="Clear search"
                  >
                    <X className="w-4 h-4" />
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={handleAiVoiceInput}
                    className={`p-1.5 rounded-lg transition-all shrink-0 cursor-pointer relative z-20 ${
                      isListening
                        ? "bg-red-500 text-white animate-pulse"
                        : "text-purple-600 dark:text-purple-400 hover:bg-purple-100 dark:hover:bg-purple-900/40"
                    }`}
                    title={isListening ? "Click to stop listening" : "Speak to filter with AI"}
                  >
                    {isListening ? (
                      <MicOff className="w-3.5 h-3.5" />
                    ) : (
                      <Mic className="w-3.5 h-3.5" />
                    )}
                  </button>
                )}
              </div>

              {/* Animated Mobile Search buses button */}
              <AnimatePresence>
                {aiFilterQuery.trim() && (
                  <motion.div
                    initial={{ opacity: 0, y: -6, scale: 0.98 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: -6, scale: 0.98 }}
                    transition={{ duration: 0.18 }}
                    className="pt-1"
                  >
                    <button
                      type="button"
                      onClick={() => handleTriggerAiSearch()}
                      className="w-full py-2.5 px-4 rounded-xl bg-gradient-to-r from-purple-600 via-indigo-600 to-purple-700 hover:from-purple-700 hover:to-indigo-700 active:scale-98 text-white font-extrabold text-xs shadow-md shadow-purple-500/20 transition-all flex items-center justify-center space-x-1.5 cursor-pointer"
                    >
                      <Sparkles className="w-3.5 h-3.5" />
                      <span>Search buses with AI</span>
                    </button>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* ================= AI SMART FILTER DETAILS BANNER (RIGHT SIDE) ================= */}
            <AnimatePresence>
              {(aiAppliedQuery || (aiFilterQuery.trim() && !isSearching)) && (
                <motion.div
                  key="ai-filter-banner"
                  initial={{ opacity: 0, y: -10, scale: 0.98 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -10, scale: 0.98 }}
                  transition={{ type: "spring", damping: 25, stiffness: 350 }}
                  className="p-3 sm:p-5 bg-gradient-to-r from-purple-50 via-indigo-50/60 to-purple-50/30 dark:from-[#17122b] dark:via-[#111936] dark:to-[#0f172a] border border-purple-300 dark:border-purple-600/50 rounded-xl sm:rounded-2xl shadow-2xs space-y-2 sm:space-y-3"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 sm:gap-3">
                    <div className="flex items-center space-x-2.5 sm:space-x-3.5">
                      <div className="w-8 h-8 sm:w-11 sm:h-11 rounded-xl sm:rounded-2xl bg-gradient-to-tr from-purple-600 to-indigo-600 text-white flex items-center justify-center shadow-md shadow-purple-500/30 shrink-0">
                        <Sparkles className="w-4 h-4 sm:w-6 sm:h-6" />
                      </div>
                      <div>
                        <div className="flex items-center space-x-1.5 sm:space-x-2">
                          <span className="text-[10px] sm:text-[11px] font-black uppercase tracking-wider text-purple-700 dark:text-purple-400">
                            AI Filter Applied
                          </span>
                          <span className="px-2 py-0.5 bg-purple-200/80 dark:bg-purple-950 text-purple-900 dark:text-purple-300 font-black text-[10px] sm:text-[11px] rounded-full border border-purple-300 dark:border-purple-800 shadow-2xs">
                            {filteredAndSortedRoutes.length} Found
                          </span>
                        </div>
                        <h3 className="text-xs sm:text-base font-black text-gray-900 dark:text-white mt-0.5">
                          Filtered for: <span className="text-purple-600 dark:text-purple-300">&ldquo;{aiAppliedQuery || aiFilterQuery}&rdquo;</span>
                        </h3>
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={clearAiFilter}
                      className="px-2.5 py-1 sm:px-4 sm:py-2 rounded-lg sm:rounded-xl bg-white dark:bg-slate-800 hover:bg-red-50 dark:hover:bg-red-950/50 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900/60 text-[11px] sm:text-xs font-bold transition-all flex items-center space-x-1 shadow-2xs self-start sm:self-center cursor-pointer active:scale-95 shrink-0"
                    >
                      <X className="w-3 h-3 sm:w-3.5 sm:h-3.5" />
                      <span>Clear AI Filter</span>
                    </button>
                  </div>

                  {/* AI Recognized Details & Criteria summary badges */}
                  <div className="pt-2 sm:pt-2.5 border-t border-purple-200/60 dark:border-purple-900/40 flex flex-wrap items-center gap-1.5 sm:gap-2 text-[11px] sm:text-xs">
                    <span className="font-bold text-gray-700 dark:text-slate-300 flex items-center gap-1">
                      <span>🧠 Criteria:</span>
                    </span>
                    <span className="px-2 sm:px-3 py-0.5 sm:py-1 bg-white dark:bg-slate-800/90 text-purple-800 dark:text-purple-300 font-bold rounded-lg border border-purple-200 dark:border-purple-800/80 shadow-2xs">
                      {aiFilterSummary}
                    </span>
                    {filteredAndSortedRoutes.length > 0 ? (
                      <span className="font-semibold text-emerald-700 dark:text-emerald-400 flex items-center gap-1 text-[10px] sm:text-xs">
                        ✓ Showing real operators
                      </span>
                    ) : (
                      <span className="font-semibold text-amber-700 dark:text-amber-400 flex items-center gap-1 text-[10px] sm:text-xs">
                        ⚠️ 0 matches found
                      </span>
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {/* 2. Desktop Route Count & Sort Bar */}
            <div className="hidden lg:flex bg-white dark:bg-[#0f172a] rounded-2xl px-5 py-3 border border-gray-200 dark:border-slate-800 shadow-xs flex-wrap items-center justify-between gap-3 text-xs">
              <div className="font-extrabold text-gray-900 dark:text-white text-sm flex items-center space-x-2">
                <span>{filteredAndSortedRoutes.length} buses found</span>
                {(aiAppliedQuery || aiFilterQuery) && (
                  <span className="px-2 py-0.5 bg-purple-100 dark:bg-purple-950/70 text-purple-700 dark:text-purple-300 font-bold rounded-md text-xs">
                    (AI Filtered)
                  </span>
                )}
              </div>

              {/* Sort by Tabs: Ratings, Departure time, Price */}
              <div className="flex items-center space-x-2">
                <span className="text-gray-400 dark:text-slate-400 font-bold mr-1">
                  Sort by:
                </span>
                <button
                  type="button"
                  onClick={() => setActiveSort("ratings")}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
                    activeSort === "ratings"
                      ? "bg-red-50 dark:bg-red-950/60 text-[#d84e55] dark:text-red-400 border border-red-200 dark:border-red-900/60 shadow-2xs"
                      : "bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-slate-300 hover:bg-gray-200 dark:hover:bg-slate-700"
                  }`}
                >
                  Ratings
                </button>
                <button
                  type="button"
                  onClick={() => setActiveSort("departure")}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
                    activeSort === "departure"
                      ? "bg-red-50 dark:bg-red-950/60 text-[#d84e55] dark:text-red-400 border border-red-200 dark:border-red-900/60 shadow-2xs"
                      : "bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-slate-300 hover:bg-gray-200 dark:hover:bg-slate-700"
                  }`}
                >
                  Departure time
                </button>
                <button
                  type="button"
                  onClick={() => setActiveSort("price")}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
                    activeSort === "price"
                      ? "bg-red-50 dark:bg-red-950/60 text-[#d84e55] dark:text-red-400 border border-red-200 dark:border-red-900/60 shadow-2xs"
                      : "bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-slate-300 hover:bg-gray-200 dark:hover:bg-slate-700"
                  }`}
                >
                  Price
                </button>
              </div>
            </div>

            {/* 3. Decorative RedBus Peach Ribbon */}
            <div className="w-full bg-[#fdeee4] dark:bg-slate-900/90 border border-orange-200/80 dark:border-slate-800 rounded-xl sm:rounded-2xl py-1.5 sm:py-2 px-3 sm:px-4 text-center">
              <span className="text-[11px] sm:text-xs font-black text-amber-950 dark:text-amber-300 tracking-wide">
                ✨ 3.6+ lakh bus routes on redBus
              </span>
            </div>

            {/* 4. Bus Cards Listing with Staggered Lazy Loading Transition */}
            {isSearching ? (
              <div className="space-y-4">
                <div className="p-3.5 bg-gradient-to-r from-red-50 via-white to-red-50 dark:from-slate-900 dark:via-slate-800/80 dark:to-slate-900 border border-red-200/80 dark:border-red-900/40 rounded-2xl flex items-center justify-between shadow-2xs">
                  <div className="flex items-center space-x-3">
                    <div className="w-5 h-5 border-2 border-[#d84e55] border-t-transparent rounded-full animate-spin shrink-0" />
                    <div>
                      <p className="text-xs font-black text-gray-900 dark:text-white">
                        Searching verified buses between {source || "source"} and {destination || "destination"}...
                      </p>
                      <p className="text-[11px] text-gray-500 dark:text-slate-400">
                        Checking real-time seat availability, live GPS schedules & operator fares
                      </p>
                    </div>
                  </div>
                  <span className="hidden sm:inline-flex text-[10px] font-extrabold text-[#d84e55] uppercase tracking-wider bg-red-100 dark:bg-red-950/60 px-2.5 py-1 rounded-full animate-pulse border border-red-200 dark:border-red-900/60">
                    Live Search
                  </span>
                </div>
                <BusCardSkeleton />
                <BusCardSkeleton />
                <BusCardSkeleton />
                <BusCardSkeleton />
              </div>
            ) : filteredAndSortedRoutes.length > 0 ? (
              <motion.div
                initial="hidden"
                animate="visible"
                variants={{
                  hidden: { opacity: 0 },
                  visible: {
                    opacity: 1,
                    transition: {
                      staggerChildren: 0.07,
                    },
                  },
                }}
                className="space-y-4"
              >
                {filteredAndSortedRoutes.map((route) => (
                  <motion.div
                    key={route.id}
                    variants={{
                      hidden: { opacity: 0, y: 15 },
                      visible: { opacity: 1, y: 0, transition: { duration: 0.28, ease: "easeOut" } },
                    }}
                  >
                    <BusCard
                      route={route}
                      autoOpen={selectRouteParam ? Number(selectRouteParam) === route.id : false}
                    />
                  </motion.div>
                ))}
              </motion.div>
            ) : (
              <div className="bg-white dark:bg-[#0f172a] rounded-2xl p-8 sm:p-12 text-center border border-gray-200 dark:border-slate-800 shadow-xs space-y-4">
                {aiAppliedQuery || aiFilterQuery ? (
                  <div className="max-w-md mx-auto space-y-3">
                    <div className="w-12 h-12 rounded-2xl bg-purple-100 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400 mx-auto flex items-center justify-center">
                      <Sparkles className="w-6 h-6" />
                    </div>
                    <h3 className="text-base font-black text-gray-900 dark:text-white">
                      No buses matched "{aiAppliedQuery || aiFilterQuery}"
                    </h3>
                    <p className="text-xs text-gray-600 dark:text-slate-300 leading-relaxed">
                      {(aiAppliedQuery || aiFilterQuery).toLowerCase().includes("morning")
                        ? `No morning departure buses (5:00 AM – 12:00 PM) found between ${source} and ${destination} on this date.`
                        : (aiAppliedQuery || aiFilterQuery).toLowerCase().includes("afternoon")
                        ? `No afternoon departure buses (12:00 PM – 5:00 PM) found between ${source} and ${destination} on this date.`
                        : (aiAppliedQuery || aiFilterQuery).toLowerCase().includes("evening")
                        ? `No evening departure buses (5:00 PM – 9:00 PM) found between ${source} and ${destination} on this date.`
                        : (aiAppliedQuery || aiFilterQuery).toLowerCase().includes("night")
                        ? `No night departure buses (8:00 PM – 5:00 AM) found between ${source} and ${destination} on this date.`
                        : `No buses found matching "${aiAppliedQuery || aiFilterQuery}" with the current filters.`}
                    </p>
                    {rawRoutes.length > 0 && (
                      <p className="text-xs font-semibold text-purple-700 dark:text-purple-300 bg-purple-50 dark:bg-purple-950/40 py-2 px-3 rounded-xl border border-purple-200 dark:border-purple-800/50">
                        💡 We found {rawRoutes.length} other verified buses operating on this route starting from ₹{Math.min(...rawRoutes.map((r) => r.basePrice))}.
                      </p>
                    )}
                    <div className="flex items-center justify-center gap-2 pt-2">
                      <button
                        type="button"
                        onClick={clearAiFilter}
                        className="px-4 py-2.5 bg-purple-600 hover:bg-purple-700 text-white rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer"
                      >
                        Clear AI Filter
                      </button>
                      <button
                        type="button"
                        onClick={clearAllFilters}
                        className="px-4 py-2.5 bg-gray-100 dark:bg-slate-800 hover:bg-gray-200 dark:hover:bg-slate-700 text-gray-800 dark:text-slate-200 rounded-xl text-xs font-bold transition-all cursor-pointer"
                      >
                        Show All {rawRoutes.length} Buses
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="max-w-md mx-auto space-y-3">
                    <p className="text-base font-extrabold text-gray-800 dark:text-white">
                      No buses found matching your active filters
                    </p>
                    <p className="text-xs text-gray-500 dark:text-slate-400">
                      Try clearing some of your departure or amenities filters, or explore buses on another date.
                    </p>
                    <button
                      type="button"
                      onClick={clearAllFilters}
                      className="px-5 py-2.5 bg-[#d84e55] text-white rounded-xl text-xs font-bold hover:bg-[#b83e44] transition-all cursor-pointer"
                    >
                      Clear All Filters
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 2-Column Sort and Filter Modal matching Image 1 & Image 2 */}
      <SortAndFilterModal
        isOpen={showFilterModal}
        onClose={() => setShowFilterModal(false)}
        routes={rawRoutes}
        activeSort={activeSort}
        setActiveSort={setActiveSort}
        selectedDepartureWindows={selectedDepartureWindows}
        setSelectedDepartureWindows={setSelectedDepartureWindows}
        selectedBusTypes={selectedBusTypes}
        setSelectedBusTypes={setSelectedBusTypes}
        selectedBoardingPoints={selectedBoardingPoints}
        setSelectedBoardingPoints={setSelectedBoardingPoints}
        selectedDroppingPoints={selectedDroppingPoints}
        setSelectedDroppingPoints={setSelectedDroppingPoints}
        selectedOperators={selectedOperators}
        setSelectedOperators={setSelectedOperators}
        selectedAmenities={selectedAmenities}
        setSelectedAmenities={setSelectedAmenities}
        isSingleSeatFilter={isSingleSeatFilter}
        setIsSingleSeatFilter={setIsSingleSeatFilter}
        isPrimoFilter={isPrimoFilter}
        setIsPrimoFilter={setIsPrimoFilter}
        isFreeCancellationFilter={isFreeCancellationFilter}
        setIsFreeCancellationFilter={setIsFreeCancellationFilter}
        isNonAcFilter={isNonAcFilter}
        setIsNonAcFilter={setIsNonAcFilter}
        aiFilterQuery={aiFilterQuery}
        setAiFilterQuery={setAiFilterQuery}
        onClearAll={clearAllFilters}
      />

      {/* Select Date Modal with Calendar matching Image 4 (Active in both Webview and Mobile view) */}
      <SelectDateModal
        isOpen={showDateModal}
        onClose={() => setShowDateModal(false)}
        selectedDate={editingDate}
        onSelectDate={(newDate) => {
          setEditingDate(newDate);
          dispatch(setTravelDate(newDate));
          router.push(
            `/search?source=${encodeURIComponent(editingSource)}&destination=${encodeURIComponent(
              editingDestination
            )}&date=${encodeURIComponent(newDate)}`
          );
        }}
      />
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense
      fallback={
        <div className="p-16 text-center text-sm font-bold text-gray-500">
          Loading RedBus search results...
        </div>
      }
    >
      <SearchResultsContent />
    </Suspense>
  );
}
