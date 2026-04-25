-- Fix for "Unknown column" and "Data truncated" errors in LMS
-- This migration applies schema fixes and adds missing columns

-- 1. Correct the column type to allow new enum values (like LIVE_CLASS or MULTIPLE_CHOICE)
-- If these were previously ENUM fields, we convert them to VARCHAR to be more flexible.
ALTER TABLE lessons MODIFY COLUMN content_type VARCHAR(50) NOT NULL DEFAULT 'TEXT';
ALTER TABLE assignments MODIFY COLUMN type VARCHAR(30) NOT NULL DEFAULT 'TEXT';

-- 2. Add missing columns for Lessons table
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS audio_url VARCHAR(500);
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS external_url VARCHAR(500);
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMP NULL;

-- 3. Add missing columns for Assignments table
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS language VARCHAR(100);
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS options TEXT;

-- 4. Add missing columns for Courses table
ALTER TABLE courses ADD COLUMN IF NOT EXISTS price DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

-- 5. Add missing columns for Enrollments table
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS tenant_id BIGINT NULL;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS valid_until DATETIME NULL;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(255) NULL;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;

-- 6. Add Foreign Key for tenant_id if it exists
ALTER TABLE enrollments ADD CONSTRAINT IF NOT EXISTS fk_enrollments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE SET NULL;

