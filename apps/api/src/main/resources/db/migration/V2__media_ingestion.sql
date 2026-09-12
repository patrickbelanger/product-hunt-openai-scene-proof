CREATE TABLE shots (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    position INTEGER NOT NULL CHECK (position >= 0),
    name VARCHAR(160) NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('IMAGE', 'VIDEO', 'UNKNOWN')),
    status VARCHAR(10) NOT NULL CHECK (status IN ('READY', 'FAILED')),
    failure_code VARCHAR(40),
    duration_ms BIGINT CHECK (duration_ms > 0 AND duration_ms <= 120000),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (project_id, position),
    CHECK ((status = 'FAILED') = (failure_code IS NOT NULL))
);
CREATE TABLE frames (
    id UUID PRIMARY KEY,
    shot_id UUID NOT NULL REFERENCES shots(id),
    position INTEGER NOT NULL CHECK (position >= 0 AND position < 32),
    timestamp_ms BIGINT CHECK (timestamp_ms >= 0 AND timestamp_ms <= 120000),
    width INTEGER NOT NULL CHECK (width > 0 AND width <= 1600),
    height INTEGER NOT NULL CHECK (height > 0 AND height <= 1600),
    storage_key VARCHAR(100) NOT NULL,
    UNIQUE (shot_id, position),
    UNIQUE (shot_id, timestamp_ms)
);
