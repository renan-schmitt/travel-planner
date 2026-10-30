import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { Providers } from '@/app/providers'
import { routes } from '@/app/routes'

/** Renderiza o app inteiro (rotas + providers) a partir de uma URL. */
export function renderApp(path = '/') {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  const user = userEvent.setup()
  const result = render(
    <Providers>
      <RouterProvider router={router} />
    </Providers>,
  )
  return { ...result, router, user }
}
