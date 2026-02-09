-- create schema
create schema auth;

-- Users table
CREATE TABLE IF NOT EXISTS auth.auth_users
(
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username               VARCHAR(50) UNIQUE  NOT NULL,
    email                  VARCHAR(100) UNIQUE NOT NULL,
    password               VARCHAR(255)        NOT NULL,
    first_name             VARCHAR(50),
    last_name              VARCHAR(50),
    enabled                BOOLEAN          DEFAULT TRUE,
    email_verified         BOOLEAN          DEFAULT FALSE,
    credential_non_expired BOOLEAN          DEFAULT TRUE,
    account_non_expired    BOOLEAN          DEFAULT TRUE,
    account_non_locked     BOOLEAN          DEFAULT TRUE,
    created_at             TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    last_login_at          TIMESTAMP
);

-- Roles table
CREATE TABLE IF NOT EXISTS auth.roles
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- User roles junction table
CREATE TABLE IF NOT EXISTS auth.user_roles
(
    user_id UUID NOT NULL REFERENCES auth_users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Permissions table
CREATE TABLE IF NOT EXISTS auth.permissions
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    resource    VARCHAR(100)        NOT NULL,
    action      VARCHAR(50)         NOT NULL
);

-- Role permissions junction table
CREATE TABLE IF NOT EXISTS auth.role_permissions
(
    role_id       UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Refresh tokens table
CREATE TABLE IF NOT EXISTS auth.refresh_tokens
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID                NOT NULL REFERENCES auth_users (id) ON DELETE CASCADE,
    token      VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP           NOT NULL,
    revoked    BOOLEAN          DEFAULT FALSE,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
    --INDEX idx_refresh_token_token (token),
    --INDEX idx_refresh_token_user_id (user_id)
);

-- Clients table (for OAuth2 client credentials)
CREATE TABLE IF NOT EXISTS auth.oauth_clients
(
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id              VARCHAR(100) UNIQUE NOT NULL,
    client_secret          VARCHAR(255)        NOT NULL,
    client_name            VARCHAR(100)        NOT NULL,
    enabled                BOOLEAN          DEFAULT TRUE,
    access_token_validity  INT              DEFAULT 3600,
    refresh_token_validity INT              DEFAULT 86400,
    scopes                 VARCHAR(500)     DEFAULT 'read write',
    authorized_grant_types VARCHAR(500)     DEFAULT 'password refresh_token client_credentials',
    created_at             TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- Insert default roles
INSERT INTO auth.roles (name, description)
VALUES ('ROLE_USER', 'Regular user'),
       ('ROLE_ADMIN', 'Administrator'),
       ('ROLE_MODERATOR', 'Content moderator');

-- Insert default permissions
INSERT INTO auth.permissions (name, description, resource, action)
VALUES ('user:read', 'Read user information', 'user', 'read'),
       ('user:write', 'Create/update users', 'user', 'write'),
       ('user:delete', 'Delete users', 'user', 'delete'),
       ('admin:access', 'Access admin panel', 'admin', 'access');


