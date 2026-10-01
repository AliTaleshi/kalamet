import { CheckCircle2, XCircle } from "lucide-react";
import type { Metadata } from "next";
import { ButtonLink } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { faNumber } from "@/lib/format";

export const metadata: Metadata = { title: "نتیجه پرداخت" };

type Props = { searchParams: Promise<Record<string, string | string[] | undefined>> };

/** Where the payment gateway sends the customer back (through the API's callback). */
export default async function PaymentResultPage({ searchParams }: Props) {
  const params = await searchParams;
  const value = (key: string) => (typeof params[key] === "string" ? (params[key] as string) : undefined);
  const success = value("status") === "success";
  const order = value("order")?.match(/^KL-\d{1,10}$/)?.[0];
  const ref = value("ref")?.match(/^[\w-]{1,40}$/)?.[0];

  return (
    <div className="flex justify-center py-10">
      <Card className="flex w-full max-w-lg flex-col items-center gap-4 p-8 text-center">
        {success ? <CheckCircle2 className="size-16 text-emerald-600" /> : <XCircle className="size-16 text-danger-500" />}
        <h1 className="text-xl font-black text-neutral-800">{success ? "سفارش شما با موفقیت ثبت شد" : "پرداخت ناموفق بود"}</h1>
        <p className="text-sm leading-7 text-neutral-600">
          {success
            ? "از خرید شما سپاسگزاریم. سفارش شما آماده‌سازی و ارسال می‌شود."
            : "مبلغی از حساب شما کسر نشده است؛ اگر کسر شده باشد، حداکثر تا ۷۲ ساعت آینده به حساب شما برمی‌گردد. می‌توانید دوباره پرداخت کنید."}
        </p>
        {order && (
          <dl className="w-full space-y-2 rounded-xl bg-neutral-50 p-4 text-sm">
            <div className="flex justify-between"><dt className="text-neutral-500">شماره سفارش</dt><dd className="font-bold" dir="ltr">{order}</dd></div>
            {ref && <div className="flex justify-between"><dt className="text-neutral-500">کد پیگیری</dt><dd className="font-bold">{faNumber(ref)}</dd></div>}
          </dl>
        )}
        <div className="flex gap-3">
          {order && <ButtonLink href={`/profile/orders/${order}`}>{success ? "مشاهده سفارش" : "تلاش دوباره برای پرداخت"}</ButtonLink>}
          <ButtonLink href="/" variant="outline">بازگشت به فروشگاه</ButtonLink>
        </div>
      </Card>
    </div>
  );
}
