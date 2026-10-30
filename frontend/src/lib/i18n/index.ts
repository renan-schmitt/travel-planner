import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import ptBR from './pt-BR.json'

export const defaultNS = 'translation'
export const resources = { 'pt-BR': { translation: ptBR } } as const

void i18n.use(initReactI18next).init({
  lng: 'pt-BR',
  fallbackLng: 'pt-BR',
  resources,
  defaultNS,
  interpolation: { escapeValue: false },
})

export default i18n
