import { differenceInCalendarDays, parseISO } from 'date-fns'

const dateFormat = new Intl.DateTimeFormat('pt-BR', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

/** Data ISO (`2027-03-10`) como meia-noite local, sem deslocamento de fuso. */
export function parseDate(iso: string): Date {
  return parseISO(iso)
}

/** "10 de mar. de 2027". */
export function formatDate(iso: string): string {
  return dateFormat.format(parseDate(iso))
}

/** "1 – 15 de jul. de 2027" (o `Intl` junta mês e ano quando se repetem). */
export function formatDateRange(start: string, end: string): string {
  return dateFormat.formatRange(parseDate(start), parseDate(end))
}

/** Dias de calendário de hoje até a data (negativo se já passou). */
export function daysUntil(iso: string, today: Date = new Date()): number {
  return differenceInCalendarDays(parseDate(iso), today)
}

/** "2027-03-10T22:40:00" (relógio local do evento, sem fuso) → "10/03 22:40". */
export function formatLocalDateTime(local: string): string {
  const [date = '', time = ''] = local.split('T')
  const [, month, day] = date.split('-')
  return `${day}/${month} ${time.slice(0, 5)}`
}

/** Offset de um instante ISO-8601 como "GMT-3", "GMT+5:30" ou "GMT+0" ("Z"). */
export function gmtLabel(instant: string): string {
  const match = /([+-])(\d{2}):(\d{2})$/.exec(instant)
  if (!match) return 'GMT+0'
  const [, sign, hours, minutes] = match
  return `GMT${sign}${Number(hours)}${minutes === '00' ? '' : `:${minutes}`}`
}

/** Duração entre dois instantes ISO-8601: "9h35", "45min", "2h". */
export function formatDuration(from: string, to: string): string {
  const total = Math.round((Date.parse(to) - Date.parse(from)) / 60_000)
  const hours = Math.floor(total / 60)
  const minutes = total % 60
  if (hours === 0) return `${minutes}min`
  return minutes === 0 ? `${hours}h` : `${hours}h${String(minutes).padStart(2, '0')}`
}
