import { LogOut, UserRound } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useTheme, type ThemePreference } from '@/lib/theme'
import { themeOptions } from './theme-options'

/** Menu do usuário. Foto, nome e "Sair" de verdade chegam com a autenticação (Fase 3). */
export function UserMenu() {
  const { t } = useTranslation()
  const { preference, setPreference } = useTheme()

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="rounded-full"
          aria-label={t('userMenu.open')}
        >
          <span className="flex size-9 items-center justify-center rounded-full bg-accent text-accent-foreground">
            <UserRound className="size-5" strokeWidth={1.75} aria-hidden />
          </span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel>{t('userMenu.guest')}</DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to="/profile">
            <UserRound strokeWidth={1.75} />
            {t('userMenu.profile')}
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuLabel className="text-xs text-muted-foreground">
          {t('userMenu.theme')}
        </DropdownMenuLabel>
        <DropdownMenuRadioGroup
          value={preference}
          onValueChange={(value) => setPreference(value as ThemePreference)}
        >
          {themeOptions.map(({ value, labelKey, icon: Icon }) => (
            <DropdownMenuRadioItem key={value} value={value}>
              <Icon strokeWidth={1.75} />
              {t(labelKey)}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
        <DropdownMenuSeparator />
        <DropdownMenuItem disabled>
          <LogOut strokeWidth={1.75} />
          {t('userMenu.signOut')}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
