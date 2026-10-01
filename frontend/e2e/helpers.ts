import { expect, type APIRequestContext, type Page } from "@playwright/test";

export const ADMIN_MOBILE = "09120000000";
export const ADMIN_STATE = "e2e/.auth/admin.json";

/** A mobile number no other test uses (the API limits login codes per number). */
export function newMobile(): string {
  return "0939" + String(Math.floor(Math.random() * 10_000_000)).padStart(7, "0");
}

export function toPersianDigits(text: string): string {
  return text.replace(/\d/g, (d) => String.fromCharCode(0x06f0 + Number(d)));
}

export function toAscii(text: string): string {
  return text.replace(/[۰-۹]/g, (d) => String(d.charCodeAt(0) - 0x06f0));
}

/**
 * Signs a new customer up through the login page, reading the code from the demo-mode banner.
 * New accounts are asked for their name before they continue.
 */
export async function signInWithUi(page: Page, mobile: string, name = { first: "سارا", last: "محمدی" }) {
  await page.getByLabel("شماره موبایل").fill(mobile);
  await page.getByRole("button", { name: "ادامه" }).click();
  const banner = page.getByText(/نسخه نمایشی/);
  await expect(banner).toBeVisible();
  const code = toAscii((await banner.textContent()) ?? "").match(/\d{6}/)![0];
  await page.getByLabel("کد تأیید").fill(code);   // six digits submit by themselves
  await expect(page.getByRole("heading", { name: "به کالامت خوش آمدید!" })).toBeVisible();
  await page.getByLabel("نام", { exact: true }).fill(name.first);
  await page.getByLabel("نام خانوادگی").fill(name.last);
  await page.getByRole("button", { name: "ذخیره و ادامه" }).click();
}

/** Signs in through the BFF (no UI); the cookies land in the request's browser context. */
export async function signInWithApi(request: APIRequestContext, mobile: string, name?: { first: string; last: string }) {
  const otp = await request.post("/bff/auth/otp", { data: { mobile } });
  expect(otp.ok(), await otp.text()).toBeTruthy();
  const { demoCode } = await otp.json();
  const verify = await request.post("/bff/auth/verify", { data: { mobile, code: demoCode } });
  expect(verify.ok(), await verify.text()).toBeTruthy();
  const body = await verify.json();
  expect(body.accessToken).toBeUndefined();   // tokens stay in httpOnly cookies
  if (name) {
    await bff(request, "PUT", "me", { firstName: name.first, lastName: name.last, email: null });
  }
  return body.user as { id: number; mobile: string };
}

export async function bff<T = unknown>(request: APIRequestContext, method: string, path: string, data?: unknown): Promise<T> {
  const response = await request.fetch(`/bff/${path}`, { method, data });
  expect(response.ok(), `${method} ${path}: ${response.status()} ${await response.text()}`).toBeTruthy();
  return (response.status() === 204 ? undefined : await response.json()) as T;
}

export async function variantId(request: APIRequestContext, slug: string, sku: string): Promise<number> {
  const product = await bff<{ variants: { id: number; sku: string }[] }>(request, "GET", `products/${slug}`);
  return product.variants.find((v) => v.sku === sku)!.id;
}

export const ADDRESS = {
  recipientName: "سارا محمدی",
  recipientMobile: "09121234567",
  provinceId: 8,
  city: "تهران",
  addressLine: "خیابان ولیعصر، کوچه نسترن",
  plaque: "12",
  unit: "3",
  postalCode: "1234567890",
  makeDefault: true,
};

/** A signed-in customer with one item in the cart and a default address, ready to check out. */
export async function customerWithCart(request: APIRequestContext, sku = "VLT-65W", slug = "voltra-usb-c-charger-65w") {
  const mobile = newMobile();
  await signInWithApi(request, mobile, { first: "رضا", last: "کریمی" });
  await bff(request, "POST", "cart/items", { variantId: await variantId(request, slug, sku), quantity: 1 });
  const address = await bff<{ id: number }>(request, "POST", "me/addresses", ADDRESS);
  return { mobile, addressId: address.id };
}

/** Places and pays an order through the API and the mock gateway; returns its number. */
export async function paidOrder(request: APIRequestContext): Promise<string> {
  const { addressId } = await customerWithCart(request);
  const order = await bff<{ orderNumber: string }>(request, "POST", "orders", { addressId });
  const pay = await bff<{ authority: string }>(request, "POST", `orders/${order.orderNumber}/pay`, { gateway: "MOCK" });
  const callback = await request.get(`/api/payments/callback?Authority=${pay.authority}&Status=OK`, { maxRedirects: 0 });
  expect(callback.status()).toBe(302);
  expect(callback.headers().location).toContain("status=success");
  return order.orderNumber;
}
