import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ChecklistItem } from '@/features/checklist/api'
import type { TemplateBody } from '@/features/checklist/templates-api'
import type { Trip } from '@/features/trips/api'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { tripMembers } from '@/test/sharing'
import { beachTemplate, ownTemplate, templateList } from '@/test/templates'
import { testTrip, tripDetail } from '@/test/trips'

const sunscreen: ChecklistItem = {
  id: 1,
  category: 'PACKING',
  title: 'Protetor solar',
  status: 'OPEN',
  source: 'MANUAL',
  sortOrder: 0,
  version: 0,
}

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

async function openChecklist() {
  const app = renderApp('/trips/42')
  await app.user.click(await screen.findByRole('tab', { name: 'Checklist' }))
  await screen.findByTestId('checklist-progress')
  return app
}

describe('Templates no checklist', () => {
  it('mostra a grade, a prévia com prazos relativos e aplica sem repetir', async () => {
    let items = [sunscreen]
    const guardaSol: ChecklistItem = {
      ...sunscreen,
      id: 2,
      category: 'BOOKINGS',
      title: 'Reservar guarda-sol',
      source: 'TEMPLATE',
      sortOrder: 1,
    }
    server.use(
      tripDetail(),
      tripMembers(),
      templateList(),
      http.get('*/api/v1/trips/42/checklist', () => HttpResponse.json(items)),
    )
    const bodies = capture<{ templateId: number }>(
      'post',
      '*/api/v1/trips/42/checklist/apply-template',
      () => {
        items = [sunscreen, guardaSol]
        return HttpResponse.json({ added: [guardaSol], skipped: 1 })
      },
    )
    const { user } = await openChecklist()

    await user.click(screen.getByRole('button', { name: 'Aplicar template' }))
    const sheet = await screen.findByRole('dialog')
    expect(within(sheet).getByRole('button', { name: 'Serra, 1 item' })).toBeInTheDocument()
    await user.click(within(sheet).getByRole('button', { name: 'Internacional, 3 itens' }))
    expect(within(sheet).getByText('60 dias antes')).toBeInTheDocument()
    expect(within(sheet).getByText('1 dia depois')).toBeInTheDocument()
    await user.click(within(sheet).getByRole('button', { name: 'Voltar' }))

    await user.click(within(sheet).getByRole('button', { name: 'Praia, 2 itens' }))
    expect(within(sheet).getByText('Sol e mar.')).toBeInTheDocument()
    expect(
      within(within(sheet).getByRole('list', { name: 'Reservas' })).getByText('7 dias antes'),
    ).toBeInTheDocument()
    await user.click(within(sheet).getByRole('button', { name: 'Aplicar 2 itens' }))

    expect(
      await screen.findByText('1 item adicionado. 1 já estava no checklist.'),
    ).toBeInTheDocument()
    expect(bodies).toEqual([{ templateId: 1 }])
    await waitFor(() => expect(screen.getByTestId('checklist-progress')).toHaveTextContent('0/2'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('salva o checklist como template, com o nome da viagem sugerido', async () => {
    server.use(
      tripDetail({ ...testTrip, role: 'VIEWER' }),
      tripMembers(),
      http.get('*/api/v1/trips/42/checklist', () => HttpResponse.json([sunscreen])),
    )
    const bodies = capture<{ name: string }>(
      'post',
      '*/api/v1/trips/42/checklist/save-as-template',
      () => HttpResponse.json({ ...ownTemplate, id: 20 }, { status: 201 }),
    )
    const { user } = await openChecklist()

    // Leitor não aplica templates, mas pode guardar a lista para si.
    expect(screen.queryByRole('button', { name: 'Aplicar template' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Salvar como template' }))
    const name = await screen.findByLabelText('Nome')
    expect(name).toHaveValue('Férias em Portugal')
    await user.clear(name)
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Salvar como template' }),
    )
    expect(screen.getByText('Informe o nome.')).toBeInTheDocument()
    await user.type(name, 'Verão')
    await user.keyboard('{Enter}')

    expect(await screen.findByText('Template "Verão" criado.')).toBeInTheDocument()
    expect(bodies).toEqual([{ name: 'Verão' }])
  })
})

describe('Templates na criação da viagem', () => {
  it('aplica os templates escolhidos, um de cada vez, na ordem', async () => {
    const created: Trip = { ...testTrip, id: 7, name: 'Bahia' }
    server.use(templateList(), tripDetail(created))
    capture('post', '*/api/v1/trips', () => HttpResponse.json(created, { status: 201 }))
    const applied = capture<{ templateId: number }>(
      'post',
      '*/api/v1/trips/7/checklist/apply-template',
      () => HttpResponse.json({ added: [], skipped: 0 }),
    )
    const { user, router } = renderApp('/trips/new')

    await user.type(await screen.findByLabelText('Nome'), 'Bahia')
    const international = await screen.findByRole('button', { name: 'Internacional, 3 itens' })
    await user.click(international)
    await user.click(screen.getByRole('button', { name: 'Praia, 2 itens' }))
    await user.click(screen.getByRole('button', { name: 'Serra, 1 item' }))
    await user.click(screen.getByRole('button', { name: 'Serra, 1 item' }))
    expect(international).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Serra, 1 item' })).toHaveAttribute(
      'aria-pressed',
      'false',
    )
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/trips/7'))
    expect(applied).toEqual([{ templateId: 2 }, { templateId: 1 }])
  })
})

describe('/profile/templates', () => {
  it('lista só os templates próprios e cria um novo com itens', async () => {
    server.use(templateList())
    const bodies = capture<TemplateBody>('post', '*/api/v1/checklist-templates', (body) =>
      HttpResponse.json({ ...ownTemplate, ...body, id: 30 }, { status: 201 }),
    )
    const { user } = renderApp('/profile/templates')

    expect(await screen.findByText('Serra')).toBeInTheDocument()
    expect(screen.queryByText('Praia')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Novo' }))
    const sheet = await screen.findByRole('dialog')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(within(sheet).getByText('Informe o nome.')).toBeInTheDocument()
    expect(within(sheet).getByText('Informe o item.')).toBeInTheDocument()

    await user.type(within(sheet).getByLabelText('Nome'), 'Fim de semana')
    await user.click(within(sheet).getByRole('radio', { name: 'Carro' }))
    await user.type(within(sheet).getByLabelText('Item 1: título'), 'Calibrar pneus')
    await user.selectOptions(within(sheet).getByLabelText('Item 1: categoria'), 'BOOKINGS')
    await user.type(within(sheet).getByLabelText('Item 1: dias antes da partida'), '2')
    await user.click(within(sheet).getByRole('button', { name: 'Adicionar item' }))
    await user.type(within(sheet).getByLabelText('Item 2: título'), 'Lanche')
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Template criado.')).toBeInTheDocument()
    expect(bodies).toEqual([
      {
        name: 'Fim de semana',
        icon: 'CAR',
        items: [
          { category: 'BOOKINGS', title: 'Calibrar pneus', dueOffsetDays: -2 },
          { category: 'OTHER', title: 'Lanche' },
        ],
      },
    ])
  })

  it('edita com a versão lida, avisa conflito e exclui', async () => {
    server.use(templateList([beachTemplate, ownTemplate]))
    const bodies = capture<TemplateBody & { version: number }>(
      'put',
      '*/api/v1/checklist-templates/9',
      () => HttpResponse.json({ status: 409 }, { status: 409 }),
    )
    let deleted = false
    server.use(
      http.delete('*/api/v1/checklist-templates/9', () => {
        deleted = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { user } = renderApp('/profile/templates')

    await user.click(await screen.findByRole('button', { name: /Serra/ }))
    const sheet = await screen.findByRole('dialog')
    expect(within(sheet).getByLabelText('Item 1: dias antes da partida')).toHaveValue(1)
    await user.click(within(sheet).getByRole('button', { name: 'Remover item 1' }))
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText(/O template foi alterado em outro lugar/)).toBeInTheDocument()
    expect(bodies).toEqual([{ version: 2, name: 'Serra', icon: 'MOUNTAIN', items: [] }])

    await user.click(within(sheet).getByRole('button', { name: 'Excluir' }))
    const confirm = await screen.findByRole('dialog', { name: 'Excluir "Serra"?' })
    await user.click(within(confirm).getByRole('button', { name: 'Excluir' }))
    expect(await screen.findByText('Template excluído.')).toBeInTheDocument()
    expect(deleted).toBe(true)
  })
})
