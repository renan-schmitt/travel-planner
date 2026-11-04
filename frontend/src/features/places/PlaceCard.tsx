import { Check, ExternalLink, Landmark, MapPin, Star, UtensilsCrossed } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { PriceTier } from '@/components/PriceTier'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { asPriceLevel, distanceKm, mapsUrl, photoUrl } from '@/features/hotels/hotel-search'
import type { DiscoverKind, PlaceResult } from './api'
import { categoryLabel } from './places-url'

/** Nota "4,6 (1.200)" com rótulo acessível. */
export function Rating({ rating, count }: { rating: number; count?: number }) {
  const { t, i18n } = useTranslation()
  const value = rating.toLocaleString(i18n.language, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  })
  return (
    <span
      className="inline-flex items-center gap-1 tabular"
      aria-label={t('hotels.ratingLabel', { rating: value, count: count ?? 0 })}
    >
      <Star className="size-4 fill-warning text-warning" strokeWidth={1.75} aria-hidden />
      <span aria-hidden>
        {value}
        {count !== undefined && (
          <span className="text-muted-foreground">{` (${count.toLocaleString(i18n.language)})`}</span>
        )}
      </span>
    </span>
  )
}

/**
 * Resultado da busca de restaurantes/atrações: foto (uma, pelo proxy), nome, ★ nota, faixa,
 * categoria, distância do centro da busca, "Ver no Maps" e "Salvar" (ou "Salvo").
 */
export function PlaceCard({
  result,
  kind,
  center,
  centerLabel,
  saved,
  saving,
  onSave,
}: {
  result: PlaceResult
  kind: DiscoverKind
  center?: { lat: number; lng: number }
  centerLabel: string
  saved: boolean
  saving: boolean
  onSave?: () => void
}) {
  const { t, i18n } = useTranslation()
  const [photoFailed, setPhotoFailed] = useState(false)
  const tier = asPriceLevel(result.priceTier)
  const category = categoryLabel(t, result.primaryType, kind)
  const distance =
    center && result.lat != null && result.lng != null
      ? distanceKm(center, { lat: result.lat, lng: result.lng })
      : undefined
  const Fallback = kind === 'RESTAURANT' ? UtensilsCrossed : Landmark

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
            <Fallback className="size-8" strokeWidth={1.5} aria-hidden />
          </div>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <h2 className="leading-snug font-semibold">{result.name}</h2>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
          {result.rating !== undefined && (
            <Rating rating={result.rating} count={result.userRatingCount} />
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
                  {t('places.distance', {
                    km: distance.toLocaleString(i18n.language, {
                      maximumFractionDigits: distance < 10 ? 1 : 0,
                      minimumFractionDigits: distance < 10 ? 1 : 0,
                    }),
                    from: centerLabel,
                  })}
                </span>
              )}
              {distance !== undefined && result.address && ' · '}
              {result.address}
            </span>
          </p>
        )}
        <div className="mt-auto flex flex-wrap gap-x-4 gap-y-1 pt-1 text-sm">
          <a
            href={mapsUrl(result)}
            target="_blank"
            rel="noreferrer"
            className="inline-flex min-h-11 items-center gap-1 text-primary underline-offset-4 hover:underline"
          >
            {t('hotels.viewOnMaps')}
            <ExternalLink className="size-3.5" strokeWidth={1.75} aria-hidden />
          </a>
        </div>
        {saved ? (
          <p className="mt-1 inline-flex min-h-9 items-center gap-1.5 text-sm font-medium text-success">
            <Check className="size-4" strokeWidth={2} aria-hidden />
            {t('places.saved')}
          </p>
        ) : (
          onSave && (
            <Button className="mt-1" onClick={onSave} disabled={saving}>
              <Star strokeWidth={2} aria-hidden />
              {t('places.save')}
            </Button>
          )
        )}
      </div>
    </Card>
  )
}
