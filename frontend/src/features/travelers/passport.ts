import { addMonths, format } from 'date-fns'

export type PassportStatus = 'expired' | 'expiringSoon' | 'valid'

/**
 * Situação do passaporte: muitos países exigem validade de 6 meses além da viagem, então avisamos
 * quando ele vence em menos de 6 meses a partir de hoje (ESP §6.4, regra PASSAPORTE_VALIDADE).
 */
export function passportStatus(expiry: string, today: Date = new Date()): PassportStatus {
  const todayIso = format(today, 'yyyy-MM-dd')
  if (expiry < todayIso) return 'expired'
  if (expiry < format(addMonths(today, 6), 'yyyy-MM-dd')) return 'expiringSoon'
  return 'valid'
}
