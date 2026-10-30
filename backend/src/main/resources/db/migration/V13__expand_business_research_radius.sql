-- Permit 2- and 3-mile searches without changing existing business radii.
ALTER TABLE business_profiles DROP CONSTRAINT IF EXISTS business_profiles_research_radius_miles_check;
ALTER TABLE business_profiles ADD CONSTRAINT business_profiles_research_radius_miles_check
    CHECK (research_radius_miles IN (2, 3, 4, 10));
