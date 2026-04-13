# Azure VM Deployment Guide - KTLO Simulator

This guide walks you through deploying the KTLO Simulator application to an Azure Ubuntu VM.

## Prerequisites

Before starting, ensure you have:

1. **Azure VM** (Ubuntu 22.04 LTS or later)
   - Minimum 2 vCPUs, 4GB RAM
   - SSH access configured
   - Public IP address

2. **Network Security Group (NSG)** configured with:
   - Port 8090 (HTTP) - for application
   - Port 9010 (JMX) - for monitoring
   - Port 22 (SSH) - for deployment

3. **Azure PostgreSQL** firewall configured to allow:
   - Your Azure VM IP address
   - Your local development machine IP (for testing)

4. **Local Prerequisites:**
   - Java 11 installed
   - Maven installed
   - SSH client
   - Database password for Azure PostgreSQL

## Step 1: Create Azure VM (if not already created)

### Using Azure Portal:
1. Go to Azure Portal → Virtual Machines → Create
2. Configure:
   - **Image**: Ubuntu Server 22.04 LTS
   - **Size**: Standard_B2s (2 vCPUs, 4GB RAM) or larger
   - **Authentication**: SSH public key
   - **Inbound ports**: 22 (SSH), 8090 (HTTP), 9010 (JMX)
3. Review + Create

### Using Azure CLI:
```bash
# Login to Azure
az login

# Create resource group (if needed)
az group create --name ktlo-rg --location eastus

# Create VM
az vm create \
  --resource-group ktlo-rg \
  --name ktlo-simulator-vm \
  --image Ubuntu2204 \
  --size Standard_B2s \
  --admin-username azureuser \
  --generate-ssh-keys \
  --public-ip-sku Standard

# Open ports
az vm open-port --port 8090 --resource-group ktlo-rg --name ktlo-simulator-vm --priority 1001
az vm open-port --port 9010 --resource-group ktlo-rg --name ktlo-simulator-vm --priority 1002

# Get VM public IP
VM_IP=$(az vm show -d -g ktlo-rg -n ktlo-simulator-vm --query publicIps -o tsv)
echo "VM Public IP: $VM_IP"
```

## Step 2: Configure Azure PostgreSQL Firewall

Add your Azure VM IP to the PostgreSQL firewall:

```bash
# Get VM IP (if not already done)
VM_IP=$(az vm show -d -g ktlo-rg -n ktlo-simulator-vm --query publicIps -o tsv)

# Add firewall rule for VM
az postgres server firewall-rule create \
  --resource-group your-postgres-resource-group \
  --server-name muliagent \
  --name AllowKtloSimulatorVM \
  --start-ip-address $VM_IP \
  --end-ip-address $VM_IP

# Verify firewall rules
az postgres server firewall-rule list \
  --resource-group your-postgres-resource-group \
  --server-name muliagent \
  --output table
```

**Or using Azure Portal:**
1. Navigate to Azure PostgreSQL server → Connection security
2. Add client IP address → Enter VM IP
3. Save changes

## Step 3: Setup Azure PostgreSQL Database (if not already done)

Run the database setup script from your local machine:

```bash
# Navigate to project directory
cd c:/loadsim_java

# Export database password
export DB_PASSWORD="agents@123"

# Make script executable
chmod +x deployment/db-setup.sh

# Run database setup
./deployment/db-setup.sh
```

**Expected output:**
```
=== Azure PostgreSQL Database Setup ===
Testing connection to Azure PostgreSQL...
✓ Connection successful
Creating database schema...
✓ Schema created successfully
Populating data...
✓ Data populated successfully
Verifying setup...
✓ Total records: 20
```

## Step 4: Build the Application

Build the JAR file locally:

```bash
# Navigate to project directory
cd c:/loadsim_java

# Clean and build
mvn clean package -DskipTests

# Verify JAR was created
ls -lh target/ktlo-simulator-1.0.0.jar
```

**Expected output:**
```
-rw-r--r-- 1 user user 45M Mar 16 10:00 target/ktlo-simulator-1.0.0.jar
```

## Step 5: Copy Files to Azure VM

Replace `<VM-IP>` with your actual Azure VM public IP address and `<USERNAME>` with your SSH username (typically `azureuser`):

```bash
# Set variables
VM_IP="<YOUR_VM_IP>"
VM_USER="azureuser"

# Copy JAR file
scp target/ktlo-simulator-1.0.0.jar ${VM_USER}@${VM_IP}:/tmp/

# Copy deployment scripts
scp deployment/azure-deployment.sh ${VM_USER}@${VM_IP}:/tmp/
scp deployment/startup.sh ${VM_USER}@${VM_IP}:/tmp/
scp deployment/ktlo-simulator.service ${VM_USER}@${VM_IP}:/tmp/

# Copy application properties (optional - uses embedded properties if not provided)
scp src/main/resources/application-prod.properties ${VM_USER}@${VM_IP}:/tmp/

# Make scripts executable
ssh ${VM_USER}@${VM_IP} "chmod +x /tmp/azure-deployment.sh /tmp/startup.sh"
```

**Example:**
```bash
VM_IP="20.185.123.45"
VM_USER="azureuser"

scp target/ktlo-simulator-1.0.0.jar ${VM_USER}@${VM_IP}:/tmp/
scp deployment/*.sh ${VM_USER}@${VM_IP}:/tmp/
scp deployment/ktlo-simulator.service ${VM_USER}@${VM_IP}:/tmp/
```

## Step 6: SSH to Azure VM and Deploy

```bash
# SSH to VM
ssh ${VM_USER}@${VM_IP}

# Verify files are copied
ls -lh /tmp/ktlo-simulator-1.0.0.jar
ls -lh /tmp/azure-deployment.sh

# Export database password
export DB_PASSWORD="agents@123"

# Run deployment script
cd /tmp
sudo -E ./azure-deployment.sh
```

**The deployment script will:**
1. Update system packages
2. Install Java 11 JDK
3. Install PostgreSQL client tools
4. Create `ktlo-simulator` system user
5. Create `/opt/ktlo-simulator` directory
6. Copy JAR file and configuration
7. Create environment file with database password
8. Install systemd service
9. Configure firewall (if UFW is active)
10. Start the application

**Expected output:**
```
=== KTLO Simulator Azure VM Deployment ===
Updating system packages...
Installing Java 11 JDK...
✓ Java 11 installed successfully
Installing PostgreSQL client tools...
✓ PostgreSQL client installed successfully
Creating application user...
✓ User ktlo-simulator created
Creating application directories...
✓ Directories created
Copying JAR file...
✓ JAR file copied
Creating environment file...
✓ Environment file created
Installing systemd service...
✓ Systemd service installed and enabled
Starting KTLO Simulator service...
✓ Service started successfully
```

## Step 7: Verify Deployment

### Check Service Status

```bash
# Check systemd service status
sudo systemctl status ktlo-simulator

# View real-time logs
sudo journalctl -u ktlo-simulator -f

# View application logs
sudo tail -f /opt/ktlo-simulator/logs/application.log
```

### Test Health Endpoint (from VM)

```bash
# Test health endpoint
curl http://localhost:8090/actuator/health

# Expected response:
# {"status":"UP"}

# Test API documentation
curl http://localhost:8090/

# Test database connectivity
curl http://localhost:8090/api/db/test-connection
```

### Test from Your Local Machine

```bash
# Replace <VM-IP> with your Azure VM public IP
VM_IP="<YOUR_VM_IP>"

# Test health
curl http://${VM_IP}:8090/actuator/health

# Open in browser
# http://<VM-IP>:8090/
# http://<VM-IP>:8090/swagger-ui/index.html
```

## Step 8: Test All Endpoints

### CPU Load Simulation

```bash
VM_IP="<YOUR_VM_IP>"

# Exhaust threadpool
curl -X POST "http://${VM_IP}:8090/api/cpu/exhaust?taskCount=25&duration=60"

# Check threadpool status
curl http://${VM_IP}:8090/api/cpu/status

# CPU intensive task
curl -X POST "http://${VM_IP}:8090/api/cpu/intensive?duration=10"
```

### Database Failure Simulation

```bash
# Trigger DB timeout
curl -X POST http://${VM_IP}:8090/api/db/timeout

# Slow query
curl -X POST "http://${VM_IP}:8090/api/db/slow-query?delay=5"

# Test connection
curl http://${VM_IP}:8090/api/db/test-connection

# Pool statistics
curl http://${VM_IP}:8090/api/db/pool-stats
```

### DDoS Simulation

```bash
# Start DDoS simulation (50 req/sec for 30 seconds)
curl -X POST "http://${VM_IP}:8090/api/ddos/start?requestsPerSecond=50&durationSeconds=30"

# Check status
curl http://${VM_IP}:8090/api/ddos/status

# Stop DDoS
curl -X POST http://${VM_IP}:8090/api/ddos/stop

# Test rate-limited endpoint
curl http://${VM_IP}:8090/api/ddos/target
```

### Health Monitoring

```bash
# Overall health
curl http://${VM_IP}:8090/api/health

# Threadpool health
curl http://${VM_IP}:8090/api/health/threadpool

# Database health
curl http://${VM_IP}:8090/api/health/database

# Spring Actuator endpoints
curl http://${VM_IP}:8090/actuator/health
curl http://${VM_IP}:8090/actuator/metrics
```

## Step 9: Connect JMX for Monitoring

### Using JConsole (GUI)

```bash
# From your local machine
jconsole <VM-IP>:9010
```

**Steps in JConsole:**
1. Open JConsole
2. Select "Remote Process"
3. Enter: `<VM-IP>:9010`
4. Click "Connect" (no authentication required for POC)
5. Navigate to MBeans tab
6. Expand `com.ktlo.simulator`

**Available MBeans:**
- `com.ktlo.simulator:type=Application,name=KtloSimulator`
  - Attributes: ActiveThreadCount, QueuedTaskCount, TotalRequestCount, ErrorCount, ApplicationStatus, UptimeMillis, UptimeFormatted
  - Operations: resetMetrics()

- `com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor`
  - Attributes: CorePoolSize, MaxPoolSize, ActiveCount, PoolSize, TaskCount, CompletedTaskCount, QueueSize, ThreadUtilization

- `com.ktlo.simulator:type=Database,name=HikariCP`
  - Attributes: ActiveConnections, IdleConnections, TotalConnections, TotalQueryCount, FailedQueryCount, QueryFailureRate, DatabaseHealthy

### Using jmxterm (CLI)

```bash
# Download jmxterm
wget https://github.com/jiaqi/jmxterm/releases/download/v1.0.2/jmxterm-1.0.2-uber.jar

# Connect to JMX
java -jar jmxterm-1.0.2-uber.jar

# In jmxterm shell:
open <VM-IP>:9010
domains
beans -d com.ktlo.simulator
get -b com.ktlo.simulator:type=Application,name=KtloSimulator ActiveThreadCount
```

## Step 10: View Logs

### Application Logs

```bash
# SSH to VM
ssh ${VM_USER}@${VM_IP}

# View application logs (stdout/stderr)
sudo tail -f /opt/ktlo-simulator/logs/application.log

# View error logs (ERROR level and above)
sudo tail -f /opt/ktlo-simulator/logs/error.log

# View HTTP access logs
sudo tail -f /opt/ktlo-simulator/logs/access.log

# View systemd service logs
sudo journalctl -u ktlo-simulator -n 100
sudo journalctl -u ktlo-simulator -f
```

### Log Locations

| Log File | Location | Content |
|----------|----------|---------|
| Application Log | `/opt/ktlo-simulator/logs/application.log` | All application output (INFO and above) |
| Error Log | `/opt/ktlo-simulator/logs/error.log` | ERROR level logs only |
| Access Log | `/opt/ktlo-simulator/logs/access.log` | HTTP request/response logs |
| Systemd Journal | `journalctl -u ktlo-simulator` | Systemd service logs |

## Troubleshooting

### Service Failed to Start

```bash
# Check service status
sudo systemctl status ktlo-simulator

# View recent logs
sudo journalctl -u ktlo-simulator -n 50

# Check application log
sudo tail -100 /opt/ktlo-simulator/logs/application.log

# Common issues:
# 1. Port 8090 already in use
sudo netstat -tulpn | grep 8090

# 2. Database connection failure
# Verify PostgreSQL firewall allows VM IP
# Check database credentials in /opt/ktlo-simulator/.env

# 3. Java not found
java -version
```

### Database Connection Issues

```bash
# Test PostgreSQL connection from VM
psql -h muliagent.postgres.database.azure.com \
     -p 5432 \
     -U multiagent \
     -d code_test \
     -c "SELECT version();"

# Check firewall rules
az postgres server firewall-rule list \
  --resource-group your-resource-group \
  --server-name muliagent \
  --output table

# Verify database password
sudo cat /opt/ktlo-simulator/.env
```

### Port Not Accessible

```bash
# Check if port is listening
sudo netstat -tulpn | grep 8090

# Check firewall (UFW)
sudo ufw status

# Check Azure NSG rules
az network nsg rule list \
  --resource-group ktlo-rg \
  --nsg-name ktlo-simulator-vmNSG \
  --output table
```

### JMX Connection Issues

```bash
# Verify JMX port is listening
sudo netstat -tulpn | grep 9010

# Check java.rmi.server.hostname
ps aux | grep ktlo-simulator

# Ensure NSG allows port 9010
az vm open-port --port 9010 --resource-group ktlo-rg --name ktlo-simulator-vm --priority 1002
```

## Useful Management Commands

### Service Management

```bash
# Start service
sudo systemctl start ktlo-simulator

# Stop service
sudo systemctl stop ktlo-simulator

# Restart service
sudo systemctl restart ktlo-simulator

# Enable on boot
sudo systemctl enable ktlo-simulator

# Disable on boot
sudo systemctl disable ktlo-simulator

# View service status
sudo systemctl status ktlo-simulator
```

### Update Application

```bash
# 1. Build new JAR locally
mvn clean package

# 2. Copy to VM
scp target/ktlo-simulator-1.0.0.jar ${VM_USER}@${VM_IP}:/tmp/

# 3. SSH to VM and update
ssh ${VM_USER}@${VM_IP}
sudo systemctl stop ktlo-simulator
sudo cp /tmp/ktlo-simulator-1.0.0.jar /opt/ktlo-simulator/
sudo chown ktlo-simulator:ktlo-simulator /opt/ktlo-simulator/ktlo-simulator-1.0.0.jar
sudo systemctl start ktlo-simulator

# 4. Verify
sudo systemctl status ktlo-simulator
curl http://localhost:8090/actuator/health
```

### Change Database Password

```bash
# SSH to VM
ssh ${VM_USER}@${VM_IP}

# Update password
sudo nano /opt/ktlo-simulator/.env
# Change DB_PASSWORD=new_password

# Restart service
sudo systemctl restart ktlo-simulator
```

### View Resource Usage

```bash
# CPU and memory usage
top -p $(pgrep -f ktlo-simulator)

# Disk usage
du -sh /opt/ktlo-simulator/logs

# Network connections
sudo netstat -an | grep :8090
```

## Cleanup / Uninstall

```bash
# SSH to VM
ssh ${VM_USER}@${VM_IP}

# Stop and disable service
sudo systemctl stop ktlo-simulator
sudo systemctl disable ktlo-simulator

# Remove service file
sudo rm /etc/systemd/system/ktlo-simulator.service
sudo systemctl daemon-reload

# Remove application directory
sudo rm -rf /opt/ktlo-simulator

# Remove user
sudo userdel ktlo-simulator

# Optional: Remove Java if not needed
sudo apt-get remove -y openjdk-11-jdk
```

## Production Hardening (Optional)

For production environments, consider these security enhancements:

### 1. Enable JMX Authentication

```bash
# Create password file
sudo bash -c 'cat > /opt/ktlo-simulator/jmxremote.password <<EOF
monitorRole  QED
controlRole  R&D
EOF'

# Create access file
sudo bash -c 'cat > /opt/ktlo-simulator/jmxremote.access <<EOF
monitorRole   readonly
controlRole   readwrite
EOF'

# Set permissions
sudo chmod 600 /opt/ktlo-simulator/jmxremote.password
sudo chown ktlo-simulator:ktlo-simulator /opt/ktlo-simulator/jmxremote.*

# Update service file
sudo nano /etc/systemd/system/ktlo-simulator.service
# Change:
#   -Dcom.sun.management.jmxremote.authenticate=true
#   -Dcom.sun.management.jmxremote.password.file=/opt/ktlo-simulator/jmxremote.password
#   -Dcom.sun.management.jmxremote.access.file=/opt/ktlo-simulator/jmxremote.access

# Reload and restart
sudo systemctl daemon-reload
sudo systemctl restart ktlo-simulator
```

### 2. Use Azure Key Vault for Secrets

```bash
# Install Azure CLI
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# Login (requires managed identity or service principal)
az login --identity

# Retrieve password
DB_PASSWORD=$(az keyvault secret show --name db-password --vault-name your-vault --query value -o tsv)
```

### 3. Enable HTTPS (Reverse Proxy)

Install nginx as reverse proxy with SSL:

```bash
sudo apt-get install -y nginx certbot python3-certbot-nginx

# Configure nginx
sudo bash -c 'cat > /etc/nginx/sites-available/ktlo-simulator <<EOF
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8090;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
EOF'

sudo ln -s /etc/nginx/sites-available/ktlo-simulator /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# Get SSL certificate
sudo certbot --nginx -d your-domain.com
```

## Summary

You now have the KTLO Simulator deployed on Azure VM with:

- ✅ Application running on port 8090
- ✅ JMX monitoring on port 9010
- ✅ Connected to Azure PostgreSQL database
- ✅ Systemd service for auto-restart
- ✅ Comprehensive logging (application, error, access)
- ✅ 21 API endpoints for simulation
- ✅ Swagger UI for interactive documentation
- ✅ 3 JMX MBeans with 28+ metrics

**Access URLs:**
- Application: http://&lt;VM-IP&gt;:8090/
- Swagger UI: http://&lt;VM-IP&gt;:8090/swagger-ui/index.html
- Health: http://&lt;VM-IP&gt;:8090/actuator/health
- JMX: &lt;VM-IP&gt;:9010

**Next Steps:**
1. Test all simulation scenarios
2. Monitor with JConsole
3. Review logs for any issues
4. Share the VM IP with your team for testing
