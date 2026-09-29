"use client";

import React, { useState, useRef, useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAppDispatch, useAppSelector } from "@/store";
import { logout, updateUser } from "@/store/authSlice";
import { toggleChat } from "@/store/chatSlice";
import {
  Bus,
  Ticket,
  Sparkles,
  Shield,
  User as UserIcon,
  LogOut,
  HelpCircle,
  Menu,
  X,
  Settings,
  ChevronDown,
  ChevronRight,
  CheckCircle2,
  AlertCircle,
  Sun,
  Moon,
  Wallet,
  Bell,
} from "lucide-react";
import { useTheme } from "@/context/ThemeContext";
import { useGetMeQuery } from "@/store/apiSlice";
import { toggleNotificationDropdown } from "@/store/notificationSlice";
import { useRealtimeNotifications } from "@/hooks/useRealtimeNotifications";
import NotificationDropdown from "@/components/NotificationDropdown";
import AuthModal from "@/components/AuthModal";
import LogoutModal from "@/components/LogoutModal";

export default function Navbar() {
  const pathname = usePathname();
  const dispatch = useAppDispatch();
  const { theme, toggleTheme } = useTheme();
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);
  const { isOpen: isNotificationOpen } = useAppSelector((state) => state.notification);
  const { unreadCount: unreadNotifCount } = useRealtimeNotifications();
  const { data: latestUser } = useGetMeQuery(undefined, { skip: !isAuthenticated });
  const activeUser = latestUser || user;
  const walletBalance = Number(activeUser?.walletBalance || 0);

  const [mounted, setMounted] = useState(false);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const notifDropdownRef = useRef<HTMLDivElement>(null);
  const mobileNotifDropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMounted(true);
  }, []);

  // Lock body scroll when mobile menu is open
  useEffect(() => {
    if (mobileMenuOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }
    return () => {
      document.body.style.overflow = "unset";
    };
  }, [mobileMenuOpen]);

  // Close mobile menu on pathname change & Escape key
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [pathname]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setMobileMenuOpen(false);
        setUserDropdownOpen(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  useEffect(() => {
    if (latestUser) {
      dispatch(updateUser(latestUser));
    }
  }, [latestUser, dispatch]);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setUserDropdownOpen(false);
      }
      const isInsideDesktopNotif = notifDropdownRef.current && notifDropdownRef.current.contains(event.target as Node);
      const isInsideMobileNotif = mobileNotifDropdownRef.current && mobileNotifDropdownRef.current.contains(event.target as Node);
      if (!isInsideDesktopNotif && !isInsideMobileNotif) {
        dispatch(toggleNotificationDropdown(false));
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [dispatch]);

  const handleLogoutConfirm = () => {
    setShowLogoutModal(false);
    setUserDropdownOpen(false);
    dispatch(logout());
  };

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .substring(0, 2);
  };

  return (
    <>
      <header className="sticky top-0 z-40 bg-white/95 dark:bg-[#0b0f19]/95 backdrop-blur-md border-b border-gray-200 dark:border-slate-800/80 shadow-xs transition-colors duration-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <div className="flex items-center space-x-8">
              <Link href="/" className="flex items-center space-x-2.5 group">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#d84e55] to-[#ef4444] flex items-center justify-center text-white shadow-md shadow-red-500/20 group-hover:scale-105 transition-transform">
                  <Bus className="w-6 h-6" />
                </div>
                <div>
                  <span className="text-2xl font-black tracking-tight text-[#d84e55]">red<span className="text-gray-900 dark:text-white">Bus</span></span>
                  <span className="hidden sm:inline-block ml-1.5 px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider bg-red-100 dark:bg-red-950/70 text-[#d84e55] dark:text-red-400 border border-red-200 dark:border-red-900/50 rounded">AI Edition</span>
                </div>
              </Link>

              {/* Navigation Links */}
              <nav className="hidden md:flex items-center space-x-1">
                <Link
                  href="/"
                  className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors ${
                    pathname === "/"
                      ? "text-[#d84e55] bg-red-50 dark:bg-red-950/40 dark:text-red-400"
                      : "text-gray-600 dark:text-slate-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-slate-800/80"
                  }`}
                >
                  Bus Tickets
                </Link>
                {activeUser?.role !== "ROLE_OPERATOR" && (
                  <Link
                    href="/my-bookings"
                    className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1.5 ${
                      pathname === "/my-bookings"
                        ? "text-[#d84e55] bg-red-50 dark:bg-red-950/40 dark:text-red-400"
                        : "text-gray-600 dark:text-slate-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-slate-800/80"
                    }`}
                  >
                    <Ticket className="w-4 h-4" />
                    <span>My Bookings</span>
                  </Link>
                )}
                <Link
                  href="/faq"
                  className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1.5 ${
                    pathname === "/faq"
                      ? "text-[#d84e55] bg-red-50 dark:bg-red-950/40 dark:text-red-400"
                      : "text-gray-600 dark:text-slate-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-slate-800/80"
                  }`}
                >
                  <HelpCircle className="w-4 h-4" />
                  <span>Help & FAQs</span>
                </Link>
                {(activeUser?.role === "ROLE_OPERATOR" || activeUser?.role === "ROLE_ADMIN") && (
                  <Link
                    href="/operator"
                    className={`px-3.5 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center space-x-1.5 ${
                      pathname === "/operator"
                        ? "text-[#d84e55] bg-red-50 dark:bg-red-950/40 dark:text-red-400"
                        : "text-gray-600 dark:text-slate-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-slate-800/80"
                    }`}
                  >
                    <Bus className="w-4 h-4 text-[#d84e55] dark:text-red-400" />
                    <span>Operator Hub</span>
                  </Link>
                )}
                {activeUser?.role === "ROLE_ADMIN" && (
                  <Link
                    href="/admin"
                    className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1.5 ${
                      pathname === "/admin"
                        ? "text-[#d84e55] bg-red-50 dark:bg-red-950/40 dark:text-red-400"
                        : "text-gray-600 dark:text-slate-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-slate-800/80"
                    }`}
                  >
                    <Shield className="w-4 h-4" />
                    <span>Admin</span>
                  </Link>
                )}
              </nav>
            </div>

            {/* Right Actions */}
            <div className="hidden md:flex items-center space-x-3">
              {/* Dark Mode Toggle */}
              <button
                onClick={toggleTheme}
                className="p-2 rounded-full border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-700 dark:text-amber-300 hover:bg-gray-100 dark:hover:bg-slate-700 transition-all shadow-2xs cursor-pointer"
                title={mounted && theme === "dark" ? "Switch to Light Mode" : "Switch to Dark Mode"}
                aria-label="Toggle Theme"
              >
                {mounted && theme === "dark" ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-slate-700" />}
              </button>


              {/* Notification Center */}
              <div className="relative" ref={notifDropdownRef}>
                <button
                  onClick={() => dispatch(toggleNotificationDropdown(!isNotificationOpen))}
                  className="relative p-2 rounded-full border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-700 dark:text-slate-300 hover:bg-gray-100 dark:hover:bg-slate-700 transition-all shadow-2xs cursor-pointer"
                  title="Notifications"
                  aria-label="Notifications"
                >
                  <Bell className="w-4 h-4" />
                  {unreadNotifCount > 0 && (
                    <span className="absolute -top-1 -right-1 flex h-4 min-w-[16px] px-1 items-center justify-center rounded-full bg-[#d84e55] text-[10px] font-extrabold text-white ring-2 ring-white dark:ring-slate-900 animate-pulse">
                      {unreadNotifCount}
                    </span>
                  )}
                </button>
                <NotificationDropdown onOpenAuthModal={() => setShowAuthModal(true)} />
              </div>

              {/* User Account / Auth */}
              {mounted && isAuthenticated && activeUser ? (
                <div className="relative" ref={dropdownRef}>
                  <button
                    onClick={() => setUserDropdownOpen(!userDropdownOpen)}
                    className="flex items-center space-x-2.5 p-1 pl-1.5 pr-3 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors border border-gray-200/80 dark:border-slate-700 shadow-xs focus:outline-none bg-white/50 dark:bg-slate-800/40 cursor-pointer"
                  >
                    <div className="w-8 h-8 rounded-full overflow-hidden bg-gradient-to-tr from-[#d84e55] to-orange-500 text-white flex items-center justify-center font-bold text-xs shadow-xs border border-white dark:border-slate-700">
                      {activeUser.avatarUrl ? (
                        /* eslint-disable-next-line @next/next/no-img-element */
                        <img
                          src={activeUser.avatarUrl}
                          alt={activeUser.name}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        getInitials(activeUser.name)
                      )}
                    </div>
                    <div className="text-left">
                      <p className="text-xs font-bold text-gray-800 dark:text-slate-100 leading-tight max-w-[120px] truncate">{activeUser.name}</p>
                      <p className="text-[10px] text-gray-400 dark:text-slate-400 font-medium">
                        {activeUser.role === "ROLE_ADMIN" ? "Administrator" : activeUser.role === "ROLE_OPERATOR" ? "Bus Operator" : "Passenger"}
                      </p>
                    </div>
                    <ChevronDown className={`w-3.5 h-3.5 text-gray-400 dark:text-slate-400 transition-transform ${userDropdownOpen ? "rotate-180" : ""}`} />
                  </button>

                  {/* Dropdown Menu */}
                  {userDropdownOpen && (
                    <div className="absolute right-0 mt-2 w-64 bg-white dark:bg-[#0f172a] rounded-2xl shadow-xl dark:shadow-2xl border border-gray-100 dark:border-slate-800 py-2 z-50 animate-scale-up">
                      {/* User Header */}
                      <div className="px-4 py-3 border-b border-gray-100 dark:border-slate-800 bg-gray-50/70 dark:bg-slate-800/60 rounded-t-2xl">
                        <div className="flex items-center space-x-2.5 mb-1.5">
                          <div className="w-8 h-8 rounded-full overflow-hidden bg-gray-200 dark:bg-slate-700 border border-gray-100 dark:border-slate-600 shrink-0">
                            {activeUser.avatarUrl ? (
                              /* eslint-disable-next-line @next/next/no-img-element */
                              <img src={activeUser.avatarUrl} alt={activeUser.name} className="w-full h-full object-cover" />
                            ) : (
                              <div className="w-full h-full bg-[#d84e55] text-white flex items-center justify-center text-xs font-bold">
                                {getInitials(activeUser.name)}
                              </div>
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-bold text-gray-900 dark:text-white truncate">{activeUser.name}</p>
                            <p className="text-[11px] text-gray-500 dark:text-slate-400 truncate">{activeUser.email}</p>
                          </div>
                        </div>

                        <div className="flex items-center justify-between pt-1">
                          {activeUser.emailVerified ? (
                            <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-700 dark:text-emerald-400 bg-emerald-100 dark:bg-emerald-950/60 px-2 py-0.5 rounded-full border border-emerald-200 dark:border-emerald-800/40">
                              <CheckCircle2 className="w-3 h-3" /> Verified
                            </span>
                          ) : (
                            <Link
                              href="/verify-email"
                              onClick={() => setUserDropdownOpen(false)}
                              className="inline-flex items-center gap-1 text-[10px] font-semibold text-amber-700 dark:text-amber-400 bg-amber-100 dark:bg-amber-950/60 hover:bg-amber-200 dark:hover:bg-amber-900/60 px-2 py-0.5 rounded-full border border-amber-200 dark:border-amber-800/40 transition-colors"
                            >
                              <AlertCircle className="w-3 h-3" /> Verify Email
                            </Link>
                          )}
                          <span className="text-[10px] text-slate-500 dark:text-slate-300 font-semibold px-2 py-0.5 bg-gray-100 dark:bg-slate-700 rounded-md">
                            {activeUser.role === "ROLE_ADMIN" ? "🛡️ Admin" : activeUser.role === "ROLE_OPERATOR" ? "🚍 Operator" : "👤 Passenger"}
                          </span>
                        </div>
                      </div>

                      {/* redBus Wallet Highlights Box */}
                      <div className="px-3 pt-2">
                        <Link
                          href="/profile"
                          onClick={() => setUserDropdownOpen(false)}
                          className="p-2.5 bg-gradient-to-r from-emerald-50 to-teal-50 dark:from-emerald-950/40 dark:to-teal-950/40 border border-emerald-200/80 dark:border-emerald-800/60 rounded-xl flex items-center justify-between hover:shadow-xs transition-all group"
                        >
                          <div className="flex items-center space-x-2.5">
                            <div className="w-8 h-8 rounded-lg bg-emerald-600 text-white flex items-center justify-center shadow-xs">
                              <Wallet className="w-4 h-4" />
                            </div>
                            <div className="text-left">
                              <span className="text-[10px] font-bold text-gray-500 dark:text-slate-400 block leading-tight">redBus Wallet</span>
                              <span className="text-xs font-black text-emerald-700 dark:text-emerald-400">
                                ₹{walletBalance.toFixed(2)}
                              </span>
                            </div>
                          </div>
                          <span className="text-[10px] font-bold text-emerald-700 dark:text-emerald-300 bg-emerald-100 dark:bg-emerald-900/60 px-2 py-0.5 rounded-md group-hover:bg-emerald-200 transition-colors">
                            Use on Trips →
                          </span>
                        </Link>
                      </div>

                      {/* Menu Links */}
                      <div className="py-1">
                        <Link
                          href="/profile"
                          onClick={() => setUserDropdownOpen(false)}
                          className="flex items-center justify-between px-4 py-2.5 text-sm text-gray-700 dark:text-slate-200 hover:bg-red-50 dark:hover:bg-slate-800 hover:text-[#d84e55] dark:hover:text-red-400 transition-colors"
                        >
                          <div className="flex items-center space-x-2.5">
                            <UserIcon className="w-4 h-4 text-gray-400 dark:text-slate-400" />
                            <span className="font-medium">My Profile</span>
                          </div>
                        </Link>
                        {activeUser?.role !== "ROLE_OPERATOR" && (
                          <Link
                            href="/my-bookings"
                            onClick={() => setUserDropdownOpen(false)}
                            className="flex items-center space-x-2.5 px-4 py-2.5 text-sm text-gray-700 dark:text-slate-200 hover:bg-red-50 dark:hover:bg-slate-800 hover:text-[#d84e55] dark:hover:text-red-400 transition-colors"
                          >
                            <Ticket className="w-4 h-4 text-gray-400 dark:text-slate-400" />
                            <span className="font-medium">My Bookings</span>
                          </Link>
                        )}
                        <Link
                          href="/settings"
                          onClick={() => setUserDropdownOpen(false)}
                          className="flex items-center space-x-2.5 px-4 py-2.5 text-sm text-gray-700 dark:text-slate-200 hover:bg-red-50 dark:hover:bg-slate-800 hover:text-[#d84e55] dark:hover:text-red-400 transition-colors"
                        >
                          <Settings className="w-4 h-4 text-gray-400 dark:text-slate-400" />
                          <span className="font-medium">Account Settings</span>
                        </Link>
                        {(activeUser.role === "ROLE_OPERATOR" || activeUser.role === "ROLE_ADMIN" || activeUser.roles?.includes("ROLE_OPERATOR") || activeUser.roles?.includes("ROLE_ADMIN")) && (
                          <Link
                            href="/operator"
                            onClick={() => setUserDropdownOpen(false)}
                            className="flex items-center space-x-2.5 px-4 py-2.5 text-sm text-gray-700 dark:text-slate-200 hover:bg-red-50 dark:hover:bg-slate-800 hover:text-[#d84e55] dark:hover:text-red-400 transition-colors"
                          >
                            <Bus className="w-4 h-4 text-[#d84e55] dark:text-red-400" />
                            <span className="font-semibold text-[#d84e55] dark:text-red-400">Operator Hub</span>
                          </Link>
                        )}
                        {(activeUser.role === "ROLE_ADMIN" || activeUser.roles?.includes("ROLE_ADMIN")) && (
                          <Link
                            href="/admin"
                            onClick={() => setUserDropdownOpen(false)}
                            className="flex items-center space-x-2.5 px-4 py-2.5 text-sm text-gray-700 dark:text-slate-200 hover:bg-red-50 dark:hover:bg-slate-800 hover:text-[#d84e55] dark:hover:text-red-400 transition-colors"
                          >
                            <Shield className="w-4 h-4 text-[#d84e55] dark:text-red-400" />
                            <span className="font-bold text-[#d84e55] dark:text-red-400">Admin Control Center</span>
                          </Link>
                        )}
                      </div>

                      {/* Logout Action */}
                      <div className="pt-1 border-t border-gray-100 dark:border-slate-800">
                        <button
                          onClick={() => {
                            setUserDropdownOpen(false);
                            setShowLogoutModal(true);
                          }}
                          className="w-full flex items-center space-x-2.5 px-4 py-2.5 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors font-medium text-left cursor-pointer"
                        >
                          <LogOut className="w-4 h-4 text-red-500 dark:text-red-400" />
                          <span>Log Out</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <button
                  onClick={() => setShowAuthModal(true)}
                  className="flex items-center space-x-1.5 px-4 py-2 rounded-xl bg-gray-900 dark:bg-red-600 dark:hover:bg-red-700 text-white hover:bg-gray-800 transition-all text-sm font-medium shadow-sm hover:shadow cursor-pointer"
                >
                  <UserIcon className="w-4 h-4" />
                  <span>Login / Sign Up</span>
                </button>
              )}
            </div>

            {/* Mobile menu button */}
            <div className="md:hidden flex items-center space-x-1.5" ref={mobileNotifDropdownRef}>
              <button
                type="button"
                onClick={() => dispatch(toggleNotificationDropdown(!isNotificationOpen))}
                className="relative p-2 text-gray-600 dark:text-slate-300 hover:text-gray-900 dark:hover:text-white rounded-lg cursor-pointer"
                aria-label="Notifications"
              >
                <Bell className="w-5 h-5" />
                {unreadNotifCount > 0 && (
                  <span className="absolute top-1 right-1 flex h-4 min-w-[16px] px-1 items-center justify-center rounded-full bg-[#d84e55] text-[9px] font-extrabold text-white ring-2 ring-white dark:ring-slate-900 animate-pulse">
                    {unreadNotifCount}
                  </span>
                )}
              </button>
              <NotificationDropdown onOpenAuthModal={() => setShowAuthModal(true)} />
              <button
                type="button"
                onClick={() => setMobileMenuOpen(true)}
                className="p-2 text-gray-700 dark:text-slate-200 hover:text-[#d84e55] dark:hover:text-red-400 rounded-lg cursor-pointer transition-colors"
                aria-label="Open Navigation Menu"
              >
                <Menu className="w-6 h-6" />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Mobile Sidebar Drawer (Croma-Style Slide-over) */}
      {mobileMenuOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity duration-300 animate-fade-in"
            onClick={() => setMobileMenuOpen(false)}
            aria-hidden="true"
          />

          {/* Slide-in Drawer */}
          <div
            className="relative w-[85%] max-w-[330px] sm:max-w-[360px] h-full bg-white dark:bg-[#121824] text-gray-900 dark:text-slate-100 flex flex-col shadow-2xl z-10 border-r border-gray-200 dark:border-slate-800 animate-drawer-slide"
            role="dialog"
            aria-modal="true"
            aria-label="Mobile Navigation"
          >
            {/* Drawer Header (Sign In or User Profile) */}
            <div className="p-4 bg-gray-50 dark:bg-[#192132] border-b border-gray-200 dark:border-slate-800/80 flex items-center justify-between">
              {mounted && isAuthenticated && activeUser ? (
                <div className="flex items-center space-x-3 min-w-0 pr-2">
                  <div className="w-10 h-10 rounded-full overflow-hidden bg-gradient-to-tr from-[#d84e55] to-orange-500 text-white flex items-center justify-center font-bold text-sm shrink-0 border border-gray-200 dark:border-slate-700 shadow-xs">
                    {activeUser.avatarUrl ? (
                      /* eslint-disable-next-line @next/next/no-img-element */
                      <img src={activeUser.avatarUrl} alt={activeUser.name} className="w-full h-full object-cover" />
                    ) : (
                      getInitials(activeUser.name)
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-bold text-gray-900 dark:text-white truncate">{activeUser.name}</p>
                    <p className="text-xs text-gray-500 dark:text-slate-400 truncate">{activeUser.email}</p>
                    <div className="flex items-center gap-1.5 mt-0.5">
                      <span className="text-[10px] font-semibold px-1.5 py-0.2 bg-gray-200 dark:bg-slate-800 text-gray-700 dark:text-slate-300 rounded">
                        {activeUser.role === "ROLE_ADMIN" ? "Admin" : activeUser.role === "ROLE_OPERATOR" ? "Operator" : "Passenger"}
                      </span>
                      {activeUser.emailVerified && (
                        <span className="inline-flex items-center gap-0.5 text-[9px] font-semibold text-emerald-700 dark:text-emerald-400 bg-emerald-100 dark:bg-emerald-950/60 px-1 rounded">
                          <CheckCircle2 className="w-2.5 h-2.5" /> Verified
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ) : (
                <button
                  onClick={() => {
                    setMobileMenuOpen(false);
                    setShowAuthModal(true);
                  }}
                  className="flex items-center space-x-2 text-sm font-bold text-gray-900 dark:text-white hover:text-[#d84e55] dark:hover:text-red-400 transition-colors cursor-pointer py-1"
                >
                  <UserIcon className="w-4 h-4 text-[#d84e55]" />
                  <span>Sign In | Create Account</span>
                </button>
              )}

              <button
                type="button"
                onClick={() => setMobileMenuOpen(false)}
                className="p-1.5 text-gray-500 dark:text-slate-400 hover:text-gray-900 dark:hover:text-white hover:bg-gray-200 dark:hover:bg-slate-800 rounded-lg transition-colors cursor-pointer ml-auto shrink-0"
                aria-label="Close menu"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Drawer Body - Scrollable */}
            <div className="flex-1 overflow-y-auto divide-y divide-gray-100 dark:divide-slate-800/60">
              {/* Wallet Section (If Logged In) */}
              {mounted && isAuthenticated && (
                <div className="p-3">
                  <Link
                    href="/profile"
                    onClick={() => setMobileMenuOpen(false)}
                    className="p-3 bg-gradient-to-r from-emerald-50 to-teal-50 dark:from-emerald-950/50 dark:to-teal-950/40 border border-emerald-200 dark:border-emerald-800/60 rounded-xl flex items-center justify-between hover:border-emerald-400 dark:hover:border-emerald-700 transition-all block shadow-2xs"
                  >
                    <div className="flex items-center space-x-2.5">
                      <div className="w-8 h-8 rounded-lg bg-emerald-600 text-white flex items-center justify-center shadow-xs">
                        <Wallet className="w-4 h-4" />
                      </div>
                      <div>
                        <span className="text-[10px] font-semibold text-gray-500 dark:text-slate-400 block uppercase tracking-wider">redBus Wallet</span>
                        <span className="text-sm font-bold text-emerald-700 dark:text-emerald-400">
                          ₹{walletBalance.toFixed(2)}
                        </span>
                      </div>
                    </div>
                    <span className="text-[11px] font-bold text-emerald-700 dark:text-emerald-400 bg-emerald-100 dark:bg-emerald-900/60 px-2 py-0.5 rounded-md">
                      View →
                    </span>
                  </Link>
                </div>
              )}

              {/* Primary Services Group */}
              <div className="py-2">
                <Link
                  href="/"
                  onClick={() => setMobileMenuOpen(false)}
                  className={`flex items-center justify-between px-4 py-3 text-sm font-medium transition-colors ${
                    pathname === "/"
                      ? "text-[#d84e55] bg-red-50 dark:text-red-400 dark:bg-red-950/30 font-semibold"
                      : "text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800/60 hover:text-gray-900 dark:hover:text-white"
                  }`}
                >
                  <div className="flex items-center space-x-3">
                    <Bus className="w-4 h-4 text-[#d84e55]" />
                    <span>Bus Tickets</span>
                  </div>
                  <ChevronRight className="w-4 h-4 text-gray-400 dark:text-slate-500" />
                </Link>

                {activeUser?.role !== "ROLE_OPERATOR" && (
                  <Link
                    href="/my-bookings"
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center justify-between px-4 py-3 text-sm font-medium transition-colors ${
                      pathname === "/my-bookings"
                        ? "text-[#d84e55] bg-red-50 dark:text-red-400 dark:bg-red-950/30 font-semibold"
                        : "text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800/60 hover:text-gray-900 dark:hover:text-white"
                    }`}
                  >
                    <div className="flex items-center space-x-3">
                      <Ticket className="w-4 h-4 text-amber-500 dark:text-amber-400" />
                      <span>My Bookings</span>
                    </div>
                    <ChevronRight className="w-4 h-4 text-gray-400 dark:text-slate-500" />
                  </Link>
                )}

                <Link
                  href="/faq"
                  onClick={() => setMobileMenuOpen(false)}
                  className={`flex items-center justify-between px-4 py-3 text-sm font-medium transition-colors ${
                    pathname === "/faq"
                      ? "text-[#d84e55] bg-red-50 dark:text-red-400 dark:bg-red-950/30 font-semibold"
                      : "text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800/60 hover:text-gray-900 dark:hover:text-white"
                  }`}
                >
                  <div className="flex items-center space-x-3">
                    <HelpCircle className="w-4 h-4 text-sky-500 dark:text-sky-400" />
                    <span>Help & FAQs</span>
                  </div>
                  <ChevronRight className="w-4 h-4 text-gray-400 dark:text-slate-500" />
                </Link>

                {(activeUser?.role === "ROLE_OPERATOR" || activeUser?.role === "ROLE_ADMIN") && (
                  <Link
                    href="/operator"
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center justify-between px-4 py-3 text-sm font-semibold transition-colors ${
                      pathname === "/operator"
                        ? "text-[#d84e55] bg-red-50 dark:text-red-400 dark:bg-red-950/30"
                        : "text-gray-800 dark:text-rose-300 hover:bg-gray-100 dark:hover:bg-slate-800/60 hover:text-gray-900 dark:hover:text-white"
                    }`}
                  >
                    <div className="flex items-center space-x-3">
                      <Bus className="w-4 h-4 text-[#d84e55]" />
                      <span>Operator Hub</span>
                    </div>
                    <span className="text-[10px] bg-red-100 dark:bg-red-950/80 border border-red-200 dark:border-red-800/60 text-[#d84e55] dark:text-red-300 px-1.5 py-0.5 rounded font-bold">PORTAL</span>
                  </Link>
                )}

                {activeUser?.role === "ROLE_ADMIN" && (
                  <Link
                    href="/admin"
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center justify-between px-4 py-3 text-sm font-medium transition-colors ${
                      pathname === "/admin"
                        ? "text-[#d84e55] bg-red-50 dark:text-red-400 dark:bg-red-950/30 font-semibold"
                        : "text-gray-700 dark:text-purple-300 hover:bg-gray-100 dark:hover:bg-slate-800/60 hover:text-gray-900 dark:hover:text-white"
                    }`}
                  >
                    <div className="flex items-center space-x-3">
                      <Shield className="w-4 h-4 text-purple-600 dark:text-purple-400" />
                      <span>Admin Dashboard</span>
                    </div>
                    <ChevronRight className="w-4 h-4 text-gray-400 dark:text-slate-500" />
                  </Link>
                )}
              </div>

              {/* Account Management Group */}
              <div className="py-2">
                <div className="px-4 py-1.5 text-[11px] font-bold text-gray-400 dark:text-slate-400 uppercase tracking-wider">
                  Account & Settings
                </div>

                {mounted && isAuthenticated ? (
                  <>
                    <Link
                      href="/profile"
                      onClick={() => setMobileMenuOpen(false)}
                      className={`flex items-center justify-between px-4 py-2.5 text-sm font-medium transition-colors ${
                        pathname === "/profile"
                          ? "text-[#d84e55] bg-red-50 dark:text-red-400 dark:bg-red-950/30 font-semibold"
                          : "text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800/60 hover:text-gray-900 dark:hover:text-white"
                      }`}
                    >
                      <div className="flex items-center space-x-3">
                        <UserIcon className="w-4 h-4 text-gray-400 dark:text-slate-400" />
                        <span>My Profile</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-gray-400 dark:text-slate-500" />
                    </Link>

                    <Link
                      href="/settings"
                      onClick={() => setMobileMenuOpen(false)}
                      className={`flex items-center justify-between px-4 py-2.5 text-sm font-medium transition-colors ${
                        pathname === "/settings"
                          ? "text-[#d84e55] bg-red-50 dark:text-red-400 dark:bg-red-950/30 font-semibold"
                          : "text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800/60 hover:text-gray-900 dark:hover:text-white"
                      }`}
                    >
                      <div className="flex items-center space-x-3">
                        <Settings className="w-4 h-4 text-gray-400 dark:text-slate-400" />
                        <span>Account Settings</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-gray-400 dark:text-slate-500" />
                    </Link>
                  </>
                ) : (
                  <button
                    onClick={() => {
                      setMobileMenuOpen(false);
                      setShowAuthModal(true);
                    }}
                    className="w-full flex items-center justify-between px-4 py-2.5 text-sm font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800/60 hover:text-gray-900 dark:hover:text-white transition-colors cursor-pointer text-left"
                  >
                    <div className="flex items-center space-x-3">
                      <UserIcon className="w-4 h-4 text-gray-400 dark:text-slate-400" />
                      <span>Login to Manage Account</span>
                    </div>
                    <ChevronRight className="w-4 h-4 text-gray-400 dark:text-slate-500" />
                  </button>
                )}

                {/* Theme Mode Toggle */}
                <button
                  onClick={toggleTheme}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-sm font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-800/60 hover:text-gray-900 dark:hover:text-white transition-colors cursor-pointer text-left"
                >
                  <div className="flex items-center space-x-3">
                    {mounted && theme === "dark" ? (
                      <Sun className="w-4 h-4 text-amber-400" />
                    ) : (
                      <Moon className="w-4 h-4 text-slate-700" />
                    )}
                    <span>Theme Mode</span>
                  </div>
                  <span className="text-xs font-semibold px-2 py-0.5 bg-gray-100 dark:bg-slate-800 text-gray-700 dark:text-slate-300 rounded border border-gray-200 dark:border-slate-700">
                    {mounted && theme === "dark" ? "Dark Mode" : "Light Mode"}
                  </span>
                </button>
              </div>

              {/* Action / Logout */}
              <div className="p-4">
                {mounted && isAuthenticated ? (
                  <button
                    onClick={() => {
                      setMobileMenuOpen(false);
                      setShowLogoutModal(true);
                    }}
                    className="w-full flex items-center justify-center space-x-2 px-4 py-2.5 bg-red-50 dark:bg-red-950/30 hover:bg-red-100 dark:hover:bg-red-900/40 text-[#d84e55] dark:text-red-400 border border-red-200 dark:border-red-900/50 rounded-xl text-sm font-semibold transition-colors cursor-pointer"
                  >
                    <LogOut className="w-4 h-4" />
                    <span>Log Out</span>
                  </button>
                ) : (
                  <button
                    onClick={() => {
                      setMobileMenuOpen(false);
                      setShowAuthModal(true);
                    }}
                    className="w-full flex items-center justify-center space-x-2 px-4 py-2.5 bg-[#d84e55] hover:bg-[#b83e44] text-white rounded-xl text-sm font-bold shadow-md shadow-red-500/20 transition-colors cursor-pointer"
                  >
                    <UserIcon className="w-4 h-4" />
                    <span>Sign In / Register</span>
                  </button>
                )}
              </div>
            </div>

            {/* Sidebar Footer Branding */}
            <div className="p-3 bg-gray-50 dark:bg-[#0d121c] border-t border-gray-200 dark:border-slate-800 text-center">
              <p className="text-[11px] font-medium text-gray-500 dark:text-slate-500">
                redBus AI Edition • India&apos;s AI Bus Network
              </p>
            </div>
          </div>
        </div>
      )}

      {showAuthModal && <AuthModal onClose={() => setShowAuthModal(false)} />}
      <LogoutModal
        isOpen={showLogoutModal}
        onClose={() => setShowLogoutModal(false)}
        onConfirm={handleLogoutConfirm}
        userName={user?.name}
      />
    </>
  );
}
