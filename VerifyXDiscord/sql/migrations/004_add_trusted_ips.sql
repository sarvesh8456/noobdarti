-- 004_add_trusted_ips.sql
-- Adds a `trusted_ips` JSONB array to the verifications table and migrates any existing `trusted_ip` values into the array.

BEGIN;

-- Add jsonb trusted_ips column if missing
ALTER TABLE IF EXISTS verifications ADD COLUMN IF NOT EXISTS trusted_ips jsonb DEFAULT '[]'::jsonb;

-- Migrate existing trusted_ip values into trusted_ips array
UPDATE verifications SET trusted_ips = jsonb_build_array(trusted_ip) WHERE trusted_ip IS NOT NULL AND trusted_ip <> '' AND (trusted_ips IS NULL OR jsonb_array_length(trusted_ips) = 0);

-- Optional: create a GIN index for fast containment queries
CREATE INDEX IF NOT EXISTS idx_verifications_trusted_ips_gin ON verifications USING GIN (trusted_ips jsonb_path_ops);

COMMIT;
