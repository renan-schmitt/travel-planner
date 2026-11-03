import type { LodgingFormValues } from '@/features/itinerary/lodging-form'
import type { Stop } from '@/features/itinerary/api'
import {
  HOME,
  localEndpoint,
  stopEndpoint,
  timeZoneOf,
  type Endpoint,
  type SegmentFormValues,
} from '@/features/itinerary/segment-form'
import type { Suggestion } from './api'
import { paramNumber, paramString } from './suggestion-text'

/** Link externo útil para resolver a lacuna (ESP §6.4). */
export type ExternalLink = { key: 'uber' | 'bookingTaxi'; href: string }

const TRANSFER_LINKS: ExternalLink[] = [
  { key: 'uber', href: 'https://m.uber.com/' },
  { key: 'bookingTaxi', href: 'https://www.booking.com/taxi/' },
]

/**
 * Como "Resolver" uma sugestão:
 * - `segment`: abre o formulário de trecho já preenchido;
 * - `lodging`: abre o formulário de hospedagem da parada;
 * - `travelers`: leva aos viajantes (validade do passaporte);
 * - `checklist`: leva à aba Checklist (o item já foi criado);
 * - `accept`: aceitar já resolve (cria o item de checklist sugerido);
 * - `none`: não há atalho (só adiar/dispensar).
 */
export type Resolution =
  | { kind: 'segment'; preset: Partial<SegmentFormValues> }
  | { kind: 'lodging'; stop: Stop; preset: Partial<LodgingFormValues> }
  | { kind: 'travelers' }
  | { kind: 'checklist' }
  | { kind: 'accept' }
  | { kind: 'none' }

function split(local?: string): { date: string; time: string } {
  const [date = '', time = ''] = (local ?? '').split('T')
  return { date, time: time.slice(0, 5) }
}

function endpoint(stopId: number | undefined): Endpoint {
  return stopId === undefined ? HOME : stopEndpoint(stopId)
}

export function resolutionOf(suggestion: Suggestion, stops: Stop[], homeTz: string): Resolution {
  const stopId = paramNumber(suggestion, 'stopId')
  const stop = stops.find((s) => s.id === stopId)
  switch (suggestion.ruleCode) {
    case 'TRANSFER_CHEGADA': {
      if (!stop) return { kind: 'none' }
      const arrival = split(
        paramString(suggestion, 'arriveAt') ?? paramString(suggestion, 'departAt'),
      )
      return {
        kind: 'segment',
        preset: {
          type: 'TRANSFER',
          origin: localEndpoint(stop.id),
          departDate: arrival.date,
          departTime: arrival.time,
          departTz: paramString(suggestion, 'arriveTz') ?? stop.timezone,
          arriveTz: stop.timezone,
        },
      }
    }
    case 'TRANSFER_SAIDA': {
      if (!stop) return { kind: 'none' }
      const departure = split(paramString(suggestion, 'departAt'))
      return {
        kind: 'segment',
        preset: {
          type: 'TRANSFER',
          origin: localEndpoint(stop.id),
          departDate: departure.date,
          departTz: paramString(suggestion, 'departTz') ?? stop.timezone,
          arriveTz: stop.timezone,
        },
      }
    }
    case 'NOITE_SEM_HOSPEDAGEM': {
      const date = paramString(suggestion, 'date')
      if (!stop || !date) return { kind: 'none' }
      return { kind: 'lodging', stop, preset: { checkIn: date } }
    }
    case 'DESTINO_SEM_TRANSPORTE': {
      const fromId = paramNumber(suggestion, 'fromStopId')
      const toId = paramNumber(suggestion, 'toStopId')
      const from = stops.find((s) => s.id === fromId)
      const to = stops.find((s) => s.id === toId)
      const origin = endpoint(fromId)
      const destination = endpoint(toId)
      return {
        kind: 'segment',
        preset: {
          type: from && to ? 'TRAIN' : 'FLIGHT',
          origin,
          destination,
          departDate: from?.departureDate ?? to?.arrivalDate ?? '',
          departTz: timeZoneOf(origin, stops, homeTz),
          arriveTz: timeZoneOf(destination, stops, homeTz),
        },
      }
    }
    case 'TRANSPORTE_LOCAL':
      if (!stop) return { kind: 'none' }
      return {
        kind: 'segment',
        preset: {
          type: 'CAR_RENTAL',
          origin: localEndpoint(stop.id),
          departDate: stop.arrivalDate,
          departTz: stop.timezone,
          arriveTz: stop.timezone,
        },
      }
    case 'PASSAPORTE_VALIDADE':
      return { kind: 'travelers' }
    case 'CHECKIN_ONLINE':
      return { kind: 'checklist' }
    default:
      return suggestion.checklist && !suggestion.checklist.automatic
        ? { kind: 'accept' }
        : { kind: 'none' }
  }
}

export function externalLinksOf(suggestion: Suggestion): ExternalLink[] {
  return suggestion.ruleCode === 'TRANSFER_CHEGADA' || suggestion.ruleCode === 'TRANSFER_SAIDA'
    ? TRANSFER_LINKS
    : []
}

/** Chave de foco usada na URL (`?focus=`) para destacar uma sugestão. */
export function focusKey(ruleCode: string, logicalKey: string) {
  return `${ruleCode}:${logicalKey}`
}
