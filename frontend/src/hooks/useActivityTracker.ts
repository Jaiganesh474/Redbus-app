"use client";

import { useEffect, useCallback } from "react";
import { useAppSelector } from "@/store";
import { useTrackUserActivityMutation } from "@/store/apiSlice";

export function useActivityTracker() {
  const { user } = useAppSelector((state) => state.auth);
  const [trackMutation] = useTrackUserActivityMutation();

  const getSessionId = useCallback(() => {
    if (typeof window === "undefined") return "server-session";
    let sid = sessionStorage.getItem("redbus_session_id");
    if (!sid) {
      sid = "ses_" + Math.random().toString(36).substring(2, 11) + "_" + Date.now().toString(36);
      sessionStorage.setItem("redbus_session_id", sid);
    }
    return sid;
  }, []);

  const trackEvent = useCallback(
    (actionType: string, metadata?: Record<string, any>, routeId?: number, busId?: number, scheduleId?: number) => {
      try {
        const sessionId = getSessionId();
        trackMutation({
          userId: user?.id,
          sessionId,
          actionType,
          routeId,
          busId,
          scheduleId,
          metadataJson: metadata ? JSON.stringify(metadata) : undefined,
          userAgent: typeof navigator !== "undefined" ? navigator.userAgent : "browser",
        });
      } catch (err) {
        // Silent catch for telemetry
      }
    },
    [user, getSessionId, trackMutation]
  );

  return { trackEvent, getSessionId };
}
