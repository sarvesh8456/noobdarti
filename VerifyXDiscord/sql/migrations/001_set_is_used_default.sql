-- Migration: Ensure otps.is_used defaults to FALSE and NULLs are fixed
-- 1) Replace existing NULLs with false
UPDATE otps SET is_used = FALSE WHERE is_used IS NULL;

-- 2) Set default to false
ALTER TABLE otps ALTER COLUMN is_used SET DEFAULT FALSE;

-- 3) Optionally make column NOT NULL (uncomment to enforce)
-- ALTER TABLE otps ALTER COLUMN is_used SET NOT NULL;

-- Helpful: check the affected rows
SELECT COUNT(*) AS null_is_used FROM otps WHERE is_used IS NULL;