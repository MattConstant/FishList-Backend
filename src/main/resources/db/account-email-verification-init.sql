-- Runs before Hibernate on Supabase/Postgres (see application-supabase.properties).
ALTER TABLE account ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE account ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(64);
ALTER TABLE account ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMPTZ;
ALTER TABLE account ADD COLUMN IF NOT EXISTS email_verified BOOLEAN;
UPDATE account SET email_verified = TRUE WHERE email_verified IS NULL;
ALTER TABLE account ALTER COLUMN email_verified SET DEFAULT TRUE;
ALTER TABLE account ALTER COLUMN email_verified SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_account_email ON account (email) WHERE email IS NOT NULL;
