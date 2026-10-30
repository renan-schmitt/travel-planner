import createClient, { type Middleware } from 'openapi-fetch'
import type { paths } from '@/api/schema'

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE'])

export const CSRF_COOKIE = 'XSRF-TOKEN'
export const CSRF_HEADER = 'X-XSRF-TOKEN'

export function readCookie(name: string): string | undefined {
  const prefix = `${name}=`
  const cookie = document.cookie.split('; ').find((c) => c.startsWith(prefix))
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : undefined
}

/** Envia o token CSRF do cookie `XSRF-TOKEN` (Spring Security) em métodos que alteram estado. */
export const csrfMiddleware: Middleware = {
  onRequest({ request }) {
    if (SAFE_METHODS.has(request.method)) return
    const token = readCookie(CSRF_COOKIE)
    if (token) request.headers.set(CSRF_HEADER, token)
    return request
  },
}

let unauthorizedHandler: () => void = () => window.location.assign('/login')

/** Define o que fazer num 401 (o app usa o router; os testes, um mock). */
export function setUnauthorizedHandler(handler: () => void) {
  unauthorizedHandler = handler
}

/** Sessão ausente ou expirada: manda para `/login`. */
export const unauthorizedMiddleware: Middleware = {
  onResponse({ response }) {
    if (response.status === 401 && window.location.pathname !== '/login') unauthorizedHandler()
    return response
  },
}

export function createApiClient() {
  const client = createClient<paths>({ baseUrl: window.location.origin })
  client.use(csrfMiddleware, unauthorizedMiddleware)
  return client
}

/** Cliente tipado da API (`src/api/schema.d.ts`, gerado por `npm run gen:api`). */
export const api = createApiClient()
