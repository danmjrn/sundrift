# NoirStay: from a booking exercise to a portfolio application

**Author:** Daniel Jr Nkulu · **Portfolio context:** Shape It Solutions.

The inherited `springboot-booking-system` was a Java 17/Spring Boot 3.0.2 exercise with
MySQL, JWT, room/guest concepts and an AWS storage adapter. It had no Next.js application.
Its availability query checked matching dates for the same guest, accepted booking entities
from requests, used floating-point money and did not enforce cancellation ownership.

This implementation preserves the original repository history while replacing those
foundations with a deliberate product: NoirStay, a premium black accommodation booking
experience for fictional South African stays. The work includes a new Next.js App Router
frontend, PostgreSQL migrations, session/CSRF authentication, original design/branding,
search, guest checkout/trips/cancellation, host listing/media/calendar management and
context-scoped administrator moderation with audit records.

The hardest decision was treating inventory as a database invariant. A shared table for
holds, bookings and host blocks makes a PostgreSQL range exclusion constraint cover every
source of occupancy. Per-listing row locks provide a consistent transition order; the
constraint remains independent protection. Checkout persists idempotency fingerprints and
responses, and records payment simulation plus confirmation in the same transaction.
Expiry explicitly releases inventory rather than relying on a changing time predicate.

A second decision was keeping portfolio access useful without giving anonymous visitors
shared administrative power. A session receives its own 24-stay synthetic context. Guest,
host and administrator views can show the same reservation while remaining isolated from
other visitors. PostgreSQL-backed sessions preserve that context across restarts. Normal
startup exposes no privileged fixtures or demo-entry controller.

The visual work is original: a restrained wordmark, black/charcoal surfaces, editorial
photography, concise booking copy, native date inputs and responsive booking controls.
Photographs are licensed illustrative assets; the application does not suggest those
properties are real bookable accommodation.

Correctness is demonstrated with real PostgreSQL Testcontainers tests, coordinated competing
transactions, policy/rounding examples, rejected cross-user/host requests, and browser
journeys through the real Spring API. The exact commands, results, screenshots and remaining
limitations are recorded in [VERIFICATION.md](VERIFICATION.md). No commercial customer counts,
production scale, revenue or unmeasured performance achievements are claimed.

Inherited work: original repository/history and the initial guest/room/booking problem domain.
New work: the delivered modules, schema, frontend, security/booking corrections, fixtures,
operations package, tests, documentation and design. No MySQL dataset was supplied, so this
case study does not claim migration of existing customer data.
