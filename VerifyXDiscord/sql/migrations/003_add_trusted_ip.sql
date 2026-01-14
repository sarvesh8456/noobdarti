-- 003_add_trusted_ip.sql
-- Adds a trusted IP and a last_known_ip column to the verifications table so
-- the plugin can persist the verified/trusted IP address for each account.

BEGIN;

-- Add columns if they don't already exist (works on PostgreSQL used by Supabase)
ALTER TABLE IF EXISTS verifications ADD COLUMN IF NOT EXISTS trusted_ip TEXT;
ALTER TABLE IF EXISTS verifications ADD COLUMN IF NOT EXISTS last_known_ip TEXT;

-- Optional: create an index to make lookups by trusted_ip faster
CREATE INDEX IF NOT EXISTS idx_verifications_trusted_ip ON verifications (trusted_ip);

COMMIT;
