/** Fusos IANA conhecidos pelo navegador, em ordem alfabética. */
export function timeZoneOptions(): string[] {
  const zones = Intl.supportedValuesOf('timeZone')
  return zones.includes('UTC') ? zones : [...zones, 'UTC'].sort()
}

/** "America/Sao_Paulo" → "America/Sao Paulo". */
export function timeZoneLabel(zone: string): string {
  return zone.replaceAll('_', ' ')
}
