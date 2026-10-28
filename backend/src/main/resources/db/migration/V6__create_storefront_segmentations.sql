CREATE TABLE storefront_segmentations (
    id UUID PRIMARY KEY,
    media_id UUID NOT NULL REFERENCES storefront_media(id) ON DELETE CASCADE,
    concept VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    model_name VARCHAR(100),
    model_response_id VARCHAR(150),
    raw_output TEXT,
    last_error VARCHAR(400),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_segment_media_concept UNIQUE (media_id, concept),
    CONSTRAINT ck_segment_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);
CREATE INDEX idx_segmentations_media_created ON storefront_segmentations (media_id, created_at DESC);
