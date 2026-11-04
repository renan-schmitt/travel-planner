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
export type Lodging = components['schemas']['LodgingResponse']
export type CreateLodging = components['schemas']['LodgingRequest']
export type UpdateLodging = components['schemas']['UpdateLodgingRequest']
export type BoardType = NonNullable<Lodging['boardType']>
export type LodgingKind = Lodging['kind']
export type Itinerary = components['schemas']['ItineraryResponse']
export type ItineraryDay = components['schemas']['ItineraryDayResponse']
export type ItineraryEvent = components['schemas']['ItineraryEventResponse']
export type ItinerarySegment = components['schemas']['ItinerarySegmentResponse']
export type ItineraryActivity = components['schemas']['ItineraryActivityResponse']
export type Activity = components['schemas']['ActivityResponse']
export type CreateActivity = components['schemas']['ActivityRequest']
export type UpdateActivity = components['schemas']['UpdateActivityRequest']

export const stopKeys = {
  trip: (tripId: number) => ['trips', 'stops', tripId] as const,
}

export const segmentKeys = {
  trip: (tripId: number) => ['trips', 'segments', tripId] as const,
}

export const lodgingKeys = {
  trip: (tripId: number) => ['trips', 'lodgings', tripId] as const,
}

export const itineraryKeys = {
  trip: (tripId: number) => ['trips', 'itinerary', tripId] as const,
}

/** Timeline dia a dia (vazia enquanto a viagem não tem datas). */
export function useItinerary(tripId: number) {
  return useQuery({
    queryKey: itineraryKeys.trip(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/itinerary', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

/** Atividades só mudam a timeline (não alimentam as regras de lacunas nem as datas). */
function invalidateActivities(queryClient: ReturnType<typeof useQueryClient>, tripId: number) {
  return queryClient.invalidateQueries({ queryKey: itineraryKeys.trip(tripId) })
}

export function useCreateActivity(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: CreateActivity) => {
      const { data, response } = await api.POST('/api/v1/trips/{tripId}/activities', {
        params: { path: { tripId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSettled: () => invalidateActivities(queryClient, tripId),
  })
}

export function useUpdateActivity(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: UpdateActivity }) => {
      const { data, response } = await api.PUT('/api/v1/trips/{tripId}/activities/{activityId}', {
        params: { path: { tripId, activityId: id } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    // Também em erro (409): recarrega para o usuário tentar de novo com a versão atual.
    onSettled: () => invalidateActivities(queryClient, tripId),
  })
}

export function useDeleteActivity(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (activityId: number) => {
      const { response } = await api.DELETE('/api/v1/trips/{tripId}/activities/{activityId}', {
        params: { path: { tripId, activityId } },
      })
      if (!response.ok) throw new ApiError(response.status)
    },
    onSettled: () => invalidateActivities(queryClient, tripId),
  })
}

/**
 * Nova ordem das atividades de um dia (arrastar e soltar), com atualização otimista da timeline:
 * `optimistic` é a timeline já com a atividade no lugar novo; em erro, volta a anterior.
 */
export function useReorderActivities(tripId: number) {
  const queryClient = useQueryClient()
  const key = itineraryKeys.trip(tripId)
  return useMutation({
    mutationFn: async ({
      day,
      activityIds,
    }: {
      day: string
      activityIds: number[]
      optimistic: Itinerary
    }) => {
      const { data, response } = await api.PUT('/api/v1/trips/{tripId}/activities/reorder', {
        params: { path: { tripId } },
        body: { day, activityIds },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onMutate: async ({ optimistic }) => {
      await queryClient.cancelQueries({ queryKey: key })
      const previous = queryClient.getQueryData<Itinerary>(key)
      queryClient.setQueryData(key, optimistic)
      return { previous }
    },
    onError: (_error, _vars, context) => {
      if (context?.previous) queryClient.setQueryData(key, context.previous)
    },
    onSettled: () => {
      // Só recarrega depois do último arrasto pendente (senão a lista "pula" entre um e outro).
      if (queryClient.isMutating({ mutationKey: ['reorderActivities', tripId] }) <= 1) {
        void invalidateActivities(queryClient, tripId)
      }
    },
    mutationKey: ['reorderActivities', tripId],
  })
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
      void queryClient.invalidateQueries({ queryKey: itineraryKeys.trip(tripId) })
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

/** Hospedagens de todas as paradas da viagem, por check-in. */
export function useLodgings(tripId: number) {
  return useQuery({
    queryKey: lodgingKeys.trip(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/lodgings', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

export function useCreateLodging(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ stopId, body }: { stopId: number; body: CreateLodging }) => {
      const { data, response } = await api.POST('/api/v1/trips/{tripId}/stops/{stopId}/lodgings', {
        params: { path: { tripId, stopId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: () => invalidateLodgings(queryClient, tripId),
  })
}

export function useUpdateLodging(tripId: number, lodging: Lodging) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: UpdateLodging) => {
      const { data, response } = await api.PUT(
        '/api/v1/trips/{tripId}/stops/{stopId}/lodgings/{lodgingId}',
        { params: { path: { tripId, stopId: lodging.stopId, lodgingId: lodging.id } }, body },
      )
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSettled: () => invalidateLodgings(queryClient, tripId),
  })
}

export function useDeleteLodging(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (lodging: Lodging) => {
      const { response } = await api.DELETE(
        '/api/v1/trips/{tripId}/stops/{stopId}/lodgings/{lodgingId}',
        { params: { path: { tripId, stopId: lodging.stopId, lodgingId: lodging.id } } },
      )
      if (!response.ok) throw new ApiError(response.status)
    },
    onSuccess: () => invalidateLodgings(queryClient, tripId),
  })
}

/** Hospedagens não mudam as datas da viagem (ficam dentro das paradas), só a timeline. */
function invalidateLodgings(queryClient: ReturnType<typeof useQueryClient>, tripId: number) {
  void queryClient.invalidateQueries({ queryKey: lodgingKeys.trip(tripId) })
  void queryClient.invalidateQueries({ queryKey: itineraryKeys.trip(tripId) })
}

/**
 * Paradas e trechos mudam as datas derivadas e a contagem de destinos da viagem; excluir uma parada
 * exclui os trechos e as hospedagens ligados a ela.
 */
function invalidate(queryClient: ReturnType<typeof useQueryClient>, tripId: number) {
  void queryClient.invalidateQueries({ queryKey: stopKeys.trip(tripId) })
  void queryClient.invalidateQueries({ queryKey: segmentKeys.trip(tripId) })
  void queryClient.invalidateQueries({ queryKey: lodgingKeys.trip(tripId) })
  void queryClient.invalidateQueries({ queryKey: itineraryKeys.trip(tripId) })
  void queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) })
  void queryClient.invalidateQueries({ queryKey: ['trips', 'list'] })
}
