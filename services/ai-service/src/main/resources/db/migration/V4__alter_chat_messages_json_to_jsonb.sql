-- Alter chat_messages table to use JSONB instead of JSON
-- This is required for proper Hibernate mapping

ALTER TABLE ai.chat_messages 
    ALTER COLUMN metadata TYPE JSONB USING metadata::jsonb,
    ALTER COLUMN extracted_records_json TYPE JSONB USING extracted_records_json::jsonb,
    ALTER COLUMN thinking_process TYPE JSONB USING thinking_process::jsonb;
