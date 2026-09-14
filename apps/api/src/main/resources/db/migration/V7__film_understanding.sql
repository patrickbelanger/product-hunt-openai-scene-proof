CREATE TABLE source_films (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL UNIQUE REFERENCES projects(id),
    name VARCHAR(160) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size BETWEEN 1 AND 104857600),
    duration_ms BIGINT NOT NULL CHECK (duration_ms BETWEEN 1 AND 120000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (id, project_id)
);

CREATE TABLE film_understanding_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    source_film_id UUID NOT NULL,
    request_id UUID NOT NULL,
    stage VARCHAR(40) NOT NULL CHECK (stage IN ('PREPARING_SOURCE', 'DETECTING_STRUCTURE', 'TRANSCRIBING_AUDIO', 'UNDERSTANDING_FILM', 'BUILDING_CANDIDATES', 'SUCCEEDED', 'FAILED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    completed_at TIMESTAMPTZ,
    failure_code VARCHAR(80),
    failure_message VARCHAR(500),
    model VARCHAR(80) NOT NULL DEFAULT 'gpt-6-astra',
    reasoning VARCHAR(10) NOT NULL DEFAULT 'medium',
    transcription_model VARCHAR(80) NOT NULL DEFAULT 'whisper-1',
    audio_status VARCHAR(30),
    audio_sha256 CHAR(64),
    audio_duration_ms BIGINT,
    transcription_request_id VARCHAR(160),
    provider_response_id VARCHAR(160),
    provider_request_id VARCHAR(160),
    usage JSONB,
    result JSONB,
    UNIQUE (project_id, request_id),
    UNIQUE (id, project_id),
    FOREIGN KEY (source_film_id, project_id) REFERENCES source_films(id, project_id),
    CHECK ((stage IN ('SUCCEEDED', 'FAILED')) = (completed_at IS NOT NULL)),
    CHECK ((stage = 'FAILED') = (failure_code IS NOT NULL)),
    CHECK (stage <> 'SUCCEEDED' OR result IS NOT NULL)
);
CREATE UNIQUE INDEX one_active_film_understanding ON film_understanding_runs ((TRUE))
    WHERE completed_at IS NULL;

CREATE TABLE film_understanding_stages (
    run_id UUID NOT NULL REFERENCES film_understanding_runs(id),
    position INT NOT NULL,
    stage VARCHAR(40) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    completed_at TIMESTAMPTZ,
    PRIMARY KEY (run_id, position),
    UNIQUE (run_id, stage)
);

CREATE TABLE film_segments (
    source_film_id UUID NOT NULL,
    project_id UUID NOT NULL,
    shot_id UUID PRIMARY KEY,
    position INT NOT NULL CHECK (position BETWEEN 0 AND 7),
    start_ms BIGINT NOT NULL CHECK (start_ms >= 0),
    end_ms BIGINT NOT NULL CHECK (end_ms > start_ms AND end_ms <= 120000),
    UNIQUE (source_film_id, position),
    FOREIGN KEY (source_film_id, project_id) REFERENCES source_films(id, project_id),
    FOREIGN KEY (shot_id, project_id) REFERENCES shots(id, project_id)
);

CREATE TABLE film_transcript_segments (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    project_id UUID NOT NULL,
    position INT NOT NULL CHECK (position BETWEEN 0 AND 199),
    start_ms BIGINT NOT NULL CHECK (start_ms >= 0),
    end_ms BIGINT NOT NULL CHECK (end_ms > start_ms AND end_ms <= 120000),
    text VARCHAR(2000) NOT NULL CHECK (length(trim(text)) > 0),
    timestamp_origin VARCHAR(80) NOT NULL DEFAULT 'WHISPER_SEGMENT_ESTIMATE_SOURCE_START',
    UNIQUE (run_id, position),
    UNIQUE (id, run_id, project_id),
    FOREIGN KEY (run_id, project_id) REFERENCES film_understanding_runs(id, project_id)
);

CREATE TABLE film_candidates (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    project_id UUID NOT NULL,
    position INT NOT NULL CHECK (position BETWEEN 0 AND 11),
    proposal JSONB NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EDITED', 'REJECTED')),
    confirmed_title VARCHAR(120),
    confirmed_rule VARCHAR(2000),
    confirmed_scope VARCHAR(1000),
    decided_at TIMESTAMPTZ,
    reference_id UUID REFERENCES visual_references(id),
    UNIQUE (run_id, position),
    FOREIGN KEY (run_id, project_id) REFERENCES film_understanding_runs(id, project_id),
    CHECK ((status = 'PENDING') = (decided_at IS NULL)),
    CHECK (status NOT IN ('ACCEPTED', 'EDITED') OR (length(trim(confirmed_title)) > 0 AND length(trim(confirmed_rule)) > 0 AND length(trim(confirmed_scope)) > 0))
);

ALTER TABLE findings ADD COLUMN film_evidence JSONB NOT NULL DEFAULT '[]';

CREATE FUNCTION protect_film_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Film evidence is immutable';
END;
$$;
CREATE TRIGGER immutable_source_film BEFORE UPDATE OR DELETE ON source_films FOR EACH ROW EXECUTE FUNCTION protect_film_history();
CREATE TRIGGER immutable_film_segments BEFORE UPDATE OR DELETE ON film_segments FOR EACH ROW EXECUTE FUNCTION protect_film_history();
CREATE TRIGGER immutable_transcript_segments BEFORE UPDATE OR DELETE ON film_transcript_segments FOR EACH ROW EXECUTE FUNCTION protect_film_history();
