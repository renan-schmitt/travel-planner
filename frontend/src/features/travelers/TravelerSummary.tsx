import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { countryFlag } from '@/lib/countries'
import type { Traveler } from './api'
import { PassportWarning } from './PassportWarning'

/** Nome, "Você", faixa etária, bandeiras das nacionalidades e aviso do passaporte. */
export function TravelerSummary({ traveler }: { traveler: Traveler }) {
  const { t } = useTranslation()
  return (
    <div className="flex min-w-0 flex-col gap-1">
      <div className="flex min-w-0 items-center gap-2">
        <span className="truncate font-medium">{traveler.name}</span>
        {traveler.me && <Badge variant="highlight">{t('travelers.you')}</Badge>}
      </div>
      <p className="text-sm text-muted-foreground">
        {t(`travelers.ageGroup.${traveler.ageGroup}`)}
        {traveler.birthYear ? ` · ${traveler.birthYear}` : ''}
        {' · '}
        <span aria-label={traveler.nationalities.join(', ')}>
          {traveler.nationalities.map(countryFlag).join(' ')}
        </span>
      </p>
      {traveler.passportExpiry && <PassportWarning expiry={traveler.passportExpiry} />}
    </div>
  )
}
