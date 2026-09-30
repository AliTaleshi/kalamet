# Kalamet

A Persian (RTL) e-commerce store in the style of large Iranian marketplaces, built as a
full-stack portfolio project.

| Part | Stack | Status |
| --- | --- | --- |
| `backend/` | Java 21, Spring Boot 4, Spring Data JPA, Flyway, PostgreSQL 16 | Schema and seed data |
| `frontend/` | Next.js, React, RTL, Vazirmatn, Jalali dates | Planned |

## Run it locally

Requirements: Java 21, Maven, Docker.

```bash
docker compose up -d                 # PostgreSQL 16
cd backend && mvn spring-boot:run    # API; Flyway migrates on startup
```

Database defaults (override with environment variables): host `localhost`, port `5432`,
database / user / password `kalamet`.

## Database

Migrations live in `backend/src/main/resources/db/migration`, one file per domain layer:

| File | Layer | Tables |
| --- | --- | --- |
| `V1__identity.sql` | Identity | users, otp_codes, provinces, addresses |
| `V2__catalog.sql` | Catalog | categories, brands, products, product_specs, product_variants, product_images |
| `V3__cart.sql` | Shopping cart | carts, cart_items |
| `V4__orders.sql` | Orders and payments | orders, order_items, payments |
| `V5__engagement.sql` | Engagement | reviews |

Conventions: money is stored as whole Rials (`BIGINT`) and shown as Toman in the UI; timestamps
are UTC (`TIMESTAMPTZ`) and shown in the Jalali calendar; customers sign in with a mobile number
and a one-time SMS code, so there are no passwords.

`db/seed/R__demo_catalog.sql` loads a Persian demo catalog. Leave it out in production with
`SPRING_FLYWAY_LOCATIONS=classpath:db/migration`.

Never edit a migration that has already run; add `V6__...sql` and onward.
To reset the local database: `docker compose down -v && docker compose up -d`.
