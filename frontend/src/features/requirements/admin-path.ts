/** Tela de administração dos requisitos, opcionalmente filtrada por país. */
export function adminRequirementsPath(countryCode?: string) {
  return countryCode ? `/admin/requirements?country=${countryCode}` : '/admin/requirements'
}
