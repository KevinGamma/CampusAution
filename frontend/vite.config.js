import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // All API calls are now under /api/v1 — a single rule covers every endpoint.
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
