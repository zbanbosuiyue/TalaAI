-- V6: Add cache metrics to ai_model_usage table
-- Purpose: Track prompt caching usage and token savings

ALTER TABLE ai.ai_model_usage
ADD COLUMN cache_used BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN cached_tokens INTEGER,
ADD COLUMN dynamic_tokens INTEGER;

-- Add index for cache usage queries
CREATE INDEX idx_cache_used ON ai.ai_model_usage(cache_used);

-- Add comments
COMMENT ON COLUMN ai.ai_model_usage.cache_used IS 'Whether prompt caching was used for this request';
COMMENT ON COLUMN ai.ai_model_usage.cached_tokens IS 'Number of tokens saved by using cache (from Gemini cachedContentTokenCount)';
COMMENT ON COLUMN ai.ai_model_usage.dynamic_tokens IS 'Number of tokens actually sent (not from cache)';
