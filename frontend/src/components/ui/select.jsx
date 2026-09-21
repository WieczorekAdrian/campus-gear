import { ChevronDown } from "lucide-react";

import { cn } from "@/lib/utils";

function Select({ className, children, ...props }) {
  return (
    <div data-slot="select-wrapper" className={cn("relative", className)}>
      <select
        data-slot="select"
        className={cn(
          "w-full h-10 appearance-none rounded-md border border-white/10 bg-white/5 pl-3 pr-9 py-2",
          "text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 transition-colors",
          "[&>option]:bg-[#1a1a1a]",
          className
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDown
        aria-hidden
        className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
      />
    </div>
  );
}

export { Select };
