import { ArrowLeft, Landmark, Search, SearchX, Star, UtensilsCrossed, X } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/EmptyState'
import { Chip, ChipGroup } from '@/components/FilterChips'
import { PriceTier, type PriceLevel } from '@/components/PriceTier'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { asPriceLevel, mapsUrl } from '@/features/hotels/hotel-search'
import { useStops, type Stop } from '@/features/itinerary/api'
import { hasRole, useTrip, type Trip } from '@/features/trips/api'
import { ApiError } from '@/lib/api'
import { formatDateRange } from '@/lib/dates'
import { cn } from '@/lib/utils'
import {
  updateBody,
  usePlaceSearch,
  useSavedPlaces,
  useSavePlace,
  useUpdateSavedPlace,
  type DiscoverKind,
  type PlaceFilters,
  type PlaceResult,
  type SavedPlace,
} from './api'
import { PlaceCard, Rating } from './PlaceCard'
import { KIND_PARAM, kindFromParam } from './places-url'
import { SavedPlaceSheet } from './SavedPlaceSheet'

const KINDS: DiscoverKind[] = ['RESTAURANT', 'ATTRACTION']
const RATINGS = [4, 4.5] as const
const TIERS: PriceLevel[] = [1, 2, 3, 4]

/**
 * `/trips/:tripId/stops/:stopId/places?kind=restaurants|attractions[&discover=1]`: seção "Lugares"
 * da parada (ESP §3.2). Abas Restaurantes/Atrações com os favoritos salvos e "Descobrir", que busca
 * no Google Places perto da hospedagem (ou do centro da cidade) e permite salvar (editor).
 */
export function PlacesPage() {
  const { t } = useTranslation()
  const params = useParams()
  const tripId = Number(params.tripId)
  const stopId = Number(params.stopId)
  const { data: trip, error } = useTrip(tripId)
  const { data: stops, error: stopsError } = useStops(tripId)
  const stop = stops?.find((s) => s.id === stopId)

  if (error || stopsError || (stops && !stop)) {
    return <EmptyState icon={SearchX} message={t('places.notFound')} />
  }
  if (!trip || !stop) {
    return <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
  }
  return <Places trip={trip} stop={stop} />
}

function Places({ trip, stop }: { trip: Trip; stop: Stop }) {
  const { t } = useTranslation()
  const [searchParams, setSearchParams] = useSearchParams()
  const kind = kindFromParam(searchParams.get('kind'))
  const discover = searchParams.get('discover') === '1'
  const canEdit = hasRole(trip.role, 'EDITOR')
  const saved = useSavedPlaces(trip.id, stop.id)
  const [editing, setEditing] = useState<SavedPlace>()

  function go(next: { kind?: DiscoverKind; discover?: boolean }) {
    const params = new URLSearchParams({ kind: KIND_PARAM[next.kind ?? kind] })
    if (next.discover ?? discover) params.set('discover', '1')
    setSearchParams(params, { replace: true })
  }

  const all = saved.data ?? []
  const ofKind = all.filter((p) => p.kind === kind)
  const others = all.filter((p) => p.kind === 'OTHER')

  return (
    <div className="flex flex-col gap-4">
      <div>
        <Button asChild variant="ghost" size="sm" className="-ml-2 text-muted-foreground">
          <Link to={`/trips/${trip.id}?tab=itinerary`}>
            <ArrowLeft strokeWidth={2} aria-hidden />
            {t('hotels.back', { trip: trip.name })}
          </Link>
        </Button>
        <h1 className="text-2xl font-bold lg:text-[1.75rem]">
          {t('places.title', { city: stop.cityName })}
        </h1>
        <p className="text-sm text-muted-foreground tabular">
          {formatDateRange(stop.arrivalDate, stop.departureDate)}
        </p>
      </div>

      <Tabs value={kind} onValueChange={(v) => go({ kind: v as DiscoverKind })}>
        <TabsList className="grid h-11 w-full grid-cols-2 dark:bg-background">
          {KINDS.map((k) => {
            const Icon = k === 'RESTAURANT' ? UtensilsCrossed : Landmark
            const count = all.filter((p) => p.kind === k).length
            return (
              <TabsTrigger key={k} value={k} className="h-full">
                <Icon strokeWidth={1.75} aria-hidden />
                {t(`places.tabs.${k}`)}
                {count > 0 && <span className="text-muted-foreground tabular">{count}</span>}
              </TabsTrigger>
            )
          })}
        </TabsList>
      </Tabs>

      <Card>
        <CardContent className="flex flex-col gap-3">
          <div className="flex items-center justify-between gap-2">
            <h2 className="font-semibold">{t(`places.savedTitle.${kind}`)}</h2>
            {!discover && (
              <Button variant="outline" onClick={() => go({ discover: true })}>
                <Search strokeWidth={1.75} aria-hidden />
                {t('places.discover')}
              </Button>
            )}
          </div>
          {saved.isPending && <p className="text-muted-foreground">{t('common.loading')}</p>}
          {saved.isError && <p className="text-destructive">{t('places.loadError')}</p>}
          {saved.isSuccess && ofKind.length === 0 && (
            <p className="text-sm text-muted-foreground">{t(`places.emptySaved.${kind}`)}</p>
          )}
          {ofKind.length > 0 && (
            <SavedList
              trip={trip}
              stop={stop}
              places={ofKind}
              label={t(`places.savedTitle.${kind}`)}
              onEdit={canEdit ? setEditing : undefined}
            />
          )}
          {others.length > 0 && (
            <>
              <h3 className="pt-2 text-sm font-medium text-muted-foreground">
                {t('places.othersTitle')}
              </h3>
              <SavedList
                trip={trip}
                stop={stop}
                places={others}
                label={t('places.othersTitle')}
                onEdit={canEdit ? setEditing : undefined}
              />
            </>
          )}
        </CardContent>
      </Card>

      {discover && (
        <Discover
          key={kind}
          trip={trip}
          stop={stop}
          kind={kind}
          saved={all}
          canEdit={canEdit}
          onClose={() => go({ discover: false })}
        />
      )}

      {canEdit && (
        <SavedPlaceSheet
          tripId={trip.id}
          stopId={stop.id}
          place={editing}
          onClose={() => setEditing(undefined)}
        />
      )}
    </div>
  )
}

function SavedList({
  trip,
  stop,
  places,
  label,
  onEdit,
}: {
  trip: Trip
  stop: Stop
  places: SavedPlace[]
  label: string
  onEdit?: (place: SavedPlace) => void
}) {
  const { t } = useTranslation()
  const update = useUpdateSavedPlace(trip.id, stop.id)

  function toggleVisited(place: SavedPlace, visited: boolean) {
    update.mutate(
      { id: place.id, body: updateBody(place, { visited }) },
      { onError: () => toast.error(t('places.saveError')) },
    )
  }

  return (
    <ul aria-label={label} className="flex flex-col divide-y">
      {places.map((place) => {
        const tier = asPriceLevel(place.priceTier)
        const checkboxId = `visited-${place.id}`
        return (
          <li key={place.id} className="flex items-start gap-3 py-3">
            {onEdit && (
              <Checkbox
                id={checkboxId}
                className="mt-0.5 size-5"
                checked={place.visited}
                disabled={update.isPending}
                aria-label={t('places.markVisited', { name: place.name })}
                onCheckedChange={(checked) => toggleVisited(place, checked === true)}
              />
            )}
            <div className="flex min-w-0 flex-1 flex-col gap-1">
              {onEdit ? (
                <button
                  type="button"
                  className="-mx-1 rounded px-1 text-left font-medium hover:underline focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
                  aria-label={t('places.edit', { name: place.name })}
                  onClick={() => onEdit(place)}
                >
                  <span className={cn(place.visited && 'text-muted-foreground line-through')}>
                    {place.name}
                  </span>
                </button>
              ) : (
                <span
                  className={cn(
                    'font-medium',
                    place.visited && 'text-muted-foreground line-through',
                  )}
                >
                  {place.name}
                </span>
              )}
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
                {place.rating !== undefined && (
                  <Rating rating={place.rating} count={place.userRatingCount} />
                )}
                {tier && (
                  <PriceTier level={tier} estimated={place.priceTierSource === 'HEURISTIC'} />
                )}
                {!onEdit && place.visited && (
                  <span className="text-success">{t('places.visitedBadge')}</span>
                )}
                <a
                  href={mapsUrl(place)}
                  target="_blank"
                  rel="noreferrer"
                  className="text-primary underline-offset-4 hover:underline"
                >
                  {t('hotels.viewOnMaps')}
                </a>
              </div>
              {place.notes && (
                <p className="line-clamp-3 text-sm whitespace-pre-line text-muted-foreground">
                  {place.notes}
                </p>
              )}
            </div>
          </li>
        )
      })}
    </ul>
  )
}

function Discover({
  trip,
  stop,
  kind,
  saved,
  canEdit,
  onClose,
}: {
  trip: Trip
  stop: Stop
  kind: DiscoverKind
  saved: SavedPlace[]
  canEdit: boolean
  onClose: () => void
}) {
  const { t } = useTranslation()
  const [filters, setFilters] = useState<PlaceFilters>({ tiers: [] })
  const search = usePlaceSearch(trip.id, stop.id, kind, filters, true)
  const save = useSavePlace(trip.id, stop.id)
  const [savingId, setSavingId] = useState<string>()
  const savedIds = useMemo(() => new Set(saved.map((p) => p.placeId)), [saved])
  const unavailable = search.error instanceof ApiError && search.error.status === 503
  const data = search.data
  const center =
    data?.centerLat != null && data.centerLng != null
      ? { lat: data.centerLat, lng: data.centerLng }
      : undefined
  const centerLabel =
    data?.center === 'LODGING' && data.centerLodgingName
      ? data.centerLodgingName
      : t('places.cityCenter')

  function toggleTier(tier: PriceLevel) {
    setFilters((f) => ({
      ...f,
      tiers: f.tiers.includes(tier) ? f.tiers.filter((x) => x !== tier) : [...f.tiers, tier],
    }))
  }

  function onSave(result: PlaceResult) {
    setSavingId(result.placeId)
    save.mutate(
      {
        placeId: result.placeId,
        kind,
        name: result.name.slice(0, 160),
        priceTier: result.priceTier,
      },
      {
        onSuccess: () => toast.success(t('places.savedToast', { name: result.name })),
        onError: (error) =>
          toast.error(
            t(
              error instanceof ApiError && error.status === 409
                ? 'places.alreadySaved'
                : 'places.saveError',
            ),
          ),
        onSettled: () => setSavingId(undefined),
      },
    )
  }

  return (
    <section aria-labelledby="discover-title" className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-2">
        <h2 id="discover-title" className="text-lg font-semibold">
          {t(`places.discoverTitle.${kind}`, { city: stop.cityName })}
        </h2>
        <Button
          variant="ghost"
          size="icon"
          aria-label={t('places.closeDiscover')}
          onClick={onClose}
        >
          <X strokeWidth={1.75} />
        </Button>
      </div>
      <div className="flex flex-col gap-2">
        <ChipGroup label={t('hotels.filters.rating')}>
          {RATINGS.map((rating) => (
            <Chip
              key={rating}
              pressed={filters.minRating === rating}
              onClick={() =>
                setFilters((f) => ({
                  ...f,
                  minRating: f.minRating === rating ? undefined : rating,
                }))
              }
            >
              {t('hotels.minRating', { rating: rating.toLocaleString('pt-BR') })}
            </Chip>
          ))}
        </ChipGroup>
        {kind === 'RESTAURANT' && (
          <ChipGroup label={t('hotels.filters.tier')}>
            {TIERS.map((tier) => (
              <Chip
                key={tier}
                pressed={filters.tiers.includes(tier)}
                onClick={() => toggleTier(tier)}
                aria-label={t('priceTier.label', { level: tier })}
              >
                <PriceTier level={tier} />
              </Chip>
            ))}
          </ChipGroup>
        )}
      </div>
      {data && (
        <p className="text-sm text-muted-foreground">
          {data.center === 'TEXT'
            ? t('places.centerText', { city: stop.cityName })
            : t('places.centerNear', { place: centerLabel })}
        </p>
      )}

      {search.isPending && <p className="text-muted-foreground">{t('places.searching')}</p>}
      {search.error && (
        <EmptyState
          icon={SearchX}
          message={t(unavailable ? 'places.unavailable' : 'places.searchError')}
        />
      )}
      {search.isSuccess && data && data.results.length === 0 && (
        <EmptyState icon={Star} message={t('places.empty')} />
      )}
      {data && data.results.length > 0 && (
        <ul aria-label={t('hotels.resultsLabel')} className="grid gap-3 md:grid-cols-2">
          {data.results.map((result) => (
            <li key={result.placeId}>
              <PlaceCard
                result={result}
                kind={kind}
                center={center}
                centerLabel={centerLabel}
                saved={savedIds.has(result.placeId)}
                saving={savingId === result.placeId}
                onSave={canEdit ? () => onSave(result) : undefined}
              />
            </li>
          ))}
        </ul>
      )}
      <p className="text-center text-xs text-muted-foreground">{t('hotels.tierDisclaimer')}</p>
    </section>
  )
}
