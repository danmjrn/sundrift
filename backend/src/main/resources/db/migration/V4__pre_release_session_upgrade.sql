-- One-time upgrade from the task's Boot 3 prototype to the supported Boot 4 release.
-- Security's serialized session classes changed major version. Invalidate only synthetic
-- demo/anonymous prototype sessions; retain every booking, user, payment and fixture.
-- This is a versioned migration, never a repeated startup/demo reset.
DELETE FROM spring_session
WHERE principal_name IS NULL
   OR principal_name LIKE '%@visitor-%.demo.invalid'
   OR principal_name LIKE '%@test-%.demo.invalid';
