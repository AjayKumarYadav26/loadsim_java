#!/bin/bash
# Manual startup script for KTLO Simulator
# Use this for testing or if systemd service is not configured

set -e  # Exit on error

# Configuration
APP_DIR="/opt/ktlo-simulator"
APP_JAR="${APP_DIR}/ktlo-simulator-1.0.0.jar"
PID_FILE="${APP_DIR}/app.pid"
LOG_DIR="${APP_DIR}/logs"
ENV_FILE="${APP_DIR}/.env"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running on Azure VM
if curl -s -H Metadata:true "http://169.254.169.254/metadata/instance?api-version=2021-02-01" > /dev/null 2>&1; then
    AZURE_VM_IP=$(curl -s -H Metadata:true "http://169.254.169.254/metadata/instance/network/interface/0/ipv4/ipAddress/0/publicIpAddress?api-version=2021-02-01&format=text")
    echo -e "${GREEN}Running on Azure VM with IP: $AZURE_VM_IP${NC}"
else
    AZURE_VM_IP="localhost"
    echo -e "${YELLOW}Not running on Azure VM, using localhost${NC}"
fi

# Load environment variables
if [ -f "$ENV_FILE" ]; then
    echo -e "${GREEN}Loading environment variables from $ENV_FILE${NC}"
    source "$ENV_FILE"
else
    echo -e "${YELLOW}Warning: Environment file not found at $ENV_FILE${NC}"
    echo -e "${YELLOW}Using default DB_PASSWORD${NC}"
    export DB_PASSWORD="${DB_PASSWORD:-agents@123}"
fi

# Check if JAR exists
if [ ! -f "$APP_JAR" ]; then
    echo -e "${RED}Error: JAR file not found at $APP_JAR${NC}"
    exit 1
fi

# Create logs directory if doesn't exist
mkdir -p "$LOG_DIR"

# Check if already running
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p "$OLD_PID" > /dev/null 2>&1; then
        echo -e "${YELLOW}Application is already running with PID: $OLD_PID${NC}"
        echo -e "Use ${GREEN}kill $OLD_PID${NC} to stop it first"
        exit 1
    else
        echo -e "${YELLOW}Removing stale PID file${NC}"
        rm -f "$PID_FILE"
    fi
fi

# Start the application
echo -e "${GREEN}Starting KTLO Simulator...${NC}"
echo -e "JAR: $APP_JAR"
echo -e "Logs: $LOG_DIR/application.log"
echo -e "JMX: $AZURE_VM_IP:9010"

nohup java \
    -Xms512m \
    -Xmx1024m \
    -Dspring.profiles.active=prod \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=9010 \
    -Dcom.sun.management.jmxremote.rmi.port=9010 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.local.only=false \
    -Djava.rmi.server.hostname="${AZURE_VM_IP}" \
    -jar "${APP_JAR}" \
    > "${LOG_DIR}/application.log" 2>&1 &

APP_PID=$!
echo $APP_PID > "$PID_FILE"

echo -e "${GREEN}✓ Application started with PID: $APP_PID${NC}"
echo -e "\n${YELLOW}Waiting for application to start...${NC}"

# Wait for application to start (check for 30 seconds)
for i in {1..30}; do
    if curl -s http://localhost:8090/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Application is healthy and ready${NC}"
        break
    fi
    sleep 1
    echo -n "."
done

echo ""

# Display status
if curl -s http://localhost:8090/actuator/health > /dev/null 2>&1; then
    echo -e "\n${GREEN}=== Application Started Successfully ===${NC}"
    echo -e "PID: $APP_PID"
    echo -e "HTTP: http://$AZURE_VM_IP:8090"
    echo -e "Health: http://$AZURE_VM_IP:8090/actuator/health"
    echo -e "JMX: $AZURE_VM_IP:9010"
    echo -e "\n${YELLOW}Useful Commands:${NC}"
    echo -e "  View logs: ${GREEN}tail -f $LOG_DIR/application.log${NC}"
    echo -e "  View error logs: ${GREEN}tail -f $LOG_DIR/error.log${NC}"
    echo -e "  View access logs: ${GREEN}tail -f $LOG_DIR/access.log${NC}"
    echo -e "  Stop application: ${GREEN}kill $APP_PID${NC} or ${GREEN}kill \$(cat $PID_FILE)${NC}"
    echo -e "  Check status: ${GREEN}curl http://localhost:8090/actuator/health${NC}"
else
    echo -e "${RED}✗ Application may have failed to start${NC}"
    echo -e "Check logs at: ${GREEN}$LOG_DIR/application.log${NC}"
    echo -e "Last 20 lines:"
    tail -20 "$LOG_DIR/application.log"
fi
