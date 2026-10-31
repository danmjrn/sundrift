# Verification evidence

Verified locally on 31 October 2026 against the real Compose application, Spring Boot
4.1.2 and PostgreSQL 18.6. Browser captures in [screenshots](screenshots/) are implemented
screens, not design mockups. The project is a synthetic portfolio demonstration. Review: [PR #1](https://github.com/danmjrn/noirstay/pull/1).

## Results

| Check | Result |
| --- | --- |
| Java 21 `./gradlew test bootJar --no-daemon` | 30 passed, zero failures/errors |
| `pnpm lint` | Passed |
| `pnpm typecheck` | Passed, strict TypeScript |
| `pnpm test` | 5 passed across 2 files |
| Multi-stage Compose backend/frontend production builds | Passed; all 4 services healthy |
| `pnpm test:e2e` | 9 passed; desktop/phone Chromium |
| Authenticated restart probe | Passed after real backend container restart; same session/reference/total |
| Remote GitHub Actions | All 3 jobs passed at `7230de2`: [run 41147486907](https://github.com/danmjrn/noirstay/actions/runs/41147486907) |

Backend tests comprise 23 reservation/security/database tests, 4 pricing/date tests and
3 normal-startup/API-security tests. Testcontainers runs actual PostgreSQL 18.6, including
Flyway and GiST exclusion constraints; no H2 approximation is used. Coordinated tasks use
latches/barriers rather than arbitrary sleeps. They cover overlapping/enclosing/adjacent
stays, independent guests, booking versus host block, confirmation versus expiry, a single
expiry cutoff, direct database rejection, same-key concurrent retries and conflicting
payloads, quote expiry, rounding/refund boundaries, changed policy snapshots, payment
retry/decline, ownership, context isolation, publication/capacity changes, uploads/removal,
review eligibility/uniqueness, CSRF, registration rollback, and login context restoration.

## Browser and visual method

Playwright uses one worker and fresh private demo contexts. It runs four journeys in
both desktop and phone Chromium: guest search/save/checkout/refresh/host inspection/
cancellation/review; host draft/photo upload/removal/submission/admin publication/audit;
protected routes/keyboard/error/empty states; and registration/account editing/logout/login.
The final browser suite targets application source `f7343b9` with the corrected tests
and screenshot-readiness refinements committed alongside this evidence. Independent CI
passed all three jobs at `7230de2`; the final evidence commit reruns the same workflow. Initial runs exposed assertions that matched a login query string before
login completed and audited metadata before client navigation settled. The final tests
wait for the actual destination pathname and non-empty page title; no accessibility rule
is disabled.

A separate responsive test checks 1440 × 1000, 768 × 1024 and 390 × 844 viewports, real image
loading, horizontal overflow, keyboard date-field traversal and gallery Escape handling.
It captures actual screenshots and asserts no uncaught browser errors or server failures.

Axe checks WCAG 2 A/AA and 2.1 A/AA on homepage, stay, checkout and trip views. Computed
component colours are measured for input text, placeholders, input/group boundaries,
disabled controls and focus outlines at all three widths. The 21 measurements are stored
in [contrast-results.json](contrast-results.json). All 21 pass: the lowest measured text
contrast is 6.38:1 and the lowest boundary/focus contrast is 3.65:1. These checks complement manual in-app
browser inspection; they do not claim a complete accessibility conformance audit or testing
on every browser/device. Browser automation is Chromium only.

The live OpenAPI export contains 29 paths, including cookie/CSRF requirements.
`/api/v1/docs` follows to Swagger UI with HTTP 200; [openapi.json](openapi.json) is the
actual exported contract. Final lint and strict type checks passed after test corrections.

## Persistence rehearsal

`scripts/restart-probe.py create` obtains its own isolated guest session, creates a quote,
hold and confirmed simulated reservation, and records its reference/total plus an ignored
local cookie jar under `.runtime/`. After a real backend container restart, `verify` uses
the same session and asserts the same confirmed booking, reference and agreed total. It
does not reseed, reset the database, create a new session or use another visitor's records.
The final rehearsal preserved confirmed reference `NS-A95C23D7` and its agreed total after
`docker compose restart backend`, readiness checks and verification with the same cookie jar.

## Baseline, limits and credentials

The inherited commit `c3ad278` compiled under isolated JDK 17/Gradle 7.6.6, with legacy
credentials replaced by placeholders. Its sole context-load test failed because no MySQL
service was supplied (connection refused). An earlier JDK 21 attempt failed in inherited
Lombok. No existing database was accessed and no MySQL data migration is claimed.

Local verification used host JDK 21 with Docker Desktop's normal Testcontainers client.
Automatic approval review rejected an earlier container command that mounted the host
Docker socket; that command was not run. Host-side testing resolved the verification gap.
Next builds use the supported Webpack option because a Turbopack worker could not bind a
port under this workstation's restrictions.

No authorized public deployment target was supplied. The running preview is local; the
portable Compose/TLS deployment, backup and rollback path is documented in DEPLOYMENT.md.
No performance/load benchmark, real payment, real email or production capacity is claimed.
Operator-managed demo-context/media retention and in-memory single-instance throttles are
explicit release tradeoffs. Previously committed database credentials, AWS credentials,
JWT signing material and legacy demo passwords require revocation/rotation wherever active;
values are not reproduced and unrelated infrastructure credentials were not changed.
