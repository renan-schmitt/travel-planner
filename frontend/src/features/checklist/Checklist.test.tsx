import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ChecklistItem } from '@/features/checklist/api'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { tripMembers } from '@/test/sharing'
import { isoInDays, testTrip, tripDetail } from '@/test/trips'

const passport: ChecklistItem = {
  id: 1,
  category: 'DOCUMENTS',
  title: 'Renovar passaporte',
  dueDate: isoInDays(-2),
  status: 'OPEN',
  assigneeUserId: 1,
  source: 'MANUAL',
  sortOrder: 0,
  version: 0,
}

const plants: ChecklistItem = {
  id: 2,
  category: 'HOME',
  title: 'Regar plantas',
  dueDate: isoInDays(2),
  status: 'OPEN',
  assigneeUserId: 2,
  source: 'MANUAL',
  sortOrder: 1,
  version: 0,
}

const gas: ChecklistItem = {
  id: 3,
  category: 'HOME',
  title: 'Desligar o gás',
  status: 'DONE',
  doneByUserId: 1,
  source: 'MANUAL',
  sortOrder: 2,
  version: 1,
}

function checklist(items: ChecklistItem[]) {
  return http.get('*/api/v1/trips/42/checklist', () => HttpResponse.json(items))
}

async function openChecklist() {
  const app = renderApp('/trips/42')
  await app.user.click(await screen.findByRole('tab', { name: 'Checklist' }))
  await screen.findByTestId('checklist-progress')
  return app
}

const progress = () => screen.getByTestId('checklist-progress')

describe('Checklist', () => {
  it('agrupa por categoria, mostra progresso, prazo e responsável', async () => {
    server.use(tripDetail(), tripMembers(), checklist([passport, plants, gas]))
    await openChecklist()

    expect(progress()).toHaveTextContent('1/3')
    const docs = screen.getByRole('list', { name: 'Documentos' })
    expect(within(docs).getByText('Renovar passaporte')).toBeInTheDocument()
    expect(within(docs).getByText(/^Venceu em/)).toHaveClass('text-destructive')
    expect(within(docs).getByRole('img', { name: 'Responsável: Ana Souza' })).toBeInTheDocument()
    const home = screen.getByRole('list', { name: 'Casa' })
    expect(within(home).getAllByRole('listitem')).toHaveLength(2)
    expect(within(home).getByText(/^Até/)).toHaveClass('text-warning-foreground')
    expect(within(home).getByRole('checkbox', { name: 'Desligar o gás' })).toBeChecked()
  })

  it('marcar é otimista: o progresso muda antes da resposta e volta se falhar', async () => {
    let release!: () => void
    const bodies: unknown[] = []
    server.use(
      tripDetail(),
      tripMembers(),
      checklist([passport, plants, gas]),
      http.patch('*/api/v1/trips/42/checklist/1/status', async ({ request }) => {
        bodies.push(await request.json())
        await new Promise<void>((resolve) => (release = resolve))
        return HttpResponse.json({ ...passport, status: 'DONE', version: 1 })
      }),
      http.patch('*/api/v1/trips/42/checklist/2/status', () =>
        HttpResponse.json({}, { status: 500 }),
      ),
    )
    const { user } = await openChecklist()

    await user.click(screen.getByRole('checkbox', { name: 'Renovar passaporte' }))
    expect(progress()).toHaveTextContent('2/3')
    expect(screen.getByRole('checkbox', { name: 'Renovar passaporte' })).toBeChecked()
    await waitFor(() => expect(bodies).toEqual([{ status: 'DONE' }]))
    release()

    await user.click(screen.getByRole('checkbox', { name: 'Regar plantas' }))
    expect(await screen.findByText(/Não foi possível atualizar o item/)).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Regar plantas' })).not.toBeChecked()
  })

  it('adição rápida: Enter adiciona na categoria escolhida', async () => {
    const created: unknown[] = []
    const items: ChecklistItem[] = []
    server.use(
      tripDetail(),
      tripMembers(),
      http.get('*/api/v1/trips/42/checklist', () => HttpResponse.json(items)),
      http.post('*/api/v1/trips/42/checklist', async ({ request }) => {
        created.push(await request.json())
        const item: ChecklistItem = {
          ...plants,
          id: 9,
          category: 'MONEY',
          title: 'Avisar o banco',
          dueDate: undefined,
        }
        items.push(item)
        return HttpResponse.json(item, { status: 201 })
      }),
    )
    const app = renderApp('/trips/42')
    await app.user.click(await screen.findByRole('tab', { name: 'Checklist' }))
    expect(await screen.findByText(/Nenhum item ainda/)).toBeInTheDocument()

    await app.user.selectOptions(screen.getByLabelText('Categoria do novo item'), 'MONEY')
    await app.user.type(screen.getByLabelText('Novo item'), '  Avisar o banco {Enter}')

    await waitFor(() => expect(created).toEqual([{ category: 'MONEY', title: 'Avisar o banco' }]))
    expect(screen.getByLabelText('Novo item')).toHaveValue('')
    const money = await screen.findByRole('list', { name: 'Dinheiro' })
    expect(within(money).getByText('Avisar o banco')).toBeInTheDocument()
  })

  it('filtra "só meus" e "esconder concluídos"', async () => {
    server.use(tripDetail(), tripMembers(), checklist([passport, plants, gas]))
    const { user } = await openChecklist()

    await user.click(screen.getByRole('button', { name: 'Só meus' }))
    expect(screen.getByRole('button', { name: 'Só meus' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('Renovar passaporte')).toBeInTheDocument()
    expect(screen.queryByText('Regar plantas')).not.toBeInTheDocument()
    expect(screen.queryByText('Desligar o gás')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Só meus' }))
    await user.click(screen.getByRole('button', { name: 'Esconder concluídos' }))
    expect(screen.getByText('Regar plantas')).toBeInTheDocument()
    expect(screen.queryByText('Desligar o gás')).not.toBeInTheDocument()
    // O progresso continua contando todos os itens.
    expect(progress()).toHaveTextContent('1/3')
  })

  it('leitor só vê: sem caixas ativas, edição nem adição', async () => {
    server.use(
      tripDetail({ ...testTrip, role: 'VIEWER' }),
      tripMembers(),
      checklist([passport, gas]),
    )
    await openChecklist()

    expect(screen.getByRole('checkbox', { name: 'Renovar passaporte' })).toBeDisabled()
    expect(screen.queryByRole('button', { name: /Editar item/ })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Novo item')).not.toBeInTheDocument()
  })

  it('edita responsável e prazo com a versão; 409 avisa', async () => {
    const updates: unknown[] = []
    let conflict = false
    server.use(
      tripDetail(),
      tripMembers(),
      checklist([passport, plants]),
      http.put('*/api/v1/trips/42/checklist/2', async ({ request }) => {
        updates.push(await request.json())
        if (conflict) return HttpResponse.json({}, { status: 409 })
        return HttpResponse.json({ ...plants, assigneeUserId: 1, version: 1 })
      }),
    )
    const { user } = await openChecklist()

    await user.click(screen.getByRole('button', { name: 'Editar item: Regar plantas' }))
    const sheet = await screen.findByRole('dialog', { name: 'Editar item' })
    expect(within(sheet).getByLabelText('Responsável')).toHaveValue('2')
    await user.selectOptions(within(sheet).getByLabelText('Responsável'), '1')
    await user.clear(within(sheet).getByLabelText('Data limite'))
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(updates).toEqual([
        { version: 0, category: 'HOME', title: 'Regar plantas', assigneeUserId: 1 },
      ]),
    )
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    conflict = true
    await user.click(screen.getByRole('button', { name: 'Editar item: Regar plantas' }))
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Salvar' }),
    )
    expect(await screen.findByText(/Alguém alterou este item agora/)).toBeInTheDocument()
  })
})
