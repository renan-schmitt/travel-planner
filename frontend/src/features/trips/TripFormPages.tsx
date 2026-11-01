import { useTranslation } from 'react-i18next'
import { Navigate, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { PageHeader } from '@/components/PageHeader'
import { Card, CardContent } from '@/components/ui/card'
import { ApiError } from '@/lib/api'
import { hasRole, useCreateTrip, useTrip, useUpdateTrip, type Trip } from './api'
import { defaultTripColor } from './trip-colors'
import { TripForm } from './TripForm'
import { toTripBody, type TripFormValues } from './trip-form'
import { TripNotFound } from './TripPage'

const emptyTrip: TripFormValues = {
  name: '',
  startDate: '',
  endDate: '',
  status: 'PLANNING',
  coverColor: defaultTripColor,
}

/** `/trips/new` */
export function NewTripPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const createTrip = useCreateTrip()

  function onSubmit(values: TripFormValues) {
    createTrip.mutate(toTripBody(values), {
      onSuccess: (trip) => {
        toast.success(t('tripForm.created'))
        void navigate(`/trips/${trip.id}`, { replace: true })
      },
      onError: () => toast.error(t('tripForm.saveError')),
    })
  }

  return (
    <>
      <PageHeader title={t('tripForm.newTitle')} />
      <Card>
        <CardContent>
          <TripForm
            initialValues={emptyTrip}
            submitting={createTrip.isPending}
            onSubmit={onSubmit}
            onCancel={() => void navigate('/trips')}
          />
        </CardContent>
      </Card>
    </>
  )
}

/** `/trips/:tripId/edit` */
export function EditTripPage() {
  const { t } = useTranslation()
  const tripId = Number(useParams().tripId)
  const { data: trip, error } = useTrip(tripId)

  if (error) return <TripNotFound />
  if (!trip) return <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
  // Leitor não edita: volta para a página da viagem.
  if (!hasRole(trip.role, 'EDITOR')) return <Navigate to={`/trips/${trip.id}`} replace />
  return <EditTripForm trip={trip} />
}

function EditTripForm({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const updateTrip = useUpdateTrip(trip.id)

  function onSubmit(values: TripFormValues) {
    updateTrip.mutate(
      { ...toTripBody(values), version: trip.version },
      {
        onSuccess: () => {
          toast.success(t('tripForm.saved'))
          void navigate(`/trips/${trip.id}`, { replace: true })
        },
        onError: (error) =>
          toast.error(
            t(
              error instanceof ApiError && error.status === 409
                ? 'tripForm.conflict'
                : 'tripForm.saveError',
            ),
          ),
      },
    )
  }

  return (
    <>
      <PageHeader title={t('tripForm.editTitle')} />
      <Card>
        <CardContent>
          <TripForm
            initialValues={{
              name: trip.name,
              // Datas vindas do roteiro não entram no formulário: salvar sem datas mantém o cálculo.
              startDate: trip.datesFromStops ? '' : (trip.startDate ?? ''),
              endDate: trip.datesFromStops ? '' : (trip.endDate ?? ''),
              status: trip.plannedStatus,
              coverColor: trip.coverColor ?? defaultTripColor,
            }}
            datesFromStops={trip.datesFromStops && Boolean(trip.startDate)}
            submitting={updateTrip.isPending}
            onSubmit={onSubmit}
            onCancel={() => void navigate(`/trips/${trip.id}`)}
          />
        </CardContent>
      </Card>
    </>
  )
}
