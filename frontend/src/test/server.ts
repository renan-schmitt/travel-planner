import { setupServer } from 'msw/node'
import { dashboard } from './dashboard'
import { loggedIn } from './handlers'

/**
 * Servidor MSW compartilhado. Por padrão há um usuário logado (e o dashboard vazio, que é a tela
 * inicial); cada teste registra os demais handlers (ou
 * `loggedOut()`) com `server.use(...)`.
 */
export const server = setupServer(loggedIn(), dashboard().handler)
