import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import { api, ApiError } from '@/lib/api'

export type TripSummary = components['schemas']['TripSummaryResponse']
export type Trip = components['schemas']['TripResponse']
export type CreateTrip = components['schemas']['CreateTripRequest']
export type UpdateTrip = components['schemas']['UpdateTripRequest']
export type TripStatus = Trip['status']
export type TripRole = Trip['role']
export type TripScope = 'upcoming' | 'past' | 'all'

export const tripKeys = {
  all: ['trips'] as const,
  list: (scope: TripScope) => ['trips', 'list', scope] as const,
  detail: (tripId: number) => ['trips', 'detail', tripId] as const,
}

/** Hierarquia de papéis (VIEWER < EDITOR < OWNER), como no backend. */
const roleRank: Record<TripRole, number> = { VIEWER: 0, EDITOR: 1, OWNER: 2 }

export function hasRole(role: TripRole, required: TripRole): boolean {
  return roleRank[role] >= roleRank[required]
}

export function useTrips(scope: TripScope) {
  return useQuery({
    queryKey: tripKeys.list(scope),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips', {
        params: { query: { scope } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

/** Detalhe da viagem. 404 (inexistente ou sem acesso) vira `ApiError` com status 404. */
export function useTrip(tripId: number) {
  return useQuery({
    queryKey: tripKeys.detail(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    retry: (count, error) => !(error instanceof ApiError && error.status === 404) && count < 2,
  })
}

export function useCreateTrip() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: CreateTrip) => {
      const { data, response } = await api.POST('/api/v1/trips', { body })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: (trip) => {
      queryClient.setQueryData(tripKeys.detail(trip.id), trip)
      void queryClient.invalidateQueries({ queryKey: tripKeys.all })
    },
  })
}

export function useUpdateTrip(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: UpdateTrip) => {
      const { data, response } = await api.PUT('/api/v1/trips/{tripId}', {
        params: { path: { tripId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: (trip) => {
      queryClient.setQueryData(tripKeys.detail(trip.id), trip)
      void queryClient.invalidateQueries({ queryKey: tripKeys.all })
    },
    onError: (error) => {
      // Versão desatualizada: recarrega a viagem para o usuário tentar de novo.
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) })
      }
    },
  })
}

export function useDeleteTrip(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const { response } = await api.DELETE('/api/v1/trips/{tripId}', {
        params: { path: { tripId } },
      })
      if (!response.ok) throw new ApiError(response.status)
    },
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: tripKeys.detail(tripId) })
      void queryClient.invalidateQueries({ queryKey: tripKeys.all })
    },
  })
}
