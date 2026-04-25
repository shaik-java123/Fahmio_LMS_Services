-- =====================================================
-- FAHMIO LMS - SUPPORT FEATURE DATABASE MIGRATION
-- =====================================================

-- Main Support Requests Table
CREATE TABLE IF NOT EXISTS support_requests (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255)    NOT NULL COMMENT 'Customer name',
    email               VARCHAR(191)    NOT NULL COMMENT 'Customer email',
    phone               VARCHAR(20)     COMMENT 'Customer phone (optional)',
    category            VARCHAR(50)     NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL, TECHNICAL, BILLING, COURSE, ACCOUNT, FEATURE',
    subject              VARCHAR(255)    NOT NULL COMMENT 'Request subject',
    message             VARCHAR(2000)   NOT NULL COMMENT 'Detailed message',
    support_ticket_id   VARCHAR(50)     UNIQUE COMMENT 'Auto-generated ticket ID',
    status              VARCHAR(50)     NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, IN_PROGRESS, WAITING_CUSTOMER, RESOLVED, CLOSED, REOPENED',
    priority            VARCHAR(50)     NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW, MEDIUM, HIGH, URGENT',
    assigned_to         VARCHAR(255)    COMMENT 'Assigned staff member ID',
    admin_response      VARCHAR(2000)   COMMENT 'Admin response text',
    responded_at        TIMESTAMP       NULL COMMENT 'Response timestamp',
    email_sent          BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'Confirmation email sent flag',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_support_ticket_id (support_ticket_id),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_priority (priority),
    INDEX idx_assigned_to (assigned_to),
    INDEX idx_created_at (created_at),
    INDEX idx_status_created (status, created_at),
    INDEX idx_category_created (category, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Support request tracking table';

