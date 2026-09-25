export interface AiBusPhoto {
  url: string;
  fallbackUrl?: string;
  type: "EXTERIOR" | "SLEEPER_CABIN" | "SEATER_ROW" | "COCKPIT" | "AMENITY";
  title: string;
  caption: string;
  prompt: string;
}

export interface AiBusTheme {
  id: string;
  name: string;
  badge: string;
  badgeColor: string;
  busType: string;
  accentColor: string;
  photos: AiBusPhoto[];
  joinedUrls: string;
}

// Fallback verified real bus stock photos (Bus exteriors, bus sleeper berths, bus seats, bus cockpits)
const VERIFIED_BUS_EXTERIORS = [
  "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80", // Red multi-axle coach
  "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?auto=format&fit=crop&w=1200&q=80", // Blue tour coach
  "https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1200&q=80", // Highway bus on road
];

const VERIFIED_BUS_SLEEPERS = [
  "https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1540518614846-7ede433c4550?auto=format&fit=crop&w=1200&q=80",
];

const VERIFIED_BUS_SEATERS = [
  "https://images.unsplash.com/photo-1587825140708-dfaf72ae4b04?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1200&q=80",
];

const VERIFIED_BUS_COCKPITS = [
  "https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1200&q=80",
];

const VERIFIED_BUS_AMENITIES = [
  "https://images.unsplash.com/photo-1540518614846-7ede433c4550?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1200&q=80",
];

export function buildAiPromptImageUrl(prompt: string, seed: number = 42): string {
  const cleanPrompt = prompt.replace(/[^\w\s,.-]/g, "").trim();
  return `https://image.pollinations.ai/prompt/${encodeURIComponent(cleanPrompt)}?width=1200&height=675&nologo=true&seed=${seed}`;
}

export const AI_BUS_THEMES: AiBusTheme[] = [
  {
    id: "volvo-9600-crimson",
    name: "Volvo 9600 Crimson Flagship",
    badge: "Flagship 9600",
    badgeColor: "bg-red-500/10 text-red-600 border-red-200",
    busType: "AC Sleeper (2+1)",
    accentColor: "from-red-600 to-rose-700",
    photos: [
      {
        url: buildAiPromptImageUrl("Cinematic photorealistic 8k photo of Volvo 9600 multi-axle luxury crimson sleeper bus exterior speeding on highway sunset golden hour, crystal clear coach details", 101),
        fallbackUrl: VERIFIED_BUS_EXTERIORS[0],
        type: "EXTERIOR",
        title: "Crimson Multi-Axle Volvo Coach",
        caption: "Aerodynamic luxury highway coach with dynamic LED matrix illumination.",
        prompt: "Cinematic photorealistic 8k photo of Volvo 9600 multi-axle luxury crimson sleeper bus exterior speeding on highway sunset golden hour, crystal clear coach details",
      },
      {
        url: buildAiPromptImageUrl("Interior photorealistic shot of Indian luxury AC sleeper bus 2+1 private berth pods with soft cyan ambient LED lighting, memory foam mattress, privacy curtains", 102),
        fallbackUrl: VERIFIED_BUS_SLEEPERS[0],
        type: "SLEEPER_CABIN",
        title: "VIP 2+1 Luxury Sleeper Berths",
        caption: "Individual privacy curtains, memory foam mattress, and reading spotlights.",
        prompt: "Interior photorealistic shot of Indian luxury AC sleeper bus 2+1 private berth pods with soft cyan ambient LED lighting, memory foam mattress, privacy curtains",
      },
      {
        url: buildAiPromptImageUrl("Commercial intercity bus driver cockpit station with wide front windshield view on open expressway, modern digital dashboard gauges, steering wheel", 103),
        fallbackUrl: VERIFIED_BUS_COCKPITS[0],
        type: "COCKPIT",
        title: "Panoramic Highway Cockpit",
        caption: "ADAS collision alert, retarder braking system and dual GPS dashboard.",
        prompt: "Commercial intercity bus driver cockpit station with wide front windshield view on open expressway, modern digital dashboard gauges, steering wheel",
      },
      {
        url: buildAiPromptImageUrl("Macro closeup of modern sleeper bus passenger amenities, individual overhead AC vent nozzles, reading spot lamp, USB-C fast charging port, clean blanket", 104),
        fallbackUrl: VERIFIED_BUS_AMENITIES[0],
        type: "AMENITY",
        title: "AC Vents & Fast USB Charging Hub",
        caption: "Individual climate control louvers, USB-C fast charging, and bottle holder.",
        prompt: "Macro closeup of modern sleeper bus passenger amenities, individual overhead AC vent nozzles, reading spot lamp, USB-C fast charging port, clean blanket",
      },
    ],
    joinedUrls: [
      buildAiPromptImageUrl("Cinematic photorealistic 8k photo of Volvo 9600 multi-axle luxury crimson sleeper bus exterior speeding on highway sunset golden hour, crystal clear coach details", 101),
      buildAiPromptImageUrl("Interior photorealistic shot of Indian luxury AC sleeper bus 2+1 private berth pods with soft cyan ambient LED lighting, memory foam mattress, privacy curtains", 102),
      buildAiPromptImageUrl("Commercial intercity bus driver cockpit station with wide front windshield view on open expressway, modern digital dashboard gauges, steering wheel", 103),
      buildAiPromptImageUrl("Macro closeup of modern sleeper bus passenger amenities, individual overhead AC vent nozzles, reading spot lamp, USB-C fast charging port, clean blanket", 104),
    ].join(","),
  },
  {
    id: "scania-touring-ocean-blue",
    name: "Scania Touring Ocean Blue",
    badge: "Euro-6 Turbo",
    badgeColor: "bg-blue-500/10 text-blue-600 border-blue-200",
    busType: "AC Sleeper (2+1)",
    accentColor: "from-blue-600 to-indigo-700",
    photos: [
      {
        url: buildAiPromptImageUrl("Photorealistic exterior shot of metallic royal blue Scania multi-axle sleeper bus moving fast on mountain highway, ultra detailed coach design 4k", 201),
        fallbackUrl: VERIFIED_BUS_EXTERIORS[1],
        type: "EXTERIOR",
        title: "Ocean Blue Metallic Multi-Axle",
        caption: "Euro-6 compliant touring coach with solar-tinted panoramic windows.",
        prompt: "Photorealistic exterior shot of metallic royal blue Scania multi-axle sleeper bus moving fast on mountain highway, ultra detailed coach design 4k",
      },
      {
        url: buildAiPromptImageUrl("Luxury bus upper deck stargazer sleeper berth interior with wide window view, plush travel bedding, gentle blue night mood lights", 202),
        fallbackUrl: VERIFIED_BUS_SLEEPERS[1],
        type: "SLEEPER_CABIN",
        title: "Upper Deck Stargazer Sleeper Berth",
        caption: "Sound-dampened acoustic cabin with wide panoramic window view.",
        prompt: "Luxury bus upper deck stargazer sleeper berth interior with wide window view, plush travel bedding, gentle blue night mood lights",
      },
      {
        url: buildAiPromptImageUrl("Front driver dashboard cockpit of high-tech modern tourist bus with GPS navigation screen and steering controls overlooking highway", 203),
        fallbackUrl: VERIFIED_BUS_COCKPITS[0],
        type: "COCKPIT",
        title: "Electronic Steering & Highway HUD Console",
        caption: "Speed governor and electronic stability management system.",
        prompt: "Front driver dashboard cockpit of high-tech modern tourist bus with GPS navigation screen and steering controls overlooking highway",
      },
      {
        url: buildAiPromptImageUrl("Sleeper coach passenger cabin amenities, reading LED spotlight, personal AC airflow control, USB mobile charging station", 204),
        fallbackUrl: VERIFIED_BUS_AMENITIES[0],
        type: "AMENITY",
        title: "Night LED Mood Ambience & Sanitized Bedding",
        caption: "Anti-glare reading spot lamps, sanitized fleece blanket, and personal AC airflow.",
        prompt: "Sleeper coach passenger cabin amenities, reading LED spotlight, personal AC airflow control, USB mobile charging station",
      },
    ],
    joinedUrls: [
      buildAiPromptImageUrl("Photorealistic exterior shot of metallic royal blue Scania multi-axle sleeper bus moving fast on mountain highway, ultra detailed coach design 4k", 201),
      buildAiPromptImageUrl("Luxury bus upper deck stargazer sleeper berth interior with wide window view, plush travel bedding, gentle blue night mood lights", 202),
      buildAiPromptImageUrl("Front driver dashboard cockpit of high-tech modern tourist bus with GPS navigation screen and steering controls overlooking highway", 203),
      buildAiPromptImageUrl("Sleeper coach passenger cabin amenities, reading LED spotlight, personal AC airflow control, USB mobile charging station", 204),
    ].join(","),
  },
  {
    id: "royal-maharaja-double-decker",
    name: "Royal Maharaja Double Decker",
    badge: "Double Decker VIP",
    badgeColor: "bg-amber-600/10 text-amber-700 border-amber-300",
    busType: "AC Sleeper (2+1)",
    accentColor: "from-amber-700 to-yellow-900",
    photos: [
      {
        url: buildAiPromptImageUrl("Grand double-decker luxury golden sleeper bus exterior standing at futuristic terminal, photorealistic coach vehicle 8k", 301),
        fallbackUrl: VERIFIED_BUS_EXTERIORS[0],
        type: "EXTERIOR",
        title: "Grand High-Floor Double Decker Coach",
        caption: "High-capacity dual-level intercity flagship with panoramic front sky-lounge.",
        prompt: "Grand double-decker luxury golden sleeper bus exterior standing at futuristic terminal, photorealistic coach vehicle 8k",
      },
      {
        url: buildAiPromptImageUrl("Upper floor luxury sleeper bus master cabin interior, individual gold and bronze curtains, wide stargazing glass window, ambient yellow lights", 302),
        fallbackUrl: VERIFIED_BUS_SLEEPERS[0],
        type: "SLEEPER_CABIN",
        title: "Upper Deck Master Sleeper Suites",
        caption: "Unmatched bird's-eye highway views, plush memory foam and personal temperature dial.",
        prompt: "Upper floor luxury sleeper bus master cabin interior, individual gold and bronze curtains, wide stargazing glass window, ambient yellow lights",
      },
      {
        url: buildAiPromptImageUrl("Bus cockpit with digital instrument cluster and multi-camera display overlooking grand highway", 303),
        fallbackUrl: VERIFIED_BUS_COCKPITS[0],
        type: "COCKPIT",
        title: "Driver Command Station & Telematics",
        caption: "Multi-angle blind spot cameras and automated vehicle air-suspension leveling.",
        prompt: "Bus cockpit with digital instrument cluster and multi-camera display overlooking grand highway",
      },
      {
        url: buildAiPromptImageUrl("First class bus passenger luxury travel kit, warm fleece blanket, personal water dock, reading light, AC louvers", 304),
        fallbackUrl: VERIFIED_BUS_AMENITIES[0],
        type: "AMENITY",
        title: "Premium Travel Comfort Kit & USB Power",
        caption: "Sanitary comfort kit, eye-mask, luxury fleece blanket, and warm beverage dock.",
        prompt: "First class bus passenger luxury travel kit, warm fleece blanket, personal water dock, reading light, AC louvers",
      },
    ],
    joinedUrls: [
      buildAiPromptImageUrl("Grand double-decker luxury golden sleeper bus exterior standing at futuristic terminal, photorealistic coach vehicle 8k", 301),
      buildAiPromptImageUrl("Upper floor luxury sleeper bus master cabin interior, individual gold and bronze curtains, wide stargazing glass window, ambient yellow lights", 302),
      buildAiPromptImageUrl("Bus cockpit with digital instrument cluster and multi-camera display overlooking grand highway", 303),
      buildAiPromptImageUrl("First class bus passenger luxury travel kit, warm fleece blanket, personal water dock, reading light, AC louvers", 304),
    ].join(","),
  },
  {
    id: "mercedes-silver-executive",
    name: "Mercedes-Benz Silver Jet Club",
    badge: "Air Suspension VIP",
    badgeColor: "bg-slate-500/10 text-slate-700 border-slate-200",
    busType: "AC Seater (2+2)",
    accentColor: "from-slate-700 to-gray-900",
    photos: [
      {
        url: buildAiPromptImageUrl("Sleek silver metallic Mercedes-Benz luxury intercity passenger bus exterior parked on airport highway ramp, 8k automotive rendering", 401),
        fallbackUrl: VERIFIED_BUS_EXTERIORS[1],
        type: "EXTERIOR",
        title: "Silver Executive Touring Coach",
        caption: "Hydrostatic air suspension, low aerodynamic drag, and alloy wheels.",
        prompt: "Sleek silver metallic Mercedes-Benz luxury intercity passenger bus exterior parked on airport highway ramp, 8k automotive rendering",
      },
      {
        url: buildAiPromptImageUrl("Interior of luxury commercial tour bus showing rows of 2+2 plush black leather reclining seats, central clean aisle, blue mood lighting", 402),
        fallbackUrl: VERIFIED_BUS_SEATERS[0],
        type: "SEATER_ROW",
        title: "2+2 Luxury Reclining Passenger Seats",
        caption: "145° reclining ergonomics with calf-support, cushioned headrests, and center armrests.",
        prompt: "Interior of luxury commercial tour bus showing rows of 2+2 plush black leather reclining seats, central clean aisle, blue mood lighting",
      },
      {
        url: buildAiPromptImageUrl("Modern tourist coach driver dashboard with leather steering wheel, navigation map, panoramic highway view", 403),
        fallbackUrl: VERIFIED_BUS_COCKPITS[0],
        type: "COCKPIT",
        title: "Intelligent Cruise Control Console",
        caption: "Dual camera telematics with real-time lane departure alerts.",
        prompt: "Modern tourist coach driver dashboard with leather steering wheel, navigation map, panoramic highway view",
      },
      {
        url: buildAiPromptImageUrl("Bus passenger seat back folding snack tray table, cup holder, USB charging phone dock, AC vent above", 404),
        fallbackUrl: VERIFIED_BUS_AMENITIES[1],
        type: "AMENITY",
        title: "Foldable Snack Tray & Universal In-Seat Power",
        caption: "Dedicated laptop workspace, cup holder, overhead baggage rack, and high-speed USB power.",
        prompt: "Bus passenger seat back folding snack tray table, cup holder, USB charging phone dock, AC vent above",
      },
    ],
    joinedUrls: [
      buildAiPromptImageUrl("Sleek silver metallic Mercedes-Benz luxury intercity passenger bus exterior parked on airport highway ramp, 8k automotive rendering", 401),
      buildAiPromptImageUrl("Interior of luxury commercial tour bus showing rows of 2+2 plush black leather reclining seats, central clean aisle, blue mood lighting", 402),
      buildAiPromptImageUrl("Modern tourist coach driver dashboard with leather steering wheel, navigation map, panoramic highway view", 403),
      buildAiPromptImageUrl("Bus passenger seat back folding snack tray table, cup holder, USB charging phone dock, AC vent above", 404),
    ].join(","),
  },
  {
    id: "greenvolt-electric-intercity",
    name: "GreenVolt Zero-Emission E-Bus",
    badge: "Eco-EV Clean Energy",
    badgeColor: "bg-teal-500/10 text-teal-600 border-teal-200",
    busType: "AC Seater (2+2)",
    accentColor: "from-teal-600 to-emerald-700",
    photos: [
      {
        url: buildAiPromptImageUrl("Futuristic emerald green and white zero-emission electric passenger bus exterior driving on modern solar expressway, ultra sharp 8k", 501),
        fallbackUrl: VERIFIED_BUS_EXTERIORS[0],
        type: "EXTERIOR",
        title: "GreenVolt Aerodynamic E-Bus",
        caption: "100% electric intercity coach with zero emissions and whisper-quiet motor.",
        prompt: "Futuristic emerald green and white zero-emission electric passenger bus exterior driving on modern solar expressway, ultra sharp 8k",
      },
      {
        url: buildAiPromptImageUrl("Modern eco-friendly bus interior with ergonomic fabric reclining seats 2+2 layout, bright clean windows, HEPA air purification ducts", 502),
        fallbackUrl: VERIFIED_BUS_SEATERS[1],
        type: "SEATER_ROW",
        title: "Whisper-Quiet Eco Cabin & Recliners",
        caption: "Zero engine vibration with HEPA allergen air purification and ergonomic pushback seating.",
        prompt: "Modern eco-friendly bus interior with ergonomic fabric reclining seats 2+2 layout, bright clean windows, HEPA air purification ducts",
      },
      {
        url: buildAiPromptImageUrl("Electric coach bus driver console showing digital battery health gauge, power kilowatt display, modern steering", 503),
        fallbackUrl: VERIFIED_BUS_COCKPITS[0],
        type: "COCKPIT",
        title: "EV Telemetry & Battery Health Console",
        caption: "Regenerative braking telemetry and dynamic range prediction dashboard.",
        prompt: "Electric coach bus driver console showing digital battery health gauge, power kilowatt display, modern steering",
      },
      {
        url: buildAiPromptImageUrl("Clean modern bus passenger armrest with wireless qi charging pad, overhead personal air vent nozzle, reading light", 504),
        fallbackUrl: VERIFIED_BUS_AMENITIES[1],
        type: "AMENITY",
        title: "HEPA Fresh Air Vents & Fast Charging",
        caption: "Live PM2.5 air filtration status and high-speed USB-C charging on seat armrests.",
        prompt: "Clean modern bus passenger armrest with wireless qi charging pad, overhead personal air vent nozzle, reading light",
      },
    ],
    joinedUrls: [
      buildAiPromptImageUrl("Futuristic emerald green and white zero-emission electric passenger bus exterior driving on modern solar expressway, ultra sharp 8k", 501),
      buildAiPromptImageUrl("Modern eco-friendly bus interior with ergonomic fabric reclining seats 2+2 layout, bright clean windows, HEPA air purification ducts", 502),
      buildAiPromptImageUrl("Electric coach bus driver console showing digital battery health gauge, power kilowatt display, modern steering", 503),
      buildAiPromptImageUrl("Clean modern bus passenger armrest with wireless qi charging pad, overhead personal air vent nozzle, reading light", 504),
    ].join(","),
  },
];

/**
 * Dynamically synthesizes an AI Bus Theme and 4 multi-angle prompts matching the exact operator name and bus type
 */
export function synthesizeAiBusSuite(params: {
  operatorName?: string;
  busType?: string;
  modelStyle?: string;
  customKeyword?: string;
}): AiBusTheme {
  const op = params.operatorName?.trim() || "Express Coach";
  const type = params.busType?.trim() || "AC Sleeper (2+1)";
  const style = params.modelStyle?.trim() || "Volvo 9600 Crimson Flagship";
  const isSleeper = type.toLowerCase().includes("sleeper");

  const seed = Math.floor(Math.random() * 90000) + 10000;

  const exteriorPrompt = `Cinematic photorealistic 8k photo of ${op} luxury ${style} ${type} multi-axle passenger highway bus exterior driving on scenic expressway at golden hour, sharp reflections, modern commercial coach`;
  
  const interiorPrompt = isSleeper
    ? `Photorealistic interior of ${op} ${type} Indian luxury sleeper bus, private 2+1 sleeper berths with soft cyan and warm LED mood lighting, plush memory foam mattress, privacy curtains, ultra clean`
    : `Interior passenger cabin of ${op} luxury ${type} commercial intercity coach, 2+2 plush leather reclining seats, central wide aisle, clean panoramic glass windows, overhead ambient ceiling lights`;

  const cockpitPrompt = `Modern commercial highway bus driver cockpit for ${op} with panoramic windshield road view, digital dashboard telematics, steering wheel controls, 8k`;

  const amenityPrompt = isSleeper
    ? `Macro closeup of sleeper bus passenger berth amenities, individual directional AC vents, personal reading lamp, high-speed USB-C charging hub, sanitized blanket dock`
    : `Passenger seat comfort features on luxury coach, in-seat USB charging port, folding snack tray table, cup holder, overhead individual AC airflow nozzle`;

  const exteriorUrl = buildAiPromptImageUrl(exteriorPrompt, seed);
  const interiorUrl = buildAiPromptImageUrl(interiorPrompt, seed + 1);
  const cockpitUrl = buildAiPromptImageUrl(cockpitPrompt, seed + 2);
  const amenityUrl = buildAiPromptImageUrl(amenityPrompt, seed + 3);

  const photos: AiBusPhoto[] = [
    {
      url: exteriorUrl,
      fallbackUrl: VERIFIED_BUS_EXTERIORS[seed % VERIFIED_BUS_EXTERIORS.length],
      type: "EXTERIOR",
      title: `${style} Exterior Profile`,
      caption: `Aerodynamic multi-axle ${type} flagship with dynamic LED illumination.`,
      prompt: exteriorPrompt,
    },
    {
      url: interiorUrl,
      fallbackUrl: isSleeper ? VERIFIED_BUS_SLEEPERS[0] : VERIFIED_BUS_SEATERS[0],
      type: isSleeper ? "SLEEPER_CABIN" : "SEATER_ROW",
      title: isSleeper ? "2+1 Luxury Sleeper Berths" : "2+2 Luxury Reclining Seater Row",
      caption: isSleeper
        ? "Private sleeper pods with memory foam mattress & individual privacy curtains."
        : "Ergonomic 145° reclining leather seats with center armrests & footrests.",
      prompt: interiorPrompt,
    },
    {
      url: cockpitUrl,
      fallbackUrl: VERIFIED_BUS_COCKPITS[0],
      type: "COCKPIT",
      title: "Navigation & Telematics Cockpit",
      caption: "ADAS safety system, speed governor and dual GPS dashboard.",
      prompt: cockpitPrompt,
    },
    {
      url: amenityUrl,
      fallbackUrl: VERIFIED_BUS_AMENITIES[0],
      type: "AMENITY",
      title: "Passenger AC & Fast Power Hub",
      caption: "Personal airflow nozzles, USB-C fast charging, and reading lamps.",
      prompt: amenityPrompt,
    },
  ];

  return {
    id: `custom-ai-${seed}`,
    name: `${op} - ${style}`,
    badge: "AI Studio 8K",
    badgeColor: "bg-red-500/10 text-red-600 border-red-200",
    busType: type,
    accentColor: "from-red-600 to-rose-700",
    photos,
    joinedUrls: [exteriorUrl, interiorUrl, cockpitUrl, amenityUrl].join(","),
  };
}

export function getNextAiBusTheme(currentUrl?: string, busType?: string): AiBusTheme {
  const isSleeper = (busType || "").toLowerCase().includes("sleeper");
  const filtered = AI_BUS_THEMES.filter((theme) => {
    if (isSleeper) return theme.busType.toLowerCase().includes("sleeper");
    return true;
  });

  const validPool = filtered.length > 0 ? filtered : AI_BUS_THEMES;
  if (!currentUrl) {
    return validPool[0];
  }

  const currentIndex = validPool.findIndex((t) => t.joinedUrls === currentUrl || currentUrl.includes(t.photos[0].url));
  if (currentIndex === -1) {
    return validPool[0];
  }

  return validPool[(currentIndex + 1) % validPool.length];
}

export function getRandomAiBusTheme(currentUrl?: string, busType?: string): AiBusTheme {
  const isSleeper = (busType || "").toLowerCase().includes("sleeper");
  const pool = AI_BUS_THEMES.filter((theme) => {
    if (isSleeper) return theme.busType.toLowerCase().includes("sleeper");
    return true;
  });

  const validPool = pool.length > 0 ? pool : AI_BUS_THEMES;
  const nonCurrent = validPool.filter((t) => t.joinedUrls !== currentUrl && !currentUrl?.includes(t.photos[0].url));

  if (nonCurrent.length > 0) {
    const randomIndex = Math.floor(Math.random() * nonCurrent.length);
    return nonCurrent[randomIndex];
  }

  return getNextAiBusTheme(currentUrl, busType);
}
