import { z } from 'zod'
import type { CreateTrip } from './api'

export const tripSchema = z
  .object({
    name: z.string().trim().min(1, 'nameRequired').max(120, 'nameTooLong'),
    startDate: z.string(),
    endDate: z.string(),
    status: z.enum(['PLANNING', 'CONFIRMED']),
    coverColor: z.string(),
  })
  .refine((v) => !v.startDate || !v.endDate || v.endDate >= v.startDate, {
    path: ['endDate'],
    message: 'endBeforeStart',
  })

export type TripFormValues = z.infer<typeof tripSchema>

/** Campos do formulário convertidos para a API (datas vazias são omitidas). */
export function toTripBody(values: TripFormValues): CreateTrip {
  return {
    name: values.name,
    startDate: values.startDate || undefined,
    endDate: values.endDate || undefined,
    status: values.status,
    coverColor: values.coverColor,
  }
}
