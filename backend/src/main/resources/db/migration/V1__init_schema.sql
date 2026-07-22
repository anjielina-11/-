-- ============================================
-- 云南农业智能诊断平台 — 数据库初始化
-- PostgreSQL 16 + PostGIS 3 + pgvector
-- ============================================

-- 启用扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 1. 用户表
CREATE TABLE users
(
    id            UUID         NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'ROLE_FARMER',
    real_name     VARCHAR(50),
    phone         VARCHAR(20),
    email         VARCHAR(100),
    avatar_url    VARCHAR(500),
    farm_id       UUID,
    status        SMALLINT              DEFAULT 1,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted       SMALLINT              DEFAULT 0
);
CREATE UNIQUE INDEX uk_users_username ON users (username) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_users_phone ON users (phone) WHERE deleted = 0 AND phone IS NOT NULL;

-- 2. 农场表 (含地理坐标)
CREATE TABLE farms
(
    id         UUID         NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    owner_id   UUID         NOT NULL REFERENCES users (id),
    name       VARCHAR(100) NOT NULL,
    address    VARCHAR(300),
    area_mu    NUMERIC(10, 2),
    location   GEOMETRY(Point, 4326),
    contact    VARCHAR(50),
    remark     TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted    SMALLINT              DEFAULT 0
);
CREATE INDEX idx_farms_owner ON farms (owner_id);
CREATE INDEX idx_farms_location ON farms USING GIST (location);

-- 3. 地块表
CREATE TABLE fields
(
    id         UUID         NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    farm_id    UUID         NOT NULL REFERENCES farms (id),
    name       VARCHAR(100) NOT NULL,
    area_mu    NUMERIC(10, 2),
    soil_type  VARCHAR(50),
    location   GEOMETRY(Point, 4326),
    boundary   GEOMETRY(Polygon, 4326),
    remark     TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted    SMALLINT              DEFAULT 0
);
CREATE INDEX idx_fields_farm ON fields (farm_id);
CREATE INDEX idx_fields_location ON fields USING GIST (location);

-- 4. 作物品种表
CREATE TABLE crops
(
    id               UUID         NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    category         VARCHAR(50),
    variety          VARCHAR(100),
    growth_days      INT,
    optimal_temp_min NUMERIC(4, 1),
    optimal_temp_max NUMERIC(4, 1),
    description      TEXT,
    image_url        VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted          SMALLINT              DEFAULT 0
);

-- 5. 种植周期表
CREATE TABLE planting_cycles
(
    id                   UUID        NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    field_id             UUID        NOT NULL REFERENCES fields (id),
    crop_id              UUID        NOT NULL REFERENCES crops (id),
    planting_date        DATE,
    expected_harvest_date DATE,
    actual_harvest_date  DATE,
    growth_stage         VARCHAR(30)          DEFAULT 'sowing',
    status               VARCHAR(20) NOT NULL DEFAULT 'active',
    area_mu              NUMERIC(10, 2),
    remark               TEXT,
    created_by           UUID        NOT NULL REFERENCES users (id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted              SMALLINT             DEFAULT 0
);
CREATE INDEX idx_pc_field ON planting_cycles (field_id);
CREATE INDEX idx_pc_crop ON planting_cycles (crop_id);
CREATE INDEX idx_pc_status ON planting_cycles (status);

-- 6. 田间观察记录表
CREATE TABLE observations
(
    id               UUID        NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    cycle_id         UUID        NOT NULL REFERENCES planting_cycles (id),
    user_id          UUID        NOT NULL REFERENCES users (id),
    observation_type VARCHAR(30) NOT NULL DEFAULT 'visual',
    description      TEXT,
    images           JSONB                DEFAULT '[]',
    location         GEOMETRY(Point, 4326),
    observed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    weather_info     JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted          SMALLINT             DEFAULT 0
);
CREATE INDEX idx_obs_cycle ON observations (cycle_id);
CREATE INDEX idx_obs_user ON observations (user_id);

-- 7. 病害诊断记录表
CREATE TABLE diagnosis_records
(
    id               UUID         NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    observation_id   UUID         NOT NULL REFERENCES observations (id),
    image_url        VARCHAR(500),
    image_hash       VARCHAR(64),
    thumbnail_url    VARCHAR(500),
    disease_name     VARCHAR(100),
    disease_category VARCHAR(50),
    confidence       NUMERIC(5, 4),
    ai_result        JSONB,
    model_version_id UUID,
    review_status    VARCHAR(20)  NOT NULL DEFAULT 'pending',
    review_comment   TEXT,
    reviewer_id      UUID REFERENCES users (id),
    reviewed_at      TIMESTAMPTZ,
    feedback         TEXT,
    feedback_at      TIMESTAMPTZ,
    severity         VARCHAR(20),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted          SMALLINT              DEFAULT 0
);
CREATE UNIQUE INDEX uk_dr_image_hash ON diagnosis_records (image_hash) WHERE image_hash IS NOT NULL AND deleted = 0;
CREATE INDEX idx_dr_observation ON diagnosis_records (observation_id);
CREATE INDEX idx_dr_review_status ON diagnosis_records (review_status);
CREATE INDEX idx_dr_disease ON diagnosis_records (disease_name);

-- 8. 天气记录表
CREATE TABLE weather_records
(
    id           UUID        NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    farm_id      UUID        NOT NULL REFERENCES farms (id),
    temperature  NUMERIC(5, 2),
    humidity     NUMERIC(5, 2),
    rainfall     NUMERIC(6, 2),
    wind_speed   NUMERIC(5, 2),
    wind_dir     VARCHAR(10),
    pressure     NUMERIC(7, 2),
    weather_desc VARCHAR(50),
    source       VARCHAR(30)          DEFAULT 'api',
    recorded_at  TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_wr_farm_time ON weather_records (farm_id, recorded_at DESC);

-- 9. 市场价格表
CREATE TABLE market_prices
(
    id          UUID         NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    crop_id     UUID         NOT NULL REFERENCES crops (id),
    crop_name   VARCHAR(100),
    price       NUMERIC(10, 2),
    unit        VARCHAR(20)           DEFAULT '元/公斤',
    market_name VARCHAR(100),
    category    VARCHAR(30),
    source      VARCHAR(30)           DEFAULT 'api',
    recorded_at DATE         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mp_crop_date ON market_prices (crop_id, recorded_at DESC);

-- 10. 农事任务表
CREATE TABLE farming_tasks
(
    id           UUID        NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    cycle_id     UUID REFERENCES planting_cycles (id),
    diagnosis_id UUID REFERENCES diagnosis_records (id),
    task_type    VARCHAR(30) NOT NULL,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    priority     SMALLINT             DEFAULT 0,
    status       VARCHAR(20) NOT NULL DEFAULT 'pending',
    assignee_id  UUID        NOT NULL REFERENCES users (id),
    created_by   UUID REFERENCES users (id),
    scheduled_date DATE,
    completed_at TIMESTAMPTZ,
    remark       TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted      SMALLINT             DEFAULT 0
);
CREATE INDEX idx_ft_assignee ON farming_tasks (assignee_id);
CREATE INDEX idx_ft_status ON farming_tasks (status);
CREATE INDEX idx_ft_scheduled ON farming_tasks (scheduled_date);
CREATE INDEX idx_ft_cycle ON farming_tasks (cycle_id);

-- 11. 知识文档表 (含向量)
CREATE TABLE knowledge_documents
(
    id          UUID         NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    category    VARCHAR(50),
    tags        JSONB                 DEFAULT '[]',
    embedding   VECTOR(1536),
    source_url  VARCHAR(500),
    version     INT                   DEFAULT 1,
    status      VARCHAR(20)           DEFAULT 'published',
    author_id   UUID REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted     SMALLINT              DEFAULT 0
);
CREATE INDEX idx_kd_embedding ON knowledge_documents USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_kd_category ON knowledge_documents (category);

-- 12. 模型版本表
CREATE TABLE model_versions
(
    id             UUID         NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    model_name     VARCHAR(100) NOT NULL,
    model_type     VARCHAR(30)  NOT NULL DEFAULT 'classification',
    version        VARCHAR(30)  NOT NULL,
    accuracy       NUMERIC(6, 4),
    precision_val  NUMERIC(6, 4),
    recall_val     NUMERIC(6, 4),
    f1_score       NUMERIC(6, 4),
    model_path     VARCHAR(500),
    config_json    JSONB,
    status         VARCHAR(20)           DEFAULT 'active',
    deployed_at    TIMESTAMPTZ,
    description    TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_mv_name_version ON model_versions (model_name, version);

-- 13. Agent 运行记录表
CREATE TABLE agent_runs
(
    id            UUID         NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    diagnosis_id  UUID REFERENCES diagnosis_records (id),
    agent_type    VARCHAR(50)  NOT NULL,
    agent_name    VARCHAR(100),
    input_json    JSONB,
    output_json   JSONB,
    status        VARCHAR(20)           DEFAULT 'running',
    error_message TEXT,
    tokens_used   INT,
    cost          NUMERIC(10, 6),
    started_at    TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_ar_diagnosis ON agent_runs (diagnosis_id);

-- 14. 审核队列表
CREATE TABLE review_queue
(
    id            UUID        NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    diagnosis_id  UUID        NOT NULL REFERENCES diagnosis_records (id),
    priority      SMALLINT             DEFAULT 0,
    status        VARCHAR(20) NOT NULL DEFAULT 'pending',
    reason        VARCHAR(100),
    assigned_to   UUID REFERENCES users (id),
    assigned_at   TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_rq_diagnosis ON review_queue (diagnosis_id);
CREATE INDEX idx_rq_status ON review_queue (status);
CREATE INDEX idx_rq_assigned ON review_queue (assigned_to);

-- 初始数据：管理员账号 (密码: admin123)
INSERT INTO users (id, username, password_hash, role, real_name, status)
VALUES (uuid_generate_v4(), 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
        'ROLE_ADMIN', '系统管理员', 1)
ON CONFLICT DO NOTHING;
