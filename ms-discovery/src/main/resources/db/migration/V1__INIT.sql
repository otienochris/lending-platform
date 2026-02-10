create schema  if not exists tasks;
create table if not exists tasks.scheduled_tasks (
                                                     task_name text not null,
                                                     task_instance text not null,
                                                     task_data bytea,
                                                     execution_time timestamp with time zone not null,
                                                     picked BOOLEAN not null,
                                                     picked_by text,
                                                     last_success timestamp with time zone,
                                                     last_failure timestamp with time zone,
                                                     consecutive_failures INT,
                                                     last_heartbeat timestamp with time zone,
                                                     version BIGINT not null,
                                                     priority SMALLINT,
                                                     PRIMARY KEY (task_name, task_instance)
    );

CREATE INDEX if not exists execution_time_idx ON tasks.scheduled_tasks (execution_time);
CREATE INDEX if not exists last_heartbeat_idx ON tasks.scheduled_tasks (last_heartbeat);
CREATE INDEX if not exists priority_execution_time_idx on tasks.scheduled_tasks (priority desc, execution_time asc);



----- **************** orchestrator db ************-----------

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
create schema if not exists orchestrator;
-- --------------------
-- Sagas
-- --------------------
CREATE TABLE if not exists orchestrator.sagas (
                                                  saga_id          UUID PRIMARY key DEFAULT uuid_generate_v4(),
    saga_type        VARCHAR(50) NOT NULL,
    business_key     VARCHAR(100) NOT NULL, -- loan_id
    status           VARCHAR(30) NOT NULL,
    current_step     VARCHAR(100),
    original_request text not null,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX if not exists idx_sagas_business_key
    ON orchestrator.sagas (business_key);


-- --------------------
-- Saga Steps
-- --------------------
--select * from orchestrator.saga_steps
CREATE TABLE if not exists orchestrator.saga_steps (
                                                       step_id          UUID PRIMARY key DEFAULT uuid_generate_v4(),
    saga_id          UUID NOT NULL,
    step_name        VARCHAR(100) NOT NULL,
    status           VARCHAR(30) NOT NULL,
    retry_count      INT NOT NULL DEFAULT 0,
    last_error       TEXT,
    executed_at      TIMESTAMP,
    CONSTRAINT fk_saga
    FOREIGN KEY (saga_id) REFERENCES orchestrator.sagas (saga_id)
    ON DELETE CASCADE
    );

CREATE INDEX if not exists  idx_saga_steps_saga
    ON orchestrator.saga_steps (saga_id);

-- --------------------
-- Outbox (Exactly-once)
-- --------------------
--select * from orchestrator.outbox_events
CREATE TABLE if not exists orchestrator.outbox_events (
                                                          event_id         UUID PRIMARY key DEFAULT uuid_generate_v4(),
    aggregate_type   VARCHAR(50) NOT NULL,
    aggregate_id     VARCHAR(100) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    payload          TEXT NOT NULL,
    published        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX if not exists idx_outbox_unpublished
    ON orchestrator.outbox_events (published)
    WHERE published = FALSE;



CREATE TABLE if not exists orchestrator.command_deduplication (
                                                                  command_id uuid NOT null DEFAULT uuid_generate_v4(),
    processed_at timestamp DEFAULT now() NOT NULL,
    CONSTRAINT command_deduplication_pkey PRIMARY KEY (command_id)
    );


----- **************** loan_disbursement db ************-----------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

create schema if not exists disbursements;
-- --------------------
-- Disbursements
-- --------------------
CREATE TABLE if not exists disbursements.loan_disbursements (
                                                                disbursement_id  UUID PRIMARY key DEFAULT uuid_generate_v4(),
    loan_id          UUID NOT NULL,
    customer_id      UUID NOT NULL,
    product_id      UUID NOT NULL,
    amount           NUMERIC(15,2) NOT NULL,
    status           VARCHAR(30) NOT NULL,
    reference_no     VARCHAR(100),
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (loan_id)
    );

-- --------------------
-- Idempotency
-- --------------------
CREATE TABLE if not exists disbursements.command_deduplication (
                                                                   command_id       UUID PRIMARY KEY,
                                                                   processed_at     TIMESTAMP NOT NULL DEFAULT now()
    );

-- --------------------
-- Outbox
-- --------------------
CREATE TABLE if not exists disbursements.outbox_events (
                                                           event_id         UUID PRIMARY key DEFAULT uuid_generate_v4(),
    aggregate_type   VARCHAR(50) NOT NULL,
    aggregate_id     VARCHAR(100) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    payload          TEXT NOT NULL,
    published        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX if not exists idx_disbursement_outbox_unpublished
    ON disbursements.outbox_events (published)
    WHERE published = FALSE;


---------*****************loan repayment*********------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
create schema if not exists repayments;

-- --------------------
-- Loans
-- --------------------
-- select * from repayments.loans;
-- update repayments.loans set status = 'OPEN' where status = 'PENDING';
CREATE TABLE if not exists repayments.loans (
                                                id					UUID PRIMARY key default uuid_generate_v4(),
    loan_id              UUID not null,
    customer_id          UUID NOT NULL,
    principal_amount     NUMERIC(15,2) NOT NULL,
    interest_rate        NUMERIC(5,2) NOT NULL,
    tenure_months        INT NOT NULL,
    outstanding_amount   NUMERIC(15,2) NOT NULL,
    status               VARCHAR(30) NOT NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT now()
    );

-- --------------------
-- Repayment Schedule
-- --------------------
-- select * from repayments.repayment_schedule
-- update repayments.repayment_schedule set status = 'OPEN' where status = 'PENDING';
-- drop  table repayments.repayment_schedule ;
CREATE TABLE if not exists repayments.repayment_schedule (
                                                             schedule_id          UUID PRIMARY key default uuid_generate_v4(),
    loan_id              UUID NOT NULL,
    due_date             DATE NOT NULL,
    total_paid			numeric(15,2) not null default 0,
    emi_amount           NUMERIC(15,2) NOT NULL,
    principal_component  NUMERIC(15,2) NOT NULL,
    interest_component   NUMERIC(15,2) NOT NULL,
    status               VARCHAR(30) NOT NULL
    );

CREATE index if not exists idx_schedule_loan
    ON repayments.repayment_schedule (loan_id);

-- --------------------
-- Repayments
-- --------------------
-- select * from repayments.repayments;
-- drop table repayments.repayments;
CREATE TABLE if not exists repayments.repayments (
                                                     repayment_id         UUID PRIMARY key default uuid_generate_v4(),
    loan_id              UUID NOT NULL,
    schedule_id          UUID not null,
    amount_paid          NUMERIC(15,2) NOT NULL,
    payment_date         TIMESTAMP NOT NULL,
    payment_mode         VARCHAR(50),
    status               VARCHAR(30) NOT NULL
    );

-- --------------------
-- Idempotency
-- --------------------
CREATE TABLE if not exists repayments.command_deduplication (
                                                                command_id           UUID PRIMARY KEY,
                                                                processed_at         TIMESTAMP NOT NULL DEFAULT now()
    );

-- --------------------
-- Outbox
-- --------------------
CREATE TABLE if not exists repayments.outbox_events (
                                                        event_id             UUID PRIMARY key default uuid_generate_v4(),
    aggregate_type       VARCHAR(50) NOT NULL,
    aggregate_id         VARCHAR(100) NOT NULL,
    event_type           VARCHAR(100) NOT NULL,
    payload              TEXT NOT NULL,
    published            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX if not exists idx_repayment_outbox_unpublished
    ON repayments.outbox_events (published)
    WHERE published = FALSE;



----********* product configuration *******--------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
create schema if not exists product_configs;

-- --------------------
-- Loan Products
-- --------------------
-- select * from product_configs.outbox_events;
CREATE TABLE if not exists product_configs.outbox_events (
                                                             event_id         UUID PRIMARY key DEFAULT uuid_generate_v4(),
    aggregate_type   VARCHAR(50) NOT NULL,
    aggregate_id     VARCHAR(100) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    payload          TEXT NOT NULL,
    published        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
    );
CREATE INDEX if not exists idx_repayment_outbox_unpublished
    ON product_configs.outbox_events (published)
    WHERE published = FALSE;


-- Create the loan_products table

-- drop table product_configs.loan_products;
-- select * from product_configs.loan_products
CREATE TABLE IF NOT EXISTS product_configs.loan_products (
    -- Primary key with UUID
                                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Product identification
    product_name VARCHAR(100) NOT null UNIQUE,

    -- Amount constraints with check for positive values
    min_amount DECIMAL(15, 2) NOT NULL CHECK (min_amount >= 0),
    max_amount DECIMAL(15, 2) NOT NULL CHECK (max_amount >= 0),

    -- Rate details
    interest_rate DECIMAL(5, 2) NOT NULL CHECK (interest_rate >= 0 AND interest_rate <= 100),
    interest_rate_type VARCHAR(20) NOT NULL,

    -- Tenure configuration
    tenure_options VARCHAR(100) NOT NULL, -- e.g., "6,12,24,36" or "12-60:6"
    tenure_options_type VARCHAR(20) NOT null,

    -- Effective date range for the product
    effective_from TIMESTAMP NOT NULL DEFAULT CURRENT_DATE,
    effective_to TIMESTAMP,

    -- Status flag with index for active products
    active BOOLEAN NOT NULL DEFAULT true,

    currency varchar(50) not null default 'KES',
    support_installments boolean not null default true,

    -- Audit columns (highly recommended)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Data integrity constraints
    CONSTRAINT chk_amount_range CHECK (max_amount >= min_amount),
    CONSTRAINT chk_effective_dates CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT chk_tenure_format CHECK (
                                           tenure_options ~ '^(\d+(-\d+(:(\d+))?)?)(,\s*\d+(-\d+(:(\d+))?)?)*$'
                                       )
    );


-- --------------------
-- Fees
-- --------------------

-- select * from product_configs.fees
CREATE TABLE if not exists product_configs.fees (
                                                    fee_id           UUID PRIMARY key DEFAULT uuid_generate_v4(),
    product_id       UUID NOT NULL,
    fee_type         VARCHAR(50) NOT NULL,
    fee_value_type   VARCHAR(50) NOT NULL,
    fee_value       NUMERIC(15,2) NOT null,
    applicable_at     varchar(255) not null
    );

CREATE INDEX if not exists idx_fees_product
    ON product_configs.fees (product_id);


------********** notification service ********---------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
create schema if not exists notifications;
-- --------------------
-- Notifications
-- --------------------
CREATE TABLE if not exists notifications.notifications (
                                                           notification_id   UUID PRIMARY KEY,
                                                           recipient         VARCHAR(100) NOT NULL,
    channel           VARCHAR(30) NOT NULL,
    template_code     VARCHAR(100) NOT NULL,
    payload           JSONB NOT NULL,
    status            VARCHAR(30) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
    );

-- --------------------
-- Outbox (optional if async delivery)
-- --------------------
CREATE TABLE if not exists notifications.outbox_events (
                                                           event_id          UUID PRIMARY KEY,
                                                           aggregate_type    VARCHAR(50) NOT NULL,
    aggregate_id      VARCHAR(100),
    event_type        VARCHAR(100) NOT NULL,
    payload           JSONB NOT NULL,
    published         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX if not exists idx_notification_outbox_unpublished
    ON notifications.outbox_events (published)
    WHERE published = FALSE;



-- =============================================
-- AUTHENTICATION SCHEMA FOR LENDSPRINT PLATFORM
-- =============================================

-- Create the auth schema
CREATE SCHEMA IF NOT EXISTS auth;

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE if not exists auth.outbox_events (
                                                  event_id         UUID PRIMARY key DEFAULT uuid_generate_v4(),
    aggregate_type   VARCHAR(50) NOT NULL,
    aggregate_id     VARCHAR(100) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    payload          TEXT NOT NULL,
    published        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
    );

-- =============================================
-- 1. PERMISSIONS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS auth.permissions (
                                                id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    resource VARCHAR(100) NOT NULL, -- e.g., 'LOAN', 'USER', 'REPORT'
    action VARCHAR(50) NOT NULL,    -- e.g., 'CREATE', 'READ', 'UPDATE', 'DELETE'

-- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Ensure unique combination of resource and action
    CONSTRAINT uq_resource_action UNIQUE (resource, action),
    CONSTRAINT chk_action CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'EXPORT'))
    );

-- Index for faster permission lookups
CREATE INDEX if not exists idx_permissions_resource ON auth.permissions(resource);
CREATE INDEX if not exists idx_permissions_name ON auth.permissions(name);

-- =============================================
-- 2. ROLES TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS auth.roles (
                                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    is_system_role BOOLEAN DEFAULT FALSE, -- System roles cannot be deleted

-- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
    );

-- Index for role name lookups
CREATE INDEX if not exists idx_roles_name ON auth.roles(name);

-- =============================================
-- 3. ROLE_PERMISSIONS (Junction table)
-- =============================================
CREATE TABLE IF NOT EXISTS auth.role_permissions (
                                                     role_id UUID NOT NULL,
                                                     permission_id UUID NOT NULL,

    -- Audit fields
                                                     assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                     assigned_by VARCHAR(100),

    PRIMARY KEY (role_id, permission_id),

    -- Foreign keys with CASCADE delete
    CONSTRAINT fk_role_permissions_role
    FOREIGN KEY (role_id)
    REFERENCES auth.roles(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
    FOREIGN KEY (permission_id)
    REFERENCES auth.permissions(id)
    ON DELETE CASCADE
    );

-- Index for permission-based role lookups
CREATE INDEX if not exists idx_role_permissions_permission ON auth.role_permissions(permission_id);

-- =============================================
-- 4. USERS TABLE (auth_users as per your entity)
-- =============================================
-- truncate table auth.auth_users cascade;
-- select * from auth.auth_users;
-- update auth.auth_users set loan_limit = 1500
CREATE TABLE IF NOT EXISTS auth.auth_users (
                                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Hashed password

-- Personal details
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),

    -- Status flags
    is_enabled BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_phone_verified BOOLEAN DEFAULT FALSE,

    -- Security flags (Spring Security compatible)
    is_credentials_non_expired BOOLEAN DEFAULT TRUE,
    is_account_non_expired BOOLEAN DEFAULT TRUE,
    is_account_non_locked BOOLEAN DEFAULT TRUE,

    -- Timestamps
    email_verified_at TIMESTAMP,
    phone_verified_at TIMESTAMP,
    last_login_at TIMESTAMP,
    password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Login tracking
    failed_login_attempts INTEGER DEFAULT 0,
    last_failed_login_at TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    msisdn varchar(255) not null,
    loan_limit NUMERIC(15,2) default 500,
    is_blacklisted varchar(255) not null,
    -- Constraints
    CONSTRAINT chk_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_username_length CHECK (LENGTH(username) >= 3)
    );

-- alter table auth.auth_users add column loan_limit NUMERIC(15,2) default 500;


-- Indexes for user lookups
CREATE INDEX if not exists idx_users_email ON auth.auth_users(email);
CREATE INDEX if not exists idx_users_username ON auth.auth_users(username);
CREATE INDEX if not exists idx_users_status ON auth.auth_users(is_enabled, is_account_non_locked);
CREATE INDEX if not exists idx_users_created_at ON auth.auth_users(created_at);

-- =============================================
-- 5. USER_ROLES (Junction table)
-- =============================================
CREATE TABLE IF NOT EXISTS auth.user_roles (
                                               user_id UUID NOT NULL,
                                               role_id UUID NOT NULL,

    -- Audit fields
                                               assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               assigned_by VARCHAR(100),

    PRIMARY KEY (user_id, role_id),

    -- Foreign keys with CASCADE delete
    CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id)
    REFERENCES auth.auth_users(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id)
    REFERENCES auth.roles(id)
    ON DELETE CASCADE
    );

-- Index for user-based role lookups
CREATE INDEX if not exists idx_user_roles_role ON auth.user_roles(role_id);

-- =============================================
-- 6. REFRESH_TOKENS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
                                                   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,

    -- Token metadata
    device_info TEXT,
    ip_address INET,
    user_agent TEXT,

    -- Expiry and status
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revoked_reason TEXT,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id)
    REFERENCES auth.auth_users(id)
    ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_expiry CHECK (expires_at > created_at)
    );

-- Indexes for token management
CREATE INDEX if not exists idx_refresh_tokens_user ON auth.refresh_tokens(user_id);
CREATE INDEX if not exists idx_refresh_tokens_token ON auth.refresh_tokens(token);
CREATE INDEX if not exists idx_refresh_tokens_expiry ON auth.refresh_tokens(expires_at) WHERE is_revoked = FALSE;
CREATE INDEX if not exists idx_refresh_tokens_active ON auth.refresh_tokens(user_id, is_revoked, expires_at);

-- =============================================
-- 7. PASSWORD_RESET_TOKENS (Optional but recommended)
-- =============================================
CREATE TABLE IF NOT EXISTS auth.password_reset_tokens (
                                                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_tokens_user
    FOREIGN KEY (user_id)
    REFERENCES auth.auth_users(id)
    ON DELETE CASCADE
    );

CREATE INDEX if not exists idx_password_reset_tokens_token ON auth.password_reset_tokens(token);
CREATE INDEX if not exists idx_password_reset_tokens_user ON auth.password_reset_tokens(user_id, is_used, expires_at);


-- =============================================
-- INSERT DEFAULT DATA
-- =============================================

-- Insert default permissions
INSERT INTO auth.permissions (name, description, resource, action) VALUES
-- User permissions
('USER_CREATE', 'Create new users', 'USER', 'CREATE'),
('USER_READ', 'View user details', 'USER', 'READ'),
('USER_UPDATE', 'Update user information', 'USER', 'UPDATE'),
('USER_DELETE', 'Delete users', 'USER', 'DELETE'),

-- Loan permissions
('LOAN_CREATE', 'Create new loan applications', 'LOAN', 'CREATE'),
('LOAN_READ', 'View loan details', 'LOAN', 'READ'),
('LOAN_UPDATE', 'Update loan information', 'LOAN', 'UPDATE'),
('LOAN_APPROVE', 'Approve loans', 'LOAN', 'APPROVE'),
('LOAN_REJECT', 'Reject loans', 'LOAN', 'REJECT'),

-- Role permissions
('ROLE_MANAGE', 'Manage roles and permissions', 'ROLE', 'UPDATE'),

-- Report permissions
('REPORT_VIEW', 'View all reports', 'REPORT', 'READ'),
('REPORT_EXPORT', 'Export report data', 'REPORT', 'EXPORT')
    ON CONFLICT (resource, action) DO NOTHING;

-- Insert default roles
INSERT INTO auth.roles (name, description, is_system_role) VALUES
                                                               ('SUPER_ADMIN', 'Full system access', TRUE),
                                                               ('LOAN_OFFICER', 'Can manage loan applications', FALSE),
                                                               ('LOAN_APPROVER', 'Can approve/reject loans', FALSE),
                                                               ('CUSTOMER', 'Regular customer user', FALSE)
    ON CONFLICT (name) DO NOTHING;

-- Assign permissions to roles
-- Super Admin gets all permissions
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM auth.roles r
         CROSS JOIN auth.permissions p
WHERE r.name = 'SUPER_ADMIN'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Loan Officer permissions
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM auth.roles r, auth.permissions p
WHERE r.name = 'LOAN_OFFICER'
  AND p.resource = 'LOAN'
  AND p.action IN ('CREATE', 'READ', 'UPDATE')
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Loan Approver permissions
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM auth.roles r, auth.permissions p
WHERE r.name = 'LOAN_APPROVER'
  AND p.resource = 'LOAN'
  AND p.action IN ('READ', 'APPROVE', 'REJECT')
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Customer permissions
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM auth.roles r, auth.permissions p
WHERE r.name = 'CUSTOMER'
  AND p.resource = 'LOAN'
  AND p.action IN ('CREATE', 'READ')
    ON CONFLICT (role_id, permission_id) DO NOTHING;



