import { CircleAlert, Info, Lightbulb, Sparkles, TriangleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Brand } from '@/components/Brand'
import { PriceTier } from '@/components/PriceTier'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import { cn } from '@/lib/utils'

const colorTokens = [
  'background',
  'card',
  'muted',
  'border',
  'primary',
  'accent',
  'highlight',
  'highlight-soft',
  'destructive',
  'warning',
  'warning-soft',
  'info',
  'info-soft',
  'success',
  'success-soft',
]

const tripColors = ['bg-trip-1', 'bg-trip-2', 'bg-trip-3', 'bg-trip-4', 'bg-trip-5', 'bg-trip-6']

/** Só existe em dev (rota registrada com `import.meta.env.DEV`): confere o visual nos dois temas. */
export function DevUiPage() {
  const { t } = useTranslation()
  return (
    <div className="min-h-dvh">
      <header className="mx-auto max-w-7xl px-4 py-6">
        <Brand />
        <h1 className="mt-4 text-[1.75rem] font-bold">{t('devUi.title')}</h1>
        <p className="text-muted-foreground">{t('devUi.description')}</p>
      </header>
      <div className="mx-auto grid max-w-7xl gap-4 px-4 pb-8 lg:grid-cols-2">
        <ThemePanel title={t('devUi.lightTheme')} />
        <ThemePanel title={t('devUi.darkTheme')} dark />
      </div>
    </div>
  )
}

function ThemePanel({ title, dark = false }: { title: string; dark?: boolean }) {
  const { t } = useTranslation()
  return (
    // Força o tema do painel, independente do tema do app.
    <section
      className={cn(dark ? 'dark' : 'light', 'rounded-2xl border')}
      style={{ colorScheme: dark ? 'dark' : 'light' }}
    >
      <div className="flex flex-col gap-3 rounded-2xl bg-background p-4 text-foreground">
        <h2 className="text-xl font-bold">{title}</h2>

        <Card>
          <CardHeader>
            <CardTitle>{t('devUi.buttons')}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            <Button>{t('devUi.primary')}</Button>
            <Button variant="secondary">{t('devUi.secondary')}</Button>
            <Button variant="outline">{t('devUi.outline')}</Button>
            <Button variant="ghost">{t('devUi.ghost')}</Button>
            <Button variant="destructive">{t('devUi.destructive')}</Button>
            <Button size="icon" variant="outline" aria-label={t('devUi.highlight')}>
              <Sparkles className="text-highlight" strokeWidth={1.75} />
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('devUi.badges')}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            <Badge>{t('devUi.primary')}</Badge>
            <Badge variant="secondary">{t('devUi.secondary')}</Badge>
            <Badge variant="outline">{t('devUi.outline')}</Badge>
            <Badge variant="highlight">{t('devUi.highlight')}</Badge>
            <Badge variant="critical">{t('severity.critical')}</Badge>
            <Badge variant="warning">{t('severity.warning')}</Badge>
            <Badge variant="info">{t('severity.info')}</Badge>
            <Badge variant="success">{t('severity.success')}</Badge>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('devUi.suggestions')}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <div className="flex gap-3 rounded-lg border border-l-4 border-l-destructive p-3">
              <CircleAlert className="size-5 shrink-0 text-destructive" strokeWidth={1.75} />
              <p className="text-sm">{t('devUi.criticalSuggestion')}</p>
            </div>
            <div className="flex gap-3 rounded-lg bg-warning-soft p-3 text-warning-foreground">
              <TriangleAlert className="size-5 shrink-0" strokeWidth={1.75} />
              <p className="text-sm">{t('devUi.warningSuggestion')}</p>
            </div>
            <div className="flex gap-3 rounded-lg bg-info-soft p-3 text-info-foreground">
              <Info className="size-5 shrink-0" strokeWidth={1.75} />
              <p className="text-sm">{t('devUi.infoSuggestion')}</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('devUi.priceTier')}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap items-center gap-4">
            <PriceTier level={1} />
            <PriceTier level={2} />
            <PriceTier level={3} />
            <PriceTier level={4} />
            <PriceTier level={2} estimated />
          </CardContent>
        </Card>

        <Card className="overflow-hidden pt-0">
          <div className="h-2 bg-trip-2" />
          <CardHeader>
            <CardTitle>{t('devUi.card')}</CardTitle>
            <CardDescription>{t('devUi.cardDescription')}</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="font-heading text-lg font-bold text-highlight tabular">
              {t('devUi.countdown', { count: 12 })}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('devUi.form')}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor={`dest-${title}`}>{t('devUi.inputLabel')}</Label>
              <Input id={`dest-${title}`} placeholder={t('devUi.inputPlaceholder')} />
            </div>
            <div className="flex min-h-11 items-center gap-3">
              <Checkbox id={`check-${title}`} defaultChecked />
              <Label htmlFor={`check-${title}`}>{t('devUi.checkboxLabel')}</Label>
            </div>
            <div className="flex flex-wrap gap-2">
              <Dialog>
                <DialogTrigger asChild>
                  <Button variant="outline">{t('devUi.openDialog')}</Button>
                </DialogTrigger>
                <DialogContent className={dark ? 'dark' : 'light'}>
                  <DialogHeader>
                    <DialogTitle>{t('devUi.dialogTitle')}</DialogTitle>
                    <DialogDescription>{t('devUi.dialogDescription')}</DialogDescription>
                  </DialogHeader>
                </DialogContent>
              </Dialog>
              <Sheet>
                <SheetTrigger asChild>
                  <Button variant="outline">{t('devUi.openSheet')}</Button>
                </SheetTrigger>
                <SheetContent side="bottom" className={cn('rounded-t-xl', dark ? 'dark' : 'light')}>
                  <SheetHeader>
                    <SheetTitle>{t('devUi.dialogTitle')}</SheetTitle>
                    <SheetDescription>{t('devUi.dialogDescription')}</SheetDescription>
                  </SheetHeader>
                </SheetContent>
              </Sheet>
              <Button variant="secondary" onClick={() => toast.success(t('devUi.toastMessage'))}>
                <Lightbulb strokeWidth={1.75} />
                {t('devUi.showToast')}
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('devUi.colors')}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-5">
              {colorTokens.map((token) => (
                <div key={token} className="flex flex-col gap-1">
                  <div
                    className="h-10 rounded-lg border"
                    style={{ backgroundColor: `var(--${token})` }}
                  />
                  <code className="truncate text-xs text-muted-foreground">--{token}</code>
                </div>
              ))}
            </div>
            <div>
              <p className="mb-2 text-sm font-medium">{t('devUi.tripColors')}</p>
              <div className="flex gap-2">
                {tripColors.map((color) => (
                  <div key={color} className={cn('size-8 rounded-full', color)} />
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </section>
  )
}
