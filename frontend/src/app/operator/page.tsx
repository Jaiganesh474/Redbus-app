"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useAppSelector } from "@/store";
import {
  useGetOperatorAnalyticsQuery,
  useGetOperatorBusesQuery,
  useGetOperatorSchedulesQuery,
  useGetOperatorCouponsQuery,
  useGetAvailableCouponsQuery,
  useGetOperatorProfileQuery,
  useUpdateOperatorProfileMutation,
  useCreateBusMutation,
  useUpdateBusMutation,
  useDeleteBusMutation,
  useCreateScheduleMutation,
  useCreateCouponMutation,
  useDeleteCouponMutation,
  useUploadBusImageMutation,
  useLazyGetAiCityPointsQuery,
  useGetAiBusPhotosQuery,
  useUpdateBusPhotosMutation,
} from "@/store/apiSlice";
import AiCityDropdown from "@/components/AiCityDropdown";
import BusImageSlider from "@/components/BusImageSlider";
import AiBusStudioModal from "@/components/AiBusStudioModal";
import { AI_BUS_THEMES, getNextAiBusTheme, getRandomAiBusTheme, synthesizeAiBusSuite, AiBusTheme } from "@/data/aiBusPhotoSuites";
import {
  Bus,
  TrendingUp,
  Ticket,
  Users,
  Calendar,
  DollarSign,
  Plus,
  ShieldCheck,
  CheckCircle2,
  Clock,
  MapPin,
  ArrowRight,
  Activity,
  Layers,
  Sparkles,
  BarChart3,
  Percent,
  X,
  AlertCircle,
  Eye,
  UploadCloud,
  Image as ImageIcon,
  Tag,
  Gift,
  Trash2,
  Pencil,
  Camera,
  Wand2,
  Sliders,
  Building2,
  CreditCard,
  User as UserIcon,
  Mail,
  Phone,
  Save,
  Edit3,
  FileCheck,
} from "lucide-react";

export default function OperatorPortalPage() {
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);
  const [activeTab, setActiveTab] = useState<"analytics" | "fleet" | "schedules" | "coupons" | "profile">("analytics");

  // Modals
  const [isAddBusOpen, setIsAddBusOpen] = useState(false);
  const [isAddScheduleOpen, setIsAddScheduleOpen] = useState(false);
  const [isAddCouponOpen, setIsAddCouponOpen] = useState(false);

  // Form states - Add Coupon
  const [couponCode, setCouponCode] = useState("");
  const [couponTitle, setCouponTitle] = useState("");
  const [couponDescription, setCouponDescription] = useState("");
  const [couponDiscountPercentage, setCouponDiscountPercentage] = useState(2);
  const [couponMaxDiscountAmount, setCouponMaxDiscountAmount] = useState(100);
  const [couponMinBookingAmount, setCouponMinBookingAmount] = useState(200);
  const [couponExpiryDate, setCouponExpiryDate] = useState(
    new Date(Date.now() + 60 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]
  );

  // Form states - Add Bus
  const [busOperatorName, setBusOperatorName] = useState("");
  const [busRegistrationNumber, setBusRegistrationNumber] = useState("");
  const [busType, setBusType] = useState("AC Sleeper (2+1)");
  const [busTotalSeats, setBusTotalSeats] = useState(30);
  const [busAmenities, setBusAmenities] = useState("WiFi,Charging Point,Water Bottle,Blanket,Live Tracking,Emergency Exit");
  const [busPhotoUrl, setBusPhotoUrl] = useState("");
  const [selectedAiThemeName, setSelectedAiThemeName] = useState("");

  // Form states - Edit Bus Modal
  const [isEditBusOpen, setIsEditBusOpen] = useState(false);
  const [editBusId, setEditingBusId] = useState<number | null>(null);
  const [editOperatorName, setEditOperatorName] = useState("");
  const [editRegistrationNumber, setEditRegistrationNumber] = useState("");
  const [editBusType, setEditBusType] = useState("AC Sleeper (2+1)");
  const [editTotalSeats, setEditTotalSeats] = useState(30);
  const [editAmenities, setEditAmenities] = useState("");
  const [editPhotoUrl, setEditPhotoUrl] = useState("");

  // Existing Bus AI Photo Studio Modal State
  const [editingBus, setEditingBus] = useState<any | null>(null);
  const [editingBusPhotos, setEditingBusPhotos] = useState("");
  const [isPhotoStudioOpen, setIsPhotoStudioOpen] = useState(false);

  // Dedicated AI Bus Studio Engine & Loading States
  const [isAiStudioModalOpen, setIsAiStudioModalOpen] = useState(false);
  const [aiStudioContext, setAiStudioContext] = useState<"NEW_BUS" | "EDIT_BUS" | "EXISTING_BUS">("NEW_BUS");
  const [isInlineAiGenerating, setIsInlineAiGenerating] = useState(false);
  const [inlineAiStep, setInlineAiStep] = useState("Synthesizing multi-angle bus prompts...");
  const [inlineAiProgress, setInlineAiProgress] = useState(0);

  const handleQuickAiGenerate = (themeId?: string) => {
    setIsInlineAiGenerating(true);
    setInlineAiProgress(15);
    setInlineAiStep("Analyzing bus aerodynamic profile & layout...");

    const targetTheme = themeId ? AI_BUS_THEMES.find((t) => t.id === themeId) : null;
    const styleName = targetTheme ? targetTheme.name : "Volvo 9600 Crimson Flagship";

    setTimeout(() => {
      setInlineAiProgress(45);
      setInlineAiStep("Rendering 8K multi-axle coach exterior & LED illumination...");
    }, 400);

    setTimeout(() => {
      setInlineAiProgress(75);
      const isSleeper = busType.toLowerCase().includes("sleeper");
      setInlineAiStep(isSleeper ? "Synthesizing 2+1 private sleeper berths & ambient lighting..." : "Synthesizing 2+2 plush reclining passenger seats...");
    }, 850);

    setTimeout(() => {
      setInlineAiProgress(90);
      setInlineAiStep("Calibrating cockpit telematics, AC louvers & fast charging ports...");
    }, 1250);

    setTimeout(() => {
      const suite = synthesizeAiBusSuite({
        operatorName: busOperatorName || "Express Coach",
        busType: busType,
        modelStyle: styleName,
      });
      setBusPhotoUrl(suite.joinedUrls);
      setSelectedAiThemeName(suite.name);
      setInlineAiProgress(100);
      setIsInlineAiGenerating(false);
    }, 1600);
  };

  // Form states - Add Schedule
  const [scheduleBusId, setScheduleBusId] = useState<number | "">("");
  const [sourceCity, setSourceCity] = useState("Bangalore");
  const [destinationCity, setDestinationCity] = useState("Chennai");
  const [departureTime, setDepartureTime] = useState("21:30");
  const [arrivalTime, setArrivalTime] = useState("05:30");
  const [basePrice, setBasePrice] = useState(850);
  const [operatingDays, setOperatingDays] = useState("DAILY");
  const [validFrom, setValidFrom] = useState(new Date().toISOString().split("T")[0]);
  const [validTo, setValidTo] = useState(
    new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]
  );
  const [boardingPoints, setBoardingPoints] = useState("Madiwala (21:00), Silk Board (21:15), Electronic City (21:30)");
  const [droppingPoints, setDroppingPoints] = useState("Koyambedu (04:30), Guindy (05:00), Tambaram (05:30)");

  // Mutation and Query Hooks
  const {
    data: analytics,
    isLoading: isLoadingAnalytics,
    refetch: refetchAnalytics,
  } = useGetOperatorAnalyticsQuery(undefined, {
    skip: !isAuthenticated,
    pollingInterval: 10000, // Poll every 10s for live booking updates
  });

  const {
    data: buses = [],
    isLoading: isLoadingBuses,
    refetch: refetchBuses,
  } = useGetOperatorBusesQuery(undefined, { skip: !isAuthenticated });

  const {
    data: schedules = [],
    isLoading: isLoadingSchedules,
    refetch: refetchSchedules,
  } = useGetOperatorSchedulesQuery(undefined, { skip: !isAuthenticated });

  const {
    data: operatorCoupons = [],
    isLoading: isLoadingCoupons,
    refetch: refetchCoupons,
  } = useGetOperatorCouponsQuery(undefined);

  const {
    data: availableCoupons = [],
    refetch: refetchAvailableCoupons,
  } = useGetAvailableCouponsQuery();

  const displayCoupons = React.useMemo(() => {
    const map = new Map<string, any>();
    if (Array.isArray(operatorCoupons)) {
      operatorCoupons.forEach((c) => map.set(c.code, c));
    }
    if (Array.isArray(availableCoupons)) {
      availableCoupons.forEach((c) => {
        if (!map.has(c.code)) map.set(c.code, c);
      });
    }
    return Array.from(map.values());
  }, [operatorCoupons, availableCoupons]);

  const [createBusMutation, { isLoading: isCreatingBus }] = useCreateBusMutation();
  const {
    data: operatorProfile,
    isLoading: isLoadingProfile,
    refetch: refetchProfile,
  } = useGetOperatorProfileQuery(undefined, { skip: !isAuthenticated });

  const [updateOperatorProfileMutation, { isLoading: isUpdatingProfile }] = useUpdateOperatorProfileMutation();

  // Profile Edit form states
  const [profileCompanyName, setProfileCompanyName] = useState("");
  const [profileContactPerson, setProfileContactPerson] = useState("");
  const [profilePhone, setProfilePhone] = useState("");
  const [profileEmail, setProfileEmail] = useState("");
  const [profileBankAccountRef, setProfileBankAccountRef] = useState("");
  const [profileKycDocUrl, setProfileKycDocUrl] = useState("");
  const [isProfileFormInitialized, setIsProfileFormInitialized] = useState(false);

  React.useEffect(() => {
    if (operatorProfile && !isProfileFormInitialized) {
      setProfileCompanyName(operatorProfile.companyName || "");
      setProfileContactPerson(operatorProfile.contactPerson || "");
      setProfilePhone(operatorProfile.phone || "");
      setProfileEmail(operatorProfile.email || "");
      setProfileBankAccountRef(operatorProfile.bankAccountRef || "");
      setProfileKycDocUrl(operatorProfile.kycDocUrl || "");
      setIsProfileFormInitialized(true);
    }
  }, [operatorProfile, isProfileFormInitialized]);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError("");
    setFormSuccess("");

    if (!profileCompanyName.trim()) {
      setFormError("Company / Agency name cannot be empty.");
      return;
    }
    if (!profileContactPerson.trim()) {
      setFormError("Contact person name is required.");
      return;
    }

    try {
      await updateOperatorProfileMutation({
        companyName: profileCompanyName.trim(),
        contactPerson: profileContactPerson.trim(),
        phone: profilePhone.trim(),
        email: profileEmail.trim(),
        bankAccountRef: profileBankAccountRef.trim(),
        kycDocUrl: profileKycDocUrl.trim(),
      }).unwrap();

      setFormSuccess("Operator profile & business details updated successfully!");
      refetchProfile();
      refetchAnalytics();
      setTimeout(() => setFormSuccess(""), 5000);
    } catch (err: any) {
      setFormError(err?.data?.message || "Failed to update profile. Please try again.");
    }
  };

  const [updateBusMutation, { isLoading: isUpdatingBus }] = useUpdateBusMutation();
  const [deleteBusMutation, { isLoading: isDeletingBus }] = useDeleteBusMutation();
  const [createScheduleMutation, { isLoading: isCreatingSchedule }] = useCreateScheduleMutation();
  const [createCouponMutation, { isLoading: isCreatingCoupon }] = useCreateCouponMutation();
  const [deleteCouponMutation, { isLoading: isDeletingCoupon }] = useDeleteCouponMutation();
  const [uploadBusImageMutation, { isLoading: isUploadingImage }] = useUploadBusImageMutation();
  const [updateBusPhotosMutation, { isLoading: isUpdatingBusPhotos }] = useUpdateBusPhotosMutation();
  const [triggerGetCityPoints] = useLazyGetAiCityPointsQuery();
  const [isAiBoardingLoading, setIsAiBoardingLoading] = useState(false);
  const [isAiDroppingLoading, setIsAiDroppingLoading] = useState(false);

  const [formSuccess, setFormSuccess] = useState("");
  const [formError, setFormError] = useState("");

  const handleCreateCoupon = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError("");
    setFormSuccess("");

    if (!couponCode.trim()) {
      setFormError("Coupon code is required.");
      return;
    }

    try {
      await createCouponMutation({
        code: couponCode.trim().toUpperCase(),
        title: couponTitle.trim() || `${couponDiscountPercentage}% Off Promo`,
        description: couponDescription.trim() || `Get instant ${couponDiscountPercentage}% off on all tickets`,
        discountPercentage: Number(couponDiscountPercentage),
        maxDiscountAmount: Number(couponMaxDiscountAmount),
        minBookingAmount: Number(couponMinBookingAmount),
        expiryDate: couponExpiryDate || undefined,
      }).unwrap();

      setFormSuccess(`Coupon ${couponCode.toUpperCase()} created and live instantly for passengers!`);
      setIsAddCouponOpen(false);
      setCouponCode("");
      setCouponTitle("");
      setCouponDescription("");
      refetchCoupons();
      refetchAvailableCoupons();
      setTimeout(() => setFormSuccess(""), 6000);
    } catch (err: any) {
      setFormError(err?.data?.message || "Failed to create coupon.");
    }
  };

  const handleCreateQuick2PercentCoupon = async () => {
    setFormError("");
    setFormSuccess("");
    const randomSuffix = Math.floor(10 + Math.random() * 90);
    const code = `OP2SAVE${randomSuffix}`;

    try {
      await createCouponMutation({
        code,
        title: "Instant 2% Operator Discount",
        description: "Special 2% instant discount on bus bookings",
        discountPercentage: 2,
        maxDiscountAmount: 150,
        minBookingAmount: 100,
        expiryDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
      }).unwrap();

      setFormSuccess(`Live coupon ${code} (2% Instant OFF) activated and available for passengers immediately!`);
      refetchCoupons();
      refetchAvailableCoupons();
      setTimeout(() => setFormSuccess(""), 6000);
    } catch (err: any) {
      setFormError(err?.data?.message || "Failed to create quick 2% coupon.");
    }
  };

  const handleDeleteCoupon = async (id: number, code: string) => {
    if (!confirm(`Are you sure you want to deactivate coupon ${code}?`)) return;
    try {
      await deleteCouponMutation(id).unwrap();
      setFormSuccess(`Coupon ${code} deleted.`);
      refetchCoupons();
      refetchAvailableCoupons();
      setTimeout(() => setFormSuccess(""), 4000);
    } catch (err: any) {
      setFormError(err?.data?.message || "Failed to delete coupon.");
    }
  };

  const handleSourceCityChange = async (city: string) => {
    setSourceCity(city);
    if (!city) return;
    try {
      setIsAiBoardingLoading(true);
      const res = await triggerGetCityPoints({ city }).unwrap();
      if (res && res.boardingPoints) {
        setBoardingPoints(res.boardingPoints);
      }
    } catch (err) {
      console.error("AI boarding points fetch failed:", err);
    } finally {
      setIsAiBoardingLoading(false);
    }
  };

  const handleDestinationCityChange = async (city: string) => {
    setDestinationCity(city);
    if (!city) return;
    try {
      setIsAiDroppingLoading(true);
      const res = await triggerGetCityPoints({ city }).unwrap();
      if (res && res.droppingPoints) {
        setDroppingPoints(res.droppingPoints);
      }
    } catch (err) {
      console.error("AI dropping points fetch failed:", err);
    } finally {
      setIsAiDroppingLoading(false);
    }
  };

  // Automatically select the first bus when buses list is loaded
  React.useEffect(() => {
    if (buses.length > 0 && !scheduleBusId) {
      setScheduleBusId(buses[0].id);
    }
  }, [buses, scheduleBusId]);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setFormError("Please upload a valid image file (PNG, JPG, JPEG, WEBP).");
      return;
    }

    setFormError("");
    const formData = new FormData();
    formData.append("image", file);

    try {
      const res = await uploadBusImageMutation(formData).unwrap();
      if (res.imageUrl) {
        setBusPhotoUrl(res.imageUrl);
      }
    } catch (err: any) {
      setFormError(err?.data?.message || err?.message || "Failed to upload image. Please try again.");
    }
  };

  const handleCreateBus = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError("");
    setFormSuccess("");

    try {
      await createBusMutation({
        operatorName: busOperatorName.trim() || (user ? `${user.name} Travels` : "Express Travels"),
        registrationNumber: busRegistrationNumber.trim() || `KA-01-EXP-${Math.floor(1000 + Math.random() * 9000)}`,
        busType,
        totalSeats: Number(busTotalSeats) || 30,
        amenities: busAmenities,
        photoUrls: busPhotoUrl || undefined,
      }).unwrap();

      setFormSuccess("Bus successfully added to your fleet with custom seat layout!");
      setIsAddBusOpen(false);
      setBusPhotoUrl("");
      refetchBuses();
      refetchAnalytics();
      setTimeout(() => setFormSuccess(""), 4000);
    } catch (err: any) {
      setFormError(err?.data?.message || "Failed to create bus. Please check details.");
    }
  };

  const handleOpenEditBus = (bus: any) => {
    setEditingBusId(bus.id);
    setEditOperatorName(bus.operatorName || "");
    setEditRegistrationNumber(bus.registrationNumber || "");
    setEditBusType(bus.busType || "AC Sleeper (2+1)");
    setEditTotalSeats(bus.totalSeats || 30);
    setEditAmenities(bus.amenities || "WiFi,Charging Point,Water Bottle,Blanket,Live Tracking,Emergency Exit");
    setEditPhotoUrl(bus.photoUrls || "");
    setIsEditBusOpen(true);
  };

  const handleUpdateBus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editBusId) return;
    setFormError("");
    setFormSuccess("");

    try {
      await updateBusMutation({
        id: editBusId,
        data: {
          operatorName: editOperatorName.trim() || (user ? `${user.name} Travels` : "Express Travels"),
          registrationNumber: editRegistrationNumber.trim(),
          busType: editBusType,
          totalSeats: Number(editTotalSeats) || 30,
          amenities: editAmenities,
          photoUrls: editPhotoUrl || undefined,
        },
      }).unwrap();

      setFormSuccess(`Bus ${editRegistrationNumber} updated successfully!`);
      setIsEditBusOpen(false);
      setEditingBusId(null);
      refetchBuses();
      refetchAnalytics();
      setTimeout(() => setFormSuccess(""), 4000);
    } catch (err: any) {
      setFormError(err?.data?.message || "Failed to update bus details.");
    }
  };

  const handleDeleteBus = async (busId: number, regNumber: string) => {
    if (!window.confirm(`Are you sure you want to delete bus "${regNumber}"? This will also remove any associated schedules.`)) {
      return;
    }
    setFormError("");
    setFormSuccess("");

    try {
      await deleteBusMutation(busId).unwrap();
      setFormSuccess(`Bus ${regNumber} was successfully removed from your fleet.`);
      refetchBuses();
      refetchAnalytics();
      refetchSchedules();
      setTimeout(() => setFormSuccess(""), 4000);
    } catch (err: any) {
      setFormError(err?.data?.message || "Failed to delete bus from fleet.");
    }
  };

  const handleCreateSchedule = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError("");
    setFormSuccess("");

    const targetBusId = scheduleBusId || (buses.length > 0 ? buses[0].id : null);
    if (!targetBusId) {
      setFormError("Please select a bus from your fleet.");
      return;
    }

    try {
      const res = await createScheduleMutation({
        busId: Number(targetBusId),
        sourceCity: sourceCity.trim(),
        destinationCity: destinationCity.trim(),
        departureTime: departureTime.length === 5 ? `${departureTime}:00` : departureTime,
        arrivalTime: arrivalTime.length === 5 ? `${arrivalTime}:00` : arrivalTime,
        basePrice: Number(basePrice),
        operatingDays,
        validFrom,
        validTo,
        boardingPoints,
        droppingPoints,
      }).unwrap();

      setFormSuccess(
        `Schedule published! Generated ${res.materializedTripsCount || "30+"} live trips immediately searchable for passengers!`
      );
      setIsAddScheduleOpen(false);
      refetchSchedules();
      refetchAnalytics();
      setTimeout(() => setFormSuccess(""), 5000);
    } catch (err: any) {
      setFormError(err?.data?.message || "Failed to create schedule.");
    }
  };

  if (!isAuthenticated || !user) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center px-4">
        <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-8 max-w-md w-full text-center">
          <div className="w-16 h-16 rounded-2xl bg-red-50 text-[#d84e55] flex items-center justify-center mx-auto mb-4">
            <Bus className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Operator Sign In Required</h2>
          <p className="text-sm text-gray-500 mb-6">
            Please log in with your operator credentials to manage your fleet, publish live routes, and inspect analytics.
          </p>
          <Link
            href="/"
            className="inline-flex items-center justify-center px-6 py-3 rounded-xl bg-[#d84e55] text-white font-semibold text-sm hover:bg-[#b83e44] transition-colors shadow-md shadow-red-500/20"
          >
            Return to Homepage
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50/50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        {/* Operator Portal Banner Header */}
        <div className="bg-gradient-to-r from-gray-900 via-slate-900 to-[#d84e55] rounded-3xl shadow-xl p-6 sm:p-8 text-white relative overflow-hidden">
          <div className="absolute top-0 right-0 w-96 h-96 bg-red-500/10 rounded-full blur-3xl -mr-20 -mt-20 pointer-events-none" />

          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-10">
            <div>
              <div className="flex items-center space-x-2.5 mb-2">
                {user.operatorStatus === "PENDING" ? (
                  <span className="px-3 py-1 bg-amber-500/20 border border-amber-400/30 text-amber-300 font-bold text-xs uppercase tracking-wider rounded-full flex items-center gap-1.5 animate-pulse">
                    <Clock className="w-3.5 h-3.5 text-amber-400" />
                    Pending Admin Approval
                  </span>
                ) : (
                  <span className="px-3 py-1 bg-red-500/20 border border-red-400/30 text-red-300 font-bold text-xs uppercase tracking-wider rounded-full flex items-center gap-1.5">
                    <ShieldCheck className="w-3.5 h-3.5 text-red-400" />
                    Verified Bus Operator Portal
                  </span>
                )}
                <span className="px-2.5 py-0.5 bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 text-xs font-semibold rounded-full flex items-center gap-1">
                  <Activity className="w-3 h-3 animate-pulse" /> Live DB Sync
                </span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-black tracking-tight">
                {analytics?.companyName || `${user.name} Transport Network`}
              </h1>
              <p className="text-xs sm:text-sm text-gray-300 mt-1 max-w-2xl">
                Dynamic inventory control: Add buses, publish daily schedules to the redBus marketplace, and track real-time ticket sales & occupancy analytics.
              </p>
              {user.operatorStatus === "PENDING" && (
                <div className="mt-3 p-3 bg-amber-500/20 border border-amber-500/40 rounded-xl text-xs text-amber-200 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-amber-300 shrink-0" />
                  <span>Your operator registration has been submitted. The RedBus Administrator will verify and activate your commercial fleet profile shortly.</span>
                </div>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <button
                onClick={() => {
                  setBusOperatorName(analytics?.companyName || user.name + " Travels");
                  setIsAddBusOpen(true);
                }}
                className="px-4 py-2.5 bg-white text-gray-900 hover:bg-gray-100 rounded-xl text-xs font-bold shadow-md transition-all flex items-center space-x-1.5 cursor-pointer"
              >
                <Plus className="w-4 h-4 text-[#d84e55]" />
                <span>Add Bus to Fleet</span>
              </button>

              <button
                onClick={() => {
                  if (buses.length > 0) setScheduleBusId(buses[0].id);
                  setIsAddScheduleOpen(true);
                }}
                className="px-4 py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold shadow-md shadow-red-500/30 transition-all flex items-center space-x-1.5 cursor-pointer"
              >
                <Calendar className="w-4 h-4" />
                <span>Publish Route Schedule</span>
              </button>

              <button
                onClick={() => setActiveTab("profile")}
                className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-bold border border-white/20 shadow-md transition-all flex items-center space-x-1.5 cursor-pointer"
              >
                <Building2 className="w-4 h-4 text-amber-400" />
                <span>Agency Profile</span>
              </button>

              <button
                onClick={() => setIsAddCouponOpen(true)}
                className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-md shadow-emerald-500/30 transition-all flex items-center space-x-1.5 cursor-pointer"
              >
                <Tag className="w-4 h-4" />
                <span>Create Live Coupon</span>
              </button>

              <Link
                href="/"
                className="px-3.5 py-2.5 bg-white/10 hover:bg-white/20 border border-white/15 text-white rounded-xl text-xs font-semibold transition-all flex items-center space-x-1.5"
                title="View passenger search view"
              >
                <Eye className="w-4 h-4" />
                <span>Search Live View</span>
              </Link>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="flex flex-wrap gap-2 mt-8 border-t border-white/10 pt-4">
            {[
              { id: "analytics", label: "Real-Time Analytics & Payouts", icon: BarChart3 },
              { id: "fleet", label: `My Fleet (${buses.length})`, icon: Bus },
              { id: "schedules", label: `Active Schedules (${schedules.length})`, icon: Calendar },
              { id: "coupons", label: `Promotions & Coupons (${operatorCoupons.length})`, icon: Tag },
              { id: "profile", label: "Agency Profile & Settings", icon: Building2 },
            ].map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center space-x-2 cursor-pointer ${
                    activeTab === tab.id
                      ? "bg-white text-gray-900 shadow-md"
                      : "bg-white/5 text-gray-300 hover:bg-white/10"
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Success Alert */}
        {formSuccess && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center space-x-3 text-sm text-emerald-900 animate-in fade-in">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span className="font-semibold">{formSuccess}</span>
          </div>
        )}

        {/* TAB 1: REAL-TIME ANALYTICS & LIVE SALES */}
        {activeTab === "analytics" && (
          <div className="space-y-6 animate-in fade-in">
            {/* KPI Cards Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
              {/* Gross Revenue */}
              <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-xs space-y-2">
                <div className="flex items-center justify-between text-gray-400">
                  <span className="text-xs font-bold uppercase tracking-wider">Gross Sales</span>
                  <div className="p-2 bg-emerald-50 text-emerald-600 rounded-xl">
                    <DollarSign className="w-4 h-4" />
                  </div>
                </div>
                <div className="text-2xl font-black text-gray-900">
                  ₹{Number(analytics?.totalRevenue || 0).toLocaleString("en-IN")}
                </div>
                <p className="text-[11px] text-emerald-600 font-semibold flex items-center gap-1">
                  <TrendingUp className="w-3 h-3" /> Live from bookings
                </p>
              </div>

              {/* Net Earnings */}
              <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-xs space-y-2">
                <div className="flex items-center justify-between text-gray-400">
                  <span className="text-xs font-bold uppercase tracking-wider">Net Payout</span>
                  <div className="p-2 bg-blue-50 text-blue-600 rounded-xl">
                    <Percent className="w-4 h-4" />
                  </div>
                </div>
                <div className="text-2xl font-black text-blue-900">
                  ₹{Number(analytics?.netEarnings || 0).toLocaleString("en-IN")}
                </div>
                <p className="text-[11px] text-gray-400 font-medium">
                  After 10% platform fee (₹{Number(analytics?.commissionPaid || 0).toLocaleString("en-IN")})
                </p>
              </div>

              {/* Tickets Sold */}
              <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-xs space-y-2">
                <div className="flex items-center justify-between text-gray-400">
                  <span className="text-xs font-bold uppercase tracking-wider">Tickets Sold</span>
                  <div className="p-2 bg-red-50 text-[#d84e55] rounded-xl">
                    <Ticket className="w-4 h-4" />
                  </div>
                </div>
                <div className="text-2xl font-black text-gray-900">
                  {analytics?.ticketsSold || 0} <span className="text-xs font-medium text-gray-400">seats</span>
                </div>
                <p className="text-[11px] text-gray-500 font-medium">Across all scheduled routes</p>
              </div>

              {/* Fleet Occupancy */}
              <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-xs space-y-2">
                <div className="flex items-center justify-between text-gray-400">
                  <span className="text-xs font-bold uppercase tracking-wider">Avg Occupancy</span>
                  <div className="p-2 bg-purple-50 text-purple-600 rounded-xl">
                    <Activity className="w-4 h-4" />
                  </div>
                </div>
                <div className="text-2xl font-black text-purple-900">
                  {analytics?.averageOccupancyPercentage || 0}%
                </div>
                <div className="w-full bg-gray-100 rounded-full h-1.5 overflow-hidden">
                  <div
                    className="bg-purple-600 h-full rounded-full transition-all"
                    style={{ width: `${Math.min(100, Number(analytics?.averageOccupancyPercentage || 0))}%` }}
                  />
                </div>
              </div>

              {/* Active Fleet */}
              <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-xs space-y-2">
                <div className="flex items-center justify-between text-gray-400">
                  <span className="text-xs font-bold uppercase tracking-wider">Active Fleet</span>
                  <div className="p-2 bg-amber-50 text-amber-600 rounded-xl">
                    <Bus className="w-4 h-4" />
                  </div>
                </div>
                <div className="text-2xl font-black text-gray-900">
                  {analytics?.activeFleetCount || 0} <span className="text-xs font-medium text-gray-400">Buses</span>
                </div>
                <p className="text-[11px] text-gray-400 font-medium">
                  {analytics?.activeSchedulesCount || 0} active schedules
                </p>
              </div>
            </div>

            {/* Daily Revenue Bar Timeline (CSS Graph) */}
            <div className="bg-white rounded-3xl p-6 border border-gray-100 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-gray-900">7-Day Ticket Revenue Timeline</h3>
                  <p className="text-xs text-gray-400">Live booking sales per day</p>
                </div>
                <span className="text-xs font-bold text-[#d84e55] bg-red-50 px-3 py-1 rounded-full">
                  Real-time Data
                </span>
              </div>

              <div className="pt-4 grid grid-cols-7 gap-2 sm:gap-4 items-end h-44 border-b border-gray-100 pb-2">
                {analytics?.dailyTimeline && analytics.dailyTimeline.length > 0 ? (
                  analytics.dailyTimeline.map((item, idx) => {
                    const maxRev = Math.max(
                      1000,
                      ...analytics.dailyTimeline.map((d) => Number(d.revenue || 0))
                    );
                    const heightPct = Math.max(8, (Number(item.revenue || 0) / maxRev) * 100);

                    return (
                      <div key={idx} className="flex flex-col items-center h-full justify-end group">
                        <span className="text-[10px] font-bold text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity mb-1">
                          ₹{Number(item.revenue || 0)}
                        </span>
                        <div
                          style={{ height: `${heightPct}%` }}
                          className="w-full max-w-[40px] bg-gradient-to-t from-[#d84e55] to-orange-400 rounded-t-xl group-hover:brightness-110 transition-all shadow-xs"
                        />
                        <span className="text-[10px] text-gray-400 font-medium mt-2 truncate w-full text-center">
                          {item.date}
                        </span>
                      </div>
                    );
                  })
                ) : (
                  <div className="col-span-7 text-center text-xs text-gray-400 py-12">
                    No booking transactions recorded yet. Publish schedules to start generating ticket sales!
                  </div>
                )}
              </div>
            </div>

            {/* Performance Grids: Routes & Recent Bookings */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Route Performance Table */}
              <div className="bg-white rounded-3xl p-6 border border-gray-100 shadow-xs space-y-4">
                <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                  <h3 className="text-base font-bold text-gray-900 flex items-center space-x-2">
                    <MapPin className="w-4 h-4 text-[#d84e55]" />
                    <span>Top Performing Routes</span>
                  </h3>
                  <span className="text-xs text-gray-400">By Revenue</span>
                </div>

                {analytics?.routePerformance && analytics.routePerformance.length > 0 ? (
                  <div className="space-y-3">
                    {analytics.routePerformance.map((r, i) => (
                      <div key={i} className="p-3.5 bg-gray-50 rounded-2xl flex items-center justify-between">
                        <div>
                          <p className="text-xs font-bold text-gray-900">{r.routeName}</p>
                          <p className="text-[11px] text-gray-500">
                            {r.bookingCount} Bookings • {r.ticketsSold} Seats Sold
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-xs font-black text-emerald-700">₹{Number(r.revenue).toLocaleString("en-IN")}</p>
                          <span className="text-[10px] font-semibold text-purple-700 bg-purple-100 px-2 py-0.5 rounded-full">
                            {r.occupancyPercentage}% Occupancy
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="py-12 text-center text-xs text-gray-400 border-2 border-dashed border-gray-100 rounded-2xl">
                    No route revenue yet.
                  </div>
                )}
              </div>

              {/* Live Bookings Table */}
              <div className="bg-white rounded-3xl p-6 border border-gray-100 shadow-xs space-y-4">
                <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                  <h3 className="text-base font-bold text-gray-900 flex items-center space-x-2">
                    <Ticket className="w-4 h-4 text-[#d84e55]" />
                    <span>Recent Passenger Bookings</span>
                  </h3>
                  <span className="text-xs font-semibold text-emerald-600 flex items-center gap-1">
                    <Activity className="w-3 h-3 animate-pulse" /> Live
                  </span>
                </div>

                {analytics?.recentBookings && analytics.recentBookings.length > 0 ? (
                  <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
                    {analytics.recentBookings.map((b) => (
                      <div
                        key={b.id}
                        className="p-3.5 bg-gray-50/80 rounded-2xl border border-gray-100 flex items-center justify-between hover:border-gray-200 transition-colors"
                      >
                        <div>
                          <div className="flex items-center space-x-2">
                            <span className="font-mono text-xs font-black text-gray-900">{b.pnr}</span>
                            <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] font-bold rounded-md">
                              {b.status}
                            </span>
                          </div>
                          <p className="text-xs font-bold text-gray-800 mt-0.5">{b.passengerName}</p>
                          <p className="text-[11px] text-gray-500">
                            {b.route} • Seats: {b.seatNumbers.join(", ")}
                          </p>
                        </div>

                        <div className="text-right">
                          <p className="text-xs font-black text-gray-900">₹{b.totalAmount}</p>
                          <p className="text-[10px] text-blue-600 font-semibold">Net: ₹{b.netAmount}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="py-12 text-center text-xs text-gray-400 border-2 border-dashed border-gray-100 rounded-2xl">
                    No tickets booked yet. When passengers book your buses, their reservations appear here in real time!
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: FLEET MANAGER */}
        {activeTab === "fleet" && (
          <div className="space-y-6 animate-in fade-in">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-gray-900">Registered Fleet</h2>
                <p className="text-xs text-gray-500">Buses and custom seat layouts managed by your transport company</p>
              </div>
              <button
                onClick={() => {
                  setBusOperatorName(analytics?.companyName || user.name + " Travels");
                  setIsAddBusOpen(true);
                }}
                className="px-4 py-2 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold shadow-md shadow-red-500/20 transition-all flex items-center space-x-1.5 cursor-pointer"
              >
                <Plus className="w-4 h-4" />
                <span>Add Bus</span>
              </button>
            </div>

            {isLoadingBuses ? (
              <div className="py-12 text-center text-xs text-gray-400">Loading fleet...</div>
            ) : buses.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 space-y-4">
                <div className="w-16 h-16 bg-red-50 text-[#d84e55] rounded-2xl flex items-center justify-center mx-auto">
                  <Bus className="w-8 h-8" />
                </div>
                <h3 className="text-base font-bold text-gray-900">No Buses in Fleet</h3>
                <p className="text-xs text-gray-500 max-w-sm mx-auto">
                  Add your first commercial bus with sleeper or seater layouts to begin publishing live routes.
                </p>
                <button
                  onClick={() => setIsAddBusOpen(true)}
                  className="px-6 py-2.5 bg-[#d84e55] text-white rounded-xl text-xs font-bold shadow-md cursor-pointer"
                >
                  + Add First Bus
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
                {buses.map((bus) => (
                  <div
                    key={bus.id}
                    className="bg-white rounded-3xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-all space-y-4 relative overflow-hidden"
                  >
                    <BusImageSlider
                      photoUrls={bus.photoUrls}
                      busName={bus.operatorName}
                      busType={bus.busType}
                      aspectRatio="video"
                      showThumbnails={true}
                    />

                    <div className="flex items-start justify-between">
                      <div>
                        <span className="px-2.5 py-0.5 bg-gray-100 text-gray-700 font-mono text-[10px] font-bold rounded-md">
                          {bus.registrationNumber}
                        </span>
                        <h3 className="text-base font-bold text-gray-900 mt-1">{bus.operatorName}</h3>
                        <p className="text-xs font-semibold text-[#d84e55]">{bus.busType}</p>
                      </div>
                      <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                        <CheckCircle2 className="w-3 h-3" /> Active
                      </span>
                    </div>

                    <div className="p-3 bg-gray-50 rounded-2xl grid grid-cols-2 gap-2 text-xs">
                      <div>
                        <span className="text-gray-400 block text-[10px]">Total Capacity</span>
                        <span className="font-bold text-gray-900">{bus.totalSeats} Seats</span>
                      </div>
                      <div>
                        <span className="text-gray-400 block text-[10px]">Layout Style</span>
                        <span className="font-bold text-gray-900">
                          {bus.busType.includes("Sleeper") ? "2+1 Double Deck" : "2+2 Single Deck"}
                        </span>
                      </div>
                    </div>

                    <div>
                      <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">
                        Amenities
                      </span>
                      <div className="flex flex-wrap gap-1">
                        {bus.amenities.split(",").map((a, i) => (
                          <span
                            key={i}
                            className="px-2 py-0.5 bg-gray-100 text-gray-600 text-[10px] font-medium rounded-md"
                          >
                            {a.trim()}
                          </span>
                        ))}
                      </div>
                    </div>

                    <div className="pt-3 border-t border-gray-100 flex flex-wrap items-center justify-between gap-2">
                      <span className="text-[11px] text-gray-500 font-semibold">
                        {(bus.photoUrls?.split(",").length || 0) > 0
                          ? `📸 ${bus.photoUrls?.split(",").length} Photos Active`
                          : "No Photos Attached"}
                      </span>
                      <div className="flex items-center gap-1.5">
                        <button
                          type="button"
                          onClick={() => handleOpenEditBus(bus)}
                          className="px-2.5 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl text-xs font-bold transition-all flex items-center gap-1 cursor-pointer"
                          title="Edit Bus Details"
                        >
                          <Pencil className="w-3.5 h-3.5 text-gray-600" />
                          <span>Edit</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteBus(bus.id, bus.registrationNumber)}
                          className="px-2.5 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 rounded-xl text-xs font-bold transition-all flex items-center gap-1 cursor-pointer"
                          title="Delete Bus from Fleet"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>Delete</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setEditingBus(bus);
                            setEditingBusPhotos(bus.photoUrls || "");
                            setIsPhotoStudioOpen(true);
                          }}
                          className="px-2.5 py-1.5 bg-red-50 hover:bg-red-100 text-[#d84e55] rounded-xl text-xs font-bold transition-all flex items-center gap-1 cursor-pointer border border-red-200/60"
                          title="AI Photo Studio"
                        >
                          <Wand2 className="w-3.5 h-3.5" />
                          <span>AI Studio</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: SCHEDULES & ROUTES */}
        {activeTab === "schedules" && (
          <div className="space-y-6 animate-in fade-in">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-gray-900">Active Published Schedules</h2>
                <p className="text-xs text-gray-500">
                  Recurring services automatically materialized as bookable daily trips on redBus
                </p>
              </div>
              <button
                onClick={() => {
                  if (buses.length > 0) setScheduleBusId(buses[0].id);
                  setIsAddScheduleOpen(true);
                }}
                className="px-4 py-2 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold shadow-md shadow-red-500/20 transition-all flex items-center space-x-1.5 cursor-pointer"
              >
                <Plus className="w-4 h-4" />
                <span>Publish Schedule</span>
              </button>
            </div>

            {isLoadingSchedules ? (
              <div className="py-12 text-center text-xs text-gray-400">Loading schedules...</div>
            ) : schedules.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 space-y-4">
                <div className="w-16 h-16 bg-red-50 text-[#d84e55] rounded-2xl flex items-center justify-center mx-auto">
                  <Calendar className="w-8 h-8" />
                </div>
                <h3 className="text-base font-bold text-gray-900">No Schedules Published</h3>
                <p className="text-xs text-gray-500 max-w-sm mx-auto">
                  Publish a recurring schedule with departure timing and fare to make your buses instantly bookable on redBus.
                </p>
                <button
                  onClick={() => {
                    if (buses.length > 0) setScheduleBusId(buses[0].id);
                    setIsAddScheduleOpen(true);
                  }}
                  className="px-6 py-2.5 bg-[#d84e55] text-white rounded-xl text-xs font-bold shadow-md cursor-pointer"
                >
                  + Publish First Schedule
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {schedules.map((sch) => (
                  <div
                    key={sch.id}
                    className="bg-white rounded-3xl p-6 border border-gray-100 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4"
                  >
                    <div className="space-y-1.5">
                      <div className="flex items-center space-x-2">
                        <span className="text-base font-black text-gray-900">
                          {sch.sourceCity} → {sch.destinationCity}
                        </span>
                        <span className="px-2.5 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] font-bold rounded-full">
                          {sch.status}
                        </span>
                      </div>
                      <p className="text-xs font-semibold text-gray-600">
                        {sch.busName} ({sch.busType})
                      </p>
                      <div className="flex flex-wrap items-center gap-3 text-xs text-gray-500 pt-1">
                        <span className="flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5 text-[#d84e55]" /> {sch.departureTime} - {sch.arrivalTime}
                        </span>
                        <span>•</span>
                        <span className="font-semibold text-gray-700">{sch.operatingDays}</span>
                        <span>•</span>
                        <span>
                          Valid: {sch.validFrom} to {sch.validTo}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center space-x-4 border-t md:border-t-0 pt-3 md:pt-0">
                      <div className="text-right">
                        <span className="text-[10px] text-gray-400 block uppercase font-bold">Base Fare</span>
                        <span className="text-lg font-black text-[#d84e55]">₹{sch.basePrice}</span>
                      </div>
                      <Link
                        href={`/bus-tickets/${sch.sourceCity.toLowerCase()}-to-${sch.destinationCity.toLowerCase()}`}
                        className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 rounded-xl text-xs font-bold transition-colors flex items-center space-x-1"
                      >
                        <span>View Live</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 4: COUPONS & PROMOTIONS */}
        {activeTab === "coupons" && (
          <div className="space-y-6 animate-in fade-in">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <Tag className="w-5 h-5 text-[#d84e55]" />
                  <span>Promotions & Live Coupons</span>
                </h2>
                <p className="text-xs text-gray-500">
                  Create instant discount codes for passengers booking your buses. All coupons go live immediately!
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={handleCreateQuick2PercentCoupon}
                  disabled={isCreatingCoupon}
                  className="px-4 py-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white rounded-xl text-xs font-black shadow-md shadow-emerald-600/20 transition-all flex items-center space-x-1.5 cursor-pointer"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>⚡ 1-Click 2% Live Promo</span>
                </button>

                <button
                  type="button"
                  onClick={() => setIsAddCouponOpen(true)}
                  className="px-4 py-2 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold shadow-md shadow-red-500/20 transition-all flex items-center space-x-1.5 cursor-pointer"
                >
                  <Plus className="w-4 h-4" />
                  <span>Create Custom Coupon</span>
                </button>
              </div>
            </div>

            {/* Instant Live Alert */}
            <div className="p-4 bg-emerald-50/80 border border-emerald-200 rounded-2xl flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                  <Percent className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-bold text-emerald-950">
                    Instant 2% to 50% Passenger Booking Discounts
                  </p>
                  <p className="text-[11px] text-emerald-800">
                    Coupons published here are instantly discoverable by travellers on the checkout page and can be applied with 1 click.
                  </p>
                </div>
              </div>
              <span className="px-2.5 py-1 bg-emerald-200 text-emerald-900 rounded-lg text-[10px] font-black uppercase tracking-wider shrink-0 hidden sm:inline-block">
                Real-time sync
              </span>
            </div>

            {isLoadingCoupons ? (
              <div className="py-12 text-center text-xs text-gray-400">Loading live coupons...</div>
            ) : displayCoupons.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 space-y-4">
                <div className="w-16 h-16 bg-red-50 text-[#d84e55] rounded-2xl flex items-center justify-center mx-auto">
                  <Gift className="w-8 h-8" />
                </div>
                <h3 className="text-base font-bold text-gray-900">No Custom Coupons Created Yet</h3>
                <p className="text-xs text-gray-500 max-w-sm mx-auto">
                  Attract more passengers to your routes! Create a 2% or 5% discount coupon to boost your occupancy.
                </p>
                <div className="flex justify-center gap-3 pt-2">
                  <button
                    onClick={handleCreateQuick2PercentCoupon}
                    disabled={isCreatingCoupon}
                    className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-md cursor-pointer"
                  >
                    ⚡ Create 2% Promo Instantly
                  </button>
                  <button
                    onClick={() => setIsAddCouponOpen(true)}
                    className="px-5 py-2.5 bg-[#d84e55] text-white rounded-xl text-xs font-bold shadow-md cursor-pointer"
                  >
                    + Custom Coupon
                  </button>
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {displayCoupons.map((coupon) => (
                  <div
                    key={coupon.id}
                    className="bg-white rounded-3xl p-5 border border-gray-100 shadow-sm hover:shadow-md transition-all relative overflow-hidden flex flex-col justify-between space-y-4"
                  >
                    <div className="flex items-start justify-between">
                      <div className="space-y-1">
                        <div className="flex items-center space-x-2">
                          <span className="px-3 py-1 bg-red-50 border border-red-200 text-[#d84e55] font-mono font-black text-sm rounded-xl tracking-wider">
                            {coupon.code}
                          </span>
                          <span
                            className={`px-2 py-0.5 text-[10px] font-bold rounded-md ${
                              (coupon.isActive ?? coupon.active ?? true)
                                ? "bg-emerald-100 text-emerald-800"
                                : "bg-gray-100 text-gray-500"
                            }`}
                          >
                            {(coupon.isActive ?? coupon.active ?? true) ? "LIVE" : "INACTIVE"}
                          </span>
                        </div>
                        <h4 className="text-xs font-bold text-gray-900 pt-1">
                          {coupon.title || `${coupon.discountPercentage}% Off Live Promo`}
                        </h4>
                        <p className="text-[11px] text-gray-500">
                          {coupon.description || `Valid on bookings with min amount ₹${coupon.minBookingAmount || 0}`}
                        </p>
                      </div>

                      <div className="text-right shrink-0">
                        <span className="text-xl font-black text-emerald-600">
                          {coupon.discountPercentage}%
                        </span>
                        <span className="text-[10px] text-gray-400 block font-semibold">OFF</span>
                      </div>
                    </div>

                    <div className="p-3 bg-gray-50 rounded-2xl grid grid-cols-3 gap-2 text-center text-xs">
                      <div>
                        <span className="text-gray-400 block text-[9px] uppercase font-semibold">Max Cap</span>
                        <span className="font-bold text-gray-800">₹{coupon.maxDiscountAmount}</span>
                      </div>
                      <div>
                        <span className="text-gray-400 block text-[9px] uppercase font-semibold">Min Booking</span>
                        <span className="font-bold text-gray-800">₹{coupon.minBookingAmount}</span>
                      </div>
                      <div>
                        <span className="text-gray-400 block text-[9px] uppercase font-semibold">Redemptions</span>
                        <span className="font-bold text-purple-700">{coupon.timesUsed ?? coupon.usageCount ?? 0}</span>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-2 border-t border-gray-100 text-[11px] text-gray-400">
                      <span>Expires: {coupon.validTo || coupon.expiryDate || "Never"}</span>
                      <button
                        type="button"
                        onClick={() => handleDeleteCoupon(coupon.id, coupon.code)}
                        disabled={isDeletingCoupon}
                        className="text-red-500 hover:text-red-700 p-1 rounded-lg hover:bg-red-50 transition-colors cursor-pointer"
                        title="Delete coupon"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 5: OPERATOR & AGENCY PROFILE EDIT */}
        {activeTab === "profile" && (
          <div className="space-y-6 animate-in fade-in">
            {/* Header / Intro */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <Building2 className="w-5 h-5 text-[#d84e55]" />
                  <span>Agency & Commercial Operator Profile</span>
                </h2>
                <p className="text-xs text-gray-500">
                  Manage your public brand identity, contact person, passenger support numbers, and payout bank details.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className="px-3 py-1.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-xl flex items-center gap-1.5">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span>Status: {operatorProfile?.status || "APPROVED"}</span>
                </span>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Left 2 Cols: Edit Profile Form */}
              <div className="lg:col-span-2 bg-white rounded-3xl p-6 sm:p-8 border border-gray-100 shadow-xs space-y-6">
                <div className="flex items-center justify-between border-b border-gray-100 pb-4">
                  <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 rounded-2xl bg-red-50 text-[#d84e55] flex items-center justify-center font-bold">
                      <Edit3 className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-gray-900">Edit Operator Credentials</h3>
                      <p className="text-xs text-gray-400">Updates will reflect across all search results & passenger e-tickets</p>
                    </div>
                  </div>
                </div>

                <form onSubmit={handleUpdateProfile} className="space-y-5">
                  {formError && (
                    <div className="p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200 flex items-center gap-2">
                      <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
                      <span>{formError}</span>
                    </div>
                  )}

                  {/* Section 1: Brand & Representative */}
                  <div className="space-y-4">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-gray-400">
                      1. Brand Identity & Representative
                    </h4>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-bold text-gray-700 mb-1">
                          Agency / Bus Brand Name <span className="text-red-500">*</span>
                        </label>
                        <div className="relative">
                          <Building2 className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
                          <input
                            type="text"
                            required
                            value={profileCompanyName}
                            onChange={(e) => setProfileCompanyName(e.target.value)}
                            placeholder="e.g. Royal Travels India"
                            className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                          />
                        </div>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-gray-700 mb-1">
                          Authorized Signatory / Contact Person <span className="text-red-500">*</span>
                        </label>
                        <div className="relative">
                          <UserIcon className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
                          <input
                            type="text"
                            required
                            value={profileContactPerson}
                            onChange={(e) => setProfileContactPerson(e.target.value)}
                            placeholder="e.g. Jai Ganesh"
                            className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Section 2: Contact & Support */}
                  <div className="space-y-4 pt-2 border-t border-gray-100">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-gray-400">
                      2. Communication & Passenger Support
                    </h4>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-bold text-gray-700 mb-1">
                          Dispatch Contact Phone / Helpline
                        </label>
                        <div className="relative">
                          <Phone className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
                          <input
                            type="text"
                            value={profilePhone}
                            onChange={(e) => setProfilePhone(e.target.value)}
                            placeholder="+91 9876543210"
                            className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                          />
                        </div>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-gray-700 mb-1">
                          Official Business Email
                        </label>
                        <div className="relative">
                          <Mail className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
                          <input
                            type="email"
                            value={profileEmail}
                            onChange={(e) => setProfileEmail(e.target.value)}
                            placeholder="support@royaltravels.com"
                            className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Section 3: Payout Settlement & KYC */}
                  <div className="space-y-4 pt-2 border-t border-gray-100">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-gray-400">
                      3. Settlement Bank Account & Compliance
                    </h4>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-bold text-gray-700 mb-1">
                          Bank Account / UPI / Settlement Reference
                        </label>
                        <div className="relative">
                          <CreditCard className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
                          <input
                            type="text"
                            value={profileBankAccountRef}
                            onChange={(e) => setProfileBankAccountRef(e.target.value)}
                            placeholder="e.g. HDFC0001234 - AC 501004928192"
                            className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                          />
                        </div>
                        <p className="text-[10px] text-gray-400 mt-1">
                          Ticket sales earnings will be disbursed automatically to this settlement account.
                        </p>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-gray-700 mb-1">
                          GSTIN / Business Registration / Document Reference
                        </label>
                        <div className="relative">
                          <FileCheck className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
                          <input
                            type="text"
                            value={profileKycDocUrl}
                            onChange={(e) => setProfileKycDocUrl(e.target.value)}
                            placeholder="e.g. 33AAAAA0000A1Z5 / Permit #8849"
                            className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Submit Button */}
                  <div className="pt-4 border-t border-gray-100 flex items-center justify-end space-x-3">
                    <button
                      type="button"
                      onClick={() => {
                        if (operatorProfile) {
                          setProfileCompanyName(operatorProfile.companyName || "");
                          setProfileContactPerson(operatorProfile.contactPerson || "");
                          setProfilePhone(operatorProfile.phone || "");
                          setProfileEmail(operatorProfile.email || "");
                          setProfileBankAccountRef(operatorProfile.bankAccountRef || "");
                          setProfileKycDocUrl(operatorProfile.kycDocUrl || "");
                        }
                      }}
                      className="px-4 py-2.5 rounded-xl border border-gray-200 text-xs font-bold text-gray-600 hover:bg-gray-50 transition-colors cursor-pointer"
                    >
                      Reset Changes
                    </button>

                    <button
                      type="submit"
                      disabled={isUpdatingProfile}
                      className="px-6 py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-xs font-bold shadow-md shadow-red-500/20 transition-all flex items-center space-x-2 cursor-pointer disabled:opacity-50"
                    >
                      {isUpdatingProfile ? (
                        <>
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                          <span>Saving Profile...</span>
                        </>
                      ) : (
                        <>
                          <Save className="w-4 h-4" />
                          <span>Save & Update Profile</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>

              {/* Right Col: Live Operator Summary Card */}
              <div className="space-y-6">
                {/* Brand Preview Card */}
                <div className="bg-gradient-to-br from-slate-900 via-slate-800 to-[#1f222e] rounded-3xl p-6 text-white border border-slate-700/50 shadow-xl space-y-5">
                  <div className="flex items-center space-x-3">
                    <div className="w-12 h-12 rounded-2xl bg-red-500/20 border border-red-400/30 text-red-400 flex items-center justify-center font-black text-lg">
                      {profileCompanyName ? profileCompanyName.charAt(0).toUpperCase() : "O"}
                    </div>
                    <div>
                      <h4 className="text-base font-black truncate max-w-[180px]">
                        {profileCompanyName || "Operator Brand"}
                      </h4>
                      <p className="text-xs text-gray-400 flex items-center gap-1">
                        <UserIcon className="w-3 h-3 text-red-400" />
                        <span>{profileContactPerson || "Authorized Manager"}</span>
                      </p>
                    </div>
                  </div>

                  <div className="border-t border-white/10 pt-4 space-y-3">
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-gray-400">Verification Status</span>
                      <span className="px-2 py-0.5 bg-emerald-500/20 text-emerald-300 font-bold rounded-md border border-emerald-400/30 text-[10px]">
                        {operatorProfile?.status || "APPROVED"}
                      </span>
                    </div>

                    <div className="flex justify-between items-center text-xs">
                      <span className="text-gray-400">Platform Commission</span>
                      <span className="font-bold text-white">
                        {operatorProfile?.commissionRate || "10.00"}% Standard Tier
                      </span>
                    </div>

                    <div className="flex justify-between items-center text-xs">
                      <span className="text-gray-400">Total Registered Buses</span>
                      <span className="font-bold text-white">{buses.length} Coaches</span>
                    </div>

                    <div className="flex justify-between items-center text-xs">
                      <span className="text-gray-400">Active Daily Routes</span>
                      <span className="font-bold text-white">{schedules.length} Published</span>
                    </div>
                  </div>

                  <div className="p-3 bg-white/5 rounded-2xl border border-white/10 text-[11px] text-gray-300 space-y-1">
                    <p className="font-bold text-white flex items-center gap-1">
                      <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                      <span>Direct Passenger Settlement</span>
                    </p>
                    <p className="text-[10px] text-gray-400">
                      All ticket revenues collected via Razorpay are settled automatically into your registered bank reference after departure.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* MODAL: ADD BUS TO FLEET */}
      {isAddBusOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl max-w-md w-full overflow-hidden shadow-2xl border border-gray-100 relative max-h-[90vh] flex flex-col">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <div className="p-2 bg-red-50 text-[#d84e55] rounded-xl">
                  <Bus className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900">Add Bus to Fleet</h3>
                  <p className="text-xs text-gray-500">Auto-generates seat map layout</p>
                </div>
              </div>
              <button
                onClick={() => setIsAddBusOpen(false)}
                className="p-2 rounded-full text-gray-400 hover:text-gray-700 hover:bg-gray-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateBus} className="p-6 overflow-y-auto space-y-4">
              {formError && (
                <div className="p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
                  <span>{formError}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Operator / Bus Brand Name</label>
                <input
                  type="text"
                  required
                  value={busOperatorName}
                  onChange={(e) => setBusOperatorName(e.target.value)}
                  placeholder="e.g. Royal Travels"
                  className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Registration Plate Number</label>
                <input
                  type="text"
                  required
                  value={busRegistrationNumber}
                  onChange={(e) => setBusRegistrationNumber(e.target.value)}
                  placeholder="e.g. KA-01-AB-1234"
                  className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-mono font-bold uppercase focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Bus Layout Type</label>
                  <select
                    value={busType}
                    onChange={(e) => {
                      setBusType(e.target.value);
                      if (e.target.value.includes("Sleeper")) setBusTotalSeats(30);
                      else setBusTotalSeats(40);
                    }}
                    className="w-full px-3 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  >
                    <option value="AC Sleeper (2+1)">AC Sleeper (2+1)</option>
                    <option value="Volvo Multi-Axle AC Sleeper (2+1)">Volvo Multi-Axle Sleeper</option>
                    <option value="BharatBenz AC Sleeper (2+1)">BharatBenz Sleeper</option>
                    <option value="Volvo Multi-Axle AC Seater (2+2)">Volvo AC Seater (2+2)</option>
                    <option value="Scania AC Seater (2+2)">Scania AC Seater (2+2)</option>
                    <option value="Non-AC Seater (2+2)">Non-AC Seater (2+2)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Total Capacity</label>
                  <input
                    type="number"
                    required
                    min={10}
                    max={60}
                    value={busTotalSeats}
                    onChange={(e) => setBusTotalSeats(Number(e.target.value))}
                    className="w-full px-3 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-bold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Amenities (Comma separated)</label>
                <textarea
                  rows={2}
                  value={busAmenities}
                  onChange={(e) => setBusAmenities(e.target.value)}
                  className="w-full px-3 py-2 bg-gray-50 rounded-xl border border-gray-200 text-xs font-medium focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              {/* AI Bus Photo Studio Generator & Real Image Upload */}
              <div className="space-y-3 bg-gradient-to-b from-gray-50 to-slate-50 p-4 rounded-2xl border border-gray-200 shadow-inner">
                <div className="flex items-center justify-between">
                  <div>
                    <label className="block text-xs font-bold text-gray-800 flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                      <span>AI Bus Photo Studio</span>
                    </label>
                    <p className="text-[10px] text-gray-500">Auto-generates verified multi-angle exterior, sleeper/seater cabin & cockpit photos</p>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      disabled={isInlineAiGenerating}
                      onClick={() => {
                        setAiStudioContext("NEW_BUS");
                        setIsAiStudioModalOpen(true);
                      }}
                      className="text-[11px] font-bold text-slate-800 hover:text-black flex items-center gap-1.5 bg-white hover:bg-gray-100 px-3 py-1.5 rounded-xl border border-gray-300 shadow-xs cursor-pointer transition-all active:scale-95 disabled:opacity-50"
                    >
                      <Sliders className="w-3.5 h-3.5 text-amber-500" />
                      <span>Studio Studio</span>
                    </button>
                    <button
                      type="button"
                      disabled={isInlineAiGenerating}
                      onClick={() => handleQuickAiGenerate()}
                      className="text-[11px] font-bold text-white flex items-center gap-1.5 bg-gradient-to-r from-amber-600 to-red-600 hover:from-amber-500 hover:to-red-500 px-3.5 py-1.5 rounded-xl border border-amber-600 shadow-sm cursor-pointer transition-all active:scale-95 disabled:opacity-50"
                    >
                      <Wand2 className="w-3.5 h-3.5 text-white" />
                      <span>{busPhotoUrl ? "🔄 Regenerate AI Bus" : "✨ Ask AI to Generate"}</span>
                    </button>
                  </div>
                </div>

                {/* AI Theme Quick Selector Carousel / Pills */}
                <div className="space-y-1.5">
                  <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                    Select Bus Model Preset:
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {AI_BUS_THEMES.map((theme) => {
                      const isSelected = busPhotoUrl === theme.joinedUrls || busPhotoUrl.includes(theme.photos[0].url);
                      return (
                        <button
                          key={theme.id}
                          type="button"
                          disabled={isInlineAiGenerating}
                          onClick={() => handleQuickAiGenerate(theme.id)}
                          className={`text-[10px] font-bold px-2.5 py-1 rounded-lg border transition-all cursor-pointer flex items-center gap-1 ${
                            isSelected
                              ? "bg-slate-900 text-white border-slate-900 shadow-xs ring-1 ring-amber-400"
                              : "bg-white text-gray-700 border-gray-200 hover:border-gray-300 hover:bg-gray-50"
                          } ${isInlineAiGenerating ? "opacity-50 cursor-not-allowed" : ""}`}
                        >
                          <span>{theme.name}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* AI Generation Loading State Indicator */}
                {isInlineAiGenerating ? (
                  <div className="p-4 bg-slate-900 text-white rounded-2xl border border-amber-500/40 space-y-2.5 animate-in fade-in">
                    <div className="flex items-center justify-between text-xs font-bold">
                      <div className="flex items-center gap-2">
                        <div className="w-3.5 h-3.5 rounded-full border-2 border-amber-400 border-t-transparent animate-spin" />
                        <span className="text-amber-400">{inlineAiStep}</span>
                      </div>
                      <span className="font-mono text-[10px] text-slate-300">{inlineAiProgress}%</span>
                    </div>
                    <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-amber-400 to-red-500 transition-all duration-300 rounded-full"
                        style={{ width: `${inlineAiProgress}%` }}
                      />
                    </div>
                    <p className="text-[10px] text-slate-400 font-mono">
                      Querying AI synthesis engine & attaching 4-angle bus suite...
                    </p>
                  </div>
                ) : busPhotoUrl ? (
                  <div className="space-y-2 pt-1">
                    {selectedAiThemeName && (
                      <div className="flex items-center justify-between text-[11px] bg-white px-3 py-1.5 rounded-xl border border-gray-200/80 shadow-xs">
                        <span className="font-bold text-gray-800 flex items-center gap-1.5">
                          <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                          <span>Active Model: <strong>{selectedAiThemeName}</strong></span>
                        </span>
                        <span className="text-[10px] text-emerald-600 font-bold flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" /> 4-Angle Bus Suite Ready
                        </span>
                      </div>
                    )}

                    <BusImageSlider
                      photoUrls={busPhotoUrl}
                      busName={busOperatorName || "Coach Preview"}
                      busType={busType}
                      aspectRatio="video"
                      showThumbnails={true}
                    />

                    <div className="flex justify-between items-center text-[11px] pt-1">
                      <span className="text-emerald-600 font-bold flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        {busPhotoUrl.split(",").length} High-Res Photos Attached
                      </span>
                      <div className="flex items-center gap-3">
                        <button
                          type="button"
                          onClick={() => handleQuickAiGenerate()}
                          className="text-amber-600 hover:text-amber-700 font-bold hover:underline cursor-pointer flex items-center gap-1"
                        >
                          <Wand2 className="w-3 h-3" />
                          <span>Try Another Angle</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setBusPhotoUrl("");
                            setSelectedAiThemeName("");
                          }}
                          className="text-red-500 hover:underline font-semibold cursor-pointer"
                        >
                          Clear Photos
                        </button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <label className="border-2 border-dashed border-gray-200 hover:border-[#d84e55] rounded-2xl p-4 flex flex-col items-center justify-center cursor-pointer bg-white hover:bg-red-50/20 transition-all">
                    {isUploadingImage ? (
                      <div className="flex flex-col items-center py-2 space-y-2">
                        <div className="w-6 h-6 border-2 border-[#d84e55] border-t-transparent rounded-full animate-spin" />
                        <span className="text-xs text-gray-500 font-semibold">Uploading to server...</span>
                      </div>
                    ) : (
                      <>
                        <UploadCloud className="w-6 h-6 text-gray-400 mb-1" />
                        <span className="text-xs font-bold text-gray-700">Click to upload custom photos</span>
                        <span className="text-[10px] text-gray-400">Or click &quot;Ask AI to Generate&quot; above for instant bus suites</span>
                      </>
                    )}
                    <input
                      type="file"
                      accept="image/*"
                      disabled={isUploadingImage}
                      onChange={handleFileUpload}
                      className="hidden"
                    />
                  </label>
                )}
              </div>

              <div className="pt-2 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsAddBusOpen(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isCreatingBus || isUploadingImage}
                  className="px-6 py-2 bg-[#d84e55] hover:bg-[#b83e44] text-white text-xs font-bold rounded-xl shadow-md shadow-red-500/20 disabled:opacity-50 cursor-pointer"
                >
                  {isCreatingBus ? "Saving..." : "Create Bus Layout"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: PUBLISH SCHEDULE */}
      {isAddScheduleOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl max-w-lg w-full overflow-hidden shadow-2xl border border-gray-100 relative max-h-[90vh] flex flex-col">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <div className="p-2 bg-red-50 text-[#d84e55] rounded-xl">
                  <Calendar className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900">Publish Route Schedule</h3>
                  <p className="text-xs text-gray-500">Materializes live bookable trips for passengers</p>
                </div>
              </div>
              <button
                onClick={() => setIsAddScheduleOpen(false)}
                className="p-2 rounded-full text-gray-400 hover:text-gray-700 hover:bg-gray-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSchedule} className="p-6 overflow-y-auto space-y-4">
              {formError && (
                <div className="p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
                  <span>{formError}</span>
                </div>
              )}

              {/* Bus Selector */}
              <div>
                <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1">Select Bus from Fleet</label>
                {buses.length > 0 ? (
                  <select
                    required
                    value={scheduleBusId || (buses.length > 0 ? buses[0].id : "")}
                    onChange={(e) => setScheduleBusId(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-slate-700 text-xs font-bold text-gray-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  >
                    {buses.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.operatorName} ({b.registrationNumber}) - {b.busType} [{b.totalSeats} seats]
                      </option>
                    ))}
                  </select>
                ) : (
                  <div className="p-3 bg-amber-50 text-amber-800 text-xs rounded-xl border border-amber-200">
                    No buses found. Please add a bus to your fleet first.
                  </div>
                )}
              </div>

              {/* Source & Destination with AI Discovery */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <AiCityDropdown
                    label="Source City"
                    value={sourceCity}
                    onChange={handleSourceCityChange}
                    excludeCity={destinationCity}
                    placeholder="Select source city..."
                    required
                  />
                </div>

                <div>
                  <AiCityDropdown
                    label="Destination City"
                    value={destinationCity}
                    onChange={handleDestinationCityChange}
                    excludeCity={sourceCity}
                    placeholder="Select destination city..."
                    required
                  />
                </div>
              </div>

              {/* Timings & Base Fare */}
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1">Departure</label>
                  <input
                    type="time"
                    required
                    value={departureTime}
                    onChange={(e) => setDepartureTime(e.target.value)}
                    className="w-full px-3 py-2.5 bg-gray-50 dark:bg-slate-800 dark:text-white rounded-xl border border-gray-200 dark:border-slate-700 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1">Arrival</label>
                  <input
                    type="time"
                    required
                    value={arrivalTime}
                    onChange={(e) => setArrivalTime(e.target.value)}
                    className="w-full px-3 py-2.5 bg-gray-50 dark:bg-slate-800 dark:text-white rounded-xl border border-gray-200 dark:border-slate-700 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1">Base Fare (₹)</label>
                  <input
                    type="number"
                    required
                    min={100}
                    value={basePrice}
                    onChange={(e) => setBasePrice(Number(e.target.value))}
                    className="w-full px-3 py-2.5 bg-gray-50 dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-slate-700 text-xs font-bold text-[#d84e55] focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              {/* Operating Days & Date Range */}
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1">Frequency</label>
                  <select
                    value={operatingDays}
                    onChange={(e) => setOperatingDays(e.target.value)}
                    className="w-full px-3 py-2.5 bg-gray-50 dark:bg-slate-800 dark:text-white rounded-xl border border-gray-200 dark:border-slate-700 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  >
                    <option value="DAILY">Daily (7 days)</option>
                    <option value="MON,TUE,WED,THU,FRI">Weekdays only</option>
                    <option value="SAT,SUN">Weekends only</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1">Valid From</label>
                  <input
                    type="date"
                    required
                    value={validFrom}
                    onChange={(e) => setValidFrom(e.target.value)}
                    className="w-full px-2 py-2.5 bg-gray-50 dark:bg-slate-800 dark:text-white rounded-xl border border-gray-200 dark:border-slate-700 text-xs font-medium focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300 mb-1">Valid To</label>
                  <input
                    type="date"
                    required
                    value={validTo}
                    onChange={(e) => setValidTo(e.target.value)}
                    className="w-full px-2 py-2.5 bg-gray-50 dark:bg-slate-800 dark:text-white rounded-xl border border-gray-200 dark:border-slate-700 text-xs font-medium focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              {/* Boarding and Dropping Points with AI Auto-Population */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300">Boarding Points</label>
                  {isAiBoardingLoading ? (
                    <span className="text-[10px] text-[#d84e55] font-semibold flex items-center gap-1 animate-pulse">
                      <Sparkles className="w-3 h-3 text-amber-500" />
                      AI Populating Boarding Stops...
                    </span>
                  ) : (
                    <span className="text-[10px] text-gray-400 font-medium">✨ Auto-populated by AI from {sourceCity}</span>
                  )}
                </div>
                <input
                  type="text"
                  required
                  value={boardingPoints}
                  onChange={(e) => setBoardingPoints(e.target.value)}
                  placeholder="e.g. Koyambedu Omni Bus Stand (20:30), Guindy (21:00)"
                  className="w-full px-3 py-2 bg-gray-50 dark:bg-slate-800 dark:text-white rounded-xl border border-gray-200 dark:border-slate-700 text-xs focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-gray-700 dark:text-slate-300">Dropping Points</label>
                  {isAiDroppingLoading ? (
                    <span className="text-[10px] text-[#d84e55] font-semibold flex items-center gap-1 animate-pulse">
                      <Sparkles className="w-3 h-3 text-amber-500" />
                      AI Populating Dropping Stops...
                    </span>
                  ) : (
                    <span className="text-[10px] text-gray-400 font-medium">✨ Auto-populated by AI from {destinationCity}</span>
                  )}
                </div>
                <input
                  type="text"
                  required
                  value={droppingPoints}
                  onChange={(e) => setDroppingPoints(e.target.value)}
                  placeholder="e.g. Electronic City Toll (04:30), Silk Board (04:45)"
                  className="w-full px-3 py-2 bg-gray-50 dark:bg-slate-800 dark:text-white rounded-xl border border-gray-200 dark:border-slate-700 text-xs focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              <div className="p-3 bg-red-50/70 border border-red-200/60 rounded-xl text-[11px] text-[#d84e55] leading-relaxed">
                ✨ <strong>Instant Materialization:</strong> When you click Publish, trips and seat maps for each calendar day are immediately saved to MySQL and become live on passenger searches.
              </div>

              <div className="pt-2 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsAddScheduleOpen(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isCreatingSchedule || buses.length === 0}
                  className="px-6 py-2 bg-[#d84e55] hover:bg-[#b83e44] text-white text-xs font-bold rounded-xl shadow-md shadow-red-500/20 disabled:opacity-50 cursor-pointer"
                >
                  {isCreatingSchedule ? "Publishing Trips..." : "Publish Live Schedule"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: CREATE LIVE COUPON */}
      {isAddCouponOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl max-w-md w-full overflow-hidden shadow-2xl border border-gray-100 relative max-h-[90vh] flex flex-col">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <div className="p-2 bg-red-50 text-[#d84e55] rounded-xl">
                  <Tag className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900">Create Live Discount Coupon</h3>
                  <p className="text-xs text-gray-500">Instant activation for passenger bookings</p>
                </div>
              </div>
              <button
                onClick={() => setIsAddCouponOpen(false)}
                className="p-2 rounded-full text-gray-400 hover:text-gray-700 hover:bg-gray-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateCoupon} className="p-6 overflow-y-auto space-y-4">
              {formError && (
                <div className="p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
                  <span>{formError}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Coupon Promo Code *</label>
                <input
                  type="text"
                  required
                  value={couponCode}
                  onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
                  placeholder="e.g. SUPER2, SAVE5, FESTIVE10"
                  className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-mono font-bold uppercase focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Promo Title</label>
                <input
                  type="text"
                  value={couponTitle}
                  onChange={(e) => setCouponTitle(e.target.value)}
                  placeholder="e.g. Special Route Offer"
                  className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Description</label>
                <input
                  type="text"
                  value={couponDescription}
                  onChange={(e) => setCouponDescription(e.target.value)}
                  placeholder="e.g. 2% off on all AC Sleeper routes"
                  className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-medium focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Discount % *</label>
                  <input
                    type="number"
                    required
                    min={1}
                    max={90}
                    value={couponDiscountPercentage}
                    onChange={(e) => setCouponDiscountPercentage(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-bold text-emerald-700 focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Max Discount (₹) *</label>
                  <input
                    type="number"
                    required
                    min={1}
                    max={5000}
                    value={couponMaxDiscountAmount}
                    onChange={(e) => setCouponMaxDiscountAmount(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-bold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Min Booking (₹)</label>
                  <input
                    type="number"
                    min={0}
                    value={couponMinBookingAmount}
                    onChange={(e) => setCouponMinBookingAmount(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-bold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Expiry Date</label>
                  <input
                    type="date"
                    value={couponExpiryDate}
                    onChange={(e) => setCouponExpiryDate(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-medium focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              <div className="p-3 bg-emerald-50 border border-emerald-200/80 rounded-xl text-[11px] text-emerald-800 leading-relaxed flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>
                  <strong>Instant Live:</strong> Once saved, passengers can immediately apply this coupon during checkout!
                </span>
              </div>

              <div className="pt-2 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsAddCouponOpen(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isCreatingCoupon}
                  className="px-6 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-md shadow-emerald-500/20 disabled:opacity-50 cursor-pointer"
                >
                  {isCreatingCoupon ? "Activating..." : "Publish Live Coupon"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 4: EDIT BUS AI PHOTO STUDIO */}
      {isPhotoStudioOpen && editingBus && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl max-w-xl w-full p-6 shadow-2xl space-y-4 border border-gray-100 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center space-x-2.5">
                <div className="w-9 h-9 bg-red-50 text-[#d84e55] rounded-xl flex items-center justify-center">
                  <Wand2 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900">AI Bus Photo Studio</h3>
                  <p className="text-xs text-gray-500">
                    {editingBus.operatorName} • {editingBus.registrationNumber} ({editingBus.busType})
                  </p>
                </div>
              </div>
              <button
                onClick={() => {
                  setIsPhotoStudioOpen(false);
                  setEditingBus(null);
                }}
                className="p-1 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 bg-gray-50/80 p-3.5 rounded-2xl border border-gray-200/80">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-gray-800">Generate Multi-Angle Bus Photos</span>
                  <p className="text-[10px] text-gray-500">Exterior, luxury cabin berths, cockpit & amenities</p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    const nextTheme = getNextAiBusTheme(editingBusPhotos, editingBus.busType);
                    setEditingBusPhotos(nextTheme.joinedUrls);
                  }}
                  className="text-[11px] font-bold text-amber-700 hover:text-amber-800 flex items-center gap-1.5 bg-amber-100/80 hover:bg-amber-200/80 px-3 py-1.5 rounded-xl border border-amber-300 shadow-xs cursor-pointer transition-all active:scale-95"
                >
                  <Wand2 className="w-3.5 h-3.5 text-amber-600" />
                  <span>{editingBusPhotos ? "🔄 Next AI Model" : "✨ Ask AI to Create Bus Photos"}</span>
                </button>
              </div>

              {/* Quick Theme Selector Pills */}
              <div className="space-y-1.5">
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                  Choose AI Concept Style:
                </span>
                <div className="flex flex-wrap gap-1.5">
                  {AI_BUS_THEMES.map((theme) => {
                    const isSelected =
                      editingBusPhotos === theme.joinedUrls || editingBusPhotos.includes(theme.photos[0].url);
                    return (
                      <button
                        key={theme.id}
                        type="button"
                        onClick={() => setEditingBusPhotos(theme.joinedUrls)}
                        className={`text-[10px] font-bold px-2.5 py-1 rounded-lg border transition-all cursor-pointer flex items-center gap-1 ${
                          isSelected
                            ? "bg-slate-900 text-white border-slate-900 shadow-xs"
                            : "bg-white text-gray-700 border-gray-200 hover:border-gray-300 hover:bg-gray-50"
                        }`}
                      >
                        <span>{theme.name}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {editingBusPhotos ? (
                <div className="space-y-2 pt-1">
                  <BusImageSlider
                    photoUrls={editingBusPhotos}
                    busName={editingBus.operatorName}
                    busType={editingBus.busType}
                    aspectRatio="video"
                    showThumbnails={true}
                  />
                  <div className="flex justify-between items-center text-[11px] pt-1">
                    <span className="text-emerald-600 font-bold flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      {editingBusPhotos.split(",").length} High-Res Photos Ready
                    </span>
                    <div className="flex items-center gap-3">
                      <button
                        type="button"
                        onClick={() => {
                          const nextTheme = getNextAiBusTheme(editingBusPhotos, editingBus.busType);
                          setEditingBusPhotos(nextTheme.joinedUrls);
                        }}
                        className="text-amber-600 hover:text-amber-700 font-bold hover:underline cursor-pointer flex items-center gap-1"
                      >
                        <Wand2 className="w-3 h-3" />
                        <span>Try Another Model</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setEditingBusPhotos("")}
                        className="text-red-500 hover:underline font-semibold cursor-pointer"
                      >
                        Clear Photos
                      </button>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="py-8 text-center bg-white rounded-2xl border-2 border-dashed border-gray-200 space-y-2">
                  <Camera className="w-8 h-8 text-gray-300 mx-auto" />
                  <p className="text-xs font-bold text-gray-700">No Bus Photos Attached Yet</p>
                  <p className="text-[11px] text-gray-400">Click &quot;Ask AI to Create Bus Photos&quot; or select a concept style above</p>
                </div>
              )}
            </div>

            <div className="pt-2 flex justify-end space-x-2 border-t border-gray-100">
              <button
                type="button"
                onClick={() => {
                  setIsPhotoStudioOpen(false);
                  setEditingBus(null);
                }}
                className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={isUpdatingBusPhotos}
                onClick={async () => {
                  try {
                    await updateBusPhotosMutation({
                      busId: editingBus.id,
                      photoUrls: editingBusPhotos || "",
                    }).unwrap();
                    setFormSuccess("Bus photos updated successfully! Passengers will now see these high-res photos.");
                    setIsPhotoStudioOpen(false);
                    setEditingBus(null);
                    refetchBuses();
                    setTimeout(() => setFormSuccess(""), 4000);
                  } catch (err: any) {
                    setFormError(err?.data?.message || "Failed to update bus photos.");
                  }
                }}
                className="px-6 py-2 bg-[#d84e55] hover:bg-[#b83e44] text-white text-xs font-bold rounded-xl shadow-md shadow-red-500/20 disabled:opacity-50 cursor-pointer"
              >
                {isUpdatingBusPhotos ? "Saving Photos..." : "Save to Bus Fleet"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 5: EDIT FLEET BUS DETAILS */}
      {isEditBusOpen && editBusId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl space-y-4 border border-gray-100 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center space-x-2.5">
                <div className="w-9 h-9 bg-red-50 text-[#d84e55] rounded-xl flex items-center justify-center">
                  <Pencil className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900">Edit Bus in Fleet</h3>
                  <p className="text-xs text-gray-500">Update registration, seating layout and amenities</p>
                </div>
              </div>
              <button
                onClick={() => {
                  setIsEditBusOpen(false);
                  setEditingBusId(null);
                }}
                className="p-1 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleUpdateBus} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Operator / Brand Name</label>
                <input
                  type="text"
                  required
                  value={editOperatorName}
                  onChange={(e) => setEditOperatorName(e.target.value)}
                  placeholder="e.g. Das Travels"
                  className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Registration No. *</label>
                  <input
                    type="text"
                    required
                    value={editRegistrationNumber}
                    onChange={(e) => setEditRegistrationNumber(e.target.value.toUpperCase())}
                    placeholder="KA-01-AB-1234"
                    className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-mono font-bold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Total Seats *</label>
                  <input
                    type="number"
                    required
                    min={10}
                    max={60}
                    value={editTotalSeats}
                    onChange={(e) => setEditTotalSeats(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-bold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Bus & Layout Type</label>
                <select
                  value={editBusType}
                  onChange={(e) => setEditBusType(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-semibold focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                >
                  <option value="AC Sleeper (2+1)">AC Sleeper (2+1) - 30/36 Berths</option>
                  <option value="Non-AC Sleeper (2+1)">Non-AC Sleeper (2+1) - 30/36 Berths</option>
                  <option value="AC Seater (2+2)">AC Seater (2+2) - 40/48 Seats</option>
                  <option value="Non-AC Seater (2+2)">Non-AC Seater (2+2) - 40/48 Seats</option>
                  <option value="Volvo Multi-Axle AC Sleeper">Volvo Multi-Axle AC Sleeper (2+1)</option>
                  <option value="Scania Multi-Axle AC Seater">Scania Multi-Axle AC Seater (2+2)</option>
                  <option value="Electric AC Luxury Coach">Electric AC Luxury Coach (Zero-Emission)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Amenities (Comma separated)</label>
                <input
                  type="text"
                  value={editAmenities}
                  onChange={(e) => setEditAmenities(e.target.value)}
                  placeholder="WiFi, Charging Point, Water Bottle, Blanket, Live Tracking, Emergency Exit"
                  className="w-full px-3.5 py-2.5 bg-gray-50 rounded-xl border border-gray-200 text-xs font-medium focus:outline-hidden focus:ring-2 focus:ring-[#d84e55]"
                />
              </div>

              {/* AI Bus Theme / Photo selection */}
              <div className="space-y-2 bg-gray-50/80 p-3 rounded-2xl border border-gray-200">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-gray-800">Bus Photos / AI Suite</span>
                  <button
                    type="button"
                    onClick={() => {
                      const nextTheme = getNextAiBusTheme(editPhotoUrl, editBusType);
                      setEditPhotoUrl(nextTheme.joinedUrls);
                    }}
                    className="text-[10px] font-bold text-amber-700 hover:text-amber-800 flex items-center gap-1 bg-amber-100 px-2 py-1 rounded-lg border border-amber-300 cursor-pointer"
                  >
                    <Wand2 className="w-3 h-3 text-amber-600" />
                    <span>Cycle AI Bus Models</span>
                  </button>
                </div>

                {editPhotoUrl && (
                  <BusImageSlider
                    photoUrls={editPhotoUrl}
                    busName={editOperatorName || "Fleet Bus"}
                    busType={editBusType}
                    aspectRatio="video"
                    showThumbnails={true}
                  />
                )}
              </div>

              <div className="pt-2 flex justify-end space-x-2 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => {
                    setIsEditBusOpen(false);
                    setEditingBusId(null);
                  }}
                  className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-xl cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isUpdatingBus}
                  className="px-6 py-2 bg-[#d84e55] hover:bg-[#b83e44] text-white text-xs font-bold rounded-xl shadow-md shadow-red-500/20 disabled:opacity-50 cursor-pointer"
                >
                  {isUpdatingBus ? "Saving Changes..." : "Save Changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      {/* MODAL: DEDICATED AI BUS STUDIO SYNTHESIZER */}
      <AiBusStudioModal
        isOpen={isAiStudioModalOpen}
        onClose={() => setIsAiStudioModalOpen(false)}
        operatorName={
          aiStudioContext === "NEW_BUS"
            ? busOperatorName || "Express Coach"
            : editOperatorName || editingBus?.operatorName || "Express Coach"
        }
        busType={
          aiStudioContext === "NEW_BUS"
            ? busType
            : editBusType || editingBus?.busType || "AC Sleeper (2+1)"
        }
        currentPhotos={
          aiStudioContext === "NEW_BUS"
            ? busPhotoUrl
            : editPhotoUrl || editingBusPhotos
        }
        onApplyPhotos={(joinedUrls, themeName) => {
          if (aiStudioContext === "NEW_BUS") {
            setBusPhotoUrl(joinedUrls);
            setSelectedAiThemeName(themeName);
          } else if (aiStudioContext === "EDIT_BUS") {
            setEditPhotoUrl(joinedUrls);
          } else if (aiStudioContext === "EXISTING_BUS" && editingBus) {
            setEditingBusPhotos(joinedUrls);
          }
        }}
      />
    </div>
  );
}
