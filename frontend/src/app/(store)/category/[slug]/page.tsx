import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Breadcrumbs } from "@/components/store/breadcrumbs";
import { ProductListing } from "@/components/store/product-listing";
import { apiGet, apiGetOrNull } from "@/lib/api/server";
import type { Brand, CategoryDetail, Page, ProductSummary } from "@/lib/api/types";
import { apiQuery, parseListing } from "@/lib/listing";

type Props = {
  params: Promise<{ slug: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

function getCategory(slug: string) {
  return apiGetOrNull<CategoryDetail>(`categories/${encodeURIComponent(slug)}`);
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const category = await getCategory((await params).slug);
  return { title: category ? `خرید ${category.name}` : "دسته‌بندی پیدا نشد" };
}

export default async function CategoryPage({ params, searchParams }: Props) {
  const category = await getCategory((await params).slug);
  if (!category) notFound();
  const listing = parseListing(await searchParams);
  const [result, brands] = await Promise.all([
    apiGet<Page<ProductSummary>>(`products?${apiQuery(listing, category.slug)}`),
    apiGet<Brand[]>(`brands?category=${encodeURIComponent(category.slug)}`),
  ]);
  return (
    <div className="flex flex-col gap-4">
      <Breadcrumbs
        items={category.breadcrumbs.map((c) => ({ label: c.name, href: c.slug === category.slug ? undefined : `/category/${c.slug}` }))}
      />
      <h1 className="text-xl font-black text-neutral-800">{category.name}</h1>
      {category.children.length > 0 && (
        <ul className="scrollbar-none flex gap-2 overflow-x-auto">
          {category.children.map((child) => (
            <li key={child.id} className="shrink-0">
              <Link
                href={`/category/${child.slug}`}
                className="block rounded-full border border-neutral-200 bg-white px-4 py-2 text-sm text-neutral-700 hover:border-brand-300 hover:text-brand-700"
              >
                {child.name}
              </Link>
            </li>
          ))}
        </ul>
      )}
      <ProductListing params={listing} result={result} brands={brands} />
    </div>
  );
}
