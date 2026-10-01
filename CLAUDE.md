# Project context

Kalamet: portfolio e-commerce app for freelancing, styled after Digikala (original branding, not a copy).
Single store (no multi-vendor marketplace). Persian, RTL UI.

## Stack
- Backend: Java 21, Spring Boot 4.1.x (Spring Framework 7, Hibernate 7, Jackson 3), Spring Data JPA,
  Spring Security resource server (HS256 JWT), Lombok, springdoc, Maven wrapper (`backend/mvnw`).
  Package-by-feature under `com.kalamet`: `user`, `catalog`, `cart`, `order`, `review`, plus
  `common` (`error`, `web` for paging and the current user, `text` for Persian helpers) and `config`.
  Lives in `backend/`.
- Every feature has the same sub-packages: `domain` (entities, enums, domain rules like `PriceQuote`),
  `repository` (Spring Data, plus SQL read models like `ProductSearch`), `service`, `web`
  (controllers; admin endpoints in separate `Admin*Controller`s) and `dto` (records grouped in
  `*Dtos` holders). Integrations get their own: `user/sms`, `order/gateway`.
- Dependencies point inwards: web -> service -> repository -> domain; dto may use domain. A feature
  uses another feature's services, never its repositories (`CatalogService` exposes variants,
  images and active products to cart, order and review; `OrderService.hasReceivedProduct` serves
  reviews). Unit tests sit next to their class's package; API tests stay at the feature root.
- Database: PostgreSQL 16 via the root `docker-compose.yml`. Flyway owns the schema; Hibernate `ddl-auto: validate`.
- Frontend in `frontend/`: Next.js 16 (App Router, `proxy.ts` instead of middleware), React 19,
  TypeScript, Tailwind CSS 4 (brand tokens in `globals.css`: teal `brand-*`, amber `accent-*`),
  TanStack Query, lucide icons, self-hosted Vazirmatn. Node runs only in Docker (`node:22-alpine`);
  the host has Node 18. Read `frontend/AGENTS.md`: this Next.js version differs from older ones.

## Frontend architecture
- `app/(store)/` is the shop and customer account (server-rendered catalog pages, `force-dynamic`
  so builds never need the API); `app/admin/` is the panel (layout checks the role server-side).
- Browser code never sees tokens. `app/bff/[...path]` forwards `/bff/x` to `API_URL/api/x` with the
  access token from the httpOnly `kl_at` cookie, refreshes from `kl_rt` (one shared refresh per
  token, `lib/auth/refresh.ts`), turns sign-in responses into cookies, checks Origin on writes and
  forwards client IP and host. `/bff/session` is `me` that answers `{user: null}` when signed out.
- `proxy.ts` renews expired access tokens before pages render and redirects signed-out visitors
  away from `/profile`, `/checkout` and `/admin`.
- `app/api/payments/[...path]` serves the API's payment callback and mock gateway on this origin;
  the API builds those URLs from the forwarded host, so `PAYMENT_CALLBACK_URL` can stay empty.
- Server components call the API with `lib/api/server.ts`; client components with `lib/api/client.ts`
  (`api()` throws `ApiError` whose message is the API's Persian `detail`).
- Cart: `lib/hooks/cart.ts` hides the difference between the server cart and the guest cart in
  localStorage, which is merged on sign-in (`mergeGuestCart`).
- Formatting only through `lib/format.ts` (Toman from Rial, Persian digits, Jalali dates in the
  Asia/Tehran zone so server and client render the same text). Product images are plain `<img>`
  (admin URLs from any host; `next/image` would make the server an open image proxy).
- React 19 lint rules are on: no `setState` directly in effects (derive state, or remount with `key`).
- Checks: `npx tsc --noEmit`, `npx eslint .`, `npx next build`, and Playwright e2e (`frontend/e2e`).

## Schema decisions (see backend/src/main/resources/db/migration and docs/database.md)
- One Flyway migration per layer: V1 identity, V2 catalog, V3 cart, V4 orders/payments, V5 reviews,
  then V6 refresh tokens. Demo data lives in `db/seed/R__demo_catalog.sql` (repeatable, idempotent,
  Persian content; left out by the prod profile).
- Money: whole Rials as BIGINT everywhere (Java `long`). UI shows Toman = Rial / 10.
- Timestamps: TIMESTAMPTZ in UTC (Java `Instant`). Business time comes from the injected `Clock`.
- Login: mobile number (`09xxxxxxxxx`) + one-time SMS code. No passwords. `otp_codes` stores an
  HMAC-SHA256 of the code (keyed by `OTP_HASH_SECRET`), an attempt counter and expiry.
- Products hold shared info; `product_variants` are the sellable unit (sku, price, compare_at_price,
  discount_ends_at for "amazing offers", stock, `version` for optimistic locking,
  `attributes` JSONB with English keys and Persian values, e.g. {"color": "مشکی", "size": "M"}).
  A product with no options has one variant with `{}`.
- Pricing rule (`catalog.domain.PriceQuote`, repeated in SQL in `catalog.repository.ProductSearch`): after
  `discount_ends_at` passes, the variant sells at `compare_at_price` with no discount shown.
- Cart and order items reference variants. Order items and orders snapshot names, prices, attributes
  and the shipping address so history never changes.
- Payments: one row per attempt, shaped for Zarinpal (authority -> redirect -> verify -> ref_id).
  Gateways: ZARINPAL, MOCK.
- Reviews: one per user per product, moderated (PENDING / APPROVED / REJECTED), optional `recommended`.
- The database enforces invariants with CHECK constraints (order total, line totals, one default
  address per user, discount cheaper than compare-at price). Keep entity code consistent with them.

## Owner decisions (2026-10-01)
- Lombok on entities (`@Getter`, `@Setter` where needed, never `@Data`); Java records for all DTOs.
- SMS: Kavenegar "verify lookup" behind the `SmsSender` interface; `LOG` provider in development.
  Limits: one code per 2 minutes, 5 per hour per number, 5 wrong attempts per code, 2-minute expiry.
- JWT: 15-minute access token; 30-day refresh token stored hashed in `refresh_tokens`, rotated on
  every use, with reuse detection (reusing a revoked token revokes all of the user's tokens).
- Shipping: flat fee, free from a threshold (60,000 / 1,000,000 Toman by default, in application.yml).

## Behaviour worth knowing
- Checkout reserves stock (decrements it). Unpaid orders are cancelled after 30 minutes by
  `OrderExpiryJob` unless a payment attempt from the last 15 minutes is still open. Stock goes
  back when an unshipped order is cancelled or refunded.
- Concurrency: `hibernate.order_updates` makes flushes lock rows in id order (no deadlocks between
  checkouts); version conflicts, lock failures and "same user, two requests" unique violations
  all become 409 `CONCURRENT_UPDATE`. OTP issuing takes a per-number advisory lock.
- OTP abuse limits: per number (resend interval, hourly cap, attempts) and per client IP
  (`OtpIpLimiter`, in memory, per instance). Behind a proxy the IP comes from `X-Forwarded-For`,
  so the proxy must set it and clients must not reach the app directly.
- Refresh-token reuse within 30 s of rotation is a benign race (401 only); later reuse revokes all.
- Every order status change locks the order row first (`OrderRepository.lockById` /
  `lockByOrderNumber`), then touches payments. Keep that order to avoid deadlocks.
- The payment callback never verifies twice and never verifies for an order that is no longer
  PENDING_PAYMENT; unverified gateway payments are reversed by the gateway.
- Admins: mobiles in `ADMIN_MOBILES` get the ADMIN role on sign-in.
- Refunds are recorded only (status + payment REFUNDED); money is not returned through the API.
- Product images are URLs (no upload endpoint yet).

## Conventions
- Enums stored as strings: `@Enumerated(EnumType.STRING)`.
- All `@ManyToOne` / `@OneToOne` are `FetchType.LAZY`. Entities never leave the service layer; use DTOs.
  `open-in-view` is off, so services build DTOs inside their transaction.
- Errors: throw `ApiException` with a stable English `code` and a Persian message; the
  `GlobalExceptionHandler` turns everything into problem+json. Map new unique/FK constraints users
  can hit in `CONSTRAINT_MESSAGES`.
- Persian text must use Persian ی and ک (U+06CC, U+06A9), never Arabic ي / ك. Run user and admin
  input through `PersianText.normalize`; mobiles through `MobileNumber.normalize`; numbers inside
  Persian messages through `PersianText.digits`. In Java sources, write Arabic letters and the ZWNJ
  only as `\u` escapes.
- Never edit a migration that has run; add `V7__...` and onward.
- Tests: `./mvnw test` needs Docker (Testcontainers starts postgres:16). API tests extend
  `IntegrationTest`, which turns on OTP demo mode so sign-in reads the code from the response. Use
  `newMobile()` per test because of the OTP rate limits.

## Next steps
- Image upload (S3-compatible storage) instead of image URLs, a shared (Redis) OTP IP limit for
  several instances, pg_trgm search indexes, CI/CD (postponed by the owner).

## Changing the schema
- Any schema change is a new migration (`V7__...`), and `docs/database.md` is updated in the same commit.

## Git
- Conventional Commits (`feat(db): ...`, `chore(backend): ...`), one logical change per commit.
- Keep the working tree clean: no build output, no `.env`, LF line endings (see `.gitattributes`).
