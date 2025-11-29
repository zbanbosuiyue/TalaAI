#!/bin/bash

echo "=========================================="
echo "TalaAI Backend - Build All Services"
echo "=========================================="

cd "$(dirname "$0")/services"

SERVICES=(
    "gateway-service"
    "event-service"
    "query-service"
    "ai-service"
    "user-service"
    "reminder-service"
    "media-service"
    "personalization-service"
)

SUCCESS_COUNT=0
FAIL_COUNT=0
FAILED_SERVICES=()

for service in "${SERVICES[@]}"; do
    echo ""
    echo "Building $service..."
    echo "----------------------------------------"
    
    if [ -d "$service" ]; then
        cd "$service"
        mvn clean package -DskipTests > /dev/null 2>&1
        
        if [ $? -eq 0 ]; then
            echo "✅ $service - BUILD SUCCESS"
            ((SUCCESS_COUNT++))
        else
            echo "❌ $service - BUILD FAILED"
            ((FAIL_COUNT++))
            FAILED_SERVICES+=("$service")
        fi
        
        cd ..
    else
        echo "⚠️  $service - Directory not found"
    fi
done

echo ""
echo "=========================================="
echo "Build Summary"
echo "=========================================="
echo "Total Services: ${#SERVICES[@]}"
echo "✅ Success: $SUCCESS_COUNT"
echo "❌ Failed: $FAIL_COUNT"

if [ $FAIL_COUNT -gt 0 ]; then
    echo ""
    echo "Failed Services:"
    for failed in "${FAILED_SERVICES[@]}"; do
        echo "  - $failed"
    done
    exit 1
else
    echo ""
    echo "🎉 All services built successfully!"
    echo ""
    echo "Generated JARs:"
    find . -name "*.jar" -path "*/target/*" ! -name "*original*" -type f | while read jar; do
        size=$(du -h "$jar" | cut -f1)
        echo "  - $jar ($size)"
    done
    exit 0
fi
