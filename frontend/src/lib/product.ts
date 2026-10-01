import type { ProductDetail, Variant } from "@/lib/api/types";

/**
 * Index of the picture that shows a variant: one linked to it, else one linked to a variant of
 * the same colour, else -1. Mirrors VariantImages in the API (cart and order lines).
 */
export function imageIndexFor(product: ProductDetail, variant: Variant | null): number {
  if (!variant) return -1;
  const exact = product.images.findIndex((image) => image.variantId === variant.id);
  if (exact >= 0) return exact;
  const color = variant.attributes.color;
  if (!color) return -1;
  return product.images.findIndex((image) => {
    const linked = product.variants.find((v) => v.id === image.variantId);
    return linked?.attributes.color === color;
  });
}

export function findVariant(variants: Variant[], selection: Record<string, string>): Variant | null {
  return variants.find((v) => Object.entries(v.attributes).every(([key, value]) => selection[key] === value)) ?? null;
}
