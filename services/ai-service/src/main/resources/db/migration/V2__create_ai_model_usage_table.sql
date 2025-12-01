-- Create ai schema if not exists
CREATE SCHEMA IF NOT EXISTS ai;

-- Create ai_model_usage table for tracking AI API usage
CREATE TABLE IF NOT EXISTS ai.ai_model_usage (
    id BIGINT PRIMARY KEY,
    profile_id BIGINT,
    user_id BIGINT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    user_message_length INTEGER,
    static_prompt_length INTEGER,
    dynamic_context_length INTEGER,
    estimated_cost_usd DOUBLE PRECISION,
    input_message TEXT,
    response_length INTEGER,
    output_message TEXT,
    raw_json_response TEXT,
    is_success BOOLEAN NOT NULL,
    error_message TEXT,
    latency_ms INTEGER,
    has_attachments BOOLEAN NOT NULL DEFAULT FALSE,
    attachment_count INTEGER,
    stage_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX idx_ai_model_usage_profile_id ON ai.ai_model_usage(profile_id);
CREATE INDEX idx_ai_model_usage_user_id ON ai.ai_model_usage(user_id);
CREATE INDEX idx_ai_model_usage_created_at ON ai.ai_model_usage(created_at);
CREATE INDEX idx_ai_model_usage_operation_type ON ai.ai_model_usage(operation_type);
CREATE INDEX idx_ai_model_usage_stage_name ON ai.ai_model_usage(stage_name);
CREATE INDEX idx_ai_model_usage_deleted_at ON ai.ai_model_usage(deleted_at);

-- Add comment
COMMENT ON TABLE ai.ai_model_usage IS 'AI model usage tracking for cost analysis and debugging';
