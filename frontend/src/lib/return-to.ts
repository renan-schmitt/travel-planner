/**
 * Caminho a abrir depois do login (ex.: `/join/:token`). O login com Google sempre volta para `/`,
 * então o destino fica no `sessionStorage` (sobrevive ao redirecionamento, só nesta aba).
 */
const KEY = 'returnTo'

export function saveReturnTo(path: string) {
  if (path === '/' || path.startsWith('/login')) return
  try {
    sessionStorage.setItem(KEY, path)
  } catch {
    // Sem storage (modo privado restrito): depois do login fica na tela inicial.
  }
}

/** Lê e apaga o destino salvo (só caminhos internos). */
export function takeReturnTo(): string | null {
  try {
    const path = sessionStorage.getItem(KEY)
    sessionStorage.removeItem(KEY)
    return path?.startsWith('/') && !path.startsWith('//') ? path : null
  } catch {
    return null
  }
}
