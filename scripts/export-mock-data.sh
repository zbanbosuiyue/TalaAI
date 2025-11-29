#!/bin/bash

# Export Mock Data from Supabase KV Store
# This script exports sample mock data that represents the current frontend data structure
# Usage: ./export-mock-data.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/../mock-data-export"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Tala Mock Data Export ===${NC}"
echo "Output directory: ${OUTPUT_DIR}"
echo "Timestamp: ${TIMESTAMP}"
echo ""

# Create output directory
mkdir -p "${OUTPUT_DIR}"

# Export sample user profiles
echo -e "${YELLOW}Exporting sample user profiles...${NC}"
cat > "${OUTPUT_DIR}/user_profiles_${TIMESTAMP}.json" << 'EOF'
[
  {
    "key": "user_profile:253082161165369344",
    "value": {
      "parentName": "Sarah Johnson",
      "role": "Mom",
      "childName": "Aria",
      "childBirthday": "2023-03-15",
      "childGender": "female",
      "concerns": [
        "Sleep & night wakings",
        "Picky eating",
        "Separation anxiety at drop-off"
      ],
      "challenges": [
        "sleep-regression",
        "picky-eating",
        "separation-anxiety"
      ],
      "hasDaycare": true,
      "daycareName": "Little Stars Daycare",
      "updateMethod": "app",
      "onboardingCompleted": true,
      "userId": "253082161165369344",
      "updatedAt": "2025-11-29T08:58:09.261Z"
    }
  },
  {
    "key": "user_profile:test_user_2",
    "value": {
      "parentName": "Michael Chen",
      "role": "Dad",
      "childName": "Lucas",
      "childBirthday": "2022-08-20",
      "childGender": "male",
      "concerns": [
        "Tantrums & big emotions",
        "Potty training",
        "Daycare sickness cycle"
      ],
      "challenges": [
        "tantrums",
        "potty-training",
        "daycare-sickness"
      ],
      "hasDaycare": true,
      "daycareName": "Sunshine Kids Academy",
      "updateMethod": "app",
      "onboardingCompleted": true,
      "userId": "test_user_2",
      "updatedAt": "2025-11-28T10:30:00.000Z"
    }
  },
  {
    "key": "user_profile:test_user_3",
    "value": {
      "parentName": "Emily Rodriguez",
      "role": "Mom",
      "childName": "Sofia",
      "childBirthday": "2024-01-10",
      "childGender": "female",
      "concerns": [
        "Starting solids",
        "Naps at daycare & nights at home",
        "Teething & fussiness"
      ],
      "challenges": [
        "picky-eating",
        "sleep-regression",
        "teething"
      ],
      "hasDaycare": false,
      "updateMethod": "app",
      "onboardingCompleted": true,
      "userId": "test_user_3",
      "updatedAt": "2025-11-27T15:45:00.000Z"
    }
  }
]
EOF

# Export sample daily data
echo -e "${YELLOW}Exporting sample daily data...${NC}"
cat > "${OUTPUT_DIR}/daily_data_${TIMESTAMP}.json" << 'EOF'
[
  {
    "key": "daily_data:253082161165369344:2025-11-29",
    "value": {
      "userId": "253082161165369344",
      "date": "2025-11-29",
      "bottleOz": 24,
      "bottleCount": 4,
      "solidsPercent": 80,
      "mealsCount": 3,
      "totalSleepHours": 11.5,
      "napCount": 2,
      "napMinutes": 150,
      "nightWakes": 1,
      "skippedNap": false,
      "wetDiapers": 6,
      "dirtyDiapers": 2,
      "pottyAttempts": 0,
      "accidents": 0,
      "dryAllDay": false,
      "temperature": 98.6,
      "teething": false,
      "medicationGiven": null,
      "mood": "happy",
      "tantrum": false,
      "sharing": true,
      "hitFriend": false,
      "bestFriend": "Emma",
      "activity": "Played with blocks and painted",
      "newWords": 2,
      "lastFeedHoursAgo": 2,
      "events": [
        {
          "type": "special-activity",
          "description": "Art class - painted with watercolors",
          "time": "10:00 AM",
          "severity": "info"
        }
      ],
      "updatedAt": "2025-11-29T20:00:00.000Z"
    }
  },
  {
    "key": "daily_data:253082161165369344:2025-11-28",
    "value": {
      "userId": "253082161165369344",
      "date": "2025-11-28",
      "bottleOz": 26,
      "bottleCount": 4,
      "solidsPercent": 70,
      "mealsCount": 3,
      "totalSleepHours": 10.5,
      "napCount": 1,
      "napMinutes": 90,
      "nightWakes": 2,
      "skippedNap": false,
      "wetDiapers": 5,
      "dirtyDiapers": 1,
      "temperature": 99.2,
      "teething": true,
      "medicationGiven": "Tylenol 2.5ml",
      "mood": "grumpy",
      "tantrum": true,
      "sharing": false,
      "hitFriend": false,
      "activity": "Quiet play, seemed tired",
      "newWords": 0,
      "events": [
        {
          "type": "health-alert",
          "title": "Teething discomfort",
          "description": "Extra fussy, drooling more than usual",
          "time": "2:00 PM",
          "severity": "warning"
        }
      ],
      "updatedAt": "2025-11-28T20:15:00.000Z"
    }
  },
  {
    "key": "daily_data:test_user_2:2025-11-29",
    "value": {
      "userId": "test_user_2",
      "date": "2025-11-29",
      "solidsPercent": 90,
      "mealsCount": 3,
      "totalSleepHours": 11,
      "napCount": 1,
      "napMinutes": 60,
      "nightWakes": 0,
      "wetDiapers": 0,
      "dirtyDiapers": 0,
      "pottyAttempts": 5,
      "accidents": 1,
      "dryAllDay": false,
      "mood": "energetic",
      "tantrum": false,
      "sharing": true,
      "bestFriend": "Noah",
      "activity": "Playground time, climbed slide",
      "newWords": 3,
      "events": [
        {
          "type": "special-activity",
          "description": "Potty training success - 4 out of 5 attempts",
          "time": "Throughout day",
          "severity": "info"
        }
      ],
      "updatedAt": "2025-11-29T19:30:00.000Z"
    }
  }
]
EOF

# Export sample child profiles
echo -e "${YELLOW}Exporting sample child profiles...${NC}"
cat > "${OUTPUT_DIR}/child_profiles_${TIMESTAMP}.json" << 'EOF'
[
  {
    "key": "child_profile:profile_1",
    "value": {
      "measurements": [
        {
          "date": "2025-11-01",
          "weight": 25.5,
          "height": 34,
          "headCircumference": 18.5,
          "unit": "lbs/in"
        },
        {
          "date": "2025-10-01",
          "weight": 24.8,
          "height": 33.5,
          "headCircumference": 18.3,
          "unit": "lbs/in"
        },
        {
          "date": "2025-09-01",
          "weight": 24.2,
          "height": 33,
          "headCircumference": 18.1,
          "unit": "lbs/in"
        }
      ],
      "allergies": [
        "peanuts",
        "tree nuts"
      ],
      "medicalHistory": {
        "vaccinations": [
          {
            "name": "DTaP",
            "date": "2023-05-15",
            "doseNumber": 1
          },
          {
            "name": "MMR",
            "date": "2024-03-15",
            "doseNumber": 1
          },
          {
            "name": "Varicella",
            "date": "2024-03-15",
            "doseNumber": 1
          }
        ],
        "pastConditions": [
          {
            "condition": "Ear infection",
            "date": "2024-08-10",
            "resolved": true
          }
        ],
        "currentMedications": []
      },
      "milestones": [
        {
          "milestone": "First steps",
          "date": "2024-01-20",
          "ageMonths": 10
        },
        {
          "milestone": "First words (mama, dada)",
          "date": "2023-12-15",
          "ageMonths": 9
        }
      ],
      "preferences": {
        "foods": [
          "bananas",
          "yogurt",
          "pasta",
          "chicken"
        ],
        "toys": [
          "blocks",
          "stuffed animals",
          "books"
        ],
        "sleepComfort": "white noise machine, favorite blanket"
      },
      "emergencyContacts": [
        {
          "name": "John Johnson",
          "relationship": "Father",
          "phone": "555-0101"
        },
        {
          "name": "Mary Johnson",
          "relationship": "Grandmother",
          "phone": "555-0102"
        }
      ],
      "pediatrician": {
        "name": "Dr. Lisa Anderson",
        "clinic": "Children's Health Center",
        "phone": "555-0200",
        "address": "123 Medical Plaza, Suite 200"
      }
    }
  },
  {
    "key": "child_profile:profile_2",
    "value": {
      "measurements": [
        {
          "date": "2025-11-01",
          "weight": 32,
          "height": 38,
          "unit": "lbs/in"
        }
      ],
      "allergies": [
        "dairy"
      ],
      "medicalHistory": {
        "vaccinations": [
          {
            "name": "DTaP",
            "date": "2022-10-20",
            "doseNumber": 4
          },
          {
            "name": "MMR",
            "date": "2023-08-20",
            "doseNumber": 1
          }
        ],
        "pastConditions": [],
        "currentMedications": []
      },
      "milestones": [
        {
          "milestone": "Potty trained (daytime)",
          "date": "2025-10-15",
          "ageMonths": 38
        }
      ],
      "preferences": {
        "foods": [
          "pizza",
          "apples",
          "crackers"
        ],
        "toys": [
          "dinosaurs",
          "cars",
          "puzzles"
        ],
        "sleepComfort": "nightlight, stuffed dinosaur"
      },
      "emergencyContacts": [
        {
          "name": "Jennifer Chen",
          "relationship": "Mother",
          "phone": "555-0301"
        }
      ],
      "pediatrician": {
        "name": "Dr. Robert Kim",
        "clinic": "Pediatric Care Associates",
        "phone": "555-0400"
      }
    }
  }
]
EOF

# Export sample timeline events (for reference)
echo -e "${YELLOW}Exporting sample timeline events structure...${NC}"
cat > "${OUTPUT_DIR}/timeline_events_structure_${TIMESTAMP}.json" << 'EOF'
{
  "description": "Timeline events are currently generated client-side based on age. This structure shows what should be stored in backend events table.",
  "sampleEvents": [
    {
      "profileId": 1,
      "eventType": "FEEDING",
      "eventTime": "2025-11-29T08:00:00Z",
      "eventData": {
        "type": "bottle",
        "amount": 6,
        "unit": "oz",
        "duration": 15,
        "notes": "Finished entire bottle"
      },
      "source": "USER_INPUT",
      "category": "Food"
    },
    {
      "profileId": 1,
      "eventType": "SLEEP",
      "eventTime": "2025-11-29T13:00:00Z",
      "eventData": {
        "type": "nap",
        "duration": 90,
        "quality": "good",
        "location": "crib",
        "notes": "Fell asleep easily"
      },
      "source": "USER_INPUT",
      "category": "Sleep"
    },
    {
      "profileId": 1,
      "eventType": "DIAPER",
      "eventTime": "2025-11-29T10:30:00Z",
      "eventData": {
        "type": "wet",
        "notes": null
      },
      "source": "USER_INPUT",
      "category": "Health"
    },
    {
      "profileId": 1,
      "eventType": "ACTIVITY",
      "eventTime": "2025-11-29T15:00:00Z",
      "eventData": {
        "type": "art",
        "description": "Painted with watercolors",
        "duration": 30,
        "location": "daycare"
      },
      "source": "DAYCARE",
      "category": "Activity"
    },
    {
      "profileId": 1,
      "eventType": "BEHAVIOR",
      "eventTime": "2025-11-29T18:00:00Z",
      "eventData": {
        "mood": "happy",
        "tantrum": false,
        "sharing": true,
        "notes": "Great mood all evening"
      },
      "source": "USER_INPUT",
      "category": "Behavior"
    },
    {
      "profileId": 1,
      "eventType": "MILESTONE",
      "eventTime": "2025-11-29T16:30:00Z",
      "eventData": {
        "type": "language",
        "description": "Said 'thank you' unprompted",
        "witnessed": true
      },
      "source": "USER_INPUT",
      "category": "Development"
    },
    {
      "profileId": 1,
      "eventType": "HEALTH",
      "eventTime": "2025-11-29T14:00:00Z",
      "eventData": {
        "type": "medication",
        "medication": "Tylenol",
        "dosage": "2.5ml",
        "reason": "teething discomfort",
        "temperature": 99.2
      },
      "source": "USER_INPUT",
      "category": "Health",
      "priority": "medium"
    },
    {
      "profileId": 1,
      "eventType": "INCIDENT",
      "eventTime": "2025-11-28T11:00:00Z",
      "eventData": {
        "type": "minor-injury",
        "description": "Bumped head on table corner",
        "severity": "minor",
        "treatment": "Ice pack applied",
        "location": "daycare"
      },
      "source": "DAYCARE",
      "category": "Health",
      "priority": "high"
    }
  ]
}
EOF

# Export insights data structure
echo -e "${YELLOW}Exporting sample insights structure...${NC}"
cat > "${OUTPUT_DIR}/insights_structure_${TIMESTAMP}.json" << 'EOF'
{
  "description": "Insights are currently generated client-side. This structure shows what backend should generate from event data.",
  "sampleInsights": [
    {
      "id": "sleep-mood-correlation",
      "type": "correlation",
      "title": "Sleep & Mood Link",
      "icon": "Moon",
      "description": "Evening energy patterns",
      "aiConclusion": "We noticed Aria's evening mood is closely connected to daytime rest. On days with 45+ minute naps, evenings are smoother.",
      "actionTip": "On shorter nap days, try starting the bedtime routine 15 minutes earlier to prevent overtiredness.",
      "chartType": "scatter",
      "dataPoints": [
        {"napMinutes": 20, "mood": 2, "day": "Mon"},
        {"napMinutes": 90, "mood": 5, "day": "Tue"},
        {"napMinutes": 35, "mood": 2, "day": "Wed"},
        {"napMinutes": 105, "mood": 5, "day": "Thu"}
      ],
      "priority": 1,
      "ageRange": [12, 36],
      "generatedAt": "2025-11-29T20:00:00Z"
    },
    {
      "id": "nutrition-variety",
      "type": "challenge",
      "title": "Nutrition Opportunities",
      "icon": "Salad",
      "description": "Meal variety patterns",
      "aiConclusion": "Aria had a carb-focused week with pasta appearing 5 times. Only 1 green veggie attempt. There's room to add more variety.",
      "actionTip": "Try hiding veggies in smoothies or mixing peas into mac & cheese this weekend.",
      "chartType": "rainbow",
      "dataPoints": {
        "colors": [
          {"color": "Red", "value": 3, "foods": ["Tomatoes", "Strawberries", "Apples"]},
          {"color": "Orange", "value": 4, "foods": ["Carrots", "Sweet Potato", "Oranges"]},
          {"color": "Yellow", "value": 9, "foods": ["Pasta", "Bread", "Banana", "Cheese"]},
          {"color": "Green", "value": 1, "foods": ["Broccoli"]},
          {"color": "White", "value": 5, "foods": ["Rice", "Milk", "Chicken"]}
        ]
      },
      "priority": 1,
      "ageRange": [12, 60],
      "generatedAt": "2025-11-29T20:00:00Z"
    },
    {
      "id": "social-circle",
      "type": "social",
      "title": "Friendship Patterns",
      "icon": "Users",
      "description": "Play connections at school",
      "aiConclusion": "Emma remains Aria's favorite playmate. This week we noticed Noah appearing more often – a new friendship is forming!",
      "actionTip": "At pickup, ask: 'What did you and Noah play today?' to encourage this budding friendship.",
      "chartType": "bubble",
      "dataPoints": {
        "friends": [
          {"name": "Emma", "frequency": 15, "relationship": "Best Friend"},
          {"name": "Noah", "frequency": 3, "relationship": "New Friend"},
          {"name": "Sophia", "frequency": 8, "relationship": "Play Partner"}
        ]
      },
      "priority": 2,
      "ageRange": [36, 60],
      "generatedAt": "2025-11-29T20:00:00Z"
    }
  ]
}
EOF

# Create migration mapping document
echo -e "${YELLOW}Creating migration mapping document...${NC}"
cat > "${OUTPUT_DIR}/MIGRATION_MAPPING_${TIMESTAMP}.md" << 'EOF'
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
EOF

# Create summary report
echo -e "${YELLOW}Creating summary report...${NC}"
cat > "${OUTPUT_DIR}/EXPORT_SUMMARY_${TIMESTAMP}.txt" << EOF
=== Tala Mock Data Export Summary ===
Export Date: $(date)
Output Directory: ${OUTPUT_DIR}

Files Created:
1. user_profiles_${TIMESTAMP}.json (3 sample user profiles)
2. daily_data_${TIMESTAMP}.json (3 sample daily data records)
3. child_profiles_${TIMESTAMP}.json (2 sample child profiles)
4. timeline_events_structure_${TIMESTAMP}.json (Event structure reference)
5. insights_structure_${TIMESTAMP}.json (Insights structure reference)
6. MIGRATION_MAPPING_${TIMESTAMP}.md (Migration guide)

Total Records:
- User Profiles: 3
- Daily Data Records: 3
- Child Profiles: 2
- Sample Events: 8 (in structure file)
- Sample Insights: 3 (in structure file)

Next Steps:
1. Review exported data for accuracy
2. Create backend migration script
3. Test migration in staging environment
4. Run parallel systems for validation
5. Complete migration and cleanup

Notes:
- This is SAMPLE data representing the frontend data structure
- Real Supabase KV Store data needs to be exported separately
- Use this as a template for the actual migration
- All sensitive data should be handled securely during migration
EOF

echo ""
echo -e "${GREEN}✓ Export completed successfully!${NC}"
echo ""
echo "Files created in: ${OUTPUT_DIR}"
echo ""
echo "Next steps:"
echo "1. Review exported files"
echo "2. Read MIGRATION_MAPPING_${TIMESTAMP}.md"
echo "3. Create backend migration script"
echo ""
