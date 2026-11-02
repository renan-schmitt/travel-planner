import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { PriceTierInput } from '@/components/PriceTierInput'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { formatDateRange } from '@/lib/dates'
import { cn } from '@/lib/utils'
import type { Stop } from './api'
import {
  BOARD_TYPES,
  hasBooking,
  LODGING_KINDS,
  lodgingSchema,
  type LodgingFormValues,
} from './lodging-form'
import { LodgingIcon } from './LodgingSummary'

type ErrorKey =
  | 'nameRequired'
  | 'checkInRequired'
  | 'checkOutRequired'
  | 'checkOutBeforeCheckIn'
  | 'outsideStop'
  | 'tooLong'

/**
 * Tipo (hotel, aluguel ou casa de amigo/parente), nome (texto livre), endereço, check-in/check-out,
 * regime, faixa de preço, reserva e notas. Na casa de amigo/parente, regime, preço e reserva somem.
 */
export function LodgingForm({
  stop,
  initialValues,
  submitting,
  onSubmit,
  onCancel,
  extraActions,
}: {
  stop: Stop
  initialValues: LodgingFormValues
  submitting: boolean
  onSubmit: (values: LodgingFormValues) => void
  onCancel: () => void
  extraActions?: React.ReactNode
}) {
  const { t } = useTranslation()
  const schema = useMemo(() => lodgingSchema(stop), [stop])
  const form = useForm({ resolver: zodResolver(schema), values: initialValues })
  const { errors } = form.formState
  const kind = useWatch({ control: form.control, name: 'kind' })
  const booking = hasBooking(kind)
  const stopDates = formatDateRange(stop.arrivalDate, stop.departureDate)
  const error = (key?: string) =>
    key ? t(`lodgingForm.${key as ErrorKey}`, { dates: stopDates }) : undefined
  const datesError = errors.checkIn?.message ?? errors.checkOut?.message

  return (
    <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
      <fieldset className="flex flex-col gap-2">
        <legend className="mb-2 text-sm font-medium">{t('lodgingForm.kind')}</legend>
        <div className="grid grid-cols-3 gap-2">
          {LODGING_KINDS.map((option) => (
            <label
              key={option}
              className={cn(
                'flex min-h-11 cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border px-1 py-2 text-center text-xs has-focus-visible:ring-3 has-focus-visible:ring-ring/50',
                kind === option
                  ? 'border-primary bg-primary/10 font-medium text-foreground'
                  : 'text-muted-foreground hover:bg-accent',
              )}
            >
              <input type="radio" value={option} className="sr-only" {...form.register('kind')} />
              <LodgingIcon kind={option} className="size-5" />
              {t(`lodgingKind.${option}`)}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="flex flex-col gap-2">
        <Label htmlFor="lodging-name">{t('lodgingForm.name')}</Label>
        <Input
          id="lodging-name"
          maxLength={160}
          placeholder={t(`lodgingForm.namePlaceholder_${kind}`)}
          aria-invalid={errors.name ? true : undefined}
          {...form.register('name')}
        />
        {errors.name && <p className="text-sm text-destructive">{error(errors.name.message)}</p>}
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="lodging-address">{t('lodgingForm.address')}</Label>
        <Input
          id="lodging-address"
          maxLength={300}
          placeholder={t('lodgingForm.optional')}
          {...form.register('address')}
        />
      </div>

      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 text-sm font-medium">
          {t('lodgingForm.dates')}{' '}
          <span className="font-normal text-muted-foreground">
            ({t('lodgingForm.stopDates', { dates: stopDates })})
          </span>
        </legend>
        <div className="grid grid-cols-2 gap-3">
          <div className="flex min-w-0 flex-col gap-2">
            <Label htmlFor="lodging-check-in">{t('lodgingForm.checkIn')}</Label>
            <Input
              id="lodging-check-in"
              type="date"
              className="tabular"
              min={stop.arrivalDate}
              max={stop.departureDate}
              aria-invalid={errors.checkIn ? true : undefined}
              {...form.register('checkIn')}
            />
          </div>
          <div className="flex min-w-0 flex-col gap-2">
            <Label htmlFor="lodging-check-out">{t('lodgingForm.checkOut')}</Label>
            <Input
              id="lodging-check-out"
              type="date"
              className="tabular"
              min={stop.arrivalDate}
              max={stop.departureDate}
              aria-invalid={errors.checkOut ? true : undefined}
              {...form.register('checkOut')}
            />
          </div>
          {datesError && (
            <p role="alert" className="col-span-2 text-sm text-destructive">
              {error(datesError)}
            </p>
          )}
        </div>
      </fieldset>

      {booking && (
        <div className="flex flex-col gap-2">
          <Label htmlFor="lodging-board">{t('lodgingForm.boardType')}</Label>
          <NativeSelect id="lodging-board" {...form.register('boardType')}>
            <option value="">{t('lodgingForm.boardUnknown')}</option>
            {BOARD_TYPES.map((board) => (
              <option key={board} value={board}>
                {t(`board.${board}`)}
              </option>
            ))}
          </NativeSelect>
        </div>
      )}

      {booking && (
        <Controller
          control={form.control}
          name="priceTier"
          render={({ field }) => (
            <PriceTierInput
              name="lodging-price-tier"
              value={field.value}
              onChange={field.onChange}
            />
          )}
        />
      )}

      {booking && (
        <div className="flex flex-col gap-2">
          <Label htmlFor="lodging-booking">{t('lodgingForm.bookingRef')}</Label>
          <Input
            id="lodging-booking"
            maxLength={80}
            placeholder={t('lodgingForm.optional')}
            {...form.register('bookingRef')}
          />
        </div>
      )}

      <div className="flex flex-col gap-2">
        <Label htmlFor="lodging-notes">{t('lodgingForm.notes')}</Label>
        <Textarea
          id="lodging-notes"
          maxLength={2000}
          placeholder={t('lodgingForm.optional')}
          {...form.register('notes')}
        />
      </div>

      <div className="flex items-center justify-end gap-2">
        {extraActions}
        <Button type="button" variant="ghost" onClick={onCancel}>
          {t('lodgingForm.cancel')}
        </Button>
        <Button type="submit" disabled={submitting}>
          {t('lodgingForm.save')}
        </Button>
      </div>
    </form>
  )
}
