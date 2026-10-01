"use client";

import { ChevronLeft, Menu, Percent } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import type { CategoryNode } from "@/lib/api/types";

/** "دسته‌بندی کالاها" mega menu plus quick links, under the header. */
export function CategoryNav({ categories }: { categories: CategoryNode[] }) {
  const pathname = usePathname();
  // Remembering where the menu was opened closes it on navigation without an effect.
  const [openOn, setOpenOn] = useState<string | null>(null);
  const open = openOn === pathname;
  const setOpen = (value: boolean | ((open: boolean) => boolean)) =>
    setOpenOn((typeof value === "function" ? value(open) : value) ? pathname : null);
  const [active, setActive] = useState(0);
  const current = categories[active];

  return (
    <nav className="relative hidden border-t border-neutral-100 md:block" onMouseLeave={() => setOpen(false)}>
      <div className="mx-auto flex max-w-7xl items-center gap-6 px-4 text-sm">
        <button
          type="button"
          aria-expanded={open}
          onMouseEnter={() => setOpen(true)}
          onClick={() => setOpen((v) => !v)}
          className="flex h-11 items-center gap-2 font-bold text-neutral-800"
        >
          <Menu className="size-5" />
          دسته‌بندی کالاها
        </button>
        <span className="h-5 w-px bg-neutral-200" />
        <Link href="/search?offers=true" className="flex items-center gap-1 text-neutral-600 hover:text-brand-700">
          <Percent className="size-4 text-accent-600" />
          شگفت‌انگیزها
        </Link>
        <Link href="/search?sort=bestselling" className="text-neutral-600 hover:text-brand-700">پرفروش‌ترین‌ها</Link>
        <Link href="/search?sort=newest" className="text-neutral-600 hover:text-brand-700">جدیدترین‌ها</Link>
      </div>
      {open && current && (
        <div className="absolute inset-x-0 top-full z-20 border-t border-neutral-100 bg-white shadow-xl">
          <div className="mx-auto flex max-w-7xl px-4">
            <ul className="w-56 border-l border-neutral-100 py-2">
              {categories.map((category, index) => (
                <li key={category.id}>
                  <Link
                    href={`/category/${category.slug}`}
                    onMouseEnter={() => setActive(index)}
                    className={`flex items-center justify-between px-4 py-3 text-sm ${index === active ? "bg-brand-50 font-bold text-brand-700" : "text-neutral-700"}`}
                  >
                    {category.name}
                    <ChevronLeft className="size-4" />
                  </Link>
                </li>
              ))}
            </ul>
            <div className="flex-1 p-6">
              <Link href={`/category/${current.slug}`} className="text-sm font-bold text-brand-700">
                همه کالاهای {current.name}
              </Link>
              <ul className="mt-4 grid grid-cols-3 gap-x-8 gap-y-3">
                {current.children.map((child) => (
                  <li key={child.id}>
                    <Link href={`/category/${child.slug}`} className="text-sm text-neutral-600 hover:text-brand-700">
                      {child.name}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}
    </nav>
  );
}
