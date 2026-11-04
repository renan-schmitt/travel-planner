import { z } from 'zod'
import type { CreateActivity, ItineraryActivity } from './api'

const TIME = /^([01]\d|2[0-3]):[0-5]\d$/

/**
 * Formulário da atividade, com as mesmas regras do backend: dia entre os da viagem, título
 * obrigatório sem favorito (com favorito, vazio = o nome dele), fim só com início e depois dele.
 * `savedPlaceId` vazio = sem favorito.
 */
export function activitySchema(days: string[]) {
  return z
    .object({
      day: z.string().refine((d) => days.includes(d), 'dayOutside'),
      title: z.string().max(200, 'tooLong'),
      startTime: z.string().refine((v) => v === '' || TIME.test(v), 'invalidTime'),
      endTime: z.string().refine((v) => v === '' || TIME.test(v), 'invalidTime'),
      savedPlaceId: z.string(),
      notes: z.string().max(2000, 'tooLong'),
    })
    .superRefine((v, ctx) => {
      if (v.title.trim() === '' && v.savedPlaceId === '') {
        ctx.addIssue({ code: 'custom', path: ['title'], message: 'titleRequired' })
      }
      if (v.endTime && !v.startTime) {
        ctx.addIssue({ code: 'custom', path: ['startTime'], message: 'startRequired' })
      } else if (v.endTime && v.endTime <= v.startTime) {
        ctx.addIssue({ code: 'custom', path: ['endTime'], message: 'endBeforeStart' })
      }
    })
}

export type ActivityFormValues = z.infer<ReturnType<typeof activitySchema>>

export function emptyActivity(day: string, savedPlaceId?: number): ActivityFormValues {
  return {
    day,
    title: '',
    startTime: '',
    endTime: '',
    savedPlaceId: savedPlaceId == null ? '' : String(savedPlaceId),
    notes: '',
  }
}

export function toFormValues(activity: ItineraryActivity): ActivityFormValues {
  return {
    day: activity.day,
    title: activity.title,
    startTime: activity.startTime ?? '',
    endTime: activity.endTime ?? '',
    savedPlaceId: activity.savedPlace ? String(activity.savedPlace.id) : '',
    notes: activity.notes ?? '',
  }
}

const blankToUndefined = (value: string) => (value.trim() === '' ? undefined : value.trim())

export function toActivityBody(values: ActivityFormValues): CreateActivity {
  return {
    day: values.day,
    title: blankToUndefined(values.title),
    startTime: blankToUndefined(values.startTime),
    endTime: blankToUndefined(values.endTime),
    savedPlaceId: values.savedPlaceId === '' ? undefined : Number(values.savedPlaceId),
    notes: blankToUndefined(values.notes),
  }
}
