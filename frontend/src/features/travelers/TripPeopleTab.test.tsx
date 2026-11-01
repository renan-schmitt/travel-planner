import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { Traveler } from '@/features/travelers/api'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import {
  childTraveler,
  meTraveler,
  myTravelers,
  otherTraveler,
  tripTravelers,
} from '@/test/travelers'
import { testTrip, tripDetail } from '@/test/trips'

function captureSet(respond: (ids: number[]) => Traveler[]) {
  const bodies: number[][] = []
  server.use(
    http.put('*/api/v1/trips/42/travelers', async ({ request }) => {
      const { travelerIds } = (await request.json()) as { travelerIds: number[] }
      bodies.push(travelerIds)
      return HttpResponse.json(respond(travelerIds))
    }),
  )
  return bodies
}

const all = [meTraveler, childTraveler, otherTraveler]
const byIds = (ids: number[]) => all.filter((t) => ids.includes(t.id))

describe('Aba Pessoas', () => {
  it('editor marca e desmarca quem vai, mantendo quem outro membro trouxe', async () => {
    server.use(tripDetail(), myTravelers(), tripTravelers(42, [meTraveler, otherTraveler]))
    const bodies = captureSet(byIds)
    const { user } = renderApp('/trips/42')

    await user.click(await screen.findByRole('tab', { name: 'Pessoas' }))
    const leo = await screen.findByRole('checkbox', { name: /Léo/ })
    expect(screen.getByRole('checkbox', { name: /Ana Souza/ })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: /Bia/ })).toBeChecked()
    expect(leo).not.toBeChecked()

    await user.click(leo)
    await waitFor(() => expect(bodies).toEqual([[1, 3, 2]]))
    await waitFor(() => expect(screen.getByRole('checkbox', { name: /Léo/ })).toBeChecked())

    await user.click(screen.getByRole('checkbox', { name: /Bia/ }))
    await waitFor(() => expect(bodies[1]).toEqual([1, 2]))
  })

  it('novo viajante pelo atalho já entra na viagem', async () => {
    const created: Traveler = { ...childTraveler, id: 9, name: 'Nina', ageGroup: 'BABY' }
    server.use(
      tripDetail(),
      myTravelers([meTraveler]),
      tripTravelers(42, [meTraveler]),
      http.post('*/api/v1/travelers', () => HttpResponse.json(created, { status: 201 })),
    )
    const bodies = captureSet(() => [meTraveler, created])
    const { user } = renderApp('/trips/42')

    await user.click(await screen.findByRole('tab', { name: 'Pessoas' }))
    await user.click(await screen.findByRole('button', { name: 'Novo viajante' }))
    const sheet = await screen.findByRole('dialog', { name: 'Novo viajante' })
    await user.type(within(sheet).getByLabelText('Nome'), 'Nina')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(bodies).toEqual([[1, 9]]))
  })

  it('leitor só vê a lista', async () => {
    server.use(tripDetail({ ...testTrip, role: 'VIEWER' }), tripTravelers(42, [otherTraveler]))
    const { user } = renderApp('/trips/42')

    await user.click(await screen.findByRole('tab', { name: 'Pessoas' }))

    expect(await screen.findByText('Bia')).toBeInTheDocument()
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Novo viajante' })).not.toBeInTheDocument()
  })
})
