-- ============================================================
-- V1_0_9 — Rename plan "Professional" → "Business"
--
-- Updates:
--   1. subscription_plans table: planKey + name
--   2. tenant_subscriptions: any references via plan_id (no change needed
--      as it's a FK — plan record itself is updated)
--   3. tenants.plan column: PROFESSIONAL → BUSINESS
--   4. schema default value update for tenants.plan column
-- ============================================================

-- 1. Update subscription_plans record
UPDATE subscription_plans
SET name     = 'Business',
    plan_key = 'business'
WHERE plan_key = 'professional';

-- 2. Update tenants.plan column: rename PROFESSIONAL → BUSINESS
UPDATE tenants
SET plan = 'BUSINESS'
WHERE plan = 'PROFESSIONAL';

-- 3. Ensure the column definition default is consistent
ALTER TABLE tenants
    MODIFY COLUMN `plan` VARCHAR(50) NOT NULL DEFAULT 'STARTER';

