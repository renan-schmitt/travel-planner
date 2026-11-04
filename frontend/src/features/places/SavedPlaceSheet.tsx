import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { PriceTierInput } from '@/components/PriceTierInput'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { asPriceLevel } from '@/features/hotels/hotel-search'
import { ApiError } from '@/lib/api'
import {
  updateBody,
  useDeleteSavedPlace,
  useUpdateSavedPlace,
  type SavedPlace,
  type SavedPlaceKind,
} from './api'

const KINDS: SavedPlaceKind[] = ['RESTAURANT', 'ATTRACTION', 'OTHER']

/** Bottom sheet de edição de um favorito: tipo, faixa, notas, visitado e remover. */
export function SavedPlaceSheet({
  tripId,
  stopId,
  place,
  onClose,
}: {
  tripId: number
  stopId: number
  place?: SavedPlace
  onClose: () => void
}) {
  const { t } = useTranslation()
  return (
    <Sheet open={place !== undefined} onOpenChange={(open) => !open && onClose()}>
      <SheetContent
        side="bottom"
        className="mx-auto max-h-[92dvh] w-full max-w-lg overflow-y-auto rounded-t-xl"
      >
        <SheetHeader className="pb-0">
          <SheetTitle className="text-lg font-semibold">{place?.name}</SheetTitle>
          <SheetDescription>{t('places.editDescription')}</SheetDescription>
        </SheetHeader>
        {place && (
          <EditForm
            key={`${place.id}-${place.version}`}
            tripId={tripId}
            stopId={stopId}
            place={place}
            onDone={onClose}
          />
        )}
      </SheetContent>
    </Sheet>
  )
}

function EditForm({
  tripId,
  stopId,
  place,
  onDone,
}: {
  tripId: number
  stopId: number
  place: SavedPlace
  onDone: () => void
}) {
  const { t } = useTranslation()
  const update = useUpdateSavedPlace(tripId, stopId)
  const remove = useDeleteSavedPlace(tripId, stopId)
  const [kind, setKind] = useState(place.kind)
  const [tier, setTier] = useState(asPriceLevel(place.priceTier) ?? null)
  const [notes, setNotes] = useState(place.notes ?? '')
  const [visited, setVisited] = useState(place.visited)
  const tooLong = notes.length > 2000

  function onSave(event: React.FormEvent) {
    event.preventDefault()
    if (tooLong) return
    update.mutate(
      {
        id: place.id,
        body: updateBody(place, {
          kind,
          priceTier: tier ?? undefined,
          notes: notes.trim() === '' ? undefined : notes,
          visited,
        }),
      },
      {
        onSuccess: () => {
          toast.success(t('places.updated'))
          onDone()
        },
        onError: (error) =>
          toast.error(
            t(
              error instanceof ApiError && error.status === 409
                ? 'places.conflict'
                : 'places.saveError',
            ),
          ),
      },
    )
  }

  function onRemove() {
    remove.mutate(place.id, {
      onSuccess: () => {
        toast.success(t('places.removed', { name: place.name }))
        onDone()
      },
      onError: () => toast.error(t('places.saveError')),
    })
  }

  return (
    <form onSubmit={onSave} className="flex flex-col gap-4 px-4 pb-4">
      <div className="flex flex-col gap-2">
        <Label htmlFor="saved-place-kind">{t('places.kindLabel')}</Label>
        <NativeSelect
          id="saved-place-kind"
          value={kind}
          onChange={(e) => setKind(e.target.value as SavedPlaceKind)}
        >
          {KINDS.map((k) => (
            <option key={k} value={k}>
              {t(`places.kinds.${k}`)}
            </option>
          ))}
        </NativeSelect>
      </div>
      <PriceTierInput value={tier} onChange={setTier} name="saved-place-tier" />
      <div className="flex flex-col gap-2">
        <Label htmlFor="saved-place-notes">{t('places.notes')}</Label>
        <Textarea
          id="saved-place-notes"
          value={notes}
          aria-invalid={tooLong || undefined}
          placeholder={t('places.notesPlaceholder')}
          onChange={(e) => setNotes(e.target.value)}
        />
        {tooLong && <p className="text-sm text-destructive">{t('places.notesTooLong')}</p>}
      </div>
      <div className="flex items-center gap-3">
        <Checkbox
          id="saved-place-visited"
          className="size-5"
          checked={visited}
          onCheckedChange={(checked) => setVisited(checked === true)}
        />
        <Label htmlFor="saved-place-visited">{t('places.visited')}</Label>
      </div>
      <div className="flex flex-wrap justify-between gap-2 pt-2">
        <Button
          type="button"
          variant="ghost"
          className="text-destructive"
          disabled={remove.isPending}
          onClick={onRemove}
        >
          {t('places.remove')}
        </Button>
        <Button type="submit" disabled={update.isPending || tooLong}>
          {t('places.saveChanges')}
        </Button>
      </div>
    </form>
  )
}
