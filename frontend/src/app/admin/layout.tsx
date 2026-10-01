import type { Metadata } from "next";
import { notFound, redirect } from "next/navigation";
import { AdminShell } from "@/components/admin/admin-shell";
import { currentUser } from "@/lib/api/server";

export const metadata: Metadata = { title: { default: "پنل مدیریت", template: "%s | پنل مدیریت کالامت" }, robots: { index: false } };
export const dynamic = "force-dynamic";

/** Admins only. The API enforces this too; here it only avoids showing an unusable panel. */
export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const user = await currentUser();
  if (!user) redirect("/login?next=/admin");
  if (user.role !== "ADMIN") notFound();
  return <AdminShell>{children}</AdminShell>;
}
