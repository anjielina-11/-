-- Backend query optimization for list, filter and timeline endpoints.
-- Existing single-column indexes are kept for compatibility; these composite indexes
-- match the most common WHERE + ORDER BY patterns used by MyBatis-Plus services.

CREATE INDEX IF NOT EXISTS idx_farms_owner_created
    ON farms (owner_id, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_fields_farm_created
    ON fields (farm_id, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_crops_category_created
    ON crops (category, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_pc_creator_status_created
    ON planting_cycles (created_by, status, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_pc_field_status_created
    ON planting_cycles (field_id, status, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_obs_user_created
    ON observations (user_id, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_obs_cycle_observed
    ON observations (cycle_id, observed_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_dr_observation_created
    ON diagnosis_records (observation_id, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_dr_review_created
    ON diagnosis_records (review_status, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_dr_disease_created
    ON diagnosis_records (disease_name, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_ft_assignee_status_created
    ON farming_tasks (assignee_id, status, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_rq_status_priority_created
    ON review_queue (status, priority, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_agent_diagnosis_created
    ON agent_runs (diagnosis_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_knowledge_category_created
    ON knowledge_documents (category, created_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_model_type_status_created
    ON model_versions (model_type, status, created_at DESC);