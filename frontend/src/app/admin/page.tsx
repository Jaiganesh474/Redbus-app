"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useAppSelector } from "@/store";
import {
  useGetAdminStatsQuery,
  useGetAdminBookingsQuery,
  useGetAdminOperatorsQuery,
  useVerifyOperatorMutation,
  useSuspendOperatorMutation,
  useUpdateOperatorCommissionMutation,
  useTogglePrimoOperatorMutation,
  useGetAdminPrimoBusesQuery,
  useGetAdminOperatorEarningsQuery,
  useGetAdminBannersQuery,
  useCreateBannerMutation,
  useDeleteBannerMutation,
  useGenerateAiBannerMutation,
} from "@/store/apiSlice";
import {
  Shield,
  TrendingUp,
  DollarSign,
  Users,
  Bus,
  CheckCircle2,
  XCircle,
  Clock,
  ArrowDownRight,
  Building2,
  Percent,
  RefreshCw,
  Mail,
  Phone,
  ArrowRight,
  AlertCircle,
  Sparkles,
  Wallet,
  CreditCard,
  Image as ImageIcon,
  Trash2,
  Plus,
  Tag,
  Eye,
  Wand2,
  Award,
  Star,
} from "lucide-react";

export default function AdminDashboardPage() {
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);
  const [activeTab, setActiveTab] = useState<
    "operators" | "primo_operators" | "earnings" | "banners" | "analytics" | "bookings"
  >("operators");
  const [page, setPage] = useState(0);

  // Banner & AI Studio State
  const [aiPrompt, setAiPrompt] = useState("Festive Diwali 25% discount for Bangalore to Goa AC sleeper buses");
  const [targetRoute, setTargetRoute] = useState("");
  const [targetDiscount, setTargetDiscount] = useState<number>(25);
  const [bannerDraft, setBannerDraft] = useState<any>({
    title: "",
    subtitle: "",
    tag: "AI EXCLUSIVE DEAL",
    promoCode: "REDBUS25",
    discountPercentage: 25,
    bgGradient: "from-slate-950 via-red-950/80 to-slate-900",
    badgeColor: "bg-red-500/20 text-red-300 border-red-500/30",
    routeInfo: "Bangalore ⇄ Goa",
    imageUrl: "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=1200&q=80",
    ctaText: "Claim 25% Off",
    ctaLink: "/search",
    active: true,
    sortOrder: 1,
    isAiGenerated: true,
  });

  // Commission edit state
  const [editingCommissionId, setEditingCommissionId] = useState<number | null>(null);
  const [commissionInput, setCommissionInput] = useState<number>(10);
  const [actionSuccess, setActionSuccess] = useState("");
  const [actionError, setActionError] = useState("");

  const isAdmin = user?.role === "ROLE_ADMIN" || user?.roles?.includes("ROLE_ADMIN");

  const {
    data: stats,
    refetch: refetchStats,
  } = useGetAdminStatsQuery(undefined, { skip: !isAdmin });

  const {
    data: operators = [],
    isLoading: isOperatorsLoading,
    refetch: refetchOperators,
  } = useGetAdminOperatorsQuery(undefined, { skip: !isAdmin });

  const {
    data: primoBuses = [],
    isLoading: isPrimoBusesLoading,
    refetch: refetchPrimoBuses,
  } = useGetAdminPrimoBusesQuery(undefined, { skip: !isAdmin });

  const {
    data: bookingsData,
    isLoading: isBookingsLoading,
    refetch: refetchBookings,
  } = useGetAdminBookingsQuery({ page, size: 12 }, { skip: !isAdmin });

  const {
    data: operatorEarnings,
    isLoading: isEarningsLoading,
    refetch: refetchEarnings,
  } = useGetAdminOperatorEarningsQuery(undefined, {
    skip: !isAdmin,
    pollingInterval: 10000,
  });

  const {
    data: adminBanners = [],
    isLoading: isBannersLoading,
    refetch: refetchBanners,
  } = useGetAdminBannersQuery(undefined, { skip: !isAdmin });

  const [createBannerMutation, { isLoading: isCreatingBanner }] = useCreateBannerMutation();
  const [deleteBannerMutation, { isLoading: isDeletingBanner }] = useDeleteBannerMutation();
  const [generateAiBannerMutation, { isLoading: isGeneratingAiBanner }] = useGenerateAiBannerMutation();

  const handleGenerateAiBanner = async () => {
    if (!aiPrompt.trim()) return;
    setActionError("");
    setActionSuccess("");
    try {
      const generated = await generateAiBannerMutation({
        prompt: aiPrompt,
        targetRoute: targetRoute.trim() || undefined,
        targetDiscount: targetDiscount > 0 ? targetDiscount : undefined,
      }).unwrap();

      setBannerDraft({
        title: generated.title || "",
        subtitle: generated.subtitle || "",
        tag: generated.tag || "AI EXCLUSIVE DEAL",
        promoCode: generated.promoCode || "REDAI20",
        discountPercentage: generated.discountPercentage || 20,
        bgGradient: generated.bgGradient || "from-slate-950 via-red-950/80 to-slate-900",
        badgeColor: generated.badgeColor || "bg-red-500/20 text-red-300 border-red-500/30",
        routeInfo: generated.routeInfo || "Pan-India Super Express",
        imageUrl: generated.imageUrl || "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=1200&q=80",
        ctaText: generated.ctaText || "Claim Offer",
        ctaLink: generated.ctaLink || "/search",
        active: true,
        sortOrder: 1,
        isAiGenerated: true,
        promptUsed: aiPrompt,
      });

      setActionSuccess("AI Promotional Banner generated! Preview and customize below before publishing.");
      setTimeout(() => setActionSuccess(""), 4000);
    } catch (err: any) {
      setActionError(err?.data?.message || "Failed to generate AI banner.");
    }
  };

  const handlePublishBanner = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bannerDraft.title.trim()) {
      setActionError("Banner title is required.");
      return;
    }
    setActionError("");
    setActionSuccess("");
    try {
      await createBannerMutation(bannerDraft).unwrap();
      setActionSuccess(`Banner "${bannerDraft.title}" created successfully and published live to homepage!`);
      refetchBanners();
      setBannerDraft({
        title: "",
        subtitle: "",
        tag: "EXCLUSIVE OFFER",
        promoCode: "",
        discountPercentage: 0,
        bgGradient: "from-slate-950 via-red-950/80 to-slate-900",
        badgeColor: "bg-red-500/20 text-red-300 border-red-500/30",
        routeInfo: "",
        imageUrl: "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=1200&q=80",
        ctaText: "Book Now",
        ctaLink: "/search",
        active: true,
        sortOrder: 1,
        isAiGenerated: false,
      });
      setTimeout(() => setActionSuccess(""), 4000);
    } catch (err: any) {
      setActionError(err?.data?.message || "Failed to publish banner.");
    }
  };

  const handleDeleteBanner = async (id: number, title: string) => {
    if (!window.confirm(`Are you sure you want to delete banner "${title}"?`)) return;
    try {
      await deleteBannerMutation(id).unwrap();
      setActionSuccess(`Banner "${title}" removed from homepage.`);
      refetchBanners();
      setTimeout(() => setActionSuccess(""), 3000);
    } catch (err: any) {
      setActionError(err?.data?.message || "Failed to delete banner.");
    }
  };

  const [verifyOperatorMutation, { isLoading: isVerifying }] = useVerifyOperatorMutation();
  const [suspendOperatorMutation, { isLoading: isSuspending }] = useSuspendOperatorMutation();
  const [updateCommissionMutation, { isLoading: isUpdatingCommission }] = useUpdateOperatorCommissionMutation();
  const [togglePrimoOperatorMutation, { isLoading: isTogglingPrimo }] = useTogglePrimoOperatorMutation();

  const handleVerifyOperator = async (operatorId: number, companyName: string) => {
    setActionError("");
    setActionSuccess("");
    try {
      await verifyOperatorMutation(operatorId).unwrap();
      setActionSuccess(`Operator "${companyName}" has been successfully approved & verified!`);
      refetchOperators();
      refetchStats();
      setTimeout(() => setActionSuccess(""), 4000);
    } catch (err: any) {
      setActionError(err?.data?.message || "Failed to verify operator.");
    }
  };

  const handleSuspendOperator = async (operatorId: number, companyName: string) => {
    setActionError("");
    setActionSuccess("");
    try {
      await suspendOperatorMutation(operatorId).unwrap();
      setActionSuccess(`Operator "${companyName}" has been suspended.`);
      refetchOperators();
      refetchStats();
      setTimeout(() => setActionSuccess(""), 4000);
    } catch (err: any) {
      setActionError(err?.data?.message || "Failed to suspend operator.");
    }
  };

  const handleSaveCommission = async (operatorId: number) => {
    setActionError("");
    setActionSuccess("");
    try {
      await updateCommissionMutation({ id: operatorId, commissionRate: commissionInput }).unwrap();
      setActionSuccess(`Commission rate updated to ${commissionInput}%`);
      setEditingCommissionId(null);
      refetchOperators();
      setTimeout(() => setActionSuccess(""), 4000);
    } catch (err: any) {
      setActionError(err?.data?.message || "Failed to update commission.");
    }
  };

  const handleTogglePrimo = async (operatorId: number, nextState: boolean, companyName: string) => {
    setActionError("");
    setActionSuccess("");
    try {
      await togglePrimoOperatorMutation({ id: operatorId, primoCertified: nextState }).unwrap();
      setActionSuccess(
        nextState
          ? `★ "${companyName}" is now a Primo Certified Bus Operator (0% Commission-Free Fleet)!`
          : `"${companyName}" Primo Certification removed (standard 10% commission restored).`
      );
      refetchOperators();
      refetchPrimoBuses();
      refetchStats();
      refetchEarnings();
      setTimeout(() => setActionSuccess(""), 4500);
    } catch (err: any) {
      setActionError(err?.data?.message || "Failed to update Primo Operator status.");
    }
  };

  if (!isAuthenticated || !isAdmin) {
    return (
      <div className="max-w-md mx-auto px-4 py-24 text-center">
        <div className="w-16 h-16 rounded-3xl bg-red-100 text-red-600 flex items-center justify-center mx-auto mb-4 shadow-md shadow-red-500/10">
          <Shield className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-black text-gray-900 tracking-tight">Admin Portal Restricted</h2>
        <p className="text-xs text-gray-500 mt-2 leading-relaxed">
          You must be logged in with the official RedBus Administrator account to access operator verifications, Primo certifications, and platform governance.
        </p>
        <div className="mt-6 p-4 bg-slate-50 border border-slate-200 rounded-2xl text-left text-xs space-y-1.5 font-mono">
          <p className="text-[11px] font-bold text-slate-700">Default Admin Credentials:</p>
          <p className="text-slate-600">
            Email: <span className="font-bold text-gray-900">redbus@admin.in</span>
          </p>
          <p className="text-slate-600">
            Password: <span className="font-bold text-gray-900">123456</span>
          </p>
        </div>
        <Link
          href="/"
          className="mt-6 inline-flex items-center gap-1.5 px-6 py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold shadow-md transition-colors"
        >
          <span>Sign In as Admin</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    );
  }

  const pendingOperators = operators.filter((op) => op.status === "PENDING");
  const approvedOperators = operators.filter((op) => op.status === "APPROVED");
  const primoOperators = operators.filter((op) => Boolean(op.primoCertified));

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-gradient-to-r from-slate-950 via-slate-900 to-slate-950 p-6 sm:p-8 rounded-3xl text-white shadow-xl relative overflow-hidden border border-slate-800">
        <div className="space-y-1 z-10">
          <div className="inline-flex items-center space-x-1.5 px-3 py-1 bg-red-500/20 border border-red-500/30 text-red-300 rounded-full text-xs font-bold uppercase tracking-wider mb-1">
            <Shield className="w-3.5 h-3.5 text-red-400" />
            <span>Master Administration & Governance Console</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
            RedBus Enterprise Control Center
          </h1>
          <p className="text-xs text-slate-400 max-w-xl">
            Logged in as <span className="text-white font-semibold">{user?.email || "Administrator"}</span> • Operator Governance, Primo Bus Certification & Financial Settlement
          </p>
        </div>

        <div className="flex items-center space-x-3 z-10">
          {pendingOperators.length > 0 && (
            <span className="px-3 py-1.5 bg-amber-500/20 border border-amber-500/40 text-amber-300 text-xs font-bold rounded-xl flex items-center gap-1.5">
              <AlertCircle className="w-4 h-4" />
              <span>{pendingOperators.length} Pending Approval</span>
            </span>
          )}
          <button
            onClick={() => {
              refetchStats();
              refetchOperators();
              refetchPrimoBuses();
              refetchBookings();
              refetchEarnings();
            }}
            className="p-2.5 bg-white/10 hover:bg-white/20 border border-white/10 rounded-xl text-white transition-colors cursor-pointer"
            title="Refresh console"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Action Alerts */}
      {actionSuccess && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center space-x-3 text-xs text-emerald-800 animate-in fade-in">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          <span className="font-semibold">{actionSuccess}</span>
        </div>
      )}

      {actionError && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-2xl flex items-center space-x-3 text-xs text-red-800 animate-in fade-in">
          <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          <span className="font-semibold">{actionError}</span>
        </div>
      )}

      {/* Metrics Overview Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="bg-white dark:bg-slate-900 p-5 rounded-3xl border border-gray-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 dark:text-slate-400 uppercase tracking-wider">Gross Revenue</span>
            <div className="p-2 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 rounded-xl">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 dark:text-white mt-2">
            ₹{stats ? Number(stats.totalRevenue || 0).toLocaleString("en-IN") : "0"}
          </p>
          <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-semibold flex items-center mt-1">
            <TrendingUp className="w-3 h-3 mr-1" /> Live platform earnings
          </span>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-3xl border border-gray-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 dark:text-slate-400 uppercase tracking-wider">Primo Certified Fleet</span>
            <div className="p-2 bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 rounded-xl">
              <Award className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 dark:text-white mt-2">
            {primoOperators.length} Operators
          </p>
          <span className="text-[11px] text-amber-600 dark:text-amber-400 font-semibold mt-1 block">
            {primoBuses.length || stats?.primoBusesCount || 0} Primo Buses • 0% Commission
          </span>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-3xl border border-gray-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 dark:text-slate-400 uppercase tracking-wider">Active Bus Fleet</span>
            <div className="p-2 bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 rounded-xl">
              <Bus className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 dark:text-white mt-2">
            {stats ? stats.totalBuses : primoBuses.length} Coaches
          </p>
          <span className="text-[11px] text-indigo-600 dark:text-indigo-400 font-semibold mt-1 block">
            {stats?.activeSchedulesCount ?? stats?.totalRoutes ?? 0} Schedules • {stats?.occupancyRate ?? 0}% Occupancy
          </span>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-3xl border border-gray-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 dark:text-slate-400 uppercase tracking-wider">Fleet Operators</span>
            <div className="p-2 bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 rounded-xl">
              <Building2 className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 dark:text-white mt-2">{operators.length}</p>
          <span className="text-[11px] text-blue-600 dark:text-blue-400 font-semibold mt-1 block">
            {approvedOperators.length} Active • {pendingOperators.length} Pending
          </span>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-3xl border border-gray-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 dark:text-slate-400 uppercase tracking-wider">Bookings</span>
            <div className="p-2 bg-red-50 dark:bg-red-950/60 text-[#d84e55] rounded-xl">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 dark:text-white mt-2">{stats ? stats.totalBookings : 0}</p>
          <span className="text-[11px] text-[#d84e55] font-semibold mt-1 block">
            {stats ? stats.confirmedBookings : 0} Confirmed
          </span>
        </div>
      </div>

      {/* Tabs Selector */}
      <div className="flex border-b border-gray-200 dark:border-slate-800 space-x-2 sm:space-x-4 overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab("operators")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "operators"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
          }`}
        >
          <Building2 className="w-4 h-4" />
          <span>Operators & Governance ({operators.length})</span>
          {pendingOperators.length > 0 && (
            <span className="px-1.5 py-0.5 bg-amber-500 text-white rounded-full text-[10px]">
              {pendingOperators.length}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveTab("primo_operators")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "primo_operators"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
          }`}
        >
          <Award className="w-4 h-4 text-amber-500" />
          <span>Primo Bus Operators ({primoOperators.length})</span>
          <span className="px-1.5 py-0.5 bg-amber-100 dark:bg-amber-950/80 text-amber-800 dark:text-amber-300 rounded-md text-[10px] font-black">
            0% COMMISSION
          </span>
        </button>

        <button
          onClick={() => setActiveTab("earnings")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "earnings"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
          }`}
        >
          <Wallet className="w-4 h-4 text-emerald-600" />
          <span>Operator Earnings & Wallets</span>
          <span className="px-1.5 py-0.5 bg-emerald-100 dark:bg-emerald-950/80 text-emerald-800 dark:text-emerald-300 rounded-md text-[10px] font-black">
            FINANCIALS
          </span>
        </button>

        <button
          onClick={() => setActiveTab("banners")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "banners"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
          }`}
        >
          <ImageIcon className="w-4 h-4 text-rose-500" />
          <span>Promotional Banners & AI Studio</span>
          <span className="px-1.5 py-0.5 bg-rose-100 dark:bg-rose-950/80 text-rose-800 dark:text-rose-300 rounded-md text-[10px] font-black">
            {adminBanners.length} LIVE
          </span>
        </button>

        <button
          onClick={() => setActiveTab("analytics")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "analytics"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
          }`}
        >
          <TrendingUp className="w-4 h-4" />
          <span>System Analytics & Corridors</span>
        </button>

        <button
          onClick={() => setActiveTab("bookings")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "bookings"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
          }`}
        >
          <Clock className="w-4 h-4" />
          <span>Master Bookings</span>
        </button>
      </div>

      {/* TAB 1: OPERATOR MANAGEMENT */}
      {activeTab === "operators" && (
        <div className="space-y-6 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-gray-200 dark:border-slate-800 shadow-xs overflow-hidden">
            <div className="p-6 border-b border-gray-100 dark:border-slate-800 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-base text-gray-900 dark:text-white">Registered Bus Operators</h3>
                <p className="text-xs text-gray-500 dark:text-slate-400">
                  Verify transport companies, certify Primo Bus Operators (0% commission-free), and govern fleet operations
                </p>
              </div>
              <span className="text-xs font-bold text-gray-500 dark:text-slate-400">{operators.length} Operators Registered</span>
            </div>

            {isOperatorsLoading ? (
              <div className="py-12 text-center text-xs text-gray-400">Loading operators...</div>
            ) : operators.length === 0 ? (
              <div className="py-12 text-center text-xs text-gray-400">
                No operators registered in the platform yet.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-gray-50 dark:bg-slate-800/80 text-gray-500 dark:text-slate-400 uppercase font-bold border-b border-gray-100 dark:border-slate-800 text-[10px] tracking-wider">
                    <tr>
                      <th className="px-6 py-3.5">Operator / Company</th>
                      <th className="px-4 py-3.5">Contact & Email</th>
                      <th className="px-4 py-3.5">Fleet & Schedules</th>
                      <th className="px-4 py-3.5">Total Revenue</th>
                      <th className="px-4 py-3.5">Commission</th>
                      <th className="px-4 py-3.5">Primo Operator</th>
                      <th className="px-4 py-3.5">Status</th>
                      <th className="px-6 py-3.5 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 dark:divide-slate-800 text-gray-700 dark:text-slate-200">
                    {operators.map((op) => {
                      const isPrimo = Boolean(op.primoCertified);
                      return (
                        <tr key={op.id} className="hover:bg-gray-50/80 dark:hover:bg-slate-800/60 transition-colors">
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-gray-900 dark:text-white text-sm">{op.companyName}</span>
                              {isPrimo && (
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-gradient-to-r from-[#1e2d5c] to-[#131d3d] text-amber-300 text-[10px] font-black border border-amber-400/40">
                                  <Star className="w-2.5 h-2.5 fill-amber-400 text-amber-400" />
                                  Primo
                                </span>
                              )}
                            </div>
                            <span className="text-[10px] text-gray-400 font-mono">ID: #{op.id}</span>
                          </td>

                          <td className="px-4 py-4 space-y-0.5">
                            <div className="font-medium text-gray-800 dark:text-slate-200">{op.contactPerson}</div>
                            <div className="text-gray-500 dark:text-slate-400 flex items-center gap-1 text-[11px]">
                              <Mail className="w-3 h-3 text-gray-400" /> {op.email}
                            </div>
                            <div className="text-gray-500 dark:text-slate-400 flex items-center gap-1 text-[11px]">
                              <Phone className="w-3 h-3 text-gray-400" /> {op.phone}
                            </div>
                          </td>

                          <td className="px-4 py-4">
                            <div className="font-bold text-gray-900 dark:text-white">{op.totalBuses || 0} Buses</div>
                            <div className="text-[11px] text-gray-500 dark:text-slate-400">{op.totalSchedules || 0} Schedules</div>
                            <div className="text-[10px] text-purple-600 dark:text-purple-400 font-semibold">{op.totalBookings || 0} Bookings</div>
                          </td>

                          <td className="px-4 py-4">
                            <span className="font-black text-emerald-700 dark:text-emerald-400 text-sm">
                              ₹{Number(op.totalRevenue || 0).toLocaleString("en-IN")}
                            </span>
                          </td>

                          <td className="px-4 py-4">
                            {isPrimo ? (
                              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800/50 text-[11px] font-black">
                                0% (Primo Free)
                              </span>
                            ) : editingCommissionId === op.id ? (
                              <div className="flex items-center gap-1">
                                <input
                                  type="number"
                                  min={0}
                                  max={50}
                                  value={commissionInput}
                                  onChange={(e) => setCommissionInput(Number(e.target.value))}
                                  className="w-14 px-2 py-1 bg-gray-50 border border-gray-300 rounded-lg text-xs font-bold"
                                />
                                <button
                                  onClick={() => handleSaveCommission(op.id)}
                                  disabled={isUpdatingCommission}
                                  className="px-2 py-1 bg-emerald-600 text-white rounded-lg text-[10px] font-bold cursor-pointer"
                                >
                                  Save
                                </button>
                                <button
                                  onClick={() => setEditingCommissionId(null)}
                                  className="px-2 py-1 bg-gray-200 text-gray-600 rounded-lg text-[10px] cursor-pointer"
                                >
                                  ✕
                                </button>
                              </div>
                            ) : (
                              <div className="flex items-center gap-1.5">
                                <span className="font-bold text-gray-900 dark:text-white">{op.commissionRate}%</span>
                                <button
                                  onClick={() => {
                                    setEditingCommissionId(op.id);
                                    setCommissionInput(op.commissionRate);
                                  }}
                                  className="text-[10px] text-blue-600 dark:text-blue-400 hover:underline cursor-pointer"
                                >
                                  Edit
                                </button>
                              </div>
                            )}
                          </td>

                          <td className="px-4 py-4">
                            <div className="inline-flex items-center rounded-xl p-0.5 bg-gray-100 dark:bg-slate-800 border border-gray-200 dark:border-slate-700">
                              <button
                                type="button"
                                disabled={isTogglingPrimo}
                                onClick={() => !isPrimo && handleTogglePrimo(op.id, true, op.companyName)}
                                className={`px-2.5 py-1 rounded-lg text-[11px] font-black transition-all cursor-pointer ${
                                  isPrimo
                                    ? "bg-gradient-to-r from-[#1e2d5c] to-[#131d3d] text-amber-300 shadow-xs"
                                    : "text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
                                }`}
                              >
                                Yes
                              </button>
                              <button
                                type="button"
                                disabled={isTogglingPrimo}
                                onClick={() => isPrimo && handleTogglePrimo(op.id, false, op.companyName)}
                                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all cursor-pointer ${
                                  !isPrimo
                                    ? "bg-white dark:bg-slate-700 text-gray-800 dark:text-white shadow-xs"
                                    : "text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white"
                                }`}
                              >
                                No
                              </button>
                            </div>
                          </td>

                          <td className="px-4 py-4">
                            {op.status === "APPROVED" ? (
                              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-full">
                                <CheckCircle2 className="w-3.5 h-3.5" /> Approved
                              </span>
                            ) : op.status === "PENDING" ? (
                              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-800 bg-amber-100 px-2.5 py-1 rounded-full animate-pulse">
                                <Clock className="w-3.5 h-3.5" /> Pending Approval
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-red-700 bg-red-100 px-2.5 py-1 rounded-full">
                                <XCircle className="w-3.5 h-3.5" /> Suspended
                              </span>
                            )}
                          </td>

                          <td className="px-6 py-4 text-right space-x-2">
                            {op.status !== "APPROVED" && (
                              <button
                                onClick={() => handleVerifyOperator(op.id, op.companyName)}
                                disabled={isVerifying}
                                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs shadow-xs transition-colors cursor-pointer"
                              >
                                Approve & Verify
                              </button>
                            )}
                            {op.status === "APPROVED" && (
                              <button
                                onClick={() => handleSuspendOperator(op.id, op.companyName)}
                                disabled={isSuspending}
                                className="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-700 font-bold rounded-xl text-xs transition-colors cursor-pointer"
                              >
                                Suspend
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB: PRIMO CERTIFIED BUS OPERATORS & COMMISSION-FREE FLEET */}
      {activeTab === "primo_operators" && (
        <div className="space-y-6 animate-in fade-in">
          {/* Primo Overview Banner */}
          <div className="bg-gradient-to-r from-[#131d3d] via-[#1c2b59] to-[#131d3d] p-6 sm:p-8 rounded-3xl border border-amber-400/30 text-white shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1.5">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-400/20 border border-amber-400/40 text-amber-300 text-xs font-black uppercase tracking-wider">
                <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                <span>Primo Certified • On Time Guarantee</span>
              </div>
              <h2 className="text-xl sm:text-2xl font-black text-white">
                Primo Certified Bus Operators & Commission-Free Fleet
              </h2>
              <p className="text-xs text-slate-300 max-w-2xl leading-relaxed">
                Operators certified as <span className="text-amber-300 font-bold">Primo Bus Operators</span> by the Administrator enjoy{" "}
                <span className="text-emerald-300 font-bold">0% Platform Commission (100% Net Fare Settlement)</span> in recognition of their top passenger ratings, punctuality, and safety standards.
              </p>
            </div>
            <div className="flex items-center gap-3 shrink-0">
              <div className="px-4 py-3 rounded-2xl bg-white/10 border border-white/15 text-center">
                <p className="text-[10px] uppercase font-bold text-amber-300">Certified Operators</p>
                <p className="text-2xl font-black text-white">{primoOperators.length}</p>
              </div>
              <div className="px-4 py-3 rounded-2xl bg-white/10 border border-white/15 text-center">
                <p className="text-[10px] uppercase font-bold text-emerald-300">Primo Buses</p>
                <p className="text-2xl font-black text-white">{primoBuses.length}</p>
              </div>
            </div>
          </div>

          {/* Primo Certified Operators Table */}
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-gray-200 dark:border-slate-800 shadow-xs overflow-hidden">
            <div className="p-6 border-b border-gray-100 dark:border-slate-800 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-base text-gray-900 dark:text-white flex items-center gap-2">
                  <Award className="w-4 h-4 text-amber-500" />
                  <span>Approved Primo Bus Operators ({primoOperators.length})</span>
                </h3>
                <p className="text-xs text-gray-500 dark:text-slate-400">
                  Operators toggled to &ldquo;Yes&rdquo; in Operators & Governance appear here with 0% commission privilege
                </p>
              </div>
              <button
                type="button"
                onClick={() => setActiveTab("operators")}
                className="px-3.5 py-2 rounded-xl bg-amber-50 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-800/50 text-xs font-bold cursor-pointer hover:bg-amber-100 transition-colors"
              >
                Manage Primo Toggles →
              </button>
            </div>

            {primoOperators.length === 0 ? (
              <div className="py-12 text-center space-y-2">
                <Award className="w-8 h-8 text-amber-400 mx-auto" />
                <p className="text-xs font-bold text-gray-700 dark:text-slate-200">No Primo Bus Operators Certified Yet</p>
                <p className="text-[11px] text-gray-400 dark:text-slate-400">
                  Go to the &ldquo;Operators & Governance&rdquo; tab and toggle &ldquo;Primo Operator&rdquo; to Yes for top-rated operators.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-gray-50 dark:bg-slate-800/80 text-gray-500 dark:text-slate-400 uppercase font-bold border-b border-gray-100 dark:border-slate-800 text-[10px] tracking-wider">
                    <tr>
                      <th className="px-6 py-3.5">Primo Operator</th>
                      <th className="px-4 py-3.5">Contact & Email</th>
                      <th className="px-4 py-3.5">Fleet & Schedules</th>
                      <th className="px-4 py-3.5">Gross Revenue</th>
                      <th className="px-4 py-3.5">Commission Benefit</th>
                      <th className="px-4 py-3.5">Certification</th>
                      <th className="px-6 py-3.5 text-right">Primo Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 dark:divide-slate-800 text-gray-700 dark:text-slate-200">
                    {primoOperators.map((op) => (
                      <tr key={op.id} className="hover:bg-gray-50/80 dark:hover:bg-slate-800/60 transition-colors">
                        <td className="px-6 py-4">
                          <div className="font-bold text-gray-900 dark:text-white text-sm">{op.companyName}</div>
                          <span className="text-[10px] text-gray-400 font-mono">ID: #{op.id}</span>
                        </td>
                        <td className="px-4 py-4 space-y-0.5">
                          <div className="font-medium text-gray-800 dark:text-slate-200">{op.contactPerson}</div>
                          <div className="text-gray-500 dark:text-slate-400 text-[11px]">{op.email}</div>
                          <div className="text-gray-500 dark:text-slate-400 text-[11px]">{op.phone}</div>
                        </td>
                        <td className="px-4 py-4">
                          <div className="font-bold text-gray-900 dark:text-white">{op.totalBuses || 0} Buses</div>
                          <div className="text-[11px] text-gray-500 dark:text-slate-400">{op.totalSchedules || 0} Schedules</div>
                        </td>
                        <td className="px-4 py-4 font-black text-emerald-700 dark:text-emerald-400 text-sm">
                          ₹{Number(op.totalRevenue || 0).toLocaleString("en-IN")}
                        </td>
                        <td className="px-4 py-4">
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 text-[11px] font-black">
                            0% Commission Free
                          </span>
                        </td>
                        <td className="px-4 py-4">
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-gradient-to-r from-[#1e2d5c] to-[#131d3d] text-amber-300 text-[11px] font-black border border-amber-400/40">
                            <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                            <span>Primo • On Time Guarantee</span>
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <button
                            type="button"
                            disabled={isTogglingPrimo}
                            onClick={() => handleTogglePrimo(op.id, false, op.companyName)}
                            className="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-700 font-bold rounded-xl text-xs transition-colors cursor-pointer"
                          >
                            Remove Primo
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Certified Primo Buses Fleet Table */}
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-gray-200 dark:border-slate-800 shadow-xs overflow-hidden">
            <div className="p-6 border-b border-gray-100 dark:border-slate-800 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-base text-gray-900 dark:text-white flex items-center gap-2">
                  <Bus className="w-4 h-4 text-[#d84e55]" />
                  <span>Certified Primo Buses Fleet ({primoBuses.length})</span>
                </h3>
                <p className="text-xs text-gray-500 dark:text-slate-400">
                  All active coaches operated by Primo Certified partners carrying the Primo On-Time Guarantee badge in passenger search
                </p>
              </div>
            </div>

            {isPrimoBusesLoading ? (
              <div className="py-12 text-center text-xs text-gray-400">Loading Primo certified buses...</div>
            ) : primoBuses.length === 0 ? (
              <div className="py-12 text-center text-xs text-gray-400">
                No buses currently under Primo certification. Certify an operator above to include their fleet.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-gray-50 dark:bg-slate-800/80 text-gray-500 dark:text-slate-400 uppercase font-bold border-b border-gray-100 dark:border-slate-800 text-[10px] tracking-wider">
                    <tr>
                      <th className="px-6 py-3.5">Bus Number</th>
                      <th className="px-4 py-3.5">Primo Operator</th>
                      <th className="px-4 py-3.5">Coach Type</th>
                      <th className="px-4 py-3.5">Capacity</th>
                      <th className="px-4 py-3.5">Rating</th>
                      <th className="px-4 py-3.5">Commission Tier</th>
                      <th className="px-6 py-3.5 text-right">Badge</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 dark:divide-slate-800 text-gray-700 dark:text-slate-200">
                    {primoBuses.map((bus) => (
                      <tr key={bus.id} className="hover:bg-gray-50/80 dark:hover:bg-slate-800/60 transition-colors">
                        <td className="px-6 py-4">
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-[#ffdf5d] text-slate-950 font-mono font-black text-xs border border-amber-500/60 shadow-2xs">
                            <span>•</span>
                            <span>{bus.registrationNumber}</span>
                            <span>•</span>
                          </span>
                        </td>
                        <td className="px-4 py-4 font-bold text-gray-900 dark:text-white">
                          {bus.operatorName}
                        </td>
                        <td className="px-4 py-4 text-gray-600 dark:text-slate-300 font-medium">
                          {bus.busType}
                        </td>
                        <td className="px-4 py-4 font-bold text-gray-800 dark:text-slate-200">
                          {bus.totalSeats} Seats
                        </td>
                        <td className="px-4 py-4">
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-600 text-white font-bold text-[11px]">
                            ★ {Number(bus.rating || 4.5).toFixed(1)}
                          </span>
                        </td>
                        <td className="px-4 py-4">
                          <span className="text-emerald-700 dark:text-emerald-400 font-black text-[11px]">
                            0% Commission-Free
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-[#1e2d5c] text-amber-300 text-[10px] font-black">
                            ★ PRIMO CERTIFIED
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB: OPERATOR FINANCIALS & EARNINGS ANALYTICS */}
      {activeTab === "earnings" && (
        <div className="space-y-6 animate-in fade-in">
          {/* Earnings Financial KPI Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold uppercase text-gray-400">Total Gross Bookings</span>
                <div className="p-2 bg-blue-50 text-blue-600 rounded-xl">
                  <CreditCard className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-black text-gray-900">
                ₹{operatorEarnings ? Number(operatorEarnings.systemGrossRevenue || 0).toLocaleString("en-IN") : "0"}
              </p>
              <p className="text-[11px] text-gray-500 font-medium">100% Passenger Fares</p>
            </div>

            <div className="bg-white p-5 rounded-3xl border border-emerald-200 bg-emerald-50/30 shadow-xs space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold uppercase text-emerald-700">Platform Commission (10%)</span>
                <div className="p-2 bg-emerald-100 text-emerald-700 rounded-xl">
                  <Percent className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-black text-emerald-800">
                ₹{operatorEarnings ? Number(operatorEarnings.systemCommissionsCollected || 0).toLocaleString("en-IN") : "0"}
              </p>
              <p className="text-[11px] text-emerald-700 font-semibold">10% redBus Platform Revenue</p>
            </div>

            <div className="bg-white p-5 rounded-3xl border border-indigo-200 bg-indigo-50/20 shadow-xs space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold uppercase text-indigo-700">Operator Net Payouts (90%)</span>
                <div className="p-2 bg-indigo-100 text-indigo-700 rounded-xl">
                  <TrendingUp className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-black text-indigo-900">
                ₹{operatorEarnings ? Number(operatorEarnings.systemNetOperatorPayouts || 0).toLocaleString("en-IN") : "0"}
              </p>
              <p className="text-[11px] text-indigo-600 font-medium">Net Credited to Operators</p>
            </div>

            <div className="bg-white p-5 rounded-3xl border border-rose-200 bg-rose-50/20 shadow-xs space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold uppercase text-rose-700">Audited Refunds Processed</span>
                <div className="p-2 bg-rose-100 text-rose-700 rounded-xl">
                  <ArrowDownRight className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-black text-rose-700">
                ₹{operatorEarnings ? Number(operatorEarnings.systemTotalRefundsProcessed || 0).toLocaleString("en-IN") : "0"}
              </p>
              <p className="text-[11px] text-rose-600 font-medium">Total Refund Disbursements</p>
            </div>
          </div>

          {/* Breakdown by Operator Table */}
          <div className="bg-white rounded-3xl border border-gray-200 shadow-xs overflow-hidden">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
                  <Wallet className="w-4 h-4 text-emerald-600" />
                  <span>Operator Earnings & Wallet Ledger Analytics</span>
                </h3>
                <p className="text-xs text-gray-500">
                  Real-time 90/10 financial split, passenger refund debits, and live redBus operator wallet balances
                </p>
              </div>
              <button
                onClick={() => refetchEarnings()}
                className="p-2 hover:bg-gray-100 rounded-xl text-gray-500 text-xs font-bold flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Refresh Ledger</span>
              </button>
            </div>

            {isEarningsLoading ? (
              <div className="py-12 text-center text-xs text-gray-400">Loading operator earnings analytics...</div>
            ) : !operatorEarnings || !operatorEarnings.operatorEarnings || operatorEarnings.operatorEarnings.length === 0 ? (
              <div className="py-12 text-center text-xs text-gray-400">No operator earnings data available.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-gray-50 text-gray-500 uppercase font-bold border-b border-gray-100 text-[10px] tracking-wider">
                    <tr>
                      <th className="px-6 py-3.5">Operator</th>
                      <th className="px-4 py-3.5">Bookings</th>
                      <th className="px-4 py-3.5">Gross Revenue</th>
                      <th className="px-4 py-3.5">Platform Cut (10%)</th>
                      <th className="px-4 py-3.5">Net Earnings (90%)</th>
                      <th className="px-4 py-3.5">Refunds Debited</th>
                      <th className="px-4 py-3.5">Operator Wallet Balance</th>
                      <th className="px-6 py-3.5 text-right">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 text-gray-700">
                    {operatorEarnings.operatorEarnings.map((op) => (
                      <tr key={op.operatorId} className="hover:bg-gray-50/80 transition-colors">
                        <td className="px-6 py-4">
                          <div className="font-bold text-gray-900 text-sm">{op.companyName}</div>
                          <div className="text-[11px] text-gray-400">{op.email}</div>
                          <span className="text-[10px] text-gray-400 font-mono">ID: #{op.operatorId}</span>
                        </td>

                        <td className="px-4 py-4">
                          <span className="font-bold text-gray-800">{op.totalConfirmedBookings} Bookings</span>
                        </td>

                        <td className="px-4 py-4">
                          <span className="font-semibold text-gray-900">
                            ₹{Number(op.grossRevenue || 0).toLocaleString("en-IN")}
                          </span>
                        </td>

                        <td className="px-4 py-4">
                          <span className="font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md text-[11px]">
                            ₹{Number(op.commissionPaid || 0).toLocaleString("en-IN")}
                          </span>
                        </td>

                        <td className="px-4 py-4">
                          <span className="font-black text-indigo-700">
                            ₹{Number(op.netEarnings || 0).toLocaleString("en-IN")}
                          </span>
                        </td>

                        <td className="px-4 py-4">
                          <span className="font-bold text-rose-600">
                            ₹{Number(op.totalRefundsApproved || 0).toLocaleString("en-IN")}
                          </span>
                        </td>

                        <td className="px-4 py-4">
                          <div className="flex items-center space-x-1.5">
                            <Wallet className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                            <span className="font-black text-amber-900 text-sm">
                              ₹{Number(op.walletBalance || 0).toLocaleString("en-IN")}
                            </span>
                          </div>
                        </td>

                        <td className="px-6 py-4 text-right">
                          <span
                            className={`inline-flex items-center gap-1 text-[10px] font-bold px-2.5 py-1 rounded-full ${
                              op.status === "APPROVED"
                                ? "text-emerald-700 bg-emerald-100"
                                : op.status === "PENDING"
                                ? "text-amber-800 bg-amber-100"
                                : "text-red-700 bg-red-100"
                            }`}
                          >
                            {op.status === "APPROVED" ? (
                              <>
                                <CheckCircle2 className="w-3 h-3" /> Active
                              </>
                            ) : (
                              op.status
                            )}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 3: PROMOTIONAL BANNERS & AI PROMOTIONAL STUDIO */}
      {activeTab === "banners" && (
        <div className="space-y-8 animate-in fade-in">
          {/* AI Banner Studio Header */}
          <div className="bg-gradient-to-r from-purple-950 via-slate-900 to-red-950 p-6 sm:p-8 rounded-3xl border border-purple-800/40 text-white shadow-xl relative overflow-hidden">
            <div className="max-w-2xl space-y-2 relative z-10">
              <div className="inline-flex items-center space-x-1.5 px-3 py-1 bg-purple-500/20 border border-purple-500/30 text-purple-300 rounded-full text-xs font-bold uppercase tracking-wider">
                <Sparkles className="w-3.5 h-3.5 text-purple-400" />
                <span>AI Promotional Studio & Campaign Engine</span>
              </div>
              <h2 className="text-xl sm:text-2xl font-black text-white">
                Generate High-Converting Promotional Banners with AI
              </h2>
              <p className="text-xs text-slate-300 leading-relaxed">
                Provide a suggestive prompt describing your festival offer, route discount, or luxury fleet promotion.
                Our AI NLP engine will generate optimized headlines, promo codes, background styling, and publish directly to the customer homepage.
              </p>
            </div>

            {/* Prompt Input Form */}
            <div className="mt-6 bg-white/10 backdrop-blur-md p-4 sm:p-5 rounded-2xl border border-white/15 space-y-4 relative z-10">
              <div>
                <label className="block text-xs font-bold text-purple-200 mb-1.5">
                  Suggestive AI Prompt / Campaign Goal
                </label>
                <div className="flex flex-col sm:flex-row gap-2">
                  <div className="relative flex-1">
                    <Wand2 className="w-4 h-4 text-purple-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={aiPrompt}
                      onChange={(e) => setAiPrompt(e.target.value)}
                      placeholder="e.g. Diwali festive 30% discount on Bangalore to Goa luxury sleeper coaches"
                      className="w-full pl-10 pr-4 py-2.5 bg-black/40 border border-white/20 rounded-xl text-xs text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={handleGenerateAiBanner}
                    disabled={isGeneratingAiBanner || !aiPrompt.trim()}
                    className="px-5 py-2.5 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white rounded-xl text-xs font-bold shadow-lg shadow-purple-600/30 flex items-center justify-center gap-2 disabled:opacity-50 transition-all cursor-pointer shrink-0"
                  >
                    {isGeneratingAiBanner ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin" />
                        <span>Generating AI Banner...</span>
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-4 h-4" />
                        <span>Generate with AI</span>
                      </>
                    )}
                  </button>
                </div>
              </div>

              {/* Suggestive Prompt Chips */}
              <div>
                <span className="text-[10px] font-bold text-slate-300 uppercase tracking-wider block mb-2">
                  Click to try suggestive campaign prompts:
                </span>
                <div className="flex flex-wrap gap-2">
                  {[
                    { label: "🪔 Diwali Utsav 30% Off", prompt: "Diwali festival special flat 30% discount for Bangalore to Hyderabad luxury Volvo buses" },
                    { label: "🏖️ Goa Beach Getaway 20%", prompt: "Weekend Beach Getaway 20% discount on Mumbai to Goa AC Sleeper buses" },
                    { label: "🛡️ SafeTravel+ Solo Women 15%", prompt: "SafeTravel+ AI smart seating for solo women with 15% discount on Chennai to Coimbatore routes" },
                    { label: "⚡ Midnight Flash Sale 25%", prompt: "Midnight Flash Sale flat 25% instant discount on Delhi to Manali express coaches" },
                    { label: "👑 Volvo 9600 Luxury Sleeper", prompt: "Ultra-luxury Volvo 9600 Multi-Axle sleeper coaches with memory foam berths and 20% discount" },
                  ].map((chip, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => {
                        setAiPrompt(chip.prompt);
                      }}
                      className="px-2.5 py-1 bg-white/10 hover:bg-white/20 border border-white/15 rounded-lg text-[11px] text-slate-200 transition-colors cursor-pointer text-left"
                    >
                      {chip.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Live Preview & Customization Editor */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left: Interactive Preview */}
            <div className="lg:col-span-6 space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-gray-900 flex items-center gap-1.5">
                  <Eye className="w-4 h-4 text-[#d84e55]" />
                  <span>Live Homepage Banner Preview</span>
                </h3>
                <span className="text-[11px] text-gray-500 font-semibold">Real-Time Mobile & Desktop Look</span>
              </div>

              {/* Live Render Card */}
              <div className="relative rounded-2xl sm:rounded-3xl overflow-hidden shadow-xl border border-slate-800 bg-slate-950 text-white min-h-[220px] p-5 sm:p-7 flex flex-col justify-between">
                {/* Background Image */}
                <div className="absolute inset-0 z-0">
                  <img
                    src={bannerDraft.imageUrl || "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=1200&q=80"}
                    alt="Preview"
                    className="w-full h-full object-cover opacity-25"
                  />
                  <div className={`absolute inset-0 bg-gradient-to-r ${bannerDraft.bgGradient || "from-slate-950 via-red-950/80 to-slate-900"} opacity-95`} />
                </div>

                <div className="relative z-10 space-y-2">
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider border backdrop-blur-md ${bannerDraft.badgeColor || "bg-red-500/20 text-red-300 border-red-500/30"}`}>
                      <Sparkles className="w-3 h-3" />
                      <span>{bannerDraft.tag || "EXCLUSIVE OFFER"}</span>
                    </span>
                    {bannerDraft.routeInfo && (
                      <span className="px-2 py-0.5 bg-white/10 rounded-full text-[10px] font-semibold text-slate-300">
                        {bannerDraft.routeInfo}
                      </span>
                    )}
                    {bannerDraft.isAiGenerated && (
                      <span className="px-2 py-0.5 bg-purple-500/20 border border-purple-500/30 rounded-md text-[9px] font-bold text-purple-300">
                        AI Generated
                      </span>
                    )}
                  </div>

                  <h4 className="text-lg sm:text-2xl font-black text-white leading-tight drop-shadow-md">
                    {bannerDraft.title || "Your Banner Headline Will Appear Here"}
                  </h4>

                  <p className="text-xs text-slate-300 leading-relaxed max-w-md line-clamp-2">
                    {bannerDraft.subtitle || "Your promotional description, bus features, and discount perks will appear here."}
                  </p>
                </div>

                <div className="relative z-10 flex items-center gap-3 pt-4">
                  <span className="inline-flex items-center gap-1 px-4 py-2 bg-[#d84e55] text-white rounded-xl text-xs font-bold shadow-md shadow-red-600/30">
                    <span>{bannerDraft.ctaText || "Claim Offer"}</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </span>
                  {bannerDraft.promoCode && (
                    <div className="flex items-center gap-1 px-3 py-1.5 bg-white/10 backdrop-blur-md rounded-xl border border-white/15 text-xs font-black text-amber-300">
                      <Tag className="w-3 h-3 text-amber-400" />
                      <span>{bannerDraft.promoCode}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Right: Customization & Publish Form */}
            <div className="lg:col-span-6 bg-white p-6 rounded-3xl border border-gray-200 shadow-xs space-y-4">
              <div className="border-b border-gray-100 pb-3">
                <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
                  <Plus className="w-4 h-4 text-emerald-600" />
                  <span>Customize & Publish Banner to Homepage</span>
                </h3>
                <p className="text-xs text-gray-500">
                  Refine the title, promo code, background theme, and instantly publish live.
                </p>
              </div>

              <form onSubmit={handlePublishBanner} className="space-y-4">
                <div className="space-y-1.5">
                  <label className="block text-xs font-bold text-gray-700">Banner Title *</label>
                  <input
                    type="text"
                    required
                    value={bannerDraft.title}
                    onChange={(e) => setBannerDraft({ ...bannerDraft, title: e.target.value })}
                    placeholder="e.g. Diwali Utsav: Flat 30% Off All Routes"
                    className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-[#d84e55] focus:outline-none"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-xs font-bold text-gray-700">Subtitle / Offer Description</label>
                  <textarea
                    rows={2}
                    value={bannerDraft.subtitle}
                    onChange={(e) => setBannerDraft({ ...bannerDraft, subtitle: e.target.value })}
                    placeholder="e.g. Book luxury AC sleeper coaches and celebrate homecoming with family."
                    className="w-full px-3.5 py-2 border border-gray-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-[#d84e55] focus:outline-none"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="space-y-1">
                    <label className="block text-[11px] font-bold text-gray-700">Tag / Badge</label>
                    <input
                      type="text"
                      value={bannerDraft.tag}
                      onChange={(e) => setBannerDraft({ ...bannerDraft, tag: e.target.value })}
                      placeholder="FESTIVE DEAL"
                      className="w-full px-3 py-1.5 border border-gray-300 rounded-xl text-xs"
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="block text-[11px] font-bold text-gray-700">Promo Code</label>
                    <input
                      type="text"
                      value={bannerDraft.promoCode}
                      onChange={(e) => setBannerDraft({ ...bannerDraft, promoCode: e.target.value.toUpperCase() })}
                      placeholder="DIWALI30"
                      className="w-full px-3 py-1.5 border border-gray-300 rounded-xl text-xs font-mono font-bold text-red-600"
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="block text-[11px] font-bold text-gray-700">Route Info</label>
                    <input
                      type="text"
                      value={bannerDraft.routeInfo}
                      onChange={(e) => setBannerDraft({ ...bannerDraft, routeInfo: e.target.value })}
                      placeholder="Bangalore ⇄ Goa"
                      className="w-full px-3 py-1.5 border border-gray-300 rounded-xl text-xs"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <label className="block text-[11px] font-bold text-gray-700">CTA Button Text</label>
                    <input
                      type="text"
                      value={bannerDraft.ctaText}
                      onChange={(e) => setBannerDraft({ ...bannerDraft, ctaText: e.target.value })}
                      placeholder="Claim 30% Off"
                      className="w-full px-3 py-1.5 border border-gray-300 rounded-xl text-xs"
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="block text-[11px] font-bold text-gray-700">CTA Link</label>
                    <input
                      type="text"
                      value={bannerDraft.ctaLink}
                      onChange={(e) => setBannerDraft({ ...bannerDraft, ctaLink: e.target.value })}
                      placeholder="/search"
                      className="w-full px-3 py-1.5 border border-gray-300 rounded-xl text-xs"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="block text-xs font-bold text-gray-700">Background Gradient Style</label>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    {[
                      { name: "Crimson Ember", bg: "from-slate-950 via-red-950/80 to-slate-900", badgeColor: "bg-red-500/20 text-red-300 border-red-500/30" },
                      { name: "Diwali Amber", bg: "from-amber-950/90 via-orange-900/80 to-slate-950", badgeColor: "bg-amber-500/20 text-amber-300 border-amber-500/30" },
                      { name: "Emerald Forest", bg: "from-slate-950 via-emerald-950/80 to-slate-900", badgeColor: "bg-emerald-500/20 text-emerald-300 border-emerald-500/30" },
                      { name: "Cyber Purple", bg: "from-purple-950/90 via-fuchsia-950/80 to-slate-950", badgeColor: "bg-purple-500/20 text-purple-300 border-purple-500/30" },
                    ].map((theme, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => setBannerDraft({ ...bannerDraft, bgGradient: theme.bg, badgeColor: theme.badgeColor })}
                        className={`p-2 rounded-xl text-[11px] font-bold text-white border text-center transition-all cursor-pointer ${
                          bannerDraft.bgGradient === theme.bg
                            ? "ring-2 ring-[#d84e55] border-transparent"
                            : "border-gray-200"
                        } bg-gradient-to-r ${theme.bg}`}
                      >
                        {theme.name}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="block text-xs font-bold text-gray-700">Image URL</label>
                  <input
                    type="text"
                    value={bannerDraft.imageUrl}
                    onChange={(e) => setBannerDraft({ ...bannerDraft, imageUrl: e.target.value })}
                    placeholder="https://images.unsplash.com/photo-..."
                    className="w-full px-3.5 py-1.5 border border-gray-300 rounded-xl text-xs"
                  />
                </div>

                <button
                  type="submit"
                  disabled={isCreatingBanner || !bannerDraft.title.trim()}
                  className="w-full py-3 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold shadow-lg shadow-red-600/30 flex items-center justify-center gap-2 disabled:opacity-50 transition-all cursor-pointer"
                >
                  {isCreatingBanner ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Publishing to Home Page...</span>
                    </>
                  ) : (
                    <>
                      <Plus className="w-4 h-4" />
                      <span>Publish Banner Live to Home Page</span>
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>

          {/* Active Banners Registry Table */}
          <div className="bg-white rounded-3xl border border-gray-200 shadow-xs overflow-hidden">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
                  <ImageIcon className="w-4 h-4 text-[#d84e55]" />
                  <span>Live Homepage Promotional Banners ({adminBanners.length})</span>
                </h3>
                <p className="text-xs text-gray-500">
                  All active campaigns dynamically rotating on the passenger homepage slider
                </p>
              </div>
              <button
                type="button"
                onClick={() => refetchBanners()}
                className="p-2 text-gray-500 hover:text-gray-900 border border-gray-200 rounded-xl cursor-pointer"
                title="Refresh banner list"
              >
                <RefreshCw className="w-4 h-4" />
              </button>
            </div>

            {isBannersLoading ? (
              <div className="p-12 text-center text-xs text-gray-500 flex items-center justify-center gap-2">
                <RefreshCw className="w-4 h-4 animate-spin text-[#d84e55]" />
                <span>Loading active banners...</span>
              </div>
            ) : adminBanners.length === 0 ? (
              <div className="p-12 text-center space-y-2">
                <ImageIcon className="w-8 h-8 text-gray-300 mx-auto" />
                <p className="text-xs font-bold text-gray-600">No promotional banners active yet</p>
                <p className="text-[11px] text-gray-400">Use the AI Studio above to generate and publish your first banner.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-gray-600">
                  <thead className="bg-gray-50 border-b border-gray-100 text-[11px] uppercase font-bold text-gray-500">
                    <tr>
                      <th className="px-6 py-4">Banner / Title</th>
                      <th className="px-4 py-4">Badge / Promo</th>
                      <th className="px-4 py-4">Target Route</th>
                      <th className="px-4 py-4">Type</th>
                      <th className="px-4 py-4">Status</th>
                      <th className="px-6 py-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {adminBanners.map((b) => (
                      <tr key={b.id} className="hover:bg-gray-50/80 transition-colors">
                        <td className="px-6 py-4">
                          <div className="flex items-center space-x-3">
                            <img
                              src={b.imageUrl || "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=100"}
                              alt={b.title}
                              className="w-12 h-10 object-cover rounded-lg border border-gray-200 shrink-0"
                            />
                            <div>
                              <p className="font-bold text-gray-900 line-clamp-1">{b.title}</p>
                              <p className="text-[11px] text-gray-500 line-clamp-1">{b.subtitle || "No description"}</p>
                            </div>
                          </div>
                        </td>

                        <td className="px-4 py-4">
                          <div className="space-y-1">
                            <span className="inline-block px-2 py-0.5 bg-gray-100 rounded text-[10px] font-bold text-gray-700">
                              {b.tag || "OFFER"}
                            </span>
                            {b.promoCode && (
                              <p className="text-[11px] font-mono font-bold text-red-600">{b.promoCode}</p>
                            )}
                          </div>
                        </td>

                        <td className="px-4 py-4 font-semibold text-gray-700">
                          {b.routeInfo || "All India Routes"}
                        </td>

                        <td className="px-4 py-4">
                          {b.isAiGenerated ? (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-purple-100 text-purple-800 rounded-md text-[10px] font-bold">
                              <Sparkles className="w-3 h-3 text-purple-600" />
                              AI Crafted
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 text-slate-700 rounded-md text-[10px] font-bold">
                              Standard
                            </span>
                          )}
                        </td>

                        <td className="px-4 py-4">
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-emerald-100 text-emerald-800 rounded-full text-[10px] font-bold">
                            <CheckCircle2 className="w-3 h-3" />
                            Live on Home
                          </span>
                        </td>

                        <td className="px-6 py-4 text-right">
                          <button
                            type="button"
                            onClick={() => handleDeleteBanner(b.id, b.title)}
                            disabled={isDeletingBanner}
                            className="p-1.5 text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                            title="Delete banner"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 2: SYSTEM ANALYTICS & POPULAR ROUTES */}
      {activeTab === "analytics" && (
        <div className="space-y-6 animate-in fade-in">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs space-y-4">
              <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-[#d84e55]" />
                <span>Top High-Demand Corridors</span>
              </h3>
              {stats?.topRoutes && stats.topRoutes.length > 0 ? (
                <div className="space-y-3">
                  {stats.topRoutes.map((r, i) => (
                    <div key={i} className="p-3.5 bg-gray-50 rounded-2xl flex items-center justify-between">
                      <div>
                        <p className="text-xs font-bold text-gray-900">
                          {r.sourceCity} → {r.destinationCity}
                        </p>
                        <p className="text-[11px] text-gray-500">Starting at ₹{r.minPrice} ({r.busCount} buses)</p>
                      </div>
                      <Link
                        href={`/bus-tickets/${r.sourceCity.toLowerCase()}-to-${r.destinationCity.toLowerCase()}`}
                        className="text-xs font-bold text-[#d84e55] hover:underline"
                      >
                        View Live →
                      </Link>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-gray-400 py-8 text-center">No route traffic recorded yet.</p>
              )}
            </div>

            <div className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs space-y-4">
              <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
                <Shield className="w-4 h-4 text-emerald-600" />
                <span>Platform Security & RBAC Status</span>
              </h3>
              <div className="space-y-3 text-xs">
                <div className="p-3 bg-emerald-50 text-emerald-900 rounded-2xl flex items-center justify-between">
                  <div>
                    <p className="font-bold">Default Admin Account</p>
                    <p className="text-[11px] text-emerald-700">redbus@admin.in (Auto-Verified, Password Hashed)</p>
                  </div>
                  <span className="px-2 py-0.5 bg-emerald-200 text-emerald-900 font-bold rounded-md text-[10px]">
                    ACTIVE
                  </span>
                </div>
                <div className="p-3 bg-blue-50 text-blue-900 rounded-2xl flex items-center justify-between">
                  <div>
                    <p className="font-bold">Stateless Spring Security (JWT)</p>
                    <p className="text-[11px] text-blue-700">DaoAuthenticationProvider with BCrypt & Method Security</p>
                  </div>
                  <span className="px-2 py-0.5 bg-blue-200 text-blue-900 font-bold rounded-md text-[10px]">
                    ONLINE
                  </span>
                </div>
                <div className="p-3 bg-purple-50 text-purple-900 rounded-2xl flex items-center justify-between">
                  <div>
                    <p className="font-bold">Operator Governance Guard</p>
                    <p className="text-[11px] text-purple-700">Operators must be approved by Admin before publishing routes</p>
                  </div>
                  <span className="px-2 py-0.5 bg-purple-200 text-purple-900 font-bold rounded-md text-[10px]">
                    ENFORCED
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: MASTER BOOKINGS LEDGER */}
      {activeTab === "bookings" && (
        <div className="bg-white rounded-3xl border border-gray-200 shadow-xs overflow-hidden animate-in fade-in">
          <div className="p-6 border-b border-gray-100 flex items-center justify-between">
            <div>
              <h3 className="font-bold text-base text-gray-900">Master Passenger Bookings</h3>
              <p className="text-xs text-gray-500">Live ticket reservations across all commercial operators</p>
            </div>
            <span className="text-xs text-gray-400 font-medium">Page {page + 1}</span>
          </div>

          {isBookingsLoading ? (
            <div className="py-12 text-center text-xs text-gray-400">Loading master bookings...</div>
          ) : !bookingsData || bookingsData.content.length === 0 ? (
            <div className="py-12 text-center text-xs text-gray-400">No booking records found.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-gray-50 text-gray-500 uppercase font-bold border-b border-gray-100 text-[10px]">
                  <tr>
                    <th className="px-6 py-3">PNR</th>
                    <th className="px-6 py-3">Passenger</th>
                    <th className="px-6 py-3">Route</th>
                    <th className="px-6 py-3">Seats</th>
                    <th className="px-6 py-3">Amount</th>
                    <th className="px-6 py-3">Status</th>
                    <th className="px-6 py-3 text-right">Details</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 text-gray-700">
                  {bookingsData.content.map((b) => (
                    <tr key={b.id} className="hover:bg-gray-50/80 transition-colors">
                      <td className="px-6 py-4 font-mono font-bold text-gray-900">{b.pnr}</td>
                      <td className="px-6 py-4">
                        <div className="font-bold text-gray-900">{b.passengers?.[0]?.name || "Passenger"}</div>
                        <div className="text-gray-400 text-[11px]">{b.contactEmail}</div>
                      </td>
                      <td className="px-6 py-4 font-medium text-gray-800">
                        {b.sourceCity} → {b.destinationCity}
                      </td>
                      <td className="px-6 py-4 font-semibold text-gray-600">
                        {b.passengers?.map((p) => p.seatNumber).join(", ") || "-"}
                      </td>
                      <td className="px-6 py-4 font-black text-gray-900">₹{b.totalAmount}</td>
                      <td className="px-6 py-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                            b.status === "CONFIRMED"
                              ? "bg-emerald-100 text-emerald-800"
                              : b.status === "CANCELLED"
                              ? "bg-red-100 text-red-800"
                              : "bg-amber-100 text-amber-800"
                          }`}
                        >
                          {b.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <Link
                          href={`/booking-confirmation?pnr=${b.pnr}`}
                          className="text-[#d84e55] font-bold hover:underline"
                        >
                          View →
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          <div className="p-4 border-t border-gray-100 flex items-center justify-between text-xs">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="px-4 py-2 border border-gray-200 rounded-xl disabled:opacity-40 hover:bg-gray-50 font-bold cursor-pointer"
            >
              Previous
            </button>
            <span className="font-bold text-gray-600">Page {page + 1}</span>
            <button
              onClick={() => setPage(page + 1)}
              disabled={!bookingsData || bookingsData.content.length < 12}
              className="px-4 py-2 border border-gray-200 rounded-xl disabled:opacity-40 hover:bg-gray-50 font-bold cursor-pointer"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
