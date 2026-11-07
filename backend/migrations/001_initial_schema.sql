-- Oi Database Initialization Schema
-- Matches 09_DATABASE_DESIGN.md and 02_FUNCTIONAL_REQUIREMENTS.md

CREATE EXTENSION IF NOT EXISTS "citext";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Users Table (One Person One Account Invariant)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    display_name VARCHAR(50) NOT NULL,
    username CITEXT NOT NULL UNIQUE,
    account_state VARCHAR(20) NOT NULL DEFAULT 'PROVISIONAL',
    -- Blinded Identity Nullifier: HMAC-SHA256(MasterPepper, SovereignDocID)
    identity_nullifier VARCHAR(64) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    verified_at TIMESTAMPTZ,
    CONSTRAINT check_account_state CHECK (account_state IN ('PROVISIONAL', 'VERIFIED', 'SUSPENDED', 'CLOSED'))
);

-- 2. Single Active Device Enforcement (PRD-005)
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(128) NOT NULL,
    session_token VARCHAR(256) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Cryptographic Prekey Bundles (X3DH / Signal Protocol)
CREATE TABLE IF NOT EXISTS prekey_bundles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    identity_key VARCHAR(64) NOT NULL,
    signed_prekey VARCHAR(64) NOT NULL,
    signed_prekey_signature VARCHAR(128) NOT NULL,
    one_time_prekeys JSONB NOT NULL DEFAULT '[]'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. 180-Day Username Quarantine Table (PRD-004 / FUNC-USR-006)
CREATE TABLE IF NOT EXISTS quarantined_usernames (
    username CITEXT PRIMARY KEY,
    previous_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quarantined_until TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '180 days'),
    released BOOLEAN NOT NULL DEFAULT FALSE
);

-- 5. Ephemeral Transit Messages (Blind Mailbox - 30 Day Transit Expiry)
CREATE TABLE IF NOT EXISTS transit_messages (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    encrypted_envelope JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered BOOLEAN NOT NULL DEFAULT FALSE,
    delivered_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_transit_recipient ON transit_messages(recipient_id) WHERE delivered = FALSE;
CREATE INDEX IF NOT EXISTS idx_transit_expiry ON transit_messages(created_at);

-- 6. Blocked Users (Bidirectional Isolation)
CREATE TABLE IF NOT EXISTS blocked_users (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY(user_id, blocked_user_id)
);
