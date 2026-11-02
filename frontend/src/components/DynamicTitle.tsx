"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { useAppSelector } from "@/store";

const OPERATOR_TITLE = "redBus Operator";
const PASSENGER_TITLE = "redBus - Online Ticket Booking Sleeper or Volvo Buses";

export default function DynamicTitle() {
  const { user } = useAppSelector((state) => state.auth);
  const pathname = usePathname();

  useEffect(() => {
    const isOperatorRole =
      user?.role === "ROLE_OPERATOR" ||
      user?.role?.toUpperCase().includes("OPERATOR") ||
      user?.roles?.some((r) => r.toUpperCase().includes("OPERATOR"));

    const isOperatorRoute = pathname?.startsWith("/operator");

    if (isOperatorRole || isOperatorRoute) {
      document.title = OPERATOR_TITLE;
    } else {
      document.title = PASSENGER_TITLE;
    }
  }, [user, pathname]);

  return null;
}

