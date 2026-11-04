import { useInfiniteQuery } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import type { PriceLevel } from '@/components/PriceTier'
import { api, ApiError } from '@/lib/api'

export type HotelResult = components['schemas']['LodgingSearchResult']
export type HotelType = 'HOTEL' | 'RESORT' | 'ANY'

/** Filtros enviados ao backend (a ordenação é feita no cliente). */
export type HotelFilters = {
  type: HotelType
  minRating?: 4 | 4.5
  tiers: PriceLevel[]
}

/**
 * Busca de hotéis de uma parada no Places, página a página (`nextPageToken`). 503 = Places fora do
 * ar ou sem chave. Sem retry e com cache longo, para não gastar quota (cada busca é cobrada).
 */
export function useHotelSearch(tripId: number, stopId: number, filters: HotelFilters) {
  const tiers = [...filters.tiers].sort()
  return useInfiniteQuery({
    queryKey: ['places', 'hotels', tripId, stopId, filters.type, filters.minRating ?? 0, tiers],
    queryFn: async ({ pageParam }) => {
      const { data, response } = await api.GET(
        '/api/v1/trips/{tripId}/stops/{stopId}/lodging-search',
        {
          params: {
            path: { tripId, stopId },
            query: {
              type: filters.type,
              minRating: filters.minRating,
              tiers: tiers.length > 0 ? tiers : undefined,
              page: pageParam,
            },
          },
        },
      )
      if (!data) throw new ApiError(response.status)
      return data
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (last) => last.nextPageToken ?? undefined,
    staleTime: 60 * 60_000,
    retry: false,
  })
}
