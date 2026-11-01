import { UserPlus } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { hasRole, type Trip } from '@/features/trips/api'
import { useSetTripTravelers, useTravelers, useTripTravelers, type Traveler } from './api'
import { TravelerSheet } from './TravelerSheet'
import { TravelerSummary } from './TravelerSummary'

/**
 * Aba "Pessoas", seção "Quem vai". Editor marca/desmarca os próprios viajantes e os que já estão na
 * viagem (trazidos por outros membros); cada mudança é salva na hora. Leitor só vê a lista.
 */
export function TripPeopleTab({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  const canEdit = hasRole(trip.role, 'EDITOR')
  const { data: going, error } = useTripTravelers(trip.id)

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('tripPeople.title')}</CardTitle>
        <CardDescription>
          {t(canEdit ? 'tripPeople.hintEditor' : 'tripPeople.hintViewer')}
        </CardDescription>
      </CardHeader>
      <CardContent>
        {error && <p className="text-sm text-destructive">{t('tripPeople.loadError')}</p>}
        {!going && !error && <p className="text-muted-foreground">{t('common.loading')}</p>}
        {going &&
          (canEdit ? (
            <TravelerPicker tripId={trip.id} going={going} />
          ) : (
            <GoingList going={going} />
          ))}
      </CardContent>
    </Card>
  )
}

function GoingList({ going }: { going: Traveler[] }) {
  const { t } = useTranslation()
  if (going.length === 0) return <p className="text-muted-foreground">{t('tripPeople.empty')}</p>
  return (
    <ul className="flex flex-col divide-y">
      {going.map((traveler) => (
        <li key={traveler.id} className="py-3">
          <TravelerSummary traveler={traveler} />
        </li>
      ))}
    </ul>
  )
}

function TravelerPicker({ tripId, going }: { tripId: number; going: Traveler[] }) {
  const { t } = useTranslation()
  const { data: mine } = useTravelers()
  const setTravelers = useSetTripTravelers(tripId)
  const [creating, setCreating] = useState(false)

  const goingIds = going.map((tr) => tr.id)
  // Os meus viajantes ("eu" primeiro) e depois os trazidos por outros membros.
  const options = [...(mine ?? []), ...going.filter((tr) => !tr.mine)]

  function save(ids: number[]) {
    setTravelers.mutate(ids, { onError: () => toast.error(t('tripPeople.saveError')) })
  }

  function toggle(travelerId: number, checked: boolean) {
    save(checked ? [...goingIds, travelerId] : goingIds.filter((id) => id !== travelerId))
  }

  return (
    <div className="flex flex-col gap-3">
      <ul className="flex flex-col divide-y">
        {options.map((traveler) => {
          const id = `trip-traveler-${traveler.id}`
          return (
            <li key={traveler.id} className="flex items-center gap-3 py-3">
              <Checkbox
                id={id}
                className="size-5"
                checked={goingIds.includes(traveler.id)}
                disabled={setTravelers.isPending}
                onCheckedChange={(checked) => toggle(traveler.id, checked === true)}
              />
              <label htmlFor={id} className="min-w-0 flex-1 cursor-pointer">
                <TravelerSummary traveler={traveler} />
              </label>
            </li>
          )
        })}
      </ul>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Button variant="outline" onClick={() => setCreating(true)}>
          <UserPlus strokeWidth={1.75} aria-hidden />
          {t('tripPeople.newTraveler')}
        </Button>
        <Button variant="link" asChild className="px-0">
          <Link to="/profile/travelers">{t('tripPeople.manage')}</Link>
        </Button>
      </div>
      <TravelerSheet
        open={creating}
        onOpenChange={setCreating}
        onCreated={(created) => save([...goingIds, created.id])}
      />
    </div>
  )
}
