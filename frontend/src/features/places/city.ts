import { z } from 'zod'

/** Cidade escolhida no autocomplete (com `placeId` e coordenadas) ou digitada à mão. */
export interface CityValue {
  name: string
  countryCode: string
  timezone: string
  placeId?: string
  lat?: number
  lng?: number
}

export const emptyCity: CityValue = { name: '', countryCode: '', timezone: '' }

/** Cidade completa (nome, país e fuso), como uma parada exige. */
export const requiredCitySchema = z.object({
  name: z.string().trim().min(1, 'cityRequired').max(160, 'cityTooLong'),
  countryCode: z.string().length(2, 'countryRequired'),
  timezone: z.string().min(1, 'timezoneRequired'),
  placeId: z.string().optional(),
  lat: z.number().optional(),
  lng: z.number().optional(),
})

/** Primeira mensagem de erro (chave de i18n) dos campos da cidade. */
export function cityErrorKey(errors?: {
  name?: { message?: string }
  countryCode?: { message?: string }
  timezone?: { message?: string }
  message?: string
}): string | undefined {
  return (
    errors?.name?.message ??
    errors?.countryCode?.message ??
    errors?.timezone?.message ??
    errors?.message
  )
}
