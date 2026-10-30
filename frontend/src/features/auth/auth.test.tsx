import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { api } from '@/lib/api'
import { loggedIn, loggedOut, testUser } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

describe('autenticação', () => {
  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
  })

  it('sem sessão, as rotas protegidas levam para /login', async () => {
    server.use(loggedOut())
    const { router } = renderApp('/trips')

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
  })

  it('o botão do Google aponta para o fluxo OAuth do backend', async () => {
    server.use(loggedOut())
    renderApp('/login')

    expect(await screen.findByRole('link', { name: 'Entrar com Google' })).toHaveAttribute(
      'href',
      '/oauth2/authorization/google',
    )
  })

  it('mostra o erro quando o login com Google falha', async () => {
    server.use(loggedOut())
    renderApp('/login?error')

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível entrar')
  })

  it('usuário já logado não vê o login', async () => {
    const { router } = renderApp('/login')

    expect(await screen.findByRole('heading', { name: 'Início', level: 1 })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/')
  })

  it('login dev envia o e-mail com CSRF e entra no app', async () => {
    document.cookie = 'XSRF-TOKEN=tok; path=/'
    let loggedInNow = false
    const requests: Array<{ email: string | null; csrf: string | null }> = []
    server.use(
      http.get('*/api/v1/me', () =>
        loggedInNow
          ? HttpResponse.json({ ...testUser, email: 'dev@example.com' })
          : new HttpResponse(null, { status: 401 }),
      ),
      http.post('*/dev/login', ({ request }) => {
        requests.push({
          email: new URL(request.url).searchParams.get('email'),
          csrf: request.headers.get('X-XSRF-TOKEN'),
        })
        loggedInNow = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { user, router } = renderApp('/login')

    await user.type(await screen.findByLabelText('E-mail (login dev)'), 'dev@example.com')
    await user.click(screen.getByRole('button', { name: 'Login dev' }))

    expect(await screen.findByRole('heading', { name: 'Início', level: 1 })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/')
    expect(requests).toEqual([{ email: 'dev@example.com', csrf: 'tok' }])
  })

  it('login dev valida o e-mail', async () => {
    server.use(loggedOut())
    const { user } = renderApp('/login')

    await user.type(await screen.findByLabelText('E-mail (login dev)'), 'nao-e-email')
    await user.click(screen.getByRole('button', { name: 'Login dev' }))

    expect(await screen.findByText('Informe um e-mail válido.')).toBeInTheDocument()
  })

  it('menu do usuário mostra nome e e-mail, e "Sair" encerra a sessão', async () => {
    document.cookie = 'XSRF-TOKEN=tok; path=/'
    const logoutCsrf: Array<string | null> = []
    let session = true
    server.use(
      http.get('*/api/v1/me', () =>
        session ? HttpResponse.json(testUser) : new HttpResponse(null, { status: 401 }),
      ),
      http.post('*/logout', ({ request }) => {
        logoutCsrf.push(request.headers.get('X-XSRF-TOKEN'))
        session = false
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { user, router } = renderApp('/')

    await user.click(await screen.findByRole('button', { name: 'Abrir menu do usuário' }))
    const menu = within(await screen.findByRole('menu'))
    expect(menu.getByText('Ana Souza')).toBeInTheDocument()
    expect(menu.getByText('ana@example.com')).toBeInTheDocument()

    await user.click(menu.getByRole('menuitem', { name: 'Sair' }))

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(logoutCsrf).toEqual(['tok'])
  })

  it('um 401 numa chamada da API manda para /login', async () => {
    server.use(loggedIn())
    const { router } = renderApp('/')
    await screen.findByRole('heading', { name: 'Início', level: 1 })

    // Sessão expirada: a próxima chamada recebe 401 (o usuário em cache não é revalidado).
    server.use(loggedOut())
    await api.GET('/api/v1/me')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
  })
})
