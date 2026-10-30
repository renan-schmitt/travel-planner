import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { Providers } from '@/app/providers'
import { routes } from '@/app/routes'
import { setUnauthorizedHandler } from '@/lib/api'
import '@/lib/i18n'
import './index.css'

const router = createBrowserRouter(routes)
setUnauthorizedHandler(() => void router.navigate('/login'))

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Providers>
      <RouterProvider router={router} />
    </Providers>
  </StrictMode>,
)
