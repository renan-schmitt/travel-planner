import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { Me, UpdateMe } from '@/features/auth/api'
import { loggedIn, testUser } from '@/test/handlers'
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

describe('ProfileForm', () => {
  it('mostra os dados atuais', async () => {
    server.use(
      loggedIn({
        ...testUser,
        nationality: 'PT',
        homeCity: { name: 'Porto Alegre', countryCode: 'BR' },
      }),
    )
    renderApp('/profile')

    expect(await screen.findByLabelText('Nacionalidade')).toHaveDisplayValue('🇵🇹 Portugal')
    expect(screen.getByLabelText('Cidade de origem')).toHaveValue('Porto Alegre')
    expect(screen.getByLabelText('País da cidade de origem')).toHaveDisplayValue('🇧🇷 Brasil')
  })

  it('salva nacionalidade e cidade de origem com a versão lida', async () => {
    const bodies = capturePatch((body) =>
      HttpResponse.json<Me>({
        ...testUser,
        nationality: body.nationality,
        homeCity: body.homeCity,
        version: 1,
      }),
    )
    const { user } = renderApp('/profile')

    await user.selectOptions(await screen.findByLabelText('Nacionalidade'), 'IT')
    await user.type(screen.getByLabelText('Cidade de origem'), '  Curitiba ')
    await user.selectOptions(screen.getByLabelText('País da cidade de origem'), 'BR')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Perfil salvo.')).toBeInTheDocument()
    expect(bodies).toEqual([
      { version: 0, nationality: 'IT', homeCity: { name: 'Curitiba', countryCode: 'BR' } },
    ])
  })

  it('exige o país quando a cidade é informada', async () => {
    const bodies = capturePatch(() => HttpResponse.json(testUser))
    const { user } = renderApp('/profile')

    await user.type(await screen.findByLabelText('Cidade de origem'), 'Curitiba')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Informe o país da cidade de origem.')).toBeInTheDocument()
    expect(bodies).toEqual([])
  })

  it('cidade vazia limpa a cidade de origem', async () => {
    server.use(loggedIn({ ...testUser, homeCity: { name: 'Recife', countryCode: 'BR' } }))
    const bodies = capturePatch(() => HttpResponse.json(testUser))
    const { user } = renderApp('/profile')

    await user.clear(await screen.findByLabelText('Cidade de origem'))
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
