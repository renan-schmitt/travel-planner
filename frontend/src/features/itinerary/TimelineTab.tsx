import {
  DndContext,
  DragOverlay,
  MouseSensor,
  TouchSensor,
  pointerWithin,
  closestCenter,
  useDraggable,
  useDroppable,
  useSensor,
  useSensors,
  type CollisionDetection,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core'
import { SortableContext, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import {
  Bookmark,
  CalendarRange,
  ChevronDown,
  GripVertical,
  House,
  Landmark,
  LogIn,
  LogOut,
  MapPin,
  MoonStar,
  Plus,
  Sparkles,
  TriangleAlert,
  UtensilsCrossed,
  type LucideIcon,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { EmptyState } from '@/components/EmptyState'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useTripSavedPlaces, type SavedPlace, type SavedPlaceKind } from '@/features/places/api'
import { focusKey } from '@/features/suggestions/resolve'
import { hasRole, type Trip } from '@/features/trips/api'
import { useTripTab } from '@/features/trips/trip-tabs'
import { countryFlag } from '@/lib/countries'
import { formatDayHeader, formatLocalDateTime, gmtLabel, todayIso } from '@/lib/dates'
import { cn } from '@/lib/utils'
import { dayActivityIds, moveActivity } from './activity-order'
import { ActivitySheet, type ActivitySheetTarget } from './ActivitySheet'
import {
  useCreateActivity,
  useItinerary,
  useReorderActivities,
  type Itinerary,
  type ItineraryActivity,
  type ItineraryDay,
  type ItineraryEvent,
  type ItinerarySegment,
} from './api'
import { LodgingIcon } from './LodgingSummary'
import { TransportIcon } from './TransportIcon'

/**
 * Aba "Timeline": a viagem dia a dia (gerada no backend a partir de paradas, trechos, hospedagens e
 * atividades). Cada dia tem as cidades, os eventos com horário local e onde se dorme; noite sem
 * hospedagem aparece em destaque. Durante a viagem, a lista rola até "hoje".
 *
 * Editor planeja atividades: "+ Atividade" por dia, toque para editar e arrastar e soltar (toque
 * longo de 200 ms no celular, para não brigar com a rolagem) para reordenar no dia, mover entre
 * dias ou trazer um lugar salvo da bandeja "Lugares salvos". As setas ↑/↓ no puxador fazem o mesmo
 * pelo teclado.
 */
export function TimelineTab({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  const canEdit = hasRole(trip.role, 'EDITOR')
  const { data, error } = useItinerary(trip.id)
  const { data: savedPlaces = [] } = useTripSavedPlaces(trip.id)
  const [sheet, setSheet] = useState<ActivitySheetTarget>()
  const reorder = useReorderActivities(trip.id)
  const today = todayIso()
  const todayRef = useRef<HTMLElement>(null)
  const days = data?.days
  const hasToday = days?.some((d) => d.date === today) ?? false

  // Rola até hoje uma vez, quando os dias chegam.
  useEffect(() => {
    if (hasToday) todayRef.current?.scrollIntoView?.({ block: 'start' })
  }, [hasToday])

  if (error) return <p className="py-8 text-center text-destructive">{t('timeline.loadError')}</p>
  if (!data || !days) {
    return <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
  }
  if (days.length === 0) return <EmptyState icon={CalendarRange} message={t('timeline.empty')} />

  const gaps = days.filter((d) => d.gap).length
  const onEdit = canEdit ? (activity: ItineraryActivity) => setSheet({ activity }) : undefined
  const itinerary = data

  function move(activityId: number, day: string, index: number) {
    const result = moveActivity(itinerary, activityId, day, index)
    if (!result) return
    reorder.mutate(result, { onError: () => toast.error(t('activities.moveError')) })
  }

  /** Setas no puxador: uma posição no dia; na ponta, para o fim do dia anterior/começo do seguinte. */
  function keyboardMove(activityId: number, delta: -1 | 1) {
    const dayIndex = itinerary.days.findIndex((d) =>
      d.events.some((e) => e.activity?.id === activityId),
    )
    const day = itinerary.days[dayIndex]
    if (!day) return
    const ids = dayActivityIds(day)
    const index = ids.indexOf(activityId) + delta
    if (index >= 0 && index < ids.length) {
      move(activityId, day.date, index)
    } else {
      const next = itinerary.days[dayIndex + delta]
      if (next) move(activityId, next.date, delta < 0 ? Number.MAX_SAFE_INTEGER : 0)
    }
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>{t('timeline.title')}</CardTitle>
          <CardDescription>{t(canEdit ? 'timeline.hintEditor' : 'timeline.hint')}</CardDescription>
          {(gaps > 0 || hasToday) && (
            <div className="flex flex-wrap items-center gap-2 pt-1">
              {gaps > 0 && (
                <Badge variant="warning">
                  <TriangleAlert aria-hidden />
                  {t('timeline.gaps', { count: gaps })}
                </Badge>
              )}
              {hasToday && (
                <Button
                  variant="outline"
                  onClick={() => todayRef.current?.scrollIntoView?.({ behavior: 'smooth' })}
                >
                  {t('timeline.goToToday')}
                </Button>
              )}
            </div>
          )}
        </CardHeader>
        <CardContent>
          <TimelineDnd
            tripId={trip.id}
            itinerary={data}
            savedPlaces={savedPlaces}
            enabled={canEdit}
            onMove={move}
          >
            <ol className="flex flex-col gap-6">
              {days.map((day) => (
                <li key={day.date}>
                  <DaySection
                    day={day}
                    isToday={day.date === today}
                    ref={day.date === today ? todayRef : undefined}
                    canEdit={canEdit}
                    onAdd={() => setSheet({ day: day.date })}
                    onEdit={onEdit}
                    onMove={keyboardMove}
                  />
                </li>
              ))}
            </ol>
            {canEdit && savedPlaces.length > 0 && (
              <PlacesTray
                places={savedPlaces}
                onPick={(place) =>
                  setSheet({ day: defaultDayFor(days, place, today), savedPlaceId: place.id })
                }
              />
            )}
          </TimelineDnd>
        </CardContent>
      </Card>
      {data.outsideActivities.length > 0 && (
        <OutsideActivities activities={data.outsideActivities} onEdit={onEdit} />
      )}
      {canEdit && (
        <ActivitySheet
          tripId={trip.id}
          days={days}
          savedPlaces={savedPlaces}
          target={sheet}
          onClose={() => setSheet(undefined)}
        />
      )}
    </>
  )
}

/** Dia sugerido para um favorito tocado na bandeja: hoje, se for na cidade dele; senão o 1º dia. */
function defaultDayFor(days: ItineraryDay[], place: SavedPlace, today: string): string {
  const inCity = days.filter((d) => d.stops.some((s) => s.id === place.stopId))
  return (
    inCity.find((d) => d.date === today)?.date ??
    inCity.find((d) => d.date >= today)?.date ??
    inCity[0]?.date ??
    days[0]?.date ??
    today
  )
}

const activityDndId = (id: number) => `a-${id}`
const dayDndId = (date: string) => `d-${date}`
const placeDndId = (id: number) => `p-${id}`

/** Prefere a atividade sob o dedo; senão o dia; fora de tudo, o mais próximo. */
const collisions: CollisionDetection = (args) => {
  const within = pointerWithin(args)
  if (within.length > 0) {
    return [...within].sort(
      (a, b) => Number(String(b.id).startsWith('a-')) - Number(String(a.id).startsWith('a-')),
    )
  }
  return closestCenter(args)
}

function TimelineDnd({
  tripId,
  itinerary,
  savedPlaces,
  enabled,
  onMove,
  children,
}: {
  tripId: number
  itinerary: Itinerary
  savedPlaces: SavedPlace[]
  enabled: boolean
  /** Atividade para o dia `day`, na posição `index` entre as atividades dele. */
  onMove: (activityId: number, day: string, index: number) => void
  children: React.ReactNode
}) {
  const { t } = useTranslation()
  const create = useCreateActivity(tripId)
  const [dragging, setDragging] = useState<string>()
  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 5 } }),
    // Toque longo: um toque curto continua sendo clique e o arrasto vertical rápido, rolagem.
    useSensor(TouchSensor, { activationConstraint: { delay: 200, tolerance: 8 } }),
  )

  function onDragStart(event: DragStartEvent) {
    setDragging(String(event.active.id))
  }

  function onDragEnd({ active, over }: DragEndEvent) {
    setDragging(undefined)
    if (!over) return
    const overId = String(over.id)
    let day: string | undefined
    let index = Number.MAX_SAFE_INTEGER
    if (overId.startsWith('d-')) {
      day = overId.slice(2)
    } else if (overId.startsWith('a-')) {
      const overActivity = Number(overId.slice(2))
      const target = itinerary.days.find((d) =>
        d.events.some((e) => e.activity?.id === overActivity),
      )
      day = target?.date
      if (target) index = dayActivityIds(target).indexOf(overActivity)
    }
    if (!day) return
    const activeId = String(active.id)
    if (activeId.startsWith('a-')) {
      onMove(Number(activeId.slice(2)), day, index)
    } else if (activeId.startsWith('p-')) {
      const place = savedPlaces.find((p) => p.id === Number(activeId.slice(2)))
      if (!place) return
      create.mutate(
        { day, savedPlaceId: place.id },
        {
          onSuccess: () => toast.success(t('activities.addedPlace', { name: place.name })),
          onError: () => toast.error(t('activities.saveError')),
        },
      )
    }
  }

  const overlay = dragging && overlayLabel(dragging, itinerary, savedPlaces)

  return (
    <DndContext
      sensors={enabled ? sensors : []}
      collisionDetection={collisions}
      onDragStart={onDragStart}
      onDragEnd={onDragEnd}
      onDragCancel={() => setDragging(undefined)}
    >
      {children}
      <DragOverlay>
        {overlay && (
          <div className="flex max-w-72 items-center gap-2 rounded-lg border bg-card px-3 py-2 text-sm font-medium shadow-lg">
            <GripVertical className="size-4 shrink-0 text-muted-foreground" aria-hidden />
            <span className="truncate">{overlay}</span>
          </div>
        )}
      </DragOverlay>
    </DndContext>
  )
}

function overlayLabel(id: string, itinerary: Itinerary, places: SavedPlace[]) {
  const n = Number(id.slice(2))
  if (id.startsWith('p-')) return places.find((p) => p.id === n)?.name
  for (const day of itinerary.days) {
    const event = day.events.find((e) => e.activity?.id === n)
    if (event) return event.activity?.title
  }
  return undefined
}

function DaySection({
  day,
  isToday,
  ref,
  canEdit,
  onAdd,
  onEdit,
  onMove,
}: {
  day: ItineraryDay
  isToday: boolean
  ref?: React.Ref<HTMLElement>
  canEdit: boolean
  onAdd: () => void
  onEdit?: (activity: ItineraryActivity) => void
  onMove: (activityId: number, delta: -1 | 1) => void
}) {
  const { t } = useTranslation()
  const { setNodeRef, isOver } = useDroppable({ id: dayDndId(day.date), disabled: !canEdit })
  const headingId = `day-${day.date}`
  const place =
    day.stops.length > 0
      ? day.stops.map((s) => `${countryFlag(s.countryCode)} ${s.cityName}`).join(' → ')
      : day.overnight.kind === 'IN_TRANSIT'
        ? t('timeline.inTransitDay')
        : undefined
  const activityIds = dayActivityIds(day)

  return (
    <section
      ref={(node) => {
        setNodeRef(node)
        if (typeof ref === 'function') ref(node)
        else if (ref) (ref as React.RefObject<HTMLElement | null>).current = node
      }}
      aria-labelledby={headingId}
      className={cn(
        '-mx-2 scroll-mt-20 rounded-xl px-2 py-1 transition-colors',
        isOver && 'bg-accent/60 ring-2 ring-primary/40',
      )}
    >
      <h3
        id={headingId}
        className="mb-2 flex flex-wrap items-center gap-x-2 text-base font-semibold"
      >
        <span className="tabular">{formatDayHeader(day.date)}</span>
        {place && <span className="font-normal text-muted-foreground">· {place}</span>}
        {isToday && <Badge variant="highlight">{t('timeline.today')}</Badge>}
      </h3>
      <ul className="ml-4 flex flex-col gap-3 border-l-2 border-border pl-5">
        {day.events.length === 0 && (
          <li className="text-sm text-muted-foreground">{t('timeline.freeDay')}</li>
        )}
        <SortableContext
          items={activityIds.map(activityDndId)}
          strategy={verticalListSortingStrategy}
        >
          {day.events.map((event, i) =>
            event.activity ? (
              <SortableActivity
                key={`a-${event.activity.id}`}
                activity={event.activity}
                canEdit={canEdit}
                onEdit={onEdit}
                onMove={onMove}
              />
            ) : (
              <EventRow key={i} event={event} />
            ),
          )}
        </SortableContext>
        {canEdit && (
          <li>
            <Button
              variant="ghost"
              className="-ml-3 text-primary"
              aria-label={t('activities.addTo', { day: formatDayHeader(day.date) })}
              onClick={onAdd}
            >
              <Plus strokeWidth={1.75} aria-hidden />
              {t('activities.addShort')}
            </Button>
          </li>
        )}
        <li>
          <Overnight day={day} />
        </li>
      </ul>
    </section>
  )
}

const PLACE_ICONS: Record<SavedPlaceKind, LucideIcon> = {
  RESTAURANT: UtensilsCrossed,
  ATTRACTION: Landmark,
  OTHER: MapPin,
}

function ActivityIcon({ activity }: { activity: ItineraryActivity }) {
  const Icon = activity.savedPlace ? PLACE_ICONS[activity.savedPlace.kind] : Sparkles
  return <Icon strokeWidth={1.75} aria-hidden />
}

/** "09:30–11:00", "20:00" ou nada. */
function activityTime(activity: ItineraryActivity) {
  if (!activity.startTime) return undefined
  return activity.endTime ? `${activity.startTime}–${activity.endTime}` : activity.startTime
}

function SortableActivity({
  activity,
  canEdit,
  onEdit,
  onMove,
}: {
  activity: ItineraryActivity
  canEdit: boolean
  onEdit?: (activity: ItineraryActivity) => void
  onMove: (activityId: number, delta: -1 | 1) => void
}) {
  const { t } = useTranslation()
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: activityDndId(activity.id), disabled: !canEdit })

  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      className={cn('relative min-h-7', isDragging && 'opacity-40')}
    >
      <Bullet>
        <ActivityIcon activity={activity} />
      </Bullet>
      <div className="flex items-start gap-1">
        <ActivityText activity={activity} onEdit={onEdit} />
        {canEdit && (
          <button
            type="button"
            ref={setActivatorNodeRef}
            {...attributes}
            {...listeners}
            // Sem o `KeyboardSensor` do dnd-kit: as setas movem direto (mais simples de usar).
            onKeyDown={(e) => {
              if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
                e.preventDefault()
                onMove(activity.id, e.key === 'ArrowUp' ? -1 : 1)
              }
            }}
            aria-roledescription={undefined}
            aria-describedby={undefined}
            aria-label={t('activities.dragHandle', { title: activity.title })}
            className="-my-2 -mr-2 flex size-11 shrink-0 cursor-grab touch-manipulation items-center justify-center rounded-lg text-muted-foreground select-none [-webkit-touch-callout:none] hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none active:cursor-grabbing"
          >
            <GripVertical className="size-5" aria-hidden />
          </button>
        )}
      </div>
    </li>
  )
}

function ActivityText({
  activity,
  onEdit,
}: {
  activity: ItineraryActivity
  onEdit?: (activity: ItineraryActivity) => void
}) {
  const { t } = useTranslation()
  const time = activityTime(activity)
  const place =
    activity.savedPlace && activity.savedPlace.name !== activity.title
      ? activity.savedPlace.name
      : undefined
  const content = (
    <>
      <span className="flex gap-2 text-sm">
        {time && <span className="shrink-0 font-medium tabular">{time}</span>}
        <span className="min-w-0 font-medium break-words">{activity.title}</span>
      </span>
      {(place ?? activity.notes) && (
        <span className="line-clamp-2 block text-xs whitespace-pre-line text-muted-foreground">
          {[place, activity.notes].filter(Boolean).join(' · ')}
        </span>
      )}
    </>
  )
  if (!onEdit) return <div className="min-w-0 flex-1 pt-1">{content}</div>
  return (
    <button
      type="button"
      className="-mx-2 min-w-0 flex-1 rounded-lg px-2 pt-1 pb-1 text-left hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
      aria-label={t('activities.edit', { title: activity.title })}
      onClick={() => onEdit(activity)}
    >
      {content}
    </button>
  )
}

/**
 * Bandeja fixa no rodapé com os lugares salvos da viagem: arrastar um para um dia cria a
 * atividade; tocar abre o formulário já com o lugar.
 */
function PlacesTray({
  places,
  onPick,
}: {
  places: SavedPlace[]
  onPick: (place: SavedPlace) => void
}) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  return (
    <div className="sticky bottom-[calc(3.5rem+1px+env(safe-area-inset-bottom))] z-20 -mx-4 mt-4 border-t bg-background/95 px-4 py-2 backdrop-blur lg:bottom-0 lg:mx-0 lg:rounded-xl lg:border">
      <Button
        variant="ghost"
        className="w-full justify-between"
        aria-expanded={open}
        onClick={() => setOpen(!open)}
      >
        <span className="flex items-center gap-2">
          <Bookmark strokeWidth={1.75} aria-hidden />
          {t('activities.trayTitle', { count: places.length })}
        </span>
        <ChevronDown
          className={cn('transition-transform', open && 'rotate-180')}
          strokeWidth={1.75}
          aria-hidden
        />
      </Button>
      {open && (
        <>
          <p className="px-1 pb-2 text-xs text-muted-foreground">{t('activities.trayHint')}</p>
          <ul
            aria-label={t('activities.trayTitle', { count: places.length })}
            className="flex gap-2 overflow-x-auto pb-1"
          >
            {places.map((place) => (
              <TrayPlace key={place.id} place={place} onPick={onPick} />
            ))}
          </ul>
        </>
      )}
    </div>
  )
}

function TrayPlace({ place, onPick }: { place: SavedPlace; onPick: (place: SavedPlace) => void }) {
  const { t } = useTranslation()
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: placeDndId(place.id),
  })
  const Icon = PLACE_ICONS[place.kind]
  return (
    <li className="shrink-0">
      <button
        type="button"
        ref={setNodeRef}
        {...attributes}
        {...listeners}
        aria-roledescription={undefined}
        aria-describedby={undefined}
        aria-label={t('activities.planPlace', { name: place.name })}
        onClick={() => onPick(place)}
        className={cn(
          'flex min-h-11 max-w-56 touch-manipulation items-center gap-2 rounded-full border bg-card px-3 text-sm select-none [-webkit-touch-callout:none] hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none',
          isDragging && 'opacity-40',
        )}
      >
        <Icon className="size-4 shrink-0" strokeWidth={1.75} aria-hidden />
        <span className="truncate">{place.name}</span>
      </button>
    </li>
  )
}

/** Atividades de dias que saíram da viagem (as datas mudaram): editar para escolher outro dia. */
function OutsideActivities({
  activities,
  onEdit,
}: {
  activities: ItineraryActivity[]
  onEdit?: (activity: ItineraryActivity) => void
}) {
  const { t } = useTranslation()
  return (
    <Card className="mt-4">
      <CardHeader>
        <CardTitle>{t('activities.outsideTitle')}</CardTitle>
        <CardDescription>{t('activities.outsideHint')}</CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="flex flex-col gap-2">
          {activities.map((activity) => (
            <li key={activity.id} className="flex items-start gap-3">
              <span className="pt-1.5 text-xs text-muted-foreground tabular">
                {formatDayHeader(activity.day)}
              </span>
              <ActivityText activity={activity} onEdit={onEdit} />
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

/** Ícone do evento num círculo `--accent`, sobre a linha vertical da timeline. */
function Bullet({ children }: { children: React.ReactNode }) {
  return (
    <span className="absolute top-0 -left-[2.1rem] flex size-7 items-center justify-center rounded-full bg-accent text-accent-foreground [&_svg]:size-4">
      {children}
    </span>
  )
}

function EventRow({ event }: { event: ItineraryEvent }) {
  const { t } = useTranslation()
  const segmentTitle = useTimelineSegmentTitle()

  if (event.lodging) {
    const checkIn = event.kind === 'CHECK_IN'
    const Icon: LucideIcon = checkIn ? LogIn : LogOut
    return (
      <li className="relative min-h-7">
        <Bullet>
          <Icon strokeWidth={1.75} aria-hidden />
        </Bullet>
        <p className="pt-1 text-sm">
          {t(checkIn ? 'timeline.checkIn' : 'timeline.checkOut', { name: event.lodging.name })}
        </p>
      </li>
    )
  }

  const segment = event.segment
  if (!segment) return null
  const title = segmentTitle(segment)
  const text =
    event.kind === 'SEGMENT_DEPARTURE'
      ? t('timeline.departure', { segment: title })
      : event.kind === 'SEGMENT_ARRIVAL'
        ? t('timeline.arrival', { segment: title })
        : title
  const arrival =
    event.kind === 'SEGMENT_DEPARTURE' && segment.arriveAt && segment.arriveInstant
      ? t('timeline.arrivesOn', {
          date: formatLocalDateTime(segment.arriveAt).split(' ')[0],
          time: `${segment.arriveAt.slice(11, 16)} (${gmtLabel(segment.arriveInstant)})`,
        })
      : event.kind === 'SEGMENT' && segment.arriveAt && segment.arriveInstant
        ? t('timeline.arrivesAt', {
            time: `${segment.arriveAt.slice(11, 16)} (${gmtLabel(segment.arriveInstant)})`,
          })
        : undefined
  const company = [segment.carrier, segment.code].filter(Boolean).join(' ')
  const details = [arrival, company].filter(Boolean).join(' · ')

  return (
    <li className="relative min-h-7">
      <Bullet>
        <TransportIcon type={segment.type} />
      </Bullet>
      <p className="flex gap-2 pt-1 text-sm">
        {event.time && (
          <span className="shrink-0 font-medium tabular">
            {event.time}
            {event.instant && (
              <span className="ml-1 text-xs font-normal text-muted-foreground">
                {gmtLabel(event.instant)}
              </span>
            )}
          </span>
        )}
        <span className="min-w-0 break-words">{text}</span>
      </p>
      {details && <p className="text-xs text-muted-foreground tabular">{details}</p>}
    </li>
  )
}

function Overnight({ day }: { day: ItineraryDay }) {
  const { t } = useTranslation()
  const segmentTitle = useTimelineSegmentTitle()
  const { goTo } = useTripTab()
  const { overnight } = day

  if (overnight.kind === 'MISSING') {
    return (
      <div className="relative -ml-1 rounded-lg border border-dashed border-warning bg-warning-soft px-3 py-2 text-warning">
        <p className="flex items-center gap-2 text-sm font-medium">
          <TriangleAlert className="size-4 shrink-0" strokeWidth={1.75} aria-hidden />
          {t('timeline.sleepMissing')}
        </p>
        <p className="text-xs">{t('timeline.sleepMissingHint')}</p>
        <Button
          variant="link"
          className="h-auto min-h-11 px-0 text-warning"
          onClick={() => goTo('suggestions', focusKey('NOITE_SEM_HOSPEDAGEM', day.date))}
        >
          {t('timeline.seeSuggestion')}
        </Button>
      </div>
    )
  }

  let icon: React.ReactNode
  let text: string
  if (overnight.kind === 'LODGING' && overnight.lodging) {
    icon = <LodgingIcon kind={overnight.lodging.kind} />
    text = t('timeline.sleep', { place: overnight.lodging.name })
  } else if (overnight.kind === 'IN_TRANSIT' && overnight.segment) {
    icon = <MoonStar strokeWidth={1.75} aria-hidden />
    text = t('timeline.sleepInTransit', { segment: segmentTitle(overnight.segment) })
  } else {
    icon = <House strokeWidth={1.75} aria-hidden />
    text = t('timeline.sleepHome')
  }
  return (
    <div className="relative min-h-7">
      <Bullet>{icon}</Bullet>
      <p className={cn('pt-1 text-sm', overnight.kind === 'HOME' && 'text-muted-foreground')}>
        {text}
      </p>
    </div>
  )
}

/** "Voo GRU → Lisboa"; num trecho local, "Transfer Aeroporto → Hotel" ou "Transfer em Lisboa". */
function useTimelineSegmentTitle() {
  const { t } = useTranslation()
  return (segment: ItinerarySegment) => {
    const type = t(`transport.${segment.type}`)
    if (segment.local) {
      const route = [segment.origin, segment.destination].filter(Boolean).join(' → ')
      return route ? `${type} ${route}` : t('segments.localIn', { type, city: segment.localCity })
    }
    const home = t('segments.home')
    return `${type} ${segment.origin ?? home} → ${segment.destination ?? home}`
  }
}
