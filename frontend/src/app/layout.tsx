import type { Metadata } from "next";
import { Plus_Jakarta_Sans } from "next/font/google";
import "./globals.css";
import Providers from "@/components/Providers";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import AiChatWidget from "@/components/AiChatWidget";
import RealtimeNotificationToast from "@/components/RealtimeNotificationToast";
import DynamicTitle from "@/components/DynamicTitle";

// Load Google Font matching Zomato / redBus modern UI design system
const plusJakartaSans = Plus_Jakarta_Sans({
  subsets: ["latin"],
  variable: "--font-zomato",
  display: "swap",
});

export const metadata: Metadata = {
  title: "redBus - Online Ticket Booking Sleeper or Volvo Buses",
  description:
    "Book bus tickets online with redBus. AI-powered search, live seat tracking, instant refunds, and 30,000+ routes across India.",
  keywords: "bus tickets, redbus, online bus booking, sleeper bus, volvo bus, ticket booking, razorpay",
  icons: {
    icon: [
      { url: "/favicon.svg", type: "image/svg+xml" },
    ],
    shortcut: "/favicon.svg",
    apple: "/apple-icon",
  },
  openGraph: {
    title: "redBus - Online Ticket Booking Sleeper or Volvo Buses",
    description: "Book bus tickets online with redBus. AI-powered search, live seat tracking, and instant refunds.",
    url: "https://redbusai.app",
    siteName: "redBus",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="en"
      className={plusJakartaSans.variable}
      suppressHydrationWarning
    >
      <head />
      <body
        className={`min-h-screen flex flex-col bg-[#f7f9fa] antialiased ${plusJakartaSans.className}`}
        suppressHydrationWarning
      >
        <Providers>
          <DynamicTitle />
          <Navbar />
          <RealtimeNotificationToast />
          <main className="flex-1">{children}</main>
          <Footer />
          <AiChatWidget />
        </Providers>
      </body>
    </html>
  );
}
