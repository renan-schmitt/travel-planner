import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import type { components } from '@/api/schema'
import { api, ApiError, csrfHeaders } from '@/lib/api'

export type Me = components['schemas']['MeResponse']
export type HomeCity = components['schemas']['HomeCityDto']
export type UpdateMe = components['schemas']['UpdateMeRequest']

export const meQueryKey = ['me'] as const

/** Usuário logado, ou `null` se não há sessão (401). */
export function useMe() {
  return useQuery({
    queryKey: meQueryKey,
    queryFn: async (): Promise<Me | null> => {
      const { data, response } = await api.GET('/api/v1/me')
      if (response.status === 401) return null
      if (!data) throw new ApiError(response.status)
      return data
    },
    staleTime: 5 * 60_000,
  })
}

export function useUpdateMe() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: UpdateMe) => {
      const { data, response } = await api.PATCH('/api/v1/me', { body })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: (me) => queryClient.setQueryData(meQueryKey, me),
    onError: (error) => {
      // Versão desatualizada: recarrega o perfil para o usuário tentar de novo.
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: meQueryKey })
      }
    },
  })
}

/** Login de desenvolvimento (só existe no backend nos perfis `local` e `test`). */
export function useDevLogin() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: async (email: string) => {
      const { response } = await api.POST('/dev/login', { params: { query: { email } } })
      if (!response.ok) throw new ApiError(response.status)
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: meQueryKey })
      await navigate('/', { replace: true })
    },
  })
}

/** `POST /logout` (fora do OpenAPI), limpa o cache e volta para `/login`. */
export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: async () => {
      const response = await fetch(new URL('/logout', window.location.origin), {
        method: 'POST',
        headers: csrfHeaders(),
        credentials: 'same-origin',
      })
      if (!response.ok) throw new ApiError(response.status)
    },
    onSuccess: async () => {
      queryClient.clear()
      queryClient.setQueryData(meQueryKey, null)
      await navigate('/login', { replace: true })
    },
  })
}
