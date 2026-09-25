"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useAppDispatch, useAppSelector } from "@/store";
import { updateUser } from "@/store/authSlice";
import { useUpdateProfileMutation, useForgotPasswordMutation } from "@/store/apiSlice";
import {
  User as UserIcon,
  Mail,
  Phone,
  Lock,
  CheckCircle2,
  AlertCircle,
  ArrowLeft,
  Shield,
  Save,
  KeyRound,
  Camera,
  Sparkles,
  ExternalLink,
} from "lucide-react";
import AvatarSelectorModal from "@/components/AvatarSelectorModal";

export default function SettingsPage() {
  const dispatch = useAppDispatch();
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);

  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [gender, setGender] = useState<"MALE" | "FEMALE">("MALE");
  const [avatarUrl, setAvatarUrl] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [otpSentMessage, setOtpSentMessage] = useState("");

  const [updateProfileMutation, { isLoading: isUpdating }] = useUpdateProfileMutation();
  const [forgotPasswordMutation, { isLoading: isSendingOtp }] = useForgotPasswordMutation();

  useEffect(() => {
    if (user) {
      setName(user.name || "");
      setPhone(user.phone || "");
      if (user.gender === "FEMALE" || user.gender === "MALE") {
        setGender(user.gender);
      }
      if (user.avatarUrl) {
        setAvatarUrl(user.avatarUrl);
      }
    }
  }, [user]);

  if (!isAuthenticated || !user) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center px-4">
        <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-8 max-w-md w-full text-center">
          <div className="w-16 h-16 rounded-2xl bg-red-50 text-[#d84e55] flex items-center justify-center mx-auto mb-4">
            <Lock className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Access Restricted</h2>
          <p className="text-sm text-gray-500 mb-6">
            Please log in to manage your account settings and update your profile information.
          </p>
          <Link
            href="/"
            className="inline-flex items-center justify-center px-6 py-3 rounded-xl bg-[#d84e55] text-white font-semibold text-sm hover:bg-[#b83e44] transition-colors shadow-md shadow-red-500/20"
          >
            Return to Homepage
          </Link>
        </div>
      </div>
    );
  }

  const handleAvatarSelect = (url: string, selectedGender: "MALE" | "FEMALE") => {
    setAvatarUrl(url);
    if (!gender) setGender(selectedGender);
  };

  const handleSendResetOtp = async () => {
    setOtpSentMessage("");
    setErrorMessage("");
    try {
      await forgotPasswordMutation({ email: user.email }).unwrap();
      setOtpSentMessage("Password reset 6-digit OTP dispatched to " + user.email + " via Brevo! Redirecting to reset page...");
      setTimeout(() => {
        window.location.href = `/reset-password?email=${encodeURIComponent(user.email)}`;
      }, 1500);
    } catch (err: any) {
      setErrorMessage(err?.data?.message || "Failed to send password reset OTP.");
    }
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMessage("");
    setErrorMessage("");

    if (newPassword && newPassword !== confirmPassword) {
      setErrorMessage("New password and confirm password do not match.");
      return;
    }

    if (newPassword && newPassword.length < 6) {
      setErrorMessage("New password must be at least 6 characters long.");
      return;
    }

    if (newPassword && !currentPassword) {
      setErrorMessage("Please enter your current password to set a new password.");
      return;
    }

    try {
      const payload: {
        name?: string;
        phone?: string;
        gender?: "MALE" | "FEMALE";
        avatarUrl?: string;
        currentPassword?: string;
        newPassword?: string;
      } = {};

      if (name.trim()) payload.name = name.trim();
      if (phone !== undefined) payload.phone = phone.trim();
      if (gender) payload.gender = gender;
      if (avatarUrl) payload.avatarUrl = avatarUrl;
      if (newPassword) {
        payload.currentPassword = currentPassword;
        payload.newPassword = newPassword;
      }

      const updatedUser = await updateProfileMutation(payload).unwrap();
      dispatch(updateUser(updatedUser));

      setSuccessMessage("Your profile and preferences have been updated successfully!");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: any) {
      setErrorMessage(
        err?.data?.message || err?.message || "Failed to update profile. Please check your inputs."
      );
    }
  };

  return (
    <div className="min-h-screen bg-gray-50/50 py-10">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center justify-between">
          <Link
            href="/profile"
            className="inline-flex items-center space-x-2 text-xs font-semibold text-gray-600 hover:text-[#d84e55] transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Profile</span>
          </Link>
          <span className="text-xs text-gray-400">Settings & Security</span>
        </div>

        {/* Header Banner */}
        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 sm:p-8">
          <h1 className="text-2xl font-black text-gray-900 tracking-tight">Account Settings</h1>
          <p className="text-xs text-gray-500 mt-1">
            Update your personal details, 3D avatar, gender preferences, and manage password security.
          </p>
        </div>

        {/* Feedback Messages */}
        {successMessage && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center space-x-3 text-sm text-emerald-800 animate-in fade-in">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span className="font-medium">{successMessage}</span>
          </div>
        )}

        {otpSentMessage && (
          <div className="p-4 bg-blue-50 border border-blue-200 rounded-2xl flex items-center space-x-3 text-sm text-blue-800 animate-in fade-in">
            <CheckCircle2 className="w-5 h-5 text-blue-600 shrink-0" />
            <span className="font-medium">{otpSentMessage}</span>
          </div>
        )}

        {errorMessage && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-2xl flex items-center space-x-3 text-sm text-red-700 animate-in fade-in">
            <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
            <span className="font-medium">{errorMessage}</span>
          </div>
        )}

        {/* Settings Form */}
        <form onSubmit={handleUpdateProfile} className="space-y-6">
          {/* Avatar & Profile Identity */}
          <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 sm:p-8 space-y-5">
            <h3 className="text-base font-bold text-gray-900 flex items-center space-x-2 border-b border-gray-100 pb-3">
              <Sparkles className="w-4 h-4 text-[#d84e55]" />
              <span>Avatar & Traveler Identity</span>
            </h3>

            <div className="flex flex-col sm:flex-row items-center sm:items-start space-y-4 sm:space-y-0 sm:space-x-6">
              <div className="relative">
                <div className="w-20 h-20 rounded-2xl bg-gray-50 border-2 border-gray-200 overflow-hidden flex items-center justify-center shadow-inner">
                  {avatarUrl ? (
                    /* eslint-disable-next-line @next/next/no-img-element */
                    <img src={avatarUrl} alt="Avatar preview" className="w-full h-full object-cover" />
                  ) : (
                    <UserIcon className="w-10 h-10 text-gray-300" />
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => setIsAvatarModalOpen(true)}
                  className="absolute -bottom-2 -right-2 p-2 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl shadow-md cursor-pointer transition-colors"
                  title="Choose 3D Avatar"
                >
                  <Camera className="w-3.5 h-3.5" />
                </button>
              </div>

              <div className="flex-1 text-center sm:text-left space-y-1.5">
                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
                  <button
                    type="button"
                    onClick={() => setIsAvatarModalOpen(true)}
                    className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 rounded-xl text-xs font-bold transition-colors cursor-pointer"
                  >
                    Change 3D Avatar
                  </button>
                  {avatarUrl && (
                    <button
                      type="button"
                      onClick={() => setAvatarUrl("")}
                      className="px-3 py-2 text-gray-400 hover:text-red-500 rounded-xl text-xs font-semibold"
                    >
                      Remove
                    </button>
                  )}
                </div>
                <p className="text-xs text-gray-400">
                  Select a male or female 3D character logo to represent your profile on tickets & reviews.
                </p>
              </div>
            </div>

            {/* Gender Selection */}
            <div className="pt-2">
              <label className="block text-xs font-bold text-gray-700 mb-2">Primary Traveler Gender</label>
              <div className="grid grid-cols-2 gap-3 max-w-sm">
                <button
                  type="button"
                  onClick={() => setGender("MALE")}
                  className={`flex items-center justify-center space-x-2 py-2.5 px-4 rounded-xl border-2 text-xs font-bold transition-all cursor-pointer ${
                    gender === "MALE"
                      ? "border-blue-500 bg-blue-50 text-blue-800 shadow-xs"
                      : "border-gray-200 bg-white text-gray-600 hover:border-gray-300"
                  }`}
                >
                  <span className="text-base">👨</span>
                  <span>Male</span>
                </button>

                <button
                  type="button"
                  onClick={() => setGender("FEMALE")}
                  className={`flex items-center justify-center space-x-2 py-2.5 px-4 rounded-xl border-2 text-xs font-bold transition-all cursor-pointer ${
                    gender === "FEMALE"
                      ? "border-pink-500 bg-pink-50 text-pink-800 shadow-xs"
                      : "border-gray-200 bg-white text-gray-600 hover:border-gray-300"
                  }`}
                >
                  <span className="text-base">👩</span>
                  <span>Female</span>
                </button>
              </div>
              <p className="text-[11px] text-gray-400 mt-1.5">
                Note: Selecting Female enables exclusive access to ladies-adjacent reserved seats.
              </p>
            </div>
          </div>

          {/* General Information Section */}
          <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 sm:p-8 space-y-5">
            <h3 className="text-base font-bold text-gray-900 flex items-center space-x-2 border-b border-gray-100 pb-3">
              <UserIcon className="w-4 h-4 text-[#d84e55]" />
              <span>Personal Information</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1.5">Full Name</label>
                <div className="relative">
                  <UserIcon className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                  <input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Your Full Name"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:border-transparent"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1.5">Mobile Number</label>
                <div className="relative">
                  <Phone className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+91 9876543210"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:border-transparent"
                  />
                </div>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1.5">Email Address</label>
              <div className="relative">
                <Mail className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                <input
                  type="email"
                  disabled
                  value={user.email}
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm bg-gray-100 text-gray-500 cursor-not-allowed"
                />
              </div>
              <p className="text-[11px] text-gray-400 mt-1">
                Your email is linked to your booking identity and Brevo notification dispatch.
              </p>
            </div>
          </div>

          {/* Password & Security Section */}
          <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 sm:p-8 space-y-5">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h3 className="text-base font-bold text-gray-900 flex items-center space-x-2">
                <KeyRound className="w-4 h-4 text-[#d84e55]" />
                <span>Change Password</span>
              </h3>

              {/* Forgot Password Trigger */}
              <button
                type="button"
                onClick={handleSendResetOtp}
                disabled={isSendingOtp}
                className="text-xs font-bold text-[#d84e55] hover:underline flex items-center space-x-1 cursor-pointer"
              >
                <span>{isSendingOtp ? "Sending OTP..." : "Forgot Password? Reset via Email OTP"}</span>
                <ExternalLink className="w-3 h-3" />
              </button>
            </div>

            <p className="text-xs text-gray-500">
              Leave these fields blank if you do not wish to modify your existing password.
            </p>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1.5">Current Password</label>
                <div className="relative">
                  <Lock className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                  <input
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Enter current password"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:border-transparent"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1.5">New Password</label>
                  <div className="relative">
                    <Lock className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                    <input
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Minimum 6 characters"
                      className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:border-transparent"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1.5">Confirm New Password</label>
                  <div className="relative">
                    <Lock className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                    <input
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Repeat new password"
                      className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:border-transparent"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Action Button: Update Profile */}
          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={isUpdating}
              className="px-8 py-3.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-2xl font-bold text-sm shadow-md shadow-red-500/20 transition-all flex items-center space-x-2 disabled:opacity-70 cursor-pointer"
            >
              {isUpdating ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>Updating Profile...</span>
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  <span>Save All Changes</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>

      {/* Avatar Modal */}
      <AvatarSelectorModal
        isOpen={isAvatarModalOpen}
        onClose={() => setIsAvatarModalOpen(false)}
        currentAvatarUrl={avatarUrl}
        onSelect={handleAvatarSelect}
      />
    </div>
  );
}
