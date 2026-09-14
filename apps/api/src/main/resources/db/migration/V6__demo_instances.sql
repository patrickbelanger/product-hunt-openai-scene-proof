ALTER TABLE projects ADD COLUMN demo_instance_id UUID;
ALTER TABLE visual_references ALTER COLUMN created_at SET DEFAULT clock_timestamp();
ALTER TABLE projects ADD COLUMN demo_template_version VARCHAR(80);
ALTER TABLE projects ADD COLUMN demo_retired BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE projects ADD CONSTRAINT demo_identity_complete CHECK (
    (demo_instance_id IS NULL AND demo_template_version IS NULL AND NOT demo_retired)
    OR (demo_instance_id IS NOT NULL AND demo_template_version IS NOT NULL)
);
CREATE UNIQUE INDEX one_current_demo_copy ON projects(demo_instance_id)
    WHERE demo_instance_id IS NOT NULL AND NOT demo_retired;

CREATE TABLE demo_replacements (
    source_project_id UUID PRIMARY KEY REFERENCES projects(id),
    replacement_project_id UUID NOT NULL UNIQUE REFERENCES projects(id)
);
