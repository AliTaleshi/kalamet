"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/error";
import type { Cart } from "@/lib/api/types";
import { useToast } from "@/components/ui/toast";
import { guestCart, useGuestCart, type GuestLine } from "./guest-cart";
import { useMe } from "./session";

export const CART_KEY = ["cart"] as const;

export type CartLineView = {
  variantId: number;
  productSlug: string;
  productName: string;
  imageUrl: string | null;
  attributes: Record<string, string>;
  quantity: number;
  unitPrice: number;
  originalPrice: number | null;
  lineTotal: number;
  maxQuantity: number;
  issue: Cart["lines"][number]["issue"];
};

export type CartState = {
  ready: boolean;
  /** The signed-in cart could not be loaded. */
  failed: boolean;
  retry: () => void;
  guest: boolean;
  lines: CartLineView[];
  itemCount: number;
  /** Totals from the API; null for a guest cart (prices are confirmed after sign-in). */
  totals: Pick<Cart, "itemsTotal" | "discountTotal" | "shippingFee" | "freeShippingRemaining" | "payable" | "hasIssues"> | null;
  guestTotal: number;
  add: (line: Omit<GuestLine, "quantity">, quantity?: number) => Promise<boolean>;
  setQuantity: (variantId: number, quantity: number) => Promise<void>;
  pending: number | null;
};

/** One cart API for the whole UI, whether the visitor is signed in or not. */
export function useCart(): CartState {
  const me = useMe();
  const signedIn = !!me.data;
  const client = useQueryClient();
  const notify = useToast();
  const guestLines = useGuestCart();

  const server = useQuery({
    queryKey: CART_KEY,
    queryFn: () => api<Cart>("cart"),
    enabled: signedIn,
  });

  const mutation = useMutation({
    mutationFn: ({ variantId, quantity, mode }: { variantId: number; quantity: number; mode: "add" | "set" }) =>
      mode === "add"
        ? api<Cart>("cart/items", { method: "POST", body: { variantId, quantity } })
        : api<Cart>(`cart/items/${variantId}`, { method: "PUT", body: { quantity } }),
    onSuccess: (cart) => client.setQueryData(CART_KEY, cart),
    onError: (error) => notify(errorMessage(error), "error"),
  });

  const add = useCallback<CartState["add"]>(
    async (line, quantity = 1) => {
      if (!signedIn) {
        guestCart.add(line, quantity);
        return true;
      }
      try {
        await mutation.mutateAsync({ variantId: line.variantId, quantity, mode: "add" });
        return true;
      } catch {
        return false;
      }
    },
    [signedIn, mutation],
  );

  const setQuantity = useCallback<CartState["setQuantity"]>(
    async (variantId, quantity) => {
      if (!signedIn) {
        guestCart.setQuantity(variantId, quantity);
        return;
      }
      await mutation.mutateAsync({ variantId, quantity: Math.max(quantity, 0), mode: "set" }).catch(() => undefined);
    },
    [signedIn, mutation],
  );

  if (signedIn) {
    const cart = server.data;
    return {
      ready: !!cart || server.isError,
      failed: server.isError && !cart,
      retry: () => void server.refetch(),
      guest: false,
      lines: cart?.lines ?? [],
      itemCount: cart?.lines.reduce((sum, l) => sum + l.quantity, 0) ?? 0,
      totals: cart ?? null,
      guestTotal: 0,
      add,
      setQuantity,
      pending: mutation.isPending ? mutation.variables?.variantId ?? null : null,
    };
  }
  const lines: CartLineView[] = guestLines.map((l) => ({ ...l, lineTotal: l.unitPrice * l.quantity, issue: null }));
  return {
    ready: !me.isLoading,
    failed: false,
    retry: () => {},
    guest: true,
    lines,
    itemCount: lines.reduce((sum, l) => sum + l.quantity, 0),
    totals: null,
    guestTotal: lines.reduce((sum, l) => sum + l.lineTotal, 0),
    add,
    setQuantity,
    pending: null,
  };
}

/** After sign-in: moves the guest cart into the account (capped to stock) and empties it. */
export async function mergeGuestCart(): Promise<void> {
  const lines = guestCart.lines();
  if (!lines.length) return;
  await api<Cart>("cart/merge", {
    method: "POST",
    body: { items: lines.map((l) => ({ variantId: l.variantId, quantity: l.quantity })) },
  });
  guestCart.clear();
}
