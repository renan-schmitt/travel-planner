import { zodResolver } from '@hookform/resolvers/zod'
import { LogIn, TriangleAlert, Wrench } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Navigate, useSearchParams } from 'react-router'
import { z } from 'zod'
import { Brand } from '@/components/Brand'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { useDevLogin, useMe } from './api'
import { FullPageLoading } from './RequireAuth'

/** Passa pelo proxy do Vite em dev; em produção é o próprio backend. */
const GOOGLE_LOGIN_URL = '/oauth2/authorization/google'

export function LoginPage() {
  const { t } = useTranslation()
  const { data: me, isPending } = useMe()
  const [searchParams] = useSearchParams()

  if (isPending) return <FullPageLoading />
  if (me) return <Navigate to="/" replace />

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
        <CardContent className="flex flex-col gap-4">
          {searchParams.has('error') && (
            <p
              role="alert"
              className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
            >
              <TriangleAlert className="mt-0.5 size-4 shrink-0" strokeWidth={1.75} aria-hidden />
              {t('login.error')}
            </p>
          )}
          <Button size="lg" asChild>
            <a href={GOOGLE_LOGIN_URL}>
              <LogIn strokeWidth={1.75} aria-hidden />
              {t('login.google')}
            </a>
          </Button>
          {import.meta.env.DEV && <DevLoginForm />}
        </CardContent>
      </Card>
    </div>
  )
}

const devLoginSchema = z.object({ email: z.email() })

function DevLoginForm() {
  const { t } = useTranslation()
  const devLogin = useDevLogin()
  const form = useForm({
    resolver: zodResolver(devLoginSchema),
    defaultValues: { email: '' },
  })
  const emailError = form.formState.errors.email

  return (
    <>
      <div className="flex items-center gap-3 text-xs text-muted-foreground">
        <Separator className="flex-1" />
        {t('login.devSection')}
        <Separator className="flex-1" />
      </div>
      <form
        noValidate
        className="flex flex-col gap-3"
        onSubmit={form.handleSubmit(({ email }) => devLogin.mutate(email))}
      >
        <div className="flex flex-col gap-2">
          <Label htmlFor="dev-email">{t('login.devEmail')}</Label>
          <Input
            id="dev-email"
            type="email"
            autoComplete="email"
            placeholder={t('login.devEmailPlaceholder')}
            aria-invalid={emailError ? true : undefined}
            aria-describedby={emailError ? 'dev-email-error' : undefined}
            {...form.register('email')}
          />
          {emailError && (
            <p id="dev-email-error" className="text-sm text-destructive">
              {t('login.devEmailInvalid')}
            </p>
          )}
        </div>
        <Button type="submit" variant="outline" disabled={devLogin.isPending}>
          <Wrench strokeWidth={1.75} aria-hidden />
          {t('login.devSubmit')}
        </Button>
        {devLogin.isError && (
          <p role="alert" className="text-sm text-destructive">
            {t('login.devError')}
          </p>
        )}
      </form>
    </>
  )
}
