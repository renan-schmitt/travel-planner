import { zodResolver } from '@hookform/resolvers/zod'
import { Check } from 'lucide-react'
import { useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { cn } from '@/lib/utils'
import { tripColors } from './trip-colors'
import { tripSchema, type TripFormValues } from './trip-form'

/** Criação e edição da viagem: nome, datas opcionais (fim ≥ início), status e cor. */
export function TripForm({
  initialValues,
  submitting,
  onSubmit,
  onCancel,
}: {
  initialValues: TripFormValues
  submitting: boolean
  onSubmit: (values: TripFormValues) => void
  onCancel: () => void
}) {
  const { t } = useTranslation()
  const form = useForm({ resolver: zodResolver(tripSchema), values: initialValues })
  const { errors } = form.formState
  const selectedColor = useWatch({ control: form.control, name: 'coverColor' })
  const startDate = useWatch({ control: form.control, name: 'startDate' })

  return (
    <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
      <div className="flex flex-col gap-2">
        <Label htmlFor="trip-name">{t('tripForm.name')}</Label>
        <Input
          id="trip-name"
          placeholder={t('tripForm.namePlaceholder')}
          maxLength={120}
          aria-invalid={errors.name ? true : undefined}
          aria-describedby={errors.name ? 'trip-name-error' : undefined}
          {...form.register('name')}
        />
        {errors.name && (
          <p id="trip-name-error" className="text-sm text-destructive">
            {t(`tripForm.${errors.name.message as 'nameRequired' | 'nameTooLong'}`)}
          </p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="trip-start">{t('tripForm.startDate')}</Label>
          <Input id="trip-start" type="date" className="tabular" {...form.register('startDate')} />
        </div>
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="trip-end">{t('tripForm.endDate')}</Label>
          <Input
            id="trip-end"
            type="date"
            className="tabular"
            min={startDate || undefined}
            aria-invalid={errors.endDate ? true : undefined}
            aria-describedby={errors.endDate ? 'trip-end-error' : undefined}
            {...form.register('endDate')}
          />
        </div>
        <p className="col-span-2 -mt-1 text-sm text-muted-foreground">{t('tripForm.datesHint')}</p>
        {errors.endDate && (
          <p id="trip-end-error" className="col-span-2 text-sm text-destructive">
            {t('tripForm.endBeforeStart')}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="trip-status">{t('tripForm.status')}</Label>
        <NativeSelect id="trip-status" {...form.register('status')}>
          <option value="PLANNING">{t('trips.status.PLANNING')}</option>
          <option value="CONFIRMED">{t('trips.status.CONFIRMED')}</option>
        </NativeSelect>
      </div>

      <fieldset className="flex flex-col gap-2">
        <legend className="mb-2 text-sm leading-none font-medium">{t('tripForm.color')}</legend>
        <div className="flex flex-wrap gap-2">
          {tripColors.map((color, i) => (
            <label
              key={color}
              className="relative flex size-11 cursor-pointer items-center justify-center rounded-full has-focus-visible:ring-3 has-focus-visible:ring-ring/50"
            >
              <input
                type="radio"
                value={color}
                className="sr-only"
                aria-label={t('tripForm.colorOption', { number: i + 1 })}
                {...form.register('coverColor')}
              />
              <span
                aria-hidden
                className={cn(
                  'flex size-9 items-center justify-center rounded-full text-white',
                  selectedColor === color &&
                    'ring-2 ring-foreground ring-offset-2 ring-offset-card',
                )}
                style={{ backgroundColor: color }}
              >
                {selectedColor === color && <Check className="size-4" strokeWidth={2.5} />}
              </span>
            </label>
          ))}
        </div>
      </fieldset>

      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" onClick={onCancel}>
          {t('tripForm.cancel')}
        </Button>
        <Button type="submit" disabled={submitting}>
          {t('tripForm.save')}
        </Button>
      </div>
    </form>
  )
}
