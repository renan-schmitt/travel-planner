import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { Stop } from '@/features/itinerary/api'
import { loggedIn, testUser } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'
import { summaryOf, testTrip, tripDetail, tripList } from '@/test/trips'
import type { Suggestion } from './api'

const lisbon: Stop = {
  id: 1,
  seq: 1,
  cityName: 'Lisboa',
  countryCode: 'PT',
  timezone: 'Europe/Lisbon',
  arrivalDate: '2099-03-11',
  departureDate: '2099-03-14',
  nights: 3,
  version: 0,
}

const insurance: Suggestion = {
  id: 1,
  ruleCode: 'SEGURO_VIAGEM',
  logicalKey: 'trip',
  severity: 'WARNING',
  status: 'PENDING',
  params: { schengen: true },
  checklist: {
    category: 'DOCUMENTS',
    title: 'Contratar seguro viagem',
    dueDate: '2099-02-25',
    automatic: false,
  },
}

const passport: Suggestion = {
  id: 2,
  ruleCode: 'PASSAPORTE_VALIDADE',
  logicalKey: '7',
  severity: 'CRITICAL',
  status: 'PENDING',
  params: { travelerId: 7, name: 'Ana', missing: false, expiry: '2099-05-01', limit: '2099-09-14' },
  checklist: {
    category: 'DOCUMENTS',
    title: 'Renovar passaporte de Ana',
    automatic: true,
  },
}

const night: Suggestion = {
  id: 3,
  ruleCode: 'NOITE_SEM_HOSPEDAGEM',
  logicalKey: '2099-03-12',
  severity: 'WARNING',
  status: 'PENDING',
  params: { date: '2099-03-12', stopId: 1, city: 'Lisboa' },
}

const arrival: Suggestion = {
  id: 4,
  ruleCode: 'TRANSFER_CHEGADA',
  logicalKey: '10',
  severity: 'WARNING',
  status: 'PENDING',
  params: {
    segmentId: 10,
    type: 'FLIGHT',
    carrier: 'TAP',
    code: 'TP 88',
    destination: 'Lisboa',
    departAt: '2099-03-10T22:40',
    departTz: 'America/Sao_Paulo',
    arriveAt: '2099-03-11T11:15',
    arriveTz: 'Europe/Lisbon',
    stopId: 1,
    city: 'Lisboa',
  },
}

const localTransport: Suggestion = {
  id: 5,
  ruleCode: 'TRANSPORTE_LOCAL',
  logicalKey: '1',
  severity: 'INFO',
  status: 'DISMISSED',
  params: { stopId: 1, city: 'Lisboa', nights: 3 },
}

function suggestionsApi(list: Suggestion[]) {
  const patches: unknown[] = []
  let current = list
  return {
    patches,
    handlers: [
      http.get('*/api/v1/trips/42/suggestions', () => HttpResponse.json(current)),
      http.patch('*/api/v1/trips/42/suggestions/:id', async ({ request, params }) => {
        const body = (await request.json()) as Partial<Suggestion>
        patches.push({ id: Number(params.id), ...body })
        current = current.map((s) => (s.id === Number(params.id) ? { ...s, ...body } : s))
        return HttpResponse.json(current.find((s) => s.id === Number(params.id)))
      }),
      http.get('*/api/v1/trips/42/stops', () => HttpResponse.json([lisbon])),
      http.get('*/api/v1/trips/42/segments', () => HttpResponse.json([])),
      http.get('*/api/v1/trips/42/lodgings', () => HttpResponse.json([])),
      http.get('*/api/v1/trips/42/checklist', () => HttpResponse.json([])),
    ],
  }
}

async function openSuggestions(path = '/trips/42?tab=suggestions') {
  const result = renderApp(path)
  await screen.findByRole('tab', { name: /Sugestões/ })
  return result
}

describe('Aba Sugestões', () => {
  it('mostra as pendentes com texto montado pelo i18n, severidade e contagem na aba', async () => {
    const api = suggestionsApi([passport, insurance, night, localTransport])
    server.use(tripDetail(), ...api.handlers)
    const { user } = await openSuggestions()

    const cards = await screen.findAllByRole('article')
    expect(cards.map((c) => within(c).getByRole('heading').textContent)).toEqual([
      'Passaporte de Ana vence em 1 de mai. de 2099',
      'Contratar seguro viagem',
      'Noite de 12/03 em Lisboa sem hospedagem',
    ])
    expect(cards[0]).toHaveTextContent('Crítico')
    expect(cards[0]).toHaveTextContent('Item no checklist: “Renovar passaporte de Ana”.')
    expect(cards[1]).toHaveTextContent('Obrigatório nos países do espaço Schengen')
    expect(cards[1]).toHaveTextContent('Ao aceitar, entra no checklist: “Contratar seguro viagem”.')
    expect(screen.getByLabelText('3 sugestões pendentes')).toHaveTextContent('3')

    // A dispensada fica num bloco recolhido e pode ser restaurada.
    await user.click(screen.getByRole('button', { name: 'Ver 1 adiada ou resolvida' }))
    expect(screen.getByText('Como vão se locomover em Lisboa?')).toBeInTheDocument()
    expect(screen.getByText('Dispensada')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Restaurar' }))
    await waitFor(() => expect(api.patches).toEqual([{ id: 5, status: 'PENDING' }]))
    expect(await screen.findAllByRole('article')).toHaveLength(4)
  })

  it('aceitar o seguro cria o item; dispensar tira da lista', async () => {
    const api = suggestionsApi([insurance, night])
    server.use(tripDetail(), ...api.handlers)
    const { user } = await openSuggestions()

    const card = (await screen.findAllByRole('article'))[0]!
    await user.click(within(card).getByRole('button', { name: 'Adicionar ao checklist' }))
    expect(await screen.findByText('Sugestão aceita.')).toBeInTheDocument()

    const nightCard = screen.getByRole('article', {
      name: 'Noite de 12/03 em Lisboa sem hospedagem',
    })
    await user.click(within(nightCard).getByRole('button', { name: 'Dispensar' }))
    await waitFor(() => expect(screen.queryAllByRole('article')).toHaveLength(0))
    expect(api.patches).toEqual([
      { id: 1, status: 'ACCEPTED' },
      { id: 3, status: 'DISMISSED' },
    ])
    expect(screen.getByText('Nenhuma lacuna encontrada. Tudo certo por aqui!')).toBeInTheDocument()
  })

  it('adia até a data escolhida', async () => {
    const api = suggestionsApi([insurance])
    server.use(tripDetail(), ...api.handlers)
    const { user } = await openSuggestions()

    await user.click(await screen.findByRole('button', { name: 'Adiar' }))
    const dialog = await screen.findByRole('dialog', { name: 'Adiar sugestão' })
    const input = within(dialog).getByLabelText('Voltar a mostrar em')
    await user.clear(input)
    await user.type(input, '2099-01-10')
    await user.click(within(dialog).getByRole('button', { name: 'Adiar' }))

    await waitFor(() =>
      expect(api.patches).toEqual([{ id: 1, status: 'SNOOZED', snoozedUntil: '2099-01-10' }]),
    )
    expect(await screen.findByText('Adiada até 10 de jan. de 2099.')).toBeInTheDocument()
  })

  it('"Resolver" abre os formulários já preenchidos', async () => {
    const api = suggestionsApi([arrival, night])
    server.use(tripDetail(), ...api.handlers)
    const { user } = await openSuggestions()

    const transfer = await screen.findByRole('article', {
      name: 'Como vai do aeroporto em Lisboa?',
    })
    expect(transfer).toHaveTextContent('O voo TAP TP 88 chega em 11/03 11:15')
    expect(within(transfer).getByRole('link', { name: /Uber/ })).toHaveAttribute(
      'href',
      'https://m.uber.com/',
    )
    await user.click(within(transfer).getByRole('button', { name: 'Adicionar trecho' }))
    const sheet = await screen.findByRole('dialog', { name: 'Novo trecho' })
    expect(within(sheet).getByRole('radio', { name: 'Transfer' })).toBeChecked()
    expect(within(sheet).getByLabelText('De')).toHaveValue('local:1')
    expect(within(sheet).getAllByLabelText('Data')[0]).toHaveValue('2099-03-11')
    expect(within(sheet).getAllByLabelText('Hora')[0]).toHaveValue('11:15')
    await user.click(within(sheet).getByRole('button', { name: 'Cancelar' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    const nightCard = screen.getByRole('article', {
      name: 'Noite de 12/03 em Lisboa sem hospedagem',
    })
    await user.click(within(nightCard).getByRole('button', { name: 'Adicionar hospedagem' }))
    const lodging = await screen.findByRole('dialog', { name: 'Nova hospedagem' })
    expect(within(lodging).getByLabelText('Check-in')).toHaveValue('2099-03-12')
  })

  it('leitor vê as sugestões sem as ações', async () => {
    const api = suggestionsApi([insurance])
    server.use(tripDetail({ ...testTrip, role: 'VIEWER' }), ...api.handlers)
    await openSuggestions()

    expect(await screen.findByRole('article')).toHaveTextContent('Contratar seguro viagem')
    expect(screen.queryByRole('button', { name: 'Dispensar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Adiar' })).not.toBeInTheDocument()
  })

  it('a noite sem hospedagem da timeline leva até a sugestão', async () => {
    const api = suggestionsApi([insurance, night])
    server.use(
      tripDetail(),
      ...api.handlers,
      http.get('*/api/v1/trips/42/itinerary', () =>
        HttpResponse.json({
          days: [
            {
              date: '2099-03-12',
              stops: [lisbon],
              events: [],
              overnight: { kind: 'MISSING' },
              gap: true,
            },
          ],
        }),
      ),
    )
    const { user, router } = await openSuggestions('/trips/42?tab=timeline')

    await user.click(await screen.findByRole('button', { name: 'Ver sugestão' }))
    expect(await screen.findByRole('tab', { name: /Sugestões/, selected: true })).toBeVisible()
    expect(router.state.location.search).toBe(
      '?tab=suggestions&focus=NOITE_SEM_HOSPEDAGEM%3A2099-03-12',
    )
    expect(
      await screen.findByRole('article', { name: 'Noite de 12/03 em Lisboa sem hospedagem' }),
    ).toHaveClass('ring-3')
  })

  it('o card da viagem mostra as sugestões pendentes', async () => {
    const { handler } = tripList({ upcoming: [summaryOf({ ...testTrip, pendingSuggestions: 4 })] })
    server.use(loggedIn(testUser), handler)
    renderApp('/trips')

    expect(await screen.findByText('4 sugestões pendentes')).toBeInTheDocument()
  })
})
