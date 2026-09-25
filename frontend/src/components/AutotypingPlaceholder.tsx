"use client";

import React, { useState, useEffect } from "react";

export interface AutotypingPlaceholderProps {
  phrases?: string[];
  typingSpeed?: number;
  deletingSpeed?: number;
  pauseDelay?: number;
  className?: string;
  cursorClassName?: string;
}

export const DEFAULT_AI_SEARCH_PHRASES = [
  "Search 'Primo bus with live tracking'",
  "Search 'need bus between chennai and bengaluru'",
  "Search 'AC sleeper under 1000'",
  "Search 'Morning bus before 11 AM'",
  "Search 'Night AC Sleeper with GPS'",
  "Search 'Sleeper under 800'",
  "Search 'Volvo multi-axle AC'",
];

export default function AutotypingPlaceholder({
  phrases = DEFAULT_AI_SEARCH_PHRASES,
  typingSpeed = 50,
  deletingSpeed = 25,
  pauseDelay = 1800,
  className = "text-xs font-semibold text-gray-400 dark:text-slate-400",
  cursorClassName = "bg-purple-600 dark:bg-purple-400",
}: AutotypingPlaceholderProps) {
  const [displayText, setDisplayText] = useState("");
  const [phraseIndex, setPhraseIndex] = useState(0);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    if (!phrases || phrases.length === 0) return;

    const currentPhrase = phrases[phraseIndex % phrases.length];

    // If full phrase is typed, pause before deleting
    if (!isDeleting && displayText === currentPhrase) {
      const pauseTimer = setTimeout(() => {
        setIsDeleting(true);
      }, pauseDelay);
      return () => clearTimeout(pauseTimer);
    }

    // If completely erased, wait briefly then move to next phrase
    if (isDeleting && displayText === "") {
      const switchTimer = setTimeout(() => {
        setIsDeleting(false);
        setPhraseIndex((prev) => (prev + 1) % phrases.length);
      }, 250);
      return () => clearTimeout(switchTimer);
    }

    const speed = isDeleting ? deletingSpeed : typingSpeed;
    const timer = setTimeout(() => {
      setDisplayText((prev) => {
        if (isDeleting) {
          return currentPhrase.substring(0, prev.length - 1);
        } else {
          return currentPhrase.substring(0, prev.length + 1);
        }
      });
    }, speed);

    return () => clearTimeout(timer);
  }, [displayText, isDeleting, phraseIndex, phrases, typingSpeed, deletingSpeed, pauseDelay]);

  return (
    <div className={`pointer-events-none flex items-center select-none overflow-hidden max-w-full ${className}`}>
      <span className="truncate">{displayText}</span>
      <span
        className={`inline-block w-[1.5px] h-3.5 ml-0.5 animate-pulse shrink-0 ${cursorClassName}`}
        aria-hidden="true"
      />
    </div>
  );
}
