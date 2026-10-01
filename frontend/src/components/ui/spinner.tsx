import { clsx } from "clsx";

export function Spinner({ className }: { className?: string }) {
  return (
    <span
      role="status"
      aria-label="در حال بارگذاری"
      className={clsx("inline-block animate-spin rounded-full border-2 border-current border-e-transparent", className ?? "size-5")}
    />
  );
}

export function PageSpinner() {
  return (
    <div className="flex justify-center py-20 text-brand-600">
      <Spinner className="size-8" />
    </div>
  );
}
