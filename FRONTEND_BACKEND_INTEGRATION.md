# Frontend-Backend Integration Design

**Date:** 2025-11-29  
**Status:** Design Phase  
**Version:** 1.0

---

## Executive Summary

This document outlines the integration strategy between the Tala mobile app frontend (React + Capacitor + Supabase) and the Tala backend microservices (Spring Boot + PostgreSQL).

### Current State
- **Frontend**: Uses Supabase Auth + Edge Functions + KV Store for mock data
- **Backend**: 8 microservices with PostgreSQL, event-driven architecture
- **Gap**: No integration layer exists between frontend and backend

### Target State
- Frontend calls backend REST APIs through API Gateway
- Backend handles all business logic, data persistence, and AI processing
- Supabase Auth continues for authentication, backend validates JWT tokens
- Gradual migration from Supabase KV Store to backend services

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Mobile App (React)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Auth Screen  │  │ Today Page   │  │ Timeline     │      │
│  │ (Supabase)   │  │              │  │              │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ HTTPS/REST
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              API Gateway (Future) / Nginx                    │
│                    Port 8080 / 80                            │
└─────────────────────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ User Service │  │Event Service │  │ Query Service│
│   :8081      │  │   :8082      │  │   :8083      │
└──────────────┘  └──────────────┘  └──────────────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           ▼
                  ┌──────────────────┐
                  │   PostgreSQL     │
                  │   (tala_db)      │
                  └──────────────────┘
```

---

## Frontend API Usage Analysis

### 1. Authentication APIs (Supabase)

**Current Implementation:**
```typescript
// mobile-app/src/utils/supabase/client.tsx
const supabase = createClient(
  `https://${projectId}.supabase.co`,
  publicAnonKey
);

// mobile-app/src/supabase/functions/server/index.tsx
POST /make-server-2da89f74/auth/signup
POST /make-server-2da89f74/auth/login (via Supabase client)
GET  /make-server-2da89f74/auth/profile
```

**Frontend Files:**
- `src/components/AuthScreen.tsx`
- `src/components/LoginPage.tsx`
- `src/components/RegisterPage.tsx`
- `src/utils/useAuth.tsx`

**Backend Mapping:**
```
Frontend Supabase Auth → Backend User Service
POST /auth/signup       → POST /api/v1/auth/register
POST /auth/login        → POST /api/v1/auth/login
GET  /auth/profile      → GET  /api/v1/profiles/user/{userId}
```

---

### 2. User Profile APIs

**Current Mock API:**
```typescript
// mobile-app/src/utils/userData.ts
POST /make-server-2da89f74/user/profile      // Save onboarding data
GET  /make-server-2da89f74/user/profile      // Load user profile
POST /make-server-2da89f74/user/update-child // Update child info
POST /make-server-2da89f74/user/update-avatar // Update avatar
```

**Data Structure:**
```typescript
interface UserProfile {
  parentName: string;
  role: 'Mom' | 'Dad' | 'Parent' | 'Grandparent' | 'Other caregiver';
  childName: string;
  childBirthday: string;
  childGender?: string;
  concerns: string[];
  hasDaycare: boolean;
  daycareName?: string;
  updateMethod?: string;
  onboardingCompleted?: boolean;
}
```

**Backend Mapping:**
```
Frontend Mock API                    → Backend Service
POST /user/profile                   → POST /api/v1/profiles (User Service)
GET  /user/profile                   → GET  /api/v1/profiles/user/{userId}
POST /user/update-child              → PUT  /api/v1/profiles/{id}
POST /user/update-avatar             → PUT  /api/v1/profiles/{id}
GET  /child-profile/:childId         → GET  /api/v1/profiles/{id}
PUT  /child-profile/:childId         → PUT  /api/v1/profiles/{id}
```

---

### 3. Daily Data / Event APIs

**Current Mock API:**
```typescript
// mobile-app/src/utils/userData.ts
POST /make-server-2da89f74/daily/update  // Save today's data
GET  /make-server-2da89f74/daily/get     // Load today's data
```

**Data Structure:**
```typescript
interface TodayData {
  userId: string;
  date: string; // YYYY-MM-DD
  
  // Food
  bottleOz?: number;
  bottleCount?: number;
  solidsPercent?: number;
  mealsCount?: number;
  
  // Sleep
  totalSleepHours?: number;
  napCount?: number;
  napMinutes?: number;
  nightWakes?: number;
  skippedNap?: boolean;
  
  // Potty
  wetDiapers?: number;
  dirtyDiapers?: number;
  pottyAttempts?: number;
  accidents?: number;
  dryAllDay?: boolean;
  
  // Health
  temperature?: number;
  teething?: boolean;
  medicationGiven?: string;
  
  // Behavior/Social
  mood?: 'happy' | 'grumpy' | 'clingy' | 'energetic' | 'calm';
  tantrum?: boolean;
  sharing?: boolean;
  hitFriend?: boolean;
  bestFriend?: string;
  
  // Activities
  activity?: string;
  newWords?: number;
  
  // Events
  events?: DaycareEvent[];
}
```

**Backend Mapping:**
```
Frontend Mock API              → Backend Service
POST /daily/update             → POST /api/v1/events (Event Service)
                                 Multiple events created based on TodayData
GET  /daily/get?date=YYYY-MM-DD → GET /api/v1/events?profileId=X&startTime=...&endTime=...
```

**Event Type Mapping:**
```
TodayData Field       → Backend Event Type
bottleOz/bottleCount  → FEEDING (type: bottle)
solidsPercent/meals   → FEEDING (type: solids)
totalSleepHours/naps  → SLEEP
wetDiapers/dirtyDiap  → DIAPER
temperature/teething  → HEALTH
mood/tantrum          → BEHAVIOR
activity              → ACTIVITY
events[]              → INCIDENT/SICKNESS
```

---

### 4. Timeline APIs

**Current Mock Data:**
```typescript
// mobile-app/src/utils/timelineData.ts
// Static age-appropriate timeline events
export const getAgeAppropriateEvents(childAgeMonths: number, childName: string)
export const getRecentEvents(childAgeMonths: number, childName: string)
```

**Backend Mapping:**
```
Frontend Mock Function           → Backend API
getAgeAppropriateEvents()        → GET /api/v1/events/timeline
                                   ?profileId=X&startTime=...&endTime=...
getRecentEvents()                → GET /api/v1/events/recent
                                   ?profileId=X&page=0&size=20
```

---

### 5. Insights APIs

**Current Mock Data:**
```typescript
// mobile-app/src/utils/insightsData.ts
export const generateInsights(
  childAge: number,
  childName: string,
  userChallenges: string[],
  recentChatTopics: string[]
): InsightCard[]
```

**Backend Mapping:**
```
Frontend Mock Function    → Backend Service
generateInsights()        → GET /api/v1/personalization/insights
                            ?profileId=X (Personalization Service)
                          → Aggregates data from Query Service
                          → Uses AI Service for analysis
```

---

## Database Schema Mapping

### Frontend Mock Storage (Supabase KV Store)

**Table:** `kv_store_2da89f74`
```sql
CREATE TABLE kv_store_2da89f74 (
  key TEXT NOT NULL PRIMARY KEY,
  value JSONB NOT NULL
);
```

**Key Patterns:**
- `user_profile:{userId}` → User onboarding data
- `child_profile:{childId}` → Child profile details
- `daily_data:{userId}:{date}` → Daily tracking data

### Backend Database (PostgreSQL)

**Schema:** `users`
```sql
-- Users table
CREATE TABLE users.users (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Profiles table (Baby profiles)
CREATE TABLE users.profiles (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    baby_name VARCHAR(255) NOT NULL,
    birth_date DATE,
    timezone VARCHAR(50) DEFAULT 'UTC',
    gender VARCHAR(20),
    photo_url TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);
```

**Schema:** `events`
```sql
CREATE TABLE events.events (
    id BIGINT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    event_data JSONB NOT NULL,
    ai_summary TEXT,
    ai_tags JSONB,
    source VARCHAR(50) DEFAULT 'USER_INPUT',
    priority VARCHAR(20),
    urgency_hours INTEGER,
    risk_level VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);
```

---

## Migration Strategy

### Phase 1: Authentication Integration (Week 1-2)

**Goal:** Replace Supabase Auth with Backend User Service

**Steps:**
1. Keep Supabase Auth for now (low priority change)
2. Add JWT token validation in backend
3. Frontend sends Supabase JWT to backend in `Authorization: Bearer {token}` header
4. Backend validates JWT and extracts user ID

**Changes Required:**
- Backend: Add Supabase JWT validation filter
- Frontend: No changes (already sends JWT)

### Phase 2: Profile Management (Week 3-4)

**Goal:** Migrate user/child profiles to backend

**Steps:**
1. Create profile in backend on first login
2. Sync existing Supabase KV data to backend (one-time migration script)
3. Update frontend to call backend profile APIs
4. Keep Supabase KV as fallback for 2 weeks

**Changes Required:**
- Frontend: Update `userData.ts` functions to call backend APIs
- Backend: Add profile sync endpoint
- Migration: Script to export KV data and import to PostgreSQL

### Phase 3: Event Tracking (Week 5-6)

**Goal:** Replace daily data storage with event-based system

**Steps:**
1. Transform `TodayData` into multiple event records
2. Create event aggregation endpoint for "Today" page
3. Update frontend to save/load events instead of daily data
4. Implement real-time event sync

**Changes Required:**
- Frontend: Refactor `saveTodayData()` to create multiple events
- Backend: Add event aggregation logic in Query Service
- Frontend: Update Today page to consume aggregated events

### Phase 4: Timeline & Insights (Week 7-8)

**Goal:** Replace mock timeline/insights with backend-generated data

**Steps:**
1. Implement timeline aggregation in Query Service
2. Implement insights generation in Personalization Service
3. Update frontend to fetch real timeline data
4. Update frontend to fetch real insights

**Changes Required:**
- Backend: Implement timeline/insights endpoints
- Frontend: Remove mock data generators
- Frontend: Update UI components to handle real data

### Phase 5: Cleanup (Week 9-10)

**Goal:** Remove Supabase dependencies

**Steps:**
1. Verify all features work with backend
2. Remove Supabase Edge Functions
3. Remove Supabase KV Store
4. Keep only Supabase Auth (or migrate to backend auth)

---

## API Integration Examples

### Example 1: User Registration

**Frontend (Current):**
```typescript
// Supabase Edge Function
POST https://{projectId}.supabase.co/functions/v1/make-server-2da89f74/auth/signup
Body: { email, password, name }
```

**Frontend (After Migration):**
```typescript
// Backend User Service
POST http://api.tala.ai/api/v1/auth/register
Body: { email, password, fullName }
Headers: { "Content-Type": "application/json" }
```

**Backend Response:**
```json
{
  "userId": 253082161165369344,
  "email": "test@example.com",
  "fullName": "Test User",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Example 2: Save Daily Data → Create Events

**Frontend (Current):**
```typescript
POST /make-server-2da89f74/daily/update
Body: {
  userId: "user123",
  date: "2025-11-29",
  bottleOz: 24,
  bottleCount: 4,
  napMinutes: 90,
  wetDiapers: 5,
  mood: "happy"
}
```

**Frontend (After Migration):**
```typescript
// Create multiple events
POST /api/v1/events (4 separate calls or batch endpoint)

// Event 1: Feeding
{
  profileId: 1,
  eventType: "FEEDING",
  eventTime: "2025-11-29T08:00:00Z",
  eventData: {
    type: "bottle",
    amount: 6,
    unit: "oz"
  }
}

// Event 2: Sleep
{
  profileId: 1,
  eventType: "SLEEP",
  eventTime: "2025-11-29T13:00:00Z",
  eventData: {
    duration: 90,
    type: "nap"
  }
}

// Event 3: Diaper
{
  profileId: 1,
  eventType: "DIAPER",
  eventTime: "2025-11-29T10:00:00Z",
  eventData: {
    type: "wet"
  }
}

// Event 4: Behavior
{
  profileId: 1,
  eventType: "BEHAVIOR",
  eventTime: "2025-11-29T18:00:00Z",
  eventData: {
    mood: "happy"
  }
}
```

### Example 3: Get Timeline

**Frontend (Current):**
```typescript
// Mock data generator
const events = getAgeAppropriateEvents(childAgeMonths, childName);
```

**Frontend (After Migration):**
```typescript
GET /api/v1/events/timeline
  ?profileId=1
  &startTime=2025-11-29T00:00:00Z
  &endTime=2025-11-29T23:59:59Z
  &eventTypes=FEEDING,SLEEP,DIAPER
```

**Backend Response:**
```json
{
  "events": [
    {
      "id": 253082273342029824,
      "profileId": 1,
      "eventType": "FEEDING",
      "eventTime": "2025-11-29T08:00:00Z",
      "eventData": {
        "type": "bottle",
        "amount": 6,
        "unit": "oz"
      },
      "aiSummary": "Morning bottle feeding, 6 oz",
      "source": "USER_INPUT"
    }
  ],
  "totalCount": 12
}
```

---

## Data Export (Mock Database)

### Export Supabase KV Store Data

**Command:**
```bash
# Connect to Supabase project
supabase db dump --db-url "postgresql://postgres:password@db.{projectId}.supabase.co:5432/postgres"

# Export specific table
psql -h db.{projectId}.supabase.co -U postgres -d postgres \
  -c "COPY (SELECT * FROM kv_store_2da89f74) TO STDOUT WITH CSV HEADER" \
  > supabase_kv_export.csv
```

### Sample Mock Data Records

**User Profile:**
```json
{
  "key": "user_profile:user123",
  "value": {
    "parentName": "Sarah Johnson",
    "role": "Mom",
    "childName": "Aria",
    "childBirthday": "2023-03-15",
    "childGender": "female",
    "concerns": ["Sleep & night wakings", "Picky eating"],
    "hasDaycare": true,
    "daycareName": "Little Stars Daycare",
    "onboardingCompleted": true,
    "updatedAt": "2025-11-29T08:00:00Z"
  }
}
```

**Daily Data:**
```json
{
  "key": "daily_data:user123:2025-11-29",
  "value": {
    "userId": "user123",
    "date": "2025-11-29",
    "bottleOz": 24,
    "bottleCount": 4,
    "solidsPercent": 80,
    "mealsCount": 3,
    "totalSleepHours": 12,
    "napCount": 2,
    "napMinutes": 150,
    "nightWakes": 1,
    "wetDiapers": 6,
    "dirtyDiapers": 2,
    "temperature": 98.6,
    "mood": "happy",
    "activity": "Played with blocks",
    "updatedAt": "2025-11-29T20:00:00Z"
  }
}
```

**Child Profile:**
```json
{
  "key": "child_profile:child456",
  "value": {
    "measurements": [
      { "date": "2025-11-01", "weight": 25, "height": 34, "unit": "lbs/in" }
    ],
    "allergies": ["peanuts", "dairy"],
    "medicalHistory": {
      "vaccinations": ["DTaP", "MMR"],
      "pastConditions": ["ear infection"],
      "currentMedications": []
    },
    "preferences": {
      "foods": ["bananas", "yogurt"],
      "toys": ["blocks", "stuffed animals"],
      "sleepComfort": "white noise"
    }
  }
}
```

---

## Implementation Checklist

### Backend Tasks

- [ ] Add Supabase JWT validation filter
- [ ] Create profile sync endpoint
- [ ] Implement event aggregation for "Today" page
- [ ] Implement timeline aggregation endpoint
- [ ] Implement insights generation endpoint
- [ ] Add batch event creation endpoint
- [ ] Create data migration scripts
- [ ] Add API documentation (Swagger/OpenAPI)
- [ ] Set up API Gateway or Nginx routing
- [ ] Configure CORS for mobile app

### Frontend Tasks

- [ ] Update API base URL configuration
- [ ] Refactor `userData.ts` to call backend APIs
- [ ] Transform `TodayData` to event creation calls
- [ ] Update timeline components to fetch real data
- [ ] Update insights components to fetch real data
- [ ] Add error handling for backend API calls
- [ ] Add loading states for API calls
- [ ] Implement retry logic for failed requests
- [ ] Add offline support (cache + sync)
- [ ] Remove mock data generators

### Testing Tasks

- [ ] Test authentication flow end-to-end
- [ ] Test profile creation and updates
- [ ] Test event creation from daily data
- [ ] Test timeline data loading
- [ ] Test insights generation
- [ ] Load testing for API endpoints
- [ ] Test offline mode and sync
- [ ] Test error scenarios
- [ ] Test data migration accuracy

### DevOps Tasks

- [ ] Set up staging environment
- [ ] Configure API Gateway
- [ ] Set up SSL certificates
- [ ] Configure monitoring and logging
- [ ] Set up CI/CD pipeline for backend
- [ ] Set up CI/CD pipeline for frontend
- [ ] Create rollback plan
- [ ] Document deployment process

---

## Risk Assessment

### High Risk
- **Data Loss During Migration**: Mitigation → Run parallel systems for 2 weeks
- **Authentication Breaking**: Mitigation → Keep Supabase Auth as fallback

### Medium Risk
- **Performance Issues**: Mitigation → Load testing before launch
- **API Compatibility**: Mitigation → Versioned APIs (v1, v2)

### Low Risk
- **Frontend UI Changes**: Mitigation → Minimal UI changes required
- **User Experience**: Mitigation → Gradual rollout with feature flags

---

## Success Metrics

- [ ] 100% of user profiles migrated successfully
- [ ] 100% of daily data converted to events
- [ ] API response time < 200ms (p95)
- [ ] Zero data loss during migration
- [ ] Mobile app works offline with sync
- [ ] All mock data removed from frontend
- [ ] Backend handles 1000+ concurrent users

---

## Next Steps

1. **Review this document** with team
2. **Create detailed technical specs** for each phase
3. **Set up staging environment** for testing
4. **Begin Phase 1** (Authentication Integration)
5. **Schedule weekly sync meetings** to track progress

---

## Appendix

### A. API Endpoint Reference

**User Service (Port 8081)**
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login user
- `POST /api/v1/profiles` - Create child profile
- `GET /api/v1/profiles/user/{userId}` - Get user's profiles
- `GET /api/v1/profiles/{id}` - Get profile by ID
- `PUT /api/v1/profiles/{id}` - Update profile
- `DELETE /api/v1/profiles/{id}` - Delete profile

**Event Service (Port 8082)**
- `POST /api/v1/events` - Create event
- `GET /api/v1/events/{id}` - Get event by ID
- `PUT /api/v1/events/{id}` - Update event
- `DELETE /api/v1/events/{id}` - Delete event
- `GET /api/v1/events` - Get events by time range
- `GET /api/v1/events/recent` - Get recent events (paginated)
- `GET /api/v1/events/timeline` - Get timeline events
- `GET /api/v1/events/calendar` - Get calendar month summary

**Query Service (Port 8083)**
- `GET /api/v1/analytics/daily-summary` - Get daily summary
- `GET /api/v1/analytics/trends` - Get trends analysis

**Personalization Service (Port 8084)**
- `GET /api/v1/personalization/today` - Get Today page data
- `GET /api/v1/personalization/insights` - Get insights

### B. Environment Variables

**Frontend:**
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_SUPABASE_URL=https://{projectId}.supabase.co
VITE_SUPABASE_ANON_KEY={anonKey}
```

**Backend:**
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/tala_db
SPRING_DATASOURCE_USERNAME=tala
SPRING_DATASOURCE_PASSWORD=tala_dev_2025
JWT_SECRET=dev-secret-key-change-in-production
SUPABASE_JWT_SECRET={supabaseJwtSecret}
```

### C. Contact Information

- **Backend Lead**: [Name]
- **Frontend Lead**: [Name]
- **DevOps Lead**: [Name]
- **Project Manager**: [Name]

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-29  
**Next Review:** 2025-12-06
