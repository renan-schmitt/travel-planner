import { ChevronRight, UsersRound } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { themeOptions } from '@/app/theme-options'
import { PageHeader } from '@/components/PageHeader'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useMe } from '@/features/auth/api'
import { useTheme, type ThemePreference } from '@/lib/theme'
import { ProfileForm } from './ProfileForm'

export function ProfilePage() {
  const { t } = useTranslation()
  const { preference, setPreference } = useTheme()
  const { data: me } = useMe()

  return (
    <>
      <PageHeader title={t('profile.title')} />
      <div className="flex flex-col gap-3">
        {me && <ProfileForm me={me} />}
        <Card className="py-0">
          <Link
            to="/profile/travelers"
            className="flex min-h-14 items-center gap-3 px-4 py-3 hover:bg-muted/50 focus-visible:bg-muted/50 focus-visible:outline-none"
          >
            <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-accent text-accent-foreground">
              <UsersRound className="size-5" strokeWidth={1.75} aria-hidden />
            </span>
            <span className="flex min-w-0 flex-1 flex-col">
              <span className="font-medium">{t('profile.travelers')}</span>
              <span className="text-sm text-muted-foreground">{t('profile.travelersHint')}</span>
            </span>
            <ChevronRight
              className="size-5 shrink-0 text-muted-foreground"
              strokeWidth={1.75}
              aria-hidden
            />
          </Link>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>{t('profile.appearance')}</CardTitle>
            <CardDescription>{t('profile.appearanceHint')}</CardDescription>
          </CardHeader>
          <CardContent>
            <Tabs value={preference} onValueChange={(v) => setPreference(v as ThemePreference)}>
              {/* No escuro --muted é igual a --card: o fundo das abas usa --background. */}
              <TabsList className="grid h-11 w-full grid-cols-3 dark:bg-background">
                {themeOptions.map(({ value, labelKey, icon: Icon }) => (
                  <TabsTrigger key={value} value={value} className="h-full">
                    <Icon strokeWidth={1.75} aria-hidden />
                    {t(labelKey)}
                  </TabsTrigger>
                ))}
              </TabsList>
            </Tabs>
          </CardContent>
        </Card>
      </div>
    </>
  )
}
