import { FileText, Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { EmptyState } from '@/components/EmptyState'
import { ChipGroup, Chip } from '@/components/FilterChips'
import { PageHeader } from '@/components/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { useMe } from '@/features/auth/api'
import { countryFlag, countryOptions } from '@/lib/countries'
import { formatDate } from '@/lib/dates'
import { useAdminRequirements, type Requirement, type RequirementStatus } from './api'
import { RequirementSheet } from './RequirementSheet'

type StatusFilter = RequirementStatus | 'ALL'
const STATUS_FILTERS: StatusFilter[] = ['ALL', 'DRAFT', 'PUBLISHED']

/**
 * `/admin/requirements` (só administradores): base de requisitos por país e status, formulário em
 * bottom sheet e "Publicar". `?country=XX` abre já filtrado (vem das sugestões e da aba Checklist).
 */
export function AdminRequirementsPage() {
  const { t, i18n } = useTranslation()
  const { data: me } = useMe()
  const [params, setParams] = useSearchParams()
  const country = params.get('country') ?? ''
  const [status, setStatus] = useState<StatusFilter>('ALL')
  const [editing, setEditing] = useState<Requirement | 'new'>()
  const countries = useMemo(() => countryOptions(i18n.language), [i18n.language])
  const names = useMemo(() => new Map(countries.map((c) => [c.code, c.name])), [countries])
  const isAdmin = me?.isAdmin === true
  const { data: requirements, error } = useAdminRequirements(isAdmin)

  if (me && !isAdmin) {
    return (
      <>
        <PageHeader title={t('adminRequirements.title')} />
        <p className="py-8 text-center text-muted-foreground">{t('adminRequirements.forbidden')}</p>
      </>
    )
  }

  const visible = (requirements ?? []).filter(
    (r) => (!country || r.countryCode === country) && (status === 'ALL' || r.status === status),
  )
  const groups = groupByCountry(visible, names)
  const current =
    editing === 'new' ? undefined : (requirements?.find((r) => r.id === editing?.id) ?? editing)

  function setCountry(code: string) {
    setParams(code ? { country: code } : {}, { replace: true })
  }

  return (
    <>
      <PageHeader title={t('adminRequirements.title')}>
        <Button onClick={() => setEditing('new')}>
          <Plus strokeWidth={1.75} aria-hidden />
          {t('adminRequirements.new')}
        </Button>
      </PageHeader>
      <div className="flex flex-col gap-3">
        <p className="text-sm text-muted-foreground">{t('adminRequirements.hint')}</p>
        <div className="flex flex-col gap-2">
          <Label htmlFor="admin-requirements-country">{t('adminRequirements.country')}</Label>
          <NativeSelect
            id="admin-requirements-country"
            value={country}
            onChange={(e) => setCountry(e.target.value)}
          >
            <option value="">{t('adminRequirements.allCountries')}</option>
            {countries.map((c) => (
              <option key={c.code} value={c.code}>
                {c.flag} {c.name}
              </option>
            ))}
          </NativeSelect>
        </div>
        <ChipGroup label={t('adminRequirements.statusFilter')}>
          {STATUS_FILTERS.map((s) => (
            <Chip key={s} pressed={status === s} onClick={() => setStatus(s)}>
              {t(`adminRequirements.statusFilters.${s}`)}
            </Chip>
          ))}
        </ChipGroup>

        {error ? (
          <p className="py-8 text-center text-destructive">{t('adminRequirements.loadError')}</p>
        ) : !requirements ? (
          <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
        ) : groups.length === 0 ? (
          <EmptyState icon={FileText} message={t('adminRequirements.empty')} />
        ) : (
          groups.map((group) => (
            <Card key={group.code} className="gap-0 py-0">
              <h2 className="flex items-center gap-2 px-4 pt-3 pb-2 font-semibold">
                <span aria-hidden>{countryFlag(group.code)}</span>
                {group.name}
              </h2>
              <ul className="divide-y border-t">
                {group.items.map((r) => (
                  <li key={r.id}>
                    <button
                      type="button"
                      onClick={() => setEditing(r)}
                      className="flex min-h-14 w-full flex-col gap-1 px-4 py-3 text-left hover:bg-muted/50 focus-visible:bg-muted/50 focus-visible:outline-none"
                    >
                      <span className="flex flex-wrap items-center gap-2">
                        <Badge variant={r.status === 'PUBLISHED' ? 'success' : 'warning'}>
                          {t(`adminRequirements.status.${r.status}`)}
                        </Badge>
                        <Badge variant="secondary">{t(`requirements.types.${r.type}`)}</Badge>
                        <span className="font-medium">{r.title}</span>
                      </span>
                      <span className="text-sm text-muted-foreground">
                        {[
                          t('adminRequirements.forNationality', {
                            nationality: names.get(r.nationality) ?? r.nationality,
                          }),
                          r.verifiedAt
                            ? t('adminRequirements.verifiedAt', { date: formatDate(r.verifiedAt) })
                            : t('adminRequirements.neverVerified'),
                        ].join(' · ')}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </Card>
          ))
        )}
      </div>
      <RequirementSheet
        open={editing !== undefined}
        requirement={current}
        defaultCountry={country}
        onClose={() => setEditing(undefined)}
      />
    </>
  )
}

function groupByCountry(list: Requirement[], names: Map<string, string>) {
  const groups = new Map<string, Requirement[]>()
  for (const r of list) groups.set(r.countryCode, [...(groups.get(r.countryCode) ?? []), r])
  return [...groups.entries()]
    .map(([code, items]) => ({ code, name: names.get(code) ?? code, items }))
    .sort((a, b) => a.name.localeCompare(b.name))
}
