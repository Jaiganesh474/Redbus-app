"use client";

import React, { useState, useRef, useEffect, useMemo } from "react";
import { useRouter } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { useAppDispatch, useAppSelector } from "@/store";
import {
  setSelectedRoute,
  prepareCheckout,
  toggleSeatSelection,
  setBoardingPoint,
  setDroppingPoint,
} from "@/store/bookingSlice";
import { setSeatSelectionOpen } from "@/store/chatSlice";
import {
  useGetRouteSeatsQuery,
  useGetBusReviewsQuery,
  useGetBusDelayPredictionQuery,
  useGetDynamicPriceQuoteQuery,
} from "@/store/apiSlice";
import type { RouteItem, SeatItem } from "@/types";
import WriteReviewModal from "./WriteReviewModal";
import BusImageSlider from "./BusImageSlider";
import {
  X,
  Star,
  Clock,
  MapPin,
  Shield,
  Coffee,
  Wifi,
  Zap,
  Tv,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  ChevronRight,
  ChevronLeft,
  ArrowLeft,
  User as UserIcon,
  ShieldCheck,
  Award,
  CircleDot,
  Radio,
  Sparkles,
  Camera,
  TrendingUp,
  Search,
  Crosshair,
  Check,
} from "lucide-react";

interface BusDetailsModalProps {
  route: RouteItem;
  isOpen: boolean;
  onClose: () => void;
  initialTab?: string;
}

// Dynamic Cancellation Policy Generator based on actual travel date and departure time
function getDynamicCancellationTiers(
  travelDateStr?: string,
  departureTimeStr?: string,
  totalFare: number = 650
) {
  let baseDate = new Date();
  if (travelDateStr) {
    const parsed = new Date(travelDateStr);
    if (!isNaN(parsed.getTime())) {
      baseDate = parsed;
    }
  }

  let depHours = 22;
  let depMinutes = 45;
  if (departureTimeStr) {
    const parts = departureTimeStr.split(":");
    if (parts.length >= 2) {
      depHours = parseInt(parts[0], 10) || 22;
      depMinutes = parseInt(parts[1], 10) || 45;
    }
  }

  const departureDateTime = new Date(
    baseDate.getFullYear(),
    baseDate.getMonth(),
    baseDate.getDate(),
    depHours,
    depMinutes
  );

  const formatShortTime = (d: Date) => {
    const day = d.getDate();
    const suffix =
      day === 1 || day === 21 || day === 31
        ? "st"
        : day === 2 || day === 22
        ? "nd"
        : day === 3 || day === 23
        ? "rd"
        : "th";
    const month = d.toLocaleDateString("en-US", { month: "short" });
    const time = d.toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    });
    return `${day}${suffix} ${month} ${time}`;
  };

  const tMinus24h = new Date(departureDateTime.getTime() - 24 * 60 * 60 * 1000);
  const tMinus12h = new Date(departureDateTime.getTime() - 12 * 60 * 60 * 1000);
  const tMinus6h = new Date(departureDateTime.getTime() - 6 * 60 * 60 * 1000);
  const tMinus2h = new Date(departureDateTime.getTime() - 2 * 60 * 60 * 1000);

  return [
    {
      timeLabel: `Before ${formatShortTime(tMinus24h)}`,
      refundPercentWithout: 85,
      refundPercentWith: 100,
      refundAmountWithout: Math.round(totalFare * 0.85),
      refundAmountWith: totalFare,
    },
    {
      timeLabel: `From ${formatShortTime(tMinus24h)} Until ${formatShortTime(tMinus12h)}`,
      refundPercentWithout: 70,
      refundPercentWith: 100,
      refundAmountWithout: Math.round(totalFare * 0.70),
      refundAmountWith: totalFare,
    },
    {
      timeLabel: `From ${formatShortTime(tMinus12h)} Until ${formatShortTime(tMinus6h)}`,
      refundPercentWithout: 50,
      refundPercentWith: 100,
      refundAmountWithout: Math.round(totalFare * 0.50),
      refundAmountWith: totalFare,
    },
    {
      timeLabel: `From ${formatShortTime(tMinus6h)} Until ${formatShortTime(tMinus2h)}`,
      refundPercentWithout: 25,
      refundPercentWith: 100,
      refundAmountWithout: Math.round(totalFare * 0.25),
      refundAmountWith: totalFare,
    },
    {
      timeLabel: `From ${formatShortTime(tMinus2h)} Until Departure (${formatShortTime(departureDateTime)})`,
      refundPercentWithout: 0,
      refundPercentWith: 0,
      refundAmountWithout: 0,
      refundAmountWith: 0,
    },
  ];
}

interface CityStopInfo {
  name: string;
  address: string;
  area: string;
  offsetMins: number;
}

// City-specific Verified Stops Database
const CITY_STOPS_DATABASE: Record<string, CityStopInfo[]> = {
  bangalore: [
    { name: "Majestic", address: "Front of Bhagya Vinayaga Temple, Opp Amar Hotel", area: "Majestic / Railway Station", offsetMins: 0 },
    { name: "Anand Rao Circle", address: "Near Race Course Road Entrance", area: "Anand Rao Circle", offsetMins: 5 },
    { name: "Shantinagar", address: "BMTC Bus Stand, Double Road Entrance", area: "Shantinagar", offsetMins: 15 },
    { name: "Madiwala", address: "Opp Police Station, Near St. John's Hospital", area: "Madiwala", offsetMins: 25 },
    { name: "Silk Board", address: "Near Silk Board Flyover Junction, Hosur Road", area: "Silk Board", offsetMins: 35 },
    { name: "BTM Layout", address: "Near Udupi Garden Signal, 16th Main", area: "BTM Layout", offsetMins: 40 },
    { name: "Jayanagar", address: "4th Block Complex, Opp Bus Terminus", area: "Jayanagar", offsetMins: 45 },
    { name: "Koramangala", address: "Near Sony World Signal, 80 Feet Road", area: "Koramangala", offsetMins: 50 },
    { name: "Electronic City", address: "Toll Gate Entrance, Opp Infosys Gate", area: "Electronic City", offsetMins: 60 },
    { name: "Bellandur", address: "Near EcoSpace Tech Park, Outer Ring Road", area: "Bellandur", offsetMins: 70 },
    { name: "Marathahalli", address: "Near Bridge, Opp Multiplex Signal", area: "Marathahalli", offsetMins: 75 },
    { name: "Whitefield", address: "ITPL Main Gate, Hope Farm Junction", area: "Whitefield", offsetMins: 85 },
    { name: "Yeshwanthpur", address: "Near Metro Station & Govardhan Theatre", area: "Yeshwanthpur", offsetMins: 90 },
    { name: "Hebbal", address: "Hebbal Flyover, Near Esteem Mall", area: "Hebbal", offsetMins: 100 },
  ],
  chennai: [
    { name: "Koyambedu", address: "Omni Bus Stand Platform 4, Near Rohini Theatre", area: "Koyambedu CMBT", offsetMins: 0 },
    { name: "Ashok Nagar", address: "Near Metro Station Pillar 88, 100 Feet Road", area: "Ashok Nagar", offsetMins: 10 },
    { name: "Guindy", address: "Kathipara Junction, Near Metro Station", area: "Guindy", offsetMins: 20 },
    { name: "Central / Egmore", address: "Opp Chennai Central Railway Station", area: "Central", offsetMins: 25 },
    { name: "Perungudi", address: "Infront of Dominos Pizza, After Toll", area: "Perungudi OMR", offsetMins: 35 },
    { name: "Thoraipakkam", address: "OMR High Road, Opp BSR Mall", area: "Thoraipakkam", offsetMins: 40 },
    { name: "Karapakkam", address: "Infront of Karapakkam Bus Stand And Madurai Sre Meenakshi Hotel", area: "Karapakkam", offsetMins: 45 },
    { name: "Sholinganallur", address: "Infront of Royal Enfield Headquarters", area: "Sholinganallur Junction", offsetMins: 50 },
    { name: "Semmancherry", address: "Infront of Sathyabama University Arch", area: "Semmancherry", offsetMins: 55 },
    { name: "Navalur", address: "Infront of HP Petrol Bunk, After Navalur Toll", area: "Navalur Toll", offsetMins: 60 },
    { name: "Siruseri", address: "Infront of HDFC ATM, Opp A2B Adyar Ananda Bhavan", area: "OMR / Siruseri", offsetMins: 65 },
    { name: "Tambaram", address: "Near Railway Station West, GST Road", area: "Tambaram", offsetMins: 75 },
    { name: "Chromepet", address: "Near MIT Bridge, GST Road", area: "Chromepet", offsetMins: 80 },
    { name: "Porur", address: "Near Roundtana, Opp Saravana Stores", area: "Porur Toll", offsetMins: 90 },
    { name: "Poonamallee Bypass", address: "Infront of Sai Sasi Mahal, Near Ambedkar Statue", area: "Poonamallee Bypass", offsetMins: 105 },
    { name: "Poonamallee (KFC)", address: "Infront of KFC, Motel Highway", area: "Poonamallee Highway", offsetMins: 110 },
    { name: "Sriperumbudur", address: "Opp Sriperumbudur Toll Plaza Arch", area: "Sriperumbudur", offsetMins: 125 },
  ],
  hyderabad: [
    { name: "MGBS", address: "Mahatma Gandhi Bus Station, Platform 6", area: "MGBS / Imlibun", offsetMins: 0 },
    { name: "Ameerpet", address: "Near Big Bazaar, Metro Pillar A1042", area: "Ameerpet", offsetMins: 15 },
    { name: "SR Nagar", address: "Near Community Hall, Main Road", area: "SR Nagar", offsetMins: 20 },
    { name: "KPHB Colony", address: "Near Metro Station, Pillar 740", area: "KPHB", offsetMins: 30 },
    { name: "Kukatpally", address: "Near Y Junction, Opp BJP Office", area: "Kukatpally", offsetMins: 35 },
    { name: "Miyapur", address: "Allwyn X Roads, Near Metro Station", area: "Miyapur", offsetMins: 45 },
    { name: "Gachibowli", address: "Outer Ring Road Junction, Opp Bio Diversity Park", area: "Gachibowli", offsetMins: 55 },
    { name: "Hitec City", address: "Cyber Towers Signal, Madhapur", area: "Hitec City", offsetMins: 60 },
    { name: "Shamshabad", address: "Near Airport Toll Gate Plaza", area: "Shamshabad", offsetMins: 75 },
  ],
  coimbatore: [
    { name: "Gandhipuram", address: "Omni Bus Stand, Cross Cut Road", area: "Gandhipuram", offsetMins: 0 },
    { name: "Hopes College", address: "Avinashi Road Bus Stop", area: "Hopes College", offsetMins: 15 },
    { name: "KMCH", address: "Near Hospital Gate, Airport Bypass", area: "KMCH", offsetMins: 25 },
    { name: "Neelambur", address: "L&T Toll Plaza Bypass", area: "Neelambur", offsetMins: 35 },
    { name: "Singanallur", address: "Near Singanallur Bus Stand", area: "Singanallur", offsetMins: 45 },
  ],
  mumbai: [
    { name: "Borivali", address: "National Park Gate, Western Express Highway", area: "Borivali East", offsetMins: 0 },
    { name: "Andheri", address: "Near Bisleri Factory, WEH", area: "Andheri East", offsetMins: 15 },
    { name: "Bandra", address: "Kalanagar Junction, Near Highway", area: "Bandra East", offsetMins: 25 },
    { name: "Sion", address: "Near Cinemax / Chunabhatti Bridge", area: "Sion", offsetMins: 35 },
    { name: "Chembur", address: "Near Maitri Park, Amar Mahal", area: "Chembur", offsetMins: 45 },
    { name: "Vashi", address: "Old Toll Naka / Highway Bridge", area: "Vashi", offsetMins: 60 },
    { name: "Panvel", address: "Kalamboli / Near McDonald's Highway", area: "Panvel", offsetMins: 75 },
  ],
  pune: [
    { name: "Wakad", address: "Near Ginger Hotel / Hinjawadi Bridge", area: "Wakad", offsetMins: 0 },
    { name: "Hinjawadi", address: "Hinjawadi Flyover, Phase 1", area: "Hinjawadi", offsetMins: 10 },
    { name: "Baner", address: "Near Balewadi Stadium Bridge", area: "Baner", offsetMins: 20 },
    { name: "Swargate", address: "Near Laxmi Narayan Theatre", area: "Swargate", offsetMins: 35 },
    { name: "Shivaji Nagar", address: "Bank of Maharashtra, Pune Railway", area: "Shivaji Nagar", offsetMins: 45 },
    { name: "Viman Nagar", address: "Near Phoenix Marketcity", area: "Viman Nagar", offsetMins: 60 },
  ],
};

function cleanStopName(rawName: string): { cleanName: string; extractedTime?: string } {
  const match = rawName.match(/^(.*?)\s*\((\d{1,2}:\d{2})\)\s*$/);
  if (match) {
    return { cleanName: match[1].trim(), extractedTime: match[2].trim() };
  }
  return { cleanName: rawName.trim() };
}

// Generate Realistic, Strictly City-Specific Stops
function getStopsForCity(
  city: string,
  baseTimeStr: string,
  isDropping: boolean,
  customPoints?: string[]
) {
  const cityKey = (city || "").toLowerCase().replace(/[^a-z]/g, "");
  const matchedKey =
    cityKey.includes("bengaluru") || cityKey.includes("bangalore")
      ? "bangalore"
      : cityKey.includes("chennai") || cityKey.includes("madras")
      ? "chennai"
      : cityKey.includes("hyderabad") || cityKey.includes("secunderabad")
      ? "hyderabad"
      : cityKey.includes("coimbatore")
      ? "coimbatore"
      : cityKey.includes("mumbai") || cityKey.includes("bombay")
      ? "mumbai"
      : cityKey.includes("pune")
      ? "pune"
      : "";

  const cityDatabase = matchedKey ? CITY_STOPS_DATABASE[matchedKey] || [] : [];

  let [hours, mins] = isDropping ? [5, 40] : [20, 15];
  if (baseTimeStr) {
    const parts = baseTimeStr.split(":");
    if (parts.length >= 2) {
      hours = parseInt(parts[0], 10) || (isDropping ? 5 : 20);
      mins = parseInt(parts[1], 10) || (isDropping ? 40 : 15);
    }
  }

  // 1. If customPoints exist for route
  if (customPoints && customPoints.length > 0) {
    return customPoints.map((pt, i) => {
      const { cleanName, extractedTime } = cleanStopName(pt);
      const knownStop = cityDatabase.find(
        (s) => s.name.toLowerCase() === cleanName.toLowerCase()
      );

      let finalTime = extractedTime;
      if (!finalTime) {
        const totalMinutes = (hours * 60 + mins + (knownStop?.offsetMins ?? i * 15)) % (24 * 60);
        const stopHours = Math.floor(totalMinutes / 60);
        const stopMins = totalMinutes % 60;
        finalTime = `${String(stopHours).padStart(2, "0")}:${String(stopMins).padStart(2, "0")}`;
      }

      return {
        name: cleanName,
        time: finalTime,
        address: knownStop ? knownStop.address : `Near ${cleanName} Main Bus Bay & Terminal`,
        area: knownStop ? knownStop.area : cleanName,
      };
    });
  }

  // 2. City verified stops database
  if (cityDatabase.length > 0) {
    return cityDatabase.map((item) => {
      const totalMinutes = (hours * 60 + mins + item.offsetMins) % (24 * 60);
      const stopHours = Math.floor(totalMinutes / 60);
      const stopMins = totalMinutes % 60;
      const formattedTime = `${String(stopHours).padStart(2, "0")}:${String(stopMins).padStart(2, "0")}`;
      return {
        name: item.name,
        time: formattedTime,
        address: item.address,
        area: item.area,
      };
    });
  }

  // 3. Fallback generic stops
  const genericStops = [
    { name: `${city} Central Bus Terminal`, offset: 0 },
    { name: `${city} Bypass Highway Toll`, offset: 20 },
    { name: `${city} Railway Station Junction`, offset: 35 },
    { name: `${city} Outer Ring Road`, offset: 50 },
  ];

  return genericStops.map((item) => {
    const totalMinutes = (hours * 60 + mins + item.offset) % (24 * 60);
    const stopHours = Math.floor(totalMinutes / 60);
    const stopMins = totalMinutes % 60;
    return {
      name: item.name,
      time: `${String(stopHours).padStart(2, "0")}:${String(stopMins).padStart(2, "0")}`,
      address: `Main Boarding / Dropping Bay, ${city}`,
      area: item.name,
    };
  });
}

export default function BusDetailsModal({
  route,
  isOpen,
  onClose,
  initialTab = "cancellation",
}: BusDetailsModalProps) {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const tabsRef = useRef<HTMLDivElement>(null);
  const { user } = useAppSelector((state) => state.auth);
  const searchState = useAppSelector((state) => state.search);
  const { selectedSeats, boardingPoint, droppingPoint } = useAppSelector(
    (state) => state.booking
  );

  // Active step flow: "SEATS" -> "BOARD_DROP"
  const [activeStep, setActiveStep] = useState<"SEATS" | "BOARD_DROP">("SEATS");
  const [boardDropSubTab, setBoardDropSubTab] = useState<"boarding" | "dropping">("boarding");
  const [areaSearchTerm, setAreaSearchTerm] = useState("");

  const [activeTab, setActiveTab] = useState<
    | "highlights"
    | "photos"
    | "ml_insights"
    | "cancellation"
    | "boarding"
    | "dropping"
    | "route"
    | "rest_stop"
    | "features"
    | "reviews"
    | "safety"
  >((initialTab as any) || "highlights");

  const [showWriteReview, setShowWriteReview] = useState(false);
  const [showAllBoarding, setShowAllBoarding] = useState(false);
  const [showAllDropping, setShowAllDropping] = useState(false);

  // Effective travel date
  const effectiveTravelDate =
    route.travelDate || searchState.travelDate || new Date().toISOString().split("T")[0];

  const formattedJourneyDate = useMemo(() => {
    try {
      const d = new Date(effectiveTravelDate);
      if (!isNaN(d.getTime())) {
        return d.toLocaleDateString("en-US", {
          weekday: "short",
          day: "numeric",
          month: "short",
        });
      }
    } catch {}
    return effectiveTravelDate;
  }, [effectiveTravelDate]);

  const { data: layoutData, isLoading: isSeatsLoading } = useGetRouteSeatsQuery(route.id, {
    refetchOnMountOrArgChange: true,
  });
  const { data: reviewData, refetch: refetchReviews } = useGetBusReviewsQuery(route.busId || route.id);
  const { data: delayPrediction } = useGetBusDelayPredictionQuery(
    {
      source: route.sourceCity,
      destination: route.destinationCity,
      departureTime: route.departureTime,
    },
    { skip: !isOpen }
  );

  // Realistic 1-second seat loading timer so seat layout loading looks authentic
  const [isMinSeatLoading, setIsMinSeatLoading] = useState(true);

  // Prevent background scrolling and hide Ask RAY on mobile when modal is open
  useEffect(() => {
    if (isOpen) {
      dispatch(setSeatSelectionOpen(true));
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = originalOverflow || "auto";
        dispatch(setSeatSelectionOpen(false));
      };
    } else {
      dispatch(setSeatSelectionOpen(false));
    }
  }, [isOpen, dispatch]);

  useEffect(() => {
    if (isOpen) {
      setIsMinSeatLoading(true);
      setActiveStep("SEATS");
      setBoardDropSubTab("boarding");
      setAreaSearchTerm("");
      const timer = setTimeout(() => {
        setIsMinSeatLoading(false);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [isOpen, route.id]);

  const showSeatsLoader = isSeatsLoading || isMinSeatLoading;

  const currentBoarding = boardingPoint || "";
  const currentDropping = droppingPoint || "";

  const seats = layoutData?.seats || [];
  const lowerSeats = seats.filter((s) => s.deck === "LOWER");
  const upperSeats = seats.filter((s) => s.deck === "UPPER");

  const totalPrice = selectedSeats.reduce((acc, s) => acc + (s.price || route.basePrice), 0);

  const handleSeatClick = (seat: SeatItem) => {
    if (seat.status !== "AVAILABLE") return;
    dispatch(setSelectedRoute(route));
    dispatch(toggleSeatSelection(seat));
  };

  const handleProceedToBoardDrop = () => {
    if (selectedSeats.length === 0) return;
    setActiveStep("BOARD_DROP");
    if (!currentBoarding) {
      setBoardDropSubTab("boarding");
    } else if (!currentDropping) {
      setBoardDropSubTab("dropping");
    }
  };

  const handleProceedToBook = () => {
    if (selectedSeats.length === 0) return;
    if (!currentBoarding || !currentDropping) {
      setActiveStep("BOARD_DROP");
      if (!currentBoarding) setBoardDropSubTab("boarding");
      else setBoardDropSubTab("dropping");
      return;
    }

    dispatch(
      prepareCheckout({
        route,
        seats: selectedSeats,
        boardingPoint: currentBoarding,
        droppingPoint: currentDropping,
      })
    );
    onClose();
    router.push(`/checkout?routeId=${route.id}`);
  };

  // Structured City-Specific Boarding & Dropping Stops
  const boardingStops = useMemo(() => {
    return getStopsForCity(route.sourceCity, route.departureTime, false, route.boardingPoints);
  }, [route.sourceCity, route.departureTime, route.boardingPoints]);

  const droppingStops = useMemo(() => {
    return getStopsForCity(route.destinationCity, route.arrivalTime, true, route.droppingPoints);
  }, [route.destinationCity, route.arrivalTime, route.droppingPoints]);

  // Filtered stops for Boarding & Dropping screen
  const currentStopsList = boardDropSubTab === "boarding" ? boardingStops : droppingStops;
  const filteredStops = useMemo(() => {
    const term = areaSearchTerm.trim().toLowerCase();
    if (!term) return currentStopsList;
    return currentStopsList.filter(
      (s) =>
        s.name.toLowerCase().includes(term) ||
        s.address.toLowerCase().includes(term) ||
        s.area.toLowerCase().includes(term)
    );
  }, [currentStopsList, areaSearchTerm]);

  // Dynamic Cancellation policy tiers
  const cancellationTiers = useMemo(() => {
    return getDynamicCancellationTiers(
      effectiveTravelDate,
      route.departureTime,
      totalPrice > 0 ? totalPrice : route.basePrice
    );
  }, [effectiveTravelDate, route.departureTime, totalPrice, route.basePrice]);

  // Group seats by rowNum
  const getDeckRows = (deckSeats: SeatItem[]) => {
    const rowsMap = new Map<number, SeatItem[]>();
    deckSeats.forEach((s, idx) => {
      const r = s.rowNum && s.rowNum > 0 ? s.rowNum : Math.floor(idx / 3) + 1;
      if (!rowsMap.has(r)) rowsMap.set(r, []);
      rowsMap.get(r)!.push(s);
    });
    return Array.from(rowsMap.keys()).sort((a, b) => a - b);
  };

  // Automatically cleanse selectedSeats if any seat is booked or locked on server
  useEffect(() => {
    if (layoutData?.seats && selectedSeats.length > 0) {
      const bookedOrLockedIds = new Set(
        layoutData.seats.filter((s) => s.status === "BOOKED").map((s) => s.seatId)
      );
      const invalidSeats = selectedSeats.filter((s) => bookedOrLockedIds.has(s.seatId));
      if (invalidSeats.length > 0) {
        invalidSeats.forEach((s) => {
          dispatch(toggleSeatSelection(s));
        });
      }
    }
  }, [layoutData?.seats, selectedSeats, dispatch]);

  // Render authentic sleeper berth
  const renderSleeperBerth = (seat: SeatItem) => {
    const isSold = seat.status === "BOOKED" || seat.status === "LOCKED";
    const isSelected = !isSold && selectedSeats.some((s) => s.seatId === seat.seatId);
    const isFemale = seat.genderRestriction === "FEMALE" || seat.bookedGender === "FEMALE";

    let containerStyle =
      "relative w-10 sm:w-12 h-18 sm:h-22 rounded-xl flex flex-col items-center justify-between py-1.5 sm:py-2 px-1 transition-all select-none ";
    let pillowStyle = "w-5 sm:w-7 h-1 sm:h-1.5 rounded-full transition-all ";
    let priceOrLabel = `₹${Math.round(seat.price || route.basePrice)}`;
    let priceColor = "text-gray-800 dark:text-slate-200 font-extrabold";

    if (isSold) {
      if (isFemale) {
        containerStyle +=
          "border border-pink-200 dark:border-pink-900/40 bg-pink-50/75 dark:bg-pink-950/25 cursor-not-allowed";
        pillowStyle += "bg-pink-200 dark:bg-pink-800/60";
      } else {
        containerStyle +=
          "border border-gray-200 dark:border-slate-700 bg-gray-50/90 dark:bg-slate-800/60 cursor-not-allowed";
        pillowStyle += "bg-gray-200 dark:bg-slate-600";
      }
      priceOrLabel = "Sold";
      priceColor = "text-gray-400 dark:text-slate-500 font-medium";
    } else if (isSelected) {
      containerStyle +=
        "bg-[#15803d] border-2 border-[#15803d] text-white shadow-md scale-105 ring-2 ring-emerald-400/40 cursor-pointer";
      pillowStyle += "bg-emerald-300";
      priceColor = "text-white font-black";
    } else {
      containerStyle +=
        "bg-white dark:bg-slate-800 border-2 border-emerald-500 hover:border-emerald-600 hover:shadow-md hover:bg-emerald-50/40 dark:hover:bg-emerald-950/20 cursor-pointer active:scale-95";
      pillowStyle += "bg-emerald-100 dark:bg-emerald-800/50";
      priceColor = "text-gray-800 dark:text-slate-200 font-extrabold";
    }

    return (
      <button
        key={seat.seatId}
        type="button"
        disabled={isSold}
        onClick={() => handleSeatClick(seat)}
        className={containerStyle}
        title={`Seat ${seat.seatNumber} (${seat.deck} Deck) - ${priceOrLabel}`}
      >
        <div className={pillowStyle} />
        <div className="flex items-center justify-center my-auto">
          {isSold ? (
            isFemale ? (
              <UserIcon className="w-3 sm:w-3.5 h-3 sm:h-3.5 text-pink-400 dark:text-pink-300" />
            ) : (
              <UserIcon className="w-3 sm:w-3.5 h-3 sm:h-3.5 text-gray-400 dark:text-slate-500" />
            )
          ) : isSelected ? (
            <CheckCircle2 className="w-3.5 sm:w-4 h-3.5 sm:h-4 text-white" />
          ) : null}
        </div>
        <div className="flex flex-col items-center leading-none">
          <span className={`text-[9px] sm:text-[11px] tracking-tight ${priceColor}`}>
            {priceOrLabel}
          </span>
          <span
            className={`text-[8px] mt-0.5 ${
              isSelected ? "text-emerald-100 font-semibold" : "text-gray-400 dark:text-slate-400 font-medium"
            }`}
          >
            {seat.seatNumber}
          </span>
        </div>
      </button>
    );
  };

  // Render authentic seater chair
  const renderSeaterChair = (seat: SeatItem) => {
    const isSold = seat.status === "BOOKED" || seat.status === "LOCKED";
    const isSelected = !isSold && selectedSeats.some((s) => s.seatId === seat.seatId);
    const isFemale = seat.genderRestriction === "FEMALE" || seat.bookedGender === "FEMALE";

    const strokeColor = isSold ? (isFemale ? "#f472b6" : "#cbd5e1") : isSelected ? "#15803d" : "#10b981";
    const fillColor = isSold ? (isFemale ? "#fdf2f8" : "#f8fafc") : isSelected ? "#dcfce7" : "#ffffff";
    const priceColor = isSold
      ? "text-gray-400 font-medium"
      : isSelected
      ? "text-emerald-700 font-extrabold"
      : "text-gray-800 dark:text-slate-200 font-bold";

    return (
      <div key={seat.seatId} className="flex flex-col items-center">
        <button
          type="button"
          disabled={isSold}
          onClick={() => handleSeatClick(seat)}
          className="relative w-9 sm:w-11 h-11 sm:h-12 flex flex-col items-center justify-center transition-all cursor-pointer disabled:cursor-not-allowed hover:scale-105 active:scale-95"
          title={`Seat ${seat.seatNumber} - ₹${seat.price || route.basePrice}`}
        >
          <svg className="w-8 sm:w-9 h-10 sm:h-11" viewBox="0 0 44 48" fill="none">
            <rect x="6" y="8" width="32" height="34" rx="8" fill={fillColor} stroke={strokeColor} strokeWidth="2.5" />
            <path d="M11 26h22" stroke={strokeColor} strokeWidth="2" strokeLinecap="round" />
            <rect
              x="13"
              y="11"
              width="18"
              height="8"
              rx="4"
              fill={isSold ? (isFemale ? "#fbcfe8" : "#cbd5e1") : isSelected ? "#86efac" : "#bbf7d0"}
            />
          </svg>
        </button>
        <span className={`text-[9px] sm:text-[10px] font-bold ${priceColor}`}>
          {isSold ? "Sold" : `₹${Math.round(seat.price || route.basePrice)}`}
        </span>
        <span className="text-[8px] text-gray-400 font-medium">{seat.seatNumber}</span>
      </div>
    );
  };

  // Render Deck Chassis
  const renderDeckChassis = (title: string, deckSeats: SeatItem[], isLowerDeck: boolean) => {
    const rows = getDeckRows(deckSeats);

    return (
      <div className="flex-1 max-w-[260px] min-w-[150px] sm:min-w-[190px]">
        <div className="flex items-center justify-between px-2 sm:px-3 mb-2">
          <span className="text-[11px] sm:text-xs font-black text-gray-800 dark:text-white uppercase tracking-wider">
            {title}
          </span>
          {isLowerDeck ? (
            <div
              className="w-6 sm:w-7 h-6 sm:h-7 rounded-full border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 flex items-center justify-center text-gray-500 shadow-2xs"
              title="Driver Steering Wheel"
            >
              <svg className="w-3.5 sm:w-4 h-3.5 sm:h-4 text-gray-600 dark:text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="9" />
                <circle cx="12" cy="12" r="3" />
                <line x1="12" y1="3" x2="12" y2="9" />
                <line x1="3" y1="12" x2="9" y2="12" />
                <line x1="15" y1="12" x2="21" y2="12" />
                <line x1="12" y1="15" x2="12" y2="21" />
              </svg>
            </div>
          ) : (
            <div
              className="w-6 sm:w-7 h-6 sm:h-7 rounded-full border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 flex items-center justify-center text-xs text-gray-400 font-bold"
              title="Upper Deck"
            >
              ⬆
            </div>
          )}
        </div>

        <div className="rounded-t-[28px] sm:rounded-t-[36px] rounded-b-2xl border-2 border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800/90 p-2 sm:p-4 shadow-xs flex flex-col justify-between min-h-[420px] sm:min-h-[460px]">
          <div className="space-y-2.5 sm:space-y-3.5">
            {rows.map((rowNum) => {
              const rowSeats = deckSeats.filter((s, idx) => {
                const r = s.rowNum && s.rowNum > 0 ? s.rowNum : Math.floor(idx / 3) + 1;
                return r === rowNum;
              });

              let leftSeat = rowSeats.find((s) => s.colNum === 1);
              let rightSeat1 = rowSeats.find((s) => s.colNum === 2);
              let rightSeat2 = rowSeats.find((s) => s.colNum === 3);

              if (!leftSeat && rowSeats.length > 0) leftSeat = rowSeats[0];
              if (!rightSeat1 && rowSeats.length > 1) rightSeat1 = rowSeats[1];
              if (!rightSeat2 && rowSeats.length > 2) rightSeat2 = rowSeats[2];

              return (
                <div key={rowNum} className="flex items-center justify-between">
                  <div className="w-10 sm:w-12 flex justify-center">
                    {leftSeat ? (
                      leftSeat.seatType === "SEATER"
                        ? renderSeaterChair(leftSeat)
                        : renderSleeperBerth(leftSeat)
                    ) : (
                      <div className="w-10 sm:w-12 h-18 sm:h-22" />
                    )}
                  </div>

                  <div className="w-3 sm:w-6" />

                  <div className="flex items-center space-x-1 sm:space-x-2">
                    {rightSeat1 ? (
                      rightSeat1.seatType === "SEATER"
                        ? renderSeaterChair(rightSeat1)
                        : renderSleeperBerth(rightSeat1)
                    ) : (
                      <div className="w-10 sm:w-12 h-18 sm:h-22" />
                    )}
                    {rightSeat2 ? (
                      rightSeat2.seatType === "SEATER"
                        ? renderSeaterChair(rightSeat2)
                        : renderSleeperBerth(rightSeat2)
                    ) : (
                      <div className="w-10 sm:w-12 h-18 sm:h-22" />
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="pt-3 pb-1 text-center border-t border-dashed border-gray-200 dark:border-slate-700/60 mt-3">
            <span className="inline-flex items-center text-[8px] sm:text-[9px] font-bold text-gray-400 dark:text-slate-500 uppercase tracking-widest">
              ▼ Emergency Exit
            </span>
          </div>
        </div>
      </div>
    );
  };

  const scrollTabs = (direction: "left" | "right") => {
    if (tabsRef.current) {
      const scrollAmount = direction === "left" ? -220 : 220;
      tabsRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
    }
  };

  return (
    <>
      <AnimatePresence>
        {isOpen && (
          <div className="fixed inset-0 z-50 overflow-y-auto flex items-center justify-center p-0 sm:p-4">
            {/* Backdrop */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={onClose}
              className="fixed inset-0 bg-black/65 backdrop-blur-xs cursor-pointer"
            />

            {/* Modal Container */}
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 15 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 15 }}
              transition={{ type: "spring", damping: 26, stiffness: 320 }}
              className="bg-white dark:bg-[#0b0f19] rounded-none sm:rounded-3xl w-full max-w-6xl xl:max-w-7xl shadow-2xl border-0 sm:border border-gray-200 dark:border-slate-800 flex flex-col h-[100dvh] sm:h-auto max-h-[100dvh] sm:max-h-[92vh] overflow-hidden relative z-10"
            >
              {/* TOP HEADER: Clean Navigation + Journey Date */}
              <div className="px-3.5 sm:px-6 py-3 sm:py-4 bg-white dark:bg-[#0f172a] border-b border-gray-200 dark:border-slate-800 flex items-center justify-between shrink-0">
                <div className="flex items-center space-x-2.5 sm:space-x-3 min-w-0">
                  {activeStep === "BOARD_DROP" ? (
                    <button
                      type="button"
                      onClick={() => setActiveStep("SEATS")}
                      className="p-1.5 sm:p-2 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 text-gray-700 dark:text-slate-300 hover:text-gray-900 transition-colors cursor-pointer flex items-center justify-center shrink-0"
                      title="Back to Seat Selection"
                    >
                      <ArrowLeft className="w-5 h-5 stroke-[2.5]" />
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={onClose}
                      className="p-1.5 sm:p-2 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 text-gray-500 dark:text-slate-400 hover:text-gray-900 transition-colors cursor-pointer shrink-0"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  )}

                  <div className="min-w-0">
                    <h2 className="text-sm sm:text-base font-black text-gray-900 dark:text-white flex items-center space-x-1.5 truncate">
                      {activeStep === "BOARD_DROP" ? (
                        <span>Select boarding & dropping points</span>
                      ) : (
                        <>
                          <span>{route.sourceCity}</span>
                          <span className="text-[#d84e55]">➔</span>
                          <span>{route.destinationCity}</span>
                        </>
                      )}
                    </h2>
                    <p className="text-[11px] sm:text-xs text-gray-500 dark:text-slate-400 font-medium truncate">
                      {activeStep === "BOARD_DROP" ? (
                        <span className="font-semibold text-gray-700 dark:text-slate-300">
                          {route.sourceCity} ➔ {route.destinationCity} • {formattedJourneyDate}
                        </span>
                      ) : (
                        <>
                          <span className="font-bold text-gray-800 dark:text-slate-200">{route.operatorName}</span>
                          <span> • </span>
                          <span>{route.busType}</span>
                          <span> • </span>
                          <span className="text-[#d84e55] font-bold">{formattedJourneyDate}</span>
                          <span className="hidden md:inline"> ({route.departureTime} - {route.arrivalTime})</span>
                        </>
                      )}
                    </p>
                  </div>
                </div>

                {/* Center Steps breadcrumbs (Desktop) */}
                <div className="hidden md:flex items-center space-x-8 text-xs font-bold shrink-0">
                  <button
                    type="button"
                    onClick={() => setActiveStep("SEATS")}
                    className={`pb-1 border-b-2 transition-all cursor-pointer ${
                      activeStep === "SEATS"
                        ? "border-[#d84e55] text-[#d84e55]"
                        : "border-transparent text-gray-500 hover:text-gray-800 dark:text-slate-400"
                    }`}
                  >
                    Select seats
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (selectedSeats.length > 0) {
                        setActiveStep("BOARD_DROP");
                      }
                    }}
                    className={`pb-1 border-b-2 transition-all cursor-pointer ${
                      activeStep === "BOARD_DROP"
                        ? "border-[#d84e55] text-[#d84e55]"
                        : selectedSeats.length > 0
                        ? "border-transparent text-gray-600 hover:text-gray-900"
                        : "border-transparent text-gray-300 cursor-not-allowed"
                    }`}
                  >
                    Board/Drop point
                  </button>
                  <button
                    type="button"
                    onClick={handleProceedToBook}
                    disabled={selectedSeats.length === 0 || !currentBoarding || !currentDropping}
                    className={`pb-1 border-b-2 transition-all cursor-pointer ${
                      currentBoarding && currentDropping
                        ? "border-transparent text-gray-600 hover:text-[#d84e55]"
                        : "border-transparent text-gray-300 cursor-not-allowed"
                    }`}
                  >
                    Passenger Info
                  </button>
                </div>

                {/* Rating Badge */}
                <div className="flex items-center space-x-2 shrink-0">
                  <div className="flex items-center space-x-1 px-2.5 py-1 bg-[#15803d] text-white rounded-lg text-xs font-bold shadow-2xs">
                    <Star className="w-3.5 h-3.5 fill-white" />
                    <span>{reviewData?.averageRating?.toFixed(1) || route.rating.toFixed(1)}</span>
                  </div>
                  <span className="text-xs text-gray-500 dark:text-slate-400 font-medium hidden sm:inline">
                    {reviewData?.totalRatings || 427} ratings
                  </span>
                </div>
              </div>

              {/* ========================================================================= */}
              {/* VIEW 1: SELECT SEATS STEP */}
              {/* ========================================================================= */}
              {activeStep === "SEATS" && (
                <div className="flex-1 overflow-y-auto grid grid-cols-1 lg:grid-cols-12 divide-y lg:divide-y-0 lg:divide-x divide-gray-200 dark:divide-slate-800">
                  {/* Left Column: Redbus Dual Deck Seat Map */}
                  <div className="lg:col-span-6 p-3 sm:p-5 bg-gray-50/70 dark:bg-slate-900/60 overflow-y-auto flex flex-col items-center">
                    <div className="w-full max-w-xl space-y-3 sm:space-y-4">
                      {/* Decks Header */}
                      <div className="flex items-center justify-between px-2">
                        <span className="text-xs font-black text-gray-800 dark:text-slate-200 uppercase tracking-wider">
                          Select Seats
                        </span>
                        <span className="text-[11px] text-[#d84e55] font-bold">
                          {route.availableSeats} Seats Available
                        </span>
                      </div>

                      {showSeatsLoader ? (
                        <div className="w-full space-y-3.5 animate-pulse">
                          <div className="p-3 bg-white dark:bg-slate-800 rounded-2xl border border-gray-200 dark:border-slate-700 shadow-2xs flex items-center justify-between">
                            <div className="flex items-center space-x-2.5">
                              <div className="w-4 h-4 border-2 border-[#d84e55] border-t-transparent rounded-full animate-spin shrink-0" />
                              <span className="text-xs font-bold text-gray-800 dark:text-slate-200">
                                Connecting to {route.operatorName} live seat inventory...
                              </span>
                            </div>
                            <span className="text-[10px] text-[#d84e55] font-black uppercase tracking-wider bg-red-50 dark:bg-red-950/40 px-2 py-0.5 rounded">
                              Syncing
                            </span>
                          </div>
                          <div className="grid grid-cols-2 gap-4">
                            <div className="h-80 sm:h-96 bg-gray-200 dark:bg-slate-700 rounded-3xl" />
                            <div className="h-80 sm:h-96 bg-gray-200 dark:bg-slate-700 rounded-3xl" />
                          </div>
                        </div>
                      ) : (
                        <div className="flex flex-row justify-center gap-2 sm:gap-6">
                          {lowerSeats.length > 0 && renderDeckChassis("Lower Deck", lowerSeats, true)}
                          {upperSeats.length > 0 && renderDeckChassis("Upper Deck", upperSeats, false)}
                        </div>
                      )}

                      {/* Legend */}
                      <div className="flex flex-wrap items-center justify-center gap-2.5 sm:gap-5 text-[10px] sm:text-[11px] text-gray-600 dark:text-slate-400 font-bold pt-2 border-t border-gray-200 dark:border-slate-800">
                        <div className="flex items-center space-x-1.5">
                          <div className="w-3.5 sm:w-4 h-3.5 sm:h-4 rounded-sm border-2 border-emerald-500 bg-white dark:bg-slate-800" />
                          <span>Available</span>
                        </div>
                        <div className="flex items-center space-x-1.5">
                          <div className="w-3.5 sm:w-4 h-3.5 sm:h-4 rounded-sm bg-[#15803d] border-2 border-[#15803d]" />
                          <span>Selected</span>
                        </div>
                        <div className="flex items-center space-x-1.5">
                          <div className="w-3.5 sm:w-4 h-3.5 sm:h-4 rounded-sm bg-gray-200 dark:bg-slate-700 border border-gray-300 dark:border-slate-600" />
                          <span>Sold</span>
                        </div>
                        <div className="flex items-center space-x-1.5">
                          <div className="w-3.5 sm:w-4 h-3.5 sm:h-4 rounded-sm border border-pink-200 bg-pink-100 dark:bg-pink-950/40 flex items-center justify-center">
                            <div className="w-2 h-2 rounded-full bg-pink-500" />
                          </div>
                          <span>Female</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Right Column: Tabbed Detail Panels */}
                  <div className="lg:col-span-6 flex flex-col justify-between bg-white dark:bg-[#0b0f19] overflow-hidden">
                    {/* Scrollable Tabs Bar */}
                    <div className="relative flex items-center px-1.5 py-2 border-b border-gray-200 dark:border-slate-800 bg-white dark:bg-[#0f172a]">
                      <button
                        type="button"
                        onClick={() => scrollTabs("left")}
                        className="w-7 h-7 rounded-full bg-white dark:bg-slate-800 border border-gray-300 dark:border-slate-600 shadow-md hover:border-[#d84e55] hover:text-[#d84e55] text-gray-700 dark:text-slate-200 transition-all flex items-center justify-center shrink-0 z-10 mr-1.5 cursor-pointer active:scale-90"
                      >
                        <ChevronLeft className="w-4 h-4 stroke-[2.5]" />
                      </button>

                      <div
                        ref={tabsRef}
                        className="flex-1 flex items-center space-x-1.5 overflow-x-auto whitespace-nowrap scrollbar-none scroll-smooth text-xs font-bold px-1"
                      >
                        {[
                          { id: "highlights", label: "Highlights" },
                          { id: "photos", label: "📸 Bus Photos" },
                          { id: "ml_insights", label: "⚡ AI Punctuality & Demand" },
                          { id: "cancellation", label: "Cancellation policy" },
                          { id: "boarding", label: "Boarding point" },
                          { id: "dropping", label: "Dropping point" },
                          { id: "route", label: "Bus route" },
                          { id: "rest_stop", label: "Rest stop" },
                          { id: "features", label: "Bus Features" },
                          { id: "reviews", label: "Rating and reviews" },
                          { id: "safety", label: "Bus Safety" },
                        ].map((tab) => (
                          <button
                            key={tab.id}
                            type="button"
                            onClick={() => setActiveTab(tab.id as any)}
                            className={`px-3 py-1.5 rounded-lg transition-all shrink-0 cursor-pointer ${
                              activeTab === tab.id
                                ? "text-[#d84e55] border-b-2 border-[#d84e55] font-extrabold bg-red-50/50 dark:bg-red-950/40"
                                : "text-gray-500 dark:text-slate-400 hover:text-gray-800 dark:hover:text-slate-200"
                            }`}
                          >
                            {tab.label}
                          </button>
                        ))}
                      </div>

                      <button
                        type="button"
                        onClick={() => scrollTabs("right")}
                        className="w-7 h-7 rounded-full bg-white dark:bg-slate-800 border border-gray-300 dark:border-slate-600 shadow-md hover:border-[#d84e55] hover:text-[#d84e55] text-gray-700 dark:text-slate-200 transition-all flex items-center justify-center shrink-0 z-10 ml-1.5 cursor-pointer active:scale-90"
                      >
                        <ChevronRight className="w-4 h-4 stroke-[2.5]" />
                      </button>
                    </div>

                    {/* Tab Contents */}
                    <div className="flex-1 p-4 sm:p-5 overflow-y-auto space-y-6">
                      <AnimatePresence mode="wait">
                        <motion.div
                          key={activeTab}
                          initial={{ opacity: 0, y: 8 }}
                          animate={{ opacity: 1, y: 0 }}
                          exit={{ opacity: 0, y: -8 }}
                          transition={{ duration: 0.18 }}
                          className="space-y-6"
                        >
                          {/* Highlights Tab */}
                          {activeTab === "highlights" && (
                            <div className="space-y-4">
                              <div className="p-4 bg-red-50/60 dark:bg-red-950/30 rounded-2xl border border-red-100 dark:border-red-900/50">
                                <h4 className="text-xs font-black text-red-900 dark:text-red-300 uppercase tracking-wide">
                                  Operator Commitment
                                </h4>
                                <p className="text-xs text-red-800 dark:text-red-200 mt-1">
                                  {route.operatorName} is rated {route.rating.toFixed(1)}/5 for comfort, timing, and cleanliness.
                                </p>
                              </div>

                              <div className="grid grid-cols-2 gap-3">
                                <div className="p-3.5 bg-gray-50 dark:bg-slate-800/80 rounded-2xl border border-gray-200 dark:border-slate-700">
                                  <span className="text-[10px] text-gray-400 uppercase font-bold block">Departure</span>
                                  <p className="text-sm font-black text-gray-900 dark:text-white mt-0.5">{route.departureTime}</p>
                                  <p className="text-[11px] text-gray-500 font-medium mt-0.5">{route.sourceCity}</p>
                                </div>
                                <div className="p-3.5 bg-gray-50 dark:bg-slate-800/80 rounded-2xl border border-gray-200 dark:border-slate-700">
                                  <span className="text-[10px] text-gray-400 uppercase font-bold block">Arrival</span>
                                  <p className="text-sm font-black text-gray-900 dark:text-white mt-0.5">{route.arrivalTime}</p>
                                  <p className="text-[11px] text-gray-500 font-medium mt-0.5">{route.destinationCity}</p>
                                </div>
                              </div>
                            </div>
                          )}

                          {/* Photos Tab */}
                          {activeTab === "photos" && (
                            <div className="space-y-4">
                              <h3 className="text-sm font-bold text-gray-900 dark:text-white flex items-center gap-1.5">
                                <Camera className="w-4 h-4 text-[#d84e55]" />
                                <span>Coach Imagery & Seat Layout</span>
                              </h3>
                              <BusImageSlider busName={route.operatorName} busType={route.busType} photoUrls={route.busPhotoUrl} />
                            </div>
                          )}

                          {/* AI Punctuality & Demand */}
                          {activeTab === "ml_insights" && (
                            <div className="space-y-4">
                              <div className="p-4 bg-gradient-to-br from-amber-50 to-orange-50 dark:from-amber-950/40 dark:to-orange-950/30 rounded-2xl border border-amber-200 dark:border-amber-800/50 space-y-2">
                                <div className="flex items-center justify-between">
                                  <span className="text-xs font-black text-amber-900 dark:text-amber-200 flex items-center gap-1.5">
                                    <Sparkles className="w-4 h-4 text-amber-600" />
                                    <span>AI Delay Prediction Model</span>
                                  </span>
                                  <span className="text-[10px] font-extrabold px-2 py-0.5 bg-amber-200/80 dark:bg-amber-900/60 text-amber-900 dark:text-amber-100 rounded-full">
                                    {delayPrediction?.confidenceScore || 92}% Accuracy
                                  </span>
                                </div>
                                <p className="text-xs text-amber-800 dark:text-amber-300 leading-relaxed">
                                  {delayPrediction?.aiExplanation ||
                                    "Predicted on-time departure based on historical route telemetry and traffic modeling."}
                                </p>
                              </div>
                            </div>
                          )}

                          {/* Dynamic Cancellation Policy Tab */}
                          {activeTab === "cancellation" && (
                            <div className="space-y-4">
                              <div>
                                <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                                  Cancellation policy
                                </h3>
                                <p className="text-xs text-gray-500 dark:text-slate-400">
                                  Dynamic refund schedule for journey on <strong className="text-gray-800 dark:text-slate-200">{formattedJourneyDate} ({route.departureTime})</strong>.
                                </p>
                              </div>

                              <div className="border border-gray-200 dark:border-slate-800 rounded-2xl overflow-hidden text-xs">
                                <table className="w-full text-left">
                                  <thead className="bg-gray-50 dark:bg-slate-800/80 text-gray-700 dark:text-slate-300 font-bold border-b border-gray-200 dark:border-slate-800">
                                    <tr>
                                      <th className="p-3">Time before travel</th>
                                      <th className="p-3 text-center">Without free cancellation</th>
                                      <th className="p-3 text-center bg-emerald-50/70 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300">
                                        With free cancellation
                                      </th>
                                    </tr>
                                  </thead>
                                  <tbody className="divide-y divide-gray-100 dark:divide-slate-800 text-gray-600 dark:text-slate-300">
                                    {cancellationTiers.map((tier, idx) => (
                                      <tr key={idx}>
                                        <td className="p-3 font-medium">{tier.timeLabel}</td>
                                        <td className="p-3 text-center font-semibold">
                                          {tier.refundPercentWithout > 0
                                            ? `${tier.refundPercentWithout}% Refund (₹${tier.refundAmountWithout})`
                                            : "0% refund"}
                                        </td>
                                        <td className="p-3 text-center font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50/30 dark:bg-emerald-950/20">
                                          {tier.refundPercentWith > 0
                                            ? `✓ 100% Refund (₹${tier.refundAmountWith})`
                                            : "0% refund"}
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              </div>

                              <div className="p-3.5 bg-red-50/70 dark:bg-red-950/40 rounded-2xl border border-red-200 dark:border-red-900/60 flex items-center space-x-3">
                                <div className="w-8 h-8 rounded-full bg-[#d84e55] text-white flex items-center justify-center font-bold text-sm shrink-0">
                                  ₹
                                </div>
                                <p className="text-xs text-gray-800 dark:text-slate-200">
                                  <span className="font-bold text-[#d84e55] dark:text-red-400">
                                    Add Free Cancellation
                                  </span>{" "}
                                  while booking to get a 100% refund on cancellation.
                                </p>
                              </div>

                              <p className="text-[10px] text-gray-400 dark:text-slate-500 leading-relaxed">
                                * Cancellation charges are computed on a per seat basis. Above cancellation fare is calculated based on {selectedSeats.length > 0 ? `${selectedSeats.length} selected seat(s) fare of ₹${totalPrice}` : `seat fare of ₹${route.basePrice}`}.
                              </p>
                            </div>
                          )}

                          {/* Boarding Point Tab */}
                          {activeTab === "boarding" && (
                            <div className="space-y-4">
                              <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                                Boarding Points in {route.sourceCity}
                              </h3>
                              <div className="space-y-2.5">
                                {boardingStops.slice(0, showAllBoarding ? boardingStops.length : 5).map((stop, sIdx) => {
                                  const isSelected = currentBoarding === stop.name;
                                  return (
                                    <div
                                      key={sIdx}
                                      onClick={() => dispatch(setBoardingPoint(stop.name))}
                                      className={`p-3 rounded-2xl border transition-all cursor-pointer flex items-start space-x-3 ${
                                        isSelected
                                          ? "border-[#d84e55] bg-red-50/50 dark:bg-red-950/30"
                                          : "border-gray-200 dark:border-slate-800 hover:bg-gray-50"
                                      }`}
                                    >
                                      <div className="pt-0.5">
                                        <div
                                          className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                                            isSelected ? "border-[#d84e55] bg-[#d84e55]" : "border-gray-300"
                                          }`}
                                        >
                                          {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                                        </div>
                                      </div>
                                      <div className="flex-1 min-w-0">
                                        <div className="flex items-center space-x-2">
                                          <span className="font-extrabold text-xs text-gray-900 dark:text-white">{stop.time}</span>
                                          <span className="text-gray-300">•</span>
                                          <span className="font-bold text-xs text-gray-900 dark:text-white">{stop.name}</span>
                                        </div>
                                        <p className="text-[11px] text-gray-500 mt-0.5">{stop.address}</p>
                                      </div>
                                    </div>
                                  );
                                })}
                              </div>
                              <button
                                type="button"
                                onClick={() => setShowAllBoarding(!showAllBoarding)}
                                className="w-full py-2 bg-red-50 dark:bg-red-950/40 text-[#d84e55] rounded-xl font-bold text-xs"
                              >
                                {showAllBoarding ? "Show less" : "View all boarding points"}
                              </button>
                            </div>
                          )}

                          {/* Dropping Point Tab */}
                          {activeTab === "dropping" && (
                            <div className="space-y-4">
                              <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                                Dropping Points in {route.destinationCity}
                              </h3>
                              <div className="space-y-2.5">
                                {droppingStops.slice(0, showAllDropping ? droppingStops.length : 5).map((stop, sIdx) => {
                                  const isSelected = currentDropping === stop.name;
                                  return (
                                    <div
                                      key={sIdx}
                                      onClick={() => dispatch(setDroppingPoint(stop.name))}
                                      className={`p-3 rounded-2xl border transition-all cursor-pointer flex items-start space-x-3 ${
                                        isSelected
                                          ? "border-[#d84e55] bg-red-50/50 dark:bg-red-950/30"
                                          : "border-gray-200 dark:border-slate-800 hover:bg-gray-50"
                                      }`}
                                    >
                                      <div className="pt-0.5">
                                        <div
                                          className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                                            isSelected ? "border-[#d84e55] bg-[#d84e55]" : "border-gray-300"
                                          }`}
                                        >
                                          {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                                        </div>
                                      </div>
                                      <div className="flex-1 min-w-0">
                                        <div className="flex items-center space-x-2">
                                          <span className="font-extrabold text-xs text-gray-900 dark:text-white">{stop.time}</span>
                                          <span className="text-gray-300">•</span>
                                          <span className="font-bold text-xs text-gray-900 dark:text-white">{stop.name}</span>
                                        </div>
                                        <p className="text-[11px] text-gray-500 mt-0.5">{stop.address}</p>
                                      </div>
                                    </div>
                                  );
                                })}
                              </div>
                              <button
                                type="button"
                                onClick={() => setShowAllDropping(!showAllDropping)}
                                className="w-full py-2 bg-red-50 dark:bg-red-950/40 text-[#d84e55] rounded-xl font-bold text-xs"
                              >
                                {showAllDropping ? "Show less" : "View all dropping points"}
                              </button>
                            </div>
                          )}

                          {/* Reviews Tab */}
                          {activeTab === "reviews" && (
                            <div className="space-y-4">
                              <div className="flex items-center justify-between">
                                <h3 className="text-sm font-bold text-gray-900 dark:text-white">Customer Reviews</h3>
                                <button
                                  type="button"
                                  onClick={() => setShowWriteReview(true)}
                                  className="text-xs font-bold text-[#d84e55] hover:underline"
                                >
                                  + Write Review
                                </button>
                              </div>
                              <div className="space-y-3">
                                {reviewData?.reviews && reviewData.reviews.length > 0 ? (
                                  reviewData.reviews.map((rev) => (
                                    <div key={rev.id} className="p-3 bg-gray-50 dark:bg-slate-800/60 rounded-2xl border border-gray-200 dark:border-slate-700">
                                      <div className="flex items-center justify-between">
                                        <span className="font-bold text-xs text-gray-900 dark:text-white">{rev.userName}</span>
                                        <div className="flex items-center space-x-1 px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded font-bold text-[10px]">
                                          <Star className="w-3 h-3 fill-emerald-800" />
                                          <span>{rev.rating}</span>
                                        </div>
                                      </div>
                                      <p className="text-xs text-gray-600 dark:text-slate-300 mt-1">{rev.comment}</p>
                                    </div>
                                  ))
                                ) : (
                                  <p className="text-xs text-gray-400">No reviews yet. Be the first to review!</p>
                                )}
                              </div>
                            </div>
                          )}
                        </motion.div>
                      </AnimatePresence>
                    </div>
                  </div>
                </div>
              )}

              {/* ========================================================================= */}
              {/* VIEW 2: SELECT BOARDING & DROPPING POINTS STEP (Strictly City-Specific) */}
              {/* ========================================================================= */}
              {activeStep === "BOARD_DROP" && (
                <div className="flex-1 overflow-y-auto flex flex-col bg-[#f8fafc] dark:bg-[#0b0f19]">
                  {/* Sub-Tabs: Boarding points vs Dropping points */}
                  <div className="grid grid-cols-2 bg-white dark:bg-[#0f172a] border-b border-gray-200 dark:border-slate-800 text-center font-bold text-xs sm:text-sm shrink-0">
                    <button
                      type="button"
                      onClick={() => {
                        setBoardDropSubTab("boarding");
                        setAreaSearchTerm("");
                      }}
                      className={`py-3 sm:py-3.5 px-3 sm:px-4 transition-all relative cursor-pointer ${
                        boardDropSubTab === "boarding"
                          ? "text-[#d84e55] font-extrabold"
                          : "text-gray-500 hover:text-gray-900 dark:text-slate-400"
                      }`}
                    >
                      <div className="flex flex-col items-center justify-center">
                        <span className="text-xs sm:text-sm">Boarding points</span>
                        <span className="text-[10px] sm:text-[11px] font-semibold text-gray-400 dark:text-slate-500">
                          {route.sourceCity}
                        </span>
                      </div>
                      {boardDropSubTab === "boarding" && (
                        <div className="absolute bottom-0 left-0 right-0 h-1 bg-[#d84e55] rounded-t-full" />
                      )}
                    </button>

                    <button
                      type="button"
                      onClick={() => {
                        setBoardDropSubTab("dropping");
                        setAreaSearchTerm("");
                      }}
                      className={`py-3 sm:py-3.5 px-3 sm:px-4 transition-all relative cursor-pointer ${
                        boardDropSubTab === "dropping"
                          ? "text-[#d84e55] font-extrabold"
                          : "text-gray-500 hover:text-gray-900 dark:text-slate-400"
                      }`}
                    >
                      <div className="flex flex-col items-center justify-center">
                        <span className="text-xs sm:text-sm">Dropping points</span>
                        <span className="text-[10px] sm:text-[11px] font-semibold text-gray-400 dark:text-slate-500">
                          {route.destinationCity}
                        </span>
                      </div>
                      {boardDropSubTab === "dropping" && (
                        <div className="absolute bottom-0 left-0 right-0 h-1 bg-[#d84e55] rounded-t-full" />
                      )}
                    </button>
                  </div>

                  {/* Search Area */}
                  <div className="p-3.5 sm:p-5 max-w-4xl w-full mx-auto space-y-3.5 sm:space-y-4">
                    <div className="space-y-1.5">
                      <label className="text-xs sm:text-sm font-bold text-gray-800 dark:text-slate-200">
                        Find the closest {boardDropSubTab === "boarding" ? "boarding" : "dropping"} point to
                      </label>
                      <div className="relative flex items-center">
                        <Search className="w-4 h-4 text-gray-400 absolute left-3.5" />
                        <input
                          type="text"
                          value={areaSearchTerm}
                          onChange={(e) => setAreaSearchTerm(e.target.value)}
                          placeholder="Search for an 'area'"
                          className="w-full pl-10 pr-10 py-2.5 sm:py-3 bg-white dark:bg-slate-800 rounded-2xl border border-gray-200 dark:border-slate-700 text-xs sm:text-sm text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#d84e55]/30 focus:border-[#d84e55] shadow-xs"
                        />
                        <button
                          type="button"
                          className="absolute right-3 p-1 text-gray-400 hover:text-gray-600"
                          title="Use current location"
                        >
                          <Crosshair className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    {/* Selected Highlight Card */}
                    {((boardDropSubTab === "boarding" && currentBoarding) ||
                      (boardDropSubTab === "dropping" && currentDropping)) && (
                      <div className="p-3.5 sm:p-4 bg-gradient-to-r from-emerald-500/10 via-emerald-500/5 to-transparent rounded-2xl border border-emerald-500/30">
                        <span className="text-[10px] sm:text-[11px] font-bold text-emerald-800 dark:text-emerald-300 block mb-1">
                          Your selected {boardDropSubTab} point
                        </span>
                        <div className="flex items-center justify-between">
                          <div>
                            <span className="font-black text-xs sm:text-sm text-gray-900 dark:text-white">
                              {boardDropSubTab === "boarding" ? currentBoarding : currentDropping}
                            </span>
                            <p className="text-[10px] sm:text-[11px] text-gray-500 mt-0.5">
                              {boardDropSubTab === "boarding"
                                ? boardingStops.find((s) => s.name === currentBoarding)?.address
                                : droppingStops.find((s) => s.name === currentDropping)?.address}
                            </p>
                          </div>
                          <div className="w-5 h-5 rounded-full bg-emerald-600 text-white flex items-center justify-center shrink-0">
                            <Check className="w-3.5 h-3.5 stroke-[3]" />
                          </div>
                        </div>
                      </div>
                    )}

                    {/* All Points Card */}
                    <div className="bg-white dark:bg-[#0f172a] rounded-2xl border border-gray-200 dark:border-slate-800 shadow-xs overflow-hidden">
                      <div className="px-4 py-2.5 sm:py-3 bg-gray-50/80 dark:bg-slate-800/50 border-b border-gray-100 dark:border-slate-800 text-xs font-bold text-gray-700 dark:text-slate-300">
                        All {boardDropSubTab} points in {boardDropSubTab === "boarding" ? route.sourceCity : route.destinationCity}
                      </div>

                      <div className="divide-y divide-gray-100 dark:divide-slate-800">
                        {filteredStops.length > 0 ? (
                          filteredStops.map((stop, sIdx) => {
                            const isSelected =
                              boardDropSubTab === "boarding"
                                ? currentBoarding === stop.name
                                : currentDropping === stop.name;

                            return (
                              <div
                                key={sIdx}
                                onClick={() => {
                                  if (boardDropSubTab === "boarding") {
                                    dispatch(setBoardingPoint(stop.name));
                                    if (!currentDropping) {
                                      setTimeout(() => setBoardDropSubTab("dropping"), 250);
                                    }
                                  } else {
                                    dispatch(setDroppingPoint(stop.name));
                                  }
                                }}
                                className={`p-3.5 sm:p-4 transition-all cursor-pointer flex items-center justify-between hover:bg-gray-50 dark:hover:bg-slate-800/50 ${
                                  isSelected ? "bg-red-50/40 dark:bg-red-950/20" : ""
                                }`}
                              >
                                <div className="flex items-start space-x-3 min-w-0 flex-1 pr-2">
                                  <span className="font-black text-xs sm:text-sm text-gray-900 dark:text-white shrink-0 mt-0.5">
                                    {stop.time}
                                  </span>
                                  <div className="min-w-0 flex-1">
                                    <h4 className="font-extrabold text-xs sm:text-sm text-gray-900 dark:text-white">
                                      {stop.name}
                                    </h4>
                                    <p className="text-[10px] sm:text-[11px] text-gray-500 dark:text-slate-400 mt-0.5 leading-relaxed">
                                      {stop.address}
                                    </p>
                                  </div>
                                </div>

                                {/* Circular Radio Selector */}
                                <div className="shrink-0 pl-2">
                                  <div
                                    className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all ${
                                      isSelected
                                        ? "border-[#d84e55] bg-[#d84e55]"
                                        : "border-gray-400 dark:border-slate-500 bg-transparent"
                                    }`}
                                  >
                                    {isSelected && <div className="w-2 h-2 rounded-full bg-white" />}
                                  </div>
                                </div>
                              </div>
                            );
                          })
                        ) : (
                          <div className="p-8 text-center text-gray-400 text-xs">
                            No stops matching &quot;{areaSearchTerm}&quot;. Try searching another area or landmark.
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* ========================================================================= */}
              {/* BOTTOM ACTION BAR */}
              {/* ========================================================================= */}
              <div className="p-3 sm:p-4 bg-white dark:bg-[#0f172a] border-t border-gray-200 dark:border-slate-800 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2.5 sm:gap-3 shrink-0">
                {/* Left: Seat and Route summary */}
                <div className="flex items-center justify-between sm:justify-start sm:space-x-4">
                  <div>
                    <div className="flex items-center space-x-1.5">
                      <span className="text-xs text-gray-500 dark:text-slate-400 font-medium">
                        {selectedSeats.length > 0
                          ? `${selectedSeats.length} seat${selectedSeats.length > 1 ? "s" : ""} selected`
                          : "Selected Seats:"}
                      </span>
                      {selectedSeats.length > 0 ? (
                        <span className="font-extrabold text-xs text-gray-900 dark:text-white">
                          ({selectedSeats.map((s) => s.seatNumber).join(", ")})
                        </span>
                      ) : (
                        <span className="text-xs text-gray-400 italic">None</span>
                      )}
                    </div>

                    <div className="flex items-center space-x-1 text-[10px] sm:text-[11px] text-gray-500 mt-0.5 truncate max-w-[260px] sm:max-w-none">
                      {currentBoarding && currentDropping ? (
                        <span className="text-emerald-600 dark:text-emerald-400 font-bold truncate">
                          Board: {currentBoarding} • Drop: {currentDropping}
                        </span>
                      ) : currentBoarding ? (
                        <span className="text-amber-600 font-medium truncate">
                          Board: {currentBoarding} • Select Dropping Point
                        </span>
                      ) : (
                        <span className="text-gray-400 italic">
                          Board & Drop points not selected
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Mobile Total Price */}
                  {selectedSeats.length > 0 && (
                    <div className="sm:hidden text-right">
                      <span className="text-[10px] text-gray-400 block font-medium">Total Fare</span>
                      <span className="text-base font-black text-gray-900 dark:text-white">
                        ₹{totalPrice}
                      </span>
                    </div>
                  )}
                </div>

                {/* Right: Total Price (Desktop) + Action Button */}
                <div className="flex items-center justify-between sm:justify-end space-x-3 sm:space-x-4">
                  {selectedSeats.length > 0 && (
                    <div className="hidden sm:block text-right">
                      <span className="text-[10px] text-gray-400 block font-medium">Total Fare</span>
                      <span className="text-lg font-black text-gray-900 dark:text-white">
                        ₹{totalPrice}
                      </span>
                    </div>
                  )}

                  {activeStep === "SEATS" ? (
                    <button
                      type="button"
                      disabled={selectedSeats.length === 0}
                      onClick={handleProceedToBoardDrop}
                      className="w-full sm:w-auto px-5 sm:px-6 py-2.5 sm:py-3 bg-[#d84e55] hover:bg-[#b83e44] disabled:bg-gray-200 dark:disabled:bg-slate-800 disabled:text-gray-400 text-white font-extrabold text-xs sm:text-sm rounded-xl shadow-md transition-all flex items-center justify-center space-x-2 cursor-pointer disabled:cursor-not-allowed active:scale-95"
                    >
                      <span>Select boarding & dropping points</span>
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  ) : (
                    <button
                      type="button"
                      disabled={selectedSeats.length === 0 || !currentBoarding || !currentDropping}
                      onClick={handleProceedToBook}
                      className="w-full sm:w-auto px-6 sm:px-7 py-2.5 sm:py-3 bg-[#d84e55] hover:bg-[#b83e44] disabled:bg-gray-200 dark:disabled:bg-slate-800 disabled:text-gray-400 text-white font-extrabold text-xs sm:text-sm rounded-xl shadow-md transition-all flex items-center justify-center space-x-2 cursor-pointer disabled:cursor-not-allowed active:scale-95"
                    >
                      <span>Proceed to Book</span>
                      <ChevronRight className="w-4 h-4 stroke-[3]" />
                    </button>
                  )}
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Write Review Modal */}
      <WriteReviewModal
        busId={route.busId || route.id}
        busName={route.operatorName}
        isOpen={showWriteReview}
        onClose={() => setShowWriteReview(false)}
        onReviewSubmitted={() => refetchReviews()}
      />
    </>
  );
}
