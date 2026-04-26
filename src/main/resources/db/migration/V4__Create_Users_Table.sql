-- Create users table for database authentication
-- Migration: V4 - Create Users Table

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- BCrypt hashed password
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(enabled);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at DESC);

-- Create composite index for common authentication queries
CREATE INDEX IF NOT EXISTS idx_users_username_enabled ON users(username, enabled);

-- Add comments for documentation
COMMENT ON TABLE users IS 'User accounts for authentication and authorization';
COMMENT ON COLUMN users.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN users.username IS 'Unique username for login';
COMMENT ON COLUMN users.email IS 'Unique email address';
COMMENT ON COLUMN users.password IS 'BCrypt hashed password';
COMMENT ON COLUMN users.role IS 'User role: ADMIN, USER, or HR_MANAGER';
COMMENT ON COLUMN users.enabled IS 'Whether the account is enabled';
COMMENT ON COLUMN users.account_non_expired IS 'Whether the account has not expired';
COMMENT ON COLUMN users.account_non_locked IS 'Whether the account is not locked';
COMMENT ON COLUMN users.credentials_non_expired IS 'Whether the credentials have not expired';

