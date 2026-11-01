import { useQuery } from '@tanstack/react-query'
import type { components } from '@/api/schema'
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
