import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { components } from '@/api/schema'
import { api, ApiError } from '@/lib/api'
import { checklistKeys } from './api'

export type ChecklistTemplate = components['schemas']['TemplateResponse']
export type TemplateItem = components['schemas']['TemplateItemPayload']
export type TemplateBody = components['schemas']['TemplateRequest']
export type TemplateIcon = ChecklistTemplate['icon']
export type ApplyTemplateResult = components['schemas']['ApplyTemplateResponse']

export const templateKeys = {
  all: ['checklist-templates'] as const,
}

/** Templates do sistema (primeiro) e do usuário, com os itens. */
export function useTemplates() {
  return useQuery({
    queryKey: templateKeys.all,
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/checklist-templates')
      if (!data) throw new ApiError(response.status)
      return data
    },
  })
}

export function useCreateTemplate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: TemplateBody) => {
      const { data, response } = await api.POST('/api/v1/checklist-templates', { body })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSettled: () => void queryClient.invalidateQueries({ queryKey: templateKeys.all }),
  })
}

export function useUpdateTemplate(template: ChecklistTemplate) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: TemplateBody) => {
      const { data, response } = await api.PUT('/api/v1/checklist-templates/{templateId}', {
        params: { path: { templateId: template.id } },
        body: { ...body, version: template.version },
      })
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSettled: () => void queryClient.invalidateQueries({ queryKey: templateKeys.all }),
  })
}

export function useDeleteTemplate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (template: ChecklistTemplate) => {
      const { response } = await api.DELETE('/api/v1/checklist-templates/{templateId}', {
        params: { path: { templateId: template.id } },
      })
      if (!response.ok) throw new ApiError(response.status)
    },
    onSettled: () => void queryClient.invalidateQueries({ queryKey: templateKeys.all }),
  })
}

/** Aplica um template na viagem (sem a parte de React, para a criação da viagem encadear vários). */
export async function applyTemplate(tripId: number, templateId: number) {
  const { data, response } = await api.POST('/api/v1/trips/{tripId}/checklist/apply-template', {
    params: { path: { tripId } },
    body: { templateId },
  })
  if (!data) throw new ApiError(response.status)
  return data
}

export function useApplyTemplate(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (templateId: number) => applyTemplate(tripId, templateId),
    onSettled: () => void queryClient.invalidateQueries({ queryKey: checklistKeys.trip(tripId) }),
  })
}

export function useSaveChecklistAsTemplate(tripId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (name: string) => {
      const { data, response } = await api.POST(
        '/api/v1/trips/{tripId}/checklist/save-as-template',
        { params: { path: { tripId } }, body: { name } },
      )
      if (!data) throw new ApiError(response.status)
      return data
    },
    onSettled: () => void queryClient.invalidateQueries({ queryKey: templateKeys.all }),
  })
}
