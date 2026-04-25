-- Create assignment_allocations table
CREATE TABLE IF NOT EXISTS assignment_allocations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    allocated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_sent_at TIMESTAMP NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ASSIGNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_assignment_candidate (assignment_id, candidate_id),
    INDEX idx_assignment_id (assignment_id),
    INDEX idx_candidate_id (candidate_id),
    INDEX idx_status (status),
    INDEX idx_notification_sent (notification_sent)
);

-- Create index for finding unnotified allocations quickly
CREATE INDEX idx_unnotified ON assignment_allocations(notification_sent, allocated_at);

-- Alter existing assignments table to add is_selective column if not exists
ALTER TABLE assignments
ADD COLUMN IF NOT EXISTS is_selective BOOLEAN DEFAULT FALSE COMMENT 'Whether this assignment is assigned to specific candidates only';

-- Add audit log table for tracking allocation changes
CREATE TABLE IF NOT EXISTS assignment_allocation_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    allocation_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    changed_by BIGINT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (allocation_id) REFERENCES assignment_allocations(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_allocation_id (allocation_id),
    INDEX idx_changed_at (changed_at)
);

