-- Database Migration for Multi-Tenant Subscription System
-- Created: April 8, 2026

-- ===================================================================
-- 1. SUBSCRIPTION PLANS TABLE
-- ===================================================================
CREATE TABLE IF NOT EXISTS subscription_plans (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    plan_key VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(1000),
    monthly_price DECIMAL(10, 2) NOT NULL,
    annual_price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    stripe_product_id VARCHAR(255),
    stripe_price_id_monthly VARCHAR(255),
    stripe_price_id_annual VARCHAR(255),

    -- Feature limits
    max_students INT NOT NULL DEFAULT 0,
    max_courses INT NOT NULL DEFAULT 0,
    max_instructors INT NOT NULL DEFAULT 0,
    max_storage_gb BIGINT NOT NULL DEFAULT 0,

    -- Feature flags
    custom_domain BOOLEAN NOT NULL DEFAULT false,
    custom_branding BOOLEAN NOT NULL DEFAULT false,
    advanced_analytics BOOLEAN NOT NULL DEFAULT false,
    api_access BOOLEAN NOT NULL DEFAULT false,
    sso BOOLEAN NOT NULL DEFAULT false,
    support_24x7 BOOLEAN NOT NULL DEFAULT false,
    dedicated_account BOOLEAN NOT NULL DEFAULT false,

    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    display_order INT NOT NULL DEFAULT 0,
    is_recommended BOOLEAN NOT NULL DEFAULT false,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_plan_key (plan_key),
    INDEX idx_status (status),
    INDEX idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- 2. PLAN FEATURES TABLE (for additional features)
-- ===================================================================
CREATE TABLE IF NOT EXISTS plan_features (
    plan_id BIGINT NOT NULL,
    feature VARCHAR(255) NOT NULL,

    PRIMARY KEY (plan_id, feature),
    FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE CASCADE,

    INDEX idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- 3. TENANT SUBSCRIPTIONS TABLE
-- ===================================================================
CREATE TABLE IF NOT EXISTS tenant_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE,
    plan_id BIGINT NOT NULL,

    billing_cycle VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    current_price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    -- Stripe subscription tracking
    stripe_subscription_id VARCHAR(255),
    stripe_customer_id VARCHAR(255),

    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    -- Trial period
    is_trial_period BOOLEAN NOT NULL DEFAULT false,
    trial_days_remaining INT DEFAULT 0,

    -- Dates
    subscribed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),

    -- Auto-renewal
    auto_renew BOOLEAN NOT NULL DEFAULT true,

    -- Usage tracking
    current_students INT NOT NULL DEFAULT 0,
    current_courses INT NOT NULL DEFAULT 0,
    current_instructors INT NOT NULL DEFAULT 0,
    current_storage_used_gb BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE RESTRICT,

    UNIQUE KEY uk_tenant_subscription (tenant_id),
    INDEX idx_status (status),
    INDEX idx_current_period_end (current_period_end),
    INDEX idx_stripe_subscription_id (stripe_subscription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- 4. INSERT DEFAULT SUBSCRIPTION PLANS
-- ===================================================================
INSERT INTO subscription_plans (
    name, plan_key, description, monthly_price, annual_price, currency,
    max_students, max_courses, max_instructors, max_storage_gb,
    custom_domain, custom_branding, advanced_analytics, api_access, sso, support_24x7, dedicated_account,
    display_order, is_recommended, status
) VALUES
    (
        'Starter',
        'starter',
        'Perfect for getting started with online learning',
        0.00,
        0.00,
        'USD',
        50,
        5,
        2,
        5,
        false, false, false, false, false, false, false,
        0,
        false,
        'ACTIVE'
    ),
    (
        'Growth',
        'growth',
        'Great for small teams and growing organizations',
        29.99,
        299.90,
        'USD',
        500,
        25,
        10,
        50,
        true, true, false, false, false, false, false,
        1,
        false,
        'ACTIVE'
    ),
    (
        'Professional',
        'professional',
        'For established organizations with advanced needs',
        99.99,
        999.90,
        'USD',
        5000,
        100,
        50,
        500,
        true, true, true, true, false, false, false,
        2,
        true,
        'ACTIVE'
    ),
    (
        'Enterprise',
        'enterprise',
        'Unlimited scale with premium support and dedicated management',
        299.99,
        2999.90,
        'USD',
        0,
        0,
        0,
        5000,
        true, true, true, true, true, true, true,
        3,
        false,
        'ACTIVE'
    );

-- ===================================================================
-- 5. INSERT ADDITIONAL FEATURES FOR PLANS
-- ===================================================================
INSERT INTO plan_features (plan_id, feature) VALUES
    -- Professional plan features (id=3)
    (3, 'White-label solution'),
    (3, 'Bulk user import'),
    (3, 'Custom certificate templates'),
    (3, 'Email notifications'),
    -- Enterprise plan features (id=4)
    (4, 'White-label solution'),
    (4, 'Bulk user import'),
    (4, 'Custom certificate templates'),
    (4, 'Email notifications'),
    (4, 'Webhook integration'),
    (4, 'Custom branding kit'),
    (4, 'Advanced reporting');

-- ===================================================================
-- 6. SEED GLOBAL TENANT SUBSCRIPTION (links global tenant to Professional plan)
-- ===================================================================
INSERT IGNORE INTO tenant_subscriptions (
    tenant_id,
    plan_id,
    billing_cycle,
    current_price,
    currency,
    status,
    is_trial_period,
    trial_days_remaining,
    subscribed_at,
    current_period_start,
    current_period_end,
    auto_renew,
    current_students,
    current_courses,
    current_instructors,
    current_storage_used_gb
) VALUES (
    1,                          -- global tenant (id=1)
    3,                          -- Professional plan (id=3)
    'ANNUAL',
    999.90,
    'USD',
    'ACTIVE',
    false,
    0,
    NOW(),
    NOW(),
    DATE_ADD(NOW(), INTERVAL 1 YEAR),
    true,
    0,
    0,
    0,
    0
);
