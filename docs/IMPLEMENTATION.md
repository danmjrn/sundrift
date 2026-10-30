# NoirStay implementation checklist

- [x] Clone original history; clean `main` at `c3ad278`; create `feat/noirstay-portfolio`.
- [x] Inspect original defects and credentials without reproducing secret values.
- [x] Record build baseline and supported pinned toolchain.
- [ ] PostgreSQL migrations, sessions/CSRF, DTOs, inventory invariant and pricing.
- [ ] Search → quote → hold → simulated payment → persisted trip.
- [ ] Accounts, cancellation, wishlist and eligible reviews.
- [ ] Host listings/media/calendar and administrator publication/audit.
- [ ] Explicit demo profile and 24 fictional stays.
- [ ] Responsive black Next.js frontend and real browser screenshots.
- [ ] Backend PostgreSQL/concurrency/security tests; frontend checks/build/journeys.
- [ ] Compose, CI, setup/deployment/rollback docs and portfolio case study.
- [ ] GitHub rename/description/topics, push and reviewable PR if authenticated.

## Audit

Original checkout has no repository instructions, README or license file. Preserve its
commit history and `com.samantha` inherited authorship in this audit. All original source
remains available at commit `c3ad278`; replacement source is attributed to Daniel Jr Nkulu.
No existing database or dataset was supplied: no MySQL data migration is claimed.

Verified defects: doubles, Date arithmetic, UUID/Long repository mismatch, entity request
bodies, exact-date/guest-only availability, hardcoded guest, cancellation ownership gap,
unconditional privileged fixtures, AWS storage dependency, overlapping MVC/reactive/view
dependencies, MySQL and backend port 3000. Current source contains database credentials,
AWS access credential configuration and JWT signing material; these categories require
revocation/rotation wherever previously used. Values are deliberately omitted here.

GitHub API: `danmjrn/noirstay` returned 404; original repository exists. Git credential
helper returned no authenticated credential. Rename/push/metadata remain unverified.

Baseline: original wrapper was not executable; sandboxed invocation could not download.
An isolated Gradle 8.14.3 / JDK 21 run reached compileJava and failed in inherited
Lombok with `JCTree$JCImport.qualid` (Java compiler incompatibility). No original tests passed;
this is a compatibility baseline, not a claim that the original Java 17 build was tested.
Selected supported line: Java 21, Spring Boot 3.5.16, Gradle 8.14.3, PostgreSQL 18.6,
Next.js 16.4.3, React 19.3.1, TypeScript 6.0.3, Tailwind 4.3.3 and pnpm 11.12.0.
