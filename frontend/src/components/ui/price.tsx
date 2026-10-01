import { clsx } from "clsx";
import { faNumber, toman } from "@/lib/format";

/** "۱۲٬۹۰۰٬۰۰۰ تومان" with an optional crossed-out price and discount badge. Amounts are Rial. */
export function Price({
  price,
  originalPrice,
  discountPercent,
  size = "md",
  className,
}: {
  price: number;
  originalPrice?: number | null;
  discountPercent?: number;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const text = size === "lg" ? "text-2xl" : size === "sm" ? "text-sm" : "text-base";
  return (
    <div className={clsx("flex flex-col items-end gap-0.5", className)}>
      {originalPrice != null && originalPrice > price && (
        <div className="flex items-center gap-2">
          <span className="text-xs text-neutral-400 line-through">{toman(originalPrice)}</span>
          {discountPercent ? <DiscountBadge percent={discountPercent} /> : null}
        </div>
      )}
      <div className="flex items-baseline gap-1">
        <span className={clsx("font-bold text-neutral-900", text)}>{price === 0 ? "رایگان" : toman(price)}</span>
        {price > 0 && <span className="text-xs text-neutral-500">تومان</span>}
      </div>
    </div>
  );
}

export function DiscountBadge({ percent }: { percent: number }) {
  return (
    <span className="rounded-full bg-danger-500 px-1.5 py-0.5 text-xs font-bold text-white">
      {faNumber(percent)}٪
    </span>
  );
}
