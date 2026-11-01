import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { testTrip, tripDetail, tripList } from '@/test/trips'

describe('TripPage', () => {
  it('mostra o cabeçalho e as abas da viagem', async () => {
    server.use(tripDetail())
    renderApp('/trips/42')

    expect(
      await screen.findByRole('heading', { name: 'Férias em Portugal', level: 1 }),
    ).toBeInTheDocument()
    expect(screen.getByText('Confirmada')).toBeInTheDocument()
    const tabs = screen.getAllByRole('tab').map((tab) => tab.textContent)
    expect(tabs).toEqual([
      'Roteiro',
      'Timeline',
      'Checklist',
      'Mala',
      'Sugestões',
      'Anexos',
      'Pessoas',
    ])
  })

  it('leitor não vê o menu de ações', async () => {
    server.use(tripDetail({ ...testTrip, role: 'VIEWER' }))
    renderApp('/trips/42')

    expect(await screen.findByText('Leitor')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ações da viagem' })).not.toBeInTheDocument()
  })

  it('editor edita, mas não exclui', async () => {
    server.use(tripDetail({ ...testTrip, role: 'EDITOR' }))
    const { user } = renderApp('/trips/42')

    await user.click(await screen.findByRole('button', { name: 'Ações da viagem' }))

    expect(await screen.findByRole('menuitem', { name: 'Editar' })).toBeInTheDocument()
    expect(screen.queryByRole('menuitem', { name: 'Excluir' })).not.toBeInTheDocument()
  })

  it('exclui depois de confirmar e volta para a lista', async () => {
    let deleted = false
    server.use(
      tripDetail(),
      tripList({}).handler,
      http.delete('*/api/v1/trips/42', () => {
        deleted = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { user, router } = renderApp('/trips/42')

    await user.click(await screen.findByRole('button', { name: 'Ações da viagem' }))
    await user.click(await screen.findByRole('menuitem', { name: 'Excluir' }))
    const dialog = await screen.findByRole('dialog', { name: 'Excluir "Férias em Portugal"?' })
    expect(deleted).toBe(false)
    await user.click(within(dialog).getByRole('button', { name: 'Excluir viagem' }))

    expect(await screen.findByText('Viagem excluída.')).toBeInTheDocument()
    expect(deleted).toBe(true)
    await waitFor(() => expect(router.state.location.pathname).toBe('/trips'))
  })

  it('viagem inexistente ou sem acesso mostra aviso', async () => {
    server.use(http.get('*/api/v1/trips/99', () => HttpResponse.json({}, { status: 404 })))
    renderApp('/trips/99')

    expect(await screen.findByText(/Viagem não encontrada/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver minhas viagens' })).toHaveAttribute(
      'href',
      '/trips',
    )
  })
})
