import { ArrowDown, ArrowUp, MapPinPlus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { hasRole, type Trip } from '@/features/trips/api'
import { countryFlag } from '@/lib/countries'
import { formatDateRange } from '@/lib/dates'
import { timeZoneLabel } from '@/lib/timezones'
import { useReorderStops, useStops, type Stop } from './api'
import { StopSheet } from './StopSheet'

/**
 * Aba "Roteiro": paradas na ordem da viagem. Editor adiciona/edita em bottom sheet e reordena com
 * ↑/↓ (o drag-and-drop chega na Fase 18). Leitor só vê a lista.
 */
export function ItineraryTab({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  const canEdit = hasRole(trip.role, 'EDITOR')
  const { data: stops, error } = useStops(trip.id)
  const reorder = useReorderStops(trip.id)
  // `editing` undefined = sheet fechado; null = nova parada.
  const [editing, setEditing] = useState<Stop | null>()

  function move(stop: Stop, index: number, delta: -1 | 1) {
    if (!stops) return
    const ids = stops.map((s) => s.id).filter((id) => id !== stop.id)
    ids.splice(index + delta, 0, stop.id)
    reorder.mutate(ids, { onError: () => toast.error(t('stops.reorderError')) })
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
        {stops && stops.length > 0 && (
          <ol aria-label={t('stops.title')} className="flex flex-col divide-y">
            {stops.map((stop, i) => (
              <li key={stop.id} className="flex items-center gap-2 py-3">
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
                {canEdit && stops.length > 1 && (
                  <div className="flex shrink-0 flex-col">
                    <Button
                      variant="ghost"
                      size="icon"
                      disabled={i === 0 || reorder.isPending}
                      aria-label={t('stops.moveUp', { city: stop.cityName })}
                      onClick={() => move(stop, i, -1)}
                    >
                      <ArrowUp strokeWidth={1.75} />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      disabled={i === stops.length - 1 || reorder.isPending}
                      aria-label={t('stops.moveDown', { city: stop.cityName })}
                      onClick={() => move(stop, i, 1)}
                    >
                      <ArrowDown strokeWidth={1.75} />
                    </Button>
                  </div>
                )}
              </li>
            ))}
          </ol>
        )}
        {canEdit && stops && (
          <Button variant="outline" className="self-start" onClick={() => setEditing(null)}>
            <MapPinPlus strokeWidth={1.75} aria-hidden />
            {t('stops.add')}
          </Button>
        )}
        {canEdit && stops && (
          <StopSheet
            trip={trip}
            stops={stops}
            stop={editing ?? undefined}
            open={editing !== undefined}
            onOpenChange={(open) => !open && setEditing(undefined)}
          />
        )}
      </CardContent>
    </Card>
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
