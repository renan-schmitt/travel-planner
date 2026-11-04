import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { CreateLodging, Lodging, Stop } from '@/features/itinerary/api'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { childTraveler, meTraveler, tripTravelers } from '@/test/travelers'
import { testTrip, tripDetail } from '@/test/trips'
import type { HotelResult } from './api'

const puntaCana: Stop = {
  id: 7,
  seq: 1,
  cityName: 'Punta Cana',
  countryCode: 'DO',
  lat: 18.582,
  lng: -68.405,
  timezone: 'America/Santo_Domingo',
  arrivalDate: '2099-01-10',
  departureDate: '2099-01-17',
  nights: 7,
  version: 0,
}

const barcelo: HotelResult = {
  placeId: 'ChIJBarcelo',
  name: 'Barceló Bávaro Palace',
  address: 'Bávaro, Punta Cana',
  lat: 18.68,
  lng: -68.42,
  rating: 4.6,
  userRatingCount: 12000,
  priceTier: 3,
  priceTierSource: 'PLACES',
  primaryType: 'resort_hotel',
  photoName: 'places/ChIJBarcelo/photos/AbC_1',
  googleMapsUri: 'https://maps.google.com/?cid=1',
}

const paradisus: HotelResult = {
  placeId: 'ChIJParadisus',
  name: 'Paradisus Palma Real',
  rating: 4.8,
  userRatingCount: 900,
  priceTier: 3,
  priceTierSource: 'HEURISTIC',
  primaryType: 'resort_hotel',
}

const hostel: HotelResult = {
  placeId: 'ChIJHostel',
  name: 'Hostel Coco',
  rating: 4.1,
  userRatingCount: 50,
  priceTier: 1,
  priceTierSource: 'HEURISTIC',
  primaryType: 'hostel',
}

/** Handlers da tela; `searches` guarda os parâmetros de cada busca e `created`, os POSTs. */
function hotelsApi(pages: Record<string, { results: HotelResult[]; nextPageToken?: string }>) {
  const searches: URLSearchParams[] = []
  const created: CreateLodging[] = []
  const lodgings: Lodging[] = []
  const handlers = [
    http.get('*/api/v1/trips/42/stops', () => HttpResponse.json([puntaCana])),
    http.get('*/api/v1/trips/42/segments', () => HttpResponse.json([])),
    http.get('*/api/v1/trips/42/lodgings', () => HttpResponse.json(lodgings)),
    tripTravelers(42, [meTraveler, childTraveler]),
    http.get('*/api/v1/trips/42/stops/7/lodging-search', ({ request }) => {
      const params = new URL(request.url).searchParams
      searches.push(params)
      return HttpResponse.json(pages[params.get('page') ?? ''] ?? { results: [] })
    }),
    http.post('*/api/v1/trips/42/stops/7/lodgings', async ({ request }) => {
      const body = (await request.json()) as CreateLodging
      created.push(body)
      const lodging: Lodging = {
        id: 90,
        stopId: 7,
        kind: body.kind,
        placeId: body.placeId,
        name: body.name ?? '',
        checkIn: body.checkIn,
        checkOut: body.checkOut,
        nights: 7,
        priceTier: body.priceTier,
        version: 0,
      }
      lodgings.push(lodging)
      return HttpResponse.json(lodging, { status: 201 })
    }),
  ]
  return { handlers, searches, created }
}

async function openSearch() {
  const result = renderApp('/trips/42/stops/7/hotels')
  await screen.findByRole('heading', { name: 'Hotéis em Punta Cana' })
  return result
}

describe('Busca de hotéis', () => {
  it('mostra os resultados com foto, nota, faixa, categoria, distância e links', async () => {
    const api = hotelsApi({ '': { results: [barcelo, paradisus] } })
    server.use(tripDetail(), ...api.handlers)
    await openSearch()

    const card = await screen.findByRole('article', { name: 'Barceló Bávaro Palace' })
    expect(within(card).getByLabelText('Nota 4,6 (12000 avaliações)')).toBeInTheDocument()
    expect(within(card).getByRole('img', { name: 'Faixa de preço: 3 de 4' })).toBeInTheDocument()
    expect(card).toHaveTextContent('Resort')
    expect(card).toHaveTextContent('11 km do centro · Bávaro, Punta Cana')
    expect(card.querySelector('img')).toHaveAttribute(
      'src',
      '/api/v1/places/photos?name=places%2FChIJBarcelo%2Fphotos%2FAbC_1&w=400',
    )
    expect(within(card).getByRole('link', { name: 'Ver no Maps' })).toHaveAttribute(
      'href',
      'https://maps.google.com/?cid=1',
    )
    const booking = new URL(
      within(card).getByRole('link', { name: 'Ver preços' }).getAttribute('href')!,
    )
    expect(booking.searchParams.get('ss')).toBe('Barceló Bávaro Palace Punta Cana')
    expect(booking.searchParams.get('checkin')).toBe('2099-01-10')
    expect(booking.searchParams.get('checkout')).toBe('2099-01-17')
    expect(booking.searchParams.get('group_adults')).toBe('1')
    expect(booking.searchParams.get('group_children')).toBe('1')

    const estimated = screen.getByRole('article', { name: 'Paradisus Palma Real' })
    expect(
      within(estimated).getByRole('img', { name: 'Faixa de preço estimada: 3 de 4' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Faixa de preço indicativa')).toBeInTheDocument()
    expect(api.searches[0]?.get('type')).toBe('HOTEL')
  })

  it('filtra por tipo, nota e faixa; ordena no cliente e carrega mais', async () => {
    const api = hotelsApi({
      '': { results: [barcelo, hostel, paradisus], nextPageToken: 'p2' },
      p2: { results: [{ ...hostel, placeId: 'ChIJOutro', name: 'Outro Hostel' }] },
    })
    server.use(tripDetail(), ...api.handlers)
    const { user } = await openSearch()
    await screen.findByRole('article', { name: 'Hostel Coco' })

    await user.click(screen.getByRole('button', { name: 'Resorts' }))
    await user.click(screen.getByRole('button', { name: '★ 4,5+' }))
    await user.click(screen.getByRole('button', { name: 'Faixa de preço: 3 de 4' }))
    await waitFor(() => {
      const last = api.searches.at(-1)!
      expect(last.get('type')).toBe('RESORT')
      expect(last.get('minRating')).toBe('4.5')
      expect(last.getAll('tiers')).toEqual(['3'])
    })
    expect(screen.getByRole('button', { name: 'Resorts' })).toHaveAttribute('aria-pressed', 'true')

    await user.selectOptions(screen.getByLabelText('Ordenar por'), 'Avaliação')
    const names = () =>
      screen.getAllByRole('article').map((a) => within(a).getByRole('heading').textContent)
    await waitFor(() =>
      expect(names()).toEqual(['Paradisus Palma Real', 'Barceló Bávaro Palace', 'Hostel Coco']),
    )

    await user.click(screen.getByRole('button', { name: 'Carregar mais' }))
    expect(await screen.findByRole('article', { name: 'Outro Hostel' })).toBeInTheDocument()
    expect(api.searches.at(-1)?.get('page')).toBe('p2')
    expect(screen.queryByRole('button', { name: 'Carregar mais' })).not.toBeInTheDocument()
  })

  it('adiciona um resultado à viagem com o lugar, a faixa e as datas da parada', async () => {
    const api = hotelsApi({ '': { results: [barcelo] } })
    server.use(tripDetail(), ...api.handlers)
    const { user, router } = await openSearch()

    const card = await screen.findByRole('article', { name: 'Barceló Bávaro Palace' })
    await user.click(within(card).getByRole('button', { name: 'Adicionar à viagem' }))
    const sheet = await screen.findByRole('dialog', { name: 'Nova hospedagem' })
    expect(within(sheet).getByLabelText('Nome')).toHaveValue('Barceló Bávaro Palace')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(api.created).toEqual([
        {
          kind: 'HOTEL',
          name: 'Barceló Bávaro Palace',
          address: 'Bávaro, Punta Cana',
          checkIn: '2099-01-10',
          checkOut: '2099-01-17',
          priceTier: 3,
          placeId: 'ChIJBarcelo',
        },
      ]),
    )
    await waitFor(() => expect(router.state.location.pathname).toBe('/trips/42'))
    expect(router.state.location.search).toBe('?tab=itinerary')
  })

  it('leitor busca mas não adiciona; Places fora do ar mostra aviso', async () => {
    const api = hotelsApi({ '': { results: [barcelo] } })
    server.use(tripDetail({ ...testTrip, role: 'VIEWER' }), ...api.handlers)
    const { user } = await openSearch()
    const card = await screen.findByRole('article', { name: 'Barceló Bávaro Palace' })
    expect(within(card).queryByRole('button', { name: 'Adicionar à viagem' })).toBeNull()

    server.use(
      http.get(
        '*/api/v1/trips/42/stops/7/lodging-search',
        () => new HttpResponse(null, { status: 503 }),
      ),
    )
    await user.click(screen.getByRole('button', { name: 'Todos' }))
    expect(await screen.findByText(/Busca de hotéis indisponível no momento/)).toBeInTheDocument()
  })

  it('a parada no roteiro leva à busca', async () => {
    const api = hotelsApi({ '': { results: [] } })
    server.use(tripDetail(), ...api.handlers)
    const { user, router } = renderApp('/trips/42')

    await user.click(await screen.findByRole('link', { name: 'Buscar hotéis em Punta Cana' }))
    expect(router.state.location.pathname).toBe('/trips/42/stops/7/hotels')
    expect(
      await screen.findByText('Nenhum hotel encontrado com esses filtros.'),
    ).toBeInTheDocument()
  })
})
