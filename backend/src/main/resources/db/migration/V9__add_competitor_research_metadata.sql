-- Retain source provenance for shortlisted businesses even without a usable website.
ALTER TABLE website_findings
    ADD COLUMN osm_source_url VARCHAR(250),
    ADD COLUMN category VARCHAR(150),
    ADD COLUMN distance_miles DOUBLE PRECISION,
    ADD COLUMN match_level VARCHAR(24);
