import type { LucideIcon } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'

export function EmptyState({ icon: Icon, message }: { icon: LucideIcon; message: string }) {
  return (
    <Card>
      <CardContent className="flex flex-col items-center gap-3 py-8 text-center">
        <span className="flex size-12 items-center justify-center rounded-full bg-accent text-accent-foreground">
          <Icon className="size-6" strokeWidth={1.75} aria-hidden />
        </span>
        <p className="text-muted-foreground">{message}</p>
      </CardContent>
    </Card>
  )
}
