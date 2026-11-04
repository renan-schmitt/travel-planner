import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import { api, ApiError } from '@/lib/api'

export type TripRequirements = components['schemas']['TripRequirementsResponse']
export type TripRequirementItem = TripRequirements['requirements'][number]
export type Requirement = components['schemas']['RequirementResponse']
export type RequirementBody = components['schemas']['RequirementRequest']
export type RequirementType = Requirement['type']
export type RequirementStatus = Requirement['status']

export const REQUIREMENT_TYPES: RequirementType[] = [
  'VISA',
  'ETA',
  'ENTRY_FORM',
  'VACCINE',
  'INSURANCE',
  'PASSPORT_VALIDITY',
  'ID_CARD',
  'OTHER',
]

export const requirementKeys = {
  trip: (tripId: number) => ['trips', 'requirements', tripId] as const,
  admin: ['admin-requirements'] as const,
}

/**
 * Documentos dos destinos estrangeiros da viagem. Fica sob `['trips', ...]`: as mutações da viagem
 * (paradas, viajantes) invalidam junto.
 */
export function useTripRequirements(tripId: number) {
  return useQuery({
    queryKey: requirementKeys.trip(tripId),
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/trips/{tripId}/requirements', {
        params: { path: { tripId } },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

/** Toda a base (o filtro por país e status é feito na tela). Só administradores. */
export function useAdminRequirements(enabled = true) {
  return useQuery({
    enabled,
    queryKey: requirementKeys.admin,
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/admin/requirements')
      if (!data) throw new ApiError(response.status)
      return data
    },
    retry: false,
  })
}

function useAdminMutation<T>(fn: (arg: T) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: requirementKeys.admin })
      // O motor recalcula as viagens do país: sugestões, checklist e documentos mudam.
      void queryClient.invalidateQueries({ queryKey: ['trips'] })
    },
  })
}

export function useCreateRequirement() {
  return useAdminMutation(async (body: RequirementBody) => {
    const { data, response } = await api.POST('/api/v1/admin/requirements', { body })
    if (!data) throw new ApiError(response.status)
    return data
  })
}

export function useUpdateRequirement() {
  return useAdminMutation(
    async ({ requirement, body }: { requirement: Requirement; body: RequirementBody }) => {
      const { data, response } = await api.PUT('/api/v1/admin/requirements/{id}', {
        params: { path: { id: requirement.id } },
        body: { ...body, version: requirement.version },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
  )
}

export function useDeleteRequirement() {
  return useAdminMutation(async (requirement: Requirement) => {
    const { response } = await api.DELETE('/api/v1/admin/requirements/{id}', {
      params: { path: { id: requirement.id } },
    })
    if (!response.ok) throw new ApiError(response.status)
  })
}

/** Publicar (ou confirmar de novo na fonte oficial) e despublicar. */
export function useSetRequirementPublished() {
  return useAdminMutation(
    async ({ requirement, publish }: { requirement: Requirement; publish: boolean }) => {
      const params = { path: { id: requirement.id } }
      const { data, response } = publish
        ? await api.POST('/api/v1/admin/requirements/{id}/publish', { params })
        : await api.POST('/api/v1/admin/requirements/{id}/unpublish', { params })
      if (!data) throw new ApiError(response.status)
      return data
    },
  )
}
