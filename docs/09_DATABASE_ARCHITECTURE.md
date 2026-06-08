# LEARN WITH VELMORTH — DATABASE ARCHITECTURE
## Section 9: Complete Database Schema

---

## 9.1 DATABASE OVERVIEW

Velmorth uses a **polyglot persistence** architecture — different databases for different data access patterns:

| Database | Technology | Purpose |
|---|---|---|
| Primary OLTP | PostgreSQL 16 | All transactional data |
| Cache | Redis Cluster | Session data, leaderboards, rate limiting |
| Search | Elasticsearch 8 | Full-text search, analytics |
| Graph | Neo4j 5 | Knowledge graph, user relationships |
| Analytics | ClickHouse | Event analytics, time-series |
| Object Storage | MinIO / GCS | Media files (audio, images, video) |
| Message Broker | Apache Kafka | Event streaming |

---

## 9.2 POSTGRESQL SCHEMA — COMPLETE

### Schema Organization
```sql
-- All tables organized into logical schemas
CREATE SCHEMA auth;         -- Authentication and authorization
CREATE SCHEMA users;        -- User profiles and preferences
CREATE SCHEMA learning;     -- Courses, lessons, content
CREATE SCHEMA progress;     -- User progress tracking
CREATE SCHEMA gamification; -- XP, levels, achievements, streaks
CREATE SCHEMA community;    -- Social features, guilds, forums
CREATE SCHEMA payments;     -- Subscriptions and transactions
CREATE SCHEMA analytics;    -- Learning analytics aggregates
CREATE SCHEMA ai;           -- AI session data and prompts
CREATE SCHEMA audit;        -- Audit trail for all mutations
```

---

### SCHEMA: auth

```sql
-- Users authentication table
CREATE TABLE auth.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) UNIQUE,              -- NULL for social-only accounts
    email_verified  BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    phone           VARCHAR(20),
    phone_verified  BOOLEAN DEFAULT FALSE,
    password_hash   VARCHAR(255),                     -- bcrypt, NULL if social-only
    
    -- Auth state
    is_active       BOOLEAN DEFAULT TRUE,
    is_banned       BOOLEAN DEFAULT FALSE,
    ban_reason      VARCHAR(500),
    ban_expires_at  TIMESTAMPTZ,
    
    -- Security
    failed_login_attempts INTEGER DEFAULT 0,
    lockout_until   TIMESTAMPTZ,
    last_login_at   TIMESTAMPTZ,
    last_login_ip   INET,
    last_login_country CHAR(2),
    
    -- 2FA
    totp_secret     VARCHAR(64) ENCRYPTED,            -- Encrypted at application level
    totp_enabled    BOOLEAN DEFAULT FALSE,
    backup_codes    TEXT[],                           -- Hashed backup codes
    
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    deleted_at      TIMESTAMPTZ                       -- Soft delete
);

CREATE INDEX idx_auth_users_email ON auth.users(email) WHERE email IS NOT NULL;
CREATE INDEX idx_auth_users_active ON auth.users(is_active) WHERE is_active = TRUE;

-- OAuth provider connections
CREATE TABLE auth.oauth_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    provider        VARCHAR(50) NOT NULL,             -- 'google', 'apple', 'facebook'
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email  VARCHAR(255),
    access_token    TEXT ENCRYPTED,
    refresh_token   TEXT ENCRYPTED,
    token_expires_at TIMESTAMPTZ,
    raw_profile     JSONB,                           -- Complete provider profile
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    
    UNIQUE(provider, provider_user_id)
);

CREATE INDEX idx_oauth_accounts_user ON auth.oauth_accounts(user_id);
CREATE INDEX idx_oauth_accounts_provider ON auth.oauth_accounts(provider, provider_user_id);

-- Active sessions
CREATE TABLE auth.sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    refresh_token   VARCHAR(255) UNIQUE NOT NULL,     -- Stored hashed
    device_id       VARCHAR(255),
    device_name     VARCHAR(255),
    device_platform VARCHAR(50),                      -- 'ios', 'android', 'web'
    ip_address      INET,
    user_agent      TEXT,
    country_code    CHAR(2),
    is_valid        BOOLEAN DEFAULT TRUE,
    last_used_at    TIMESTAMPTZ DEFAULT now(),
    created_at      TIMESTAMPTZ DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL              -- 30 days from creation
);

CREATE INDEX idx_sessions_user_id ON auth.sessions(user_id);
CREATE INDEX idx_sessions_refresh_token ON auth.sessions(refresh_token);
CREATE INDEX idx_sessions_expires ON auth.sessions(expires_at);

-- Email verification tokens
CREATE TABLE auth.verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token       VARCHAR(255) UNIQUE NOT NULL,
    token_type  VARCHAR(50) NOT NULL,                -- 'email_verify', 'password_reset', 'change_email'
    new_value   VARCHAR(255),                        -- New email (for change_email type)
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT now()
);
```

---

### SCHEMA: users

```sql
-- Core user profile
CREATE TABLE users.profiles (
    id                  UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Identity
    display_name        VARCHAR(100) NOT NULL,
    username            VARCHAR(50) UNIQUE NOT NULL,
    bio                 VARCHAR(300),
    avatar_url          TEXT,
    avatar_type         VARCHAR(20) DEFAULT 'static',  -- 'static', 'animated', 'premium'
    
    -- Location and Language
    country_code        CHAR(2),
    timezone            VARCHAR(50),
    interface_language  VARCHAR(10) DEFAULT 'en',       -- BCP-47 language tag
    native_languages    VARCHAR(10)[] DEFAULT '{"en"}', -- BCP-47 array
    
    -- Learning goals
    primary_goal        VARCHAR(50),                    -- 'travel', 'career', 'study', etc.
    secondary_goals     VARCHAR(50)[],
    
    -- Preferences
    receive_marketing   BOOLEAN DEFAULT FALSE,
    receive_weekly_report BOOLEAN DEFAULT TRUE,
    show_on_leaderboard BOOLEAN DEFAULT TRUE,
    profile_visibility  VARCHAR(20) DEFAULT 'public',   -- 'public', 'friends', 'private'
    
    -- Status flags
    onboarding_complete BOOLEAN DEFAULT FALSE,
    onboarding_step     INTEGER DEFAULT 0,
    is_premium          BOOLEAN DEFAULT FALSE,
    premium_expires_at  TIMESTAMPTZ,
    premium_plan_id     UUID,
    
    -- Gamification display
    selected_pet_id     UUID,
    selected_border_id  UUID,
    selected_title      VARCHAR(100),
    pinned_achievements UUID[],                         -- Up to 3 pinned
    
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_profiles_username ON users.profiles(username);
CREATE INDEX idx_profiles_country ON users.profiles(country_code);

-- Notification preferences (separate for granularity)
CREATE TABLE users.notification_preferences (
    user_id             UUID PRIMARY KEY REFERENCES users.profiles(id) ON DELETE CASCADE,
    
    -- Push notifications
    push_enabled        BOOLEAN DEFAULT TRUE,
    streak_reminder     BOOLEAN DEFAULT TRUE,
    streak_reminder_time TIME DEFAULT '20:00',
    lesson_reminder     BOOLEAN DEFAULT TRUE,
    lesson_reminder_time TIME DEFAULT '08:30',
    guild_notifications BOOLEAN DEFAULT TRUE,
    friend_activity     BOOLEAN DEFAULT TRUE,
    achievement_alerts  BOOLEAN DEFAULT TRUE,
    boss_battle_alerts  BOOLEAN DEFAULT TRUE,
    weekly_report       BOOLEAN DEFAULT TRUE,
    
    -- Email notifications
    email_enabled       BOOLEAN DEFAULT TRUE,
    email_weekly_digest BOOLEAN DEFAULT TRUE,
    email_achievements  BOOLEAN DEFAULT FALSE,
    email_marketing     BOOLEAN DEFAULT FALSE,
    
    -- Optimal send time (calculated by analytics)
    optimal_push_hour   SMALLINT,                       -- 0-23
    optimal_push_day    SMALLINT,                       -- 0-6 (Mon-Sun)
    
    updated_at          TIMESTAMPTZ DEFAULT now()
);

-- User relationships
CREATE TABLE users.friendships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id    UUID NOT NULL REFERENCES users.profiles(id) ON DELETE CASCADE,
    addressee_id    UUID NOT NULL REFERENCES users.profiles(id) ON DELETE CASCADE,
    status          VARCHAR(20) DEFAULT 'pending',  -- 'pending', 'accepted', 'blocked'
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    
    UNIQUE(requester_id, addressee_id),
    CHECK(requester_id != addressee_id)
);

CREATE INDEX idx_friendships_requester ON users.friendships(requester_id);
CREATE INDEX idx_friendships_addressee ON users.friendships(addressee_id);
CREATE INDEX idx_friendships_status ON users.friendships(status);

-- Learning DNA profiles
CREATE TABLE users.learning_dna (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID UNIQUE NOT NULL REFERENCES users.profiles(id) ON DELETE CASCADE,
    version         INTEGER DEFAULT 1,
    
    -- Cognitive style
    primary_cognitive_style     VARCHAR(30),  -- 'visual', 'auditory', 'kinesthetic', 'reading'
    secondary_cognitive_style   VARCHAR(30),
    context_dependency          DECIMAL(3,2), -- 0.00-1.00
    chunk_size_preference       SMALLINT,     -- 3-7
    elaboration_preference      DECIMAL(3,2), -- 0.00-1.00
    
    -- Memory profile
    acquisition_speed           DECIMAL(3,2),
    consolidation_speed         DECIMAL(3,2),
    decay_rate_multiplier       DECIMAL(4,2), -- Multiplier on standard SRS intervals
    interference_sensitivity    DECIMAL(3,2),
    optimal_session_minutes     SMALLINT,
    max_new_items_per_session   SMALLINT,
    review_to_new_ratio         DECIMAL(3,2),
    
    -- Motivation profile
    primary_motivator           VARCHAR(30),  -- 'achievement', 'social', 'streak', 'story'
    gamification_responsiveness DECIMAL(3,2),
    competitive_preference      DECIMAL(3,2),
    social_accountability_pref  DECIMAL(3,2),
    challenge_tolerance         DECIMAL(3,2),
    novelty_preference          DECIMAL(3,2),
    reward_sensitivity_type     VARCHAR(20),  -- 'variable_ratio', 'fixed_ratio', 'immediate'
    
    -- Performance profile (JSONB for flexibility)
    accuracy_by_exercise_type   JSONB DEFAULT '{}',
    accuracy_by_skill           JSONB DEFAULT '{}',
    accuracy_trend              VARCHAR(20),   -- 'improving', 'stable', 'declining'
    
    -- Schedule profile
    peak_performance_hours      SMALLINT[],   -- Array of hours (0-23)
    optimal_session_start_hour  SMALLINT,
    session_length_sweet_spot   SMALLINT,     -- Minutes
    days_of_week_engagement     JSONB DEFAULT '{}', -- {mon: 0.8, tue: 0.9, ...}
    
    -- Error profile (JSONB for evolving patterns)
    common_error_types          JSONB DEFAULT '[]',
    persistent_confusions       JSONB DEFAULT '[]', -- [{word1, word2, count}]
    pronunciation_weak_phonemes TEXT[],
    
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);
```

---

### SCHEMA: learning

```sql
-- Languages catalog
CREATE TABLE learning.languages (
    code            VARCHAR(10) PRIMARY KEY,  -- BCP-47 language code
    name_en         VARCHAR(100) NOT NULL,
    name_native     VARCHAR(100) NOT NULL,
    script          VARCHAR(30),              -- 'latin', 'cyrillic', 'arabic', etc.
    direction       CHAR(3) DEFAULT 'ltr',    -- 'ltr' or 'rtl'
    is_active       BOOLEAN DEFAULT FALSE,    -- Only active languages shown
    is_beta         BOOLEAN DEFAULT FALSE,
    flag_emoji      VARCHAR(10),
    flag_icon_url   TEXT,
    speaker_count_m INTEGER,                  -- Millions of native speakers
    difficulty_for_english_speakers SMALLINT, -- 1-4 (FSI difficulty rating)
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- Course structure
CREATE TABLE learning.courses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_language VARCHAR(10) NOT NULL REFERENCES learning.languages(code),
    instruction_language VARCHAR(10) NOT NULL REFERENCES learning.languages(code),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    version         VARCHAR(20) DEFAULT '1.0.0',
    is_active       BOOLEAN DEFAULT TRUE,
    learner_count   INTEGER DEFAULT 0,        -- Denormalized for performance
    
    -- Metadata
    min_age         SMALLINT DEFAULT 7,
    max_age         SMALLINT,
    proficiency_start VARCHAR(5) DEFAULT 'A0', -- CEFR
    proficiency_end   VARCHAR(5) DEFAULT 'C2',
    
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    
    UNIQUE(target_language, instruction_language)
);

-- Course units (top-level grouping)
CREATE TABLE learning.units (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID NOT NULL REFERENCES learning.courses(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    icon_url        TEXT,
    color_hex       CHAR(7),                  -- Unit theme color
    sequence_order  SMALLINT NOT NULL,
    cefr_level      VARCHAR(5),               -- A1, A2, B1, B2, C1, C2
    is_active       BOOLEAN DEFAULT TRUE,
    
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    
    UNIQUE(course_id, sequence_order)
);

-- Lessons within units
CREATE TABLE learning.lessons (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id         UUID NOT NULL REFERENCES learning.units(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    sequence_order  SMALLINT NOT NULL,
    lesson_type     VARCHAR(30) NOT NULL,     -- 'standard', 'review', 'story', 'boss', 'challenge'
    cefr_level      VARCHAR(5),
    estimated_duration_minutes SMALLINT DEFAULT 10,
    
    -- Content
    is_ai_generated BOOLEAN DEFAULT FALSE,
    generation_params JSONB,                  -- Params used to generate this lesson
    content_version INTEGER DEFAULT 1,
    
    -- Prerequisites
    prerequisite_lesson_ids UUID[] DEFAULT '{}',
    
    -- Metadata
    skills_taught   TEXT[],                   -- e.g., ['vocabulary', 'grammar', 'listening']
    topic_tags      TEXT[],                   -- e.g., ['food', 'travel', 'present_tense']
    
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_lessons_unit ON learning.lessons(unit_id);
CREATE INDEX idx_lessons_type ON learning.lessons(lesson_type);

-- Exercise items (the atomic learning units)
CREATE TABLE learning.exercise_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id       UUID REFERENCES learning.lessons(id) ON DELETE SET NULL,  -- Can be unassigned (pool)
    course_id       UUID NOT NULL REFERENCES learning.courses(id),
    
    -- Item identity
    item_type       VARCHAR(30) NOT NULL,     -- 'vocabulary', 'grammar', 'pronunciation', 'cultural'
    exercise_type   VARCHAR(30) NOT NULL,     -- 'multiple_choice', 'translation', 'speak', etc.
    cefr_level      VARCHAR(5) NOT NULL,
    difficulty      DECIMAL(3,2) NOT NULL,    -- 0.0-1.0
    
    -- Content (JSONB for flexibility across exercise types)
    content         JSONB NOT NULL,           -- Exercise-type-specific content
    
    -- For vocabulary items
    target_word     VARCHAR(500),             -- The word/phrase being taught
    target_word_ipa  VARCHAR(500),            -- IPA pronunciation
    target_translation VARCHAR(500),          -- Primary translation
    alternative_translations TEXT[],          -- Other acceptable translations
    
    -- Media
    audio_url       TEXT,                     -- TTS audio URL
    image_url       TEXT,
    
    -- Quality metrics (from user performance)
    avg_accuracy    DECIMAL(5,4),             -- Average correct rate (0-1)
    avg_response_time_ms INTEGER,
    discrimination  DECIMAL(4,3),             -- IRT discrimination parameter
    difficulty_irt  DECIMAL(4,3),             -- IRT difficulty (b parameter)
    guessing_param  DECIMAL(4,3),             -- IRT guessing (c parameter)
    
    -- Metadata
    source          VARCHAR(30) DEFAULT 'human', -- 'human', 'ai_generated'
    is_active       BOOLEAN DEFAULT TRUE,
    review_status   VARCHAR(20) DEFAULT 'approved',
    
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_exercise_items_lesson ON learning.exercise_items(lesson_id);
CREATE INDEX idx_exercise_items_course_level ON learning.exercise_items(course_id, cefr_level);
CREATE INDEX idx_exercise_items_type ON learning.exercise_items(item_type, exercise_type);
CREATE INDEX idx_exercise_items_word ON learning.exercise_items(target_word) WHERE target_word IS NOT NULL;
```

---

### SCHEMA: progress

```sql
-- User's enrollment in courses
CREATE TABLE progress.course_enrollments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.profiles(id) ON DELETE CASCADE,
    course_id       UUID NOT NULL REFERENCES learning.courses(id),
    
    enrolled_at     TIMESTAMPTZ DEFAULT now(),
    last_active_at  TIMESTAMPTZ DEFAULT now(),
    
    -- Current position
    current_unit_id UUID REFERENCES learning.units(id),
    current_lesson_id UUID REFERENCES learning.lessons(id),
    
    -- Aggregate progress
    overall_progress DECIMAL(5,4) DEFAULT 0,  -- 0.0-1.0
    lessons_completed INTEGER DEFAULT 0,
    items_mastered    INTEGER DEFAULT 0,
    total_study_minutes INTEGER DEFAULT 0,
    
    -- Current level estimate
    estimated_cefr_level VARCHAR(5) DEFAULT 'A0',
    last_assessment_at   TIMESTAMPTZ,
    
    is_active       BOOLEAN DEFAULT TRUE,
    is_completed    BOOLEAN DEFAULT FALSE,
    completed_at    TIMESTAMPTZ,
    
    UNIQUE(user_id, course_id)
);

CREATE INDEX idx_enrollments_user ON progress.course_enrollments(user_id);
CREATE INDEX idx_enrollments_active ON progress.course_enrollments(user_id, is_active);

-- Lesson completion records
CREATE TABLE progress.lesson_completions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.profiles(id),
    lesson_id       UUID NOT NULL REFERENCES learning.lessons(id),
    session_id      UUID NOT NULL,            -- Lesson session identifier
    
    -- Results
    score           DECIMAL(5,4) NOT NULL,    -- 0.0-1.0
    accuracy        DECIMAL(5,4) NOT NULL,
    stars_earned    SMALLINT NOT NULL,         -- 1, 2, or 3
    xp_earned       INTEGER NOT NULL,
    time_taken_seconds INTEGER NOT NULL,
    
    -- Detail
    items_total     SMALLINT NOT NULL,
    items_correct   SMALLINT NOT NULL,
    items_skipped   SMALLINT DEFAULT 0,
    hints_used      SMALLINT DEFAULT 0,
    hearts_lost     SMALLINT DEFAULT 0,
    
    is_perfect      BOOLEAN DEFAULT FALSE,
    is_first_completion BOOLEAN DEFAULT TRUE,
    
    completed_at    TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_completions_user ON progress.lesson_completions(user_id);
CREATE INDEX idx_completions_lesson ON progress.lesson_completions(lesson_id);
CREATE INDEX idx_completions_date ON progress.lesson_completions(completed_at DESC);

-- Individual answer records
CREATE TABLE progress.answer_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.profiles(id),
    session_id      UUID NOT NULL,
    item_id         UUID NOT NULL REFERENCES learning.exercise_items(id),
    
    -- Answer details
    user_answer     TEXT,                     -- What the user answered (serialized)
    correct_answer  TEXT,                     -- The correct answer at time of answering
    is_correct      BOOLEAN NOT NULL,
    quality_score   SMALLINT,                 -- 0-5 (SM-18 quality rating)
    response_time_ms INTEGER,
    
    -- Context
    hint_was_used   BOOLEAN DEFAULT FALSE,
    was_reviewed    BOOLEAN DEFAULT FALSE,    -- True if this was a review (not new item)
    difficulty_at_time DECIMAL(3,2),          -- Item difficulty when answered
    
    -- Voice-specific (if speaking exercise)
    pronunciation_score DECIMAL(5,4),
    phoneme_scores  JSONB,                    -- Per-phoneme scores
    
    answered_at     TIMESTAMPTZ DEFAULT now()
)
PARTITION BY RANGE (answered_at);

-- Monthly partitions for answer_records (high volume)
CREATE TABLE progress.answer_records_2025_01 PARTITION OF progress.answer_records
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
-- (Partitions created automatically via pg_partman)

CREATE INDEX idx_answers_user_session ON progress.answer_records(user_id, session_id);
CREATE INDEX idx_answers_item ON progress.answer_records(item_id);
CREATE INDEX idx_answers_date ON progress.answer_records(answered_at DESC);

-- SRS memory state (per user, per item)
CREATE TABLE progress.memory_states (
    user_id         UUID NOT NULL REFERENCES users.profiles(id) ON DELETE CASCADE,
    item_id         UUID NOT NULL REFERENCES learning.exercise_items(id) ON DELETE CASCADE,
    
    -- SRS state (SM-18 + FSRS hybrid)
    easiness_factor DECIMAL(4,2) DEFAULT 2.50,
    interval_days   SMALLINT DEFAULT 0,
    repetitions     SMALLINT DEFAULT 0,
    
    -- FSRS parameters
    stability       DECIMAL(8,4) DEFAULT 0,   -- S: days of stability
    difficulty_fsrs DECIMAL(5,4) DEFAULT 0.3, -- D: inherent difficulty
    retrievability  DECIMAL(5,4) DEFAULT 0,   -- R: current recall probability
    
    -- Velmorth enhanced
    mastery_score   DECIMAL(5,4) DEFAULT 0,
    mastery_level   VARCHAR(20) DEFAULT 'not_started',
    emotional_encoding DECIMAL(3,2) DEFAULT 0,
    context_diversity DECIMAL(3,2) DEFAULT 0,
    modalities_practiced JSONB DEFAULT '{"visual":false,"auditory":false,"production":false}',
    
    -- Schedule
    first_introduced_at TIMESTAMPTZ,
    last_reviewed_at    TIMESTAMPTZ,
    next_review_at      TIMESTAMPTZ,
    
    -- Stats
    total_reviews   SMALLINT DEFAULT 0,
    correct_reviews SMALLINT DEFAULT 0,
    
    PRIMARY KEY (user_id, item_id)
);

CREATE INDEX idx_memory_states_next_review ON progress.memory_states(user_id, next_review_at)
    WHERE mastery_level != 'not_started';
CREATE INDEX idx_memory_states_mastery ON progress.memory_states(user_id, mastery_score);
```

---

### SCHEMA: gamification

```sql
-- XP transaction ledger
CREATE TABLE gamification.xp_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.profiles(id),
    
    amount          INTEGER NOT NULL,         -- Base XP before multipliers
    multiplier      DECIMAL(4,2) DEFAULT 1.0,
    net_amount      INTEGER NOT NULL,         -- After multipliers
    
    source_type     VARCHAR(50) NOT NULL,     -- 'lesson', 'achievement', 'streak', 'challenge', etc.
    source_id       UUID,                     -- Reference to source entity
    source_metadata JSONB,                    -- Extra context
    
    season_id       INTEGER,                  -- Which season this belongs to
    created_at      TIMESTAMPTZ DEFAULT now()
)
PARTITION BY RANGE (created_at);

CREATE INDEX idx_xp_transactions_user ON gamification.xp_transactions(user_id, created_at DESC);
CREATE INDEX idx_xp_transactions_season ON gamification.xp_transactions(user_id, season_id);

-- User XP and level summary (maintained by triggers)
CREATE TABLE gamification.user_xp_summary (
    user_id         UUID PRIMARY KEY REFERENCES users.profiles(id) ON DELETE CASCADE,
    
    total_xp        BIGINT DEFAULT 0,
    current_level   SMALLINT DEFAULT 1,
    level_xp_start  BIGINT DEFAULT 0,        -- XP at start of current level
    xp_to_next_level INTEGER DEFAULT 300,
    
    -- Season XP
    current_season_id   INTEGER,
    current_season_xp   BIGINT DEFAULT 0,
    season_rank         INTEGER,              -- Position in season leaderboard
    
    -- Periodic summaries
    daily_xp        INTEGER DEFAULT 0,
    daily_xp_reset_at TIMESTAMPTZ,
    weekly_xp       INTEGER DEFAULT 0,
    weekly_xp_reset_at TIMESTAMPTZ,
    monthly_xp      INTEGER DEFAULT 0,
    monthly_xp_reset_at TIMESTAMPTZ,
    
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- Streak records
CREATE TABLE gamification.streaks (
    user_id         UUID PRIMARY KEY REFERENCES users.profiles(id) ON DELETE CASCADE,
    
    current_streak  INTEGER DEFAULT 0,
    longest_streak  INTEGER DEFAULT 0,
    
    -- Current streak dates
    streak_started_at    DATE,
    streak_last_updated  DATE,
    
    -- Freeze inventory
    freeze_count    SMALLINT DEFAULT 0,      -- Number of streak freezes owned
    freeze_used_dates DATE[],               -- Dates when freeze was applied
    
    -- Repair option
    repair_available    BOOLEAN DEFAULT FALSE,
    repair_cost_gems    INTEGER,
    repair_expires_at   TIMESTAMPTZ,
    
    -- History
    streak_history  JSONB DEFAULT '[]',      -- [{start, end, length}] for all past streaks
    
    -- Statistics
    total_study_days INTEGER DEFAULT 0,
    total_streak_days INTEGER DEFAULT 0,    -- Total days in any streak
    
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- Achievement catalog
CREATE TABLE gamification.achievement_definitions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) UNIQUE NOT NULL,  -- e.g., 'streak_7_days'
    name            VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    hint            TEXT,                    -- Hint shown when locked
    category        VARCHAR(50) NOT NULL,    -- 'streaks', 'learning', 'social', 'special'
    
    -- Visual
    icon_url        TEXT,
    badge_url       TEXT,                   -- Full badge image
    rarity          VARCHAR(20) DEFAULT 'common', -- 'common', 'rare', 'epic', 'legendary'
    
    -- Rewards
    xp_reward       INTEGER DEFAULT 0,
    gem_reward      INTEGER DEFAULT 0,
    title_reward    VARCHAR(100),           -- Title granted if any
    badge_id        UUID,                   -- Badge granted
    
    -- Condition (evaluated by achievement engine)
    condition_type  VARCHAR(50) NOT NULL,   -- 'streak', 'count', 'milestone', 'special'
    condition_params JSONB NOT NULL,        -- Parameters for condition evaluation
    
    -- Display
    is_secret       BOOLEAN DEFAULT FALSE,  -- Hidden achievement
    sort_order      INTEGER DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- User achievement awards
CREATE TABLE gamification.user_achievements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.profiles(id) ON DELETE CASCADE,
    achievement_id  UUID NOT NULL REFERENCES gamification.achievement_definitions(id),
    
    -- Progress (for progressive achievements)
    current_progress INTEGER DEFAULT 0,
    target_progress  INTEGER,
    is_unlocked     BOOLEAN DEFAULT FALSE,
    unlocked_at     TIMESTAMPTZ,
    
    -- Rewards claimed
    xp_claimed      BOOLEAN DEFAULT FALSE,
    gems_claimed    BOOLEAN DEFAULT FALSE,
    
    UNIQUE(user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user ON gamification.user_achievements(user_id);
CREATE INDEX idx_user_achievements_unlocked ON gamification.user_achievements(user_id, is_unlocked);

-- Virtual currency (Gems) ledger
CREATE TABLE gamification.gem_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.profiles(id),
    
    amount          INTEGER NOT NULL,        -- Positive = earned, Negative = spent
    source_type     VARCHAR(50) NOT NULL,    -- 'purchase', 'achievement', 'mission', 'spend_hearts', etc.
    source_id       UUID,
    description     VARCHAR(500),
    
    balance_after   INTEGER NOT NULL,        -- Denormalized for quick balance queries
    created_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_gem_transactions_user ON gamification.gem_transactions(user_id, created_at DESC);

-- Gem balance summary (maintained by triggers)
CREATE TABLE gamification.gem_balances (
    user_id         UUID PRIMARY KEY REFERENCES users.profiles(id) ON DELETE CASCADE,
    balance         INTEGER DEFAULT 0,
    lifetime_earned INTEGER DEFAULT 0,
    lifetime_spent  INTEGER DEFAULT 0,
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- League and rank system
CREATE TABLE gamification.leagues (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.profiles(id),
    
    -- Current position
    current_league  VARCHAR(20) DEFAULT 'bronze',
    current_cohort_id UUID,               -- Which cohort of 50 users they're in
    current_week_xp  INTEGER DEFAULT 0,
    current_rank_in_cohort SMALLINT,
    
    -- Season
    season_id       INTEGER,
    
    -- History
    promotions      INTEGER DEFAULT 0,
    demotions       INTEGER DEFAULT 0,
    
    -- Week boundaries
    week_start      DATE,
    week_end        DATE,
    
    updated_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, week_start)
);

-- League cohorts (groups of 50 users for weekly competition)
CREATE TABLE gamification.league_cohorts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_tier     VARCHAR(20) NOT NULL,
    week_start      DATE NOT NULL,
    week_end        DATE NOT NULL,
    member_count    SMALLINT DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- Daily and weekly missions
CREATE TABLE gamification.missions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.profiles(id),
    
    mission_type    VARCHAR(50) NOT NULL,   -- 'daily', 'weekly'
    mission_code    VARCHAR(100) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    icon            VARCHAR(10),
    
    -- Target and progress
    target_value    INTEGER NOT NULL,
    current_value   INTEGER DEFAULT 0,
    is_completed    BOOLEAN DEFAULT FALSE,
    completed_at    TIMESTAMPTZ,
    
    -- Rewards
    xp_reward       INTEGER DEFAULT 0,
    gem_reward      INTEGER DEFAULT 0,
    
    -- Validity
    valid_from      TIMESTAMPTZ NOT NULL,
    valid_until     TIMESTAMPTZ NOT NULL,
    
    created_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_missions_user_active ON gamification.missions(user_id, valid_until)
    WHERE is_completed = FALSE;
```

---

### SCHEMA: community

```sql
-- Guilds
CREATE TABLE community.guilds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    short_code      VARCHAR(10) UNIQUE,     -- For invitation
    
    -- Visual
    emblem_url      TEXT,
    banner_url      TEXT,
    color_primary   CHAR(7),               -- HEX color
    
    -- Config
    language_focus  VARCHAR(10),           -- Target language or NULL for multi-language
    is_open         BOOLEAN DEFAULT TRUE,  -- Open to join or invite-only
    max_members     SMALLINT DEFAULT 50,
    min_level_to_join SMALLINT DEFAULT 1,
    
    -- Stats (denormalized)
    member_count    SMALLINT DEFAULT 0,
    weekly_xp       BIGINT DEFAULT 0,
    total_xp        BIGINT DEFAULT 0,
    guild_tier      VARCHAR(20) DEFAULT 'starter',
    
    -- Metadata
    founded_by      UUID REFERENCES users.profiles(id),
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_guilds_language ON community.guilds(language_focus);
CREATE INDEX idx_guilds_open ON community.guilds(is_open) WHERE is_open = TRUE;
CREATE INDEX idx_guilds_weekly_xp ON community.guilds(weekly_xp DESC);

-- Guild memberships
CREATE TABLE community.guild_memberships (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guild_id    UUID NOT NULL REFERENCES community.guilds(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users.profiles(id) ON DELETE CASCADE,
    role        VARCHAR(20) DEFAULT 'member',   -- 'leader', 'officer', 'member'
    
    joined_at   TIMESTAMPTZ DEFAULT now(),
    
    -- Contribution stats
    weekly_xp_contribution BIGINT DEFAULT 0,
    total_xp_contribution  BIGINT DEFAULT 0,
    
    is_active   BOOLEAN DEFAULT TRUE,
    left_at     TIMESTAMPTZ,
    
    UNIQUE(guild_id, user_id)
);
```

---

### SCHEMA: payments

```sql
-- Subscription plans
CREATE TABLE payments.plans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50) UNIQUE NOT NULL,  -- 'free', 'premium_monthly', 'premium_annual', 'family'
    
    price_usd       DECIMAL(10,2) NOT NULL,
    billing_period  VARCHAR(20) NOT NULL,         -- 'monthly', 'annual', 'lifetime'
    seats           SMALLINT DEFAULT 1,           -- For family/enterprise plans
    
    -- Features (JSONB for flexibility)
    features        JSONB NOT NULL,
    
    is_active       BOOLEAN DEFAULT TRUE,
    stripe_price_id VARCHAR(100),                 -- Stripe price ID
    app_store_product_id VARCHAR(100),            -- Apple IAP product ID
    play_store_product_id VARCHAR(100),           -- Google IAP product ID
    
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- User subscriptions
CREATE TABLE payments.subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.profiles(id),
    plan_id         UUID NOT NULL REFERENCES payments.plans(id),
    
    -- Status
    status          VARCHAR(30) NOT NULL,         -- 'active', 'cancelled', 'expired', 'trial', 'past_due'
    is_trial        BOOLEAN DEFAULT FALSE,
    trial_ends_at   TIMESTAMPTZ,
    
    -- Billing
    current_period_start TIMESTAMPTZ NOT NULL,
    current_period_end   TIMESTAMPTZ NOT NULL,
    cancelled_at    TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    
    -- Provider info
    payment_provider VARCHAR(20) NOT NULL,        -- 'stripe', 'app_store', 'play_store'
    provider_subscription_id VARCHAR(255),        -- External subscription ID
    provider_customer_id VARCHAR(255),
    
    -- Family plan
    family_owner_id UUID REFERENCES users.profiles(id),
    
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_subscriptions_user ON payments.subscriptions(user_id, status);
CREATE INDEX idx_subscriptions_expiring ON payments.subscriptions(current_period_end)
    WHERE status = 'active';
```

---

### SCHEMA: audit

```sql
-- Immutable audit trail for all sensitive operations
CREATE TABLE audit.event_log (
    id              BIGSERIAL PRIMARY KEY,
    
    -- Who
    user_id         UUID,                    -- NULL for system events
    actor_type      VARCHAR(20) NOT NULL,    -- 'user', 'admin', 'system', 'api'
    
    -- What
    event_type      VARCHAR(100) NOT NULL,   -- 'user.login', 'payment.created', etc.
    entity_type     VARCHAR(50),             -- 'user', 'subscription', 'lesson', etc.
    entity_id       UUID,
    
    -- Context
    ip_address      INET,
    user_agent      TEXT,
    request_id      UUID,
    
    -- Change data
    old_values      JSONB,
    new_values      JSONB,
    metadata        JSONB,
    
    -- Result
    result          VARCHAR(20) DEFAULT 'success',  -- 'success', 'failure', 'error'
    error_message   TEXT,
    
    created_at      TIMESTAMPTZ DEFAULT now()
)
PARTITION BY RANGE (created_at);
-- Partitioned by month, retained for 7 years for compliance

CREATE INDEX idx_audit_user ON audit.event_log(user_id, created_at DESC);
CREATE INDEX idx_audit_event_type ON audit.event_log(event_type, created_at DESC);
```

---

## 9.3 COMPLETE INDEX STRATEGY

```sql
-- Performance-critical indexes beyond those defined above

-- Fast leaderboard queries
CREATE INDEX idx_xp_summary_weekly ON gamification.user_xp_summary(weekly_xp DESC)
    WHERE weekly_xp > 0;

-- Fast SRS due items retrieval  
CREATE INDEX idx_memory_due ON progress.memory_states(user_id, next_review_at ASC)
    WHERE mastery_level NOT IN ('not_started', 'long_term')
    AND next_review_at IS NOT NULL;

-- Fast active session lookup
CREATE INDEX idx_sessions_valid ON auth.sessions(refresh_token)
    WHERE is_valid = TRUE AND expires_at > now();

-- Text search on user display names
CREATE INDEX idx_profiles_search ON users.profiles 
    USING GIN(to_tsvector('english', display_name || ' ' || username));

-- Partial index for active premium users
CREATE INDEX idx_profiles_premium ON users.profiles(id, premium_expires_at)
    WHERE is_premium = TRUE;
```

---

## 9.4 DATABASE PARTITIONING STRATEGY

```sql
-- High-volume tables use time-based partitioning
-- Managed by pg_partman extension

-- answer_records: Partitioned by month
-- xp_transactions: Partitioned by month
-- audit.event_log: Partitioned by month

-- Partition maintenance schedule:
-- Pre-create next 3 months of partitions
-- Auto-detach partitions older than 12 months (moved to archive)
-- Archive partitions to Cloud Storage as compressed Parquet files

-- Retention policy:
-- answer_records: 2 years hot, 5 years archive
-- xp_transactions: 3 years hot, 7 years archive  
-- audit.event_log: 7 years (compliance requirement)
-- progress.lesson_completions: 5 years hot
```

---

## 9.5 CACHING LAYER DESIGN

```
PostgreSQL → Application Cache (Redis) → Client Cache (Flutter/Web)

Data categorized by:

HOT (Redis, short TTL):
  - Active session state
  - Leaderboard data (Redis Sorted Sets)
  - Rate limit counters
  - Real-time XP calculations

WARM (Redis, medium TTL):
  - User profiles (15 min)
  - Learning DNA (1 hour)
  - Course catalog (24 hours)
  - Achievement list (15 min)

COLD (No cache, DB direct):
  - Audit logs (immutable, no cache needed)
  - Billing records (must be fresh)
  - Security-critical decisions

WRITE-THROUGH STRATEGY:
  All writes: Update DB first, then update/invalidate Redis
  
INVALIDATION:
  Event-driven: Kafka consumer listens for entity change events
  → Purges relevant cache keys automatically
```
