#!/bin/bash
# Deployment script for OCI instances
# Uses Podman and podman-compose (pre-installed via cloud-init)

set -e

echo "=========================================="
echo "AnonymousWall Backend Deployment Script"
echo "=========================================="

# Configuration
APP_DIR="/opt/anonymouswall"
IMAGE_NAME="anonymouswall-backend"
IMAGE_TAG="${IMAGE_TAG:-latest}"
COMPOSE_FILE="docker-compose.prod.yml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running as root or with sudo
if [ "$EUID" -eq 0 ]; then 
    print_warning "Running as root. This script should be run as the 'opc' user."
fi

# Check if Podman is installed (pre-installed via cloud-init)
if ! command -v podman &> /dev/null; then
    print_error "Podman is not installed!"
    print_info "Podman should be pre-installed via cloud-init. If not, install it:"
    echo ""
    echo "  sudo dnf install -y podman"
    echo "  pip3 install podman-compose==1.0.6"
    echo "  sudo systemctl enable --now podman.socket"
    echo ""
    print_info "After installing Podman, run this script again."
    exit 1
fi

# Check if podman-compose is installed
if ! command -v podman-compose &> /dev/null; then
    print_error "podman-compose is not installed!"
    print_info "Install podman-compose:"
    echo ""
    echo "  pip3 install podman-compose==1.0.6"
    echo ""
    print_info "After installing podman-compose, run this script again."
    exit 1
fi

COMPOSE_CMD="podman-compose"
print_info "Using Podman with podman-compose"

# Check if .env file exists
if [ ! -f "${APP_DIR}/.env" ]; then
    print_error ".env file not found at ${APP_DIR}/.env"
    print_info "Please create a .env file with required environment variables:"
    echo ""
    echo "JWT_GENERATOR_SIGNATURE_SECRET=your-secret-key-min-32-chars"
    echo "DATABASE_URL=jdbc:oracle:thin:@anonwalldb_high?TNS_ADMIN=/app/wallet"
    echo "DATABASE_USER=ADMIN"
    echo "DATABASE_PASSWORD=your-db-password"
    echo "# REDIS_URI=redis://localhost:6379  # Optional, defaults to localhost:6379"
    echo ""
    echo "NOTE: OCI Autonomous Database uses Oracle Database with mTLS."
    echo "You need to set up the wallet:"
    echo "  1. Extract wallet: terraform output -raw adb_wallet_content | base64 -d > wallet.zip"
    echo "  2. Create wallet directory: mkdir -p ${APP_DIR}/wallet"
    echo "  3. Unzip: unzip wallet.zip -d ${APP_DIR}/wallet"
    echo "  4. The TNS alias (e.g., anonwalldb_high) is in wallet/tnsnames.ora"
    echo ""
    exit 1
fi

# Check if wallet directory exists (required for mTLS connections to ADB)
if [ ! -d "${APP_DIR}/wallet" ]; then
    print_error "Wallet directory not found at ${APP_DIR}/wallet"
    print_info "ADB requires a wallet for mTLS connections."
    print_info "To set up the wallet:"
    echo "  1. From your local machine: terraform output -raw adb_wallet_content | base64 -d > wallet.zip"
    echo "  2. Transfer wallet.zip to this instance"
    echo "  3. mkdir -p ${APP_DIR}/wallet && unzip wallet.zip -d ${APP_DIR}/wallet"
    echo ""
    exit 1
fi

# Create log directory if it doesn't exist
# The directory must be writable by the container user (UID 1001 - micronaut user)
print_info "Creating log directory..."
sudo mkdir -p ${APP_DIR}/logs
sudo chmod 777 ${APP_DIR}/logs

# Stop existing containers
print_info "Stopping existing containers..."
cd ${APP_DIR}
${COMPOSE_CMD} -f ${COMPOSE_FILE} down || true

# Pull or build the container image
if [ -f "${APP_DIR}/Dockerfile" ]; then
    print_info "Building container image from source..."
    podman build -t ${IMAGE_NAME}:${IMAGE_TAG} ${APP_DIR}
else
    print_warning "Dockerfile not found. Assuming image is available in registry..."
    # If you have a container registry, pull from there
    # podman pull your-registry/${IMAGE_NAME}:${IMAGE_TAG}
fi

# Start the application
print_info "Starting application with podman-compose..."
${COMPOSE_CMD} -f ${COMPOSE_FILE} up -d

# Wait for application to be healthy
print_info "Waiting for application to be healthy..."
MAX_RETRIES=30
RETRY_COUNT=0

# First, ensure container is running
while [ $RETRY_COUNT -lt 10 ]; do
    if podman ps | grep -q anonymouswall-backend; then
        print_info "Container is running"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
    sleep 2
done

if [ $RETRY_COUNT -eq 10 ]; then
    print_error "Container failed to start"
    podman logs anonymouswall-backend --tail 50
    exit 1
fi

# Determine which HTTP client is available in the container (check once)
print_info "Checking available health check tools..."
if podman exec anonymouswall-backend sh -c "command -v curl > /dev/null 2>&1"; then
    HEALTH_CHECK_CMD="curl -f http://localhost:8080/health"
    print_info "Using curl for health checks"
elif podman exec anonymouswall-backend sh -c "command -v wget > /dev/null 2>&1"; then
    HEALTH_CHECK_CMD="wget -q -O - http://localhost:8080/health"
    print_info "Using wget for health checks"
else
    print_warning "Neither curl nor wget found in container, will check container status only"
    HEALTH_CHECK_CMD=""
fi

# Reset counter for health check
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if [ -n "$HEALTH_CHECK_CMD" ]; then
        if podman exec anonymouswall-backend $HEALTH_CHECK_CMD > /dev/null 2>&1; then
            print_info "Application is healthy!"
            break
        fi
    else
        # No HTTP client available, just check if container is still running
        if podman ps | grep -q anonymouswall-backend; then
            print_info "Container is running, assuming healthy"
            break
        fi
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
    sleep 2
done

echo ""

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    print_error "Application failed to become healthy after ${MAX_RETRIES} retries"
    print_info "Checking logs..."
    podman logs anonymouswall-backend --tail 50
    exit 1
fi

# Show status
print_info "Deployment completed successfully!"
print_info "Application is running on port 8080"
print_info ""
print_info "Useful commands:"
echo "  - View logs:       podman logs -f anonymouswall-backend"
echo "  - Check status:    ${COMPOSE_CMD} -f ${COMPOSE_FILE} ps"
echo "  - Stop app:        ${COMPOSE_CMD} -f ${COMPOSE_FILE} down"
echo "  - Restart app:     ${COMPOSE_CMD} -f ${COMPOSE_FILE} restart"
echo "  - Health check:    curl http://localhost:8080/health"
