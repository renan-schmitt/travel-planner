import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { Providers } from '@/app/providers'
import { routes } from '@/app/routes'
import { setUnauthorizedHandler } from '@/lib/api'
import { saveReturnTo } from '@/lib/return-to'

/** Renderiza o app inteiro (rotas + providers) a partir de uma URL. */
export function renderApp(path = '/') {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  // Como no main.tsx, mas olhando a rota do router em memória (o window.location não muda).
  setUnauthorizedHandler(() => {
    const { pathname, search } = router.state.location
    if (pathname === '/login') return
    saveReturnTo(pathname + search)
    void router.navigate('/login')
  })
  const user = userEvent.setup()
  const result = render(
    <Providers>
      <RouterProvider router={router} />
    </Providers>,
  )
  return { ...result, router, user }
}
