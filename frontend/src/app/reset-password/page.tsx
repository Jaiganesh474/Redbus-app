"use client";

import React, { useState, useEffect, Suspense } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { useAppDispatch } from "@/store";
import { setCredentials } from "@/store/authSlice";
import {
  useForgotPasswordMutation,
  useResetPasswordMutation,
} from "@/store/apiSlice";
import {
  KeyRound,
  Mail,
  Lock,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  RefreshCw,
  ShieldCheck,
} from "lucide-react";

function ResetPasswordContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const dispatch = useAppDispatch();

  const [step, setStep] = useState<"request" | "reset">("request");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [feedbackMessage, setFeedbackMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [resendCooldown, setResendCooldown] = useState(0);

  const [forgotPasswordMutation, { isLoading: isSendingOtp }] = useForgotPasswordMutation();
  const [resetPasswordMutation, { isLoading: isResetting }] = useResetPasswordMutation();

  useEffect(() => {
    const emailParam = searchParams.get("email");
    const otpParam = searchParams.get("otp");

    if (emailParam) setEmail(emailParam);
    if (otpParam) {
      setOtp(otpParam);
      setStep("reset");
    }
  }, [searchParams]);

  const handleRequestOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setFeedbackMessage("");

    if (!email.trim()) {
      setErrorMessage("Please enter your registered email.");
      return;
    }

    try {
      await forgotPasswordMutation({ email: email.trim().toLowerCase() }).unwrap();
      setFeedbackMessage("A 6-digit password reset OTP has been dispatched to your email via Brevo!");
      setStep("reset");
      startCooldown();
    } catch (err: any) {
      setErrorMessage(err?.data?.message || "Failed to send reset OTP. Please check your email.");
    }
  };

  const startCooldown = () => {
    setResendCooldown(60);
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

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setFeedbackMessage("");

    if (!otp.trim()) {
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
      const res = await resetPasswordMutation({
        email: email.trim().toLowerCase(),
        otp: otp.trim(),
        newPassword,
      }).unwrap();

      dispatch(setCredentials(res));
      setFeedbackMessage("Password reset successfully! You are now logged in.");
      setTimeout(() => {
        router.push("/profile");
      }, 1500);
    } catch (err: any) {
      setErrorMessage(err?.data?.message || "Failed to reset password. OTP may be invalid or expired.");
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <div className="bg-white rounded-3xl shadow-xl border border-gray-100 max-w-md w-full overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-[#d84e55] to-[#ef4444] px-6 py-8 text-white text-center">
          <div className="w-14 h-14 rounded-2xl bg-white/15 flex items-center justify-center mx-auto mb-3 backdrop-blur-xs">
            <KeyRound className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl font-black tracking-tight">Reset Your Password</h1>
          <p className="text-xs text-red-100 mt-1 px-2">
            Secure verification code sent to your registered email via Brevo
          </p>
        </div>

        <div className="p-6 sm:p-8 space-y-5">
          {feedbackMessage && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center space-x-2.5 text-xs text-emerald-800 animate-in fade-in">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
              <span>{feedbackMessage}</span>
            </div>
          )}

          {errorMessage && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-2xl flex items-start space-x-2.5 text-xs text-red-700 animate-in fade-in">
              <AlertCircle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {step === "request" ? (
            <form onSubmit={handleRequestOtp} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Email Address</label>
                <div className="relative">
                  <Mail className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="name@example.com"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isSendingOtp}
                className="w-full py-3 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl font-bold text-sm shadow-md shadow-red-500/20 transition-all flex items-center justify-center space-x-2 disabled:opacity-70 cursor-pointer"
              >
                {isSendingOtp ? (
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <span>Send Reset OTP via Brevo</span>
                )}
              </button>

              <div className="text-center pt-2">
                <Link href="/" className="text-xs text-gray-500 hover:text-gray-800">
                  ← Return to Home
                </Link>
              </div>
            </form>
          ) : (
            <form onSubmit={handleResetPassword} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Email Address</label>
                <div className="relative">
                  <Mail className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm bg-gray-50"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-medium text-gray-700">6-Digit OTP Code</label>
                  <span className="text-[10px] text-gray-400">Sent via Brevo</span>
                </div>
                <div className="relative">
                  <KeyRound className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                  <input
                    type="text"
                    required
                    maxLength={6}
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/[^0-9]/g, ""))}
                    placeholder="e.g. 581902"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-base font-mono font-bold tracking-widest text-center focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">New Password</label>
                <div className="relative">
                  <Lock className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                  <input
                    type="password"
                    required
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="At least 6 characters"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Confirm New Password</label>
                <div className="relative">
                  <Lock className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
                  <input
                    type="password"
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Repeat password"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isResetting}
                className="w-full py-3 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl font-bold text-sm shadow-md shadow-red-500/20 transition-all flex items-center justify-center space-x-2 disabled:opacity-70 cursor-pointer"
              >
                {isResetting ? (
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <span>Reset Password & Log In</span>
                )}
              </button>

              <div className="flex items-center justify-between pt-2">
                <button
                  type="button"
                  onClick={() => setStep("request")}
                  className="text-xs text-gray-500 hover:text-gray-800"
                >
                  ← Resend to another email
                </button>
                <button
                  type="button"
                  disabled={resendCooldown > 0 || isSendingOtp}
                  onClick={handleRequestOtp}
                  className="text-xs font-semibold text-[#d84e55] hover:underline disabled:opacity-50 flex items-center space-x-1"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${isSendingOtp ? "animate-spin" : ""}`} />
                  <span>{resendCooldown > 0 ? `Resend in ${resendCooldown}s` : "Resend OTP"}</span>
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-[70vh] flex items-center justify-center">
          <div className="w-8 h-8 border-3 border-[#d84e55] border-t-transparent rounded-full animate-spin" />
        </div>
      }
    >
      <ResetPasswordContent />
    </Suspense>
  );
}
