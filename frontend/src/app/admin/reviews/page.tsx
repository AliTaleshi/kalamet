import type { Metadata } from "next";
import { AdminReviews } from "@/components/admin/moderation";

export const metadata: Metadata = { title: "دیدگاه‌ها" };

export default function Page() {
  return <AdminReviews />;
}
