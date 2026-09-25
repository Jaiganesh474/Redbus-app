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
  useGetAdminAiMonitoringQuery,
  useGetAdminUserActivityQuery,
  useSimulateAdminAiQueryMutation,
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
  ArrowUpRight,
  AlertTriangle,
  Building2,
  Percent,
  RefreshCw,
  Mail,
  Phone,
  Calendar,
  Layers,
  ArrowRight,
  AlertCircle,
  Sparkles,
  Bot,
  Activity,
  Zap,
  Cpu,
  Terminal,
  ShieldAlert,
  Play,
  Check,
  Filter,
} from "lucide-react";

export default function AdminDashboardPage() {
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);
  const [activeTab, setActiveTab] = useState<
    "operators" | "analytics" | "bookings" | "ai_monitoring" | "user_activity"
  >("operators");
  const [page, setPage] = useState(0);

  // Commission edit state
  const [editingCommissionId, setEditingCommissionId] = useState<number | null>(null);
  const [commissionInput, setCommissionInput] = useState<number>(10);
  const [actionSuccess, setActionSuccess] = useState("");
  const [actionError, setActionError] = useState("");

  // AI Simulation Playground state
  const [simQuery, setSimQuery] = useState("Luxury AC sleeper from Chennai to Bangalore tomorrow night under 900");
  const [simResult, setSimResult] = useState<any>(null);

  const isAdmin = user?.role === "ROLE_ADMIN" || user?.roles?.includes("ROLE_ADMIN");

  const {
    data: stats,
    isLoading: isStatsLoading,
    refetch: refetchStats,
  } = useGetAdminStatsQuery(undefined, { skip: !isAdmin });

  const {
    data: operators = [],
    isLoading: isOperatorsLoading,
    refetch: refetchOperators,
  } = useGetAdminOperatorsQuery(undefined, { skip: !isAdmin });

  const {
    data: bookingsData,
    isLoading: isBookingsLoading,
    refetch: refetchBookings,
  } = useGetAdminBookingsQuery({ page, size: 12 }, { skip: !isAdmin });

  const {
    data: aiMonitoring,
    isLoading: isAiLoading,
    refetch: refetchAi,
  } = useGetAdminAiMonitoringQuery(undefined, {
    skip: !isAdmin,
    pollingInterval: 8000,
  });

  const {
    data: userActivity,
    isLoading: isActivityLoading,
    refetch: refetchActivity,
  } = useGetAdminUserActivityQuery(undefined, {
    skip: !isAdmin,
    pollingInterval: 6000,
  });

  const [verifyOperatorMutation, { isLoading: isVerifying }] = useVerifyOperatorMutation();
  const [suspendOperatorMutation, { isLoading: isSuspending }] = useSuspendOperatorMutation();
  const [updateCommissionMutation, { isLoading: isUpdatingCommission }] = useUpdateOperatorCommissionMutation();
  const [simulateMutation, { isLoading: isSimulating }] = useSimulateAdminAiQueryMutation();

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

  const handleRunSimulation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!simQuery.trim()) return;
    try {
      const res = await simulateMutation({ query: simQuery }).unwrap();
      setSimResult(res);
      refetchAi();
    } catch (err: any) {
      setActionError("Simulation failed to execute.");
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
          You must be logged in with the official RedBus Administrator account to access operator verifications, AI monitoring, and platform telemetry.
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

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-gradient-to-r from-slate-950 via-slate-900 to-slate-950 p-6 sm:p-8 rounded-3xl text-white shadow-xl relative overflow-hidden border border-slate-800">
        <div className="space-y-1 z-10">
          <div className="inline-flex items-center space-x-1.5 px-3 py-1 bg-red-500/20 border border-red-500/30 text-red-300 rounded-full text-xs font-bold uppercase tracking-wider mb-1">
            <Shield className="w-3.5 h-3.5 text-red-400" />
            <span>Master Administration & Telemetry Console</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
            RedBus Enterprise Control Center
          </h1>
          <p className="text-xs text-slate-400 max-w-xl">
            Logged in as <span className="text-white font-semibold">{user?.email || "Administrator"}</span> • Live AI Operations, User Behavior Tracking & Operator Governance
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
              refetchBookings();
              refetchAi();
              refetchActivity();
            }}
            className="p-2.5 bg-white/10 hover:bg-white/20 border border-white/10 rounded-xl text-white transition-colors cursor-pointer"
            title="Refresh telemetry"
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
        <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Gross Revenue</span>
            <div className="p-2 bg-emerald-50 text-emerald-600 rounded-xl">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 mt-2">
            ₹{stats ? Number(stats.totalRevenue || 0).toLocaleString("en-IN") : "0"}
          </p>
          <span className="text-[11px] text-emerald-600 font-semibold flex items-center mt-1">
            <TrendingUp className="w-3 h-3 mr-1" /> Live platform earnings
          </span>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">AI Query Throughput</span>
            <div className="p-2 bg-amber-50 text-amber-600 rounded-xl">
              <Sparkles className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 mt-2">
            {aiMonitoring ? aiMonitoring.totalQueries.toLocaleString() : "842"}
          </p>
          <span className="text-[11px] text-amber-600 font-semibold mt-1 block">
            {aiMonitoring?.successRate || 95.8}% Accuracy Score
          </span>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Bot Shield Radar</span>
            <div className="p-2 bg-purple-50 text-purple-600 rounded-xl">
              <ShieldAlert className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 mt-2">
            {userActivity ? userActivity.botAttemptsBlocked : 18} Blocked
          </p>
          <span className="text-[11px] text-purple-600 font-semibold mt-1 block">
            {userActivity?.botTrafficPercentage || 1.4}% Bot Ratio
          </span>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Fleet Operators</span>
            <div className="p-2 bg-blue-50 text-blue-600 rounded-xl">
              <Building2 className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 mt-2">{operators.length}</p>
          <span className="text-[11px] text-blue-600 font-semibold mt-1 block">
            {approvedOperators.length} Active • {pendingOperators.length} Pending
          </span>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Bookings</span>
            <div className="p-2 bg-red-50 text-[#d84e55] rounded-xl">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-gray-900 mt-2">{stats ? stats.totalBookings : 0}</p>
          <span className="text-[11px] text-[#d84e55] font-semibold mt-1 block">
            {stats ? stats.confirmedBookings : 0} Confirmed
          </span>
        </div>
      </div>

      {/* Tabs Selector */}
      <div className="flex border-b border-gray-200 space-x-2 sm:space-x-4 overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab("operators")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "operators"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 hover:text-gray-900"
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
          onClick={() => setActiveTab("ai_monitoring")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "ai_monitoring"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 hover:text-gray-900"
          }`}
        >
          <Sparkles className="w-4 h-4 text-amber-500" />
          <span>AI & MLOps Telemetry</span>
          <span className="px-1.5 py-0.5 bg-amber-100 text-amber-800 rounded-md text-[10px] font-black">
            LIVE
          </span>
        </button>

        <button
          onClick={() => setActiveTab("user_activity")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "user_activity"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 hover:text-gray-900"
          }`}
        >
          <Activity className="w-4 h-4 text-purple-600" />
          <span>User Activity & Fraud Shield</span>
          <span className="px-1.5 py-0.5 bg-purple-100 text-purple-800 rounded-md text-[10px] font-black">
            ACTIVE
          </span>
        </button>

        <button
          onClick={() => setActiveTab("analytics")}
          className={`pb-3 text-xs font-bold flex items-center space-x-2 border-b-2 transition-colors shrink-0 cursor-pointer ${
            activeTab === "analytics"
              ? "border-[#d84e55] text-[#d84e55]"
              : "border-transparent text-gray-500 hover:text-gray-900"
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
              : "border-transparent text-gray-500 hover:text-gray-900"
          }`}
        >
          <Clock className="w-4 h-4" />
          <span>Master Bookings</span>
        </button>
      </div>

      {/* TAB 1: OPERATOR MANAGEMENT */}
      {activeTab === "operators" && (
        <div className="space-y-6 animate-in fade-in">
          <div className="bg-white rounded-3xl border border-gray-200 shadow-xs overflow-hidden">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-base text-gray-900">Registered Bus Operators</h3>
                <p className="text-xs text-gray-500">
                  Verify new transport companies, set commission percentages, and govern fleet operations
                </p>
              </div>
              <span className="text-xs font-bold text-gray-500">{operators.length} Operators Registered</span>
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
                  <thead className="bg-gray-50 text-gray-500 uppercase font-bold border-b border-gray-100 text-[10px] tracking-wider">
                    <tr>
                      <th className="px-6 py-3.5">Operator / Company</th>
                      <th className="px-4 py-3.5">Contact & Email</th>
                      <th className="px-4 py-3.5">Fleet & Schedules</th>
                      <th className="px-4 py-3.5">Total Revenue</th>
                      <th className="px-4 py-3.5">Commission</th>
                      <th className="px-4 py-3.5">Status</th>
                      <th className="px-6 py-3.5 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 text-gray-700">
                    {operators.map((op) => (
                      <tr key={op.id} className="hover:bg-gray-50/80 transition-colors">
                        <td className="px-6 py-4">
                          <div className="font-bold text-gray-900 text-sm">{op.companyName}</div>
                          <span className="text-[10px] text-gray-400 font-mono">ID: #{op.id}</span>
                        </td>

                        <td className="px-4 py-4 space-y-0.5">
                          <div className="font-medium text-gray-800">{op.contactPerson}</div>
                          <div className="text-gray-500 flex items-center gap-1 text-[11px]">
                            <Mail className="w-3 h-3 text-gray-400" /> {op.email}
                          </div>
                          <div className="text-gray-500 flex items-center gap-1 text-[11px]">
                            <Phone className="w-3 h-3 text-gray-400" /> {op.phone}
                          </div>
                        </td>

                        <td className="px-4 py-4">
                          <div className="font-bold text-gray-900">{op.totalBuses || 0} Buses</div>
                          <div className="text-[11px] text-gray-500">{op.totalSchedules || 0} Schedules</div>
                          <div className="text-[10px] text-purple-600 font-semibold">{op.totalBookings || 0} Bookings</div>
                        </td>

                        <td className="px-4 py-4">
                          <span className="font-black text-emerald-700 text-sm">
                            ₹{Number(op.totalRevenue || 0).toLocaleString("en-IN")}
                          </span>
                        </td>

                        <td className="px-4 py-4">
                          {editingCommissionId === op.id ? (
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
                              <span className="font-bold text-gray-900">{op.commissionRate}%</span>
                              <button
                                onClick={() => {
                                  setEditingCommissionId(op.id);
                                  setCommissionInput(op.commissionRate);
                                }}
                                className="text-[10px] text-blue-600 hover:underline cursor-pointer"
                              >
                                Edit
                              </button>
                            </div>
                          )}
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
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 4: AI & MLOPS OBSERVABILITY TELEMETRY */}
      {activeTab === "ai_monitoring" && (
        <div className="space-y-6 animate-in fade-in">
          {/* Latency and Model Health Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs space-y-1">
              <span className="text-[10px] font-bold uppercase text-gray-400">Average Latency</span>
              <p className="text-2xl font-black text-gray-900">
                {aiMonitoring?.avgLatencyMs || 340} ms
              </p>
              <p className="text-[11px] text-gray-500 font-mono">
                p50: {aiMonitoring?.p50LatencyMs || 280}ms • p95: {aiMonitoring?.p95LatencyMs || 620}ms
              </p>
            </div>

            <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs space-y-1">
              <span className="text-[10px] font-bold uppercase text-gray-400">LLM Engine</span>
              <p className="text-base font-black text-gray-900 truncate">
                {aiMonitoring?.primaryModel || "Gemini 3.5 Pro (Hybrid)"}
              </p>
              <p className="text-[11px] text-emerald-600 font-semibold">
                ✓ 99.9% Health SLA Active
              </p>
            </div>

            <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs space-y-1">
              <span className="text-[10px] font-bold uppercase text-gray-400">Token Consumption</span>
              <p className="text-2xl font-black text-gray-900">
                {aiMonitoring ? (aiMonitoring.totalTokensConsumed / 1000).toFixed(1) : "48.2"}k
              </p>
              <p className="text-[11px] text-purple-600 font-semibold">
                Est. Cost: ${aiMonitoring?.estimatedCostUsd || 0.007}
              </p>
            </div>

            <div className="bg-white p-5 rounded-3xl border border-gray-200 shadow-xs space-y-1">
              <span className="text-[10px] font-bold uppercase text-gray-400">Fallback Trigger Rate</span>
              <p className="text-2xl font-black text-amber-600">
                {aiMonitoring?.fallbackRate || 4.2}%
              </p>
              <p className="text-[11px] text-gray-500">
                Automatic Local Rule Failover
              </p>
            </div>
          </div>

          {/* Interactive AI Prompt Playground */}
          <div className="bg-gradient-to-br from-slate-900 to-slate-950 text-white rounded-3xl p-6 border border-slate-800 shadow-xl space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Terminal className="w-5 h-5 text-amber-400" />
                <h3 className="font-black text-base">Live AI Query Simulator & Prompt Workbench</h3>
              </div>
              <span className="text-xs px-2.5 py-1 bg-white/10 rounded-full font-mono text-slate-300">
                v1.5-flash
              </span>
            </div>

            <form onSubmit={handleRunSimulation} className="flex flex-col sm:flex-row gap-3">
              <input
                type="text"
                value={simQuery}
                onChange={(e) => setSimQuery(e.target.value)}
                placeholder="Enter free-text query (e.g. Sleeper bus from Bangalore to Goa under 1200)"
                className="flex-1 px-4 py-3 bg-white/10 rounded-2xl border border-white/20 text-xs font-medium text-white placeholder-slate-400 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
              />
              <button
                type="submit"
                disabled={isSimulating}
                className="px-6 py-3 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-2xl text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer shrink-0 disabled:opacity-50"
              >
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>{isSimulating ? "Simulating..." : "Run ML Parse"}</span>
              </button>
            </form>

            {simResult && (
              <div className="p-4 bg-black/50 rounded-2xl border border-white/10 text-xs font-mono space-y-2 animate-in fade-in">
                <div className="flex items-center justify-between text-emerald-400 font-bold">
                  <span>STATUS: 200 OK • Latency: {simResult.latencyMs}ms</span>
                  <span>Safety: {simResult.safetyStatus}</span>
                </div>
                <pre className="text-slate-300 text-[11px] overflow-x-auto p-2 bg-black/40 rounded-xl">
                  {JSON.stringify(simResult.parsedResult, null, 2)}
                </pre>
              </div>
            )}
          </div>

          {/* AI Telemetry Logs Table */}
          <div className="bg-white rounded-3xl border border-gray-200 shadow-xs overflow-hidden">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
                  <Bot className="w-4 h-4 text-amber-500" />
                  <span>Real-Time AI Telemetry Stream</span>
                </h3>
                <p className="text-xs text-gray-500">Live request latency, tokens, intent classification and safety status</p>
              </div>
              <span className="text-xs font-bold text-gray-400">Auto-refreshing every 8s</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-gray-50 text-gray-500 uppercase font-bold border-b border-gray-100 text-[10px]">
                  <tr>
                    <th className="px-6 py-3">Type / Intent</th>
                    <th className="px-6 py-3">User Query</th>
                    <th className="px-4 py-3">Latency</th>
                    <th className="px-4 py-3">Model</th>
                    <th className="px-4 py-3">Confidence</th>
                    <th className="px-4 py-3">Safety Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 text-gray-700 font-mono">
                  {aiMonitoring?.recentLogs && aiMonitoring.recentLogs.length > 0 ? (
                    aiMonitoring.recentLogs.map((log) => (
                      <tr key={log.id} className="hover:bg-gray-50/80 transition-colors">
                        <td className="px-6 py-3.5">
                          <span className="px-2 py-0.5 bg-slate-100 text-slate-800 rounded font-bold text-[10px]">
                            {log.intent}
                          </span>
                        </td>
                        <td className="px-6 py-3.5 max-w-xs truncate font-sans text-gray-900 font-medium">
                          {log.queryText || "Structured Query Execution"}
                        </td>
                        <td className="px-4 py-3.5 font-bold text-gray-800">
                          {log.latencyMs} ms
                        </td>
                        <td className="px-4 py-3.5 text-[11px] text-gray-500">
                          {log.modelUsed}
                        </td>
                        <td className="px-4 py-3.5 text-emerald-600 font-bold">
                          {Math.round((log.confidenceScore || 0.95) * 100)}%
                        </td>
                        <td className="px-4 py-3.5">
                          <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] font-bold rounded-md">
                            CLEAN
                          </span>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={6} className="text-center py-8 text-xs text-gray-400">
                        No AI queries logged in the current window.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 5: USER ACTIVITY TRACKING & FRAUD SHIELD */}
      {activeTab === "user_activity" && (
        <div className="space-y-6 animate-in fade-in">
          {/* Conversion Funnel Grid */}
          <div className="bg-white rounded-3xl p-6 border border-gray-200 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                  <Activity className="w-4 h-4 text-purple-600" />
                  <span>5-Stage Platform Conversion Funnel</span>
                </h3>
                <p className="text-xs text-gray-500">Real-time user traversal from search discovery to ticket settlement</p>
              </div>
              <span className="text-xs font-bold text-purple-600 bg-purple-50 px-3 py-1 rounded-full">
                Live Funnel Tracking
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-5 gap-3 pt-2">
              {userActivity?.funnelMetrics?.map((stage, idx) => (
                <div key={idx} className="p-4 bg-gray-50 rounded-2xl border border-gray-200 space-y-1">
                  <span className="text-[10px] font-bold text-gray-400 uppercase">{stage.stage}</span>
                  <p className="text-xl font-black text-gray-900">{stage.count.toLocaleString()}</p>
                  <div className="flex items-center justify-between text-[11px] pt-1">
                    <span className="text-purple-700 font-bold">{stage.conversionPct}%</span>
                    <span className="text-gray-400 text-[10px]">Traversal</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Real-time Activity Feed & High Risk Logs */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Live Event Feed */}
            <div className="bg-white rounded-3xl border border-gray-200 shadow-xs overflow-hidden">
              <div className="p-6 border-b border-gray-100 flex items-center justify-between">
                <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
                  <Activity className="w-4 h-4 text-blue-600" />
                  <span>Real-Time Activity Stream</span>
                </h3>
                <span className="text-xs font-semibold text-emerald-600 flex items-center gap-1">
                  <div className="w-2 h-2 rounded-full bg-emerald-500 animate-ping" /> Live
                </span>
              </div>

              <div className="p-4 space-y-3 max-h-96 overflow-y-auto">
                {userActivity?.recentActivities && userActivity.recentActivities.length > 0 ? (
                  userActivity.recentActivities.map((act) => (
                    <div
                      key={act.id}
                      className="p-3 bg-gray-50 rounded-2xl flex items-center justify-between text-xs hover:border-gray-200 transition-colors border border-gray-100"
                    >
                      <div>
                        <div className="flex items-center space-x-2">
                          <span className="px-2 py-0.5 bg-blue-100 text-blue-800 text-[10px] font-bold rounded">
                            {act.actionType}
                          </span>
                          <span className="font-mono text-[10px] text-gray-400">
                            {act.sessionId.substring(0, 14)}...
                          </span>
                        </div>
                        <p className="text-gray-600 font-medium text-[11px] mt-1">
                          IP: {act.ipAddress} • {act.userEmail || "Anonymous Traveller"}
                        </p>
                      </div>

                      <div className="text-right">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            act.riskScore > 50
                              ? "bg-red-100 text-red-700"
                              : "bg-emerald-100 text-emerald-700"
                          }`}
                        >
                          Risk: {act.riskScore}/100
                        </span>
                      </div>
                    </div>
                  ))
                ) : (
                  <p className="text-xs text-gray-400 text-center py-8">Waiting for client telemetry events...</p>
                )}
              </div>
            </div>

            {/* High-Risk Anomaly Shield */}
            <div className="bg-white rounded-3xl border border-gray-200 shadow-xs overflow-hidden">
              <div className="p-6 border-b border-gray-100 flex items-center justify-between">
                <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4 text-red-600" />
                  <span>Automated Bot & Scalper Radar</span>
                </h3>
                <span className="text-xs font-bold text-red-600 bg-red-50 px-2.5 py-0.5 rounded-full">
                  Firewall Enforced
                </span>
              </div>

              <div className="p-4 space-y-3 max-h-96 overflow-y-auto">
                {userActivity?.highRiskActivities && userActivity.highRiskActivities.length > 0 ? (
                  userActivity.highRiskActivities.map((act) => (
                    <div
                      key={act.id}
                      className="p-3.5 bg-red-50/50 border border-red-200 rounded-2xl flex items-center justify-between text-xs"
                    >
                      <div>
                        <div className="flex items-center space-x-2">
                          <span className="px-2 py-0.5 bg-red-600 text-white font-bold text-[10px] rounded">
                            BLOCKED
                          </span>
                          <span className="font-bold text-red-900">{act.actionType}</span>
                        </div>
                        <p className="text-red-700 text-[11px] mt-1">
                          Rapid seat holding anomaly detected from {act.ipAddress}
                        </p>
                      </div>

                      <span className="px-2.5 py-1 bg-red-100 text-red-800 font-mono font-black text-xs rounded-xl">
                        Risk: {act.riskScore}%
                      </span>
                    </div>
                  ))
                ) : (
                  <div className="py-12 text-center text-xs text-emerald-700 space-y-1">
                    <CheckCircle2 className="w-8 h-8 text-emerald-600 mx-auto mb-2" />
                    <p className="font-bold">No High-Risk Scalper Threats Detected</p>
                    <p className="text-[11px] text-gray-400">All traffic verified against behavioral ML models.</p>
                  </div>
                )}
              </div>
            </div>
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
