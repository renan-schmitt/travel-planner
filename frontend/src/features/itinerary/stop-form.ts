import { differenceInCalendarDays } from 'date-fns'
import { z } from 'zod'
import { emptyCity, requiredCitySchema } from '@/features/places/city'
import { parseDate } from '@/lib/dates'
import type { CreateStop, Lodging, Stop } from './api'

export const stopSchema = z
  .object({
    city: requiredCitySchema,
    arrivalDate: z.string().min(1, 'arrivalRequired'),
    departureDate: z.string().min(1, 'departureRequired'),
    notes: z.string().max(2000, 'notesTooLong'),
  })
  .refine((v) => !v.arrivalDate || !v.departureDate || v.departureDate >= v.arrivalDate, {
    path: ['departureDate'],
    message: 'departureBeforeArrival',
  })

export type StopFormValues = z.infer<typeof stopSchema>

/** Nova parada começa no dia em que a última termina (ou no início da viagem). */
export function emptyStop(stops: Stop[], tripStart?: string | null): StopFormValues {
  const lastDeparture = stops
    .map((s) => s.departureDate)
    .sort()
    .at(-1)
  return {
    city: emptyCity,
    arrivalDate: lastDeparture ?? tripStart ?? '',
    departureDate: '',
    notes: '',
  }
}

export function toFormValues(stop: Stop): StopFormValues {
  return {
    city: {
      name: stop.cityName,
      countryCode: stop.countryCode,
      timezone: stop.timezone,
      placeId: stop.cityPlaceId ?? undefined,
      lat: stop.lat ?? undefined,
      lng: stop.lng ?? undefined,
    },
    arrivalDate: stop.arrivalDate,
    departureDate: stop.departureDate,
    notes: stop.notes ?? '',
  }
}

export function toStopBody(values: StopFormValues): CreateStop {
  const { city } = values
  return {
    cityPlaceId: city.placeId,
    cityName: city.name.trim(),
    countryCode: city.countryCode,
    lat: city.lat,
    lng: city.lng,
    timezone: city.timezone,
    arrivalDate: values.arrivalDate,
    departureDate: values.departureDate,
    notes: values.notes.trim() || undefined,
  }
}

/** Noites entre chegada e saída (0 = bate-volta). */
export function nightsBetween(arrival: string, departure: string): number {
  return differenceInCalendarDays(parseDate(departure), parseDate(arrival))
}

/**
 * Parada que divide mais de um dia com o período (o dia da troca de cidade pode ser compartilhado),
 * como no backend.
 */
export function findOverlap(arrival: string, departure: string, others: Stop[]): Stop | undefined {
  if (!arrival || !departure || departure < arrival) return undefined
  return others.find((o) => {
    const latestArrival = arrival > o.arrivalDate ? arrival : o.arrivalDate
    const earliestDeparture = departure < o.departureDate ? departure : o.departureDate
    return latestArrival < earliestDeparture
  })
}

/** Hospedagem que ficaria fora das datas da parada (check-in antes da chegada ou check-out depois da saída). */
export function findLodgingOutside(
  arrival: string,
  departure: string,
  lodgings: Lodging[],
): Lodging | undefined {
  if (!arrival || !departure || departure < arrival) return undefined
  return lodgings.find((l) => l.checkIn < arrival || l.checkOut > departure)
}
