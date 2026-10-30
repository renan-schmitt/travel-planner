import { CalendarCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'

export function DashboardPage() {
  const { t } = useTranslation()
  return (
    <>
      <PageHeader title={t('dashboard.title')} />
      <EmptyState icon={CalendarCheck} message={t('dashboard.empty')} />
    </>
  )
}
