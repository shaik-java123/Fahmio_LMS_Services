-- Add User Status Column for Multi-Tenant Subscription System
-- Created: April 8, 2026
-- NOTE: MySQL 8.0+ supports ADD COLUMN IF NOT EXISTS via a stored procedure guard.
-- This script uses a safe procedure to avoid duplicate column errors on re-run.

DROP PROCEDURE IF EXISTS add_user_status_column;

DELIMITER $$
CREATE PROCEDURE add_user_status_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'users'
          AND COLUMN_NAME  = 'status'
    ) THEN
        ALTER TABLE users
            ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
            COMMENT 'User account status: ACTIVE, INACTIVE, SUSPENDED, DELETED'
            AFTER email_verified;

        ALTER TABLE users
            ADD INDEX idx_users_status (status);
    END IF;
END$$
DELIMITER ;

CALL add_user_status_column();
DROP PROCEDURE IF EXISTS add_user_status_column;
