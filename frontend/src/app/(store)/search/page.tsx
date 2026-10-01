import type { Metadata } from "next";
import { ProductListing } from "@/components/store/product-listing";
import { apiGet } from "@/lib/api/server";
import type { Brand, Page, ProductSummary } from "@/lib/api/types";
import { apiQuery, parseListing } from "@/lib/listing";

type Props = { searchParams: Promise<Record<string, string | string[] | undefined>> };

export async function generateMetadata({ searchParams }: Props): Promise<Metadata> {
  const { q } = parseListing(await searchParams);
  return { title: q ? `جستجوی «${q}»` : "همه کالاها" };
}

export default async function SearchPage({ searchParams }: Props) {
  const listing = parseListing(await searchParams);
  const [result, brands] = await Promise.all([
    apiGet<Page<ProductSummary>>(`products?${apiQuery(listing)}`),
    apiGet<Brand[]>("brands"),
  ]);
  const title = listing.q ? `نتایج جستجو برای «${listing.q}»` : listing.offers ? "پیشنهادهای شگفت‌انگیز" : "همه کالاها";
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-black text-neutral-800">{title}</h1>
      <ProductListing params={listing} result={result} brands={brands} />
    </div>
  );
}
