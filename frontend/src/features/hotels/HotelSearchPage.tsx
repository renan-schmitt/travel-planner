import { ArrowLeft, BedDouble, Hotel, SearchX } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router'
import { EmptyState } from '@/components/EmptyState'
import { PriceTier, type PriceLevel } from '@/components/PriceTier'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { useLodgings, useStops, type Stop } from '@/features/itinerary/api'
import { emptyLodging, type LodgingFormValues } from '@/features/itinerary/lodging-form'
import { LodgingSheet } from '@/features/itinerary/LodgingSheet'
import { useTripTravelers } from '@/features/travelers/api'
import { hasRole, useTrip, type Trip } from '@/features/trips/api'
import { ApiError } from '@/lib/api'
import { formatDateRange } from '@/lib/dates'
import { cn } from '@/lib/utils'
import { useHotelSearch, type HotelFilters, type HotelResult, type HotelType } from './api'
import { HotelCard } from './HotelCard'
import { asPriceLevel, guestCounts, HOTEL_SORTS, sortHotels, type HotelSort } from './hotel-search'

const TYPES: HotelType[] = ['HOTEL', 'RESORT', 'ANY']
const RATINGS = [4, 4.5] as const
const TIERS: PriceLevel[] = [1, 2, 3, 4]

/**
 * `/trips/:tripId/stops/:stopId/hotels`: busca de hotéis/resorts da parada no Google Places (ESP
 * §3.1), com filtros em chips, ordenação no cliente e "Adicionar à viagem" (editor), que abre o
 * formulário de hospedagem já preenchido com o lugar e as datas da parada.
 */
export function HotelSearchPage() {
  const { t } = useTranslation()
  const params = useParams()
  const tripId = Number(params.tripId)
  const stopId = Number(params.stopId)
  const { data: trip, error } = useTrip(tripId)
  const { data: stops, error: stopsError } = useStops(tripId)
  const stop = stops?.find((s) => s.id === stopId)

  if (error || stopsError || (stops && !stop)) {
    return <EmptyState icon={SearchX} message={t('hotels.notFound')} />
  }
  if (!trip || !stop) {
    return <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
  }
  return <HotelSearch trip={trip} stop={stop} />
}

function HotelSearch({ trip, stop }: { trip: Trip; stop: Stop }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const canEdit = hasRole(trip.role, 'EDITOR')
  const [filters, setFilters] = useState<HotelFilters>({ type: 'HOTEL', tiers: [] })
  const [sort, setSort] = useState<HotelSort>('relevance')
  const search = useHotelSearch(trip.id, stop.id, filters)
  const { data: lodgings = [] } = useLodgings(trip.id)
  const { data: travelers = [] } = useTripTravelers(trip.id)
  const [adding, setAdding] = useState<HotelResult>()
  const back = `/trips/${trip.id}?tab=itinerary`

  // As datas do "Ver preços" e do formulário: a primeira noite ainda sem hospedagem até a saída.
  const dates = useMemo(() => emptyLodging(stop, lodgings), [stop, lodgings])
  const guests = useMemo(() => guestCounts(travelers), [travelers])
  const results = useMemo(
    () => sortHotels(search.data?.pages.flatMap((p) => p.results) ?? [], sort),
    [search.data, sort],
  )
  const unavailable = search.error instanceof ApiError && search.error.status === 503

  const preset = (result: HotelResult): Partial<LodgingFormValues> => ({
    kind: 'HOTEL',
    name: result.name.slice(0, 160),
    address: (result.address ?? '').slice(0, 300),
    priceTier: asPriceLevel(result.priceTier) ?? null,
  })

  function toggleTier(tier: PriceLevel) {
    setFilters((f) => ({
      ...f,
      tiers: f.tiers.includes(tier) ? f.tiers.filter((x) => x !== tier) : [...f.tiers, tier],
    }))
  }

  return (
    <div className="flex flex-col gap-4">
      <div>
        <Button asChild variant="ghost" size="sm" className="-ml-2 text-muted-foreground">
          <Link to={back}>
            <ArrowLeft strokeWidth={2} aria-hidden />
            {t('hotels.back', { trip: trip.name })}
          </Link>
        </Button>
        <h1 className="text-2xl font-bold lg:text-[1.75rem]">
          {t('hotels.title', { city: stop.cityName })}
        </h1>
        <p className="text-sm text-muted-foreground tabular">
          {formatDateRange(stop.arrivalDate, stop.departureDate)}
          {' · '}
          {t('stops.nights', { count: stop.nights })}
        </p>
      </div>

      <div className="flex flex-col gap-2">
        <ChipGroup label={t('hotels.filters.type')}>
          {TYPES.map((type) => (
            <Chip
              key={type}
              pressed={filters.type === type}
              onClick={() => setFilters((f) => ({ ...f, type }))}
            >
              {t(`hotels.types.${type}`)}
            </Chip>
          ))}
        </ChipGroup>
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
        <div className="flex items-center gap-2">
          <Label htmlFor="hotel-sort" className="shrink-0 text-sm text-muted-foreground">
            {t('hotels.sortLabel')}
          </Label>
          <div className="min-w-0 flex-1 sm:max-w-56">
            <NativeSelect
              id="hotel-sort"
              value={sort}
              onChange={(e) => setSort(e.target.value as HotelSort)}
            >
              {HOTEL_SORTS.map((s) => (
                <option key={s} value={s}>
                  {t(`hotels.sort.${s}`)}
                </option>
              ))}
            </NativeSelect>
          </div>
        </div>
      </div>

      {search.isPending && <p className="text-muted-foreground">{t('hotels.searching')}</p>}
      {search.error && (
        <EmptyState
          icon={SearchX}
          message={t(unavailable ? 'hotels.unavailable' : 'hotels.loadError')}
        />
      )}
      {search.isSuccess && results.length === 0 && (
        <EmptyState icon={Hotel} message={t('hotels.empty')} />
      )}
      {results.length > 0 && (
        <ul aria-label={t('hotels.resultsLabel')} className="grid gap-3 md:grid-cols-2">
          {results.map((result) => (
            <li key={result.placeId}>
              <HotelCard
                result={result}
                stop={stop}
                dates={dates}
                guests={guests}
                onAdd={canEdit && stop.nights > 0 ? () => setAdding(result) : undefined}
              />
            </li>
          ))}
        </ul>
      )}
      {search.hasNextPage && (
        <Button
          variant="outline"
          className="self-center"
          disabled={search.isFetchingNextPage}
          onClick={() => void search.fetchNextPage()}
        >
          {t(search.isFetchingNextPage ? 'hotels.loadingMore' : 'hotels.loadMore')}
        </Button>
      )}
      <p className="flex items-center justify-center gap-1.5 text-xs text-muted-foreground">
        <BedDouble className="size-3.5" strokeWidth={1.75} aria-hidden />
        {t('hotels.tierDisclaimer')}
      </p>

      {canEdit && (
        <LodgingSheet
          tripId={trip.id}
          stop={adding ? stop : undefined}
          lodgings={lodgings}
          preset={adding ? preset(adding) : undefined}
          placeId={adding?.placeId}
          open={adding !== undefined}
          onOpenChange={(open) => !open && setAdding(undefined)}
          onCreated={() => void navigate(back)}
        />
      )}
    </div>
  )
}

function ChipGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div role="group" aria-label={label} className="flex flex-wrap items-center gap-2">
      {children}
    </div>
  )
}

function Chip({
  pressed,
  className,
  ...props
}: React.ComponentProps<typeof Button> & { pressed: boolean }) {
  return (
    <Button
      type="button"
      size="sm"
      variant={pressed ? 'default' : 'outline'}
      aria-pressed={pressed}
      className={cn(
        'rounded-full',
        pressed &&
          '[&_.text-border]:text-primary-foreground/40 [&_.text-foreground]:text-primary-foreground',
        className,
      )}
      {...props}
    />
  )
}
