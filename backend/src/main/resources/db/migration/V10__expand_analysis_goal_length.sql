-- Preserve full owner instructions entered in the unified analysis form.
ALTER TABLE analysis_runs
    ALTER COLUMN goal TYPE VARCHAR(5000);

ALTER TABLE business_profiles
    ALTER COLUMN primary_goal TYPE VARCHAR(5000);
