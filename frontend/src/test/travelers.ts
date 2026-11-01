import { http, HttpResponse } from 'msw'
import type { Traveler } from '@/features/travelers/api'

export const meTraveler: Traveler = {
  id: 1,
  name: 'Ana Souza',
  ageGroup: 'ADULT',
  nationalities: ['BR'],
  passportExpiry: '2099-01-01',
  me: true,
  mine: true,
  version: 0,
}

export const childTraveler: Traveler = {
  id: 2,
  name: 'Léo',
  ageGroup: 'CHILD',
  birthYear: 2018,
  nationalities: ['BR', 'PT'],
  me: false,
  mine: true,
  version: 4,
}

/** Viajante trazido por outro membro da viagem. */
export const otherTraveler: Traveler = {
  id: 3,
  name: 'Bia',
  ageGroup: 'ADULT',
  nationalities: ['PT'],
  me: false,
  mine: false,
  version: 0,
}

export function myTravelers(list: Traveler[] = [meTraveler, childTraveler]) {
  return http.get('*/api/v1/travelers', () => HttpResponse.json(list))
}

export function tripTravelers(tripId: number, list: Traveler[]) {
  return http.get(`*/api/v1/trips/${tripId}/travelers`, () => HttpResponse.json(list))
}
