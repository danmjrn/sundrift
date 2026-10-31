# Sundrift

**Somewhere to slow down.**

Sundrift helps travellers discover South African stays, check available dates and reserve
with clear prices. It brings trips, cancellations, saved stays and guest reviews together
with host listing management and administrator publication in one accommodation system.

## Features

- **Stay discovery:** Search by destination, dates and guests. Refine by price, type,
  amenities and rating, with sorting, shareable URL filters and pagination.
- **Considered stay pages:** Explore photographs, amenities, sleeping arrangements,
  approximate location, house rules, fictional reviews and an availability calendar.
- **Clear reservations:** Receive server-calculated nightly rates, cleaning/service fees
  and demo VAT. A ten-minute hold protects selected dates while simulated checkout completes.
- **Trips and flexibility:** Keep a confirmation reference, reopen persisted trip details,
  preview policy-based refunds, cancel and release dates. Completed stays accept one review.
- **Your account:** Register, sign in, edit account details and save favourite stays using
  HttpOnly sessions and CSRF-protected requests.
- **Host studio:** Create and edit listings, upload/remove photographs, submit for
  publication, block/unblock dates and inspect reservations and booking-derived totals.
- **Publication desk:** Review, approve or unpublish stays and inspect context-scoped users,
  bookings, simulated payment attempts and administrative audit records.
- **Private demo perspectives:** Enter as guest, host or administrator without shared
  passwords. Each visitor has a separate synthetic context with 24 fictional stays.
- **Reservations that hold up:** PostgreSQL protects holds, confirmed stays and host blocks
  with one exclusion constraint. Transactional settlement, expiry, cancellation and persisted
  idempotency protect retries and competing requests.

![Sundrift running application](docs/screenshots/home-desktop-viewport.png)

[Stay on phone](docs/screenshots/stay-phone.png) · [Checkout](docs/screenshots/checkout-desktop.png) ·
[Host studio](docs/screenshots/host-desktop.png) · [Publication desk](docs/screenshots/admin-desktop.png)

## Requirements

**Docker with Compose** runs the complete system. Native development uses Java **21 LTS**,
Gradle **8.14.3**, PostgreSQL **18.6**, Node **24** and pnpm **11.12.0**.

The application uses Spring Boot **4.1.2**, Next.js **16.4.3**, React **19.3.1**, strict
TypeScript **6.0.3** and Tailwind **4.3.3**. [Version references](docs/TOOLCHAIN.md) record the pins.

## Settings and Configuration

Copy `.env.example` to an untracked `.env` and set a unique database password. The explicit
`demo` profile enables fictional fixtures and visitor-specific entry points. Normal startup
creates no demo users or privileged demo credentials.

The public origin serves Next.js and routes `/api/v1` to Spring. PostgreSQL remains internal;
local media and database volumes persist across container replacement. Production uses a
TLS edge and Secure cookies. [Operations](docs/DEPLOYMENT.md) covers environment settings,
migrations, backups, deployment and rollback.

## Known Issues and Scope

- One listing is one independently reservable unit. There is no pooled inventory or
  multi-room checkout. Rates are flat per night and currency is ZAR; no conversion is claimed.
- Stays, hosts and seeded reviews are fictional. Payments and email are simulated. No card
  data is collected and no money is charged or booking email sent.
- Demo-context/media retention is operator-managed. Login/context throttles are in memory;
  this release targets one application instance.
- Existing MySQL data was not supplied or migrated. Historical bookings survive unpublication.
- Browser automation uses Chromium. It complements manual responsive inspection and does
  not claim a complete accessibility conformance audit or measured production capacity.
- Rotate any active database, AWS, JWT or demo-account credentials from the inherited project.
  Their values are not reproduced, and Git history is preserved.

## Release Notes

### Sundrift launch — 31 October 2026

- Renamed the existing NoirStay repository and application to Sundrift, preserving history.
- Introduced sand, oat, clay and cocoa colours, a sun/horizon wordmark and warm editorial styling.
- Adopted the feature-first README structure used by the
  [Reverse Engineering & Documentation Plugin](https://github.com/danmjrn/ReverseEngineeringDocPlugin).
- Preserved booking rules and existing local data; an additive migration updates fictional
  listing copy without changing applied Flyway migrations.

[Verification](docs/VERIFICATION.md) distinguishes the earlier application evidence from
fresh Sundrift launch checks. [Rebrand notes](docs/REBRAND.md) explain storage compatibility.

## Running the Project

```sh
git clone --branch feat/noirstay-portfolio git@github.com:danmjrn/sundrift.git
cd sundrift
cp .env.example .env
# Replace DATABASE_PASSWORD with a unique local password.
docker compose up --build -d --wait
```

Open [localhost:3000](http://localhost:3000). Choose **Portfolio demo → Demo guest**, **Demo
host** or **Demo admin**. Switching roles retains your visitor context; the host can see the
guest reservation. Account → Restore my demo creates a fresh private context.

Target hosted address: [sundrift.shapeit.solutions](https://sundrift.shapeit.solutions).
Deployment is recorded only after DNS, TLS, rendering and persistent journeys verify.

For verification:

```sh
cd backend
./gradlew test bootJar --no-daemon
cd ../frontend
pnpm install --frozen-lockfile
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm exec playwright install chromium
pnpm test:e2e
```

## For More Information

- [Five-minute walkthrough](docs/WALKTHROUGH.md), [API examples](docs/API.md) and
  [live OpenAPI](http://localhost:3000/api/v1/openapi).
- [Architecture and entity relationship diagram](docs/ARCHITECTURE.md), including the
  booking lifecycle, pricing, concurrency, cancellation and isolation rules.
- [Portfolio case study](docs/CASE_STUDY.md), [source audit](docs/IMPLEMENTATION.md) and
  [GitHub verification](docs/GITHUB.md).
- [Asset attribution](docs/ASSETS.md) and [inherited notices](NOTICE).
- [Reusable README structure](docs/README_STANDARD.md).

Created by **Daniel Jr Nkulu / Shape It Solutions**. Photographs illustrate fictional stays.
