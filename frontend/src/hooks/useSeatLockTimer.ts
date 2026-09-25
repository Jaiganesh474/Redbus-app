"use client";

import { useEffect } from "react";
import { useAppDispatch, useAppSelector } from "@/store";
import { tickLockTimer, clearSeatLocks } from "@/store/bookingSlice";
import { useUnlockSeatsMutation } from "@/store/apiSlice";

export function useSeatLockTimer() {
  const dispatch = useAppDispatch();
  const { lockSecondsRemaining, lockedSeatIds, selectedRoute } = useAppSelector(
    (state) => state.booking
  );
  const [unlockSeats] = useUnlockSeatsMutation();

  useEffect(() => {
    if (lockSecondsRemaining <= 0 || lockedSeatIds.length === 0) {
      return;
    }

    const interval = setInterval(() => {
      dispatch(tickLockTimer());
    }, 1000);

    return () => clearInterval(interval);
  }, [lockSecondsRemaining, lockedSeatIds.length, dispatch]);

  // When timer hits zero after being active, release server locks
  useEffect(() => {
    if (lockSecondsRemaining === 0 && lockedSeatIds.length > 0 && selectedRoute) {
      unlockSeats({
        routeId: selectedRoute.id,
        seatIds: lockedSeatIds,
      });
      dispatch(clearSeatLocks());
    }
  }, [lockSecondsRemaining, lockedSeatIds, selectedRoute, unlockSeats, dispatch]);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  return {
    lockSecondsRemaining,
    formattedTime: formatTime(lockSecondsRemaining),
    isLocked: lockSecondsRemaining > 0 && lockedSeatIds.length > 0,
  };
}
