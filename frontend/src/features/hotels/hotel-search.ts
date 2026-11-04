import type { PriceLevel } from '@/components/PriceTier'
import type { Stop } from '@/features/itinerary/api'
import type { Traveler } from '@/features/travelers/api'
import type { HotelResult } from './api'

export type HotelSort = 'relevance' | 'rating' | 'reviews' | 'price'

export const HOTEL_SORTS: HotelSort[] = ['relevance', 'rating', 'reviews', 'price']

/**
 * Ordena os resultados no cliente. "Relevância" é a ordem do Google; nas demais, quem não tem o
 * dado vai para o fim e o empate mantém a ordem do Google.
 */
export function sortHotels(results: HotelResult[], sort: HotelSort): HotelResult[] {
  if (sort === 'relevance') return results
  const key = (r: HotelResult): number | undefined =>
    sort === 'rating' ? r.rating : sort === 'reviews' ? r.userRatingCount : r.priceTier
  const direction = sort === 'price' ? 1 : -1
  return results
    .map((r, i) => ({ r, i, k: key(r) }))
    .sort((a, b) => {
      if (a.k === undefined && b.k === undefined) return a.i - b.i
      if (a.k === undefined) return 1
      if (b.k === undefined) return -1
      return (a.k - b.k) * direction || a.i - b.i
    })
    .map(({ r }) => r)
}

/** Distância em km entre dois pontos (fórmula de Haversine). */
export function distanceKm(
  a: { lat: number; lng: number },
  b: { lat: number; lng: number },
): number {
  const rad = (deg: number) => (deg * Math.PI) / 180
  const dLat = rad(b.lat - a.lat)
  const dLng = rad(b.lng - a.lng)
  const h =
    Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2
  return 2 * 6371 * Math.asin(Math.sqrt(h))
}

/** Distância do resultado ao centro da parada; indefinida se faltar alguma coordenada. */
export function distanceFromStop(stop: Stop, result: HotelResult): number | undefined {
  if (stop.lat == null || stop.lng == null || result.lat == null || result.lng == null) {
    return undefined
  }
  return distanceKm({ lat: stop.lat, lng: stop.lng }, { lat: result.lat, lng: result.lng })
}

/** Adultos e crianças da viagem para o Booking (adolescentes, crianças e bebês contam como crianças). */
export function guestCounts(travelers: Pick<Traveler, 'ageGroup'>[]) {
  const adults = travelers.filter((t) => t.ageGroup === 'ADULT').length
  return { adults: Math.max(adults, 1), children: travelers.length - adults }
}

/** Deep link "Ver preços" no Booking com as datas e os hóspedes já preenchidos. */
export function bookingUrl(
  result: Pick<HotelResult, 'name'>,
  stop: Pick<Stop, 'cityName'>,
  dates: { checkIn: string; checkOut: string },
  guests: { adults: number; children: number },
): string {
  const params = new URLSearchParams({
    ss: `${result.name} ${stop.cityName}`,
    checkin: dates.checkIn,
    checkout: dates.checkOut,
    group_adults: String(guests.adults),
    group_children: String(guests.children),
  })
  return `https://www.booking.com/searchresults.html?${params.toString()}`
}

/** Foto pelo proxy do backend (a chave do Places nunca vai para o navegador). */
export function photoUrl(photoName: string, width = 400): string {
  const params = new URLSearchParams({ name: photoName, w: String(width) })
  return `/api/v1/places/photos?${params.toString()}`
}

/** "Ver no Maps": o link do Places ou, sem ele, uma busca pelo `placeId`. */
export function mapsUrl(result: Pick<HotelResult, 'googleMapsUri' | 'placeId' | 'name'>): string {
  if (result.googleMapsUri) return result.googleMapsUri
  const params = new URLSearchParams({
    api: '1',
    query: result.name,
    query_place_id: result.placeId,
  })
  return `https://www.google.com/maps/search/?${params.toString()}`
}

export const asPriceLevel = (tier?: number): PriceLevel | undefined =>
  tier !== undefined && tier >= 1 && tier <= 4 ? (tier as PriceLevel) : undefined
