"use client";

import React, { useEffect } from "react";
import { LogOut, X } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface LogoutModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  userName?: string;
}

export default function LogoutModal({ isOpen, onClose, onConfirm, userName }: LogoutModalProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      window.addEventListener("keydown", handleKeyDown);
      return () => {
        document.body.style.overflow = originalOverflow || "auto";
        window.removeEventListener("keydown", handleKeyDown);
      };
    }
  }, [isOpen, onClose]);

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm"
            onClick={onClose}
          />

          {/* Modal Dialog */}
          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 15 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.94, y: 15 }}
            transition={{ type: "spring", damping: 25, stiffness: 350 }}
            className="relative bg-white rounded-3xl shadow-2xl max-w-md w-full p-6 border border-gray-100 z-10"
          >
            {/* Close Button */}
            <button
              onClick={onClose}
              className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Icon & Heading */}
            <div className="flex flex-col items-center text-center pt-2 pb-4">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", damping: 15, stiffness: 300, delay: 0.1 }}
                className="w-16 h-16 rounded-2xl bg-red-50 border border-red-100 flex items-center justify-center text-[#d84e55] shadow-inner mb-4"
              >
                <LogOut className="w-8 h-8 text-[#d84e55] stroke-[2.2]" />
              </motion.div>

              <h3 className="text-xl font-bold text-gray-900 tracking-tight">
                Log Out Confirmation
              </h3>
              <p className="text-sm text-gray-500 mt-2 leading-relaxed px-2">
                Are you sure you want to log out{userName ? `, ${userName}` : ""}? You will need to sign in again to access your e-tickets and active bookings.
              </p>
            </div>

            {/* Action Buttons */}
            <div className="grid grid-cols-2 gap-3 mt-4">
              <button
                type="button"
                onClick={onClose}
                className="w-full py-2.5 px-4 rounded-xl border border-gray-200 text-sm font-semibold text-gray-700 hover:bg-gray-50 transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={onConfirm}
                className="w-full py-2.5 px-4 rounded-xl bg-[#d84e55] hover:bg-[#b93b41] text-sm font-semibold text-white shadow-md shadow-red-500/20 transition-all flex items-center justify-center space-x-2 cursor-pointer active:scale-95"
              >
                <LogOut className="w-4 h-4" />
                <span>Yes, Log Out</span>
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
