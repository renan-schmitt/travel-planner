import { TZDate } from '@date-fns/tz'
import { z } from 'zod'
import type { CreateSegment, Segment, Stop, TransportType } from './api'

export const TRANSPORT_TYPES: TransportType[] = [
  'FLIGHT',
  'TRAIN',
  'BUS',
  'CAR',
  'CAR_RENTAL',
  'FERRY',
  'TRANSFER',
  'TAXI_RIDE',
  'OTHER',
]

/**
 * Ponta de um trecho no formulário: `home` (cidade de origem do usuário), `stop:<id>` ou, só na
 * origem, `local:<id>` (trecho local dentro da parada: transfer, aluguel de carro).
 */
export type Endpoint = string

export const HOME: Endpoint = 'home'
export const stopEndpoint = (id: number): Endpoint => `stop:${id}`
export const localEndpoint = (id: number): Endpoint => `local:${id}`

export function parseEndpoint(value: Endpoint): { kind: 'home' | 'stop' | 'local'; id?: number } {
  if (value === HOME) return { kind: 'home' }
  const [kind, id] = value.split(':')
  return { kind: kind === 'local' ? 'local' : 'stop', id: Number(id) }
}

/** Instante (ms) de uma data e hora no relógio local do fuso. */
export function toInstant(date: string, time: string, timeZone: string): number {
  const [year = 0, month = 1, day = 1] = date.split('-').map(Number)
  const [hours = 0, minutes = 0] = time.split(':').map(Number)
  return new TZDate(year, month - 1, day, hours, minutes, timeZone).getTime()
}

export const segmentSchema = z
  .object({
    type: z.enum(TRANSPORT_TYPES as [TransportType, ...TransportType[]]),
    origin: z.string().min(1),
    destination: z.string(),
    originLabel: z.string().max(160, 'labelTooLong'),
    destinationLabel: z.string().max(160, 'labelTooLong'),
    departDate: z.string().min(1, 'departDateRequired'),
    departTime: z.string().min(1, 'departTimeRequired'),
    departTz: z.string().min(1, 'timezoneRequired'),
    arriveDate: z.string(),
    arriveTime: z.string(),
    arriveTz: z.string(),
    carrier: z.string().max(120, 'tooLong'),
    code: z.string().max(40, 'tooLong'),
    bookingRef: z.string().max(80, 'tooLong'),
    notes: z.string().max(2000, 'tooLong'),
  })
  .superRefine((v, ctx) => {
    const local = parseEndpoint(v.origin).kind === 'local'
    if (!local && v.origin === v.destination) {
      ctx.addIssue({ code: 'custom', path: ['destination'], message: 'sameEnds' })
    }
    const hasArrival = v.arriveDate !== '' || v.arriveTime !== ''
    if (!hasArrival) return
    if (!v.arriveDate || !v.arriveTime) {
      ctx.addIssue({ code: 'custom', path: ['arriveDate'], message: 'arrivalIncomplete' })
      return
    }
    if (!v.arriveTz) {
      ctx.addIssue({ code: 'custom', path: ['arriveTz'], message: 'timezoneRequired' })
      return
    }
    if (
      v.departDate &&
      v.departTime &&
      v.departTz &&
      toInstant(v.arriveDate, v.arriveTime, v.arriveTz) <=
        toInstant(v.departDate, v.departTime, v.departTz)
    ) {
      ctx.addIssue({ code: 'custom', path: ['arriveDate'], message: 'arrivalBeforeDeparture' })
    }
  })

export type SegmentFormValues = z.infer<typeof segmentSchema>

/** Fuso sugerido para uma ponta: o da parada, ou o da cidade de origem do usuário. */
export function timeZoneOf(endpoint: Endpoint, stops: Stop[], homeTz: string): string {
  const { kind, id } = parseEndpoint(endpoint)
  if (kind === 'home') return homeTz
  return stops.find((s) => s.id === id)?.timezone ?? homeTz
}

/**
 * Valores iniciais de um novo trecho. Sem ida, sugere casa → primeira parada; com ida e sem volta,
 * última parada → casa; senão, a primeira parada sem trecho de saída → a seguinte.
 */
export function emptySegment(
  stops: Stop[],
  segments: Segment[],
  homeTz: string,
): SegmentFormValues {
  const first = stops[0]
  const last = stops.at(-1)
  let origin: Endpoint = HOME
  let destination: Endpoint = first ? stopEndpoint(first.id) : HOME
  let date = first?.arrivalDate ?? ''
  const leavesHome = segments.some((s) => !s.local && s.originStopId == null)
  const returnsHome = segments.some((s) => !s.local && s.destinationStopId == null)
  if (leavesHome && last) {
    const pending = stops.find(
      (stop, i) => i < stops.length - 1 && !segments.some((s) => s.originStopId === stop.id),
    )
    const from = !returnsHome || !pending ? last : pending
    origin = stopEndpoint(from.id)
    const next = stops[stops.indexOf(from) + 1]
    destination = from === last || !next ? HOME : stopEndpoint(next.id)
    date = from.departureDate
  }
  return {
    type: 'FLIGHT',
    origin,
    destination,
    originLabel: '',
    destinationLabel: '',
    departDate: date,
    departTime: '',
    departTz: timeZoneOf(origin, stops, homeTz),
    arriveDate: '',
    arriveTime: '',
    arriveTz: timeZoneOf(destination, stops, homeTz),
    carrier: '',
    code: '',
    bookingRef: '',
    notes: '',
  }
}

function splitLocal(local?: string): [string, string] {
  if (!local) return ['', '']
  const [date = '', time = ''] = local.split('T')
  return [date, time.slice(0, 5)]
}

export function toFormValues(segment: Segment): SegmentFormValues {
  const [departDate, departTime] = splitLocal(segment.departAt)
  const [arriveDate, arriveTime] = splitLocal(segment.arriveAt)
  return {
    type: segment.type,
    origin: segment.local
      ? localEndpoint(segment.localStopId!)
      : segment.originStopId == null
        ? HOME
        : stopEndpoint(segment.originStopId),
    destination: segment.destinationStopId == null ? HOME : stopEndpoint(segment.destinationStopId),
    originLabel: segment.originLabel ?? '',
    destinationLabel: segment.destinationLabel ?? '',
    departDate,
    departTime,
    departTz: segment.departTz,
    arriveDate,
    arriveTime,
    arriveTz: segment.arriveTz ?? segment.departTz,
    carrier: segment.carrier ?? '',
    code: segment.code ?? '',
    bookingRef: segment.bookingRef ?? '',
    notes: segment.notes ?? '',
  }
}

const optional = (value: string) => value.trim() || undefined

export function toSegmentBody(values: SegmentFormValues): CreateSegment {
  const origin = parseEndpoint(values.origin)
  const destination = parseEndpoint(values.destination)
  const local = origin.kind === 'local'
  const hasArrival = values.arriveDate !== '' && values.arriveTime !== ''
  return {
    type: values.type,
    originStopId: origin.kind === 'stop' ? origin.id : undefined,
    destinationStopId: !local && destination.kind === 'stop' ? destination.id : undefined,
    localStopId: local ? origin.id : undefined,
    originLabel: optional(values.originLabel),
    destinationLabel: optional(values.destinationLabel),
    departAt: `${values.departDate}T${values.departTime}`,
    departTz: values.departTz,
    arriveAt: hasArrival ? `${values.arriveDate}T${values.arriveTime}` : undefined,
    arriveTz: hasArrival ? values.arriveTz : undefined,
    carrier: optional(values.carrier),
    code: optional(values.code),
    bookingRef: optional(values.bookingRef),
    notes: optional(values.notes),
  }
}

/** Fuso do navegador, usado quando o usuário ainda não informou a cidade de origem. */
export function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}
