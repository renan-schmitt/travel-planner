import { useTranslation } from 'react-i18next'
import { themeOptions } from '@/app/theme-options'
import { PageHeader } from '@/components/PageHeader'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useTheme, type ThemePreference } from '@/lib/theme'

export function ProfilePage() {
  const { t } = useTranslation()
  const { preference, setPreference } = useTheme()

  return (
    <>
      <PageHeader title={t('profile.title')} />
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
    </>
  )
}
