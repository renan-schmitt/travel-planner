import type { Segment, Stop } from './api'
import { emptySegment, toInstant } from './segment-form'

const stop = (id: number, arrival: string, departure: string): Stop => ({
  id,
  seq: id,
  cityName: `Cidade ${id}`,
  countryCode: 'PT',
  timezone: 'Europe/Lisbon',
  arrivalDate: arrival,
  departureDate: departure,
  nights: 0,
  version: 0,
})

const segment = (originStopId?: number, destinationStopId?: number): Segment => ({
  id: Math.random(),
  type: 'FLIGHT',
  local: false,
  originStopId,
  destinationStopId,
  departAt: '2099-03-10T10:00:00',
  departTz: 'Europe/Lisbon',
  departInstant: '2099-03-10T10:00:00Z',
  version: 0,
})

describe('formulário de trecho', () => {
  it('converte data e hora locais em instante pelo fuso', () => {
    expect(toInstant('2099-03-10', '22:40', 'America/Sao_Paulo')).toBe(
      Date.parse('2099-03-11T01:40:00Z'),
    )
    expect(toInstant('2099-07-10', '12:00', 'Europe/Lisbon')).toBe(
      Date.parse('2099-07-10T11:00:00Z'),
    )
  })

  it('sugere ida, depois o trecho entre cidades, depois a volta', () => {
    const stops = [stop(1, '2099-03-11', '2099-03-14'), stop(2, '2099-03-14', '2099-03-17')]
    const home = 'America/Sao_Paulo'

    expect(emptySegment(stops, [], home)).toMatchObject({
      origin: 'home',
      destination: 'stop:1',
      departDate: '2099-03-11',
      departTz: home,
      arriveTz: 'Europe/Lisbon',
    })
    expect(emptySegment(stops, [segment(undefined, 1)], home)).toMatchObject({
      origin: 'stop:2',
      destination: 'home',
      departDate: '2099-03-17',
    })
    expect(emptySegment(stops, [segment(undefined, 1), segment(2, undefined)], home)).toMatchObject(
      { origin: 'stop:1', destination: 'stop:2', departDate: '2099-03-14' },
    )
  })
})
