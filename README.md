# KTLO Simulator - Document Processing Platform

A web-based document processing and analytics platform built with Spring Boot. Behind the scenes, user interactions trigger simulation APIs to create realistic load scenarios for KTLO (Keeping The Lights On) training and demonstration.

## Overview

**What it looks like**: A professional document processing platform where users upload files and process them with various operations (Analyze, Convert, Optimize, Report).

**What it actually does**: Each user action secretly triggers system load simulations (CPU exhaustion, database failures, DDoS attacks) to create realistic failure scenarios for training purposes.

## Features

### Web Portal (User-Facing)
- **Document Upload**: Drag-and-drop file upload with validation (PDF, CSV, XLSX, TXT, max 50MB)
- **Processing Operations**: Analyze, Convert, Optimize, Report documents
- **Quality Levels**: High, Medium, Low (secretly controls simulation intensity)
- **Real-time Progress**: Live progress tracking with estimated time remaining
- **Analytics Dashboard**: Charts, statistics, and job history using Chart.js
- **Responsive Design**: Bootstrap 5 UI works on desktop and mobile

### Admin Panel (Monitoring)
- **Simulation Transparency**: Shows which user operations trigger which simulations
- **Manual Triggers**: Direct access to all simulation endpoints for testing
- **Real-time Metrics**: Thread count, memory usage, CPU info from JMX
- **Job History**: Complete view of all processing jobs with simulation details

### Simulation APIs (Backend)
- **CPU Exhaustion**: Simulate high CPU load and threadpool exhaustion
- **Database Failures**: Connection timeouts, slow queries, schema errors
- **DDoS Simulation**: Request flooding and rate limiting
- **JMX Monitoring**: Real-time metrics via JMX beans
- **Comprehensive Logging**: Separate error.log and access.log files

## Tech Stack

**Backend**:
- **Java**: 11
- **Framework**: Spring Boot 2.7.18
- **Database**: Azure PostgreSQL (code_test)
- **Build Tool**: Maven 3.9+
- **Server Port**: 8090

**Frontend**:
- **Template Engine**: Thymeleaf 3.x
- **UI Framework**: Bootstrap 5.3.2
- **JavaScript**: jQuery 3.7.1
- **Charts**: Chart.js 4.4.1 (CDN)
- **Icons**: Font Awesome 6.5.1

## Simulation Mapping

How user operations map to simulations:

| User Operation | Quality | Simulation Type | API Endpoint | Duration |
|----------------|---------|----------------|--------------|----------|
| Analyze Document | High | CPU Exhaustion | `/api/cpu/exhaust` | 120s |
| Analyze Document | Medium | CPU Intensive | `/api/cpu/intensive` | 60s |
| Analyze Document | Low | Fibonacci | `/api/cpu/fibonacci/40` | 30s |
| Convert Format | High | DB Timeout | `/api/db/timeout` | 90s |
| Convert Format | Medium | Slow Query | `/api/db/slow-query` | 50s |
| Convert Format | Low | Slow Query | `/api/db/slow-query` | 25s |
| Optimize Document | High | DDoS 100 rps | `/api/ddos/start` | 30s |
| Optimize Document | Medium | DDoS 50 rps | `/api/ddos/start` | 20s |
| Optimize Document | Low | DDoS 25 rps | `/api/ddos/start` | 15s |
| Generate Report | High | Primes 10M | `/api/cpu/primes` | 90s |
| Generate Report | Medium | Primes 5M | `/api/cpu/primes` | 45s |
| Generate Report | Low | Primes 1M | `/api/cpu/primes` | 20s |

## Quick Start

### Prerequisites

- Java 11 JDK
- Maven 3.9+
- PostgreSQL client tools (psql)
- Access to Azure PostgreSQL database

### 1. Setup Database

```bash
# Export database password
export DB_PASSWORD="your_password"

# Run database setup script
chmod +x deployment/db-setup.sh
./deployment/db-setup.sh
```

### 2. Build Application

```bash
mvn clean package
```

### 3. Run Application

```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using JAR
export DB_PASSWORD="your_password"
java -jar target/ktlo-simulator-1.0.0.jar

# Option 3: With JMX enabled
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -DDB_PASSWORD="your_password" \
     -jar target/ktlo-simulator-1.0.0.jar
```

### 4. Verify Application

```bash
# Check health
curl http://localhost:8090/actuator/health

# Test database connectivity
curl http://localhost:8090/api/db/test-connection

# Check threadpool status
curl http://localhost:8090/api/cpu/status
```

### 5. Access Web Portal

Open your browser and navigate to:

- **User Portal**: http://localhost:8090/portal
  - Upload documents
  - Monitor processing jobs
  - View analytics dashboard

- **Admin Panel**: http://localhost:8090/admin
  - Monitor active simulations in real-time
  - View which operations trigger which simulations
  - Manually trigger simulations for testing
  - View system metrics (threads, memory, CPU)

- **API Documentation**: http://localhost:8090/swagger-ui/index.html

- **Health Check**: http://localhost:8090/actuator/health

## Web Portal Usage

### For End Users

1. **Upload a Document**:
   - Navigate to `/portal/upload`
   - Drag-and-drop or browse for a file (PDF, CSV, XLSX, TXT)
   - Select an operation: Analyze, Convert, Optimize, or Report
   - Choose quality level: High (best results, longer time), Medium (balanced), or Low (fast)
   - Click "Start Processing"

2. **Monitor Progress**:
   - Automatically redirected to processing page
   - View real-time progress bar (0-100%)
   - See elapsed time and estimated time remaining
   - Cancel job if needed

3. **View Analytics**:
   - Navigate to `/portal/dashboard`
   - See total jobs, success rate, average processing time
   - View charts: jobs over time, operations breakdown, status distribution
   - Check recent jobs table

### For Administrators

1. **Monitor Simulations**:
   - Navigate to `/admin`
   - View active simulations table showing:
     - Which user operations are currently running
     - What simulation type each operation triggered
     - Actual API endpoints being called
   - Refresh automatically every 5 seconds

2. **Manual Testing**:
   - Use the manual trigger buttons to invoke simulations directly
   - Available triggers:
     - CPU: Exhaustion, Intensive, Fibonacci, Primes
     - Database: Timeout, Slow Query, Connection Failure
     - DDoS: Start, Stop
   - Useful for testing without going through the upload flow

3. **View All Jobs**:
   - Navigate to `/admin/simulations`
   - See complete history with simulation details revealed
   - Search and filter jobs

## API Endpoints

### CPU Load Simulation

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/cpu/exhaust` | POST | Exhaust threadpool with 25 tasks |
| `/api/cpu/intensive?duration=10` | POST | Single CPU-intensive task |
| `/api/cpu/fibonacci/{n}` | POST | Calculate Fibonacci number |
| `/api/cpu/primes?limit=100000` | POST | Find prime numbers |
| `/api/cpu/status` | GET | Get threadpool status |

### Database Failure Simulation

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/db/timeout` | POST | Trigger DB timeout |
| `/api/db/slow-query?delay=5` | POST | Execute slow query |
| `/api/db/schema-mismatch` | POST | Trigger schema error |
| `/api/db/connection-failure` | POST | Connection failure |
| `/api/db/auth-failure` | POST | Authentication error |
| `/api/db/test-connection` | GET | Test DB connectivity |
| `/api/db/pool-stats` | GET | Get connection pool stats |

### DDoS Simulation

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ddos/start?rps={rate}&duration={sec}` | POST | Start DDoS simulation |
| `/api/ddos/stop` | POST | Stop ongoing DDoS |
| `/api/ddos/status` | GET | Get simulation status |
| `/api/ddos/target` | GET | Rate-limited target (10 req/min) |

### Health & Monitoring

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/health` | GET | Overall health status |
| `/api/health/threadpool` | GET | Threadpool metrics |
| `/api/health/database` | GET | Database health |
| `/actuator/health` | GET | Spring Actuator health |
| `/actuator/metrics` | GET | Application metrics |

## Example Usage

### Exhaust Threadpool

```bash
curl -X POST "http://localhost:8090/api/cpu/exhaust?taskCount=25&duration=60"
```

### Trigger Database Timeout

```bash
curl -X POST http://localhost:8090/api/db/timeout
```

### Execute Slow Query

```bash
curl -X POST "http://localhost:8090/api/db/slow-query?delay=10"
```

### Start DDoS Simulation

```bash
# Start DDoS: 100 requests per second for 30 seconds
curl -X POST "http://localhost:8090/api/ddos/start?requestsPerSecond=100&durationSeconds=30"

# Check status
curl http://localhost:8090/api/ddos/status

# Stop simulation
curl -X POST http://localhost:8090/api/ddos/stop
```

### Check Health

```bash
# Overall health
curl http://localhost:8090/api/health

# Threadpool health
curl http://localhost:8090/api/health/threadpool

# Database health
curl http://localhost:8090/api/health/database
```

### Check Logs

```bash
# Error log (ERROR level and above)
tail -f logs/error.log

# Access log (HTTP requests)
tail -f logs/access.log
```

## JMX Monitoring

Connect using JConsole:

```bash
# Local connection
jconsole localhost:9010

# Remote connection (Azure VM)
jconsole <vm-ip>:9010
```

Navigate to MBeans → `com.ktlo.simulator` to view:

### Available MBeans

**1. Application Metrics** - `com.ktlo.simulator:type=Application,name=KtloSimulator`
- ActiveThreadCount - Current active threads
- QueuedTaskCount - Tasks waiting in queue
- TotalRequestCount - Total HTTP requests
- ErrorCount - Total errors logged
- ApplicationStatus - HEALTHY/DEGRADED/DOWN
- UptimeMillis - Application uptime
- UptimeFormatted - Formatted uptime string

**2. ThreadPool Metrics** - `com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor`
- CorePoolSize - Minimum threads (10)
- MaxPoolSize - Maximum threads (20)
- ActiveCount - Currently active threads
- PoolSize - Total threads created
- TaskCount - Total tasks (queued + completed)
- CompletedTaskCount - Successfully completed tasks
- QueueSize - Current queue size
- QueueRemainingCapacity - Available queue slots
- ThreadUtilization - Percentage of threads in use
- QueueUtilization - Percentage of queue used

**3. Database Metrics** - `com.ktlo.simulator:type=Database,name=HikariCP`
- ActiveConnections - Active DB connections
- IdleConnections - Idle DB connections
- TotalConnections - Total connections in pool
- MaxPoolSize - Maximum pool size
- ConnectionTimeout - Timeout in milliseconds
- TotalQueryCount - Total queries executed
- FailedQueryCount - Failed queries
- QueryFailureRate - Failure percentage
- DatabaseHealthy - Health status (true/false)
- ThreadsAwaitingConnection - Threads waiting for connection

## Configuration

Key configuration properties in `application.properties`:

```properties
# Server
server.port=8090

# Threadpool
async.executor.core-pool-size=10
async.executor.max-pool-size=20

# Database
spring.datasource.url=jdbc:postgresql://muliagent.postgres.database.azure.com:5432/code_test
spring.datasource.username=multiagent
spring.datasource.password=${DB_PASSWORD}
```

## Azure VM Deployment

### Quick Deploy to Azure Ubuntu VM

1. **Build the application locally:**
```bash
mvn clean package
```

2. **Copy files to Azure VM:**
```bash
# Copy JAR and configuration files
scp target/ktlo-simulator-1.0.0.jar azureuser@<vm-ip>:/tmp/
scp deployment/ktlo-simulator.service azureuser@<vm-ip>:/tmp/
scp deployment/azure-deployment.sh azureuser@<vm-ip>:/tmp/
```

3. **SSH to VM and run deployment:**
```bash
ssh azureuser@<vm-ip>
cd /tmp
chmod +x azure-deployment.sh
export DB_PASSWORD="agents@123"
sudo -E ./azure-deployment.sh
```

4. **Verify deployment:**
```bash
# Check service status
sudo systemctl status ktlo-simulator

# Test endpoints
curl http://<vm-ip>:8090/actuator/health
curl http://<vm-ip>:8090/api/health
```

5. **Connect JMX remotely:**
```bash
jconsole <vm-ip>:9010
```

### Manual Startup (Alternative)

```bash
# Make startup script executable
chmod +x deployment/startup.sh

# Run startup script
./deployment/startup.sh
```

For detailed deployment instructions, see [plan.md](plan.md)

## Project Structure

```
loadsim_java/
├── src/main/java/com/ktlo/simulator/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── service/         # Business logic
│   ├── repository/      # Database repositories
│   ├── model/           # Data models
│   ├── jmx/             # JMX MBeans
│   ├── filter/          # Servlet filters
│   └── exception/       # Exception handlers
├── src/main/resources/
│   ├── application.properties
│   ├── logback-spring.xml
│   └── db/              # Database scripts
└── deployment/          # Deployment scripts
    ├── db-setup.sh      # Database setup
    ├── azure-deployment.sh  # Azure VM deployment
    ├── startup.sh       # Manual startup script
    └── ktlo-simulator.service  # Systemd service
```

## License

This is a demonstration/simulation application for KTLO training purposes.
