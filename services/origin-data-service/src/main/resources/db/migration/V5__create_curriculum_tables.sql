-- Origin Data Service - Curriculum Tables
-- Weekly and Daily Curriculum for DayCare/PreSchool

-- ============================================================================
-- 1. Weekly Curriculum Header
-- ============================================================================
CREATE TABLE origin_data.weekly_curriculum_header (
    id BIGINT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    origin_data_id BIGINT REFERENCES origin_data.origin_data(id),
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    title TEXT,
    theme TEXT,
    age_group TEXT,
    summary_text TEXT,
    meta_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_weekly_curriculum_origin_data ON origin_data.weekly_curriculum_header(origin_data_id);
CREATE INDEX idx_weekly_curriculum_school_classroom ON origin_data.weekly_curriculum_header(school_id, classroom_id);
CREATE INDEX idx_weekly_curriculum_week_dates ON origin_data.weekly_curriculum_header(week_start_date, week_end_date);

COMMENT ON TABLE origin_data.weekly_curriculum_header IS 'Weekly curriculum/newsletter for classroom';
COMMENT ON COLUMN origin_data.weekly_curriculum_header.meta_json IS 'Provider-specific metadata (letters, numbers, colors, patterns, teacher schedules, news & reminders)';

-- ============================================================================
-- 2. Weekly Curriculum Detail
-- ============================================================================
CREATE TABLE origin_data.weekly_curriculum_detail (
    id BIGINT PRIMARY KEY,
    header_id BIGINT NOT NULL REFERENCES origin_data.weekly_curriculum_header(id) ON DELETE CASCADE,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('WEEK', 'DAY')),
    day_of_week SMALLINT CHECK (day_of_week >= 1 AND day_of_week <= 7),
    item_type VARCHAR(50) NOT NULL CHECK (item_type IN ('THEME', 'TOPIC', 'SKILL', 'SUBJECT', 'EVENT', 'REMINDER', 'OTHER')),
    learning_domain VARCHAR(50) CHECK (learning_domain IN ('COGNITIVE', 'LANGUAGE', 'SOCIAL_EMOTIONAL', 'PHYSICAL', 'ART', 'MATH', 'OTHER')),
    label TEXT NOT NULL,
    value TEXT,
    start_at TIMESTAMP WITH TIME ZONE,
    end_at TIMESTAMP WITH TIME ZONE,
    display_order INTEGER NOT NULL DEFAULT 0,
    extra_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_weekly_detail_header ON origin_data.weekly_curriculum_detail(header_id);
CREATE INDEX idx_weekly_detail_scope ON origin_data.weekly_curriculum_detail(scope);
CREATE INDEX idx_weekly_detail_day ON origin_data.weekly_curriculum_detail(day_of_week);
CREATE INDEX idx_weekly_detail_type ON origin_data.weekly_curriculum_detail(item_type);
CREATE INDEX idx_weekly_detail_domain ON origin_data.weekly_curriculum_detail(learning_domain);

COMMENT ON TABLE origin_data.weekly_curriculum_detail IS 'Curriculum items optimized for timeline/calendar UI';
COMMENT ON COLUMN origin_data.weekly_curriculum_detail.scope IS 'Whether item applies to WEEK or specific DAY';
COMMENT ON COLUMN origin_data.weekly_curriculum_detail.day_of_week IS '1=Monday, 7=Sunday; NULL if scope=WEEK';
COMMENT ON COLUMN origin_data.weekly_curriculum_detail.extra_json IS 'Provider-specific extra data (location, icons, raw text snippet)';

-- ============================================================================
-- 3. Daily Curriculum Day (Header)
-- ============================================================================
CREATE TABLE origin_data.daily_curriculum_day (
    id BIGINT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    origin_data_id BIGINT REFERENCES origin_data.origin_data(id),
    date DATE NOT NULL,
    week_header_id BIGINT REFERENCES origin_data.weekly_curriculum_header(id),
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_daily_curriculum_origin_data ON origin_data.daily_curriculum_day(origin_data_id);
CREATE INDEX idx_daily_curriculum_school_classroom ON origin_data.daily_curriculum_day(school_id, classroom_id);
CREATE INDEX idx_daily_curriculum_date ON origin_data.daily_curriculum_day(date);
CREATE INDEX idx_daily_curriculum_week_header ON origin_data.daily_curriculum_day(week_header_id);

COMMENT ON TABLE origin_data.daily_curriculum_day IS 'Daily curriculum header describing planned activities by development domain';
COMMENT ON COLUMN origin_data.daily_curriculum_day.week_header_id IS 'Link to parent weekly curriculum if available';

-- ============================================================================
-- 4. Daily Curriculum Item (Detail)
-- ============================================================================
CREATE TABLE origin_data.daily_curriculum_item (
    id BIGINT PRIMARY KEY,
    day_id BIGINT NOT NULL REFERENCES origin_data.daily_curriculum_day(id) ON DELETE CASCADE,
    domain VARCHAR(50) NOT NULL CHECK (domain IN ('COGNITIVE', 'LANGUAGE', 'SOCIAL_EMOTIONAL', 'PHYSICAL', 'ART', 'MATH', 'OTHER')),
    activity TEXT NOT NULL,
    objective TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    extra_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_daily_item_day ON origin_data.daily_curriculum_item(day_id);
CREATE INDEX idx_daily_item_domain ON origin_data.daily_curriculum_item(domain);

COMMENT ON TABLE origin_data.daily_curriculum_item IS 'Domain-level planned activities for a specific day';
COMMENT ON COLUMN origin_data.daily_curriculum_item.domain IS 'Learning domain: COGNITIVE, LANGUAGE, SOCIAL_EMOTIONAL, PHYSICAL, ART, MATH, OTHER';
COMMENT ON COLUMN origin_data.daily_curriculum_item.activity IS 'Activity name (e.g., "Finger Painting")';
COMMENT ON COLUMN origin_data.daily_curriculum_item.objective IS 'Learning objective (e.g., "Promote sensory experience")';
