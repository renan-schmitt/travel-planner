import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { loggedIn, testUser } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { tripMembers } from '@/test/sharing'
import { tripDetail } from '@/test/trips'
import type { Requirement, TripRequirements } from './api'

const dominicanRepublic: TripRequirements = {
  countryCode: 'DO',
  country: {
    code: 'DO',
    name: 'República Dominicana',
    flag: '🇩🇴',
    plugTypes: 'A,B',
    voltage: '120V',
    currency: 'DOP',
    schengen: false,
    brIdCardAccepted: false,
  },
  arrivalDate: '2027-03-10',
  requirements: [
    {
      id: 7,
      nationality: 'BR',
      type: 'ENTRY_FORM',
      title: 'Preencher o e-Ticket',
      description: 'Formulário de entrada e saída.',
      leadDays: 3,
      officialUrl: 'https://eticket.migracion.gob.do/',
      verifiedAt: '2026-11-01',
      dueDate: '2027-03-07',
      travelers: ['Ana', 'Bruno'],
    },
  ],
  nationalitiesWithoutData: [],
}

const japan: TripRequirements = {
  countryCode: 'JP',
  arrivalDate: '2027-03-17',
  requirements: [],
  nationalitiesWithoutData: ['BR'],
}

function tripRequirements(list: TripRequirements[]) {
  return http.get('*/api/v1/trips/42/requirements', () => HttpResponse.json(list))
}

const emptyChecklist = http.get('*/api/v1/trips/42/checklist', () => HttpResponse.json([]))

const eticket: Requirement = {
  id: 7,
  countryCode: 'DO',
  nationality: 'BR',
  type: 'ENTRY_FORM',
  title: 'Preencher o e-Ticket',
  leadDays: 3,
  officialUrl: 'https://eticket.migracion.gob.do/',
  status: 'DRAFT',
  source: 'MANUAL',
  version: 0,
}

const usVisa: Requirement = {
  id: 8,
  countryCode: 'US',
  nationality: 'BR',
  type: 'VISA',
  title: 'Visto americano',
  officialUrl: 'https://travel.state.gov/',
  status: 'PUBLISHED',
  verifiedAt: '2026-10-01',
  source: 'MANUAL',
  version: 2,
}

const admin = loggedIn({ ...testUser, isAdmin: true })

describe('Documentos da viagem', () => {
  it('mostra requisitos com prazo, link oficial, verificação e o aviso de fonte oficial', async () => {
    server.use(
      tripDetail(),
      tripMembers(),
      emptyChecklist,
      tripRequirements([dominicanRepublic, japan]),
    )
    const { user } = renderApp('/trips/42')
    await user.click(await screen.findByRole('tab', { name: 'Checklist' }))

    const section = await screen.findByRole('region', { name: /República Dominicana/ })
    expect(
      screen.getByText(/Informação indicativa: confirme sempre na fonte oficial/),
    ).toBeVisible()
    expect(within(section).getByText('Formulário de entrada')).toBeVisible()
    expect(within(section).getByText('Preencher o e-Ticket')).toBeVisible()
    expect(within(section).getByText('7 de mar. de 2027')).toBeVisible()
    expect(within(section).getByText('Ana, Bruno')).toBeVisible()
    expect(within(section).getByText('1 de nov. de 2026')).toBeVisible()
    expect(within(section).getByText(/Tomadas A, B · 120V · DOP/)).toBeVisible()
    expect(within(section).getByRole('link', { name: /Site oficial/ })).toHaveAttribute(
      'href',
      'https://eticket.migracion.gob.do/',
    )

    // Destino sem dados: "sem informações", nunca "não precisa de nada"; sem link para não-admin.
    const noData = screen.getByRole('region', { name: /Japão/ })
    expect(
      within(noData).getByText(/Sem informações de documentos para este destino \(Brasil\)/),
    ).toBeVisible()
    expect(within(noData).queryByRole('link')).not.toBeInTheDocument()
  })

  it('não aparece em viagem nacional e leva o admin ao cadastro do país sem dados', async () => {
    server.use(tripDetail(), tripMembers(), emptyChecklist, tripRequirements([]))
    const first = renderApp('/trips/42')
    await first.user.click(await screen.findByRole('tab', { name: 'Checklist' }))
    expect(await screen.findByText(/Nenhum item ainda/)).toBeVisible()
    expect(screen.queryByText(/Informação indicativa/)).not.toBeInTheDocument()
    first.unmount()

    server.use(admin, tripRequirements([japan]))
    const { user } = renderApp('/trips/42')
    await user.click(await screen.findByRole('tab', { name: 'Checklist' }))
    expect(
      await screen.findByRole('link', { name: 'Cadastrar requisitos deste país' }),
    ).toHaveAttribute('href', '/admin/requirements?country=JP')
  })
})

describe('Administração de requisitos', () => {
  it('bloqueia quem não é administrador', async () => {
    renderApp('/admin/requirements')
    expect(await screen.findByText('Só administradores podem ver esta página.')).toBeVisible()
  })

  it('lista por país, filtra por status e publica um rascunho', async () => {
    let published: unknown = null
    server.use(
      admin,
      http.get('*/api/v1/admin/requirements', () => HttpResponse.json([eticket, usVisa])),
      http.post('*/api/v1/admin/requirements/:id/publish', ({ params }) => {
        published = params.id
        return HttpResponse.json({
          ...eticket,
          status: 'PUBLISHED',
          verifiedAt: '2026-11-04',
          version: 1,
        })
      }),
    )
    const { user } = renderApp('/admin/requirements')

    const dominican = await screen.findByRole('heading', { name: /República Dominicana/ })
    expect(dominican).toBeVisible()
    expect(screen.getByRole('heading', { name: /Estados Unidos/ })).toBeVisible()
    expect(screen.getByText(/nunca verificado/)).toBeVisible()

    await user.click(screen.getByRole('button', { name: 'Publicados' }))
    expect(screen.queryByText('Preencher o e-Ticket')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Rascunhos' }))
    await user.click(screen.getByRole('button', { name: /Preencher o e-Ticket/ }))

    const sheet = await screen.findByRole('dialog', { name: 'Editar requisito' })
    expect(within(sheet).getByLabelText('Antecedência (dias)')).toHaveValue('3')
    await user.click(within(sheet).getByRole('button', { name: 'Publicar' }))
    await waitFor(() => expect(published).toBe('7'))
    expect(await screen.findByText('Requisito publicado.')).toBeVisible()
  })

  it('cria um requisito já com o país do filtro e valida os campos', async () => {
    let body: unknown
    server.use(
      admin,
      http.get('*/api/v1/admin/requirements', () => HttpResponse.json([])),
      http.post('*/api/v1/admin/requirements', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ ...eticket, id: 9 }, { status: 201 })
      }),
    )
    const { user } = renderApp('/admin/requirements?country=DO')
    expect(await screen.findByText('Nenhum requisito com estes filtros.')).toBeVisible()
    await user.click(screen.getByRole('button', { name: 'Novo' }))

    const sheet = await screen.findByRole('dialog', { name: 'Novo requisito' })
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))
    expect(await within(sheet).findByText('Informe o título.')).toBeVisible()
    expect(within(sheet).getByText('Use um link começando com https://.')).toBeVisible()

    await user.selectOptions(within(sheet).getByLabelText('Tipo'), 'ENTRY_FORM')
    await user.type(within(sheet).getByLabelText('Título'), 'Preencher o e-Ticket')
    await user.type(within(sheet).getByLabelText('Antecedência (dias)'), '3')
    await user.type(
      within(sheet).getByLabelText('Link oficial'),
      'https://eticket.migracion.gob.do/',
    )
    await user.click(within(sheet).getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(body).toEqual({
        countryCode: 'DO',
        nationality: 'BR',
        type: 'ENTRY_FORM',
        title: 'Preencher o e-Ticket',
        leadDays: 3,
        officialUrl: 'https://eticket.migracion.gob.do/',
      }),
    )
  })
})
