import { QueryClientProvider } from '@tanstack/react-query'
import { lazy, Suspense, useState } from 'react'
import { Toaster } from '@/components/ui/sonner'
import { TooltipProvider } from '@/components/ui/tooltip'
import { createQueryClient } from '@/lib/query-client'
import { ThemeProvider } from './ThemeProvider'

const ReactQueryDevtools = import.meta.env.DEV
  ? lazy(() =>
      import('@tanstack/react-query-devtools').then((m) => ({ default: m.ReactQueryDevtools })),
    )
  : () => null

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(createQueryClient)
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <TooltipProvider delayDuration={200}>
          {children}
          <Toaster position="top-center" />
        </TooltipProvider>
        <Suspense>
          {/* No canto superior esquerdo para não cobrir o menu do usuário nem a navegação inferior. */}
          <ReactQueryDevtools buttonPosition="top-left" />
        </Suspense>
      </QueryClientProvider>
    </ThemeProvider>
  )
}
