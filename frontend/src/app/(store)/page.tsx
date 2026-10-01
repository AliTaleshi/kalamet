import { Percent, Sparkles, Truck } from "lucide-react";
import Link from "next/link";
import { ProductScroller } from "@/components/store/product-scroller";
import { apiGet } from "@/lib/api/server";
import type { CategoryNode, Page, ProductSummary } from "@/lib/api/types";

export default async function HomePage() {
  const [offers, newest, bestselling, categories] = await Promise.all([
    apiGet<Page<ProductSummary>>("products?offers=true&inStock=true&sort=biggest-discount&size=12"),
    apiGet<Page<ProductSummary>>("products?sort=newest&size=12"),
    apiGet<Page<ProductSummary>>("products?sort=bestselling&size=12"),
    apiGet<CategoryNode[]>("categories"),
  ]);
  return (
    <div className="flex flex-col gap-8">
      <Hero />
      <ProductScroller title="شگفت‌انگیزها" href="/search?offers=true" products={offers.items} highlight />
      <section>
        <h2 className="mb-4 text-center text-lg font-black text-neutral-800">خرید بر اساس دسته‌بندی</h2>
        <ul className="flex flex-wrap justify-center gap-4">
          {categories.flatMap((c) => [c, ...c.children]).map((category) => (
            <li key={category.id}>
              <Link
                href={`/category/${category.slug}`}
                className="flex h-24 w-32 flex-col items-center justify-center gap-2 rounded-2xl border border-neutral-200 bg-white text-sm text-neutral-700 transition hover:border-brand-300 hover:text-brand-700"
              >
                <span className="flex size-10 items-center justify-center rounded-full bg-brand-50 font-black text-brand-600">
                  {category.name.charAt(0)}
                </span>
                {category.name}
              </Link>
            </li>
          ))}
        </ul>
      </section>
      <ProductScroller title="پرفروش‌ترین کالاها" href="/search?sort=bestselling" products={bestselling.items} />
      <ProductScroller title="جدیدترین کالاها" href="/search?sort=newest" products={newest.items} />
    </div>
  );
}

function Hero() {
  return (
    <section className="grid gap-4 md:grid-cols-3">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-l from-brand-700 via-brand-600 to-brand-500 p-8 text-white md:col-span-2">
        <span className="inline-flex items-center gap-1 rounded-full bg-white/15 px-3 py-1 text-xs">
          <Sparkles className="size-4 text-accent-300" />
          حراج پاییزه
        </span>
        <h1 className="mt-4 text-3xl font-black leading-tight md:text-4xl">
          تا <span className="text-accent-300">۴۰٪</span> تخفیف
          <br />
          روی کالاهای منتخب
        </h1>
        <p className="mt-3 max-w-md text-sm text-brand-50">پیشنهادهای شگفت‌انگیز هر روز تازه می‌شوند؛ تا تمام نشده‌اند بخرید.</p>
        <Link href="/search?offers=true" className="mt-6 inline-flex h-11 items-center rounded-xl bg-accent-400 px-6 font-bold text-neutral-900 hover:bg-accent-300">
          مشاهده پیشنهادها
        </Link>
        <span className="absolute -bottom-16 -left-10 size-56 rounded-full bg-white/10" />
        <span className="absolute -top-10 left-32 size-24 rounded-full bg-accent-400/30" />
      </div>
      <div className="grid gap-4">
        <PromoTile icon={Truck} title="ارسال رایگان" text="برای سفارش‌های بالای ۱ میلیون تومان" />
        <PromoTile icon={Percent} title="تخفیف ویژه اعضا" text="با ورود به حساب، قیمت‌های ویژه را ببینید" />
      </div>
    </section>
  );
}

function PromoTile({ icon: Icon, title, text }: { icon: typeof Truck; title: string; text: string }) {
  return (
    <div className="flex items-center gap-4 rounded-3xl border border-accent-200 bg-accent-50 p-6">
      <span className="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-accent-400 text-neutral-900">
        <Icon className="size-7" />
      </span>
      <div>
        <p className="font-black text-neutral-800">{title}</p>
        <p className="mt-1 text-sm text-neutral-600">{text}</p>
      </div>
    </div>
  );
}
