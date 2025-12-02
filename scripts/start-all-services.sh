#!/bin/bash

# TalaAI - Start All Services Script
# This script builds and starts all microservices in containers

set -e

# Enable Docker BuildKit for better caching and performance
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

echo "======================================"
echo "TalaAI Backend - Starting All Services"
echo "======================================"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Change to backend directory
cd "$(dirname "$0")/.."

echo -e "${YELLOW}Step 1: Stopping existing business services...${NC}"
docker-compose -f docker-compose.services.yml down

echo -e "${YELLOW}Step 2: Building all service images...${NC}"
echo "This may take 5-10 minutes on first run..."

# Build all services (need both files for dependency resolution)
docker-compose -f docker-compose.yml -f docker-compose.services.yml build --parallel

echo -e "${GREEN}✓ All images built successfully${NC}"

echo -e "${YELLOW}Step 3: Verifying infrastructure services are running...${NC}"
if ! docker ps | grep -q postgres-dev; then
    echo "PostgreSQL not running. Please start infrastructure first with: docker-compose up -d"
    exit 1
fi

echo "Waiting for PostgreSQL to be healthy..."
until docker exec postgres-dev pg_isready -U tala > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "${GREEN}✓ PostgreSQL is ready${NC}"

echo -e "${YELLOW}Step 4: Starting all microservices...${NC}"
docker-compose -f docker-compose.services.yml up -d

echo ""
echo "======================================"
echo "Waiting for services to be healthy..."
echo "======================================"

# Wait for services to be healthy
services=("user-service:8081" "query-service:8083" "personalization-service:8084" "ai-service:8085" "reminder-service:8086" "media-service:8087" "file-service:8088" "origin-data-service:8089")

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    echo -n "Waiting for $name..."
    
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
    fi
done

echo ""
echo "======================================"
echo -e "${GREEN}All Services Started!${NC}"
echo "======================================"
echo ""
echo "Service URLs:"
echo "  Gateway Service:         http://localhost:8080"
echo "  User Service:            http://localhost:8081"
echo "  Query Service:           http://localhost:8083"
echo "  Personalization Service: http://localhost:8084"
echo "  AI Service:              http://localhost:8085"
echo "  Reminder Service:        http://localhost:8086"
echo "  Media Service:           http://localhost:8087"
echo "  File Service:            http://localhost:8088"
echo "  Origin Data Service:     http://localhost:8089"
echo ""
echo "Infrastructure:"
echo "  PostgreSQL:             localhost:5432"
echo "  Redis:                  localhost:6379"
echo ""
echo "View logs:"
echo "  docker-compose -f docker-compose.services.yml logs -f [service-name]"
echo ""
echo "Stop all business services:"
echo "  docker-compose -f docker-compose.services.yml down"
echo ""
