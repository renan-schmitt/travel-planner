import { useState } from 'react'
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
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import type { Trip } from '@/features/trips/api'
import { ApiError } from '@/lib/api'
import { useCreateStop, useDeleteStop, useUpdateStop, type Stop } from './api'
import { StopForm } from './StopForm'
import { emptyStop, toFormValues, toStopBody, type StopFormValues } from './stop-form'

/** Bottom sheet de criação/edição de parada. Com `stop`, edita (e permite excluir). */
export function StopSheet({
  trip,
  stops,
  stop,
  open,
  onOpenChange,
}: {
  trip: Trip
  stops: Stop[]
  stop?: Stop
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation()
  const close = () => onOpenChange(false)
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="bottom"
        className="mx-auto max-h-[92dvh] w-full max-w-lg overflow-y-auto rounded-t-xl"
      >
        <SheetHeader className="pb-0">
          <SheetTitle className="text-lg font-semibold">
            {t(stop ? 'stopForm.editTitle' : 'stopForm.newTitle')}
          </SheetTitle>
          <SheetDescription>{t('stopForm.description')}</SheetDescription>
        </SheetHeader>
        <div className="px-4 pb-4">
          {stop ? (
            <EditStop tripId={trip.id} stop={stop} stops={stops} onDone={close} />
          ) : (
            <NewStop trip={trip} stops={stops} onDone={close} />
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}

function saveErrorKey(error: Error) {
  if (error instanceof ApiError && error.status === 409) return 'stopForm.conflict'
  if (error instanceof ApiError && error.status === 400) return 'stopForm.invalid'
  return 'stopForm.saveError'
}

function NewStop({ trip, stops, onDone }: { trip: Trip; stops: Stop[]; onDone: () => void }) {
  const { t } = useTranslation()
  const create = useCreateStop(trip.id)

  function onSubmit(values: StopFormValues) {
    create.mutate(toStopBody(values), {
      onSuccess: () => {
        toast.success(t('stopForm.created'))
        onDone()
      },
      onError: (error) => toast.error(t(saveErrorKey(error))),
    })
  }

  return (
    <StopForm
      initialValues={emptyStop(stops, trip.startDate)}
      otherStops={stops}
      submitting={create.isPending}
      onSubmit={onSubmit}
      onCancel={onDone}
    />
  )
}

function EditStop({
  tripId,
  stop,
  stops,
  onDone,
}: {
  tripId: number
  stop: Stop
  stops: Stop[]
  onDone: () => void
}) {
  const { t } = useTranslation()
  const update = useUpdateStop(tripId, stop.id)
  const [confirmDelete, setConfirmDelete] = useState(false)

  function onSubmit(values: StopFormValues) {
    update.mutate(
      { ...toStopBody(values), version: stop.version },
      {
        onSuccess: () => {
          toast.success(t('stopForm.saved'))
          onDone()
        },
        onError: (error) => toast.error(t(saveErrorKey(error))),
      },
    )
  }

  return (
    <>
      <StopForm
        initialValues={toFormValues(stop)}
        otherStops={stops.filter((s) => s.id !== stop.id)}
        submitting={update.isPending}
        onSubmit={onSubmit}
        onCancel={onDone}
        extraActions={
          <Button
            type="button"
            variant="ghost"
            className="mr-auto text-destructive hover:text-destructive"
            onClick={() => setConfirmDelete(true)}
          >
            {t('stopForm.delete')}
          </Button>
        }
      />
      <DeleteStopDialog
        tripId={tripId}
        stop={stop}
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        onDeleted={onDone}
      />
    </>
  )
}

function DeleteStopDialog({
  tripId,
  stop,
  open,
  onOpenChange,
  onDeleted,
}: {
  tripId: number
  stop: Stop
  open: boolean
  onOpenChange: (open: boolean) => void
  onDeleted: () => void
}) {
  const { t } = useTranslation()
  const remove = useDeleteStop(tripId)

  function onConfirm() {
    remove.mutate(stop.id, {
      onSuccess: () => {
        toast.success(t('stopForm.deleted'))
        onOpenChange(false)
        onDeleted()
      },
      onError: () => toast.error(t('stopForm.deleteError')),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('stopForm.deleteTitle', { city: stop.cityName })}</DialogTitle>
          <DialogDescription>{t('stopForm.deleteDescription')}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('stopForm.cancel')}</Button>
          </DialogClose>
          <Button variant="destructive" disabled={remove.isPending} onClick={onConfirm}>
            {t('stopForm.deleteConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
