import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { BuyBox } from "@/components/product/buy-box";
import { Gallery } from "@/components/product/gallery";
import { Reviews } from "@/components/product/reviews";
import { Breadcrumbs } from "@/components/store/breadcrumbs";
import { RatingValue } from "@/components/ui/rating";
import { apiGet, apiGetOrNull } from "@/lib/api/server";
import type { Page, ProductDetail, Review } from "@/lib/api/types";

type Props = { params: Promise<{ slug: string }> };

function getProduct(slug: string) {
  return apiGetOrNull<ProductDetail>(`products/${encodeURIComponent(slug)}`);
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const product = await getProduct((await params).slug);
  if (!product) return { title: "کالا پیدا نشد" };
  return {
    title: product.name,
    description: product.description?.slice(0, 160) ?? undefined,
    openGraph: { title: product.name, images: product.images.slice(0, 1).map((i) => i.url) },
  };
}

export default async function ProductPage({ params }: Props) {
  const slug = (await params).slug;
  const product = await getProduct(slug);
  if (!product) notFound();
  const reviews = await apiGet<Page<Review>>(`products/${encodeURIComponent(slug)}/reviews?size=10`);

  return (
    <div className="flex flex-col gap-6">
      <Breadcrumbs items={product.breadcrumbs.map((c) => ({ label: c.name, href: `/category/${c.slug}` }))} />
      <article className="grid gap-8 rounded-2xl border border-neutral-200 bg-white p-5 lg:grid-cols-[minmax(0,26rem)_1fr_20rem]">
        <Gallery images={product.images} name={product.name} />
        <div className="flex flex-col gap-4">
          {product.brand && (
            <Link href={`/search?brand=${product.brand.slug}`} className="text-sm font-medium text-brand-700">
              {product.brand.name}
            </Link>
          )}
          <h1 className="text-xl font-black leading-9 text-neutral-800">{product.name}</h1>
          {product.nameEn && <p className="text-sm text-neutral-400" dir="ltr">{product.nameEn}</p>}
          <RatingValue rating={product.rating.average} count={product.rating.count} className="text-sm" />
          {product.specGroups[0] && (
            <div>
              <h2 className="mb-2 font-bold text-neutral-800">ویژگی‌ها</h2>
              <ul className="space-y-1.5 text-sm text-neutral-600">
                {product.specGroups[0].specs.slice(0, 4).map((spec) => (
                  <li key={spec.name}>
                    <span className="text-neutral-400">{spec.name}:</span> {spec.value}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
        <BuyBox product={product} />
      </article>

      {product.description && (
        <section className="rounded-2xl border border-neutral-200 bg-white p-5">
          <h2 className="mb-3 text-lg font-black text-neutral-800">معرفی</h2>
          <p className="whitespace-pre-line text-sm leading-8 text-neutral-700">{product.description}</p>
        </section>
      )}

      {product.specGroups.length > 0 && (
        <section className="rounded-2xl border border-neutral-200 bg-white p-5">
          <h2 className="mb-3 text-lg font-black text-neutral-800">مشخصات</h2>
          {product.specGroups.map((group) => (
            <div key={group.name} className="mb-4 last:mb-0">
              <h3 className="mb-2 text-sm font-bold text-neutral-600">{group.name}</h3>
              <dl className="divide-y divide-neutral-100">
                {group.specs.map((spec) => (
                  <div key={spec.name} className="grid grid-cols-[10rem_1fr] gap-4 py-3 text-sm">
                    <dt className="text-neutral-500">{spec.name}</dt>
                    <dd className="text-neutral-800">{spec.value}</dd>
                  </div>
                ))}
              </dl>
            </div>
          ))}
        </section>
      )}

      <section className="rounded-2xl border border-neutral-200 bg-white p-5">
        <h2 className="mb-5 text-lg font-black text-neutral-800">امتیاز و دیدگاه کاربران</h2>
        <Reviews slug={product.slug} summary={product.rating} firstPage={reviews} />
      </section>
    </div>
  );
}
