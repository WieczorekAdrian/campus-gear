import { cva } from "class-variance-authority";

import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold transition-colors",
  {
    variants: {
      variant: {
        default: "border-transparent bg-primary text-primary-foreground",
        secondary: "border-transparent bg-secondary text-secondary-foreground",
        success:
          "border-green-500/20 bg-green-500/10 text-green-400",
        muted: "border-white/10 bg-white/10 text-muted-foreground",
        destructive:
          "border-transparent bg-destructive/10 text-destructive",
        outline: "text-foreground",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

function statusVariant(status) {
  switch (status) {
    case "AKTYWNA":
    case "DOSTEPNY":
      return "success";
    case "ANULOWANA":
    case "ZNISZCZONY":
      return "destructive";
    case "ZAKONCZONA":
    case "WYPOZYCZONY":
    case "ZAREZERWOWANY":
      return "secondary";
    default:
      return "muted";
  }
}

function Badge({ className, variant, status, ...props }) {
  return (
    <span
      data-slot="badge"
      className={cn(badgeVariants({ variant: variant ?? (status ? statusVariant(status) : "default") }), className)}
      {...props}
    />
  );
}

export { Badge, badgeVariants };
