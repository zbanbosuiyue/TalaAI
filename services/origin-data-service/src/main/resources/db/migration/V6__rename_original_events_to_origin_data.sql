-- Rename original_events table to origin_data to support both event and document types
-- This migration renames the table and columns to be more generic

-- Step 1: Rename the table
ALTER TABLE origin_data.original_events RENAME TO origin_data;

-- Step 2: Rename columns to be more generic
ALTER TABLE origin_data.origin_data RENAME COLUMN source_event_id TO source_data_id;
ALTER TABLE origin_data.origin_data RENAME COLUMN event_time TO data_time;

-- Step 3: Drop old indexes
DROP INDEX IF EXISTS origin_data.idx_original_event_profile_id;
DROP INDEX IF EXISTS origin_data.idx_original_event_source_type;
DROP INDEX IF EXISTS origin_data.idx_original_event_source_id;
DROP INDEX IF EXISTS origin_data.idx_original_event_time;
DROP INDEX IF EXISTS origin_data.idx_original_event_ai_processed;
DROP INDEX IF EXISTS origin_data.idx_original_event_source_unique;

-- Step 4: Create new indexes with updated names
CREATE INDEX idx_origin_data_profile_id ON origin_data.origin_data(profile_id);
CREATE INDEX idx_origin_data_source_type ON origin_data.origin_data(source_type);
CREATE INDEX idx_origin_data_source_id ON origin_data.origin_data(source_data_id);
CREATE INDEX idx_origin_data_time ON origin_data.origin_data(data_time);
CREATE INDEX idx_origin_data_ai_processed ON origin_data.origin_data(ai_processed);
CREATE UNIQUE INDEX idx_origin_data_source_unique ON origin_data.origin_data(source_type, source_data_id) WHERE source_data_id IS NOT NULL;

-- Step 5: Update foreign key references in child tables
-- Note: Foreign keys will be automatically updated by PostgreSQL when table is renamed

-- Step 6: Update comments
COMMENT ON TABLE origin_data.origin_data IS 'Top-level data sourcing table for all external data (events and documents)';
COMMENT ON COLUMN origin_data.origin_data.source_data_id IS 'External source identifier for idempotency';
COMMENT ON COLUMN origin_data.origin_data.data_time IS 'Timestamp of the data (event time or document date)';
COMMENT ON COLUMN origin_data.origin_data.raw_payload IS 'Original JSON payload from external source';
COMMENT ON COLUMN origin_data.origin_data.ai_processed IS 'Whether AI has processed this data';
