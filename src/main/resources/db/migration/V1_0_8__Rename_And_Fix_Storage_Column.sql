-- ============================================================
-- V1_0_8 — Rename current_storage_usedgb → current_storage_used_gb
--          and ensure all critical column defaults are set.
--
-- This is the definitive fix for:
--   "Field 'current_storage_usedgb' doesn't have a default value"
--
-- Uses IF EXISTS / information_schema guards — safe on any DB state.
-- No DELIMITER — Flyway plain JDBC compatible.
-- ============================================================

-- ── Step 1: Rename the wrongly-named column if it exists ─────
-- Hibernate generated "current_storage_usedgb" (no underscore before gb)
-- before the @Column(name="current_storage_used_gb") annotation was added.
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'tenant_subscriptions'
      AND COLUMN_NAME  = 'current_storage_usedgb'
);

-- Only rename if the old column exists AND the new column does NOT exist
SET @rename_sql = IF(
    @col_exists > 0,
    'ALTER TABLE tenant_subscriptions CHANGE COLUMN `current_storage_usedgb` `current_storage_used_gb` BIGINT NOT NULL DEFAULT 0',
    'SELECT 1 -- column already correctly named, skipping rename'
);

PREPARE rename_stmt FROM @rename_sql;
EXECUTE rename_stmt;
DEALLOCATE PREPARE rename_stmt;

-- ── Step 2: Ensure current_storage_used_gb exists with default ─
-- Add the column if it doesn't exist at all yet
SET @add_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'tenant_subscriptions'
      AND COLUMN_NAME  = 'current_storage_used_gb'
);

SET @add_sql = IF(
    @add_col = 0,
    'ALTER TABLE tenant_subscriptions ADD COLUMN `current_storage_used_gb` BIGINT NOT NULL DEFAULT 0',
    'SELECT 1 -- column exists, skipping add'
);

PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

-- ── Step 3: Fix all NOT NULL columns that are missing DEFAULT ─
ALTER TABLE tenant_subscriptions
    MODIFY COLUMN `billing_cycle`           VARCHAR(50)    NOT NULL DEFAULT 'MONTHLY',
    MODIFY COLUMN `current_price`           DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    MODIFY COLUMN `currency`                CHAR(3)        NOT NULL DEFAULT 'USD',
    MODIFY COLUMN `status`                  VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    MODIFY COLUMN `is_trial_period`         TINYINT(1)     NOT NULL DEFAULT 0,
    MODIFY COLUMN `trial_days_remaining`    INT            NOT NULL DEFAULT 0,
    MODIFY COLUMN `auto_renew`              TINYINT(1)     NOT NULL DEFAULT 1,
    MODIFY COLUMN `current_students`        INT            NOT NULL DEFAULT 0,
    MODIFY COLUMN `current_courses`         INT            NOT NULL DEFAULT 0,
    MODIFY COLUMN `current_instructors`     INT            NOT NULL DEFAULT 0,
    MODIFY COLUMN `current_storage_used_gb` BIGINT         NOT NULL DEFAULT 0;

-- ── Step 4: Fix subscription_plans column defaults ───────────
ALTER TABLE subscription_plans
    MODIFY COLUMN `monthly_price`       DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    MODIFY COLUMN `annual_price`        DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    MODIFY COLUMN `currency`            CHAR(3)        NOT NULL DEFAULT 'USD',
    MODIFY COLUMN `max_students`        INT            NOT NULL DEFAULT 0,
    MODIFY COLUMN `max_courses`         INT            NOT NULL DEFAULT 0,
    MODIFY COLUMN `max_instructors`     INT            NOT NULL DEFAULT 0,
    MODIFY COLUMN `max_storage_gb`      BIGINT         NOT NULL DEFAULT 0,
    MODIFY COLUMN `custom_domain`       TINYINT(1)     NOT NULL DEFAULT 0,
    MODIFY COLUMN `custom_branding`     TINYINT(1)     NOT NULL DEFAULT 0,
    MODIFY COLUMN `advanced_analytics`  TINYINT(1)     NOT NULL DEFAULT 0,
    MODIFY COLUMN `api_access`          TINYINT(1)     NOT NULL DEFAULT 0,
    MODIFY COLUMN `sso`                 TINYINT(1)     NOT NULL DEFAULT 0,
    MODIFY COLUMN `support_24x7`        TINYINT(1)     NOT NULL DEFAULT 0,
    MODIFY COLUMN `dedicated_account`   TINYINT(1)     NOT NULL DEFAULT 0,
    MODIFY COLUMN `status`              VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    MODIFY COLUMN `display_order`       INT            NOT NULL DEFAULT 0,
    MODIFY COLUMN `is_recommended`      TINYINT(1)     NOT NULL DEFAULT 0;

