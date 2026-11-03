import { ListChecks, NotebookText, Plus } from 'lucide-react'
import { useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { EmptyState } from '@/components/EmptyState'
import { UserAvatar } from '@/components/UserAvatar'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { useMe } from '@/features/auth/api'
import { useMembers, type Member } from '@/features/sharing/api'
import { hasRole, type Trip } from '@/features/trips/api'
import { formatDate } from '@/lib/dates'
import { cn } from '@/lib/utils'
import {
  useChecklist,
  useCreateChecklistItem,
  useSetChecklistStatus,
  type ChecklistCategory,
  type ChecklistItem,
} from './api'
import {
  applyFilters,
  CATEGORIES,
  categoryIcons,
  dueState,
  groupByCategory,
  type ChecklistFilters,
} from './categories'
import { ChecklistItemSheet } from './ChecklistItemSheet'

/**
 * Aba "Checklist": progresso no topo, filtros ("só meus", "esconder concluídos"), itens agrupados
 * por categoria e adição rápida fixa no rodapé. Marcar/desmarcar é otimista. Leitor só vê.
 */
export function ChecklistTab({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  const canEdit = hasRole(trip.role, 'EDITOR')
  const { data: items, error } = useChecklist(trip.id)
  const { data: members } = useMembers(trip.id)
  const { data: me } = useMe()
  const [filters, setFilters] = useState<ChecklistFilters>({ onlyMine: false, hideDone: false })
  const [editing, setEditing] = useState<ChecklistItem>()
  const [sheetOpen, setSheetOpen] = useState(false)

  const byUserId = useMemo(
    () => new Map((members ?? []).filter((m) => m.userId != null).map((m) => [m.userId!, m])),
    [members],
  )

  if (error) return <p className="py-8 text-center text-destructive">{t('checklist.loadError')}</p>
  if (!items) return <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>

  const visible = applyFilters(items, filters, me?.id)
  const groups = groupByCategory(visible)
  // O item aberto no sheet sai da lista atualizada (versão nova depois de salvar).
  const current = editing && items.find((i) => i.id === editing.id)

  function openItem(item: ChecklistItem) {
    setEditing(item)
    setSheetOpen(true)
  }

  return (
    <div className="flex flex-col gap-3">
      {items.length === 0 ? (
        <EmptyState
          icon={ListChecks}
          message={t(canEdit ? 'checklist.emptyEditor' : 'checklist.empty')}
        />
      ) : (
        <>
          <Progress items={items} />
          <div className="flex flex-wrap gap-2" role="group" aria-label={t('checklist.filters')}>
            <FilterChip
              pressed={filters.onlyMine}
              onClick={() => setFilters((f) => ({ ...f, onlyMine: !f.onlyMine }))}
            >
              {t('checklist.onlyMine')}
            </FilterChip>
            <FilterChip
              pressed={filters.hideDone}
              onClick={() => setFilters((f) => ({ ...f, hideDone: !f.hideDone }))}
            >
              {t('checklist.hideDone')}
            </FilterChip>
          </div>
          {groups.length === 0 && (
            <p className="py-6 text-center text-muted-foreground">{t('checklist.noneFiltered')}</p>
          )}
          {groups.map(({ category, items: groupItems }) => (
            <CategoryCard
              key={category}
              tripId={trip.id}
              category={category}
              items={groupItems}
              byUserId={byUserId}
              canEdit={canEdit}
              onOpen={openItem}
            />
          ))}
        </>
      )}
      {canEdit && <QuickAdd tripId={trip.id} />}
      {canEdit && (
        <ChecklistItemSheet
          tripId={trip.id}
          item={current}
          members={members ?? []}
          open={sheetOpen && !!current}
          onOpenChange={setSheetOpen}
        />
      )}
    </div>
  )
}

function Progress({ items }: { items: ChecklistItem[] }) {
  const { t } = useTranslation()
  const done = items.filter((i) => i.status === 'DONE').length
  const percent = Math.round((done / items.length) * 100)
  return (
    <Card className="gap-2 py-4">
      <CardContent className="flex flex-col gap-2">
        <div className="flex items-baseline justify-between">
          <span className="text-sm font-medium">{t('checklist.progress')}</span>
          <span className="text-lg font-semibold tabular" data-testid="checklist-progress">
            {done}/{items.length}
          </span>
        </div>
        <div
          role="progressbar"
          aria-label={t('checklist.progressLabel', { done, total: items.length })}
          aria-valuemin={0}
          aria-valuemax={items.length}
          aria-valuenow={done}
          className="h-2 overflow-hidden rounded-full bg-muted"
        >
          <div
            className="h-full rounded-full bg-success transition-[width] duration-300"
            style={{ width: `${percent}%` }}
          />
        </div>
      </CardContent>
    </Card>
  )
}

function FilterChip({
  pressed,
  onClick,
  children,
}: {
  pressed: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <Button
      type="button"
      variant={pressed ? 'default' : 'outline'}
      aria-pressed={pressed}
      className="rounded-full"
      onClick={onClick}
    >
      {children}
    </Button>
  )
}

function CategoryCard({
  tripId,
  category,
  items,
  byUserId,
  canEdit,
  onOpen,
}: {
  tripId: number
  category: ChecklistCategory
  items: ChecklistItem[]
  byUserId: Map<number, Member>
  canEdit: boolean
  onOpen: (item: ChecklistItem) => void
}) {
  const { t } = useTranslation()
  const Icon = categoryIcons[category]
  const done = items.filter((i) => i.status === 'DONE').length
  const title = t(`checklistCategory.${category}`)
  return (
    <Card className="gap-1 py-3">
      <CardHeader className="flex flex-row items-center justify-between gap-2">
        <CardTitle className="flex items-center gap-2 text-base">
          <Icon className="size-5 text-primary" strokeWidth={1.75} aria-hidden />
          {title}
        </CardTitle>
        <span className="text-sm text-muted-foreground tabular">
          {done}/{items.length}
        </span>
      </CardHeader>
      <CardContent className="px-2">
        <ul aria-label={title} className="flex flex-col">
          {items.map((item) => (
            <ItemRow
              key={item.id}
              tripId={tripId}
              item={item}
              assignee={item.assigneeUserId != null ? byUserId.get(item.assigneeUserId) : undefined}
              canEdit={canEdit}
              onOpen={onOpen}
            />
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

function ItemRow({
  tripId,
  item,
  assignee,
  canEdit,
  onOpen,
}: {
  tripId: number
  item: ChecklistItem
  assignee?: Member
  canEdit: boolean
  onOpen: (item: ChecklistItem) => void
}) {
  const { t } = useTranslation()
  const setStatus = useSetChecklistStatus(tripId)
  const done = item.status === 'DONE'
  const due = dueState(item)

  function toggle(checked: boolean) {
    setStatus.mutate(
      { item, status: checked ? 'DONE' : 'OPEN' },
      { onError: () => toast.error(t('checklist.statusError')) },
    )
  }

  const body = (
    <span className="flex min-w-0 flex-1 flex-col gap-0.5 py-2 text-left">
      <span className={cn('break-words', done && 'text-muted-foreground line-through')}>
        {item.title}
      </span>
      {(item.dueDate || item.notes) && (
        <span className="flex flex-wrap items-center gap-x-2 text-xs text-muted-foreground">
          {item.dueDate && (
            <span
              className={cn(
                'tabular',
                due === 'overdue' && 'font-medium text-destructive',
                due === 'soon' && 'font-medium text-warning-foreground',
              )}
            >
              {t(due === 'overdue' ? 'checklist.overdue' : 'checklist.due', {
                date: formatDate(item.dueDate),
              })}
            </span>
          )}
          {item.notes && (
            <NotebookText
              className="size-3.5"
              strokeWidth={1.75}
              aria-label={t('checklist.hasNotes')}
            />
          )}
        </span>
      )}
    </span>
  )

  return (
    <li className="flex items-center gap-1">
      {/* Área de toque de 44 px em volta da caixa. */}
      <label className="flex size-11 shrink-0 cursor-pointer items-center justify-center">
        <Checkbox
          className="size-6 rounded-md [&_svg]:size-4"
          checked={done}
          disabled={!canEdit}
          aria-label={item.title}
          onCheckedChange={(checked) => toggle(checked === true)}
        />
      </label>
      {canEdit ? (
        <button
          type="button"
          className="flex min-h-11 min-w-0 flex-1 items-center gap-2 rounded-lg pr-2 hover:bg-accent/50 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
          aria-label={t('checklist.editItem', { title: item.title })}
          onClick={() => onOpen(item)}
        >
          {body}
          {assignee && <Assignee member={assignee} />}
        </button>
      ) : (
        <div className="flex min-h-11 min-w-0 flex-1 items-center gap-2 pr-2">
          {body}
          {assignee && <Assignee member={assignee} />}
        </div>
      )}
    </li>
  )
}

function Assignee({ member }: { member: Member }) {
  const { t } = useTranslation()
  const name = member.name ?? member.email
  return (
    <span title={name} role="img" aria-label={t('checklist.assignedTo', { name })}>
      <UserAvatar pictureUrl={member.pictureUrl} className="size-7 [&_svg]:size-4" />
    </span>
  )
}

/** Input fixo no rodapé (acima da navegação no celular): Enter adiciona na categoria escolhida. */
function QuickAdd({ tripId }: { tripId: number }) {
  const { t } = useTranslation()
  const create = useCreateChecklistItem(tripId)
  const [category, setCategory] = useState<ChecklistCategory>('OTHER')
  const [title, setTitle] = useState('')
  const input = useRef<HTMLInputElement>(null)

  function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    const value = title.trim()
    if (!value) return
    setTitle('')
    create.mutate(
      { category, title: value },
      {
        onError: () => {
          setTitle(value)
          toast.error(t('checklist.addError'))
        },
      },
    )
    input.current?.focus()
  }

  return (
    <form
      onSubmit={onSubmit}
      aria-label={t('checklist.quickAdd')}
      className="sticky bottom-[calc(3.5rem+1px+env(safe-area-inset-bottom))] z-20 -mx-4 flex gap-2 border-t bg-background/95 px-4 py-2 backdrop-blur lg:bottom-0 lg:mx-0 lg:rounded-xl lg:border"
    >
      <div className="w-32 shrink-0">
        <NativeSelect
          aria-label={t('checklist.category')}
          value={category}
          onChange={(e) => setCategory(e.target.value as ChecklistCategory)}
        >
          {CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {t(`checklistCategory.${c}`)}
            </option>
          ))}
        </NativeSelect>
      </div>
      <Input
        ref={input}
        value={title}
        maxLength={200}
        enterKeyHint="done"
        placeholder={t('checklist.addPlaceholder')}
        aria-label={t('checklist.newItem')}
        onChange={(e) => setTitle(e.target.value)}
      />
      <Button
        type="submit"
        size="icon"
        className="size-11 shrink-0"
        aria-label={t('checklist.add')}
      >
        <Plus className="size-5" strokeWidth={2} />
      </Button>
    </form>
  )
}
