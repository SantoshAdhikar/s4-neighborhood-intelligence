CREATE TABLE evidence_reports (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    source_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    model_name VARCHAR(100),
    model_response_id VARCHAR(150),
    report_json TEXT,
    last_error VARCHAR(400),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_evidence_report_sources UNIQUE (business_id, source_fingerprint),
    CONSTRAINT ck_evidence_report_status CHECK (status IN ('PENDING', 'GENERATING', 'COMPLETED', 'FAILED'))
);
CREATE INDEX idx_evidence_reports_business_created ON evidence_reports (business_id, created_at DESC);
