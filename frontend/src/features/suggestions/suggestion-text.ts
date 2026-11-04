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
    case 'RESTAURANTES':
    case 'ATRACOES':
      return {
        title: t(`suggestions.rules.${s.ruleCode}.title`, { city: str(p, 'city') }),
        description: t(`suggestions.rules.${s.ruleCode}.description`, {
          count: num(p, 'nights') ?? 1,
        }),
      }
    case 'RESORT_OPCOES':
      return {
        title: t('suggestions.rules.RESORT_OPCOES.title', { name: str(p, 'name') }),
        description:
          p.allInclusive === true
            ? t('suggestions.rules.RESORT_OPCOES.descriptionAllInclusive')
            : t('suggestions.rules.RESORT_OPCOES.description'),
      }
    case 'DOCUMENTO_DESTINO': {
      const due = str(p, 'dueDate')
      const travelers = str(p, 'travelers')
      const requirement = str(p, 'title') ?? ''
      const country = str(p, 'country') ?? ''
      return {
        // O título do requisito muitas vezes já cita o país ("e-Ticket (República Dominicana)").
        title: requirement.includes(country)
          ? requirement
          : t('suggestions.rules.DOCUMENTO_DESTINO.title', { title: requirement, country }),
        description: [
          due
            ? t('suggestions.rules.DOCUMENTO_DESTINO.due', { date: formatDate(due) })
            : t('suggestions.rules.DOCUMENTO_DESTINO.noDue'),
          travelers && t('suggestions.rules.DOCUMENTO_DESTINO.travelers', { names: travelers }),
        ]
          .filter(Boolean)
          .join(' '),
      }
    }
    case 'DESTINO_SEM_REQUISITOS':
      return {
        title: t('suggestions.rules.DESTINO_SEM_REQUISITOS.title', { country: str(p, 'country') }),
        description: t('suggestions.rules.DESTINO_SEM_REQUISITOS.description'),
      }
    case 'REQUISITO_DESATUALIZADO': {
      const verifiedAt = str(p, 'verifiedAt')
      return {
        title: t('suggestions.rules.REQUISITO_DESATUALIZADO.title', {
          country: str(p, 'country'),
        }),
        description: verifiedAt
          ? t('suggestions.rules.REQUISITO_DESATUALIZADO.description', {
              title: str(p, 'title'),
              date: formatDate(verifiedAt),
            })
          : t('suggestions.rules.REQUISITO_DESATUALIZADO.descriptionNever', {
              title: str(p, 'title'),
            }),
      }
    }
    default:
      return { title: t('suggestions.unknown'), description: '' }
  }
}
