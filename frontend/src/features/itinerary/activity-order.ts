import type { Itinerary, ItineraryDay, ItineraryEvent } from './api'

/** Ids das atividades do dia na ordem em que aparecem na timeline. */
export function dayActivityIds(day: ItineraryDay): number[] {
  return day.events.flatMap((e) => (e.activity ? [e.activity.id] : []))
}

/**
 * Ordem dos eventos do dia, como no backend: check-outs, trechos e atividades com horário pelo
 * horário, check-ins e, por último, as atividades sem horário pela posição (`sortOrder`).
 */
function group(event: ItineraryEvent): number {
  switch (event.kind) {
    case 'CHECK_OUT':
      return 0
    case 'CHECK_IN':
      return 2
    case 'ACTIVITY':
      return event.time ? 1 : 3
    default:
      return 1
  }
}

function compareEvents(a: ItineraryEvent, b: ItineraryEvent): number {
  return (
    group(a) - group(b) ||
    (a.time ?? '').localeCompare(b.time ?? '') ||
    (a.activity?.sortOrder ?? 0) - (b.activity?.sortOrder ?? 0)
  )
}

export type ActivityMove = {
  day: string
  /** Todas as atividades do dia de destino, na ordem nova (corpo do `PUT .../reorder`). */
  activityIds: number[]
  /** A timeline já com a atividade no lugar novo (atualização otimista). */
  optimistic: Itinerary
}

/**
 * Move a atividade `activityId` para o dia `targetDay`, na posição `targetIndex` entre as
 * atividades desse dia (sem contar a própria). `null` se nada muda ou se a atividade/dia não
 * existem. Atividades com horário continuam ordenadas pelo horário; a posição vale para as demais.
 */
export function moveActivity(
  itinerary: Itinerary,
  activityId: number,
  targetDay: string,
  targetIndex: number,
): ActivityMove | null {
  const source = itinerary.days.find((d) => d.events.some((e) => e.activity?.id === activityId))
  const target = itinerary.days.find((d) => d.date === targetDay)
  if (!source || !target) return null
  const event = source.events.find((e) => e.activity?.id === activityId)!
  const ids = dayActivityIds(target).filter((id) => id !== activityId)
  const index = Math.max(0, Math.min(targetIndex, ids.length))
  ids.splice(index, 0, activityId)
  if (source === target && ids.join() === dayActivityIds(source).join()) return null

  const order = new Map(ids.map((id, i) => [id, i]))
  const moved: ItineraryEvent = {
    ...event,
    activity: { ...event.activity!, day: targetDay },
  }
  const days = itinerary.days.map((day) => {
    if (day !== source && day !== target) return day
    let events = day.events.filter((e) => e.activity?.id !== activityId)
    if (day === target) {
      events = [...events, moved]
        .map((e) =>
          e.activity
            ? { ...e, activity: { ...e.activity, sortOrder: order.get(e.activity.id)! } }
            : e,
        )
        .sort(compareEvents)
    }
    return { ...day, events }
  })
  return { day: targetDay, activityIds: ids, optimistic: { ...itinerary, days } }
}
