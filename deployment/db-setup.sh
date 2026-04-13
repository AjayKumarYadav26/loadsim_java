#!/bin/bash
# Azure PostgreSQL database setup script

set -e  # Exit on error

# Configuration
DB_HOST="muliagent.postgres.database.azure.com"
DB_PORT="5432"
DB_NAME="code_test"
DB_USER="multiagent"
DB_PASSWORD="${DB_PASSWORD:-agents@123}"  # Read from environment variable or use default

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA_FILE="${SCRIPT_DIR}/../src/main/resources/db/schema-postgres.sql"
DATA_FILE="${SCRIPT_DIR}/../src/main/resources/db/data-postgres.sql"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Azure PostgreSQL Database Setup ===${NC}"

# Check if password is provided
if [ -z "$DB_PASSWORD" ]; then
    echo -e "${RED}Error: DB_PASSWORD environment variable not set${NC}"
    echo "Usage: DB_PASSWORD=your_password ./db-setup.sh"
    exit 1
fi

# Check if psql is installed
if ! command -v psql &> /dev/null; then
    echo -e "${RED}Error: psql command not found${NC}"
    echo "Please install PostgreSQL client tools:"
    echo "  Ubuntu: sudo apt-get install postgresql-client"
    echo "  macOS: brew install postgresql"
    echo "  Windows: Download from https://www.postgresql.org/download/windows/"
    exit 1
fi

# Check if SQL files exist
if [ ! -f "$SCHEMA_FILE" ]; then
    echo -e "${RED}Error: Schema file not found: $SCHEMA_FILE${NC}"
    exit 1
fi

if [ ! -f "$DATA_FILE" ]; then
    echo -e "${RED}Error: Data file not found: $DATA_FILE${NC}"
    exit 1
fi

# Test connection
echo -e "${YELLOW}Testing connection to Azure PostgreSQL...${NC}"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT version();" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Connection successful${NC}"
else
    echo -e "${RED}✗ Connection failed${NC}"
    echo "Please check:"
    echo "  - Database credentials"
    echo "  - Firewall rules (Azure PostgreSQL allows your IP)"
    echo "  - SSL/TLS settings"
    exit 1
fi

# Execute schema
echo -e "${YELLOW}Creating database schema...${NC}"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f $SCHEMA_FILE
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Schema created successfully${NC}"
else
    echo -e "${RED}✗ Schema creation failed${NC}"
    exit 1
fi

# Execute data population
echo -e "${YELLOW}Populating data...${NC}"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f $DATA_FILE
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Data populated successfully${NC}"
else
    echo -e "${RED}✗ Data population failed${NC}"
    exit 1
fi

# Verify setup
echo -e "${YELLOW}Verifying setup...${NC}"
RECORD_COUNT=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM loadsimulator;")
echo -e "${GREEN}✓ Total records: $RECORD_COUNT${NC}"

# Show table structure
echo -e "${YELLOW}Table structure:${NC}"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "\d loadsimulator"

# Show sample data
echo -e "${YELLOW}Sample data (first 5 records):${NC}"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT id, name, status, created_at FROM loadsimulator LIMIT 5;"

echo -e "${GREEN}=== Database setup completed successfully ===${NC}"
