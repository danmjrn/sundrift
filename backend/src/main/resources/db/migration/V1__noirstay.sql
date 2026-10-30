-- btree_gist is a trusted extension; the migration role needs CREATE on this database.
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE demo_context (
 id varchar(80) PRIMARY KEY, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app_user (
 id uuid PRIMARY KEY, email varchar(254) NOT NULL UNIQUE, display_name varchar(100) NOT NULL,
 password_hash varchar(100) NOT NULL, role varchar(10) NOT NULL CHECK(role IN ('GUEST','HOST','ADMIN')),
 demo_context varchar(80) REFERENCES demo_context(id) ON DELETE RESTRICT,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE listing (
 id uuid PRIMARY KEY, host_id uuid NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
 context varchar(80) NOT NULL, slug varchar(120) NOT NULL, title varchar(120) NOT NULL,
 description text NOT NULL, type varchar(20) NOT NULL CHECK(type IN ('APARTMENT','VILLA','CABIN','SUITE','HOTEL_ROOM')),
 destination varchar(80) NOT NULL, latitude numeric(9,5) NOT NULL, longitude numeric(9,5) NOT NULL,
 address_visibility varchar(20) NOT NULL DEFAULT 'APPROXIMATE', capacity integer NOT NULL CHECK(capacity BETWEEN 1 AND 16),
 beds integer NOT NULL CHECK(beds BETWEEN 1 AND 16), bathrooms integer NOT NULL CHECK(bathrooms BETWEEN 1 AND 12),
 amenities jsonb NOT NULL DEFAULT '[]', images jsonb NOT NULL DEFAULT '[]',
 nightly_rate numeric(12,2) NOT NULL CHECK(nightly_rate > 0), cleaning_fee numeric(12,2) NOT NULL CHECK(cleaning_fee >= 0),
 currency varchar(3) NOT NULL DEFAULT 'ZAR' CHECK(currency = 'ZAR'), timezone varchar(60) NOT NULL DEFAULT 'Africa/Johannesburg',
 house_rules text NOT NULL, cancellation_policy varchar(20) NOT NULL CHECK(cancellation_policy IN ('FLEXIBLE','MODERATE')),
 state varchar(12) NOT NULL CHECK(state IN ('DRAFT','PENDING','PUBLISHED','UNPUBLISHED')),
 price_version integer NOT NULL DEFAULT 1, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(context,slug)
);
CREATE INDEX listing_search ON listing(context,state,destination,type,nightly_rate,id);
CREATE INDEX listing_host ON listing(host_id);
CREATE TABLE quote (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
 listing_id uuid NOT NULL REFERENCES listing(id) ON DELETE RESTRICT,
 check_in date NOT NULL, check_out date NOT NULL, guests integer NOT NULL,
 price_version integer NOT NULL, snapshot jsonb NOT NULL, expires_at timestamptz NOT NULL,
 CHECK(check_out > check_in)
);
CREATE TABLE booking (
 id uuid PRIMARY KEY, reference varchar(30) NOT NULL UNIQUE,
 guest_id uuid NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
 listing_id uuid NOT NULL REFERENCES listing(id) ON DELETE RESTRICT,
 quote_id uuid UNIQUE REFERENCES quote(id) ON DELETE RESTRICT,
 check_in date NOT NULL, check_out date NOT NULL, guests integer NOT NULL CHECK(guests BETWEEN 1 AND 16),
 status varchar(12) NOT NULL CHECK(status IN ('HOLD','CONFIRMED','CANCELLED','EXPIRED','FAILED')),
 snapshot jsonb NOT NULL, total numeric(12,2) NOT NULL, currency varchar(3) NOT NULL DEFAULT 'ZAR',
 expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL, confirmed_at timestamptz,
 cancelled_at timestamptz, refund numeric(12,2) NOT NULL DEFAULT 0,
 CHECK(check_out > check_in), CHECK(total > 0), CHECK(refund BETWEEN 0 AND total)
);
CREATE INDEX booking_guest ON booking(guest_id,created_at DESC,id);
CREATE INDEX booking_listing ON booking(listing_id,check_in);
CREATE INDEX booking_expiry ON booking(listing_id,expires_at) WHERE status='HOLD';
-- A single invariant covers bookings, holds and host blocks. No current-time predicate.
CREATE TABLE inventory (
 id uuid PRIMARY KEY, listing_id uuid NOT NULL REFERENCES listing(id) ON DELETE RESTRICT,
 booking_id uuid UNIQUE REFERENCES booking(id) ON DELETE RESTRICT,
 check_in date NOT NULL, check_out date NOT NULL, active boolean NOT NULL,
 kind varchar(10) NOT NULL CHECK(kind IN ('BOOKING','BLOCK')), CHECK(check_out > check_in),
 CHECK((kind='BOOKING' AND booking_id IS NOT NULL) OR (kind='BLOCK' AND booking_id IS NULL)),
 EXCLUDE USING gist (listing_id WITH =, daterange(check_in,check_out,'[)') WITH &&) WHERE (active)
);
CREATE TABLE payment_attempt (
 id uuid PRIMARY KEY, booking_id uuid NOT NULL REFERENCES booking(id) ON DELETE RESTRICT,
 outcome varchar(12) NOT NULL CHECK(outcome IN ('SUCCESS','DECLINE','RETRY','REFUND')),
 amount numeric(12,2) NOT NULL, created_at timestamptz NOT NULL
);
CREATE INDEX payment_booking ON payment_attempt(booking_id);
CREATE TABLE idempotency (
 user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
 operation varchar(100) NOT NULL, key varchar(100) NOT NULL, payload_hash varchar(64) NOT NULL,
 response jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(user_id,operation,key)
);
CREATE TABLE review (
 id uuid PRIMARY KEY, booking_id uuid UNIQUE NOT NULL REFERENCES booking(id) ON DELETE RESTRICT,
 listing_id uuid NOT NULL REFERENCES listing(id) ON DELETE RESTRICT,
 author_id uuid NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
 rating integer NOT NULL CHECK(rating BETWEEN 1 AND 5), body varchar(1500) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX review_listing ON review(listing_id,created_at DESC);
CREATE TABLE wishlist (
 user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
 listing_id uuid NOT NULL REFERENCES listing(id) ON DELETE CASCADE, PRIMARY KEY(user_id,listing_id)
);
CREATE TABLE audit_event (
 id uuid PRIMARY KEY, actor_id uuid NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
 context varchar(80) NOT NULL, action varchar(60) NOT NULL, resource_id uuid NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX audit_context ON audit_event(context,created_at DESC);
