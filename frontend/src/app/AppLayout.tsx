import { useTranslation } from 'react-i18next'
import { NavLink, Outlet } from 'react-router'
import { Brand } from '@/components/Brand'
import { cn } from '@/lib/utils'
import { navItems } from './navigation'
import { UserMenu } from './UserMenu'

/**
 * Celular: barra superior + navegação inferior fixa. Desktop (`lg:`): navegação lateral.
 * Conteúdo centralizado em `max-w-3xl` com margem lateral de 16 px (ESP §13.1).
 */
export function AppLayout() {
  const { t } = useTranslation()

  return (
    <div className="min-h-dvh lg:pl-64">
      <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col border-r bg-card lg:flex">
        <div className="flex h-16 items-center px-6">
          <Brand />
        </div>
        <nav aria-label={t('nav.label')} className="flex flex-col gap-1 px-3 py-2">
          {navItems.map(({ to, labelKey, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                cn(
                  'flex min-h-11 items-center gap-3 rounded-lg px-3 text-sm font-medium transition-colors duration-150',
                  isActive
                    ? 'bg-accent text-accent-foreground'
                    : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                )
              }
            >
              <Icon className="size-5" strokeWidth={1.75} aria-hidden />
              {t(labelKey)}
            </NavLink>
          ))}
        </nav>
      </aside>

      <header className="sticky top-0 z-30 border-b bg-background/90 backdrop-blur supports-[backdrop-filter]:bg-background/75">
        <div className="mx-auto flex h-14 max-w-3xl items-center justify-between px-4 lg:h-16">
          <Brand className="lg:invisible" />
          <UserMenu />
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 pt-4 pb-[calc(5rem+env(safe-area-inset-bottom))] lg:pt-8 lg:pb-8">
        <Outlet />
      </main>

      <nav
        aria-label={t('nav.label')}
        className="fixed inset-x-0 bottom-0 z-30 border-t bg-card pb-[env(safe-area-inset-bottom)] lg:hidden"
      >
        <ul className="mx-auto grid max-w-3xl grid-cols-3">
          {navItems.map(({ to, labelKey, icon: Icon, end }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={end}
                className={({ isActive }) =>
                  cn(
                    'flex min-h-14 flex-col items-center justify-center gap-0.5 text-xs font-medium transition-colors duration-150',
                    isActive ? 'text-primary' : 'text-muted-foreground hover:text-foreground',
                  )
                }
              >
                <Icon className="size-6" strokeWidth={1.75} aria-hidden />
                {t(labelKey)}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}
