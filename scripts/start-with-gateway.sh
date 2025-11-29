#!/bin/bash

# Start Tala Backend with API Gateway
# This script starts all backend services plus the Nginx API Gateway

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$BACKEND_DIR"

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     Starting Tala Backend with API Gateway                ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop."
    exit 1
fi

echo "✓ Docker is running"
echo ""

# Start infrastructure + services + gateway
echo "Starting services..."
docker-compose \
    -f docker-compose.yml \
    -f docker-compose.services.yml \
    -f docker-compose.gateway.yml \
    up -d

echo ""
echo "Waiting for services to be healthy..."
sleep 10

# Check service health
echo ""
echo "Service Status:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_service() {
    local name=$1
    local url=$2
    
    if curl -s -f "$url" > /dev/null 2>&1; then
        echo "✅ $name - $url"
    else
        echo "❌ $name - $url (not ready)"
    fi
}

check_service "API Gateway      " "http://localhost:8080/health"
check_service "User Service     " "http://localhost:8081/actuator/health"
check_service "Event Service    " "http://localhost:8082/actuator/health"
check_service "Query Service    " "http://localhost:8083/actuator/health"
check_service "Personalization  " "http://localhost:8084/actuator/health"
check_service "AI Service       " "http://localhost:8085/actuator/health"
check_service "Reminder Service " "http://localhost:8086/actuator/health"
check_service "Media Service    " "http://localhost:8087/actuator/health"
check_service "File Service     " "http://localhost:8088/actuator/health"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test gateway routing
echo "Testing API Gateway Routing:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_route() {
    local name=$1
    local path=$2
    
    if curl -s -f "http://localhost:8080$path" > /dev/null 2>&1; then
        echo "✅ $name → $path"
    else
        echo "⚠️  $name → $path (endpoint may not exist yet)"
    fi
}

test_route "User Service     " "/api/v1/auth/health"
test_route "Event Service    " "/api/v1/events/health"
test_route "Query Service    " "/api/v1/analytics/health"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "╔════════════════════════════════════════════════════════════╗"
echo "║                 Backend Started Successfully!              ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "API Gateway:  http://localhost:8080"
echo "Health Check: http://localhost:8080/health"
echo ""
echo "Frontend should use: VITE_BACKEND_API_URL=http://localhost:8080"
echo ""
echo "To stop all services:"
echo "  docker-compose -f docker-compose.yml -f docker-compose.services.yml -f docker-compose.gateway.yml down"
echo ""
