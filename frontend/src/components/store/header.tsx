import { Menu } from "lucide-react";
import Link from "next/link";
import { Suspense } from "react";
import type { CategoryNode } from "@/lib/api/types";
import { CategoryNav } from "./category-nav";
import { HeaderActions } from "./header-actions";
import { Logo } from "./logo";
import { SearchBox } from "./search-box";

export function Header({ categories }: { categories: CategoryNode[] }) {
  return (
    <header className="sticky top-0 z-30 bg-white shadow-sm">
      <div className="bg-brand-700 py-1.5 text-center text-xs text-brand-50">
        ارسال رایگان برای خریدهای بالای ۱ میلیون تومان
      </div>
      <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-6 gap-y-3 px-4 py-3 md:flex-nowrap">
        <Link href="/categories" aria-label="دسته‌بندی کالاها" className="-me-4 rounded-lg p-2 text-neutral-700 hover:bg-neutral-100 md:hidden">
          <Menu className="size-6" />
        </Link>
        <Logo />
        <div className="order-last w-full md:order-none md:max-w-xl md:flex-1">
          <Suspense>
            <SearchBox />
          </Suspense>
        </div>
        <div className="ms-auto">
          <HeaderActions />
        </div>
      </div>
      <CategoryNav categories={categories} />
    </header>
  );
}
