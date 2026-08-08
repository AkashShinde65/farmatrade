CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    aadhaar_hash VARCHAR(128) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_by_admin_id BIGINT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_mobile UNIQUE (mobile),
    CONSTRAINT uk_users_aadhaar_hash UNIQUE (aadhaar_hash),
    CONSTRAINT fk_users_created_by_admin FOREIGN KEY (created_by_admin_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE security_audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL,
    acting_user_id BIGINT NULL,
    target_user_id BIGINT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_event_type ON security_audit_events(event_type);
CREATE INDEX idx_audit_created_at ON security_audit_events(created_at);
CREATE INDEX idx_audit_acting_user_id ON security_audit_events(acting_user_id);
CREATE INDEX idx_audit_target_user_id ON security_audit_events(target_user_id);
