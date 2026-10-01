import Link from "next/link";
import type { ProductSummary } from "@/lib/api/types";
import { Countdown } from "@/components/ui/countdown";
import { Price } from "@/components/ui/price";
import { ProductImage } from "@/components/ui/product-image";
import { RatingValue } from "@/components/ui/rating";

export function ProductCard({ product, compact = false }: { product: ProductSummary; compact?: boolean }) {
  return (
    <Link
      href={`/product/${product.slug}`}
      // relative: keeps any positioned child inside horizontal scrollers (an escaped one widened the page on phones)
      className="group relative flex h-full flex-col gap-3 bg-white p-4 transition-shadow hover:z-10 hover:shadow-lg"
    >
      {product.offerEndsAt && product.inStock && (
        <div className="flex flex-wrap items-center justify-between gap-1 text-xs font-bold text-danger-600">
          <span>شگفت‌انگیز</span>
          <Countdown endsAt={product.offerEndsAt} className="text-xs" />
        </div>
      )}
      <ProductImage
        src={product.imageUrl}
        alt={product.name}
        className={`mx-auto aspect-square w-full rounded-lg ${product.inStock ? "" : "opacity-50 grayscale"} ${compact ? "max-w-40" : ""}`}
      />
      <h3 className="line-clamp-2 min-h-10 text-sm leading-6 text-neutral-700 group-hover:text-brand-700">{product.name}</h3>
      <div className="mt-auto flex items-end justify-between gap-2">
        <RatingValue rating={product.rating} />
        {product.inStock ? (
          <Price price={product.price} originalPrice={product.originalPrice} discountPercent={product.discountPercent} />
        ) : (
          <span className="text-sm font-bold text-neutral-400">ناموجود</span>
        )}
      </div>
    </Link>
  );
}

export function ProductGrid({ products }: { products: ProductSummary[] }) {
  return (
    // Cell borders instead of a coloured gap, so a short last row leaves white space, not grey.
    <ul className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4">
      {products.map((product) => (
        <li key={product.id} className="border-b border-s border-neutral-100">
          <ProductCard product={product} />
        </li>
      ))}
    </ul>
  );
}
