import type { TFunction } from 'i18next'
import type { DiscoverKind } from './api'

/** Valor de `?kind=` na URL da seção "Lugares". */
export type KindParam = 'restaurants' | 'attractions'

export const KIND_PARAM: Record<DiscoverKind, KindParam> = {
  RESTAURANT: 'restaurants',
  ATTRACTION: 'attractions',
}

export function kindFromParam(value: string | null): DiscoverKind {
  return value === 'attractions' ? 'ATTRACTION' : 'RESTAURANT'
}

/** `/trips/1/stops/2/places?kind=restaurants` (com `&discover=1`, já abre a busca). */
export function placesPath(
  tripId: number,
  stopId: number,
  kind: DiscoverKind = 'RESTAURANT',
  discover = false,
): string {
  const params = new URLSearchParams({ kind: KIND_PARAM[kind] })
  if (discover) params.set('discover', '1')
  return `/trips/${tripId}/stops/${stopId}/places?${params.toString()}`
}

/** Categoria pelo `primaryType` do Places (desconhecida = genérica do tipo da busca). */
export function categoryLabel(
  t: TFunction,
  primaryType: string | undefined,
  kind: DiscoverKind,
): string | undefined {
  if (!primaryType) return undefined
  return t(`places.categories.${primaryType}`, {
    defaultValue: t(`places.categories.${kind === 'RESTAURANT' ? 'restaurant' : 'other'}`),
  })
}
