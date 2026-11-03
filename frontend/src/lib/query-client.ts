import { MutationCache, QueryClient } from '@tanstack/react-query'

export function createQueryClient() {
  const queryClient: QueryClient = new QueryClient({
    defaultOptions: {
      queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
    },
    // Qualquer alteração (roteiro, viajantes, checklist...) pode abrir ou fechar lacunas: o motor
    // roda no backend antes da resposta, então basta recarregar as sugestões e as contagens dos
    // cards. Sem `await`, para não atrasar o `onSuccess` de quem chamou.
    mutationCache: new MutationCache({
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: ['trips', 'suggestions'] })
        void queryClient.invalidateQueries({ queryKey: ['trips', 'list'] })
      },
    }),
  })
  return queryClient
}
