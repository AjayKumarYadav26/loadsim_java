# Manual Database Setup Guide

## Connection Information
- **Host**: muliagent.postgres.database.azure.com
- **Port**: 5432
- **Database**: code_test
- **User**: multiagent
- **Password**: agents@123

## Method 1: Using psql Command Line

### Step 1: Connect to Database
```bash
psql -h muliagent.postgres.database.azure.com -p 5432 -U multiagent -d code_test
```
Enter password when prompted: `agents@123`

### Step 2: Run Schema File
```sql
\i C:/loadsim_java/src/main/resources/db/schema-postgres.sql
```

### Step 3: Run Data File
```sql
\i C:/loadsim_java/src/main/resources/db/data-postgres.sql
```

### Step 4: Verify
```sql
SELECT COUNT(*) FROM loadsimulator;
SELECT * FROM loadsimulator LIMIT 5;
```

---

## Method 2: Copy-Paste SQL Commands Directly

### Step 1: Connect to database (as above)

### Step 2: Copy and paste Schema SQL

```sql
-- Drop existing table
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

-- Create indexes
CREATE INDEX idx_loadsimulator_name ON loadsimulator(name);
CREATE INDEX idx_loadsimulator_status ON loadsimulator(status);
CREATE INDEX idx_loadsimulator_created_at ON loadsimulator(created_at);

-- Add constraints
ALTER TABLE loadsimulator ADD CONSTRAINT chk_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'FAILED'));

-- Create trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger
CREATE TRIGGER update_loadsimulator_updated_at
    BEFORE UPDATE ON loadsimulator
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### Step 3: Copy and paste Data SQL

```sql
-- Clear existing data
TRUNCATE TABLE loadsimulator RESTART IDENTITY CASCADE;

-- Insert test data
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
```

### Step 4: Verify Setup

```sql
-- Count records
SELECT COUNT(*) as total_records FROM loadsimulator;

-- View by status
SELECT status, COUNT(*) as count FROM loadsimulator GROUP BY status ORDER BY status;

-- View sample data
SELECT id, name, status, created_at FROM loadsimulator ORDER BY id LIMIT 5;

-- Check table structure
\d loadsimulator
```

---

## Method 3: Using Azure Portal Query Editor

1. Go to Azure Portal → PostgreSQL server → Query editor
2. Connect with credentials
3. Copy-paste the SQL commands from Method 2 above
4. Execute each block separately

---

## Method 4: Using DBeaver / pgAdmin

1. Create new connection with details above
2. Open SQL Editor
3. Copy-paste schema SQL → Execute
4. Copy-paste data SQL → Execute
5. Refresh and verify

---

## Processing Jobs Table (Optional)

If you also need the `processing_jobs` table, run this after the above:

```sql
DROP TABLE IF EXISTS processing_jobs CASCADE;

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

CREATE INDEX idx_job_status ON processing_jobs(status);
CREATE INDEX idx_job_created ON processing_jobs(created_at);
CREATE INDEX idx_job_operation ON processing_jobs(operation_type);

ALTER TABLE processing_jobs ADD CONSTRAINT chk_job_status
    CHECK (status IN ('QUEUED', 'UPLOADING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'));

ALTER TABLE processing_jobs ADD CONSTRAINT chk_job_operation
    CHECK (operation_type IN ('ANALYZE', 'CONVERT', 'OPTIMIZE', 'REPORT'));

ALTER TABLE processing_jobs ADD CONSTRAINT chk_job_quality
    CHECK (quality IN ('LOW', 'MEDIUM', 'HIGH'));

ALTER TABLE processing_jobs ADD CONSTRAINT chk_job_progress
    CHECK (progress >= 0 AND progress <= 100);

CREATE TRIGGER update_processing_jobs_updated_at
    BEFORE UPDATE ON processing_jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

---

## Troubleshooting

**Connection Issues:**
- Check Azure PostgreSQL firewall rules (allow your IP)
- Verify SSL settings (Azure requires SSL by default)
- Test with: `psql "sslmode=require host=muliagent.postgres.database.azure.com port=5432 user=multiagent dbname=code_test"`

**Permission Issues:**
- Ensure user has CREATE, INSERT, UPDATE privileges
- Check if you're connected to the correct database

**Windows psql Path:**
- Add PostgreSQL bin to PATH: `C:\Program Files\PostgreSQL\XX\bin`
- Or use full path: `"C:\Program Files\PostgreSQL\15\bin\psql.exe" -h ...`
