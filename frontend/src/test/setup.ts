import '@testing-library/jest-dom/vitest'
import { configure } from '@testing-library/react'
import '@/lib/i18n'
import { server } from './server'

// O padrão de 1 s dos findBy*/waitFor é curto no runner do CI: o autocomplete de cidade gasta
// 300 ms só de debounce antes de buscar, e o teste estourava o limite de forma intermitente.
configure({ asyncUtilTimeout: 3000 })

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

// jsdom não implementa ResizeObserver (o Checkbox do Radix dentro de um <form> mede o controle).
globalThis.ResizeObserver ??= class {
  observe() {}
  unobserve() {}
  disconnect() {}
}

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  server.resetHandlers()
  localStorage.clear()
  sessionStorage.clear()
  document.documentElement.className = ''
})
afterAll(() => server.close())
