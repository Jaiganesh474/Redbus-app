import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const backendUrl =
      process.env.NEXT_PUBLIC_API_URL || "https://redbus-api.duckdns.org/api/v1";
    const baseTarget = backendUrl.replace(/\/api\/v1\/?$/, "");

    return [
      {
        source: "/api/v1/:path*",
        destination: `${baseTarget}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
