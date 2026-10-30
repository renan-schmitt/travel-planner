import '@testing-library/jest-dom/vitest'
import '@/lib/i18n'
import { server } from './server'

// jsdom não implementa matchMedia (usado pelo tema "sistema").
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: () => {},
    removeEventListener: () => {},
    addListener: () => {},
    removeListener: () => {},
    dispatchEvent: () => false,
  }),
})

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  server.resetHandlers()
  localStorage.clear()
  document.documentElement.className = ''
})
afterAll(() => server.close())
