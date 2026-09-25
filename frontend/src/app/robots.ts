import { MetadataRoute } from "next";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: "*",
        allow: ["/", "/bus-tickets/*", "/faq"],
        disallow: ["/admin", "/my-bookings", "/checkout", "/api/*"],
      },
    ],
    sitemap: "https://www.redbus.in/sitemap.xml",
  };
}
