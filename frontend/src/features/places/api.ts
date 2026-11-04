import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import type { PriceLevel } from '@/components/PriceTier'
import { api, ApiError } from '@/lib/api'

export type CitySuggestion = components['schemas']['CitySuggestionResponse']
export type City = components['schemas']['CityResponse']

/** Mínimo de caracteres para buscar (o backend recusa menos). */
export const MIN_QUERY = 3

/**
 * Sugestões de cidade para o texto (já com debounce). 503 = Places fora do ar ou sem quota: o
 * componente oferece a digitação manual. Sem retry, para não gastar quota.
 */
export function useCityAutocomplete(query: string, session: string) {
  const q = query.trim()
  return useQuery({
    queryKey: ['places', 'cities', q.toLowerCase()],
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/places/cities/autocomplete', {
        params: { query: { q, session } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    enabled: q.length >= MIN_QUERY,
    staleTime: 10 * 60_000,
    retry: false,
  })
}

/** País, coordenadas e fuso da cidade escolhida (encerra a sessão do autocomplete). */
export async function fetchCity(placeId: string, session: string): Promise<City> {
  const { data, response } = await api.GET('/api/v1/places/cities/{placeId}', {
    params: { path: { placeId }, query: { session } },
  })
  if (!data) throw new ApiError(response.status)
  return data
}

// Lugares favoritos e busca de restaurantes/atrações (Fase 17).

export type SavedPlace = components['schemas']['SavedPlaceResponse']
export type CreateSavedPlace = components['schemas']['SavedPlaceRequest']
export type UpdateSavedPlace = components['schemas']['UpdateSavedPlaceRequest']
export type PlaceSearch = components['schemas']['PlaceSearchResponse']
export type PlaceResult = components['schemas']['PlaceSearchResult']
export type SavedPlaceKind = SavedPlace['kind']
/** O que dá para descobrir no Places (o favorito ainda pode virar "Outro" depois). */
export type DiscoverKind = 'RESTAURANT' | 'ATTRACTION'

/** Filtros enviados ao backend. */
export type PlaceFilters = {
  minRating?: 4 | 4.5
  tiers: PriceLevel[]
}

export const savedPlaceKeys = {
  trip: (tripId: number) => ['trips', 'savedPlaces', tripId] as const,
  stop: (tripId: number, stopId: number) => ['trips', 'savedPlaces', tripId, stopId] as const,
}

/** Favoritos de todas as paradas (sem nota/posição do Places), para contagens no Roteiro. */
export function useTripSavedPlaces(tripId: number) {
  return useQuery({
    queryKey: savedPlaceKeys.trip(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/saved-places', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

/** Favoritos da parada, com nota, posição e link do Maps (do Places, quando disponível). */
export function useSavedPlaces(tripId: number, stopId: number) {
  return useQuery({
    queryKey: savedPlaceKeys.stop(tripId, stopId),
    queryFn: async () => {
      const { data, response } = await api.GET(
        '/api/v1/trips/{tripId}/stops/{stopId}/saved-places',
        { params: { path: { tripId, stopId } } },
      )
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

/**
 * Restaurantes/atrações perto da parada. 503 = Places fora do ar ou sem chave. Sem retry e com
 * cache longo, para não gastar quota (cada busca é cobrada).
 */
export function usePlaceSearch(
  tripId: number,
  stopId: number,
  kind: DiscoverKind,
  filters: PlaceFilters,
  enabled: boolean,
) {
  const tiers = [...filters.tiers].sort()
  return useQuery({
    queryKey: ['places', 'nearby', tripId, stopId, kind, filters.minRating ?? 0, tiers],
    queryFn: async () => {
      const { data, response } = await api.GET(
        '/api/v1/trips/{tripId}/stops/{stopId}/places-search',
        {
          params: {
            path: { tripId, stopId },
            query: {
              kind,
              minRating: filters.minRating,
              tiers: tiers.length > 0 ? tiers : undefined,
            },
          },
        },
      )
      if (!data) throw new ApiError(response.status)
      return data
    },
    enabled,
    staleTime: 60 * 60_000,
    retry: false,
  })
}

function invalidate(queryClient: ReturnType<typeof useQueryClient>, tripId: number) {
  return queryClient.invalidateQueries({ queryKey: savedPlaceKeys.trip(tripId) })
}

export function useSavePlace(tripId: number, stopId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: CreateSavedPlace) => {
      const { data, response } = await api.POST(
        '/api/v1/trips/{tripId}/stops/{stopId}/saved-places',
        { params: { path: { tripId, stopId } }, body },
      )
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSettled: () => invalidate(queryClient, tripId),
  })
}

export function useUpdateSavedPlace(tripId: number, stopId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: UpdateSavedPlace }) => {
      const { data, response } = await api.PATCH(
        '/api/v1/trips/{tripId}/stops/{stopId}/saved-places/{savedPlaceId}',
        { params: { path: { tripId, stopId, savedPlaceId: id } }, body },
      )
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSettled: () => invalidate(queryClient, tripId),
  })
}

export function useDeleteSavedPlace(tripId: number, stopId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (savedPlaceId: number) => {
      const { response } = await api.DELETE(
        '/api/v1/trips/{tripId}/stops/{stopId}/saved-places/{savedPlaceId}',
        { params: { path: { tripId, stopId, savedPlaceId } } },
      )
      if (!response.ok) throw new ApiError(response.status)
    },
    onSettled: () => invalidate(queryClient, tripId),
  })
}

/** Corpo do PATCH a partir do favorito, com as mudanças. */
export function updateBody(
  place: SavedPlace,
  changes: Partial<UpdateSavedPlace>,
): UpdateSavedPlace {
  return {
    version: place.version,
    kind: place.kind,
    priceTier: place.priceTier,
    notes: place.notes,
    visited: place.visited,
    ...changes,
  }
}
