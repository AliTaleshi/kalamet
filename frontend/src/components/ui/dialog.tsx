"use client";

import { X } from "lucide-react";
import { useEffect, useId, useRef } from "react";

/** Modal built on <dialog>: focus trapping, Escape and the backdrop come from the browser. */
export function Dialog({ open, onClose, title, children }: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      onClose={onClose}
      onClick={(event) => event.target === ref.current && onClose()}
      className="m-auto w-[min(36rem,calc(100vw-2rem))] rounded-2xl p-0 backdrop:bg-black/40"
    >
      <div className="flex items-center justify-between border-b border-neutral-100 px-5 py-4">
        <h2 id={titleId} className="font-bold text-neutral-800">{title}</h2>
        <button type="button" onClick={onClose} aria-label="بستن" className="rounded-lg p-1 text-neutral-500 hover:bg-neutral-100">
          <X className="size-5" />
        </button>
      </div>
      <div className="max-h-[75vh] overflow-y-auto p-5">{open && children}</div>
    </dialog>
  );
}
