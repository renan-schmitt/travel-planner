import { http, HttpResponse } from 'msw'
import type { Dashboard, UpcomingTrip } from '@/features/dashboard/api'
import { isoInDays } from './trips'

export const upcomingTrip: UpcomingTrip = {
  id: 42,
  name: 'Férias em Portugal',
  startDate: isoInDays(12),
  endDate: isoInDays(20),
  status: 'CONFIRMED',
  coverColor: '#7C5CC4',
  role: 'OWNER',
  destinationCount: 2,
  daysUntilStart: 12,
  checklistDone: 3,
  checklistTotal: 4,
  pendingSuggestions: { critical: 1, warning: 2, info: 0 },
}

export const emptyDashboard: Dashboard = { upcomingTrips: [], alerts: [], pastTrips: [] }

/** `GET /api/v1/dashboard`; `calls` conta as leituras (para ver se recarregou). */
export function dashboard(data: Dashboard = emptyDashboard) {
  const state = { calls: 0, data }
  const handler = http.get('*/api/v1/dashboard', () => {
    state.calls++
    return HttpResponse.json(state.data)
  })
  return { handler, state }
}
