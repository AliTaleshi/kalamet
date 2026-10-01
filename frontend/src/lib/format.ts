// Persian presentation helpers. Every formatter pins the Tehran time zone so the server and the
// browser render the same text (no hydration mismatches).

const TIME_ZONE = "Asia/Tehran";
const numberFormat = new Intl.NumberFormat("fa-IR");
const dateFormat = new Intl.DateTimeFormat("fa-IR-u-ca-persian", {
  timeZone: TIME_ZONE,
  year: "numeric",
  month: "long",
  day: "numeric",
});
const dateTimeFormat = new Intl.DateTimeFormat("fa-IR-u-ca-persian", {
  timeZone: TIME_ZONE,
  year: "numeric",
  month: "long",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

/** 1234567 -> "۱٬۲۳۴٬۵۶۷" */
export function faNumber(value: number | string): string {
  return typeof value === "number" ? numberFormat.format(value) : toPersianDigits(value);
}

export function toPersianDigits(text: string): string {
  return text.replace(/[0-9]/g, (d) => String.fromCharCode(0x06f0 + Number(d)));
}

/** The API stores Rial; customers see Toman (Rial / 10), without the unit. */
export function toman(rial: number): string {
  return numberFormat.format(Math.floor(rial / 10));
}

export function jalaliDate(iso: string): string {
  return dateFormat.format(new Date(iso));
}

export function jalaliDateTime(iso: string): string {
  return dateTimeFormat.format(new Date(iso));
}

/** Persian names of variant attribute keys; unknown keys are shown as they are. */
const ATTRIBUTE_LABELS: Record<string, string> = {
  color: "رنگ",
  size: "سایز",
  material: "جنس",
  capacity: "ظرفیت",
  warranty: "گارانتی",
};

export function attributeLabel(key: string): string {
  return ATTRIBUTE_LABELS[key] ?? key;
}

export function attributesText(attributes: Record<string, string>): string {
  return Object.entries(attributes)
    .map(([key, value]) => `${attributeLabel(key)}: ${value}`)
    .join("، ");
}

export const ORDER_STATUS_LABELS = {
  PENDING_PAYMENT: "در انتظار پرداخت",
  PAID: "پرداخت‌شده",
  SHIPPED: "ارسال‌شده",
  DELIVERED: "تحویل‌شده",
  CANCELLED: "لغوشده",
  REFUNDED: "مرجوع‌شده",
} as const;

export const PAYMENT_STATUS_LABELS = {
  PENDING: "در جریان",
  SUCCEEDED: "موفق",
  FAILED: "ناموفق",
  REFUNDED: "بازگشت داده‌شده",
} as const;

export const REVIEW_STATUS_LABELS = {
  PENDING: "در انتظار تأیید",
  APPROVED: "تأییدشده",
  REJECTED: "ردشده",
} as const;

/** Iranian mobile in its canonical 09xxxxxxxxx form, or null. Mirrors the backend's rules. */
export function normalizeMobile(input: string): string | null {
  let digits = input
    .replace(/[۰-۹]/g, (d) => String(d.charCodeAt(0) - 0x06f0))
    .replace(/[٠-٩]/g, (d) => String(d.charCodeAt(0) - 0x0660))
    .replace(/[\s-]/g, "");
  if (digits.startsWith("+98")) digits = "0" + digits.slice(3);
  else if (digits.startsWith("0098")) digits = "0" + digits.slice(4);
  else if (digits.startsWith("98") && digits.length === 12) digits = "0" + digits.slice(2);
  else if (digits.startsWith("9") && digits.length === 10) digits = "0" + digits;
  return /^09\d{9}$/.test(digits) ? digits : null;
}
