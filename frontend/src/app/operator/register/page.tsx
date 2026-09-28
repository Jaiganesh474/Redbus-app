"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAppDispatch } from "@/store";
import { setCredentials } from "@/store/authSlice";
import {
  useRegisterOperatorMutation,
  useVerifyEmailMutation,
  useResendVerificationMutation,
} from "@/store/apiSlice";
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
  KeyRound,
  RefreshCw,
  Clock,
  ArrowLeft,
  Sparkles,
} from "lucide-react";

function OperatorRegisterContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const dispatch = useAppDispatch();

  // Multi-step states: 'form' | 'verify' | 'pending-approval'
  const [step, setStep] = useState<"form" | "verify" | "pending-approval">("form");

  // Form fields
  const [companyName, setCompanyName] = useState("");
  const [contactName, setContactName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [city, setCity] = useState("");
  const [state, setState] = useState("");
  const [address, setAddress] = useState("");
  const [gstNumber, setGstNumber] = useState("");

  // OTP Verification state
  const [otpToken, setOtpToken] = useState("");
  const [resendCooldown, setResendCooldown] = useState(0);

  // Status feedback
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // RTK Query hooks
  const [registerOperatorMutation, { isLoading: isRegistering }] = useRegisterOperatorMutation();
  const [verifyEmailMutation, { isLoading: isVerifying }] = useVerifyEmailMutation();
  const [resendVerificationMutation, { isLoading: isResending }] = useResendVerificationMutation();

  // Check URL query parameters if user clicked link in email (e.g. ?email=...&token=...)
  useEffect(() => {
    const urlEmail = searchParams?.get("email");
    const urlToken = searchParams?.get("token") || searchParams?.get("otp");
    if (urlEmail) {
      setEmail(urlEmail);
      if (urlToken) {
        setOtpToken(urlToken);
        setStep("verify");
      }
    }
  }, [searchParams]);

  // Resend OTP countdown timer
  useEffect(() => {
    if (resendCooldown > 0) {
      const timer = setTimeout(() => setResendCooldown((prev) => prev - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [resendCooldown]);

  // Submit Step 1: Register Form
  const handleSubmitForm = async (e: React.FormEvent) => {
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

      // Check if email was already verified
      if (res.user && res.user.emailVerified) {
        dispatch(setCredentials(res));
        setStep("pending-approval");
      } else {
        // Requires email verification OTP!
        setSuccessMessage(`A 6-digit verification code has been dispatched to ${email.trim().toLowerCase()}`);
        setStep("verify");
        setResendCooldown(60);
      }
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

  // Submit Step 2: Verify OTP
  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    if (!otpToken.trim() || otpToken.trim().length < 4) {
      setErrorMessage("Please enter the complete 6-digit verification code sent to your email.");
      return;
    }

    try {
      const res = await verifyEmailMutation({
        email: email.trim().toLowerCase(),
        token: otpToken.trim(),
      }).unwrap();

      dispatch(setCredentials(res));
      setSuccessMessage("Business email verified successfully! Your application has been submitted.");
      setStep("pending-approval");
    } catch (err: any) {
      const msg =
        err?.data?.message ||
        err?.data?.error ||
        (typeof err?.data === "string" ? err.data : null) ||
        err?.message ||
        "Invalid or expired verification code. Please try again or request a new code.";
      setErrorMessage(msg);
    }
  };

  // Resend OTP Code
  const handleResendOtp = async () => {
    if (resendCooldown > 0 || !email.trim()) return;
    setErrorMessage("");
    setSuccessMessage("");

    try {
      await resendVerificationMutation({ email: email.trim().toLowerCase() }).unwrap();
      setSuccessMessage("A fresh 6-digit OTP verification code has been sent to your email.");
      setResendCooldown(60);
    } catch (err: any) {
      const msg =
        err?.data?.message ||
        err?.data?.error ||
        (typeof err?.data === "string" ? err.data : null) ||
        "Failed to resend verification email. Please try again.";
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
          {step === "form" && "Register Your Fleet with redBus"}
          {step === "verify" && "Verify Operator Email"}
          {step === "pending-approval" && "Application Received"}
        </h2>
        <p className="mt-2 text-sm text-slate-400">
          {step === "form" && "List your buses, manage schedules, upload high-res bus photos & track bookings directly"}
          {step === "verify" && "Enter the 6-digit verification code sent to your registered business email"}
          {step === "pending-approval" && "Your business email is verified and registration is queued for Admin Approval"}
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-xl z-10 px-4 sm:px-0">
        <div className="bg-slate-900/90 backdrop-blur-md border border-slate-800 py-8 px-6 shadow-2xl rounded-3xl sm:px-10">
          
          {/* Error Message Banner */}
          {errorMessage && (
            <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-2xl flex items-start space-x-3 text-xs text-red-400">
              <AlertCircle className="w-5 h-5 shrink-0 text-red-500 mt-0.5" />
              <div className="flex-1">
                <span className="font-medium">{errorMessage}</span>
              </div>
            </div>
          )}

          {/* Success Message Banner */}
          {successMessage && (
            <div className="mb-6 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-2xl flex items-start space-x-3 text-xs text-emerald-400">
              <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-500 mt-0.5" />
              <span className="font-medium">{successMessage}</span>
            </div>
          )}

          {/* STEP 1: Registration Form */}
          {step === "form" && (
            <form onSubmit={handleSubmitForm} className="space-y-4">
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
                disabled={isRegistering}
                className="w-full py-4 mt-2 bg-gradient-to-r from-[#d84e55] to-red-600 hover:from-red-600 hover:to-red-700 text-white rounded-xl font-bold text-sm shadow-lg shadow-red-600/30 transition-all flex items-center justify-center space-x-2 disabled:opacity-60 cursor-pointer"
              >
                {isRegistering ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    <span>Create Account & Verify Email</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>
          )}

          {/* STEP 2: Email Verification OTP */}
          {step === "verify" && (
            <div className="space-y-6">
              <div className="p-4 bg-slate-950/80 border border-slate-800 rounded-2xl text-center">
                <div className="w-12 h-12 mx-auto mb-3 bg-[#d84e55]/10 border border-[#d84e55]/20 rounded-full flex items-center justify-center text-[#d84e55]">
                  <Mail className="w-6 h-6" />
                </div>
                <h3 className="text-sm font-bold text-white mb-1">Check Your Email Inbox</h3>
                <p className="text-xs text-slate-400">
                  We've sent a 6-digit OTP verification code to:
                </p>
                <div className="inline-block mt-2 px-3 py-1 bg-slate-900 border border-slate-700 rounded-lg text-xs font-bold text-white tracking-wide">
                  {email}
                </div>
              </div>

              <form onSubmit={handleVerifyOtp} className="space-y-5">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2 text-center">
                    Enter 6-Digit Verification Code
                  </label>
                  <div className="relative max-w-xs mx-auto">
                    <KeyRound className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                    <input
                      type="text"
                      maxLength={6}
                      required
                      value={otpToken}
                      onChange={(e) => setOtpToken(e.target.value.replace(/\D/g, ""))}
                      placeholder="123456"
                      className="w-full pl-11 pr-4 py-3 bg-slate-950/90 border border-slate-700 rounded-xl text-center font-mono text-xl tracking-[0.4em] text-white placeholder-slate-600 focus:outline-hidden focus:border-[#d84e55] focus:ring-1 focus:ring-[#d84e55]"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isVerifying || otpToken.length < 4}
                  className="w-full py-4 bg-gradient-to-r from-[#d84e55] to-red-600 hover:from-red-600 hover:to-red-700 text-white rounded-xl font-bold text-sm shadow-lg shadow-red-600/30 transition-all flex items-center justify-center space-x-2 disabled:opacity-60 cursor-pointer"
                >
                  {isVerifying ? (
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <>
                      <ShieldCheck className="w-4 h-4" />
                      <span>Verify Email & Complete Registration</span>
                    </>
                  )}
                </button>
              </form>

              {/* Resend OTP & Back Action */}
              <div className="pt-4 border-t border-slate-800 flex items-center justify-between text-xs">
                <button
                  type="button"
                  onClick={() => setStep("form")}
                  className="text-slate-400 hover:text-white flex items-center gap-1.5 transition-colors cursor-pointer"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Edit Details</span>
                </button>

                <button
                  type="button"
                  onClick={handleResendOtp}
                  disabled={resendCooldown > 0 || isResending}
                  className="text-[#d84e55] hover:text-red-400 font-semibold disabled:text-slate-600 flex items-center gap-1.5 transition-colors cursor-pointer"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${isResending ? "animate-spin" : ""}`} />
                  <span>
                    {resendCooldown > 0 ? `Resend code in ${resendCooldown}s` : "Resend OTP Code"}
                  </span>
                </button>
              </div>
            </div>
          )}

          {/* STEP 3: Pending Admin Approval & Success */}
          {step === "pending-approval" && (
            <div className="space-y-6 text-center py-2">
              <div className="w-16 h-16 mx-auto bg-emerald-500/10 border border-emerald-500/30 rounded-2xl flex items-center justify-center text-emerald-400 shadow-lg shadow-emerald-500/10">
                <CheckCircle2 className="w-8 h-8" />
              </div>

              <div>
                <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-bold rounded-full mb-3">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  Email Verified Successfully
                </div>
                <h3 className="text-xl font-extrabold text-white">
                  Registration Application Submitted
                </h3>
                <p className="mt-2 text-xs text-slate-300 leading-relaxed max-w-md mx-auto">
                  Your operator profile for <strong className="text-white">{companyName || "your fleet"}</strong> has been registered. Our redBus operations & compliance team is reviewing your details.
                </p>
              </div>

              {/* Status breakdown card */}
              <div className="bg-slate-950/80 border border-slate-800 rounded-2xl p-4 text-left space-y-3">
                <div className="flex items-center justify-between text-xs pb-2 border-b border-slate-800/80">
                  <span className="text-slate-400">Business Email:</span>
                  <span className="font-semibold text-white flex items-center gap-1">
                    {email} <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs pb-2 border-b border-slate-800/80">
                  <span className="text-slate-400">Operator Approval:</span>
                  <span className="font-semibold text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded-md flex items-center gap-1">
                    <Clock className="w-3 h-3" /> PENDING ADMIN APPROVAL
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-400">Fleet Console:</span>
                  <span className="font-semibold text-emerald-400">Ready to configure</span>
                </div>
              </div>

              <div className="pt-2">
                <button
                  type="button"
                  onClick={() => router.push("/operator")}
                  className="w-full py-4 bg-gradient-to-r from-[#d84e55] to-red-600 hover:from-red-600 hover:to-red-700 text-white rounded-xl font-bold text-sm shadow-lg shadow-red-600/30 transition-all flex items-center justify-center space-x-2 cursor-pointer"
                >
                  <span>Launch Operator Dashboard</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}

          {/* Login Link */}
          {step === "form" && (
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
          )}
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

export default function OperatorRegisterPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-slate-950 flex items-center justify-center text-white text-xs">
          Loading registration portal...
        </div>
      }
    >
      <OperatorRegisterContent />
    </Suspense>
  );
}
