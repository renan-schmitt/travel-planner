import { useTranslation } from 'react-i18next'
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
