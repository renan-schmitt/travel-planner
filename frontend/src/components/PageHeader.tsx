export function PageHeader({ title, children }: { title: string; children?: React.ReactNode }) {
  return (
    <div className="mb-4 flex items-center justify-between gap-3">
      <h1 className="text-2xl font-bold lg:text-[1.75rem]">{title}</h1>
      {children}
    </div>
  )
}
