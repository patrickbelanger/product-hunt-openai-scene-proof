ALTER TABLE film_candidates ADD CONSTRAINT film_reference_project
    FOREIGN KEY (reference_id, project_id) REFERENCES visual_references(id, project_id);
CREATE UNIQUE INDEX film_reference_provenance ON film_candidates(reference_id) WHERE reference_id IS NOT NULL;

CREATE FUNCTION protect_film_decision() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.id, NEW.run_id, NEW.project_id, NEW.position, NEW.proposal)
        IS DISTINCT FROM (OLD.id, OLD.run_id, OLD.project_id, OLD.position, OLD.proposal) THEN
        RAISE EXCEPTION 'Original film discovery is immutable';
    END IF;
    IF OLD.status <> 'PENDING' AND
        (NEW.status, NEW.confirmed_title, NEW.confirmed_rule, NEW.confirmed_scope, NEW.decided_at)
        IS DISTINCT FROM (OLD.status, OLD.confirmed_title, OLD.confirmed_rule, OLD.confirmed_scope, OLD.decided_at) THEN
        RAISE EXCEPTION 'Recorded creator decision is immutable';
    END IF;
    IF (OLD.reference_id IS NOT NULL AND NEW.reference_id IS DISTINCT FROM OLD.reference_id)
        OR (NEW.reference_id IS NOT NULL AND NEW.status NOT IN ('ACCEPTED', 'EDITED')) THEN
        RAISE EXCEPTION 'Film reference provenance requires an immutable confirmed decision';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_film_decision BEFORE UPDATE ON film_candidates FOR EACH ROW EXECUTE FUNCTION protect_film_decision();
CREATE TRIGGER retained_film_decision BEFORE DELETE ON film_candidates FOR EACH ROW EXECUTE FUNCTION protect_film_history();

CREATE FUNCTION protect_completed_film_run() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.completed_at IS NOT NULL AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Completed Film Understanding runs are immutable';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_completed_film_run BEFORE UPDATE ON film_understanding_runs FOR EACH ROW EXECUTE FUNCTION protect_completed_film_run();
