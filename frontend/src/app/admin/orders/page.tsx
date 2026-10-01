import type { Metadata } from "next";
import { Suspense } from "react";
import { AdminOrders } from "@/components/admin/orders";
import { PageSpinner } from "@/components/ui/spinner";

export const metadata: Metadata = { title: "سفارش‌ها" };

export default function AdminOrdersPage() {
  return (
    <Suspense fallback={<PageSpinner />}>
      <AdminOrders />
    </Suspense>
  );
}
