import type { Metadata } from "next";
import { AdminCategories } from "@/components/admin/taxonomy";

export const metadata: Metadata = { title: "دسته‌بندی‌ها" };

export default function Page() {
  return <AdminCategories />;
}
