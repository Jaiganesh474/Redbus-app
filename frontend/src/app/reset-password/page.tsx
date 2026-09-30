"use client";

import React, { useState, useEffect, Suspense } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { useAppDispatch } from "@/store";
import { setCredentials } from "@/store/authSlice";
import {
  useForgotPasswordMutation,
  useResetPasswordMutation,
  useSendMobileOtpMutation,
  useResetPasswordWithMobileOtpMutation,
} from "@/store/apiSlice";
import {
  KeyRound,
  Mail,
  Lock,
  Phone,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  RefreshCw,
  ShieldCheck,
  Smartphone,
  ArrowLeft,
} from "lucide-react";

function ResetPasswordContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const dispatch = useAppDispatch();

  const [method, setMethod] = useState<"mobile" | "email">("mobile");
  const [step, setStep] = useState<"request" | "reset">("request");
  const [phone, setPhone] = useState("");
  const [countryCode, setCountryCode] = useState("+91");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [feedbackMessage, setFeedbackMessage] = useState("");
  const [previewOtp, setPreviewOtp] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [resendCooldown, setResendCooldown] = useState(0);

  const [forgotPasswordMutation, { isLoading: isSendingEmailOtp }] = useForgotPasswordMutation();
  const [resetPasswordMutation, { isLoading: isResettingEmail }] = useResetPasswordMutation();
  const [sendMobileOtpMutation, { isLoading: isSendingMobileOtp }] = useSendMobileOtpMutation();
  const [resetPasswordWithMobileOtpMutation, { isLoading: isResettingMobile }] = useResetPasswordWithMobileOtpMutation();

  const isSendingOtp = isSendingEmailOtp || isSendingMobileOtp;
  const isResetting = isResettingEmail || isResettingMobile;

  const getFullPhone = () => {
    const raw = phone.trim();
    if (raw.startsWith("+")) return raw;
    const cleanNum = raw.replace(/^0+/, "");
    return `${countryCode}${cleanNum}`;
  };

  useEffect(() => {
    const emailParam = searchParams.get("email");
    const phoneParam = searchParams.get("phone");
    const otpParam = searchParams.get("otp");

    if (phoneParam) {
      setPhone(phoneParam);
      setMethod("mobile");
    } else if (emailParam) {
      setEmail(emailParam);
      setMethod("email");
    }

    if (otpParam) {
      setOtp(otpParam);
      setStep("reset");
    }
  }, [searchParams]);

  const startCooldown = (seconds = 60) => {
    setResendCooldown(seconds);
    const interval = setInterval(() => {
      setResendCooldown((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleRequestOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setFeedbackMessage("");
    setPreviewOtp(null);

    if (method === "mobile") {
      const fullPhone = getFullPhone();
      if (!phone.trim() || phone.replace(/\D/g, "").length < 7) {
        setErrorMessage("Please enter a valid 10-digit mobile number.");
        return;
      }

      try {
        const res = await sendMobileOtpMutation({
          phone: fullPhone,
          purpose: "RESET_PASSWORD",
        }).unwrap();

        setFeedbackMessage(res.message || `Password reset OTP has been sent via SMS to ${fullPhone}`);
        if (res.previewOtp) {
          setPreviewOtp(res.previewOtp);
        }
        setStep("reset");
        startCooldown(60);
      } catch (err: any) {
        setErrorMessage(
          err?.data?.message || err?.data?.error || "Failed to send reset OTP. Please ensure your mobile number is registered."
        );
      }
    } else {
      if (!email.trim()) {
        setErrorMessage("Please enter your registered email.");
        return;
      }

      try {
        await forgotPasswordMutation({ email: email.trim().toLowerCase() }).unwrap();
        setFeedbackMessage("A 6-digit password reset OTP has been dispatched to your email!");
        setStep("reset");
        startCooldown(60);
      } catch (err: any) {
        setErrorMessage(err?.data?.message || "Failed to send reset OTP. Please check your email.");
      }
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setFeedbackMessage("");

    if (!otp.trim() || otp.trim().length !== 6) {
      setErrorMessage("Please enter the 6-digit OTP code.");
      return;
    }

    if (newPassword.length < 6) {
      setErrorMessage("New password must be at least 6 characters long.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage("Passwords do not match.");
      return;
    }

    try {
      let res;
      if (method === "mobile") {
        res = await resetPasswordWithMobileOtpMutation({
          phone: getFullPhone(),
          otp: otp.trim(),
          newPassword,
        }).unwrap();
      } else {
        res = await resetPasswordMutation({
          email: email.trim().toLowerCase(),
          otp: otp.trim(),
          newPassword,
        }).unwrap();
      }

      dispatch(setCredentials(res));
      setFeedbackMessage("Password reset successfully! You are now logged in.");
      setTimeout(() => {
        router.push("/profile");
      }, 1500);
    } catch (err: any) {
      setErrorMessage(err?.data?.message || err?.data?.error || "Failed to reset password. OTP may be invalid or expired.");
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-xl border border-gray-100 dark:border-slate-800 max-w-md w-full overflow-hidden text-gray-900 dark:text-white">
        {/* Header */}
        <div className="bg-gradient-to-r from-[#d84e55] to-[#ef4444] px-6 py-8 text-white text-center">
          <div className="w-14 h-14 rounded-2xl bg-white/15 flex items-center justify-center mx-auto mb-3 backdrop-blur-xs">
            <KeyRound className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl font-black tracking-tight">Reset Your Password</h1>
          <p className="text-xs text-red-100 mt-1 px-2">
            {step === "request"
              ? "Choose mobile SMS or email to receive your secure verification code"
              : "Enter your 6-digit OTP and set your new password"}
          </p>

          {/* Method Selection Tabs (only on request step) */}
          {step === "request" && (
            <div className="flex bg-black/20 p-1 rounded-xl mt-4 max-w-xs mx-auto">
              <button
                type="button"
                onClick={() => {
                  setMethod("mobile");
                  setErrorMessage("");
                  setFeedbackMessage("");
                }}
                className={`flex-1 py-1.5 px-2 text-xs font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                  method === "mobile" ? "bg-white text-[#d84e55] shadow-xs" : "text-white/80 hover:text-white"
                }`}
              >
                <Smartphone className="w-3.5 h-3.5" />
                <span>Mobile SMS</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setMethod("email");
                  setErrorMessage("");
                  setFeedbackMessage("");
                }}
                className={`flex-1 py-1.5 px-2 text-xs font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                  method === "email" ? "bg-white text-[#d84e55] shadow-xs" : "text-white/80 hover:text-white"
                }`}
              >
                <Mail className="w-3.5 h-3.5" />
                <span>Email OTP</span>
              </button>
            </div>
          )}
        </div>

        <div className="p-6 sm:p-8 space-y-5">
          {feedbackMessage && (
            <div className="p-4 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/60 rounded-2xl flex items-start space-x-2.5 text-xs text-emerald-800 dark:text-emerald-300 animate-in fade-in">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
              <div className="flex-1">
                <span>{feedbackMessage}</span>
                {previewOtp && (
                  <div className="mt-1 font-mono font-bold text-emerald-900 dark:text-emerald-200 bg-emerald-100/90 dark:bg-emerald-900/60 px-2 py-1 rounded inline-block">
                    Verification Code: {previewOtp}
                  </div>
                )}
              </div>
            </div>
          )}

          {errorMessage && (
            <div className="p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800/60 rounded-2xl flex items-start space-x-2.5 text-xs text-red-700 dark:text-red-300 animate-in fade-in">
              <AlertCircle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {step === "request" ? (
            /* STEP 1: REQUEST OTP */
            <form onSubmit={handleRequestOtp} className="space-y-4">
              {method === "mobile" ? (
                <div>
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                    Registered Mobile Number
                  </label>
                  <div className="flex rounded-2xl border border-gray-200 dark:border-slate-700 overflow-hidden focus-within:ring-2 focus-within:ring-[#d84e55] bg-white dark:bg-slate-800">
                    <select
                      value={countryCode}
                      onChange={(e) => setCountryCode(e.target.value)}
                      className="bg-gray-50 dark:bg-slate-800 border-r border-gray-200 dark:border-slate-700 text-xs px-3 py-3 font-semibold text-gray-700 dark:text-slate-200 focus:outline-hidden cursor-pointer"
                    >
                      <option value="+91" className="bg-white dark:bg-slate-800 text-gray-900 dark:text-white">🇮🇳 +91</option>
                      <option value="+1" className="bg-white dark:bg-slate-800 text-gray-900 dark:text-white">🇺🇸 +1</option>
                      <option value="+44" className="bg-white dark:bg-slate-800 text-gray-900 dark:text-white">🇬🇧 +44</option>
                      <option value="+971" className="bg-white dark:bg-slate-800 text-gray-900 dark:text-white">🇦🇪 +971</option>
                    </select>
                    <div className="relative flex-1">
                      <Phone className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400 dark:text-slate-500" />
                      <input
                        type="tel"
                        required
                        value={phone}
                        onChange={(e) => setPhone(e.target.value.replace(/[^\d]/g, ""))}
                        placeholder="98765 43210"
                        className="w-full pl-10 pr-4 py-3 text-sm focus:outline-hidden font-medium bg-transparent text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-500"
                      />
                    </div>
                  </div>
                  <p className="text-[11px] text-gray-500 dark:text-slate-400 mt-1.5 flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" /> A free 6-digit OTP will be sent to your phone
                  </p>
                </div>
              ) : (
                <div>
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                    Registered Email Address
                  </label>
                  <div className="relative">
                    <Mail className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400 dark:text-slate-500" />
                    <input
                      type="email"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="e.g. traveler@example.com"
                      className="w-full pl-10 pr-4 py-3 rounded-2xl border border-gray-200 dark:border-slate-700 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] bg-white dark:bg-slate-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-500"
                    />
                  </div>
                </div>
              )}

              <button
                type="submit"
                disabled={isSendingOtp}
                className="w-full py-3.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-2xl font-bold text-sm shadow-md shadow-red-500/20 transition-all flex items-center justify-center space-x-2 disabled:opacity-70 cursor-pointer"
              >
                {isSendingOtp ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    <span>Send Reset OTP</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>
          ) : (
            /* STEP 2: VERIFY OTP AND SET NEW PASSWORD */
            <form onSubmit={handleResetPassword} className="space-y-4">
              <div className="flex items-center justify-between pb-1">
                <span className="text-xs text-gray-500 dark:text-slate-400">
                  Target: <span className="font-semibold text-gray-800 dark:text-slate-200">{method === "mobile" ? getFullPhone() : email}</span>
                </span>
                <button
                  type="button"
                  onClick={() => {
                    setStep("request");
                    setOtp("");
                    setPreviewOtp(null);
                  }}
                  className="text-xs font-semibold text-[#d84e55] dark:text-red-400 hover:underline cursor-pointer flex items-center gap-1"
                >
                  <ArrowLeft className="w-3 h-3" /> Change
                </button>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  6-Digit OTP Code
                </label>
                <div className="relative">
                  <KeyRound className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400 dark:text-slate-500" />
                  <input
                    type="text"
                    required
                    maxLength={6}
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/[^0-9]/g, ""))}
                    placeholder="e.g. 123456"
                    className="w-full pl-10 pr-4 py-3 rounded-2xl border border-gray-200 dark:border-slate-700 text-base tracking-widest font-mono font-bold text-gray-900 dark:text-white bg-white dark:bg-slate-800 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] text-center placeholder-gray-400 dark:placeholder-slate-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  New Password
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400 dark:text-slate-500" />
                  <input
                    type="password"
                    required
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Minimum 6 characters"
                    className="w-full pl-10 pr-4 py-3 rounded-2xl border border-gray-200 dark:border-slate-700 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] bg-white dark:bg-slate-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                  Confirm New Password
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400 dark:text-slate-500" />
                  <input
                    type="password"
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Re-type new password"
                    className="w-full pl-10 pr-4 py-3 rounded-2xl border border-gray-200 dark:border-slate-700 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] bg-white dark:bg-slate-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-500"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isResetting}
                className="w-full py-3.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-2xl font-bold text-sm shadow-md shadow-red-500/20 transition-all flex items-center justify-center space-x-2 disabled:opacity-70 cursor-pointer"
              >
                {isResetting ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <span>Reset & Save Password</span>
                )}
              </button>

              <div className="flex items-center justify-between pt-1">
                <button
                  type="button"
                  onClick={() => setStep("request")}
                  className="text-xs text-gray-500 dark:text-slate-400 hover:text-gray-800 dark:hover:text-white cursor-pointer"
                >
                  ← Back to Request
                </button>
                <button
                  type="button"
                  disabled={resendCooldown > 0 || isSendingOtp}
                  onClick={handleRequestOtp}
                  className="text-xs font-semibold text-[#d84e55] dark:text-red-400 hover:underline disabled:opacity-50 flex items-center gap-1 cursor-pointer"
                >
                  <RefreshCw className={`w-3 h-3 ${isSendingOtp ? "animate-spin" : ""}`} />
                  {resendCooldown > 0 ? `Resend OTP in ${resendCooldown}s` : "Resend OTP"}
                </button>
              </div>
            </form>
          )}

          <div className="text-center pt-2 border-t border-gray-100 dark:border-slate-800">
            <Link href="/" className="text-xs font-semibold text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white transition-colors">
              ← Return to Home
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="min-h-[80vh] flex items-center justify-center">Loading...</div>}>
      <ResetPasswordContent />
    </Suspense>
  );
}
