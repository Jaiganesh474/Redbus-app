import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface SearchState {
  sourceCity: string;
  destinationCity: string;
  travelDate: string;
  busType: string;
  departureWindow: string; // 'ALL', 'MORNING', 'AFTERNOON', 'EVENING', 'NIGHT'
  minPrice?: number;
  maxPrice?: number;
  sortBy: string; // 'departure_asc', 'departure_desc', 'price_asc', 'price_desc', 'rating_desc'
}

const getTomorrowDate = () => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().split("T")[0];
};

const initialState: SearchState = {
  sourceCity: "",
  destinationCity: "",
  travelDate: getTomorrowDate(),
  busType: "",
  departureWindow: "ALL",
  minPrice: undefined,
  maxPrice: undefined,
  sortBy: "departure_asc",
};

export const searchSlice = createSlice({
  name: "search",
  initialState,
  reducers: {
    setSearchParams: (
      state,
      action: PayloadAction<{
        sourceCity: string;
        destinationCity: string;
        travelDate?: string;
        busType?: string;
        maxPrice?: number;
        timePreference?: string;
      }>
    ) => {
      state.sourceCity = action.payload.sourceCity;
      state.destinationCity = action.payload.destinationCity;
      if (action.payload.travelDate) state.travelDate = action.payload.travelDate;
      if (action.payload.busType) state.busType = action.payload.busType;
      if (action.payload.maxPrice) state.maxPrice = action.payload.maxPrice;
      if (action.payload.timePreference) state.departureWindow = action.payload.timePreference;
    },
    swapCities: (state) => {
      const temp = state.sourceCity;
      state.sourceCity = state.destinationCity;
      state.destinationCity = temp;
    },
    setBusTypeFilter: (state, action: PayloadAction<string>) => {
      state.busType = action.payload;
    },
    setDepartureWindowFilter: (state, action: PayloadAction<string>) => {
      state.departureWindow = action.payload;
    },
    setPriceRangeFilter: (
      state,
      action: PayloadAction<{ min?: number; max?: number }>
    ) => {
      state.minPrice = action.payload.min;
      state.maxPrice = action.payload.max;
    },
    setSortBy: (state, action: PayloadAction<string>) => {
      state.sortBy = action.payload;
    },
    setSourceCity: (state, action: PayloadAction<string>) => {
      state.sourceCity = action.payload;
    },
    setDestinationCity: (state, action: PayloadAction<string>) => {
      state.destinationCity = action.payload;
    },
    setTravelDate: (state, action: PayloadAction<string>) => {
      state.travelDate = action.payload;
    },
    resetFilters: (state) => {
      state.busType = "";
      state.departureWindow = "ALL";
      state.minPrice = undefined;
      state.maxPrice = undefined;
      state.sortBy = "departure_asc";
    },
  },
});

export const {
  setSearchParams,
  swapCities,
  setSourceCity,
  setDestinationCity,
  setTravelDate,
  setBusTypeFilter,
  setDepartureWindowFilter,
  setPriceRangeFilter,
  setSortBy,
  resetFilters,
} = searchSlice.actions;

export default searchSlice.reducer;
