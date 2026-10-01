import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import type { ProductSummary } from "@/lib/api/types";
import { ProductCard } from "./product-card";

/** A horizontal row of products with a "see all" link, used on the home page. */
export function ProductScroller({ title, href, products, highlight = false }: {
  title: string;
  href: string;
  products: ProductSummary[];
  highlight?: boolean;
}) {
  if (!products.length) return null;
  return (
    <section className={highlight ? "rounded-2xl bg-gradient-to-l from-brand-700 to-brand-500 p-4" : "rounded-2xl border border-neutral-200 bg-white p-4"}>
      <div className="mb-3 flex items-center justify-between">
        <h2 className={`text-lg font-black ${highlight ? "text-white" : "text-neutral-800"}`}>{title}</h2>
        <Link href={href} className={`flex items-center gap-1 text-sm ${highlight ? "text-white" : "text-brand-700"}`}>
          مشاهده همه
          <ChevronLeft className="size-4" />
        </Link>
      </div>
      <ul className="scrollbar-none -mx-1 flex snap-x gap-2 overflow-x-auto px-1">
        {products.map((product) => (
          <li key={product.id} className="w-44 shrink-0 snap-start overflow-hidden rounded-xl sm:w-52">
            <ProductCard product={product} compact />
          </li>
        ))}
      </ul>
    </section>
  );
}
