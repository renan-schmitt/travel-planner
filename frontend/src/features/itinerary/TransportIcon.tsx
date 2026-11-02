import {
  Bus,
  Car,
  CarFront,
  CarTaxiFront,
  Plane,
  Route,
  Ship,
  TrainFront,
  Van,
  type LucideProps,
} from 'lucide-react'
import type { TransportType } from './api'

const ICONS: Record<TransportType, React.ComponentType<LucideProps>> = {
  FLIGHT: Plane,
  TRAIN: TrainFront,
  BUS: Bus,
  CAR: Car,
  CAR_RENTAL: CarFront,
  FERRY: Ship,
  TRANSFER: Van,
  TAXI_RIDE: CarTaxiFront,
  OTHER: Route,
}

export function TransportIcon({ type, ...props }: { type: TransportType } & LucideProps) {
  const Icon = ICONS[type]
  return <Icon strokeWidth={1.75} aria-hidden {...props} />
}
