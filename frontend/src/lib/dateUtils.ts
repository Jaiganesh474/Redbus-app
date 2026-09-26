/**
 * Real-time Local Date Formatting & Trip Status Tracking Utilities
 * Prevents UTC timezone shift bug (e.g. toISOString() converting IST 2am to previous day UTC)
 */

export function formatLocalDate(date: Date = new Date()): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function getTodayDateStr(): string {
  return formatLocalDate(new Date());
}

export function getTomorrowDateStr(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return formatLocalDate(d);
}

export function getOffsetDateStr(offsetDays: number = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return formatLocalDate(d);
}

export function formatJourneyDisplayDate(dateStr: string): string {
  if (!dateStr) return "";
  try {
    const todayStr = getTodayDateStr();
    const tomorrowStr = getTomorrowDateStr();

    const parts = dateStr.split("-");
    if (parts.length === 3) {
      const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
      const day = d.getDate();
      const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
      const month = monthNames[d.getMonth()];
      const year = d.getFullYear();

      let suffix = "";
      if (dateStr === todayStr) suffix = " (Today)";
      else if (dateStr === tomorrowStr) suffix = " (Tomorrow)";

      return `${day} ${month}, ${year}${suffix}`;
    }
  } catch {}
  return dateStr;
}

export type RealtimeTripStatusType =
  | "CONFIRMED"
  | "TRIP_STARTED"
  | "TRIP_COMPLETED"
  | "CANCELLED"
  | "REFUNDED"
  | "PENDING_PAYMENT"
  | "EXPIRED";

export interface TripStatusInfo {
  status: RealtimeTripStatusType;
  label: string;
  isLive: boolean;
  isCompleted: boolean;
  isUpcoming: boolean;
}

export function parseTimeToMinutes(timeStr: string): number {
  if (!timeStr) return 0;
  const clean = timeStr.trim();
  const isPM = clean.toUpperCase().includes("PM");
  const isAM = clean.toUpperCase().includes("AM");
  const numPart = clean.replace(/[^\d:]/g, "");
  const [hStr, mStr] = numPart.split(":");
  let hours = parseInt(hStr || "0", 10);
  const minutes = parseInt(mStr || "0", 10);
  if (isPM && hours < 12) hours += 12;
  if (isAM && hours === 12) hours = 0;
  return hours * 60 + minutes;
}

/**
 * Calculates real-time status of a trip:
 * - Ongoing/Started (now >= departure and now < arrival): Red live blinking dot ("Trip Started")
 * - Completed (now >= arrival): Green dot symbol ("Trip Completed")
 * - Upcoming (now < departure): Confirmed/Upcoming ("CONFIRMED")
 */
export function getRealtimeTripStatus(
  travelDate: string,
  departureTime: string,
  arrivalTime?: string,
  bookingStatus: string = "CONFIRMED"
): TripStatusInfo {
  if (bookingStatus === "CANCELLED") {
    return { status: "CANCELLED", label: "CANCELLED", isLive: false, isCompleted: false, isUpcoming: false };
  }
  if (bookingStatus === "REFUNDED") {
    return { status: "REFUNDED", label: "REFUNDED", isLive: false, isCompleted: false, isUpcoming: false };
  }
  if (bookingStatus === "PENDING_PAYMENT") {
    return { status: "PENDING_PAYMENT", label: "PENDING PAYMENT", isLive: false, isCompleted: false, isUpcoming: false };
  }
  if (bookingStatus === "EXPIRED") {
    return { status: "EXPIRED", label: "EXPIRED", isLive: false, isCompleted: false, isUpcoming: false };
  }

  try {
    const parts = (travelDate || "").split("-");
    if (parts.length === 3) {
      const year = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10) - 1;
      const day = parseInt(parts[2], 10);

      const depMinutes = parseTimeToMinutes(departureTime);
      const depHour = Math.floor(depMinutes / 60);
      const depMin = depMinutes % 60;

      const depDate = new Date(year, month, day, depHour, depMin, 0);

      let arrDate: Date;
      if (arrivalTime) {
        const arrMinutes = parseTimeToMinutes(arrivalTime);
        const arrHour = Math.floor(arrMinutes / 60);
        const arrMin = arrMinutes % 60;
        const dayOffset = arrMinutes <= depMinutes ? 1 : 0;
        arrDate = new Date(year, month, day + dayOffset, arrHour, arrMin, 0);
      } else {
        // Assume standard intercity journey duration of 6.5 hours if arrival is not specified
        arrDate = new Date(depDate.getTime() + 6.5 * 60 * 60 * 1000);
      }

      const now = new Date();

      if (now < depDate) {
        return { status: "CONFIRMED", label: "CONFIRMED", isLive: false, isCompleted: false, isUpcoming: true };
      } else if (now >= depDate && now < arrDate) {
        return { status: "TRIP_STARTED", label: "Trip Started", isLive: true, isCompleted: false, isUpcoming: false };
      } else {
        return { status: "TRIP_COMPLETED", label: "Trip Completed", isLive: false, isCompleted: true, isUpcoming: false };
      }
    }
  } catch {}

  return { status: "CONFIRMED", label: bookingStatus || "CONFIRMED", isLive: false, isCompleted: false, isUpcoming: true };
}
