# End-to-end tests

Playwright drives a real browser through the whole stack: Next.js frontend, BFF, Spring Boot API
and PostgreSQL. The tests need:

- the demo catalog (a fresh database is best: tests add orders, products and reviews),
- the API with `OTP_DEMO_MODE=true` (the login page then shows the code) and `09120000000` in
  `ADMIN_MOBILES`,
- `FRONTEND_URL` on the API equal to the address the browser uses (payment redirects go there),
- for repeated runs, a higher per-IP login-code limit (`KALAMET_OTP_MAXPERIP=1000`): every test
  browser shares one IP, and the default allows 20 codes per 10 minutes.

With the full stack from the root `docker-compose.yml` (`docker compose --profile app up --build`),
run the tests with the Playwright image, so no browser has to be installed:

```bash
docker run --rm --network host -v "$PWD/frontend:/app" -w /app -e E2E_BASE_URL=http://localhost:3000 \
  mcr.microsoft.com/playwright:v1.63.0-noble npx playwright test
```

The HTML report is written to `frontend/e2e/playwright-report/`. Tests tagged `@mobile` run on a
phone-sized viewport; everything else on desktop Chrome.
