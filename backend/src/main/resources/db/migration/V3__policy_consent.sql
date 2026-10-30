ALTER TABLE booking ADD COLUMN policy_accepted_at timestamptz;
-- Historical synthetic fixtures predate the explicit consent audit field.
ALTER TABLE spring_session ALTER COLUMN principal_name TYPE varchar(254);
