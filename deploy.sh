#!/bin/bash
# Deployment script for OCI instances

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
    print_warning "Running as root. This script should be run as the 'opc' user with sudo."
fi

# Check if .env file exists
if [ ! -f "${APP_DIR}/.env" ]; then
    print_error ".env file not found at ${APP_DIR}/.env"
    print_info "Please create a .env file with required environment variables:"
    echo ""
    echo "JWT_GENERATOR_SIGNATURE_SECRET=your-secret-key-min-32-chars"
    echo "DATABASE_URL=jdbc:mysql://your-adb-host:3306/anonymous_wall"
    echo "DATABASE_USER=your-db-user"
    echo "DATABASE_PASSWORD=your-db-password"
    echo "REDIS_URI=redis://your-redis-host:6379"
    echo ""
    exit 1
fi

# Create log directory if it doesn't exist
print_info "Creating log directory..."
mkdir -p ${APP_DIR}/logs
chmod 755 ${APP_DIR}/logs

# Stop existing containers
print_info "Stopping existing containers..."
cd ${APP_DIR}
docker-compose -f ${COMPOSE_FILE} down || true

# Pull or build the Docker image
if [ -f "${APP_DIR}/Dockerfile" ]; then
    print_info "Building Docker image from source..."
    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ${APP_DIR}
else
    print_warning "Dockerfile not found. Assuming image is available in registry..."
    # If you have a container registry, pull from there
    # docker pull your-registry/${IMAGE_NAME}:${IMAGE_TAG}
fi

# Start the application
print_info "Starting application with docker-compose..."
docker-compose -f ${COMPOSE_FILE} up -d

# Wait for application to be healthy
print_info "Waiting for application to be healthy..."
MAX_RETRIES=30
RETRY_COUNT=0

# First, ensure container is running
while [ $RETRY_COUNT -lt 10 ]; do
    if docker ps | grep -q anonymouswall-backend; then
        print_info "Container is running"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
    sleep 2
done

if [ $RETRY_COUNT -eq 10 ]; then
    print_error "Container failed to start"
    docker logs anonymouswall-backend --tail 50
    exit 1
fi

# Determine which HTTP client is available in the container (check once)
print_info "Checking available health check tools..."
if docker exec anonymouswall-backend sh -c "command -v curl > /dev/null 2>&1"; then
    HEALTH_CHECK_CMD="curl -f http://localhost:8080/health"
    print_info "Using curl for health checks"
elif docker exec anonymouswall-backend sh -c "command -v wget > /dev/null 2>&1"; then
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
        if docker exec anonymouswall-backend $HEALTH_CHECK_CMD > /dev/null 2>&1; then
            print_info "Application is healthy!"
            break
        fi
    else
        # No HTTP client available, just check if container is still running
        if docker ps | grep -q anonymouswall-backend; then
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
    docker logs anonymouswall-backend --tail 50
    exit 1
fi

# Show status
print_info "Deployment completed successfully!"
print_info "Application is running on port 8080"
print_info ""
print_info "Useful commands:"
echo "  - View logs:       docker logs -f anonymouswall-backend"
echo "  - Check status:    docker-compose -f ${COMPOSE_FILE} ps"
echo "  - Stop app:        docker-compose -f ${COMPOSE_FILE} down"
echo "  - Restart app:     docker-compose -f ${COMPOSE_FILE} restart"
echo "  - Health check:    curl http://localhost:8080/health"
