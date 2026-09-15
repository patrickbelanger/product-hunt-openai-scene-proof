CREATE TABLE paid_run_reservations (
    run_id UUID PRIMARY KEY,
    reserved_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX paid_run_reservations_time ON paid_run_reservations (reserved_at);
