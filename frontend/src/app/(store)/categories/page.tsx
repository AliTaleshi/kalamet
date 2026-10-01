import { ChevronLeft } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { apiGet } from "@/lib/api/server";
import type { CategoryNode } from "@/lib/api/types";

export const metadata: Metadata = { title: "دسته‌بندی کالاها" };

/** All categories on one page; the mobile header links here instead of the desktop mega menu. */
export default async function CategoriesPage() {
  const categories = await apiGet<CategoryNode[]>("categories");
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-black text-neutral-800">دسته‌بندی کالاها</h1>
      {categories.map((category) => (
        <section key={category.id} className="rounded-2xl border border-neutral-200 bg-white">
          <Link href={`/category/${category.slug}`} className="flex items-center justify-between border-b border-neutral-100 px-5 py-4 font-bold text-neutral-800">
            {category.name}
            <span className="flex items-center gap-1 text-sm font-normal text-brand-700">همه <ChevronLeft className="size-4" /></span>
          </Link>
          <ul className="grid grid-cols-2 gap-2 p-4 sm:grid-cols-4">
            {category.children.map((child) => (
              <li key={child.id}>
                <Link href={`/category/${child.slug}`} className="block rounded-xl bg-neutral-50 px-4 py-3 text-sm text-neutral-700 hover:bg-brand-50 hover:text-brand-700">
                  {child.name}
                </Link>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
