import 'i18next'
import type { defaultNS, resources } from '.'

// Chaves de tradução tipadas: `t('nav.hoem')` não compila.
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: typeof defaultNS
    resources: (typeof resources)['pt-BR']
  }
}
