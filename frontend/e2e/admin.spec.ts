import { expect, test } from "@playwright/test";
import { ADMIN_STATE, bff, newMobile, paidOrder, signInWithApi, toPersianDigits } from "./helpers";

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

test.describe("admin catalog and users", () => {
  test.use({ storageState: ADMIN_STATE });

  test("categories and brands are created and removed", async ({ page }) => {
    const stamp = Date.now();
    await page.goto("/admin/categories");
    await page.getByRole("button", { name: "دسته‌بندی جدید" }).click();
    const dialog = page.getByRole("dialog", { name: "دسته‌بندی جدید" });
    await dialog.getByLabel("نام", { exact: true }).fill(`لوازم ورزشی ${stamp}`);
    await dialog.getByLabel("نامک").fill(`sports-${stamp}`);
    await dialog.getByLabel("والد").selectOption({ label: "کالای دیجیتال" });
    await dialog.getByRole("button", { name: "ذخیره" }).click();
    await expect(page.getByText("دسته‌بندی ذخیره شد")).toBeVisible();
    const row = page.locator("li", { hasText: `sports-${stamp}` });
    await expect(row).toBeVisible();

    page.once("dialog", (d) => d.accept());
    await page.locator("li", { hasText: "fashion" }).first().getByRole("button", { name: "حذف" }).click();
    await expect(page.getByText("این دسته‌بندی زیرمجموعه دارد و حذف نمی‌شود.")).toBeVisible();   // refused by the API
    page.once("dialog", (d) => d.accept());
    await row.getByRole("button", { name: "حذف" }).click();
    await expect(page.getByText("دسته‌بندی حذف شد")).toBeVisible();

    await page.goto("/admin/brands");
    await page.getByRole("button", { name: "برند جدید" }).click();
    const brandDialog = page.getByRole("dialog", { name: "برند جدید" });
    await brandDialog.getByLabel("نام", { exact: true }).fill(`نوآور ${stamp}`);
    await brandDialog.getByLabel("نامک").fill(`noavar-${stamp}`);
    await brandDialog.getByRole("button", { name: "ذخیره" }).click();
    const brandRow = page.locator("tr", { hasText: `noavar-${stamp}` });
    await expect(brandRow).toBeVisible();
    page.once("dialog", (d) => d.accept());
    await brandRow.getByRole("button", { name: "حذف" }).click();
    await expect(page.getByText("برند حذف شد")).toBeVisible();
    await expect(brandRow).toHaveCount(0);
  });

  test("stock edits are checked against the version the admin saw", async ({ page }) => {
    const slug = `e2e-stock-${Date.now()}`;
    const categories = await bff<{ slug: string; children: { id: number; slug: string }[] }[]>(page.request, "GET", "categories");
    const mugs = categories.flatMap((c) => c.children).find((c) => c.slug === "mugs")!;
    const product = await bff<{ id: number; variants: { id: number; version: number }[] }>(page.request, "POST", "admin/products", {
      product: { categoryId: mugs.id, name: "ماگ موجودی", slug },
      variants: [{ sku: slug.toUpperCase(), price: 1000000, stock: 10 }],
    });
    const variant = product.variants[0];

    await page.goto(`/admin/products/${product.id}`);
    await page.getByRole("button", { name: "ویرایش تنوع" }).click();
    const dialog = page.getByRole("dialog", { name: "ویرایش تنوع" });
    await dialog.getByLabel("موجودی").fill("25");
    // Someone else changes the variant while the form is open.
    await bff(page.request, "PUT", `admin/variants/${variant.id}`, { sku: slug.toUpperCase(), price: 1000000, stock: 9, version: variant.version });
    await dialog.getByRole("button", { name: "ذخیره تنوع" }).click();
    await expect(dialog.getByText(/پس از بارگذاری فرم تغییر کرده است/)).toBeVisible();

    await page.reload();
    await page.getByRole("button", { name: "ویرایش تنوع" }).click();
    await page.getByRole("dialog", { name: "ویرایش تنوع" }).getByLabel("موجودی").fill("25");
    await page.getByRole("dialog", { name: "ویرایش تنوع" }).getByRole("button", { name: "ذخیره تنوع" }).click();
    await expect(page.getByText("تنوع ذخیره شد")).toBeVisible();
    await expect(page.getByRole("cell", { name: "۲۵" })).toBeVisible();
  });

  test("a delivered order is refunded", async ({ page, browser }) => {
    const customer = await browser.newContext({ baseURL: test.info().project.use.baseURL });
    const orderNumber = await paidOrder(customer.request);
    await customer.close();

    await page.goto(`/admin/orders/${orderNumber}`);
    page.once("dialog", (d) => d.accept());
    await page.getByRole("button", { name: "ثبت بازگشت وجه" }).click();
    await expect(page.getByText("مرجوع‌شده").first()).toBeVisible();
    await expect(page.getByText("بازگشت داده‌شده")).toBeVisible();   // the payment too
    await expect(page.getByRole("button", { name: "ثبت ارسال" })).toHaveCount(0);
  });

  test("an admin disables a customer account", async ({ page, browser }) => {
    const mobile = newMobile();
    const customer = await browser.newContext({ baseURL: test.info().project.use.baseURL });
    await signInWithApi(customer.request, mobile, { first: "کامران", last: "یزدانی" });

    await page.goto("/admin/users");
    await page.getByRole("searchbox", { name: "جستجوی کاربر" }).fill(mobile);
    const row = page.locator("tr", { hasText: toPersianDigits(mobile) });   // the table shows Persian digits
    await expect(row).toBeVisible();
    page.once("dialog", (d) => d.accept());
    await row.getByRole("button", { name: "فعال" }).click();
    await expect(row.getByText("غیرفعال")).toBeVisible();

    // The customer's session ends: refreshing it no longer works.
    await customer.clearCookies({ name: "kl_at" });
    const session = await (await customer.request.get("/bff/session")).json();
    expect(session.user).toBeNull();
    await customer.close();
  });
});
