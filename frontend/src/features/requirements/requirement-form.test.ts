import { emptyRequirement, requirementSchema, toBody } from './requirement-form'

describe('requirement form', () => {
  it('validates the official link, the lead days and the country', () => {
    const result = requirementSchema.safeParse({
      ...emptyRequirement(),
      title: 'Visto',
      officialUrl: 'eticket.gob.do',
      leadDays: '-3',
    })
    expect(result.success).toBe(false)
    const messages = result.error!.issues.map((i) => `${i.path.join('.')}:${i.message}`)
    expect(messages).toEqual(
      expect.arrayContaining([
        'countryCode:countryRequired',
        'officialUrl:urlInvalid',
        'leadDays:leadDaysInvalid',
      ]),
    )
  })

  it('builds the request body with optional fields left out', () => {
    expect(
      toBody({
        ...emptyRequirement('DO'),
        type: 'ENTRY_FORM',
        title: ' e-Ticket ',
        leadDays: '3',
        officialUrl: 'https://eticket.migracion.gob.do/',
      }),
    ).toEqual({
      countryCode: 'DO',
      nationality: 'BR',
      type: 'ENTRY_FORM',
      title: 'e-Ticket',
      description: undefined,
      leadDays: 3,
      officialUrl: 'https://eticket.migracion.gob.do/',
    })
  })
})
