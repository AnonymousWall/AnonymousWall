#!/bin/bash
# Quick start script for local testing environment
# This script sets up a local testing environment that mimics production

set -e

echo "========================================"
echo "Anonymous Wall - Local Testing Setup"
echo "Production Mimic with Oracle Database"
echo "========================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    echo "Please install Docker Desktop or Docker Engine first"
    exit 1
fi

# Check if Docker Compose is available
if ! docker compose version &> /dev/null; then
    echo -e "${RED}Error: Docker Compose is not available${NC}"
    echo "Please install Docker Compose v2"
    exit 1
fi

echo -e "${GREEN}✓${NC} Docker is available"
echo ""

# Check if .env file exists, if not create from .env.local
if [ ! -f .env ]; then
    echo -e "${YELLOW}Creating .env file from .env.local template...${NC}"
    cp .env.local .env
    echo -e "${GREEN}✓${NC} Created .env file"
    echo ""
fi

# Display configuration
echo "Configuration:"
echo "  - Database: Oracle Database XE 21c"
echo "  - Redis: Redis 7"
echo "  - Application: Micronaut with Oracle JDBC"
echo ""

# Ask user if they want to proceed
read -p "Start the local testing environment? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Setup cancelled."
    exit 0
fi

echo ""
echo -e "${YELLOW}Starting services...${NC}"
echo "This may take 2-3 minutes on first run (downloading Oracle image)"
echo ""

# Start services
docker compose -f docker-compose.local.yml up -d

echo ""
echo -e "${YELLOW}Waiting for services to be ready...${NC}"
echo ""

# Wait for Oracle to be ready
echo -n "Waiting for Oracle Database"
for _ in {1..60}; do
    if docker compose -f docker-compose.local.yml ps | grep -q "oracle-db.*healthy"; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 3
done

# Wait for Redis to be ready
echo -n "Waiting for Redis"
for _ in {1..10}; do
    if docker compose -f docker-compose.local.yml ps | grep -q "redis.*healthy"; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# Wait for application to be ready
echo -n "Waiting for Application"
for _ in {1..30}; do
    if docker compose -f docker-compose.local.yml ps | grep -q "app.*healthy"; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    if curl -s -f http://localhost:8080/health > /dev/null 2>&1; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 3
done

echo ""
echo -e "${GREEN}========================================"
echo "✓ Local Testing Environment Ready!"
echo "========================================${NC}"
echo ""
echo "Services:"
echo "  - Application:  http://localhost:8080"
echo "  - Swagger UI:   http://localhost:8080/swagger-ui.html"
echo "  - Health Check: http://localhost:8080/health"
echo "  - Oracle DB:    localhost:1521/XEPDB1 (user: system, pass: OraclePass123!)"
echo "  - Oracle EM:    https://localhost:5500/em"
echo "  - Redis:        localhost:6379"
echo ""
echo "Commands:"
echo "  View logs:      docker compose -f docker-compose.local.yml logs -f"
echo "  Stop services:  docker compose -f docker-compose.local.yml stop"
echo "  Start services: docker compose -f docker-compose.local.yml start"
echo "  Cleanup:        docker compose -f docker-compose.local.yml down -v"
echo ""
echo "Documentation:"
echo "  See LOCAL_TESTING.md for detailed instructions"
echo ""
echo -e "${YELLOW}Testing the health endpoint...${NC}"
curl -s http://localhost:8080/health | jq . || echo "Service is starting up..."
echo ""
