import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { CreateTrip, Trip, UpdateTrip } from '@/features/trips/api'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { testTrip, tripDetail } from '@/test/trips'

function capture<T>(method: 'post' | 'put', path: string, respond: (body: T) => Response) {
  const bodies: T[] = []
  server.use(
    http[method](path, async ({ request }) => {
      const body = (await request.json()) as T
      bodies.push(body)
      return respond(body)
    }),
  )
  return bodies
}

describe('Nova viagem', () => {
  it('cria a viagem e abre a página dela', async () => {
    const created: Trip = { ...testTrip, id: 7, name: 'Lisboa', status: 'PLANNING' }
    const bodies = capture<CreateTrip>('post', '*/api/v1/trips', () =>
      HttpResponse.json(created, { status: 201 }),
    )
    server.use(tripDetail(created))
    const { user, router } = renderApp('/trips/new')

    await user.type(await screen.findByLabelText('Nome'), '  Lisboa ')
    await user.type(screen.getByLabelText('Início'), '2099-07-01')
    await user.type(screen.getByLabelText('Fim'), '2099-07-15')
    await user.click(screen.getByRole('radio', { name: 'Cor 3' }))
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Viagem criada.')).toBeInTheDocument()
    expect(bodies).toEqual([
      {
        name: 'Lisboa',
        startDate: '2099-07-01',
        endDate: '2099-07-15',
        status: 'PLANNING',
        coverColor: '#1D6FB8',
      },
    ])
    await waitFor(() => expect(router.state.location.pathname).toBe('/trips/7'))
  })

  it('aceita viagem sem datas', async () => {
    const bodies = capture<CreateTrip>('post', '*/api/v1/trips', () =>
      HttpResponse.json(testTrip, { status: 201 }),
    )
    server.use(tripDetail())
    const { user } = renderApp('/trips/new')

    await user.type(await screen.findByLabelText('Nome'), 'Algum dia')
    await user.selectOptions(screen.getByLabelText('Status'), 'CONFIRMED')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(bodies).toEqual([{ name: 'Algum dia', status: 'CONFIRMED', coverColor: '#0E7C86' }]),
    )
  })

  it('valida nome e fim antes do início', async () => {
    const bodies = capture<CreateTrip>('post', '*/api/v1/trips', () =>
      HttpResponse.json(testTrip, { status: 201 }),
    )
    const { user } = renderApp('/trips/new')

    await user.type(await screen.findByLabelText('Início'), '2099-07-10')
    await user.type(screen.getByLabelText('Fim'), '2099-07-01')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Informe o nome da viagem.')).toBeInTheDocument()
    expect(screen.getByText('O fim deve ser no mesmo dia ou depois do início.')).toBeInTheDocument()
    expect(bodies).toEqual([])
  })
})

describe('Editar viagem', () => {
  it('mostra os dados atuais e salva com a versão lida', async () => {
    server.use(tripDetail())
    const bodies = capture<UpdateTrip>('put', '*/api/v1/trips/42', (body) =>
      HttpResponse.json<Trip>({ ...testTrip, ...body, version: 4 }),
    )
    const { user, router } = renderApp('/trips/42/edit')

    const name = await screen.findByLabelText('Nome')
    expect(name).toHaveValue('Férias em Portugal')
    expect(screen.getByLabelText('Início')).toHaveValue('2099-07-01')
    expect(screen.getByLabelText('Status')).toHaveValue('CONFIRMED')
    expect(screen.getByRole('radio', { name: 'Cor 4' })).toBeChecked()

    await user.clear(name)
    await user.type(name, 'Portugal e Espanha')
    await user.clear(screen.getByLabelText('Fim'))
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Viagem salva.')).toBeInTheDocument()
    expect(bodies).toEqual([
      {
        version: 3,
        name: 'Portugal e Espanha',
        startDate: '2099-07-01',
        status: 'CONFIRMED',
        coverColor: '#7C5CC4',
      },
    ])
    await waitFor(() => expect(router.state.location.pathname).toBe('/trips/42'))
  })

  it('avisa quando a viagem foi alterada por outra pessoa (409)', async () => {
    server.use(tripDetail())
    capture<UpdateTrip>('put', '*/api/v1/trips/42', () =>
      HttpResponse.json({ status: 409 }, { status: 409 }),
    )
    const { user } = renderApp('/trips/42/edit')

    await user.click(await screen.findByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText(/alterada por outra pessoa/)).toBeInTheDocument()
  })

  it('leitor volta para a página da viagem', async () => {
    server.use(tripDetail({ ...testTrip, role: 'VIEWER' }))
    const { router } = renderApp('/trips/42/edit')

    await waitFor(() => expect(router.state.location.pathname).toBe('/trips/42'))
  })
})
