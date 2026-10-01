"use client";

import { clsx } from "clsx";
import { FolderTree, LayoutDashboard, LogOut, MessageSquare, Package, ShoppingBag, Store, Tags, Users } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSignOut } from "@/lib/hooks/session";

const LINKS = [
  { href: "/admin", label: "داشبورد", icon: LayoutDashboard },
  { href: "/admin/orders", label: "سفارش‌ها", icon: ShoppingBag },
  { href: "/admin/products", label: "کالاها", icon: Package },
  { href: "/admin/categories", label: "دسته‌بندی‌ها", icon: FolderTree },
  { href: "/admin/brands", label: "برندها", icon: Tags },
  { href: "/admin/reviews", label: "دیدگاه‌ها", icon: MessageSquare },
  { href: "/admin/users", label: "کاربران", icon: Users },
];

export function AdminShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const signOut = useSignOut();
  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[15rem_1fr]">
      <aside className="flex flex-col bg-brand-900 text-brand-50 lg:sticky lg:top-0 lg:h-dvh">
        <div className="flex items-center gap-2 px-5 py-5">
          <span className="flex size-9 items-center justify-center rounded-xl bg-brand-500 text-lg font-black">ک</span>
          <span className="font-black">پنل مدیریت کالامت</span>
        </div>
        <nav className="scrollbar-none flex gap-1 overflow-x-auto px-3 pb-3 lg:flex-1 lg:flex-col">
          {LINKS.map(({ href, label, icon: Icon }) => {
            const active = href === "/admin" ? pathname === href : pathname.startsWith(href);
            return (
              <Link
                key={href}
                href={href}
                aria-current={active ? "page" : undefined}
                className={clsx("flex shrink-0 items-center gap-3 rounded-xl px-4 py-2.5 text-sm", active ? "bg-white/15 font-bold text-white" : "text-brand-100 hover:bg-white/10")}
              >
                <Icon className="size-5" />
                {label}
              </Link>
            );
          })}
        </nav>
        <div className="hidden border-t border-white/10 p-3 lg:block">
          <Link href="/" className="flex items-center gap-3 rounded-xl px-4 py-2.5 text-sm text-brand-100 hover:bg-white/10">
            <Store className="size-5" />
            مشاهده فروشگاه
          </Link>
          <button type="button" onClick={() => signOut.mutate()} className="flex w-full items-center gap-3 rounded-xl px-4 py-2.5 text-sm text-brand-100 hover:bg-white/10">
            <LogOut className="size-5" />
            خروج
          </button>
        </div>
      </aside>
      <main className="min-w-0 bg-neutral-50 p-4 lg:p-8">{children}</main>
    </div>
  );
}

export function AdminPageHeader({ title, action }: { title: string; action?: React.ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
      <h1 className="text-xl font-black text-neutral-800">{title}</h1>
      {action}
    </div>
  );
}

/** Simple responsive table wrapper with Persian headings. */
export function AdminTable({ head, children }: { head: string[]; children: React.ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-neutral-200 bg-white">
      <table className="w-full min-w-[40rem] text-sm">
        <thead className="bg-neutral-50 text-neutral-500">
          <tr>
            {head.map((h, i) => (
              <th key={i} className="px-4 py-3 text-start font-medium">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-neutral-100 text-neutral-700">{children}</tbody>
      </table>
    </div>
  );
}
