import type { Metadata } from "next";
import { AdminBrands } from "@/components/admin/taxonomy";

export const metadata: Metadata = { title: "برندها" };

export default function Page() {
  return <AdminBrands />;
}
