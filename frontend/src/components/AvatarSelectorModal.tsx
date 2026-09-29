"use client";

import React, { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Check, Sparkles, Upload, Camera, Loader2, User as UserIcon, Trash2 } from "lucide-react";
import { MALE_AVATARS, FEMALE_AVATARS, ALL_AVATARS, AvatarOption } from "@/lib/avatars";
import { useUploadAvatarMutation, API_BASE_URL } from "@/store/apiSlice";

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
  const [activeTab, setActiveTab] = useState<"ALL" | "MALE" | "FEMALE" | "CUSTOM">("ALL");
  const [selectedUrl, setSelectedUrl] = useState<string>(currentAvatarUrl || "");
  const [selectedGender, setSelectedGender] = useState<"MALE" | "FEMALE">("MALE");
  const [customImage, setCustomImage] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [uploadAvatarMutation] = useUploadAvatarMutation();

  // Prevent background scrolling when modal is open and sync currentAvatarUrl
  useEffect(() => {
    if (isOpen) {
      setSelectedUrl(currentAvatarUrl || "");
      if (currentAvatarUrl && !ALL_AVATARS.some((a) => a.url === currentAvatarUrl)) {
        setCustomImage(currentAvatarUrl);
      }
      setUploadError("");
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = originalOverflow || "auto";
      };
    }
  }, [isOpen, currentAvatarUrl]);

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

  const handleSelectDefaultIcon = () => {
    setSelectedUrl("");
  };

  const handleRemovePhotoImmediately = () => {
    setSelectedUrl("");
    onSelect("", selectedGender);
    onClose();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsUploading(true);
    setUploadError("");

    try {
      // 1. Client-side Square Crop & Canvas Optimization
      const croppedBlob = await new Promise<Blob>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (event) => {
          const img = new Image();
          img.onload = () => {
            const canvas = document.createElement("canvas");
            const size = 320;
            canvas.width = size;
            canvas.height = size;
            const ctx = canvas.getContext("2d");
            if (!ctx) {
              reject(new Error("Canvas error"));
              return;
            }
            const minDim = Math.min(img.width, img.height);
            const startX = (img.width - minDim) / 2;
            const startY = (img.height - minDim) / 2;
            ctx.drawImage(img, startX, startY, minDim, minDim, 0, 0, size, size);
            canvas.toBlob(
              (blob) => {
                if (blob) resolve(blob);
                else reject(new Error("Blob generation failed"));
              },
              "image/jpeg",
              0.88
            );
          };
          img.onerror = () => reject(new Error("Failed to load image"));
          img.src = event.target?.result as string;
        };
        reader.onerror = () => reject(new Error("File reading failed"));
        reader.readAsDataURL(file);
      });

      // 2. Try Cloudinary direct upload if configured, else upload via Backend
      let uploadedUrl = "";
      const cloudName = process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME;
      const uploadPreset = process.env.NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET || "ml_default";

      if (cloudName) {
        try {
          const cloudFormData = new FormData();
          cloudFormData.append("file", croppedBlob, "avatar.jpg");
          cloudFormData.append("upload_preset", uploadPreset);
          const cloudRes = await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/image/upload`, {
            method: "POST",
            body: cloudFormData,
          });
          if (cloudRes.ok) {
            const cloudJson = await cloudRes.json();
            if (cloudJson.secure_url) {
              uploadedUrl = cloudJson.secure_url;
            }
          }
        } catch (cloudErr) {
          console.warn("Cloudinary upload failed, using backend storage:", cloudErr);
        }
      }

      // 3. Backend Storage Fallback
      if (!uploadedUrl) {
        const formData = new FormData();
        formData.append("file", croppedBlob, "avatar.jpg");
        const res = await uploadAvatarMutation(formData).unwrap();
        const backendHost = API_BASE_URL.replace(/\/api\/v1\/?$/, "");
        uploadedUrl = res.avatarUrl.startsWith("http") ? res.avatarUrl : `${backendHost}${res.avatarUrl}`;
      }

      setCustomImage(uploadedUrl);
      setSelectedUrl(uploadedUrl);
      setActiveTab("CUSTOM");
    } catch (err: any) {
      console.error("Avatar upload error:", err);
      setUploadError(err?.data?.message || err?.message || "Failed to upload image. Please try again.");
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleSave = () => {
    onSelect(selectedUrl, selectedGender);
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
            className="bg-white dark:bg-slate-900 rounded-3xl max-w-lg w-full overflow-hidden shadow-2xl border border-gray-100 dark:border-slate-800 relative flex flex-col max-h-[90vh] z-10"
          >
            {/* Hidden File Input */}
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handleFileChange}
            />

            {/* Header */}
            <div className="p-6 pb-4 border-b border-gray-100 dark:border-slate-800 flex items-center justify-between">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-xl bg-red-50 dark:bg-red-950/60 text-[#d84e55] flex items-center justify-center shadow-2xs">
                  <Sparkles className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-white">
                    Choose Profile Photo
                  </h3>
                  <p className="text-xs text-gray-500 dark:text-slate-400">
                    Pick a 3D avatar, upload your own photo, or use the default icon
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="p-2 rounded-full text-gray-400 hover:text-gray-700 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Filter Tabs & Remove Photo Action */}
            <div className="px-6 pt-4 flex flex-wrap items-center justify-between gap-2">
              <div className="flex flex-wrap gap-1.5">
                {[
                  { id: "ALL", label: "All Avatars" },
                  { id: "MALE", label: "👨 Male 3D" },
                  { id: "FEMALE", label: "👩 Female 3D" },
                  { id: "CUSTOM", label: "📷 Device Upload" },
                ].map((tab) => (
                  <button
                    key={tab.id}
                    type="button"
                    onClick={() => setActiveTab(tab.id as any)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                      activeTab === tab.id
                        ? "bg-[#d84e55] text-white shadow-xs"
                        : "bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-slate-300 hover:bg-gray-200 dark:hover:bg-slate-700"
                    }`}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>

              {currentAvatarUrl && (
                <button
                  type="button"
                  onClick={handleRemovePhotoImmediately}
                  className="px-2.5 py-1 text-[11px] font-bold text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg flex items-center gap-1 transition-colors cursor-pointer"
                  title="Remove custom photo and use default icon"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Remove Photo</span>
                </button>
              )}
            </div>

            {/* Modal Body */}
            <div className="p-6 overflow-y-auto space-y-4 flex-1">
              {uploadError && (
                <div className="p-3 bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 rounded-xl text-xs text-red-600 dark:text-red-300 font-medium">
                  {uploadError}
                </div>
              )}

              {/* Device Upload Section */}
              {activeTab === "CUSTOM" ? (
                <div className="space-y-4">
                  <div
                    onClick={() => !isUploading && fileInputRef.current?.click()}
                    className="p-8 border-2 border-dashed border-red-200 dark:border-slate-700 hover:border-[#d84e55] dark:hover:border-red-500 rounded-3xl bg-red-50/40 dark:bg-slate-800/50 flex flex-col items-center justify-center text-center cursor-pointer transition-all group"
                  >
                    <div className="w-14 h-14 rounded-2xl bg-white dark:bg-slate-700 shadow-md flex items-center justify-center text-[#d84e55] mb-3 group-hover:scale-110 transition-transform">
                      {isUploading ? (
                        <Loader2 className="w-6 h-6 animate-spin text-[#d84e55]" />
                      ) : (
                        <Upload className="w-6 h-6" />
                      )}
                    </div>
                    <h4 className="text-sm font-bold text-gray-900 dark:text-white">
                      {isUploading ? "Uploading & Processing..." : "Upload from your Device"}
                    </h4>
                    <p className="text-xs text-gray-500 dark:text-slate-400 mt-1 max-w-xs">
                      Tap to browse files or take a photo with your camera. Supports JPG, PNG, WEBP.
                    </p>
                    <button
                      type="button"
                      disabled={isUploading}
                      className="mt-4 px-4 py-2 bg-[#d84e55] text-white rounded-xl text-xs font-bold shadow-xs hover:bg-[#b83e44] transition-colors disabled:opacity-50"
                    >
                      {isUploading ? "Uploading..." : "Choose Photo"}
                    </button>
                  </div>

                  {/* Preview Selected Custom Image */}
                  {customImage && (
                    <div className="p-4 bg-gray-50 dark:bg-slate-800/80 rounded-2xl border border-gray-200 dark:border-slate-700 flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <div className="w-16 h-16 rounded-2xl overflow-hidden ring-2 ring-[#d84e55] shadow-sm shrink-0 bg-white">
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img
                            src={customImage}
                            alt="Custom uploaded preview"
                            className="w-full h-full object-cover"
                          />
                        </div>
                        <div>
                          <span className="text-xs font-bold text-gray-900 dark:text-white block">
                            Custom Device Photo
                          </span>
                          <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-semibold block">
                            ✓ Ready to apply
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center space-x-2">
                        <button
                          type="button"
                          onClick={() => fileInputRef.current?.click()}
                          disabled={isUploading}
                          className="px-3 py-1.5 bg-white dark:bg-slate-700 border border-gray-200 dark:border-slate-600 rounded-xl text-xs font-bold text-gray-700 dark:text-slate-200 hover:bg-gray-50 cursor-pointer disabled:opacity-50"
                        >
                          Change
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setCustomImage(null);
                            setSelectedUrl("");
                          }}
                          className="px-3 py-1.5 text-xs font-semibold text-red-500 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-xl cursor-pointer"
                        >
                          Clear
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                /* Avatar 3D Grid + Default Icon Option */
                <div className="space-y-4">
                  {/* Direct upload quick trigger box */}
                  <div
                    onClick={() => fileInputRef.current?.click()}
                    className="p-3 bg-gradient-to-r from-rose-50 to-orange-50 dark:from-slate-800 dark:to-slate-800/80 rounded-2xl border border-rose-200/80 dark:border-slate-700 flex items-center justify-between cursor-pointer hover:border-red-400 transition-colors"
                  >
                    <div className="flex items-center space-x-3">
                      <div className="w-9 h-9 rounded-xl bg-white dark:bg-slate-700 text-[#d84e55] flex items-center justify-center shadow-xs shrink-0">
                        {isUploading ? (
                          <Loader2 className="w-5 h-5 animate-spin text-[#d84e55]" />
                        ) : (
                          <Camera className="w-5 h-5" />
                        )}
                      </div>
                      <div>
                        <span className="text-xs font-bold text-gray-900 dark:text-white block">
                          Upload Custom Photo
                        </span>
                        <span className="text-[10px] text-gray-500 dark:text-slate-400">
                          Select an image from your device storage
                        </span>
                      </div>
                    </div>
                    <span className="px-2.5 py-1 bg-[#d84e55] text-white text-[11px] font-bold rounded-lg shadow-2xs shrink-0">
                      {isUploading ? "Uploading..." : "Upload"}
                    </span>
                  </div>

                  <div className="grid grid-cols-3 sm:grid-cols-4 gap-3">
                    {/* Default Icon / No Photo Card */}
                    <button
                      type="button"
                      onClick={handleSelectDefaultIcon}
                      className={`flex flex-col items-center p-3 rounded-2xl border-2 transition-all group relative cursor-pointer ${
                        !selectedUrl
                          ? "border-[#d84e55] bg-red-50/60 dark:bg-red-950/40 shadow-md scale-102"
                          : "border-gray-200 dark:border-slate-800 bg-gray-50/80 dark:bg-slate-800/50 hover:border-gray-300 dark:hover:border-slate-700 hover:bg-white"
                      }`}
                    >
                      <div className="w-16 h-16 rounded-full overflow-hidden bg-gradient-to-tr from-[#d84e55] to-orange-400 text-white shadow-md flex items-center justify-center border-2 border-white dark:border-slate-700 shrink-0 group-hover:scale-105 transition-transform">
                        <UserIcon className="w-8 h-8 text-white drop-shadow-xs" />
                      </div>
                      <span className="text-[11px] font-bold text-gray-900 dark:text-white mt-2 text-center truncate max-w-full">
                        Default Icon
                      </span>
                      <span className="text-[9px] text-[#d84e55] dark:text-red-400 font-semibold">
                        Initial Badge
                      </span>

                      {!selectedUrl && (
                        <div className="absolute top-1 right-1 w-5 h-5 rounded-full bg-[#d84e55] text-white flex items-center justify-center shadow-xs">
                          <Check className="w-3 h-3 stroke-[3]" />
                        </div>
                      )}
                    </button>

                    {/* 3D Character Avatars */}
                    {displayList.map((opt) => {
                      const isSelected = selectedUrl === opt.url;
                      return (
                        <button
                          key={opt.id}
                          type="button"
                          onClick={() => handleChoose(opt)}
                          className={`flex flex-col items-center p-3 rounded-2xl border-2 transition-all group relative cursor-pointer ${
                            isSelected
                              ? "border-[#d84e55] bg-red-50/50 dark:bg-red-950/30 shadow-md scale-102"
                              : "border-gray-100 dark:border-slate-800 bg-gray-50 dark:bg-slate-800/60 hover:border-gray-300 dark:hover:border-slate-700 hover:bg-white"
                          }`}
                        >
                          <div className="w-16 h-16 rounded-full overflow-hidden bg-white dark:bg-slate-700 shadow-inner flex items-center justify-center border border-gray-100 dark:border-slate-700 shrink-0">
                            {/* eslint-disable-next-line @next/next/no-img-element */}
                            <img
                              src={opt.url}
                              alt={opt.name}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                              loading="lazy"
                              crossOrigin="anonymous"
                              onError={(e) => {
                                e.currentTarget.src = `https://api.dicebear.com/7.x/bottts/svg?seed=${opt.id}&backgroundColor=ffd5dc`;
                              }}
                            />
                          </div>
                          <span className="text-[11px] font-bold text-gray-800 dark:text-slate-200 mt-2 text-center truncate max-w-full">
                            {opt.name}
                          </span>
                          <span className="text-[9px] text-gray-400 dark:text-slate-400 capitalize">
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
                </div>
              )}
            </div>

            {/* Footer Actions */}
            <div className="p-4 bg-gray-50 dark:bg-slate-900 border-t border-gray-100 dark:border-slate-800 flex items-center justify-between">
              {selectedUrl ? (
                <div className="flex items-center space-x-2">
                  <div className="w-8 h-8 rounded-full overflow-hidden border border-gray-200 dark:border-slate-700 shrink-0 bg-white shadow-2xs">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={selectedUrl} alt="Selected preview" className="w-full h-full object-cover" />
                  </div>
                  <span className="text-xs font-semibold text-gray-600 dark:text-slate-300 truncate max-w-[120px]">
                    Photo selected
                  </span>
                </div>
              ) : (
                <div className="flex items-center space-x-2">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-[#d84e55] to-orange-400 text-white flex items-center justify-center shadow-2xs">
                    <UserIcon className="w-4 h-4" />
                  </div>
                  <span className="text-xs font-semibold text-gray-600 dark:text-slate-300">
                    Default Icon
                  </span>
                </div>
              )}

              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-gray-600 dark:text-slate-400 hover:bg-gray-200 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSave}
                  disabled={isUploading}
                  className="px-6 py-2 rounded-xl text-xs font-bold bg-[#d84e55] hover:bg-[#b83e44] text-white shadow-md shadow-red-500/20 disabled:opacity-50 transition-all cursor-pointer"
                >
                  {!selectedUrl ? "Apply Default Icon" : "Save Photo"}
                </button>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
