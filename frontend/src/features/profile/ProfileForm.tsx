import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { useUpdateMe, type HomeCity, type Me } from '@/features/auth/api'
import { ApiError } from '@/lib/api'
import { countryOptions } from '@/lib/countries'

const profileSchema = z
  .object({
    nationality: z.string().length(2),
    homeCityName: z.string().trim().max(255),
    homeCountryCode: z.string(),
  })
  .refine((v) => !v.homeCityName || v.homeCountryCode, {
    path: ['homeCountryCode'],
    message: 'required',
  })

type ProfileValues = z.infer<typeof profileSchema>

/**
 * Nacionalidade e cidade de origem, usadas nas regras de viagem internacional e de transfer. A
 * cidade é texto livre até o autocomplete (Fase 7).
 */
export function ProfileForm({ me }: { me: Me }) {
  const { t, i18n } = useTranslation()
  const updateMe = useUpdateMe()
  const countries = useMemo(() => countryOptions(i18n.language), [i18n.language])

  const form = useForm({
    resolver: zodResolver(profileSchema),
    values: {
      nationality: me.nationality,
      homeCityName: me.homeCity?.name ?? '',
      homeCountryCode: me.homeCity?.countryCode ?? '',
    },
  })
  const countryError = form.formState.errors.homeCountryCode

  function onSubmit(values: ProfileValues) {
    updateMe.mutate(
      {
        version: me.version,
        nationality: values.nationality,
        homeCity: toHomeCity(values, me.homeCity),
      },
      {
        onSuccess: () => toast.success(t('profile.saved')),
        onError: (error) =>
          toast.error(
            t(
              error instanceof ApiError && error.status === 409
                ? 'profile.conflict'
                : 'profile.saveError',
            ),
          ),
      },
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('profile.personal')}</CardTitle>
        <CardDescription>{t('profile.personalHint')}</CardDescription>
      </CardHeader>
      <CardContent>
        <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
          <div className="flex flex-col gap-2">
            <Label htmlFor="nationality">{t('profile.nationality')}</Label>
            <NativeSelect id="nationality" {...form.register('nationality')}>
              {countries.map(({ code, name, flag }) => (
                <option key={code} value={code}>
                  {flag} {name}
                </option>
              ))}
            </NativeSelect>
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="homeCityName">{t('profile.homeCity')}</Label>
            <Input
              id="homeCityName"
              autoComplete="address-level2"
              placeholder={t('profile.homeCityPlaceholder')}
              {...form.register('homeCityName')}
            />
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="homeCountryCode">{t('profile.homeCountry')}</Label>
            <NativeSelect
              id="homeCountryCode"
              aria-invalid={countryError ? true : undefined}
              aria-describedby={countryError ? 'homeCountryCode-error' : undefined}
              {...form.register('homeCountryCode')}
            >
              <option value="">{t('profile.homeCountryEmpty')}</option>
              {countries.map(({ code, name, flag }) => (
                <option key={code} value={code}>
                  {flag} {name}
                </option>
              ))}
            </NativeSelect>
            {countryError && (
              <p id="homeCountryCode-error" className="text-sm text-destructive">
                {t('profile.homeCountryRequired')}
              </p>
            )}
          </div>

          <Button type="submit" className="self-end" disabled={updateMe.isPending}>
            {t('profile.save')}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

/**
 * Cidade vazia limpa a cidade de origem. `placeId` e `timezone` (que virão do autocomplete) só são
 * mantidos se a cidade não mudou.
 */
function toHomeCity(values: ProfileValues, current?: HomeCity | null): HomeCity | undefined {
  if (!values.homeCityName) return undefined
  const unchanged =
    current?.name === values.homeCityName && current.countryCode === values.homeCountryCode
  return {
    name: values.homeCityName,
    countryCode: values.homeCountryCode,
    placeId: unchanged ? current.placeId : undefined,
    timezone: unchanged ? current.timezone : undefined,
  }
}
