import { z } from 'zod'
import type { CreateTraveler, Traveler } from './api'

const currentYear = new Date().getFullYear()

export const travelerSchema = z.object({
  name: z.string().trim().min(1, 'nameRequired').max(120, 'nameTooLong'),
  ageGroup: z.enum(['BABY', 'CHILD', 'TEEN', 'ADULT']),
  birthYear: z
    .string()
    .trim()
    .refine(
      (v) => !v || (/^\d{4}$/.test(v) && +v >= 1900 && +v <= currentYear),
      'birthYearInvalid',
    ),
  nationalities: z.array(z.string().length(2)).min(1, 'nationalityRequired').max(10),
  passportExpiry: z.string(),
})

export type TravelerFormValues = z.infer<typeof travelerSchema>

export function emptyTraveler(nationality = 'BR'): TravelerFormValues {
  return {
    name: '',
    ageGroup: 'ADULT',
    birthYear: '',
    nationalities: [nationality],
    passportExpiry: '',
  }
}

export function toFormValues(traveler: Traveler): TravelerFormValues {
  return {
    name: traveler.name,
    ageGroup: traveler.ageGroup,
    birthYear: traveler.birthYear?.toString() ?? '',
    nationalities: traveler.nationalities,
    passportExpiry: traveler.passportExpiry ?? '',
  }
}

/** Campos vazios são omitidos. */
export function toTravelerBody(values: TravelerFormValues): CreateTraveler {
  return {
    name: values.name,
    ageGroup: values.ageGroup,
    birthYear: values.birthYear ? Number(values.birthYear) : undefined,
    nationalities: values.nationalities,
    passportExpiry: values.passportExpiry || undefined,
  }
}
