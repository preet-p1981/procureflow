-- V2: identity module (users, roles, permissions)

CREATE TABLE permission (
    code        VARCHAR(60)  PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE role (
    code        VARCHAR(40)  PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE role_permission (
    role_code       VARCHAR(40) NOT NULL REFERENCES role (code),
    permission_code VARCHAR(60) NOT NULL REFERENCES permission (code),
    PRIMARY KEY (role_code, permission_code)
);

CREATE TABLE app_user (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT chk_failed_attempts CHECK (failed_attempts >= 0)
);

-- one account per email, case-insensitive
CREATE UNIQUE INDEX uq_app_user_email ON app_user (lower(email));

CREATE TABLE user_role (
    user_id   UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_code VARCHAR(40) NOT NULL REFERENCES role (code),
    PRIMARY KEY (user_id, role_code)
);

CREATE INDEX idx_user_role_role ON user_role (role_code);

-- seed data: roles from the project plan
INSERT INTO role (code, description) VALUES
    ('ADMIN',               'System administrator'),
    ('REQUESTER',           'Employee who raises requisitions'),
    ('DEPARTMENT_MANAGER',  'First-level approver'),
    ('FINANCE_APPROVER',    'Approves high-value spend, invoices and payments'),
    ('PROCUREMENT_OFFICER', 'Manages vendors, RFQs and purchase orders'),
    ('WAREHOUSE_CLERK',     'Records goods receipts'),
    ('VENDOR',              'External vendor using the portal'),
    ('AUDITOR',             'Read-only access including audit log');

-- identity permissions only; each later module adds its own in its own migration
INSERT INTO permission (code, description) VALUES
    ('USER_READ',   'View users'),
    ('USER_MANAGE', 'Create, update, lock and unlock users'),
    ('ROLE_MANAGE', 'Assign roles to users'),
    ('AUDIT_READ',  'View the audit log');

INSERT INTO role_permission (role_code, permission_code) VALUES
    ('ADMIN',   'USER_READ'),
    ('ADMIN',   'USER_MANAGE'),
    ('ADMIN',   'ROLE_MANAGE'),
    ('ADMIN',   'AUDIT_READ'),
    ('AUDITOR', 'USER_READ'),
    ('AUDITOR', 'AUDIT_READ');