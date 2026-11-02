import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/utils'
import type { PriceLevel } from './PriceTier'

const LEVELS: PriceLevel[] = [1, 2, 3, 4]

/**
 * Seletor da faixa de preço: "Não sei", `$`, `$$`, `$$$` e `$$$$` (rádios). `null` = não sei.
 * Par do {@link PriceTier}, que exibe a faixa.
 */
export function PriceTierInput({
  value,
  onChange,
  name = 'price-tier',
  legend,
}: {
  value: PriceLevel | null
  onChange: (value: PriceLevel | null) => void
  name?: string
  legend?: string
}) {
  const { t } = useTranslation()
  const options: { level: PriceLevel | null; text: string; label: string }[] = [
    { level: null, text: t('priceTier.unknown'), label: t('priceTier.unknown') },
    ...LEVELS.map((level) => ({
      level,
      text: '$'.repeat(level),
      label: t('priceTier.label', { level }),
    })),
  ]
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-medium">{legend ?? t('priceTier.title')}</legend>
      <div className="grid grid-cols-5 gap-2">
        {options.map((option) => {
          const checked = option.level === value
          return (
            <label
              key={option.level ?? 'unknown'}
              className={cn(
                'flex min-h-11 cursor-pointer items-center justify-center rounded-lg border px-1 text-center text-sm has-focus-visible:ring-3 has-focus-visible:ring-ring/50',
                option.level != null && 'font-semibold tracking-tight tabular',
                option.level == null && 'text-xs',
                checked
                  ? 'border-primary bg-primary/10 text-foreground'
                  : 'text-muted-foreground hover:bg-accent',
              )}
            >
              <input
                type="radio"
                name={name}
                className="sr-only"
                aria-label={option.label}
                checked={checked}
                onChange={() => onChange(option.level)}
              />
              <span aria-hidden={option.level != null}>{option.text}</span>
            </label>
          )
        })}
      </div>
    </fieldset>
  )
}
