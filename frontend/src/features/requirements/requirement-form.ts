import { z } from 'zod'
import {
  REQUIREMENT_TYPES,
  type Requirement,
  type RequirementBody,
  type RequirementType,
} from './api'

export const requirementSchema = z.object({
  countryCode: z.string().regex(/^[A-Z]{2}$/, 'countryRequired'),
  nationality: z.string().regex(/^[A-Z]{2}$/, 'countryRequired'),
  type: z.enum(REQUIREMENT_TYPES as [RequirementType, ...RequirementType[]]),
  title: z.string().trim().min(1, 'titleRequired').max(200, 'tooLong'),
  description: z.string().max(4000, 'tooLong'),
  /** Dias antes da chegada, como texto (`''` = sem prazo). */
  leadDays: z.string().regex(/^(\d{1,3})?$/, 'leadDaysInvalid'),
  officialUrl: z
    .string()
    .trim()
    .regex(/^https?:\/\/\S+$/, 'urlInvalid')
    .max(500, 'tooLong'),
})

export type RequirementFormValues = z.infer<typeof requirementSchema>

export function emptyRequirement(countryCode = ''): RequirementFormValues {
  return {
    countryCode,
    nationality: 'BR',
    type: 'VISA',
    title: '',
    description: '',
    leadDays: '',
    officialUrl: '',
  }
}

export function toFormValues(r: Requirement): RequirementFormValues {
  return {
    countryCode: r.countryCode,
    nationality: r.nationality,
    type: r.type,
    title: r.title,
    description: r.description ?? '',
    leadDays: r.leadDays != null ? String(r.leadDays) : '',
    officialUrl: r.officialUrl,
  }
}

export function toBody(values: RequirementFormValues): RequirementBody {
  return {
    countryCode: values.countryCode,
    nationality: values.nationality,
    type: values.type,
    title: values.title.trim(),
    description: values.description.trim() || undefined,
    leadDays: values.leadDays === '' ? undefined : Number(values.leadDays),
    officialUrl: values.officialUrl.trim(),
  }
}
