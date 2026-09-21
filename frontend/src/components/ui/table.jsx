import { cn } from "@/lib/utils";

function Table({ className, ...props }) {
  return (
    <div data-slot="table-wrapper" className="overflow-x-auto">
      <table
        data-slot="table"
        className={cn("w-full text-sm text-left text-foreground whitespace-nowrap", className)}
        {...props}
      />
    </div>
  );
}

function TableHeader({ className, ...props }) {
  return (
    <thead
      data-slot="table-header"
      className={cn("text-xs uppercase bg-black/20 text-muted-foreground border-b border-white/10", className)}
      {...props}
    />
  );
}

function TableBody({ className, ...props }) {
  return (
    <tbody
      data-slot="table-body"
      className={cn("divide-y divide-white/5", className)}
      {...props}
    />
  );
}

function TableRow({ className, ...props }) {
  return (
    <tr
      data-slot="table-row"
      className={cn("hover:bg-white/5 transition-colors", className)}
      {...props}
    />
  );
}

function TableHead({ className, ...props }) {
  return (
    <th
      data-slot="table-head"
      scope="col"
      className={cn("px-6 py-4 font-medium tracking-wider", className)}
      {...props}
    />
  );
}

function TableCell({ className, ...props }) {
  return (
    <td
      data-slot="table-cell"
      className={cn("px-6 py-4", className)}
      {...props}
    />
  );
}

export { Table, TableHeader, TableBody, TableRow, TableHead, TableCell };
