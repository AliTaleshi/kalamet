"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, Clock, MessageSquare, Truck } from "lucide-react";
import Link from "next/link";
import { api } from "@/lib/api/client";
import type { AdminReview, LowStock, OrderSummary, Page } from "@/lib/api/types";
import { faNumber, jalaliDateTime, toman } from "@/lib/format";
import { Card, CardHeader } from "@/components/ui/card";
import { PageSpinner } from "@/components/ui/spinner";
import { OrderStatusBadge } from "@/components/ui/status-badges";
import { AdminPageHeader } from "./admin-shell";

export function AdminDashboard() {
  const toShip = useQuery({ queryKey: ["admin", "orders", "PAID", 0], queryFn: () => api<Page<OrderSummary>>("admin/orders?status=PAID&size=1") });
  const unpaid = useQuery({ queryKey: ["admin", "orders", "PENDING_PAYMENT", 0], queryFn: () => api<Page<OrderSummary>>("admin/orders?status=PENDING_PAYMENT&size=1") });
  const reviews = useQuery({ queryKey: ["admin", "reviews", "PENDING", 0], queryFn: () => api<Page<AdminReview>>("admin/reviews?status=PENDING&size=1") });
  const lowStock = useQuery({ queryKey: ["admin", "low-stock"], queryFn: () => api<LowStock[]>("admin/variants/low-stock?threshold=5") });
  const recent = useQuery({ queryKey: ["admin", "orders", "recent"], queryFn: () => api<Page<OrderSummary>>("admin/orders?size=8") });

  return (
    <>
      <AdminPageHeader title="داشبورد" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Stat href="/admin/orders?status=PAID" icon={Truck} label="سفارش‌های آماده ارسال" value={toShip.data?.totalItems} tone="text-brand-600" />
        <Stat href="/admin/orders?status=PENDING_PAYMENT" icon={Clock} label="در انتظار پرداخت" value={unpaid.data?.totalItems} tone="text-accent-600" />
        <Stat href="/admin/reviews" icon={MessageSquare} label="دیدگاه‌های در انتظار تأیید" value={reviews.data?.totalItems} tone="text-sky-600" />
        <Stat href="/admin/products" icon={AlertTriangle} label="تنوع‌های رو به اتمام" value={lowStock.data?.length} tone="text-danger-600" />
      </div>
      <div className="mt-6 grid gap-4 xl:grid-cols-[1fr_22rem]">
        <Card>
          <CardHeader title="آخرین سفارش‌ها" action={<Link href="/admin/orders" className="text-sm text-brand-700">همه سفارش‌ها</Link>} />
          {recent.isLoading ? <PageSpinner /> : (
            <ul className="divide-y divide-neutral-100 text-sm">
              {recent.data?.items.map((order) => (
                <li key={order.orderNumber}>
                  <Link href={`/admin/orders/${order.orderNumber}`} className="flex flex-wrap items-center justify-between gap-2 px-5 py-3 hover:bg-neutral-50">
                    <span className="font-medium" dir="ltr">{order.orderNumber}</span>
                    <span className="text-neutral-500">{order.customer?.name}</span>
                    <span className="text-neutral-500">{jalaliDateTime(order.createdAt)}</span>
                    <span>{toman(order.total)} تومان</span>
                    <OrderStatusBadge status={order.status} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card>
          <CardHeader title="موجودی رو به اتمام" />
          {lowStock.isLoading ? <PageSpinner /> : !lowStock.data?.length ? (
            <p className="p-5 text-sm text-neutral-500">موجودی همه کالاها کافی است.</p>
          ) : (
            <ul className="divide-y divide-neutral-100 text-sm">
              {lowStock.data.map((item) => (
                <li key={item.variantId}>
                  <Link href={`/admin/products/${item.productId}`} className="flex items-center justify-between gap-2 px-5 py-3 hover:bg-neutral-50">
                    <span className="min-w-0">
                      <span className="block truncate text-neutral-800">{item.productName}</span>
                      <span className="text-xs text-neutral-400" dir="ltr">{item.sku}</span>
                    </span>
                    <span className={`font-bold ${item.stock === 0 ? "text-danger-600" : "text-accent-700"}`}>{faNumber(item.stock)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}

function Stat({ href, icon: Icon, label, value, tone }: { href: string; icon: typeof Truck; label: string; value?: number; tone: string }) {
  return (
    <Link href={href} className="flex items-center gap-4 rounded-2xl border border-neutral-200 bg-white p-5 hover:border-brand-300">
      <Icon className={`size-8 ${tone}`} />
      <div>
        <p className="text-2xl font-black text-neutral-800">{value === undefined ? "…" : faNumber(value)}</p>
        <p className="text-sm text-neutral-500">{label}</p>
      </div>
    </Link>
  );
}
