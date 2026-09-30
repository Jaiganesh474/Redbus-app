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

export { app, auth, googleProvider, RecaptchaVerifier, signInWithPhoneNumber };
export type { ConfirmationResult };


