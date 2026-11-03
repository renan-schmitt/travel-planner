import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import type { Trip } from '@/features/trips/api'
import { TemplateGrid, TemplatePreview } from './TemplatePicker'
import { useApplyTemplate, useTemplates, type ChecklistTemplate } from './templates-api'

/** Bottom sheet "Aplicar template": grade → prévia dos itens → aplicar no checklist da viagem. */
export function ApplyTemplateSheet({
  trip,
  open,
  onOpenChange,
}: {
  trip: Trip
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation()
  const { data: templates, error } = useTemplates()
  const apply = useApplyTemplate(trip.id)
  const [chosen, setChosen] = useState<ChecklistTemplate>()

  function changeOpen(value: boolean) {
    if (!value) setChosen(undefined)
    onOpenChange(value)
  }

  function onApply(template: ChecklistTemplate) {
    apply.mutate(template.id, {
      onSuccess: ({ added, skipped }) => {
        toast.success(
          [
            t('templates.applied', { count: added.length }),
            skipped > 0 ? t('templates.skipped', { count: skipped }) : '',
          ]
            .filter(Boolean)
            .join(' '),
        )
        changeOpen(false)
      },
      onError: () => toast.error(t('templates.applyError')),
    })
  }

  return (
    <Sheet open={open} onOpenChange={changeOpen}>
      <SheetContent
        side="bottom"
        className="mx-auto max-h-[92dvh] w-full max-w-lg overflow-y-auto rounded-t-xl"
      >
        <SheetHeader className="pb-0">
          <SheetTitle className="text-lg font-semibold">
            {chosen ? chosen.name : t('templates.applyTitle')}
          </SheetTitle>
          <SheetDescription>
            {chosen
              ? t(trip.startDate ? 'templates.previewHint' : 'templates.previewNoDate')
              : t('templates.applyHint')}
          </SheetDescription>
        </SheetHeader>
        <div className="flex flex-col gap-4 px-4 pb-4">
          {error && <p className="text-destructive">{t('templates.loadError')}</p>}
          {!templates && !error && <p className="text-muted-foreground">{t('common.loading')}</p>}
          {templates && !chosen && <TemplateGrid templates={templates} onPick={setChosen} />}
          {chosen && (
            <>
              <TemplatePreview template={chosen} />
              <div className="sticky bottom-0 -mx-4 flex gap-2 border-t bg-background px-4 pt-3">
                <Button type="button" variant="ghost" onClick={() => setChosen(undefined)}>
                  <ArrowLeft strokeWidth={2} aria-hidden />
                  {t('templates.back')}
                </Button>
                <Button
                  type="button"
                  className="ml-auto"
                  disabled={apply.isPending}
                  onClick={() => onApply(chosen)}
                >
                  {t('templates.apply', { count: chosen.items.length })}
                </Button>
              </div>
            </>
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}
