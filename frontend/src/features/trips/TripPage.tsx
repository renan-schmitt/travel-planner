import {
  CalendarDays,
  CalendarRange,
  ListChecks,
  Luggage,
  MapPinned,
  MoreVertical,
  Paperclip,
  Pencil,
  SearchX,
  Sparkles,
  Trash2,
  UsersRound,
  type LucideIcon,
} from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/EmptyState'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { ChecklistTab } from '@/features/checklist/ChecklistTab'
import { ItineraryTab } from '@/features/itinerary/ItineraryTab'
import { TimelineTab } from '@/features/itinerary/TimelineTab'
import { pendingCount, useSuggestions } from '@/features/suggestions/api'
import { SuggestionsTab } from '@/features/suggestions/SuggestionsTab'
import { TripPeopleTab } from '@/features/travelers/TripPeopleTab'
import { hasRole, useDeleteTrip, useTrip, type Trip } from './api'
import { defaultTripColor } from './trip-colors'
import { TripDates } from './TripDates'
import { TripStatusBadge } from './TripStatusBadge'
import { useTripTab, type TabKey } from './trip-tabs'

/** Abas da viagem; cada uma é preenchida numa fase seguinte do plano. */
const tabs: { key: TabKey; icon: LucideIcon }[] = [
  { key: 'itinerary', icon: MapPinned },
  { key: 'timeline', icon: CalendarRange },
  { key: 'checklist', icon: ListChecks },
  { key: 'packing', icon: Luggage },
  { key: 'suggestions', icon: Sparkles },
  { key: 'attachments', icon: Paperclip },
  { key: 'people', icon: UsersRound },
]

/** `/trips/:tripId`: cabeçalho (nome, datas, status, menu) e abas. */
export function TripPage() {
  const { t } = useTranslation()
  const tripId = Number(useParams().tripId)
  const { data: trip, error } = useTrip(tripId)

  if (error) return <TripNotFound />
  if (!trip) return <p className="py-8 text-center text-muted-foreground">{t('common.loading')}</p>
  return <TripTabs trip={trip} />
}

function TripTabs({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  // Durante a viagem, a timeline é a tela principal (ESP §6.3).
  const { tab, goTo } = useTripTab(trip.status === 'IN_PROGRESS' ? 'timeline' : 'itinerary')
  const { data: suggestions } = useSuggestions(trip.id)
  const pending = pendingCount(suggestions)

  return (
    <>
      <TripHeader trip={trip} />
      <Tabs value={tab} onValueChange={(value) => goTo(value as TabKey)}>
        {/* Rola na horizontal no celular; as abas não cabem em 360 px. */}
        <div className="-mx-4 [scrollbar-width:none] overflow-x-auto px-4">
          <TabsList variant="line" className="h-11 w-max">
            {tabs.map(({ key, icon: Icon }) => (
              <TabsTrigger key={key} value={key} className="h-full px-3">
                <Icon strokeWidth={1.75} aria-hidden />
                {t(`trip.tabs.${key}`)}
                {key === 'suggestions' && pending > 0 && (
                  <span
                    className="ml-0.5 min-w-5 rounded-full bg-warning-soft px-1.5 text-xs font-semibold text-warning-foreground tabular"
                    aria-label={t('suggestions.pendingBadge', { count: pending })}
                  >
                    {pending}
                  </span>
                )}
              </TabsTrigger>
            ))}
          </TabsList>
        </div>
        {tabs.map(({ key, icon }) => (
          <TabsContent key={key} value={key}>
            {key === 'itinerary' ? (
              <ItineraryTab trip={trip} />
            ) : key === 'timeline' ? (
              <TimelineTab trip={trip} />
            ) : key === 'checklist' ? (
              <ChecklistTab trip={trip} />
            ) : key === 'suggestions' ? (
              <SuggestionsTab trip={trip} />
            ) : key === 'people' ? (
              <TripPeopleTab trip={trip} />
            ) : (
              <EmptyState icon={icon} message={t('trip.comingSoon')} />
            )}
          </TabsContent>
        ))}
      </Tabs>
    </>
  )
}

function TripHeader({ trip }: { trip: Trip }) {
  const { t } = useTranslation()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const canEdit = hasRole(trip.role, 'EDITOR')
  const canDelete = hasRole(trip.role, 'OWNER')

  return (
    <Card className="mb-3 gap-3 overflow-hidden pt-0">
      <div className="h-2" style={{ backgroundColor: trip.coverColor ?? defaultTripColor }} />
      <div className="flex items-start justify-between gap-2 pl-4">
        <div className="flex min-w-0 flex-col gap-2 pt-1">
          <h1 className="text-2xl leading-tight font-bold break-words lg:text-[1.75rem]">
            {trip.name}
          </h1>
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <CalendarDays className="size-4 shrink-0" strokeWidth={1.75} aria-hidden />
            <TripDates startDate={trip.startDate} endDate={trip.endDate} />
          </p>
          <div className="flex flex-wrap items-center gap-2">
            <TripStatusBadge status={trip.status} />
            {trip.role !== 'OWNER' && (
              <span className="text-sm text-muted-foreground">{t(`trips.role.${trip.role}`)}</span>
            )}
          </div>
        </div>
        {canEdit && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="mr-1" aria-label={t('trip.menu')}>
                <MoreVertical className="size-5" strokeWidth={1.75} />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              <DropdownMenuItem asChild className="min-h-11">
                <Link to={`/trips/${trip.id}/edit`}>
                  <Pencil strokeWidth={1.75} />
                  {t('trip.edit')}
                </Link>
              </DropdownMenuItem>
              {canDelete && (
                <DropdownMenuItem
                  variant="destructive"
                  className="min-h-11"
                  onSelect={() => setConfirmDelete(true)}
                >
                  <Trash2 strokeWidth={1.75} />
                  {t('trip.delete')}
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>
      {canDelete && (
        <DeleteTripDialog trip={trip} open={confirmDelete} onOpenChange={setConfirmDelete} />
      )}
    </Card>
  )
}

function DeleteTripDialog({
  trip,
  open,
  onOpenChange,
}: {
  trip: Trip
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const deleteTrip = useDeleteTrip(trip.id)

  function onConfirm() {
    deleteTrip.mutate(undefined, {
      onSuccess: () => {
        toast.success(t('trip.deleted'))
        void navigate('/trips', { replace: true })
      },
      onError: () => toast.error(t('trip.deleteError')),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('trip.deleteTitle', { name: trip.name })}</DialogTitle>
          <DialogDescription>{t('trip.deleteDescription')}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('trip.deleteCancel')}</Button>
          </DialogClose>
          <Button variant="destructive" disabled={deleteTrip.isPending} onClick={onConfirm}>
            {t('trip.deleteConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

/** Viagem inexistente ou sem acesso (o backend responde 404 nos dois casos). */
export function TripNotFound() {
  const { t } = useTranslation()
  return (
    <div className="flex flex-col gap-3">
      <EmptyState icon={SearchX} message={t('trip.notFound')} />
      <Button variant="outline" asChild className="self-center">
        <Link to="/trips">{t('trip.backToTrips')}</Link>
      </Button>
    </div>
  )
}
