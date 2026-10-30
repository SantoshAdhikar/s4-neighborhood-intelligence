-- Preserve individual, source-linked page inspections without rewriting older homepage evidence.
CREATE TABLE website_page_findings (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE CASCADE,
    business_id UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    website_finding_id UUID NOT NULL REFERENCES website_findings(id) ON DELETE CASCADE,
    page_type VARCHAR(24) NOT NULL,
    page_url VARCHAR(500) NOT NULL,
    page_title VARCHAR(250),
    status VARCHAR(24) NOT NULL,
    observed_summary VARCHAR(2000),
    note VARCHAR(350),
    fetched_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX website_page_findings_by_run ON website_page_findings(run_id, fetched_at);
CREATE INDEX website_page_findings_by_website ON website_page_findings(website_finding_id);
