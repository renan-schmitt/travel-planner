import { http, HttpResponse } from 'msw'
import type { Me } from '@/features/auth/api'

export const testUser: Me = {
  id: 1,
  email: 'ana@example.com',
  name: 'Ana Souza',
  pictureUrl: 'https://example.com/ana.png',
  nationality: 'BR',
  isAdmin: false,
  version: 0,
}

/** `GET /api/v1/me` respondendo com o usuário (padrão: `testUser`). */
export function loggedIn(user: Me = testUser) {
  return http.get('*/api/v1/me', () => HttpResponse.json(user))
}

/** `GET /api/v1/me` sem sessão. */
export function loggedOut() {
  return http.get('*/api/v1/me', () => new HttpResponse(null, { status: 401 }))
}
