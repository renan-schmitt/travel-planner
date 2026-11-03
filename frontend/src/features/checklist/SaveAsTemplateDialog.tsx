import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Trip } from '@/features/trips/api'
import { useSaveChecklistAsTemplate } from './templates-api'

/** Guarda o checklist atual como template próprio (prazos viram "dias antes da partida"). */
export function SaveAsTemplateDialog({
  trip,
  open,
  onOpenChange,
}: {
  trip: Trip
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        {/* Montado a cada abertura: o nome começa com o da viagem. */}
        <SaveForm trip={trip} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function SaveForm({ trip, onDone }: { trip: Trip; onDone: () => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const save = useSaveChecklistAsTemplate(trip.id)
  const [name, setName] = useState(trip.name)
  const [invalid, setInvalid] = useState(false)

  function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    const value = name.trim()
    if (!value) {
      setInvalid(true)
      return
    }
    save.mutate(value, {
      onSuccess: () => {
        toast.success(t('templates.savedFromTrip', { name: value }), {
          action: {
            label: t('templates.manage'),
            onClick: () => void navigate('/profile/templates'),
          },
        })
        onDone()
      },
      onError: () => toast.error(t('templates.saveError')),
    })
  }

  return (
    <form noValidate onSubmit={onSubmit} className="flex flex-col gap-4">
      <DialogHeader>
        <DialogTitle>{t('templates.saveAsTitle')}</DialogTitle>
        <DialogDescription>
          {t(trip.startDate ? 'templates.saveAsHint' : 'templates.saveAsHintNoDate')}
        </DialogDescription>
      </DialogHeader>
      <div className="flex flex-col gap-2">
        <Label htmlFor="template-from-trip-name">{t('templateForm.name')}</Label>
        <Input
          id="template-from-trip-name"
          value={name}
          maxLength={120}
          aria-invalid={invalid || undefined}
          onChange={(e) => {
            setName(e.target.value)
            setInvalid(false)
          }}
        />
        {invalid && <p className="text-sm text-destructive">{t('templateForm.nameRequired')}</p>}
      </div>
      <DialogFooter>
        <DialogClose asChild>
          <Button type="button" variant="ghost">
            {t('common.cancel')}
          </Button>
        </DialogClose>
        <Button type="submit" disabled={save.isPending}>
          {t('templates.saveAs')}
        </Button>
      </DialogFooter>
    </form>
  )
}
