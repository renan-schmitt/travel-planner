import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { loggedOut } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import {
  editorMember,
  ownerMember,
  pendingMember,
  shareLinks,
  tripMembers,
  viewerLink,
} from '@/test/sharing'
import { myTravelers, tripTravelers } from '@/test/travelers'
import { testTrip, tripDetail, tripList } from '@/test/trips'

async function renderPeople() {
  const app = renderApp('/trips/42')
  await app.user.click(await screen.findByRole('tab', { name: 'Pessoas' }))
  return app
}

describe('Partilha (aba Pessoas)', () => {
  beforeEach(() => {
    server.use(myTravelers(), tripTravelers(42, []))
  })

  it('dono vê membros, convida por e-mail e muda papel', async () => {
    const invites: unknown[] = []
    const updates: unknown[] = []
    server.use(
      tripDetail(),
      tripMembers(),
      shareLinks(),
      http.post('*/api/v1/trips/42/members', async ({ request }) => {
        invites.push(await request.json())
        return HttpResponse.json(
          { ...pendingMember, id: 13, email: 'dani@example.com' },
          {
            status: 201,
          },
        )
      }),
      http.put('*/api/v1/trips/42/members/11', async ({ request }) => {
        updates.push(await request.json())
        return HttpResponse.json({ ...editorMember, role: 'VIEWER', version: 3 })
      }),
    )
    const { user } = await renderPeople()

    expect(await screen.findByText('Beatriz')).toBeInTheDocument()
    expect(screen.getByText('(você)')).toBeInTheDocument()
    expect(screen.getByText('caio@example.com')).toBeInTheDocument()
    expect(screen.getByText('Pendente')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Papel de Beatriz'), 'VIEWER')
    await waitFor(() => expect(updates).toEqual([{ version: 2, role: 'VIEWER' }]))

    await user.type(screen.getByLabelText('E-mail'), 'nao-e-email')
    await user.click(screen.getByRole('button', { name: 'Convidar' }))
    expect(await screen.findByText('Informe um e-mail válido.')).toBeInTheDocument()
    expect(invites).toEqual([])

    await user.clear(screen.getByLabelText('E-mail'))
    await user.type(screen.getByLabelText('E-mail'), 'dani@example.com')
    await user.selectOptions(screen.getByLabelText('Papel'), 'VIEWER')
    await user.click(screen.getByRole('button', { name: 'Convidar' }))
    await waitFor(() => expect(invites).toEqual([{ email: 'dani@example.com', role: 'VIEWER' }]))
    expect(await screen.findByText(/Convite criado para dani@example.com/)).toBeInTheDocument()
  })

  it('avisa quando o último dono seria rebaixado (409)', async () => {
    server.use(
      tripDetail(),
      tripMembers(42, [ownerMember]),
      shareLinks(),
      http.put('*/api/v1/trips/42/members/10', () => new HttpResponse(null, { status: 409 })),
    )
    const { user } = await renderPeople()

    await user.selectOptions(await screen.findByLabelText('Papel de Ana Souza'), 'EDITOR')
    expect(await screen.findByText(/pelo menos um dono/)).toBeInTheDocument()
  })

  it('dono cria, copia e revoga link', async () => {
    let links = [viewerLink]
    const created: unknown[] = []
    server.use(
      tripDetail(),
      tripMembers(),
      http.get('*/api/v1/trips/42/share-links', () => HttpResponse.json(links)),
      http.post('*/api/v1/trips/42/share-links', async ({ request }) => {
        created.push(await request.json())
        const link = { ...viewerLink, id: 6, role: 'EDITOR' as const, token: 'novo' }
        links = [...links, link]
        return HttpResponse.json(link, { status: 201 })
      }),
      http.delete('*/api/v1/trips/42/share-links/5', () => {
        links = links.filter((l) => l.id !== 5)
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { user } = await renderPeople()

    expect(
      await screen.findByText(`${window.location.origin}/join/${viewerLink.token}`),
    ).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Papel de quem entrar'), 'EDITOR')
    await user.click(screen.getByRole('button', { name: 'Criar link' }))
    await waitFor(() => expect(created).toEqual([{ role: 'EDITOR' }]))
    expect(await screen.findByText(`${window.location.origin}/join/novo`)).toBeInTheDocument()
    await waitFor(async () =>
      expect(await navigator.clipboard.readText()).toBe(`${window.location.origin}/join/novo`),
    )

    await user.click(screen.getAllByRole('button', { name: 'Revogar' })[0]!)
    const dialog = await screen.findByRole('dialog', { name: 'Revogar o link?' })
    await user.click(within(dialog).getByRole('button', { name: 'Revogar' }))
    await waitFor(() =>
      expect(screen.queryByText(new RegExp(viewerLink.token))).not.toBeInTheDocument(),
    )
  })

  it('leitor só vê os membros e pode sair da viagem', async () => {
    const viewer = { ...pendingMember, id: 20, name: 'Eu', pending: false, me: true }
    let deleted = false
    server.use(
      tripDetail({ ...testTrip, role: 'VIEWER' }),
      tripMembers(42, [{ ...ownerMember, me: false }, viewer]),
      tripList({ upcoming: [] }).handler,
      http.delete('*/api/v1/trips/42/members/20', () => {
        deleted = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { user, router } = await renderPeople()

    expect(await screen.findByText('Ana Souza')).toBeInTheDocument()
    expect(screen.queryByLabelText(/Papel de/)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Convidar' })).not.toBeInTheDocument()
    expect(screen.queryByText('Links de convite')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Sair da viagem' }))
    const dialog = await screen.findByRole('dialog', { name: 'Sair da viagem?' })
    await user.click(within(dialog).getByRole('button', { name: 'Sair' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/trips'))
    expect(deleted).toBe(true)
  })
})

describe('Entrar por link (/join/:token)', () => {
  it('entra e vai para a viagem', async () => {
    server.use(
      http.post('*/api/v1/share-links/abc/join', () =>
        HttpResponse.json({ tripId: 42, role: 'VIEWER' }),
      ),
      tripDetail({ ...testTrip, role: 'VIEWER' }),
    )
    const { router } = renderApp('/join/abc')

    await waitFor(() => expect(router.state.location.pathname).toBe('/trips/42'))
    expect(await screen.findByText('Você entrou na viagem.')).toBeInTheDocument()
  })

  it('link revogado mostra aviso', async () => {
    server.use(
      http.post('*/api/v1/share-links/velho/join', () => new HttpResponse(null, { status: 404 })),
    )
    renderApp('/join/velho')

    expect(await screen.findByText(/inválido ou foi revogado/)).toBeInTheDocument()
  })

  it('sem login, guarda o link e volta para ele depois do login', async () => {
    server.use(loggedOut())
    const { router } = renderApp('/join/abc')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(sessionStorage.getItem('returnTo')).toBe('/join/abc')
  })
})
