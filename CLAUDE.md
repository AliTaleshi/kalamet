# Project context

Kalamet: portfolio e-commerce app for freelancing, styled after Digikala (original branding, not a copy).
Single store (no multi-vendor marketplace). Persian, RTL UI.

## Stack
- Backend: Java 21, Spring Boot 4.1.x (Spring Framework 7, Hibernate 7, Jackson 3), Spring Data JPA,
  Spring Security resource server (HS256 JWT), Lombok, springdoc, Maven wrapper (`backend/mvnw`).
  Package-by-feature under `com.kalamet`: `user`, `catalog`, `cart`, `order`, `review`, plus
  `common` (errors, paging, Persian text helpers) and `config`. Lives in `backend/`.
- Database: PostgreSQL 16 via the root `docker-compose.yml`. Flyway owns the schema; Hibernate `ddl-auto: validate`.
- Frontend (next, in `frontend/`): Next.js / React, RTL, Vazirmatn font, Persian digits, Jalali dates.
  The API it will call is documented in `docs/api.md`.

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
- Pricing rule (`catalog.PriceQuote`, repeated in SQL in `catalog.ProductSearch`): after
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
1. Frontend in `frontend/` (Next.js, RTL, Persian) against `docs/api.md`.
2. Optional backend extras: image upload (S3-compatible storage), a shared (Redis) OTP IP limit
   for multiple instances, a Dockerfile for deployment, pg_trgm search indexes.

## Changing the schema
- Any schema change is a new migration (`V7__...`), and `docs/database.md` is updated in the same commit.

## Git
- Conventional Commits (`feat(db): ...`, `chore(backend): ...`), one logical change per commit.
- Keep the working tree clean: no build output, no `.env`, LF line endings (see `.gitattributes`).
