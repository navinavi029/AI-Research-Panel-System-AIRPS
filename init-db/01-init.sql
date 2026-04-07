-- AI Panelist System Database Initialization Script
-- This script runs automatically when the PostgreSQL container starts for the first time

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create indexes for better performance (will be created by Hibernate, but we can add custom ones)
-- Note: Most tables and indexes are created by Hibernate based on JPA entities

-- Create a function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE aipanelist TO aipanelist;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO aipanelist;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO aipanelist;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO aipanelist;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO aipanelist;

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'AI Panelist System database initialized successfully';
END $$;
