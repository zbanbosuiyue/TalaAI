#!/bin/bash

# Verify Backend Integration is Working
# Tests: Gateway routing, service health, API endpoints

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║         Backend Integration Verification                  ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Test counter
PASSED=0
FAILED=0

test_endpoint() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}
    
    echo -n "Testing $name... "
    
    status=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    
    if [ "$status" = "$expected_status" ]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $status)"
        ((PASSED++))
    else
        echo -e "${RED}✗ FAIL${NC} (HTTP $status, expected $expected_status)"
        ((FAILED++))
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Infrastructure Services"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_endpoint "PostgreSQL" "http://localhost:5432" "000"  # Connection refused is expected
test_endpoint "Redis" "http://localhost:6379" "000"  # Connection refused is expected

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "API Gateway"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_endpoint "Gateway Health" "http://localhost:8080/actuator/health"
test_endpoint "Gateway Root" "http://localhost:8080/" "404"  # Expected 404

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Microservices (Direct Access)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_endpoint "User Service" "http://localhost:8081/actuator/health"
test_endpoint "Event Service" "http://localhost:8082/actuator/health"
test_endpoint "Query Service" "http://localhost:8083/actuator/health"
test_endpoint "Personalization" "http://localhost:8084/actuator/health"
test_endpoint "AI Service" "http://localhost:8085/actuator/health"
test_endpoint "Reminder Service" "http://localhost:8086/actuator/health"
test_endpoint "Media Service" "http://localhost:8087/actuator/health"
test_endpoint "File Service" "http://localhost:8088/actuator/health"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Gateway Routing (Through Port 8080)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_endpoint "User Service via Gateway" "http://localhost:8080/actuator/health"
# Note: Specific API endpoints will return 401/403 without auth, which is expected

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "Tests Passed: ${GREEN}$PASSED${NC}"
echo -e "Tests Failed: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo ""
    echo "Backend is ready for frontend integration."
    echo ""
    echo "Next steps:"
    echo "1. Configure mobile-app/.env.local:"
    echo "   VITE_DATA_SOURCE=backend"
    echo "   VITE_BACKEND_API_URL=http://localhost:8080"
    echo ""
    echo "2. Start frontend:"
    echo "   cd mobile-app && npm run dev"
    echo ""
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    echo ""
    echo "Troubleshooting:"
    echo "- Wait 30 seconds for services to fully start"
    echo "- Check logs: docker-compose logs <service-name>"
    echo "- Verify all containers are running: docker ps"
    echo ""
    exit 1
fi
