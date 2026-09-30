import { initializeApp, getApps, getApp, FirebaseApp } from "firebase/app";
import {
  getAuth,
  Auth,
  GoogleAuthProvider,
  RecaptchaVerifier,
  signInWithPhoneNumber,
  ConfirmationResult,
} from "firebase/auth";

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY || "AIzaSyBLzeoEEC7XPE49ONNr6VyCHZ_7ZkL-QW8",
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN || "redbus-app-89d34.firebaseapp.com",
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID || "redbus-app-89d34",
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET || "redbus-app-89d34.firebasestorage.app",
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID || "272837059560",
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID || "1:272837059560:web:6780e2fab91906cd0c7a5d",
};

let app: FirebaseApp;
let auth: Auth;
let googleProvider: GoogleAuthProvider;

try {
  app = !getApps().length ? initializeApp(firebaseConfig) : getApp();
  auth = getAuth(app);
  googleProvider = new GoogleAuthProvider();
} catch {
  // Safe fallback during SSR / Next.js static prerendering
  app = {} as FirebaseApp;
  auth = {} as Auth;
  googleProvider = {} as GoogleAuthProvider;
}

export const setupRecaptcha = (containerId: string = "auth-recaptcha-container") => {
  if (typeof window === "undefined" || !auth || !auth.app) return null;
  try {
    const container = document.getElementById(containerId);
    if (!container) return null;

    if ((window as any).recaptchaVerifier) {
      try {
        (window as any).recaptchaVerifier.clear();
      } catch {}
      (window as any).recaptchaVerifier = null;
    }
    container.innerHTML = "";

    const verifier = new RecaptchaVerifier(auth, containerId, {
      size: "invisible",
      callback: () => {},
      "expired-callback": () => {
        try {
          if ((window as any).recaptchaVerifier) {
            (window as any).recaptchaVerifier.clear();
          }
        } catch {}
      },
    });
    (window as any).recaptchaVerifier = verifier;
    return verifier;
  } catch (err) {
    console.warn("reCAPTCHA initialization warning:", err);
    return null;
  }
};

export const sendFirebasePhoneOtp = async (
  phoneNumber: string,
  containerId: string = "auth-recaptcha-container"
): Promise<ConfirmationResult> => {
  const verifier = setupRecaptcha(containerId);
  if (!verifier) {
    throw new Error("reCAPTCHA initialization failed. Please refresh the page.");
  }
  return await signInWithPhoneNumber(auth, phoneNumber, verifier);
};

export const formatFirebaseAuthError = (error: any): string => {
  const code = error?.code || "";
  const msg = error?.message || "";

  if (code.includes("too-many-requests") || msg.includes("too-many-requests")) {
    return "Too many OTP requests have been made from this device. Please wait 2-5 minutes before trying again, or use Email / Google login.";
  }
  if (code.includes("quota-exceeded") || msg.includes("quota-exceeded")) {
    return "SMS limit reached. Please sign in using Email or Google Account.";
  }
  if (code.includes("invalid-phone-number") || msg.includes("invalid-phone-number")) {
    return "Invalid mobile number format. Please ensure you entered a valid 10-digit number.";
  }
  if (code.includes("captcha-check-failed") || msg.includes("captcha-check-failed")) {
    return "reCAPTCHA verification failed. Please refresh the page and try again.";
  }
  if (code.includes("invalid-verification-code") || msg.includes("invalid-verification-code")) {
    return "Invalid 6-digit OTP code. Please check and try again.";
  }
  if (code.includes("code-expired") || msg.includes("code-expired")) {
    return "OTP code has expired. Please request a new code.";
  }
  if (code.includes("network-request-failed") || msg.includes("network-request-failed")) {
    return "Network error. Please check your internet connection.";
  }
  return msg || "Authentication failed. Please try again.";
};

export { app, auth, googleProvider, RecaptchaVerifier, signInWithPhoneNumber };
export type { ConfirmationResult };
