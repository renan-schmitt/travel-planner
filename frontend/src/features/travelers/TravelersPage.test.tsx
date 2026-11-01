import { screen, waitFor, within } from '@testing-library/react'
import { addMonths, format } from 'date-fns'
import { http, HttpResponse } from 'msw'
import type { CreateTraveler, Traveler, UpdateTraveler } from '@/features/travelers/api'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { childTraveler, meTraveler, myTravelers } from '@/test/travelers'

describe('TravelersPage', () => {
  it('lista os viajantes com "Você", faixa etária e aviso de passaporte', async () => {
    const soon = format(addMonths(new Date(), 2), 'yyyy-MM-dd')
    server.use(myTravelers([meTraveler, { ...childTraveler, passportExpiry: soon }]))
    renderApp('/profile/travelers')

    const me = await screen.findByRole('button', { name: /Ana Souza/ })
    expect(within(me).getByText('Você')).toBeInTheDocument()
    expect(within(me).queryByText(/Passaporte/)).not.toBeInTheDocument()
    const leo = screen.getByRole('button', { name: /Léo/ })
    expect(within(leo).getByText(/Criança · 2018/)).toBeInTheDocument()
    expect(within(leo).getByText(/em menos de 6 meses/)).toBeInTheDocument()
  })

  it('cadastra um viajante com duas nacionalidades', async () => {
    const bodies: CreateTraveler[] = []
    server.use(
      myTravelers([meTraveler]),
      http.post('*/api/v1/travelers', async ({ request }) => {
        bodies.push((await request.json()) as CreateTraveler)
        return HttpResponse.json(childTraveler, { status: 201 })
      }),
    )
    const { user } = renderApp('/profile/travelers')

    await user.click(await screen.findByRole('button', { name: 'Novo' }))
    const sheet = await screen.findByRole('dialog', { name: 'Novo viajante' })
    await user.type(within(sheet).getByLabelText('Nome'), 'Léo')
    await user.selectOptions(within(sheet).getByLabelText('Faixa etária'), 'CHILD')
    await user.type(within(sheet).getByLabelText('Ano de nascimento'), '2018')
    await user.selectOptions(
      within(sheet).getByRole('combobox', { name: 'Adicionar nacionalidade' }),
      'PT',
    )
    await user.type(within(sheet).getByLabelText('Validade do passaporte'), '2030-05-01')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Viajante cadastrado.')).toBeInTheDocument()
    expect(bodies).toEqual([
      {
        name: 'Léo',
        ageGroup: 'CHILD',
        birthYear: 2018,
        nationalities: ['BR', 'PT'],
        passportExpiry: '2030-05-01',
      },
    ])
  })

  it('valida nome, ano e nacionalidade', async () => {
    server.use(myTravelers([meTraveler]))
    const { user } = renderApp('/profile/travelers')

    await user.click(await screen.findByRole('button', { name: 'Novo' }))
    const sheet = await screen.findByRole('dialog', { name: 'Novo viajante' })
    await user.type(within(sheet).getByLabelText('Ano de nascimento'), '3000')
    await user.click(within(sheet).getByRole('button', { name: 'Remover Brasil' }))
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    expect(await within(sheet).findByText('Informe o nome.')).toBeInTheDocument()
    expect(within(sheet).getByText(/Informe um ano com 4 dígitos/)).toBeInTheDocument()
    expect(within(sheet).getByText('Informe ao menos uma nacionalidade.')).toBeInTheDocument()
  })

  it('edita e exclui um viajante; o "eu" não pode ser excluído', async () => {
    const updates: UpdateTraveler[] = []
    let deleted = false
    server.use(
      myTravelers(),
      http.put('*/api/v1/travelers/2', async ({ request }) => {
        const body = (await request.json()) as UpdateTraveler
        updates.push(body)
        return HttpResponse.json({ ...childTraveler, ...body } as Traveler)
      }),
      http.delete('*/api/v1/travelers/2', () => {
        deleted = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { user } = renderApp('/profile/travelers')

    await user.click(await screen.findByRole('button', { name: /Ana Souza/ }))
    let sheet = await screen.findByRole('dialog', { name: 'Editar viajante' })
    expect(within(sheet).queryByRole('button', { name: 'Excluir' })).not.toBeInTheDocument()
    await user.click(within(sheet).getByRole('button', { name: 'Cancelar' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: /Léo/ }))
    sheet = await screen.findByRole('dialog', { name: 'Editar viajante' })
    const name = within(sheet).getByLabelText('Nome')
    await user.clear(name)
    await user.type(name, 'Leonardo')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('Viajante salvo.')).toBeInTheDocument()
    expect(updates).toEqual([
      {
        version: 4,
        name: 'Leonardo',
        ageGroup: 'CHILD',
        birthYear: 2018,
        nationalities: ['BR', 'PT'],
      },
    ])

    await user.click(screen.getByRole('button', { name: /Léo/ }))
    sheet = await screen.findByRole('dialog', { name: 'Editar viajante' })
    await user.click(within(sheet).getByRole('button', { name: 'Excluir' }))
    const confirm = await screen.findByRole('dialog', { name: 'Excluir "Léo"?' })
    await user.click(within(confirm).getByRole('button', { name: 'Excluir viajante' }))
    expect(await screen.findByText('Viajante excluído.')).toBeInTheDocument()
    expect(deleted).toBe(true)
  })
})
