ALTER TABLE usuarios ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- Existing accounts remain usable. Public registration explicitly creates unverified accounts.
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_email_verification_tokens_expiry ON email_verification_tokens(expires_at);
