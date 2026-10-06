import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
// Le frontend appelle le vrai backend. Sans VITE_API_URL (voir .env.example),
// le client utilise la base relative "/api" : en développement, ce proxy la
// redirige vers le backend Spring Boot (context-path /api, port 8080).
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
