import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
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
import type { Member } from '@/features/sharing/api'
import { ApiError } from '@/lib/api'
import { useDeleteChecklistItem, useUpdateChecklistItem, type ChecklistItem } from './api'
import { CATEGORIES } from './categories'
import {
  checklistItemSchema,
  toFormValues,
  toItemBody,
  type ChecklistItemFormValues,
} from './checklist-form'

/** Bottom sheet de edição (e exclusão) de um item: título, categoria, prazo, responsável, notas. */
export function ChecklistItemSheet({
  tripId,
  item,
  members,
  open,
  onOpenChange,
}: {
  tripId: number
  item?: ChecklistItem
  members: Member[]
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation()
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="bottom"
        className="mx-auto max-h-[92dvh] w-full max-w-lg overflow-y-auto rounded-t-xl"
      >
        <SheetHeader className="pb-0">
          <SheetTitle className="text-lg font-semibold">{t('checklistForm.editTitle')}</SheetTitle>
          <SheetDescription>{t('checklistForm.description')}</SheetDescription>
        </SheetHeader>
        <div className="px-4 pb-4">
          {item && (
            <EditItem
              key={item.id}
              tripId={tripId}
              item={item}
              members={members}
              onDone={() => onOpenChange(false)}
            />
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}

function saveErrorKey(error: Error) {
  if (error instanceof ApiError && error.status === 409) return 'checklistForm.conflict'
  if (error instanceof ApiError && error.status === 400) return 'checklistForm.invalid'
  return 'checklistForm.saveError'
}

function EditItem({
  tripId,
  item,
  members,
  onDone,
}: {
  tripId: number
  item: ChecklistItem
  members: Member[]
  onDone: () => void
}) {
  const { t } = useTranslation()
  const update = useUpdateChecklistItem(tripId, item)
  const [confirmDelete, setConfirmDelete] = useState(false)
  // Valores fixados ao abrir: a lista pode ser recarregada enquanto o formulário está aberto.
  const [initialValues] = useState(() => toFormValues(item))
  const form = useForm({ resolver: zodResolver(checklistItemSchema), values: initialValues })
  const { errors } = form.formState
  const people = members.filter((m) => m.userId != null)
  const error = (key?: string) => (key ? t(`checklistForm.${key as 'titleRequired'}`) : undefined)

  function onSubmit(values: ChecklistItemFormValues) {
    update.mutate(
      { ...toItemBody(values), version: item.version },
      {
        onSuccess: () => {
          toast.success(t('checklistForm.saved'))
          onDone()
        },
        onError: (e) => toast.error(t(saveErrorKey(e))),
      },
    )
  }

  return (
    <>
      <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
        <div className="flex flex-col gap-2">
          <Label htmlFor="checklist-title">{t('checklistForm.title')}</Label>
          <Input
            id="checklist-title"
            maxLength={200}
            aria-invalid={errors.title ? true : undefined}
            {...form.register('title')}
          />
          {errors.title && (
            <p className="text-sm text-destructive">{error(errors.title.message)}</p>
          )}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="flex min-w-0 flex-col gap-2">
            <Label htmlFor="checklist-category">{t('checklistForm.category')}</Label>
            <NativeSelect id="checklist-category" {...form.register('category')}>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {t(`checklistCategory.${c}`)}
                </option>
              ))}
            </NativeSelect>
          </div>
          <div className="flex min-w-0 flex-col gap-2">
            <Label htmlFor="checklist-due">{t('checklistForm.dueDate')}</Label>
            <Input
              id="checklist-due"
              type="date"
              className="tabular"
              {...form.register('dueDate')}
            />
          </div>
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="checklist-assignee">{t('checklistForm.assignee')}</Label>
          <NativeSelect id="checklist-assignee" {...form.register('assignee')}>
            <option value="">{t('checklistForm.nobody')}</option>
            {people.map((m) => (
              <option key={m.userId} value={String(m.userId)}>
                {m.me ? t('checklistForm.me', { name: m.name ?? m.email }) : (m.name ?? m.email)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="checklist-notes">{t('checklistForm.notes')}</Label>
          <Textarea
            id="checklist-notes"
            maxLength={2000}
            placeholder={t('checklistForm.optional')}
            {...form.register('notes')}
          />
        </div>
        <div className="flex items-center justify-end gap-2">
          <Button
            type="button"
            variant="ghost"
            className="mr-auto text-destructive hover:text-destructive"
            onClick={() => setConfirmDelete(true)}
          >
            {t('checklistForm.delete')}
          </Button>
          <Button type="button" variant="ghost" onClick={onDone}>
            {t('checklistForm.cancel')}
          </Button>
          <Button type="submit" disabled={update.isPending}>
            {t('checklistForm.save')}
          </Button>
        </div>
      </form>
      <DeleteItemDialog
        tripId={tripId}
        item={item}
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        onDeleted={onDone}
      />
    </>
  )
}

function DeleteItemDialog({
  tripId,
  item,
  open,
  onOpenChange,
  onDeleted,
}: {
  tripId: number
  item: ChecklistItem
  open: boolean
  onOpenChange: (open: boolean) => void
  onDeleted: () => void
}) {
  const { t } = useTranslation()
  const remove = useDeleteChecklistItem(tripId)

  function onConfirm() {
    remove.mutate(item, {
      onSuccess: () => {
        toast.success(t('checklistForm.deleted'))
        onOpenChange(false)
        onDeleted()
      },
      onError: () => toast.error(t('checklistForm.deleteError')),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('checklistForm.deleteTitle', { title: item.title })}</DialogTitle>
          <DialogDescription>{t('checklistForm.deleteDescription')}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('checklistForm.cancel')}</Button>
          </DialogClose>
          <Button variant="destructive" disabled={remove.isPending} onClick={onConfirm}>
            {t('checklistForm.deleteConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
