/**
 * Real-time Local Date Formatting Utilities
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
