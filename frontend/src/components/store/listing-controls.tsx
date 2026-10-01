"use client";

import { clsx } from "clsx";
import { ArrowUpDown, SlidersHorizontal, X } from "lucide-react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState, useTransition } from "react";
import type { Brand } from "@/lib/api/types";
import { faNumber } from "@/lib/format";
import { SORTS, type ListingParams } from "@/lib/listing";
import { Pagination } from "@/components/ui/pagination";
import { Spinner } from "@/components/ui/spinner";

/** Updates the listing URL; the server page re-renders with the new results. */
function useListingNavigation() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const [pending, startTransition] = useTransition();
  const update = (changes: Record<string, string | string[] | null>, isPageChange = false) => {
    const next = new URLSearchParams(params.toString());
    for (const [key, value] of Object.entries(changes)) {
      next.delete(key);
      if (Array.isArray(value)) value.forEach((v) => next.append(key, v));
      else if (value !== null && value !== "") next.set(key, value);
    }
    // A new filter or sort starts again from the first page.
    if (!isPageChange) next.delete("page");
    const query = next.toString();
    startTransition(() => router.push(query ? `${pathname}?${query}` : pathname, { scroll: isPageChange }));
  };
  return { update, pending };
}

export function SortBar({ params, total }: { params: ListingParams; total: number }) {
  const { update, pending } = useListingNavigation();
  return (
    <div className="flex items-center gap-4 border-b border-neutral-100 px-4 py-3 text-sm">
      <span className="hidden items-center gap-1 font-bold text-neutral-700 sm:flex">
        <ArrowUpDown className="size-4" />
        مرتب‌سازی:
      </span>
      <div className="scrollbar-none flex flex-1 gap-4 overflow-x-auto">
        {SORTS.map((sort) => (
          <button
            key={sort.value}
            type="button"
            aria-pressed={params.sort === sort.value}
            onClick={() => update({ sort: sort.value })}
            className={clsx("shrink-0 py-1", params.sort === sort.value ? "font-bold text-brand-700" : "text-neutral-500 hover:text-neutral-800")}
          >
            {sort.label}
          </button>
        ))}
      </div>
      {pending ? <Spinner className="size-4 text-brand-600" /> : <span className="shrink-0 text-neutral-500">{faNumber(total)} کالا</span>}
    </div>
  );
}

export function ListingPagination({ page, totalPages }: { page: number; totalPages: number }) {
  const { update } = useListingNavigation();
  return <Pagination page={page} totalPages={totalPages} onChange={(p) => update({ page: p > 0 ? String(p + 1) : null }, true)} />;
}

export function Filters({ params, brands }: { params: ListingParams; brands: Brand[] }) {
  const { update: navigate } = useListingNavigation();
  const [mobileOpen, setMobileOpen] = useState(false);
  // The checkboxes and switches show the new value immediately instead of waiting for the server
  // to re-render the page (this component remounts when the URL changes, see ProductListing).
  const [selected, setSelected] = useState({ brand: params.brand, inStock: params.inStock, offers: params.offers });
  const update = (changes: Record<string, string | string[] | null>) => {
    setSelected((s) => ({
      brand: "brand" in changes ? ((changes.brand as string[] | null) ?? []) : s.brand,
      inStock: "inStock" in changes ? changes.inStock === "true" : s.inStock,
      offers: "offers" in changes ? changes.offers === "true" : s.offers,
    }));
    navigate(changes);
  };
  const [min, setMin] = useState(params.minPrice?.toString() ?? "");
  const [max, setMax] = useState(params.maxPrice?.toString() ?? "");
  const active = selected.brand.length + (params.minPrice !== undefined || params.maxPrice !== undefined ? 1 : 0)
    + (selected.inStock ? 1 : 0) + (selected.offers ? 1 : 0);

  const digits = (value: string) =>
    value.replace(/[۰-۹]/g, (d) => String(d.charCodeAt(0) - 0x06f0)).replace(/\D/g, "") || null;

  const panel = (
    <div className="flex flex-col divide-y divide-neutral-100">
      <div className="flex items-center justify-between pb-3">
        <h2 className="font-bold text-neutral-800">فیلترها</h2>
        {active > 0 && (
          <button
            type="button"
            className="text-sm text-brand-700"
            onClick={() => {
              setMin("");
              setMax("");
              update({ brand: null, minPrice: null, maxPrice: null, inStock: null, offers: null });
            }}
          >
            حذف فیلترها
          </button>
        )}
      </div>
      <Toggle label="فقط کالاهای موجود" checked={selected.inStock} onChange={(v) => update({ inStock: v ? "true" : null })} />
      <Toggle label="فقط شگفت‌انگیزها" checked={selected.offers} onChange={(v) => update({ offers: v ? "true" : null })} />
      {brands.length > 0 && (
        <fieldset className="py-4">
          <legend className="mb-3 text-sm font-bold text-neutral-700">برند</legend>
          <ul className="flex max-h-56 flex-col gap-2 overflow-y-auto">
            {brands.map((brand) => {
              const checked = selected.brand.includes(brand.slug);
              return (
                <li key={brand.id}>
                  <label className="flex cursor-pointer items-center justify-between gap-2 text-sm text-neutral-700">
                    <span className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        checked={checked}
                        className="size-4 accent-brand-600"
                        onChange={() =>
                          update({ brand: checked ? selected.brand.filter((b) => b !== brand.slug) : [...selected.brand, brand.slug] })
                        }
                      />
                      {brand.name}
                    </span>
                    {brand.nameEn && <span className="text-xs text-neutral-400">{brand.nameEn}</span>}
                  </label>
                </li>
              );
            })}
          </ul>
        </fieldset>
      )}
      <form
        className="flex flex-col gap-3 py-4"
        onSubmit={(event) => {
          event.preventDefault();
          update({ minPrice: digits(min), maxPrice: digits(max) });
          setMobileOpen(false);
        }}
      >
        <span className="text-sm font-bold text-neutral-700">محدوده قیمت (تومان)</span>
        <div className="flex items-center gap-2">
          <input
            inputMode="numeric"
            value={min}
            onChange={(e) => setMin(e.target.value)}
            placeholder="از"
            aria-label="کمترین قیمت (تومان)"
            className="h-10 w-full rounded-lg border border-neutral-300 px-3 text-sm"
          />
          <input
            inputMode="numeric"
            value={max}
            onChange={(e) => setMax(e.target.value)}
            placeholder="تا"
            aria-label="بیشترین قیمت (تومان)"
            className="h-10 w-full rounded-lg border border-neutral-300 px-3 text-sm"
          />
        </div>
        <button type="submit" className="h-10 rounded-lg bg-brand-600 text-sm font-medium text-white hover:bg-brand-700">
          اعمال محدوده قیمت
        </button>
      </form>
    </div>
  );

  return (
    <>
      <button
        type="button"
        onClick={() => setMobileOpen(true)}
        className="flex h-10 w-fit items-center gap-2 rounded-lg border border-neutral-200 bg-white px-3 text-sm lg:hidden"
      >
        <SlidersHorizontal className="size-4" />
        فیلترها
        {active > 0 && <span className="rounded-full bg-brand-600 px-1.5 text-xs text-white">{faNumber(active)}</span>}
      </button>
      <aside className="hidden h-fit rounded-2xl border border-neutral-200 bg-white p-4 lg:sticky lg:top-36 lg:block">{panel}</aside>
      {mobileOpen && (
        <div className="fixed inset-0 z-40 bg-black/40 lg:hidden" onClick={() => setMobileOpen(false)}>
          <div
            role="dialog"
            aria-label="فیلترها"
            className="absolute inset-y-0 right-0 w-80 max-w-full overflow-y-auto bg-white p-4"
            onClick={(e) => e.stopPropagation()}
          >
            <button type="button" aria-label="بستن" onClick={() => setMobileOpen(false)} className="mb-2 rounded-lg p-1 hover:bg-neutral-100">
              <X className="size-5" />
            </button>
            {panel}
          </div>
        </div>
      )}
    </>
  );
}

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="relative flex cursor-pointer items-center justify-between py-4 text-sm font-medium text-neutral-700">
      {label}
      <input type="checkbox" role="switch" checked={checked} onChange={(e) => onChange(e.target.checked)} className="peer sr-only" />
      <span className="relative h-6 w-11 rounded-full bg-neutral-300 transition-colors after:absolute after:top-0.5 after:right-0.5 after:size-5 after:rounded-full after:bg-white after:transition-transform peer-checked:bg-brand-600 peer-checked:after:-translate-x-5 peer-focus-visible:outline-2 peer-focus-visible:outline-brand-500" />
    </label>
  );
}
