"use client";

import { clsx } from "clsx";
import { Check, ShieldCheck, ShoppingCart, Store, Truck } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import type { ProductDetail, Variant } from "@/lib/api/types";
import { attributeLabel, faNumber } from "@/lib/format";
import { useCart } from "@/lib/hooks/cart";
import { Button } from "@/components/ui/button";
import { Countdown } from "@/components/ui/countdown";
import { Price } from "@/components/ui/price";
import { QuantityStepper } from "@/components/ui/quantity";
import { useToast } from "@/components/ui/toast";

/** Option picker (colour, size...) plus price and add-to-cart for the chosen variant. */
export function BuyBox({ product }: { product: ProductDetail }) {
  const initial = product.variants.find((v) => v.inStock) ?? product.variants[0];
  const [selection, setSelection] = useState<Record<string, string>>(initial?.attributes ?? {});
  const variant = useMemo(() => findVariant(product.variants, selection), [product.variants, selection]);

  const choose = (key: string, value: string) => {
    const wanted = { ...selection, [key]: value };
    // Keep the other choices when that combination exists; otherwise jump to a variant that has this value.
    const exact = findVariant(product.variants, wanted);
    const fallback = product.variants.find((v) => v.attributes[key] === value && v.inStock)
      ?? product.variants.find((v) => v.attributes[key] === value);
    setSelection(exact ? wanted : fallback?.attributes ?? wanted);
  };

  return (
    <div className="flex flex-col gap-5">
      {product.options.map((option) => (
        <fieldset key={option.key}>
          <legend className="mb-2 text-sm text-neutral-600">
            {attributeLabel(option.key)}: <span className="font-bold text-neutral-800">{selection[option.key]}</span>
          </legend>
          <div className="flex flex-wrap gap-2">
            {option.values.map((value) => {
              const selected = selection[option.key] === value;
              const candidate = findVariant(product.variants, { ...selection, [option.key]: value });
              const unavailable = !candidate || !candidate.inStock;
              return (
                <button
                  key={value}
                  type="button"
                  aria-pressed={selected}
                  onClick={() => choose(option.key, value)}
                  className={clsx(
                    "flex h-10 min-w-12 items-center gap-1 rounded-full border px-4 text-sm transition-colors",
                    selected ? "border-brand-600 bg-brand-50 font-bold text-brand-700" : "border-neutral-300 text-neutral-700 hover:border-neutral-400",
                    unavailable && !selected && "text-neutral-400 line-through",
                  )}
                >
                  {selected && <Check className="size-4" />}
                  {value}
                </button>
              );
            })}
          </div>
        </fieldset>
      ))}
      <PurchasePanel product={product} variant={variant} />
    </div>
  );
}

function PurchasePanel({ product, variant }: { product: ProductDetail; variant: Variant | null }) {
  const cart = useCart();
  const notify = useToast();
  const [adding, setAdding] = useState(false);
  const inCart = variant ? cart.lines.find((l) => l.variantId === variant.id) : undefined;

  const add = async () => {
    if (!variant) return;
    setAdding(true);
    const ok = await cart.add({
      variantId: variant.id,
      productSlug: product.slug,
      productName: product.name,
      imageUrl: product.images.find((i) => i.variantId === variant.id)?.url ?? product.images[0]?.url ?? null,
      attributes: variant.attributes,
      unitPrice: variant.price,
      originalPrice: variant.originalPrice,
      maxQuantity: Math.min(variant.remaining ?? 10, 10),
    });
    setAdding(false);
    if (ok) notify("به سبد خرید اضافه شد");
  };

  return (
    <div className="rounded-2xl border border-neutral-200 bg-neutral-50 p-5">
      <ul className="mb-4 space-y-3 border-b border-neutral-200 pb-4 text-sm text-neutral-700">
        <li className="flex items-center gap-2"><Store className="size-5 text-brand-600" />فروش و ارسال توسط کالامت</li>
        <li className="flex items-center gap-2"><ShieldCheck className="size-5 text-brand-600" />گارانتی اصالت و سلامت فیزیکی کالا</li>
        <li className="flex items-center gap-2"><Truck className="size-5 text-brand-600" />ارسال رایگان برای خرید بالای ۱ میلیون تومان</li>
      </ul>
      {!variant || !variant.inStock ? (
        <div className="py-2 text-center">
          <p className="font-bold text-neutral-500">ناموجود</p>
          <p className="mt-1 text-xs text-neutral-400">این کالا فعلاً موجود نیست؛ تنوع دیگری را انتخاب کنید.</p>
        </div>
      ) : (
        <>
          {variant.offerEndsAt && (
            <div className="mb-3 flex items-center justify-between rounded-lg bg-red-50 px-3 py-2 text-danger-600">
              <span className="text-sm font-bold">پیشنهاد شگفت‌انگیز</span>
              <Countdown endsAt={variant.offerEndsAt} />
            </div>
          )}
          <Price price={variant.price} originalPrice={variant.originalPrice} discountPercent={variant.discountPercent} size="lg" />
          {variant.remaining !== null && (
            <p className="mt-2 text-end text-xs font-bold text-danger-600">تنها {faNumber(variant.remaining)} عدد در انبار باقی مانده</p>
          )}
          <div className="mt-4">
            {inCart ? (
              <div className="flex items-center justify-between gap-3">
                <QuantityStepper
                  value={inCart.quantity}
                  max={inCart.maxQuantity}
                  busy={cart.pending === variant.id}
                  onChange={(q) => cart.setQuantity(variant.id, q)}
                />
                <Link href="/cart" className="text-sm font-medium text-brand-700">مشاهده سبد خرید</Link>
              </div>
            ) : (
              <Button size="lg" className="w-full" loading={adding} onClick={add}>
                <ShoppingCart className="size-5" />
                افزودن به سبد خرید
              </Button>
            )}
          </div>
        </>
      )}
    </div>
  );
}

function findVariant(variants: Variant[], selection: Record<string, string>): Variant | null {
  return variants.find((v) => Object.entries(v.attributes).every(([key, value]) => selection[key] === value)) ?? null;
}
