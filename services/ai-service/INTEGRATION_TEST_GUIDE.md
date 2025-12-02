# AI Service Integration Test Guide

## Overview
This guide provides step-by-step instructions for testing the iOS frontend chat integration with the backend AI service using SSE streaming and file uploads.

## Prerequisites
- Backend services running (ai-service, file-service, gateway-service, user-service)
- iOS frontend app running
- Valid user authentication token
- MinIO storage service running (for file uploads)

## Test Scenarios

### 1. Chat with SSE Streaming

#### Test Case 1.1: Basic Text Message
**Endpoint:** `POST /api/v1/chat/stream`

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "profileId": 1,
    "userId": 1,
    "message": "Baby drank 120ml formula at 2pm"
  }'
```

**Expected Response:** SSE stream with events:
```
event: thinking
data: {"stage":"initialization","message":"Processing your message..."}

event: thinking
data: {"stage":"memory_retrieval","message":"Retrieving conversation context..."}

event: thinking
data: {"stage":"ai_processing","message":"Analyzing your message with AI..."}

event: classification
data: {"interactionType":"EVENT_LOGGING","confidence":0.95,"reason":"User is logging a feeding event"}

event: extraction
data: {"aiMessage":"Got it! I've recorded that baby had 120ml of formula at 2pm.","intentUnderstanding":"Log feeding event","confidence":0.95,"eventsCount":1}

event: event
data: {"eventType":"FEEDING","eventCategory":"NUTRITION","timestamp":"2024-12-01T14:00:00Z","summary":"Formula feeding: 120ml","confidence":0.95,"eventData":{"amount":120,"unit":"ml","type":"formula"}}

event: storage
data: {"success":true,"message":"Events stored successfully","eventsCount":1}

event: complete
data: {"success":true,"message":"Got it! I've recorded that baby had 120ml of formula at 2pm."}
```

**Frontend Validation:**
- User message appears in chat
- Typing indicator shows during processing
- AI response appears after completion
- No errors displayed

---

#### Test Case 1.2: Question/Answer
**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "profileId": 1,
    "userId": 1,
    "message": "Why does my baby wake up at night?"
  }'
```

**Expected Response:**
- Classification: `QUESTION_ANSWER`
- AI provides helpful parenting advice
- No events extracted (question only)

---

### 2. File Upload Tests

#### Test Case 2.1: Upload Image
**Endpoint:** `POST /api/v1/files/upload`

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/files/upload \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@test-image.jpg" \
  -F "userId=1" \
  -F "profileId=1"
```

**Expected Response:**
```json
{
  "id": 123,
  "userId": 1,
  "profileId": 1,
  "originalFilename": "test-image.jpg",
  "storageKey": "uuid_test-image.jpg",
  "fileType": "IMAGE",
  "mimeType": "image/jpeg",
  "fileSize": 1024000,
  "publicUrl": "http://localhost:9000/tala-files/uuid_test-image.jpg",
  "thumbnailUrl": "http://localhost:9000/tala-files/uuid_test-image_thumb.jpg",
  "width": 1920,
  "height": 1080,
  "createdAt": "2024-12-01T10:00:00Z",
  "updatedAt": "2024-12-01T10:00:00Z"
}
```

**Frontend Validation:**
- Upload progress bar shows 0-100%
- File preview appears with thumbnail
- File can be removed before sending
- Success message after upload

---

#### Test Case 2.2: Upload with Invalid File Type
**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/files/upload \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@malicious.exe" \
  -F "userId=1"
```

**Expected Response:**
```json
{
  "error": "File type not allowed"
}
```
**HTTP Status:** 400 Bad Request

**Frontend Validation:**
- Error message displayed
- File not uploaded
- User can retry with valid file

---

#### Test Case 2.3: Upload File Too Large
**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/files/upload \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@large-file.mp4" \
  -F "userId=1"
```

**Expected Response:**
```json
{
  "error": "File size exceeds maximum allowed size of 100 MB"
}
```
**HTTP Status:** 400 Bad Request

---

### 3. Chat with Attachments

#### Test Case 3.1: Send Message with Image
**Request:**
```bash
# First upload file
FILE_RESPONSE=$(curl -X POST http://localhost:8080/api/v1/files/upload \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@baby-photo.jpg" \
  -F "userId=1" \
  -F "profileId=1")

FILE_URL=$(echo $FILE_RESPONSE | jq -r '.publicUrl')

# Then send chat with attachment
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d "{
    \"profileId\": 1,
    \"userId\": 1,
    \"message\": \"Look at this photo from today!\",
    \"attachmentUrls\": [\"$FILE_URL\"]
  }"
```

**Expected Response:**
- AI analyzes image content
- Provides contextual response about the photo
- Events extracted if relevant (e.g., activity, mood)

**Frontend Validation:**
- Uploaded file shows in message
- Image thumbnail displays
- AI responds to both text and image

---

### 4. Error Handling Tests

#### Test Case 4.1: Unauthorized Request
**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "profileId": 1,
    "userId": 1,
    "message": "Test message"
  }'
```

**Expected Response:**
```json
{
  "error": "Unauthorized",
  "message": "Missing or invalid Authorization header"
}
```
**HTTP Status:** 401 Unauthorized

**Frontend Validation:**
- Error message displayed
- User prompted to log in
- Session cleared if token expired

---

#### Test Case 4.2: Network Timeout
**Simulation:** Stop backend service mid-stream

**Expected Frontend Behavior:**
- Connection error detected
- Error message: "Connection lost. Please try again."
- User can retry message
- Previous messages preserved

---

#### Test Case 4.3: Invalid Profile ID
**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "profileId": 99999,
    "userId": 1,
    "message": "Test message"
  }'
```

**Expected Response:**
- Error event in SSE stream
- Appropriate error message

---

### 5. Security Tests

#### Test Case 5.1: JWT Token Validation
**Request:** Use expired token
```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer EXPIRED_TOKEN" \
  -d '{
    "profileId": 1,
    "userId": 1,
    "message": "Test"
  }'
```

**Expected Response:** 401 Unauthorized

---

#### Test Case 5.2: Cross-User Access
**Request:** Try to access another user's profile
```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer USER1_TOKEN" \
  -d '{
    "profileId": 999,
    "userId": 2,
    "message": "Test"
  }'
```

**Expected Response:** 403 Forbidden or appropriate error

---

#### Test Case 5.3: SQL Injection Attempt
**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "profileId": 1,
    "userId": 1,
    "message": "Test'; DROP TABLE users; --"
  }'
```

**Expected Response:**
- Message processed safely
- No database corruption
- AI responds normally

---

## Frontend Integration Tests

### Test 1: Complete Chat Flow
1. Open iOS app
2. Navigate to Chat tab
3. Type message: "Baby had lunch at noon"
4. Press send
5. **Verify:**
   - Message appears in chat
   - Typing indicator shows
   - AI response appears
   - No errors

### Test 2: File Upload Flow
1. Open Chat tab
2. Click + button
3. Select "Photo or Video"
4. Choose image from gallery
5. **Verify:**
   - Upload progress shows
   - File preview appears
   - Can remove file
6. Type message: "Here's a photo"
7. Press send
8. **Verify:**
   - Message sent with attachment
   - AI responds to image

### Test 3: Error Recovery
1. Turn off WiFi
2. Try to send message
3. **Verify:**
   - Error message displays
   - Can dismiss error
4. Turn on WiFi
5. Retry message
6. **Verify:**
   - Message sends successfully

### Test 4: Multiple Files
1. Upload 3 images
2. **Verify:**
   - All 3 show in preview
   - Can remove individual files
3. Send message with all 3
4. **Verify:**
   - All attachments sent
   - AI acknowledges all files

---

## Performance Tests

### Test 1: SSE Latency
- **Metric:** Time from send to first SSE event
- **Target:** < 500ms
- **Measurement:** Browser DevTools Network tab

### Test 2: File Upload Speed
- **Metric:** Upload time for 5MB image
- **Target:** < 3 seconds on good connection
- **Measurement:** Frontend progress callback

### Test 3: Concurrent Users
- **Metric:** 10 simultaneous chat streams
- **Target:** All complete successfully
- **Measurement:** Load testing tool (k6, Artillery)

---

## Automated Test Script

```bash
#!/bin/bash
# integration-test.sh

API_BASE="http://localhost:8080"
TOKEN="YOUR_ACCESS_TOKEN"

echo "=== Running Integration Tests ==="

# Test 1: Health Check
echo "Test 1: Health Check"
curl -s $API_BASE/health | jq .

# Test 2: Chat Stream
echo "Test 2: Chat Stream"
curl -N -X POST $API_BASE/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "profileId": 1,
    "userId": 1,
    "message": "Test message"
  }'

# Test 3: File Upload
echo "Test 3: File Upload"
curl -X POST $API_BASE/api/v1/files/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.jpg" \
  -F "userId=1" | jq .

echo "=== Tests Complete ==="
```

---

## Troubleshooting

### Issue: SSE Connection Drops
**Solution:**
- Check nginx timeout settings
- Verify network stability
- Check backend logs for errors

### Issue: File Upload Fails
**Solution:**
- Verify MinIO is running
- Check file size limits
- Validate file type

### Issue: AI Response Slow
**Solution:**
- Check Gemini API quota
- Verify network latency
- Review AI service logs

---

## Monitoring

### Key Metrics to Track:
1. **Chat Success Rate:** % of messages processed successfully
2. **Average Response Time:** Time from send to complete
3. **File Upload Success Rate:** % of uploads completed
4. **Error Rate:** % of requests with errors
5. **SSE Connection Duration:** How long streams stay open

### Logging:
- All requests logged with user ID and timestamp
- Errors logged with stack traces
- Performance metrics logged for slow requests

---

## Next Steps

1. Set up automated CI/CD tests
2. Add end-to-end tests with Playwright
3. Implement monitoring dashboard
4. Create load testing suite
5. Document API rate limits
