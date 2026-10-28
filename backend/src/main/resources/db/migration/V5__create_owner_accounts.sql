CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE business_profiles
    ADD COLUMN owner_user_id UUID REFERENCES app_users(id);

CREATE INDEX idx_business_profiles_owner ON business_profiles(owner_user_id);
