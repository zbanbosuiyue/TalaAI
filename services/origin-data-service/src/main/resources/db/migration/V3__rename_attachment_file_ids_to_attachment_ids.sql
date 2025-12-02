-- V3: Rename attachment_file_ids to attachment_ids for consistency with BaseAttachmentEntity

ALTER TABLE origin_data.original_events
    RENAME COLUMN attachment_file_ids TO attachment_ids;

COMMENT ON COLUMN origin_data.original_events.attachment_ids IS 'Attachment resource IDs stored as JSONB array';
