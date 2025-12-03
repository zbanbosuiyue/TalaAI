-- Add original user message reference to timeline entries
-- DB: PostgreSQL (see docker-compose.services.yml and logs)

ALTER TABLE origin_data.timeline_entries
    ADD COLUMN original_user_message TEXT;
