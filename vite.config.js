import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
// mockApiPlugin() a été retiré : le frontend appelle désormais le vrai
// backend (voir .env -> VITE_API_URL). Le fichier mock-server/mockApiPlugin.js
// est conservé pour référence mais n'est plus utilisé.
export default defineConfig({
  plugins: [react()],
})
