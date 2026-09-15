import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { loadEnv } from 'vite';

const apiTarget = loadEnv('development', '.', 'API_').API_PROXY_TARGET ?? 'http://127.0.0.1:8085';

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    headers: {
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'Referrer-Policy': 'no-referrer',
      'Content-Security-Policy': "frame-ancestors 'none'; object-src 'none'; base-uri 'self'",
    },
    port: 5173,
    strictPort: true,
    cors: false,
    proxy: {
      '/api': { target: apiTarget, changeOrigin: false },
      '/actuator': apiTarget,
      '/openapi.json': apiTarget,
      '/__test': apiTarget,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    clearMocks: true,
  },
});
