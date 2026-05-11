import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // Forward API paths to the Spring Boot backend
      '/auctions': { target: 'http://localhost:8080', changeOrigin: true },
      '/users':    { target: 'http://localhost:8080', changeOrigin: true },
      '/admin':    { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
