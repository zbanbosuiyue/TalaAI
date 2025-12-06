#!/usr/bin/env bash

# TalaAI - Restart Specific Services Script
# Usage: ./restart-services.sh [service1] [service2] ...
# Example: ./restart-services.sh origin-data-service ai-service
# If no services specified, restarts all services

set -e

# Enable Docker BuildKit for better caching and performance
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Change to backend directory
cd "$(dirname "$0")/.."

# All available services
ALL_SERVICES=(
    "user-service"
    "reminder-service"
    "media-service"
    "file-service"
    "query-service"
    "ai-service"
    "origin-data-service"
    "personalization-service"
    "gateway-service"
)

# Function to get port for a service
get_service_port() {
    case "$1" in
        "user-service") echo "8081" ;;
        "query-service") echo "8083" ;;
        "personalization-service") echo "8084" ;;
        "ai-service") echo "8085" ;;
        "reminder-service") echo "8086" ;;
        "media-service") echo "8087" ;;
        "file-service") echo "8088" ;;
        "origin-data-service") echo "8089" ;;
        "gateway-service") echo "8080" ;;
        *) echo "" ;;
    esac
}

# Determine which services to restart
if [ $# -eq 0 ]; then
    echo -e "${BLUE}No services specified. Restarting all services...${NC}"
    SERVICES_TO_RESTART=("${ALL_SERVICES[@]}")
else
    SERVICES_TO_RESTART=("$@")
    echo -e "${BLUE}Restarting specified services: ${SERVICES_TO_RESTART[*]}${NC}"
fi

# Convert service names to container names (tala-service-name)
CONTAINER_NAMES=()
for service in "${SERVICES_TO_RESTART[@]}"; do
    CONTAINER_NAMES+=("tala-$service")
done

echo "======================================"
echo "TalaAI Backend - Restart Services"
echo "======================================"

echo -e "${YELLOW}Step 1: Stopping specified services...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.services.yml stop "${SERVICES_TO_RESTART[@]}" || true

echo -e "${YELLOW}Step 2: Removing stopped containers...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.services.yml rm -f "${SERVICES_TO_RESTART[@]}" || true

echo -e "${YELLOW}Step 3: Building service images...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.services.yml build "${SERVICES_TO_RESTART[@]}"

echo -e "${GREEN}✓ Images built successfully${NC}"

echo -e "${YELLOW}Step 4: Verifying infrastructure services are running...${NC}"
if ! docker ps | grep -q postgres-dev; then
    echo -e "${RED}PostgreSQL not running. Please start infrastructure first with: docker-compose up -d${NC}"
    exit 1
fi

echo "Waiting for PostgreSQL to be healthy..."
until docker exec postgres-dev pg_isready -U tala > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "${GREEN}✓ PostgreSQL is ready${NC}"

echo -e "${YELLOW}Step 5: Starting specified services...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.services.yml up -d "${SERVICES_TO_RESTART[@]}"

echo ""
echo "======================================"
echo "Waiting for services to be healthy..."
echo "======================================"

# Wait for services to be healthy
for service in "${SERVICES_TO_RESTART[@]}"; do
    port=$(get_service_port "$service")
    
    if [ -z "$port" ]; then
        echo -e "${YELLOW}⚠ No health check configured for $service${NC}"
        continue
    fi
    
    echo -n "Waiting for $service..."
    
    max_attempts=60
    attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -sf http://localhost:$port/actuator/health > /dev/null 2>&1; then
            echo -e " ${GREEN}✓ UP${NC}"
            break
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    if [ $attempt -eq $max_attempts ]; then
        echo -e " ${RED}✗ TIMEOUT${NC}"
        echo -e "${YELLOW}Check logs with: docker logs tala-$service${NC}"
    fi
done

echo ""
echo "======================================"
echo -e "${GREEN}Services Restarted!${NC}"
echo "======================================"
echo ""
echo "View logs:"
echo "  docker logs -f tala-[service-name]"
echo ""
echo "Examples:"
echo "  docker logs -f tala-origin-data-service"
echo "  docker logs -f tala-ai-service"
echo ""
