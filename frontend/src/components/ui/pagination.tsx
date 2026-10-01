"use client";

import { clsx } from "clsx";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { faNumber } from "@/lib/format";

/** Zero-based page numbers, like the API. Shows at most seven buttons. */
export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages <= 1) return null;
  const pages = visiblePages(page, totalPages);
  return (
    <nav aria-label="صفحه‌بندی" className="flex items-center justify-center gap-1 py-6">
      <PageButton disabled={page === 0} onClick={() => onChange(page - 1)} label="صفحه قبل">
        <ChevronRight className="size-4" />
      </PageButton>
      {pages.map((p, i) =>
        p === null ? (
          <span key={`gap-${i}`} className="px-2 text-neutral-400">…</span>
        ) : (
          <PageButton key={p} active={p === page} onClick={() => onChange(p)} label={`صفحه ${faNumber(p + 1)}`}>
            {faNumber(p + 1)}
          </PageButton>
        ),
      )}
      <PageButton disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)} label="صفحه بعد">
        <ChevronLeft className="size-4" />
      </PageButton>
    </nav>
  );
}

function PageButton({ active, disabled, onClick, label, children }: {
  active?: boolean;
  disabled?: boolean;
  onClick: () => void;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      aria-current={active ? "page" : undefined}
      disabled={disabled}
      onClick={onClick}
      className={clsx(
        "flex size-9 items-center justify-center rounded-lg text-sm transition-colors disabled:opacity-40",
        active ? "bg-brand-600 font-bold text-white" : "text-neutral-700 hover:bg-neutral-100",
      )}
    >
      {children}
    </button>
  );
}

function visiblePages(page: number, total: number): (number | null)[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const set = new Set([0, total - 1, page - 1, page, page + 1].filter((p) => p >= 0 && p < total));
  const sorted = [...set].sort((a, b) => a - b);
  const result: (number | null)[] = [];
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) result.push(null);
    result.push(p);
  });
  return result;
}
