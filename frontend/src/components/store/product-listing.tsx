import { SearchX } from "lucide-react";
import { Suspense } from "react";
import type { Brand, Page, ProductSummary } from "@/lib/api/types";
import type { ListingParams } from "@/lib/listing";
import { EmptyState } from "@/components/ui/empty-state";
import { Filters, ListingPagination, SortBar } from "./listing-controls";
import { ProductGrid } from "./product-card";

/** Filters sidebar, sort bar and results grid, shared by the category and search pages. */
export function ProductListing({ params, result, brands }: { params: ListingParams; result: Page<ProductSummary>; brands: Brand[] }) {
  return (
    <div className="grid gap-4 lg:grid-cols-[16rem_1fr]">
      <Suspense>
        {/* key: start from the URL's values whenever it changes (back button, "remove filters") */}
        <Filters key={JSON.stringify([params.brand, params.minPrice, params.maxPrice, params.inStock, params.offers])} params={params} brands={brands} />
      </Suspense>
      <div className="min-w-0">
        <div className="overflow-hidden rounded-2xl border border-neutral-200 bg-white">
          <Suspense>
            <SortBar params={params} total={result.totalItems} />
          </Suspense>
          {result.items.length ? (
            <div className="p-px">
              <ProductGrid products={result.items} />
            </div>
          ) : (
            <EmptyState icon={SearchX} title="کالایی پیدا نشد" description="فیلترها را تغییر دهید یا عبارت دیگری را جستجو کنید." />
          )}
        </div>
        <Suspense>
          <ListingPagination page={result.page} totalPages={result.totalPages} />
        </Suspense>
      </div>
    </div>
  );
}
