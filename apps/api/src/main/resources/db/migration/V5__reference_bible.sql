CREATE TABLE visual_references (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    title VARCHAR(120) NOT NULL CHECK (length(trim(title)) > 0),
    guidance VARCHAR(2000) NOT NULL,
    width INTEGER NOT NULL CHECK (width BETWEEN 1 AND 1600),
    height INTEGER NOT NULL CHECK (height BETWEEN 1 AND 1600),
    sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    UNIQUE (id, project_id)
);

CREATE INDEX references_project_order ON visual_references(project_id, created_at, id);

CREATE TABLE analysis_references (
    analysis_run_id UUID NOT NULL,
    reference_id UUID NOT NULL,
    project_id UUID NOT NULL,
    title VARCHAR(120) NOT NULL CHECK (length(trim(title)) > 0),
    guidance VARCHAR(2000) NOT NULL,
    width INTEGER NOT NULL CHECK (width BETWEEN 1 AND 1600),
    height INTEGER NOT NULL CHECK (height BETWEEN 1 AND 1600),
    sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    position INTEGER NOT NULL CHECK (position BETWEEN 0 AND 7),
    PRIMARY KEY (analysis_run_id, reference_id, project_id),
    UNIQUE (analysis_run_id, position),
    FOREIGN KEY (analysis_run_id, project_id) REFERENCES analysis_runs(id, project_id),
    FOREIGN KEY (reference_id, project_id) REFERENCES visual_references(id, project_id)
);

ALTER TABLE findings ADD CONSTRAINT findings_id_run_project_unique UNIQUE (id, analysis_run_id, project_id);

CREATE TABLE finding_references (
    finding_id UUID NOT NULL,
    analysis_run_id UUID NOT NULL,
    reference_id UUID NOT NULL,
    project_id UUID NOT NULL,
    PRIMARY KEY (finding_id, reference_id),
    FOREIGN KEY (finding_id, analysis_run_id, project_id) REFERENCES findings(id, analysis_run_id, project_id),
    FOREIGN KEY (analysis_run_id, reference_id, project_id) REFERENCES analysis_references(analysis_run_id, reference_id, project_id)
);

CREATE FUNCTION preserve_reference_image() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.id, NEW.project_id, NEW.width, NEW.height, NEW.sha256, NEW.created_at)
        IS DISTINCT FROM ROW(OLD.id, OLD.project_id, OLD.width, OLD.height, OLD.sha256, OLD.created_at)
        OR (OLD.archived_at IS NOT NULL AND NEW IS DISTINCT FROM OLD) THEN
        RAISE EXCEPTION 'Reference image and archived metadata are immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER reference_image_immutable BEFORE UPDATE ON visual_references
    FOR EACH ROW EXECUTE FUNCTION preserve_reference_image();
CREATE TRIGGER analysis_references_immutable BEFORE UPDATE OR DELETE ON analysis_references
    FOR EACH ROW EXECUTE FUNCTION reject_steering_mutation();
CREATE TRIGGER finding_references_immutable BEFORE UPDATE OR DELETE ON finding_references
    FOR EACH ROW EXECUTE FUNCTION reject_steering_mutation();
