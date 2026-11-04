import { activitySchema, emptyActivity, toActivityBody } from './activity-form'

const schema = activitySchema(['2099-03-12', '2099-03-13'])

function errors(values: Partial<ReturnType<typeof emptyActivity>>) {
  const result = schema.safeParse({ ...emptyActivity('2099-03-12'), ...values })
  return result.success
    ? {}
    : Object.fromEntries(result.error.issues.map((i) => [i.path[0], i.message]))
}

describe('activitySchema', () => {
  it('pede título sem lugar salvo; com lugar, o título pode ficar vazio', () => {
    expect(errors({})).toEqual({ title: 'titleRequired' })
    expect(errors({ savedPlaceId: '7' })).toEqual({})
  })

  it('confere o dia e os horários', () => {
    expect(errors({ title: 'X', day: '2099-03-14' })).toEqual({ day: 'dayOutside' })
    expect(errors({ title: 'X', endTime: '10:00' })).toEqual({ startTime: 'startRequired' })
    expect(errors({ title: 'X', startTime: '10:00', endTime: '10:00' })).toEqual({
      endTime: 'endBeforeStart',
    })
    expect(errors({ title: 'X', startTime: '9:00' })).toEqual({ startTime: 'invalidTime' })
  })

  it('monta o corpo sem campos vazios', () => {
    expect(
      toActivityBody({ ...emptyActivity('2099-03-12', 7), title: '  ', notes: ' Reserva ' }),
    ).toEqual({ day: '2099-03-12', savedPlaceId: 7, notes: 'Reserva' })
  })
})
