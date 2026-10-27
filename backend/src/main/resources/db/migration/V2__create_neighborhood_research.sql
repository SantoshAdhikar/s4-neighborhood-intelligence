
CREATE TABLE neighborhood_researches (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    address_key VARCHAR(700) NOT NULL,
    radius_miles INTEGER NOT NULL CHECK (radius_miles IN (4, 10)),
    center_latitude DOUBLE PRECISION NOT NULL,
    center_longitude DOUBLE PRECISION NOT NULL,
    matched_address TEXT NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_neighborhood_researches_cache
    ON neighborhood_researches (business_id, radius_miles, fetched_at DESC);

CREATE TABLE nearby_places (
    id UUID PRIMARY KEY,
    research_id UUID NOT NULL REFERENCES neighborhood_researches(id) ON DELETE CASCADE,
    osm_type VARCHAR(12) NOT NULL,
    osm_id BIGINT NOT NULL,
    name VARCHAR(250) NOT NULL,
    category VARCHAR(150) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    distance_miles DOUBLE PRECISION NOT NULL,
    website VARCHAR(500),
    source_url VARCHAR(250) NOT NULL,
    CONSTRAINT uq_nearby_places_osm UNIQUE (research_id, osm_type, osm_id)
);

CREATE INDEX idx_nearby_places_research ON nearby_places (research_id);
