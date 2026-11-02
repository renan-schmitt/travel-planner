import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { cn } from '@/lib/utils'
import { timeZoneLabel, timeZoneOptions } from '@/lib/timezones'
import type { Stop } from './api'
import {
  HOME,
  localEndpoint,
  parseEndpoint,
  segmentSchema,
  stopEndpoint,
  timeZoneOf,
  TRANSPORT_TYPES,
  type SegmentFormValues,
} from './segment-form'
import { TransportIcon } from './TransportIcon'

type ErrorKey =
  | 'sameEnds'
  | 'labelTooLong'
  | 'departDateRequired'
  | 'departTimeRequired'
  | 'timezoneRequired'
  | 'arrivalIncomplete'
  | 'arrivalBeforeDeparture'
  | 'tooLong'

/**
 * Tipo (ícones), origem/destino entre "Casa" e as paradas (ou trecho local numa parada), partida e
 * chegada no relógio local com o fuso de cada ponta, companhia, código, reserva e observações.
 */
export function SegmentForm({
  initialValues,
  stops,
  homeTz,
  submitting,
  onSubmit,
  onCancel,
  extraActions,
}: {
  initialValues: SegmentFormValues
  stops: Stop[]
  /** Fuso da cidade de origem do usuário (sugerido nas pontas "Casa"). */
  homeTz: string
  submitting: boolean
  onSubmit: (values: SegmentFormValues) => void
  onCancel: () => void
  extraActions?: React.ReactNode
}) {
  const { t } = useTranslation()
  const form = useForm({ resolver: zodResolver(segmentSchema), values: initialValues })
  const { errors } = form.formState
  const [type, origin] = useWatch({ control: form.control, name: ['type', 'origin'] })
  const local = parseEndpoint(origin).kind === 'local'
  const zones = useMemo(() => timeZoneOptions(), [])
  const error = (key?: string) => (key ? t(`segmentForm.${key as ErrorKey}`) : undefined)

  // Ao trocar uma ponta, sugere o fuso dela (o usuário ainda pode trocar).
  const originField = form.register('origin', {
    onChange: (event: React.ChangeEvent<HTMLSelectElement>) => {
      const value = event.target.value
      const zone = timeZoneOf(value, stops, homeTz)
      form.setValue('departTz', zone)
      if (parseEndpoint(value).kind === 'local') form.setValue('arriveTz', zone)
    },
  })
  const destinationField = form.register('destination', {
    onChange: (event: React.ChangeEvent<HTMLSelectElement>) =>
      form.setValue('arriveTz', timeZoneOf(event.target.value, stops, homeTz)),
  })

  function zoneSelect(id: string, name: 'departTz' | 'arriveTz', label: string) {
    return (
      <div className="flex min-w-0 flex-col gap-2">
        <Label htmlFor={id}>{label}</Label>
        <NativeSelect
          id={id}
          aria-invalid={errors[name] ? true : undefined}
          {...form.register(name)}
        >
          <option value="">{t('segmentForm.chooseTimezone')}</option>
          {zones.map((zone) => (
            <option key={zone} value={zone}>
              {timeZoneLabel(zone)}
            </option>
          ))}
        </NativeSelect>
        {errors[name] && <p className="text-sm text-destructive">{error(errors[name].message)}</p>}
      </div>
    )
  }

  return (
    <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
      <fieldset className="flex flex-col gap-2">
        <legend className="mb-2 text-sm font-medium">{t('segmentForm.type')}</legend>
        <div className="grid grid-cols-3 gap-2">
          {TRANSPORT_TYPES.map((option) => (
            <label
              key={option}
              className={cn(
                'flex min-h-11 cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border px-1 py-2 text-center text-xs has-focus-visible:ring-3 has-focus-visible:ring-ring/50',
                type === option
                  ? 'border-primary bg-primary/10 font-medium text-foreground'
                  : 'text-muted-foreground hover:bg-accent',
              )}
            >
              <input type="radio" value={option} className="sr-only" {...form.register('type')} />
              <TransportIcon type={option} className="size-5" />
              {t(`transport.${option}`)}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="segment-origin">{t('segmentForm.origin')}</Label>
          <NativeSelect id="segment-origin" {...originField}>
            <option value={HOME}>{t('segments.home')}</option>
            {stops.map((stop) => (
              <option key={stop.id} value={stopEndpoint(stop.id)}>
                {stop.cityName}
              </option>
            ))}
            <optgroup label={t('segmentForm.localGroup')}>
              {stops.map((stop) => (
                <option key={stop.id} value={localEndpoint(stop.id)}>
                  {t('segmentForm.localIn', { city: stop.cityName })}
                </option>
              ))}
            </optgroup>
          </NativeSelect>
        </div>
        {!local && (
          <div className="flex min-w-0 flex-col gap-2">
            <Label htmlFor="segment-destination">{t('segmentForm.destination')}</Label>
            <NativeSelect
              id="segment-destination"
              aria-invalid={errors.destination ? true : undefined}
              {...destinationField}
            >
              <option value={HOME}>{t('segments.home')}</option>
              {stops.map((stop) => (
                <option key={stop.id} value={stopEndpoint(stop.id)}>
                  {stop.cityName}
                </option>
              ))}
            </NativeSelect>
            {errors.destination && (
              <p className="text-sm text-destructive">{error(errors.destination.message)}</p>
            )}
          </div>
        )}
        {local && (
          <p className="text-sm text-muted-foreground sm:self-end">{t('segmentForm.localHint')}</p>
        )}
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="segment-origin-label">{t('segmentForm.originLabel')}</Label>
          <Input
            id="segment-origin-label"
            maxLength={160}
            placeholder={t(
              local ? 'segmentForm.localFromPlaceholder' : 'segmentForm.fromPlaceholder',
            )}
            {...form.register('originLabel')}
          />
        </div>
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="segment-destination-label">{t('segmentForm.destinationLabel')}</Label>
          <Input
            id="segment-destination-label"
            maxLength={160}
            placeholder={t(local ? 'segmentForm.localToPlaceholder' : 'segmentForm.toPlaceholder')}
            {...form.register('destinationLabel')}
          />
        </div>
      </div>

      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 text-sm font-medium">{t('segmentForm.departure')}</legend>
        <div className="grid grid-cols-2 gap-3">
          <div className="flex min-w-0 flex-col gap-2">
            <Label htmlFor="segment-depart-date">{t('segmentForm.departDate')}</Label>
            <Input
              id="segment-depart-date"
              type="date"
              className="tabular"
              aria-invalid={errors.departDate ? true : undefined}
              {...form.register('departDate')}
            />
          </div>
          <div className="flex min-w-0 flex-col gap-2">
            <Label htmlFor="segment-depart-time">{t('segmentForm.departTime')}</Label>
            <Input
              id="segment-depart-time"
              type="time"
              className="tabular"
              aria-invalid={errors.departTime ? true : undefined}
              {...form.register('departTime')}
            />
          </div>
          {(errors.departDate || errors.departTime) && (
            <p className="col-span-2 text-sm text-destructive">
              {error(errors.departDate?.message ?? errors.departTime?.message)}
            </p>
          )}
        </div>
        {zoneSelect('segment-depart-tz', 'departTz', t('segmentForm.departTz'))}
      </fieldset>

      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 text-sm font-medium">
          {t('segmentForm.arrival')}{' '}
          <span className="font-normal text-muted-foreground">({t('segmentForm.optional')})</span>
        </legend>
        <div className="grid grid-cols-2 gap-3">
          <div className="flex min-w-0 flex-col gap-2">
            <Label htmlFor="segment-arrive-date">{t('segmentForm.arriveDate')}</Label>
            <Input
              id="segment-arrive-date"
              type="date"
              className="tabular"
              aria-invalid={errors.arriveDate ? true : undefined}
              {...form.register('arriveDate')}
            />
          </div>
          <div className="flex min-w-0 flex-col gap-2">
            <Label htmlFor="segment-arrive-time">{t('segmentForm.arriveTime')}</Label>
            <Input
              id="segment-arrive-time"
              type="time"
              className="tabular"
              aria-invalid={errors.arriveDate ? true : undefined}
              {...form.register('arriveTime')}
            />
          </div>
          {errors.arriveDate && (
            <p role="alert" className="col-span-2 text-sm text-destructive">
              {error(errors.arriveDate.message)}
            </p>
          )}
        </div>
        {zoneSelect('segment-arrive-tz', 'arriveTz', t('segmentForm.arriveTz'))}
      </fieldset>

      <div className="grid grid-cols-2 gap-3">
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="segment-carrier">{t('segmentForm.carrier')}</Label>
          <Input
            id="segment-carrier"
            maxLength={120}
            placeholder={t('segmentForm.optional')}
            {...form.register('carrier')}
          />
        </div>
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="segment-code">
            {t(type === 'FLIGHT' ? 'segmentForm.flightNumber' : 'segmentForm.code')}
          </Label>
          <Input
            id="segment-code"
            maxLength={40}
            placeholder={t('segmentForm.optional')}
            {...form.register('code')}
          />
        </div>
        <div className="col-span-2 flex min-w-0 flex-col gap-2">
          <Label htmlFor="segment-booking">{t('segmentForm.bookingRef')}</Label>
          <Input
            id="segment-booking"
            maxLength={80}
            placeholder={t('segmentForm.optional')}
            {...form.register('bookingRef')}
          />
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="segment-notes">{t('segmentForm.notes')}</Label>
        <Textarea
          id="segment-notes"
          maxLength={2000}
          placeholder={t('segmentForm.optional')}
          {...form.register('notes')}
        />
      </div>

      <div className="flex items-center justify-end gap-2">
        {extraActions}
        <Button type="button" variant="ghost" onClick={onCancel}>
          {t('segmentForm.cancel')}
        </Button>
        <Button type="submit" disabled={submitting}>
          {t('segmentForm.save')}
        </Button>
      </div>
    </form>
  )
}
