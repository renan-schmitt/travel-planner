import { screen } from '@testing-library/react'
import { THEME_STORAGE_KEY } from '@/lib/theme'
import { renderApp } from '@/test/render'

describe('ProfilePage — tema', () => {
  it('segue o sistema por padrão (claro no jsdom)', async () => {
    renderApp('/profile')

    expect(await screen.findByRole('tab', { name: 'Sistema' })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    expect(document.documentElement).not.toHaveClass('dark')
  })

  it('aplica e guarda a escolha manual', async () => {
    const { user } = renderApp('/profile')

    await user.click(await screen.findByRole('tab', { name: 'Escuro' }))
    expect(document.documentElement).toHaveClass('dark')
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark')

    await user.click(screen.getByRole('tab', { name: 'Sistema' }))
    expect(document.documentElement).not.toHaveClass('dark')
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBeNull()
  })

  it('restaura a escolha guardada', async () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'dark')
    renderApp('/profile')

    expect(await screen.findByRole('tab', { name: 'Escuro' })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    expect(document.documentElement).toHaveClass('dark')
  })
})
