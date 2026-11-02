import { useTranslation } from 'react-i18next'
import { formatDuration, formatLocalDateTime, gmtLabel } from '@/lib/dates'
import type { Segment, Stop } from './api'
import { useSegmentTitle } from './segment-title'
import { TransportIcon } from './TransportIcon'

/**
 * Linha do trecho: tipo, rota, companhia/código e horários no fuso de cada ponta, ex.: "10/03 22:40
 * (GMT-3) → 11/03 11:15 (GMT+0) · 9h35".
 */
export function SegmentSummary({ segment, stops }: { segment: Segment; stops: Stop[] }) {
  const { t } = useTranslation()
  const title = useSegmentTitle(segment, stops)
  const depart = `${formatLocalDateTime(segment.departAt)} (${gmtLabel(segment.departInstant)})`
  const arrive =
    segment.arriveAt && segment.arriveInstant
      ? `${formatLocalDateTime(segment.arriveAt)} (${gmtLabel(segment.arriveInstant)})`
      : undefined
  const duration =
    segment.arriveInstant && formatDuration(segment.departInstant, segment.arriveInstant)
  const company = [segment.carrier, segment.code].filter(Boolean).join(' ')
  return (
    <div className="flex min-w-0 items-start gap-3">
      <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-secondary text-secondary-foreground">
        <TransportIcon type={segment.type} className="size-4" />
      </span>
      <div className="flex min-w-0 flex-col gap-0.5">
        <p className="truncate text-sm font-medium">{title}</p>
        <p className="text-sm tabular">
          {arrive ? `${depart} → ${arrive}` : depart}
          {duration && <span className="text-muted-foreground">{` · ${duration}`}</span>}
        </p>
        {(company || segment.bookingRef) && (
          <p className="truncate text-xs text-muted-foreground">
            {[company, segment.bookingRef && t('segments.booking', { ref: segment.bookingRef })]
              .filter(Boolean)
              .join(' · ')}
          </p>
        )}
      </div>
    </div>
  )
}
