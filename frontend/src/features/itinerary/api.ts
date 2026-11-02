import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import { tripKeys } from '@/features/trips/api'
import { api, ApiError } from '@/lib/api'

export type Stop = components['schemas']['StopResponse']
export type CreateStop = components['schemas']['StopRequest']
export type UpdateStop = components['schemas']['UpdateStopRequest']
export type Segment = components['schemas']['SegmentResponse']
export type CreateSegment = components['schemas']['SegmentRequest']
export type UpdateSegment = components['schemas']['UpdateSegmentRequest']
export type TransportType = Segment['type']

export const stopKeys = {
  trip: (tripId: number) => ['trips', 'stops', tripId] as const,
}

export const segmentKeys = {
  trip: (tripId: number) => ['trips', 'segments', tripId] as const,
}

/** Paradas na ordem do roteiro. */
export function useStops(tripId: number) {
  return useQuery({
    queryKey: stopKeys.trip(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/stops', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

export function useCreateStop(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: CreateStop) => {
      const { data, response } = await api.POST('/api/v1/trips/{tripId}/stops', {
        params: { path: { tripId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: () => invalidate(queryClient, tripId),
  })
}

export function useUpdateStop(tripId: number, stopId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: UpdateStop) => {
      const { data, response } = await api.PUT('/api/v1/trips/{tripId}/stops/{stopId}', {
        params: { path: { tripId, stopId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    // Também em erro (409): recarrega para o usuário tentar de novo com a versão atual.
    onSettled: () => invalidate(queryClient, tripId),
  })
}

export function useDeleteStop(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (stopId: number) => {
      const { response } = await api.DELETE('/api/v1/trips/{tripId}/stops/{stopId}', {
        params: { path: { tripId, stopId } },
      })
      if (!response.ok) throw new ApiError(response.status)
    },
    onSuccess: () => invalidate(queryClient, tripId),
  })
}

export function useReorderStops(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (stopIds: number[]) => {
      const { data, response } = await api.PUT('/api/v1/trips/{tripId}/stops/order', {
        params: { path: { tripId } },
        body: { stopIds },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: (stops) => {
      queryClient.setQueryData(stopKeys.trip(tripId), stops)
      void queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) })
    },
    onError: () => void queryClient.invalidateQueries({ queryKey: stopKeys.trip(tripId) }),
  })
}

/** Trechos de transporte em ordem cronológica de partida. */
export function useSegments(tripId: number) {
  return useQuery({
    queryKey: segmentKeys.trip(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/segments', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

export function useCreateSegment(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: CreateSegment) => {
      const { data, response } = await api.POST('/api/v1/trips/{tripId}/segments', {
        params: { path: { tripId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: () => invalidate(queryClient, tripId),
  })
}

export function useUpdateSegment(tripId: number, segmentId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: UpdateSegment) => {
      const { data, response } = await api.PUT('/api/v1/trips/{tripId}/segments/{segmentId}', {
        params: { path: { tripId, segmentId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSettled: () => invalidate(queryClient, tripId),
  })
}

export function useDeleteSegment(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (segmentId: number) => {
      const { response } = await api.DELETE('/api/v1/trips/{tripId}/segments/{segmentId}', {
        params: { path: { tripId, segmentId } },
      })
      if (!response.ok) throw new ApiError(response.status)
    },
    onSuccess: () => invalidate(queryClient, tripId),
  })
}

/**
 * Paradas e trechos mudam as datas derivadas e a contagem de destinos da viagem; excluir uma parada
 * exclui os trechos ligados a ela.
 */
function invalidate(queryClient: ReturnType<typeof useQueryClient>, tripId: number) {
  void queryClient.invalidateQueries({ queryKey: stopKeys.trip(tripId) })
  void queryClient.invalidateQueries({ queryKey: segmentKeys.trip(tripId) })
  void queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) })
  void queryClient.invalidateQueries({ queryKey: ['trips', 'list'] })
}
