import { setupServer } from 'msw/node'
import { loggedIn } from './handlers'

/**
 * Servidor MSW compartilhado. Por padrão há um usuário logado; cada teste registra os demais
 * handlers (ou `loggedOut()`) com `server.use(...)`.
 */
export const server = setupServer(loggedIn())
