import {
  closestCenter,
  DndContext,
  MouseSensor,
  TouchSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core'
import { restrictToVerticalAxis } from '@dnd-kit/modifiers'
import {
  arrayMove,
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { BedDouble, GripVertical, House, MapPinPlus, Route, Search, Star } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useMe } from '@/features/auth/api'
import { useTripSavedPlaces } from '@/features/places/api'
import { placesPath } from '@/features/places/places-url'
import { hasRole, type Trip } from '@/features/trips/api'
import { countryFlag } from '@/lib/countries'
import { formatDateRange } from '@/lib/dates'
import { timeZoneLabel } from '@/lib/timezones'
import { cn } from '@/lib/utils'
import {
  useLodgings,
  useReorderStops,
  useSegments,
  useStops,
  type Lodging,
  type Segment,
  type Stop,
} from './api'
import { LodgingSheet } from './LodgingSheet'
import { LodgingSummary } from './LodgingSummary'
import { browserTimeZone } from './segment-form'
import { SegmentSheet } from './SegmentSheet'
import { useSegmentTitle } from './segment-title'
import { SegmentSummary } from './SegmentSummary'
import { StopSheet } from './StopSheet'

/**
 * Aba "Roteiro": paradas na ordem da viagem, cada uma com as suas hospedagens, e os trechos de
 * transporte intercalados (casa → ✈ → Lisboa → 🚆 → Porto → ✈ → casa). Os trechos que saem de casa vêm antes da primeira parada; os
 * locais e os que saem de uma parada, logo abaixo dela. Editor adiciona/edita em bottom sheet e
 * reordena as paradas arrastando o puxador (toque longo no celular) ou com as setas ↑/↓ nele.
 * Leitor só vê o roteiro.
 */
export function ItineraryTab({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  const canEdit = hasRole(trip.role, 'EDITOR')
  const { data: stops, error } = useStops(trip.id)
  const { data: segments = [] } = useSegments(trip.id)
  const { data: lodgings = [] } = useLodgings(trip.id)
  const { data: savedPlaces = [] } = useTripSavedPlaces(trip.id)
  const { data: me } = useMe()
  const homeTz = me?.homeCity?.timezone ?? browserTimeZone()
  const reorder = useReorderStops(trip.id)
  // `editing` undefined = sheet fechado; null = nova parada.
  const [editing, setEditing] = useState<Stop | null>()
  const [editingSegment, setEditingSegment] = useState<Segment | null>()
  // Sheet de hospedagem: a parada e, ao editar, a hospedagem.
  const [editingLodging, setEditingLodging] = useState<{ stop: Stop; lodging?: Lodging }>()
  const fromHome = segments.filter((s) => !s.local && s.originStopId == null)
  const toHome = segments.some((s) => !s.local && s.destinationStopId == null)
  const homeName = me?.homeCity?.name

  const segmentRows = (list: Segment[]) =>
    list.length > 0 && (
      <div className="ml-4 flex flex-col gap-1 border-l-2 border-dashed border-border py-1 pl-3">
        {list.map((segment) => (
          <SegmentRow
            key={segment.id}
            segment={segment}
            stops={stops ?? []}
            onEdit={canEdit ? () => setEditingSegment(segment) : undefined}
          />
        ))}
      </div>
    )

  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 5 } }),
    // Toque longo: o arrasto rápido continua rolando a página.
    useSensor(TouchSensor, { activationConstraint: { delay: 200, tolerance: 8 } }),
  )

  function reorderTo(from: number, to: number) {
    if (!stops || from === to || to < 0 || to >= stops.length || reorder.isPending) return
    const ids = arrayMove(
      stops.map((s) => s.id),
      from,
      to,
    )
    reorder.mutate(ids, { onError: () => toast.error(t('stops.reorderError')) })
  }

  function onDragEnd({ active, over }: DragEndEvent) {
    if (!stops || !over) return
    reorderTo(
      stops.findIndex((s) => s.id === active.id),
      stops.findIndex((s) => s.id === over.id),
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('stops.title')}</CardTitle>
        <CardDescription>{t(canEdit ? 'stops.hintEditor' : 'stops.hintViewer')}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {error && <p className="text-sm text-destructive">{t('stops.loadError')}</p>}
        {!stops && !error && <p className="text-muted-foreground">{t('common.loading')}</p>}
        {stops?.length === 0 && <p className="text-muted-foreground">{t('stops.empty')}</p>}
        {stops && fromHome.length > 0 && (
          <div>
            <HomeNode name={homeName} />
            {segmentRows(fromHome)}
          </div>
        )}
        {stops && stops.length > 0 && (
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            modifiers={[restrictToVerticalAxis]}
            onDragEnd={onDragEnd}
          >
            <SortableContext items={stops.map((s) => s.id)} strategy={verticalListSortingStrategy}>
              <ol aria-label={t('stops.title')} className="flex flex-col divide-y">
                {stops.map((stop, i) => (
                  <SortableStop
                    key={stop.id}
                    stop={stop}
                    draggable={canEdit && stops.length > 1}
                    onMove={(delta) => reorderTo(i, i + delta)}
                  >
                    <div className="flex items-center gap-2 py-1">
                      {canEdit ? (
                        <button
                          type="button"
                          className="-mx-2 min-w-0 flex-1 rounded-lg px-2 py-1 text-left hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
                          aria-label={t('stops.editStop', { city: stop.cityName })}
                          onClick={() => setEditing(stop)}
                        >
                          <StopSummary stop={stop} />
                        </button>
                      ) : (
                        <div className="min-w-0 flex-1">
                          <StopSummary stop={stop} />
                        </div>
                      )}
                    </div>
                    <StopPlacesLink
                      tripId={trip.id}
                      stop={stop}
                      count={savedPlaces.filter((p) => p.stopId === stop.id).length}
                    />
                    <StopLodgings
                      tripId={trip.id}
                      stop={stop}
                      lodgings={lodgings.filter((l) => l.stopId === stop.id)}
                      onAdd={canEdit ? () => setEditingLodging({ stop }) : undefined}
                      onEdit={
                        canEdit ? (lodging) => setEditingLodging({ stop, lodging }) : undefined
                      }
                    />
                    {segmentRows(
                      segments.filter(
                        (s) => s.originStopId === stop.id || s.localStopId === stop.id,
                      ),
                    )}
                  </SortableStop>
                ))}
              </ol>
            </SortableContext>
          </DndContext>
        )}
        {stops && stops.length > 0 && toHome && <HomeNode name={homeName} />}
        {canEdit && stops && (
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => setEditing(null)}>
              <MapPinPlus strokeWidth={1.75} aria-hidden />
              {t('stops.add')}
            </Button>
            {stops.length > 0 && (
              <Button variant="outline" onClick={() => setEditingSegment(null)}>
                <Route strokeWidth={1.75} aria-hidden />
                {t('segments.add')}
              </Button>
            )}
          </div>
        )}
        {canEdit && stops && (
          <SegmentSheet
            tripId={trip.id}
            stops={stops}
            segments={segments}
            segment={editingSegment ?? undefined}
            homeTz={homeTz}
            open={editingSegment !== undefined}
            onOpenChange={(open) => !open && setEditingSegment(undefined)}
          />
        )}
        {canEdit && (
          <LodgingSheet
            tripId={trip.id}
            stop={editingLodging?.stop}
            lodgings={lodgings}
            lodging={editingLodging?.lodging}
            open={editingLodging !== undefined}
            onOpenChange={(open) => !open && setEditingLodging(undefined)}
          />
        )}
        {canEdit && stops && (
          <StopSheet
            trip={trip}
            stops={stops}
            lodgings={lodgings}
            stop={editing ?? undefined}
            open={editing !== undefined}
            onOpenChange={(open) => !open && setEditing(undefined)}
          />
        )}
      </CardContent>
    </Card>
  )
}

function HomeNode({ name }: { name?: string }) {
  const { t } = useTranslation()
  return (
    <div className="flex items-center gap-3 py-1">
      <span className="flex size-8 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <House className="size-4" strokeWidth={1.75} aria-hidden />
      </span>
      <p className="font-medium">
        {t('segments.home')}
        {name && <span className="font-normal text-muted-foreground">{` · ${name}`}</span>}
      </p>
    </div>
  )
}

/**
 * Hospedagens da parada, por check-in. Editor toca para editar e adiciona outra (ex.: dois hotéis
 * na mesma cidade); bate-volta (0 noite) não tem hospedagem. "Buscar hotéis" leva à busca no
 * Places (também para o leitor, que só não adiciona).
 */
function StopLodgings({
  tripId,
  stop,
  lodgings,
  onAdd,
  onEdit,
}: {
  tripId: number
  stop: Stop
  lodgings: Lodging[]
  onAdd?: () => void
  onEdit?: (lodging: Lodging) => void
}) {
  const { t } = useTranslation()
  if (lodgings.length === 0 && stop.nights === 0) return null
  return (
    <div
      role="group"
      aria-label={t('lodgings.listLabel', { city: stop.cityName })}
      className="ml-11 flex flex-col gap-1 pb-1"
    >
      {lodgings.map((lodging) => (
        <div key={lodging.id}>
          {onEdit ? (
            <button
              type="button"
              className="-mx-2 w-full rounded-lg px-2 py-1 text-left hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
              aria-label={t('lodgings.edit', { name: lodging.name })}
              onClick={() => onEdit(lodging)}
            >
              <LodgingSummary lodging={lodging} />
            </button>
          ) : (
            <div className="py-1">
              <LodgingSummary lodging={lodging} />
            </div>
          )}
        </div>
      ))}
      {stop.nights > 0 && (
        <div className="-mx-2 flex flex-wrap gap-x-1">
          {onAdd && (
            <Button
              variant="ghost"
              size="sm"
              className="text-muted-foreground"
              aria-label={t('lodgings.addIn', { city: stop.cityName })}
              onClick={onAdd}
            >
              <BedDouble strokeWidth={1.75} aria-hidden />
              {t(lodgings.length === 0 ? 'lodgings.add' : 'lodgings.addAnother')}
            </Button>
          )}
          <Button asChild variant="ghost" size="sm" className="text-muted-foreground">
            <Link
              to={`/trips/${tripId}/stops/${stop.id}/hotels`}
              aria-label={t('lodgings.searchIn', { city: stop.cityName })}
            >
              <Search strokeWidth={1.75} aria-hidden />
              {t('lodgings.search')}
            </Link>
          </Button>
        </div>
      )}
    </div>
  )
}

/** Atalho para a seção "Lugares" da parada (restaurantes e atrações), com o nº de favoritos. */
function StopPlacesLink({ tripId, stop, count }: { tripId: number; stop: Stop; count: number }) {
  const { t } = useTranslation()
  return (
    <div className="-mx-2 ml-9 pb-1">
      <Button asChild variant="ghost" size="sm" className="text-muted-foreground">
        <Link
          to={placesPath(tripId, stop.id)}
          aria-label={t('places.openIn', { city: stop.cityName, count })}
        >
          <Star strokeWidth={1.75} aria-hidden />
          {count > 0 ? t('places.linkWithCount', { count }) : t('places.link')}
        </Link>
      </Button>
    </div>
  )
}

function SegmentRow({
  segment,
  stops,
  onEdit,
}: {
  segment: Segment
  stops: Stop[]
  onEdit?: () => void
}) {
  const { t } = useTranslation()
  const title = useSegmentTitle(segment, stops)
  if (!onEdit) {
    return (
      <div className="py-1">
        <SegmentSummary segment={segment} stops={stops} />
      </div>
    )
  }
  return (
    <button
      type="button"
      className="rounded-lg px-2 py-1 text-left hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
      aria-label={t('segments.edit', { segment: title })}
      onClick={onEdit}
    >
      <SegmentSummary segment={segment} stops={stops} />
    </button>
  )
}

/**
 * Parada arrastável pelo puxador à direita do cabeçalho (só para editor com 2+ paradas). As setas
 * ↑/↓ no puxador movem uma posição.
 */
function SortableStop({
  stop,
  draggable,
  onMove,
  children,
}: {
  stop: Stop
  draggable: boolean
  onMove: (delta: -1 | 1) => void
  children: React.ReactNode
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
  } = useSortable({ id: stop.id, disabled: !draggable })
  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      className={cn(
        'relative bg-card py-2',
        draggable && 'pr-12',
        isDragging && 'z-10 rounded-lg shadow-lg',
      )}
    >
      {children}
      {draggable && (
        <button
          type="button"
          ref={setActivatorNodeRef}
          {...attributes}
          {...listeners}
          aria-roledescription={undefined}
          aria-describedby={undefined}
          aria-label={t('stops.dragHandle', { city: stop.cityName })}
          onKeyDown={(e) => {
            if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
              e.preventDefault()
              onMove(e.key === 'ArrowUp' ? -1 : 1)
            }
          }}
          className="absolute top-3 right-0 flex size-11 cursor-grab touch-manipulation items-center justify-center rounded-lg text-muted-foreground select-none [-webkit-touch-callout:none] hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none active:cursor-grabbing"
        >
          <GripVertical className="size-5" aria-hidden />
        </button>
      )}
    </li>
  )
}

function StopSummary({ stop }: { stop: Stop }) {
  const { t, i18n } = useTranslation()
  const country = useMemo(
    () => new Intl.DisplayNames([i18n.language], { type: 'region' }).of(stop.countryCode),
    [i18n.language, stop.countryCode],
  )
  return (
    <div className="flex min-w-0 items-start gap-3">
      <span className="text-2xl leading-none" aria-hidden>
        {countryFlag(stop.countryCode)}
      </span>
      <div className="flex min-w-0 flex-col gap-0.5">
        <p className="truncate font-medium">
          {stop.cityName}
          <span className="font-normal text-muted-foreground">{country ? `, ${country}` : ''}</span>
        </p>
        <p className="text-sm tabular">
          {formatDateRange(stop.arrivalDate, stop.departureDate)}
          {' · '}
          {stop.nights === 0 ? t('stops.dayTrip') : t('stops.nights', { count: stop.nights })}
        </p>
        <p className="truncate text-xs text-muted-foreground">
          {t('stops.timezone', { zone: timeZoneLabel(stop.timezone) })}
        </p>
        {stop.notes && (
          <p className="line-clamp-2 text-sm whitespace-pre-line text-muted-foreground">
            {stop.notes}
          </p>
        )}
      </div>
    </div>
  )
}
