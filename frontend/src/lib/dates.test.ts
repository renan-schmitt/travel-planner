import { formatDuration, formatLocalDateTime, gmtLabel } from './dates'

describe('datas de transporte', () => {
  it('formata o horário local sem converter de fuso', () => {
    expect(formatLocalDateTime('2027-03-10T22:40:00')).toBe('10/03 22:40')
    expect(formatLocalDateTime('2027-03-11T07:05')).toBe('11/03 07:05')
  })

  it('mostra o offset do instante', () => {
    expect(gmtLabel('2027-03-10T22:40:00-03:00')).toBe('GMT-3')
    expect(gmtLabel('2027-03-11T11:15:00Z')).toBe('GMT+0')
    expect(gmtLabel('2027-03-11T11:15:00+05:30')).toBe('GMT+5:30')
  })

  it('calcula a duração pelos instantes, não pelo relógio local', () => {
    expect(formatDuration('2027-03-10T22:40:00-03:00', '2027-03-11T11:15:00Z')).toBe('9h35')
    expect(formatDuration('2027-03-14T09:00:00Z', '2027-03-14T09:45:00Z')).toBe('45min')
    expect(formatDuration('2027-03-14T09:00:00Z', '2027-03-14T11:00:00Z')).toBe('2h')
  })
})
