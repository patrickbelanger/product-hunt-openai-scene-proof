CREATE TABLE project_deletions (
    project_id UUID PRIMARY KEY,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    media_cleaned_at TIMESTAMPTZ
);

DO $$
DECLARE relation RECORD;
BEGIN
    FOR relation IN
        SELECT conrelid::regclass AS table_name, conname, pg_get_constraintdef(oid) AS definition
        FROM pg_constraint
        WHERE contype = 'f' AND connamespace = current_schema()::regnamespace
          AND conrelid::regclass::text IN (
            'shots', 'frames', 'analysis_runs', 'findings', 'finding_shots', 'finding_frames',
            'finding_actions', 'finding_action_shots', 'targeted_results', 'visual_references',
            'analysis_references', 'finding_references', 'source_films', 'film_understanding_runs',
            'film_understanding_stages', 'film_segments', 'film_transcript_segments', 'film_candidates'
          )
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', relation.table_name, relation.conname);
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s ON DELETE CASCADE', relation.table_name, relation.conname, relation.definition);
    END LOOP;
END;
$$;

CREATE FUNCTION project_history_deletion_allowed(record_data JSONB) RETURNS BOOLEAN LANGUAGE plpgsql AS $$
DECLARE owner_id UUID;
BEGIN
    owner_id := (record_data->>'project_id')::UUID;
    IF owner_id IS NULL AND record_data ? 'action_id' THEN
        SELECT project_id INTO owner_id FROM finding_actions WHERE id = (record_data->>'action_id')::UUID;
        IF owner_id IS NULL THEN
            RETURN pg_trigger_depth() > 1;
        END IF;
    END IF;
    RETURN owner_id IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM projects WHERE id = owner_id)
        AND EXISTS (SELECT 1 FROM project_deletions WHERE project_id = owner_id);
END;
$$;

CREATE OR REPLACE FUNCTION reject_steering_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND project_history_deletion_allowed(to_jsonb(OLD)) THEN RETURN OLD; END IF;
    RAISE EXCEPTION 'Steering history is immutable';
END;
$$;

CREATE OR REPLACE FUNCTION protect_film_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND project_history_deletion_allowed(to_jsonb(OLD)) THEN RETURN OLD; END IF;
    RAISE EXCEPTION 'Film evidence is immutable';
END;
$$;
