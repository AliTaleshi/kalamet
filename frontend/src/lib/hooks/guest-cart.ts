"use client";

import { useSyncExternalStore } from "react";

/**
 * Cart of a visitor who has not signed in, kept in localStorage. Each line keeps a snapshot of
 * what the visitor saw; real prices and stock are applied when it is merged after sign-in.
 */
export type GuestLine = {
  variantId: number;
  quantity: number;
  productSlug: string;
  productName: string;
  imageUrl: string | null;
  attributes: Record<string, string>;
  unitPrice: number;
  originalPrice: number | null;
  maxQuantity: number;
};

const KEY = "kalamet.guest-cart";
const EVENT = "kalamet:guest-cart";
const EMPTY: GuestLine[] = [];
let cache: { raw: string | null; lines: GuestLine[] } = { raw: null, lines: EMPTY };

function read(): GuestLine[] {
  let raw: string | null = null;
  try {
    raw = window.localStorage.getItem(KEY);
  } catch {
    return EMPTY;   // storage blocked (private mode, disabled cookies)
  }
  if (raw !== cache.raw) {
    try {
      cache = { raw, lines: raw ? (JSON.parse(raw) as GuestLine[]) : EMPTY };
    } catch {
      cache = { raw, lines: EMPTY };
    }
  }
  return cache.lines;
}

function write(lines: GuestLine[]) {
  try {
    if (lines.length) window.localStorage.setItem(KEY, JSON.stringify(lines));
    else window.localStorage.removeItem(KEY);
  } catch {
    // Storage blocked: the cart simply does not persist.
  }
  window.dispatchEvent(new Event(EVENT));
}

function subscribe(onChange: () => void) {
  window.addEventListener(EVENT, onChange);
  window.addEventListener("storage", onChange);   // other tabs
  return () => {
    window.removeEventListener(EVENT, onChange);
    window.removeEventListener("storage", onChange);
  };
}

export function useGuestCart(): GuestLine[] {
  return useSyncExternalStore(subscribe, read, () => EMPTY);
}

export const guestCart = {
  lines: read,
  add(line: Omit<GuestLine, "quantity">, quantity: number) {
    const lines = read();
    const existing = lines.find((l) => l.variantId === line.variantId);
    const next = existing
      ? lines.map((l) => (l.variantId === line.variantId ? { ...l, quantity: Math.min(l.quantity + quantity, l.maxQuantity) } : l))
      : [...lines, { ...line, quantity: Math.min(quantity, line.maxQuantity) }];
    write(next);
  },
  setQuantity(variantId: number, quantity: number) {
    write(quantity <= 0
      ? read().filter((l) => l.variantId !== variantId)
      : read().map((l) => (l.variantId === variantId ? { ...l, quantity: Math.min(quantity, l.maxQuantity) } : l)));
  },
  clear() {
    write(EMPTY);
  },
};
