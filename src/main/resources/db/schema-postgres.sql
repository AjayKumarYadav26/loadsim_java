-- KTLO Simulator PostgreSQL Schema
-- Drop table if exists (for clean setup)
DROP TABLE IF EXISTS loadsimulator CASCADE;

-- Create loadsimulator table
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

-- Create function for updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for updated_at
CREATE TRIGGER update_loadsimulator_updated_at
    BEFORE UPDATE ON loadsimulator
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE loadsimulator IS 'Table for KTLO load simulation testing';
COMMENT ON COLUMN loadsimulator.id IS 'Primary key - auto-incrementing ID';
COMMENT ON COLUMN loadsimulator.name IS 'Name of the test record';
COMMENT ON COLUMN loadsimulator.description IS 'Description of the test scenario';
COMMENT ON COLUMN loadsimulator.status IS 'Status values: ACTIVE, INACTIVE, PENDING, FAILED';
COMMENT ON COLUMN loadsimulator.created_at IS 'Timestamp when record was created';
COMMENT ON COLUMN loadsimulator.updated_at IS 'Timestamp when record was last updated';

-- ============================================================================
-- Processing Jobs Table for Document Processing Platform
-- ============================================================================

-- Drop table if exists
DROP TABLE IF EXISTS processing_jobs CASCADE;

-- Create processing_jobs table
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

-- Create trigger for updated_at on processing_jobs
CREATE TRIGGER update_processing_jobs_updated_at
    BEFORE UPDATE ON processing_jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE processing_jobs IS 'Stores document processing jobs from the portal';
COMMENT ON COLUMN processing_jobs.job_id IS 'Unique job identifier (UUID)';
COMMENT ON COLUMN processing_jobs.file_name IS 'Original uploaded filename';
COMMENT ON COLUMN processing_jobs.file_type IS 'File extension (PDF, CSV, XLSX, TXT)';
COMMENT ON COLUMN processing_jobs.file_size IS 'File size in bytes';
COMMENT ON COLUMN processing_jobs.file_path IS 'Server storage path for the uploaded file';
COMMENT ON COLUMN processing_jobs.operation_type IS 'Type of operation: ANALYZE, CONVERT, OPTIMIZE, REPORT';
COMMENT ON COLUMN processing_jobs.quality IS 'Processing quality: LOW, MEDIUM, HIGH';
COMMENT ON COLUMN processing_jobs.status IS 'Job status: QUEUED, UPLOADING, PROCESSING, COMPLETED, FAILED, CANCELLED';
COMMENT ON COLUMN processing_jobs.progress IS 'Processing progress percentage (0-100)';
COMMENT ON COLUMN processing_jobs.simulation_type IS 'Type of simulation triggered (for admin transparency)';
COMMENT ON COLUMN processing_jobs.simulation_endpoint IS 'API endpoint called for simulation';
COMMENT ON COLUMN processing_jobs.result IS 'Processing result or output message';
COMMENT ON COLUMN processing_jobs.error_message IS 'Error details if job failed';
COMMENT ON COLUMN processing_jobs.created_at IS 'Timestamp when job was created';
COMMENT ON COLUMN processing_jobs.updated_at IS 'Timestamp when job was last updated';
COMMENT ON COLUMN processing_jobs.started_at IS 'Timestamp when processing started';
COMMENT ON COLUMN processing_jobs.completed_at IS 'Timestamp when processing completed';
