import type { Metadata } from "next";
import { AdminUsers } from "@/components/admin/moderation";

export const metadata: Metadata = { title: "کاربران" };

export default function Page() {
  return <AdminUsers />;
}
