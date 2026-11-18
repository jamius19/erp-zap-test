/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8088',
    },
  },
  // Serves the production build (dist/) via `npm run preview` on the same URL as dev.
  preview: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8088',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
});
