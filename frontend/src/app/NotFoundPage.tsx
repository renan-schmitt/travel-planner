import { Compass } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  const { t } = useTranslation()
  return (
    <div className="flex flex-col items-center gap-4 py-12 text-center">
      <span className="flex size-14 items-center justify-center rounded-full bg-highlight-soft text-highlight-soft-foreground">
        <Compass className="size-7" strokeWidth={1.75} aria-hidden />
      </span>
      <h1 className="text-2xl font-bold">{t('notFound.title')}</h1>
      <p className="max-w-sm text-muted-foreground">{t('notFound.description')}</p>
      <Button asChild>
        <Link to="/">{t('notFound.back')}</Link>
      </Button>
    </div>
  )
}
