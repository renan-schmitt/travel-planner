import { TriangleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { formatDate } from '@/lib/dates'
import { cn } from '@/lib/utils'
import { passportStatus } from './passport'

/** Aviso quando o passaporte está vencido ou vence em menos de 6 meses; nada se estiver ok. */
export function PassportWarning({ expiry, className }: { expiry: string; className?: string }) {
  const { t } = useTranslation()
  const status = passportStatus(expiry)
  if (status === 'valid') return null
  return (
    <p
      className={cn(
        'flex items-start gap-1.5 text-sm',
        status === 'expired' ? 'text-destructive' : 'text-warning-foreground',
        className,
      )}
    >
      <TriangleAlert className="mt-0.5 size-4 shrink-0" strokeWidth={1.75} aria-hidden />
      {t(`travelers.passport.${status}`, { date: formatDate(expiry) })}
    </p>
  )
}
