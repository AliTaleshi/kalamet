import { clsx } from "clsx";

export function Card({ className, children }: { className?: string; children: React.ReactNode }) {
  return <section className={clsx("rounded-2xl border border-neutral-200 bg-white", className)}>{children}</section>;
}

export function CardHeader({ title, action }: { title: React.ReactNode; action?: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 border-b border-neutral-100 px-5 py-4">
      <h2 className="text-base font-bold text-neutral-800">{title}</h2>
      {action}
    </div>
  );
}
