import { expect, test } from "@playwright/test";

test.describe("storefront", () => {
  test("home page shows offers and categories", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle(/کالامت/);
    await expect(page.getByRole("heading", { name: "شگفت‌انگیزها" })).toBeVisible();
    await expect(page.getByRole("heading", { name: "خرید بر اساس دسته‌بندی" })).toBeVisible();
    await page.getByRole("link", { name: "مشاهده پیشنهادها" }).click();
    await expect(page.getByRole("heading", { level: 1, name: "پیشنهادهای شگفت‌انگیز" })).toBeVisible();
    await expect(page.getByRole("link", { name: /هدفون بی‌سیم آوا صدا/ })).toBeVisible();
  });

  test("search finds Persian words regardless of half-spaces", async ({ page }) => {
    await page.goto("/");
    const search = page.getByRole("searchbox", { name: "جستجو" });
    await search.fill("تی شرت");
    await search.press("Enter");
    await expect(page).toHaveURL(/\/search\?q=/);
    await expect(page.getByRole("link", { name: /تی‌شرت نخی آستین کوتاه آرین/ })).toBeVisible();
  });

  test("category listing filters by brand and sorts", async ({ page }) => {
    await page.goto("/category/fashion");
    await expect(page.getByRole("heading", { level: 1, name: "مد و پوشاک" })).toBeVisible();
    await expect(page.getByRole("link", { name: /هودی زیپ‌دار آرین/ })).toBeVisible();

    await page.getByRole("button", { name: "ارزان‌ترین" }).click();
    await expect(page).toHaveURL(/sort=cheapest/);
    await expect(page.getByRole("button", { name: "ارزان‌ترین" })).toHaveAttribute("aria-pressed", "true");
    // The cheapest fashion item (the tee) comes first.
    await expect(page.locator("main ul li a").first()).toContainText("تی‌شرت");

    await page.getByRole("checkbox", { name: /آرین/ }).check();
    await expect(page).toHaveURL(/brand=arian/);
  });

  test("product page picks variants and shows prices in Toman", async ({ page }) => {
    await page.goto("/product/arian-essential-cotton-tee");
    await expect(page.getByRole("heading", { level: 1, name: "تی‌شرت نخی آستین کوتاه آرین" })).toBeVisible();
    await page.getByRole("button", { name: "مشکی" }).click();
    await page.getByRole("button", { name: "S", exact: true }).click();
    await expect(page.getByRole("button", { name: "مشکی" })).toHaveAttribute("aria-pressed", "true");
    await expect(page.getByRole("button", { name: "S", exact: true })).toHaveAttribute("aria-pressed", "true");
    await expect(page.getByText("۱٬۲۹۰٬۰۰۰").first()).toBeVisible();   // 12,900,000 Rial
    await expect(page.getByRole("heading", { name: "مشخصات", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "افزودن به سبد خرید" })).toBeEnabled();
  });

  test("unknown pages get the Persian 404", async ({ page }) => {
    const response = await page.goto("/product/no-such-product");
    expect(response?.status()).toBe(404);
    await expect(page.getByText("صفحه‌ای که دنبال آن بودید پیدا نشد!")).toBeVisible();
  });

  test("security headers are set", async ({ request }) => {
    const response = await request.get("/");
    expect(response.headers()["x-frame-options"]).toBe("DENY");
    expect(response.headers()["x-content-type-options"]).toBe("nosniff");
    expect(response.headers()["x-powered-by"]).toBeUndefined();
  });
});

test("mobile visitors browse categories from the header @mobile", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: "دسته‌بندی کالاها" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "دسته‌بندی کالاها" })).toBeVisible();
  await page.getByRole("link", { name: "هدفون", exact: true }).click();
  await expect(page.getByRole("heading", { level: 1, name: "هدفون" })).toBeVisible();
  // The filters live behind a button on small screens.
  await page.getByRole("button", { name: "فیلترها" }).click();
  await expect(page.getByRole("dialog", { name: "فیلترها" })).toBeVisible();
});

test.describe("listing filters and guest cart", () => {
  test("a price range keeps only products inside it", async ({ page }) => {
    await page.goto("/search");
    await page.getByLabel("کمترین قیمت (تومان)").fill("۱۰۰۰۰۰۰");
    await page.getByLabel("بیشترین قیمت (تومان)").fill("2000000");
    await page.getByRole("button", { name: "اعمال محدوده قیمت" }).click();
    await expect(page).toHaveURL(/minPrice=1000000&maxPrice=2000000/);
    await expect(page.getByRole("link", { name: /شارژر دیواری ولترا/ })).toBeVisible();   // 1,850,000 Toman
    await expect(page.getByRole("link", { name: /هدفون بی‌سیم/ })).toHaveCount(0);       // 8,900,000 Toman
  });

  test("a guest changes quantities and empties the cart", async ({ page }) => {
    await page.goto("/product/koozegar-stoneware-mug");
    await page.getByRole("button", { name: "افزودن به سبد خرید" }).click();
    await expect(page.getByText("به سبد خرید اضافه شد")).toBeVisible();
    await page.getByRole("link", { name: "مشاهده سبد خرید" }).click();

    await page.getByRole("button", { name: "افزایش تعداد" }).click();
    await expect(page.getByText("۲ کالا")).toBeVisible();
    await page.getByRole("button", { name: "کاهش تعداد" }).click();
    await page.getByRole("button", { name: "حذف از سبد" }).click();
    await expect(page.getByText("سبد خرید شما خالی است!")).toBeVisible();
  });
});
