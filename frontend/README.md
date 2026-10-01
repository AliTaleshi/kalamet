# Kalamet frontend

Persian (RTL) storefront, customer account and admin panel for the Kalamet API. Next.js 16 (App
Router), React 19, TypeScript, Tailwind CSS 4, TanStack Query.

## Run

Node.js does not have to be installed: everything runs in the `node:22-alpine` image. From the
repository root:

```bash
docker compose --profile dev up frontend-dev    # dev server on http://localhost:3000, API expected on :8080
docker compose --profile app up --build         # production build of the whole stack
```

| Variable | Default | Purpose |
| --- | --- | --- |
| `API_URL` | `http://localhost:8080` | The Spring Boot API, as the Next.js server reaches it. |
| `COOKIE_SECURE` | on in production | Set `false` only for plain-http local runs. |

## Layout

| Path | What it is |
| --- | --- |
| `src/app/(store)` | Store pages (home, category, search, product, cart, checkout) and `/profile` |
| `src/app/admin` | Admin panel (admins only) |
| `src/app/bff/[...path]` | Backend-for-frontend: forwards `/bff/x` to the API with the session cookies |
| `src/app/api/payments/[...path]` | The API's payment callback and mock gateway, served on this origin |
| `src/proxy.ts` | Renews expired sessions and guards private pages |
| `src/lib` | API clients, types, formatting (Toman, Persian digits, Jalali dates), hooks |
| `src/components` | `ui` primitives, `store`, `product`, `cart`, `account`, `admin` |
| `e2e` | Playwright end-to-end tests (see `e2e/README.md`) |

## Checks

```bash
npx tsc --noEmit && npx eslint . && npx next build
```
