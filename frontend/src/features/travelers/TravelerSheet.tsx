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
import { useMe } from '@/features/auth/api'
import { ApiError } from '@/lib/api'
import { useCreateTraveler, useDeleteTraveler, useUpdateTraveler, type Traveler } from './api'
import { TravelerForm } from './TravelerForm'
import {
  emptyTraveler,
  toFormValues,
  toTravelerBody,
  type TravelerFormValues,
} from './traveler-form'

/**
 * Bottom sheet de criação/edição de viajante. Com `traveler`, edita (e permite excluir, exceto o
 * "eu"); sem, cria e devolve o novo viajante em `onCreated`.
 */
export function TravelerSheet({
  open,
  onOpenChange,
  traveler,
  onCreated,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  traveler?: Traveler
  onCreated?: (traveler: Traveler) => void
}) {
  const { t } = useTranslation()
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="bottom"
        className="mx-auto max-h-[92dvh] w-full max-w-lg overflow-y-auto rounded-t-xl"
      >
        <SheetHeader className="pb-0">
          <SheetTitle className="text-lg font-semibold">
            {t(traveler ? 'travelerForm.editTitle' : 'travelerForm.newTitle')}
          </SheetTitle>
          <SheetDescription>{t('travelerForm.description')}</SheetDescription>
        </SheetHeader>
        <div className="px-4 pb-4">
          {traveler ? (
            <EditTraveler traveler={traveler} onDone={() => onOpenChange(false)} />
          ) : (
            <NewTraveler
              onDone={(created) => {
                onOpenChange(false)
                if (created) onCreated?.(created)
              }}
            />
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}

function NewTraveler({ onDone }: { onDone: (created?: Traveler) => void }) {
  const { t } = useTranslation()
  const { data: me } = useMe()
  const create = useCreateTraveler()

  function onSubmit(values: TravelerFormValues) {
    create.mutate(toTravelerBody(values), {
      onSuccess: (created) => {
        toast.success(t('travelerForm.created'))
        onDone(created)
      },
      onError: () => toast.error(t('travelerForm.saveError')),
    })
  }

  return (
    <TravelerForm
      initialValues={emptyTraveler(me?.nationality)}
      submitting={create.isPending}
      onSubmit={onSubmit}
      onCancel={() => onDone()}
    />
  )
}

function EditTraveler({ traveler, onDone }: { traveler: Traveler; onDone: () => void }) {
  const { t } = useTranslation()
  const update = useUpdateTraveler(traveler.id)
  const [confirmDelete, setConfirmDelete] = useState(false)

  function onSubmit(values: TravelerFormValues) {
    update.mutate(
      { ...toTravelerBody(values), version: traveler.version },
      {
        onSuccess: () => {
          toast.success(t('travelerForm.saved'))
          onDone()
        },
        onError: (error) =>
          toast.error(
            t(
              error instanceof ApiError && error.status === 409
                ? 'travelerForm.conflict'
                : 'travelerForm.saveError',
            ),
          ),
      },
    )
  }

  return (
    <>
      <TravelerForm
        initialValues={toFormValues(traveler)}
        submitting={update.isPending}
        onSubmit={onSubmit}
        onCancel={onDone}
        extraActions={
          !traveler.me && (
            <Button
              type="button"
              variant="ghost"
              className="mr-auto text-destructive hover:text-destructive"
              onClick={() => setConfirmDelete(true)}
            >
              {t('travelerForm.delete')}
            </Button>
          )
        }
      />
      <DeleteTravelerDialog
        traveler={traveler}
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        onDeleted={onDone}
      />
    </>
  )
}

function DeleteTravelerDialog({
  traveler,
  open,
  onOpenChange,
  onDeleted,
}: {
  traveler: Traveler
  open: boolean
  onOpenChange: (open: boolean) => void
  onDeleted: () => void
}) {
  const { t } = useTranslation()
  const remove = useDeleteTraveler()

  function onConfirm() {
    remove.mutate(traveler.id, {
      onSuccess: () => {
        toast.success(t('travelerForm.deleted'))
        onOpenChange(false)
        onDeleted()
      },
      onError: () => toast.error(t('travelerForm.deleteError')),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('travelerForm.deleteTitle', { name: traveler.name })}</DialogTitle>
          <DialogDescription>{t('travelerForm.deleteDescription')}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('travelerForm.cancel')}</Button>
          </DialogClose>
          <Button variant="destructive" disabled={remove.isPending} onClick={onConfirm}>
            {t('travelerForm.deleteConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
