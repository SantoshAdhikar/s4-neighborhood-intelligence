
CREATE TABLE business_profiles (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    industry VARCHAR(120) NOT NULL,
    street_address VARCHAR(250) NOT NULL,
    city VARCHAR(120) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    website VARCHAR(500),
    services TEXT NOT NULL,
    primary_goal VARCHAR(1000) NOT NULL,
    research_radius_miles INTEGER NOT NULL
        CHECK (research_radius_miles IN (4, 10)),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_business_profiles_city_state
    ON business_profiles (city, state);
