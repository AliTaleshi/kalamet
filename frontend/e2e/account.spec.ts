import { expect, test } from "@playwright/test";
import { bff, customerWithCart, newMobile, signInWithApi } from "./helpers";

test("private pages send visitors to the login page and back", async ({ page }) => {
  await page.goto("/profile/orders");
  await expect(page).toHaveURL(/\/login\?next=%2Fprofile%2Forders/);
});

test("the session survives an expired access token and ends on sign-out", async ({ page, context }) => {
  await signInWithApi(page.request, newMobile(), { first: "نگار", last: "احمدی" });
  await page.goto("/profile");
  await expect(page.getByLabel("نام", { exact: true })).toHaveValue("نگار");

  // Simulate the 15-minute access token running out: the proxy renews it from the refresh token.
  await context.clearCookies({ name: "kl_at" });
  await page.reload();
  await expect(page.getByLabel("نام", { exact: true })).toHaveValue("نگار");
  expect((await context.cookies()).some((c) => c.name === "kl_at" && c.httpOnly)).toBeTruthy();

  await page.getByRole("button", { name: "خروج" }).click();
  await expect(page.getByRole("link", { name: /ورود \| ثبت‌نام/ })).toBeVisible();
  await page.goto("/profile");
  await expect(page).toHaveURL(/\/login/);
});

test("a failed payment can be retried or the order cancelled", async ({ page }) => {
  await customerWithCart(page.request);
  await page.goto("/checkout");
  await page.getByRole("button", { name: "پرداخت", exact: true }).click();
  await page.getByRole("link", { name: "انصراف از پرداخت" }).click();

  await expect(page.getByRole("heading", { name: "پرداخت ناموفق بود" })).toBeVisible();
  await page.getByRole("link", { name: "تلاش دوباره برای پرداخت" }).click();
  await expect(page.getByText("در انتظار پرداخت").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "پرداخت سفارش" })).toBeVisible();
  await expect(page.getByText("ناموفق", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "لغو سفارش" }).click();
  await expect(page.getByText("لغوشده").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "پرداخت سفارش" })).toHaveCount(0);
});

test("addresses can be added, edited, made default and removed", async ({ page }) => {
  await signInWithApi(page.request, newMobile(), { first: "علی", last: "رضایی" });
  await bff(page.request, "POST", "me/addresses", {
    recipientName: "علی رضایی", recipientMobile: "09121112233", provinceId: 11, city: "مشهد",
    addressLine: "بلوار سجاد", plaque: "5", unit: null, postalCode: "9187654321", makeDefault: true,
  });
  await page.goto("/profile/addresses");
  await page.getByRole("button", { name: "ثبت نشانی جدید" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("نام و نام خانوادگی گیرنده").fill("علی رضایی");
  await dialog.getByLabel("شماره موبایل گیرنده").fill("09121112233");
  await dialog.getByLabel("استان").selectOption({ label: "اصفهان" });
  await dialog.getByLabel("شهر").fill("اصفهان");
  await dialog.getByLabel("نشانی پستی").fill("خیابان چهارباغ");
  await dialog.getByLabel("پلاک").fill("۱۲ الف");
  await dialog.getByLabel("کد پستی").fill("12345");
  await dialog.getByRole("button", { name: "ذخیره نشانی" }).click();
  await expect(dialog.getByText("کد پستی باید ۱۰ رقم باشد.")).toBeVisible();   // the API's message
  await dialog.getByLabel("کد پستی").fill("8173456789");
  await dialog.getByRole("button", { name: "ذخیره نشانی" }).click();
  await expect(page.getByText(/اصفهان، اصفهان، خیابان چهارباغ، پلاک ۱۲ الف/)).toBeVisible();

  const isfahan = page.locator("li", { hasText: "خیابان چهارباغ" });
  await isfahan.getByRole("button", { name: "انتخاب به‌عنوان پیش‌فرض" }).click();
  await expect(isfahan.getByText("پیش‌فرض", { exact: true })).toBeVisible();

  page.once("dialog", (d) => d.accept());
  await page.locator("li", { hasText: "بلوار سجاد" }).getByRole("button", { name: "حذف" }).click();
  await expect(page.getByText("بلوار سجاد")).toHaveCount(0);
});
