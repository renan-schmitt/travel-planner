import type { TFunction } from 'i18next'
import { formatDate, formatLocalDateTime } from '@/lib/dates'
import type { Suggestion } from './api'

/** Parâmetros que o backend manda (valores simples). */
type Params = Record<string, unknown>

function str(params: Params, key: string): string | undefined {
  const value = params[key]
  return typeof value === 'string' && value !== '' ? value : undefined
}

function num(params: Params, key: string): number | undefined {
  const value = params[key]
  return typeof value === 'number' ? value : undefined
}

export function paramString(s: Suggestion, key: string) {
  return str(s.params, key)
}

export function paramNumber(s: Suggestion, key: string) {
  return num(s.params, key)
}

/** "TAP TP82"; sem companhia nem código, "São Paulo → Lisboa" (ponta ausente = casa). */
function flightLabel(t: TFunction, params: Params): string {
  const byCode = [str(params, 'carrier'), str(params, 'code')].filter(Boolean).join(' ')
  if (byCode) return byCode
  const home = t('segments.home')
  return `${str(params, 'origin') ?? home} → ${str(params, 'destination') ?? home}`
}

/**
 * Título e descrição da sugestão, montados no frontend (o backend só manda `ruleCode` + `params`).
 * Regra desconhecida (de uma versão mais nova do backend) cai num texto genérico.
 */
export function suggestionText(
  t: TFunction,
  s: Pick<Suggestion, 'ruleCode' | 'params'>,
): { title: string; description: string } {
  const p = s.params
  const home = t('segments.home')
  switch (s.ruleCode) {
    case 'TRANSFER_CHEGADA':
      return {
        title: t('suggestions.rules.TRANSFER_CHEGADA.title', { city: str(p, 'city') }),
        description: t('suggestions.rules.TRANSFER_CHEGADA.description', {
          flight: flightLabel(t, p),
          when: formatLocalDateTime(str(p, 'arriveAt') ?? str(p, 'departAt') ?? ''),
        }),
      }
    case 'TRANSFER_SAIDA':
      return {
        title:
          p.fromHome === true
            ? t('suggestions.rules.TRANSFER_SAIDA.titleHome')
            : t('suggestions.rules.TRANSFER_SAIDA.title', { city: str(p, 'city') }),
        description: t('suggestions.rules.TRANSFER_SAIDA.description', {
          flight: flightLabel(t, p),
          when: formatLocalDateTime(str(p, 'departAt') ?? ''),
        }),
      }
    case 'NOITE_SEM_HOSPEDAGEM': {
      const [, month, day] = (str(p, 'date') ?? '').split('-')
      const date = `${day}/${month}`
      const city = str(p, 'city')
      return {
        title: city
          ? t('suggestions.rules.NOITE_SEM_HOSPEDAGEM.title', { date, city })
          : t('suggestions.rules.NOITE_SEM_HOSPEDAGEM.titleNoCity', { date }),
        description: t('suggestions.rules.NOITE_SEM_HOSPEDAGEM.description'),
      }
    }
    case 'DESTINO_SEM_TRANSPORTE': {
      const from = str(p, 'fromCity') ?? home
      const to = str(p, 'toCity') ?? home
      return {
        title: t('suggestions.rules.DESTINO_SEM_TRANSPORTE.title', { from, to }),
        description: t('suggestions.rules.DESTINO_SEM_TRANSPORTE.description', { from, to }),
      }
    }
    case 'TRANSPORTE_LOCAL':
      return {
        title: t('suggestions.rules.TRANSPORTE_LOCAL.title', { city: str(p, 'city') }),
        description: t('suggestions.rules.TRANSPORTE_LOCAL.description', {
          count: num(p, 'nights') ?? 2,
        }),
      }
    case 'CHECKIN_ONLINE':
      return {
        title: t('suggestions.rules.CHECKIN_ONLINE.title', { flight: flightLabel(t, p) }),
        description: t('suggestions.rules.CHECKIN_ONLINE.description', {
          when: formatLocalDateTime(str(p, 'departAt') ?? ''),
        }),
      }
    case 'PASSAPORTE_VALIDADE':
      return p.missing === true
        ? {
            title: t('suggestions.rules.PASSAPORTE_VALIDADE.titleMissing', {
              name: str(p, 'name'),
            }),
            description: t('suggestions.rules.PASSAPORTE_VALIDADE.descriptionMissing'),
          }
        : {
            title: t('suggestions.rules.PASSAPORTE_VALIDADE.titleExpiring', {
              name: str(p, 'name'),
              expiry: formatDate(str(p, 'expiry') ?? ''),
            }),
            description: t('suggestions.rules.PASSAPORTE_VALIDADE.descriptionExpiring', {
              limit: formatDate(str(p, 'limit') ?? ''),
            }),
          }
    case 'SEGURO_VIAGEM':
      return {
        title: t('suggestions.rules.SEGURO_VIAGEM.title'),
        description:
          p.schengen === true
            ? t('suggestions.rules.SEGURO_VIAGEM.descriptionSchengen')
            : t('suggestions.rules.SEGURO_VIAGEM.description'),
      }
    default:
      return { title: t('suggestions.unknown'), description: '' }
  }
}
