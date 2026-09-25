"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAppDispatch, useAppSelector } from "@/store";
import { setCredentials } from "@/store/authSlice";
import { useLoginMutation } from "@/store/apiSlice";
import {
  Bus,
  Lock,
  Mail,
  ArrowRight,
  ShieldCheck,
  TrendingUp,
  BarChart3,
  Users,
  AlertCircle,
  CheckCircle2,
} from "lucide-react";

export default function OperatorLoginPage() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { user, token } = useAppSelector((state) => state.auth);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const [loginMutation, { isLoading }] = useLoginMutation();

  // If already logged in as operator, redirect to operator dashboard
  useEffect(() => {
    const isOp =
      user?.role?.includes("OPERATOR") ||
      user?.role?.includes("ADMIN") ||
      user?.roles?.includes("ROLE_OPERATOR") ||
      user?.roles?.includes("ROLE_ADMIN");

    if (token && user && isOp) {
      router.push("/operator");
    }
  }, [token, user, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const res = await loginMutation({ email, password }).unwrap();
      dispatch(setCredentials(res));

      const isOp =
        res.user?.role?.includes("OPERATOR") ||
        res.user?.role?.includes("ADMIN") ||
        res.user?.roles?.includes("ROLE_OPERATOR") ||
        res.user?.roles?.includes("ROLE_ADMIN");

      if (isOp) {
        setSuccessMessage("Login successful! Redirecting to Operator Dashboard...");
        setTimeout(() => {
          router.push("/operator");
        }, 800);
      } else {
        setErrorMessage(
          "Your account is registered as a Passenger. Please register your Operator profile to access the Partner Portal."
        );
      }
    } catch (err: any) {
      const msg =
        err?.data?.message ||
        err?.data?.error ||
        (typeof err?.data === "string" ? err.data : null) ||
        err?.message ||
        "Authentication failed. Please verify your operator credentials.";
      setErrorMessage(msg);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden font-[family-name:var(--font-zomato)] selection:bg-[#d84e55] selection:text-white">
      {/* Background glowing gradients */}
      <div className="absolute top-0 left-1/4 w-96 h-96 bg-red-600/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-amber-600/10 rounded-full blur-3xl pointer-events-none" />

      {/* Header bar */}
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center z-10">
        <Link href="/" className="inline-flex items-center gap-2.5 mb-6 group">
          <div className="w-12 h-12 bg-gradient-to-tr from-[#d84e55] to-red-500 rounded-2xl flex items-center justify-center shadow-lg shadow-red-500/20 group-hover:scale-105 transition-transform">
            <Bus className="w-6 h-6 text-white" />
          </div>
          <div className="text-left">
            <span className="text-2xl font-black tracking-tight text-white block">
              redBus <span className="text-[#d84e55] font-light">Partner</span>
            </span>
            <span className="text-[10px] uppercase font-bold tracking-widest text-slate-400">
              Fleet & Route Console
            </span>
          </div>
        </Link>
        <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
          Sign in to Partner Portal
        </h2>
        <p className="mt-2 text-sm text-slate-400 font-normal">
          Manage your fleet, published schedules, live seat occupancy & revenue
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md z-10 px-4 sm:px-0">
        <div className="bg-slate-900/90 backdrop-blur-md border border-slate-800 py-8 px-6 shadow-2xl rounded-3xl sm:px-10">
          {errorMessage && (
            <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-2xl flex items-start space-x-3 text-xs text-red-400">
              <AlertCircle className="w-5 h-5 shrink-0 text-red-500 mt-0.5" />
              <div className="flex-1">
                <span className="font-medium">{errorMessage}</span>
                {errorMessage.includes("Passenger") && (
                  <Link
                    href="/operator/register"
                    className="block mt-2 font-bold text-red-400 hover:text-red-300 underline"
                  >
                    Click here to register as a Bus Operator →
                  </Link>
                )}
              </div>
            </div>
          )}

          {successMessage && (
            <div className="mb-6 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-2xl flex items-start space-x-3 text-xs text-emerald-400">
              <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-500 mt-0.5" />
              <span className="font-medium">{successMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2">
                Operator Business Email
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="operator@travels.com"
                  className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-hidden focus:border-[#d84e55] focus:ring-1 focus:ring-[#d84e55] transition-all"
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                  Password
                </label>
              </div>
              <div className="relative">
                <Lock className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-hidden focus:border-[#d84e55] focus:ring-1 focus:ring-[#d84e55] transition-all"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3.5 bg-gradient-to-r from-[#d84e55] to-red-600 hover:from-red-600 hover:to-red-700 text-white rounded-xl font-bold text-sm shadow-lg shadow-red-600/30 transition-all flex items-center justify-center space-x-2 disabled:opacity-60 cursor-pointer"
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <span>Access Dashboard</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Registration link */}
          <div className="mt-8 pt-6 border-t border-slate-800 text-center">
            <p className="text-xs text-slate-400">
              New bus partner or transport company?{" "}
              <Link
                href="/operator/register"
                className="font-bold text-[#d84e55] hover:text-red-400 transition-colors ml-1"
              >
                Sign up your fleet →
              </Link>
            </p>
          </div>
        </div>

        {/* Feature Highlights */}
        <div className="mt-8 grid grid-cols-3 gap-3 text-center">
          <div className="p-3 bg-slate-900/40 border border-slate-800/80 rounded-2xl">
            <TrendingUp className="w-5 h-5 text-[#d84e55] mx-auto mb-1.5" />
            <p className="text-[11px] font-bold text-slate-300">Live Analytics</p>
            <p className="text-[10px] text-slate-500">Real-time revenue</p>
          </div>
          <div className="p-3 bg-slate-900/40 border border-slate-800/80 rounded-2xl">
            <Bus className="w-5 h-5 text-amber-500 mx-auto mb-1.5" />
            <p className="text-[11px] font-bold text-slate-300">Fleet Control</p>
            <p className="text-[10px] text-slate-500">Add buses & photos</p>
          </div>
          <div className="p-3 bg-slate-900/40 border border-slate-800/80 rounded-2xl">
            <ShieldCheck className="w-5 h-5 text-emerald-500 mx-auto mb-1.5" />
            <p className="text-[11px] font-bold text-slate-300">Direct Bookings</p>
            <p className="text-[10px] text-slate-500">Zero commission</p>
          </div>
        </div>

        {/* Back to passenger portal */}
        <div className="mt-6 text-center">
          <Link
            href="/"
            className="text-xs font-medium text-slate-400 hover:text-white transition-colors"
          >
            ← Back to redBus Passenger Portal
          </Link>
        </div>
      </div>
    </div>
  );
}
