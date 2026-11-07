"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { useAppSelector } from "@/store";
import { useGetMeQuery } from "@/store/apiSlice";

const ADMIN_TITLE = "redBus Admin";
const OPERATOR_TITLE = "redBus Operator";
const PASSENGER_TITLE = "redBus - Online Ticket Booking Sleeper or Volvo Buses";

export default function DynamicTitle() {
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);
  const { data: latestUser } = useGetMeQuery(undefined, { skip: !isAuthenticated });
  const activeUser = latestUser || user;
  const pathname = usePathname();

  useEffect(() => {
    const isAdminRole =
      activeUser?.role === "ROLE_ADMIN" ||
      activeUser?.role?.toUpperCase().includes("ADMIN") ||
      activeUser?.roles?.some((r) => r.toUpperCase().includes("ADMIN")) ||
      activeUser?.email?.toLowerCase() === "redbus@admin.in";

    const isAdminRoute = pathname?.startsWith("/admin");

    const isOperatorRole =
      !isAdminRole &&
      (activeUser?.role === "ROLE_OPERATOR" ||
        activeUser?.role?.toUpperCase().includes("OPERATOR") ||
        activeUser?.roles?.some((r) => r.toUpperCase().includes("OPERATOR")));

    const isOperatorRoute = !isAdminRole && pathname?.startsWith("/operator");

    if (isAdminRole || isAdminRoute) {
      document.title = ADMIN_TITLE;
    } else if (isOperatorRole || isOperatorRoute) {
      document.title = OPERATOR_TITLE;
    } else {
      document.title = PASSENGER_TITLE;
    }
  }, [activeUser, pathname]);

  return null;
}


