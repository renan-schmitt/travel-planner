import { useSearchParams } from 'react-router'

export const TRIP_TABS = [
  'itinerary',
  'timeline',
  'checklist',
  'packing',
  'suggestions',
  'attachments',
  'people',
] as const

export type TabKey = (typeof TRIP_TABS)[number]

function isTab(value: string | null): value is TabKey {
  return value !== null && (TRIP_TABS as readonly string[]).includes(value)
}

/**
 * Aba da viagem na URL (`?tab=suggestions&focus=NOITE_SEM_HOSPEDAGEM:2027-03-11`), para outras abas
 * poderem levar até uma sugestão específica. Sem `tab`, vale `fallback`.
 */
export function useTripTab(fallback: TabKey = 'itinerary') {
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab')
  const focus = params.get('focus') ?? undefined

  function goTo(next: TabKey, nextFocus?: string) {
    setParams(
      (current) => {
        const updated = new URLSearchParams(current)
        updated.set('tab', next)
        if (nextFocus) updated.set('focus', nextFocus)
        else updated.delete('focus')
        return updated
      },
      { replace: true },
    )
  }

  return { tab: isTab(tab) ? tab : fallback, focus, goTo }
}
