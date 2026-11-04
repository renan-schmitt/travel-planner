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
import { placesPath } from '@/features/places/places-url'
import { adminRequirementsPath } from '@/features/requirements/admin-path'
import type { Suggestion } from './api'
import { paramNumber, paramString } from './suggestion-text'

/** Link externo útil para resolver a lacuna (ESP §6.4). */
export type ExternalLink = {
  key: 'uber' | 'bookingTaxi' | 'resortPage' | 'officialSite'
  href: string
}

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
    case 'DOCUMENTO_DESTINO':
      return { kind: 'checklist' }
    default:
      return suggestion.checklist && !suggestion.checklist.automatic
        ? { kind: 'accept' }
        : { kind: 'none' }
  }
}

export function externalLinksOf(suggestion: Suggestion): ExternalLink[] {
  if (suggestion.ruleCode === 'TRANSFER_CHEGADA' || suggestion.ruleCode === 'TRANSFER_SAIDA') {
    return TRANSFER_LINKS
  }
  if (suggestion.ruleCode === 'DOCUMENTO_DESTINO') {
    const href = paramString(suggestion, 'officialUrl')
    return href && /^https?:\/\//.test(href) ? [{ key: 'officialSite', href }] : []
  }
  if (suggestion.ruleCode === 'RESORT_OPCOES') {
    // A página do lugar no Maps mostra o site e as reservas do resort.
    const placeId = paramString(suggestion, 'placeId')
    const name = paramString(suggestion, 'name') ?? ''
    const query = [name, paramString(suggestion, 'city')].filter(Boolean).join(' ')
    const params = new URLSearchParams({ api: '1', query })
    if (placeId) params.set('query_place_id', placeId)
    return [{ key: 'resortPage', href: `https://www.google.com/maps/search/?${params.toString()}` }]
  }
  return []
}

/** Ação extra ao lado de "Resolver": uma tela de busca do sistema. */
export type SearchLink = {
  to: string
  label: 'searchHotels' | 'discoverRestaurants' | 'discoverAttractions' | 'reviewRequirements'
}

/**
 * Busca de hotéis da parada da noite sem hospedagem; "Descobrir" restaurantes/atrações da parada
 * sem favoritos; para administradores, a base de requisitos do país sem dados ou desatualizado.
 */
export function searchLinkOf(
  suggestion: Suggestion,
  tripId: number,
  isAdmin = false,
): SearchLink | undefined {
  if (
    isAdmin &&
    (suggestion.ruleCode === 'DESTINO_SEM_REQUISITOS' ||
      suggestion.ruleCode === 'REQUISITO_DESATUALIZADO')
  ) {
    const country = paramString(suggestion, 'countryCode')
    return { to: adminRequirementsPath(country), label: 'reviewRequirements' }
  }
  const stopId = paramNumber(suggestion, 'stopId')
  if (stopId === undefined) return undefined
  switch (suggestion.ruleCode) {
    case 'NOITE_SEM_HOSPEDAGEM':
      return { to: `/trips/${tripId}/stops/${stopId}/hotels`, label: 'searchHotels' }
    case 'RESTAURANTES':
      return { to: placesPath(tripId, stopId, 'RESTAURANT', true), label: 'discoverRestaurants' }
    case 'ATRACOES':
      return { to: placesPath(tripId, stopId, 'ATTRACTION', true), label: 'discoverAttractions' }
    default:
      return undefined
  }
}

/** Chave de foco usada na URL (`?focus=`) para destacar uma sugestão. */
export function focusKey(ruleCode: string, logicalKey: string) {
  return `${ruleCode}:${logicalKey}`
}
