import { LoaderCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Navigate } from 'react-router'
import { useMe } from './api'

/** Só renderiza o conteúdo com usuário logado; sem sessão, vai para `/login`. */
export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { data: me, isPending, isError, error } = useMe()

  if (isPending) return <FullPageLoading />
  if (isError) throw error
  if (!me) return <Navigate to="/login" replace />
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
