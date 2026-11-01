import { screen, within } from '@testing-library/react'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { isoInDays, summaryOf, testTrip, tripList } from '@/test/trips'

describe('TripsPage', () => {
  it('lista as próximas viagens com datas, status, contagem e papel', async () => {
    const { handler, scopes } = tripList({
      upcoming: [
        summaryOf({ ...testTrip, id: 1, name: 'Lisboa', startDate: isoInDays(10) }),
        summaryOf({
          ...testTrip,
          id: 2,
          name: 'Sem data',
          startDate: undefined,
          endDate: undefined,
          status: 'PLANNING',
          role: 'VIEWER',
        }),
      ],
    })
    server.use(handler)
    renderApp('/trips')

    const lisboa = (await screen.findByRole('link', { name: /Lisboa/ })) as HTMLElement
    expect(lisboa).toHaveAttribute('href', '/trips/1')
    expect(within(lisboa).getByText('Confirmada')).toBeInTheDocument()
    expect(within(lisboa).getByText('Faltam 10 dias')).toBeInTheDocument()

    const semData = screen.getByRole('link', { name: /Sem data/ })
    expect(within(semData).getByText('Datas a definir')).toBeInTheDocument()
    expect(within(semData).getByText('Planejando')).toBeInTheDocument()
    expect(within(semData).getByText('Leitor')).toBeInTheDocument()
    expect(scopes).toEqual(['upcoming'])
  })

  it('formata o período no padrão brasileiro', async () => {
    server.use(tripList({ upcoming: [summaryOf(testTrip)] }).handler)
    renderApp('/trips')

    expect(await screen.findByText(/1.+15 de jul\. de 2099/)).toBeInTheDocument()
  })

  it('troca para as passadas e mostra o vazio', async () => {
    const { handler, scopes } = tripList({ upcoming: [summaryOf(testTrip)] })
    server.use(handler)
    const { user, router } = renderApp('/trips')
    await screen.findByText('Férias em Portugal')

    await user.click(screen.getByRole('tab', { name: 'Passadas' }))

    expect(await screen.findByText('Nenhuma viagem concluída ainda.')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?scope=past')
    expect(scopes).toEqual(['upcoming', 'past'])
  })

  it('sem viagens, convida a criar a primeira', async () => {
    server.use(tripList({}).handler)
    const { user, router } = renderApp('/trips')

    expect(await screen.findByText(/Você ainda não tem viagens/)).toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Nova viagem' }))
    expect(router.state.location.pathname).toBe('/trips/new')
  })
})
