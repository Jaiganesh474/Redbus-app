import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type {
  RouteItem,
  SeatLayoutData,
  BookingDetails,
  PaymentOrder,
  NlpParseResult,
  CityPair,
  AdminStats,
  User,
  SavedTraveller,
  Operator,
  OperatorProfile,
  BusResponse,
  ScheduleResponse,
  OperatorBooking,
  OperatorAnalytics,
  AiCityItem,
  AiCityPoints,
  Coupon,
  CreateCouponRequest,
  CouponValidationResponse,
  CancelBookingResponse,
  ReviewDto,
  ReviewSummaryDto,
  CreateReviewRequest,
  AiTelemetryItem,
  AiMonitoringStats,
  UserActivityItem,
  UserActivityStats,
  DelayPrediction,
  DynamicPricePrediction,
  SmartSeatRecommendation,
  AiBusPhoto,
  OperatorPassengerManifest,
  OperatorRefund,
  OperatorAiPriceIntelligence,
  OperatorWalletLedger,
  AdminOperatorEarnings,
  Banner,
  CreateBannerRequest,
  GenerateAiBannerRequest,
  UserDeviceSession,
} from "@/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

export const apiSlice = createApi({
  reducerPath: "api",
  baseQuery: fetchBaseQuery({
    baseUrl: API_BASE_URL,
    prepareHeaders: (headers) => {
      if (typeof window !== "undefined") {
        const token = localStorage.getItem("redbus_token");
        if (token) {
          headers.set("Authorization", `Bearer ${token}`);
        }
      }
      return headers;
    },
  }),
  tagTypes: ["Route", "Seat", "Booking", "Auth", "Admin", "AdminOperators", "Seo", "SavedTraveller", "OperatorBuses", "OperatorSchedules", "OperatorAnalytics", "OperatorProfile", "Coupons", "BusReviews", "AiMonitoring", "UserActivity", "BusPhotos", "Refunds", "OperatorWallet", "Banners", "DeviceSessions"],
  endpoints: (builder) => ({
    // Auth
    login: builder.mutation<{ token: string; user: User }, any>({
      query: (credentials) => ({
        url: "/auth/login",
        method: "POST",
        body: credentials,
      }),
      invalidatesTags: ["Auth"],
    }),
    register: builder.mutation<{ token: string; user: User; message?: string }, any>({
      query: (userData) => ({
        url: "/auth/register",
        method: "POST",
        body: userData,
      }),
      invalidatesTags: ["Auth"],
    }),
    verifyEmail: builder.mutation<{ token: string; user: User }, { email: string; token: string }>({
      query: (data) => ({
        url: "/auth/verify-email",
        method: "POST",
        body: data,
      }),
      invalidatesTags: ["Auth"],
    }),
    resendVerification: builder.mutation<{ message: string }, { email: string }>({
      query: (data) => ({
        url: "/auth/resend-verification",
        method: "POST",
        body: data,
      }),
    }),
    forgotPassword: builder.mutation<{ message: string }, { email: string }>({
      query: (data) => ({
        url: "/auth/forgot-password",
        method: "POST",
        body: data,
      }),
    }),
    resetPassword: builder.mutation<{ token: string; user: User }, { email: string; otp: string; newPassword: string }>({
      query: (data) => ({
        url: "/auth/reset-password",
        method: "POST",
        body: data,
      }),
      invalidatesTags: ["Auth"],
    }),
    firebaseLogin: builder.mutation<{ token: string; user: User }, { idToken: string; email: string; name?: string }>({
      query: (data) => ({
        url: "/auth/firebase-login",
        method: "POST",
        body: data,
      }),
      invalidatesTags: ["Auth"],
    }),
    updateProfile: builder.mutation<
      User,
      { name?: string; phone?: string; currentPassword?: string; newPassword?: string; avatarUrl?: string; gender?: string }
    >({
      query: (data) => ({
        url: "/auth/profile",
        method: "PUT",
        body: data,
      }),
      invalidatesTags: ["Auth"],
    }),
    getSavedTravellers: builder.query<SavedTraveller[], void>({
      query: () => "/auth/saved-travellers",
      providesTags: ["SavedTraveller"],
    }),
    addSavedTraveller: builder.mutation<SavedTraveller, { name: string; age: number; gender: string }>({
      query: (body) => ({
        url: "/auth/saved-travellers",
        method: "POST",
        body,
      }),
      invalidatesTags: ["SavedTraveller"],
    }),
    deleteSavedTraveller: builder.mutation<void, number>({
      query: (id) => ({
        url: `/auth/saved-travellers/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["SavedTraveller"],
    }),
    getMe: builder.query<User, void>({
      query: () => "/auth/me",
      providesTags: ["Auth"],
    }),

    // Operator Marketplace Endpoints
    registerOperator: builder.mutation<
      { token: string; user: User; message?: string },
      {
        companyName: string;
        contactPerson?: string;
        contactName?: string;
        email: string;
        phone: string;
        password: string;
        address?: string;
        city?: string;
        state?: string;
        gstNumber?: string;
      }
    >({
      query: (data) => ({
        url: "/auth/operator/register",
        method: "POST",
        body: {
          ...data,
          contactPerson: data.contactPerson || data.contactName,
          contactName: data.contactPerson || data.contactName,
        },
      }),
      invalidatesTags: ["Auth"],
    }),
    uploadBusImage: builder.mutation<{ imageUrl: string; message?: string }, FormData>({
      query: (formData) => ({
        url: "/operator/buses/upload-image",
        method: "POST",
        body: formData,
      }),
    }),
    getOperatorBuses: builder.query<BusResponse[], void>({
      query: () => "/operator/buses",
      providesTags: ["OperatorBuses"],
    }),
    createBus: builder.mutation<BusResponse, any>({
      query: (body) => ({
        url: "/operator/buses",
        method: "POST",
        body,
      }),
      invalidatesTags: ["OperatorBuses", "OperatorAnalytics", "Route"],
    }),
    updateBus: builder.mutation<BusResponse, { id: number; data: any }>({
      query: ({ id, data }) => ({
        url: `/operator/buses/${id}`,
        method: "PUT",
        body: data,
      }),
      invalidatesTags: ["OperatorBuses", "OperatorAnalytics", "Route"],
    }),
    deleteBus: builder.mutation<void, number>({
      query: (id) => ({
        url: `/operator/buses/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["OperatorBuses", "OperatorAnalytics", "Route", "OperatorSchedules"],
    }),
    getOperatorSchedules: builder.query<ScheduleResponse[], void>({
      query: () => "/operator/schedules",
      providesTags: ["OperatorSchedules"],
    }),
    createSchedule: builder.mutation<ScheduleResponse, any>({
      query: (body) => ({
        url: "/operator/schedules",
        method: "POST",
        body,
      }),
      invalidatesTags: ["OperatorSchedules", "OperatorAnalytics", "Route"],
    }),
    getOperatorAnalytics: builder.query<OperatorAnalytics, void>({
      query: () => "/operator/analytics/overview",
      providesTags: ["OperatorAnalytics"],
    }),
    getOperatorBookings: builder.query<OperatorBooking[], void>({
      query: () => "/operator/bookings",
      providesTags: ["OperatorAnalytics"],
    }),
    getOperatorManifest: builder.query<
      OperatorPassengerManifest[],
      { date?: string; busId?: number; scheduleId?: number }
    >({
      query: (params) => ({
        url: "/operator/manifest",
        params: params || {},
      }),
      providesTags: ["OperatorAnalytics", "Booking"],
    }),
    getOperatorProfile: builder.query<OperatorProfile, void>({
      query: () => "/operator/profile",
      providesTags: ["OperatorProfile"],
    }),
    updateOperatorProfile: builder.mutation<OperatorProfile, Partial<OperatorProfile>>({
      query: (body) => ({
        url: "/operator/profile",
        method: "PUT",
        body,
      }),
      invalidatesTags: ["OperatorProfile", "Auth"],
    }),
    getOperatorRefunds: builder.query<OperatorRefund[], void>({
      query: () => "/operator/refunds",
      providesTags: ["Refunds", "Booking"],
    }),
    approveOperatorRefund: builder.mutation<CancelBookingResponse, { pnr: string }>({
      query: ({ pnr }) => ({
        url: `/operator/refunds/${pnr}/approve`,
        method: "POST",
      }),
      invalidatesTags: ["Refunds", "Booking", "OperatorAnalytics", "OperatorWallet", "Auth"],
    }),
    getOperatorAiPriceIntelligence: builder.query<OperatorAiPriceIntelligence[], void>({
      query: () => "/operator/ai/price-intelligence",
      providesTags: ["OperatorAnalytics"],
    }),
    getOperatorWalletLedger: builder.query<OperatorWalletLedger, void>({
      query: () => "/operator/wallet/ledger",
      providesTags: ["OperatorWallet", "Booking", "Refunds"],
    }),

    // Routes / Search
    searchRoutes: builder.query<
      RouteItem[],
      {
        source: string;
        destination: string;
        date: string;
        busType?: string;
        minPrice?: number;
        maxPrice?: number;
        departureWindow?: string;
        sortBy?: string;
      }
    >({
      query: ({ source, destination, date, busType, minPrice, maxPrice, departureWindow, sortBy }) => {
        const params = new URLSearchParams();
        if (source) params.set("source", source);
        if (destination) params.set("destination", destination);
        if (date) params.set("date", date);
        if (busType) params.set("busType", busType);
        if (minPrice !== undefined && minPrice !== null) params.set("minPrice", minPrice.toString());
        if (maxPrice !== undefined && maxPrice !== null) params.set("maxPrice", maxPrice.toString());
        if (departureWindow && departureWindow !== "ALL") params.set("departureWindow", departureWindow);
        if (sortBy) params.set("sortBy", sortBy);
        return `/routes/search?${params.toString()}`;
      },
      providesTags: ["Route"],
    }),
    getRouteById: builder.query<RouteItem, number>({
      query: (id) => `/routes/${id}`,
      providesTags: (_res, _err, id) => [{ type: "Route", id }],
    }),
    getRouteSeats: builder.query<SeatLayoutData, number>({
      query: (id) => `/routes/${id}/seats`,
      providesTags: (_res, _err, id) => [{ type: "Seat", id }],
    }),
    getPopularRoutes: builder.query<CityPair[], void>({
      query: () => "/routes/popular",
      providesTags: ["Route"],
    }),
    getAvailableCities: builder.query<string[], void>({
      query: () => "/routes/cities",
      providesTags: ["Route"],
    }),

    // Seat Locking
    lockSeats: builder.mutation<
      {
        success: boolean;
        expiresAt?: string;
        lockedSeatIds: number[];
        lockExpiry: string;
        remainingSeconds: number;
      },
      { routeId: number; seatIds: number[]; userId?: number }
    >({
      query: (body) => ({
        url: "/seats/lock",
        method: "POST",
        body,
      }),
      invalidatesTags: (_res, _err, { routeId }) => [{ type: "Seat", id: routeId }],
    }),
    unlockSeats: builder.mutation<{ success: boolean }, { routeId: number; seatIds: number[]; userId?: number }>({
      query: (body) => ({
        url: "/seats/unlock",
        method: "POST",
        body,
      }),
      invalidatesTags: (_res, _err, { routeId }) => [{ type: "Seat", id: routeId }],
    }),

    // Bookings
    createBooking: builder.mutation<BookingDetails, any>({
      query: (body) => ({
        url: "/bookings",
        method: "POST",
        body,
      }),
      invalidatesTags: ["Booking", "Seat", "OperatorAnalytics", "Auth"],
    }),
    getBookingByPnr: builder.query<BookingDetails, string>({
      query: (pnr) => `/bookings/${pnr}`,
      providesTags: (_res, _err, pnr) => [{ type: "Booking", id: pnr }],
    }),
    sendTicketEmail: builder.mutation<{ success: boolean; message: string }, { pnr: string; email?: string }>({
      query: ({ pnr, email }) => ({
        url: `/bookings/${pnr}/send-ticket`,
        method: "POST",
        params: email ? { email } : undefined,
      }),
    }),
    getMyBookings: builder.query<{ content: BookingDetails[]; totalElements: number }, { page?: number; size?: number }>({
      query: ({ page = 0, size = 10 } = {}) => `/bookings/my-bookings?page=${page}&size=${size}`,
      providesTags: ["Booking"],
    }),
    cancelBooking: builder.mutation<CancelBookingResponse, { pnr: string; reason?: string; refundDestination?: "WALLET" | "ORIGINAL_PAYMENT" }>({
      query: ({ pnr, reason, refundDestination }) => ({
        url: `/bookings/${pnr}/cancel`,
        method: "POST",
        body: { reason, refundDestination },
      }),
      invalidatesTags: ["Booking", "Seat", "OperatorAnalytics", "Auth", "Refunds", "OperatorWallet"],
    }),

    // Payments
    createPaymentOrder: builder.mutation<PaymentOrder, { pnr: string }>({
      query: (body) => ({
        url: "/payments/create-order",
        method: "POST",
        body,
      }),
    }),
    verifyPayment: builder.mutation<{ success: boolean; pnr: string }, any>({
      query: (body) => ({
        url: "/payments/verify",
        method: "POST",
        body,
      }),
      invalidatesTags: ["Booking", "Seat", "OperatorAnalytics", "Auth"],
    }),

    // AI Features
    parseNlpQuery: builder.mutation<NlpParseResult, { query: string }>({
      query: (body) => ({
        url: "/ai/parse-query",
        method: "POST",
        body,
      }),
    }),
    chatWithAi: builder.mutation<
      { sessionId: string; reply: string; toolExecuted?: string; toolData?: any; suggestedPrompts?: string[] },
      { sessionId?: string; message: string; userId?: number; sourceCity?: string; destinationCity?: string; travelDate?: string }
    >({
      query: (body) => ({
        url: "/ai/chat",
        method: "POST",
        body,
      }),
    }),
    getAiRecommendations: builder.query<
      { recommendedRoutes: RouteItem[]; reason: string },
      { userId?: number; source?: string; destination?: string }
    >({
      query: (params) => ({
        url: "/ai/recommendations",
        params,
      }),
    }),
    searchCitiesWithAi: builder.query<
      AiCityItem[],
      { query?: string; exclude?: string }
    >({
      query: (params) => ({
        url: "/ai/cities/search",
        params,
      }),
    }),
    getAiCityPoints: builder.query<AiCityPoints, { city: string }>({
      query: ({ city }) => ({
        url: "/ai/cities/points",
        params: { city },
      }),
    }),

    // Admin
    getAdminStats: builder.query<AdminStats, void>({
      query: () => "/admin/stats",
      providesTags: ["Admin"],
    }),
    getAdminBookings: builder.query<{ content: BookingDetails[] }, { page?: number; size?: number }>({
      query: ({ page = 0, size = 15 } = {}) => `/admin/bookings?page=${page}&size=${size}`,
      providesTags: ["Admin"],
    }),
    getAdminOperators: builder.query<Operator[], void>({
      query: () => "/admin/operators",
      providesTags: ["AdminOperators"],
    }),
    verifyOperator: builder.mutation<Operator, number>({
      query: (id) => ({
        url: `/admin/operators/${id}/verify`,
        method: "PUT",
      }),
      invalidatesTags: ["AdminOperators", "Admin"],
    }),
    suspendOperator: builder.mutation<Operator, number>({
      query: (id) => ({
        url: `/admin/operators/${id}/suspend`,
        method: "PUT",
      }),
      invalidatesTags: ["AdminOperators", "Admin"],
    }),
    updateOperatorCommission: builder.mutation<Operator, { id: number; commissionRate: number }>({
      query: ({ id, commissionRate }) => ({
        url: `/admin/operators/${id}/commission`,
        method: "PUT",
        body: { commissionRate },
      }),
      invalidatesTags: ["AdminOperators", "Admin"],
    }),
    getAdminOperatorEarnings: builder.query<AdminOperatorEarnings, void>({
      query: () => "/admin/operator-earnings",
      providesTags: ["Admin", "AdminOperators"],
    }),

    // Public SEO & FAQs
    getPublicFaqs: builder.query<{ question: string; answer: string }[], void>({
      query: () => "/seo/faqs",
    }),

    // Coupons
    getAvailableCoupons: builder.query<Coupon[], { operatorId?: number } | void>({
      query: (params) => ({
        url: "/coupons/available",
        params: params || {},
      }),
      providesTags: ["Coupons"],
    }),
    getOperatorCoupons: builder.query<Coupon[], void>({
      query: () => "/coupons/operator",
      providesTags: ["Coupons"],
    }),
    createCoupon: builder.mutation<Coupon, CreateCouponRequest>({
      query: (body) => ({
        url: "/coupons/operator",
        method: "POST",
        body,
      }),
      invalidatesTags: ["Coupons"],
    }),
    deleteCoupon: builder.mutation<void, number>({
      query: (id) => ({
        url: `/coupons/operator/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Coupons"],
    }),
    validateCoupon: builder.mutation<
      CouponValidationResponse,
      { code: string; bookingAmount: number; operatorId?: number }
    >({
      query: (body) => ({
        url: "/coupons/validate",
        method: "POST",
        body,
      }),
    }),

    // Reviews & Ratings
    getBusReviews: builder.query<ReviewSummaryDto, number>({
      query: (busId) => `/reviews/bus/${busId}`,
      providesTags: (_result, _error, busId) => [{ type: "BusReviews", id: busId }],
    }),
    createReview: builder.mutation<ReviewDto, CreateReviewRequest>({
      query: (body) => ({
        url: "/reviews",
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, arg) => [
        { type: "BusReviews", id: arg.busId },
        "Route",
      ],
    }),

    // ML & User Activity Monitoring
    trackUserActivity: builder.mutation<UserActivityItem, Partial<UserActivityItem>>({
      query: (body) => ({
        url: "/activity/track",
        method: "POST",
        body,
      }),
      invalidatesTags: ["UserActivity"],
    }),
    getBusDelayPrediction: builder.query<
      DelayPrediction,
      { scheduleId?: number; source?: string; destination?: string; departureTime?: string }
    >({
      query: (params) => ({
        url: "/ml/predict-delay",
        params,
      }),
    }),
    getDynamicPriceQuote: builder.query<
      DynamicPricePrediction,
      { scheduleId?: number; source?: string; destination?: string; basePrice?: number }
    >({
      query: (params) => ({
        url: "/ml/dynamic-price",
        params,
      }),
    }),
    getSmartSeatRecommendations: builder.query<
      SmartSeatRecommendation,
      { scheduleId?: number; userId?: number; gender?: string }
    >({
      query: (params) => ({
        url: "/ml/smart-seat-recommendations",
        params,
      }),
    }),
    getAiBusPhotos: builder.query<
      AiBusPhoto[],
      { busType?: string; busName?: string; category?: string }
    >({
      query: (params) => ({
        url: "/ml/bus-photos",
        params,
      }),
      providesTags: ["BusPhotos"],
    }),
    updateBusPhotos: builder.mutation<BusResponse, { busId: number; photoUrls: string }>({
      query: ({ busId, photoUrls }) => ({
        url: `/operator/buses/${busId}/photos`,
        method: "PUT",
        body: { photoUrls },
      }),
      invalidatesTags: ["OperatorBuses", "Route", "BusPhotos"],
    }),
    getAdminAiMonitoring: builder.query<AiMonitoringStats, void>({
      query: () => "/admin/ml/ai-monitoring",
      providesTags: ["AiMonitoring"],
    }),
    getAdminUserActivity: builder.query<UserActivityStats, void>({
      query: () => "/admin/ml/user-activity",
      providesTags: ["UserActivity"],
    }),
    simulateAdminAiQuery: builder.mutation<
      { query: string; latencyMs: number; parsedResult: any; modelUsed: string; confidenceScore: number; safetyStatus: string },
      { query: string }
    >({
      query: (body) => ({
        url: "/admin/ml/simulate-query",
        method: "POST",
        body,
      }),
      invalidatesTags: ["AiMonitoring"],
    }),
    // Banners
    getBanners: builder.query<Banner[], void>({
      query: () => "/banners",
      providesTags: ["Banners"],
    }),
    getAdminBanners: builder.query<Banner[], void>({
      query: () => "/banners/admin",
      providesTags: ["Banners"],
    }),
    createBanner: builder.mutation<Banner, CreateBannerRequest>({
      query: (body) => ({
        url: "/banners/admin",
        method: "POST",
        body,
      }),
      invalidatesTags: ["Banners"],
    }),
    updateBanner: builder.mutation<Banner, { id: number; data: CreateBannerRequest }>({
      query: ({ id, data }) => ({
        url: `/banners/admin/${id}`,
        method: "PUT",
        body: data,
      }),
      invalidatesTags: ["Banners"],
    }),
    deleteBanner: builder.mutation<{ message: string; id: number }, number>({
      query: (id) => ({
        url: `/banners/admin/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Banners"],
    }),
    generateAiBanner: builder.mutation<Banner, GenerateAiBannerRequest>({
      query: (body) => ({
        url: "/banners/admin/ai-generate",
        method: "POST",
        body,
      }),
    }),
    // Device Sessions & Security
    getUserDeviceSessions: builder.query<UserDeviceSession[], void>({
      query: () => "/users/me/sessions",
      providesTags: ["DeviceSessions"],
    }),
    revokeDeviceSession: builder.mutation<{ message: string }, number>({
      query: (sessionId) => ({
        url: `/users/me/sessions/${sessionId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["DeviceSessions"],
    }),
    revokeAllOtherSessions: builder.mutation<{ message: string }, void>({
      query: () => ({
        url: "/users/me/sessions/revoke-others",
        method: "POST",
      }),
      invalidatesTags: ["DeviceSessions"],
    }),
  }),
});

export const {
  useLoginMutation,
  useRegisterMutation,
  useVerifyEmailMutation,
  useResendVerificationMutation,
  useForgotPasswordMutation,
  useResetPasswordMutation,
  useFirebaseLoginMutation,
  useUpdateProfileMutation,
  useGetSavedTravellersQuery,
  useAddSavedTravellerMutation,
  useDeleteSavedTravellerMutation,
  useGetMeQuery,
  useRegisterOperatorMutation,
  useUploadBusImageMutation,
  useGetOperatorProfileQuery,
  useUpdateOperatorProfileMutation,
  useGetOperatorBusesQuery,
  useCreateBusMutation,
  useUpdateBusMutation,
  useDeleteBusMutation,
  useGetOperatorSchedulesQuery,
  useCreateScheduleMutation,
  useGetOperatorAnalyticsQuery,
  useGetOperatorBookingsQuery,
  useGetOperatorManifestQuery,
  useLazyGetOperatorManifestQuery,
  useGetOperatorRefundsQuery,
  useApproveOperatorRefundMutation,
  useGetOperatorAiPriceIntelligenceQuery,
  useGetOperatorWalletLedgerQuery,
  useGetAdminOperatorEarningsQuery,
  useSearchRoutesQuery,
  useGetRouteByIdQuery,
  useGetRouteSeatsQuery,
  useGetPopularRoutesQuery,
  useGetAvailableCitiesQuery,
  useLockSeatsMutation,
  useUnlockSeatsMutation,
  useCreateBookingMutation,
  useGetBookingByPnrQuery,
  useSendTicketEmailMutation,
  useGetMyBookingsQuery,
  useCancelBookingMutation,
  useCreatePaymentOrderMutation,
  useVerifyPaymentMutation,
  useParseNlpQueryMutation,
  useChatWithAiMutation,
  useGetAiRecommendationsQuery,
  useSearchCitiesWithAiQuery,
  useLazySearchCitiesWithAiQuery,
  useGetAiCityPointsQuery,
  useLazyGetAiCityPointsQuery,
  useGetAdminStatsQuery,
  useGetAdminBookingsQuery,
  useGetAdminOperatorsQuery,
  useVerifyOperatorMutation,
  useSuspendOperatorMutation,
  useUpdateOperatorCommissionMutation,
  useGetPublicFaqsQuery,
  useGetAvailableCouponsQuery,
  useGetOperatorCouponsQuery,
  useCreateCouponMutation,
  useDeleteCouponMutation,
  useValidateCouponMutation,
  useGetBusReviewsQuery,
  useCreateReviewMutation,
  useTrackUserActivityMutation,
  useGetBusDelayPredictionQuery,
  useLazyGetBusDelayPredictionQuery,
  useGetDynamicPriceQuoteQuery,
  useLazyGetDynamicPriceQuoteQuery,
  useGetSmartSeatRecommendationsQuery,
  useGetAiBusPhotosQuery,
  useUpdateBusPhotosMutation,
  useGetAdminAiMonitoringQuery,
  useGetAdminUserActivityQuery,
  useSimulateAdminAiQueryMutation,
  useGetBannersQuery,
  useGetAdminBannersQuery,
  useCreateBannerMutation,
  useUpdateBannerMutation,
  useDeleteBannerMutation,
  useGenerateAiBannerMutation,
  useGetUserDeviceSessionsQuery,
  useRevokeDeviceSessionMutation,
  useRevokeAllOtherSessionsMutation,
} = apiSlice;


