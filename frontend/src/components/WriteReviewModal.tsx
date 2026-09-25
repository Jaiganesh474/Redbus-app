"use client";

import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useCreateReviewMutation } from "@/store/apiSlice";
import { useAppSelector } from "@/store";
import { Star, X, CheckCircle2, Sparkles } from "lucide-react";

interface WriteReviewModalProps {
  busId: number;
  busName: string;
  isOpen: boolean;
  onClose: () => void;
  onReviewSubmitted?: () => void;
}

const AVAILABLE_TAGS = [
  "Punctuality",
  "Staff behavior",
  "Seat / Sleep Comfort",
  "Driving",
  "Cleanliness",
  "Rest stop hygiene",
  "AC",
  "Live tracking",
];

export default function WriteReviewModal({
  busId,
  busName,
  isOpen,
  onClose,
  onReviewSubmitted,
}: WriteReviewModalProps) {
  const { user } = useAppSelector((state) => state.auth);
  const [rating, setRating] = useState<number>(5);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [userName, setUserName] = useState<string>(user?.name || "");
  const [comment, setComment] = useState<string>("");
  const [selectedTags, setSelectedTags] = useState<string[]>([
    "Punctuality",
    "Seat / Sleep Comfort",
  ]);
  const [submitted, setSubmitted] = useState<boolean>(false);

  const [createReviewMutation, { isLoading }] = useCreateReviewMutation();

  // Prevent background scrolling when modal is open
  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = originalOverflow || "auto";
      };
    }
  }, [isOpen]);

  const toggleTag = (tag: string) => {
    if (selectedTags.includes(tag)) {
      setSelectedTags(selectedTags.filter((t) => t !== tag));
    } else {
      setSelectedTags([...selectedTags, tag]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createReviewMutation({
        busId,
        rating,
        comment,
        userName: userName.trim() || user?.name || "Verified Passenger",
        tags: selectedTags,
      }).unwrap();

      setSubmitted(true);
      if (onReviewSubmitted) onReviewSubmitted();
      setTimeout(() => {
        setSubmitted(false);
        onClose();
      }, 1600);
    } catch (err) {
      console.error("Failed to submit review:", err);
    }
  };

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
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-xs cursor-pointer"
          />

          {/* Modal Dialog */}
          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.94, y: 20 }}
            transition={{ type: "spring", damping: 25, stiffness: 320 }}
            className="bg-white dark:bg-[#0f172a] rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-gray-100 dark:border-slate-800 relative z-10"
          >
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {submitted ? (
          <div className="py-12 text-center space-y-3">
            <div className="w-14 h-14 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold text-gray-900 dark:text-white">
              Thank You for Your Review!
            </h3>
            <p className="text-xs text-gray-500 dark:text-slate-400">
              Your feedback has been published to help verified travelers on redBus.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <div className="flex items-center space-x-2 text-xs font-semibold text-[#d84e55] uppercase tracking-wider">
                <Sparkles className="w-3.5 h-3.5" />
                <span>Verified Traveler Review</span>
              </div>
              <h3 className="text-lg font-bold text-gray-900 dark:text-white mt-1">
                Rate your trip with {busName}
              </h3>
              <p className="text-xs text-gray-500 dark:text-slate-400">
                Share your authentic travel experience to help future passengers.
              </p>
            </div>

            {/* Star Rating selector */}
            <div className="p-4 bg-gray-50 dark:bg-slate-800/60 rounded-2xl border border-gray-100 dark:border-slate-800 flex flex-col items-center justify-center space-y-2">
              <span className="text-xs font-bold text-gray-700 dark:text-slate-300">
                Overall Bus Experience:
              </span>
              <div className="flex items-center space-x-2">
                {[1, 2, 3, 4, 5].map((star) => {
                  const filled = (hoverRating || rating) >= star;
                  return (
                    <button
                      key={star}
                      type="button"
                      onMouseEnter={() => setHoverRating(star)}
                      onMouseLeave={() => setHoverRating(0)}
                      onClick={() => setRating(star)}
                      className="p-1 transition-transform hover:scale-110 cursor-pointer"
                    >
                      <Star
                        className={`w-7 h-7 ${
                          filled
                            ? "fill-amber-400 text-amber-400"
                            : "fill-gray-200 text-gray-300 dark:fill-slate-700 dark:text-slate-600"
                        }`}
                      />
                    </button>
                  );
                })}
              </div>
              <span className="text-xs font-bold text-[#d84e55] dark:text-red-400">
                {rating === 5 && "Excellent (5.0 ★)"}
                {rating === 4 && "Very Good (4.0 ★)"}
                {rating === 3 && "Average (3.0 ★)"}
                {rating === 2 && "Below Average (2.0 ★)"}
                {rating === 1 && "Poor (1.0 ★)"}
              </span>
            </div>

            {/* Passenger Name */}
            <div>
              <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1.5">
                Your Name
              </label>
              <input
                type="text"
                value={userName}
                onChange={(e) => setUserName(e.target.value)}
                placeholder="Enter your name"
                className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-xl text-xs text-gray-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
              />
            </div>

            {/* Loved Tags Pills */}
            <div>
              <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1.5">
                What did you like about this trip?
              </label>
              <div className="flex flex-wrap gap-1.5">
                {AVAILABLE_TAGS.map((tag) => {
                  const isSelected = selectedTags.includes(tag);
                  return (
                    <button
                      key={tag}
                      type="button"
                      onClick={() => toggleTag(tag)}
                      className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all cursor-pointer ${
                        isSelected
                          ? "bg-emerald-600 text-white shadow-xs"
                          : "bg-gray-100 dark:bg-slate-800 text-gray-700 dark:text-slate-300 hover:bg-gray-200 dark:hover:bg-slate-700"
                      }`}
                    >
                      {tag} {isSelected && "✓"}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Review Comment */}
            <div>
              <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1.5">
                Review & Suggestions
              </label>
              <textarea
                rows={3}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="How was the cleanliness, punctuality, driver behavior, and sleeper comfort?"
                className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-xl text-xs text-gray-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
              />
            </div>

            <div className="flex items-center justify-end space-x-3 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-xs font-bold text-gray-600 dark:text-slate-300 hover:bg-gray-100 dark:hover:bg-slate-800 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isLoading}
                className="px-5 py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white text-xs font-bold rounded-xl shadow-md transition-all disabled:opacity-50 cursor-pointer"
              >
                {isLoading ? "Submitting..." : "Submit Review"}
              </button>
            </div>
          </form>
        )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
