import type { ProductSort } from "@/lib/api/types";

/** Listing filters as they appear in page URLs (/search, /category/...). Prices are Toman. */
export type ListingParams = {
  q?: string;
  brand: string[];
  minPrice?: number;
  maxPrice?: number;
  inStock: boolean;
  offers: boolean;
  sort: ProductSort;
  page: number;
};

export const SORTS: { value: ProductSort; label: string }[] = [
  { value: "newest", label: "جدیدترین" },
  { value: "bestselling", label: "پرفروش‌ترین" },
  { value: "cheapest", label: "ارزان‌ترین" },
  { value: "most-expensive", label: "گران‌ترین" },
  { value: "biggest-discount", label: "بیشترین تخفیف" },
  { value: "top-rated", label: "محبوب‌ترین" },
];

type Search = Record<string, string | string[] | undefined>;

function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function amount(value: string | undefined): number | undefined {
  const n = Number(value);
  return value && Number.isFinite(n) && n >= 0 ? Math.floor(n) : undefined;
}

export function parseListing(search: Search): ListingParams {
  const sort = first(search.sort);
  const page = Number(first(search.page));
  const brand = search.brand;
  return {
    q: first(search.q)?.trim() || undefined,
    brand: (Array.isArray(brand) ? brand : brand ? [brand] : []).filter(Boolean),
    minPrice: amount(first(search.minPrice)),
    maxPrice: amount(first(search.maxPrice)),
    inStock: first(search.inStock) === "true",
    offers: first(search.offers) === "true",
    sort: SORTS.some((s) => s.value === sort) ? (sort as ProductSort) : "newest",
    // The URL shows 1-based pages; the API is 0-based.
    page: Number.isInteger(page) && page > 1 ? page - 1 : 0,
  };
}

/** API query string for a listing: prices converted from Toman to Rial, page zero-based. */
export function apiQuery(params: ListingParams, category?: string): string {
  const search = new URLSearchParams();
  if (params.q) search.set("q", params.q.slice(0, 200));
  if (category) search.set("category", category);
  params.brand.forEach((b) => search.append("brand", b));
  if (params.minPrice !== undefined) search.set("minPrice", String(params.minPrice * 10));
  if (params.maxPrice !== undefined) search.set("maxPrice", String(params.maxPrice * 10));
  if (params.inStock) search.set("inStock", "true");
  if (params.offers) search.set("offers", "true");
  search.set("sort", params.sort);
  search.set("page", String(params.page));
  search.set("size", "24");
  return search.toString();
}
