import type { HotelResult } from './api'
import { bookingUrl, distanceKm, guestCounts, mapsUrl, photoUrl, sortHotels } from './hotel-search'

const hotel = (placeId: string, extra: Partial<HotelResult> = {}): HotelResult => ({
  placeId,
  name: placeId,
  ...extra,
})

describe('busca de hotéis', () => {
  const results = [
    hotel('a', { rating: 4.2, userRatingCount: 100, priceTier: 3 }),
    hotel('b', { rating: 4.8, priceTier: 1 }),
    hotel('c', { userRatingCount: 5000 }),
    hotel('d', { rating: 4.8, userRatingCount: 10, priceTier: 2 }),
  ]

  it('ordena por avaliação, nº de avaliações e preço, com quem não tem o dado no fim', () => {
    const ids = (sort: Parameters<typeof sortHotels>[1]) =>
      sortHotels(results, sort).map((r) => r.placeId)
    expect(ids('relevance')).toEqual(['a', 'b', 'c', 'd'])
    expect(ids('rating')).toEqual(['b', 'd', 'a', 'c'])
    expect(ids('reviews')).toEqual(['c', 'a', 'd', 'b'])
    expect(ids('price')).toEqual(['b', 'd', 'a', 'c'])
  })

  it('calcula a distância pela fórmula de Haversine', () => {
    const lisboa = { lat: 38.7223, lng: -9.1393 }
    const porto = { lat: 41.1579, lng: -8.6291 }
    expect(distanceKm(lisboa, porto)).toBeCloseTo(274, 0)
    expect(distanceKm(lisboa, lisboa)).toBe(0)
  })

  it('conta adultos e crianças para o Booking (pelo menos um adulto)', () => {
    expect(
      guestCounts([{ ageGroup: 'ADULT' }, { ageGroup: 'CHILD' }, { ageGroup: 'TEEN' }]),
    ).toEqual({ adults: 1, children: 2 })
    expect(guestCounts([])).toEqual({ adults: 1, children: 0 })
  })

  it('monta os links do Booking, do Maps e da foto (pelo proxy)', () => {
    const url = new URL(
      bookingUrl(
        { name: 'Barceló Bávaro' },
        { cityName: 'Punta Cana' },
        { checkIn: '2027-01-10', checkOut: '2027-01-17' },
        { adults: 2, children: 1 },
      ),
    )
    expect(url.origin + url.pathname).toBe('https://www.booking.com/searchresults.html')
    expect(Object.fromEntries(url.searchParams)).toEqual({
      ss: 'Barceló Bávaro Punta Cana',
      checkin: '2027-01-10',
      checkout: '2027-01-17',
      group_adults: '2',
      group_children: '1',
    })
    expect(mapsUrl(hotel('x', { googleMapsUri: 'https://maps.google.com/?cid=1' }))).toBe(
      'https://maps.google.com/?cid=1',
    )
    expect(mapsUrl(hotel('ChIJx'))).toContain('query_place_id=ChIJx')
    expect(photoUrl('places/ChIJx/photos/AbC')).toBe(
      '/api/v1/places/photos?name=places%2FChIJx%2Fphotos%2FAbC&w=400',
    )
  })
})
