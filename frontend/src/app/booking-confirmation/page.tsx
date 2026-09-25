"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import confetti from "canvas-confetti";
import { useGetBookingByPnrQuery, useSendTicketEmailMutation } from "@/store/apiSlice";
import {
  CheckCircle2,
  Download,
  Calendar,
  Clock,
  MapPin,
  Bus,
  ArrowRight,
  Share2,
  ShieldCheck,
  Ticket,
  Mail,
  Wallet,
  Tag,
} from "lucide-react";

function ConfirmationContent() {
  const searchParams = useSearchParams();
  const pnr = searchParams.get("pnr") || "";
  const [emailStatus, setEmailStatus] = useState<string | null>(null);

  const { data: booking, isLoading, isError } = useGetBookingByPnrQuery(pnr, {
    skip: !pnr,
  });
  const [sendTicketEmail, { isLoading: isSendingEmail }] = useSendTicketEmailMutation();

  useEffect(() => {
    if (pnr) {
      try {
        confetti({
          particleCount: 80,
          spread: 70,
          origin: { y: 0.6 },
          colors: ["#d84e55", "#10b981", "#3b82f6", "#f59e0b"],
        });
      } catch (e) {}
    }
  }, [pnr]);

  if (isLoading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center">
        <div className="w-10 h-10 border-3 border-[#d84e55] border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="text-base font-bold text-gray-800">Loading your confirmed booking...</p>
      </div>
    );
  }

  if (isError || !booking) {
    return (
      <div className="max-w-xl mx-auto px-4 py-20 text-center">
        <p className="text-lg font-bold text-gray-800">Booking details could not be retrieved</p>
        <p className="text-xs text-gray-500 mt-1">Please check your PNR number or visit My Bookings.</p>
        <Link
          href="/"
          className="mt-6 inline-block px-6 py-2.5 bg-[#d84e55] text-white rounded-xl font-bold text-xs"
        >
          Return Home
        </Link>
      </div>
    );
  }

  const pdfDownloadUrl = `http://localhost:8080/api/bookings/${booking.pnr}/ticket-pdf`;

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Success Badge */}
      <div className="text-center mb-6">
        <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-3 shadow-md">
          <CheckCircle2 className="w-10 h-10" />
        </div>
        <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
          Booking Confirmed!
        </h1>
        <p className="text-xs sm:text-sm text-gray-500 mt-1">
          Your ticket has been booked successfully and confirmed with the operator.
        </p>
      </div>

      {/* Email Dispatched Notification Banner */}
      <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex flex-col sm:flex-row items-center justify-between gap-3 text-xs shadow-xs">
        <div className="flex items-center space-x-2.5 text-emerald-900">
          <Mail className="w-5 h-5 text-emerald-600 shrink-0" />
          <div>
            <span className="font-bold">E-Ticket Emailed to: </span>
            <span className="font-semibold text-emerald-700">{booking.contactEmail}</span>
            <p className="text-[11px] text-emerald-600">The PDF e-ticket with QR code and journey details has been sent to your inbox.</p>
          </div>
        </div>
        <button
          type="button"
          disabled={isSendingEmail}
          onClick={async () => {
            try {
              const res = await sendTicketEmail({ pnr: booking.pnr }).unwrap();
              setEmailStatus(res.message || "Email resent successfully!");
            } catch (err) {
              setEmailStatus("Failed to resend email.");
            }
          }}
          className="px-3.5 py-1.5 bg-white border border-emerald-300 hover:bg-emerald-100/50 text-emerald-800 rounded-xl font-bold transition-colors shrink-0 disabled:opacity-50"
        >
          {isSendingEmail ? "Sending..." : "Resend Email"}
        </button>
      </div>

      {emailStatus && (
        <div className="mb-6 p-3 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-700 flex items-center space-x-2">
          <CheckCircle2 className="w-4 h-4 text-blue-500 shrink-0" />
          <span>{emailStatus}</span>
        </div>
      )}

      {/* Ticket Card */}
      <div className="bg-white rounded-3xl border border-gray-200 shadow-xl overflow-hidden mb-8">
        {/* Banner Header */}
        <div className="bg-gradient-to-r from-[#d84e55] to-red-600 px-6 py-5 text-white flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <span className="text-[10px] uppercase font-bold tracking-widest text-red-100 block">
              Booking PNR
            </span>
            <span className="font-mono text-xl sm:text-2xl font-black tracking-wide">
              {booking.pnr}
            </span>
          </div>

          <div className="flex items-center space-x-2">
            <a
              href={pdfDownloadUrl}
              target="_blank"
              rel="noreferrer"
              className="px-4 py-2 bg-white text-[#d84e55] hover:bg-red-50 rounded-xl text-xs font-bold transition-all shadow-sm flex items-center space-x-1.5"
            >
              <Download className="w-4 h-4" />
              <span>Download E-Ticket PDF</span>
            </a>
          </div>
        </div>

        {/* Ticket Details Body */}
        <div className="p-6 sm:p-8 space-y-6">
          {/* Route & Timing Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-gray-100 gap-4">
            <div>
              <div className="flex items-center space-x-2 text-xl sm:text-2xl font-black text-gray-900">
                <span>{booking.sourceCity}</span>
                <span className="text-[#d84e55]">➔</span>
                <span>{booking.destinationCity}</span>
              </div>
              <p className="text-xs text-gray-500 font-medium mt-1">
                {booking.operatorName} • {booking.busType}
              </p>
            </div>

            <div className="text-left sm:text-right">
              <span className="text-[10px] text-gray-400 font-bold uppercase block">Travel Date</span>
              <p className="text-base font-bold text-gray-900">{booking.travelDate}</p>
              <p className="text-xs text-gray-500">{booking.departureTime}</p>
            </div>
          </div>

          {/* Boarding / Dropping info */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-4 bg-gray-50 rounded-2xl text-xs">
            <div>
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">
                Boarding Point
              </span>
              <p className="font-semibold text-gray-900">{booking.boardingPoint}</p>
              <p className="text-gray-500 mt-0.5">Please arrive 15 minutes before scheduled departure.</p>
            </div>
            <div>
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">
                Dropping Point
              </span>
              <p className="font-semibold text-gray-900">{booking.droppingPoint}</p>
              <p className="text-gray-500 mt-0.5">Approx. arrival: {booking.arrivalTime}</p>
            </div>
          </div>

          {/* Passengers Table */}
          <div>
            <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-3">
              Passenger Details
            </h4>
            <div className="border border-gray-200 rounded-2xl overflow-hidden">
              <table className="w-full text-left text-xs">
                <thead className="bg-gray-50 border-b border-gray-200 text-gray-500 font-semibold">
                  <tr>
                    <th className="px-4 py-2.5">Seat No</th>
                    <th className="px-4 py-2.5">Passenger Name</th>
                    <th className="px-4 py-2.5">Age</th>
                    <th className="px-4 py-2.5">Gender</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {booking.passengers?.map((p) => (
                    <tr key={p.seatId} className="hover:bg-gray-50/50">
                      <td className="px-4 py-2.5 font-black text-[#d84e55]">{p.seatNumber}</td>
                      <td className="px-4 py-2.5 font-semibold text-gray-900">{p.name}</td>
                      <td className="px-4 py-2.5 text-gray-600">{p.age} yrs</td>
                      <td className="px-4 py-2.5 text-gray-600">{p.gender}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Itemized Financial & Tax Invoice Breakdown */}
          {(() => {
            const walletUsed = Number(booking.walletAmountUsed) || 0;
            const discount = Number(booking.discountAmount) || 0;
            const totalPaid = Number(booking.totalAmount) || 0;
            const passengerCount = booking.passengers?.length || 1;
            const cancelFee = booking.hasFreeCancellation ? (Number(booking.freeCancellationFee) || 21 * passengerCount) : 0;
            const approxBaseFare = totalPaid + walletUsed + discount - cancelFee;
            const isWalletFull = walletUsed > 0 && totalPaid === 0;
            const isSplitPay = walletUsed > 0 && totalPaid > 0;

            return (
              <div className="p-4 bg-gray-50 rounded-2xl border border-gray-200 text-xs space-y-2">
                <span className="font-black text-gray-900 block uppercase tracking-wider text-[11px]">
                  Tax Invoice & Payment Breakdown
                </span>
                <div className="flex justify-between text-gray-600">
                  <span>Base Ticket Price ({passengerCount} seat{passengerCount > 1 ? "s" : ""})</span>
                  <span className="font-semibold text-gray-900">₹{approxBaseFare.toFixed(2)}</span>
                </div>
                {booking.hasFreeCancellation && (
                  <div className="flex justify-between text-emerald-700">
                    <span className="flex items-center gap-1">
                      <ShieldCheck className="w-3.5 h-3.5" /> Free Cancellation Protection
                    </span>
                    <span className="font-bold">+₹{cancelFee.toFixed(2)}</span>
                  </div>
                )}
                {discount > 0 && (
                  <div className="flex justify-between text-emerald-600 font-bold">
                    <span className="flex items-center gap-1">
                      <Tag className="w-3.5 h-3.5" /> Coupon Discount ({booking.couponCode})
                    </span>
                    <span>-₹{discount.toFixed(2)}</span>
                  </div>
                )}
                {walletUsed > 0 && (
                  <div className="flex justify-between text-emerald-600 font-bold">
                    <span className="flex items-center gap-1">
                      <Wallet className="w-3.5 h-3.5" /> redBus Wallet Deducted
                    </span>
                    <span>-₹{walletUsed.toFixed(2)}</span>
                  </div>
                )}
                <div className="flex justify-between text-gray-600">
                  <span>Operator Service Fee & GST</span>
                  <span className="font-bold text-emerald-600">FREE (₹0.00)</span>
                </div>
                <div className="pt-2 border-t border-gray-200 flex justify-between items-center text-sm">
                  <div>
                    <span className="font-black text-gray-900 block">Final Gateway Paid</span>
                    <span className="text-[11px] text-gray-500 font-normal">
                      {isWalletFull
                        ? "100% Paid via redBus Wallet Balance"
                        : isSplitPay
                        ? `Wallet: ₹${walletUsed.toFixed(2)} + Gateway: ₹${totalPaid.toFixed(2)}`
                        : "Online Payment (Razorpay UPI / Cards)"}
                    </span>
                  </div>
                  <span className="font-black text-xl text-[#d84e55]">₹{totalPaid.toFixed(2)}</span>
                </div>
              </div>
            );
          })()}

          {/* Contact & Fare */}
          <div className="pt-4 border-t border-gray-100 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
            <div className="text-xs text-gray-500">
              <span>Contact: {booking.contactPhone} • {booking.contactEmail}</span>
            </div>
            <div className="text-left sm:text-right">
              <span className="text-xs text-gray-400">Total Paid: </span>
              <span className="text-xl font-black text-gray-900">₹{booking.totalAmount.toFixed(2)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Return Journey Card (Cross-sell recommendation) */}
      <div className="bg-gradient-to-r from-red-50 to-orange-50 rounded-3xl p-6 border border-red-100 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold text-[#d84e55] uppercase tracking-wider block">
            Plan Ahead
          </span>
          <h3 className="text-lg font-black text-gray-900">
            Book your return trip: {booking.destinationCity} ➔ {booking.sourceCity}
          </h3>
          <p className="text-xs text-gray-600 mt-0.5">
            Lock in top seats before fares rise for your return journey.
          </p>
        </div>

        <Link
          href={`/search?source=${encodeURIComponent(booking.destinationCity)}&destination=${encodeURIComponent(booking.sourceCity)}`}
          className="px-5 py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold transition-all shadow-sm flex items-center space-x-1.5 shrink-0"
        >
          <span>Search Return Buses</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>
    </div>
  );
}

export default function BookingConfirmationPage() {
  return (
    <Suspense fallback={<div className="p-12 text-center text-sm">Loading confirmation...</div>}>
      <ConfirmationContent />
    </Suspense>
  );
}
