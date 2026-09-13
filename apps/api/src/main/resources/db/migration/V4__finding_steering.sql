ALTER TABLE findings ADD CONSTRAINT findings_origin_unique UNIQUE (id, analysis_run_id, project_id);
ALTER TABLE analysis_runs ADD COLUMN kind VARCHAR(24) NOT NULL DEFAULT 'SEQUENCE'
    CHECK (kind IN ('SEQUENCE', 'TARGETED'));

CREATE TABLE finding_actions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    finding_id UUID NOT NULL,
    original_analysis_run_id UUID NOT NULL,
    request_id UUID NOT NULL,
    action_type VARCHAR(24) NOT NULL CHECK (action_type IN ('INTENTIONAL_CHANGE', 'RESOLVE', 'DISMISS')),
    explanation VARCHAR(2000) NOT NULL CHECK (length(trim(explanation)) > 0),
    scope VARCHAR(1000) NOT NULL CHECK (length(trim(scope)) > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reanalysis_run_id UUID UNIQUE,
    supersedes_action_id UUID,
    UNIQUE (project_id, request_id),
    UNIQUE (id, finding_id, project_id),
    FOREIGN KEY (finding_id, original_analysis_run_id, project_id) REFERENCES findings(id, analysis_run_id, project_id),
    FOREIGN KEY (reanalysis_run_id, project_id) REFERENCES analysis_runs(id, project_id),
    FOREIGN KEY (supersedes_action_id, finding_id, project_id) REFERENCES finding_actions(id, finding_id, project_id),
    CHECK ((action_type = 'INTENTIONAL_CHANGE') = (reanalysis_run_id IS NOT NULL))
);
CREATE INDEX finding_actions_history ON finding_actions (finding_id, created_at, id);

CREATE TABLE finding_action_shots (
    action_id UUID NOT NULL,
    finding_id UUID NOT NULL,
    project_id UUID NOT NULL,
    shot_id UUID NOT NULL,
    PRIMARY KEY (action_id, shot_id),
    FOREIGN KEY (action_id, finding_id, project_id) REFERENCES finding_actions(id, finding_id, project_id),
    FOREIGN KEY (finding_id, shot_id) REFERENCES finding_shots(finding_id, shot_id)
);

CREATE TABLE targeted_results (
    action_id UUID PRIMARY KEY REFERENCES finding_actions(id),
    outcome VARCHAR(32) NOT NULL CHECK (outcome IN ('INTENT_ACCEPTED', 'ISSUE_REMAINS', 'INSUFFICIENT_EVIDENCE')),
    result JSONB NOT NULL CHECK (jsonb_typeof(result) = 'object'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE FUNCTION reject_steering_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Steering history is immutable';
END;
$$;
CREATE TRIGGER finding_actions_immutable BEFORE UPDATE OR DELETE ON finding_actions
    FOR EACH ROW EXECUTE FUNCTION reject_steering_mutation();
CREATE TRIGGER finding_action_shots_immutable BEFORE UPDATE OR DELETE ON finding_action_shots
    FOR EACH ROW EXECUTE FUNCTION reject_steering_mutation();
CREATE TRIGGER targeted_results_immutable BEFORE UPDATE OR DELETE ON targeted_results
    FOR EACH ROW EXECUTE FUNCTION reject_steering_mutation();

CREATE VIEW finding_current_state AS
SELECT findings.id,
    COALESCE(latest.effective_status, 'OPEN') AS effective_status,
    latest.id AS latest_action_id
FROM findings
LEFT JOIN LATERAL (
    SELECT actions.id, CASE
        WHEN actions.action_type = 'RESOLVE' THEN 'RESOLVED'
        WHEN actions.action_type = 'DISMISS' THEN 'DISMISSED'
        WHEN results.outcome = 'INTENT_ACCEPTED' THEN 'INTENTIONAL'
        ELSE 'OPEN' END AS effective_status
    FROM finding_actions actions
    LEFT JOIN analysis_runs runs ON runs.id = actions.reanalysis_run_id
    LEFT JOIN targeted_results results ON results.action_id = actions.id
    WHERE actions.finding_id = findings.id
        AND (actions.reanalysis_run_id IS NULL OR (runs.status = 'SUCCEEDED' AND results.action_id IS NOT NULL))
    ORDER BY actions.created_at DESC, actions.id DESC LIMIT 1
) latest ON true;
