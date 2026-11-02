import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { Itinerary, ItineraryDay, ItinerarySegment } from '@/features/itinerary/api'
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

function itinerary(body: Itinerary) {
  return http.get('*/api/v1/trips/42/itinerary', () => HttpResponse.json(body))
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
