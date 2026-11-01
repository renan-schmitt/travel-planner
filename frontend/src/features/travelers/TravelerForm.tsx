import { zodResolver } from '@hookform/resolvers/zod'
import { X } from 'lucide-react'
import { useMemo } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { countryOptions } from '@/lib/countries'
import { ageGroups } from './api'
import { PassportWarning } from './PassportWarning'
import { travelerSchema, type TravelerFormValues } from './traveler-form'

type ErrorKey = 'nameRequired' | 'nameTooLong' | 'birthYearInvalid' | 'nationalityRequired'

/**
 * Nome, faixa etária, ano de nascimento (opcional), nacionalidades e validade do passaporte. Não
 * pede o número do passaporte (ESP §6.2).
 */
export function TravelerForm({
  initialValues,
  submitting,
  onSubmit,
  onCancel,
  extraActions,
}: {
  initialValues: TravelerFormValues
  submitting: boolean
  onSubmit: (values: TravelerFormValues) => void
  onCancel: () => void
  extraActions?: React.ReactNode
}) {
  const { t, i18n } = useTranslation()
  const countries = useMemo(() => countryOptions(i18n.language), [i18n.language])
  const countryByCode = useMemo(() => new Map(countries.map((c) => [c.code, c])), [countries])
  const form = useForm({ resolver: zodResolver(travelerSchema), values: initialValues })
  const { errors } = form.formState
  const passportExpiry = useWatch({ control: form.control, name: 'passportExpiry' })
  const error = (key?: string) => (key ? t(`travelerForm.${key as ErrorKey}`) : undefined)

  return (
    <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
      <div className="flex flex-col gap-2">
        <Label htmlFor="traveler-name">{t('travelerForm.name')}</Label>
        <Input
          id="traveler-name"
          maxLength={120}
          autoComplete="off"
          aria-invalid={errors.name ? true : undefined}
          aria-describedby={errors.name ? 'traveler-name-error' : undefined}
          {...form.register('name')}
        />
        {errors.name && (
          <p id="traveler-name-error" className="text-sm text-destructive">
            {error(errors.name.message)}
          </p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="traveler-age">{t('travelerForm.ageGroup')}</Label>
          <NativeSelect id="traveler-age" {...form.register('ageGroup')}>
            {ageGroups.map((group) => (
              <option key={group} value={group}>
                {t(`travelers.ageGroup.${group}`)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="traveler-birth-year">{t('travelerForm.birthYear')}</Label>
          <Input
            id="traveler-birth-year"
            inputMode="numeric"
            maxLength={4}
            placeholder={t('travelerForm.optional')}
            className="tabular"
            aria-invalid={errors.birthYear ? true : undefined}
            aria-describedby={errors.birthYear ? 'traveler-birth-year-error' : undefined}
            {...form.register('birthYear')}
          />
        </div>
        {errors.birthYear && (
          <p id="traveler-birth-year-error" className="col-span-2 text-sm text-destructive">
            {error(errors.birthYear.message)}
          </p>
        )}
      </div>

      <Controller
        control={form.control}
        name="nationalities"
        render={({ field, fieldState }) => (
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-sm leading-none font-medium">
              {t('travelerForm.nationalities')}
            </legend>
            {field.value.length > 0 && (
              <ul className="flex flex-wrap gap-2">
                {field.value.map((code) => {
                  const country = countryByCode.get(code)
                  return (
                    <li
                      key={code}
                      className="flex h-9 items-center gap-1 rounded-full bg-accent pr-1 pl-3 text-sm text-accent-foreground"
                    >
                      <span aria-hidden>{country?.flag}</span>
                      {country?.name ?? code}
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-sm"
                        className="rounded-full"
                        aria-label={t('travelerForm.removeNationality', {
                          name: country?.name ?? code,
                        })}
                        onClick={() => field.onChange(field.value.filter((c) => c !== code))}
                      >
                        <X />
                      </Button>
                    </li>
                  )
                })}
              </ul>
            )}
            <NativeSelect
              aria-label={t('travelerForm.addNationality')}
              value=""
              aria-invalid={fieldState.error ? true : undefined}
              onChange={(e) => {
                if (e.target.value) field.onChange([...field.value, e.target.value])
              }}
            >
              <option value="">{t('travelerForm.addNationality')}</option>
              {countries
                .filter((c) => !field.value.includes(c.code))
                .map(({ code, name, flag }) => (
                  <option key={code} value={code}>
                    {flag} {name}
                  </option>
                ))}
            </NativeSelect>
            {fieldState.error && (
              <p className="text-sm text-destructive">{error(fieldState.error.message)}</p>
            )}
          </fieldset>
        )}
      />

      <div className="flex flex-col gap-2">
        <Label htmlFor="traveler-passport">{t('travelerForm.passportExpiry')}</Label>
        <Input
          id="traveler-passport"
          type="date"
          className="tabular"
          {...form.register('passportExpiry')}
        />
        <p className="text-sm text-muted-foreground">{t('travelerForm.passportHint')}</p>
        {passportExpiry && <PassportWarning expiry={passportExpiry} />}
      </div>

      <div className="flex items-center justify-end gap-2">
        {extraActions}
        <Button type="button" variant="ghost" onClick={onCancel}>
          {t('travelerForm.cancel')}
        </Button>
        <Button type="submit" disabled={submitting}>
          {t('travelerForm.save')}
        </Button>
      </div>
    </form>
  )
}
