"use client";

import { clsx } from "clsx";
import { LogOut, MapPin, MessageSquare, Package, UserRound } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { faNumber } from "@/lib/format";
import { displayName, useMe, useSignOut } from "@/lib/hooks/session";

const LINKS = [
  { href: "/profile", label: "اطلاعات حساب", icon: UserRound },
  { href: "/profile/orders", label: "سفارش‌ها", icon: Package },
  { href: "/profile/addresses", label: "نشانی‌ها", icon: MapPin },
  { href: "/profile/reviews", label: "دیدگاه‌ها", icon: MessageSquare },
];

export function ProfileNav() {
  const pathname = usePathname();
  const { data: me } = useMe();
  const signOut = useSignOut();
  return (
    <aside className="rounded-2xl border border-neutral-200 bg-white lg:sticky lg:top-36">
      {me && (
        <div className="border-b border-neutral-100 p-5">
          <p className="font-bold text-neutral-800">{displayName(me)}</p>
          <p className="mt-1 text-sm text-neutral-500" dir="ltr">{faNumber(me.mobile)}</p>
        </div>
      )}
      <nav className="scrollbar-none flex overflow-x-auto p-2 lg:flex-col">
        {LINKS.map(({ href, label, icon: Icon }) => {
          const active = href === "/profile" ? pathname === href : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              aria-current={active ? "page" : undefined}
              className={clsx(
                "flex shrink-0 items-center gap-3 rounded-xl px-4 py-3 text-sm",
                active ? "bg-brand-50 font-bold text-brand-700" : "text-neutral-700 hover:bg-neutral-50",
              )}
            >
              <Icon className="size-5" />
              {label}
            </Link>
          );
        })}
        <button
          type="button"
          onClick={() => signOut.mutate()}
          className="flex shrink-0 items-center gap-3 rounded-xl px-4 py-3 text-sm text-neutral-700 hover:bg-neutral-50"
        >
          <LogOut className="size-5" />
          خروج
        </button>
      </nav>
    </aside>
  );
}
