"use client";

import React from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { Bus, ShieldCheck, Clock, Headphones, RefreshCw } from "lucide-react";

export default function Footer() {
  return (
    <footer className="bg-[#1f222e] text-gray-300 pt-8 sm:pt-16 pb-8 sm:pb-12 border-t border-gray-800 mt-8 sm:mt-20 overflow-hidden">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Features banner with bottom-to-top lazy loading */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-20px" }}
          transition={{ duration: 0.4, ease: "easeOut" }}
          className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-6 pb-6 sm:pb-12 border-b border-gray-700/60 mb-6 sm:mb-12"
        >
          <div className="flex items-start space-x-2.5 sm:space-x-3.5">
            <div className="p-2 sm:p-3 bg-red-500/10 text-[#d84e55] rounded-lg sm:rounded-xl shrink-0">
              <ShieldCheck className="w-4 h-4 sm:w-6 sm:h-6" />
            </div>
            <div>
              <h4 className="text-white font-semibold text-xs sm:text-sm">Safe & Verified</h4>
              <p className="text-[10px] sm:text-xs text-gray-400 mt-0.5 leading-snug line-clamp-2 sm:line-clamp-none">
                3,500+ bus partners with sanitized coaches & CCTV.
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-2.5 sm:space-x-3.5">
            <div className="p-2 sm:p-3 bg-red-500/10 text-[#d84e55] rounded-lg sm:rounded-xl shrink-0">
              <RefreshCw className="w-4 h-4 sm:w-6 sm:h-6" />
            </div>
            <div>
              <h4 className="text-white font-semibold text-xs sm:text-sm">Instant Refunds</h4>
              <p className="text-[10px] sm:text-xs text-gray-400 mt-0.5 leading-snug line-clamp-2 sm:line-clamp-none">
                Swift refunds directly back into payment source.
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-2.5 sm:space-x-3.5">
            <div className="p-2 sm:p-3 bg-red-500/10 text-[#d84e55] rounded-lg sm:rounded-xl shrink-0">
              <Clock className="w-4 h-4 sm:w-6 sm:h-6" />
            </div>
            <div>
              <h4 className="text-white font-semibold text-xs sm:text-sm">Live GPS Tracking</h4>
              <p className="text-[10px] sm:text-xs text-gray-400 mt-0.5 leading-snug line-clamp-2 sm:line-clamp-none">
                Real-time tracking shared with family & friends.
              </p>
            </div>
          </div>

          <div className="flex items-start space-x-2.5 sm:space-x-3.5">
            <div className="p-2 sm:p-3 bg-red-500/10 text-[#d84e55] rounded-lg sm:rounded-xl shrink-0">
              <Headphones className="w-4 h-4 sm:w-6 sm:h-6" />
            </div>
            <div>
              <h4 className="text-white font-semibold text-xs sm:text-sm">24x7 AI Support</h4>
              <p className="text-[10px] sm:text-xs text-gray-400 mt-0.5 leading-snug line-clamp-2 sm:line-clamp-none">
                Instant conversational help backed by Gemini AI.
              </p>
            </div>
          </div>
        </motion.div>

        {/* Links grid with bottom-to-top lazy loading */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-20px" }}
          transition={{ duration: 0.4, ease: "easeOut", delay: 0.05 }}
          className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-8 mb-6 sm:mb-12"
        >
          <div>
            <h3 className="text-white font-bold text-xs sm:text-sm mb-2.5 sm:mb-4 tracking-wider uppercase">Top Routes</h3>
            <ul className="space-y-1.5 sm:space-y-2.5 text-[11px] sm:text-xs text-gray-400">
              <li>
                <Link href="/bus-tickets/bangalore-to-chennai" className="hover:text-white transition-colors">
                  Bangalore to Chennai
                </Link>
              </li>
              <li>
                <Link href="/bus-tickets/mumbai-to-pune" className="hover:text-white transition-colors">
                  Mumbai to Pune
                </Link>
              </li>
              <li>
                <Link href="/bus-tickets/delhi-to-jaipur" className="hover:text-white transition-colors">
                  Delhi to Jaipur
                </Link>
              </li>
              <li>
                <Link href="/bus-tickets/hyderabad-to-bangalore" className="hover:text-white transition-colors">
                  Hyderabad to Bangalore
                </Link>
              </li>
              <li>
                <Link href="/bus-tickets/chennai-to-bangalore" className="hover:text-white transition-colors">
                  Chennai to Bangalore
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-bold text-xs sm:text-sm mb-2.5 sm:mb-4 tracking-wider uppercase">Operators</h3>
            <ul className="space-y-1.5 sm:space-y-2.5 text-[11px] sm:text-xs text-gray-400">
              <li>IntrCity SmartBus</li>
              <li>Zingbus Plus</li>
              <li>Orange Travels</li>
              <li>SRS Travels</li>
              <li>KSRTC Airavat</li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-bold text-xs sm:text-sm mb-2.5 sm:mb-4 tracking-wider uppercase">About redBus</h3>
            <ul className="space-y-1.5 sm:space-y-2.5 text-[11px] sm:text-xs text-gray-400">
              <li>
                <Link href="/faq" className="hover:text-white transition-colors">
                  Cancellation Policies
                </Link>
              </li>
              <li>
                <Link href="/faq" className="hover:text-white transition-colors">
                  Baggage Guidelines
                </Link>
              </li>
              <li>
                <Link href="/faq" className="hover:text-white transition-colors">
                  FAQ & Support
                </Link>
              </li>
              <li>Terms & Privacy</li>
            </ul>
          </div>

          <div className="col-span-2 md:col-span-1">
            <div className="flex items-center space-x-2 text-white font-bold text-base sm:text-lg mb-2 sm:mb-4">
              <Bus className="w-5 h-5 sm:w-6 sm:h-6 text-[#d84e55]" />
              <span>red<span className="text-red-500">Bus</span></span>
            </div>
            <p className="text-[11px] sm:text-xs text-gray-400 leading-relaxed">
              World&apos;s largest bus ticket booking service trusted by 25M+ customers.
            </p>
            <div className="mt-2.5 sm:mt-4 p-2 sm:p-3 bg-gray-800/80 rounded-lg sm:rounded-xl border border-gray-700 text-[10px] sm:text-xs text-gray-300">
              <span className="font-semibold text-white">✨ AI Powered:</span> Conversational queries & automated assistant.
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.4 }}
          className="pt-4 sm:pt-8 border-t border-gray-800 text-center text-[10px] sm:text-xs text-gray-500"
        >
          <p>© {new Date().getFullYear()} redBus Clone. All Rights Reserved. Built with Next.js, Spring Boot & Google Gemini AI.</p>
        </motion.div>
      </div>
    </footer>
  );
}
