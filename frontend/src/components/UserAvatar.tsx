import { UserRound } from 'lucide-react'
import { useState } from 'react'
import { cn } from 'cn'

/** Foto do usuário (Google) ou um ícone genérico, se não houver foto ou ela falhar. */
export function UserAvatar({
  pictureUrl,
  className,
}: {
  pictureUrl?: string | null
  className?: string
}) {
  const [failed, setFailed] = useState(false)
  if (pictureUrl && !failed) {
    return (
      <img
        src={pictureUrl}
        alt=""
        // O googleusercontent.com recusa imagens com referrer de outros sites.
        referrerPolicy="no-referrer"
        className={cn('size-9 shrink-0 rounded-full object-cover', className)}
        onError={() => setFailed(true)}
      />
    )
  }
  return (
    <span
      className={cn(
        'flex size-9 shrink-0 items-center justify-center rounded-full bg-accent text-accent-foreground',
        className,
      )}
    >
      <UserRound className="size-5" strokeWidth={1.75} aria-hidden />
    </span>
  )
}
