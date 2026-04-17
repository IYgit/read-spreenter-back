ALTER TABLE users
    ADD COLUMN email_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN verification_token VARCHAR(128) UNIQUE;

