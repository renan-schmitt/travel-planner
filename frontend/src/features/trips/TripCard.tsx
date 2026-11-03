import { CalendarDays, MapPin, Sparkles, UsersRound } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Card } from '@/components/ui/card'
import { daysUntil } from '@/lib/dates'
import type { TripSummary } from './api'
import { defaultTripColor } from './trip-colors'
import { TripDates } from './TripDates'
import { TripStatusBadge } from './TripStatusBadge'

/** Card da lista (ESP §13.7): faixa com a cor da viagem e contagem regressiva em destaque. */
export function TripCard({ trip }: { trip: TripSummary }) {
  const { t } = useTranslation()
  const days = trip.startDate && trip.status !== 'DONE' ? daysUntil(trip.startDate) : undefined

  return (
    <Link
      to={`/trips/${trip.id}`}
      className="block rounded-xl transition-transform duration-150 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none active:scale-[0.99] motion-reduce:transition-none"
    >
      <Card className="gap-3 overflow-hidden pt-0">
        <div className="h-2" style={{ backgroundColor: trip.coverColor ?? defaultTripColor }} />
        <div className="flex flex-col gap-2 px-4">
          <div className="flex items-start justify-between gap-3">
            <h2 className="font-heading text-lg leading-snug font-bold">{trip.name}</h2>
            <TripStatusBadge status={trip.status} />
          </div>
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <CalendarDays className="size-4 shrink-0" strokeWidth={1.75} aria-hidden />
            <TripDates startDate={trip.startDate} endDate={trip.endDate} />
          </p>
          {trip.destinationCount > 0 && (
            <p className="flex items-center gap-2 text-sm text-muted-foreground">
              <MapPin className="size-4 shrink-0" strokeWidth={1.75} aria-hidden />
              {t('trips.destinations', { count: trip.destinationCount })}
            </p>
          )}
          {trip.pendingSuggestions > 0 && (
            <p className="flex items-center gap-2 text-sm text-warning-foreground">
              <Sparkles className="size-4 shrink-0" strokeWidth={1.75} aria-hidden />
              {t('trips.pendingSuggestions', { count: trip.pendingSuggestions })}
            </p>
          )}
          <div className="flex items-center justify-between gap-3 text-sm">
            {days !== undefined && days >= 0 ? (
              <span className="font-heading font-bold text-highlight tabular">
                {days === 0 ? t('trips.startsToday') : t('trips.countdown', { count: days })}
              </span>
            ) : (
              <span />
            )}
            {trip.role !== 'OWNER' && (
              <span className="flex items-center gap-1.5 text-muted-foreground">
                <UsersRound className="size-4" strokeWidth={1.75} aria-hidden />
                {t(`trips.role.${trip.role}`)}
              </span>
            )}
          </div>
        </div>
      </Card>
    </Link>
  )
}
