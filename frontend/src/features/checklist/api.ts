import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import { api, ApiError } from '@/lib/api'

export type ChecklistItem = components['schemas']['ChecklistItemResponse']
export type CreateChecklistItem = components['schemas']['ChecklistItemRequest']
export type UpdateChecklistItem = components['schemas']['UpdateChecklistItemRequest']
export type ChecklistCategory = ChecklistItem['category']
export type ChecklistStatus = ChecklistItem['status']

export const checklistKeys = {
  trip: (tripId: number) => ['trips', 'checklist', tripId] as const,
}

/** Todos os itens da viagem, na ordem do usuário (os filtros da aba são aplicados no cliente). */
export function useChecklist(tripId: number) {
  return useQuery({
    queryKey: checklistKeys.trip(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/checklist', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

function replaceItem(queryClient: QueryClient, tripId: number, item: ChecklistItem) {
  queryClient.setQueryData<ChecklistItem[]>(checklistKeys.trip(tripId), (list) =>
    list?.map((i) => (i.id === item.id ? item : i)),
  )
}

export function useCreateChecklistItem(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: CreateChecklistItem) => {
      const { data, response } = await api.POST('/api/v1/trips/{tripId}/checklist', {
        params: { path: { tripId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: (item) => {
      queryClient.setQueryData<ChecklistItem[]>(checklistKeys.trip(tripId), (list) =>
        list ? [...list, item] : list,
      )
      void queryClient.invalidateQueries({ queryKey: checklistKeys.trip(tripId) })
    },
  })
}

/**
 * Conclui ou reabre o item com atualização otimista: a lista (e o progresso) mudam na hora e voltam
 * ao estado anterior se o pedido falhar.
 */
export function useSetChecklistStatus(tripId: number) {
  const queryClient = useQueryClient()
  const key = checklistKeys.trip(tripId)
  return useMutation({
    mutationFn: async ({ item, status }: { item: ChecklistItem; status: ChecklistStatus }) => {
      const { data, response } = await api.PATCH(
        '/api/v1/trips/{tripId}/checklist/{itemId}/status',
        { params: { path: { tripId, itemId: item.id } }, body: { status } },
      )
      if (!data) throw new ApiError(response.status)
      return data
    },
    onMutate: async ({ item, status }) => {
      await queryClient.cancelQueries({ queryKey: key })
      const previous = queryClient.getQueryData<ChecklistItem[]>(key)
      queryClient.setQueryData<ChecklistItem[]>(key, (list) =>
        list?.map((i) => (i.id === item.id ? { ...i, status } : i)),
      )
      return { previous }
    },
    onError: (_error, _vars, context) => {
      if (context?.previous) queryClient.setQueryData(key, context.previous)
    },
    onSuccess: (item) => replaceItem(queryClient, tripId, item),
    onSettled: () => {
      // Com toques rápidos em vários itens, só recarrega depois do último.
      if (queryClient.isMutating({ mutationKey: ['checklist-status', tripId] }) <= 1) {
        void queryClient.invalidateQueries({ queryKey: key })
      }
    },
    mutationKey: ['checklist-status', tripId],
  })
}

export function useUpdateChecklistItem(tripId: number, item: ChecklistItem) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: UpdateChecklistItem) => {
      const { data, response } = await api.PUT('/api/v1/trips/{tripId}/checklist/{itemId}', {
        params: { path: { tripId, itemId: item.id } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: (updated) => replaceItem(queryClient, tripId, updated),
    onError: () => void queryClient.invalidateQueries({ queryKey: checklistKeys.trip(tripId) }),
  })
}

export function useDeleteChecklistItem(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (item: ChecklistItem) => {
      const { response } = await api.DELETE('/api/v1/trips/{tripId}/checklist/{itemId}', {
        params: { path: { tripId, itemId: item.id } },
      })
      if (!response.ok) throw new ApiError(response.status)
      return item
    },
    onSuccess: (item) => {
      queryClient.setQueryData<ChecklistItem[]>(checklistKeys.trip(tripId), (list) =>
        list?.filter((i) => i.id !== item.id),
      )
    },
  })
}
