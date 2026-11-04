import { addDays, format } from 'date-fns'
import {
  AlarmClock,
  CircleCheck,
  ExternalLink,
  Info,
  OctagonAlert,
  RotateCcw,
  Search,
  Sparkles,
  TriangleAlert,
  X,
  type LucideIcon,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/EmptyState'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useMe } from '@/features/auth/api'
import { useLodgings, useSegments, useStops } from '@/features/itinerary/api'
import { LodgingSheet } from '@/features/itinerary/LodgingSheet'
import { browserTimeZone } from '@/features/itinerary/segment-form'
import { SegmentSheet } from '@/features/itinerary/SegmentSheet'
import { hasRole, type Trip } from '@/features/trips/api'
import { useTripTab } from '@/features/trips/trip-tabs'
import { formatDate } from '@/lib/dates'
import { cn } from '@/lib/utils'
import {
  useDecideSuggestion,
  useSuggestions,
  type Severity,
  type Suggestion,
  type SuggestionStatus,
} from './api'
import { externalLinksOf, focusKey, resolutionOf, searchLinkOf, type Resolution } from './resolve'
import { suggestionText } from './suggestion-text'

const SEVERITY: Record<
  Severity,
  { icon: LucideIcon; badge: 'critical' | 'warning' | 'info'; card: string; iconClass: string }
> = {
  CRITICAL: {
    icon: OctagonAlert,
    badge: 'critical',
    card: 'border-l-destructive',
    iconClass: 'text-destructive',
  },
  WARNING: {
    icon: TriangleAlert,
    badge: 'warning',
    card: 'border-l-warning',
    iconClass: 'text-warning',
  },
  INFO: { icon: Info, badge: 'info', card: 'border-l-info', iconClass: 'text-info' },
}

type Sheet =
  | { kind: 'segment'; preset: Extract<Resolution, { kind: 'segment' }>['preset'] }
  | { kind: 'lodging'; resolution: Extract<Resolution, { kind: 'lodging' }> }

/**
 * Aba "Sugestões": as lacunas que o motor encontrou (ESP §6.4), mais graves primeiro, com
 * "Resolver" (atalho para cadastrar o que falta), "Adiar" e "Dispensar". Adiadas, aceitas e
 * dispensadas ficam num bloco recolhido, de onde podem ser restauradas.
 */
export function SuggestionsTab({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  const { data: suggestions, error } = useSuggestions(trip.id)
  const { data: stops = [] } = useStops(trip.id)
  const { data: segments = [] } = useSegments(trip.id)
  const { data: lodgings = [] } = useLodgings(trip.id)
  const { data: me } = useMe()
  const homeTz = me?.homeCity?.timezone ?? browserTimeZone()
  const canEdit = hasRole(trip.role, 'EDITOR')
  const { focus } = useTripTab()
  const [sheet, setSheet] = useState<Sheet>()
  const [snoozing, setSnoozing] = useState<Suggestion>()
  const [showOthers, setShowOthers] = useState(false)

  if (error)
    return <p className="py-8 text-center text-destructive">{t('suggestions.loadError')}</p>
  if (!suggestions)
    return <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>

  const pending = suggestions.filter((s) => s.status === 'PENDING')
  const others = suggestions.filter((s) => s.status !== 'PENDING')

  return (
    <div className="flex flex-col gap-3">
      <Card>
        <CardHeader>
          <CardTitle>{t('suggestions.title')}</CardTitle>
          <CardDescription>{t('suggestions.hint')}</CardDescription>
        </CardHeader>
      </Card>

      {pending.length === 0 ? (
        <EmptyState icon={Sparkles} message={t('suggestions.empty')} />
      ) : (
        <ul className="flex flex-col gap-3" aria-label={t('suggestions.pendingList')}>
          {pending.map((s) => (
            <li key={s.id}>
              <SuggestionCard
                trip={trip}
                suggestion={s}
                focused={focus === focusKey(s.ruleCode, s.logicalKey)}
                resolution={resolutionOf(s, stops, homeTz)}
                canEdit={canEdit}
                onOpenSheet={setSheet}
                onSnooze={() => setSnoozing(s)}
              />
            </li>
          ))}
        </ul>
      )}

      {others.length > 0 && (
        <div className="flex flex-col gap-3">
          <Button
            variant="ghost"
            className="self-start"
            aria-expanded={showOthers}
            onClick={() => setShowOthers((v) => !v)}
          >
            {t(showOthers ? 'suggestions.hideOthers' : 'suggestions.showOthers', {
              count: others.length,
            })}
          </Button>
          {showOthers && (
            <ul className="flex flex-col gap-2">
              {others.map((s) => (
                <li key={s.id}>
                  <DecidedRow trip={trip} suggestion={s} canEdit={canEdit} />
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      <SegmentSheet
        tripId={trip.id}
        stops={stops}
        segments={segments}
        homeTz={homeTz}
        preset={sheet?.kind === 'segment' ? sheet.preset : undefined}
        open={sheet?.kind === 'segment'}
        onOpenChange={(open) => !open && setSheet(undefined)}
      />
      <LodgingSheet
        tripId={trip.id}
        stop={sheet?.kind === 'lodging' ? sheet.resolution.stop : undefined}
        lodgings={lodgings}
        preset={sheet?.kind === 'lodging' ? sheet.resolution.preset : undefined}
        open={sheet?.kind === 'lodging'}
        onOpenChange={(open) => !open && setSheet(undefined)}
      />
      <SnoozeDialog trip={trip} suggestion={snoozing} onClose={() => setSnoozing(undefined)} />
    </div>
  )
}

function SuggestionCard({
  trip,
  suggestion,
  focused,
  resolution,
  canEdit,
  onOpenSheet,
  onSnooze,
}: {
  trip: Trip
  suggestion: Suggestion
  focused: boolean
  resolution: Resolution
  canEdit: boolean
  onOpenSheet: (sheet: Sheet) => void
  onSnooze: () => void
}) {
  const { t } = useTranslation()
  const { goTo } = useTripTab()
  const decide = useDecideSuggestion(trip.id)
  const ref = useRef<HTMLDivElement>(null)
  const { title, description } = suggestionText(t, suggestion)
  const severity = SEVERITY[suggestion.severity]
  const Icon = severity.icon
  const links = externalLinksOf(suggestion)
  const searchLink = searchLinkOf(suggestion, trip.id)
  const headingId = `suggestion-${suggestion.id}`

  useEffect(() => {
    if (focused) ref.current?.scrollIntoView?.({ block: 'center' })
  }, [focused])

  function send(status: SuggestionStatus, done: 'suggestions.accepted' | 'suggestions.dismissed') {
    decide.mutate(
      { suggestion, status },
      {
        onSuccess: () => toast.success(t(done)),
        onError: () => toast.error(t('suggestions.saveError')),
      },
    )
  }

  return (
    <Card
      ref={ref}
      role="article"
      aria-labelledby={headingId}
      className={cn('gap-3 border-l-4 py-4', severity.card, focused && 'ring-3 ring-ring/50')}
    >
      <CardContent className="flex flex-col gap-3 px-4">
        <div className="flex items-start gap-3">
          <Icon
            className={cn('mt-0.5 size-5 shrink-0', severity.iconClass)}
            strokeWidth={1.75}
            aria-hidden
          />
          <div className="flex min-w-0 flex-col gap-1">
            <h3 id={headingId} className="leading-snug font-semibold">
              {title}
            </h3>
            {description && <p className="text-sm text-muted-foreground">{description}</p>}
            {suggestion.checklist && (
              <p className="text-sm text-muted-foreground">
                {t(
                  suggestion.checklist.automatic
                    ? 'suggestions.checklistAdded'
                    : 'suggestions.checklistOnAccept',
                  { title: suggestion.checklist.title },
                )}
              </p>
            )}
          </div>
          <Badge variant={severity.badge} className="ml-auto shrink-0">
            {t(`suggestions.severity.${suggestion.severity}`)}
          </Badge>
        </div>

        {links.length > 0 && (
          <div className="flex flex-wrap gap-x-4 gap-y-1 pl-8 text-sm">
            {links.map((link) => (
              <a
                key={link.key}
                href={link.href}
                target="_blank"
                rel="noreferrer"
                className="inline-flex min-h-11 items-center gap-1 text-primary underline-offset-4 hover:underline"
              >
                {t(`suggestions.links.${link.key}`)}
                <ExternalLink className="size-3.5" strokeWidth={1.75} aria-hidden />
              </a>
            ))}
          </div>
        )}

        {canEdit && (
          <div className="flex flex-wrap gap-2 pl-8">
            <ResolveButton
              resolution={resolution}
              onOpenSheet={onOpenSheet}
              onAccept={() => send('ACCEPTED', 'suggestions.accepted')}
              onChecklist={() => goTo('checklist')}
              disabled={decide.isPending}
            />
            {searchLink && (
              <Button asChild variant={resolution.kind === 'none' ? 'default' : 'outline'}>
                <Link to={searchLink.to}>
                  <Search strokeWidth={1.75} aria-hidden />
                  {t(`suggestions.${searchLink.label}`)}
                </Link>
              </Button>
            )}
            <Button variant="outline" onClick={onSnooze} disabled={decide.isPending}>
              <AlarmClock strokeWidth={1.75} aria-hidden />
              {t('suggestions.snooze')}
            </Button>
            <Button
              variant="ghost"
              onClick={() => send('DISMISSED', 'suggestions.dismissed')}
              disabled={decide.isPending}
            >
              <X strokeWidth={1.75} aria-hidden />
              {t('suggestions.dismiss')}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function ResolveButton({
  resolution,
  onOpenSheet,
  onAccept,
  onChecklist,
  disabled,
}: {
  resolution: Resolution
  onOpenSheet: (sheet: Sheet) => void
  onAccept: () => void
  onChecklist: () => void
  disabled: boolean
}) {
  const { t } = useTranslation()
  switch (resolution.kind) {
    case 'segment':
      return (
        <Button onClick={() => onOpenSheet({ kind: 'segment', preset: resolution.preset })}>
          {t('suggestions.resolve.segment')}
        </Button>
      )
    case 'lodging':
      return (
        <Button onClick={() => onOpenSheet({ kind: 'lodging', resolution })}>
          {t('suggestions.resolve.lodging')}
        </Button>
      )
    case 'travelers':
      return (
        <Button asChild>
          <Link to="/profile/travelers">{t('suggestions.resolve.travelers')}</Link>
        </Button>
      )
    case 'checklist':
      return <Button onClick={onChecklist}>{t('suggestions.resolve.checklist')}</Button>
    case 'accept':
      return (
        <Button onClick={onAccept} disabled={disabled}>
          <CircleCheck strokeWidth={1.75} aria-hidden />
          {t('suggestions.resolve.accept')}
        </Button>
      )
    case 'none':
      return null
  }
}

/** Sugestão já decidida (adiada, aceita ou dispensada), com "Restaurar". */
function DecidedRow({
  trip,
  suggestion,
  canEdit,
}: {
  trip: Trip
  suggestion: Suggestion
  canEdit: boolean
}) {
  const { t } = useTranslation()
  const decide = useDecideSuggestion(trip.id)
  const { title } = suggestionText(t, suggestion)
  const status =
    suggestion.status === 'SNOOZED' && suggestion.snoozedUntil
      ? t('suggestions.status.SNOOZED', { date: formatDate(suggestion.snoozedUntil) })
      : t(`suggestions.status.${suggestion.status}`)

  return (
    <div className="flex items-center gap-3 rounded-lg border px-3 py-2">
      <div className="flex min-w-0 flex-col">
        <span className="text-sm">{title}</span>
        <span className="text-xs text-muted-foreground">{status}</span>
      </div>
      {canEdit && (
        <Button
          variant="ghost"
          className="ml-auto shrink-0"
          disabled={decide.isPending}
          onClick={() =>
            decide.mutate(
              { suggestion, status: 'PENDING' },
              { onError: () => toast.error(t('suggestions.saveError')) },
            )
          }
        >
          <RotateCcw strokeWidth={1.75} aria-hidden />
          {t('suggestions.restore')}
        </Button>
      )}
    </div>
  )
}

function SnoozeDialog({
  trip,
  suggestion,
  onClose,
}: {
  trip: Trip
  suggestion?: Suggestion
  onClose: () => void
}) {
  const { t } = useTranslation()
  const decide = useDecideSuggestion(trip.id)
  const tomorrow = format(addDays(new Date(), 1), 'yyyy-MM-dd')
  const [date, setDate] = useState(() => format(addDays(new Date(), 7), 'yyyy-MM-dd'))
  const valid = date >= tomorrow

  function onConfirm() {
    if (!suggestion || !valid) return
    decide.mutate(
      { suggestion, status: 'SNOOZED', snoozedUntil: date },
      {
        onSuccess: () => {
          toast.success(t('suggestions.snoozed', { date: formatDate(date) }))
          onClose()
        },
        onError: () => toast.error(t('suggestions.saveError')),
      },
    )
  }

  return (
    <Dialog open={!!suggestion} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('suggestions.snoozeTitle')}</DialogTitle>
          <DialogDescription>{t('suggestions.snoozeDescription')}</DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-2">
          <Label htmlFor="snooze-until">{t('suggestions.snoozeUntil')}</Label>
          <Input
            id="snooze-until"
            type="date"
            min={tomorrow}
            value={date}
            onChange={(e) => setDate(e.target.value)}
            aria-invalid={!valid}
          />
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('common.cancel')}</Button>
          </DialogClose>
          <Button disabled={!valid || decide.isPending} onClick={onConfirm}>
            {t('suggestions.snoozeConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
