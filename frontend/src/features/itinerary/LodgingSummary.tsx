import { BedDouble, HeartHandshake, KeyRound } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { PriceTier, type PriceLevel } from '@/components/PriceTier'
import { formatDateRange } from '@/lib/dates'
import type { Lodging, LodgingKind } from './api'

const ICONS = { HOTEL: BedDouble, RENTAL: KeyRound, FRIENDS_HOME: HeartHandshake }

export function LodgingIcon({ kind, className }: { kind: LodgingKind; className?: string }) {
  const Icon = ICONS[kind]
  return <Icon className={className} strokeWidth={1.75} aria-hidden />
}

/**
 * Linha da hospedagem: ícone do tipo, nome, datas e noites, regime, faixa de preço e reserva (ou
 * "Casa de amigo/parente" e o endereço).
 */
export function LodgingSummary({ lodging }: { lodging: Lodging }) {
  const { t } = useTranslation()
  const details = [
    lodging.kind !== 'HOTEL' && t(`lodgingKind.${lodging.kind}`),
    lodging.kind === 'FRIENDS_HOME' && lodging.address,
    lodging.boardType && t(`board.${lodging.boardType}`),
    lodging.bookingRef && t('segments.booking', { ref: lodging.bookingRef }),
  ].filter(Boolean)
  return (
    <div className="flex min-w-0 items-start gap-3">
      <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-secondary text-secondary-foreground">
        <LodgingIcon kind={lodging.kind} className="size-4" />
      </span>
      <div className="flex min-w-0 flex-col gap-0.5">
        <p className="flex min-w-0 items-center gap-2 text-sm font-medium">
          <span className="truncate">{lodging.name}</span>
          {lodging.priceTier != null && (
            <PriceTier
              level={lodging.priceTier as PriceLevel}
              estimated={lodging.priceTierSource != null && lodging.priceTierSource !== 'USER'}
              className="shrink-0 text-xs"
            />
          )}
        </p>
        <p className="text-sm tabular">
          {formatDateRange(lodging.checkIn, lodging.checkOut)}
          <span className="text-muted-foreground">
            {` · ${t('stops.nights', { count: lodging.nights })}`}
          </span>
        </p>
        {details.length > 0 && (
          <p className="truncate text-xs text-muted-foreground">{details.join(' · ')}</p>
        )}
      </div>
    </div>
  )
}
