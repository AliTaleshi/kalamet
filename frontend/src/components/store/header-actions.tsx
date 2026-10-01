"use client";

import { ChevronDown, LayoutDashboard, LogIn, LogOut, MapPin, MessageSquare, Package, ShoppingCart, User } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { faNumber } from "@/lib/format";
import { useCart } from "@/lib/hooks/cart";
import { displayName, useMe, useSignOut } from "@/lib/hooks/session";

export function HeaderActions() {
  return (
    <div className="flex items-center gap-2">
      <AccountMenu />
      <span className="h-6 w-px bg-neutral-200" />
      <CartButton />
    </div>
  );
}

function CartButton() {
  const { itemCount } = useCart();
  return (
    <Link href="/cart" aria-label="سبد خرید" className="relative rounded-lg p-2 text-neutral-700 hover:bg-neutral-100">
      <ShoppingCart className="size-6" />
      {itemCount > 0 && (
        <span className="absolute -top-0.5 -left-0.5 flex min-w-5 items-center justify-center rounded-md bg-danger-500 px-1 text-xs font-bold text-white ring-2 ring-white">
          {faNumber(itemCount)}
        </span>
      )}
    </Link>
  );
}

function AccountMenu() {
  const { data: me, isLoading } = useMe();
  const signOut = useSignOut();
  const pathname = usePathname();
  // Remembering where the menu was opened closes it on navigation without an effect.
  const [openOn, setOpenOn] = useState<string | null>(null);
  const open = openOn === pathname;
  const setOpen = (value: boolean | ((open: boolean) => boolean)) =>
    setOpenOn((typeof value === "function" ? value(open) : value) ? pathname : null);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent) => {
      if (!ref.current?.contains(event.target as Node)) setOpenOn(null);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, [open]);

  if (isLoading) return <span className="h-10 w-28 animate-pulse rounded-lg bg-neutral-100" />;
  if (!me) {
    return (
      <Link
        href={pathname.startsWith("/login") ? "/login" : `/login?next=${encodeURIComponent(pathname)}`}
        className="flex h-10 items-center gap-2 rounded-lg border border-neutral-200 px-3 text-sm font-medium text-neutral-700 hover:bg-neutral-50"
      >
        <LogIn className="size-5" />
        <span className="hidden sm:inline">ورود | ثبت‌نام</span>
      </Link>
    );
  }
  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        aria-label="حساب کاربری"
        aria-expanded={open}
        aria-haspopup="menu"
        onClick={() => setOpen((v) => !v)}
        className="flex h-10 items-center gap-1 rounded-lg px-2 text-neutral-700 hover:bg-neutral-100"
      >
        <User className="size-6" />
        <ChevronDown className="size-4" />
      </button>
      {open && (
        <div role="menu" className="absolute left-0 top-12 z-30 w-64 rounded-xl border border-neutral-200 bg-white py-2 shadow-xl">
          <Link href="/profile" className="block border-b border-neutral-100 px-4 pb-3 pt-1">
            <p className="font-bold text-neutral-800">{displayName(me)}</p>
            <p className="text-xs text-neutral-500" dir="ltr">{faNumber(me.mobile)}</p>
          </Link>
          {me.role === "ADMIN" && <MenuLink href="/admin" icon={LayoutDashboard}>پنل مدیریت</MenuLink>}
          <MenuLink href="/profile/orders" icon={Package}>سفارش‌ها</MenuLink>
          <MenuLink href="/profile/addresses" icon={MapPin}>نشانی‌ها</MenuLink>
          <MenuLink href="/profile/reviews" icon={MessageSquare}>دیدگاه‌ها</MenuLink>
          <button
            type="button"
            role="menuitem"
            onClick={() => signOut.mutate()}
            className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-neutral-700 hover:bg-neutral-50"
          >
            <LogOut className="size-5 text-neutral-500" />
            خروج از حساب
          </button>
        </div>
      )}
    </div>
  );
}

function MenuLink({ href, icon: Icon, children }: { href: string; icon: typeof User; children: React.ReactNode }) {
  return (
    <Link role="menuitem" href={href} className="flex items-center gap-3 px-4 py-2.5 text-sm text-neutral-700 hover:bg-neutral-50">
      <Icon className="size-5 text-neutral-500" />
      {children}
    </Link>
  );
}
