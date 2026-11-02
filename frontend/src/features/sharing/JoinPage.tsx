import { LinkIcon, LoaderCircle } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/EmptyState'
import { Button } from '@/components/ui/button'
import { useJoinTrip } from './api'

/**
 * `/join/:token`: entra na viagem pelo link de partilha. Fica atrás do `RequireAuth`, então sem
 * sessão a pessoa passa pelo login e volta para cá.
 */
export function JoinPage() {
  const { t } = useTranslation()
  const { token = '' } = useParams()
  const navigate = useNavigate()
  const join = useJoinTrip()
  const started = useRef(false)

  useEffect(() => {
    // Uma vez só (o StrictMode monta o efeito duas vezes em dev).
    if (started.current) return
    started.current = true
    join.mutate(token, {
      onSuccess: ({ tripId }) => {
        toast.success(t('sharing.joined'))
        void navigate(`/trips/${tripId}`, { replace: true })
      },
    })
  }, [join, navigate, t, token])

  if (join.isError) {
    return (
      <div className="flex flex-col gap-3">
        <EmptyState icon={LinkIcon} message={t('sharing.joinInvalid')} />
        <Button variant="outline" asChild className="self-center">
          <Link to="/trips">{t('trip.backToTrips')}</Link>
        </Button>
      </div>
    )
  }
  return (
    <div className="flex flex-col items-center gap-3 py-12 text-muted-foreground" role="status">
      <LoaderCircle
        className="size-8 animate-spin text-primary motion-reduce:animate-none"
        strokeWidth={1.75}
        aria-hidden
      />
      {t('sharing.joining')}
    </div>
  )
}
