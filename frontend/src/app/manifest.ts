import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "redBus - Online Bus Ticket Booking",
    short_name: "redBus",
    description: "Book Bus Tickets Online across 30,000+ routes with AI assistant and live seat tracking",
    start_url: "/",
    display: "standalone",
    background_color: "#FFFFFF",
    theme_color: "#D84E55",
    icons: [
      {
        src: "/favicon.svg",
        sizes: "any",
        type: "image/svg+xml",
      },
    ],
  };
}
