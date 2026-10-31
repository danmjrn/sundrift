# Sundrift launch

Sundrift replaces the original NoirStay identity with a warm nude palette, sun/horizon
wordmark and feature-first product documentation. Booking rules and existing records are
preserved. Applied Flyway migrations remain byte-for-byte unchanged; V5 only updates
synthetic listing presentation text.

Existing local data volumes and database identifiers are explicitly retained through ignored
.env overrides. Fresh installations use sundrift names. Sessions store Spring security
types rather than renamed project-owned classes.

Live: https://sundrift.shapeit.solutions, isolated from the company application and all
other deployed projects. DNS, TLS and all 11 live browser checks passed. The real screenshots
and [launch report](LAUNCH_VERIFICATION.md) record the delivered application.
