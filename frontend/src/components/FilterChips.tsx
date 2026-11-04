import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

/** Grupo de chips de filtro (com rótulo acessível). */
export function ChipGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div role="group" aria-label={label} className="flex flex-wrap items-center gap-2">
      {children}
    </div>
  )
}

/** Chip de filtro liga/desliga (`aria-pressed`). */
export function Chip({
  pressed,
  className,
  ...props
}: React.ComponentProps<typeof Button> & { pressed: boolean }) {
  return (
    <Button
      type="button"
      size="sm"
      variant={pressed ? 'default' : 'outline'}
      aria-pressed={pressed}
      className={cn(
        'rounded-full',
        pressed &&
          '[&_.text-border]:text-primary-foreground/40 [&_.text-foreground]:text-primary-foreground',
        className,
      )}
      {...props}
    />
  )
}
