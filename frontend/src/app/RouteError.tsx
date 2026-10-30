import { TriangleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useRouteError } from 'react-router'
import { Button } from '@/components/ui/button'

export function RouteError() {
  const { t } = useTranslation()
  console.error(useRouteError())
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-4 px-4 text-center">
      <TriangleAlert className="size-10 text-destructive" strokeWidth={1.75} aria-hidden />
      <h1 className="text-2xl font-bold">{t('error.title')}</h1>
      <p className="text-muted-foreground">{t('error.description')}</p>
      <Button onClick={() => window.location.reload()}>{t('error.reload')}</Button>
    </div>
  )
}
