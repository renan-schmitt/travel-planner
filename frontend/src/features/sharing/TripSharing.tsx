import { zodResolver } from '@hookform/resolvers/zod'
import { Copy, Link2, LogOut, Mail, Share2, Trash2, X } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { z } from 'zod'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
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
import { NativeSelect } from '@/components/ui/native-select'
import { UserAvatar } from '@/components/UserAvatar'
import { hasRole, type Trip, type TripRole } from '@/features/trips/api'
import { ApiError } from '@/lib/api'
import {
  joinUrl,
  useCreateShareLink,
  useInviteMember,
  useMembers,
  useRemoveMember,
  useRevokeShareLink,
  useShareLinks,
  useUpdateMember,
  type LinkRole,
  type Member,
  type ShareLink,
} from './api'

const roles: TripRole[] = ['OWNER', 'EDITOR', 'VIEWER']
const linkRoles: LinkRole[] = ['VIEWER', 'EDITOR']

/** Seções de partilha da aba "Pessoas": membros, convite por e-mail e links (só o dono). */
export function TripSharing({ trip }: { trip: Trip }) {
  const isOwner = hasRole(trip.role, 'OWNER')
  return (
    <>
      <MembersCard trip={trip} isOwner={isOwner} />
      {isOwner && <ShareLinksCard tripId={trip.id} />}
    </>
  )
}

function MembersCard({ trip, isOwner }: { trip: Trip; isOwner: boolean }) {
  const { t } = useTranslation()
  const { data: members, error } = useMembers(trip.id)
  const [removing, setRemoving] = useState<Member | null>(null)

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('sharing.membersTitle')}</CardTitle>
        <CardDescription>
          {t(isOwner ? 'sharing.membersHintOwner' : 'sharing.membersHint')}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {error && <p className="text-sm text-destructive">{t('sharing.loadError')}</p>}
        {!members && !error && <p className="text-muted-foreground">{t('common.loading')}</p>}
        {members && (
          <ul className="flex flex-col divide-y">
            {members.map((member) => (
              <MemberRow
                key={member.id}
                tripId={trip.id}
                member={member}
                isOwner={isOwner}
                onRemove={() => setRemoving(member)}
              />
            ))}
          </ul>
        )}
        {isOwner && <InviteForm tripId={trip.id} />}
      </CardContent>
      <RemoveMemberDialog tripId={trip.id} member={removing} onClose={() => setRemoving(null)} />
    </Card>
  )
}

function MemberRow({
  tripId,
  member,
  isOwner,
  onRemove,
}: {
  tripId: number
  member: Member
  isOwner: boolean
  onRemove: () => void
}) {
  const { t } = useTranslation()
  const update = useUpdateMember(tripId)
  const label = member.name ?? member.email

  function changeRole(role: TripRole) {
    update.mutate(
      { member, role },
      { onError: (error) => toast.error(t(conflictKey(error, 'sharing.roleError'))) },
    )
  }

  return (
    <li className="flex flex-wrap items-center gap-3 py-3">
      <UserAvatar pictureUrl={member.pictureUrl} />
      <div className="flex min-w-0 flex-1 flex-col">
        <span className="flex flex-wrap items-center gap-2">
          <span className="truncate font-medium">
            {label}
            {member.me && <span className="text-muted-foreground"> {t('sharing.you')}</span>}
          </span>
          {member.pending && <Badge variant="warning">{t('sharing.pending')}</Badge>}
        </span>
        {member.name && (
          <span className="truncate text-sm text-muted-foreground">{member.email}</span>
        )}
      </div>
      <div className="flex items-center gap-1">
        {isOwner ? (
          <NativeSelect
            aria-label={t('sharing.roleOf', { name: label })}
            className="w-32"
            value={member.role}
            disabled={update.isPending}
            onChange={(e) => changeRole(e.target.value as TripRole)}
          >
            {roles.map((role) => (
              <option key={role} value={role}>
                {t(`trips.role.${role}`)}
              </option>
            ))}
          </NativeSelect>
        ) : (
          <Badge variant="secondary">{t(`trips.role.${member.role}`)}</Badge>
        )}
        {(isOwner || member.me) && (
          <Button
            variant="ghost"
            size="icon"
            aria-label={
              member.me
                ? t('sharing.leave')
                : t(member.pending ? 'sharing.cancelInviteOf' : 'sharing.removeOf', {
                    name: label,
                  })
            }
            onClick={onRemove}
          >
            {member.me ? (
              <LogOut strokeWidth={1.75} aria-hidden />
            ) : (
              <X strokeWidth={1.75} aria-hidden />
            )}
          </Button>
        )}
      </div>
    </li>
  )
}

function RemoveMemberDialog({
  tripId,
  member,
  onClose,
}: {
  tripId: number
  member: Member | null
  onClose: () => void
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const remove = useRemoveMember(tripId)
  const label = member ? (member.name ?? member.email) : ''
  const kind = member?.me ? 'leave' : member?.pending ? 'cancelInvite' : 'remove'

  function onConfirm() {
    if (!member) return
    remove.mutate(member, {
      onSuccess: (removed) => {
        onClose()
        if (removed.me) {
          toast.success(t('sharing.left'))
          void navigate('/trips', { replace: true })
        }
      },
      onError: (error) => {
        onClose()
        toast.error(t(conflictKey(error, 'sharing.removeError')))
      },
    })
  }

  return (
    <Dialog open={member !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t(`sharing.${kind}Title`, { name: label })}</DialogTitle>
          <DialogDescription>{t(`sharing.${kind}Description`)}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('common.cancel')}</Button>
          </DialogClose>
          <Button variant="destructive" disabled={remove.isPending} onClick={onConfirm}>
            {t(`sharing.${kind}Confirm`)}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

const inviteSchema = z.object({
  email: z.email(),
  role: z.enum(['EDITOR', 'VIEWER']),
})

function InviteForm({ tripId }: { tripId: number }) {
  const { t } = useTranslation()
  const invite = useInviteMember(tripId)
  const form = useForm({
    resolver: zodResolver(inviteSchema),
    defaultValues: { email: '', role: 'EDITOR' as const },
  })
  const emailError = form.formState.errors.email

  return (
    <form
      noValidate
      className="flex flex-col gap-3 rounded-xl border p-3"
      onSubmit={form.handleSubmit((values) =>
        invite.mutate(values, {
          onSuccess: (member) => {
            toast.success(
              t(member.pending ? 'sharing.invitedPending' : 'sharing.invitedNow', {
                email: member.email,
              }),
            )
            form.reset({ email: '', role: values.role })
          },
          onError: (error) =>
            toast.error(
              t(
                error instanceof ApiError && error.status === 409
                  ? 'sharing.inviteConflict'
                  : 'sharing.inviteError',
              ),
            ),
        }),
      )}
    >
      <p className="flex items-center gap-2 font-medium">
        <Mail className="size-4" strokeWidth={1.75} aria-hidden />
        {t('sharing.inviteTitle')}
      </p>
      <div className="flex flex-col gap-2">
        <Label htmlFor="invite-email">{t('sharing.inviteEmail')}</Label>
        <Input
          id="invite-email"
          type="email"
          autoComplete="off"
          inputMode="email"
          placeholder={t('sharing.inviteEmailPlaceholder')}
          aria-invalid={emailError ? true : undefined}
          aria-describedby={emailError ? 'invite-email-error' : 'invite-hint'}
          {...form.register('email')}
        />
        {emailError && (
          <p id="invite-email-error" className="text-sm text-destructive">
            {t('sharing.inviteEmailInvalid')}
          </p>
        )}
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="invite-role">{t('sharing.inviteRole')}</Label>
        <NativeSelect id="invite-role" {...form.register('role')}>
          {linkRoles.map((role) => (
            <option key={role} value={role}>
              {t(`sharing.roleHint.${role}`)}
            </option>
          ))}
        </NativeSelect>
      </div>
      <p id="invite-hint" className="text-sm text-muted-foreground">
        {t('sharing.inviteHint')}
      </p>
      <Button type="submit" disabled={invite.isPending}>
        {t('sharing.inviteSubmit')}
      </Button>
    </form>
  )
}

function ShareLinksCard({ tripId }: { tripId: number }) {
  const { t } = useTranslation()
  const { data: links, error } = useShareLinks(tripId, true)
  const create = useCreateShareLink(tripId)
  const [role, setRole] = useState<LinkRole>('VIEWER')
  const [revoking, setRevoking] = useState<ShareLink | null>(null)

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('sharing.linksTitle')}</CardTitle>
        <CardDescription>{t('sharing.linksHint')}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {error && <p className="text-sm text-destructive">{t('sharing.loadError')}</p>}
        {links && links.length === 0 && (
          <p className="text-muted-foreground">{t('sharing.linksEmpty')}</p>
        )}
        {links && links.length > 0 && (
          <ul className="flex flex-col divide-y">
            {links.map((link) => (
              <ShareLinkRow key={link.id} link={link} onRevoke={() => setRevoking(link)} />
            ))}
          </ul>
        )}
        <div className="flex flex-wrap items-end gap-2">
          <div className="flex min-w-36 flex-1 flex-col gap-2">
            <Label htmlFor="link-role">{t('sharing.linkRole')}</Label>
            <NativeSelect
              id="link-role"
              value={role}
              onChange={(e) => setRole(e.target.value as LinkRole)}
            >
              {linkRoles.map((r) => (
                <option key={r} value={r}>
                  {t(`sharing.roleHint.${r}`)}
                </option>
              ))}
            </NativeSelect>
          </div>
          <Button
            variant="outline"
            disabled={create.isPending}
            onClick={() =>
              create.mutate(role, {
                onSuccess: (link) => void copyLink(link, t),
                onError: () => toast.error(t('sharing.linkError')),
              })
            }
          >
            <Link2 strokeWidth={1.75} aria-hidden />
            {t('sharing.createLink')}
          </Button>
        </div>
      </CardContent>
      <RevokeLinkDialog tripId={tripId} link={revoking} onClose={() => setRevoking(null)} />
    </Card>
  )
}

type T = ReturnType<typeof useTranslation>['t']

async function copyLink(link: ShareLink, t: T) {
  try {
    await navigator.clipboard.writeText(joinUrl(link.token))
    toast.success(t('sharing.copied'))
  } catch {
    toast.error(t('sharing.copyError'))
  }
}

function ShareLinkRow({ link, onRevoke }: { link: ShareLink; onRevoke: () => void }) {
  const { t } = useTranslation()
  const url = joinUrl(link.token)
  const canShare = typeof navigator.share === 'function'

  return (
    <li className="flex flex-col gap-2 py-3">
      <div className="flex items-center gap-2">
        <Badge variant="info">{t(`trips.role.${link.role}`)}</Badge>
        <code className="min-w-0 flex-1 truncate text-sm text-muted-foreground">{url}</code>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button variant="outline" onClick={() => void copyLink(link, t)}>
          <Copy strokeWidth={1.75} aria-hidden />
          {t('sharing.copy')}
        </Button>
        {canShare && (
          <Button
            variant="outline"

            onClick={() => {
              navigator.share({ title: t('sharing.shareTitle'), url }).catch(() => {})
            }}
          >
            <Share2 strokeWidth={1.75} aria-hidden />
            {t('sharing.share')}
          </Button>
        )}
        <Button variant="ghost" className="text-destructive" onClick={onRevoke}>
          <Trash2 strokeWidth={1.75} aria-hidden />
          {t('sharing.revoke')}
        </Button>
      </div>
    </li>
  )
}

function RevokeLinkDialog({
  tripId,
  link,
  onClose,
}: {
  tripId: number
  link: ShareLink | null
  onClose: () => void
}) {
  const { t } = useTranslation()
  const revoke = useRevokeShareLink(tripId)

  return (
    <Dialog open={link !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('sharing.revokeTitle')}</DialogTitle>
          <DialogDescription>{t('sharing.revokeDescription')}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost">{t('common.cancel')}</Button>
          </DialogClose>
          <Button
            variant="destructive"
            disabled={revoke.isPending}
            onClick={() =>
              link &&
              revoke.mutate(link.id, {
                onSuccess: () => {
                  onClose()
                  toast.success(t('sharing.revoked'))
                },
                onError: () => toast.error(t('sharing.linkError')),
              })
            }
          >
            {t('sharing.revokeConfirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

/** 409 do backend em papéis/remoção = viagem ficaria sem dono (ou versão divergente). */
function conflictKey<K extends 'sharing.roleError' | 'sharing.removeError'>(
  error: unknown,
  fallback: K,
): K | 'sharing.lastOwner' {
  return error instanceof ApiError && error.status === 409 ? 'sharing.lastOwner' : fallback
}
