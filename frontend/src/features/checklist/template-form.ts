import { z } from 'zod'
import type { ChecklistCategory } from './api'
import { CATEGORIES } from './categories'
import { TEMPLATE_ICONS } from './template-icons'
import type { ChecklistTemplate, TemplateBody, TemplateIcon } from './templates-api'

/** Maior distância (em dias) entre o prazo e a partida aceita pela API. */
export const MAX_OFFSET_DAYS = 365

const daysBefore = z
  .string()
  .trim()
  .refine(
    (v) => v === '' || (/^-?\d+$/.test(v) && Math.abs(Number(v)) <= MAX_OFFSET_DAYS),
    'daysInvalid',
  )

export const templateItemSchema = z.object({
  category: z.enum(CATEGORIES as [ChecklistCategory, ...ChecklistCategory[]]),
  title: z.string().trim().min(1, 'titleRequired').max(200, 'tooLong'),
  /** Dias antes da partida, como texto (`''` = sem prazo; negativo = depois da partida). */
  daysBefore,
})

export const templateSchema = z.object({
  name: z.string().trim().min(1, 'nameRequired').max(120, 'tooLong'),
  description: z.string().max(500, 'tooLong'),
  icon: z.enum(TEMPLATE_ICONS as [TemplateIcon, ...TemplateIcon[]]),
  items: z.array(templateItemSchema).max(100, 'tooManyItems'),
})

export type TemplateFormValues = z.infer<typeof templateSchema>
export type TemplateItemFormValues = z.infer<typeof templateItemSchema>

export const emptyTemplateItem: TemplateItemFormValues = {
  category: 'OTHER',
  title: '',
  daysBefore: '',
}

export const emptyTemplate: TemplateFormValues = {
  name: '',
  description: '',
  icon: 'LIST',
  items: [emptyTemplateItem],
}

export function toFormValues(template: ChecklistTemplate): TemplateFormValues {
  return {
    name: template.name,
    description: template.description ?? '',
    icon: template.icon,
    items: template.items.map((i) => ({
      category: i.category,
      title: i.title,
      daysBefore: i.dueOffsetDays == null ? '' : String(-i.dueOffsetDays),
    })),
  }
}

export function toTemplateBody(values: TemplateFormValues): TemplateBody {
  return {
    name: values.name.trim(),
    description: values.description.trim() || undefined,
    icon: values.icon,
    items: values.items.map((i) => ({
      category: i.category,
      title: i.title.trim(),
      // `0 - n` em vez de `-n`: evita mandar -0.
      dueOffsetDays: i.daysBefore.trim() === '' ? undefined : 0 - Number(i.daysBefore),
    })),
  }
}
