import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import type { User } from "@/types";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
}

const getInitialToken = () => {
  if (typeof window !== "undefined") {
    return localStorage.getItem("redbus_token");
  }
  return null;
};

const getInitialUser = () => {
  if (typeof window !== "undefined") {
    const userStr = localStorage.getItem("redbus_user");
    if (userStr) {
      try {
        return JSON.parse(userStr);
      } catch (e) {
        return null;
      }
    }
  }
  return null;
};

const initialState: AuthState = {
  token: getInitialToken(),
  user: getInitialUser(),
  isAuthenticated: !!getInitialToken(),
};

export const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setCredentials: (
      state,
      action: PayloadAction<{ token: string; user: User }>
    ) => {
      state.token = action.payload.token;
      state.user = action.payload.user;
      state.isAuthenticated = true;
      if (typeof window !== "undefined") {
        localStorage.setItem("redbus_token", action.payload.token);
        localStorage.setItem("redbus_user", JSON.stringify(action.payload.user));
      }
    },
    updateUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
      if (typeof window !== "undefined") {
        localStorage.setItem("redbus_user", JSON.stringify(action.payload));
      }
    },
    logout: (state) => {
      state.token = null;
      state.user = null;
      state.isAuthenticated = false;
      if (typeof window !== "undefined") {
        localStorage.removeItem("redbus_token");
        localStorage.removeItem("redbus_user");
      }
    },
  },
});

export const { setCredentials, updateUser, logout } = authSlice.actions;
export default authSlice.reducer;
