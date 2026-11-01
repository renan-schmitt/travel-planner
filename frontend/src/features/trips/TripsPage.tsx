import { Map, Plus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useTrips, type TripScope } from './api'
import { TripCard } from './TripCard'

/** Lista de viagens em abas "Próximas" / "Passadas" (a aba fica na URL: `?scope=past`). */
export function TripsPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const scope: TripScope = params.get('scope') === 'past' ? 'past' : 'upcoming'
  const { data: trips, isPending, isError } = useTrips(scope)

  return (
    <>
      <PageHeader title={t('trips.title')} />
      <Tabs
        value={scope}
        onValueChange={(value) => setParams(value === 'past' ? { scope: 'past' } : {})}
        className="mb-3"
      >
        {/* No escuro --muted é igual a --card: o fundo das abas usa --background. */}
        <TabsList className="grid h-11 w-full grid-cols-2 dark:bg-background">
          <TabsTrigger value="upcoming" className="h-full">
            {t('trips.upcoming')}
          </TabsTrigger>
          <TabsTrigger value="past" className="h-full">
            {t('trips.past')}
          </TabsTrigger>
        </TabsList>
      </Tabs>

      {isPending ? (
        <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
      ) : isError ? (
        <EmptyState icon={Map} message={t('trips.loadError')} />
      ) : trips.length === 0 ? (
        <EmptyState icon={Map} message={t(scope === 'past' ? 'trips.emptyPast' : 'trips.empty')} />
      ) : (
        // pb-20: o botão flutuante não cobre o último card.
        <ul className="flex flex-col gap-3 pb-20">
          {trips.map((trip) => (
            <li key={trip.id}>
              <TripCard trip={trip} />
            </li>
          ))}
        </ul>
      )}

      <Link
        to="/trips/new"
        className="fixed right-4 bottom-[calc(4.5rem+env(safe-area-inset-bottom))] z-20 flex h-14 items-center gap-2 rounded-full bg-primary px-5 font-medium text-primary-foreground shadow-lg transition-colors duration-150 hover:bg-primary/90 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none lg:right-8 lg:bottom-8"
      >
        <Plus className="size-5" strokeWidth={1.75} aria-hidden />
        {t('trips.new')}
      </Link>
    </>
  )
}
