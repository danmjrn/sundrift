import type { NextConfig } from 'next';
const backend = process.env.API_INTERNAL_URL || 'http://127.0.0.1:8080';
const config: NextConfig = {
  output: 'standalone',
  async rewrites() { return [{ source: '/api/:path*', destination: `${backend}/api/:path*` }, { source: '/media/:path*', destination: `${backend}/media/:path*` }]; },
  async headers() { return [{source:'/:path*',headers:[{key:'X-Content-Type-Options',value:'nosniff'},{key:'Referrer-Policy',value:'strict-origin-when-cross-origin'},{key:'X-Frame-Options',value:'DENY'}]}]; }
};
export default config;
