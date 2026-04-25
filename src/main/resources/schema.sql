-- ============================================================
--  LearnHub LMS – Complete Database Schema
--  Database : lms_db
--  Engine   : MySQL 8.x / InnoDB
--  Generated: 2026-03-18
--
--  Tables (20):
--    Core       : tenants, users
--    Auth       : refresh_tokens, email_verifications, password_reset_tokens
--    Courses    : courses, modules, lessons, course_prices
--    Commerce   : orders
--    Learning   : enrollments, assignments, submissions
--    Quiz       : quizzes, questions, question_options,
--                 quiz_attempts, quiz_answers, quiz_answer_options
--    Progress   : lesson_progress
--    Completion : certificates
--    Community  : discussion_topics, discussion_comments
-- ============================================================

CREATE DATABASE IF NOT EXISTS lms_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lms_db;

SET FOREIGN_KEY_CHECKS = 0;

-- ──────────────────────────────────────────────────────────────
-- 1. TENANTS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenants (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255)    NOT NULL,
    subdomain     VARCHAR(100)    NOT NULL,
    plan          VARCHAR(50)     NOT NULL DEFAULT 'FREE',
    -- ENUM: FREE, STARTER, PROFESSIONAL, ENTERPRISE
    status        VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    -- ENUM: ACTIVE, SUSPENDED, CANCELLED
    custom_domain VARCHAR(255),
    logo_url      VARCHAR(500),
    primary_color VARCHAR(20)     NOT NULL DEFAULT '#0284c7',
    accent_color  VARCHAR(20)     NOT NULL DEFAULT '#0ea5e9',
    tagline       VARCHAR(500),
    contact_email VARCHAR(191),
    stripe_account_id VARCHAR(255),
    
    -- Certificate Configuration
    cert_signature_url       VARCHAR(500),
    cert_authority_name      VARCHAR(255),
    cert_authority_title     VARCHAR(255),
    cert_background_image_url VARCHAR(500),
    cert_logo_url            VARCHAR(500),
    cert_primary_color       VARCHAR(20) DEFAULT '#4f46e5',
    cert_accent_color        VARCHAR(20) DEFAULT '#db2777',
    cert_footer_text         VARCHAR(500),
    
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tenants_subdomain (subdomain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 2. USERS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id         BIGINT,
    first_name        VARCHAR(100)    NOT NULL,
    last_name         VARCHAR(100)    NOT NULL,
    email             VARCHAR(191)    NOT NULL,
    password          VARCHAR(255)    NOT NULL,
    role              VARCHAR(50)     NOT NULL DEFAULT 'STUDENT',
    -- ENUM: STUDENT, INSTRUCTOR, ADMIN
    phone_number      VARCHAR(30),
    profile_picture   VARCHAR(500),
    bio               TEXT,
    enabled           TINYINT(1)      NOT NULL DEFAULT 1,
    email_verified    TINYINT(1)      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_tenant (tenant_id),
    KEY idx_users_role (role),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 3. REFRESH TOKENS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,
    token       VARCHAR(191)    NOT NULL,
    expires_at  TIMESTAMP       NOT NULL,
    revoked     TINYINT(1)      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_token (token),
    KEY idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 4. EMAIL VERIFICATIONS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS email_verifications (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,
    token       VARCHAR(191)    NOT NULL,
    expires_at  TIMESTAMP       NOT NULL,
    used        TINYINT(1)      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_email_verif_token (token),
    KEY idx_email_verif_user (user_id),
    CONSTRAINT fk_email_verif_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 5. PASSWORD RESET TOKENS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,
    token       VARCHAR(191)    NOT NULL,
    expires_at  TIMESTAMP       NOT NULL,
    used        TINYINT(1)      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pwd_reset_token (token),
    KEY idx_pwd_reset_user (user_id),
    CONSTRAINT fk_pwd_reset_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 6. COURSES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS courses (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    instructor_id     BIGINT          NOT NULL,
    tenant_id         BIGINT,
    title             VARCHAR(300)    NOT NULL,
    description       TEXT,
    category          VARCHAR(100),
    level             VARCHAR(50)     NOT NULL DEFAULT 'BEGINNER',
    -- ENUM: BEGINNER, INTERMEDIATE, ADVANCED
    thumbnail         VARCHAR(500),
    duration          INT,                    -- total minutes
    price             DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    published         TINYINT(1)      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NULL,
    PRIMARY KEY (id),
    KEY idx_courses_instructor (instructor_id),
    KEY idx_courses_tenant (tenant_id),
    KEY idx_courses_published (published),
    KEY idx_courses_category (category),
    CONSTRAINT fk_courses_instructor FOREIGN KEY (instructor_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_courses_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 7. MODULES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS modules (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    course_id   BIGINT          NOT NULL,
    title       VARCHAR(255)    NOT NULL,
    description VARCHAR(1000),
    order_index INT             NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_modules_course (course_id),
    CONSTRAINT fk_modules_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 8. LESSONS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lessons (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    module_id        BIGINT          NOT NULL,
    title            VARCHAR(255)    NOT NULL,
    content          TEXT,
    content_type     VARCHAR(50)     NOT NULL DEFAULT 'TEXT',
    -- ENUM: TEXT, VIDEO, DOCUMENT, PDF, QUIZ, LINK, SCORM, EMBED
    video_url        VARCHAR(500),
    audio_url        VARCHAR(500),
    document_url     VARCHAR(500),
    external_url     VARCHAR(500),
    scheduled_at     TIMESTAMP       NULL,
    order_index      INT             NOT NULL DEFAULT 0,
    duration_minutes INT,
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_lessons_module (module_id),
    CONSTRAINT fk_lessons_module FOREIGN KEY (module_id)
        REFERENCES modules (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 9. COURSE PRICES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS course_prices (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    course_id         BIGINT          NOT NULL,
    type              VARCHAR(50)     NOT NULL DEFAULT 'FREE',
    -- ENUM: FREE, ONE_TIME, SUBSCRIPTION
    amount            DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    currency          CHAR(3)         NOT NULL DEFAULT 'USD',
    stripe_product_id VARCHAR(191),
    stripe_price_id   VARCHAR(191),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_course_prices_course (course_id),
    CONSTRAINT fk_course_prices_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 10. ORDERS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id                        BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                   BIGINT          NOT NULL,
    course_id                 BIGINT          NOT NULL,
    amount                    DECIMAL(10, 2)  NOT NULL,
    currency                  CHAR(3)         NOT NULL DEFAULT 'USD',
    status                    VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    -- ENUM: PENDING, COMPLETED, FAILED, REFUNDED
    stripe_payment_intent_id  VARCHAR(191)    UNIQUE,
    stripe_session_id         VARCHAR(191),
    razorpay_order_id         VARCHAR(191),
    razorpay_payment_id       VARCHAR(191),
    created_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_orders_user (user_id),
    KEY idx_orders_course (course_id),
    KEY idx_orders_status (status),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_orders_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 11. ENROLLMENTS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS enrollments (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    student_id   BIGINT      NOT NULL,
    course_id    BIGINT      NOT NULL,
    tenant_id    BIGINT      NULL,
    enrolled_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NULL,
    progress     INT         NOT NULL DEFAULT 0,    -- 0-100 %
    status       VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    -- ENUM: ACTIVE, COMPLETED, DROPPED
    completed_at TIMESTAMP,
    stripe_subscription_id VARCHAR(255),
    valid_until TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_enrollments_student_course (student_id, course_id),
    KEY idx_enrollments_student (student_id),
    KEY idx_enrollments_course (course_id),
    KEY idx_enrollments_status (status),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_enrollments_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



-- ──────────────────────────────────────────────────────────────
-- 12. ASSIGNMENTS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS assignments (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    course_id      BIGINT          NOT NULL,
    title          VARCHAR(255)    NOT NULL,
    description    TEXT,
    due_date       TIMESTAMP       NOT NULL,
    max_points     INT             NOT NULL DEFAULT 100,
    type           VARCHAR(50)     NOT NULL DEFAULT 'TEXT',
    language       VARCHAR(100),
    options        TEXT,
    attachment_url VARCHAR(500),
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_assignments_course (course_id),
    CONSTRAINT fk_assignments_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 13. SUBMISSIONS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS submissions (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    assignment_id  BIGINT      NOT NULL,
    student_id     BIGINT      NOT NULL,
    content        TEXT,
    file_url       VARCHAR(500),
    submitted_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    grade          INT,
    feedback       VARCHAR(1000),
    graded_at      TIMESTAMP,
    status         VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    -- ENUM: SUBMITTED, GRADED, LATE
    PRIMARY KEY (id),
    KEY idx_submissions_assignment (assignment_id),
    KEY idx_submissions_student (student_id),
    CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id)
        REFERENCES assignments (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_submissions_student FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 14. QUIZZES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quizzes (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    course_id           BIGINT          NOT NULL,
    lesson_id           BIGINT,                         -- optional: linked to a lesson
    title               VARCHAR(255)    NOT NULL,
    description         VARCHAR(1000),
    time_limit_minutes  INT,                            -- NULL = no limit
    passing_score       INT             NOT NULL DEFAULT 70,    -- 0-100
    attempts_allowed    INT,                            -- NULL = unlimited
    shuffle_questions   TINYINT(1)      NOT NULL DEFAULT 0,
    published           TINYINT(1)      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_quizzes_course (course_id),
    KEY idx_quizzes_lesson (lesson_id),
    KEY idx_quizzes_published (published),
    CONSTRAINT fk_quizzes_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_quizzes_lesson FOREIGN KEY (lesson_id)
        REFERENCES lessons (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 15. QUESTIONS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS questions (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    quiz_id     BIGINT          NOT NULL,
    type        VARCHAR(50)     NOT NULL,
    -- ENUM: MCQ, MSQ, TRUE_FALSE, SHORT_ANSWER, FILL_BLANK
    text        VARCHAR(2000)   NOT NULL,
    explanation VARCHAR(2000),
    points      INT             NOT NULL DEFAULT 1,
    order_index INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_questions_quiz (quiz_id),
    CONSTRAINT fk_questions_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 16. QUESTION OPTIONS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS question_options (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    question_id  BIGINT          NOT NULL,
    text         VARCHAR(1000)   NOT NULL,
    correct      TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_question_options_question (question_id),
    CONSTRAINT fk_q_options_question FOREIGN KEY (question_id)
        REFERENCES questions (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 17. QUIZ ATTEMPTS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quiz_attempts (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    user_id          BIGINT          NOT NULL,
    quiz_id          BIGINT          NOT NULL,
    score            DOUBLE,                         -- percentage 0.0–100.0
    points_earned    INT,
    points_possible  INT,
    passed           TINYINT(1)      NOT NULL DEFAULT 0,
    status           VARCHAR(50)     NOT NULL DEFAULT 'IN_PROGRESS',
    -- ENUM: IN_PROGRESS, SUBMITTED, GRADED
    started_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at     TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_quiz_attempts_user (user_id),
    KEY idx_quiz_attempts_quiz (quiz_id),
    KEY idx_quiz_attempts_status (status),
    CONSTRAINT fk_quiz_attempts_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_quiz_attempts_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 18. QUIZ ANSWERS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quiz_answers (
    id                 BIGINT          NOT NULL AUTO_INCREMENT,
    attempt_id         BIGINT          NOT NULL,
    question_id        BIGINT          NOT NULL,
    selected_option_id BIGINT,                     -- MCQ / TRUE_FALSE
    text_answer        VARCHAR(2000),              -- SHORT_ANSWER / FILL_BLANK
    correct            TINYINT(1)      NOT NULL DEFAULT 0,
    points_awarded     INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_quiz_answers_attempt (attempt_id),
    KEY idx_quiz_answers_question (question_id),
    CONSTRAINT fk_quiz_answers_attempt FOREIGN KEY (attempt_id)
        REFERENCES quiz_attempts (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_quiz_answers_question FOREIGN KEY (question_id)
        REFERENCES questions (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_quiz_answers_option FOREIGN KEY (selected_option_id)
        REFERENCES question_options (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 18a. QUIZ ANSWER OPTIONS (join table for MSQ multi-select)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quiz_answer_options (
    answer_id  BIGINT  NOT NULL,
    option_id  BIGINT  NOT NULL,
    PRIMARY KEY (answer_id, option_id),
    CONSTRAINT fk_qao_answer FOREIGN KEY (answer_id)
        REFERENCES quiz_answers (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_qao_option FOREIGN KEY (option_id)
        REFERENCES question_options (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 19. LESSON PROGRESS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lesson_progress (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    user_id               BIGINT      NOT NULL,
    lesson_id             BIGINT      NOT NULL,
    watched_seconds       INT         NOT NULL DEFAULT 0,
    completion_percentage INT         NOT NULL DEFAULT 0,    -- 0-100
    completed             TINYINT(1)  NOT NULL DEFAULT 0,
    completed_at          TIMESTAMP,
    last_position         INT         NOT NULL DEFAULT 0,   -- resume position (seconds)
    PRIMARY KEY (id),
    UNIQUE KEY uq_lesson_progress_user_lesson (user_id, lesson_id),
    KEY idx_lesson_progress_user (user_id),
    KEY idx_lesson_progress_lesson (lesson_id),
    CONSTRAINT fk_lesson_progress_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_lesson_progress_lesson FOREIGN KEY (lesson_id)
        REFERENCES lessons (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 20. CERTIFICATES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS certificates (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    user_id           BIGINT          NOT NULL,
    course_id         BIGINT          NOT NULL,
    verification_code VARCHAR(50)     NOT NULL,
    pdf_url           VARCHAR(500),
    issued_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_certificates_user_course (user_id, course_id),
    UNIQUE KEY uq_certificates_code (verification_code),
    KEY idx_certificates_user (user_id),
    KEY idx_certificates_course (course_id),
    CONSTRAINT fk_certificates_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_certificates_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 21. REVIEWS & RATINGS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    course_id   BIGINT          NOT NULL,
    student_id  BIGINT          NOT NULL,
    rating      INT             NOT NULL, -- 1 to 5
    comment     TEXT,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_reviews_student_course (student_id, course_id),
    KEY idx_reviews_course (course_id),
    KEY idx_reviews_student (student_id),
    CONSTRAINT fk_reviews_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_reviews_student FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- 22. LIVE CLASSES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS live_classes (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    course_id        BIGINT          NOT NULL,
    title            VARCHAR(255)    NOT NULL,
    description      TEXT,
    start_time       TIMESTAMP       NOT NULL,
    duration_minutes INT,
    meeting_url      VARCHAR(500)    NOT NULL,
    status           VARCHAR(50)     NOT NULL DEFAULT 'UPCOMING',
    -- ENUM: UPCOMING, LIVE, COMPLETED, CANCELLED
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_live_classes_course (course_id),
    KEY idx_live_classes_status (status),
    CONSTRAINT fk_live_classes_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ──────────────────────────────────────────────────────────────
-- 23. NOTIFICATIONS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,
    title       VARCHAR(255)    NOT NULL,
    message     TEXT            NOT NULL,
    type        VARCHAR(50)     NOT NULL,
    -- ENUM: SYSTEM, ENROLLMENT, ASSIGNMENT, PAYMENT
    is_read     TINYINT(1)      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notifications_user (user_id),
    KEY idx_notifications_is_read (is_read),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ──────────────────────────────────────────────────────────────
-- Re-enable FK checks
-- ──────────────────────────────────────────────────────────────
SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- Sample Seed Data  (optional – safe to remove in production)
-- ============================================================

-- Default super-admin tenant
INSERT IGNORE INTO tenants (id, name, subdomain, plan, status, contact_email, primary_color, accent_color, tagline)
VALUES (1, 'LearnHub Global', 'global', 'BUSINESS', 'ACTIVE', 'admin@learnhub.com', '#4f46e5', '#db2777', 'LMS Infrastructure');

-- Default admin user  (password: Admin@123 – BCrypt hash)
INSERT IGNORE INTO users (id, tenant_id, first_name, last_name, email, password, role, status, email_verified)
VALUES (
    1, 1,
    'Super', 'Admin',
    'admin@learnhub.com',
    '$2a$12$7QJ4z3mOEW.BQEj1BMyaGei8VR0oSuEnzPBFXOO6LDfHiVHlm0O0K',
    'SUPER_ADMIN',
    'ACTIVE',
    1
);

