"use client";

import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Check, Sparkles } from "lucide-react";
import { MALE_AVATARS, FEMALE_AVATARS, ALL_AVATARS, AvatarOption } from "@/lib/avatars";

interface AvatarSelectorModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentAvatarUrl?: string;
  onSelect: (url: string, gender: "MALE" | "FEMALE") => void;
}

export default function AvatarSelectorModal({
  isOpen,
  onClose,
  currentAvatarUrl,
  onSelect,
}: AvatarSelectorModalProps) {
  const [activeTab, setActiveTab] = useState<"ALL" | "MALE" | "FEMALE">("ALL");
  const [selectedUrl, setSelectedUrl] = useState<string>(currentAvatarUrl || "");
  const [selectedGender, setSelectedGender] = useState<"MALE" | "FEMALE">("MALE");

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

  const displayList: AvatarOption[] =
    activeTab === "MALE"
      ? MALE_AVATARS
      : activeTab === "FEMALE"
      ? FEMALE_AVATARS
      : ALL_AVATARS;

  const handleChoose = (opt: AvatarOption) => {
    setSelectedUrl(opt.url);
    setSelectedGender(opt.gender);
  };

  const handleSave = () => {
    if (selectedUrl) {
      onSelect(selectedUrl, selectedGender);
    }
    onClose();
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
            className="bg-white rounded-3xl max-w-lg w-full overflow-hidden shadow-2xl border border-gray-100 relative flex flex-col max-h-[90vh] z-10"
          >
        {/* Header */}
        <div className="p-6 pb-4 border-b border-gray-100 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 rounded-xl bg-red-50 text-[#d84e55] flex items-center justify-center">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-gray-900">Choose Profile Avatar</h3>
              <p className="text-xs text-gray-500">Pick a stylized 3D avatar character</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Filter Tabs */}
        <div className="px-6 pt-4 flex space-x-2">
          {(["ALL", "MALE", "FEMALE"] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                activeTab === tab
                  ? "bg-[#d84e55] text-white shadow-xs"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {tab === "ALL" ? "All Avatars" : tab === "MALE" ? "👨 Male 3D" : "👩 Female 3D"}
            </button>
          ))}
        </div>

        {/* Avatar Grid */}
        <div className="p-6 overflow-y-auto grid grid-cols-3 sm:grid-cols-4 gap-4">
          {displayList.map((opt) => {
            const isSelected = selectedUrl === opt.url;
            return (
              <button
                key={opt.id}
                onClick={() => handleChoose(opt)}
                className={`flex flex-col items-center p-3 rounded-2xl border-2 transition-all group relative cursor-pointer ${
                  isSelected
                    ? "border-[#d84e55] bg-red-50/50 shadow-md scale-102"
                    : "border-gray-100 bg-gray-50 hover:border-gray-300 hover:bg-white"
                }`}
              >
                <div className="w-16 h-16 rounded-full overflow-hidden bg-white shadow-inner flex items-center justify-center border border-gray-100">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={opt.url}
                    alt={opt.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                    loading="lazy"
                  />
                </div>
                <span className="text-[11px] font-semibold text-gray-700 mt-2 text-center truncate max-w-full">
                  {opt.name.split(" ")[0]}
                </span>
                <span className="text-[9px] text-gray-400 capitalize">
                  {opt.gender.toLowerCase()}
                </span>

                {isSelected && (
                  <div className="absolute top-1 right-1 w-5 h-5 rounded-full bg-[#d84e55] text-white flex items-center justify-center shadow-xs">
                    <Check className="w-3 h-3 stroke-[3]" />
                  </div>
                )}
              </button>
            );
          })}
        </div>

        {/* Footer Actions */}
        <div className="p-4 bg-gray-50 border-t border-gray-100 flex items-center justify-end space-x-3">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-200 transition-colors"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={!selectedUrl}
            className="px-6 py-2 rounded-xl text-xs font-bold bg-[#d84e55] hover:bg-[#b83e44] text-white shadow-md shadow-red-500/20 disabled:opacity-50 transition-all cursor-pointer"
          >
            Select Avatar
          </button>
        </div>
      </motion.div>
    </div>
      )}
    </AnimatePresence>
  );
}
