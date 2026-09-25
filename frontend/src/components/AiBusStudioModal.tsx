"use client";

import React, { useState, useEffect } from "react";
import {
  Sparkles,
  Wand2,
  RefreshCw,
  CheckCircle2,
  Cpu,
  Layers,
  Camera,
  Compass,
  Wind,
  Bed,
  Check,
  X,
  Sliders,
  Terminal,
} from "lucide-react";
import {
  AiBusTheme,
  AI_BUS_THEMES,
  synthesizeAiBusSuite,
  getNextAiBusTheme,
} from "@/data/aiBusPhotoSuites";
import BusImageSlider from "./BusImageSlider";

interface AiBusStudioModalProps {
  isOpen: boolean;
  onClose: () => void;
  operatorName: string;
  busType: string;
  currentPhotos?: string;
  onApplyPhotos: (joinedUrls: string, themeName: string) => void;
}

const GENERATION_STEPS = [
  {
    step: 1,
    title: "Synthesizing AI Fleet Prompts",
    detail: "Analyzing coach aerodynamic profile, chassis specs & layout requirements...",
    icon: Terminal,
  },
  {
    step: 2,
    title: "Rendering 8K Coach Exterior",
    detail: "Synthesizing multi-axle highway coach exterior with LED matrix lighting...",
    icon: Camera,
  },
  {
    step: 3,
    title: "Generating Passenger Cabin & Berths",
    detail: "Synthesizing 2+1 sleeper berths with ambient mood lighting & memory foam...",
    icon: Bed,
  },
  {
    step: 4,
    title: "Calibrating Driver Cockpit & Telematics",
    detail: "Rendering commercial driver station, panoramic windshield & navigation HUD...",
    icon: Compass,
  },
  {
    step: 5,
    title: "Finalizing Studio Suite & AC Amenities",
    detail: "Attaching individual AC airflow louvers, fast USB-C power docks & 4K metadata...",
    icon: Wind,
  },
];

export default function AiBusStudioModal({
  isOpen,
  onClose,
  operatorName,
  busType,
  currentPhotos,
  onApplyPhotos,
}: AiBusStudioModalProps) {
  const [selectedThemeId, setSelectedThemeId] = useState<string>("volvo-9600-crimson");
  const [customKeyword, setCustomKeyword] = useState<string>("");
  const [isGenerating, setIsGenerating] = useState<boolean>(false);
  const [currentStepIndex, setCurrentStepIndex] = useState<number>(0);
  const [progress, setProgress] = useState<number>(0);
  const [generatedSuite, setGeneratedSuite] = useState<AiBusTheme | null>(null);

  // Initialize with matching theme or synthesis when modal opens
  useEffect(() => {
    if (isOpen) {
      const isSleeper = (busType || "").toLowerCase().includes("sleeper");
      const initial = AI_BUS_THEMES.find((t) =>
        isSleeper ? t.busType.includes("Sleeper") : !t.busType.includes("Sleeper")
      ) || AI_BUS_THEMES[0];

      setSelectedThemeId(initial.id);
      setGeneratedSuite(initial);
      setIsGenerating(false);
      setProgress(100);
    }
  }, [isOpen, busType]);

  const handleStartGeneration = (themeIdToUse?: string) => {
    setIsGenerating(true);
    setCurrentStepIndex(0);
    setProgress(5);

    const themeToUse = themeIdToUse || selectedThemeId;
    const baseTheme = AI_BUS_THEMES.find((t) => t.id === themeToUse);
    const styleName = baseTheme ? baseTheme.name : "Luxury Express Flagship";

    // Step 1
    setTimeout(() => {
      setCurrentStepIndex(1);
      setProgress(25);
    }, 450);

    // Step 2
    setTimeout(() => {
      setCurrentStepIndex(2);
      setProgress(50);
    }, 900);

    // Step 3
    setTimeout(() => {
      setCurrentStepIndex(3);
      setProgress(75);
    }, 1350);

    // Step 4
    setTimeout(() => {
      setCurrentStepIndex(4);
      setProgress(90);
    }, 1800);

    // Finalize
    setTimeout(() => {
      const synthesized = synthesizeAiBusSuite({
        operatorName: operatorName || "Express Coach",
        busType: busType || "AC Sleeper (2+1)",
        modelStyle: styleName,
        customKeyword: customKeyword,
      });

      setGeneratedSuite(synthesized);
      setProgress(100);
      setIsGenerating(false);
    }, 2200);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in">
      <div className="bg-slate-900 border border-slate-700/80 rounded-3xl max-w-2xl w-full overflow-hidden shadow-2xl relative max-h-[92vh] flex flex-col text-white">
        {/* Header */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-gradient-to-tr from-amber-500/20 to-red-500/20 border border-amber-500/30 text-amber-400 rounded-2xl shadow-inner">
              <Sparkles className="w-5 h-5 text-amber-400 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-black tracking-tight text-white">
                  AI Bus Studio & Prompt Synthesizer
                </h3>
                <span className="px-2 py-0.5 bg-amber-500/10 text-amber-300 border border-amber-500/30 rounded-full text-[10px] font-bold">
                  Nano-Vision 8K
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Generates 4 multi-angle bus exterior, sleeper/seater cabin, cockpit & AC amenity images
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-5">
          {/* Target Fleet Specs Banner */}
          <div className="p-3 bg-slate-800/60 rounded-2xl border border-slate-700/50 flex flex-wrap items-center justify-between gap-3 text-xs">
            <div className="flex items-center gap-2">
              <span className="text-slate-400">Operator:</span>
              <span className="font-bold text-white bg-slate-700/50 px-2 py-0.5 rounded-lg">
                {operatorName || "Express Coach"}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-slate-400">Layout:</span>
              <span className="font-bold text-amber-400 bg-amber-400/10 border border-amber-400/20 px-2 py-0.5 rounded-lg">
                {busType || "AC Sleeper (2+1)"}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-slate-400">Resolution:</span>
              <span className="font-bold text-emerald-400 bg-emerald-400/10 border border-emerald-400/20 px-2 py-0.5 rounded-lg">
                4K Multi-Angle Suite
              </span>
            </div>
          </div>

          {/* AI Model Style Presets */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                <Cpu className="w-3.5 h-3.5 text-amber-400" />
                <span>Select AI Concept Base Model</span>
              </label>
              <span className="text-[11px] text-slate-400 font-medium">Click to synthesize instantly</span>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              {AI_BUS_THEMES.map((theme) => {
                const isSelected = selectedThemeId === theme.id;
                return (
                  <button
                    key={theme.id}
                    type="button"
                    disabled={isGenerating}
                    onClick={() => {
                      setSelectedThemeId(theme.id);
                      handleStartGeneration(theme.id);
                    }}
                    className={`p-2.5 rounded-xl border text-left transition-all cursor-pointer relative overflow-hidden ${
                      isSelected
                        ? "bg-slate-800 border-amber-400/60 shadow-lg shadow-amber-500/10 ring-1 ring-amber-400"
                        : "bg-slate-800/40 border-slate-700 hover:border-slate-600 hover:bg-slate-800/80"
                    } ${isGenerating ? "opacity-50 cursor-not-allowed" : ""}`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-md bg-slate-900/60 text-slate-300">
                        {theme.busType}
                      </span>
                      {isSelected && <Check className="w-3.5 h-3.5 text-amber-400" />}
                    </div>
                    <p className="text-xs font-bold text-white truncate">{theme.name}</p>
                  </button>
                );
              })}
            </div>
          </div>

          {/* AI Generation In-Progress Overlay or Results */}
          {isGenerating ? (
            <div className="p-6 bg-slate-950/80 rounded-3xl border border-amber-500/30 space-y-4 animate-in fade-in">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded-full border-2 border-amber-400 border-t-transparent animate-spin" />
                  <span className="text-xs font-bold text-amber-400">
                    AI Studio Engine Active • Synthesizing Bus Photos...
                  </span>
                </div>
                <span className="font-mono text-xs font-bold text-slate-300">{progress}%</span>
              </div>

              {/* Progress Bar */}
              <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-amber-500 via-red-500 to-amber-400 transition-all duration-300 rounded-full"
                  style={{ width: `${progress}%` }}
                />
              </div>

              {/* Active Step Indicator */}
              <div className="space-y-2 pt-2">
                {GENERATION_STEPS.map((step, idx) => {
                  const Icon = step.icon;
                  const isCurrent = idx === currentStepIndex;
                  const isDone = idx < currentStepIndex;
                  return (
                    <div
                      key={step.step}
                      className={`flex items-start gap-3 p-2.5 rounded-xl transition-all ${
                        isCurrent
                          ? "bg-amber-500/10 border border-amber-500/30 text-white"
                          : isDone
                          ? "text-emerald-400 opacity-60"
                          : "text-slate-500 opacity-30"
                      }`}
                    >
                      <Icon className={`w-4 h-4 mt-0.5 shrink-0 ${isCurrent ? "text-amber-400 animate-bounce" : ""}`} />
                      <div className="text-xs">
                        <p className="font-bold">{step.title}</p>
                        {isCurrent && <p className="text-[11px] text-slate-300 font-mono mt-0.5">{step.detail}</p>}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : generatedSuite ? (
            <div className="space-y-4">
              {/* Studio Multi-Angle Slider */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-300 flex items-center gap-1.5">
                    <Layers className="w-3.5 h-3.5 text-amber-400" />
                    <span>Generated 4-Angle Fleet Preview</span>
                  </span>
                  <span className="text-[11px] text-emerald-400 font-bold flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Verified Bus Suite Ready</span>
                  </span>
                </div>

                <BusImageSlider
                  photoUrls={generatedSuite.joinedUrls}
                  busName={operatorName || "Coach Preview"}
                  busType={busType || "AC Sleeper"}
                  aspectRatio="video"
                  showThumbnails={true}
                />
              </div>

              {/* Generated Prompts Terminal Card */}
              <div className="p-3.5 bg-slate-950 rounded-2xl border border-slate-800 space-y-2">
                <div className="flex items-center justify-between text-[11px] font-mono text-slate-400 border-b border-slate-800/80 pb-1.5">
                  <span className="flex items-center gap-1.5 text-amber-400 font-bold">
                    <Terminal className="w-3.5 h-3.5" />
                    <span>AI Synthesizer Prompts (4-Angle Pipeline)</span>
                  </span>
                  <span>Engine: Nano-Vision v3.5</span>
                </div>
                <div className="space-y-1.5 max-h-32 overflow-y-auto font-mono text-[10px] text-slate-300 scrollbar-thin">
                  {generatedSuite.photos.map((photo, i) => (
                    <div key={i} className="p-1.5 rounded-lg bg-slate-900/80 border border-slate-800">
                      <span className="text-amber-400 font-bold uppercase">[{photo.type}]: </span>
                      <span>{photo.prompt}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : null}
        </div>

        {/* Footer Actions */}
        <div className="p-5 border-t border-slate-800 flex items-center justify-between bg-slate-950/60">
          <button
            type="button"
            disabled={isGenerating}
            onClick={() => handleStartGeneration()}
            className="px-4 py-2.5 rounded-xl border border-slate-700 bg-slate-800 hover:bg-slate-700 text-xs font-bold text-white flex items-center gap-1.5 transition-all cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isGenerating ? "animate-spin" : ""}`} />
            <span>Regenerate Fresh Angles</span>
          </button>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={isGenerating || !generatedSuite}
              onClick={() => {
                if (generatedSuite) {
                  onApplyPhotos(generatedSuite.joinedUrls, generatedSuite.name);
                  onClose();
                }
              }}
              className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-red-600 to-[#d84e55] hover:from-red-500 hover:to-red-600 text-white text-xs font-bold shadow-lg shadow-red-600/30 flex items-center gap-1.5 transition-all cursor-pointer disabled:opacity-50 transform hover:-translate-y-0.5"
            >
              <Check className="w-4 h-4" />
              <span>Apply 4-Angle Bus Suite</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
