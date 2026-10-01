import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tests against a running stack (frontend + API + PostgreSQL with the demo catalog).
 * The API must run with OTP demo mode on and 09120000000 as an admin mobile; see e2e/README.md.
 */
export default defineConfig({
  testDir: "./e2e",
  outputDir: "./e2e/test-results",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"], ["html", { outputFolder: "./e2e/playwright-report", open: "never" }]],
  globalSetup: "./e2e/global-setup.ts",
  timeout: 60_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    locale: "fa-IR",
    timezoneId: "Asia/Tehran",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] }, grepInvert: /@mobile/ },
    { name: "mobile", use: { ...devices["Pixel 7"] }, grep: /@mobile/ },
  ],
});
