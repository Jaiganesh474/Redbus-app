import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import type { RouteItem, SeatItem, PassengerInput } from "@/types";

interface BookingState {
  selectedRoute: RouteItem | null;
  selectedSeats: SeatItem[];
  lockedSeatIds: number[];
  lockExpiry: string | null;
  lockSecondsRemaining: number;
  boardingPoint: string;
  droppingPoint: string;
  passengers: PassengerInput[];
}

const loadInitialState = (): BookingState => {
  if (typeof window !== "undefined") {
    try {
      const saved = sessionStorage.getItem("redbus_booking_state");
      if (saved) {
        return JSON.parse(saved);
      }
    } catch {}
  }
  return {
    selectedRoute: null,
    selectedSeats: [],
    lockedSeatIds: [],
    lockExpiry: null,
    lockSecondsRemaining: 0,
    boardingPoint: "",
    droppingPoint: "",
    passengers: [],
  };
};

const saveToSession = (state: BookingState) => {
  if (typeof window !== "undefined") {
    try {
      sessionStorage.setItem("redbus_booking_state", JSON.stringify(state));
    } catch {}
  }
};

const initialState: BookingState = loadInitialState();

export const bookingSlice = createSlice({
  name: "booking",
  initialState,
  reducers: {
    setSelectedRoute: (state, action: PayloadAction<RouteItem>) => {
      if (state.selectedRoute?.id !== action.payload.id) {
        state.selectedRoute = action.payload;
        state.selectedSeats = [];
        state.lockedSeatIds = [];
        state.lockExpiry = null;
        state.lockSecondsRemaining = 0;
        state.boardingPoint = "";
        state.droppingPoint = "";
        state.passengers = [];
      } else {
        state.selectedRoute = action.payload;
      }
      saveToSession(state);
    },
    prepareCheckout: (
      state,
      action: PayloadAction<{
        route: RouteItem;
        seats: SeatItem[];
        boardingPoint?: string;
        droppingPoint?: string;
      }>
    ) => {
      state.selectedRoute = action.payload.route;
      state.selectedSeats = action.payload.seats;
      state.boardingPoint = action.payload.boardingPoint || "";
      state.droppingPoint = action.payload.droppingPoint || "";
      state.passengers = action.payload.seats.map((seat) => ({
        seatId: seat.seatId,
        seatNumber: seat.seatNumber,
        name: "",
        age: 25,
        gender: seat.genderRestriction === "FEMALE" ? "FEMALE" : "MALE",
      }));
      saveToSession(state);
    },
    toggleSeatSelection: (state, action: PayloadAction<SeatItem>) => {
      const seat = action.payload;
      const index = state.selectedSeats.findIndex((s) => s.seatId === seat.seatId);

      if (index >= 0) {
        state.selectedSeats.splice(index, 1);
        state.passengers = state.passengers.filter((p) => p.seatId !== seat.seatId);
      } else {
        if (state.selectedSeats.length >= 6) {
          return; // max 6 seats per booking
        }
        state.selectedSeats.push(seat);
        state.passengers.push({
          seatId: seat.seatId,
          seatNumber: seat.seatNumber,
          name: "",
          age: 25,
          gender: seat.genderRestriction === "FEMALE" ? "FEMALE" : "MALE",
        });
      }
      saveToSession(state);
    },
    setLockDetails: (
      state,
      action: PayloadAction<{
        lockedSeatIds: number[];
        lockExpiry: string;
        remainingSeconds: number;
      }>
    ) => {
      state.lockedSeatIds = action.payload.lockedSeatIds;
      state.lockExpiry = action.payload.lockExpiry;
      state.lockSecondsRemaining = action.payload.remainingSeconds;
      saveToSession(state);
    },
    tickLockTimer: (state) => {
      if (state.lockSecondsRemaining > 0) {
        state.lockSecondsRemaining -= 1;
      } else {
        // Timer expired, clear selected seats
        state.lockedSeatIds = [];
        state.selectedSeats = [];
        state.lockExpiry = null;
      }
      saveToSession(state);
    },
    clearSeatLocks: (state) => {
      state.lockedSeatIds = [];
      state.selectedSeats = [];
      state.lockExpiry = null;
      state.lockSecondsRemaining = 0;
      state.passengers = [];
      saveToSession(state);
    },
    setBoardingPoint: (state, action: PayloadAction<string>) => {
      state.boardingPoint = action.payload;
      saveToSession(state);
    },
    setDroppingPoint: (state, action: PayloadAction<string>) => {
      state.droppingPoint = action.payload;
      saveToSession(state);
    },
    updatePassengerDetails: (
      state,
      action: PayloadAction<{ index: number; passenger: Partial<PassengerInput> }>
    ) => {
      const { index, passenger } = action.payload;
      if (state.passengers[index]) {
        state.passengers[index] = { ...state.passengers[index], ...passenger };
      } else {
        state.passengers[index] = {
          seatId: passenger.seatId || 0,
          seatNumber: passenger.seatNumber || "",
          name: passenger.name || "",
          age: passenger.age || 25,
          gender: passenger.gender || "MALE",
          ...passenger,
        };
      }
      saveToSession(state);
    },
    syncPassengers: (state, action: PayloadAction<PassengerInput[]>) => {
      state.passengers = action.payload;
      saveToSession(state);
    },
    hydrateBookingState: (state, action: PayloadAction<Partial<BookingState>>) => {
      Object.assign(state, action.payload);
      saveToSession(state);
    },
    ensureDefaultPassenger: (state, action: PayloadAction<{ route?: RouteItem | null; userName?: string; userGender?: string }>) => {
      if (state.selectedSeats.length === 0) {
        const defaultSeat: SeatItem = {
          id: 1,
          seatId: 1,
          seatNumber: "1A",
          seatType: "SEATER",
          deck: "LOWER",
          rowNum: 1,
          colNum: 1,
          genderRestriction: "NONE",
          status: "AVAILABLE",
          price: action.payload.route?.basePrice || 750,
        };
        state.selectedSeats = [defaultSeat];
      }
      if (state.passengers.length === 0) {
        state.passengers = state.selectedSeats.map((seat, idx) => ({
          seatId: seat.seatId,
          seatNumber: seat.seatNumber,
          name: idx === 0 && action.payload.userName ? action.payload.userName : "",
          age: 25,
          gender: (action.payload.userGender as any) || "MALE",
        }));
      }
      saveToSession(state);
    },
    resetBookingState: (state) => {
      state.selectedRoute = null;
      state.selectedSeats = [];
      state.lockedSeatIds = [];
      state.lockExpiry = null;
      state.lockSecondsRemaining = 0;
      state.boardingPoint = "";
      state.droppingPoint = "";
      state.passengers = [];
      saveToSession(state);
    },
  },
});

export const {
  setSelectedRoute,
  prepareCheckout,
  toggleSeatSelection,
  setLockDetails,
  tickLockTimer,
  clearSeatLocks,
  setBoardingPoint,
  setDroppingPoint,
  updatePassengerDetails,
  syncPassengers,
  hydrateBookingState,
  ensureDefaultPassenger,
  resetBookingState,
} = bookingSlice.actions;

export default bookingSlice.reducer;
