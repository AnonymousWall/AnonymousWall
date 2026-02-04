#!/bin/bash
# Stop script for local testing environment

set -e

echo "========================================"
echo "Anonymous Wall - Stop Local Testing"
echo "========================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Ask what to do
echo "What would you like to do?"
echo ""
echo "1) Stop services (preserve data)"
echo "2) Stop and remove containers (preserve data volumes)"
echo "3) Stop and remove everything (including data)"
echo "4) Cancel"
echo ""
read -p "Choose option (1-4): " -n 1 -r
echo ""

case $REPLY in
    1)
        echo -e "${YELLOW}Stopping services...${NC}"
        docker compose -f docker-compose.local.yml stop
        echo -e "${GREEN}✓${NC} Services stopped (data preserved)"
        echo ""
        echo "To start again: docker compose -f docker-compose.local.yml start"
        echo "Or run: ./start-local-test.sh"
        ;;
    2)
        echo -e "${YELLOW}Stopping and removing containers...${NC}"
        docker compose -f docker-compose.local.yml down
        echo -e "${GREEN}✓${NC} Containers removed (data volumes preserved)"
        echo ""
        echo "To start again: docker compose -f docker-compose.local.yml up -d"
        echo "Or run: ./start-local-test.sh"
        ;;
    3)
        echo -e "${RED}WARNING: This will delete all data (database and Redis)${NC}"
        read -p "Are you sure? (y/n) " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}Stopping and removing everything...${NC}"
            docker compose -f docker-compose.local.yml down -v
            echo -e "${GREEN}✓${NC} Everything removed"
            echo ""
            echo "To start fresh: docker compose -f docker-compose.local.yml up -d"
            echo "Or run: ./start-local-test.sh"
        else
            echo "Cancelled."
        fi
        ;;
    4)
        echo "Cancelled."
        ;;
    *)
        echo -e "${RED}Invalid option${NC}"
        exit 1
        ;;
esac

echo ""
