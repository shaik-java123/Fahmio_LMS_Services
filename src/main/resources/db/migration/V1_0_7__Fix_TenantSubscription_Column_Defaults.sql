-- ============================================================
-- V1_0_7 — Fix tenant_subscriptions & subscription_plans column defaults
--
-- WHY: Hibernate ddl-auto=update created NOT NULL columns without
-- DEFAULT values. INSERTs fail with "Field '...' doesn't have a
-- default value". Also renames the mis-named column
-- current_storage_usedgb → current_storage_used_gb if it exists.
--
-- NOTE: No DELIMITER statements — Flyway uses plain JDBC which
-- does not support DELIMITER. Each statement is standalone.
-- ============================================================

-- ── 1. Rename mis-named column (created by Hibernate before @Column(name=) was added) ──
-- Rename current_storage_usedgb → current_storage_used_gb (if old name exists)
ALTER TABLE tenant_subscriptions
    CHANGE COLUMN IF EXISTS `current_storage_usedgb`
                            `current_storage_used_gb` BIGINT NOT NULL DEFAULT 0;

-- ── 2. Fix tenant_subscriptions column defaults ──────────────
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

-- ── 3. Fix subscription_plans column defaults ────────────────
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
