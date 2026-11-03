import {
  templateSchema,
  toFormValues,
  toTemplateBody,
  type TemplateFormValues,
} from './template-form'

const values: TemplateFormValues = {
  name: ' Fim de semana ',
  description: ' ',
  icon: 'CAR',
  items: [
    { category: 'BOOKINGS', title: ' Revisão do carro ', daysBefore: '10' },
    { category: 'HOME', title: 'Desligar o gás', daysBefore: '0' },
    { category: 'OTHER', title: 'Mandar fotos', daysBefore: '-2' },
    { category: 'PACKING', title: 'Toalhas', daysBefore: '' },
  ],
}

describe('template-form', () => {
  it('converte "dias antes" em offset relativo à partida', () => {
    expect(toTemplateBody(values)).toEqual({
      name: 'Fim de semana',
      description: undefined,
      icon: 'CAR',
      items: [
        { category: 'BOOKINGS', title: 'Revisão do carro', dueOffsetDays: -10 },
        { category: 'HOME', title: 'Desligar o gás', dueOffsetDays: 0 },
        { category: 'OTHER', title: 'Mandar fotos', dueOffsetDays: 2 },
        { category: 'PACKING', title: 'Toalhas', dueOffsetDays: undefined },
      ],
    })
  })

  it('volta do template para o formulário', () => {
    const form = toFormValues({
      id: 1,
      name: 'X',
      icon: 'LIST',
      system: false,
      version: 0,
      items: [
        { category: 'MONEY', title: 'Avisar banco', dueOffsetDays: -3 },
        { category: 'OTHER', title: 'Sem prazo' },
      ],
    })
    expect(form.description).toBe('')
    expect(form.items.map((i) => i.daysBefore)).toEqual(['3', ''])
  })

  it('valida nome, título e dias', () => {
    const result = templateSchema.safeParse({
      ...values,
      name: ' ',
      items: [
        { category: 'OTHER', title: '', daysBefore: '1.5' },
        { category: 'OTHER', title: 'ok', daysBefore: '400' },
      ],
    })
    expect(result.success).toBe(false)
    const messages = result.error!.issues.map((i) => `${i.path.join('.')}:${i.message}`)
    expect(messages).toEqual([
      'name:nameRequired',
      'items.0.title:titleRequired',
      'items.0.daysBefore:daysInvalid',
      'items.1.daysBefore:daysInvalid',
    ])
  })
})
