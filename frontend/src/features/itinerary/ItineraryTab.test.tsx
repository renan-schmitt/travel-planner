import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { CreateStop, Stop, UpdateStop } from '@/features/itinerary/api'
import { cityAutocomplete, cityDetails, placesUnavailable } from '@/test/places'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { testTrip, tripDetail } from '@/test/trips'

const lisbonStop: Stop = {
  id: 1,
  seq: 1,
  cityPlaceId: 'ChIJLisboa',
  cityName: 'Lisboa',
  countryCode: 'PT',
  lat: 38.722252,
  lng: -9.139337,
  timezone: 'Europe/Lisbon',
  arrivalDate: '2099-03-10',
  departureDate: '2099-03-14',
  nights: 4,
  notes: 'Alfama',
  version: 0,
}

const portoStop: Stop = {
  ...lisbonStop,
  id: 2,
  seq: 2,
  cityPlaceId: 'ChIJPorto',
  cityName: 'Porto',
  arrivalDate: '2099-03-14',
  departureDate: '2099-03-17',
  nights: 3,
  notes: undefined,
}

function stopList(stops: Stop[]) {
  return http.get('*/api/v1/trips/42/stops', () => HttpResponse.json(stops))
}

async function stopItems() {
  return within(await screen.findByRole('list', { name: 'Paradas' })).getAllByRole('listitem')
}

async function openItinerary(path = '/trips/42') {
  const result = renderApp(path)
  // "Roteiro" é a aba inicial.
  await screen.findByRole('tab', { name: 'Roteiro', selected: true })
  return result
}

describe('Aba Roteiro', () => {
  it('lista as paradas com país, datas, noites e fuso', async () => {
    server.use(tripDetail(), stopList([lisbonStop, portoStop]))
    await openItinerary()

    const items = await stopItems()
    expect(items).toHaveLength(2)
    expect(within(items[0]!).getByText(/Lisboa/)).toHaveTextContent('Lisboa, Portugal')
    expect(within(items[0]!).getByText(/4 noites/)).toBeInTheDocument()
    expect(within(items[0]!).getByText('Fuso: Europe/Lisbon')).toBeInTheDocument()
    expect(within(items[0]!).getByText('Alfama')).toBeInTheDocument()
    expect(within(items[1]!).getByText(/3 noites/)).toBeInTheDocument()
  })

  it('adiciona Porto pelo autocomplete, começando no fim da última parada', async () => {
    const search = cityAutocomplete([
      { placeId: 'ChIJPorto', name: 'Porto', description: 'Portugal' },
    ])
    const details = cityDetails([
      {
        placeId: 'ChIJPorto',
        name: 'Porto',
        countryCode: 'PT',
        lat: 41.15,
        lng: -8.62,
        timezone: 'Europe/Lisbon',
      },
    ])
    const bodies: CreateStop[] = []
    server.use(
      tripDetail(),
      stopList([lisbonStop]),
      search.handler,
      details.handler,
      http.post('*/api/v1/trips/42/stops', async ({ request }) => {
        bodies.push((await request.json()) as CreateStop)
        return HttpResponse.json(portoStop, { status: 201 })
      }),
    )
    const { user } = await openItinerary()

    await user.click(await screen.findByRole('button', { name: 'Adicionar parada' }))
    const sheet = await screen.findByRole('dialog', { name: 'Nova parada' })
    expect(within(sheet).getByLabelText('Chegada')).toHaveValue('2099-03-14')
    await user.type(within(sheet).getByRole('combobox', { name: 'Cidade' }), 'Port')
    await user.click(await within(sheet).findByRole('option', { name: /Porto/ }))
    await within(sheet).findByText('Portugal · Europe/Lisbon')
    await user.type(within(sheet).getByLabelText('Saída'), '2099-03-17')
    expect(within(sheet).getByText('3 noites')).toBeInTheDocument()
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(bodies).toEqual([
        {
          cityPlaceId: 'ChIJPorto',
          cityName: 'Porto',
          countryCode: 'PT',
          lat: 41.15,
          lng: -8.62,
          timezone: 'Europe/Lisbon',
          arrivalDate: '2099-03-14',
          departureDate: '2099-03-17',
        },
      ]),
    )
    expect(await screen.findByText('Parada adicionada.')).toBeInTheDocument()
  })

  it('com o Places fora do ar, cadastra a cidade à mão', async () => {
    const bodies: CreateStop[] = []
    server.use(
      tripDetail(),
      stopList([]),
      placesUnavailable(),
      http.post('*/api/v1/trips/42/stops', async ({ request }) => {
        bodies.push((await request.json()) as CreateStop)
        return HttpResponse.json(lisbonStop, { status: 201 })
      }),
    )
    const { user } = await openItinerary()

    await user.click(await screen.findByRole('button', { name: 'Adicionar parada' }))
    const sheet = await screen.findByRole('dialog', { name: 'Nova parada' })
    // Sem paradas, a chegada sugerida é o início da viagem.
    expect(within(sheet).getByLabelText('Chegada')).toHaveValue(testTrip.startDate)
    await user.type(within(sheet).getByRole('combobox', { name: 'Cidade' }), 'Lisboa')
    await user.click(
      await within(sheet).findByRole('button', { name: 'Digitar a cidade manualmente' }),
    )
    await user.selectOptions(within(sheet).getByLabelText('País'), 'PT')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(await within(sheet).findByText('Informe o fuso horário da cidade.')).toBeInTheDocument()

    await user.selectOptions(within(sheet).getByLabelText('Fuso horário'), 'Europe/Lisbon')
    await user.type(within(sheet).getByLabelText('Saída'), '2099-07-01')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(bodies).toEqual([
        {
          cityName: 'Lisboa',
          countryCode: 'PT',
          timezone: 'Europe/Lisbon',
          arrivalDate: '2099-07-01',
          departureDate: '2099-07-01',
        },
      ]),
    )
  })

  it('avisa a sobreposição de datas antes de salvar', async () => {
    const puts: UpdateStop[] = []
    server.use(
      tripDetail(),
      stopList([lisbonStop, portoStop]),
      http.put('*/api/v1/trips/42/stops/2', async ({ request }) => {
        puts.push((await request.json()) as UpdateStop)
        return HttpResponse.json(portoStop)
      }),
    )
    const { user } = await openItinerary()

    await user.click(await screen.findByRole('button', { name: 'Editar parada em Porto' }))
    const sheet = await screen.findByRole('dialog', { name: 'Editar parada' })
    const arrival = within(sheet).getByLabelText('Chegada')
    await user.clear(arrival)
    await user.type(arrival, '2099-03-12')

    expect(within(sheet).getByRole('alert')).toHaveTextContent(/sobrepõem à parada em Lisboa/)
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(puts).toEqual([])

    await user.clear(arrival)
    await user.type(arrival, '2099-03-14')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts).toHaveLength(1))
    expect(puts[0]).toMatchObject({
      version: 0,
      cityPlaceId: 'ChIJPorto',
      arrivalDate: '2099-03-14',
    })
  })

  it('reordena com os botões ↑/↓', async () => {
    const orders: number[][] = []
    server.use(
      tripDetail(),
      stopList([lisbonStop, portoStop]),
      http.put('*/api/v1/trips/42/stops/order', async ({ request }) => {
        const { stopIds } = (await request.json()) as { stopIds: number[] }
        orders.push(stopIds)
        return HttpResponse.json([portoStop, lisbonStop])
      }),
    )
    const { user } = await openItinerary()

    expect(await screen.findByRole('button', { name: 'Subir Lisboa' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Descer Lisboa' }))

    await waitFor(() => expect(orders).toEqual([[2, 1]]))
    await waitFor(async () => expect((await stopItems())[0]).toHaveTextContent('Porto'))
  })

  it('exclui a parada com confirmação', async () => {
    let deleted = false
    server.use(
      tripDetail(),
      stopList([lisbonStop]),
      http.delete('*/api/v1/trips/42/stops/1', () => {
        deleted = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { user } = await openItinerary()

    await user.click(await screen.findByRole('button', { name: 'Editar parada em Lisboa' }))
    await user.click(await screen.findByRole('button', { name: 'Excluir' }))
    await user.click(await screen.findByRole('button', { name: 'Excluir parada' }))

    await waitFor(() => expect(deleted).toBe(true))
  })

  it('leitor só vê a lista', async () => {
    server.use(tripDetail({ ...testTrip, role: 'VIEWER' }), stopList([lisbonStop, portoStop]))
    await openItinerary()

    expect(await stopItems()).toHaveLength(2)
    expect(screen.queryByRole('button', { name: 'Adicionar parada' })).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /Subir|Descer|Editar parada/ }),
    ).not.toBeInTheDocument()
  })
})
