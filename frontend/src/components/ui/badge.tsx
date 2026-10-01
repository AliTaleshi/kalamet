import { clsx } from "clsx";

type Tone = "neutral" | "brand" | "accent" | "success" | "danger" | "warning";

const TONES: Record<Tone, string> = {
  neutral: "bg-neutral-100 text-neutral-700",
  brand: "bg-brand-50 text-brand-700",
  accent: "bg-accent-100 text-accent-700",
  success: "bg-emerald-50 text-emerald-700",
  danger: "bg-red-50 text-danger-600",
  warning: "bg-amber-50 text-amber-700",
};

export function Badge({ tone = "neutral", children, className }: { tone?: Tone; children: React.ReactNode; className?: string }) {
  return (
    <span className={clsx("inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium", TONES[tone], className)}>
      {children}
    </span>
  );
}
