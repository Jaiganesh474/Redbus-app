"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAppDispatch } from "@/store";
import { setCredentials } from "@/store/authSlice";
import { useRegisterOperatorMutation } from "@/store/apiSlice";
import {
  Bus,
  Building2,
  User,
  Mail,
  Phone,
  Lock,
  MapPin,
  FileText,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
} from "lucide-react";

export default function OperatorRegisterPage() {
  const router = useRouter();
  const dispatch = useAppDispatch();

  const [companyName, setCompanyName] = useState("");
  const [contactName, setContactName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [city, setCity] = useState("");
  const [state, setState] = useState("");
  const [address, setAddress] = useState("");
  const [gstNumber, setGstNumber] = useState("");

  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const [registerOperatorMutation, { isLoading }] = useRegisterOperatorMutation();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    if (!companyName.trim()) {
      setErrorMessage("Company / Travel Agency Name is required.");
      return;
    }
    if (password.length < 6) {
      setErrorMessage("Password must be at least 6 characters long.");
      return;
    }

    try {
      const res = await registerOperatorMutation({
        companyName: companyName.trim(),
        contactPerson: contactName.trim(),
        contactName: contactName.trim(),
        email: email.trim().toLowerCase(),
        phone: phone.trim(),
        password,
        city: city.trim(),
        state: state.trim(),
        address: address.trim(),
        gstNumber: gstNumber.trim().toUpperCase(),
      }).unwrap();

      dispatch(setCredentials(res));
      setSuccessMessage("Operator profile registered successfully! Redirecting to dashboard...");
      setTimeout(() => {
        router.push("/operator");
      }, 1000);
    } catch (err: any) {
      const msg =
        err?.data?.message ||
        err?.data?.error ||
        (typeof err?.data === "string" ? err.data : null) ||
        err?.message ||
        "Registration failed. Please try again.";
      setErrorMessage(msg);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden font-[family-name:var(--font-zomato)] selection:bg-[#d84e55] selection:text-white">
      {/* Glow */}
      <div className="absolute top-0 right-1/4 w-96 h-96 bg-red-600/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-1/4 w-96 h-96 bg-amber-600/10 rounded-full blur-3xl pointer-events-none" />

      {/* Header */}
      <div className="sm:mx-auto sm:w-full sm:max-w-xl text-center z-10">
        <Link href="/" className="inline-flex items-center gap-2 mb-6 group">
          <div className="w-12 h-12 bg-gradient-to-tr from-[#d84e55] to-red-500 rounded-2xl flex items-center justify-center shadow-lg shadow-red-500/20 group-hover:scale-105 transition-transform">
            <Bus className="w-6 h-6 text-white" />
          </div>
          <div className="text-left">
            <span className="text-2xl font-black tracking-tight text-white block">
              redBus <span className="text-[#d84e55] font-light">Partner</span>
            </span>
            <span className="text-[10px] uppercase font-bold tracking-widest text-slate-400">
              Operator Fleet Registration
            </span>
          </div>
        </Link>
        <h2 className="text-2xl font-extrabold text-white tracking-tight">
          Register Your Fleet with redBus
        </h2>
        <p className="mt-2 text-sm text-slate-400">
          List your buses, manage schedules, upload high-res bus photos & track bookings directly
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-xl z-10 px-4 sm:px-0">
        <div className="bg-slate-900/90 backdrop-blur-md border border-slate-800 py-8 px-6 shadow-2xl rounded-3xl sm:px-10">
          {errorMessage && (
            <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-2xl flex items-start space-x-3 text-xs text-red-400">
              <AlertCircle className="w-5 h-5 shrink-0 text-red-500 mt-0.5" />
              <div className="flex-1">
                <span className="font-medium">{errorMessage}</span>
              </div>
            </div>
          )}

          {successMessage && (
            <div className="mb-6 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-2xl flex items-start space-x-3 text-xs text-emerald-400">
              <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-500 mt-0.5" />
              <span className="font-medium">{successMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Company Name */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                Bus Operator / Travels Name <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <Building2 className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                <input
                  type="text"
                  required
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  placeholder="e.g. Royal Star Travels / SRS Express"
                  className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-hidden focus:border-[#d84e55] focus:ring-1 focus:ring-[#d84e55]"
                />
              </div>
            </div>

            {/* Contact Person & Phone */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                  Contact Person Name <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <User className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="text"
                    required
                    value={contactName}
                    onChange={(e) => setContactName(e.target.value)}
                    placeholder="e.g. Rajesh Kumar"
                    className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-hidden focus:border-[#d84e55]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                  Phone / Mobile Number <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Phone className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="tel"
                    required
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+91 98765 43210"
                    className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-hidden focus:border-[#d84e55]"
                  />
                </div>
              </div>
            </div>

            {/* Email & Password */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                  Business Email <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Mail className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="partner@travels.com"
                    className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-hidden focus:border-[#d84e55]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                  Create Password <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Lock className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="At least 6 characters"
                    className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-hidden focus:border-[#d84e55]"
                  />
                </div>
              </div>
            </div>

            {/* City & State */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                  Operating City / Hub
                </label>
                <div className="relative">
                  <MapPin className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="text"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="e.g. Bangalore / Chennai"
                    className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-hidden focus:border-[#d84e55]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                  GST / Fleet Permit No. (Optional)
                </label>
                <div className="relative">
                  <FileText className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="text"
                    value={gstNumber}
                    onChange={(e) => setGstNumber(e.target.value)}
                    placeholder="e.g. 29AAAAA0000A1Z5"
                    className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 uppercase focus:outline-hidden focus:border-[#d84e55]"
                  />
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-4 mt-2 bg-gradient-to-r from-[#d84e55] to-red-600 hover:from-red-600 hover:to-red-700 text-white rounded-xl font-bold text-sm shadow-lg shadow-red-600/30 transition-all flex items-center justify-center space-x-2 disabled:opacity-60 cursor-pointer"
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <span>Create Operator Account & Launch Fleet</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Login Link */}
          <div className="mt-8 pt-6 border-t border-slate-800 text-center">
            <p className="text-xs text-slate-400">
              Already have an operator account?{" "}
              <Link
                href="/operator/login"
                className="font-bold text-[#d84e55] hover:text-red-400 transition-colors ml-1"
              >
                Sign in here →
              </Link>
            </p>
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
