import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import { tripKeys } from '@/features/trips/api'
import { api, ApiError } from '@/lib/api'

export type Member = components['schemas']['MemberResponse']
export type ShareLink = components['schemas']['ShareLinkResponse']
export type InviteMember = components['schemas']['InviteMemberRequest']
export type LinkRole = 'EDITOR' | 'VIEWER'

export const sharingKeys = {
  members: (tripId: number) => ['trips', 'members', tripId] as const,
  links: (tripId: number) => ['trips', 'share-links', tripId] as const,
}

/** URL pública do link (abre `/join/:token`, que exige login). */
export function joinUrl(token: string): string {
  return `${window.location.origin}/join/${token}`
}

export function useMembers(tripId: number) {
  return useQuery({
    queryKey: sharingKeys.members(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/members', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

export function useInviteMember(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: InviteMember) => {
      const { data, response } = await api.POST('/api/v1/trips/{tripId}/members', {
        params: { path: { tripId } },
        body,
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: sharingKeys.members(tripId) }),
  })
}

export function useUpdateMember(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ member, role }: { member: Member; role: Member['role'] }) => {
      const { data, response } = await api.PUT('/api/v1/trips/{tripId}/members/{memberId}', {
        params: { path: { tripId, memberId: member.id } },
        body: { version: member.version, role },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSettled: async (member) => {
      await queryClient.invalidateQueries({ queryKey: sharingKeys.members(tripId) })
      // Mudar o próprio papel muda o que a página da viagem mostra.
      if (member?.me) await queryClient.invalidateQueries({ queryKey: tripKeys.all })
    },
  })
}

/** Remove o membro (dono) ou sai da viagem (o próprio membro). */
export function useRemoveMember(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (member: Member) => {
      const { response } = await api.DELETE('/api/v1/trips/{tripId}/members/{memberId}', {
        params: { path: { tripId, memberId: member.id } },
      })
      if (!response.ok) throw new ApiError(response.status)
      return member
    },
    onSuccess: async (member) => {
      if (member.me) {
        queryClient.removeQueries({ queryKey: tripKeys.detail(tripId) })
        await queryClient.invalidateQueries({ queryKey: tripKeys.all })
      } else {
        await queryClient.invalidateQueries({ queryKey: sharingKeys.members(tripId) })
      }
    },
  })
}

export function useShareLinks(tripId: number, enabled: boolean) {
  return useQuery({
    queryKey: sharingKeys.links(tripId),
    enabled,
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/share-links', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

export function useCreateShareLink(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (role: LinkRole) => {
      const { data, response } = await api.POST('/api/v1/trips/{tripId}/share-links', {
        params: { path: { tripId } },
        body: { role },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: sharingKeys.links(tripId) }),
  })
}

export function useRevokeShareLink(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (linkId: number) => {
      const { response } = await api.DELETE('/api/v1/trips/{tripId}/share-links/{linkId}', {
        params: { path: { tripId, linkId } },
      })
      if (!response.ok) throw new ApiError(response.status)
    },
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: sharingKeys.links(tripId) }),
  })
}

/** Entra na viagem pelo link. Link inexistente ou revogado → `ApiError` 404. */
export function useJoinTrip() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (token: string) => {
      const { data, response } = await api.POST('/api/v1/share-links/{token}/join', {
        params: { path: { token } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: tripKeys.all }),
  })
}
