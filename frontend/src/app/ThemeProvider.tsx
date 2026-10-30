import { useCallback, useLayoutEffect, useMemo, useState, useSyncExternalStore } from 'react'
import {
  applyTheme,
  readStoredPreference,
  storePreference,
  subscribeToSystemTheme,
  systemTheme,
  ThemeContext,
  type ThemePreference,
} from '@/lib/theme'

/** Tema claro/escuro/sistema (ESP §13.3), aplicado como classe `dark` no `<html>`. */
export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(readStoredPreference)
  const system = useSyncExternalStore(subscribeToSystemTheme, systemTheme)
  const resolved = preference === 'system' ? system : preference

  useLayoutEffect(() => applyTheme(resolved), [resolved])

  const setPreference = useCallback((next: ThemePreference) => {
    storePreference(next)
    setPreferenceState(next)
  }, [])

  const value = useMemo(
    () => ({ preference, resolved, setPreference }),
    [preference, resolved, setPreference],
  )
  return <ThemeContext value={value}>{children}</ThemeContext>
}
