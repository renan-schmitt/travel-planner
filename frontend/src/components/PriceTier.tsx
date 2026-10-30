import { useTranslation } from 'react-i18next'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { cn } from '@/lib/utils'

export type PriceLevel = 1 | 2 | 3 | 4

/**
 * Faixa de preço (ESP §13.7): sempre 4 cifrões, os ativos em `--foreground` e os demais em
 * `--border`. Faixa estimada: contorno pontilhado e tooltip "Estimado".
 */
export function PriceTier({
  level,
  estimated = false,
  className,
}: {
  level: PriceLevel
  estimated?: boolean
  className?: string
}) {
  const { t } = useTranslation()
  const label = t(estimated ? 'priceTier.estimatedLabel' : 'priceTier.label', { level })

  const tier = (
    <span
      role="img"
      aria-label={label}
      tabIndex={estimated ? 0 : undefined}
      className={cn(
        'inline-flex font-semibold tracking-tight tabular',
        estimated && 'rounded-md border border-dotted border-muted-foreground px-1',
        className,
      )}
    >
      {[1, 2, 3, 4].map((i) => (
        <span key={i} aria-hidden className={i <= level ? 'text-foreground' : 'text-border'}>
          $
        </span>
      ))}
    </span>
  )

  if (!estimated) return tier
  return (
    <Tooltip>
      <TooltipTrigger asChild>{tier}</TooltipTrigger>
      <TooltipContent>{t('priceTier.estimated')}</TooltipContent>
    </Tooltip>
  )
}
