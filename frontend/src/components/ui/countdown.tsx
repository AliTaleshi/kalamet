"use client";

import { clsx } from "clsx";
import { useEffect, useState } from "react";
import { faNumber, toPersianDigits } from "@/lib/format";

/** Time left on an offer: "۶ روز", then "۰۲:۱۴:۰۹" on the last day. Client-only (no clock mismatch with the server). */
export function Countdown({ endsAt, className }: { endsAt: string; className?: string }) {
  const [left, setLeft] = useState<number | null>(null);
  useEffect(() => {
    const end = new Date(endsAt).getTime();
    const tick = () => setLeft(Math.max(0, end - Date.now()));
    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, [endsAt]);
  if (left === null || left === 0) return null;
  const total = Math.floor(left / 1000);
  const days = Math.floor(total / 86400);
  const parts = [Math.floor((total % 86400) / 3600), Math.floor((total % 3600) / 60), total % 60]
    .map((n) => String(n).padStart(2, "0"));
  // A ticking clock only matters on the last day; before that, "۶ روز" says enough.
  if (days > 0) {
    return <span className={clsx("text-sm font-bold", className)}>{faNumber(days)} روز</span>;
  }
  return (
    <span dir="ltr" className={clsx("font-mono text-sm font-bold tabular-nums", className)}>
      {toPersianDigits(parts.join(":"))}
    </span>
  );
}
