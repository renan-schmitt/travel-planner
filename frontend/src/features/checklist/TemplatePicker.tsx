import { Check } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/utils'
import { CATEGORIES, categoryIcons } from './categories'
import { templateIcons } from './template-icons'
import type { ChecklistTemplate } from './templates-api'

/**
 * Grade de templates (ícone, nome, nº de itens). Com `selected`, os cartões são alternáveis
 * (seleção múltipla, `aria-pressed`); sem, cada toque chama `onPick`.
 */
export function TemplateGrid({
  templates,
  selected,
  onPick,
}: {
  templates: ChecklistTemplate[]
  selected?: number[]
  onPick: (template: ChecklistTemplate) => void
}) {
  const { t } = useTranslation()
  return (
    <ul className="grid grid-cols-2 gap-2 sm:grid-cols-3">
      {templates.map((template) => {
        const Icon = templateIcons[template.icon]
        const isSelected = selected?.includes(template.id)
        return (
          <li key={template.id}>
            <button
              type="button"
              aria-pressed={selected ? isSelected : undefined}
              aria-label={t('templates.cardLabel', {
                name: template.name,
                count: template.items.length,
              })}
              className={cn(
                'relative flex h-full min-h-24 w-full flex-col items-start gap-1.5 rounded-xl border bg-card p-3 text-left transition-colors hover:bg-accent/50 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none',
                isSelected && 'border-primary bg-primary/5 ring-1 ring-primary',
              )}
              onClick={() => onPick(template)}
            >
              <span className="flex size-9 items-center justify-center rounded-full bg-accent text-accent-foreground">
                <Icon className="size-5" strokeWidth={1.75} aria-hidden />
              </span>
              <span className="leading-tight font-medium">{template.name}</span>
              <span className="text-xs text-muted-foreground">
                {t('templates.itemCount', { count: template.items.length })}
              </span>
              {isSelected && (
                <span className="absolute top-2 right-2 flex size-5 items-center justify-center rounded-full bg-primary text-primary-foreground">
                  <Check className="size-3.5" strokeWidth={2.5} aria-hidden />
                </span>
              )}
            </button>
          </li>
        )
      })}
    </ul>
  )
}

/** Prazo de um item de template em relação à partida ("7 dias antes", "No dia da partida"). */
function useOffsetLabel() {
  const { t } = useTranslation()
  return (offset?: number) => {
    if (offset == null) return undefined
    if (offset === 0) return t('templates.onDeparture')
    return offset < 0
      ? t('templates.daysBefore', { count: -offset })
      : t('templates.daysAfter', { count: offset })
  }
}

/** Itens do template agrupados por categoria, com o prazo relativo. */
export function TemplatePreview({ template }: { template: ChecklistTemplate }) {
  const { t } = useTranslation()
  const offsetLabel = useOffsetLabel()
  const groups = CATEGORIES.map((category) => ({
    category,
    items: template.items.filter((i) => i.category === category),
  })).filter((g) => g.items.length > 0)

  return (
    <div className="flex flex-col gap-3">
      {template.description && (
        <p className="text-sm text-muted-foreground">{template.description}</p>
      )}
      {groups.map(({ category, items }) => {
        const Icon = categoryIcons[category]
        const title = t(`checklistCategory.${category}`)
        return (
          <section key={category} className="flex flex-col gap-1">
            <h3 className="flex items-center gap-2 text-sm font-medium">
              <Icon className="size-4 text-primary" strokeWidth={1.75} aria-hidden />
              {title}
            </h3>
            <ul aria-label={title} className="flex flex-col">
              {items.map((item, i) => (
                <li
                  key={i}
                  className="flex items-baseline justify-between gap-3 border-b py-1.5 text-sm last:border-b-0"
                >
                  <span className="min-w-0 break-words">{item.title}</span>
                  {item.dueOffsetDays != null && (
                    <span className="shrink-0 text-xs text-muted-foreground tabular">
                      {offsetLabel(item.dueOffsetDays)}
                    </span>
                  )}
                </li>
              ))}
            </ul>
          </section>
        )
      })}
    </div>
  )
}
