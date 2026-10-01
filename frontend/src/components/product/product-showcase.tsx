"use client";

import { useMemo, useState } from "react";
import type { ProductDetail } from "@/lib/api/types";
import { findVariant, imageIndexFor } from "@/lib/product";
import { BuyBox } from "./buy-box";
import { Gallery } from "./gallery";

/** Gallery, product facts and buy box; picking a colour shows that colour's picture. */
export function ProductShowcase({ product, children }: { product: ProductDetail; children: React.ReactNode }) {
  const initial = product.variants.find((v) => v.inStock) ?? product.variants[0];
  const [selection, setSelection] = useState<Record<string, string>>(initial?.attributes ?? {});
  const variant = useMemo(() => findVariant(product.variants, selection), [product.variants, selection]);
  const imageIndex = Math.max(imageIndexFor(product, variant), 0);

  return (
    <article className="grid gap-8 rounded-2xl border border-neutral-200 bg-white p-5 lg:grid-cols-[minmax(0,26rem)_1fr_20rem]">
      {/* key: jump to the chosen colour's picture; thumbnails stay clickable in between */}
      <Gallery key={imageIndex} images={product.images} name={product.name} initialIndex={imageIndex} />
      <div className="flex flex-col gap-4">{children}</div>
      <BuyBox product={product} selection={selection} onSelect={setSelection} variant={variant} />
    </article>
  );
}
