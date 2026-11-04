import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type {
  CreateActivity,
  Itinerary,
  ItineraryActivity,
  ItineraryDay,
  ItinerarySegment,
  UpdateActivity,
} from '@/features/itinerary/api'
import type { SavedPlace } from '@/features/places/api'
import { todayIso } from '@/lib/dates'
import { loggedIn, testUser } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { testTrip, tripDetail } from '@/test/trips'

const lisbon = {
  id: 1,
  cityName: 'Lisboa',
  countryCode: 'PT',
  timezone: 'Europe/Lisbon',
  arrivalDate: '2099-03-11',
  departureDate: '2099-03-14',
}
const porto = { ...lisbon, id: 2, cityName: 'Porto', arrivalDate: '2099-03-14' }

/** GRU 10/03 22:40 (GMT-3) → Lisboa 11/03 11:15 (GMT+0). */
const flight: ItinerarySegment = {
  id: 10,
  type: 'FLIGHT',
  local: false,
  origin: 'GRU',
  destination: 'Lisboa',
  departAt: '2099-03-10T22:40:00',
  departInstant: '2099-03-10T22:40:00-03:00',
  arriveAt: '2099-03-11T11:15:00',
  arriveInstant: '2099-03-11T11:15:00Z',
  carrier: 'TAP',
  code: 'TP 88',
}

const train: ItinerarySegment = {
  id: 11,
  type: 'TRAIN',
  local: false,
  origin: 'Lisboa',
  destination: 'Porto',
  departAt: '2099-03-14T10:00:00',
  departInstant: '2099-03-14T10:00:00Z',
  arriveAt: '2099-03-14T12:50:00',
  arriveInstant: '2099-03-14T12:50:00Z',
}

const hotel = {
  id: 100,
  stopId: 1,
  kind: 'HOTEL' as const,
  name: 'Hotel Avenida',
  checkIn: '2099-03-11',
  checkOut: '2099-03-14',
}

const days: ItineraryDay[] = [
  {
    date: '2099-03-10',
    stops: [],
    events: [
      {
        kind: 'SEGMENT_DEPARTURE',
        time: '22:40',
        instant: '2099-03-10T22:40:00-03:00',
        segment: flight,
      },
    ],
    overnight: { kind: 'IN_TRANSIT', segment: flight },
    gap: false,
  },
  {
    date: '2099-03-11',
    stops: [lisbon],
    events: [
      { kind: 'SEGMENT_ARRIVAL', time: '11:15', instant: '2099-03-11T11:15:00Z', segment: flight },
      { kind: 'CHECK_IN', lodging: hotel },
    ],
    overnight: { kind: 'LODGING', lodging: hotel },
    gap: false,
  },
  {
    date: '2099-03-14',
    stops: [lisbon, porto],
    events: [
      { kind: 'CHECK_OUT', lodging: hotel },
      { kind: 'SEGMENT', time: '10:00', instant: '2099-03-14T10:00:00Z', segment: train },
    ],
    overnight: { kind: 'MISSING' },
    gap: true,
  },
  {
    date: '2099-03-15',
    stops: [porto],
    events: [],
    overnight: { kind: 'MISSING' },
    gap: true,
  },
]

function itinerary(body: Partial<Itinerary>) {
  return http.get('*/api/v1/trips/42/itinerary', () =>
    HttpResponse.json({ days: [], outsideActivities: [], ...body }),
  )
}

async function openTimeline() {
  const result = renderApp('/trips/42')
  await result.user.click(await screen.findByRole('tab', { name: 'Timeline' }))
  return result
}

function day(name: RegExp) {
  return screen.getByRole('region', { name })
}

describe('Timeline dia a dia', () => {
  it('mostra os dias com eventos no horário local, onde se dorme e as noites descobertas', async () => {
    server.use(loggedIn(testUser), tripDetail(), itinerary({ days }))
    await openTimeline()

    const first = await screen.findByRole('region', { name: /^Ter, 10\/03/ })
    expect(first).toHaveTextContent('Em trânsito')
    expect(first).toHaveTextContent('22:40GMT-3Partida · Voo GRU → Lisboa')
    expect(first).toHaveTextContent('Chegada 11/03 11:15 (GMT+0) · TAP TP 88')
    expect(first).toHaveTextContent('Dormir: em trânsito (Voo GRU → Lisboa)')

    const arrival = day(/^Qua, 11\/03/)
    expect(arrival).toHaveTextContent('🇵🇹 Lisboa')
    expect(arrival).toHaveTextContent('11:15GMT+0Chegada · Voo GRU → Lisboa')
    expect(arrival).toHaveTextContent('Check-in · Hotel Avenida')
    expect(arrival).toHaveTextContent('Dormir: Hotel Avenida')

    const change = day(/^Sáb, 14\/03/)
    expect(change).toHaveTextContent('🇵🇹 Lisboa → 🇵🇹 Porto')
    const items = within(change).getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('Check-out · Hotel Avenida')
    expect(items[1]).toHaveTextContent('10:00GMT+0Trem Lisboa → Porto')
    expect(items[1]).toHaveTextContent('Chegada 12:50 (GMT+0)')
    expect(change).toHaveTextContent('Sem hospedagem para esta noite')

    expect(day(/^Dom, 15\/03/)).toHaveTextContent('Dia livre')
    expect(screen.getByText('2 noites sem hospedagem')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ir para hoje' })).not.toBeInTheDocument()
  })

  it('sem datas, explica quando a timeline aparece', async () => {
    server.use(loggedIn(testUser), tripDetail(), itinerary({ days: [] }))
    await openTimeline()

    expect(
      await screen.findByText(
        'A timeline aparece quando a viagem tiver datas ou paradas no roteiro.',
      ),
    ).toBeInTheDocument()
  })

  it('durante a viagem abre na timeline e rola até hoje', async () => {
    const scrolled: Element[] = []
    const original = Element.prototype.scrollIntoView
    Element.prototype.scrollIntoView = function (this: Element) {
      scrolled.push(this)
    }
    try {
      const today = todayIso()
      server.use(
        loggedIn(testUser),
        tripDetail({ ...testTrip, status: 'IN_PROGRESS' }),
        itinerary({
          days: [
            { ...days[1]!, date: '2000-01-01' },
            { ...days[1]!, date: today },
          ],
        }),
      )
      renderApp('/trips/42')

      expect(await screen.findByRole('tab', { name: 'Timeline', selected: true })).toBeVisible()
      const todaySection = await screen.findByRole('region', { name: /Hoje$/ })
      expect(scrolled).toEqual([todaySection])
      expect(screen.getByRole('button', { name: 'Ir para hoje' })).toBeInTheDocument()
    } finally {
      Element.prototype.scrollIntoView = original
    }
  })
})

const belcanto: SavedPlace = {
  id: 7,
  stopId: 1,
  placeId: 'ChIJBelcanto',
  name: 'Belcanto',
  kind: 'RESTAURANT',
  visited: false,
  version: 0,
}

function activity(id: number, title: string, extra: Partial<ItineraryActivity> = {}) {
  return {
    id,
    day: '2099-03-12',
    title,
    sortOrder: id,
    version: 0,
    ...extra,
  } satisfies ItineraryActivity
}

/** Lisboa, 12/03: castelo às 09:30–11:00, depois Belcanto e o elétrico sem horário. */
function planned(): ItineraryDay {
  const castle = activity(1, 'Castelo de São Jorge', { startTime: '09:30', endTime: '11:00' })
  const dinner = activity(2, 'Jantar', {
    savedPlace: { id: 7, stopId: 1, placeId: 'ChIJBelcanto', name: 'Belcanto', kind: 'RESTAURANT' },
    notes: 'Reserva para 4',
  })
  const tram = activity(3, 'Elétrico 28')
  return {
    date: '2099-03-12',
    stops: [lisbon],
    events: [
      { kind: 'ACTIVITY', time: '09:30', activity: castle },
      { kind: 'ACTIVITY', activity: dinner },
      { kind: 'ACTIVITY', activity: tram },
    ],
    overnight: { kind: 'LODGING', lodging: hotel },
    gap: false,
  }
}

describe('Atividades na timeline', () => {
  it('mostra horário, lugar salvo e notas; o editor adiciona uma atividade livre', async () => {
    let created: CreateActivity | undefined
    server.use(
      loggedIn(testUser),
      tripDetail(),
      itinerary({ days: [days[1]!, planned()] }),
      http.post('*/api/v1/trips/42/activities', async ({ request }) => {
        created = (await request.json()) as CreateActivity
        return HttpResponse.json({ id: 9, sortOrder: 0, version: 0, ...created }, { status: 201 })
      }),
    )
    const { user } = await openTimeline()

    const day12 = await screen.findByRole('region', { name: /^Qui, 12\/03/ })
    const items = within(day12).getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('09:30–11:00Castelo de São Jorge')
    expect(items[1]).toHaveTextContent('Jantar')
    expect(items[1]).toHaveTextContent('Belcanto · Reserva para 4')
    expect(items[2]).toHaveTextContent('Elétrico 28')

    await user.click(
      within(day12).getByRole('button', { name: 'Adicionar atividade em Qui, 12/03' }),
    )
    await user.click(await screen.findByRole('button', { name: 'Adicionar' }))
    expect(
      await screen.findByText('Diga o que vai fazer ou escolha um lugar salvo.'),
    ).toBeInTheDocument()
    expect(created).toBeUndefined()

    await user.type(screen.getByLabelText('O que vai fazer'), 'Passeio de catamarã')
    await user.type(screen.getByLabelText('Início'), '09:00')
    await user.type(screen.getByLabelText('Fim'), '08:00')
    await user.click(screen.getByRole('button', { name: 'Adicionar' }))
    expect(await screen.findByText('O fim deve ser depois do início.')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Fim'))
    await user.click(screen.getByRole('button', { name: 'Adicionar' }))
    await waitFor(() =>
      expect(created).toEqual({
        day: '2099-03-12',
        title: 'Passeio de catamarã',
        startTime: '09:00',
      }),
    )
  })

  it('planeja um restaurante salvo a partir da bandeja de lugares', async () => {
    let created: CreateActivity | undefined
    server.use(
      loggedIn(testUser),
      tripDetail(),
      itinerary({ days: [days[1]!, planned()] }),
      http.get('*/api/v1/trips/42/saved-places', () => HttpResponse.json([belcanto])),
      http.post('*/api/v1/trips/42/activities', async ({ request }) => {
        created = (await request.json()) as CreateActivity
        return HttpResponse.json(
          { id: 9, title: 'Belcanto', sortOrder: 3, version: 0, ...created },
          { status: 201 },
        )
      }),
    )
    const { user } = await openTimeline()

    await user.click(await screen.findByRole('button', { name: '1 lugar salvo' }))
    await user.click(screen.getByRole('button', { name: /^Planejar Belcanto/ }))

    // Já vem com o lugar e o primeiro dia em Lisboa; o título fica o nome do lugar.
    expect(await screen.findByLabelText('Lugar salvo')).toHaveValue('7')
    expect(screen.getByLabelText('Dia')).toHaveValue('2099-03-11')
    expect(screen.getByLabelText('O que vai fazer')).toHaveAttribute('placeholder', 'Belcanto')
    await user.selectOptions(screen.getByLabelText('Dia'), '2099-03-12')
    await user.type(screen.getByLabelText('Início'), '20:00')
    await user.click(screen.getByRole('button', { name: 'Adicionar' }))

    await waitFor(() =>
      expect(created).toEqual({ day: '2099-03-12', savedPlaceId: 7, startTime: '20:00' }),
    )
  })

  it('reordena pelo puxador com as setas, passando para o dia seguinte na ponta', async () => {
    const reorders: { day: string; activityIds: number[] }[] = []
    let release = () => {}
    const gate = new Promise<void>((resolve) => (release = resolve))
    server.use(
      loggedIn(testUser),
      tripDetail(),
      itinerary({ days: [planned(), { ...days[2]!, date: '2099-03-13', events: [] }] }),
      http.put('*/api/v1/trips/42/activities/reorder', async ({ request }) => {
        reorders.push((await request.json()) as { day: string; activityIds: number[] })
        await gate
        return HttpResponse.json([])
      }),
    )
    const { user } = await openTimeline()

    const tram = await screen.findByRole('button', { name: /^Mover Elétrico 28/ })
    tram.focus()
    await user.keyboard('{ArrowUp}')
    await waitFor(() => expect(reorders).toEqual([{ day: '2099-03-12', activityIds: [1, 3, 2] }]))
    // Otimista: o elétrico já aparece antes do jantar, antes de o servidor responder.
    const day12 = screen.getByRole('region', { name: /^Qui, 12\/03/ })
    expect(within(day12).getAllByRole('listitem')[1]).toHaveTextContent('Elétrico 28')
    release()

    // O jantar é o último do dia: ↓ leva para o começo do dia 13.
    screen.getByRole('button', { name: /^Mover Jantar/ }).focus()
    await user.keyboard('{ArrowDown}')
    await waitFor(() => expect(reorders[1]).toEqual({ day: '2099-03-13', activityIds: [2] }))
  })

  it('edita com a versão lida e avisa o conflito', async () => {
    let updated: UpdateActivity | undefined
    server.use(
      loggedIn(testUser),
      tripDetail(),
      itinerary({ days: [planned()] }),
      http.put('*/api/v1/trips/42/activities/3', async ({ request }) => {
        updated = (await request.json()) as UpdateActivity
        return HttpResponse.json({ status: 409 }, { status: 409 })
      }),
    )
    const { user } = await openTimeline()

    await user.click(await screen.findByRole('button', { name: 'Editar atividade Elétrico 28' }))
    const title = await screen.findByLabelText('O que vai fazer')
    await user.clear(title)
    await user.type(title, 'Elétrico 28 até Prazeres')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(updated).toEqual({ version: 0, day: '2099-03-12', title: 'Elétrico 28 até Prazeres' }),
    )
    expect(await screen.findByText(/alterada por outra pessoa/)).toBeInTheDocument()
  })

  it('leitor vê as atividades sem editar nem arrastar; as de fora das datas aparecem à parte', async () => {
    server.use(
      loggedIn(testUser),
      tripDetail({ ...testTrip, role: 'VIEWER' }),
      itinerary({
        days: [planned()],
        outsideActivities: [activity(5, 'Cascais', { day: '2099-03-20' })],
      }),
      http.get('*/api/v1/trips/42/saved-places', () => HttpResponse.json([belcanto])),
    )
    await openTimeline()

    expect(await screen.findByText('Castelo de São Jorge')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /Mover|Editar atividade|Adicionar atividade/ }),
    ).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /lugar salvo/ })).not.toBeInTheDocument()
    expect(screen.getByText('Fora das datas da viagem')).toBeInTheDocument()
    expect(screen.getByText('Cascais')).toBeInTheDocument()
  })
})
