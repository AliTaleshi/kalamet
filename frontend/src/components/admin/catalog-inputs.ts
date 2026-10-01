// Conversions between admin form inputs and API values.

/** Admins type Toman; the API stores Rial. Accepts Persian digits and separators. */
export function tomanToRial(value: string): number | null {
  const digits = value.replace(/[۰-۹]/g, (d) => String(d.charCodeAt(0) - 0x06f0)).replace(/[^\d]/g, "");
  return digits ? Number(digits) * 10 : null;
}

export function rialToToman(rial: number | null | undefined): string {
  return rial == null ? "" : String(Math.floor(rial / 10));
}

/** ISO instant to a <input type="datetime-local"> value in the admin's own time zone. */
export function toLocalInput(iso: string | null): string {
  if (!iso) return "";
  const date = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function fromLocalInput(value: string): string | null {
  return value ? new Date(value).toISOString() : null;
}

/** Suggests a slug from the Latin product name ("Arian Zip Hoodie" -> "arian-zip-hoodie"). */
export function slugify(text: string): string {
  return text
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/[\s_]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}
