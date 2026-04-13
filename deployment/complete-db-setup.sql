-- ============================================================================
-- KTLO Simulator - Complete Database Setup Script
-- Execute this entire file in DBeaver SQL Editor
-- Database: code_test @ muliagent.postgres.database.azure.com
-- ============================================================================

-- ============================================================================
-- DATABASE VERIFICATION - Ensure we're connected to code_test
-- ============================================================================

DO $$
BEGIN
    IF current_database() != 'code_test' THEN
        RAISE EXCEPTION 'ERROR: You are connected to database "%". Please connect to "code_test" database before running this script!', current_database();
    END IF;
    RAISE NOTICE 'SUCCESS: Connected to correct database: %', current_database();
END $$;

-- ============================================================================
-- SECTION 1: DROP EXISTING TABLES (Clean Start)
-- ============================================================================

DROP TABLE IF EXISTS processing_jobs CASCADE;
DROP TABLE IF EXISTS loadsimulator CASCADE;

-- ============================================================================
-- SECTION 2: CREATE LOADSIMULATOR TABLE
-- ============================================================================

CREATE TABLE loadsimulator (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_loadsimulator_name ON loadsimulator(name);
CREATE INDEX idx_loadsimulator_status ON loadsimulator(status);
CREATE INDEX idx_loadsimulator_created_at ON loadsimulator(created_at);

-- Add constraints
ALTER TABLE loadsimulator ADD CONSTRAINT chk_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'FAILED'));

-- Add comments for documentation
COMMENT ON TABLE loadsimulator IS 'Table for KTLO load simulation testing';
COMMENT ON COLUMN loadsimulator.id IS 'Primary key - auto-incrementing ID';
COMMENT ON COLUMN loadsimulator.name IS 'Name of the test record';
COMMENT ON COLUMN loadsimulator.description IS 'Description of the test scenario';
COMMENT ON COLUMN loadsimulator.status IS 'Status values: ACTIVE, INACTIVE, PENDING, FAILED';
COMMENT ON COLUMN loadsimulator.created_at IS 'Timestamp when record was created';
COMMENT ON COLUMN loadsimulator.updated_at IS 'Timestamp when record was last updated';

-- ============================================================================
-- SECTION 3: CREATE TRIGGER FUNCTION FOR AUTO-UPDATE TIMESTAMP
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for loadsimulator table
CREATE TRIGGER update_loadsimulator_updated_at
    BEFORE UPDATE ON loadsimulator
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- SECTION 4: CREATE PROCESSING_JOBS TABLE (Document Processing Platform)
-- ============================================================================

CREATE TABLE processing_jobs (
    job_id VARCHAR(36) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(10) NOT NULL,
    file_size BIGINT,
    file_path VARCHAR(500),
    operation_type VARCHAR(20) NOT NULL,
    quality VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    progress INTEGER DEFAULT 0,
    simulation_type VARCHAR(50),
    simulation_endpoint VARCHAR(200),
    result TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_job_status ON processing_jobs(status);
CREATE INDEX idx_job_created ON processing_jobs(created_at);
CREATE INDEX idx_job_operation ON processing_jobs(operation_type);

-- Add constraints
ALTER TABLE processing_jobs ADD CONSTRAINT chk_job_status
    CHECK (status IN ('QUEUED', 'UPLOADING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'));

ALTER TABLE processing_jobs ADD CONSTRAINT chk_job_operation
    CHECK (operation_type IN ('ANALYZE', 'CONVERT', 'OPTIMIZE', 'REPORT'));

ALTER TABLE processing_jobs ADD CONSTRAINT chk_job_quality
    CHECK (quality IN ('LOW', 'MEDIUM', 'HIGH'));

ALTER TABLE processing_jobs ADD CONSTRAINT chk_job_progress
    CHECK (progress >= 0 AND progress <= 100);

-- Create trigger for processing_jobs table
CREATE TRIGGER update_processing_jobs_updated_at
    BEFORE UPDATE ON processing_jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE processing_jobs IS 'Stores document processing jobs from the portal';
COMMENT ON COLUMN processing_jobs.job_id IS 'Unique job identifier (UUID)';

-- ============================================================================
-- SECTION 5: POPULATE TEST DATA FOR LOADSIMULATOR
-- ============================================================================

INSERT INTO loadsimulator (name, description, status) VALUES
    ('Test Record 1', 'Initial test data for load simulation', 'ACTIVE'),
    ('Test Record 2', 'Database timeout test record', 'ACTIVE'),
    ('Test Record 3', 'Slow query test record', 'ACTIVE'),
    ('Test Record 4', 'Connection failure test record', 'PENDING'),
    ('Test Record 5', 'Schema mismatch test record', 'ACTIVE'),
    ('Test Record 6', 'Network partition test record', 'INACTIVE'),
    ('Test Record 7', 'High load test record', 'ACTIVE'),
    ('Test Record 8', 'Concurrent access test record', 'ACTIVE'),
    ('Test Record 9', 'Transaction rollback test record', 'FAILED'),
    ('Test Record 10', 'Deadlock simulation test record', 'ACTIVE'),
    ('Test Record 11', 'Long transaction test record', 'PENDING'),
    ('Test Record 12', 'Query optimization test record', 'ACTIVE'),
    ('Test Record 13', 'Index performance test record', 'ACTIVE'),
    ('Test Record 14', 'Bulk insert test record', 'ACTIVE'),
    ('Test Record 15', 'Bulk update test record', 'ACTIVE'),
    ('Test Record 16', 'Bulk delete test record', 'INACTIVE'),
    ('Test Record 17', 'Foreign key constraint test', 'ACTIVE'),
    ('Test Record 18', 'Unique constraint test', 'ACTIVE'),
    ('Test Record 19', 'Check constraint test', 'ACTIVE'),
    ('Test Record 20', 'Trigger execution test', 'ACTIVE');

-- ============================================================================
-- SECTION 6: VERIFICATION QUERIES
-- ============================================================================

-- Count total records
SELECT COUNT(*) as total_records FROM loadsimulator;

-- Count records by status
SELECT status, COUNT(*) as count FROM loadsimulator GROUP BY status ORDER BY status;

-- Show first 5 records
SELECT id, name, status, created_at FROM loadsimulator ORDER BY id LIMIT 5;

-- Show table structure
SELECT 
    column_name, 
    data_type, 
    character_maximum_length,
    column_default,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'loadsimulator'
ORDER BY ordinal_position;

-- ============================================================================
-- SETUP COMPLETE
-- ============================================================================
-- Tables created: loadsimulator, processing_jobs
-- Test records inserted: 20 records in loadsimulator
-- Indexes created: 6 indexes total
-- Triggers created: 2 update triggers
-- ============================================================================
