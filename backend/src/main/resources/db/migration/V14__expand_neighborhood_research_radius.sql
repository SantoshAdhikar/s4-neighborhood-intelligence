-- V13 expanded business search radii but did not update saved neighborhood research.
ALTER TABLE neighborhood_researches
    DROP CONSTRAINT IF EXISTS neighborhood_researches_radius_miles_check;

ALTER TABLE neighborhood_researches
    ADD CONSTRAINT neighborhood_researches_radius_miles_check
    CHECK (radius_miles IN (2, 3, 4, 10));
