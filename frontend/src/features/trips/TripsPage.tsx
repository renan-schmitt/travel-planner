import { Map } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'

export function TripsPage() {
  const { t } = useTranslation()
  return (
    <>
      <PageHeader title={t('trips.title')} />
      <EmptyState icon={Map} message={t('trips.empty')} />
    </>
  )
}
