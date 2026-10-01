import Link from "next/link";
import type { Order } from "@/lib/api/types";
import { attributesText, faNumber, jalaliDateTime, toman } from "@/lib/format";
import { Card, CardHeader } from "@/components/ui/card";
import { ProductImage } from "@/components/ui/product-image";
import { OrderStatusBadge, PaymentStatusBadge } from "@/components/ui/status-badges";

/** Read-only order sheet, shared by the customer's order page and the admin order page. */
export function OrderSheet({ order, actions }: { order: Order; actions?: React.ReactNode }) {
  const address = order.shippingAddress;
  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader title={<span>سفارش <span dir="ltr">{order.orderNumber}</span></span>} action={<OrderStatusBadge status={order.status} />} />
        <dl className="grid gap-4 p-5 text-sm sm:grid-cols-2">
          <Item label="تاریخ ثبت" value={jalaliDateTime(order.createdAt)} />
          {order.customer && <Item label="مشتری" value={`${order.customer.name} (${faNumber(order.customer.mobile)})`} />}
          <Item label="تحویل‌گیرنده" value={`${address.recipientName} · ${faNumber(address.recipientMobile)}`} />
          <Item
            label="نشانی"
            value={`${address.province}، ${address.city}، ${address.addressLine}، پلاک ${faNumber(address.plaque)}${address.unit ? `، واحد ${faNumber(address.unit)}` : ""} · کد پستی ${faNumber(address.postalCode)}`}
          />
          <Item label="مبلغ کالاها" value={`${toman(order.itemsTotal)} تومان`} />
          <Item label="هزینه ارسال" value={order.shippingFee ? `${toman(order.shippingFee)} تومان` : "رایگان"} />
          {order.discountTotal > 0 && <Item label="سود شما" value={`${toman(order.discountTotal)} تومان`} />}
          <Item label="مبلغ کل" value={`${toman(order.total)} تومان`} strong />
        </dl>
        {actions && <div className="flex flex-wrap items-center gap-3 border-t border-neutral-100 p-5">{actions}</div>}
      </Card>
      <Card>
        <CardHeader title={`کالاها (${faNumber(order.items.reduce((s, i) => s + i.quantity, 0))})`} />
        <ul className="divide-y divide-neutral-100">
          {order.items.map((item) => (
            <li key={item.variantId} className="flex gap-4 p-5">
              <ProductImage src={item.imageUrl} alt={item.productName} className="size-20 shrink-0 rounded-xl" />
              <div className="flex min-w-0 flex-1 flex-col gap-1 text-sm">
                <Link href={`/product/${item.productSlug}`} className="font-medium text-neutral-800 hover:text-brand-700">{item.productName}</Link>
                {Object.keys(item.attributes).length > 0 && <span className="text-xs text-neutral-500">{attributesText(item.attributes)}</span>}
                <span className="text-xs text-neutral-400" dir="ltr">{item.sku}</span>
                <span className="mt-auto text-neutral-600">
                  {faNumber(item.quantity)} × {toman(item.unitPrice)} = <b className="text-neutral-800">{toman(item.lineTotal)} تومان</b>
                </span>
              </div>
            </li>
          ))}
        </ul>
      </Card>
      {order.payments.length > 0 && (
        <Card>
          <CardHeader title="پرداخت‌ها" />
          <ul className="divide-y divide-neutral-100 text-sm">
            {order.payments.map((payment) => (
              <li key={payment.id} className="flex flex-wrap items-center justify-between gap-2 p-5">
                <span className="flex items-center gap-2">
                  <PaymentStatusBadge status={payment.status} />
                  <span className="text-neutral-600">{payment.gateway === "MOCK" ? "درگاه آزمایشی" : "زرین‌پال"}</span>
                </span>
                <span className="text-neutral-500">{jalaliDateTime(payment.createdAt)}</span>
                <span className="text-neutral-700">{toman(payment.amount)} تومان</span>
                {payment.refId && <span className="text-neutral-500">کد پیگیری: {faNumber(payment.refId)}</span>}
                {payment.failureReason && <span className="w-full text-xs text-danger-600">{payment.failureReason}</span>}
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}

function Item({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div>
      <dt className="text-neutral-500">{label}</dt>
      <dd className={`mt-1 leading-7 ${strong ? "font-bold text-neutral-900" : "text-neutral-800"}`}>{value}</dd>
    </div>
  );
}
