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
docker compose exec -T database pg_dump -U noirstay -d noirstay -Fc > .runtime/backups/noirstay.dump
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
