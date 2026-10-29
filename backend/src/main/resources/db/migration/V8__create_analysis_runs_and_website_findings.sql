CREATE TABLE analysis_runs (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    step VARCHAR(40) NOT NULL,
    detail VARCHAR(500),
    goal VARCHAR(1000) NOT NULL,
    website VARCHAR(500),
    customer_feedback VARCHAR(1500),
    media_id UUID REFERENCES storefront_media(id) ON DELETE SET NULL,
    report_id UUID REFERENCES evidence_reports(id) ON DELETE SET NULL,
    use_meta BOOLEAN NOT NULL DEFAULT FALSE,
    segment_sign BOOLEAN NOT NULL DEFAULT FALSE,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX analysis_runs_one_active_per_business
  ON analysis_runs(business_id) WHERE status IN ('QUEUED', 'RUNNING');
CREATE INDEX analysis_runs_by_business ON analysis_runs(business_id, created_at DESC);

CREATE TABLE website_findings (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE CASCADE,
    business_id UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    kind VARCHAR(24) NOT NULL,
    business_name VARCHAR(160) NOT NULL,
    url VARCHAR(500) NOT NULL,
    title VARCHAR(250),
    observed_summary VARCHAR(1200),
    status VARCHAR(24) NOT NULL,
    note VARCHAR(350),
    fetched_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX website_findings_by_run ON website_findings(run_id);
