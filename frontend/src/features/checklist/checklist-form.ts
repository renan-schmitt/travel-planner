import { z } from 'zod'
import type { ChecklistCategory, ChecklistItem, CreateChecklistItem } from './api'
import { CATEGORIES } from './categories'

export const checklistItemSchema = z.object({
  category: z.enum(CATEGORIES as [ChecklistCategory, ...ChecklistCategory[]]),
  title: z.string().trim().min(1, 'titleRequired').max(200, 'tooLong'),
  dueDate: z.string(),
  /** Id do usuário responsável, como texto (`''` = ninguém). */
  assignee: z.string(),
  notes: z.string().max(2000, 'tooLong'),
})

export type ChecklistItemFormValues = z.infer<typeof checklistItemSchema>

export function toFormValues(item: ChecklistItem): ChecklistItemFormValues {
  return {
    category: item.category,
    title: item.title,
    dueDate: item.dueDate ?? '',
    assignee: item.assigneeUserId != null ? String(item.assigneeUserId) : '',
    notes: item.notes ?? '',
  }
}

export function toItemBody(values: ChecklistItemFormValues): CreateChecklistItem {
  return {
    category: values.category,
    title: values.title.trim(),
    dueDate: values.dueDate || undefined,
    assigneeUserId: values.assignee ? Number(values.assignee) : undefined,
    notes: values.notes.trim() || undefined,
  }
}
