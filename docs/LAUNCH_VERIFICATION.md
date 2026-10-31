# Sundrift launch verification — 31 October 2026

Live application: **https://sundrift.shapeit.solutions/**.
Featured project: **https://danieljrnkulu.shapeit.solutions/work/sundrift**.

## Exact release and results

The VPS runs source `d0bc85ed7e0b16eaf526c8ebe25171dec80105a4`, built from its exact
pushed Git archive. Backend/frontend images are tagged by that full revision. Runtime
configuration is generated on the host, mode 0600, and encrypted to distinct host and
owner-recovery age recipients. No deployment credential is distributed through GitHub CI.

| Verification | Result |
| --- | --- |
| Backend PostgreSQL/Testcontainers tests | 30 passed in CI |
| Frontend unit tests | 5 passed |
| Lint and strict TypeScript | Passed |
| Java jar and Next.js production builds | Passed in CI and on the VPS |
| Actual HTTPS browser journeys | **11 passed**, desktop and phone Chromium, no retries |
| Responsive screenshots | 1440 × 1000, 768 × 1024, 390 × 844 |
| Axe and computed component contrast | Passed; 21 measurements in contrast-results.json |
| Public DNS/TLS and real APIs | Verified; exported OpenAPI has 29 paths, Swagger UI responds HTTP 200 |
| Deployment services | Database, backend, frontend and edge healthy |
| Session/booking persistence | Same authenticated confirmed `NS-EB31C8AE`, reference and total survived container replacement |
| Encrypted database/media recovery | Restored into an isolated PostgreSQL instance; confirmed reference and 4 referenced photographs verified |
| Existing sites | 8 peer Nginx configurations unchanged; public/sign-in pages and protected gates still responded |

Independent CI for the exact application fix passed in
[run 41152908384](https://github.com/danmjrn/sundrift/actions/runs/41152908384) and
[run 41152911073](https://github.com/danmjrn/sundrift/actions/runs/41152911073).

The live test command was `PREVIEW_URL=https://sundrift.shapeit.solutions pnpm test:e2e`.
It exercises discovery, wishlist persistence, checkout, confirmation refresh, host reservation
inspection, cancellation, completed-stay review, photo upload/removal, publication/audit,
registration/account editing/login, protected/error/empty states and keyboard/responsive UI.

## A failure that improved the release

The first live pass found a phone save action could precede account loading and redirect an
authenticated guest to login. A controlled delayed `/auth/me` regression failed against the
old release. Save controls now remain disabled until identity and saved stays finish loading.
That regression passes on desktop and phone in the final 11-test suite. This was fixed in
application behavior; the failing assertion was retained.

The screenshot check waits for visible photographs to decode. Captures under screenshots/
are the real deployed nude-themed application, including fully loaded images.

## Publication and recovery

Only `sundrift.shapeit.solutions` was added to DNS, with an A record to the existing VPS and
TTL 300. Only its own Nginx site/certificate was added under both established host locks.
App ingress is loopback-only on port 3110; PostgreSQL has no public port. The inner proxy
trusts forwarded headers only in the production overlay behind that host edge.

Sundrift was published first in Selected work through the existing owner editor API:
publication version 4, four projects total. The prior three project objects are preserved.
There were no unpublished owner edits at publication. The case study, real screenshot preview,
HTTPS project link and public repository link were verified in the rendered site.

Backup `20261031T010959Z` was authenticated/decrypted and restored into a disposable,
network-isolated PostgreSQL container using the same database image. Its confirmed booking
and referenced media were verified; the labelled rehearsal container and scratch files were
removed. Applied migrations were not reversed or existing app data reset. Daily backup unit
files and an explicit code/image rollback action are included; a live rollback round trip
was not performed. Recovery keys stay outside Git.

## Honest limits

Local Docker Desktop reported storage I/O errors during this rebrand, preventing a fresh
local Testcontainers/Compose pass. CI and the VPS completed the actual builds, PostgreSQL
checks and HTTPS browser verification. Original local database/media volumes were retained;
no local data was silently copied to the VPS or erased. No MySQL data migration is claimed.

One intermediate CI image build encountered Maven Central HTTP 403 responses; later runs
passed without relaxing dependency checks. The first recovery fixture observed PostgreSQL's
temporary initialization socket; waiting for final TCP readiness corrected the fixture.

The public GitGuardian PR alert refers to the required environment-variable expression on
historical compose.yml line 8 (`7736ec3`), not a literal password. That classification was
verified from public Git source. The user authorized review of that specific private incident. The dashboard requires a new
GitHub OAuth authorization (identity, email read and acting on behalf), which is awaiting a
separate decision. No scanner, branch protection or historical secret was silently waived. Previously committed
legacy credentials still require rotation wherever active.

This remains a synthetic, single-instance portfolio system: one unit per listing, flat ZAR
rates, simulated payments/email and operator-managed context/media retention. No load benchmark,
production capacity, complete accessibility conformance or non-Chromium device coverage is claimed.
