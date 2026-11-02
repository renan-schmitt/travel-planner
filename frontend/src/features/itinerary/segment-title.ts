import { useTranslation } from 'react-i18next'
import type { Segment, Stop } from './api'

/** Nome de uma ponta: o rótulo digitado ou, sem ele, a cidade da parada (ou "Casa"). */
export function endpointName(
  label: string | undefined,
  stopId: number | undefined,
  stops: Stop[],
  home: string,
): string {
  if (label) return label
  if (stopId == null) return home
  return stops.find((s) => s.id === stopId)?.cityName ?? home
}

/** Título do trecho: "Voo GRU → LIS" ou, num trecho local, "Transfer Aeroporto → Hotel". */
export function useSegmentTitle(segment: Segment, stops: Stop[]): string {
  const { t } = useTranslation()
  const home = t('segments.home')
  const type = t(`transport.${segment.type}`)
  if (segment.local) {
    const route = [segment.originLabel, segment.destinationLabel].filter(Boolean).join(' → ')
    const city = stops.find((s) => s.id === segment.localStopId)?.cityName ?? ''
    return route ? `${type} ${route}` : t('segments.localIn', { type, city })
  }
  const from = endpointName(segment.originLabel, segment.originStopId, stops, home)
  const to = endpointName(segment.destinationLabel, segment.destinationStopId, stops, home)
  return `${type} ${from} → ${to}`
}
