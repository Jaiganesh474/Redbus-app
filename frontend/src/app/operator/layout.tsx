import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "redBus Operator",
  description: "redBus Operator Portal - Fleet Management, Real-time Passenger Manifest, Dynamic Pricing, and Settlements",
};

export default function OperatorLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
