"use client";

import React, { useState } from "react";
import Link from "next/link";
import { motion, AnimatePresence } from "framer-motion";
import {
  HelpCircle,
  ShieldCheck,
  RefreshCw,
  Clock,
  Sparkles,
  User,
  Building2,
  Wallet,
  TrendingUp,
  FileText,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
} from "lucide-react";

export default function FaqPage() {
  const [activeTab, setActiveTab] = useState<"passenger" | "operator">("passenger");

  const passengerFaqs = [
    {
      question: "Stage-Wise Cancellation & Refund Policy",
      answer:
        "Passengers can cancel tickets up to 2 hours before scheduled departure. During cancellation, you can choose to receive your refund directly to your redBus Wallet (Instant) or your Original Payment Method (3-5 business days). Cancellation progresses via a transparent 4-stage live tracker: (1) Cancellation Requested ➔ (2) Operator Audit ➔ (3) Refund Processing ➔ (4) Credited / Completed.",
      highlight: "Instant redBus Wallet refunds or 3-5 days to bank/card",
    },
    {
      question: "Cancellation Refund Slabs",
      answer:
        "• More than 24 hours prior to departure: 90% refund (10% standard cancellation fee).\n• 12 to 24 hours prior: 75% refund.\n• 2 to 12 hours prior: 50% refund.\n• Under 2 hours: No refund applicable.\n*Free Cancellation Guarantee riders receive 100% of base fare back when cancelled up to 2 hours before departure.",
      highlight: "Up to 90% refund >24 hrs prior to trip",
    },
    {
      question: "Luggage and Baggage Guidelines",
      answer:
        "Each passenger is allowed up to 15 kg of personal luggage free of charge. Excess baggage is subject to operator discretion and nominal charges (approx Rs 20/kg). Hazardous materials, explosives, and contraband are strictly prohibited.",
      highlight: "15 kg complimentary baggage per passenger",
    },
    {
      question: "Boarding and Govt Photo ID Rules",
      answer:
        "Passengers must arrive at the designated boarding point at least 15 minutes before scheduled departure. Always carry a valid Govt Photo ID (Aadhaar, Voter ID, Passport, or Driving License) alongside your SMS/Email E-Ticket.",
      highlight: "Report 15 mins prior with Valid Photo ID",
    },
    {
      question: "Live GPS Bus Tracking",
      answer:
        "Once your bus is dispatched, you will receive a live GPS tracking link via SMS and WhatsApp 30 minutes before departure. You can also monitor the real-time location directly in the redBus app.",
      highlight: "Live tracking 30 mins before departure",
    },
    {
      question: "Child and Infant Ticket Policy",
      answer:
        "Children aged 5 and above require an individual seat and full ticket fare. Children under 5 travel free when sharing a seat with an accompanying adult.",
      highlight: "Under 5 years travels free on lap",
    },
  ];

  const operatorFaqs = [
    {
      question: "Operator redBus Wallet & Automatic Earnings Credit",
      answer:
        "When passengers book tickets on your buses, the net fare (Total Ticket Fare minus 10% redBus platform commission) is automatically credited directly to your Operator redBus Wallet. You can track all credits, debits, and platform commissions in real time under the Wallet tab in your Operator Hub.",
      highlight: "Net 90% earnings credited instantly to Operator Wallet",
    },
    {
      question: "Passenger Refund Audit & Approval Workflow",
      answer:
        "When a passenger cancels a seat on your bus, their seat is immediately made available back in inventory for other travelers. The cancellation request enters 'Operator Audit' stage. In your Operator Hub ➔ Refunds section, you can review the passenger PNR, cancellation fee, and click 'Approve & Refund' to disburse the amount directly from your Operator Wallet.",
      highlight: "Audit & approve passenger refunds with 1 click",
    },
    {
      question: "AI Price Intelligence & Competitor Benchmarking",
      answer:
        "Our AI Dynamic Pricing engine scans active routes across all operators to compute average competitor fares, optimal dynamic pricing, occupancy forecasts, and promo suggestions. Operators can benchmark their prices against competitors and apply AI promotional discounts to boost bus occupancy.",
      highlight: "Real-time competitor benchmarking & occupancy forecasts",
    },
    {
      question: "Operator Booking Policy & Restriction",
      answer:
        "To maintain audit compliance, prevent inventory monopolization, and comply with platform fair-trade policies, Operator accounts are restricted from booking passenger tickets on the platform. Operators have dedicated access to the Operator Hub for route, bus, schedule, and revenue management.",
      highlight: "Operator accounts dedicated to fleet & revenue operations",
    },
    {
      question: "Fleet Manifest & Passenger List Export",
      answer:
        "Operators can download digital passenger manifests (PDF / CSV) with passenger names, phone numbers, boarding points, and seat numbers for any dispatched bus directly from the Passenger Manifest tab.",
      highlight: "Real-time PDF manifest generation for conductors & staff",
    },
    {
      question: "Platform Commission & Payout Settlements",
      answer:
        "redBus levies a transparent 10% platform fee on completed bookings to cover gateway processing, server infrastructure, 24/7 passenger support, and AI route optimization. Operator Wallet balances can be withdrawn to registered company bank accounts.",
      highlight: "Transparent 10% platform fee with regular bank settlements",
    },
  ];

  const currentFaqs = activeTab === "passenger" ? passengerFaqs : operatorFaqs;

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8 overflow-hidden">
      {/* Title */}
      <motion.div
        initial={{ opacity: 0, y: 25 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: "easeOut" }}
        className="text-center max-w-2xl mx-auto"
      >
        <div className="w-12 h-12 rounded-2xl bg-red-50 dark:bg-red-950/50 text-[#d84e55] dark:text-red-400 flex items-center justify-center mx-auto mb-3 shadow-sm">
          <HelpCircle className="w-6 h-6" />
        </div>
        <h1 className="text-3xl sm:text-4xl font-black text-gray-900 dark:text-white tracking-tight">
          Help, Policies & FAQs
        </h1>
        <p className="text-xs sm:text-sm text-gray-500 dark:text-slate-400 mt-2">
          Official platform guidelines, refund timelines, and operating rules tailored for Passengers and Bus Operators.
        </p>
      </motion.div>

      {/* Role Switcher Tabs */}
      <div className="flex justify-center">
        <div className="bg-gray-100 dark:bg-slate-800 p-1.5 rounded-2xl inline-flex space-x-2 border border-gray-200 dark:border-slate-700 shadow-inner">
          <button
            onClick={() => setActiveTab("passenger")}
            className={`flex items-center space-x-2 px-6 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all ${
              activeTab === "passenger"
                ? "bg-white dark:bg-slate-900 text-[#d84e55] dark:text-red-400 shadow-sm"
                : "text-gray-600 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
            }`}
          >
            <User className="w-4 h-4" />
            <span>Passenger FAQs & Policies</span>
          </button>
          <button
            onClick={() => setActiveTab("operator")}
            className={`flex items-center space-x-2 px-6 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all ${
              activeTab === "operator"
                ? "bg-white dark:bg-slate-900 text-[#d84e55] dark:text-red-400 shadow-sm"
                : "text-gray-600 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
            }`}
          >
            <Building2 className="w-4 h-4" />
            <span>Operator Policies & Earnings</span>
          </button>
        </div>
      </div>

      {/* Role Banner */}
      <AnimatePresence mode="wait">
        {activeTab === "passenger" ? (
          <motion.div
            key="passenger-banner"
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -15 }}
            className="bg-gradient-to-r from-red-50 to-orange-50 dark:from-red-950/20 dark:to-orange-950/20 border border-red-200 dark:border-red-900/40 rounded-2xl p-4 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
          >
            <div className="flex items-start space-x-3">
              <div className="w-10 h-10 rounded-xl bg-red-100 dark:bg-red-900/40 text-[#d84e55] flex items-center justify-center shrink-0">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-bold text-gray-900 dark:text-white">
                  100% Verified Passenger Protection
                </h4>
                <p className="text-xs text-gray-600 dark:text-slate-400 mt-0.5">
                  Real-time refund tracking, instant redBus wallet payouts, and 24/7 AI-guided trip support.
                </p>
              </div>
            </div>
            <Link
              href="/my-bookings"
              className="px-4 py-2 bg-[#d84e55] hover:bg-red-600 text-white rounded-xl text-xs font-bold shrink-0 transition-all flex items-center space-x-1.5"
            >
              <span>View My Bookings</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </motion.div>
        ) : (
          <motion.div
            key="operator-banner"
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -15 }}
            className="bg-gradient-to-r from-amber-50 to-indigo-50 dark:from-amber-950/20 dark:to-indigo-950/20 border border-amber-200 dark:border-amber-900/40 rounded-2xl p-4 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
          >
            <div className="flex items-start space-x-3">
              <div className="w-10 h-10 rounded-xl bg-amber-100 dark:bg-amber-900/40 text-amber-600 flex items-center justify-center shrink-0">
                <Wallet className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-bold text-gray-900 dark:text-white">
                  Operator Wallet & AI Growth Hub
                </h4>
                <p className="text-xs text-gray-600 dark:text-slate-400 mt-0.5">
                  Automated ticket credit settlements, refund audit control, and AI-powered route pricing intelligence.
                </p>
              </div>
            </div>
            <Link
              href="/operator"
              className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-bold shrink-0 transition-all flex items-center space-x-1.5"
            >
              <span>Go to Operator Hub</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </motion.div>
        )}
      </AnimatePresence>

      {/* FAQ Grid */}
      <div className="space-y-4">
        {currentFaqs.map((faq, idx) => (
          <motion.div
            key={`${activeTab}-${idx}`}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, delay: idx * 0.05 }}
            className="bg-white dark:bg-slate-900 rounded-2xl border border-gray-200 dark:border-slate-800 p-5 sm:p-6 shadow-xs hover:border-[#d84e55] dark:hover:border-red-500/60 transition-all"
          >
            <div className="flex items-start justify-between gap-3">
              <h3 className="text-sm sm:text-base font-bold text-gray-900 dark:text-white flex items-center space-x-2.5">
                <span className="w-6 h-6 rounded-lg bg-red-50 dark:bg-red-950/40 text-[#d84e55] dark:text-red-400 font-mono font-black text-xs flex items-center justify-center shrink-0">
                  {idx + 1}
                </span>
                <span>{faq.question}</span>
              </h3>
              {faq.highlight && (
                <span className="hidden sm:inline-block text-[10px] uppercase tracking-wider font-bold px-2.5 py-1 rounded-full bg-red-50 dark:bg-red-950/40 text-[#d84e55] dark:text-red-400 border border-red-200 dark:border-red-900/50 shrink-0">
                  {faq.highlight}
                </span>
              )}
            </div>
            <p className="text-xs text-gray-600 dark:text-slate-300 mt-3 pl-8 leading-relaxed whitespace-pre-line">
              {faq.answer}
            </p>
          </motion.div>
        ))}
      </div>

      {/* AI Assistant CTA */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.5 }}
        className="bg-gradient-to-r from-[#d84e55] via-red-600 to-rose-600 rounded-3xl p-6 sm:p-8 text-white text-center shadow-lg"
      >
        <div className="w-10 h-10 rounded-2xl bg-white/20 backdrop-blur-md flex items-center justify-center mx-auto mb-2 text-white">
          <Sparkles className="w-5 h-5 animate-pulse" />
        </div>
        <h3 className="text-lg sm:text-xl font-black">Need instant assistance?</h3>
        <p className="text-xs sm:text-sm text-red-100 mt-1 max-w-md mx-auto">
          Our Gemini AI Assistant is available 24/7 to answer questions about bookings, refunds, route schedules, or operator onboarding.
        </p>
      </motion.div>
    </div>
  );
}
