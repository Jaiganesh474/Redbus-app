import type { Metadata } from "next";
import { Plus_Jakarta_Sans, Poppins, Outfit } from "next/font/google";
import "./globals.css";
import Providers from "@/components/Providers";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import AiChatWidget from "@/components/AiChatWidget";

// Load Google Fonts matching Zomato / redBus modern UI design system
const plusJakartaSans = Plus_Jakarta_Sans({
  subsets: ["latin"],
  weight: ["300", "400", "500", "600", "700", "800"],
  variable: "--font-zomato",
  display: "swap",
});

const poppins = Poppins({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-poppins",
  display: "swap",
});

const outfit = Outfit({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-outfit",
  display: "swap",
});

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
    <html
      lang="en"
      className={`${plusJakartaSans.variable} ${poppins.variable} ${outfit.variable}`}
      suppressHydrationWarning
    >
      <head />
      <body
        className={`min-h-screen flex flex-col bg-[#f7f9fa] antialiased ${plusJakartaSans.className}`}
        suppressHydrationWarning
      >
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
