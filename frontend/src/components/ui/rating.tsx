import { clsx } from "clsx";
import { Star } from "lucide-react";
import { faNumber } from "@/lib/format";

export function RatingValue({ rating, count, className }: { rating: number | null; count?: number; className?: string }) {
  if (rating == null) return null;
  return (
    <span className={clsx("inline-flex items-center gap-1 text-xs text-neutral-600", className)}>
      <Star className="size-3.5 fill-accent-400 text-accent-400" />
      {faNumber(rating)}
      {count != null && <span className="text-neutral-400">({faNumber(count)})</span>}
    </span>
  );
}

export function Stars({ value, size = "sm" }: { value: number; size?: "sm" | "md" }) {
  return (
    <span className="inline-flex" aria-label={`${faNumber(value)} از ۵ ستاره`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <Star
          key={star}
          className={clsx(size === "md" ? "size-5" : "size-4", star <= Math.round(value) ? "fill-accent-400 text-accent-400" : "text-neutral-300")}
        />
      ))}
    </span>
  );
}
