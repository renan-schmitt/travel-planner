import {
  CalendarCheck,
  CalendarDays,
  ChevronDown,
  CircleAlert,
  History,
  MapPin,
  OctagonAlert,
  Plus,
} from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { categoryIcons } from '@/features/checklist/categories'
import { focusKey } from '@/features/suggestions/resolve'
import { suggestionText } from '@/features/suggestions/suggestion-text'
import { defaultTripColor } from '@/features/trips/trip-colors'
import { TripDates } from '@/features/trips/TripDates'
import { TripStatusBadge } from '@/features/trips/TripStatusBadge'
import { daysUntil } from '@/lib/dates'
import { cn } from '@/lib/utils'
import { useDashboard, type DashboardAlert, type PastTrip, type UpcomingTrip } from './api'

const linkClass =
  'block rounded-xl transition-transform duration-150 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none active:scale-[0.99] motion-reduce:transition-none'

/** Tela inicial (ESP §6.7): alertas com prazo no topo, próximas viagens e o histórico. */
export function DashboardPage() {
  const { t } = useTranslation()
  const { data, isPending, isError } = useDashboard()

  if (isPending) {
    return (
      <>
        <PageHeader title={t('dashboard.title')} />
        <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
      </>
    )
  }
  if (isError) {
    return (
      <>
        <PageHeader title={t('dashboard.title')} />
        <EmptyState icon={CalendarCheck} message={t('dashboard.loadError')} />
      </>
    )
  }

  const { upcomingTrips, alerts, pastTrips } = data
  const noTrips = upcomingTrips.length === 0 && pastTrips.length === 0

  return (
    <>
      <PageHeader title={t('dashboard.title')} />
      {noTrips ? (
        <FirstTrip />
      ) : (
        <div className="flex flex-col gap-6 pb-6">
          {upcomingTrips.length > 0 && <Alerts alerts={alerts} />}
          <section aria-labelledby="dashboard-upcoming" className="flex flex-col gap-3">
            <div className="flex items-center justify-between gap-3">
              <h2 id="dashboard-upcoming" className="font-heading text-lg font-bold">
                {t('dashboard.upcoming')}
              </h2>
              <Link
                to="/trips/new"
                className="flex h-11 items-center gap-1.5 rounded-md px-2 text-sm font-medium text-primary hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
              >
                <Plus className="size-4" strokeWidth={1.75} aria-hidden />
                {t('dashboard.newTrip')}
              </Link>
            </div>
            {upcomingTrips.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t('dashboard.noUpcoming')}</p>
            ) : (
              <ul className="flex flex-col gap-3">
                {upcomingTrips.map((trip) => (
                  <li key={trip.id}>
                    <UpcomingTripCard trip={trip} />
                  </li>
                ))}
              </ul>
            )}
          </section>
          {pastTrips.length > 0 && <PastTrips trips={pastTrips} />}
        </div>
      )}
    </>
  )
}

function FirstTrip() {
  const { t } = useTranslation()
  return (
    <Card>
      <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
        <span className="flex size-14 items-center justify-center rounded-full bg-accent text-accent-foreground">
          <MapPin className="size-7" strokeWidth={1.75} aria-hidden />
        </span>
        <h2 className="font-heading text-xl font-bold">{t('dashboard.emptyTitle')}</h2>
        <p className="max-w-sm text-muted-foreground">{t('dashboard.empty')}</p>
        <Link
          to="/trips/new"
          className="mt-2 flex h-11 items-center gap-2 rounded-full bg-primary px-5 font-medium text-primary-foreground transition-colors duration-150 hover:bg-primary/90 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
        >
          <Plus className="size-5" strokeWidth={1.75} aria-hidden />
          {t('dashboard.newTrip')}
        </Link>
      </CardContent>
    </Card>
  )
}

function Alerts({ alerts }: { alerts: DashboardAlert[] }) {
  const { t } = useTranslation()
  return (
    <section aria-labelledby="dashboard-alerts" className="flex flex-col gap-3">
      <h2 id="dashboard-alerts" className="font-heading text-lg font-bold">
        {t('dashboard.alerts')}
      </h2>
      {alerts.length === 0 ? (
        <p className="flex items-center gap-2 text-sm text-muted-foreground">
          <CalendarCheck className="size-4 shrink-0" strokeWidth={1.75} aria-hidden />
          {t('dashboard.noAlerts')}
        </p>
      ) : (
        <Card className="gap-0 py-0">
          <ul className="divide-y divide-border">
            {alerts.map((alert) => (
              <li key={`${alert.kind}-${alert.checklistItem?.id ?? alert.suggestion?.id}`}>
                <AlertRow alert={alert} />
              </li>
            ))}
          </ul>
        </Card>
      )}
    </section>
  )
}

function alertLink(alert: DashboardAlert): string {
  const base = `/trips/${alert.tripId}`
  if (alert.suggestion) {
    const focus = focusKey(alert.suggestion.ruleCode, alert.suggestion.logicalKey)
    return `${base}?tab=suggestions&focus=${encodeURIComponent(focus)}`
  }
  return `${base}?tab=checklist`
}

function AlertRow({ alert }: { alert: DashboardAlert }) {
  const { t } = useTranslation()
  const critical = alert.overdue || alert.severity === 'CRITICAL'
  const Icon = alert.checklistItem
    ? categoryIcons[alert.checklistItem.category]
    : alert.severity === 'CRITICAL'
      ? OctagonAlert
      : CircleAlert
  const title = alert.checklistItem
    ? alert.checklistItem.title
    : suggestionText(t, alert.suggestion!).title

  return (
    <Link
      to={alertLink(alert)}
      className="flex min-h-14 items-center gap-3 px-4 py-3 transition-colors duration-150 hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span
        className={cn(
          'flex size-9 shrink-0 items-center justify-center rounded-full',
          critical
            ? 'bg-destructive/10 text-destructive'
            : 'bg-warning-soft text-warning-foreground',
        )}
      >
        <Icon className="size-5" strokeWidth={1.75} aria-hidden />
      </span>
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="font-medium">{title}</span>
        <span className="truncate text-sm text-muted-foreground">{alert.tripName}</span>
      </span>
      {alert.checklistItem && alert.date ? (
        <DueLabel date={alert.date} />
      ) : (
        alert.severity === 'CRITICAL' && (
          <Badge variant="critical">{t('suggestions.severity.CRITICAL')}</Badge>
        )
      )}
    </Link>
  )
}

function DueLabel({ date }: { date: string }) {
  const { t } = useTranslation()
  const days = daysUntil(date)
  const label =
    days < 0
      ? t('dashboard.due.overdue', { count: -days })
      : days === 0
        ? t('dashboard.due.today')
        : days === 1
          ? t('dashboard.due.tomorrow')
          : t('dashboard.due.inDays', { count: days })
  return (
    <span
      className={cn(
        'shrink-0 text-right text-sm font-medium tabular',
        days < 0
          ? 'text-destructive'
          : days <= 1
            ? 'text-warning-foreground'
            : 'text-muted-foreground',
      )}
    >
      {label}
    </span>
  )
}

function UpcomingTripCard({ trip }: { trip: UpcomingTrip }) {
  const { t } = useTranslation()
  const days = trip.daysUntilStart
  const percent =
    trip.checklistTotal > 0 ? Math.round((trip.checklistDone / trip.checklistTotal) * 100) : 0
  const pending = (['CRITICAL', 'WARNING', 'INFO'] as const)
    .map((severity) => ({
      severity,
      count: trip.pendingSuggestions[severity.toLowerCase() as 'critical' | 'warning' | 'info'],
    }))
    .filter((p) => p.count > 0)
  const badge = { CRITICAL: 'critical', WARNING: 'warning', INFO: 'info' } as const

  return (
    <Link to={`/trips/${trip.id}`} className={linkClass}>
      <Card className="gap-3 overflow-hidden pt-0">
        <div className="h-2" style={{ backgroundColor: trip.coverColor ?? defaultTripColor }} />
        <div className="flex flex-col gap-3 px-4">
          <div className="flex items-start justify-between gap-3">
            <div className="flex min-w-0 flex-col gap-1">
              <h3 className="font-heading text-lg leading-snug font-bold">{trip.name}</h3>
              <p className="flex items-center gap-2 text-sm text-muted-foreground">
                <CalendarDays className="size-4 shrink-0" strokeWidth={1.75} aria-hidden />
                <TripDates startDate={trip.startDate} endDate={trip.endDate} />
              </p>
            </div>
            {days !== undefined && days > 0 ? (
              <span className="shrink-0 text-right font-heading font-bold text-highlight tabular">
                {t('trips.countdown', { count: days })}
              </span>
            ) : days === 0 ? (
              <span className="shrink-0 font-heading font-bold text-highlight">
                {t('trips.startsToday')}
              </span>
            ) : (
              <TripStatusBadge status={trip.status} />
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>
                {trip.checklistTotal > 0
                  ? t('dashboard.checklist', {
                      done: trip.checklistDone,
                      total: trip.checklistTotal,
                    })
                  : t('dashboard.checklistEmpty')}
              </span>
              {trip.checklistTotal > 0 && <span className="tabular">{percent}%</span>}
            </div>
            <div
              role="progressbar"
              aria-label={t('dashboard.checklistProgress')}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={percent}
              className="h-2 overflow-hidden rounded-full bg-muted dark:bg-background"
            >
              <div
                className="h-full rounded-full bg-primary transition-[width] duration-300 motion-reduce:transition-none"
                style={{ width: `${percent}%` }}
              />
            </div>
          </div>

          {pending.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              {pending.map((p) => (
                <Badge key={p.severity} variant={badge[p.severity]}>
                  {t(`dashboard.pending.${p.severity}`, { count: p.count })}
                </Badge>
              ))}
            </div>
          )}
        </div>
      </Card>
    </Link>
  )
}

function PastTrips({ trips }: { trips: PastTrip[] }) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  return (
    <section className="flex flex-col gap-3">
      <button
        type="button"
        aria-expanded={open}
        aria-controls="dashboard-past"
        onClick={() => setOpen((v) => !v)}
        className="flex h-11 items-center gap-2 rounded-md text-left font-heading text-lg font-bold focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
      >
        <History className="size-5 text-muted-foreground" strokeWidth={1.75} aria-hidden />
        <span className="flex-1">{t('dashboard.past', { count: trips.length })}</span>
        <ChevronDown
          className={cn(
            'size-5 text-muted-foreground transition-transform duration-150 motion-reduce:transition-none',
            open && 'rotate-180',
          )}
          strokeWidth={1.75}
          aria-hidden
        />
      </button>
      {open && (
        <ul id="dashboard-past" className="flex flex-col gap-2">
          {trips.map((trip) => (
            <li key={trip.id}>
              <Link to={`/trips/${trip.id}`} className={linkClass}>
                <Card className="flex-row items-center gap-3 px-4 py-3">
                  <span
                    className="size-3 shrink-0 rounded-full"
                    style={{ backgroundColor: trip.coverColor ?? defaultTripColor }}
                    aria-hidden
                  />
                  <span className="flex min-w-0 flex-1 flex-col">
                    <span className="truncate font-medium">{trip.name}</span>
                    <span className="text-sm text-muted-foreground">
                      <TripDates startDate={trip.startDate} endDate={trip.endDate} />
                    </span>
                  </span>
                  {trip.destinationCount > 0 && (
                    <span className="shrink-0 text-sm text-muted-foreground">
                      {t('trips.destinations', { count: trip.destinationCount })}
                    </span>
                  )}
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
