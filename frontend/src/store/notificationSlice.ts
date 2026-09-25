import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import type { AppNotification } from "@/types";

interface NotificationState {
  readIds: string[];
  dismissedIds: string[];
  customNotifications: AppNotification[];
  isOpen: boolean;
}

const getStoredIds = (key: string): string[] => {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

const setStoredIds = (key: string, ids: string[]) => {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(key, JSON.stringify(ids));
  } catch {}
};

const initialState: NotificationState = {
  readIds: getStoredIds("redbus_read_notifs"),
  dismissedIds: getStoredIds("redbus_dismissed_notifs"),
  customNotifications: [],
  isOpen: false,
};

export const notificationSlice = createSlice({
  name: "notification",
  initialState,
  reducers: {
    toggleNotificationDropdown: (state, action: PayloadAction<boolean | undefined>) => {
      state.isOpen = action.payload !== undefined ? action.payload : !state.isOpen;
    },
    addNotification: (state, action: PayloadAction<AppNotification>) => {
      // Add custom runtime notification if not already existing
      if (!state.customNotifications.some((n) => n.id === action.payload.id)) {
        state.customNotifications.unshift(action.payload);
      }
    },
    markAsRead: (state, action: PayloadAction<string>) => {
      if (!state.readIds.includes(action.payload)) {
        state.readIds.push(action.payload);
        setStoredIds("redbus_read_notifs", state.readIds);
      }
    },
    markAllAsRead: (state, action: PayloadAction<string[] | undefined>) => {
      const idsToMark = action.payload || [];
      idsToMark.forEach((id) => {
        if (!state.readIds.includes(id)) {
          state.readIds.push(id);
        }
      });
      setStoredIds("redbus_read_notifs", state.readIds);
    },
    removeNotification: (state, action: PayloadAction<string>) => {
      if (!state.dismissedIds.includes(action.payload)) {
        state.dismissedIds.push(action.payload);
        setStoredIds("redbus_dismissed_notifs", state.dismissedIds);
      }
      state.customNotifications = state.customNotifications.filter((n) => n.id !== action.payload);
    },
    clearAllNotifications: (state, action: PayloadAction<string[] | undefined>) => {
      const idsToClear = action.payload || [];
      idsToClear.forEach((id) => {
        if (!state.dismissedIds.includes(id)) {
          state.dismissedIds.push(id);
        }
      });
      setStoredIds("redbus_dismissed_notifs", state.dismissedIds);
      state.customNotifications = [];
    },
  },
});

export const {
  toggleNotificationDropdown,
  addNotification,
  markAsRead,
  markAllAsRead,
  removeNotification,
  clearAllNotifications,
} = notificationSlice.actions;

export default notificationSlice.reducer;

