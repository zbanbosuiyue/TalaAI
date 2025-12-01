-- Chat Messages Table
-- Stores all chat interactions between users and AI assistant

CREATE TABLE IF NOT EXISTS ai.chat_messages (
    id BIGINT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'EVENT', 'SUGGESTION')),
    metadata JSONB,
    extracted_records_json JSONB,
    thinking_process JSONB,
    interaction_type VARCHAR(50),
    confidence DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_chat_messages_profile_id ON ai.chat_messages(profile_id);
CREATE INDEX idx_chat_messages_user_id ON ai.chat_messages(user_id);
CREATE INDEX idx_chat_messages_created_at ON ai.chat_messages(created_at);
CREATE INDEX idx_chat_messages_deleted_at ON ai.chat_messages(deleted_at);

-- Comments
COMMENT ON TABLE ai.chat_messages IS 'Chat messages between users and AI assistant';
COMMENT ON COLUMN ai.chat_messages.id IS 'Snowflake ID';
COMMENT ON COLUMN ai.chat_messages.profile_id IS 'Baby profile ID';
COMMENT ON COLUMN ai.chat_messages.user_id IS 'User ID';
COMMENT ON COLUMN ai.chat_messages.role IS 'Message role: USER, ASSISTANT, SYSTEM';
COMMENT ON COLUMN ai.chat_messages.content IS 'Message content';
COMMENT ON COLUMN ai.chat_messages.message_type IS 'Message type: TEXT, EVENT, SUGGESTION';
COMMENT ON COLUMN ai.chat_messages.metadata IS 'Raw AI response metadata (JSON)';
COMMENT ON COLUMN ai.chat_messages.extracted_records_json IS 'Extracted event records (JSON)';
COMMENT ON COLUMN ai.chat_messages.thinking_process IS 'AI thinking process details (JSON)';
COMMENT ON COLUMN ai.chat_messages.interaction_type IS 'Classification result: DATA_RECORDING, QUESTION_ANSWERING, etc.';
COMMENT ON COLUMN ai.chat_messages.confidence IS 'AI confidence score (0.0-1.0)';
COMMENT ON COLUMN ai.chat_messages.created_at IS 'Creation timestamp';
COMMENT ON COLUMN ai.chat_messages.updated_at IS 'Last update timestamp';
COMMENT ON COLUMN ai.chat_messages.deleted_at IS 'Soft delete timestamp';
