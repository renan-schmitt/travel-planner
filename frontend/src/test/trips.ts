import { addDays, format } from 'date-fns'
import { http, HttpResponse } from 'msw'
import type { Trip, TripSummary } from '@/features/trips/api'

/** Data ISO relativa a hoje (os cards mostram contagem regressiva a partir de hoje). */
export function isoInDays(days: number): string {
  return format(addDays(new Date(), days), 'yyyy-MM-dd')
}

export const testTrip: Trip = {
  id: 42,
  name: 'Férias em Portugal',
  startDate: '2099-07-01',
  endDate: '2099-07-15',
  datesFromStops: false,
  status: 'CONFIRMED',
  plannedStatus: 'CONFIRMED',
  coverColor: '#7C5CC4',
  role: 'OWNER',
  destinationCount: 0,
  pendingSuggestions: 0,
  version: 3,
}

export function summaryOf(trip: Trip): TripSummary {
  return {
    id: trip.id,
    name: trip.name,
    startDate: trip.startDate,
    endDate: trip.endDate,
    status: trip.status,
    coverColor: trip.coverColor,
    role: trip.role,
    destinationCount: trip.destinationCount,
    pendingSuggestions: trip.pendingSuggestions,
  }
}

/** `GET /api/v1/trips/{id}` respondendo com a viagem. */
export function tripDetail(trip: Trip = testTrip) {
  return http.get(`*/api/v1/trips/${trip.id}`, () => HttpResponse.json(trip))
}

/** `GET /api/v1/trips` por escopo; registra os escopos pedidos. */
export function tripList(byScope: Partial<Record<string, TripSummary[]>>) {
  const scopes: string[] = []
  const handler = http.get('*/api/v1/trips', ({ request }) => {
    const scope = new URL(request.url).searchParams.get('scope') ?? 'upcoming'
    scopes.push(scope)
    return HttpResponse.json(byScope[scope] ?? [])
  })
  return { handler, scopes }
}
