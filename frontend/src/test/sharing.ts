import { http, HttpResponse } from 'msw'
import type { Member, ShareLink } from '@/features/sharing/api'

export const ownerMember: Member = {
  id: 10,
  userId: 1,
  email: 'ana@example.com',
  name: 'Ana Souza',
  role: 'OWNER',
  pending: false,
  me: true,
  version: 0,
}

export const editorMember: Member = {
  id: 11,
  userId: 2,
  email: 'beatriz@example.com',
  name: 'Beatriz',
  role: 'EDITOR',
  pending: false,
  me: false,
  version: 2,
}

export const pendingMember: Member = {
  id: 12,
  email: 'caio@example.com',
  role: 'VIEWER',
  pending: true,
  me: false,
  version: 0,
}

export const viewerLink: ShareLink = {
  id: 5,
  token: 'tok_abcdefghijklmnopqrstuvwxyz0123456789ABCDE',
  role: 'VIEWER',
  createdAt: '2026-11-01T10:00:00Z',
}

export function tripMembers(
  tripId = 42,
  list: Member[] = [ownerMember, editorMember, pendingMember],
) {
  return http.get(`*/api/v1/trips/${tripId}/members`, () => HttpResponse.json(list))
}

export function shareLinks(tripId = 42, list: ShareLink[] = []) {
  return http.get(`*/api/v1/trips/${tripId}/share-links`, () => HttpResponse.json(list))
}
