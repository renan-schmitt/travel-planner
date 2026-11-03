import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { dashboardKeys } from '@/features/dashboard/api'
import { createQueryClient } from '@/lib/query-client'
import { dashboard, emptyDashboard, upcomingTrip } from '@/test/dashboard'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { isoInDays } from '@/test/trips'

describe('DashboardPage', () => {
  it('sem viagens, convida a planejar a primeira', async () => {
    server.use(dashboard(emptyDashboard).handler)
    const { user, router } = renderApp('/')

    expect(await screen.findByText('Planeje sua primeira viagem')).toBeInTheDocument()
    expect(screen.queryByText('Alertas')).not.toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Nova viagem' }))
    expect(router.state.location.pathname).toBe('/trips/new')
  })

  it('mostra uma viagem com contagem regressiva, progresso e sugestões pendentes', async () => {
    server.use(dashboard({ ...emptyDashboard, upcomingTrips: [upcomingTrip] }).handler)
    renderApp('/')

    const card = (await screen.findByRole('link', { name: /Férias em Portugal/ })) as HTMLElement
    expect(card).toHaveAttribute('href', '/trips/42')
    expect(within(card).getByText('Faltam 12 dias')).toBeInTheDocument()
    expect(within(card).getByText('Checklist: 3 de 4')).toBeInTheDocument()
    expect(within(card).getByRole('progressbar')).toHaveAttribute('aria-valuenow', '75')
    expect(within(card).getByText('1 crítica')).toBeInTheDocument()
    expect(within(card).getByText('2 atenção')).toBeInTheDocument()
    expect(within(card).queryByText(/dica/)).not.toBeInTheDocument()
    expect(screen.getByText('Nenhum prazo nos próximos 14 dias. Tudo em dia!')).toBeInTheDocument()
  })

  it('lista alertas com link para a aba certa e as viagens passadas recolhidas', async () => {
    server.use(
      dashboard({
        upcomingTrips: [
          { ...upcomingTrip, id: 1, name: 'Agora', status: 'IN_PROGRESS', daysUntilStart: -1 },
          {
            ...upcomingTrip,
            id: 2,
            name: 'Sem data',
            startDate: undefined,
            endDate: undefined,
            daysUntilStart: undefined,
            checklistTotal: 0,
            checklistDone: 0,
          },
        ],
        alerts: [
          {
            kind: 'CHECKLIST_ITEM',
            tripId: 1,
            tripName: 'Agora',
            severity: 'WARNING',
            date: isoInDays(-2),
            overdue: true,
            checklistItem: { id: 7, category: 'DOCUMENTS', title: 'Tirar visto' },
          },
          {
            kind: 'SUGGESTION',
            tripId: 1,
            tripName: 'Agora',
            severity: 'CRITICAL',
            overdue: false,
            suggestion: {
              id: 9,
              ruleCode: 'SEGURO_VIAGEM',
              logicalKey: 'trip',
              params: {},
            },
          },
          {
            kind: 'CHECKLIST_ITEM',
            tripId: 2,
            tripName: 'Sem data',
            severity: 'WARNING',
            date: isoInDays(0),
            overdue: false,
            checklistItem: { id: 8, category: 'BOOKINGS', title: 'Reservar museu' },
          },
        ],
        pastTrips: [
          {
            id: 5,
            name: 'Chile',
            startDate: '2025-01-10',
            endDate: '2025-01-20',
            destinationCount: 3,
          },
        ],
      }).handler,
    )
    const { user } = renderApp('/')

    const visto = await screen.findByRole('link', { name: /Tirar visto/ })
    expect(visto).toHaveAttribute('href', '/trips/1?tab=checklist')
    expect(within(visto).getByText('Venceu há 2 dias')).toBeInTheDocument()
    const seguro = screen.getByRole('link', { name: /Contratar seguro viagem/ })
    expect(seguro).toHaveAttribute('href', '/trips/1?tab=suggestions&focus=SEGURO_VIAGEM%3Atrip')
    expect(within(seguro).getByText('Crítico')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Reservar museu/ })).toHaveTextContent('Vence hoje')

    const agora = screen.getByRole('link', { name: /^Agora/ })
    expect(within(agora).getByText('Em andamento')).toBeInTheDocument()
    expect(
      within(screen.getByRole('link', { name: /^Sem data/ })).getByText('Checklist vazio'),
    ).toBeInTheDocument()

    expect(screen.queryByText('Chile')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Viagem passada \(1\)/ }))
    expect(screen.getByRole('link', { name: /Chile/ })).toHaveAttribute('href', '/trips/5')
  })

  it('só com viagens passadas, não mostra o bloco de alertas', async () => {
    server.use(
      dashboard({
        ...emptyDashboard,
        pastTrips: [{ id: 5, name: 'Chile', destinationCount: 0 }],
      }).handler,
    )
    renderApp('/')

    expect(await screen.findByText('Nenhuma viagem pela frente.')).toBeInTheDocument()
    expect(screen.queryByText('Alertas')).not.toBeInTheDocument()
  })

  it('é invalidado por qualquer alteração (o motor de lacunas roda no backend)', async () => {
    const queryClient = createQueryClient()
    queryClient.setQueryData(dashboardKeys.all, emptyDashboard)

    await queryClient
      .getMutationCache()
      .build(queryClient, { mutationFn: () => Promise.resolve() })
      .execute(undefined)

    expect(queryClient.getQueryState(dashboardKeys.all)?.isInvalidated).toBe(true)
  })

  it('erro ao carregar', async () => {
    server.use(http.get('*/api/v1/dashboard', () => new HttpResponse(null, { status: 500 })))
    renderApp('/')
    expect(
      await screen.findByText('Não foi possível carregar o início. Tente recarregar a página.'),
    ).toBeInTheDocument()
  })
})
