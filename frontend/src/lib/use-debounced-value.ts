import { useEffect, useState } from 'react'

/** O valor só muda depois de `delay` ms sem alteração (ex.: busca enquanto o usuário digita). */
export function useDebouncedValue<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}
