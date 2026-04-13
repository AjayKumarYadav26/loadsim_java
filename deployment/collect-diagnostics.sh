#!/bin/bash
# KTLO Simulator Diagnostics Collection Script
# Collects thread dumps and logs for troubleshooting

set -e

# Configuration
BASE_DIR="/home/manish/ktlo_java_sim"
THREADDUMP_DIR="${BASE_DIR}/threaddump"
TOP_DIR="${BASE_DIR}/top"
LOG_DIR="${BASE_DIR}/logs"
THREAD_DUMP_COUNT=10
THREAD_DUMP_INTERVAL=3  # seconds between dumps

echo "=========================================="
echo "KTLO Simulator Diagnostics Collection"
echo "=========================================="
echo ""

# Step 1: Delete existing threaddump folder
if [ -d "${THREADDUMP_DIR}" ]; then
    echo "[1/4] Removing existing threaddump folder..."
    rm -rf "${THREADDUMP_DIR}"
    echo "      Removed: ${THREADDUMP_DIR}"
else
    echo "[1/4] No existing threaddump folder found"
fi

# Delete existing top folder
if [ -d "${TOP_DIR}" ]; then
    echo "      Removing existing top folder..."
    rm -rf "${TOP_DIR}"
    echo "      Removed: ${TOP_DIR}"
fi

# Step 2: Create fresh threaddump and top directories
echo "[2/4] Creating threaddump and top directories..."
mkdir -p "${THREADDUMP_DIR}"
mkdir -p "${TOP_DIR}"
echo "      Created: ${THREADDUMP_DIR}"
echo "      Created: ${TOP_DIR}"
echo ""

# Step 3: Collect thread dumps and top output
echo "[3/4] Collecting ${THREAD_DUMP_COUNT} thread dumps and top snapshots..."
echo "      Interval: ${THREAD_DUMP_INTERVAL} seconds"
echo ""

# Find Java process PID
JAVA_PID=$(pgrep -f "ktlo-simulator-1.0.0.jar" | head -1)

if [ -z "${JAVA_PID}" ]; then
    echo "ERROR: Java application is not running!"
    echo "Please start the ktlo-java-simulator service first."
    exit 1
fi

echo "      Found Java process: PID ${JAVA_PID}"
echo ""

# Collect thread dumps
for i in $(seq 1 ${THREAD_DUMP_COUNT}); do
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    DUMP_FILE="${THREADDUMP_DIR}/threaddump_${i}_${TIMESTAMP}.txt"
    TOP_FILE="${TOP_DIR}/top_${i}_${TIMESTAMP}.txt"
    
    echo "      Capturing thread dump ${i}/${THREAD_DUMP_COUNT}..."
    
    # Capture top output (batch mode, 1 iteration, show threads)
    top -b -n 1 -H -p ${JAVA_PID} > "${TOP_FILE}" 2>&1
    
    # Use jstack if available, otherwise use kill -3
    if command -v jstack &> /dev/null; then
        jstack -l ${JAVA_PID} > "${DUMP_FILE}" 2>&1
    else
        # Alternative: use kill -3 (requires redirecting stdout)
        echo "Thread dump captured at ${TIMESTAMP}" > "${DUMP_FILE}"
        kill -3 ${JAVA_PID}
        # Note: Thread dump will appear in application logs
        echo "WARNING: jstack not found, using kill -3 (check logs for output)" >> "${DUMP_FILE}"
    fi
    
    # Add process stats to dump file
    echo "" >> "${DUMP_FILE}"
    echo "========== Process Information ==========" >> "${DUMP_FILE}"
    ps -p ${JAVA_PID} -o pid,ppid,cmd,%mem,%cpu,etime >> "${DUMP_FILE}"
    
    # Don't sleep after last dump
    if [ ${i} -lt ${THREAD_DUMP_COUNT} ]; then
        sleep ${THREAD_DUMP_INTERVAL}
    fi
done

echo ""
echo "      Collected ${THREAD_DUMP_COUNT} thread dumps successfully"
echo "      Collected ${THREAD_DUMP_COUNT} top snapshots successfully"
echo ""

# Print folder paths for agent to download
echo "=========================================="
echo "Diagnostics collection completed!"
echo "=========================================="
echo ""
echo "LOGS_FOLDER_PATH=${LOG_DIR}"
echo "THREADDUMP_FOLDER_PATH=${THREADDUMP_DIR}"
echo "TOP_FOLDER_PATH=${TOP_DIR}"
echo ""
echo "Contents:"
echo "  - ${THREAD_DUMP_COUNT} thread dumps in ${THREADDUMP_DIR}"
echo "  - ${THREAD_DUMP_COUNT} top snapshots in ${TOP_DIR}"
echo "  - Application logs in ${LOG_DIR}"
echo ""

