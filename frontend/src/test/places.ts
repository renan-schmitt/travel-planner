import { http, HttpResponse } from 'msw'
import type { City, CitySuggestion } from '@/features/places/api'

export const lisbonSuggestion: CitySuggestion = {
  placeId: 'ChIJLisboa',
  name: 'Lisboa',
  description: 'Portugal',
}

export const lisbon: City = {
  placeId: 'ChIJLisboa',
  name: 'Lisboa',
  countryCode: 'PT',
  lat: 38.7222524,
  lng: -9.1393366,
  timezone: 'Europe/Lisbon',
}

/** Autocomplete devolvendo `suggestions`; registra os textos e sessões pedidos. */
export function cityAutocomplete(suggestions: CitySuggestion[]) {
  const requests: { q: string; session: string }[] = []
  const handler = http.get('*/api/v1/places/cities/autocomplete', ({ request }) => {
    const url = new URL(request.url)
    requests.push({
      q: url.searchParams.get('q') ?? '',
      session: url.searchParams.get('session') ?? '',
    })
    return HttpResponse.json(suggestions)
  })
  return { handler, requests }
}

/** Detalhes de cidades por placeId; registra as sessões usadas. */
export function cityDetails(cities: City[]) {
  const sessions: string[] = []
  const handler = http.get('*/api/v1/places/cities/:placeId', ({ params, request }) => {
    sessions.push(new URL(request.url).searchParams.get('session') ?? '')
    const city = cities.find((c) => c.placeId === params.placeId)
    return city ? HttpResponse.json(city) : HttpResponse.json({ status: 404 }, { status: 404 })
  })
  return { handler, sessions }
}

/** Places fora do ar (503) no autocomplete e nos detalhes. */
export function placesUnavailable() {
  return http.get('*/api/v1/places/cities/*', () =>
    HttpResponse.json({ status: 503, detail: 'Busca de lugares indisponível.' }, { status: 503 }),
  )
}
