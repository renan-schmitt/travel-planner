import { ExternalLink, Hotel, MapPin, Plus, Star } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { PriceTier } from '@/components/PriceTier'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import type { Stop } from '@/features/itinerary/api'
import type { HotelResult } from './api'
import { asPriceLevel, bookingUrl, distanceFromStop, mapsUrl, photoUrl } from './hotel-search'

/**
 * Resultado da busca: foto (no máximo uma, pelo proxy), nome, ★ nota (nº de avaliações), faixa de
 * preço (pontilhada quando estimada), categoria, distância do centro da parada e as ações.
 */
export function HotelCard({
  result,
  stop,
  dates,
  guests,
  onAdd,
}: {
  result: HotelResult
  stop: Stop
  dates: { checkIn: string; checkOut: string }
  guests: { adults: number; children: number }
  onAdd?: () => void
}) {
  const { t, i18n } = useTranslation()
  const [photoFailed, setPhotoFailed] = useState(false)
  const tier = asPriceLevel(result.priceTier)
  const distance = distanceFromStop(stop, result)
  const number = (value: number, digits: number) =>
    value.toLocaleString(i18n.language, {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    })
  const category = result.primaryType
    ? t(`hotels.categories.${result.primaryType}`, { defaultValue: t('hotels.categories.other') })
    : undefined

  return (
    <Card role="article" aria-label={result.name} className="h-full gap-0 overflow-hidden py-0">
      <div className="aspect-[16/9] w-full bg-muted">
        {result.photoName && !photoFailed ? (
          <img
            src={photoUrl(result.photoName)}
            alt=""
            loading="lazy"
            className="size-full object-cover"
            onError={() => setPhotoFailed(true)}
          />
        ) : (
          <div className="flex size-full items-center justify-center text-muted-foreground">
            <Hotel className="size-8" strokeWidth={1.5} aria-hidden />
          </div>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <h2 className="leading-snug font-semibold">{result.name}</h2>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
          {result.rating !== undefined && (
            <span
              className="inline-flex items-center gap-1 tabular"
              aria-label={t('hotels.ratingLabel', {
                rating: number(result.rating, 1),
                count: result.userRatingCount ?? 0,
              })}
            >
              <Star className="size-4 fill-warning text-warning" strokeWidth={1.75} aria-hidden />
              <span aria-hidden>
                {number(result.rating, 1)}
                {result.userRatingCount !== undefined && (
                  <span className="text-muted-foreground">
                    {` (${result.userRatingCount.toLocaleString(i18n.language)})`}
                  </span>
                )}
              </span>
            </span>
          )}
          {tier && <PriceTier level={tier} estimated={result.priceTierSource === 'HEURISTIC'} />}
          {category && <span className="text-muted-foreground">{category}</span>}
        </div>
        {(distance !== undefined || result.address) && (
          <p className="flex items-start gap-1 text-sm text-muted-foreground">
            <MapPin className="mt-0.5 size-4 shrink-0" strokeWidth={1.75} aria-hidden />
            <span className="min-w-0">
              {distance !== undefined && (
                <span className="tabular">
                  {t('hotels.distance', { km: number(distance, distance < 10 ? 1 : 0) })}
                </span>
              )}
              {distance !== undefined && result.address && ' · '}
              {result.address}
            </span>
          </p>
        )}
        <div className="mt-auto flex flex-wrap gap-x-4 gap-y-1 pt-1 text-sm">
          <ExternalAction href={mapsUrl(result)}>{t('hotels.viewOnMaps')}</ExternalAction>
          <ExternalAction href={bookingUrl(result, stop, dates, guests)}>
            {t('hotels.viewPrices')}
          </ExternalAction>
        </div>
        {onAdd && (
          <Button className="mt-1" onClick={onAdd}>
            <Plus strokeWidth={2} aria-hidden />
            {t('hotels.add')}
          </Button>
        )}
      </div>
    </Card>
  )
}

function ExternalAction({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noreferrer"
      className="inline-flex min-h-11 items-center gap-1 text-primary underline-offset-4 hover:underline"
    >
      {children}
      <ExternalLink className="size-3.5" strokeWidth={1.75} aria-hidden />
    </a>
  )
}
