"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { api, query } from "@/lib/api/client";
import type { AdminProductSummary, Page } from "@/lib/api/types";
import { jalaliDate } from "@/lib/format";
import { useDebounced } from "@/lib/hooks/debounce";
import { Badge } from "@/components/ui/badge";
import { ButtonLink } from "@/components/ui/button";
import { Pagination } from "@/components/ui/pagination";
import { PageSpinner } from "@/components/ui/spinner";
import { AdminPageHeader, AdminTable } from "./admin-shell";

export function AdminProducts() {
  const [search, setSearch] = useState("");
  const [active, setActive] = useState<"" | "true" | "false">("");
  const [page, setPage] = useState(0);
  const q = useDebounced(search.trim());
  const products = useQuery({
    queryKey: ["admin", "products", q, active, page],
    queryFn: () => api<Page<AdminProductSummary>>(`admin/products${query({ q, active, page, size: 20 })}`),
    placeholderData: keepPreviousData,
  });

  return (
    <>
      <AdminPageHeader title="کالاها" action={<ButtonLink href="/admin/products/new"><Plus className="size-4" />کالای جدید</ButtonLink>} />
      <div className="mb-4 flex flex-wrap gap-2">
        <input
          type="search"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
          placeholder="جستجوی نام یا نامک"
          aria-label="جستجوی کالا"
          className="h-10 w-72 rounded-lg border border-neutral-300 bg-white px-3 text-sm"
        />
        <select
          value={active}
          aria-label="وضعیت"
          onChange={(e) => {
            setActive(e.target.value as typeof active);
            setPage(0);
          }}
          className="h-10 rounded-lg border border-neutral-300 bg-white px-3 text-sm"
        >
          <option value="">همه</option>
          <option value="true">فعال</option>
          <option value="false">غیرفعال</option>
        </select>
      </div>
      {products.isLoading ? <PageSpinner /> : (
        <>
          <AdminTable head={["نام کالا", "دسته‌بندی", "برند", "تاریخ ثبت", "وضعیت"]}>
            {products.data?.items.map((product) => (
              <tr key={product.id} className="hover:bg-neutral-50">
                <td className="px-4 py-3">
                  <Link href={`/admin/products/${product.id}`} className="font-medium text-brand-700">{product.name}</Link>
                  <span className="block text-xs text-neutral-400" dir="ltr">{product.slug}</span>
                </td>
                <td className="px-4 py-3">{product.categoryName}</td>
                <td className="px-4 py-3">{product.brandName ?? "—"}</td>
                <td className="px-4 py-3 text-neutral-500">{jalaliDate(product.createdAt)}</td>
                <td className="px-4 py-3">{product.active ? <Badge tone="success">فعال</Badge> : <Badge>غیرفعال</Badge>}</td>
              </tr>
            ))}
          </AdminTable>
          {products.data && !products.data.items.length && <p className="p-6 text-center text-sm text-neutral-500">کالایی پیدا نشد.</p>}
          <Pagination page={page} totalPages={products.data?.totalPages ?? 0} onChange={setPage} />
        </>
      )}
    </>
  );
}
