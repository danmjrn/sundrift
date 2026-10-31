# Local setup, deployment and rollback

No remote target was supplied; no VPS or unrelated live service was changed.
The release is packaged as Docker Compose services for a single VPS.

## Fresh checkout

Install Docker with Compose. Copy `.env.example` to `.env`, supply a unique database
password, and explicitly choose `SPRING_PROFILES_ACTIVE=demo` for synthetic data.

```sh
cp .env.example .env
# Edit .env: replace the placeholder password.
docker compose up --build -d --wait
```

Open http://localhost:3000. The edge is the only published service in the default Compose
file. PostgreSQL and media use named persistent volumes. Repeated `up` and `restart` retain
bookings, sessions and media. Migrations run before Hibernate `validate`; never change an
already-applied migration. `docker compose down` stops services without deleting volumes.
Do not use `down -v` to stop a database containing data you want to retain.

The `demo` profile seeds 24 stays once per context. Switch it off (empty profile) for normal
operation; normal registration creates guests only. Do not expose a normal deployment
until the operator has deliberately provisioned host/admin accounts and appropriate
operational controls. This release is designed as a synthetic portfolio demonstration.

## Separate development commands

Install Java 21, Node 22/24 and pnpm 11.12.0. Start just PostgreSQL with loopback access:

```sh
docker compose -f compose.yml -f infrastructure/development.yml up -d --wait database
```

Export the database password from your local environment (do not paste it into shell
history), then run:

```sh
cd backend
./gradlew bootRun --args='--spring.profiles.active=demo'
```

In a second terminal:

```sh
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

Defaults: Spring 8080, Next 3000. Java's port is `PORT`; Next accepts `--port`/`PORT`;
Compose's public edge uses `PUBLIC_PORT`. `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
and `MEDIA_ROOT` configure Spring. `API_INTERNAL_URL` is server-only and defaults to the
host's 8080. The Docker build sets it to `http://backend:8080` for rewrite generation as
well as runtime SSR. If changing that Docker address, rebuild the frontend too.
`DEMO_REFERENCE_DATE` optionally fixes the fixture reference date for a controlled rehearsal;
unset it for dates relative to each new demo context. V4 is a one-time compatibility migration
from the task’s pre-release Spring 3 prototype: it invalidates only synthetic/anonymous
prototype sessions, retaining bookings and users. Ordinary release restarts retain sessions.
`PUBLIC_ORIGIN` sets social metadata; it is a server build setting, never a secret.

## Tests and builds

Docker must be running for PostgreSQL Testcontainers tests. Run them directly from Java;
no host Docker socket needs to be mounted inside a test container.

```sh
cd backend
./gradlew test bootJar --no-daemon
```

```sh
cd frontend
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm exec playwright install chromium
pnpm test:e2e
```

Browser tests target an already-running http://localhost:3000, or `PREVIEW_URL`. They create
only synthetic data. Repeated local runs can use `DEMO_CONTEXTS_PER_HOUR=100` in `.env` and
recreate the backend; retain the default 10 on an exposed demo. Next uses its supported
Webpack build option because this workstation's Turbopack CSS process cannot bind a local
port under its execution restrictions. All application features use standard Next APIs.

To verify a real application restart with the same cookie:

```sh
python3 scripts/restart-probe.py create
# Restart only this project's backend, then wait for health.
docker compose restart backend
docker compose up -d --wait backend
python3 scripts/restart-probe.py verify
```

The probe uses a dedicated synthetic guest/context and saves its cookie under ignored
`.runtime/`. Delete those local probe artifacts when done. It never contacts a payment
provider or an email server.

## VPS deployment

1. Select a VPS/domain explicitly authorized for this project. Back up any existing service.
2. Use an immutable reviewed Git commit, a strong ignored environment file and the default
   internal database topology. Pull/build images for that exact commit.
3. Put an operator-managed TLS proxy in front of the edge, bind the edge to loopback if using
   a host proxy, set `COOKIE_SECURE=true`, and forward the correct HTTPS scheme through the
   trusted proxy chain. Adapt `infrastructure/nginx.conf` to that specific trusted topology;
   do not trust arbitrary client forwarding headers. Serve media with the same origin.
4. Back up the PostgreSQL database and media volume before migrations; verify a restore in
   an isolated environment. The database role installing `btree_gist` needs CREATE permission.
5. Run `docker compose up --build -d --wait`, inspect health and logs, then run a synthetic
   booking/cancellation smoke test. Add a GitHub homepage only after the deployed URL works.
6. Apply memory/disk limits, monitor volume capacity and failed health checks, and maintain
   operating system/container security updates. No measured production capacity is claimed.

Example backup stream (the invoking shell creates the backup under a restricted umask):

```sh
umask 077
mkdir -p .runtime/backups
docker compose exec -T database pg_dump -U sundrift -d sundrift -Fc > .runtime/backups/sundrift.dump
```

Copy the media volume through an operator-approved backup process. Backups contain sessions
and synthetic/registered account data; keep them private.

Rollback: stop the edge from accepting writes, preserve current database/media backups,
rebuild/start the preceding known-good commit, and verify schema compatibility before
reopening traffic. Flyway migrations are forward-only. If the previous application cannot
use the current schema, restore the matching pre-release backup to a separate database
and verify it before switching; do not attempt a destructive automatic downgrade.

## Restore and retention

Account → Restore my demo creates a new visitor context and switches only that session.
It does not delete existing reservations or reset anyone else's context. There is no public
bulk reset endpoint. Context retention/garbage collection is deliberately operator-managed
for this release. Do not delete active contexts as part of routine startup.


## Verified Sundrift VPS deployment

The owner authorized this target on 31 October 2026: https://sundrift.shapeit.solutions/.
The deployed source and live acceptance are recorded in LAUNCH_VERIFICATION.md. Earlier
absence-of-target notes describe the initial local-only delivery, not the current status.

Production uses compose.yml plus infrastructure/production.yml, private runtime configuration
at /opt/sundrift/shared/runtime.env, loopback ingress 127.0.0.1:3110 and the host TLS edge.
PUBLIC_BIND=127.0.0.1, COOKIE_SECURE=true and PUBLIC_ORIGIN=https://sundrift.shapeit.solutions
are required. The production inner proxy relies on that private edge to supply forwarding
headers; use the normal stripping proxy for a directly accessible local installation.

ops/deploy_vps.py admits only exact pushed source. Its prepare, build, activate and edge
actions stage immutable releases, build one application image at a time, check readiness,
and preserve the previous release. The operator’s existing administrator SSH identity stays
local and is never placed in this repository or CI. App runtime images and volumes are
isolated from every other service. Backend/database/frontend/edge memory limits are defined
in the production overlay.

Backups encrypt PostgreSQL custom dumps and immutable media with app-specific host and owner
recovery recipients. The owner recovery identity is local under ignored .runtime/, mode0600;
the host identity remains private under /opt/sundrift/shared/. The recovery rehearsal restored
the encrypted database and referenced photographs without networking. ops/sundrift-backup.*
provide a daily 05:30 SAST schedule with up to five minutes of jitter. Retention is operator
managed; these units do not delete old data.

For an operator-controlled rollback, invoke the deployed ops/vps_release.py rollback action:
it takes a new encrypted backup, selects the recorded previous source/images and checks
health. Database migrations are not automatically reversed. Keep the patched current release
and verified backups; choose any schema recovery deliberately. The live rollback path is
documented but was not executed as an acceptance claim.
