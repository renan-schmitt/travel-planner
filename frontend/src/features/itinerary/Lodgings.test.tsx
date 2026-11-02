import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { CreateLodging, Lodging, Stop, UpdateLodging } from '@/features/itinerary/api'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { testTrip, tripDetail } from '@/test/trips'

const lisbon: Stop = {
  id: 1,
  seq: 1,
  cityName: 'Lisboa',
  countryCode: 'PT',
  timezone: 'Europe/Lisbon',
  arrivalDate: '2099-03-11',
  departureDate: '2099-03-14',
  nights: 3,
  version: 0,
}

const sintra: Stop = {
  ...lisbon,
  id: 2,
  seq: 2,
  cityName: 'Sintra',
  arrivalDate: '2099-03-14',
  departureDate: '2099-03-14',
  nights: 0,
}

const avenida: Lodging = {
  id: 20,
  stopId: 1,
  kind: 'HOTEL',
  name: 'Hotel Avenida',
  checkIn: '2099-03-11',
  checkOut: '2099-03-13',
  nights: 2,
  boardType: 'BREAKFAST',
  priceTier: 3,
  priceTierSource: 'USER',
  bookingRef: 'BK-1',
  version: 0,
}

const alfama: Lodging = {
  id: 21,
  stopId: 1,
  kind: 'RENTAL',
  name: 'Casa Alfama',
  checkIn: '2099-03-13',
  checkOut: '2099-03-14',
  nights: 1,
  version: 0,
}

function itinerary(stops: Stop[], lodgings: Lodging[]) {
  return [
    http.get('*/api/v1/trips/42/stops', () => HttpResponse.json(stops)),
    http.get('*/api/v1/trips/42/segments', () => HttpResponse.json([])),
    http.get('*/api/v1/trips/42/lodgings', () => HttpResponse.json(lodgings)),
  ]
}

async function openItinerary() {
  const result = renderApp('/trips/42')
  await screen.findByRole('tab', { name: 'Roteiro', selected: true })
  return result
}

describe('Hospedagens no roteiro', () => {
  it('mostra duas hospedagens na mesma parada, com datas, regime e faixa de preço', async () => {
    server.use(tripDetail(), ...itinerary([lisbon], [avenida, alfama]))
    await openItinerary()

    const group = await screen.findByRole('group', { name: 'Hospedagens em Lisboa' })
    const hotel = within(group).getByRole('button', { name: 'Editar hospedagem: Hotel Avenida' })
    expect(hotel).toHaveTextContent('2 noites')
    expect(hotel).toHaveTextContent('Café da manhã · Reserva BK-1')
    expect(within(hotel).getByRole('img', { name: 'Faixa de preço: 3 de 4' })).toBeInTheDocument()
    const casa = within(group).getByRole('button', { name: 'Editar hospedagem: Casa Alfama' })
    expect(casa).toHaveTextContent('1 noite')
    expect(casa).toHaveTextContent('Aluguel')
    expect(within(casa).queryByRole('img')).not.toBeInTheDocument()
    expect(
      within(group).getByRole('button', { name: 'Adicionar hospedagem em Lisboa' }),
    ).toHaveTextContent('Outra hospedagem')
  })

  it('cadastra uma hospedagem com regime e faixa de preço', async () => {
    const bodies: CreateLodging[] = []
    server.use(
      tripDetail(),
      ...itinerary([lisbon, sintra], [avenida]),
      http.post('*/api/v1/trips/42/stops/1/lodgings', async ({ request }) => {
        bodies.push((await request.json()) as CreateLodging)
        return HttpResponse.json(alfama, { status: 201 })
      }),
    )
    const { user } = await openItinerary()

    // Bate-volta não tem hospedagem.
    expect(
      screen.queryByRole('button', { name: 'Adicionar hospedagem em Sintra' }),
    ).not.toBeInTheDocument()
    await user.click(await screen.findByRole('button', { name: 'Adicionar hospedagem em Lisboa' }))
    const sheet = await screen.findByRole('dialog', { name: 'Nova hospedagem' })
    expect(within(sheet).getByRole('radio', { name: 'Hotel/pousada' })).toBeChecked()
    // Começa depois da hospedagem que já existe.
    expect(within(sheet).getByLabelText('Check-in')).toHaveValue('2099-03-13')
    expect(within(sheet).getByLabelText('Check-out')).toHaveValue('2099-03-14')
    expect(within(sheet).getByRole('radio', { name: 'Não sei' })).toBeChecked()

    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(await within(sheet).findByText('Informe o nome da hospedagem.')).toBeInTheDocument()

    await user.click(within(sheet).getByRole('radio', { name: 'Aluguel' }))
    await user.type(within(sheet).getByLabelText('Nome'), 'Casa Alfama')
    await user.selectOptions(within(sheet).getByLabelText('Regime'), 'ALL_INCLUSIVE')
    await user.click(within(sheet).getByRole('radio', { name: 'Faixa de preço: 2 de 4' }))
    await user.type(within(sheet).getByLabelText('Código da reserva'), 'XYZ')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(bodies).toEqual([
        {
          kind: 'RENTAL',
          name: 'Casa Alfama',
          checkIn: '2099-03-13',
          checkOut: '2099-03-14',
          boardType: 'ALL_INCLUSIVE',
          priceTier: 2,
          bookingRef: 'XYZ',
        },
      ]),
    )
    expect(await screen.findByText('Hospedagem adicionada.')).toBeInTheDocument()
  })

  it('cadastra a casa de um amigo/parente, sem regime, preço nem reserva', async () => {
    const bodies: CreateLodging[] = []
    const home: Lodging = {
      id: 22,
      stopId: 1,
      kind: 'FRIENDS_HOME',
      name: 'Casa da tia Ana',
      address: 'Rua das Flores, 10',
      checkIn: '2099-03-11',
      checkOut: '2099-03-14',
      nights: 3,
      version: 0,
    }
    let lodgings: Lodging[] = []
    server.use(
      tripDetail(),
      // Antes do `itinerary`: o primeiro handler que casa ganha.
      http.get('*/api/v1/trips/42/lodgings', () => HttpResponse.json(lodgings)),
      ...itinerary([lisbon], []),
      http.post('*/api/v1/trips/42/stops/1/lodgings', async ({ request }) => {
        bodies.push((await request.json()) as CreateLodging)
        lodgings = [home]
        return HttpResponse.json(home, { status: 201 })
      }),
    )
    const { user } = await openItinerary()

    await user.click(await screen.findByRole('button', { name: 'Adicionar hospedagem em Lisboa' }))
    const sheet = await screen.findByRole('dialog', { name: 'Nova hospedagem' })
    await user.click(within(sheet).getByRole('radio', { name: 'Casa de amigo/parente' }))
    expect(within(sheet).queryByLabelText('Regime')).not.toBeInTheDocument()
    expect(within(sheet).queryByRole('radio', { name: 'Não sei' })).not.toBeInTheDocument()
    expect(within(sheet).queryByLabelText('Código da reserva')).not.toBeInTheDocument()
    await user.type(within(sheet).getByLabelText('Nome'), 'Casa da tia Ana')
    await user.type(within(sheet).getByLabelText('Endereço'), 'Rua das Flores, 10')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(bodies).toEqual([
        {
          kind: 'FRIENDS_HOME',
          name: 'Casa da tia Ana',
          address: 'Rua das Flores, 10',
          checkIn: '2099-03-11',
          checkOut: '2099-03-14',
        },
      ]),
    )
    expect(
      await screen.findByRole('button', { name: 'Editar hospedagem: Casa da tia Ana' }),
    ).toHaveTextContent('Casa de amigo/parente · Rua das Flores, 10')
  })

  it('recusa datas fora da parada e edita com a versão', async () => {
    const puts: UpdateLodging[] = []
    server.use(
      tripDetail(),
      ...itinerary([lisbon], [avenida]),
      http.put('*/api/v1/trips/42/stops/1/lodgings/20', async ({ request }) => {
        puts.push((await request.json()) as UpdateLodging)
        return HttpResponse.json(avenida)
      }),
    )
    const { user } = await openItinerary()

    await user.click(
      await screen.findByRole('button', { name: 'Editar hospedagem: Hotel Avenida' }),
    )
    const sheet = await screen.findByRole('dialog', { name: 'Editar hospedagem' })
    expect(within(sheet).getByRole('radio', { name: 'Faixa de preço: 3 de 4' })).toBeChecked()
    const checkOut = within(sheet).getByLabelText('Check-out')
    await user.clear(checkOut)
    await user.type(checkOut, '2099-03-16')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(await within(sheet).findByRole('alert')).toHaveTextContent(
      'As datas devem ficar dentro da parada',
    )
    expect(puts).toEqual([])

    await user.clear(checkOut)
    await user.type(checkOut, '2099-03-14')
    await user.click(within(sheet).getByRole('radio', { name: 'Não sei' }))
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts).toHaveLength(1))
    expect(puts[0]).toMatchObject({ version: 0, checkOut: '2099-03-14', boardType: 'BREAKFAST' })
    expect(puts[0]).not.toHaveProperty('priceTier')
  })

  it('avisa ao encurtar uma parada que deixaria uma hospedagem de fora', async () => {
    const puts: unknown[] = []
    server.use(
      tripDetail(),
      ...itinerary([lisbon], [alfama]),
      http.put('*/api/v1/trips/42/stops/1', async ({ request }) => {
        puts.push(await request.json())
        return HttpResponse.json(lisbon)
      }),
    )
    const { user } = await openItinerary()

    await user.click(await screen.findByRole('button', { name: 'Editar parada em Lisboa' }))
    const sheet = await screen.findByRole('dialog', { name: 'Editar parada' })
    const departure = within(sheet).getByLabelText('Saída')
    await user.clear(departure)
    await user.type(departure, '2099-03-13')
    expect(await within(sheet).findByRole('alert')).toHaveTextContent('A hospedagem Casa Alfama')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(puts).toEqual([])
  })

  it('leitor vê as hospedagens sem poder editar', async () => {
    server.use(tripDetail({ ...testTrip, role: 'VIEWER' }), ...itinerary([lisbon], [avenida]))
    await openItinerary()

    expect(await screen.findByText('Hotel Avenida')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Editar hospedagem/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Adicionar hospedagem/ })).not.toBeInTheDocument()
  })
})
