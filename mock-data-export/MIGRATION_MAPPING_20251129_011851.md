# Mock Data to Backend Database Migration Mapping

## Overview
This document maps the current Supabase KV Store data structure to the backend PostgreSQL schema.

## User Profile Mapping

### Source: `user_profile:{userId}` (KV Store)
```json
{
  "parentName": "Sarah Johnson",
  "role": "Mom",
  "childName": "Aria",
  "childBirthday": "2023-03-15",
  "childGender": "female",
  "concerns": ["Sleep & night wakings"],
  "hasDaycare": true,
  "daycareName": "Little Stars Daycare"
}
```

### Target: `users.users` + `users.profiles` (PostgreSQL)

**users.users:**
```sql
INSERT INTO users.users (id, email, password_hash, full_name, created_at, updated_at)
VALUES (
  253082161165369344,
  'test@example.com',
  'hashed_password',
  'Sarah Johnson',
  '2025-11-29 08:58:09.261',
  '2025-11-29 08:58:09.261'
);
```

**users.profiles:**
```sql
INSERT INTO users.profiles (id, user_id, baby_name, birth_date, gender, timezone, created_at, updated_at)
VALUES (
  1,
  253082161165369344,
  'Aria',
  '2023-03-15',
  'female',
  'America/Los_Angeles',
  '2025-11-29 08:58:09.261',
  '2025-11-29 08:58:09.261'
);
```

**users.user_interest_profiles:**
```sql
INSERT INTO users.user_interest_profiles (id, user_id, profile_id, interests, concerns, created_at, updated_at)
VALUES (
  1,
  253082161165369344,
  1,
  '{"hasDaycare": true, "daycareName": "Little Stars Daycare", "role": "Mom"}'::jsonb,
  '["Sleep & night wakings"]'::jsonb,
  '2025-11-29 08:58:09.261',
  '2025-11-29 08:58:09.261'
);
```

## Daily Data to Events Mapping

### Source: `daily_data:{userId}:{date}` (KV Store)
```json
{
  "date": "2025-11-29",
  "bottleOz": 24,
  "bottleCount": 4,
  "napMinutes": 150,
  "wetDiapers": 6,
  "mood": "happy"
}
```

### Target: Multiple `events.events` records

**Feeding Events (4 records):**
```sql
INSERT INTO events.events (id, profile_id, user_id, event_type, event_time, event_data, source, created_at, updated_at)
VALUES
  (1, 1, 253082161165369344, 'FEEDING', '2025-11-29 08:00:00', '{"type": "bottle", "amount": 6, "unit": "oz"}'::jsonb, 'USER_INPUT', NOW(), NOW()),
  (2, 1, 253082161165369344, 'FEEDING', '2025-11-29 12:00:00', '{"type": "bottle", "amount": 6, "unit": "oz"}'::jsonb, 'USER_INPUT', NOW(), NOW()),
  (3, 1, 253082161165369344, 'FEEDING', '2025-11-29 16:00:00', '{"type": "bottle", "amount": 6, "unit": "oz"}'::jsonb, 'USER_INPUT', NOW(), NOW()),
  (4, 1, 253082161165369344, 'FEEDING', '2025-11-29 20:00:00', '{"type": "bottle", "amount": 6, "unit": "oz"}'::jsonb, 'USER_INPUT', NOW(), NOW());
```

**Sleep Event:**
```sql
INSERT INTO events.events (id, profile_id, user_id, event_type, event_time, event_data, source, created_at, updated_at)
VALUES (5, 1, 253082161165369344, 'SLEEP', '2025-11-29 13:00:00', '{"type": "nap", "duration": 150}'::jsonb, 'USER_INPUT', NOW(), NOW());
```

**Diaper Events (6 records):**
```sql
INSERT INTO events.events (id, profile_id, user_id, event_type, event_time, event_data, source, created_at, updated_at)
VALUES
  (6, 1, 253082161165369344, 'DIAPER', '2025-11-29 09:00:00', '{"type": "wet"}'::jsonb, 'USER_INPUT', NOW(), NOW()),
  (7, 1, 253082161165369344, 'DIAPER', '2025-11-29 11:00:00', '{"type": "wet"}'::jsonb, 'USER_INPUT', NOW(), NOW()),
  -- ... (4 more records)
```

**Behavior Event:**
```sql
INSERT INTO events.events (id, profile_id, user_id, event_type, event_time, event_data, source, created_at, updated_at)
VALUES (12, 1, 253082161165369344, 'BEHAVIOR', '2025-11-29 18:00:00', '{"mood": "happy"}'::jsonb, 'USER_INPUT', NOW(), NOW());
```

## Child Profile Mapping

### Source: `child_profile:{childId}` (KV Store)
```json
{
  "measurements": [{"date": "2025-11-01", "weight": 25.5, "height": 34}],
  "allergies": ["peanuts"],
  "medicalHistory": {...}
}
```

### Target: Extended `users.profiles` + separate tables (future)

**For now, store in profiles.photo_url as JSONB (temporary):**
```sql
UPDATE users.profiles
SET photo_url = '{"measurements": [...], "allergies": [...], "medicalHistory": {...}}'::text
WHERE id = 1;
```

**Future: Create dedicated tables:**
- `users.profile_measurements`
- `users.profile_allergies`
- `users.profile_medical_history`

## Migration Script Pseudocode

```python
# 1. Export all KV Store data
kv_data = export_from_supabase_kv()

# 2. For each user_profile
for profile in kv_data['user_profiles']:
    # Create user record
    user_id = create_user(profile['email'], profile['parentName'])
    
    # Create profile record
    profile_id = create_profile(user_id, profile['childName'], profile['childBirthday'])
    
    # Create interest profile
    create_interest_profile(user_id, profile_id, profile['concerns'], profile['hasDaycare'])

# 3. For each daily_data
for daily in kv_data['daily_data']:
    profile_id = get_profile_id(daily['userId'])
    
    # Create feeding events
    for i in range(daily['bottleCount']):
        create_event('FEEDING', profile_id, daily['date'], {...})
    
    # Create sleep events
    if daily['napMinutes']:
        create_event('SLEEP', profile_id, daily['date'], {...})
    
    # Create diaper events
    for i in range(daily['wetDiapers']):
        create_event('DIAPER', profile_id, daily['date'], {...})
    
    # Create behavior event
    if daily['mood']:
        create_event('BEHAVIOR', profile_id, daily['date'], {...})

# 4. Verify migration
verify_record_counts()
verify_data_integrity()
```

## Validation Queries

```sql
-- Count users
SELECT COUNT(*) FROM users.users;

-- Count profiles
SELECT COUNT(*) FROM users.profiles;

-- Count events by type
SELECT event_type, COUNT(*) 
FROM events.events 
GROUP BY event_type;

-- Verify date range
SELECT MIN(event_time), MAX(event_time) 
FROM events.events;

-- Check for orphaned records
SELECT COUNT(*) 
FROM events.events e
LEFT JOIN users.profiles p ON e.profile_id = p.id
WHERE p.id IS NULL;
```

## Rollback Plan

1. Keep Supabase KV Store data for 30 days
2. Run parallel systems for 2 weeks
3. If issues found, switch frontend back to Supabase
4. Fix backend issues
5. Re-run migration

## Notes

- All timestamps should be in UTC
- Event times are estimated based on typical daily schedules
- Some data loss is acceptable for non-critical fields
- Priority: Preserve user profiles, child info, and recent events (last 30 days)
