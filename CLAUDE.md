# Project context

Kalamet: portfolio e-commerce app for freelancing, styled after Digikala (original branding, not a copy).
Single store (no multi-vendor marketplace). Persian, RTL UI.

## Stack
- Backend: Java 21, Spring Boot 4.1.x, Spring Data JPA, Maven, package-by-feature
  (`user`, `catalog`, `cart`, `order`, `review`) under `com.kalamet`. Lives in `backend/`.
- Database: PostgreSQL 16 via the root `docker-compose.yml`. Flyway owns the schema; Hibernate `ddl-auto: validate`.
- Frontend (later, in `frontend/`): Next.js / React, RTL, Vazirmatn font, Persian digits, Jalali dates.

## Schema decisions (see backend/src/main/resources/db/migration)
- One Flyway migration per layer: V1 identity, V2 catalog, V3 cart, V4 orders/payments, V5 reviews.
  Demo data lives in `db/seed/R__demo_catalog.sql` (repeatable, idempotent, Persian content).
- Money: whole Rials as BIGINT everywhere (Java `long`). UI shows Toman = Rial / 10.
- Timestamps: TIMESTAMPTZ in UTC (Java `Instant`).
- Login: mobile number (`09xxxxxxxxx`) + one-time SMS code. No passwords. `otp_codes` stores a SHA-256
  hash of the code, an attempt counter and expiry. SMS provider (e.g. Kavenegar) not wired yet.
- Products hold shared info; `product_variants` are the sellable unit (sku, price, compare_at_price,
  discount_ends_at for "amazing offers", stock, `version` for optimistic locking,
  `attributes` JSONB with English keys and Persian values, e.g. {"color": "مشکی", "size": "M"}).
  A product with no options has one variant with `{}`.
- Cart and order items reference variants. Order items and orders snapshot names, prices, attributes
  and the shipping address so history never changes.
- Payments: one row per attempt, shaped for Zarinpal (authority -> redirect -> verify -> ref_id).
  Gateways: ZARINPAL, MOCK.
- Reviews: one per user per product, moderated (PENDING / APPROVED / REJECTED), optional `recommended`.
- The database enforces invariants with CHECK constraints (order total, line totals, one default
  address per user, discount cheaper than compare-at price). Keep entity code consistent with them.

## Conventions
- Enums stored as strings: `@Enumerated(EnumType.STRING)`.
- All `@ManyToOne` / `@OneToOne` are `FetchType.LAZY`. Entities never leave the service layer; use DTOs.
- Persian text must use Persian ی and ک (U+06CC, U+06A9), never Arabic ي / ك.
- Never edit a migration that has run; add `V6__...` and onward.

## Next steps
1. Verify the migrations run cleanly on PostgreSQL 16 (`docker compose up -d`, then `mvn spring-boot:run` in `backend/`).
2. JPA entities per layer (owner still to decide: Lombok or plain Java).
3. OTP auth + JWT, catalog read API, cart, checkout, Zarinpal/mock payment, reviews, admin endpoints.

## Git
- Conventional Commits (`feat(db): ...`, `chore(backend): ...`), one logical change per commit.
- Keep the working tree clean: no build output, no `.env`, LF line endings (see `.gitattributes`).
