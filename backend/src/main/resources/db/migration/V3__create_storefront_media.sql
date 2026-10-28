CREATE TABLE storefront_media (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business_profiles(id) ON DELETE CASCADE,
    original_filename VARCHAR(200) NOT NULL,
    storage_key VARCHAR(80) NOT NULL UNIQUE,
    mime_type VARCHAR(30) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    analysis_attempts INTEGER NOT NULL DEFAULT 0,
    analysis_json TEXT,
    model_name VARCHAR(100),
    model_response_id VARCHAR(150),
    last_error VARCHAR(400),
    created_at TIMESTAMPTZ NOT NULL,
    analyzed_at TIMESTAMPTZ,
    CONSTRAINT ck_storefront_status CHECK (status IN ('UPLOADED', 'ANALYZING', 'ANALYZED', 'FAILED'))
);
CREATE INDEX idx_storefront_media_business_created ON storefront_media (business_id, created_at DESC);
