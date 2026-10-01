import { expect, test } from "@playwright/test";
import { ADMIN_STATE, bff, newMobile, paidOrder, signInWithApi } from "./helpers";

test("customers cannot open the admin panel", async ({ page }) => {
  await signInWithApi(page.request, newMobile());
  const response = await page.goto("/admin");
  expect(response?.status()).toBe(404);
  const api = await page.request.get("/bff/admin/orders");
  expect(api.status()).toBe(403);
});

test.describe("admin", () => {
  test.use({ storageState: ADMIN_STATE });

  test("dashboard shows the work to do", async ({ page }) => {
    await page.goto("/admin");
    await expect(page.getByRole("heading", { level: 1, name: "داشبورد" })).toBeVisible();
    await expect(page.getByText("سفارش‌های آماده ارسال")).toBeVisible();
    await expect(page.getByRole("heading", { name: "موجودی رو به اتمام" })).toBeVisible();
  });

  test("a new product appears in the store", async ({ page }) => {
    const slug = `e2e-mug-${Date.now()}`;
    await page.goto("/admin/products/new");
    await page.getByLabel("نام کالا").fill("ماگ آزمایشی سرامیکی");
    await page.getByLabel("نامک (در نشانی صفحه)").fill(slug);
    await page.getByLabel("دسته‌بندی").selectOption({ label: "— ماگ" });
    await page.getByLabel("کد انبار (SKU)").fill(slug.toUpperCase());
    await page.getByLabel("قیمت فروش (تومان)").fill("250000");
    await page.getByLabel("قیمت قبل از تخفیف (تومان)").fill("300000");
    await page.getByLabel("موجودی").fill("7");
    await page.getByRole("button", { name: "ثبت کالا" }).click();
    await expect(page.getByRole("heading", { level: 1, name: "ماگ آزمایشی سرامیکی" })).toBeVisible();

    await page.getByLabel("نشانی تصویر (URL)").fill("https://picsum.photos/seed/e2e-mug/800/800");
    await page.getByRole("button", { name: "افزودن", exact: true }).click();
    await expect(page.getByText("تصویر اضافه شد")).toBeVisible();

    await page.goto(`/product/${slug}`);
    await expect(page.getByRole("heading", { level: 1, name: "ماگ آزمایشی سرامیکی" })).toBeVisible();
    await expect(page.getByText("۲۵۰٬۰۰۰").first()).toBeVisible();
    await expect(page.getByText("۱۶٪").first()).toBeVisible();
  });

  test("an order is shipped and delivered", async ({ page, browser }) => {
    const customer = await browser.newContext({ baseURL: test.info().project.use.baseURL });
    const orderNumber = await paidOrder(customer.request);
    await customer.close();

    await page.goto(`/admin/orders?status=PAID`);
    await page.getByRole("link", { name: orderNumber }).click();
    await page.getByRole("button", { name: "ثبت ارسال" }).click();
    await expect(page.getByText("ارسال‌شده").first()).toBeVisible();
    await page.getByRole("button", { name: "ثبت تحویل" }).click();
    await expect(page.getByText("تحویل‌شده").first()).toBeVisible();
    await expect(page.getByRole("button", { name: "ثبت بازگشت وجه" })).toBeVisible();
  });

  test("an approved review shows on the product page", async ({ page, browser }) => {
    const customer = await browser.newContext({ baseURL: test.info().project.use.baseURL });
    await signInWithApi(customer.request, newMobile(), { first: "مینا", last: "صادقی" });
    const title = `کیفیت عالی ${Date.now()}`;
    await bff(customer.request, "POST", "products/koozegar-stoneware-mug/reviews", { rating: 5, title, comment: "رنگش خیلی قشنگ است.", recommended: true });
    await customer.close();

    await page.goto("/admin/reviews");
    const card = page.locator("section", { hasText: title });
    await card.getByRole("button", { name: "تأیید" }).click();
    await expect(page.getByText("دیدگاه تأیید شد")).toBeVisible();

    await page.goto("/product/koozegar-stoneware-mug");
    await expect(page.getByRole("heading", { name: title })).toBeVisible();
    await expect(page.getByText("مینا صادقی").first()).toBeVisible();
  });
});
