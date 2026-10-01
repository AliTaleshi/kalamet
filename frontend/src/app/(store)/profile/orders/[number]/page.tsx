import type { Metadata } from "next";
import { Suspense } from "react";
import { OrderDetail } from "@/components/account/order-detail";
import { PageSpinner } from "@/components/ui/spinner";

export const metadata: Metadata = { title: "جزئیات سفارش" };

export default async function OrderPage({ params }: { params: Promise<{ number: string }> }) {
  const { number } = await params;
  return (
    <Suspense fallback={<PageSpinner />}>
      <OrderDetail orderNumber={number} />
    </Suspense>
  );
}
