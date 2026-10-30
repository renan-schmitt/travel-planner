import { House, Map, UserRound, type LucideIcon } from 'lucide-react'

export interface NavItem {
  to: string
  labelKey: 'nav.home' | 'nav.trips' | 'nav.profile'
  icon: LucideIcon
  end?: boolean
}

export const navItems: NavItem[] = [
  { to: '/', labelKey: 'nav.home', icon: House, end: true },
  { to: '/trips', labelKey: 'nav.trips', icon: Map },
  { to: '/profile', labelKey: 'nav.profile', icon: UserRound },
]
