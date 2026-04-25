-- =====================================================
-- FAHMIO LMS - SUPPORT REQUEST SYSTEM
-- Database Schema and Setup Script
-- =====================================================
-- This script creates all tables and structures needed
-- for the contact support feature in Fahmio LMS
-- =====================================================

-- Drop existing table if exists (optional, uncomment to use)
-- DROP TABLE IF EXISTS support_requests;

-- =====================================================
-- 1. MAIN SUPPORT REQUESTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS support_requests (
    id                  BIGINT              NOT NULL AUTO_INCREMENT PRIMARY KEY,

    -- Customer Information
    name                VARCHAR(255)        NOT NULL COMMENT 'Customer full name',
    email               VARCHAR(191)        NOT NULL COMMENT 'Customer email address',
    phone               VARCHAR(20)         COMMENT 'Customer phone number (optional)',

    -- Request Details
    category            VARCHAR(50)         NOT NULL DEFAULT 'GENERAL'
                        COMMENT 'Support category: GENERAL, TECHNICAL, BILLING, COURSE, ACCOUNT, FEATURE',
    subject             VARCHAR(255)        NOT NULL COMMENT 'Request subject/title',
    message             VARCHAR(2000)       NOT NULL COMMENT 'Detailed message from customer',

    -- Ticket Information
    support_ticket_id   VARCHAR(50)         UNIQUE COMMENT 'Auto-generated unique ticket ID (e.g., TKT-1234567890)',

    -- Status & Priority
    status              VARCHAR(50)         NOT NULL DEFAULT 'OPEN'
                        COMMENT 'Status: OPEN, IN_PROGRESS, WAITING_CUSTOMER, RESOLVED, CLOSED, REOPENED',
    priority            VARCHAR(50)         NOT NULL DEFAULT 'MEDIUM'
                        COMMENT 'Priority: LOW, MEDIUM, HIGH, URGENT',

    -- Admin Management
    assigned_to         VARCHAR(255)        COMMENT 'ID or name of assigned staff member',
    admin_response      VARCHAR(2000)       COMMENT 'Response from support team',
    responded_at        TIMESTAMP           NULL COMMENT 'When response was sent',

    -- Metadata
    email_sent          BOOLEAN             NOT NULL DEFAULT FALSE COMMENT 'Whether confirmation email was sent',
    created_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Request creation time',
    updated_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        COMMENT 'Last update time',

    -- Indexes for Performance
    INDEX idx_email (email) COMMENT 'Find requests by customer email',
    INDEX idx_status (status) COMMENT 'Filter by status',
    INDEX idx_category (category) COMMENT 'Filter by category',
    INDEX idx_priority (priority) COMMENT 'Filter by priority',
    INDEX idx_assigned_to (assigned_to) COMMENT 'Find requests assigned to staff',
    INDEX idx_created_at (created_at) COMMENT 'Sort by creation date',
    INDEX idx_status_created (status, created_at) COMMENT 'Get open requests by date',
    INDEX idx_category_created (category, created_at) COMMENT 'Get category requests by date',

    -- Unique Constraints
    UNIQUE KEY uq_support_ticket_id (support_ticket_id) COMMENT 'Ticket ID must be unique'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Customer support requests and tracking';

-- =====================================================
-- 2. SUPPORT AUDIT LOG TABLE (Optional but Recommended)
-- =====================================================
CREATE TABLE IF NOT EXISTS support_audit_log (
    id                  BIGINT              NOT NULL AUTO_INCREMENT PRIMARY KEY,
    support_request_id  BIGINT              NOT NULL,
    action              VARCHAR(100)        NOT NULL COMMENT 'ACTION: CREATED, STATUS_CHANGED, RESPONSE_ADDED, ASSIGNED, CLOSED',
    old_value           VARCHAR(500)        COMMENT 'Previous value',
    new_value           VARCHAR(500)        COMMENT 'New value',
    changed_by          VARCHAR(255)        COMMENT 'Who made the change',
    changed_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_request_id (support_request_id),
    INDEX idx_action (action),
    INDEX idx_changed_at (changed_at),

    CONSTRAINT fk_audit_request
        FOREIGN KEY (support_request_id)
        REFERENCES support_requests(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail for support request changes';

-- =====================================================
-- 3. SUPPORT STATISTICS TABLE (For Dashboard)
-- =====================================================
CREATE TABLE IF NOT EXISTS support_statistics (
    id                  BIGINT              NOT NULL AUTO_INCREMENT PRIMARY KEY,
    stat_date           DATE                NOT NULL UNIQUE COMMENT 'Date of statistics',

    total_requests      INT                 DEFAULT 0,
    open_requests       INT                 DEFAULT 0,
    in_progress_requests INT                DEFAULT 0,
    resolved_requests   INT                 DEFAULT 0,
    closed_requests     INT                 DEFAULT 0,
    urgent_requests     INT                 DEFAULT 0,
    high_priority_requests INT              DEFAULT 0,

    avg_response_time_hours DECIMAL(5,2)    COMMENT 'Average response time in hours',
    resolution_rate     DECIMAL(5,2)        COMMENT 'Resolution rate as percentage',

    updated_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Daily support statistics for analytics';

-- =====================================================
-- 4. VIEWS FOR COMMON QUERIES
-- =====================================================

-- Open Requests View
CREATE OR REPLACE VIEW vw_open_requests AS
SELECT
    id,
    support_ticket_id as ticket_id,
    name as customer_name,
    email as customer_email,
    category,
    subject,
    priority,
    status,
    created_at,
    DATEDIFF(NOW(), created_at) as days_open
FROM support_requests
WHERE status IN ('OPEN', 'IN_PROGRESS', 'WAITING_CUSTOMER')
ORDER BY priority DESC, created_at ASC;

-- Requests By Category View
CREATE OR REPLACE VIEW vw_requests_by_category AS
SELECT
    category,
    COUNT(*) as total_requests,
    SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) as open_count,
    SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) as resolved_count,
    SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) as closed_count
FROM support_requests
GROUP BY category
ORDER BY total_requests DESC;

-- Requests By Priority View
CREATE OR REPLACE VIEW vw_requests_by_priority AS
SELECT
    priority,
    COUNT(*) as total_requests,
    SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) as open_count,
    SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) as resolved_count
FROM support_requests
GROUP BY priority
ORDER BY FIELD(priority, 'URGENT', 'HIGH', 'MEDIUM', 'LOW');

-- Average Response Time View
CREATE OR REPLACE VIEW vw_response_metrics AS
SELECT
    AVG(TIMESTAMPDIFF(HOUR, created_at, responded_at)) as avg_response_hours,
    MIN(TIMESTAMPDIFF(HOUR, created_at, responded_at)) as min_response_hours,
    MAX(TIMESTAMPDIFF(HOUR, created_at, responded_at)) as max_response_hours,
    COUNT(CASE WHEN responded_at IS NOT NULL THEN 1 END) as responded_count,
    COUNT(*) as total_count
FROM support_requests
WHERE responded_at IS NOT NULL;

-- =====================================================
-- 5. STORED PROCEDURES FOR COMMON OPERATIONS
-- =====================================================

-- Procedure to update request status
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS sp_update_request_status(
    IN p_request_id BIGINT,
    IN p_new_status VARCHAR(50),
    IN p_changed_by VARCHAR(255)
)
BEGIN
    DECLARE v_old_status VARCHAR(50);

    -- Get old status
    SELECT status INTO v_old_status
    FROM support_requests
    WHERE id = p_request_id;

    -- Update request
    UPDATE support_requests
    SET status = p_new_status,
        updated_at = NOW()
    WHERE id = p_request_id;

    -- Log the change
    INSERT INTO support_audit_log
    (support_request_id, action, old_value, new_value, changed_by)
    VALUES (p_request_id, 'STATUS_CHANGED', v_old_status, p_new_status, p_changed_by);

END //
DELIMITER ;

-- Procedure to close request
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS sp_close_request(
    IN p_request_id BIGINT,
    IN p_response TEXT,
    IN p_closed_by VARCHAR(255)
)
BEGIN
    UPDATE support_requests
    SET status = 'CLOSED',
        admin_response = p_response,
        responded_at = NOW(),
        updated_at = NOW()
    WHERE id = p_request_id;

    INSERT INTO support_audit_log
    (support_request_id, action, new_value, changed_by)
    VALUES (p_request_id, 'CLOSED', 'Request Closed', p_closed_by);

END //
DELIMITER ;

-- Procedure to get support statistics
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS sp_get_support_stats()
BEGIN
    SELECT
        COUNT(*) as total_requests,
        SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) as open_requests,
        SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress_requests,
        SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) as resolved_requests,
        SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) as closed_requests,
        SUM(CASE WHEN priority = 'URGENT' THEN 1 ELSE 0 END) as urgent_requests,
        SUM(CASE WHEN priority = 'HIGH' THEN 1 ELSE 0 END) as high_priority_requests
    FROM support_requests;
END //
DELIMITER ;

-- =====================================================
-- 6. SAMPLE DATA (Optional - for testing)
-- =====================================================
-- Uncomment to insert sample data

-- INSERT INTO support_requests
-- (name, email, phone, category, subject, message, support_ticket_id, status, priority)
-- VALUES
-- ('John Doe', 'john@example.com', '+1234567890', 'TECHNICAL', 'API not working', 'The API endpoint returns 500 error', 'TKT-001', 'OPEN', 'HIGH'),
-- ('Jane Smith', 'jane@example.com', NULL, 'BILLING', 'Payment issue', 'Charge appeared twice on my account', 'TKT-002', 'IN_PROGRESS', 'URGENT'),
-- ('Bob Johnson', 'bob@example.com', '+9876543210', 'GENERAL', 'Account question', 'How do I reset my password?', 'TKT-003', 'RESOLVED', 'LOW');

-- =====================================================
-- 7. INDEXES FOR BETTER PERFORMANCE
-- =====================================================
-- These are already included in the table definition above
-- but documented here for reference:
-- - idx_email: Fast lookup by customer email
-- - idx_status: Fast filtering by status
-- - idx_category: Fast filtering by category
-- - idx_priority: Fast filtering by priority
-- - idx_assigned_to: Find requests assigned to specific staff
-- - idx_created_at: Sort by date
-- - idx_status_created: Combined index for status and date
-- - idx_category_created: Combined index for category and date
-- - uq_support_ticket_id: Ensure ticket IDs are unique

-- =====================================================
-- 8. VERIFICATION QUERIES
-- =====================================================
-- Run these queries to verify installation

-- Check if support_requests table exists
-- SELECT COUNT(*) FROM information_schema.tables
-- WHERE table_schema = DATABASE() AND table_name = 'support_requests';

-- Check table structure
-- DESCRIBE support_requests;

-- Check table row count
-- SELECT COUNT(*) as total_requests FROM support_requests;

-- =====================================================
-- END OF SCRIPT
-- =====================================================
-- Created: March 31, 2026
-- Database: MySQL 5.7+
-- Charset: utf8mb4
-- Collation: utf8mb4_unicode_ci
-- =====================================================

