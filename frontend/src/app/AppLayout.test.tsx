import { screen, within } from '@testing-library/react'
import { renderApp } from '@/test/render'

// O layout renderiza duas navegações (inferior no celular, lateral no desktop); o CSS esconde
// uma delas conforme a largura, mas no jsdom as duas existem.
function bottomNav() {
  const navs = screen.getAllByRole('navigation', { name: 'Navegação principal' })
  return navs[navs.length - 1]!
}

describe('AppLayout', () => {
  it('mostra a navegação com Início, Viagens e Perfil', async () => {
    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Início', level: 1 })).toBeInTheDocument()
    const nav = within(bottomNav())
    expect(nav.getByRole('link', { name: 'Início' })).toHaveAttribute('href', '/')
    expect(nav.getByRole('link', { name: 'Viagens' })).toHaveAttribute('href', '/trips')
    expect(nav.getByRole('link', { name: 'Perfil' })).toHaveAttribute('href', '/profile')
    expect(nav.getByRole('link', { name: 'Início' })).toHaveAttribute('aria-current', 'page')
  })

  it('navega entre as rotas e marca o item ativo', async () => {
    const { user, router } = renderApp('/')

    await user.click(within(bottomNav()).getByRole('link', { name: 'Viagens' }))

    expect(await screen.findByRole('heading', { name: 'Viagens', level: 1 })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/trips')
    expect(within(bottomNav()).getByRole('link', { name: 'Viagens' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(within(bottomNav()).getByRole('link', { name: 'Início' })).not.toHaveAttribute(
      'aria-current',
    )

    await user.click(within(bottomNav()).getByRole('link', { name: 'Perfil' }))
    expect(await screen.findByRole('heading', { name: 'Perfil', level: 1 })).toBeInTheDocument()
  })

  it('mostra a página 404 dentro do layout para rotas desconhecidas', async () => {
    const { user } = renderApp('/nao-existe')

    expect(
      await screen.findByRole('heading', { name: 'Página não encontrada' }),
    ).toBeInTheDocument()
    expect(bottomNav()).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Voltar ao início' }))
    expect(await screen.findByRole('heading', { name: 'Início', level: 1 })).toBeInTheDocument()
  })

  it('mostra o login fora do layout', async () => {
    renderApp('/login')

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
  })
})
