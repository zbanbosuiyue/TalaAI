-- Create profile_extended table for storing extended child profile metadata

CREATE TABLE users.profile_extended (
    id BIGINT PRIMARY KEY,
    profile_id BIGINT NOT NULL UNIQUE,
    data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);

-- Index on profile_id for quick lookup
CREATE UNIQUE INDEX ux_profile_extended_profile_id
    ON users.profile_extended(profile_id)
    WHERE deleted_at IS NULL;

-- Optional GIN index for JSONB queries
CREATE INDEX idx_profile_extended_data_gin
    ON users.profile_extended USING GIN (data);
