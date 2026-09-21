import { cn } from "@/lib/utils";

function PageHeader({ title, description, actions, className, ...props }) {
  return (
    <div
      data-slot="page-header"
      className={cn("flex flex-wrap items-end justify-between gap-4 mb-6", className)}
      {...props}
    >
      <div className="space-y-1">
        <h2 className="text-2xl font-bold tracking-tight text-foreground">{title}</h2>
        {description && (
          <p className="text-sm text-muted-foreground">{description}</p>
        )}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}

export { PageHeader };
