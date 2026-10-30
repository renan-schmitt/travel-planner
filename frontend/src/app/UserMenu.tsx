import { LogOut, UserRound } from 'lucide-react'
import { useState } from 'react'
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
import { useLogout, useMe } from '@/features/auth/api'
import { useTheme, type ThemePreference } from '@/lib/theme'
import { themeOptions } from './theme-options'

/** Menu do usuário: foto, nome, perfil, tema e "Sair". Fica dentro do `RequireAuth`. */
export function UserMenu() {
  const { t } = useTranslation()
  const { preference, setPreference } = useTheme()
  const { data: me } = useMe()
  const logout = useLogout()

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="rounded-full"
          aria-label={t('userMenu.open')}
        >
          <UserAvatar pictureUrl={me?.pictureUrl} />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel className="flex flex-col gap-0.5">
          <span className="truncate">{me?.name ?? me?.email}</span>
          {me?.name && (
            <span className="truncate text-xs font-normal text-muted-foreground">{me.email}</span>
          )}
        </DropdownMenuLabel>
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
        <DropdownMenuItem disabled={logout.isPending} onSelect={() => logout.mutate()}>
          <LogOut strokeWidth={1.75} />
          {t('userMenu.signOut')}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

/** Foto do Google; sem foto (ou se ela não carregar), um ícone. */
function UserAvatar({ pictureUrl }: { pictureUrl?: string | null }) {
  const [failed, setFailed] = useState(false)
  if (pictureUrl && !failed) {
    return (
      <img
        src={pictureUrl}
        alt=""
        // O googleusercontent.com recusa imagens com referrer de outros sites.
        referrerPolicy="no-referrer"
        className="size-9 rounded-full object-cover"
        onError={() => setFailed(true)}
      />
    )
  }
  return (
    <span className="flex size-9 items-center justify-center rounded-full bg-accent text-accent-foreground">
      <UserRound className="size-5" strokeWidth={1.75} aria-hidden />
    </span>
  )
}
