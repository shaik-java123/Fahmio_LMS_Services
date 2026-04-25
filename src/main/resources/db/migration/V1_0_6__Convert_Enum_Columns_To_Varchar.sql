-- ============================================================
-- V1_0_6 — Convert MySQL native ENUM columns → VARCHAR(50)
--
-- WHY: Hibernate ddl-auto=update created native MySQL ENUM columns.
-- With ddl-auto=validate + columnDefinition=VARCHAR(50) on entities,
-- Hibernate validation fails: "found [enum], but expecting [varchar(50)]"
--
-- FIX: ALTER all affected ENUM columns to VARCHAR(50).
-- Uses CHANGE COLUMN IF EXISTS — safe on fresh DBs.
-- NOTE: No DELIMITER — Flyway uses plain JDBC.
-- ============================================================

-- ── lessons ──────────────────────────────────────────────────
ALTER TABLE lessons
    MODIFY COLUMN `content_type` VARCHAR(50) NOT NULL DEFAULT 'TEXT';

-- ── mock_interviews ───────────────────────────────────────────
ALTER TABLE mock_interviews
    MODIFY COLUMN `difficulty`      VARCHAR(50) NOT NULL DEFAULT 'INTERMEDIATE',
    MODIFY COLUMN `interview_type`  VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    MODIFY COLUMN `status`          VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS';

-- ── interview_questions ───────────────────────────────────────
ALTER TABLE interview_questions
    MODIFY COLUMN `type`   VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'UNANSWERED';

-- ── orders ───────────────────────────────────────────────────
ALTER TABLE orders
    MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING';

-- ── submissions ───────────────────────────────────────────────
ALTER TABLE submissions
    MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED';

-- ── quiz_attempts ─────────────────────────────────────────────
ALTER TABLE quiz_attempts
    MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS';

-- ── live_classes ──────────────────────────────────────────────
ALTER TABLE live_classes
    MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'UPCOMING';

-- ── support_requests ─────────────────────────────────────────
ALTER TABLE support_requests
    MODIFY COLUMN `category` VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    MODIFY COLUMN `status`   VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    MODIFY COLUMN `priority` VARCHAR(50) NOT NULL DEFAULT 'MEDIUM';

-- ── subscription_plans ────────────────────────────────────────
ALTER TABLE subscription_plans
    MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- ── tenant_subscriptions ─────────────────────────────────────
ALTER TABLE tenant_subscriptions
    MODIFY COLUMN `billing_cycle` VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    MODIFY COLUMN `status`        VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- ── users ─────────────────────────────────────────────────────
ALTER TABLE users
    MODIFY COLUMN `role`   VARCHAR(50) NOT NULL DEFAULT 'STUDENT',
    MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- ── tenants ───────────────────────────────────────────────────
ALTER TABLE tenants
    MODIFY COLUMN `plan`   VARCHAR(50) NOT NULL DEFAULT 'STARTER',
    MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- ── questions (quiz) ─────────────────────────────────────────
ALTER TABLE questions
    MODIFY COLUMN `type` VARCHAR(50) NOT NULL;

-- ── notifications ────────────────────────────────────────────
ALTER TABLE notifications
    MODIFY COLUMN `type` VARCHAR(50) NOT NULL;

-- ── enrollments ──────────────────────────────────────────────
ALTER TABLE enrollments
    MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- ── assignments ──────────────────────────────────────────────
ALTER TABLE assignments
    MODIFY COLUMN `type` VARCHAR(50) NOT NULL DEFAULT 'TEXT';

-- ── course_prices ─────────────────────────────────────────────
ALTER TABLE course_prices
    MODIFY COLUMN `type` VARCHAR(50) NOT NULL DEFAULT 'FREE';
