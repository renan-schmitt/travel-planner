import { LoaderCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Navigate, useLocation } from 'react-router'
import { saveReturnTo, takeReturnTo } from '@/lib/return-to'
import { useMe } from './api'

/**
 * Só renderiza o conteúdo com usuário logado; sem sessão, guarda o caminho pedido e vai para
 * `/login`. Depois do login (que volta para `/`), retoma o caminho guardado.
 */
export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { data: me, isPending, isError, error } = useMe()
  const location = useLocation()

  if (isPending) return <FullPageLoading />
  if (isError) throw error
  if (!me) {
    saveReturnTo(location.pathname + location.search)
    return <Navigate to="/login" replace />
  }
  if (location.pathname === '/') {
    const returnTo = takeReturnTo()
    if (returnTo) return <Navigate to={returnTo} replace />
  }
  return children
}

export function FullPageLoading() {
  const { t } = useTranslation()
  return (
    <div className="flex min-h-dvh items-center justify-center" role="status">
      <LoaderCircle
        className="size-8 animate-spin text-primary motion-reduce:animate-none"
        strokeWidth={1.75}
        aria-hidden
      />
      <span className="sr-only">{t('common.loading')}</span>
    </div>
  )
}
