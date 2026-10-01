"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { ChevronLeft, Package } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { api } from "@/lib/api/client";
import type { OrderSummary, Page } from "@/lib/api/types";
import { faNumber, jalaliDate, toman } from "@/lib/format";
import { ButtonLink } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { ProductImage } from "@/components/ui/product-image";
import { PageSpinner } from "@/components/ui/spinner";
import { OrderStatusBadge } from "@/components/ui/status-badges";

export function OrderList() {
  const [page, setPage] = useState(0);
  const orders = useQuery({
    queryKey: ["orders", page],
    queryFn: () => api<Page<OrderSummary>>(`orders?page=${page}&size=10`),
    placeholderData: keepPreviousData,
  });

  return (
    <Card>
      <CardHeader title="تاریخچه سفارش‌ها" />
      {orders.isLoading ? (
        <PageSpinner />
      ) : !orders.data?.items.length ? (
        <EmptyState icon={Package} title="هنوز سفارشی ثبت نکرده‌اید" action={<ButtonLink href="/">شروع خرید</ButtonLink>} />
      ) : (
        <>
          <ul className="divide-y divide-neutral-100">
            {orders.data.items.map((order) => (
              <li key={order.orderNumber}>
                <Link href={`/profile/orders/${order.orderNumber}`} className="flex flex-col gap-3 p-5 hover:bg-neutral-50">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <OrderStatusBadge status={order.status} />
                    <ChevronLeft className="size-5 text-neutral-400" />
                  </div>
                  <div className="flex flex-wrap gap-x-6 gap-y-1 text-sm text-neutral-500">
                    <span>{jalaliDate(order.createdAt)}</span>
                    <span>کد سفارش <b className="text-neutral-700" dir="ltr">{order.orderNumber}</b></span>
                    <span>مبلغ <b className="text-neutral-700">{toman(order.total)}</b> تومان</span>
                    <span>{faNumber(order.itemCount)} کالا</span>
                  </div>
                  <div className="flex gap-2">
                    {order.imageUrls.map((url) => (
                      <ProductImage key={url} src={url} alt="" className="size-16 rounded-lg border border-neutral-100" />
                    ))}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
          <Pagination page={page} totalPages={orders.data.totalPages} onChange={setPage} />
        </>
      )}
    </Card>
  );
}
