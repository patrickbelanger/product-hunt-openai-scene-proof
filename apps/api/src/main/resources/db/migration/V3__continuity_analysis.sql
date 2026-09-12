ALTER TABLE shots ADD CONSTRAINT shots_id_project_unique UNIQUE (id, project_id);
ALTER TABLE frames ADD CONSTRAINT frames_id_shot_unique UNIQUE (id, shot_id);

CREATE TABLE analysis_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    request_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    provider VARCHAR(16) NOT NULL DEFAULT 'OPENAI' CHECK (provider = 'OPENAI'),
    model VARCHAR(80) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failure_code VARCHAR(80),
    failure_message VARCHAR(300),
    shot_count INTEGER NOT NULL DEFAULT 0 CHECK (shot_count BETWEEN 0 AND 8),
    frame_count INTEGER NOT NULL DEFAULT 0 CHECK (frame_count BETWEEN 0 AND 24),
    context JSONB,
    project_summary VARCHAR(2000),
    warnings JSONB NOT NULL DEFAULT '[]',
    usage JSONB,
    provider_response_id VARCHAR(160),
    provider_request_id VARCHAR(160),
    UNIQUE (project_id, request_id),
    UNIQUE (id, project_id),
    CHECK ((status = 'RUNNING' AND completed_at IS NULL AND failure_code IS NULL)
        OR (status = 'SUCCEEDED' AND completed_at IS NOT NULL AND failure_code IS NULL AND project_summary IS NOT NULL)
        OR (status = 'FAILED' AND completed_at IS NOT NULL AND failure_code IS NOT NULL AND failure_message IS NOT NULL))
);

CREATE UNIQUE INDEX analysis_single_running ON analysis_runs ((1)) WHERE status = 'RUNNING';
CREATE INDEX analysis_project_started ON analysis_runs (project_id, started_at DESC, id);

CREATE TABLE findings (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL,
    project_id UUID NOT NULL,
    category VARCHAR(40) NOT NULL CHECK (category IN ('CHARACTER_IDENTITY', 'CHARACTER_APPEARANCE', 'HAIR', 'WARDROBE', 'PROP', 'OBJECT_STATE', 'ENVIRONMENT', 'SPATIAL_CONTINUITY', 'SCREEN_DIRECTION', 'LIGHTING', 'TIME_OF_DAY', 'HAND_OBJECT_INTERACTION', 'DEVICE_UI', 'TEXT_CONTINUITY', 'OTHER')),
    severity VARCHAR(8) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    expected_state VARCHAR(2000) NOT NULL,
    observed_state VARCHAR(2000) NOT NULL,
    explanation VARCHAR(3000) NOT NULL,
    suggested_correction_prompt VARCHAR(3000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status = 'OPEN'),
    position INTEGER NOT NULL CHECK (position BETWEEN 0 AND 19),
    UNIQUE (analysis_run_id, position),
    UNIQUE (id, project_id),
    FOREIGN KEY (analysis_run_id, project_id) REFERENCES analysis_runs(id, project_id)
);

CREATE TABLE finding_shots (
    finding_id UUID NOT NULL,
    shot_id UUID NOT NULL,
    project_id UUID NOT NULL,
    PRIMARY KEY (finding_id, shot_id),
    FOREIGN KEY (finding_id, project_id) REFERENCES findings(id, project_id),
    FOREIGN KEY (shot_id, project_id) REFERENCES shots(id, project_id)
);

CREATE TABLE finding_frames (
    finding_id UUID NOT NULL,
    frame_id UUID NOT NULL,
    shot_id UUID NOT NULL,
    PRIMARY KEY (finding_id, frame_id),
    FOREIGN KEY (finding_id, shot_id) REFERENCES finding_shots(finding_id, shot_id),
    FOREIGN KEY (frame_id, shot_id) REFERENCES frames(id, shot_id)
);
