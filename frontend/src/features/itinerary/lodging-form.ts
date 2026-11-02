import { z } from 'zod'
import type { PriceLevel } from '@/components/PriceTier'
import type { BoardType, CreateLodging, Lodging, LodgingKind, Stop } from './api'

export const LODGING_KINDS: LodgingKind[] = ['HOTEL', 'RENTAL', 'FRIENDS_HOME']

/** Na casa de um amigo ou parente não há regime, faixa de preço nem reserva. */
export const hasBooking = (kind: LodgingKind) => kind !== 'FRIENDS_HOME'

export const BOARD_TYPES: BoardType[] = [
  'ROOM_ONLY',
  'BREAKFAST',
  'HALF_BOARD',
  'FULL_BOARD',
  'ALL_INCLUSIVE',
]

/**
 * Formulário da hospedagem. Check-out depois do check-in e as duas datas dentro (ou no limite) das
 * datas da parada, como no backend. `boardType` vazio = não informado; `priceTier` nulo = não sei.
 */
export function lodgingSchema(stop: Pick<Stop, 'arrivalDate' | 'departureDate'>) {
  return z
    .object({
      kind: z.enum(LODGING_KINDS as [LodgingKind, ...LodgingKind[]]),
      name: z.string().trim().min(1, 'nameRequired').max(160, 'tooLong'),
      address: z.string().max(300, 'tooLong'),
      checkIn: z.string().min(1, 'checkInRequired'),
      checkOut: z.string().min(1, 'checkOutRequired'),
      boardType: z.enum(['', ...BOARD_TYPES] as ['', ...BoardType[]]),
      priceTier: z.custom<PriceLevel | null>(
        (v) => v === null || (typeof v === 'number' && v >= 1 && v <= 4),
      ),
      bookingRef: z.string().max(80, 'tooLong'),
      notes: z.string().max(2000, 'tooLong'),
    })
    .superRefine((v, ctx) => {
      if (!v.checkIn || !v.checkOut) return
      // Datas ISO (AAAA-MM-DD) comparam como texto.
      if (v.checkOut <= v.checkIn) {
        ctx.addIssue({ code: 'custom', path: ['checkOut'], message: 'checkOutBeforeCheckIn' })
      } else if (v.checkIn < stop.arrivalDate) {
        ctx.addIssue({ code: 'custom', path: ['checkIn'], message: 'outsideStop' })
      } else if (v.checkOut > stop.departureDate) {
        ctx.addIssue({ code: 'custom', path: ['checkOut'], message: 'outsideStop' })
      }
    })
}

export type LodgingFormValues = z.infer<ReturnType<typeof lodgingSchema>>

/**
 * Nova hospedagem: o check-in na primeira noite ainda sem hospedagem na parada (depois da última
 * que já existe) e o check-out na saída da parada.
 */
export function emptyLodging(stop: Stop, lodgings: Lodging[]): LodgingFormValues {
  const covered = lodgings
    .filter((l) => l.stopId === stop.id && l.checkOut < stop.departureDate)
    .map((l) => l.checkOut)
    .sort()
    .at(-1)
  return {
    kind: 'HOTEL',
    name: '',
    address: '',
    checkIn: covered && covered > stop.arrivalDate ? covered : stop.arrivalDate,
    checkOut: stop.departureDate,
    boardType: '',
    priceTier: null,
    bookingRef: '',
    notes: '',
  }
}

export function toFormValues(lodging: Lodging): LodgingFormValues {
  return {
    kind: lodging.kind,
    name: lodging.name,
    address: lodging.address ?? '',
    checkIn: lodging.checkIn,
    checkOut: lodging.checkOut,
    boardType: lodging.boardType ?? '',
    priceTier: (lodging.priceTier as PriceLevel | undefined) ?? null,
    bookingRef: lodging.bookingRef ?? '',
    notes: lodging.notes ?? '',
  }
}

const optional = (value: string) => value.trim() || undefined

export function toLodgingBody(values: LodgingFormValues): CreateLodging {
  const booking = hasBooking(values.kind)
  return {
    kind: values.kind,
    name: values.name.trim(),
    address: optional(values.address),
    checkIn: values.checkIn,
    checkOut: values.checkOut,
    boardType: (booking && values.boardType) || undefined,
    priceTier: (booking && values.priceTier) || undefined,
    bookingRef: booking ? optional(values.bookingRef) : undefined,
    notes: optional(values.notes),
  }
}
