import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { CreateSegment, Segment, Stop, UpdateSegment } from '@/features/itinerary/api'
import { loggedIn, testUser } from '@/test/handlers'
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

const porto: Stop = {
  ...lisbon,
  id: 2,
  seq: 2,
  cityName: 'Porto',
  arrivalDate: '2099-03-14',
  departureDate: '2099-03-17',
}

/** GRU 10/03 22:40 (GMT-3) → LIS 11/03 11:15 (GMT+0): voo noturno de 9h35. */
const outbound: Segment = {
  id: 10,
  type: 'FLIGHT',
  local: false,
  destinationStopId: 1,
  originLabel: 'GRU',
  destinationLabel: 'LIS',
  departAt: '2099-03-10T22:40:00',
  departTz: 'America/Sao_Paulo',
  departInstant: '2099-03-10T22:40:00-03:00',
  arriveAt: '2099-03-11T11:15:00',
  arriveTz: 'Europe/Lisbon',
  arriveInstant: '2099-03-11T11:15:00Z',
  carrier: 'TAP',
  code: 'TP 88',
  bookingRef: 'ABC123',
  version: 0,
}

const transfer: Segment = {
  id: 11,
  type: 'TRANSFER',
  local: true,
  localStopId: 1,
  originLabel: 'Aeroporto',
  destinationLabel: 'Hotel',
  departAt: '2099-03-11T12:00:00',
  departTz: 'Europe/Lisbon',
  departInstant: '2099-03-11T12:00:00Z',
  version: 0,
}

const train: Segment = {
  id: 12,
  type: 'TRAIN',
  local: false,
  originStopId: 1,
  destinationStopId: 2,
  departAt: '2099-03-14T09:00:00',
  departTz: 'Europe/Lisbon',
  departInstant: '2099-03-14T09:00:00Z',
  version: 0,
}

const inbound: Segment = {
  id: 13,
  type: 'FLIGHT',
  local: false,
  originStopId: 2,
  originLabel: 'OPO',
  destinationLabel: 'GRU',
  departAt: '2099-03-17T23:30:00',
  departTz: 'Europe/Lisbon',
  departInstant: '2099-03-17T23:30:00Z',
  arriveAt: '2099-03-18T06:40:00',
  arriveTz: 'America/Sao_Paulo',
  arriveInstant: '2099-03-18T06:40:00-03:00',
  version: 0,
}

const homeUser = loggedIn({
  ...testUser,
  homeCity: { name: 'São Paulo', countryCode: 'BR', timezone: 'America/Sao_Paulo' },
})

function itinerary(stops: Stop[], segments: Segment[]) {
  return [
    http.get('*/api/v1/trips/42/stops', () => HttpResponse.json(stops)),
    http.get('*/api/v1/trips/42/segments', () => HttpResponse.json(segments)),
    http.get('*/api/v1/trips/42/lodgings', () => HttpResponse.json([])),
  ]
}

async function openItinerary() {
  const result = renderApp('/trips/42')
  await screen.findByRole('tab', { name: 'Roteiro', selected: true })
  return result
}

describe('Trechos de transporte no roteiro', () => {
  it('intercala os trechos entre as paradas, com o horário no fuso de cada ponta', async () => {
    server.use(
      homeUser,
      tripDetail(),
      ...itinerary([lisbon, porto], [outbound, transfer, train, inbound]),
    )
    await openItinerary()

    const flight = await screen.findByRole('button', { name: 'Editar trecho: Voo GRU → LIS' })
    expect(flight).toHaveTextContent('10/03 22:40 (GMT-3) → 11/03 11:15 (GMT+0) · 9h35')
    expect(flight).toHaveTextContent('TAP TP 88 · Reserva ABC123')

    // Ordem visual: casa → voo → Lisboa → transfer → trem → Porto → voo → casa.
    const order = [
      screen.getAllByText(/^Casa/)[0]!,
      flight,
      screen.getByRole('button', { name: 'Editar parada em Lisboa' }),
      screen.getByRole('button', { name: 'Editar trecho: Transfer Aeroporto → Hotel' }),
      screen.getByRole('button', { name: 'Editar trecho: Trem Lisboa → Porto' }),
      screen.getByRole('button', { name: 'Editar parada em Porto' }),
      screen.getByRole('button', { name: 'Editar trecho: Voo OPO → GRU' }),
      screen.getAllByText(/^Casa/)[1]!,
    ]
    for (let i = 1; i < order.length; i++) {
      expect(order[i - 1]!.compareDocumentPosition(order[i]!)).toBe(
        Node.DOCUMENT_POSITION_FOLLOWING,
      )
    }
    expect(screen.getAllByText('· São Paulo')).toHaveLength(2)
    expect(screen.getByRole('button', { name: 'Editar trecho: Voo OPO → GRU' })).toHaveTextContent(
      '17/03 23:30 (GMT+0) → 18/03 06:40 (GMT-3) · 10h10',
    )
  })

  it('cadastra o voo de ida: casa → primeira parada, com os fusos sugeridos', async () => {
    const bodies: CreateSegment[] = []
    server.use(
      homeUser,
      tripDetail(),
      ...itinerary([lisbon, porto], []),
      http.post('*/api/v1/trips/42/segments', async ({ request }) => {
        bodies.push((await request.json()) as CreateSegment)
        return HttpResponse.json(outbound, { status: 201 })
      }),
    )
    const { user } = await openItinerary()

    await user.click(await screen.findByRole('button', { name: 'Adicionar trecho' }))
    const sheet = await screen.findByRole('dialog', { name: 'Novo trecho' })
    expect(within(sheet).getByRole('radio', { name: 'Voo' })).toBeChecked()
    expect(within(sheet).getByLabelText('De')).toHaveValue('home')
    expect(within(sheet).getByLabelText('Para')).toHaveValue('stop:1')
    expect(within(sheet).getByLabelText('Fuso da partida')).toHaveValue('America/Sao_Paulo')
    expect(within(sheet).getByLabelText('Fuso da chegada')).toHaveValue('Europe/Lisbon')

    await user.type(within(sheet).getByLabelText('Local de partida'), 'GRU')
    await user.type(within(sheet).getByLabelText('Local de chegada'), 'LIS')
    const [departDate, arriveDate] = within(sheet).getAllByLabelText('Data')
    const [departTime, arriveTime] = within(sheet).getAllByLabelText('Hora')
    await user.clear(departDate!)
    await user.type(departDate!, '2099-03-10')
    await user.type(departTime!, '22:40')
    await user.type(arriveDate!, '2099-03-11')
    await user.type(arriveTime!, '11:15')
    await user.type(within(sheet).getByLabelText('Companhia'), 'TAP')
    await user.type(within(sheet).getByLabelText('Nº do voo'), 'TP 88')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(bodies).toEqual([
        {
          type: 'FLIGHT',
          originLabel: 'GRU',
          destinationStopId: 1,
          destinationLabel: 'LIS',
          departAt: '2099-03-10T22:40',
          departTz: 'America/Sao_Paulo',
          arriveAt: '2099-03-11T11:15',
          arriveTz: 'Europe/Lisbon',
          carrier: 'TAP',
          code: 'TP 88',
        },
      ]),
    )
    expect(await screen.findByText('Trecho adicionado.')).toBeInTheDocument()
  })

  it('valida a chegada pelos fusos: aceita "chegar antes" no relógio, recusa antes no tempo real', async () => {
    const puts: UpdateSegment[] = []
    server.use(
      homeUser,
      tripDetail(),
      ...itinerary([lisbon, porto], [train]),
      http.put('*/api/v1/trips/42/segments/12', async ({ request }) => {
        puts.push((await request.json()) as UpdateSegment)
        return HttpResponse.json(train)
      }),
    )
    const { user } = await openItinerary()

    await user.click(
      await screen.findByRole('button', { name: 'Editar trecho: Trem Lisboa → Porto' }),
    )
    const sheet = await screen.findByRole('dialog', { name: 'Editar trecho' })
    const arriveDate = within(sheet).getAllByLabelText('Data')[1]!
    const arriveTime = within(sheet).getAllByLabelText('Hora')[1]!
    // Partida 09:00 em Lisboa; chegada 10:30 em Roma = 09:30 em Lisboa → válida.
    // Chegada 09:50 em Roma = 08:50 em Lisboa → antes da partida.
    await user.type(arriveDate, '2099-03-14')
    await user.type(arriveTime, '09:50')
    await user.selectOptions(within(sheet).getByLabelText('Fuso da chegada'), 'Europe/Rome')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(await within(sheet).findByRole('alert')).toHaveTextContent(
      'A chegada deve ser depois da partida, considerando os fusos.',
    )
    expect(puts).toEqual([])

    // Açores (GMT-1): chega 08:50 no relógio local, mas são 09:50 em Lisboa → válida.
    await user.selectOptions(within(sheet).getByLabelText('Fuso da chegada'), 'Atlantic/Azores')
    await user.clear(arriveTime)
    await user.type(arriveTime, '08:50')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts).toHaveLength(1))
    expect(puts[0]).toMatchObject({
      version: 0,
      originStopId: 1,
      destinationStopId: 2,
      arriveAt: '2099-03-14T08:50',
      arriveTz: 'Atlantic/Azores',
    })
  })

  it('cadastra um trecho local numa parada, sem destino', async () => {
    const bodies: CreateSegment[] = []
    server.use(
      homeUser,
      tripDetail(),
      ...itinerary([lisbon, porto], [outbound]),
      http.post('*/api/v1/trips/42/segments', async ({ request }) => {
        bodies.push((await request.json()) as CreateSegment)
        return HttpResponse.json(transfer, { status: 201 })
      }),
    )
    const { user } = await openItinerary()

    await user.click(await screen.findByRole('button', { name: 'Adicionar trecho' }))
    const sheet = await screen.findByRole('dialog', { name: 'Novo trecho' })
    await user.click(within(sheet).getByRole('radio', { name: 'Transfer' }))
    await user.selectOptions(within(sheet).getByLabelText('De'), 'local:1')
    expect(within(sheet).queryByLabelText('Para')).not.toBeInTheDocument()
    expect(within(sheet).getByLabelText('Fuso da partida')).toHaveValue('Europe/Lisbon')
    await user.type(within(sheet).getByLabelText('Local de partida'), 'Aeroporto')
    await user.type(within(sheet).getByLabelText('Local de chegada'), 'Hotel')
    const departDate = within(sheet).getAllByLabelText('Data')[0]!
    await user.clear(departDate)
    await user.type(departDate, '2099-03-11')
    await user.type(within(sheet).getAllByLabelText('Hora')[0]!, '12:00')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(bodies).toEqual([
        {
          type: 'TRANSFER',
          localStopId: 1,
          originLabel: 'Aeroporto',
          destinationLabel: 'Hotel',
          departAt: '2099-03-11T12:00',
          departTz: 'Europe/Lisbon',
        },
      ]),
    )
  })

  it('leitor vê os trechos sem poder editar', async () => {
    server.use(
      homeUser,
      tripDetail({ ...testTrip, role: 'VIEWER' }),
      ...itinerary([lisbon], [outbound]),
    )
    await openItinerary()

    expect(await screen.findByText('Voo GRU → LIS')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Editar trecho/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Adicionar trecho' })).not.toBeInTheDocument()
  })
})
