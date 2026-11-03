import {
  CalendarRange,
  House,
  LogIn,
  LogOut,
  MoonStar,
  TriangleAlert,
  type LucideIcon,
} from 'lucide-react'
import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { EmptyState } from '@/components/EmptyState'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { focusKey } from '@/features/suggestions/resolve'
import type { Trip } from '@/features/trips/api'
import { useTripTab } from '@/features/trips/trip-tabs'
import { countryFlag } from '@/lib/countries'
import { formatDayHeader, formatLocalDateTime, gmtLabel, todayIso } from '@/lib/dates'
import { cn } from '@/lib/utils'
import { useItinerary, type ItineraryDay, type ItineraryEvent, type ItinerarySegment } from './api'
import { LodgingIcon } from './LodgingSummary'
import { TransportIcon } from './TransportIcon'

/**
 * Aba "Timeline": a viagem dia a dia (gerada no backend a partir de paradas, trechos e
 * hospedagens). Cada dia tem as cidades, os eventos com horário local e onde se dorme; noite sem
 * hospedagem aparece em destaque. Durante a viagem, a lista rola até "hoje".
 */
export function TimelineTab({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  const { data, error } = useItinerary(trip.id)
  const today = todayIso()
  const todayRef = useRef<HTMLElement>(null)
  const days = data?.days
  const hasToday = days?.some((d) => d.date === today) ?? false

  // Rola até hoje uma vez, quando os dias chegam.
  useEffect(() => {
    if (hasToday) todayRef.current?.scrollIntoView?.({ block: 'start' })
  }, [hasToday])

  if (error) return <p className="py-8 text-center text-destructive">{t('timeline.loadError')}</p>
  if (!days) return <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
  if (days.length === 0) return <EmptyState icon={CalendarRange} message={t('timeline.empty')} />

  const gaps = days.filter((d) => d.gap).length

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('timeline.title')}</CardTitle>
        <CardDescription>{t('timeline.hint')}</CardDescription>
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
        <ol className="flex flex-col gap-6">
          {days.map((day) => (
            <li key={day.date}>
              <DaySection
                day={day}
                isToday={day.date === today}
                ref={day.date === today ? todayRef : undefined}
              />
            </li>
          ))}
        </ol>
      </CardContent>
    </Card>
  )
}

function DaySection({
  day,
  isToday,
  ref,
}: {
  day: ItineraryDay
  isToday: boolean
  ref?: React.Ref<HTMLElement>
}) {
  const { t } = useTranslation()
  const headingId = `day-${day.date}`
  const place =
    day.stops.length > 0
      ? day.stops.map((s) => `${countryFlag(s.countryCode)} ${s.cityName}`).join(' → ')
      : day.overnight.kind === 'IN_TRANSIT'
        ? t('timeline.inTransitDay')
        : undefined

  return (
    <section ref={ref} aria-labelledby={headingId} className="scroll-mt-20">
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
        {day.events.map((event, i) => (
          <EventRow key={i} event={event} />
        ))}
        <li>
          <Overnight day={day} />
        </li>
      </ul>
    </section>
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
