import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { Stop } from '@/features/itinerary/api'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { testTrip, tripDetail } from '@/test/trips'
import type {
  CreateSavedPlace,
  PlaceResult,
  PlaceSearch,
  SavedPlace,
  UpdateSavedPlace,
} from './api'

const lisboa: Stop = {
  id: 1,
  seq: 1,
  cityName: 'Lisboa',
  countryCode: 'PT',
  lat: 38.7223,
  lng: -9.1393,
  timezone: 'Europe/Lisbon',
  arrivalDate: '2099-03-11',
  departureDate: '2099-03-14',
  nights: 3,
  version: 0,
}

const belcanto: PlaceResult = {
  placeId: 'ChIJBelcanto',
  name: 'Belcanto',
  address: 'Chiado, Lisboa',
  lat: 38.71,
  lng: -9.14,
  rating: 4.7,
  userRatingCount: 3000,
  priceTier: 4,
  priceTierSource: 'PLACES',
  primaryType: 'fine_dining_restaurant',
  photoName: 'places/ChIJBelcanto/photos/F1',
}

const bifana: PlaceResult = {
  placeId: 'ChIJBifana',
  name: 'Bifanas do Afonso',
  rating: 4.5,
  userRatingCount: 900,
  priceTier: 1,
  priceTierSource: 'HEURISTIC',
  primaryType: 'fast_food_restaurant',
}

const jeronimos: PlaceResult = {
  placeId: 'ChIJJeronimos',
  name: 'Mosteiro dos Jerónimos',
  rating: 4.7,
  primaryType: 'historical_landmark',
}

const savedTasca: SavedPlace = {
  id: 50,
  stopId: 1,
  placeId: 'ChIJTasca',
  name: 'Tasca do Chico',
  kind: 'RESTAURANT',
  priceTier: 2,
  priceTierSource: 'USER',
  notes: 'Fado à noite',
  visited: false,
  rating: 4.4,
  userRatingCount: 1200,
  googleMapsUri: 'https://maps.google.com/?cid=50',
  version: 0,
}

/** Handlers da tela; `searches` guarda as buscas, `created` os POSTs e `patches` os PATCHes. */
function placesApi(
  results: Record<string, PlaceResult[]>,
  saved: SavedPlace[] = [],
  center: Partial<PlaceSearch> = { center: 'LODGING', centerLodgingName: 'Hotel Avenida' },
) {
  const searches: URLSearchParams[] = []
  const created: CreateSavedPlace[] = []
  const patches: UpdateSavedPlace[] = []
  let current = [...saved]
  const handlers = [
    http.get('*/api/v1/trips/42/stops', () => HttpResponse.json([lisboa])),
    http.get('*/api/v1/trips/42/stops/1/saved-places', () => HttpResponse.json(current)),
    http.get('*/api/v1/trips/42/stops/1/places-search', ({ request }) => {
      const params = new URL(request.url).searchParams
      searches.push(params)
      const body: PlaceSearch = {
        results: results[params.get('kind') ?? ''] ?? [],
        center: 'CITY',
        centerLat: 38.7223,
        centerLng: -9.1393,
        ...center,
      }
      return HttpResponse.json(body)
    }),
    http.post('*/api/v1/trips/42/stops/1/saved-places', async ({ request }) => {
      const body = (await request.json()) as CreateSavedPlace
      created.push(body)
      const place: SavedPlace = {
        id: 60 + created.length,
        stopId: 1,
        placeId: body.placeId,
        name: body.name ?? body.placeId,
        kind: body.kind,
        priceTier: body.priceTier,
        visited: false,
        version: 0,
      }
      current = [...current, place]
      return HttpResponse.json(place, { status: 201 })
    }),
    http.patch('*/api/v1/trips/42/stops/1/saved-places/:id', async ({ request, params }) => {
      const body = (await request.json()) as UpdateSavedPlace
      patches.push(body)
      current = current.map((p) =>
        p.id === Number(params.id) ? { ...p, ...body, version: p.version + 1 } : p,
      )
      return HttpResponse.json(current.find((p) => p.id === Number(params.id)))
    }),
  ]
  return { handlers, searches, created, patches }
}

async function openPlaces(path = '/trips/42/stops/1/places?kind=restaurants') {
  const result = renderApp(path)
  await screen.findByRole('heading', { name: 'Lugares em Lisboa' })
  return result
}

describe('Lugares da parada', () => {
  it('lista os salvos e descobre e salva restaurantes', async () => {
    const api = placesApi({ RESTAURANT: [belcanto, bifana] }, [savedTasca])
    server.use(tripDetail(), ...api.handlers)
    const { user } = await openPlaces()

    const list = await screen.findByRole('list', { name: 'Restaurantes salvos' })
    expect(within(list).getByText('Tasca do Chico')).toBeInTheDocument()
    expect(within(list).getByLabelText('Nota 4,4 (1200 avaliações)')).toBeInTheDocument()
    expect(within(list).getByText('Fado à noite')).toBeInTheDocument()
    expect(within(list).getByRole('link', { name: 'Ver no Maps' })).toHaveAttribute(
      'href',
      'https://maps.google.com/?cid=50',
    )
    expect(api.searches).toHaveLength(0) // só busca no Places ao tocar em "Descobrir"

    await user.click(screen.getByRole('button', { name: 'Descobrir' }))
    const card = await screen.findByRole('article', { name: 'Belcanto' })
    expect(screen.getByText('Perto de Hotel Avenida, por popularidade.')).toBeInTheDocument()
    expect(within(card).getByLabelText('Nota 4,7 (3000 avaliações)')).toBeInTheDocument()
    expect(card).toHaveTextContent('Alta gastronomia')
    expect(card).toHaveTextContent('1,4 km de Hotel Avenida · Chiado, Lisboa')
    expect(api.searches[0]?.get('kind')).toBe('RESTAURANT')

    // Filtro de faixa vai para o backend.
    await user.click(screen.getByRole('button', { name: 'Faixa de preço: 1 de 4' }))
    await waitFor(() => expect(api.searches.at(-1)?.getAll('tiers')).toEqual(['1']))
    await user.click(screen.getByRole('button', { name: 'Faixa de preço: 1 de 4' }))

    await user.click(
      within(await screen.findByRole('article', { name: 'Belcanto' })).getByRole('button', {
        name: 'Salvar',
      }),
    )
    await waitFor(() =>
      expect(api.created).toEqual([
        { placeId: 'ChIJBelcanto', kind: 'RESTAURANT', name: 'Belcanto', priceTier: 4 },
      ]),
    )
    expect(
      await within(screen.getByRole('article', { name: 'Belcanto' })).findByText('Salvo'),
    ).toBeInTheDocument()
    expect(
      await within(screen.getByRole('list', { name: 'Restaurantes salvos' })).findByText(
        'Belcanto',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: /Restaurantes/ })).toHaveTextContent('2')
  })

  it('abre direto na busca de atrações (link da sugestão), sem filtro de faixa', async () => {
    const api = placesApi({ ATTRACTION: [jeronimos] }, [], { center: 'CITY' })
    server.use(tripDetail(), ...api.handlers)
    await openPlaces('/trips/42/stops/1/places?kind=attractions&discover=1')

    expect(
      await screen.findByRole('article', { name: 'Mosteiro dos Jerónimos' }),
    ).toHaveTextContent('Patrimônio histórico')
    expect(screen.getByText('Perto de centro da cidade, por popularidade.')).toBeInTheDocument()
    expect(screen.getByText(/Nenhuma atração salva/)).toBeInTheDocument()
    expect(screen.queryByRole('group', { name: 'Faixa de preço' })).not.toBeInTheDocument()
    expect(api.searches[0]?.get('kind')).toBe('ATTRACTION')
  })

  it('marca como visitado e edita notas', async () => {
    const api = placesApi({}, [savedTasca])
    server.use(tripDetail(), ...api.handlers)
    const { user } = await openPlaces()

    await user.click(await screen.findByRole('checkbox', { name: 'Visitado: Tasca do Chico' }))
    await waitFor(() =>
      expect(api.patches).toEqual([
        {
          version: 0,
          kind: 'RESTAURANT',
          priceTier: 2,
          notes: 'Fado à noite',
          visited: true,
        },
      ]),
    )

    await user.click(await screen.findByRole('button', { name: 'Editar Tasca do Chico' }))
    const sheet = await screen.findByRole('dialog', { name: 'Tasca do Chico' })
    const notes = within(sheet).getByLabelText('Notas')
    await user.clear(notes)
    await user.type(notes, 'Reservar')
    await user.selectOptions(within(sheet).getByLabelText('Tipo'), 'OTHER')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(api.patches).toHaveLength(2))
    expect(api.patches[1]).toMatchObject({ version: 1, kind: 'OTHER', notes: 'Reservar' })
    expect(await screen.findByRole('list', { name: 'Outros lugares' })).toBeInTheDocument()
  })

  it('leitor vê os salvos e a busca, sem salvar nem marcar', async () => {
    const api = placesApi({ RESTAURANT: [belcanto] }, [savedTasca])
    server.use(tripDetail({ ...testTrip, role: 'VIEWER' }), ...api.handlers)
    await openPlaces('/trips/42/stops/1/places?kind=restaurants&discover=1')

    const card = await screen.findByRole('article', { name: 'Belcanto' })
    expect(within(card).queryByRole('button', { name: 'Salvar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
    expect(screen.getByText('Tasca do Chico')).toBeInTheDocument()
  })

  it('avisa quando a busca está indisponível', async () => {
    const api = placesApi({})
    server.use(
      tripDetail(),
      http.get('*/api/v1/trips/42/stops/1/places-search', () =>
        HttpResponse.json({}, { status: 503 }),
      ),
      ...api.handlers,
    )
    await openPlaces('/trips/42/stops/1/places?kind=restaurants&discover=1')
    expect(await screen.findByText(/Busca de lugares indisponível/)).toBeInTheDocument()
  })
})
