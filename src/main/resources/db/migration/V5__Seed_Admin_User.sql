-- Seed initial admin user
-- Migration: V5 - Seed Admin User
-- This creates a default admin user for initial system access
-- Default credentials: admin / Admin@123
-- IMPORTANT: Change password after first login in production!

-- Insert default admin user with BCrypt hashed password
-- Password hash for: Admin@123
-- Generated using BCrypt with strength 10
INSERT INTO users (
    id,
    username,
    email,
    password,
    first_name,
    last_name,
    role,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid()::text,
    'admin',
    'admin@hrragwiser.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye/IVI6Z.dWShRBY5QdYKvvFqB.EqLv0y', -- BCrypt hash for 'Admin@123'
    'System',
    'Administrator',
    'ADMIN',
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING; -- Prevent duplicate if migration is re-run

-- Insert sample HR Manager user
-- Password: HrManager@123
INSERT INTO users (
    id,
    username,
    email,
    password,
    first_name,
    last_name,
    role,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid()::text,
    'hrmanager',
    'hrmanager@hrragwiser.com',
    '$2a$10$8K1p/a0dL2LKl1YyNjI7.O7P9XU1YqFUvGKKJ2zxGv6vBxqGFoTmq', -- BCrypt hash for 'HrManager@123'
    'HR',
    'Manager',
    'HR_MANAGER',
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Insert sample regular user
-- Password: User@123
INSERT INTO users (
    id,
    username,
    email,
    password,
    first_name,
    last_name,
    role,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid()::text,
    'user',
    'user@hrragwiser.com',
    '$2a$10$dXJ3SW6G7P37LnV1UaSTOe/cbKCqpCDZCb0tMszXRKvkRrJZUvvWe', -- BCrypt hash for 'User@123'
    'Regular',
    'User',
    'USER',
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Add comments
COMMENT ON CONSTRAINT users_username_key ON users IS 'Ensures username uniqueness';
COMMENT ON CONSTRAINT users_email_key ON users IS 'Ensures email uniqueness';

-- Log seed completion
DO $$
BEGIN
    RAISE NOTICE 'Admin user seeding completed';
    RAISE NOTICE 'Default credentials:';
    RAISE NOTICE '  Admin    - Username: admin,      Password: Admin@123';
    RAISE NOTICE '  HR Mgr   - Username: hrmanager,  Password: HrManager@123';
    RAISE NOTICE '  User     - Username: user,       Password: User@123';
    RAISE NOTICE 'IMPORTANT: Change these passwords in production!';
END $$;

