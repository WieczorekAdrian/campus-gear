import { PackageSearch } from "lucide-react";

import { cn } from "@/lib/utils";

function EmptyState({ icon, title, description, className, ...props }) {
  const Icon = icon ?? PackageSearch;
  return (
    <div
      data-slot="empty-state"
      className={cn("flex flex-col items-center justify-center gap-2 px-6 py-12 text-center", className)}
      {...props}
    >
      <span className="flex size-11 items-center justify-center rounded-full bg-white/5 border border-white/10">
        <Icon className="size-5 text-muted-foreground" aria-hidden />
      </span>
      <p className="text-sm font-medium text-foreground">{title}</p>
      {description && (
        <p className="text-sm text-muted-foreground">{description}</p>
      )}
    </div>
  );
}

export { EmptyState };
