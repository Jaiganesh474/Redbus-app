"use client";

import React, { useState, useEffect } from "react";
import {
  X,
  CreditCard,
  QrCode,
  Building2,
  Wallet,
  ShieldCheck,
  Lock,
  ArrowRight,
  ChevronRight,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface RazorpayModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (paymentDetails: { paymentId: string; signature: string }) => Promise<void>;
  amount: number;
  pnr: string;
  contactEmail: string;
  contactPhone: string;
  passengerName?: string;
  orderId?: string;
}

export default function RazorpayModal({
  isOpen,
  onClose,
  onSuccess,
  amount,
  pnr,
  contactEmail,
  contactPhone,
  passengerName = "Passenger",
  orderId = "order_rzp_live",
}: RazorpayModalProps) {
  const [selectedMethod, setSelectedMethod] = useState<"upi" | "card" | "netbanking" | "wallet">("upi");
  const [upiId, setUpiId] = useState("");
  const [cardNumber, setCardNumber] = useState("4532 •••• •••• 8892");
  const [cardExpiry, setCardExpiry] = useState("08/28");
  const [cardCvv, setCardCvv] = useState("•••");
  const [cardHolder, setCardHolder] = useState(passengerName);
  const [selectedBank, setSelectedBank] = useState("HDFC");
  const [selectedWallet, setSelectedWallet] = useState("AmazonPay");
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingStep, setProcessingStep] = useState("");

  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = originalOverflow || "auto";
      };
    }
  }, [isOpen]);

  const handlePay = async () => {
    setIsProcessing(true);
    setProcessingStep("Contacting Razorpay Gateway...");

    setTimeout(() => {
      setProcessingStep("Authorizing transaction with bank...");
    }, 600);

    setTimeout(() => {
      setProcessingStep("Securing 256-bit encryption...");
    }, 1200);

    setTimeout(async () => {
      try {
        const mockPaymentId = "pay_" + Math.random().toString(36).substring(2, 12);
        const mockSignature = "mock_sig_sandbox";
        await onSuccess({ paymentId: mockPaymentId, signature: mockSignature });
      } catch (err) {
        setIsProcessing(false);
      }
    }, 1800);
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 bg-black/75 backdrop-blur-xs"
            onClick={!isProcessing ? onClose : undefined}
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 15 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 15 }}
            transition={{ type: "spring", damping: 25, stiffness: 350 }}
            className="relative w-full max-w-2xl bg-white dark:bg-[#0b1120] rounded-3xl shadow-2xl border border-gray-100 dark:border-slate-800 overflow-hidden flex flex-col md:flex-row max-h-[90vh] z-10"
          >
        
        {/* Left Sidebar: Razorpay Brand & Order Summary */}
        <div className="w-full md:w-5/12 bg-gradient-to-b from-[#0c2340] to-[#08182b] text-white p-6 flex flex-col justify-between relative overflow-hidden">
          <div className="absolute -right-12 -bottom-12 w-48 h-48 bg-blue-500/10 rounded-full blur-2xl pointer-events-none" />
          
          <div>
            {/* Header / Brand */}
            <div className="flex items-center space-x-2.5 mb-6">
              <div className="w-9 h-9 bg-white rounded-xl flex items-center justify-center p-1.5 shadow-md">
                <span className="text-[#d84e55] font-black text-sm tracking-tighter">redBus</span>
              </div>
              <div>
                <h3 className="text-sm font-black tracking-tight text-white flex items-center gap-1.5">
                  <span>redBus India</span>
                  <span className="px-1.5 py-0.2 bg-blue-500/30 text-blue-300 text-[9px] font-bold rounded-md uppercase">Verified</span>
                </h3>
                <p className="text-[11px] text-slate-300 font-medium">Official Ticketing Gateway</p>
              </div>
            </div>

            {/* PNR & Amount Card */}
            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-4 border border-white/10 space-y-3 mb-6">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-300">Booking PNR</span>
                <span className="font-mono font-bold text-amber-300 tracking-wider">{pnr}</span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-300">Passenger</span>
                <span className="font-semibold text-white truncate max-w-[120px]">{passengerName}</span>
              </div>
              <div className="pt-2.5 border-t border-white/10 flex items-baseline justify-between">
                <span className="text-xs font-semibold text-slate-300">Amount Payable</span>
                <span className="text-2xl font-black text-emerald-400">₹{amount.toFixed(2)}</span>
              </div>
            </div>

            {/* Payment Method Selector (Sidebar Tabs) */}
            <div className="space-y-1.5">
              <p className="text-[10px] uppercase tracking-wider text-slate-400 font-bold px-1 mb-1">
                Payment Options
              </p>
              
              <button
                type="button"
                onClick={() => setSelectedMethod("upi")}
                className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${
                  selectedMethod === "upi"
                    ? "bg-blue-600 text-white shadow-md shadow-blue-600/30"
                    : "text-slate-300 hover:bg-white/5 hover:text-white"
                }`}
              >
                <div className="flex items-center space-x-2.5">
                  <QrCode className="w-4 h-4" />
                  <span>UPI / QR Code</span>
                </div>
                <ChevronRight className="w-3.5 h-3.5 opacity-60" />
              </button>

              <button
                type="button"
                onClick={() => setSelectedMethod("card")}
                className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${
                  selectedMethod === "card"
                    ? "bg-blue-600 text-white shadow-md shadow-blue-600/30"
                    : "text-slate-300 hover:bg-white/5 hover:text-white"
                }`}
              >
                <div className="flex items-center space-x-2.5">
                  <CreditCard className="w-4 h-4" />
                  <span>Cards (Credit / Debit)</span>
                </div>
                <ChevronRight className="w-3.5 h-3.5 opacity-60" />
              </button>

              <button
                type="button"
                onClick={() => setSelectedMethod("netbanking")}
                className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${
                  selectedMethod === "netbanking"
                    ? "bg-blue-600 text-white shadow-md shadow-blue-600/30"
                    : "text-slate-300 hover:bg-white/5 hover:text-white"
                }`}
              >
                <div className="flex items-center space-x-2.5">
                  <Building2 className="w-4 h-4" />
                  <span>Net Banking</span>
                </div>
                <ChevronRight className="w-3.5 h-3.5 opacity-60" />
              </button>

              <button
                type="button"
                onClick={() => setSelectedMethod("wallet")}
                className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${
                  selectedMethod === "wallet"
                    ? "bg-blue-600 text-white shadow-md shadow-blue-600/30"
                    : "text-slate-300 hover:bg-white/5 hover:text-white"
                }`}
              >
                <div className="flex items-center space-x-2.5">
                  <Wallet className="w-4 h-4" />
                  <span>Wallets & PayLater</span>
                </div>
                <ChevronRight className="w-3.5 h-3.5 opacity-60" />
              </button>
            </div>
          </div>

          {/* Razorpay Trust Badge */}
          <div className="pt-4 border-t border-white/10 flex items-center justify-between text-[11px] text-slate-300 mt-6">
            <div className="flex items-center space-x-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Razorpay Secured</span>
            </div>
            <span className="font-bold tracking-tight text-blue-300">256-bit SSL</span>
          </div>
        </div>

        {/* Right Content: Active Payment Method Form */}
        <div className="w-full md:w-7/12 p-6 flex flex-col justify-between bg-gray-50/50 dark:bg-slate-900/50">
          <div>
            {/* Modal Close Button */}
            <div className="flex items-center justify-between pb-4 border-b border-gray-200 dark:border-slate-800">
              <div className="flex items-center space-x-2">
                <Lock className="w-4 h-4 text-blue-600" />
                <span className="text-xs font-bold uppercase tracking-wider text-gray-700 dark:text-slate-200">
                  Secure Checkout
                </span>
              </div>
              <button
                type="button"
                onClick={onClose}
                disabled={isProcessing}
                className="p-1.5 rounded-full hover:bg-gray-200 dark:hover:bg-slate-800 text-gray-500 dark:text-slate-400 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* TAB 1: UPI / QR Code */}
            {selectedMethod === "upi" && (
              <div className="py-4 space-y-4">
                <div className="p-4 bg-white dark:bg-slate-800 rounded-2xl border border-gray-200 dark:border-slate-700 text-center space-y-3 shadow-xs">
                  <span className="text-xs font-bold text-gray-800 dark:text-white block">
                    Scan QR Code using any UPI App
                  </span>
                  
                  {/* Visual QR Code Generator */}
                  <div className="mx-auto w-36 h-36 bg-white p-2 rounded-xl border border-gray-200 shadow-inner flex flex-col items-center justify-center relative group">
                    <div className="w-full h-full bg-slate-900 rounded-lg flex flex-col items-center justify-center p-2 text-white text-center">
                      <QrCode className="w-20 h-20 text-white" />
                      <span className="text-[9px] font-black uppercase text-amber-300 tracking-wider mt-1">
                        Scan with GPay/PhonePe
                      </span>
                    </div>
                  </div>

                  {/* UPI Brand Logos */}
                  <div className="flex items-center justify-center gap-2 pt-1">
                    <span className="px-2 py-0.5 bg-blue-50 text-blue-700 font-black text-[10px] rounded-md border border-blue-200">GPay</span>
                    <span className="px-2 py-0.5 bg-purple-50 text-purple-700 font-black text-[10px] rounded-md border border-purple-200">PhonePe</span>
                    <span className="px-2 py-0.5 bg-sky-50 text-sky-700 font-black text-[10px] rounded-md border border-sky-200">Paytm</span>
                    <span className="px-2 py-0.5 bg-emerald-50 text-emerald-700 font-black text-[10px] rounded-md border border-emerald-200">BHIM</span>
                  </div>
                </div>

                {/* Or Enter UPI ID */}
                <div className="space-y-1.5">
                  <label className="block text-xs font-semibold text-gray-700 dark:text-slate-300">
                    Or enter UPI ID / VPA
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      value={upiId}
                      onChange={(e) => setUpiId(e.target.value)}
                      placeholder="e.g. yourname@okaxis, mobile@paytm"
                      className="w-full px-3.5 py-2.5 bg-white dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-xl text-xs font-semibold text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
                    />
                    <button
                      type="button"
                      onClick={() => setUpiId("traveler@okhdfcbank")}
                      className="absolute right-2 top-1.5 px-2 py-1 bg-gray-100 dark:bg-slate-700 text-gray-600 dark:text-slate-300 rounded-md text-[10px] font-bold hover:bg-gray-200 cursor-pointer"
                    >
                      Fill Demo VPA
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* TAB 2: Card Payment */}
            {selectedMethod === "card" && (
              <div className="py-4 space-y-3.5">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-slate-300 mb-1">
                    Card Number
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      value={cardNumber}
                      onChange={(e) => setCardNumber(e.target.value)}
                      placeholder="4532 0000 0000 0000"
                      className="w-full pl-10 pr-3.5 py-2.5 bg-white dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-xl text-xs font-mono font-bold text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
                    />
                    <CreditCard className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-gray-700 dark:text-slate-300 mb-1">
                      Expiry (MM/YY)
                    </label>
                    <input
                      type="text"
                      value={cardExpiry}
                      onChange={(e) => setCardExpiry(e.target.value)}
                      placeholder="MM/YY"
                      className="w-full px-3.5 py-2.5 bg-white dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-xl text-xs font-mono font-bold text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-gray-700 dark:text-slate-300 mb-1">
                      CVV / CVC
                    </label>
                    <input
                      type="password"
                      maxLength={4}
                      value={cardCvv}
                      onChange={(e) => setCardCvv(e.target.value)}
                      placeholder="123"
                      className="w-full px-3.5 py-2.5 bg-white dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-xl text-xs font-mono font-bold text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-slate-300 mb-1">
                    Cardholder Name
                  </label>
                  <input
                    type="text"
                    value={cardHolder}
                    onChange={(e) => setCardHolder(e.target.value)}
                    placeholder="Name on Card"
                    className="w-full px-3.5 py-2.5 bg-white dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-xl text-xs font-semibold text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
                  />
                </div>
              </div>
            )}

            {/* TAB 3: Net Banking */}
            {selectedMethod === "netbanking" && (
              <div className="py-4 space-y-3">
                <label className="block text-xs font-semibold text-gray-700 dark:text-slate-300">
                  Select Popular Bank
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {["HDFC", "SBI", "ICICI", "Axis", "Kotak", "PNB"].map((bank) => (
                    <button
                      key={bank}
                      type="button"
                      onClick={() => setSelectedBank(bank)}
                      className={`p-3 rounded-xl border text-xs font-bold transition-all text-center cursor-pointer ${
                        selectedBank === bank
                          ? "border-blue-600 bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-300 ring-2 ring-blue-500"
                          : "border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-800 dark:text-white hover:border-gray-300"
                      }`}
                    >
                      🏦 {bank} Bank
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* TAB 4: Wallets & PayLater */}
            {selectedMethod === "wallet" && (
              <div className="py-4 space-y-3">
                <label className="block text-xs font-semibold text-gray-700 dark:text-slate-300">
                  Select Digital Wallet
                </label>
                <div className="grid grid-cols-2 gap-2.5">
                  {[
                    { id: "AmazonPay", name: "Amazon Pay", desc: "Fast 1-click checkout" },
                    { id: "Mobikwik", name: "MobiKwik", desc: "SuperCash accepted" },
                    { id: "Airtel", name: "Airtel Money", desc: "Instant cashback" },
                    { id: "Simpl", name: "Simpl PayLater", desc: "Pay next month" },
                  ].map((w) => (
                    <button
                      key={w.id}
                      type="button"
                      onClick={() => setSelectedWallet(w.id)}
                      className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                        selectedWallet === w.id
                          ? "border-blue-600 bg-blue-50 dark:bg-blue-950/60 ring-2 ring-blue-500"
                          : "border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 hover:border-gray-300"
                      }`}
                    >
                      <p className="text-xs font-bold text-gray-900 dark:text-white">{w.name}</p>
                      <p className="text-[10px] text-gray-500 dark:text-slate-400">{w.desc}</p>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Pay Button / Loader */}
          <div className="pt-4 border-t border-gray-200 dark:border-slate-800 space-y-2">
            {isProcessing ? (
              <div className="w-full py-3.5 bg-blue-600 text-white rounded-2xl flex flex-col items-center justify-center space-y-1 shadow-lg shadow-blue-600/30">
                <div className="flex items-center space-x-2">
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span className="text-xs font-bold">Processing Payment...</span>
                </div>
                <span className="text-[10px] text-blue-200 font-medium">{processingStep}</span>
              </div>
            ) : (
              <button
                type="button"
                onClick={handlePay}
                className="w-full py-3.5 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-black text-sm rounded-2xl shadow-lg shadow-blue-600/30 transition-all transform active:scale-98 flex items-center justify-center space-x-2 cursor-pointer"
              >
                <Lock className="w-4 h-4" />
                <span>Pay ₹{amount.toFixed(2)} with Razorpay</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            )}

            <p className="text-[10px] text-center text-gray-400 dark:text-slate-500">
              By clicking Pay, you agree to redBus & Razorpay terms of service.
            </p>
          </div>
        </div>
      </motion.div>
    </div>
    )}
  </AnimatePresence>
  );
}
