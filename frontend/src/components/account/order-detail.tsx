"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CreditCard, PackageX } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/error";
import type { Order, PayResponse } from "@/lib/api/types";
import { jalaliDateTime } from "@/lib/format";
import { Button, ButtonLink } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError } from "@/components/ui/field";
import { PageSpinner } from "@/components/ui/spinner";
import { useToast } from "@/components/ui/toast";
import { OrderSheet } from "./order-view";

export function OrderDetail({ orderNumber }: { orderNumber: string }) {
  const client = useQueryClient();
  const notify = useToast();
  const params = useSearchParams();
  const order = useQuery({ queryKey: ["order", orderNumber], queryFn: () => api<Order>(`orders/${encodeURIComponent(orderNumber)}`) });
  const pay = useMutation({
    mutationFn: () => api<PayResponse>(`orders/${encodeURIComponent(orderNumber)}/pay`, { method: "POST", body: {} }),
    onSuccess: (payment) => window.location.assign(payment.paymentUrl),
  });
  const cancel = useMutation({
    mutationFn: () => api<Order>(`orders/${encodeURIComponent(orderNumber)}/cancel`, { method: "POST" }),
    onSuccess: (updated) => {
      client.setQueryData(["order", orderNumber], updated);
      client.invalidateQueries({ queryKey: ["orders"] });
      notify("سفارش لغو شد");
    },
  });

  if (order.isLoading) return <PageSpinner />;
  if (!order.data) {
    return <EmptyState icon={PackageX} title="سفارش پیدا نشد" action={<ButtonLink href="/profile/orders">بازگشت به سفارش‌ها</ButtonLink>} />;
  }
  const data = order.data;
  const payable = data.status === "PENDING_PAYMENT" && data.payableUntil && new Date(data.payableUntil) > new Date();

  return (
    <OrderSheet
      order={data}
      actions={
        data.status === "PENDING_PAYMENT" ? (
          <>
            {params.get("payment") === "failed" && <FormError message="اتصال به درگاه پرداخت ممکن نشد؛ دوباره تلاش کنید." />}
            {payable ? (
              <>
                <Button loading={pay.isPending} onClick={() => pay.mutate()}>
                  <CreditCard className="size-5" />
                  پرداخت سفارش
                </Button>
                <span className="text-xs text-neutral-500">مهلت پرداخت تا {jalaliDateTime(data.payableUntil!)}</span>
              </>
            ) : (
              <span className="text-sm text-neutral-500">مهلت پرداخت این سفارش تمام شده است.</span>
            )}
            <Button variant="ghost" loading={cancel.isPending} onClick={() => cancel.mutate()} className="ms-auto text-danger-600">
              لغو سفارش
            </Button>
            <FormError message={pay.error ? errorMessage(pay.error) : cancel.error ? errorMessage(cancel.error) : null} />
          </>
        ) : undefined
      }
    />
  );
}
