-- Previous accounts remain usable; they are not silently labelled email-verified.
ALTER TABLE app_users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_sha256 VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_email_verification_user ON email_verification_tokens(user_id, created_at DESC);

-- A claim is attached to a private workspace. It never transfers the workspace.
-- Status VERIFIED is reserved for a future audited ownership-verification process.
CREATE TABLE business_claims (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL UNIQUE REFERENCES business_profiles(id) ON DELETE CASCADE,
    claimant_user_id UUID NOT NULL REFERENCES app_users(id),
    identity_sha256 VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING_REVIEW', 'CONFLICT_REVIEW', 'VERIFIED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_business_claims_identity ON business_claims(identity_sha256);

-- Never auto-select a legitimate owner from duplicate names or submitted emails.
