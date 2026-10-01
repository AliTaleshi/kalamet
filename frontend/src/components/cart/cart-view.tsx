"use client";

import { AlertTriangle, ServerCrash, ShoppingCart, Truck } from "lucide-react";
import Link from "next/link";
import type { LineIssue } from "@/lib/api/types";
import { attributesText, faNumber, toman } from "@/lib/format";
import { useCart, type CartLineView } from "@/lib/hooks/cart";
import { Button, ButtonLink } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { Price } from "@/components/ui/price";
import { ProductImage } from "@/components/ui/product-image";
import { QuantityStepper } from "@/components/ui/quantity";
import { PageSpinner } from "@/components/ui/spinner";

const ISSUES: Record<LineIssue, string> = {
  UNAVAILABLE: "این کالا دیگر فروخته نمی‌شود؛ آن را حذف کنید.",
  OUT_OF_STOCK: "ناموجود شد؛ آن را حذف کنید.",
  INSUFFICIENT_STOCK: "موجودی کافی نیست؛ تعداد را کم کنید.",
};

export function CartView() {
  const cart = useCart();
  if (!cart.ready) return <PageSpinner />;
  if (cart.failed) {
    return (
      <EmptyState
        icon={ServerCrash}
        title="سبد خرید بارگذاری نشد"
        description="ارتباط با فروشگاه برقرار نشد. لطفاً دوباره تلاش کنید."
        action={<Button onClick={cart.retry}>تلاش دوباره</Button>}
      />
    );
  }
  if (!cart.lines.length) {
    return (
      <EmptyState
        icon={ShoppingCart}
        title="سبد خرید شما خالی است!"
        description="می‌توانید برای مشاهده محصولات بیشتر به صفحه‌های زیر بروید."
        action={<ButtonLink href="/search?offers=true">مشاهده شگفت‌انگیزها</ButtonLink>}
      />
    );
  }
  return (
    <div className="grid items-start gap-4 lg:grid-cols-[1fr_20rem]">
      <Card>
        <div className="flex items-center justify-between border-b border-neutral-100 px-5 py-4">
          <h1 className="font-black text-neutral-800">سبد خرید شما</h1>
          <span className="text-sm text-neutral-500">{faNumber(cart.itemCount)} کالا</span>
        </div>
        <ul className="divide-y divide-neutral-100">
          {cart.lines.map((line) => (
            <CartLineRow key={line.variantId} line={line} busy={cart.pending === line.variantId} onChange={(q) => cart.setQuantity(line.variantId, q)} />
          ))}
        </ul>
      </Card>
      <Summary />
    </div>
  );
}

function CartLineRow({ line, busy, onChange }: { line: CartLineView; busy: boolean; onChange: (quantity: number) => void }) {
  return (
    <li className="flex gap-4 p-5">
      <Link href={`/product/${line.productSlug}`} className="shrink-0">
        <ProductImage src={line.imageUrl} alt={line.productName} className="size-24 rounded-xl" />
      </Link>
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <Link href={`/product/${line.productSlug}`} className="font-medium leading-7 text-neutral-800 hover:text-brand-700">
          {line.productName}
        </Link>
        {Object.keys(line.attributes).length > 0 && <p className="text-xs text-neutral-500">{attributesText(line.attributes)}</p>}
        {line.issue && (
          <p className="flex items-center gap-1 text-xs font-medium text-danger-600">
            <AlertTriangle className="size-4" />
            {ISSUES[line.issue]}
          </p>
        )}
        <div className="mt-auto flex flex-wrap items-end justify-between gap-3">
          <QuantityStepper value={line.quantity} max={Math.max(line.maxQuantity, 0)} busy={busy} onChange={onChange} />
          <Price price={line.lineTotal} originalPrice={line.originalPrice ? line.originalPrice * line.quantity : null} />
        </div>
      </div>
    </li>
  );
}

function Summary() {
  const cart = useCart();
  const totals = cart.totals;
  return (
    <Card className="sticky top-36 p-5">
      {totals ? (
        <>
          <dl className="space-y-3 text-sm">
            <Row label={`قیمت کالاها (${faNumber(cart.itemCount)})`} value={totals.itemsTotal + totals.discountTotal} />
            {totals.discountTotal > 0 && <Row label="سود شما از خرید" value={totals.discountTotal} tone="text-danger-600" />}
            <Row label="هزینه ارسال" value={totals.shippingFee} free={totals.shippingFee === 0} />
            <div className="border-t border-neutral-100 pt-3">
              <Row label="جمع سبد خرید" value={totals.payable} strong />
            </div>
          </dl>
          {totals.freeShippingRemaining > 0 && (
            <p className="mt-4 flex items-start gap-2 rounded-xl bg-brand-50 p-3 text-xs leading-6 text-brand-800">
              <Truck className="size-5 shrink-0" />
              با افزودن {toman(totals.freeShippingRemaining)} تومان دیگر، ارسال رایگان می‌شود.
            </p>
          )}
          {totals.hasIssues && <p className="mt-4 text-xs text-danger-600">برای ادامه، کالاهای دارای مشکل را اصلاح کنید.</p>}
          {totals.hasIssues ? (
            <Button size="lg" className="mt-5 w-full" disabled>تأیید و تکمیل سفارش</Button>
          ) : (
            <ButtonLink href="/checkout" size="lg" className="mt-5 w-full">تأیید و تکمیل سفارش</ButtonLink>
          )}
        </>
      ) : (
        <>
          <dl className="text-sm">
            <Row label={`قیمت کالاها (${faNumber(cart.itemCount)})`} value={cart.guestTotal} strong />
          </dl>
          <p className="mt-3 text-xs leading-6 text-neutral-500">
            قیمت نهایی، موجودی و هزینه ارسال پس از ورود به حساب کاربری محاسبه می‌شود.
          </p>
          <ButtonLink href="/login?next=/cart" size="lg" className="mt-5 w-full">ورود و ادامه خرید</ButtonLink>
        </>
      )}
    </Card>
  );
}

function Row({ label, value, strong, free, tone }: { label: string; value: number; strong?: boolean; free?: boolean; tone?: string }) {
  return (
    <div className={`flex items-center justify-between ${strong ? "font-bold text-neutral-900" : "text-neutral-600"}`}>
      <dt>{label}</dt>
      <dd className={tone}>{free ? <span className="text-brand-700">رایگان</span> : <>{toman(value)} <span className="text-xs">تومان</span></>}</dd>
    </div>
  );
}
