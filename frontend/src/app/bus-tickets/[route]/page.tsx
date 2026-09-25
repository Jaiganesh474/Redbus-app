import React from "react";
import Link from "next/link";
import { Metadata } from "next";
import { Bus, Clock, Shield, Star, Calendar, ArrowRight, CheckCircle2 } from "lucide-react";

interface RoutePageProps {
  params: Promise<{
    route: string;
  }>;
}

// Capitalize helper
function formatCityName(slug: string) {
  return slug
    .split("-")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

export async function generateMetadata({ params }: RoutePageProps): Promise<Metadata> {
  const { route } = await params;
  const parts = route.split("-to-");
  const source = formatCityName(parts[0] || "Bangalore");
  const destination = formatCityName(parts[1] || "Chennai");

  return {
    title: `${source} to ${destination} Bus Tickets | Online Booking Starting ₹420 - redBus`,
    description: `Book ${source} to ${destination} bus tickets online with redBus. Compare 20+ AC Sleeper, Volvo and Seater buses with live seat tracking, GPS, and instant refunds.`,
    keywords: `${source} to ${destination} bus, ${source} ${destination} bus tickets, sleeper bus, volvo bus, redbus`,
    openGraph: {
      title: `${source} to ${destination} Bus Tickets - redBus`,
      description: `Book ${source} to ${destination} bus tickets online with redBus. Live seat layout & instant booking.`,
      url: `https://www.redbus.in/bus-tickets/${route}`,
    },
  };
}

export default async function ProgrammaticRoutePage({ params }: RoutePageProps) {
  const { route } = await params;
  const parts = route.split("-to-");
  const source = formatCityName(parts[0] || "Bangalore");
  const destination = formatCityName(parts[1] || "Chennai");

  // Fetch live route data from backend server-side
  let routes: any[] = [];
  try {
    const res = await fetch(
      `http://localhost:8080/api/routes/search?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(destination)}`,
      { next: { revalidate: 60 } }
    );
    if (res.ok) {
      routes = await res.json();
    }
  } catch (e) {
    // If backend offline at build time, render static fallback
  }

  const minPrice = routes.length > 0
    ? Math.min(...routes.map((r) => r.basePrice))
    : 450;
  const avgDuration = routes.length > 0 ? routes[0].durationHours : 6.5;

  // JSON-LD Structured Data Schema for SEO
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "Trip",
    name: `${source} to ${destination} Bus Journey`,
    description: `Direct AC Sleeper and Seater bus services between ${source} and ${destination}.`,
    itinerary: {
      "@type": "ItemList",
      itemListElement: [
        {
          "@type": "ListItem",
          position: 1,
          name: source,
        },
        {
          "@type": "ListItem",
          position: 2,
          name: destination,
        },
      ],
    },
    offers: {
      "@type": "AggregateOffer",
      lowPrice: minPrice,
      priceCurrency: "INR",
      offerCount: routes.length || 5,
    },
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-12">
      {/* JSON-LD Schema */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />

      {/* SEO Breadcrumb */}
      <nav className="text-xs text-gray-500 flex items-center space-x-2">
        <Link href="/" className="hover:text-[#d84e55]">Home</Link>
        <span>/</span>
        <Link href="/" className="hover:text-[#d84e55]">Bus Tickets</Link>
        <span>/</span>
        <span className="font-semibold text-gray-800">{source} to {destination}</span>
      </nav>

      {/* Route Header Banner */}
      <div className="bg-gradient-to-r from-[#d84e55] to-red-600 rounded-3xl p-8 text-white shadow-xl relative overflow-hidden">
        <div className="max-w-3xl space-y-3">
          <div className="inline-flex items-center space-x-1.5 px-3 py-1 bg-white/20 rounded-full text-xs font-bold uppercase tracking-wider">
            <span>Verified Bus Route</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-black tracking-tight">
            {source} to {destination} Bus Tickets
          </h1>
          <p className="text-sm text-red-100 leading-relaxed">
            Starting from ₹{minPrice.toFixed(0)} • {routes.length || "10+"} daily services • Average travel time: {avgDuration} hrs
          </p>

          <div className="pt-4 flex flex-wrap gap-3">
            <Link
              href={`/search?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(destination)}`}
              className="px-6 py-3 bg-white text-[#d84e55] hover:bg-red-50 rounded-2xl text-xs font-black shadow-md transition-all flex items-center space-x-2"
            >
              <span>View Available Buses & Seats</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </div>

      {/* Route Quick Facts Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-xs text-center">
          <span className="text-[11px] font-bold text-gray-400 uppercase block">Starting Fare</span>
          <p className="text-xl font-black text-gray-900 mt-1">₹{minPrice.toFixed(0)}</p>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-xs text-center">
          <span className="text-[11px] font-bold text-gray-400 uppercase block">Journey Duration</span>
          <p className="text-xl font-black text-gray-900 mt-1">{avgDuration} Hours</p>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-xs text-center">
          <span className="text-[11px] font-bold text-gray-400 uppercase block">Total Daily Buses</span>
          <p className="text-xl font-black text-gray-900 mt-1">{routes.length || 5}+ Buses</p>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-xs text-center">
          <span className="text-[11px] font-bold text-gray-400 uppercase block">Safety Rating</span>
          <p className="text-xl font-black text-emerald-600 mt-1">★ 4.7 / 5</p>
        </div>
      </div>

      {/* Available Buses List on this route */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-black text-gray-900 tracking-tight">
            Available Buses for {source} to {destination}
          </h2>
          <Link
            href={`/search?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(destination)}`}
            className="text-xs font-bold text-[#d84e55] hover:underline flex items-center space-x-1"
          >
            <span>Search with filters</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="space-y-3">
          {routes.map((r) => (
            <div
              key={r.id}
              className="bg-white p-5 rounded-2xl border border-gray-200 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
            >
              <div>
                <div className="flex items-center space-x-2">
                  <h4 className="font-bold text-gray-900 text-base">{r.operatorName}</h4>
                  <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded text-xs font-bold">
                    ★ {r.rating}
                  </span>
                </div>
                <p className="text-xs text-gray-500 mt-0.5">{r.busType}</p>
                <div className="flex items-center space-x-4 mt-2 text-xs text-gray-700">
                  <span>Dep: <strong>{r.departureTime}</strong></span>
                  <span>➔</span>
                  <span>Arr: <strong>{r.arrivalTime}</strong></span>
                </div>
              </div>

              <div className="text-left sm:text-right">
                <p className="text-xl font-black text-gray-900">₹{r.basePrice.toFixed(0)}</p>
                <Link
                  href={`/search?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(destination)}`}
                  className="mt-2 inline-block px-4 py-2 bg-[#d84e55] text-white rounded-xl text-xs font-bold hover:bg-[#b83e44] transition-all"
                >
                  Select Seats
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Return Journey Link */}
      <div className="p-6 bg-gray-100 rounded-3xl flex flex-col sm:flex-row items-center justify-between gap-4">
        <div>
          <h3 className="font-bold text-sm text-gray-900">
            Looking for return tickets: {destination} to {source}?
          </h3>
          <p className="text-xs text-gray-500 mt-0.5">
            Check live schedules and reserve return seats with zero hassle.
          </p>
        </div>
        <Link
          href={`/bus-tickets/${parts[1] || "chennai"}-to-${parts[0] || "bangalore"}`}
          className="px-4 py-2 bg-white text-gray-900 hover:text-[#d84e55] rounded-xl text-xs font-bold border border-gray-200 shadow-xs"
        >
          View Return Buses
        </Link>
      </div>
    </div>
  );
}
