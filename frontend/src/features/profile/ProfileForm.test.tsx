import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { Me, UpdateMe } from '@/features/auth/api'
import { loggedIn, testUser } from '@/test/handlers'
import {
  cityAutocomplete,
  cityDetails,
  lisbon,
  lisbonSuggestion,
  placesUnavailable,
} from '@/test/places'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

function capturePatch(respond: (body: UpdateMe) => Response) {
  const bodies: UpdateMe[] = []
  server.use(
    http.patch('*/api/v1/me', async ({ request }) => {
      const body = (await request.json()) as UpdateMe
      bodies.push(body)
      return respond(body)
    }),
  )
  return bodies
}

const savedMe = (body: UpdateMe) =>
  HttpResponse.json<Me>({
    ...testUser,
    nationality: body.nationality,
    homeCity: body.homeCity,
    version: 1,
  })

describe('ProfileForm', () => {
  it('mostra os dados atuais', async () => {
    server.use(
      loggedIn({
        ...testUser,
        nationality: 'PT',
        homeCity: { name: 'Porto Alegre', countryCode: 'BR', timezone: 'America/Sao_Paulo' },
      }),
    )
    renderApp('/profile')

    expect(await screen.findByLabelText('Nacionalidade')).toHaveDisplayValue('🇵🇹 Portugal')
    const city = screen.getByRole('group', { name: 'Cidade de origem' })
    expect(within(city).getByText('Porto Alegre')).toBeInTheDocument()
    expect(within(city).getByText('Brasil · America/Sao Paulo')).toBeInTheDocument()
  })

  it('escolhe a cidade de origem no autocomplete, com fuso, numa única sessão', async () => {
    const search = cityAutocomplete([lisbonSuggestion])
    const details = cityDetails([lisbon])
    server.use(search.handler, details.handler)
    const bodies = capturePatch(savedMe)
    const { user } = renderApp('/profile')

    await user.selectOptions(await screen.findByLabelText('Nacionalidade'), 'IT')
    await user.type(screen.getByRole('combobox', { name: 'Cidade de origem' }), 'Lisb')
    await user.click(await screen.findByRole('option', { name: /Lisboa/ }))
    expect(await screen.findByText('Portugal · Europe/Lisbon')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Perfil salvo.')).toBeInTheDocument()
    expect(bodies).toEqual([
      {
        version: 0,
        nationality: 'IT',
        homeCity: {
          name: 'Lisboa',
          countryCode: 'PT',
          placeId: 'ChIJLisboa',
          timezone: 'Europe/Lisbon',
        },
      },
    ])
    // Debounce: uma busca só para "Lisb"; detalhes na mesma sessão do autocomplete.
    expect(search.requests.map((r) => r.q)).toEqual(['Lisb'])
    expect(details.sessions).toEqual([search.requests[0]!.session])
  })

  it('com o Places fora do ar, digita a cidade à mão', async () => {
    server.use(placesUnavailable())
    const bodies = capturePatch(savedMe)
    const { user } = renderApp('/profile')

    await user.type(await screen.findByRole('combobox', { name: 'Cidade de origem' }), 'Curitiba')
    expect(
      await screen.findByText('A busca de cidades está indisponível agora.'),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Digitar a cidade manualmente' }))

    expect(screen.getByLabelText('Nome da cidade')).toHaveValue('Curitiba')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('Informe o país da cidade.')).toBeInTheDocument()
    expect(bodies).toEqual([])

    await user.selectOptions(screen.getByLabelText('País'), 'BR')
    await user.selectOptions(screen.getByLabelText('Fuso horário'), 'America/Sao_Paulo')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(bodies).toEqual([
        {
          version: 0,
          nationality: 'BR',
          homeCity: { name: 'Curitiba', countryCode: 'BR', timezone: 'America/Sao_Paulo' },
        },
      ]),
    )
  })

  it('trocar a cidade e salvar sem escolher outra limpa a cidade de origem', async () => {
    server.use(loggedIn({ ...testUser, homeCity: { name: 'Recife', countryCode: 'BR' } }))
    const bodies = capturePatch(() => HttpResponse.json(testUser))
    const { user } = renderApp('/profile')

    await user.click(await screen.findByRole('button', { name: 'Trocar' }))
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(bodies).toEqual([{ version: 0, nationality: 'BR' }]))
  })

  it('avisa quando o perfil foi alterado em outro lugar (409)', async () => {
    capturePatch(() => HttpResponse.json({ status: 409 }, { status: 409 }))
    const { user } = renderApp('/profile')

    await user.click(await screen.findByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText(/alterado em outro lugar/)).toBeInTheDocument()
  })
})
