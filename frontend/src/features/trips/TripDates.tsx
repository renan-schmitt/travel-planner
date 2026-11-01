import { useTranslation } from 'react-i18next'
import { formatDate, formatDateRange } from '@/lib/dates'

/** Período da viagem; datas são opcionais até existirem trechos de transporte. */
export function TripDates({ startDate, endDate }: { startDate?: string; endDate?: string }) {
  const { t } = useTranslation()
  let text: string
  if (startDate && endDate) text = formatDateRange(startDate, endDate)
  else if (startDate) text = t('trips.dates.from', { date: formatDate(startDate) })
  else if (endDate) text = t('trips.dates.until', { date: formatDate(endDate) })
  else text = t('trips.dates.none')
  return <span className="tabular">{text}</span>
}
