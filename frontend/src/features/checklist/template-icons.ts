import {
  Baby,
  Briefcase,
  Building2,
  Car,
  Globe,
  House,
  ListChecks,
  Mountain,
  PawPrint,
  Snowflake,
  Tent,
  TreePalm,
  type LucideIcon,
} from 'lucide-react'
import type { TemplateIcon } from './templates-api'

export const templateIcons: Record<TemplateIcon, LucideIcon> = {
  BEACH: TreePalm,
  SNOW: Snowflake,
  CITY: Building2,
  GLOBE: Globe,
  BABY: Baby,
  PET: PawPrint,
  HOME: House,
  MOUNTAIN: Mountain,
  CAMPING: Tent,
  CAR: Car,
  BRIEFCASE: Briefcase,
  LIST: ListChecks,
}

/** Ordem da escolha de ícone no editor. */
export const TEMPLATE_ICONS = Object.keys(templateIcons) as TemplateIcon[]
