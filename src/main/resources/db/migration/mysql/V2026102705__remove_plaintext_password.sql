-- Remove the legacy test-only readable password cache; authentication hashes remain unchanged.
-- Kept as a forward migration so already-applied migration checksums do not change.
ALTER TABLE users DROP COLUMN password_plaintext;
