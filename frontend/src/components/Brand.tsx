import { Plane } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/utils'

export function Brand({ className }: { className?: string }) {
  const { t } = useTranslation()
  return (
    <span className={cn('flex items-center gap-2 font-heading text-lg font-bold', className)}>
      <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
        <Plane className="size-4.5" strokeWidth={1.75} aria-hidden />
      </span>
      {t('app.name')}
    </span>
  )
}
