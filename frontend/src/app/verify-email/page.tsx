"use client";

import React, { useState, useEffect, Suspense } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { useAppDispatch } from "@/store";
import { setCredentials } from "@/store/authSlice";
import {
  useVerifyEmailMutation,
  useResendVerificationMutation,
} from "@/store/apiSlice";
import {
  Mail,
  KeyRound,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  ArrowRight,
  ShieldCheck,
} from "lucide-react";

function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const dispatch = useAppDispatch();

  const [email, setEmail] = useState("");
  const [token, setToken] = useState("");
  const [status, setStatus] = useState<"idle" | "success" | "error">("idle");
  const [feedbackMessage, setFeedbackMessage] = useState("");
  const [resendCooldown, setResendCooldown] = useState(0);

  const [verifyEmailMutation, { isLoading: isVerifying }] = useVerifyEmailMutation();
  const [resendVerificationMutation, { isLoading: isResending }] = useResendVerificationMutation();

  useEffect(() => {
    const emailParam = searchParams.get("email");
    const tokenParam = searchParams.get("token");

    if (emailParam) setEmail(emailParam);
    if (tokenParam) {
      setToken(tokenParam);
      // Automatically trigger verification if both params present
      if (emailParam) {
        handleAutoVerify(emailParam, tokenParam);
      }
    }
  }, [searchParams]);

  const handleAutoVerify = async (emailToVerify: string, tokenToVerify: string) => {
    try {
      const res = await verifyEmailMutation({
        email: emailToVerify.trim().toLowerCase(),
        token: tokenToVerify.trim(),
      }).unwrap();

      dispatch(setCredentials(res));
      setStatus("success");
      setFeedbackMessage("Your email has been successfully verified! You are now logged in.");
    } catch (err: any) {
      setStatus("error");
      setFeedbackMessage(err?.data?.message || "Invalid or expired verification token.");
    }
  };

  const handleManualSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !token.trim()) {
      setStatus("error");
      setFeedbackMessage("Please provide both your registered email and the 6-digit code.");
      return;
    }

    try {
      const res = await verifyEmailMutation({
        email: email.trim().toLowerCase(),
        token: token.trim(),
      }).unwrap();

      dispatch(setCredentials(res));
      setStatus("success");
      setFeedbackMessage("Your email address has been verified! Welcome to redBus.");
    } catch (err: any) {
      setStatus("error");
      setFeedbackMessage(err?.data?.message || "Verification code is incorrect or has expired.");
    }
  };

  const handleResend = async () => {
    if (!email.trim()) {
      setStatus("error");
      setFeedbackMessage("Please provide your email address to resend the code.");
      return;
    }

    try {
      await resendVerificationMutation({ email: email.trim().toLowerCase() }).unwrap();
      setStatus("idle");
      setFeedbackMessage("A new 6-digit verification code has been dispatched to your email via Brevo.");
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
    } catch (err: any) {
      setStatus("error");
      setFeedbackMessage(err?.data?.message || "Could not resend code. Please try again.");
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <div className="bg-white rounded-3xl shadow-xl border border-gray-100 max-w-md w-full overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-[#d84e55] to-[#ef4444] px-6 py-8 text-white text-center">
          <div className="w-14 h-14 rounded-2xl bg-white/15 flex items-center justify-center mx-auto mb-3 backdrop-blur-xs">
            <ShieldCheck className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl font-black tracking-tight">Email Verification</h1>
          <p className="text-xs text-red-100 mt-1 px-2">
            Confirm your email to enable e-ticket delivery via Brevo and fast passenger checkouts.
          </p>
        </div>

        <div className="p-6 sm:p-8 space-y-5">
          {/* Status Banners */}
          {status === "success" && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl space-y-3 text-center animate-in fade-in">
              <CheckCircle2 className="w-10 h-10 text-emerald-600 mx-auto" />
              <h3 className="text-base font-bold text-emerald-900">Email Verified!</h3>
              <p className="text-xs text-emerald-700">{feedbackMessage}</p>
              <div className="pt-2">
                <Link
                  href="/profile"
                  className="inline-flex items-center space-x-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  <span>Go to My Profile</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            </div>
          )}

          {status === "error" && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-2xl flex items-start space-x-2.5 text-xs text-red-700 animate-in fade-in">
              <AlertCircle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
              <div className="flex-1">
                <span className="font-semibold block mb-0.5">Verification Error</span>
                <span>{feedbackMessage}</span>
              </div>
            </div>
          )}

          {status !== "success" && (
            <form onSubmit={handleManualSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Email Address</label>
                <div className="relative">
                  <Mail className="w-4 h-4 absolute left-3.5 top-3 text-gray-400" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="name@example.com"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:border-transparent"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-medium text-gray-700">6-Digit Verification Code</label>
                  <span className="text-[10px] text-gray-400">Powered by Brevo</span>
                </div>
                <div className="relative">
                  <KeyRound className="w-4 h-4 absolute left-3.5 top-3 text-gray-400" />
                  <input
                    type="text"
                    required
                    maxLength={6}
                    value={token}
                    onChange={(e) => setToken(e.target.value.replace(/[^0-9]/g, ""))}
                    placeholder="e.g. 529104"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-lg font-mono font-bold tracking-widest text-center focus:outline-hidden focus:ring-2 focus:ring-[#d84e55] focus:border-transparent"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isVerifying}
                className="w-full py-3 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl font-bold text-sm shadow-md shadow-red-500/20 transition-all flex items-center justify-center space-x-2 disabled:opacity-70"
              >
                {isVerifying ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <span>Verify Email Address</span>
                )}
              </button>

              <div className="pt-2 flex items-center justify-between">
                <Link
                  href="/"
                  className="text-xs text-gray-500 hover:text-gray-800 transition-colors"
                >
                  ← Return to Home
                </Link>
                <button
                  type="button"
                  disabled={resendCooldown > 0 || isResending}
                  onClick={handleResend}
                  className="text-xs font-semibold text-[#d84e55] hover:underline disabled:opacity-50 flex items-center space-x-1"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${isResending ? "animate-spin" : ""}`} />
                  <span>{resendCooldown > 0 ? `Resend code (${resendCooldown}s)` : "Resend Code via Brevo"}</span>
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-[70vh] flex items-center justify-center">
          <div className="w-8 h-8 border-3 border-[#d84e55] border-t-transparent rounded-full animate-spin" />
        </div>
      }
    >
      <VerifyEmailContent />
    </Suspense>
  );
}
