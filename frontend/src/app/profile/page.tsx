"use client";

import React, { useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { useAppDispatch, useAppSelector } from "@/store";
import { updateUser } from "@/store/authSlice";
import {
  useUpdateProfileMutation,
  useGetSavedTravellersQuery,
  useAddSavedTravellerMutation,
  useDeleteSavedTravellerMutation,
  useGetMeQuery,
} from "@/store/apiSlice";
import {
  User as UserIcon,
  Mail,
  Phone,
  ShieldCheck,
  AlertCircle,
  CheckCircle2,
  Settings,
  Ticket,
  ArrowRight,
  Sparkles,
  Camera,
  Users,
  Plus,
  Trash2,
  KeyRound,
  Shield,
  Wallet,
} from "lucide-react";
import AvatarSelectorModal from "@/components/AvatarSelectorModal";

export default function ProfilePage() {
  const dispatch = useAppDispatch();
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);
  const { data: latestUser } = useGetMeQuery(undefined, { skip: !isAuthenticated });
  const activeUser = latestUser || user;
  const walletBalance = Number(activeUser?.walletBalance || 0);

  React.useEffect(() => {
    if (latestUser) {
      dispatch(updateUser(latestUser));
    }
  }, [latestUser, dispatch]);

  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [updateProfileMutation, { isLoading: isUpdatingProfile }] = useUpdateProfileMutation();

  // Saved Travellers RTK Queries
  const {
    data: savedTravellers = [],
    isLoading: isLoadingTravellers,
    refetch: refetchTravellers,
  } = useGetSavedTravellersQuery(undefined, { skip: !isAuthenticated });
  const [addSavedTravellerMutation, { isLoading: isAddingTraveller }] = useAddSavedTravellerMutation();
  const [deleteSavedTravellerMutation] = useDeleteSavedTravellerMutation();

  // Add Traveller Form state
  const [showAddTraveller, setShowAddTraveller] = useState(false);
  const [travellerName, setTravellerName] = useState("");
  const [travellerAge, setTravellerAge] = useState<number | "">("");
  const [travellerGender, setTravellerGender] = useState<"MALE" | "FEMALE">("MALE");
  const [travellerError, setTravellerError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .substring(0, 2);
  };

  const handleAvatarSelect = async (avatarUrl: string, gender: "MALE" | "FEMALE") => {
    try {
      const updated = await updateProfileMutation({
        avatarUrl,
        gender: user?.gender || gender,
      }).unwrap();
      dispatch(updateUser(updated));
      setActionSuccess("Profile avatar updated!");
      setTimeout(() => setActionSuccess(""), 3000);
    } catch (err: any) {
      console.error("Failed to update avatar:", err);
    }
  };

  const handleAddTravellerSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setTravellerError("");
    if (!travellerName.trim()) {
      setTravellerError("Passenger name is required.");
      return;
    }
    const ageNum = Number(travellerAge);
    if (!ageNum || ageNum < 1 || ageNum > 120) {
      setTravellerError("Please enter a valid age between 1 and 120.");
      return;
    }

    try {
      await addSavedTravellerMutation({
        name: travellerName.trim(),
        age: ageNum,
        gender: travellerGender,
      }).unwrap();

      setTravellerName("");
      setTravellerAge("");
      setShowAddTraveller(false);
      setActionSuccess("Traveller saved successfully!");
      refetchTravellers();
      setTimeout(() => setActionSuccess(""), 3000);
    } catch (err: any) {
      setTravellerError(err?.data?.message || "Failed to save traveller.");
    }
  };

  const handleDeleteTraveller = async (id: number) => {
    try {
      await deleteSavedTravellerMutation(id).unwrap();
      refetchTravellers();
      setActionSuccess("Traveller removed.");
      setTimeout(() => setActionSuccess(""), 3000);
    } catch (err: any) {
      console.error("Failed to delete traveller:", err);
    }
  };

  if (!isAuthenticated || !user) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center px-4">
        <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-8 max-w-md w-full text-center">
          <div className="w-16 h-16 rounded-2xl bg-red-50 text-[#d84e55] flex items-center justify-center mx-auto mb-4">
            <UserIcon className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Sign In Required</h2>
          <p className="text-sm text-gray-500 mb-6">
            Please log in or register to view your personal redBus profile and manage preferences.
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

  return (
    <div className="min-h-screen bg-gray-50/50 py-10">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        {/* Verification Alert Banner if unverified */}
        {!user.emailVerified && (
          <div className="bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200/80 rounded-2xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-xs">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-amber-100 rounded-xl text-amber-700 shrink-0">
                <AlertCircle className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-bold text-amber-900">Email Verification Required</h4>
                <p className="text-xs text-amber-700">
                  Your email is not verified yet. Verify now to receive instant Brevo e-tickets and payment receipts.
                </p>
              </div>
            </div>
            <Link
              href={`/verify-email?email=${encodeURIComponent(user.email)}`}
              className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors shrink-0"
            >
              Verify with Code →
            </Link>
          </div>
        )}

        {/* Action Success Flash Alert */}
        {actionSuccess && (
          <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center space-x-2 text-xs font-bold text-emerald-800 animate-in fade-in">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>{actionSuccess}</span>
          </div>
        )}

        {/* Profile Card Header with bottom-to-top entrance */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: "easeOut" }}
          className="bg-white dark:bg-slate-900 rounded-3xl shadow-sm border border-gray-100 dark:border-slate-800 overflow-hidden"
        >
          {/* Cover Banner */}
          <div className="h-36 sm:h-44 bg-gradient-to-r from-[#d84e55] via-[#ef4444] to-orange-500 relative">
            <div className="absolute inset-0 bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:16px_16px] opacity-20" />
            <div className="absolute top-4 right-4 flex items-center gap-2">
              <span className="px-3 py-1 bg-black/20 backdrop-blur-md text-white/90 rounded-full text-xs font-semibold flex items-center gap-1.5 border border-white/10 shadow-xs">
                <Sparkles className="w-3.5 h-3.5 text-amber-300 animate-pulse" />
                <span>redBus Member</span>
              </span>
            </div>
          </div>

          {/* Profile Content Body */}
          <div className="px-6 sm:px-8 pb-6">
            {/* Avatar & Action Buttons Row */}
            <div className="flex flex-col sm:flex-row sm:items-end justify-between -mt-16 sm:-mt-20 gap-4 mb-4">
              {/* Avatar with Ring and Camera Trigger */}
              <div className="relative group self-start sm:self-auto">
                <div className="w-28 h-28 sm:w-32 sm:h-32 rounded-3xl bg-white dark:bg-slate-900 p-1.5 shadow-xl ring-4 ring-white dark:ring-slate-900">
                  {user.avatarUrl ? (
                    /* eslint-disable-next-line @next/next/no-img-element */
                    <img
                      src={user.avatarUrl}
                      alt={user.name}
                      className="w-full h-full rounded-2xl object-cover border border-gray-100 dark:border-slate-800 bg-gray-50 dark:bg-slate-800"
                    />
                  ) : (
                    <div className="w-full h-full rounded-2xl bg-gradient-to-tr from-[#d84e55] to-orange-400 text-white flex items-center justify-center font-black text-3xl shadow-inner">
                      {getInitials(user.name)}
                    </div>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => setIsAvatarModalOpen(true)}
                  className="absolute bottom-1 right-1 p-2 rounded-xl bg-gray-900 dark:bg-slate-800 text-white shadow-lg hover:bg-[#d84e55] transition-all cursor-pointer border-2 border-white dark:border-slate-900"
                  title="Change 3D Avatar"
                >
                  <Camera className="w-4 h-4" />
                </button>
              </div>

              {/* Action Buttons on the right side */}
              <div className="flex items-center gap-2.5 pt-2 sm:pt-0">
                <button
                  type="button"
                  onClick={() => setIsAvatarModalOpen(true)}
                  className="inline-flex items-center space-x-1.5 px-4 py-2.5 bg-gray-100 dark:bg-slate-800 hover:bg-gray-200 dark:hover:bg-slate-700 text-gray-800 dark:text-slate-200 rounded-xl text-xs font-bold transition-all cursor-pointer border border-gray-200/80 dark:border-slate-700"
                >
                  <Camera className="w-3.5 h-3.5 text-[#d84e55]" />
                  <span>Choose 3D Avatar</span>
                </button>

                <Link
                  href="/settings"
                  className="inline-flex items-center space-x-2 px-4 py-2.5 bg-gray-900 hover:bg-gray-800 dark:bg-white dark:hover:bg-gray-100 text-white dark:text-gray-900 rounded-xl text-xs font-bold shadow-sm transition-all"
                >
                  <Settings className="w-3.5 h-3.5" />
                  <span>Account Settings</span>
                </Link>
              </div>
            </div>

            {/* User Details (Name, Badges, Status) */}
            <div className="pt-1">
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-2xl sm:text-3xl font-black text-gray-900 dark:text-white tracking-tight">
                  {user.name}
                </h1>
                {user.emailVerified ? (
                  <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-100 dark:bg-emerald-950/60 dark:text-emerald-300 px-2.5 py-1 rounded-full border border-emerald-200 dark:border-emerald-800">
                    <CheckCircle2 className="w-3.5 h-3.5" /> Verified
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 text-xs font-bold text-amber-700 bg-amber-100 dark:bg-amber-950/60 dark:text-amber-300 px-2.5 py-1 rounded-full border border-amber-200 dark:border-amber-800">
                    <AlertCircle className="w-3.5 h-3.5" /> Unverified
                  </span>
                )}
              </div>

              <div className="flex flex-wrap items-center gap-2 mt-2 text-xs text-gray-500 dark:text-slate-400 font-medium">
                <span className="font-semibold text-gray-700 dark:text-slate-300">
                  {user.role === "ROLE_ADMIN" ? "System Administrator" : "redBus Verified Traveler"}
                </span>
                {user.gender && (
                  <>
                    <span className="text-gray-300 dark:text-slate-600">•</span>
                    <span className="px-2.5 py-0.5 bg-gray-100 dark:bg-slate-800 text-gray-700 dark:text-slate-300 rounded-md font-semibold text-[11px] border border-gray-200/60 dark:border-slate-700">
                      {user.gender === "FEMALE" ? "Female Traveler 👩" : "Male Traveler 👨"}
                    </span>
                  </>
                )}
                <span className="text-gray-300 dark:text-slate-600">•</span>
                <span className="text-gray-500 dark:text-slate-400">{user.email}</span>
                {user.phone && (
                  <>
                    <span className="text-gray-300 dark:text-slate-600">•</span>
                    <span className="text-gray-500 dark:text-slate-400">{user.phone}</span>
                  </>
                )}
              </div>
            </div>
          </div>
        </motion.div>

        {/* Profile Details & Saved Travellers Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Main Information & Saved Travellers */}
          <div className="md:col-span-2 space-y-6">
            {/* Personal Details with bottom-to-top lazy loading */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-30px" }}
              transition={{ duration: 0.45, ease: "easeOut" }}
              className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 sm:p-8 space-y-6"
            >
              <h3 className="text-base font-bold text-gray-900 flex items-center space-x-2 border-b border-gray-100 pb-3">
                <UserIcon className="w-4 h-4 text-[#d84e55]" />
                <span>Personal Details</span>
              </h3>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div className="p-4 rounded-2xl bg-gray-50/70 border border-gray-100">
                  <span className="text-xs font-medium text-gray-400 block mb-1">Full Legal Name</span>
                  <p className="text-sm font-bold text-gray-900">{user.name}</p>
                </div>

                <div className="p-4 rounded-2xl bg-gray-50/70 border border-gray-100">
                  <span className="text-xs font-medium text-gray-400 block mb-1">Email Address</span>
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-bold text-gray-900 truncate mr-2">{user.email}</p>
                    <Mail className="w-4 h-4 text-gray-400 shrink-0" />
                  </div>
                </div>

                <div className="p-4 rounded-2xl bg-gray-50/70 border border-gray-100">
                  <span className="text-xs font-medium text-gray-400 block mb-1">Mobile Contact</span>
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-bold text-gray-900">{user.phone || "Not provided"}</p>
                    <Phone className="w-4 h-4 text-gray-400 shrink-0" />
                  </div>
                </div>

                <div className="p-4 rounded-2xl bg-gray-50/70 border border-gray-100">
                  <span className="text-xs font-medium text-gray-400 block mb-1">Gender</span>
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-bold text-gray-900">
                      {user.gender ? (user.gender === "FEMALE" ? "Female" : "Male") : "Not specified"}
                    </p>
                    <UserIcon className="w-4 h-4 text-gray-400 shrink-0" />
                  </div>
                </div>
              </div>

              <div className="pt-1 flex items-center justify-between">
                <Link
                  href="/reset-password"
                  className="text-xs font-semibold text-gray-600 hover:text-[#d84e55] flex items-center space-x-1"
                >
                  <KeyRound className="w-3.5 h-3.5 text-[#d84e55]" />
                  <span>Reset Password via Brevo OTP</span>
                </Link>

                <Link
                  href="/settings"
                  className="text-xs font-bold text-[#d84e55] hover:text-[#b83e44] flex items-center space-x-1"
                >
                  <span>Edit in Settings</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </motion.div>

            {/* SAVED TRAVELLERS SECTION with bottom-to-top lazy loading */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-30px" }}
              transition={{ duration: 0.45, ease: "easeOut", delay: 0.1 }}
              className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 sm:p-8 space-y-5"
            >
              <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                <div className="flex items-center space-x-2">
                  <div className="p-1.5 bg-red-50 text-[#d84e55] rounded-xl">
                    <Users className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-gray-900">Saved Travellers</h3>
                    <p className="text-xs text-gray-400">Auto-fill passenger details at bus checkout</p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => setShowAddTraveller(!showAddTraveller)}
                  className="inline-flex items-center space-x-1 px-3 py-1.5 rounded-xl bg-red-50 text-[#d84e55] hover:bg-red-100 text-xs font-bold transition-colors cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>{showAddTraveller ? "Cancel" : "Add Traveller"}</span>
                </button>
              </div>

              {/* Add Traveller Form */}
              {showAddTraveller && (
                <form
                  onSubmit={handleAddTravellerSubmit}
                  className="p-4 bg-gray-50/90 rounded-2xl border border-gray-200/80 space-y-4 animate-in fade-in"
                >
                  <h4 className="text-xs font-bold text-gray-800 uppercase tracking-wider">New Passenger Profile</h4>

                  {travellerError && (
                    <div className="text-xs text-red-600 bg-red-50 p-2 rounded-lg border border-red-200">
                      {travellerError}
                    </div>
                  )}

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div>
                      <label className="block text-[11px] font-bold text-gray-700 mb-1">Full Name</label>
                      <input
                        type="text"
                        required
                        value={travellerName}
                        onChange={(e) => setTravellerName(e.target.value)}
                        placeholder="e.g. Priya Sharma"
                        className="w-full px-3 py-2 bg-white rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-bold text-gray-700 mb-1">Age</label>
                      <input
                        type="number"
                        required
                        min={1}
                        max={120}
                        value={travellerAge}
                        onChange={(e) => setTravellerAge(e.target.value ? Number(e.target.value) : "")}
                        placeholder="e.g. 26"
                        className="w-full px-3 py-2 bg-white rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-bold text-gray-700 mb-1">Gender</label>
                      <select
                        value={travellerGender}
                        onChange={(e) => setTravellerGender(e.target.value as "MALE" | "FEMALE")}
                        className="w-full px-3 py-2 bg-white rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                      >
                        <option value="MALE">Male (👨)</option>
                        <option value="FEMALE">Female (👩)</option>
                      </select>
                    </div>
                  </div>

                  <div className="flex justify-end space-x-2 pt-1">
                    <button
                      type="button"
                      onClick={() => setShowAddTraveller(false)}
                      className="px-3 py-1.5 text-xs text-gray-600 hover:bg-gray-200 rounded-xl"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={isAddingTraveller}
                      className="px-4 py-1.5 bg-[#d84e55] hover:bg-[#b83e44] text-white text-xs font-bold rounded-xl shadow-xs disabled:opacity-50 cursor-pointer"
                    >
                      {isAddingTraveller ? "Saving..." : "Save Traveller"}
                    </button>
                  </div>
                </form>
              )}

              {/* Saved Travellers List */}
              {isLoadingTravellers ? (
                <div className="py-6 text-center text-xs text-gray-400">Loading travellers...</div>
              ) : savedTravellers.length === 0 ? (
                <div className="py-6 text-center border-2 border-dashed border-gray-100 rounded-2xl">
                  <p className="text-xs text-gray-400">No saved travellers yet.</p>
                  <p className="text-[11px] text-gray-400 mt-0.5">Add family or frequent co-travellers for 1-click booking.</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {savedTravellers.map((t) => (
                    <div
                      key={t.id}
                      className="p-3.5 rounded-2xl bg-gray-50/80 border border-gray-100 flex items-center justify-between hover:border-gray-200 transition-colors"
                    >
                      <div className="flex items-center space-x-3">
                        <div
                          className={`w-9 h-9 rounded-xl flex items-center justify-center text-sm font-bold shadow-xs ${
                            t.gender === "FEMALE"
                              ? "bg-pink-100 text-pink-700"
                              : "bg-blue-100 text-blue-700"
                          }`}
                        >
                          {t.gender === "FEMALE" ? "👩" : "👨"}
                        </div>
                        <div>
                          <p className="text-xs font-bold text-gray-900">{t.name}</p>
                          <p className="text-[11px] text-gray-500">
                            {t.age} yrs • <span className="capitalize">{t.gender.toLowerCase()}</span>
                          </p>
                        </div>
                      </div>

                      <button
                        onClick={() => handleDeleteTraveller(t.id)}
                        className="p-1.5 text-gray-400 hover:text-red-500 hover:bg-white rounded-lg transition-colors cursor-pointer"
                        title="Delete traveller"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </motion.div>
          </div>

          {/* Quick Stats & Bookings Sidebar with bottom-to-top lazy loading */}
          <div className="space-y-6">
            {/* redBus Wallet Card */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-30px" }}
              transition={{ duration: 0.45, ease: "easeOut" }}
              className="bg-gradient-to-br from-emerald-500/10 via-teal-500/10 to-emerald-500/5 rounded-3xl shadow-sm border border-emerald-200/80 p-6 space-y-4"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2.5">
                  <div className="w-10 h-10 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shadow-md shadow-emerald-600/20">
                    <Wallet className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-gray-900">redBus Wallet</h3>
                    <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                      Instant 1-Click Pay
                    </span>
                  </div>
                </div>
              </div>

              <div className="p-4 bg-white/90 rounded-2xl border border-emerald-100/80 shadow-2xs">
                <span className="text-xs font-semibold text-gray-500 block">Available Balance</span>
                <span className="text-2xl font-black text-emerald-700 block mt-0.5">
                  ₹{walletBalance.toFixed(2)}
                </span>
                <p className="text-[11px] text-gray-500 mt-1 leading-relaxed">
                  Automatic refund credits from cancelled tickets are added here instantly and can be used on checkout for any bus booking.
                </p>
              </div>

              <Link
                href="/"
                className="w-full py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs transition-colors flex items-center justify-between group shadow-sm shadow-emerald-600/20 cursor-pointer"
              >
                <span>Book a Trip with Wallet</span>
                <ArrowRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
              </Link>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-30px" }}
              transition={{ duration: 0.45, ease: "easeOut", delay: 0.1 }}
              className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 space-y-4"
            >
              <h3 className="text-base font-bold text-gray-900 flex items-center space-x-2">
                <Ticket className="w-4 h-4 text-[#d84e55]" />
                <span>My Travel Hub</span>
              </h3>
              <p className="text-xs text-gray-500 leading-relaxed">
                Access your past bus reservations, live boarding status, and instant Brevo PDF e-tickets.
              </p>
              <Link
                href="/my-bookings"
                className="w-full py-2.5 px-4 rounded-xl bg-red-50 text-[#d84e55] font-semibold text-xs hover:bg-red-100 transition-colors flex items-center justify-between group"
              >
                <span>View All My Bookings</span>
                <ArrowRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
              </Link>
            </motion.div>

            {/* Password Reset Direct Card */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-30px" }}
              transition={{ duration: 0.45, ease: "easeOut", delay: 0.15 }}
              className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 space-y-3"
            >
              <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
                <KeyRound className="w-4 h-4" />
              </div>
              <h4 className="text-sm font-bold text-gray-900">Security & Password</h4>
              <p className="text-xs text-gray-500 leading-relaxed">
                Need to reset your redBus password? Send a secure 6-digit OTP to your verified email.
              </p>
              <Link
                href="/reset-password"
                className="inline-flex items-center space-x-1.5 text-xs font-bold text-[#d84e55] hover:underline"
              >
                <span>Reset with OTP →</span>
              </Link>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-30px" }}
              transition={{ duration: 0.45, ease: "easeOut", delay: 0.2 }}
              className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-3xl shadow-md p-6 text-white space-y-3"
            >
              <div className="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center text-red-400">
                <Sparkles className="w-5 h-5" />
              </div>
              <h4 className="text-sm font-bold">AI Smart Concierge</h4>
              <p className="text-xs text-gray-300 leading-relaxed">
                Need to reschedule, email a ticket, or ask route recommendations? Ask your AI Assistant anytime.
              </p>
            </motion.div>
          </div>
        </div>
      </div>

      {/* Avatar Selector Modal */}
      <AvatarSelectorModal
        isOpen={isAvatarModalOpen}
        onClose={() => setIsAvatarModalOpen(false)}
        currentAvatarUrl={user.avatarUrl}
        onSelect={handleAvatarSelect}
      />
    </div>
  );
}
