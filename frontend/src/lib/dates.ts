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
