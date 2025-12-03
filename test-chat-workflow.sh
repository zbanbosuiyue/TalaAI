#!/bin/bash

# Test complete chat workflow: ai-service -> origin-data-service -> HomeEvent -> Timeline
# User: test01@gmail.com / Test1234
# Baby: Aria (profileId: 253345057044692992)

set -e

BASE_URL="http://localhost:8080"  # Gateway
AUTH_URL="${BASE_URL}/api/v1/auth"
CHAT_URL="${BASE_URL}/api/v1/chat"
ORIGIN_URL="http://localhost:8089/api/v1"

echo "=== Testing Complete Workflow: Chat -> OriginEvent -> HomeEvent -> Timeline ==="
echo ""

# Step 1: Login to get JWT token
echo "Step 1: Login..."
LOGIN_RESPONSE=$(curl -s -X POST "${AUTH_URL}/login" \
    -H "Content-Type: application/json" \
    -d '{
        "email": "test01@gmail.com",
        "password": "Test1234"
    }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
USER_ID=$(echo $LOGIN_RESPONSE | jq -r '.userId')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "❌ Login failed!"
    echo $LOGIN_RESPONSE | jq .
    exit 1
fi

echo "✅ Logged in successfully"
echo "   User ID: $USER_ID"
echo "   Token: ${TOKEN:0:20}..."
echo ""

# Step 2: Send chat message
PROFILE_ID="253345057044692992"
MESSAGE="She just ate two grapes"

echo "Step 2: Sending chat message..."
echo "   Profile ID: $PROFILE_ID"
echo "   Message: $MESSAGE"
echo ""

# Note: SSE endpoint returns streaming data, we'll just check if it starts
curl -s -N -X POST "${CHAT_URL}/stream" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{
        \"profileId\": $PROFILE_ID,
        \"userId\": $USER_ID,
        \"message\": \"$MESSAGE\"
    }" &

CURL_PID=$!
sleep 5
kill $CURL_PID 2>/dev/null || true

echo ""
echo "Step 3: Checking ai-service logs for event processing..."
docker logs tala-ai-service --tail 20 | grep -E "(EventExtraction|origin-data-service|events)" || echo "No relevant logs found"

echo ""
echo "Step 4: Checking origin-data-service logs..."
docker logs tala-origin-data-service --tail 20 | grep -E "(chat-events|HomeEvent|Timeline)" || echo "No relevant logs found"

echo ""
echo "Step 5: Querying database for results..."
echo "   Checking OriginalEvent..."
docker exec -i tala-postgres psql -U tala -d tala_db << EOF
SELECT id, profile_id, source_type, ai_processed, created_at 
FROM origin_data.original_events 
WHERE profile_id = $PROFILE_ID 
ORDER BY created_at DESC 
LIMIT 3;
EOF

echo ""
echo "   Checking HomeEvent..."
docker exec -i tala-postgres psql -U tala -d tala_db << EOF
SELECT id, original_event_id, profile_id, event_type, title, created_at 
FROM origin_data.home_events 
WHERE profile_id = $PROFILE_ID 
ORDER BY created_at DESC 
LIMIT 3;
EOF

echo ""
echo "   Checking Timeline..."
docker exec -i tala-postgres psql -U tala -d tala_db << EOF
SELECT id, original_event_id, profile_id, timeline_type, title, ai_summary, created_at 
FROM origin_data.timeline_entries 
WHERE profile_id = $PROFILE_ID 
ORDER BY created_at DESC 
LIMIT 3;
EOF

echo ""
echo "=== Test Complete ==="
