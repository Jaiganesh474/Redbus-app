"use client";

import React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { useAppDispatch } from "@/store";
import { setSearchParams } from "@/store/searchSlice";
import {
  useGetPopularRoutesQuery,
  useGetAiRecommendationsQuery,
  useGetPublicFaqsQuery,
} from "@/store/apiSlice";
import HeroSearch from "@/components/HeroSearch";
import AiBusBanners from "@/components/AiBusBanners";
import {
  Sparkles,
  ShieldCheck,
  TrendingUp,
  Award,
  Users,
  CheckCircle2,
  ChevronRight,
  Bus,
  ArrowRight,
} from "lucide-react";

export default function HomePage() {
  const router = useRouter();
  const dispatch = useAppDispatch();

  const { data: popularRoutes = [] } = useGetPopularRoutesQuery();
  const { data: recommendations } = useGetAiRecommendationsQuery({});
  const { data: faqs = [] } = useGetPublicFaqsQuery();

  const handleQuickRouteSearch = (from: string, to: string) => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dateStr = tomorrow.toISOString().split("T")[0];

    dispatch(
      setSearchParams({
        sourceCity: from,
        destinationCity: to,
        travelDate: dateStr,
      })
    );
    router.push(`/search?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&date=${dateStr}`);
  };

  return (
    <div className="space-y-8 sm:space-y-16 pb-8 sm:pb-12 overflow-x-hidden">
      {/* Hero Search Section with Down-to-Up entrance */}
      <motion.section
        initial={{ opacity: 0, y: 25 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: "easeOut" }}
        className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 pt-3 sm:pt-8"
      >
        <HeroSearch />
        {/* Dynamic AI Curated Bus Banners Carousel */}
        <AiBusBanners />
      </motion.section>

      {/* AI Recommendations Section with Scroll Lazy Loading */}
      {recommendations && recommendations.recommendedRoutes?.length > 0 && (
        <motion.section
          initial={{ opacity: 0, y: 25 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-30px" }}
          transition={{ duration: 0.45, ease: "easeOut" }}
          className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8"
        >
          <div className="bg-gradient-to-r from-red-50 via-orange-50/60 to-rose-50 dark:from-slate-900 dark:via-slate-850 dark:to-slate-900 rounded-2xl sm:rounded-3xl p-4 sm:p-8 border border-red-100 dark:border-slate-800 relative overflow-hidden">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 sm:gap-4 mb-4 sm:mb-6">
              <div>
                <div className="inline-flex items-center space-x-1.5 px-2.5 py-0.5 sm:py-1 bg-white dark:bg-slate-800 rounded-full text-[10px] sm:text-xs font-bold text-[#d84e55] dark:text-red-400 shadow-2xs mb-1.5 border border-red-200 dark:border-slate-700">
                  <Sparkles className="w-3 h-3 sm:w-3.5 sm:h-3.5 text-[#d84e55] dark:text-red-400" />
                  <span>AI Smart Picks</span>
                </div>
                <h2 className="text-lg sm:text-2xl font-black text-gray-900 dark:text-white tracking-tight">
                  Recommended For Your Journey
                </h2>
                <p className="text-[11px] sm:text-sm text-gray-600 dark:text-slate-300 mt-0.5">
                  {recommendations.reason || "Handpicked top-rated sleeper and luxury buses across popular routes"}
                </p>
              </div>
            </div>

            <motion.div
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: "-30px" }}
              variants={{
                hidden: { opacity: 0 },
                visible: {
                  opacity: 1,
                  transition: { staggerChildren: 0.06 },
                },
              }}
              className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4"
            >
              {recommendations.recommendedRoutes.map((route) => (
                <motion.div
                  key={route.id}
                  variants={{
                    hidden: { opacity: 0, y: 20 },
                    visible: { opacity: 1, y: 0, transition: { duration: 0.4, ease: "easeOut" } },
                  }}
                  whileHover={{ y: -3, transition: { duration: 0.2 } }}
                  onClick={() => handleQuickRouteSearch(route.sourceCity, route.destinationCity)}
                  className="bg-white dark:bg-slate-800 rounded-xl sm:rounded-2xl p-3.5 sm:p-4 border border-gray-200 dark:border-slate-700 hover:border-[#d84e55] dark:hover:border-red-500 shadow-2xs hover:shadow-md transition-all cursor-pointer group flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="text-[10px] sm:text-[11px] font-bold text-gray-400 dark:text-slate-400 uppercase tracking-wider">
                        {route.durationHours} hrs
                      </span>
                      <span className="px-2 py-0.5 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-400 rounded text-[11px] sm:text-xs font-bold">
                        ★ {route.rating.toFixed(1)}
                      </span>
                    </div>
                    <div className="flex items-center space-x-1.5 font-black text-gray-900 dark:text-white text-xs sm:text-sm group-hover:text-[#d84e55] dark:group-hover:text-red-400 transition-colors">
                      <span>{route.sourceCity}</span>
                      <span className="text-[#d84e55] dark:text-red-400">➔</span>
                      <span>{route.destinationCity}</span>
                    </div>
                    <p className="text-[11px] sm:text-xs text-gray-500 dark:text-slate-400 font-medium mt-0.5 truncate">{route.operatorName}</p>
                    <p className="text-[10px] sm:text-[11px] text-gray-400 dark:text-slate-500 mt-0.5">{route.busType}</p>
                  </div>

                  <div className="mt-3 pt-2.5 border-t border-gray-100 dark:border-slate-700 flex items-center justify-between">
                    <div>
                      <span className="text-[9px] sm:text-[10px] text-gray-400 dark:text-slate-400">Starting from</span>
                      <p className="text-sm sm:text-base font-black text-gray-900 dark:text-white">₹{route.basePrice.toFixed(0)}</p>
                    </div>
                    <span className="w-7 h-7 sm:w-8 sm:h-8 rounded-full bg-red-50 dark:bg-red-950/40 text-[#d84e55] dark:text-red-400 flex items-center justify-center group-hover:bg-[#d84e55] group-hover:text-white transition-all">
                      <ArrowRight className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
                    </span>
                  </div>
                </motion.div>
              ))}
            </motion.div>
          </div>
        </motion.section>
      )}

      {/* Popular Bus Routes Across India with Down-to-Up Scroll Lazy Loading */}
      <motion.section
        initial={{ opacity: 0, y: 25 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-30px" }}
        transition={{ duration: 0.45, ease: "easeOut" }}
        className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8"
      >
        <div className="flex items-center justify-between mb-4 sm:mb-6">
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-gray-900 dark:text-white tracking-tight">Popular Bus Routes</h2>
            <p className="text-xs sm:text-sm text-gray-500 dark:text-slate-400 mt-0.5">Top-booked intercity bus corridors with daily departures</p>
          </div>
        </div>

        <motion.div
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: "-30px" }}
          variants={{
            hidden: { opacity: 0 },
            visible: {
              opacity: 1,
              transition: { staggerChildren: 0.04 },
            },
          }}
          className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 sm:gap-4"
        >
          {popularRoutes.map((pair) => (
            <motion.div
              key={`${pair.sourceCity}-${pair.destinationCity}`}
              variants={{
                hidden: { opacity: 0, y: 20 },
                visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" } },
              }}
              whileHover={{ y: -3, transition: { duration: 0.2 } }}
              onClick={() => handleQuickRouteSearch(pair.sourceCity, pair.destinationCity)}
              className="bg-white dark:bg-slate-850 dark:bg-slate-800 p-3.5 sm:p-4 rounded-xl sm:rounded-2xl border border-gray-200 dark:border-slate-700 hover:border-[#d84e55] dark:hover:border-red-500 shadow-2xs hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="flex items-center space-x-1.5 text-xs sm:text-sm font-black text-gray-900 dark:text-white group-hover:text-[#d84e55] dark:group-hover:text-red-400 transition-colors">
                <span>{pair.sourceCity}</span>
                <span className="text-gray-400 dark:text-slate-500 group-hover:text-[#d84e55]">➔</span>
                <span>{pair.destinationCity}</span>
              </div>
              <p className="text-[11px] sm:text-xs text-gray-500 dark:text-slate-400 mt-1">{pair.busCount} daily buses available</p>
              <div className="mt-2.5 sm:mt-3 flex items-center justify-between text-xs">
                <span className="font-semibold text-gray-900 dark:text-white">From ₹{pair.minPrice.toFixed(0)}</span>
                <span className="text-[#d84e55] dark:text-red-400 font-bold group-hover:translate-x-1 transition-transform flex items-center">
                  Book <ChevronRight className="w-3.5 h-3.5 ml-0.5" />
                </span>
              </div>
            </motion.div>
          ))}
        </motion.div>
      </motion.section>

      {/* Operators Showcase with Scroll Lazy Loading */}
      <motion.section
        initial={{ opacity: 0, y: 25 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-30px" }}
        transition={{ duration: 0.45, ease: "easeOut" }}
        className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8"
      >
        <div className="bg-white dark:bg-slate-900 rounded-2xl sm:rounded-3xl p-4 sm:p-8 border border-gray-200 dark:border-slate-800 shadow-2xs">
          <div className="text-center max-w-xl mx-auto mb-5 sm:mb-8">
            <h2 className="text-xl sm:text-2xl font-black text-gray-900 dark:text-white tracking-tight">Certified Bus Operators</h2>
            <p className="text-xs sm:text-sm text-gray-500 dark:text-slate-400 mt-0.5">Travel with India&apos;s highest-rated Volvo & Sleeper operators</p>
          </div>

          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: "-30px" }}
            variants={{
              hidden: { opacity: 0 },
              visible: {
                opacity: 1,
                transition: { staggerChildren: 0.05 },
              },
            }}
            className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2.5 sm:gap-4"
          >
            {[
              { name: "IntrCity SmartBus", rating: "4.8", fleet: "Smart Lounges & GPS" },
              { name: "Zingbus Plus", rating: "4.7", fleet: "Luxury Multi-Axle" },
              { name: "Orange Travels", rating: "4.6", fleet: "BharatBenz Sleepers" },
              { name: "SRS Travels", rating: "4.3", fleet: "Pan-India Express" },
              { name: "KSRTC Airavat", rating: "4.5", fleet: "Club Class Volvos" },
            ].map((op) => (
              <motion.div
                key={op.name}
                variants={{
                  hidden: { opacity: 0, y: 20 },
                  visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" } },
                }}
                whileHover={{ y: -3, transition: { duration: 0.2 } }}
                className="p-3 sm:p-4 bg-gray-50 dark:bg-slate-800 rounded-xl sm:rounded-2xl border border-gray-100 dark:border-slate-700 text-center hover:border-[#d84e55] dark:hover:border-red-500 transition-all"
              >
                <div className="w-9 h-9 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl bg-white dark:bg-slate-900 shadow-2xs border border-gray-200 dark:border-slate-700 flex items-center justify-center mx-auto mb-2 sm:mb-3 text-[#d84e55] dark:text-red-400">
                  <Bus className="w-4 h-4 sm:w-6 sm:h-6" />
                </div>
                <h4 className="text-xs sm:text-sm font-bold text-gray-900 dark:text-white leading-tight truncate">{op.name}</h4>
                <p className="text-[10px] sm:text-[11px] text-gray-500 dark:text-slate-400 mt-0.5 truncate">{op.fleet}</p>
                <span className="inline-block mt-1.5 sm:mt-2 px-1.5 sm:px-2 py-0.5 bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-400 rounded text-[10px] sm:text-xs font-bold">
                  ★ {op.rating}
                </span>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </motion.section>

      {/* FAQs Section with Scroll Lazy Loading */}
      <motion.section
        initial={{ opacity: 0, y: 25 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-30px" }}
        transition={{ duration: 0.45, ease: "easeOut" }}
        className="max-w-4xl mx-auto px-3 sm:px-6 lg:px-8"
      >
        <div className="text-center mb-5 sm:mb-8">
          <h2 className="text-xl sm:text-2xl font-black text-gray-900 dark:text-white tracking-tight">Frequently Asked Questions</h2>
          <p className="text-xs sm:text-sm text-gray-500 dark:text-slate-400 mt-0.5">Everything you need to know about booking, cancellations, and policies</p>
        </div>

        <motion.div
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: "-30px" }}
          variants={{
            hidden: { opacity: 0 },
            visible: {
              opacity: 1,
              transition: { staggerChildren: 0.06 },
            },
          }}
          className="space-y-2.5 sm:space-y-3"
        >
          {faqs.map((faq, idx) => (
            <motion.details
              key={idx}
              variants={{
                hidden: { opacity: 0, y: 15 },
                visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" } },
              }}
              className="group bg-white dark:bg-slate-900 rounded-xl sm:rounded-2xl border border-gray-200 dark:border-slate-800 p-3.5 sm:p-5 open:ring-2 open:ring-red-100 dark:open:ring-red-950/60 transition-all cursor-pointer"
            >
              <summary className="font-bold text-gray-900 dark:text-white text-xs sm:text-sm flex items-center justify-between list-none">
                <span>{faq.question}</span>
                <span className="text-[#d84e55] dark:text-red-400 group-open:rotate-180 transition-transform text-xs">▼</span>
              </summary>
              <p className="text-xs text-gray-600 dark:text-slate-300 mt-2 sm:mt-3 pt-2 sm:pt-3 border-t border-gray-100 dark:border-slate-800 leading-relaxed">
                {faq.answer}
              </p>
            </motion.details>
          ))}
        </motion.div>
      </motion.section>

      {/* Structured Data JSON-LD for SEO */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            "@context": "https://schema.org",
            "@type": "Organization",
            name: "redBus India",
            url: "https://www.redbus.in",
            logo: "https://www.redbus.in/logo.png",
            aggregateRating: {
              "@type": "AggregateRating",
              ratingValue: "4.7",
              reviewCount: "850000",
            },
          }),
        }}
      />
    </div>
  );
}
