"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { HelpCircle, ShieldCheck, RefreshCw, Clock, ArrowRight, Sparkles } from "lucide-react";

export default function FaqPage() {
  const [faqs, setFaqs] = useState<{ question: string; answer: string }[]>([
    {
      question: "Cancellation and Refund Policy",
      answer:
        "Passengers can cancel tickets up to 2 hours before scheduled departure. More than 24 hours: 90% refund (10% fee). 12-24 hours: 75% refund. 2-12 hours: 50% refund. Under 2 hours: No refund. Refunds are credited within 3-5 business days directly to your payment source or instantly to your redBus Wallet.",
    },
    {
      question: "Luggage and Baggage Rules",
      answer:
        "Each passenger is allowed up to 15 kg of personal luggage free of charge. Excess baggage is subject to operator discretion (approx Rs 20/kg). Hazardous materials are strictly prohibited.",
    },
    {
      question: "Boarding and Identification Guidelines",
      answer:
        "Passengers must arrive at the boarding point 15 minutes before scheduled departure. Carry a valid Govt Photo ID (Aadhaar, Passport, DL) along with your SMS/E-Ticket.",
    },
    {
      question: "Child and Infant Fare Policy",
      answer:
        "Children aged 5 and above require an individual seat and full ticket. Children under 5 travel free when sharing a seat with an accompanying adult.",
    },
    {
      question: "Live GPS Bus Tracking",
      answer:
        "Once your bus is dispatched, you will receive a live GPS tracking link via SMS and WhatsApp 30 minutes before departure. You can also track your bus directly in the app.",
    },
    {
      question: "Free Cancellation Guarantee Protection",
      answer:
        "If you opt for Free Cancellation during booking, you will receive a 100% refund of your base ticket fare if cancelled anytime up to 2 hours before bus departure.",
    },
  ]);

  useEffect(() => {
    fetch("http://localhost:8080/api/seo/faqs")
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (data && data.length > 0) {
          setFaqs(data);
        }
      })
      .catch(() => {});
  }, []);

  const faqSchema = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: faqs.map((f) => ({
      "@type": "Question",
      name: f.question,
      acceptedAnswer: {
        "@type": "Answer",
        text: f.answer,
      },
    })),
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-10 overflow-hidden">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(faqSchema) }}
      />

      {/* Title with Down-to-Up entrance */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="text-center max-w-xl mx-auto"
      >
        <div className="w-12 h-12 rounded-2xl bg-red-50 dark:bg-red-950/50 text-[#d84e55] dark:text-red-400 flex items-center justify-center mx-auto mb-3">
          <HelpCircle className="w-6 h-6" />
        </div>
        <h1 className="text-3xl font-black text-gray-900 dark:text-white tracking-tight">
          Help, Policies & FAQs
        </h1>
        <p className="text-xs sm:text-sm text-gray-500 dark:text-slate-400 mt-1">
          Official guidelines powered by redBus Knowledge Base and verified operator terms.
        </p>
      </motion.div>

      {/* FAQ Items with Scroll Lazy Loading */}
      <motion.div
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true, margin: "-40px" }}
        variants={{
          hidden: { opacity: 0 },
          visible: {
            opacity: 1,
            transition: { staggerChildren: 0.08 },
          },
        }}
        className="space-y-4"
      >
        {faqs.map((faq, idx) => (
          <motion.div
            key={idx}
            variants={{
              hidden: { opacity: 0, y: 25 },
              visible: { opacity: 1, y: 0, transition: { duration: 0.45, ease: "easeOut" } },
            }}
            className="bg-white dark:bg-slate-900 rounded-2xl border border-gray-200 dark:border-slate-800 p-6 shadow-xs hover:border-[#d84e55] dark:hover:border-red-500/60 transition-all"
          >
            <h3 className="text-base font-bold text-gray-900 dark:text-white flex items-center space-x-2">
              <span className="text-[#d84e55] dark:text-red-400 font-mono font-black">{idx + 1}.</span>
              <span>{faq.question}</span>
            </h3>
            <p className="text-xs text-gray-600 dark:text-slate-300 mt-3 pl-6 leading-relaxed">
              {faq.answer}
            </p>
          </motion.div>
        ))}
      </motion.div>

      {/* AI Assistant Help CTA with Lazy Loading */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-40px" }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="bg-gradient-to-r from-[#d84e55] via-red-600 to-rose-600 rounded-3xl p-6 sm:p-8 text-white text-center shadow-lg"
      >
        <div className="w-10 h-10 rounded-2xl bg-white/20 backdrop-blur-md flex items-center justify-center mx-auto mb-2 text-white">
          <Sparkles className="w-5 h-5 animate-pulse" />
        </div>
        <h3 className="text-lg sm:text-xl font-black">Still have questions?</h3>
        <p className="text-xs sm:text-sm text-red-100 mt-1 max-w-md mx-auto">
          Chat with our Gemini AI Assistant anytime for instant answers about your specific PNR, seat selection, or route.
        </p>
      </motion.div>
    </div>
  );
}
