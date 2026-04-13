#!/bin/bash
# Azure Ubuntu VM deployment script for KTLO Simulator

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== KTLO Simulator Azure VM Deployment ===${NC}"

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    echo -e "${RED}Please run this script as a regular user with sudo privileges, not as root${NC}"
    exit 1
fi

# Update system packages
echo -e "${YELLOW}Updating system packages...${NC}"
sudo apt-get update
sudo apt-get upgrade -y

# Install Java 11 JDK
echo -e "${YELLOW}Installing Java 11 JDK...${NC}"
sudo apt-get install -y openjdk-11-jdk

# Verify Java installation
java -version
if [ $? -ne 0 ]; then
    echo -e "${RED}Java installation failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java 11 installed successfully${NC}"

# Install PostgreSQL client tools
echo -e "${YELLOW}Installing PostgreSQL client tools...${NC}"
sudo apt-get install -y postgresql-client

# Verify psql installation
psql --version
if [ $? -ne 0 ]; then
    echo -e "${RED}PostgreSQL client installation failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ PostgreSQL client installed successfully${NC}"

# Create application user (if doesn't exist)
if ! id -u ktlo-simulator &>/dev/null; then
    echo -e "${YELLOW}Creating application user...${NC}"
    sudo useradd -r -s /bin/false ktlo-simulator
    echo -e "${GREEN}✓ User ktlo-simulator created${NC}"
else
    echo -e "${GREEN}✓ User ktlo-simulator already exists${NC}"
fi

# Create application directory
echo -e "${YELLOW}Creating application directories...${NC}"
sudo mkdir -p /opt/ktlo-simulator
sudo mkdir -p /opt/ktlo-simulator/logs
sudo chown -R ktlo-simulator:ktlo-simulator /opt/ktlo-simulator
echo -e "${GREEN}✓ Directories created${NC}"

# Check if JAR file exists in /tmp
if [ ! -f "/tmp/ktlo-simulator-1.0.0.jar" ]; then
    echo -e "${RED}Error: JAR file not found at /tmp/ktlo-simulator-1.0.0.jar${NC}"
    echo "Please copy the JAR file to /tmp first:"
    echo "  scp target/ktlo-simulator-1.0.0.jar user@vm-ip:/tmp/"
    exit 1
fi

# Copy JAR file
echo -e "${YELLOW}Copying JAR file...${NC}"
sudo cp /tmp/ktlo-simulator-1.0.0.jar /opt/ktlo-simulator/
sudo chown ktlo-simulator:ktlo-simulator /opt/ktlo-simulator/ktlo-simulator-1.0.0.jar
echo -e "${GREEN}✓ JAR file copied${NC}"

# Copy application properties if exists
if [ -f "/tmp/application-prod.properties" ]; then
    echo -e "${YELLOW}Copying application-prod.properties...${NC}"
    sudo cp /tmp/application-prod.properties /opt/ktlo-simulator/
    sudo chown ktlo-simulator:ktlo-simulator /opt/ktlo-simulator/application-prod.properties
    echo -e "${GREEN}✓ Application properties copied${NC}"
fi

# Create environment file for database password
echo -e "${YELLOW}Creating environment file...${NC}"
if [ -z "$DB_PASSWORD" ]; then
    echo -e "${YELLOW}DB_PASSWORD not set, using default${NC}"
    DB_PASSWORD="agents@123"
fi

sudo bash -c "echo 'DB_PASSWORD=$DB_PASSWORD' > /opt/ktlo-simulator/.env"
sudo chown ktlo-simulator:ktlo-simulator /opt/ktlo-simulator/.env
sudo chmod 600 /opt/ktlo-simulator/.env
echo -e "${GREEN}✓ Environment file created${NC}"

# Copy systemd service file
if [ -f "/tmp/ktlo-simulator.service" ]; then
    echo -e "${YELLOW}Installing systemd service...${NC}"

    # Get VM public IP
    VM_IP=$(curl -s -H Metadata:true "http://169.254.169.254/metadata/instance/network/interface/0/ipv4/ipAddress/0/publicIpAddress?api-version=2021-02-01&format=text" 2>/dev/null || echo "localhost")

    # Update service file with VM IP
    sudo cp /tmp/ktlo-simulator.service /etc/systemd/system/
    sudo sed -i "s/<AZURE_VM_PUBLIC_IP>/$VM_IP/g" /etc/systemd/system/ktlo-simulator.service

    # Reload systemd
    sudo systemctl daemon-reload

    # Enable service
    sudo systemctl enable ktlo-simulator

    echo -e "${GREEN}✓ Systemd service installed and enabled${NC}"
else
    echo -e "${YELLOW}Warning: systemd service file not found at /tmp/ktlo-simulator.service${NC}"
    echo "Service will need to be configured manually"
fi

# Configure firewall (if ufw is active)
if sudo ufw status | grep -q "Status: active"; then
    echo -e "${YELLOW}Configuring firewall...${NC}"
    sudo ufw allow 8090/tcp
    sudo ufw allow 9010/tcp
    sudo ufw reload
    echo -e "${GREEN}✓ Firewall configured${NC}"
else
    echo -e "${YELLOW}UFW not active, skipping firewall configuration${NC}"
fi

# Start the service
if [ -f "/etc/systemd/system/ktlo-simulator.service" ]; then
    echo -e "${YELLOW}Starting KTLO Simulator service...${NC}"
    sudo systemctl start ktlo-simulator

    # Wait for service to start
    sleep 5

    # Check service status
    if sudo systemctl is-active --quiet ktlo-simulator; then
        echo -e "${GREEN}✓ Service started successfully${NC}"
        sudo systemctl status ktlo-simulator --no-pager
    else
        echo -e "${RED}✗ Service failed to start${NC}"
        echo "Check logs with: sudo journalctl -u ktlo-simulator -n 50"
        exit 1
    fi
else
    echo -e "${YELLOW}Systemd service not installed, skipping service start${NC}"
fi

# Display deployment summary
echo -e "\n${GREEN}=== Deployment Summary ===${NC}"
echo -e "Application directory: /opt/ktlo-simulator"
echo -e "JAR file: /opt/ktlo-simulator/ktlo-simulator-1.0.0.jar"
echo -e "Logs directory: /opt/ktlo-simulator/logs"
echo -e "Service name: ktlo-simulator"
echo -e "\n${YELLOW}Useful Commands:${NC}"
echo -e "  Check service status: ${GREEN}sudo systemctl status ktlo-simulator${NC}"
echo -e "  View logs: ${GREEN}sudo journalctl -u ktlo-simulator -f${NC}"
echo -e "  Restart service: ${GREEN}sudo systemctl restart ktlo-simulator${NC}"
echo -e "  Stop service: ${GREEN}sudo systemctl stop ktlo-simulator${NC}"
echo -e "  View application logs: ${GREEN}sudo tail -f /opt/ktlo-simulator/logs/application.log${NC}"
echo -e "\n${GREEN}=== Deployment completed successfully ===${NC}"
