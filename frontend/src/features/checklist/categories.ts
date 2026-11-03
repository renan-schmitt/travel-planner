import {
  FileText,
  HeartPulse,
  House,
  ListTodo,
  Luggage,
  Ticket,
  Wallet,
  type LucideIcon,
} from 'lucide-react'
import { daysUntil } from '@/lib/dates'
import type { ChecklistCategory, ChecklistItem } from './api'

/** Ordem dos grupos na aba. */
export const CATEGORIES: ChecklistCategory[] = [
  'DOCUMENTS',
  'BOOKINGS',
  'PACKING',
  'HOME',
  'HEALTH',
  'MONEY',
  'OTHER',
]

export const categoryIcons: Record<ChecklistCategory, LucideIcon> = {
  DOCUMENTS: FileText,
  BOOKINGS: Ticket,
  PACKING: Luggage,
  HOME: House,
  HEALTH: HeartPulse,
  MONEY: Wallet,
  OTHER: ListTodo,
}

/** Dias antes do prazo em que o item aberto passa a ser "em breve". */
export const SOON_DAYS = 3

export type DueState = 'overdue' | 'soon' | 'later'

/** Situação do prazo de um item aberto (`undefined` se concluído ou sem data limite). */
export function dueState(item: ChecklistItem, today: Date = new Date()): DueState | undefined {
  if (item.status === 'DONE' || !item.dueDate) return undefined
  const days = daysUntil(item.dueDate, today)
  if (days < 0) return 'overdue'
  if (days <= SOON_DAYS) return 'soon'
  return 'later'
}

export interface ChecklistFilters {
  onlyMine: boolean
  hideDone: boolean
}

export function applyFilters(
  items: ChecklistItem[],
  filters: ChecklistFilters,
  userId: number | undefined,
): ChecklistItem[] {
  return items.filter(
    (i) =>
      (!filters.onlyMine || i.assigneeUserId === userId) &&
      (!filters.hideDone || i.status !== 'DONE'),
  )
}

/** Grupos não vazios, na ordem de `CATEGORIES`, mantendo a ordem dos itens. */
export function groupByCategory(items: ChecklistItem[]) {
  return CATEGORIES.map((category) => ({
    category,
    items: items.filter((i) => i.category === category),
  })).filter((g) => g.items.length > 0)
}
