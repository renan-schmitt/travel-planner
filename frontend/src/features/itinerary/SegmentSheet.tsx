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
  useCreateSegment,
  useDeleteSegment,
  useUpdateSegment,
  type Segment,
  type Stop,
} from './api'
import { SegmentForm } from './SegmentForm'
import { emptySegment, toFormValues, toSegmentBody, type SegmentFormValues } from './segment-form'
import { useSegmentTitle } from './segment-title'

/** Bottom sheet de criação/edição de trecho. Com `segment`, edita (e permite excluir). */
export function SegmentSheet({
  tripId,
  stops,
  segments,
  segment,
  homeTz,
  preset,
  open,
  onOpenChange,
}: {
  tripId: number
  stops: Stop[]
  segments: Segment[]
  segment?: Segment
  homeTz: string
  /** Valores que substituem os sugeridos num trecho novo (ex.: "Resolver" de uma sugestão). */
  preset?: Partial<SegmentFormValues>
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
            {t(segment ? 'segmentForm.editTitle' : 'segmentForm.newTitle')}
          </SheetTitle>
          <SheetDescription>{t('segmentForm.description')}</SheetDescription>
        </SheetHeader>
        <div className="px-4 pb-4">
          {segment ? (
            <EditSegment
              tripId={tripId}
              segment={segment}
              stops={stops}
              homeTz={homeTz}
              onDone={close}
            />
          ) : (
            <NewSegment
              tripId={tripId}
              stops={stops}
              segments={segments}
              homeTz={homeTz}
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
  if (error instanceof ApiError && error.status === 409) return 'segmentForm.conflict'
  if (error instanceof ApiError && error.status === 400) return 'segmentForm.invalid'
  return 'segmentForm.saveError'
}

function NewSegment({
  tripId,
  stops,
  segments,
  homeTz,
  preset,
  onDone,
}: {
  tripId: number
  stops: Stop[]
  segments: Segment[]
  homeTz: string
  preset?: Partial<SegmentFormValues>
  onDone: () => void
}) {
  const { t } = useTranslation()
  const create = useCreateSegment(tripId)
  // Calculado uma vez: o formulário não deve ser reiniciado se a lista mudar enquanto está aberto.
  const [initialValues] = useState(() => ({
    ...emptySegment(stops, segments, homeTz),
    ...preset,
  }))

  function onSubmit(values: SegmentFormValues) {
    create.mutate(toSegmentBody(values), {
      onSuccess: () => {
        toast.success(t('segmentForm.created'))
        onDone()
      },
      onError: (error) => toast.error(t(saveErrorKey(error))),
    })
  }

  return (
    <SegmentForm
      initialValues={initialValues}
      stops={stops}
      homeTz={homeTz}
      submitting={create.isPending}
      onSubmit={onSubmit}
      onCancel={onDone}
    />
  )
}

function EditSegment({
  tripId,
  segment,
  stops,
  homeTz,
  onDone,
}: {
  tripId: number
  segment: Segment
  stops: Stop[]
  homeTz: string
  onDone: () => void
}) {
  const { t } = useTranslation()
  const update = useUpdateSegment(tripId, segment.id)
  const [confirmDelete, setConfirmDelete] = useState(false)

  function onSubmit(values: SegmentFormValues) {
    update.mutate(
      { ...toSegmentBody(values), version: segment.version },
      {
        onSuccess: () => {
          toast.success(t('segmentForm.saved'))
          onDone()
        },
        onError: (error) => toast.error(t(saveErrorKey(error))),
      },
    )
  }

  return (
    <>
      <SegmentForm
        initialValues={toFormValues(segment)}
        stops={stops}
        homeTz={homeTz}
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
            {t('segmentForm.delete')}
          </Button>
        }
      />
      <DeleteSegmentDialog
        tripId={tripId}
        segment={segment}
        stops={stops}
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        onDeleted={onDone}
      />
    </>
  )
}

function DeleteSegmentDialog({
  tripId,
  segment,
  stops,
  open,
  onOpenChange,
  onDeleted,
}: {
  tripId: number
  segment: Segment
  stops: Stop[]
  open: boolean
  onOpenChange: (open: boolean) => void
  onDeleted: () => void
}) {
  const { t } = useTranslation()
  const remove = useDeleteSegment(tripId)
  const title = useSegmentTitle(segment, stops)

  function onConfirm() {
    remove.mutate(segment.id, {
      onSuccess: () => {
        toast.success(t('segmentForm.deleted'))
        onOpenChange(false)
        onDeleted()
      },
      onError: () => toast.error(t('segmentForm.deleteError')),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('segmentForm.deleteTitle', { segment: title })}</DialogTitle>
          <DialogDescription>{t('segmentForm.deleteDescription')}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('segmentForm.cancel')}</Button>
          </DialogClose>
          <Button variant="destructive" disabled={remove.isPending} onClick={onConfirm}>
            {t('segmentForm.deleteConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
