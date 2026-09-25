import { MetadataRoute } from "next";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = "https://www.redbus.in";

  const staticPages: MetadataRoute.Sitemap = [
    {
      url: baseUrl,
      lastModified: new Date(),
      changeFrequency: "daily",
      priority: 1.0,
    },
    {
      url: `${baseUrl}/faq`,
      lastModified: new Date(),
      changeFrequency: "weekly",
      priority: 0.8,
    },
  ];

  // Dynamic route pairs for programmatic SEO
  const routes = [
    "bangalore-to-chennai",
    "chennai-to-bangalore",
    "mumbai-to-pune",
    "delhi-to-jaipur",
    "hyderabad-to-bangalore",
  ];

  const routePages: MetadataRoute.Sitemap = routes.map((r) => ({
    url: `${baseUrl}/bus-tickets/${r}`,
    lastModified: new Date(),
    changeFrequency: "daily",
    priority: 0.9,
  }));

  return [...staticPages, ...routePages];
}
