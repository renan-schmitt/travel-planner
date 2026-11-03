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
import { ApiError } from '@/lib/api'
import {
  useCreateLodging,
  useDeleteLodging,
  useUpdateLodging,
  type Lodging,
  type Stop,
} from './api'
import { LodgingForm } from './LodgingForm'
import { emptyLodging, toFormValues, toLodgingBody, type LodgingFormValues } from './lodging-form'

/** Bottom sheet de criação/edição de hospedagem numa parada. Com `lodging`, edita (e exclui). */
export function LodgingSheet({
  tripId,
  stop,
  lodgings,
  lodging,
  preset,
  open,
  onOpenChange,
}: {
  tripId: number
  stop?: Stop
  lodgings: Lodging[]
  lodging?: Lodging
  /** Valores que substituem os sugeridos numa hospedagem nova (ex.: "Resolver" de uma sugestão). */
  preset?: Partial<LodgingFormValues>
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
            {t(lodging ? 'lodgingForm.editTitle' : 'lodgingForm.newTitle')}
          </SheetTitle>
          <SheetDescription>
            {stop ? t('lodgingForm.description', { city: stop.cityName }) : ''}
          </SheetDescription>
        </SheetHeader>
        <div className="px-4 pb-4">
          {stop && lodging && (
            <EditLodging tripId={tripId} stop={stop} lodging={lodging} onDone={close} />
          )}
          {stop && !lodging && (
            <NewLodging
              tripId={tripId}
              stop={stop}
              lodgings={lodgings}
              preset={preset}
              onDone={close}
            />
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}

function saveErrorKey(error: Error) {
  if (error instanceof ApiError && error.status === 409) return 'lodgingForm.conflict'
  if (error instanceof ApiError && error.status === 400) return 'lodgingForm.invalid'
  return 'lodgingForm.saveError'
}

function NewLodging({
  tripId,
  stop,
  lodgings,
  preset,
  onDone,
}: {
  tripId: number
  stop: Stop
  lodgings: Lodging[]
  preset?: Partial<LodgingFormValues>
  onDone: () => void
}) {
  const { t } = useTranslation()
  const create = useCreateLodging(tripId)
  // Calculado uma vez: o formulário não deve ser reiniciado se a lista mudar enquanto está aberto.
  const [initialValues] = useState(() => ({ ...emptyLodging(stop, lodgings), ...preset }))

  function onSubmit(values: LodgingFormValues) {
    create.mutate(
      { stopId: stop.id, body: toLodgingBody(values) },
      {
        onSuccess: () => {
          toast.success(t('lodgingForm.created'))
          onDone()
        },
        onError: (error) => toast.error(t(saveErrorKey(error))),
      },
    )
  }

  return (
    <LodgingForm
      stop={stop}
      initialValues={initialValues}
      submitting={create.isPending}
      onSubmit={onSubmit}
      onCancel={onDone}
    />
  )
}

function EditLodging({
  tripId,
  stop,
  lodging,
  onDone,
}: {
  tripId: number
  stop: Stop
  lodging: Lodging
  onDone: () => void
}) {
  const { t } = useTranslation()
  const update = useUpdateLodging(tripId, lodging)
  const [confirmDelete, setConfirmDelete] = useState(false)

  function onSubmit(values: LodgingFormValues) {
    update.mutate(
      { ...toLodgingBody(values), version: lodging.version },
      {
        onSuccess: () => {
          toast.success(t('lodgingForm.saved'))
          onDone()
        },
        onError: (error) => toast.error(t(saveErrorKey(error))),
      },
    )
  }

  return (
    <>
      <LodgingForm
        stop={stop}
        initialValues={toFormValues(lodging)}
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
            {t('lodgingForm.delete')}
          </Button>
        }
      />
      <DeleteLodgingDialog
        tripId={tripId}
        lodging={lodging}
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        onDeleted={onDone}
      />
    </>
  )
}

function DeleteLodgingDialog({
  tripId,
  lodging,
  open,
  onOpenChange,
  onDeleted,
}: {
  tripId: number
  lodging: Lodging
  open: boolean
  onOpenChange: (open: boolean) => void
  onDeleted: () => void
}) {
  const { t } = useTranslation()
  const remove = useDeleteLodging(tripId)

  function onConfirm() {
    remove.mutate(lodging, {
      onSuccess: () => {
        toast.success(t('lodgingForm.deleted'))
        onOpenChange(false)
        onDeleted()
      },
      onError: () => toast.error(t('lodgingForm.deleteError')),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('lodgingForm.deleteTitle', { name: lodging.name })}</DialogTitle>
          <DialogDescription>{t('lodgingForm.deleteDescription')}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('lodgingForm.cancel')}</Button>
          </DialogClose>
          <Button variant="destructive" disabled={remove.isPending} onClick={onConfirm}>
            {t('lodgingForm.deleteConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
