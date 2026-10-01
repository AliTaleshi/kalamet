import type { Metadata } from "next";
import { AdminProducts } from "@/components/admin/products";

export const metadata: Metadata = { title: "کالاها" };

export default function AdminProductsPage() {
  return <AdminProducts />;
}
