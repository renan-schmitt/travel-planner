import { http, HttpResponse } from 'msw'
import createClient from 'openapi-fetch'
import { server } from '@/test/server'
import { csrfMiddleware, setUnauthorizedHandler, unauthorizedMiddleware } from './api'

// O schema real ainda não tem endpoints; os middlewares são testados com um contrato mínimo.
interface TestPaths {
  '/api/v1/ping': {
    parameters: { query?: never; header?: never; path?: never; cookie?: never }
    get: {
      parameters: { query?: never; header?: never; path?: never; cookie?: never }
      requestBody?: never
      responses: {
        200: { headers: Record<string, unknown>; content: { 'application/json': object } }
      }
    }
    post: {
      parameters: { query?: never; header?: never; path?: never; cookie?: never }
      requestBody?: never
      responses: {
        200: { headers: Record<string, unknown>; content: { 'application/json': object } }
      }
    }
  }
}

function client() {
  const c = createClient<TestPaths>({ baseUrl: window.location.origin })
  c.use(csrfMiddleware, unauthorizedMiddleware)
  return c
}

function captureCsrfHeader() {
  const seen: Array<string | null> = []
  const capture = ({ request }: { request: Request }) => {
    seen.push(request.headers.get('X-XSRF-TOKEN'))
    return HttpResponse.json({})
  }
  server.use(http.get('*/api/v1/ping', capture), http.post('*/api/v1/ping', capture))
  return seen
}

describe('cliente da API', () => {
  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
  })

  it('envia o header CSRF do cookie em métodos que alteram estado', async () => {
    document.cookie = 'XSRF-TOKEN=abc%3D123; path=/'
    const seen = captureCsrfHeader()

    await client().POST('/api/v1/ping')

    expect(seen).toEqual(['abc=123'])
  })

  it('não envia o header CSRF em GET', async () => {
    document.cookie = 'XSRF-TOKEN=abc; path=/'
    const seen = captureCsrfHeader()

    await client().GET('/api/v1/ping')

    expect(seen).toEqual([null])
  })

  it('chama o handler de 401 (redireciona para /login)', async () => {
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    server.use(http.get('*/api/v1/ping', () => new HttpResponse(null, { status: 401 })))

    const { response } = await client().GET('/api/v1/ping')

    expect(response.status).toBe(401)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('não chama o handler de 401 em outras respostas', async () => {
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    server.use(http.get('*/api/v1/ping', () => new HttpResponse(null, { status: 403 })))

    await client().GET('/api/v1/ping')

    expect(onUnauthorized).not.toHaveBeenCalled()
  })
})
