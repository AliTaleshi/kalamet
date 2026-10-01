import type { Metadata } from "next";
import { OrderList } from "@/components/account/order-list";

export const metadata: Metadata = { title: "سفارش‌ها" };

export default function OrdersPage() {
  return <OrderList />;
}
