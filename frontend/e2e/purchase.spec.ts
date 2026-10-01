import { expect, test } from "@playwright/test";
import { newMobile, signInWithUi } from "./helpers";

test("a guest builds a cart, signs up, checks out and pays", async ({ page }) => {
  // 1. Guest cart.
  await page.goto("/product/voltra-usb-c-charger-65w");
  await page.getByRole("button", { name: "افزودن به سبد خرید" }).click();
  await expect(page.getByText("به سبد خرید اضافه شد")).toBeVisible();
  await expect(page.getByRole("link", { name: "سبد خرید" }).first()).toContainText("۱");

  await page.goto("/cart");
  await expect(page.getByText("شارژر دیواری ولترا ۶۵ وات USB-C")).toBeVisible();
  await expect(page.getByText(/پس از ورود به حساب کاربری محاسبه می‌شود/)).toBeVisible();

  // 2. Sign up from the cart; the guest cart moves into the new account.
  await page.getByRole("link", { name: "ورود و ادامه خرید" }).click();
  await expect(page).toHaveURL(/\/login\?next=(%2F|\/)cart/);
  await signInWithUi(page, newMobile());
  await expect(page).toHaveURL(/\/cart$/);
  await expect(page.getByText("جمع سبد خرید")).toBeVisible();
  await expect(page.getByText("شارژر دیواری ولترا ۶۵ وات USB-C")).toBeVisible();

  // 3. Checkout with a new address.
  await page.getByRole("link", { name: "تأیید و تکمیل سفارش" }).click();
  await expect(page).toHaveURL(/\/checkout$/);
  await page.getByRole("button", { name: "افزودن نشانی" }).click();
  const dialog = page.getByRole("dialog", { name: "افزودن نشانی جدید" });
  await dialog.getByLabel("نام و نام خانوادگی گیرنده").fill("سارا محمدی");
  await dialog.getByLabel("شماره موبایل گیرنده").fill("۰۹۱۲۱۲۳۴۵۶۷");
  await dialog.getByLabel("استان").selectOption({ label: "تهران" });
  await dialog.getByLabel("شهر").fill("تهران");
  await dialog.getByLabel("نشانی پستی").fill("خیابان ولیعصر، کوچه نسترن");
  await dialog.getByLabel("پلاک").fill("۱۲");
  await dialog.getByLabel("کد پستی").fill("۱۲۳۴۵۶۷۸۹۰");
  await dialog.getByRole("button", { name: "ذخیره نشانی" }).click();
  await expect(page.getByRole("radio")).toBeChecked();

  // 4. Pay with the mock gateway.
  await page.getByRole("button", { name: "پرداخت", exact: true }).click();
  await expect(page).toHaveURL(/\/api\/payments\/mock\/MOCK-/);
  await expect(page.getByRole("heading", { name: "درگاه پرداخت آزمایشی" })).toBeVisible();
  await page.getByRole("link", { name: "پرداخت موفق" }).click();

  // 5. Back on the store: success with order number and tracking code.
  await expect(page).toHaveURL(/\/checkout\/result\?status=success/);
  await expect(page.getByRole("heading", { name: "سفارش شما با موفقیت ثبت شد" })).toBeVisible();
  await expect(page.getByText("کد پیگیری")).toBeVisible();
  await page.getByRole("link", { name: "مشاهده سفارش" }).click();
  await expect(page.getByText("پرداخت‌شده").first()).toBeVisible();
  await expect(page.getByText("موفق", { exact: true })).toBeVisible();

  // The cart is empty again.
  await page.goto("/cart");
  await expect(page.getByText("سبد خرید شما خالی است!")).toBeVisible();
});
