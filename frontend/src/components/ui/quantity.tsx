"use client";

import { Minus, Plus, Trash2 } from "lucide-react";
import { faNumber } from "@/lib/format";
import { Spinner } from "./spinner";

/** − 2 + stepper; at quantity 1 the minus becomes a remove button. */
export function QuantityStepper({ value, max, busy, onChange }: {
  value: number;
  max: number;
  busy?: boolean;
  onChange: (quantity: number) => void;
}) {
  return (
    <div className="inline-flex h-10 items-center gap-3 rounded-lg border border-neutral-200 px-2 text-brand-700">
      <button
        type="button"
        aria-label="افزایش تعداد"
        disabled={busy || value >= max}
        onClick={() => onChange(value + 1)}
        className="disabled:text-neutral-300"
      >
        <Plus className="size-4" />
      </button>
      <span className="min-w-5 text-center font-bold">{busy ? <Spinner className="size-4" /> : faNumber(value)}</span>
      <button
        type="button"
        aria-label={value <= 1 ? "حذف از سبد" : "کاهش تعداد"}
        disabled={busy}
        onClick={() => onChange(value - 1)}
        className="disabled:text-neutral-300"
      >
        {value <= 1 ? <Trash2 className="size-4 text-danger-500" /> : <Minus className="size-4" />}
      </button>
    </div>
  );
}
