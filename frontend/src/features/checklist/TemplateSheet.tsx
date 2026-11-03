import { zodResolver } from '@hookform/resolvers/zod'
import { Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { useFieldArray, useForm, useWatch, type FieldError } from 'react-hook-form'
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
import { ApiError } from '@/lib/api'
import { cn } from '@/lib/utils'
import { CATEGORIES } from './categories'
import {
  emptyTemplate,
  emptyTemplateItem,
  MAX_OFFSET_DAYS,
  templateSchema,
  toFormValues,
  toTemplateBody,
  type TemplateFormValues,
} from './template-form'
import { TEMPLATE_ICONS, templateIcons } from './template-icons'
import {
  useCreateTemplate,
  useDeleteTemplate,
  useUpdateTemplate,
  type ChecklistTemplate,
} from './templates-api'

/** Bottom sheet de criação/edição de template próprio: nome, ícone, descrição e itens. */
export function TemplateSheet({
  open,
  onOpenChange,
  template,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  template?: ChecklistTemplate
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
            {t(template ? 'templateForm.editTitle' : 'templateForm.newTitle')}
          </SheetTitle>
          <SheetDescription>{t('templateForm.description')}</SheetDescription>
        </SheetHeader>
        <div className="px-4 pb-4">
          {open &&
            (template ? (
              <EditTemplate
                key={template.id}
                template={template}
                onDone={() => onOpenChange(false)}
              />
            ) : (
              <NewTemplate onDone={() => onOpenChange(false)} />
            ))}
        </div>
      </SheetContent>
    </Sheet>
  )
}

function saveErrorKey(error: Error) {
  if (error instanceof ApiError && error.status === 409) return 'templateForm.conflict'
  if (error instanceof ApiError && error.status === 400) return 'templateForm.invalid'
  return 'templateForm.saveError'
}

function NewTemplate({ onDone }: { onDone: () => void }) {
  const { t } = useTranslation()
  const create = useCreateTemplate()
  return (
    <TemplateForm
      initialValues={emptyTemplate}
      submitting={create.isPending}
      onCancel={onDone}
      onSubmit={(values) =>
        create.mutate(toTemplateBody(values), {
          onSuccess: () => {
            toast.success(t('templateForm.created'))
            onDone()
          },
          onError: (e) => toast.error(t(saveErrorKey(e))),
        })
      }
    />
  )
}

function EditTemplate({ template, onDone }: { template: ChecklistTemplate; onDone: () => void }) {
  const { t } = useTranslation()
  const update = useUpdateTemplate(template)
  const [confirmDelete, setConfirmDelete] = useState(false)
  // Valores fixados ao abrir: a lista pode ser recarregada enquanto o formulário está aberto.
  const [initialValues] = useState(() => toFormValues(template))
  return (
    <>
      <TemplateForm
        initialValues={initialValues}
        submitting={update.isPending}
        onCancel={onDone}
        onDelete={() => setConfirmDelete(true)}
        onSubmit={(values) =>
          update.mutate(toTemplateBody(values), {
            onSuccess: () => {
              toast.success(t('templateForm.saved'))
              onDone()
            },
            onError: (e) => toast.error(t(saveErrorKey(e))),
          })
        }
      />
      <DeleteTemplateDialog
        template={template}
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        onDeleted={onDone}
      />
    </>
  )
}

function TemplateForm({
  initialValues,
  submitting,
  onSubmit,
  onCancel,
  onDelete,
}: {
  initialValues: TemplateFormValues
  submitting: boolean
  onSubmit: (values: TemplateFormValues) => void
  onCancel: () => void
  onDelete?: () => void
}) {
  const { t } = useTranslation()
  const form = useForm({ resolver: zodResolver(templateSchema), defaultValues: initialValues })
  const { fields, append, remove } = useFieldArray({ control: form.control, name: 'items' })
  const icon = useWatch({ control: form.control, name: 'icon' })
  const { errors } = form.formState
  const error = (e?: FieldError) =>
    e?.message ? t(`templateForm.${e.message as 'nameRequired'}`) : undefined

  function addItem() {
    append(emptyTemplateItem, { shouldFocus: true })
  }

  return (
    <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
      <div className="flex flex-col gap-2">
        <Label htmlFor="template-name">{t('templateForm.name')}</Label>
        <Input
          id="template-name"
          maxLength={120}
          placeholder={t('templateForm.namePlaceholder')}
          aria-invalid={errors.name ? true : undefined}
          {...form.register('name')}
        />
        {errors.name && <p className="text-sm text-destructive">{error(errors.name)}</p>}
      </div>

      <fieldset className="flex flex-col gap-2">
        <legend className="mb-2 text-sm leading-none font-medium">{t('templateForm.icon')}</legend>
        <div className="flex flex-wrap gap-1.5">
          {TEMPLATE_ICONS.map((key) => {
            const Icon = templateIcons[key]
            return (
              <label
                key={key}
                className={cn(
                  'flex size-11 cursor-pointer items-center justify-center rounded-full border has-focus-visible:ring-3 has-focus-visible:ring-ring/50',
                  icon === key && 'border-primary bg-primary text-primary-foreground',
                )}
              >
                <input
                  type="radio"
                  value={key}
                  className="sr-only"
                  aria-label={t(`templateIcon.${key}`)}
                  {...form.register('icon')}
                />
                <Icon className="size-5" strokeWidth={1.75} aria-hidden />
              </label>
            )
          })}
        </div>
      </fieldset>

      <div className="flex flex-col gap-2">
        <Label htmlFor="template-description">{t('templateForm.descriptionLabel')}</Label>
        <Textarea
          id="template-description"
          maxLength={500}
          rows={2}
          placeholder={t('templateForm.optional')}
          {...form.register('description')}
        />
      </div>

      <fieldset className="flex flex-col gap-2">
        <legend className="mb-1 text-sm leading-none font-medium">
          {t('templateForm.items', { count: fields.length })}
        </legend>
        <p className="text-sm text-muted-foreground">{t('templateForm.daysHint')}</p>
        <ul className="flex flex-col gap-3">
          {fields.map((field, index) => {
            const itemErrors = errors.items?.[index]
            const number = index + 1
            return (
              <li
                key={field.id}
                className="flex flex-col gap-2 rounded-lg border p-2"
                aria-label={t('templateForm.itemLabel', { number })}
              >
                <div className="flex gap-2">
                  <Input
                    maxLength={200}
                    placeholder={t('templateForm.itemTitle')}
                    aria-label={t('templateForm.itemTitleLabel', { number })}
                    aria-invalid={itemErrors?.title ? true : undefined}
                    {...form.register(`items.${index}.title`)}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-11 shrink-0 text-muted-foreground"
                    aria-label={t('templateForm.removeItem', { number })}
                    onClick={() => remove(index)}
                  >
                    <Trash2 className="size-5" strokeWidth={1.75} />
                  </Button>
                </div>
                <div className="grid grid-cols-[1fr_7rem] gap-2">
                  <NativeSelect
                    aria-label={t('templateForm.itemCategoryLabel', { number })}
                    {...form.register(`items.${index}.category`)}
                  >
                    {CATEGORIES.map((c) => (
                      <option key={c} value={c}>
                        {t(`checklistCategory.${c}`)}
                      </option>
                    ))}
                  </NativeSelect>
                  <Input
                    type="number"
                    inputMode="numeric"
                    min={-MAX_OFFSET_DAYS}
                    max={MAX_OFFSET_DAYS}
                    className="tabular"
                    placeholder={t('templateForm.daysPlaceholder')}
                    aria-label={t('templateForm.itemDaysLabel', { number })}
                    aria-invalid={itemErrors?.daysBefore ? true : undefined}
                    {...form.register(`items.${index}.daysBefore`)}
                  />
                </div>
                {(itemErrors?.title || itemErrors?.daysBefore) && (
                  <p className="text-sm text-destructive">
                    {error(itemErrors.title) ?? error(itemErrors.daysBefore)}
                  </p>
                )}
              </li>
            )
          })}
        </ul>
        {fields.length < 100 && (
          <Button type="button" variant="outline" className="self-start" onClick={addItem}>
            <Plus strokeWidth={2} aria-hidden />
            {t('templateForm.addItem')}
          </Button>
        )}
      </fieldset>

      <div className="flex items-center justify-end gap-2">
        {onDelete && (
          <Button
            type="button"
            variant="ghost"
            className="mr-auto text-destructive hover:text-destructive"
            onClick={onDelete}
          >
            {t('templateForm.delete')}
          </Button>
        )}
        <Button type="button" variant="ghost" onClick={onCancel}>
          {t('common.cancel')}
        </Button>
        <Button type="submit" disabled={submitting}>
          {t('templateForm.save')}
        </Button>
      </div>
    </form>
  )
}

function DeleteTemplateDialog({
  template,
  open,
  onOpenChange,
  onDeleted,
}: {
  template: ChecklistTemplate
  open: boolean
  onOpenChange: (open: boolean) => void
  onDeleted: () => void
}) {
  const { t } = useTranslation()
  const remove = useDeleteTemplate()

  function onConfirm() {
    remove.mutate(template, {
      onSuccess: () => {
        toast.success(t('templateForm.deleted'))
        onOpenChange(false)
        onDeleted()
      },
      onError: () => toast.error(t('templateForm.deleteError')),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('templateForm.deleteTitle', { name: template.name })}</DialogTitle>
          <DialogDescription>{t('templateForm.deleteDescription')}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('common.cancel')}</Button>
          </DialogClose>
          <Button variant="destructive" disabled={remove.isPending} onClick={onConfirm}>
            {t('templateForm.deleteConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
