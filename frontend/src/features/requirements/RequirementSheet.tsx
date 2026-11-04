import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
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
import { countryOptions } from '@/lib/countries'
import { formatDate } from '@/lib/dates'
import {
  REQUIREMENT_TYPES,
  useCreateRequirement,
  useDeleteRequirement,
  useSetRequirementPublished,
  useUpdateRequirement,
  type Requirement,
} from './api'
import {
  emptyRequirement,
  requirementSchema,
  toBody,
  toFormValues,
  type RequirementFormValues,
} from './requirement-form'

function errorKey(error: Error) {
  if (error instanceof ApiError && error.status === 409) return 'adminRequirements.conflict'
  if (error instanceof ApiError && error.status === 400) return 'adminRequirements.invalid'
  return 'adminRequirements.saveError'
}

/** Bottom sheet de criar/editar um requisito, com publicar/despublicar e excluir. */
export function RequirementSheet({
  open,
  requirement,
  defaultCountry,
  onClose,
}: {
  open: boolean
  requirement?: Requirement
  defaultCountry: string
  onClose: () => void
}) {
  const { t } = useTranslation()
  return (
    <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
      <SheetContent
        side="bottom"
        className="mx-auto max-h-[92dvh] w-full max-w-lg overflow-y-auto rounded-t-xl"
      >
        <SheetHeader className="pb-0">
          <SheetTitle className="text-lg font-semibold">
            {t(requirement ? 'adminRequirements.editTitle' : 'adminRequirements.newTitle')}
          </SheetTitle>
          <SheetDescription>{t('adminRequirements.formHint')}</SheetDescription>
        </SheetHeader>
        {open && (
          <RequirementForm
            key={requirement ? `${requirement.id}-${requirement.version}` : 'new'}
            requirement={requirement}
            defaultCountry={defaultCountry}
            onDone={onClose}
          />
        )}
      </SheetContent>
    </Sheet>
  )
}

function RequirementForm({
  requirement,
  defaultCountry,
  onDone,
}: {
  requirement?: Requirement
  defaultCountry: string
  onDone: () => void
}) {
  const { t, i18n } = useTranslation()
  const countries = useMemo(() => countryOptions(i18n.language), [i18n.language])
  const create = useCreateRequirement()
  const update = useUpdateRequirement()
  const remove = useDeleteRequirement()
  const publish = useSetRequirementPublished()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [initialValues] = useState(() =>
    requirement ? toFormValues(requirement) : emptyRequirement(defaultCountry),
  )
  const form = useForm({ resolver: zodResolver(requirementSchema), values: initialValues })
  const { errors } = form.formState
  const busy = create.isPending || update.isPending || remove.isPending || publish.isPending
  const message = (key?: string) =>
    key ? t(`adminRequirements.errors.${key as 'titleRequired'}`) : undefined

  function onSubmit(values: RequirementFormValues) {
    const body = toBody(values)
    const options = {
      onSuccess: () => {
        toast.success(t('adminRequirements.saved'))
        onDone()
      },
      onError: (e: Error) => toast.error(t(errorKey(e))),
    }
    if (requirement) update.mutate({ requirement, body }, options)
    else create.mutate(body, options)
  }

  function onPublish(value: boolean) {
    if (!requirement) return
    publish.mutate(
      { requirement, publish: value },
      {
        onSuccess: () => {
          toast.success(t(value ? 'adminRequirements.published' : 'adminRequirements.unpublished'))
          onDone()
        },
        onError: (e) => toast.error(t(errorKey(e))),
      },
    )
  }

  function onDelete() {
    if (!requirement) return
    remove.mutate(requirement, {
      onSuccess: () => {
        toast.success(t('adminRequirements.deleted'))
        onDone()
      },
      onError: (e) => toast.error(t(errorKey(e))),
    })
  }

  const countrySelect = (id: string, name: 'countryCode' | 'nationality', label: string) => (
    <div className="flex min-w-0 flex-col gap-2">
      <Label htmlFor={id}>{label}</Label>
      <NativeSelect id={id} aria-invalid={errors[name] ? true : undefined} {...form.register(name)}>
        <option value="">{t('adminRequirements.choose')}</option>
        {countries.map((c) => (
          <option key={c.code} value={c.code}>
            {c.flag} {c.name}
          </option>
        ))}
      </NativeSelect>
      {errors[name] && <p className="text-sm text-destructive">{message(errors[name].message)}</p>}
    </div>
  )

  return (
    <form
      noValidate
      className="flex flex-col gap-4 px-4 pb-4"
      onSubmit={form.handleSubmit(onSubmit)}
    >
      {requirement && (
        <p className="text-sm text-muted-foreground">
          {t(`adminRequirements.status.${requirement.status}`)}
          {' · '}
          {requirement.verifiedAt
            ? t('adminRequirements.verifiedAt', { date: formatDate(requirement.verifiedAt) })
            : t('adminRequirements.neverVerified')}
        </p>
      )}
      <div className="grid grid-cols-2 gap-3">
        {countrySelect('requirement-country', 'countryCode', t('adminRequirements.country'))}
        {countrySelect(
          'requirement-nationality',
          'nationality',
          t('adminRequirements.nationality'),
        )}
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="requirement-type">{t('adminRequirements.type')}</Label>
          <NativeSelect id="requirement-type" {...form.register('type')}>
            {REQUIREMENT_TYPES.map((type) => (
              <option key={type} value={type}>
                {t(`requirements.types.${type}`)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor="requirement-lead">{t('adminRequirements.leadDays')}</Label>
          <Input
            id="requirement-lead"
            inputMode="numeric"
            className="tabular"
            placeholder={t('adminRequirements.optional')}
            aria-invalid={errors.leadDays ? true : undefined}
            {...form.register('leadDays')}
          />
          {errors.leadDays && (
            <p className="text-sm text-destructive">{message(errors.leadDays.message)}</p>
          )}
        </div>
      </div>
      <p className="-mt-2 text-xs text-muted-foreground">{t('adminRequirements.leadDaysHint')}</p>
      <div className="flex flex-col gap-2">
        <Label htmlFor="requirement-title">{t('adminRequirements.titleLabel')}</Label>
        <Input
          id="requirement-title"
          maxLength={200}
          aria-invalid={errors.title ? true : undefined}
          {...form.register('title')}
        />
        {errors.title && (
          <p className="text-sm text-destructive">{message(errors.title.message)}</p>
        )}
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="requirement-description">{t('adminRequirements.description')}</Label>
        <Textarea
          id="requirement-description"
          maxLength={4000}
          placeholder={t('adminRequirements.optional')}
          {...form.register('description')}
        />
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="requirement-url">{t('adminRequirements.officialUrl')}</Label>
        <Input
          id="requirement-url"
          type="url"
          inputMode="url"
          placeholder="https://"
          aria-invalid={errors.officialUrl ? true : undefined}
          {...form.register('officialUrl')}
        />
        {errors.officialUrl && (
          <p className="text-sm text-destructive">{message(errors.officialUrl.message)}</p>
        )}
      </div>
      {requirement && (
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="outline" disabled={busy} onClick={() => onPublish(true)}>
            {t(
              requirement.status === 'PUBLISHED'
                ? 'adminRequirements.reverify'
                : 'adminRequirements.publish',
            )}
          </Button>
          {requirement.status === 'PUBLISHED' && (
            <Button type="button" variant="ghost" disabled={busy} onClick={() => onPublish(false)}>
              {t('adminRequirements.unpublish')}
            </Button>
          )}
        </div>
      )}
      <div className="flex flex-wrap items-center justify-end gap-2 pt-2">
        {requirement && (
          <Button
            type="button"
            variant="ghost"
            className="mr-auto text-destructive hover:text-destructive"
            disabled={busy}
            onClick={() => (confirmDelete ? onDelete() : setConfirmDelete(true))}
          >
            {t(confirmDelete ? 'adminRequirements.confirmDelete' : 'adminRequirements.delete')}
          </Button>
        )}
        <Button type="button" variant="ghost" onClick={onDone}>
          {t('adminRequirements.cancel')}
        </Button>
        <Button type="submit" disabled={busy}>
          {t('adminRequirements.save')}
        </Button>
      </div>
    </form>
  )
}
