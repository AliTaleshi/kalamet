"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { clsx } from "clsx";
import { ArrowRight } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { api, query } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/error";
import type { Order, OrderStatus, OrderSummary, Page } from "@/lib/api/types";
import { faNumber, jalaliDateTime, ORDER_STATUS_LABELS, toman } from "@/lib/format";
import { useDebounced } from "@/lib/hooks/debounce";
import { OrderSheet } from "@/components/account/order-view";
import { Button } from "@/components/ui/button";
import { Pagination } from "@/components/ui/pagination";
import { PageSpinner } from "@/components/ui/spinner";
import { OrderStatusBadge } from "@/components/ui/status-badges";
import { useToast } from "@/components/ui/toast";
import { AdminPageHeader, AdminTable } from "./admin-shell";

const STATUSES = Object.keys(ORDER_STATUS_LABELS) as OrderStatus[];

/** Mirrors OrderStatus.adminTargets() in the API. */
const NEXT_STATUSES: Record<OrderStatus, OrderStatus[]> = {
  PENDING_PAYMENT: ["CANCELLED"],
  PAID: ["SHIPPED", "REFUNDED"],
  SHIPPED: ["DELIVERED", "REFUNDED"],
  DELIVERED: ["REFUNDED"],
  CANCELLED: [],
  REFUNDED: [],
};

const ACTION_LABELS: Partial<Record<OrderStatus, string>> = {
  SHIPPED: "ثبت ارسال",
  DELIVERED: "ثبت تحویل",
  CANCELLED: "لغو سفارش",
  REFUNDED: "ثبت بازگشت وجه",
};

export function AdminOrders() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const status = (STATUSES as string[]).includes(params.get("status") ?? "") ? (params.get("status") as OrderStatus) : null;
  const [search, setSearch] = useState("");
  const q = useDebounced(search.trim());
  const [page, setPage] = useState(0);
  const orders = useQuery({
    queryKey: ["admin", "orders", status, q, page],
    queryFn: () => api<Page<OrderSummary>>(`admin/orders${query({ status, q, page, size: 20 })}`),
    placeholderData: keepPreviousData,
  });
  const setStatus = (value: OrderStatus | null) => {
    setPage(0);
    router.replace(value ? `${pathname}?status=${value}` : pathname);
  };

  return (
    <>
      <AdminPageHeader title="سفارش‌ها" />
      <div className="mb-4 flex flex-wrap items-center gap-2">
        {[null, ...STATUSES].map((value) => (
          <button
            key={value ?? "all"}
            type="button"
            onClick={() => setStatus(value)}
            className={clsx("rounded-full border px-3 py-1.5 text-sm", value === status ? "border-brand-600 bg-brand-600 text-white" : "border-neutral-300 bg-white text-neutral-600")}
          >
            {value ? ORDER_STATUS_LABELS[value] : "همه"}
          </button>
        ))}
        <input
          type="search"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
          placeholder="شماره سفارش یا موبایل مشتری"
          aria-label="جستجوی سفارش"
          className="ms-auto h-10 w-72 rounded-lg border border-neutral-300 bg-white px-3 text-sm"
        />
      </div>
      {orders.isLoading ? <PageSpinner /> : (
        <>
          <AdminTable head={["شماره سفارش", "مشتری", "تاریخ", "کالاها", "مبلغ", "وضعیت"]}>
            {orders.data?.items.map((order) => (
              <tr key={order.orderNumber} className="hover:bg-neutral-50">
                <td className="px-4 py-3">
                  <Link href={`/admin/orders/${order.orderNumber}`} className="font-medium text-brand-700" dir="ltr">{order.orderNumber}</Link>
                </td>
                <td className="px-4 py-3">{order.customer?.name}<span className="block text-xs text-neutral-400" dir="ltr">{order.customer && faNumber(order.customer.mobile)}</span></td>
                <td className="px-4 py-3 text-neutral-500">{jalaliDateTime(order.createdAt)}</td>
                <td className="px-4 py-3">{faNumber(order.itemCount)}</td>
                <td className="px-4 py-3">{toman(order.total)}</td>
                <td className="px-4 py-3"><OrderStatusBadge status={order.status} /></td>
              </tr>
            ))}
          </AdminTable>
          {orders.data && !orders.data.items.length && <p className="p-6 text-center text-sm text-neutral-500">سفارشی پیدا نشد.</p>}
          <Pagination page={page} totalPages={orders.data?.totalPages ?? 0} onChange={setPage} />
        </>
      )}
    </>
  );
}

export function AdminOrderDetail({ orderNumber }: { orderNumber: string }) {
  const client = useQueryClient();
  const notify = useToast();
  const key = ["admin", "order", orderNumber];
  const order = useQuery({ queryKey: key, queryFn: () => api<Order>(`admin/orders/${encodeURIComponent(orderNumber)}`) });
  const change = useMutation({
    mutationFn: (status: OrderStatus) =>
      api<Order>(`admin/orders/${encodeURIComponent(orderNumber)}/status`, { method: "PATCH", body: { status } }),
    onSuccess: (updated) => {
      client.setQueryData(key, updated);
      client.invalidateQueries({ queryKey: ["admin", "orders"] });
      notify(`وضعیت به «${ORDER_STATUS_LABELS[updated.status]}» تغییر کرد`);
    },
    onError: (e) => notify(errorMessage(e), "error"),
  });

  if (order.isLoading) return <PageSpinner />;
  if (!order.data) return <p className="text-neutral-500">سفارش پیدا نشد.</p>;
  const targets = NEXT_STATUSES[order.data.status];
  return (
    <>
      <Link href="/admin/orders" className="mb-4 flex w-fit items-center gap-1 text-sm text-neutral-500 hover:text-neutral-800">
        <ArrowRight className="size-4" />
        بازگشت به سفارش‌ها
      </Link>
      <OrderSheet
        order={order.data}
        actions={targets.length ? targets.map((target) => (
          <Button
            key={target}
            variant={target === "REFUNDED" || target === "CANCELLED" ? "outline" : "primary"}
            loading={change.isPending && change.variables === target}
            onClick={() => {
              if (target === "REFUNDED" || target === "CANCELLED") {
                if (!window.confirm(`${ACTION_LABELS[target]}؟ این کار برگشت‌پذیر نیست.`)) return;
              }
              change.mutate(target);
            }}
          >
            {ACTION_LABELS[target]}
          </Button>
        )) : undefined}
      />
    </>
  );
}
