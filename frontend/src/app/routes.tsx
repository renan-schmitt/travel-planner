import type { RouteObject } from 'react-router'
import { LoginPage } from '@/features/auth/LoginPage'
import { TemplatesPage } from '@/features/checklist/TemplatesPage'
import { RequireAuth } from '@/features/auth/RequireAuth'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { JoinPage } from '@/features/sharing/JoinPage'
import { ProfilePage } from '@/features/profile/ProfilePage'
import { TravelersPage } from '@/features/travelers/TravelersPage'
import { EditTripPage, NewTripPage } from '@/features/trips/TripFormPages'
import { TripPage } from '@/features/trips/TripPage'
import { TripsPage } from '@/features/trips/TripsPage'
import { AppLayout } from './AppLayout'
import { NotFoundPage } from './NotFoundPage'
import { RouteError } from './RouteError'

// Só em dev; o import dinâmico garante que a página não entra no build de produção.
const devRoutes: RouteObject[] = import.meta.env.DEV
  ? [
      {
        path: '/dev/ui',
        lazy: () => import('@/features/dev/DevUiPage').then((m) => ({ Component: m.DevUiPage })),
      },
    ]
  : []

export const routes: RouteObject[] = [
  {
    errorElement: <RouteError />,
    children: [
      { path: '/login', element: <LoginPage /> },
      ...devRoutes,
      {
        element: (
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        ),
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'trips', element: <TripsPage /> },
          { path: 'trips/new', element: <NewTripPage /> },
          { path: 'trips/:tripId', element: <TripPage /> },
          { path: 'trips/:tripId/edit', element: <EditTripPage /> },
          { path: 'profile', element: <ProfilePage /> },
          { path: 'profile/travelers', element: <TravelersPage /> },
          { path: 'profile/templates', element: <TemplatesPage /> },
          { path: 'join/:token', element: <JoinPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]
