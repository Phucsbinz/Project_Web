-- Optional testing reference, populated only by the explicit reset tool.
-- Authentication continues to use password_hash. Personal password changes clear this field.
ALTER TABLE users ADD COLUMN password_plaintext VARCHAR(72) NULL;
