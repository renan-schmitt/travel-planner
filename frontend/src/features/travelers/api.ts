import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import { api, ApiError } from '@/lib/api'

export type Traveler = components['schemas']['TravelerResponse']
export type CreateTraveler = components['schemas']['TravelerRequest']
export type UpdateTraveler = components['schemas']['UpdateTravelerRequest']
export type AgeGroup = Traveler['ageGroup']

export const ageGroups: AgeGroup[] = ['ADULT', 'TEEN', 'CHILD', 'BABY']

export const travelerKeys = {
  mine: ['travelers'] as const,
  trip: (tripId: number) => ['trips', 'travelers', tripId] as const,
  allTrips: ['trips', 'travelers'] as const,
}

/** Viajantes do usuário ("eu" primeiro). */
export function useTravelers() {
  return useQuery({
    queryKey: travelerKeys.mine,
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/travelers')
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

/** Quem vai na viagem (inclui os viajantes trazidos por outros membros). */
export function useTripTravelers(tripId: number) {
  return useQuery({
    queryKey: travelerKeys.trip(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/travelers', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

export function useCreateTraveler() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: CreateTraveler) => {
      const { data, response } = await api.POST('/api/v1/travelers', { body })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: travelerKeys.mine }),
  })
}

export function useUpdateTraveler(travelerId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: UpdateTraveler) => {
      const { data, response } = await api.PUT('/api/v1/travelers/{travelerId}', {
        params: { path: { travelerId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    // Também em erro (409): recarrega para o usuário tentar de novo com a versão atual.
    onSettled: () => invalidateAll(queryClient),
  })
}

export function useDeleteTraveler() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (travelerId: number) => {
      const { response } = await api.DELETE('/api/v1/travelers/{travelerId}', {
        params: { path: { travelerId } },
      })
      if (!response.ok) throw new ApiError(response.status)
    },
    onSuccess: () => invalidateAll(queryClient),
  })
}

export function useSetTripTravelers(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (travelerIds: number[]) => {
      const { data, response } = await api.PUT('/api/v1/trips/{tripId}/travelers', {
        params: { path: { tripId } },
        body: { travelerIds },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: (travelers) => queryClient.setQueryData(travelerKeys.trip(tripId), travelers),
    onError: () => void queryClient.invalidateQueries({ queryKey: travelerKeys.trip(tripId) }),
  })
}

/** Um viajante aparece na lista do usuário e nas viagens em que está. */
function invalidateAll(queryClient: ReturnType<typeof useQueryClient>) {
  void queryClient.invalidateQueries({ queryKey: travelerKeys.mine })
  void queryClient.invalidateQueries({ queryKey: travelerKeys.allTrips })
}
