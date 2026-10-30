import type { IncomingMessage } from 'node:http'
import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Backend local (perfil `local`) roda na 8082: 8080/8081 estão ocupadas na máquina de desenvolvimento.
const backend = process.env.BACKEND_URL ?? 'http://localhost:8082'
const proxied = ['/api', '/oauth2', '/login', '/logout', '/dev']

// `/login` e `/dev/ui` também são páginas da SPA: navegações GET para elas não vão ao backend
// (o callback `/login/oauth2/code/*` e o `POST /dev/login` continuam indo).
const spaPages = new Set(['/login', '/dev/ui'])

function serveSpaPage(req: IncomingMessage) {
  const path = req.url?.split('?')[0] ?? ''
  if (req.method === 'GET' && spaPages.has(path)) return '/index.html'
}

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    proxy: Object.fromEntries(
      proxied.map((path) => [path, { target: backend, bypass: serveSpaPage }]),
    ),
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
