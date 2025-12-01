-- Add attachment support to chat_messages table
-- Enables multimodal AI interactions with images, audio, documents

ALTER TABLE ai.chat_messages
    ADD COLUMN attachment_ids JSONB NOT NULL DEFAULT '[]';

-- Index for querying messages with attachments
CREATE INDEX idx_chat_messages_has_attachments ON ai.chat_messages((jsonb_array_length(attachment_ids) > 0));

-- Comment
COMMENT ON COLUMN ai.chat_messages.attachment_ids IS 'File/media attachment IDs (JSONB array of Long)';
