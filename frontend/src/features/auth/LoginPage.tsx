import { useTranslation } from 'react-i18next'
import { Brand } from '@/components/Brand'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

/** Placeholder: o login com Google e o login de desenvolvimento chegam na Fase 3. */
export function LoginPage() {
  const { t } = useTranslation()
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-6 px-4 py-8">
      <Brand className="text-2xl" />
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>
            <h1 className="text-xl font-bold">{t('login.title')}</h1>
          </CardTitle>
          <CardDescription>{t('app.tagline')}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <Button size="lg" disabled>
            {t('login.google')}
          </Button>
          <p className="text-center text-sm text-muted-foreground">{t('login.comingSoon')}</p>
        </CardContent>
      </Card>
    </div>
  )
}
