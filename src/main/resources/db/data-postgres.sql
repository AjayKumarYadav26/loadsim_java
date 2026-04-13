-- KTLO Simulator Test Data Population
-- Truncate table before inserting (for re-runs)
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

-- Verify insertion
SELECT COUNT(*) as total_records FROM loadsimulator;
SELECT status, COUNT(*) as count FROM loadsimulator GROUP BY status ORDER BY status;

-- Show sample data
SELECT id, name, status, created_at FROM loadsimulator ORDER BY id LIMIT 5;
