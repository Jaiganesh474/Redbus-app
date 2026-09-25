"use client";

import { useMemo } from "react";
import { useAppSelector } from "@/store";
import {
  useGetMyBookingsQuery,
  useGetAvailableCouponsQuery,
  useGetMeQuery,
} from "@/store/apiSlice";
import type { AppNotification } from "@/types";

export function useRealtimeNotifications() {
  const { isAuthenticated, user } = useAppSelector((state) => state.auth);
  const { readIds, dismissedIds, customNotifications, isOpen } = useAppSelector(
    (state) => state.notification
  );

  const { data: latestUser } = useGetMeQuery(undefined, { skip: !isAuthenticated });
  const activeUser = latestUser || user;

  const { data: bookingsData } = useGetMyBookingsQuery(
    { page: 0, size: 20 },
    { skip: !isAuthenticated, pollingInterval: 15000 }
  );

  const { data: availableCoupons = [] } = useGetAvailableCouponsQuery(undefined, {
    pollingInterval: 60000,
  });

  const notifications = useMemo(() => {
    const list: AppNotification[] = [];
    const todayStr = new Date().toISOString().split("T")[0];

    // 1. If Authenticated User -> Real Bookings & Account Notifications
    if (isAuthenticated && activeUser) {
      const bookings = bookingsData?.content || [];

      // A. Real Bookings Notifications from Database
      bookings.forEach((booking) => {
        if (booking.status === "CONFIRMED") {
          const isToday = booking.travelDate === todayStr;
          const seatNumbers =
            booking.passengers?.map((p) => p.seatNumber).filter(Boolean).join(", ") || "Confirmed";

          if (isToday) {
            list.push({
              id: `booking-live-${booking.pnr}`,
              type: "booking",
              title: "Live Boarding Today! 📍",
              message: `Your bus ${booking.operatorName} (${booking.sourceCity} ➔ ${booking.destinationCity}) departs today at ${booking.departureTime}. Boarding at ${booking.boardingPoint || "Main Terminal"}. PNR: ${booking.pnr}`,
              timestamp: `Today at ${booking.departureTime}`,
              read: readIds.includes(`booking-live-${booking.pnr}`),
              actionUrl: "/my-bookings",
              actionLabel: "View Ticket",
              pnr: booking.pnr,
            });
          } else {
            list.push({
              id: `booking-conf-${booking.pnr}`,
              type: "booking",
              title: "Trip Confirmed! 🚌",
              message: `Booking confirmed for ${booking.sourceCity} ➔ ${booking.destinationCity} on ${booking.travelDate} (${booking.departureTime}) with ${booking.operatorName}. Seat(s): ${seatNumbers}. PNR: ${booking.pnr}`,
              timestamp: booking.travelDate,
              read: readIds.includes(`booking-conf-${booking.pnr}`),
              actionUrl: "/my-bookings",
              actionLabel: "View Ticket",
              pnr: booking.pnr,
            });
          }
        } else if (booking.status === "CANCELLED" || booking.status === "REFUNDED") {
          const refund =
            booking.refundAmount !== undefined && booking.refundAmount > 0
              ? booking.refundAmount
              : booking.totalAmount;
          list.push({
            id: `booking-cancel-${booking.pnr}`,
            type: "wallet",
            title: "Refund Credited to Wallet 💰",
            message: `Booking ${booking.pnr} (${booking.sourceCity} ➔ ${booking.destinationCity}) was cancelled. Refund of ₹${refund.toFixed(2)} credited to your redBus Wallet.`,
            timestamp: booking.travelDate || "Cancelled",
            read: readIds.includes(`booking-cancel-${booking.pnr}`),
            actionUrl: "/profile",
            actionLabel: "Check Wallet",
            pnr: booking.pnr,
          });
        } else if (booking.status === "PENDING_PAYMENT") {
          list.push({
            id: `booking-pending-${booking.pnr}`,
            type: "alert",
            title: "Payment Pending ⏳",
            message: `Complete payment for your ${booking.sourceCity} ➔ ${booking.destinationCity} seats before your reservation lock expires.`,
            timestamp: "Pending",
            read: readIds.includes(`booking-pending-${booking.pnr}`),
            actionUrl: `/checkout?routeId=${booking.routeId || 1}`,
            actionLabel: "Complete Payment",
            pnr: booking.pnr,
          });
        }
      });

      // B. Real Account Alerts (Wallet & Email Verification)
      const walletBalance = Number(activeUser.walletBalance || 0);
      if (walletBalance > 0) {
        list.push({
          id: `alert-wallet-${activeUser.id}`,
          type: "wallet",
          title: "redBus Wallet Active 💰",
          message: `You have ₹${walletBalance.toFixed(2)} available in your redBus Wallet. Ready to use with 1-click on your next bus!`,
          timestamp: "Available",
          read: readIds.includes(`alert-wallet-${activeUser.id}`),
          actionUrl: "/profile",
          actionLabel: "View Wallet",
        });
      }

      if (!activeUser.emailVerified) {
        list.push({
          id: `alert-verify-email-${activeUser.id}`,
          type: "alert",
          title: "Verify Email for PDF Tickets ⚠️",
          message: `Your email (${activeUser.email}) is not verified yet. Verify now to receive instant Brevo e-tickets and PDF receipts.`,
          timestamp: "Action Required",
          read: readIds.includes(`alert-verify-email-${activeUser.id}`),
          actionUrl: `/verify-email?email=${encodeURIComponent(activeUser.email)}`,
          actionLabel: "Verify Now",
        });
      }

      // C. Personalized Welcome if no bookings
      if (bookings.length === 0) {
        list.push({
          id: `alert-welcome-${activeUser.id}`,
          type: "alert",
          title: `Welcome to redBus, ${activeUser.name.split(" ")[0]}! ✨`,
          message: `Your verified account is ready. Explore 30,000+ routes with live seat layouts, zero double bookings, and 24/7 AI trip assistance.`,
          timestamp: "New Member",
          read: readIds.includes(`alert-welcome-${activeUser.id}`),
          actionUrl: "/",
          actionLabel: "Search Buses",
        });
      }
    } else {
      // 2. Non-Logged In Guest User -> Real System Prompts (Zero Mock Bookings)
      list.push({
        id: "guest-login-prompt",
        type: "alert",
        title: "Sign in for Real-Time Trip Alerts 🔔",
        message: "Log in with your phone or email to track your live buses, access confirmed ticket PNRs, and receive instant refund notices.",
        timestamp: "Personalize",
        read: readIds.includes("guest-login-prompt"),
        actionUrl: "/login",
        actionLabel: "Log In / Sign Up",
      });

      list.push({
        id: "guest-welcome",
        type: "alert",
        title: "Welcome to redBus India 🚌",
        message: "Book intercity bus tickets with instant seat selection, GPS tracking, and Gemini AI trip assistance.",
        timestamp: "Official",
        read: readIds.includes("guest-welcome"),
        actionUrl: "/",
        actionLabel: "Book Now",
      });
    }

    // 3. Real Active Database Coupons for Everyone
    availableCoupons.forEach((coupon) => {
      list.push({
        id: `offer-${coupon.code}`,
        type: "offer",
        title: `Save ${coupon.discountPercentage}% with ${coupon.code} 🎉`,
        message: `Use promo code ${coupon.code} at checkout to get ${coupon.discountPercentage}% off (up to ₹${coupon.maxDiscountAmount}) on your bus trip.`,
        timestamp: "Live Coupon",
        read: readIds.includes(`offer-${coupon.code}`),
        actionUrl: "/",
        actionLabel: `Grab ${coupon.code}`,
        discountCode: coupon.code,
      });
    });

    // 4. Append any custom runtime notifications
    customNotifications.forEach((cn) => {
      if (!list.some((item) => item.id === cn.id)) {
        list.unshift({
          ...cn,
          read: readIds.includes(cn.id),
        });
      }
    });

    // 5. Filter out dismissed notifications
    return list.filter((n) => !dismissedIds.includes(n.id));
  }, [
    isAuthenticated,
    activeUser,
    bookingsData,
    availableCoupons,
    readIds,
    dismissedIds,
    customNotifications,
  ]);

  const unreadCount = useMemo(() => {
    return notifications.filter((n) => !n.read).length;
  }, [notifications]);

  return {
    notifications,
    unreadCount,
    isOpen,
    isAuthenticated,
    activeUser,
  };
}
