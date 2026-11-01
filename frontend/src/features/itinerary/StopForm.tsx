import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { cityErrorKey } from '@/features/places/city'
import { CityAutocomplete } from '@/features/places/CityAutocomplete'
import { formatDateRange } from '@/lib/dates'
import type { Stop } from './api'
import { findOverlap, nightsBetween, stopSchema, type StopFormValues } from './stop-form'

type ErrorKey = 'arrivalRequired' | 'departureRequired' | 'departureBeforeArrival' | 'notesTooLong'

/** Cidade (autocomplete ou manual), chegada, saída (com nº de noites) e observações. */
export function StopForm({
  initialValues,
  otherStops,
  submitting,
  onSubmit,
  onCancel,
  extraActions,
}: {
  initialValues: StopFormValues
  /** Demais paradas da viagem, para avisar de sobreposição antes de salvar. */
  otherStops: Stop[]
  submitting: boolean
  onSubmit: (values: StopFormValues) => void
  onCancel: () => void
  extraActions?: React.ReactNode
}) {
  const { t } = useTranslation()
  const form = useForm({ resolver: zodResolver(stopSchema), values: initialValues })
  const { errors } = form.formState
  const [arrival, departure] = useWatch({
    control: form.control,
    name: ['arrivalDate', 'departureDate'],
  })
  const overlap = findOverlap(arrival, departure, otherStops)
  const validRange = arrival && departure && departure >= arrival
  const nights = validRange ? nightsBetween(arrival, departure) : undefined
  const error = (key?: string) => (key ? t(`stopForm.${key as ErrorKey}`) : undefined)

  return (
    <form
      noValidate
      className="flex flex-col gap-4"
      onSubmit={form.handleSubmit((values) => {
        if (!overlap) onSubmit(values)
      })}
    >
      <Controller
        control={form.control}
        name="city"
        render={({ field }) => (
          <CityAutocomplete
            label={t('stopForm.city')}
            value={field.value}
            onChange={field.onChange}
            error={cityErrorKey(errors.city)}
          />
        )}
      />

      <div className="grid grid-cols-2 gap-3">
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="stop-arrival">{t('stopForm.arrivalDate')}</Label>
          <Input
            id="stop-arrival"
            type="date"
            className="tabular"
            aria-invalid={errors.arrivalDate || overlap ? true : undefined}
            {...form.register('arrivalDate')}
          />
        </div>
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="stop-departure">{t('stopForm.departureDate')}</Label>
          <Input
            id="stop-departure"
            type="date"
            className="tabular"
            min={arrival || undefined}
            aria-invalid={errors.departureDate || overlap ? true : undefined}
            {...form.register('departureDate')}
          />
        </div>
        {nights !== undefined && (
          <p className="col-span-2 -mt-1 text-sm text-muted-foreground">
            {nights === 0 ? t('stops.dayTrip') : t('stops.nights', { count: nights })}
          </p>
        )}
        {(errors.arrivalDate || errors.departureDate) && (
          <p className="col-span-2 text-sm text-destructive">
            {error(errors.arrivalDate?.message ?? errors.departureDate?.message)}
          </p>
        )}
        {overlap && (
          <p role="alert" className="col-span-2 text-sm text-destructive">
            {t('stopForm.overlap', {
              city: overlap.cityName,
              dates: formatDateRange(overlap.arrivalDate, overlap.departureDate),
            })}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="stop-notes">{t('stopForm.notes')}</Label>
        <Textarea
          id="stop-notes"
          maxLength={2000}
          placeholder={t('stopForm.optional')}
          {...form.register('notes')}
        />
        {errors.notes && <p className="text-sm text-destructive">{error(errors.notes.message)}</p>}
      </div>

      <div className="flex items-center justify-end gap-2">
        {extraActions}
        <Button type="button" variant="ghost" onClick={onCancel}>
          {t('stopForm.cancel')}
        </Button>
        <Button type="submit" disabled={submitting}>
          {t('stopForm.save')}
        </Button>
      </div>
    </form>
  )
}
