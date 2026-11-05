-- Keep the physical type aligned with the JPA String mapping and avoid CHAR padding.
ALTER TABLE email_verification_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64) USING token_hash::VARCHAR(64);
