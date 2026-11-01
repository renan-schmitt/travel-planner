import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { useUpdateMe, type HomeCity, type Me } from '@/features/auth/api'
import { cityErrorKey } from '@/features/places/city'
import { CityAutocomplete } from '@/features/places/CityAutocomplete'
import { ApiError } from '@/lib/api'
import { countryOptions } from '@/lib/countries'

const profileSchema = z.object({
  nationality: z.string().length(2),
  // Cidade de origem opcional; se houver, precisa do país (o fuso é opcional).
  homeCity: z
    .object({
      name: z.string().trim().max(160, 'cityTooLong'),
      countryCode: z.string(),
      timezone: z.string(),
      placeId: z.string().optional(),
      lat: z.number().optional(),
      lng: z.number().optional(),
    })
    .refine((c) => !c.name || c.countryCode, { path: ['countryCode'], message: 'countryRequired' }),
})

type ProfileValues = z.infer<typeof profileSchema>

/**
 * Nacionalidade e cidade de origem, usadas nas regras de viagem internacional e de transfer. A
 * cidade vem do autocomplete (com fuso), ou é digitada à mão se o Places estiver indisponível.
 */
export function ProfileForm({ me }: { me: Me }) {
  const { t, i18n } = useTranslation()
  const updateMe = useUpdateMe()
  const countries = useMemo(() => countryOptions(i18n.language), [i18n.language])

  const form = useForm({
    resolver: zodResolver(profileSchema),
    values: {
      nationality: me.nationality,
      homeCity: {
        name: me.homeCity?.name ?? '',
        countryCode: me.homeCity?.countryCode ?? '',
        timezone: me.homeCity?.timezone ?? '',
        placeId: me.homeCity?.placeId ?? undefined,
      },
    },
  })

  function onSubmit(values: ProfileValues) {
    updateMe.mutate(
      {
        version: me.version,
        nationality: values.nationality,
        homeCity: toHomeCity(values.homeCity),
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
            <Controller
              control={form.control}
              name="homeCity"
              render={({ field }) => (
                <CityAutocomplete
                  label={t('profile.homeCity')}
                  value={field.value}
                  onChange={field.onChange}
                  error={cityErrorKey(form.formState.errors.homeCity)}
                />
              )}
            />
            <p className="text-sm text-muted-foreground">{t('profile.homeCityHint')}</p>
          </div>

          <Button type="submit" className="self-end" disabled={updateMe.isPending}>
            {t('profile.save')}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

/** Cidade vazia limpa a cidade de origem. */
function toHomeCity(city: ProfileValues['homeCity']): HomeCity | undefined {
  if (!city.name.trim()) return undefined
  return {
    name: city.name.trim(),
    countryCode: city.countryCode,
    placeId: city.placeId,
    timezone: city.timezone || undefined,
  }
}
