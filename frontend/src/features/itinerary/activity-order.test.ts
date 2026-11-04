import { dayActivityIds, moveActivity } from './activity-order'
import type { Itinerary, ItineraryDay, ItineraryEvent } from './api'

function act(id: number, day: string, sortOrder: number, time?: string): ItineraryEvent {
  return {
    kind: 'ACTIVITY',
    time,
    activity: { id, day, title: `A${id}`, startTime: time, sortOrder, version: 0 },
  }
}

const train: ItineraryEvent = {
  kind: 'SEGMENT',
  time: '10:00',
  segment: {
    id: 50,
    type: 'TRAIN',
    local: false,
    departAt: '2099-03-12T10:00:00',
    departInstant: '2099-03-12T10:00:00Z',
  },
}

function day(date: string, events: ItineraryEvent[]): ItineraryDay {
  return { date, stops: [], events, overnight: { kind: 'HOME' }, gap: false }
}

/** Dia 12: 09:00 (1), trem 10:00, sem horário 2 e 3. Dia 13: sem horário 4. */
const itinerary: Itinerary = {
  days: [
    day('2099-03-12', [
      act(1, '2099-03-12', 0, '09:00'),
      train,
      act(2, '2099-03-12', 1),
      act(3, '2099-03-12', 2),
    ]),
    day('2099-03-13', [act(4, '2099-03-13', 0)]),
  ],
  outsideActivities: [],
}

describe('moveActivity', () => {
  it('reordena dentro do dia, mantendo as com horário no horário', () => {
    const result = moveActivity(itinerary, 3, '2099-03-12', 1)!

    expect(result.activityIds).toEqual([1, 3, 2])
    const events = result.optimistic.days[0]!.events
    expect(events.map((e) => e.activity?.id ?? 'trem')).toEqual([1, 'trem', 3, 2])
    expect(events[2]!.activity!.sortOrder).toBe(1)
  })

  it('move para outro dia na posição pedida e tira do dia de origem', () => {
    const result = moveActivity(itinerary, 2, '2099-03-13', 0)!

    expect(result).toMatchObject({ day: '2099-03-13', activityIds: [2, 4] })
    expect(dayActivityIds(result.optimistic.days[0]!)).toEqual([1, 3])
    expect(dayActivityIds(result.optimistic.days[1]!)).toEqual([2, 4])
    expect(result.optimistic.days[1]!.events[0]!.activity!.day).toBe('2099-03-13')
  })

  it('com horário, continua no horário mesmo se soltar no fim', () => {
    const result = moveActivity(itinerary, 1, '2099-03-13', Number.MAX_SAFE_INTEGER)!

    expect(result.activityIds).toEqual([4, 1])
    expect(dayActivityIds(result.optimistic.days[1]!)).toEqual([1, 4])
  })

  it('não faz nada se a posição não muda ou se a atividade/dia não existem', () => {
    expect(moveActivity(itinerary, 2, '2099-03-12', 1)).toBeNull()
    expect(moveActivity(itinerary, 99, '2099-03-12', 0)).toBeNull()
    expect(moveActivity(itinerary, 2, '2099-04-01', 0)).toBeNull()
  })
})
