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

GitHub API: an anonymous lookup of `danmjrn/noirstay` returned 404; private name collisions
cannot be ruled out without authentication. The original repository exists. Git credential
helper returned no authenticated credential. Rename/push/metadata remain unverified.

Baseline: original wrapper was not executable; sandboxed invocation could not download.
An isolated Gradle 8.14.3 / JDK 21 run reached compileJava and failed in inherited
Lombok with `JCTree$JCImport.qualid` (Java compiler incompatibility). No original tests passed;
this is a compatibility baseline, not a claim that the original Java 17 build was tested.
Selected supported line: Java 21, Spring Boot 4.1.2, Gradle 8.14.3, PostgreSQL 18.6,
Next.js 16.4.3, React 19.3.1, TypeScript 6.0.3, Tailwind 4.3.3 and pnpm 11.12.0.

An additional isolated baseline at c3ad278 used JDK 17 / Gradle 7.6.6 with old credentials
overridden by placeholders and the MySQL destination restricted to the container loopback.
Compilation succeeded; the sole contextLoads test failed because no MySQL datasource was
available (CommunicationsException / ConnectException). No old credential was used and no
existing database was touched. This is an environment-dependent failing baseline, not a
claim that the inherited application cannot run when MySQL is provisioned.

GitHub access resolved: SSH authenticated as danmjrn and pushed foundation commit 865880e.
The signed-in browser owner session verified `noirstay is available`, renamed the existing
repository, and saved the exact description plus all eight requested topics. SSH remote
now points to git@github.com:danmjrn/noirstay.git. Remote main remains c3ad278 and the existing
feature branch remains intact. The old HTTP API credential returned 401; it was not modified.
