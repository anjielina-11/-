-- 为农场和作物品种增加生命周期状态
ALTER TABLE farms
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';

ALTER TABLE crops
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';

UPDATE farms SET status = 'active' WHERE status IS NULL;
UPDATE crops SET status = 'active' WHERE status IS NULL;

CREATE INDEX IF NOT EXISTS idx_farms_owner_status ON farms (owner_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_crops_status_category ON crops (status, category, created_at DESC);
