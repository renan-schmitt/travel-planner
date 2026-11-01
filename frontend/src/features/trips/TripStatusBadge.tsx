import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import type { TripStatus } from './api'

const variants = {
  PLANNING: 'info',
  CONFIRMED: 'success',
  IN_PROGRESS: 'highlight',
  DONE: 'secondary',
} as const satisfies Record<TripStatus, string>

export function TripStatusBadge({ status }: { status: TripStatus }) {
  const { t } = useTranslation()
  return <Badge variant={variants[status]}>{t(`trips.status.${status}`)}</Badge>
}
