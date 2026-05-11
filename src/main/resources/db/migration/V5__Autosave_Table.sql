CREATE TABLE IF NOT EXISTS autosave_snapshot (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    snapshot_data TEXT NOT NULL,
    entity_count INTEGER NOT NULL
);
