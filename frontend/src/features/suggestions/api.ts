import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import { api, ApiError } from '@/lib/api'

export type Suggestion = components['schemas']['SuggestionResponse']
export type SuggestionStatus = Suggestion['status']
export type Severity = Suggestion['severity']

export const suggestionKeys = {
  all: ['trips', 'suggestions'] as const,
  trip: (tripId: number) => ['trips', 'suggestions', tripId] as const,
}

/**
 * Todas as sugestões da viagem (pendentes, adiadas, aceitas e dispensadas), mais graves primeiro.
 * O motor roda no backend a cada alteração; o `MutationCache` do query client invalida esta lista
 * depois de qualquer mutação.
 */
export function useSuggestions(tripId: number) {
  return useQuery({
    queryKey: suggestionKeys.trip(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/suggestions', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

export function pendingCount(suggestions: Suggestion[] | undefined): number {
  return suggestions?.filter((s) => s.status === 'PENDING').length ?? 0
}

/** Aceitar, adiar (`snoozedUntil`), dispensar ou restaurar (`PENDING`). */
export function useDecideSuggestion(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      suggestion,
      status,
      snoozedUntil,
    }: {
      suggestion: Suggestion
      status: SuggestionStatus
      snoozedUntil?: string
    }) => {
      const { data, response } = await api.PATCH(
        '/api/v1/trips/{tripId}/suggestions/{suggestionId}',
        {
          params: { path: { tripId, suggestionId: suggestion.id } },
          body: { status, snoozedUntil },
        },
      )
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: (updated) => {
      queryClient.setQueryData<Suggestion[]>(suggestionKeys.trip(tripId), (list) =>
        list?.map((s) => (s.id === updated.id ? updated : s)),
      )
      // Aceitar pode criar um item de checklist.
      void queryClient.invalidateQueries({ queryKey: ['trips', 'checklist', tripId] })
    },
  })
}
