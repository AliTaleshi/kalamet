import type { OrderStatus, PaymentStatus, ReviewStatus } from "@/lib/api/types";
import { ORDER_STATUS_LABELS, PAYMENT_STATUS_LABELS, REVIEW_STATUS_LABELS } from "@/lib/format";
import { Badge } from "./badge";

const ORDER_TONES = {
  PENDING_PAYMENT: "warning",
  PAID: "brand",
  SHIPPED: "accent",
  DELIVERED: "success",
  CANCELLED: "neutral",
  REFUNDED: "danger",
} as const;

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <Badge tone={ORDER_TONES[status]}>{ORDER_STATUS_LABELS[status]}</Badge>;
}

const PAYMENT_TONES = { PENDING: "warning", SUCCEEDED: "success", FAILED: "danger", REFUNDED: "neutral" } as const;

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  return <Badge tone={PAYMENT_TONES[status]}>{PAYMENT_STATUS_LABELS[status]}</Badge>;
}

const REVIEW_TONES = { PENDING: "warning", APPROVED: "success", REJECTED: "danger" } as const;

export function ReviewStatusBadge({ status }: { status: ReviewStatus }) {
  return <Badge tone={REVIEW_TONES[status]}>{REVIEW_STATUS_LABELS[status]}</Badge>;
}
