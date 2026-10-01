"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { clsx } from "clsx";
import { MapPin, Plus, ShieldCheck } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/error";
import type { Address, Order, PayResponse } from "@/lib/api/types";
import { faNumber, toman } from "@/lib/format";
import { CART_KEY, useCart } from "@/lib/hooks/cart";
import { AddressForm } from "@/components/account/address-form";
import { Button, ButtonLink } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { FormError } from "@/components/ui/field";
import { ProductImage } from "@/components/ui/product-image";
import { PageSpinner } from "@/components/ui/spinner";

export function CheckoutView() {
  const router = useRouter();
  const client = useQueryClient();
  const cart = useCart();
  const addresses = useQuery({ queryKey: ["addresses"], queryFn: () => api<Address[]>("me/addresses") });
  const [chosen, setChosen] = useState<number | null>(null);
  const [adding, setAdding] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // The proxy already sends signed-out visitors to /login; this covers a session that ended meanwhile.
  useEffect(() => {
    if (cart.ready && cart.guest) router.replace("/login?next=/checkout");
  }, [cart.ready, cart.guest, router]);

  if (!cart.ready || cart.guest || addresses.isLoading) return <PageSpinner />;
  if (!cart.totals || !cart.lines.length) {
    return (
      <div className="py-16 text-center">
        <p className="text-neutral-600">سبد خرید شما خالی است.</p>
        <ButtonLink href="/" className="mt-4">بازگشت به فروشگاه</ButtonLink>
      </div>
    );
  }
  const totals = cart.totals;
  const list = addresses.data ?? [];
  const addressId = chosen ?? list.find((a) => a.isDefault)?.id ?? list[0]?.id ?? null;

  const placeOrder = async () => {
    if (!addressId) return;
    setBusy(true);
    setError(null);
    let order: Order;
    try {
      order = await api<Order>("orders", { method: "POST", body: { addressId, expectedPayable: totals.payable } });
    } catch (e) {
      setError(errorMessage(e));
      if (e instanceof ApiError && ["PRICES_CHANGED", "CART_HAS_ISSUES", "CONCURRENT_UPDATE"].includes(e.code)) {
        await client.invalidateQueries({ queryKey: CART_KEY });
      }
      setBusy(false);
      return;
    }
    client.setQueryData(CART_KEY, undefined);
    await client.invalidateQueries({ queryKey: CART_KEY });
    if (order.status !== "PENDING_PAYMENT") {
      router.push(`/checkout/result?status=success&order=${order.orderNumber}`);
      return;
    }
    try {
      const payment = await api<PayResponse>(`orders/${order.orderNumber}/pay`, { method: "POST", body: {} });
      window.location.assign(payment.paymentUrl);
    } catch {
      // The order exists and holds the stock; the customer can pay from the order page.
      router.push(`/profile/orders/${order.orderNumber}?payment=failed`);
    }
  };

  return (
    <div className="grid items-start gap-4 lg:grid-cols-[1fr_20rem]">
      <div className="flex flex-col gap-4">
        <Card>
          <CardHeader
            title="نشانی تحویل سفارش"
            action={
              <button type="button" onClick={() => setAdding(true)} className="flex items-center gap-1 text-sm font-medium text-brand-700">
                <Plus className="size-4" />
                نشانی جدید
              </button>
            }
          />
          {list.length === 0 ? (
            <div className="flex flex-col items-center gap-3 p-8 text-center text-sm text-neutral-500">
              <MapPin className="size-8 text-brand-500" />
              هنوز نشانی ثبت نکرده‌اید.
              <Button onClick={() => setAdding(true)}>افزودن نشانی</Button>
            </div>
          ) : (
            <ul className="flex flex-col gap-3 p-5" role="radiogroup" aria-label="نشانی تحویل">
              {list.map((address) => (
                <li key={address.id}>
                  <label
                    className={clsx(
                      "flex cursor-pointer gap-3 rounded-xl border p-4",
                      address.id === addressId ? "border-brand-500 bg-brand-50/50" : "border-neutral-200",
                    )}
                  >
                    <input type="radio" name="address" className="mt-1 accent-brand-600" checked={address.id === addressId} onChange={() => setChosen(address.id)} />
                    <span className="flex flex-col gap-1 text-sm">
                      <span className="font-medium text-neutral-800">
                        {address.province.name}، {address.city}، {address.addressLine}، پلاک {faNumber(address.plaque)}
                        {address.unit ? `، واحد ${faNumber(address.unit)}` : ""}
                      </span>
                      <span className="text-neutral-500">
                        {address.recipientName} · {faNumber(address.recipientMobile)} · کد پستی {faNumber(address.postalCode)}
                      </span>
                    </span>
                  </label>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card>
          <CardHeader title={`مرسوله (${faNumber(cart.itemCount)} کالا)`} />
          <ul className="flex flex-wrap gap-3 p-5">
            {cart.lines.map((line) => (
              <li key={line.variantId} className="relative">
                <ProductImage src={line.imageUrl} alt={line.productName} className="size-20 rounded-xl border border-neutral-100" />
                <span className="absolute -bottom-1 -left-1 rounded-md bg-neutral-800 px-1.5 text-xs text-white">{faNumber(line.quantity)}</span>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <Card className="sticky top-36 p-5">
        <dl className="space-y-3 text-sm text-neutral-600">
          <div className="flex justify-between"><dt>قیمت کالاها</dt><dd>{toman(totals.itemsTotal + totals.discountTotal)} تومان</dd></div>
          {totals.discountTotal > 0 && (
            <div className="flex justify-between text-danger-600"><dt>سود شما</dt><dd>{toman(totals.discountTotal)} تومان</dd></div>
          )}
          <div className="flex justify-between">
            <dt>هزینه ارسال</dt>
            <dd>{totals.shippingFee === 0 ? <span className="text-brand-700">رایگان</span> : `${toman(totals.shippingFee)} تومان`}</dd>
          </div>
          <div className="flex justify-between border-t border-neutral-100 pt-3 font-bold text-neutral-900">
            <dt>مبلغ قابل پرداخت</dt><dd>{toman(totals.payable)} تومان</dd>
          </div>
        </dl>
        <div className="mt-4">
          <FormError message={error} />
        </div>
        <Button size="lg" className="mt-4 w-full" disabled={!addressId || totals.hasIssues} loading={busy} onClick={placeOrder}>
          پرداخت
        </Button>
        <p className="mt-3 flex items-center justify-center gap-1 text-xs text-neutral-500">
          <ShieldCheck className="size-4 text-brand-600" />
          پرداخت امن از طریق درگاه بانکی
        </p>
      </Card>

      <Dialog open={adding} onClose={() => setAdding(false)} title="افزودن نشانی جدید">
        <AddressForm
          onSaved={async (address) => {
            setAdding(false);
            setChosen(address.id);
            await addresses.refetch();
          }}
        />
      </Dialog>
    </div>
  );
}
