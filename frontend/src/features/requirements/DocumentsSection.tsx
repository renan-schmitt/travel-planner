import { ExternalLink, FileWarning, Plug } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useMe } from '@/features/auth/api'
import { countryFlag } from '@/lib/countries'
import { formatDate } from '@/lib/dates'
import { adminRequirementsPath } from './admin-path'
import { useTripRequirements, type TripRequirementItem, type TripRequirements } from './api'

/** Nome do país no idioma da interface (para as nacionalidades sem dados). */
function regionName(code: string, locale: string) {
  try {
    return new Intl.DisplayNames([locale], { type: 'region' }).of(code) ?? code
  } catch {
    return code
  }
}

/**
 * Seção "Documentos" (aba Checklist): requisitos publicados por destino estrangeiro, com prazo,
 * link oficial e data da verificação, e o aviso fixo "informação indicativa". Destino sem dados
 * mostra "sem informações" (nunca um "não precisa de nada"). Viagem nacional: nada.
 */
export function DocumentsSection({ tripId }: { tripId: number }) {
  const { t } = useTranslation()
  const { data } = useTripRequirements(tripId)
  const { data: me } = useMe()
  if (!data || data.length === 0) return null

  return (
    <Card className="gap-3">
      <CardHeader>
        <CardTitle>{t('requirements.title')}</CardTitle>
        <CardDescription>{t('requirements.disclaimer')}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {data.map((destination) => (
          <Destination
            key={destination.countryCode}
            destination={destination}
            isAdmin={me?.isAdmin === true}
          />
        ))}
      </CardContent>
    </Card>
  )
}

function Destination({
  destination,
  isAdmin,
}: {
  destination: TripRequirements
  isAdmin: boolean
}) {
  const { t, i18n } = useTranslation()
  const { country, countryCode } = destination
  const name = country?.name ?? regionName(countryCode, i18n.language)
  const headingId = `documents-${countryCode}`
  const facts = [
    country?.plugTypes && t('requirements.plugs', { types: country.plugTypes.replace(/,/g, ', ') }),
    country?.voltage,
    country?.currency,
  ].filter(Boolean)

  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-2">
      <h3 id={headingId} className="flex items-center gap-2 font-semibold">
        <span aria-hidden>{country?.flag ?? countryFlag(countryCode)}</span>
        {name}
      </h3>
      {facts.length > 0 && (
        <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
          <Plug className="size-4 shrink-0" strokeWidth={1.75} aria-hidden />
          {facts.join(' · ')}
        </p>
      )}
      {destination.requirements.length > 0 && (
        <ul className="flex flex-col gap-2">
          {destination.requirements.map((r) => (
            <li key={r.id}>
              <RequirementCard requirement={r} />
            </li>
          ))}
        </ul>
      )}
      {destination.nationalitiesWithoutData.length > 0 && (
        <div className="flex gap-2 rounded-lg bg-warning-soft p-3 text-sm text-warning-foreground">
          <FileWarning className="mt-0.5 size-4 shrink-0" strokeWidth={1.75} aria-hidden />
          <div className="flex flex-col gap-1">
            <p>
              {t('requirements.noData', {
                nationalities: destination.nationalitiesWithoutData
                  .map((n) => regionName(n, i18n.language))
                  .join(', '),
              })}
            </p>
            {isAdmin && (
              <Link
                to={adminRequirementsPath(countryCode)}
                className="inline-flex min-h-11 items-center font-medium underline underline-offset-4"
              >
                {t('requirements.addForCountry')}
              </Link>
            )}
          </div>
        </div>
      )}
    </section>
  )
}

function RequirementCard({ requirement: r }: { requirement: TripRequirementItem }) {
  const { t } = useTranslation()
  return (
    <div className="flex flex-col gap-1 rounded-lg border p-3">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant={r.type === 'VISA' ? 'critical' : 'secondary'}>
          {t(`requirements.types.${r.type}`)}
        </Badge>
        <span className="font-medium">{r.title}</span>
      </div>
      {r.description && <p className="text-sm text-muted-foreground">{r.description}</p>}
      <dl className="flex flex-wrap gap-x-4 gap-y-1 text-sm">
        {r.dueDate && (
          <div className="flex gap-1">
            <dt className="text-muted-foreground">{t('requirements.dueDate')}</dt>
            <dd className="font-medium tabular">{formatDate(r.dueDate)}</dd>
          </div>
        )}
        {r.travelers.length > 0 && (
          <div className="flex gap-1">
            <dt className="text-muted-foreground">{t('requirements.travelers')}</dt>
            <dd>{r.travelers.join(', ')}</dd>
          </div>
        )}
        {r.verifiedAt && (
          <div className="flex gap-1">
            <dt className="text-muted-foreground">{t('requirements.verifiedAt')}</dt>
            <dd className="tabular">{formatDate(r.verifiedAt)}</dd>
          </div>
        )}
      </dl>
      <a
        href={r.officialUrl}
        target="_blank"
        rel="noreferrer"
        className="inline-flex min-h-11 items-center gap-1 self-start text-sm text-primary underline-offset-4 hover:underline"
      >
        {t('requirements.officialSite')}
        <ExternalLink className="size-3.5" strokeWidth={1.75} aria-hidden />
      </a>
    </div>
  )
}
