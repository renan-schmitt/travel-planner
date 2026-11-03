import { http, HttpResponse } from 'msw'
import type { ChecklistTemplate } from '@/features/checklist/templates-api'

export const beachTemplate: ChecklistTemplate = {
  id: 1,
  name: 'Praia',
  description: 'Sol e mar.',
  icon: 'BEACH',
  system: true,
  version: 0,
  items: [
    { category: 'PACKING', title: 'Protetor solar' },
    { category: 'BOOKINGS', title: 'Reservar guarda-sol', dueOffsetDays: -7 },
  ],
}

export const internationalTemplate: ChecklistTemplate = {
  id: 2,
  name: 'Internacional',
  icon: 'GLOBE',
  system: true,
  version: 0,
  items: [
    { category: 'DOCUMENTS', title: 'Conferir validade do passaporte', dueOffsetDays: -60 },
    { category: 'MONEY', title: 'Avisar banco', dueOffsetDays: -3 },
    { category: 'OTHER', title: 'Mandar fotos', dueOffsetDays: 1 },
  ],
}

export const ownTemplate: ChecklistTemplate = {
  id: 9,
  name: 'Serra',
  icon: 'MOUNTAIN',
  system: false,
  version: 2,
  items: [{ category: 'PACKING', title: 'Casaco', dueOffsetDays: -1 }],
}

export function templateList(
  list: ChecklistTemplate[] = [beachTemplate, internationalTemplate, ownTemplate],
) {
  return http.get('*/api/v1/checklist-templates', () => HttpResponse.json(list))
}
