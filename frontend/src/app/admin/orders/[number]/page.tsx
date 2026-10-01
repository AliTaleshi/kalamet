import type { Metadata } from "next";
import { AdminOrderDetail } from "@/components/admin/orders";

export const metadata: Metadata = { title: "جزئیات سفارش" };

export default async function AdminOrderPage({ params }: { params: Promise<{ number: string }> }) {
  return <AdminOrderDetail orderNumber={(await params).number} />;
}
