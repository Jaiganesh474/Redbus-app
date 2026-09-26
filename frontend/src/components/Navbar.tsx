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
                {(user?.role === "ROLE_OPERATOR" || user?.role === "ROLE_ADMIN") && (
                  <Link
                    href="/operator"
                    className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1.5 ${
                      pathname === "/operator"
                        ? "text-[#d84e55] bg-red-50 dark:bg-red-950/40 dark:text-red-400"
                        : "text-gray-600 dark:text-slate-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-slate-800/80"
                    }`}
                  >
                    <Bus className="w-4 h-4 text-[#d84e55] dark:text-red-400" />
                    <span>Operator Portal</span>
                  </Link>
                )}
                {user?.role === "ROLE_ADMIN" && (
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
                        <Link
                          href="/my-bookings"
                          onClick={() => setUserDropdownOpen(false)}
                          className="flex items-center space-x-2.5 px-4 py-2.5 text-sm text-gray-700 dark:text-slate-200 hover:bg-red-50 dark:hover:bg-slate-800 hover:text-[#d84e55] dark:hover:text-red-400 transition-colors"
                        >
                          <Ticket className="w-4 h-4 text-gray-400 dark:text-slate-400" />
                          <span className="font-medium">My Bookings</span>
                        </Link>
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
                            <span className="font-medium">Operator Portal</span>
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
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="p-2 text-gray-600 dark:text-slate-300 hover:text-gray-900 dark:hover:text-white"
              >
                {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
              </button>
            </div>
          </div>

          {/* Mobile dropdown */}
          {mobileMenuOpen && (
            <div className="md:hidden py-3 border-t border-gray-100 dark:border-slate-800 space-y-1 bg-white dark:bg-[#0b0f19]">
              <Link
                href="/"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-800"
              >
                Bus Tickets
              </Link>
              <Link
                href="/my-bookings"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-800"
              >
                My Bookings
              </Link>
              <Link
                href="/faq"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-800"
              >
                Help & FAQs
              </Link>
              {user?.role === "ROLE_ADMIN" && (
                <Link
                  href="/admin"
                  onClick={() => setMobileMenuOpen(false)}
                  className="block px-3 py-2 rounded-md text-base font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-800"
                >
                  Admin Dashboard
                </Link>
              )}

              {/* Theme toggle mobile */}
              <button
                onClick={toggleTheme}
                className="w-full flex items-center justify-between px-3 py-2 rounded-md text-sm font-semibold text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-slate-800"
              >
                <span>Theme Mode</span>
                <span className="flex items-center gap-1.5 text-xs text-[#d84e55] dark:text-amber-400 font-bold">
                  {mounted && theme === "dark" ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-slate-700" />}
                  {mounted && theme === "dark" ? "Dark Theme" : "Light Theme"}
                </span>
              </button>
              <div className="pt-2 border-t border-gray-100 dark:border-slate-800">
                {mounted && isAuthenticated && activeUser ? (
                  <div className="space-y-1">
                    <div className="px-3 py-2 bg-gray-50 dark:bg-slate-800/80 rounded-lg mb-1">
                      <p className="text-sm font-semibold text-gray-800 dark:text-slate-100">{activeUser.name}</p>
                      <p className="text-xs text-gray-500 dark:text-slate-400">{activeUser.email}</p>
                    </div>

                    {/* redBus Wallet Mobile */}
                    <Link
                      href="/profile"
                      onClick={() => setMobileMenuOpen(false)}
                      className="px-3 py-2 bg-gradient-to-r from-emerald-50 to-teal-50 dark:from-emerald-950/40 dark:to-teal-950/40 border border-emerald-200/70 dark:border-emerald-800/50 rounded-lg flex items-center justify-between mb-1"
                    >
                      <div className="flex items-center space-x-2">
                        <Wallet className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                        <span className="text-xs font-bold text-gray-700 dark:text-slate-200">redBus Wallet</span>
                      </div>
                      <span className="text-xs font-black text-emerald-700 dark:text-emerald-400">
                        ₹{walletBalance.toFixed(2)}
                      </span>
                    </Link>

                    <Link
                      href="/profile"
                      onClick={() => setMobileMenuOpen(false)}
                      className="block px-3 py-2 rounded-md text-sm font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-800"
                    >
                      My Profile
                    </Link>
                    <Link
                      href="/my-bookings"
                      onClick={() => setMobileMenuOpen(false)}
                      className="block px-3 py-2 rounded-md text-sm font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-800"
                    >
                      My Bookings
                    </Link>
                    <Link
                      href="/settings"
                      onClick={() => setMobileMenuOpen(false)}
                      className="block px-3 py-2 rounded-md text-sm font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-800"
                    >
                      Account Settings
                    </Link>
                    <button
                      onClick={() => {
                        setMobileMenuOpen(false);
                        setShowLogoutModal(true);
                      }}
                      className="w-full text-left px-3 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 rounded-md font-medium"
                    >
                      Log Out
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => {
                      setShowAuthModal(true);
                      setMobileMenuOpen(false);
                    }}
                    className="w-full text-left px-3 py-2 text-base font-medium text-[#d84e55] dark:text-red-400"
                  >
                    Login / Sign Up
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </header>

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
