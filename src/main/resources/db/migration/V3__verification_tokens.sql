CREATE TABLE verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    token_hash VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    attempts INT DEFAULT 0,
    CONSTRAINT fk_user_token FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT TRUE;
