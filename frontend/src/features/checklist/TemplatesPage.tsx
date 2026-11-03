import { ChevronRight, LayoutTemplate, Plus } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { templateIcons } from './template-icons'
import { TemplateSheet } from './TemplateSheet'
import { useTemplates, type ChecklistTemplate } from './templates-api'

/** `/profile/templates`: templates de checklist do usuário (os do sistema não são editáveis). */
export function TemplatesPage() {
  const { t } = useTranslation()
  const { data: templates, error } = useTemplates()
  const [editing, setEditing] = useState<ChecklistTemplate>()
  const [sheetOpen, setSheetOpen] = useState(false)
  const own = templates?.filter((tpl) => !tpl.system)
  // O template aberto sai da lista atualizada (versão nova depois de salvar).
  const current = editing && own?.find((tpl) => tpl.id === editing.id)

  function open(template?: ChecklistTemplate) {
    setEditing(template)
    setSheetOpen(true)
  }

  return (
    <>
      <PageHeader title={t('templates.title')}>
        <Button onClick={() => open()}>
          <Plus strokeWidth={2} aria-hidden />
          {t('templates.new')}
        </Button>
      </PageHeader>
      <p className="mb-3 text-sm text-muted-foreground">{t('templates.hint')}</p>

      {error && <EmptyState icon={LayoutTemplate} message={t('templates.loadError')} />}
      {!templates && !error && (
        <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
      )}
      {own && own.length === 0 && (
        <EmptyState icon={LayoutTemplate} message={t('templates.empty')} />
      )}
      {own && own.length > 0 && (
        <Card className="gap-0 py-0">
          <ul className="divide-y">
            {own.map((template) => {
              const Icon = templateIcons[template.icon]
              return (
                <li key={template.id}>
                  <button
                    type="button"
                    className="flex min-h-14 w-full items-center gap-3 px-4 py-3 text-left hover:bg-muted/50 focus-visible:bg-muted/50 focus-visible:outline-none"
                    onClick={() => open(template)}
                  >
                    <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-accent text-accent-foreground">
                      <Icon className="size-5" strokeWidth={1.75} aria-hidden />
                    </span>
                    <span className="flex min-w-0 flex-1 flex-col">
                      <span className="truncate font-medium">{template.name}</span>
                      <span className="text-sm text-muted-foreground">
                        {t('templates.itemCount', { count: template.items.length })}
                      </span>
                    </span>
                    <ChevronRight
                      className="size-5 shrink-0 text-muted-foreground"
                      strokeWidth={1.75}
                      aria-hidden
                    />
                  </button>
                </li>
              )
            })}
          </ul>
        </Card>
      )}

      <TemplateSheet
        open={sheetOpen && (!editing || !!current)}
        onOpenChange={setSheetOpen}
        template={current}
      />
    </>
  )
}
