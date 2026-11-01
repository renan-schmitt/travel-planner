import { ChevronRight, Plus, UsersRound } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useTravelers, type Traveler } from './api'
import { TravelerSheet } from './TravelerSheet'
import { TravelerSummary } from './TravelerSummary'

/** `/profile/travelers`: família/grupo cadastrado uma vez e reaproveitado nas viagens. */
export function TravelersPage() {
  const { t } = useTranslation()
  const { data: travelers, error } = useTravelers()
  const [editing, setEditing] = useState<Traveler | undefined>()
  const [sheetOpen, setSheetOpen] = useState(false)

  function open(traveler?: Traveler) {
    setEditing(traveler)
    setSheetOpen(true)
  }

  return (
    <>
      <PageHeader title={t('travelers.title')}>
        <Button onClick={() => open()}>
          <Plus strokeWidth={2} aria-hidden />
          {t('travelers.new')}
        </Button>
      </PageHeader>
      <p className="mb-3 text-sm text-muted-foreground">{t('travelers.hint')}</p>

      {error && <EmptyState icon={UsersRound} message={t('travelers.loadError')} />}
      {!travelers && !error && (
        <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
      )}
      {travelers && (
        <Card className="gap-0 py-0">
          <ul className="divide-y">
            {travelers.map((traveler) => (
              <li key={traveler.id}>
                <button
                  type="button"
                  className="flex min-h-14 w-full items-center justify-between gap-3 px-4 py-3 text-left hover:bg-muted/50 focus-visible:bg-muted/50 focus-visible:outline-none"
                  onClick={() => open(traveler)}
                >
                  <TravelerSummary traveler={traveler} />
                  <ChevronRight
                    className="size-5 shrink-0 text-muted-foreground"
                    strokeWidth={1.75}
                    aria-hidden
                  />
                </button>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <TravelerSheet open={sheetOpen} onOpenChange={setSheetOpen} traveler={editing} />
    </>
  )
}
