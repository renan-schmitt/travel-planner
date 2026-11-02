import type { Lodging, Stop } from './api'
import { emptyLodging, lodgingSchema, toLodgingBody } from './lodging-form'

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

const lodging = (checkIn: string, checkOut: string, stopId = 1): Lodging => ({
  id: Math.random(),
  stopId,
  kind: 'HOTEL',
  name: 'Hotel',
  checkIn,
  checkOut,
  nights: 1,
  version: 0,
})

const values = (checkIn: string, checkOut: string) => ({
  ...emptyLodging(lisbon, []),
  name: 'Hotel',
  checkIn,
  checkOut,
})

describe('formulário de hospedagem', () => {
  it('sugere a estadia inteira na parada sem hospedagem', () => {
    expect(emptyLodging(lisbon, [])).toMatchObject({
      checkIn: '2099-03-11',
      checkOut: '2099-03-14',
      priceTier: null,
    })
  })

  it('sugere o check-in depois da última hospedagem da parada', () => {
    const others = [lodging('2099-03-11', '2099-03-12'), lodging('2099-03-01', '2099-03-13', 2)]
    expect(emptyLodging(lisbon, others)).toMatchObject({
      checkIn: '2099-03-12',
      checkOut: '2099-03-14',
    })
  })

  it('valida check-out depois do check-in e dentro das datas da parada', () => {
    const schema = lodgingSchema(lisbon)
    expect(schema.safeParse(values('2099-03-11', '2099-03-14')).success).toBe(true)
    const issue = (checkIn: string, checkOut: string) =>
      schema.safeParse(values(checkIn, checkOut)).error?.issues[0]
    expect(issue('2099-03-12', '2099-03-12')).toMatchObject({
      path: ['checkOut'],
      message: 'checkOutBeforeCheckIn',
    })
    expect(issue('2099-03-10', '2099-03-12')).toMatchObject({
      path: ['checkIn'],
      message: 'outsideStop',
    })
    expect(issue('2099-03-12', '2099-03-15')).toMatchObject({
      path: ['checkOut'],
      message: 'outsideStop',
    })
  })

  it('omite os campos opcionais vazios no corpo do pedido', () => {
    expect(toLodgingBody({ ...values('2099-03-11', '2099-03-14'), name: ' Hotel ' })).toEqual({
      kind: 'HOTEL',
      name: 'Hotel',
      checkIn: '2099-03-11',
      checkOut: '2099-03-14',
    })
  })

  it('não envia regime, faixa de preço nem reserva na casa de amigo/parente', () => {
    const body = toLodgingBody({
      ...values('2099-03-11', '2099-03-14'),
      kind: 'FRIENDS_HOME',
      boardType: 'BREAKFAST',
      priceTier: 2,
      bookingRef: 'X',
      address: 'Rua das Flores, 10',
    })
    expect(body).toEqual({
      kind: 'FRIENDS_HOME',
      name: 'Hotel',
      address: 'Rua das Flores, 10',
      checkIn: '2099-03-11',
      checkOut: '2099-03-14',
    })
  })
})
