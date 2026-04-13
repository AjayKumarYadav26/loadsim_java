# KTLO Simulation Application - Implementation Plan

## Context

This plan creates a Java-based web application to simulate and demonstrate KTLO (Keep The Lights On) activities for training and demonstration purposes. The application will enable controlled failure scenarios including:

- **Threadpool exhaustion** - Simulating high CPU load that consumes all available threads
- **Database failures** - Connection timeouts, misconfigurations, network partitions, schema changes
- **DDoS attacks** - Request flooding and rate limiting demonstrations
- **Comprehensive logging** - Separate error and access logs for monitoring

The goal is to provide a realistic, API-driven simulation platform that can demonstrate common production issues and their impacts without affecting real systems.

## Technology Stack

- **Build Tool**: Maven 3.9+
- **Framework**: Spring Boot 3.2.x
- **Java Version**: Java 17
- **Database**: Azure PostgreSQL with HikariCP connection pooling
  - Host: muliagent.postgres.database.azure.com:5432
  - Database: code_test
  - Table: loadsimulator
- **Logging**: Logback (Spring Boot default)
- **Rate Limiting**: Bucket4j
- **Server**: Embedded Tomcat (port 8090)
- **Monitoring**: JMX (Java Management Extensions) beans for metrics exposure
- **Deployment**: Local development + Azure Ubuntu VM

## Project Structure

```
loadsim_java/
├── src/
│   ├── main/
│   │   ├── java/com/ktlo/simulator/
│   │   │   ├── KtloSimulatorApplication.java
│   │   │   ├── config/
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   ├── DatabaseConfig.java
│   │   │   │   └── JmxConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── CpuLoadController.java
│   │   │   │   ├── DatabaseFailureController.java
│   │   │   │   ├── DDoSController.java
│   │   │   │   └── HealthController.java
│   │   │   ├── service/
│   │   │   │   ├── CpuLoadService.java
│   │   │   │   ├── DatabaseFailureService.java
│   │   │   │   ├── DDoSService.java
│   │   │   │   └── ThreadPoolMonitorService.java
│   │   │   ├── jmx/
│   │   │   │   ├── KtloSimulatorMXBean.java
│   │   │   │   ├── KtloSimulatorMXBeanImpl.java
│   │   │   │   ├── ThreadPoolMetricsMXBean.java
│   │   │   │   ├── ThreadPoolMetricsMXBeanImpl.java
│   │   │   │   ├── DatabaseMetricsMXBean.java
│   │   │   │   └── DatabaseMetricsMXBeanImpl.java
│   │   │   ├── filter/
│   │   │   │   └── AccessLogFilter.java
│   │   │   ├── model/
│   │   │   │   ├── SimulationRequest.java
│   │   │   │   ├── SimulationResponse.java
│   │   │   │   └── ThreadPoolStatus.java
│   │   │   ├── repository/
│   │   │   │   └── DummyDataRepository.java
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── logback-spring.xml
│   │       └── db/
│   │           ├── schema-postgres.sql
│   │           └── data-postgres.sql
│   └── test/java/com/ktlo/simulator/
│       └── (test classes mirror main structure)
├── logs/ (created at runtime)
├── deployment/
│   ├── azure-deployment.sh
│   ├── startup.sh
│   ├── db-setup.sh
│   └── systemd/ktlo-simulator.service
├── pom.xml
└── README.md
```

## Critical Files to Create

### 1. pom.xml
Maven configuration with dependencies:
- spring-boot-starter-web (REST API support)
- spring-boot-starter-data-jpa (Database operations)
- spring-boot-starter-actuator (Health monitoring)
- postgresql (PostgreSQL JDBC driver)
- bucket4j-core (Rate limiting for DDoS)
- lombok (Reduce boilerplate code)

### 2. application.properties
Core configuration:
- Server port: 8090
- Tomcat max threads: 20
- Custom threadpool: core=10, max=20, queue=100
- HikariCP settings for timeout simulation
- Azure PostgreSQL database configuration (externalized)
- Logging configuration reference
- JMX configuration

### 2a. application-dev.properties
Development environment overrides:
- Local PostgreSQL or H2 for local development
- Debug logging enabled
- JMX local access only

### 2b. application-prod.properties
Production environment overrides:
- Azure PostgreSQL connection details
- Info-level logging
- JMX remote access enabled

### 3. Database Scripts

**src/main/resources/db/schema-postgres.sql**
PostgreSQL schema creation script:
- Create `loadsimulator` table with columns:
  - id (BIGSERIAL PRIMARY KEY)
  - name (VARCHAR(255))
  - description (TEXT)
  - status (VARCHAR(50))
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)
- Create indexes for performance
- Add constraints

**src/main/resources/db/data-postgres.sql**
Data population script:
- Insert initial test data (10-20 rows)
- Sample records for testing failure scenarios

**deployment/db-setup.sh**
Automated database setup script:
- Connect to Azure PostgreSQL
- Execute schema creation
- Execute data population
- Verify table creation and data

### 4. logback-spring.xml
Logging configuration with:
- **CONSOLE appender** - Development debugging
- **ERROR_FILE appender** - ERROR level and above → error.log
- **ACCESS_FILE appender** - HTTP access logs → access.log
- Rolling policy: 10MB per file, 30 days retention
- Separate logger for ACCESS_LOG

### 5. AsyncConfig.java
Threadpool configuration:
- `@EnableAsync` annotation
- ThreadPoolTaskExecutor bean
- Core pool size: 10 threads
- Max pool size: 20 threads (requirement #1)
- Queue capacity: 100
- Rejection policy: CallerRunsPolicy
- Thread name prefix for debugging

### 6. AccessLogFilter.java
HTTP request/response logging:
- Implements servlet Filter
- Captures: timestamp, method, URI, status, response time, client IP
- Writes to dedicated ACCESS_LOG logger → access.log
- Uses MDC for request correlation ID

## Implementation Approach

### Phase 1: Threadpool & CPU Exhaustion (Requirement #1, #2)

**CpuLoadService.java**
- `@Async executeIntensiveTask(int durationSeconds)` - CPU burn for specified duration
- `@Async calculatePrimes(int limit)` - Find primes (CPU-intensive)
- `@Async calculateFibonacci(int n)` - Recursive Fibonacci
- `exhaustThreadPool()` - Submit 25 tasks to 20-thread pool to cause rejection

**CpuLoadController.java**
API endpoints:
- `POST /api/cpu/exhaust` - Exhaust all 20 threads in threadpool
- `POST /api/cpu/intensive?duration={seconds}` - Single CPU task
- `POST /api/cpu/fibonacci/{n}` - Calculate Fibonacci number
- `GET /api/cpu/status` - Current threadpool status

**ThreadPoolMonitorService.java**
- Track active threads, queue size, completed tasks
- Return ThreadPoolStatus with pool metrics
- Monitor for exhaustion state

### Phase 2: Database Failure Simulation (Requirement #3)

**DatabaseConfig.java**
Configure multiple DataSource beans:
- **Primary DataSource** - Normal Azure PostgreSQL connection
  - Read from application.properties
  - URL: jdbc:postgresql://muliagent.postgres.database.azure.com:5432/code_test
  - Username: multiagent
  - Password: externalized via environment variable or properties
- **Timeout DataSource** - Connection timeout = 100ms (very low)
- **Invalid DataSource** - Wrong JDBC URL for connection failure
- **Closed DataSource** - Programmatically closed pool

**LoadSimulatorEntity.java & LoadSimulatorRepository.java**
- JPA entity mapping to `loadsimulator` table
- Fields: id, name, description, status, createdAt, updatedAt
- `@Table(name = "loadsimulator")` annotation
- Repository with custom slow query method
- CRUD operations for testing
- Custom query with pg_sleep() for slow query simulation

**DatabaseFailureService.java**
Methods to trigger each scenario:
- `triggerTimeout()` - Execute query with 100ms timeout + pg_sleep()
- `triggerConnectionFailure()` - Close HikariCP connection pool
- `executeSlowQuery(int delaySeconds)` - Query with pg_sleep(delaySeconds)
- `triggerSchemaMismatch()` - Query non-existent table/column
- `simulateNetworkPartition()` - Use invalid hostname or firewall simulation
- `simulateAuthFailure()` - Use wrong credentials (separate DataSource)
- `testConnection()` - Validate connectivity to Azure PostgreSQL
- `recoverConnectionPool()` - Reinitialize connection pool

**DatabaseFailureController.java**
API endpoints:
- `POST /api/db/timeout` - DB timeout scenario
- `POST /api/db/connection-failure` - Connection failure
- `POST /api/db/slow-query?delay={seconds}` - Slow query
- `POST /api/db/invalid-connection` - Invalid config
- `POST /api/db/network-partition` - Network partition
- `POST /api/db/schema-change` - Schema mismatch
- `GET /api/db/test-connection` - Test connectivity
- `POST /api/db/recover` - Recover from failures

### Phase 3: Logging Configuration (Requirement #4, #5)

**logback-spring.xml structure:**
```xml
<appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/error.log</file>
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>ERROR</level>
    </filter>
    <rollingPolicy>...</rollingPolicy>
</appender>

<appender name="ACCESS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/access.log</file>
    <rollingPolicy>...</rollingPolicy>
</appender>

<logger name="ACCESS_LOG" level="INFO" additivity="false">
    <appender-ref ref="ACCESS_FILE"/>
</logger>

<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="ERROR_FILE"/>
</root>
```

**AccessLogFilter.java implementation:**
- Wrap request/response in try-finally
- Calculate response time
- Log format: `{IP} {METHOD} {URI} {STATUS} {DURATION}ms {REQUEST_ID}`
- Use SLF4J logger named "ACCESS_LOG"

### Phase 4: Web Application Setup (Requirement #6)

**KtloSimulatorApplication.java**
- `@SpringBootApplication` - Auto-configuration
- `@EnableAsync` - Enable async threadpool
- Main method with `SpringApplication.run()`

**Embedded Tomcat Configuration:**
- Max threads: 20 (aligns with custom threadpool)
- Accept count: 100
- Max connections: 200

### Phase 5: DDoS Simulation (Requirement #7)

**DDoSService.java**
- `startDDoSSimulation(int requestsPerSecond, int durationSeconds)` - Spawn threads making requests
- `stopDDoSSimulation()` - Stop ongoing simulation
- `getDDoSStatus()` - Return metrics (requests sent, success rate, rejections)
- Use RestTemplate or WebClient for internal HTTP requests

**DDoSController.java**
API endpoints:
- `POST /api/ddos/start?rps={rate}&duration={seconds}` - Start DDoS
- `POST /api/ddos/stop` - Stop DDoS
- `GET /api/ddos/status` - Get simulation status
- `GET /api/ddos/target` - Rate-limited target endpoint

**Rate Limiting with Bucket4j:**
```java
private final Bucket bucket = Bucket.builder()
    .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
    .build();

if (bucket.tryConsume(1)) {
    // Process request
} else {
    // Return 429 Too Many Requests
}
```

### Phase 6: API for All Scenarios (Requirement #8)

All failure scenarios exposed via REST endpoints:
- **CPU Exhaustion**: POST /api/cpu/exhaust
- **DB Timeout**: POST /api/db/timeout
- **DB Connection Failure**: POST /api/db/connection-failure
- **DB Slow Query**: POST /api/db/slow-query
- **DB Network Partition**: POST /api/db/network-partition
- **DB Schema Mismatch**: POST /api/db/schema-change
- **DDoS Attack**: POST /api/ddos/start

**Model Classes:**
- **SimulationRequest** - duration, intensity, parameters map
- **SimulationResponse** - status (SUCCESS/FAILURE/IN_PROGRESS), message, timestamp, details
- **ThreadPoolStatus** - activeThreads, queuedTasks, completedTasks, poolSize, isExhausted

### Phase 7: Error Handling & Monitoring

**GlobalExceptionHandler.java**
- `@ControllerAdvice` for centralized exception handling
- Handle SQLException → log to error.log, return 500
- Handle TimeoutException → return 504 Gateway Timeout
- Handle RejectedExecutionException → return 503 Service Unavailable
- Handle generic Exception → catch-all, log stack trace

**HealthController.java**
Monitoring endpoints:
- `GET /api/health` - Overall health status
- `GET /api/health/threadpool` - Threadpool metrics
- `GET /api/health/database` - DB connection status

**Spring Actuator:**
- Enable /actuator/health, /actuator/metrics, /actuator/threaddump
- Expose thread pool metrics
- Database health indicator

### Phase 8: JMX Beans Implementation

**JmxConfig.java**
- `@Configuration` class
- Enable JMX with `@EnableMBeanExport`
- Configure MBeanServer
- Register custom MBeans programmatically
- Configure JMX remote access settings

**KtloSimulatorMXBean.java (Interface)**
JMX interface for overall application metrics:
- `int getActiveThreadCount()` - Current active threads
- `int getQueuedTaskCount()` - Tasks in queue
- `long getTotalRequestCount()` - Total HTTP requests processed
- `long getErrorCount()` - Total errors logged
- `String getApplicationStatus()` - Current application status
- `void resetMetrics()` - Reset all metrics
- `Map<String, Object> getAllMetrics()` - Get all metrics as map

**KtloSimulatorMXBeanImpl.java**
- `@Component` annotation
- Implements KtloSimulatorMXBean
- `@ManagedResource` annotation with objectName
- Inject required services (ThreadPoolMonitorService, etc.)
- Implement all metric getters
- Track metrics using AtomicLong/AtomicInteger

**ThreadPoolMetricsMXBean.java (Interface)**
JMX interface for threadpool-specific metrics:
- `int getCorePoolSize()`
- `int getMaxPoolSize()`
- `int getActiveCount()`
- `int getPoolSize()`
- `long getTaskCount()`
- `long getCompletedTaskCount()`
- `int getQueueSize()`
- `int getQueueRemainingCapacity()`
- `boolean isPoolExhausted()`
- `double getThreadUtilization()` - Percentage of threads in use

**ThreadPoolMetricsMXBeanImpl.java**
- Implements ThreadPoolMetricsMXBean
- `@ManagedResource(objectName = "com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor")`
- Inject ThreadPoolTaskExecutor bean
- Expose real-time threadpool metrics
- Calculate utilization percentages

**DatabaseMetricsMXBean.java (Interface)**
JMX interface for database metrics:
- `int getActiveConnections()`
- `int getIdleConnections()`
- `int getTotalConnections()`
- `int getMaxPoolSize()`
- `long getConnectionTimeout()`
- `long getTotalQueryCount()`
- `long getFailedQueryCount()`
- `double getQueryFailureRate()`
- `boolean isDatabaseHealthy()`
- `String getLastError()`

**DatabaseMetricsMXBeanImpl.java**
- Implements DatabaseMetricsMXBean
- `@ManagedResource(objectName = "com.ktlo.simulator:type=Database,name=HikariCP")`
- Inject HikariDataSource
- Access HikariCP metrics via HikariPoolMXBean
- Track query counts and failures
- Maintain health status

**JMX ObjectName Convention:**
```
com.ktlo.simulator:type=Application,name=KtloSimulator
com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor
com.ktlo.simulator:type=Database,name=HikariCP
```

## Reusable Components

No existing code to reuse - this is a greenfield project. However, the design follows these patterns:
- **Spring Boot conventions** - Standard project structure, configuration
- **12-factor app principles** - Externalized configuration, separate logs
- **Microservices patterns** - Health endpoints, graceful degradation

## Testing & Verification

### Manual Testing

1. **Start Application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Test CPU Exhaustion:**
   ```bash
   # Exhaust threadpool
   curl -X POST http://localhost:8080/api/cpu/exhaust

   # Check threadpool status
   curl http://localhost:8080/api/cpu/status
   # Should show 20 active threads, queued tasks, isExhausted=true

   # Verify error.log contains RejectedExecutionException
   tail -f logs/error.log
   ```

3. **Test Database Timeout:**
   ```bash
   curl -X POST http://localhost:8080/api/db/timeout
   # Should return 504 or 500 with timeout error

   # Verify error.log contains SQLTimeoutException
   grep "SQLTimeoutException" logs/error.log
   ```

4. **Test Database Connection Failure:**
   ```bash
   curl -X POST http://localhost:8080/api/db/connection-failure
   # Should return 500 with connection error

   # Test recovery
   curl -X POST http://localhost:8080/api/db/recover
   curl http://localhost:8080/api/db/test-connection
   # Should return 200 OK after recovery
   ```

5. **Test DDoS Simulation:**
   ```bash
   # Start DDoS (100 requests/second for 30 seconds)
   curl -X POST "http://localhost:8080/api/ddos/start?rps=100&duration=30"

   # Check status during simulation
   curl http://localhost:8080/api/ddos/status

   # Verify access.log shows 429 responses
   grep "429" logs/access.log | wc -l
   ```

6. **Verify Logging:**
   ```bash
   # Check error.log exists and contains ERROR level logs
   ls -lh logs/error.log
   tail -20 logs/error.log

   # Check access.log contains HTTP request logs
   ls -lh logs/access.log
   tail -20 logs/access.log
   # Should show: IP METHOD URI STATUS DURATION REQUEST_ID
   ```

7. **Health Monitoring:**
   ```bash
   curl http://localhost:8080/api/health/threadpool
   curl http://localhost:8080/api/health/database
   curl http://localhost:8080/actuator/health
   ```

8. **JMX Monitoring:**
   ```bash
   # Using JConsole (GUI)
   jconsole localhost:8080
   # Navigate to MBeans tab → com.ktlo.simulator

   # Using jmxterm (CLI)
   java -jar jmxterm.jar
   open localhost:8080
   domains
   beans -d com.ktlo.simulator
   get -b com.ktlo.simulator:type=Application,name=KtloSimulator ActiveThreadCount

   # Remote JMX from another machine
   jconsole <azure-vm-ip>:9010
   ```

### Automated Tests

- **Unit Tests**: CpuLoadServiceTest, DatabaseFailureServiceTest
- **Integration Tests**: Use @SpringBootTest for controller testing
- **Concurrent Tests**: Submit multiple requests to verify threadpool behavior
- **Log Verification**: Assert log files contain expected entries

### Success Criteria

✅ Application starts on port 8080
✅ All 11+ API endpoints respond correctly
✅ CPU exhaustion causes RejectedExecutionException when >20 tasks submitted
✅ Database timeout returns error within configured timeout
✅ Database connection failures are logged to error.log
✅ DDoS simulation generates rate-limited 429 responses
✅ error.log contains only ERROR level and above
✅ access.log contains all HTTP requests with response times
✅ Health endpoints return accurate metrics
✅ Application recovers from simulated failures

## Implementation Order

1. **Foundation** (45 min)
   - Create pom.xml with PostgreSQL dependency
   - Create application.properties (all profiles)
   - Create database schema and data scripts
   - Create logback-spring.xml
   - Create main application class
   - Create database setup script

2. **Threadpool & CPU** (45 min)
   - AsyncConfig.java
   - CpuLoadService.java
   - CpuLoadController.java
   - ThreadPoolMonitorService.java
   - Model classes (ThreadPoolStatus, SimulationResponse)

3. **Database Simulation** (75 min)
   - DatabaseConfig.java (PostgreSQL configuration)
   - LoadSimulatorEntity.java & LoadSimulatorRepository.java
   - Test Azure PostgreSQL connectivity
   - DatabaseFailureService.java (PostgreSQL-specific failures)
   - DatabaseFailureController.java
   - Setup Azure PostgreSQL database

4. **Logging** (30 min)
   - AccessLogFilter.java
   - Test log file generation
   - Verify log separation

5. **DDoS Simulation** (45 min)
   - DDoSService.java
   - DDoSController.java
   - Bucket4j rate limiting configuration

6. **Monitoring & Error Handling** (30 min)
   - GlobalExceptionHandler.java
   - HealthController.java
   - Spring Actuator configuration

7. **JMX Beans** (45 min)
   - JmxConfig.java
   - KtloSimulatorMXBean & implementation
   - ThreadPoolMetricsMXBean & implementation
   - DatabaseMetricsMXBean & implementation
   - Test with JConsole/JVisualVM

8. **Deployment Setup** (60 min)
   - Create deployment scripts for Azure
   - Configure systemd service
   - Setup JMX remote access
   - Test local deployment

9. **Testing & Documentation** (45 min)
   - Write unit tests
   - Create README.md with deployment instructions
   - Manual testing of all scenarios
   - Fix any issues

**Total Estimated Time:** 7-8 hours

## Configuration Reference

### Key application.properties Settings

```properties
# Server Configuration
server.port=8090
server.tomcat.threads.max=20
server.tomcat.threads.min-spare=10
server.tomcat.accept-count=100
server.tomcat.max-connections=200

# Threadpool Configuration
async.executor.core-pool-size=10
async.executor.max-pool-size=20
async.executor.queue-capacity=100
async.executor.thread-name-prefix=ktlo-async-

# Azure PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://muliagent.postgres.database.azure.com:5432/code_test?sslmode=require
spring.datasource.username=multiagent
spring.datasource.password=${DB_PASSWORD:your_password_here}
spring.datasource.driver-class-name=org.postgresql.Driver

# HikariCP Configuration
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.pool-name=KtloHikariPool

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

# Logging Configuration
logging.config=classpath:logback-spring.xml
logging.level.root=INFO
logging.level.com.ktlo.simulator=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# JMX Configuration
spring.jmx.enabled=true
spring.jmx.default-domain=com.ktlo.simulator
management.endpoints.jmx.exposure.include=*

# Actuator Configuration
management.endpoints.web.exposure.include=health,metrics,threaddump,heapdump
management.endpoint.health.show-details=always
```

**application-dev.properties:**
```properties
# Development profile - can use local PostgreSQL or H2
server.port=8090

# Local PostgreSQL (optional)
spring.datasource.url=jdbc:postgresql://localhost:5432/code_test
spring.datasource.username=postgres
spring.datasource.password=postgres

# OR H2 for local development
# spring.datasource.url=jdbc:h2:mem:ktlodb
# spring.datasource.driver-class-name=org.h2.Driver
# spring.h2.console.enabled=true

# Debug logging
logging.level.com.ktlo.simulator=DEBUG
logging.level.org.springframework.web=DEBUG

# JMX local only
-Dcom.sun.management.jmxremote.local.only=true
```

**application-prod.properties:**
```properties
# Production profile - Azure PostgreSQL
server.port=8090

# Azure PostgreSQL
spring.datasource.url=jdbc:postgresql://muliagent.postgres.database.azure.com:5432/code_test?sslmode=require&ssl=true
spring.datasource.username=multiagent
spring.datasource.password=${DB_PASSWORD}

# Production logging
logging.level.root=INFO
logging.level.com.ktlo.simulator=INFO
logging.level.org.hibernate.SQL=WARN

# Connection pool tuning
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
```

## API Endpoint Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| /api/cpu/exhaust | POST | Exhaust all 20 threads |
| /api/cpu/intensive | POST | Single CPU task |
| /api/cpu/fibonacci/{n} | POST | Calculate Fibonacci |
| /api/cpu/status | GET | Threadpool status |
| /api/db/timeout | POST | Trigger DB timeout |
| /api/db/connection-failure | POST | DB connection failure |
| /api/db/slow-query | POST | Execute slow query |
| /api/db/network-partition | POST | Simulate network partition |
| /api/db/schema-change | POST | Schema mismatch error |
| /api/db/test-connection | GET | Test DB connectivity |
| /api/db/recover | POST | Recover from failures |
| /api/ddos/start | POST | Start DDoS simulation |
| /api/ddos/stop | POST | Stop DDoS simulation |
| /api/ddos/status | GET | DDoS simulation status |
| /api/ddos/target | GET | Rate-limited target |
| /api/health | GET | Overall health |
| /api/health/threadpool | GET | Threadpool metrics |
| /api/health/database | GET | DB status |
| /actuator/health | GET | Spring Actuator health |
| /actuator/metrics | GET | Application metrics |

## Database Setup

### Azure PostgreSQL Schema

**schema-postgres.sql:**
```sql
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

-- Create indexes
CREATE INDEX idx_loadsimulator_name ON loadsimulator(name);
CREATE INDEX idx_loadsimulator_status ON loadsimulator(status);
CREATE INDEX idx_loadsimulator_created_at ON loadsimulator(created_at);

-- Add constraints
ALTER TABLE loadsimulator ADD CONSTRAINT chk_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'FAILED'));

-- Create trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_loadsimulator_updated_at
    BEFORE UPDATE ON loadsimulator
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments
COMMENT ON TABLE loadsimulator IS 'Table for KTLO load simulation testing';
COMMENT ON COLUMN loadsimulator.status IS 'Status values: ACTIVE, INACTIVE, PENDING, FAILED';
```

**data-postgres.sql:**
```sql
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
SELECT status, COUNT(*) as count FROM loadsimulator GROUP BY status;
```

### Database Setup Script

**deployment/db-setup.sh:**
```bash
#!/bin/bash
# Azure PostgreSQL database setup script

set -e  # Exit on error

# Configuration
DB_HOST="muliagent.postgres.database.azure.com"
DB_PORT="5432"
DB_NAME="code_test"
DB_USER="multiagent"
DB_PASSWORD="${DB_PASSWORD:-}"  # Read from environment variable

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
```

**Make script executable:**
```bash
chmod +x deployment/db-setup.sh
```

**Run database setup:**
```bash
# Export password
export DB_PASSWORD="your_actual_password"

# Run setup
./deployment/db-setup.sh
```

### Azure PostgreSQL Firewall Configuration

**Add your IP to firewall rules:**
```bash
# Using Azure CLI
az postgres server firewall-rule create \
  --resource-group your-resource-group \
  --server-name muliagent \
  --name AllowMyIP \
  --start-ip-address YOUR_IP \
  --end-ip-address YOUR_IP

# Allow Azure services
az postgres server firewall-rule create \
  --resource-group your-resource-group \
  --server-name muliagent \
  --name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

**Using Azure Portal:**
1. Navigate to Azure PostgreSQL server
2. Settings → Connection security
3. Add client IP address
4. Enable "Allow access to Azure services"
5. Save changes

## Deployment

### Local Development

**Prerequisites:**
- Java 17 JDK installed
- Maven 3.9+ installed
- PostgreSQL client tools (psql)
- Azure PostgreSQL database password
- Git (optional)

**Steps:**

1. **Setup Azure PostgreSQL database:**
   ```bash
   # Export database password
   export DB_PASSWORD="your_actual_password"

   # Run database setup script
   cd c:/loadsim_java
   chmod +x deployment/db-setup.sh
   ./deployment/db-setup.sh
   ```

2. **Configure application:**
   ```bash
   # Create environment variable for DB password
   export DB_PASSWORD="your_actual_password"

   # Or edit application-dev.properties with your password
   ```

3. **Build the application:**
   ```bash
   cd c:/loadsim_java
   mvn clean package
   ```

4. **Run locally:**
   ```bash
   # Option 1: Using Maven with dev profile
   mvn spring-boot:run -Dspring-boot.run.profiles=dev

   # Option 2: Using JAR with environment variable
   export DB_PASSWORD="your_actual_password"
   java -jar target/ktlo-simulator-1.0.0.jar

   # Option 3: With JMX enabled for remote access
   java -Dcom.sun.management.jmxremote \
        -Dcom.sun.management.jmxremote.port=9010 \
        -Dcom.sun.management.jmxremote.authenticate=false \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Dcom.sun.management.jmxremote.local.only=false \
        -DDB_PASSWORD="your_actual_password" \
        -jar target/ktlo-simulator-1.0.0.jar

   # Option 4: With production profile
   java -Dspring.profiles.active=prod \
        -DDB_PASSWORD="your_actual_password" \
        -jar target/ktlo-simulator-1.0.0.jar
   ```

5. **Verify application is running:**
   ```bash
   # Check health
   curl http://localhost:8090/actuator/health

   # Test database connectivity
   curl http://localhost:8090/api/db/test-connection
   ```

6. **Connect JConsole:**
   ```bash
   jconsole localhost:9010
   ```

### Azure Ubuntu VM Deployment

**VM Prerequisites:**
- Ubuntu 22.04 LTS or later
- Minimum 2 vCPUs, 4GB RAM
- Java 17 runtime installed
- PostgreSQL client tools installed
- Ports 8090 (HTTP) and 9010 (JMX) open in NSG
- Network access to Azure PostgreSQL server
- Database password stored securely (Azure Key Vault or environment variable)

**Deployment Script: deployment/azure-deployment.sh**
```bash
#!/bin/bash
# Azure VM deployment script

# Update system
sudo apt-get update
sudo apt-get upgrade -y

# Install Java 17 and PostgreSQL client
sudo apt-get install -y openjdk-17-jdk postgresql-client

# Create application user
sudo useradd -r -s /bin/false ktlo-simulator

# Create application directory
sudo mkdir -p /opt/ktlo-simulator
sudo mkdir -p /opt/ktlo-simulator/logs
sudo chown -R ktlo-simulator:ktlo-simulator /opt/ktlo-simulator

# Copy JAR file and configuration (assumes SCP'd to /tmp)
sudo cp /tmp/ktlo-simulator-1.0.0.jar /opt/ktlo-simulator/
sudo cp /tmp/application-prod.properties /opt/ktlo-simulator/
sudo chown ktlo-simulator:ktlo-simulator /opt/ktlo-simulator/ktlo-simulator-1.0.0.jar
sudo chown ktlo-simulator:ktlo-simulator /opt/ktlo-simulator/application-prod.properties

# Set database password in environment file
sudo bash -c 'echo "DB_PASSWORD=your_actual_password" > /opt/ktlo-simulator/.env'
sudo chown ktlo-simulator:ktlo-simulator /opt/ktlo-simulator/.env
sudo chmod 600 /opt/ktlo-simulator/.env

# Install systemd service
sudo cp ktlo-simulator.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable ktlo-simulator
sudo systemctl start ktlo-simulator

# Check status
sudo systemctl status ktlo-simulator
```

**Systemd Service: deployment/systemd/ktlo-simulator.service**
```ini
[Unit]
Description=KTLO Simulator Application
After=network.target

[Service]
Type=simple
User=ktlo-simulator
WorkingDirectory=/opt/ktlo-simulator
EnvironmentFile=/opt/ktlo-simulator/.env
ExecStart=/usr/bin/java \
  -Xms512m -Xmx1024m \
  -Dspring.profiles.active=prod \
  -Dspring.config.additional-location=file:/opt/ktlo-simulator/application-prod.properties \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9010 \
  -Dcom.sun.management.jmxremote.rmi.port=9010 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Djava.rmi.server.hostname=<AZURE_VM_PUBLIC_IP> \
  -jar /opt/ktlo-simulator/ktlo-simulator-1.0.0.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

StandardOutput=append:/opt/ktlo-simulator/logs/application.log
StandardError=append:/opt/ktlo-simulator/logs/application.log

[Install]
WantedBy=multi-user.target
```

**Startup Script: deployment/startup.sh**
```bash
#!/bin/bash
# Manual startup script for testing

APP_JAR="/opt/ktlo-simulator/ktlo-simulator-1.0.0.jar"
PID_FILE="/opt/ktlo-simulator/app.pid"
LOG_DIR="/opt/ktlo-simulator/logs"

# Get Azure VM public IP
AZURE_VM_IP=$(curl -s -H Metadata:true "http://169.254.169.254/metadata/instance/network/interface/0/ipv4/ipAddress/0/publicIpAddress?api-version=2021-02-01&format=text")

java -Xms512m -Xmx1024m \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9010 \
  -Dcom.sun.management.jmxremote.rmi.port=9010 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Djava.rmi.server.hostname=${AZURE_VM_IP} \
  -jar ${APP_JAR} > ${LOG_DIR}/application.log 2>&1 &

echo $! > ${PID_FILE}
echo "Application started with PID: $(cat ${PID_FILE})"
```

**Azure VM Setup Steps:**

1. **Create and configure Azure VM:**
   ```bash
   # Using Azure CLI
   az vm create \
     --resource-group ktlo-rg \
     --name ktlo-simulator-vm \
     --image Ubuntu2204 \
     --size Standard_B2s \
     --admin-username azureuser \
     --generate-ssh-keys

   # Open ports
   az vm open-port --port 8090 --resource-group ktlo-rg --name ktlo-simulator-vm --priority 1001
   az vm open-port --port 9010 --resource-group ktlo-rg --name ktlo-simulator-vm --priority 1002
   ```

2. **Setup Azure PostgreSQL database:**
   ```bash
   # On local machine - run database setup
   export DB_PASSWORD="your_actual_password"
   ./deployment/db-setup.sh

   # Add VM IP to PostgreSQL firewall (get VM IP first)
   VM_IP=$(az vm show -d -g ktlo-rg -n ktlo-simulator-vm --query publicIps -o tsv)
   az postgres server firewall-rule create \
     --resource-group your-resource-group \
     --server-name muliagent \
     --name AllowVMIP \
     --start-ip-address $VM_IP \
     --end-ip-address $VM_IP
   ```

3. **Build and copy application:**
   ```bash
   # On local machine
   mvn clean package

   # Copy to Azure VM
   scp target/ktlo-simulator-1.0.0.jar azureuser@<VM-IP>:/tmp/
   scp src/main/resources/application-prod.properties azureuser@<VM-IP>:/tmp/
   scp deployment/azure-deployment.sh azureuser@<VM-IP>:/tmp/
   scp deployment/systemd/ktlo-simulator.service azureuser@<VM-IP>:/tmp/
   ```

4. **SSH to VM and deploy:**
   ```bash
   ssh azureuser@<VM-IP>
   cd /tmp
   chmod +x azure-deployment.sh
   sudo ./azure-deployment.sh
   ```

5. **Configure database password:**
   ```bash
   # On Azure VM
   sudo nano /opt/ktlo-simulator/.env
   # Add: DB_PASSWORD=your_actual_password

   sudo chown ktlo-simulator:ktlo-simulator /opt/ktlo-simulator/.env
   sudo chmod 600 /opt/ktlo-simulator/.env

   # Restart service
   sudo systemctl restart ktlo-simulator
   ```

6. **Verify deployment:**
   ```bash
   # Check service status
   sudo systemctl status ktlo-simulator

   # Check application logs
   sudo tail -f /opt/ktlo-simulator/logs/application.log

   # Test HTTP endpoint
   curl http://localhost:8090/actuator/health

   # Test database connectivity
   curl http://localhost:8090/api/db/test-connection

   # From local machine
   curl http://<VM-IP>:8090/actuator/health
   curl http://<VM-IP>:8090/api/db/test-connection
   ```

7. **Connect JMX remotely:**
   ```bash
   # From local machine
   jconsole <VM-IP>:9010
   ```

**JMX Remote Access Configuration:**

For secure production environments, enable JMX authentication:

**jmxremote.password:**
```
# Create /opt/ktlo-simulator/jmxremote.password
monitorRole  QED
controlRole  R&D
```

**jmxremote.access:**
```
# Create /opt/ktlo-simulator/jmxremote.access
monitorRole   readonly
controlRole   readwrite
```

Update systemd service:
```ini
-Dcom.sun.management.jmxremote.authenticate=true \
-Dcom.sun.management.jmxremote.password.file=/opt/ktlo-simulator/jmxremote.password \
-Dcom.sun.management.jmxremote.access.file=/opt/ktlo-simulator/jmxremote.access \
```

**Firewall Configuration:**
```bash
# If UFW is enabled
sudo ufw allow 8090/tcp
sudo ufw allow 9010/tcp
sudo ufw reload
```

### Database Password Management

**Option 1: Environment Variable (Development)**
```bash
export DB_PASSWORD="your_password"
java -jar ktlo-simulator-1.0.0.jar
```

**Option 2: Environment File (Production)**
```bash
# /opt/ktlo-simulator/.env
DB_PASSWORD=your_actual_password
```

**Option 3: Azure Key Vault (Recommended for Production)**
```bash
# Install Azure CLI
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# Login
az login --identity  # If using managed identity

# Retrieve password from Key Vault
DB_PASSWORD=$(az keyvault secret show --name db-password --vault-name your-vault --query value -o tsv)
export DB_PASSWORD
```

**Option 4: Application Properties Encryption (Spring Cloud Config)**
Use Jasypt or Spring Cloud Config Server for encrypted properties.

### Monitoring JMX Metrics

**Available MBeans:**

1. **com.ktlo.simulator:type=Application,name=KtloSimulator**
   - ActiveThreadCount
   - QueuedTaskCount
   - TotalRequestCount
   - ErrorCount
   - ApplicationStatus

2. **com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor**
   - CorePoolSize
   - MaxPoolSize
   - ActiveCount
   - PoolSize
   - TaskCount
   - CompletedTaskCount
   - QueueSize
   - ThreadUtilization

3. **com.ktlo.simulator:type=Database,name=HikariCP**
   - ActiveConnections
   - IdleConnections
   - TotalConnections
   - MaxPoolSize
   - TotalQueryCount
   - FailedQueryCount
   - QueryFailureRate
   - DatabaseHealthy

**JConsole Usage:**
1. Open JConsole
2. Connect to `<VM-IP>:9010`
3. Navigate to MBeans tab
4. Expand `com.ktlo.simulator`
5. Select specific MBean
6. View attributes and invoke operations

**Programmatic JMX Access:**
```java
// Example: Reading JMX metrics programmatically
MBeanServerConnection mbsc = ...;
ObjectName objectName = new ObjectName("com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor");
Integer activeCount = (Integer) mbsc.getAttribute(objectName, "ActiveCount");
```

## Notes

- **Azure PostgreSQL**: Persistent database across application restarts. Ensure proper backup strategy.
- **Database Password**: Never commit passwords to source control. Use environment variables or Azure Key Vault.
- **SSL/TLS**: Azure PostgreSQL requires SSL by default. Connection string includes `sslmode=require`.
- **Port Change**: Application runs on port 8090 (not 8080) to avoid conflicts.
- **Thread Safety**: All services are thread-safe using Spring's singleton beans.
- **Error Recovery**: Most scenarios have corresponding recovery endpoints.
- **Production Readiness**: This is a simulation tool, not production code. Includes intentional vulnerabilities.
- **Extensibility**: Easy to add more failure scenarios following existing patterns.
- **JMX Security**: Default configuration disables authentication for demo purposes. Enable authentication for production.
- **Azure Costs**: Remember to deallocate VM when not in use to save costs: `az vm deallocate --resource-group ktlo-rg --name ktlo-simulator-vm`
- **Firewall Rules**: Ensure Azure PostgreSQL firewall allows connections from:
  - Your local development machine IP
  - Azure VM IP
  - Azure services (if needed)
