import { useQuery } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import { api, ApiError } from '@/lib/api'

export type Dashboard = components['schemas']['DashboardResponse']
export type UpcomingTrip = components['schemas']['UpcomingTripResponse']
export type DashboardAlert = components['schemas']['AlertResponse']
export type PastTrip = components['schemas']['PastTripResponse']

/** Sob `trips`: as mutações das viagens (que invalidam `['trips']`) também atualizam o dashboard. */
export const dashboardKeys = {
  all: ['trips', 'dashboard'] as const,
}

export function useDashboard() {
  return useQuery({
    queryKey: dashboardKeys.all,
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/dashboard')
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}
