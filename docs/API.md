# API quick reference

The live generated contract is at `/api/v1/openapi`, with Swagger UI at `/api/v1/docs`.
All responses, including private SSR/API responses, use no-store. Errors have `code`,
`message` and `correlationId`, with meaningful 400/401/403/404/409/413/429/500 statuses.

| Area | Implemented routes under `/api/v1` |
| --- | --- |
| Public catalogue | GET `/catalogue`, `/catalogue/{slug}`, `/catalogue/units/{id}/availability`, `/catalogue/units/{id}/price` |
| Identity | GET `/auth/csrf`, `/auth/me`; POST `/auth/register`, `/auth/login`, `/auth/logout`; PATCH `/auth/me` |
| Demo profile only | GET `/demo/status`; POST `/demo/enter`, `/demo/restore` |
| Checkout | POST `/quotes`, `/bookings`, `/bookings/{id}/confirm` |
| Trips | GET `/bookings`, `/bookings/{id}`, `/bookings/{id}/refund`; POST `/bookings/{id}/cancel`, `/bookings/{id}/review` |
| Wishlist | GET `/wishlist`; POST/DELETE `/wishlist/{listingId}` |
| Host | GET `/host`; POST `/host/listings`; PUT `/host/listings/{id}`; POST `/host/listings/{id}/images`, `/host/listings/{id}/blocks`; DELETE `/host/listings/{id}/blocks/{blockId}` |
| Administration | GET `/admin`; POST `/admin/listings/{id}/publication` |

Use a browser cookie jar or cookie-aware API client. Obtain `GET /auth/csrf` first; it
returns `{token,headerName}` and establishes a session. Include that returned header/token
on every mutation, including login, demo entry and logout. Never put session cookies in
browser storage or publicly cache authenticated results. Login/demonstration role switching
changes the session ID; use the updated cookie. The UI API client handles this transport.

Demo entry body: `{"role":"GUEST"}` (also HOST or ADMIN). Registration accepts only
`email`, `password` (10–72 characters) and `name`; callers cannot assign their own role.
Normal-profile registration creates only guests and does not create demo data.

Search example:

```text
GET /api/v1/catalogue?destination=Cape%20Town&checkIn=2026-11-06&checkOut=2026-11-08&guests=2&min=1000&max=3500&amenities=Parking&sort=price_asc&page=0
```

Use future dates appropriate to the current demo. Supported sorts are `recommended`,
`price_asc`, `price_desc`, `rating`; page size is 12, page index 0–1000. The response includes
`items`, `page`, `pageSize`, `hasMore`. Amenities can be a comma-separated list (all required).
When dates are selected, each item includes the full server-calculated `stayPrice`.

Quote example (UUID is obtained from the current authenticated catalogue/context):

```json
{"listingId":"<listing UUID>","checkIn":"2026-11-06","checkOut":"2026-11-08","guests":2}
```

The quote response contains `id`, dates, guests, a complete `price` object and `expiresAt`.
POST `/bookings` with `{"quoteId":"<quote UUID>"}` and a fresh `Idempotency-Key` of 8–100
letters, digits, dashes or underscores. It returns the persisted HOLD booking and reference.

POST `/bookings/{id}/confirm`, using a different operation key:

```json
{"scenario":"SUCCESS","policyAccepted":true}
```

Other scenarios are `DECLINE` and `RETRY`. `policyAccepted=false` or omitted is rejected.
No card number, guest identity, total, payment status or booking state is accepted. Unknown
JSON properties are rejected. A lost response can be retried with the same operation/key
and exact payload. A deliberately new simulator attempt uses a new key. Confirmed/failed/
expired bookings cannot be revived by another confirmation request.

GET `/bookings/{id}/refund` obtains a policy-based preview. POST `/bookings/{id}/cancel`
requires CSRF plus its own idempotency key and no arbitrary refund/body fields. The agreed
snapshot and audit/payment history remain readable by that guest after cancellation.
Review example: `{"rating":5,"body":"A thoughtful fictional stay."}`, only for an eligible
completed confirmed booking, once per booking.

Host publication is a separate moderation action. Host saves accept only the documented
listing input, including `submit` (false=draft, true=pending). Photograph uploads are multipart
with field `file`. The administrator body is `{"state":"PUBLISHED"}` or `UNPUBLISHED`.
Roles, ownership and context are enforced by Spring for each protected operation.
