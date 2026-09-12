CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL CHECK (char_length(trim(name)) > 0),
    description VARCHAR(2000) NOT NULL DEFAULT '',
    rules VARCHAR(8000) NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX projects_created_at_idx ON projects (created_at DESC, id DESC);
