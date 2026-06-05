-- Run once on Supabase SQL editor if startup migration did not run (existing account rows).
-- Step 1: add nullable columns
ALTER TABLE account ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE account ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(64);
ALTER TABLE account ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMPTZ;
ALTER TABLE account ADD COLUMN IF NOT EXISTS email_verified BOOLEAN;

-- Step 2: backfill legacy users as verified
UPDATE account SET email_verified = TRUE WHERE email_verified IS NULL;

-- Step 3: enforce defaults and NOT NULL
ALTER TABLE account ALTER COLUMN email_verified SET DEFAULT TRUE;
ALTER TABLE account ALTER COLUMN email_verified SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_account_email ON account (email) WHERE email IS NOT NULL;
