import type { Metadata } from "next";
import "./globals.css";
import Providers from "@/components/Providers";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import AiChatWidget from "@/components/AiChatWidget";

export const metadata: Metadata = {
  title: "redBus - Online Bus Ticket Booking, Sleeper & Volvo Buses",
  description:
    "Book bus tickets online with redBus. AI-powered search, live seat tracking, instant refunds, and 30,000+ routes across India.",
  keywords: "bus tickets, redbus, online bus booking, sleeper bus, volvo bus, ticket booking, razorpay",
  openGraph: {
    title: "redBus - India's No. 1 Online Bus Ticketing Platform",
    description: "Book bus tickets online with redBus. AI-powered search, live seat tracking, and instant refunds.",
    url: "https://www.redbus.in",
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
    <html lang="en" suppressHydrationWarning>
      <head />
      <body className="min-h-screen flex flex-col bg-[#f7f9fa] antialiased" suppressHydrationWarning>
        <Providers>
          <Navbar />
          <main className="flex-1">{children}</main>
          <Footer />
          <AiChatWidget />
        </Providers>
      </body>
    </html>
  );
}
