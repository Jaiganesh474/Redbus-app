export interface User {
  id: number;
  name: string;
  email: string;
  phone?: string;
  role: string;
  roles?: string[];
  emailVerified?: boolean;
  avatarUrl?: string;
  gender?: "MALE" | "FEMALE" | "OTHER";
  operatorStatus?: string;
  walletBalance?: number;
}

export interface OperatorProfile {
  id: number;
  userId: number;
  companyName: string;
  contactPerson: string;
  email: string;
  phone: string;
  kycDocUrl?: string;
  bankAccountRef?: string;
  commissionRate?: number;
  status: string;
  createdAt?: string;
  totalBuses?: number;
  totalSchedules?: number;
}

export interface SavedTraveller {
  id: number;
  name: string;
  age: number;
  gender: "MALE" | "FEMALE" | "OTHER";
}

export interface RouteItem {
  id: number;
  busId: number;
  operatorName: string;
  busType: string;
  rating: number;
  amenities: string[];
  sourceCity: string;
  destinationCity: string;
  departureTime: string;
  arrivalTime: string;
  travelDate: string;
  durationHours: number;
  basePrice: number;
  availableSeats: number;
  boardingPoints: string[];
  droppingPoints: string[];
  busPhotoUrl?: string;
}

export interface SeatItem {
  id: number;
  seatId: number;
  seatNumber: string;
  seatType: "SEATER" | "SLEEPER";
  deck: "LOWER" | "UPPER";
  rowNum: number;
  colNum: number;
  status: "AVAILABLE" | "LOCKED" | "BOOKED";
  genderRestriction: "NONE" | "FEMALE" | "MALE";
  bookedGender?: "MALE" | "FEMALE";
  price: number;
  lockedByUserId?: number | null;
}

export interface SeatLayoutData {
  routeId: number;
  busId: number;
  operatorName: string;
  busType: string;
  basePrice: number;
  seats: SeatItem[];
}

export interface CityPair {
  sourceCity: string;
  destinationCity: string;
  busCount: number;
  minPrice: number;
}

export interface PassengerInput {
  seatId: number;
  seatNumber: string;
  name: string;
  age: number;
  gender: "MALE" | "FEMALE" | "OTHER";
}

export interface BookingDetails {
  id: number;
  pnr: string;
  routeId: number;
  sourceCity: string;
  destinationCity: string;
  travelDate: string;
  departureTime: string;
  arrivalTime: string;
  operatorName: string;
  busType: string;
  totalAmount: number;
  status: "PENDING_PAYMENT" | "CONFIRMED" | "CANCELLED" | "REFUNDED" | "EXPIRED";
  refundStatus?: "REQUESTED" | "AUDIT_PENDING" | "APPROVED" | "REFUNDED";
  refundDestination?: "WALLET" | "ORIGINAL_PAYMENT";
  refundStage?: "REQUESTED" | "OPERATOR_AUDIT" | "REFUND_PROCESSING" | "COMPLETED";
  refundRequestedAt?: string;
  refundApprovedAt?: string;
  boardingPoint: string;
  droppingPoint: string;
  contactEmail: string;
  contactPhone: string;
  couponCode?: string;
  discountAmount?: number;
  hasFreeCancellation?: boolean;
  freeCancellationFee?: number;
  hasTripGuarantee?: boolean;
  tripGuaranteeFee?: number;
  serviceFee?: number;
  walletAmountUsed?: number;
  cancellationReason?: string;
  refundAmount?: number;
  createdAt: string;
  passengers: PassengerInput[];
}

export interface Coupon {
  id: number;
  operatorId?: number | null;
  operatorName?: string;
  code: string;
  title?: string;
  description?: string;
  discountPercentage: number;
  maxDiscountAmount: number;
  minBookingAmount: number;
  active?: boolean;
  isActive?: boolean;
  validFrom?: string;
  validTo?: string;
  expiryDate?: string;
  usageCount?: number;
  timesUsed?: number;
  usageLimit?: number;
  createdAt?: string;
}

export interface CreateCouponRequest {
  code: string;
  title?: string;
  description?: string;
  discountPercentage: number;
  maxDiscountAmount: number;
  minBookingAmount: number;
  validFrom?: string;
  validTo?: string;
  expiryDate?: string;
  usageLimit?: number;
}

export interface CouponValidationResponse {
  valid: boolean;
  code?: string;
  discountPercentage?: number;
  discountAmount?: number;
  finalAmount?: number;
  message?: string;
}

export interface CancelBookingResponse {
  pnr: string;
  status: string;
  refundAmount: number;
  walletBalance?: number;
  message: string;
}

export interface NlpParseResult {
  sourceCity?: string;
  destinationCity?: string;
  travelDate?: string;
  busType?: string;
  maxPrice?: number;
  timePreference?: "MORNING" | "AFTERNOON" | "EVENING" | "NIGHT";
  rawQuery?: string;
}

export interface ChatMessageItem {
  id?: string;
  role: "user" | "assistant" | "system";
  content: string;
  toolExecuted?: string;
  toolData?: any;
  suggestedPrompts?: string[];
  timestamp: string;
}

export interface PaymentOrder {
  orderId: string;
  currency: string;
  amountInPaise: number;
  amount: number;
  keyId: string;
  pnr: string;
}

export interface AdminStats {
  totalBookings: number;
  confirmedBookings: number;
  cancelledBookings: number;
  totalRevenue: number;
  occupancyRate: number;
  totalRoutes: number;
  totalBuses: number;
  topRoutes: CityPair[];
}

// Operator Marketplace Interfaces
export interface Operator {
  id: number;
  userId: number;
  companyName: string;
  contactPerson: string;
  email: string;
  phone: string;
  commissionRate: number;
  status: string;
  createdAt?: string;
  totalBuses?: number;
  totalSchedules?: number;
  totalBookings?: number;
  totalRevenue?: number;
  kycDocUrl?: string;
  bankAccountRef?: string;
}

export interface BusResponse {
  id: number;
  operatorId: number;
  operatorName: string;
  registrationNumber: string;
  busType: string;
  totalSeats: number;
  amenities: string;
  photoUrls?: string;
  active: boolean;
  rating: number;
  createdAt?: string;
}

export interface ScheduleResponse {
  id: number;
  operatorId: number;
  routeId?: number;
  busId: number;
  busName: string;
  busType: string;
  sourceCity: string;
  destinationCity: string;
  departureTime: string;
  arrivalTime: string;
  operatingDays: string;
  basePrice: number;
  validFrom: string;
  validTo: string;
  status: string;
  materializedTripsCount?: number;
  createdAt?: string;
}

export interface OperatorBooking {
  id: number;
  pnr: string;
  passengerName: string;
  passengerEmail: string;
  passengerPhone: string;
  route: string;
  travelDate: string;
  busName: string;
  seatNumbers: string[];
  seatCount: number;
  totalAmount: number;
  commissionAmount: number;
  netAmount: number;
  status: string;
  bookingTime: string;
}

export interface OperatorPassengerManifest {
  bookingId: number;
  pnr: string;
  passengerName: string;
  age: number;
  gender: string;
  seatNumber: string;
  seatType: string;
  deck: string;
  berthLabel: string;
  seatDisplay: string;
  boardingPoint: string;
  droppingPoint: string;
  contactPhone: string;
  contactEmail: string;
  travelDate: string;
  sourceCity: string;
  destinationCity: string;
  departureTime: string;
  arrivalTime: string;
  busOperator: string;
  busRegistration: string;
  busType: string;
  status: string;
  verificationStatus?: string;
}


export interface OperatorAnalytics {
  operatorName: string;
  companyName: string;
  totalRevenue: number;
  netEarnings: number;
  commissionPaid: number;
  ticketsSold: number;
  activeFleetCount: number;
  activeSchedulesCount: number;
  totalTripsRun: number;
  averageOccupancyPercentage: number;
  routePerformance: {
    routeName: string;
    bookingCount: number;
    ticketsSold: number;
    revenue: number;
    occupancyPercentage: number;
  }[];
  busPerformance: {
    registrationNumber: string;
    busType: string;
    tripsCount: number;
    revenue: number;
  }[];
  dailyTimeline: {
    date: string;
    revenue: number;
    ticketsSold: number;
  }[];
  recentBookings: OperatorBooking[];
}

export interface AiCityItem {
  name: string;
  state: string;
  tag: string;
  aliases: string[];
  activeRoutesCount: number;
}

export interface AiCityPoints {
  city: string;
  boardingPoints: string;
  droppingPoints: string;
  majorLandmarks: string[];
}

export interface AppNotification {
  id: string;
  type: "booking" | "offer" | "alert" | "wallet";
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  actionUrl?: string;
  actionLabel?: string;
  pnr?: string;
  discountCode?: string;
}

export interface ReviewDto {
  id: number;
  busId: number;
  userId?: number;
  userName: string;
  rating: number;
  comment?: string;
  tags?: string[];
  createdAt: string;
}

export interface ReviewSummaryDto {
  averageRating: number;
  totalRatings: number;
  starPercentages: Record<number, number>;
  lovedTags: Record<string, number>;
  reviews: ReviewDto[];
}

export interface CreateReviewRequest {
  busId: number;
  rating: number;
  comment?: string;
  userName?: string;
  tags?: string[];
}

export interface AiTelemetryItem {
  id: number;
  sessionId?: string;
  userId?: number;
  requestType: string;
  queryText: string;
  responseSummary: string;
  latencyMs: number;
  tokensUsed: number;
  modelUsed: string;
  confidenceScore: number;
  sentiment: string;
  intent: string;
  isFallback: boolean;
  isAnomaly: boolean;
  safetyFlag: string;
  createdAt: string;
}

export interface AiMonitoringStats {
  totalQueries: number;
  activeToday: number;
  avgLatencyMs: number;
  p50LatencyMs: number;
  p95LatencyMs: number;
  p99LatencyMs: number;
  successRate: number;
  fallbackRate: number;
  avgConfidence: number;
  totalTokensConsumed: number;
  estimatedCostUsd: number;
  anomalyCount: number;
  primaryModel: string;
  intentDistribution: Array<{ intent: string; count: number; pct: number }>;
  sentimentDistribution: Array<{ sentiment: string; count: number; pct: number }>;
  recentLogs: AiTelemetryItem[];
}

export interface UserActivityItem {
  id: number;
  userId?: number;
  userEmail?: string;
  sessionId: string;
  ipAddress: string;
  userAgent?: string;
  actionType: string;
  routeId?: number;
  busId?: number;
  scheduleId?: number;
  metadataJson?: string;
  riskScore: number;
  isBot: boolean;
  createdAt: string;
}

export interface UserActivityStats {
  totalEvents: number;
  activeUsersToday: number;
  botAttemptsBlocked: number;
  botTrafficPercentage: number;
  averageRiskScore: number;
  funnelMetrics: Array<{ stage: string; count: number; conversionPct: number }>;
  topActionsDistribution: Array<{ action: string; count: number }>;
  recentActivities: UserActivityItem[];
  highRiskActivities: UserActivityItem[];
}

export interface DelayPrediction {
  scheduleId?: number;
  routeName: string;
  predictedDepartureDelayMinutes: number;
  predictedArrivalDelayMinutes: number;
  onTimeProbability: number;
  punctualityGrade: "EXCELLENT" | "GOOD" | "MODERATE_RISK";
  trafficCondition: string;
  weatherRisk: string;
  confidenceScore: string;
  aiExplanation: string;
}

export interface DynamicPricePrediction {
  scheduleId?: number;
  sourceCity: string;
  destinationCity: string;
  basePrice: number;
  currentDynamicPrice: number;
  surgeMultiplier: number;
  demandLevel: "NORMAL" | "MODERATE" | "HIGH" | "PEAK_FESTIVE" | "VALUE_SAVER";
  occupancyPercentage: number;
  hoursUntilDeparture: number;
  reason: string;
  priceTrajectory: Array<{ timeLabel: string; price: number; demandIndex: number }>;
}

export interface SmartSeatRecommendation {
  scheduleId?: number;
  recommendedSeats: Array<{
    seatNumber: string;
    matchScore: number;
    badge: string;
    reason: string;
  }>;
  femaleSafeSeatNumbers: string[];
  quietZoneSeatNumbers: string[];
  panoramicWindowSeatNumbers: string[];
}

export interface AiBusPhoto {
  photoUrl: string;
  photoType: "EXTERIOR" | "SLEEPER_CABIN" | "SEATER_ROW" | "COCKPIT" | "AMENITY";
  title: string;
  description: string;
  promptUsed: string;
  qualityScore: number;
}

export interface OperatorRefund {
  bookingId: number;
  pnr: string;
  passengerName: string;
  contactEmail: string;
  contactPhone: string;
  sourceCity: string;
  destinationCity: string;
  travelDate: string;
  busName: string;
  seatNumbers: string[];
  totalPaid: number;
  refundAmount: number;
  refundDestination: "WALLET" | "ORIGINAL_PAYMENT";
  refundStatus: "REQUESTED" | "AUDIT_PENDING" | "APPROVED" | "REFUNDED";
  refundStage: "REQUESTED" | "OPERATOR_AUDIT" | "REFUND_PROCESSING" | "COMPLETED";
  cancellationReason?: string;
  requestedAt?: string;
  approvedAt?: string;
}

export interface CompetitorBenchmark {
  operatorName: string;
  busType: string;
  price: number;
  rating: number;
  differenceFromMe: number;
}

export interface OperatorAiPriceIntelligence {
  corridor: string;
  myRouteId: number;
  myBusType: string;
  myCurrentPrice: number;
  marketAveragePrice: number;
  marketLowestPrice: number;
  marketHighestPrice: number;
  priceDifferencePercentage: number;
  priceCompetitiveness: string;
  aiRecommendation: string;
  suggestedPromoCode: string;
  suggestedPromoDiscount: number;
  predictedDemandOccupancy: number;
  competitorBenchmarks: CompetitorBenchmark[];
}

export interface OperatorWalletTransactionItem {
  id: number;
  pnr?: string;
  type: string;
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export interface OperatorWalletLedger {
  operatorId: number;
  companyName: string;
  currentWalletBalance: number;
  totalEarningsCredited: number;
  totalRefundsDebited: number;
  totalPlatformCommissionPaid: number;
  transactions: OperatorWalletTransactionItem[];
}

export interface OperatorEarningItem {
  operatorId: number;
  companyName: string;
  contactPerson: string;
  email: string;
  phone: string;
  status: string;
  totalBuses: number;
  totalRoutes: number;
  totalConfirmedBookings: number;
  totalCancelledBookings: number;
  grossRevenue: number;
  commissionPaid: number;
  netEarnings: number;
  walletBalance: number;
  totalRefundsApproved: number;
}

export interface AdminOperatorEarnings {
  systemGrossRevenue: number;
  systemNetOperatorPayouts: number;
  systemCommissionsCollected: number;
  systemTotalRefundsProcessed: number;
  operatorEarnings: OperatorEarningItem[];
}

export interface Banner {
  id: number;
  title: string;
  subtitle?: string;
  tag?: string;
  promoCode?: string;
  discountPercentage?: number;
  bgGradient?: string;
  badgeColor?: string;
  routeInfo?: string;
  imageUrl?: string;
  ctaText?: string;
  ctaLink?: string;
  active?: boolean;
  sortOrder?: number;
  isAiGenerated?: boolean;
  promptUsed?: string;
  createdAt?: string;
}

export interface CreateBannerRequest {
  title: string;
  subtitle?: string;
  tag?: string;
  promoCode?: string;
  discountPercentage?: number;
  bgGradient?: string;
  badgeColor?: string;
  routeInfo?: string;
  imageUrl?: string;
  ctaText?: string;
  ctaLink?: string;
  active?: boolean;
  sortOrder?: number;
  isAiGenerated?: boolean;
  promptUsed?: string;
}

export interface GenerateAiBannerRequest {
  prompt: string;
  targetRoute?: string;
  targetDiscount?: number;
}

export interface UserDeviceSession {
  id: number;
  deviceName: string;
  browser: string;
  operatingSystem: string;
  ipAddress: string;
  location: string;
  isCurrent: boolean;
  lastActive: string;
  createdAt: string;
}






