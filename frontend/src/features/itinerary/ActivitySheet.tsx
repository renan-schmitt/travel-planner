import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import type { SavedPlace } from '@/features/places/api'
import { ApiError } from '@/lib/api'
import { formatDayHeader } from '@/lib/dates'
import {
  activitySchema,
  emptyActivity,
  toActivityBody,
  toFormValues,
  type ActivityFormValues,
} from './activity-form'
import {
  useCreateActivity,
  useDeleteActivity,
  useUpdateActivity,
  type ItineraryActivity,
  type ItineraryDay,
} from './api'

/** O que o sheet está fazendo: nova atividade num dia (opcionalmente de um favorito) ou edição. */
export type ActivitySheetTarget =
  | { day: string; savedPlaceId?: number; activity?: undefined }
  | { activity: ItineraryActivity; day?: undefined; savedPlaceId?: undefined }

type ErrorKey =
  'dayOutside' | 'titleRequired' | 'invalidTime' | 'startRequired' | 'endBeforeStart' | 'tooLong'

/** Bottom sheet de criação/edição de atividade da timeline. Na edição, também exclui. */
export function ActivitySheet({
  tripId,
  days,
  savedPlaces,
  target,
  onClose,
}: {
  tripId: number
  days: ItineraryDay[]
  savedPlaces: SavedPlace[]
  target?: ActivitySheetTarget
  onClose: () => void
}) {
  const { t } = useTranslation()
  const activity = target?.activity
  return (
    <Sheet open={target !== undefined} onOpenChange={(open) => !open && onClose()}>
      <SheetContent
        side="bottom"
        className="mx-auto max-h-[92dvh] w-full max-w-lg overflow-y-auto rounded-t-xl"
      >
        <SheetHeader className="pb-0">
          <SheetTitle className="text-lg font-semibold">
            {t(activity ? 'activities.editTitle' : 'activities.newTitle')}
          </SheetTitle>
          <SheetDescription>{t('activities.description')}</SheetDescription>
        </SheetHeader>
        <div className="px-4 pb-4">
          {target && (
            <ActivityEditor
              key={activity ? `${activity.id}-${activity.version}` : `new-${target.day}`}
              tripId={tripId}
              days={days}
              savedPlaces={savedPlaces}
              target={target}
              onDone={onClose}
            />
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}

function saveErrorKey(error: Error) {
  if (error instanceof ApiError && error.status === 409) return 'activities.conflict'
  if (error instanceof ApiError && error.status === 400) return 'activities.invalid'
  return 'activities.saveError'
}

function ActivityEditor({
  tripId,
  days,
  savedPlaces,
  target,
  onDone,
}: {
  tripId: number
  days: ItineraryDay[]
  savedPlaces: SavedPlace[]
  target: ActivitySheetTarget
  onDone: () => void
}) {
  const { t } = useTranslation()
  const create = useCreateActivity(tripId)
  const update = useUpdateActivity(tripId)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const activity = target.activity
  const dates = useMemo(() => days.map((d) => d.date), [days])
  const schema = useMemo(() => activitySchema(dates), [dates])
  const [initialValues] = useState(() =>
    activity ? toFormValues(activity) : emptyActivity(target.day!, target.savedPlaceId),
  )
  const form = useForm({ resolver: zodResolver(schema), defaultValues: initialValues })
  const { errors } = form.formState
  const error = (key?: string) => (key ? t(`activities.errors.${key as ErrorKey}`) : undefined)
  const selectedPlace = useWatch({ control: form.control, name: 'savedPlaceId' })
  const day = useWatch({ control: form.control, name: 'day' })
  const placeName = savedPlaces.find((p) => String(p.id) === selectedPlace)?.name
  const groups = usePlaceGroups(days, savedPlaces, day)
  const pending = create.isPending || update.isPending

  function onSubmit(values: ActivityFormValues) {
    const body = toActivityBody(values)
    const options = {
      onSuccess: () => {
        toast.success(t(activity ? 'activities.saved' : 'activities.created'))
        onDone()
      },
      onError: (e: Error) => toast.error(t(saveErrorKey(e))),
    }
    if (activity) {
      update.mutate({ id: activity.id, body: { ...body, version: activity.version } }, options)
    } else {
      create.mutate(body, options)
    }
  }

  return (
    <>
      <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
        <div className="flex flex-col gap-2">
          <Label htmlFor="activity-place">{t('activities.place')}</Label>
          <NativeSelect id="activity-place" {...form.register('savedPlaceId')}>
            <option value="">{t('activities.noPlace')}</option>
            {groups.map((g) => (
              <optgroup key={g.label} label={g.label}>
                {g.places.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </optgroup>
            ))}
          </NativeSelect>
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="activity-title">{t('activities.titleLabel')}</Label>
          <Input
            id="activity-title"
            placeholder={placeName ?? t('activities.titlePlaceholder')}
            aria-invalid={errors.title ? true : undefined}
            {...form.register('title')}
          />
          {errors.title && (
            <p className="text-sm text-destructive">{error(errors.title.message)}</p>
          )}
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="activity-day">{t('activities.day')}</Label>
          <NativeSelect
            id="activity-day"
            aria-invalid={errors.day ? true : undefined}
            {...form.register('day')}
          >
            {!dates.includes(initialValues.day) && (
              <option value={initialValues.day}>{formatDayHeader(initialValues.day)}</option>
            )}
            {days.map((d) => (
              <option key={d.date} value={d.date}>
                {formatDayHeader(d.date)}
                {d.stops.length > 0 ? ` · ${d.stops.map((s) => s.cityName).join(' → ')}` : ''}
              </option>
            ))}
          </NativeSelect>
          {errors.day && <p className="text-sm text-destructive">{error(errors.day.message)}</p>}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="flex flex-col gap-2">
            <Label htmlFor="activity-start">{t('activities.start')}</Label>
            <Input
              id="activity-start"
              type="time"
              aria-invalid={errors.startTime ? true : undefined}
              {...form.register('startTime')}
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="activity-end">{t('activities.end')}</Label>
            <Input
              id="activity-end"
              type="time"
              aria-invalid={errors.endTime ? true : undefined}
              {...form.register('endTime')}
            />
          </div>
        </div>
        {(errors.startTime ?? errors.endTime) && (
          <p className="-mt-2 text-sm text-destructive">
            {error(errors.startTime?.message ?? errors.endTime?.message)}
          </p>
        )}
        <p className="-mt-2 text-xs text-muted-foreground">{t('activities.timeHint')}</p>
        <div className="flex flex-col gap-2">
          <Label htmlFor="activity-notes">{t('activities.notes')}</Label>
          <Textarea
            id="activity-notes"
            aria-invalid={errors.notes ? true : undefined}
            {...form.register('notes')}
          />
          {errors.notes && (
            <p className="text-sm text-destructive">{error(errors.notes.message)}</p>
          )}
        </div>
        <div className="flex flex-wrap justify-end gap-2 pt-2">
          {activity && (
            <Button
              type="button"
              variant="ghost"
              className="mr-auto text-destructive hover:text-destructive"
              onClick={() => setConfirmDelete(true)}
            >
              {t('activities.delete')}
            </Button>
          )}
          <Button type="button" variant="ghost" onClick={onDone}>
            {t('activities.cancel')}
          </Button>
          <Button type="submit" disabled={pending}>
            {t(activity ? 'activities.save' : 'activities.add')}
          </Button>
        </div>
      </form>
      {activity && (
        <DeleteActivityDialog
          tripId={tripId}
          activity={activity}
          open={confirmDelete}
          onOpenChange={setConfirmDelete}
          onDeleted={onDone}
        />
      )}
    </>
  )
}

/**
 * Favoritos agrupados por cidade; as cidades do dia escolhido primeiro (é onde se está nesse dia).
 */
function usePlaceGroups(days: ItineraryDay[], savedPlaces: SavedPlace[], day: string) {
  const { t } = useTranslation()
  return useMemo(() => {
    const cities = new Map<number, string>()
    for (const d of days) for (const s of d.stops) cities.set(s.id, s.cityName)
    const today = new Set(days.find((d) => d.date === day)?.stops.map((s) => s.id))
    const byStop = new Map<number, SavedPlace[]>()
    for (const p of savedPlaces) byStop.set(p.stopId, [...(byStop.get(p.stopId) ?? []), p])
    return [...byStop.entries()]
      .sort(([a], [b]) => Number(today.has(b)) - Number(today.has(a)))
      .map(([stopId, places]) => ({
        label: cities.get(stopId) ?? t('activities.otherCity'),
        places,
      }))
  }, [days, savedPlaces, day, t])
}

function DeleteActivityDialog({
  tripId,
  activity,
  open,
  onOpenChange,
  onDeleted,
}: {
  tripId: number
  activity: ItineraryActivity
  open: boolean
  onOpenChange: (open: boolean) => void
  onDeleted: () => void
}) {
  const { t } = useTranslation()
  const remove = useDeleteActivity(tripId)

  function onConfirm() {
    remove.mutate(activity.id, {
      onSuccess: () => {
        toast.success(t('activities.deleted'))
        onOpenChange(false)
        onDeleted()
      },
      onError: () => toast.error(t('activities.deleteError')),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('activities.deleteTitle', { title: activity.title })}</DialogTitle>
          <DialogDescription>{t('activities.deleteDescription')}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('activities.cancel')}</Button>
          </DialogClose>
          <Button variant="destructive" disabled={remove.isPending} onClick={onConfirm}>
            {t('activities.deleteConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
