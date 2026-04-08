-- Optional manual fix if you prefer DB-only migration. The app also supports automatic schema:
-- JPA maps google_sub as nullable so Hibernate can ADD COLUMN on non-empty tables; AccountGoogleSubBackfill
-- updates legacy NULL rows on startup (PostgreSQL).
--
-- If you still want NOT NULL at the database level after backfill:

-- 1) Add column nullable first (skip if it already exists from a failed partial migration)
ALTER TABLE account ADD COLUMN IF NOT EXISTS google_sub VARCHAR(255);

-- 2) Backfill any existing rows (pre-Google users) with stable synthetic subjects
UPDATE account
SET google_sub = 'legacy-migrated-' || id::text
WHERE google_sub IS NULL;

-- 3) Enforce constraints to match JPA entity
ALTER TABLE account ALTER COLUMN google_sub SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_account_google_sub ON account (google_sub);
