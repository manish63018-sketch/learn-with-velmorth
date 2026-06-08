# LEARN WITH VELMORTH — API ARCHITECTURE
## Section 10: Complete API Design

---

## 10.1 API DESIGN PRINCIPLES

- **RESTful** for standard CRUD operations
- **GraphQL** for complex queries with flexible field selection (admin dashboard, analytics)
- **WebSocket** for real-time bidirectional communication
- **gRPC** for high-performance internal service-to-service communication
- All public APIs versioned: `/api/v1/...`
- All responses: `{ data: {...}, meta: {...}, errors: [...] }`
- Request IDs: Every request returns `X-Request-ID` for tracing

---

## 10.2 AUTHENTICATION APIs

### POST /api/v1/auth/google
**Description**: Authenticate with Google OAuth
```json
// Request
{
  "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6...",
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "device_platform": "android",
  "device_name": "Samsung Galaxy S24"
}

// Response 200
{
  "data": {
    "access_token": "eyJhbGciOiJSUzI1Ni...",
    "refresh_token": "v2.550e8400...",
    "token_type": "Bearer",
    "expires_in": 900,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "display_name": "Manish Sharma",
      "username": "manish_sharma",
      "avatar_url": "https://cdn.velmorth.com/avatars/...",
      "is_premium": false,
      "onboarding_complete": false
    },
    "is_new_user": true
  },
  "meta": { "request_id": "req_xyz123" }
}

// Response 401
{
  "errors": [{ "code": "INVALID_GOOGLE_TOKEN", "message": "Google ID token is invalid or expired" }],
  "meta": { "request_id": "req_xyz123" }
}
```

### POST /api/v1/auth/refresh
```json
// Request (cookie OR body)
{ "refresh_token": "v2.550e8400..." }

// Response 200
{
  "data": {
    "access_token": "eyJhbGciOiJSUzI1Ni...",
    "expires_in": 900
  }
}

// Response 401 (expired/invalid refresh token — requires re-login)
{
  "errors": [{ "code": "REFRESH_TOKEN_EXPIRED", "message": "Session expired. Please sign in again." }]
}
```

### POST /api/v1/auth/logout
```json
// Headers: Authorization: Bearer {access_token}
// Request
{ "all_devices": false }  // true = logout all sessions

// Response 200
{ "data": { "message": "Logged out successfully" } }
```

### POST /api/v1/auth/2fa/enable
```json
// Request
{ "password": "current_password" }

// Response 200
{
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",
    "qr_code_url": "otpauth://totp/Velmorth:user@email.com?secret=...",
    "backup_codes": ["12345678", "87654321", ...]  // 10 codes, show once
  }
}
```

---

## 10.3 USER APIs

### GET /api/v1/users/me
```json
// Response 200
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "display_name": "Manish Sharma",
    "username": "manish_sharma",
    "email": "manish@example.com",
    "bio": "Learning Spanish for my trip to Barcelona! 🇪🇸",
    "avatar_url": "https://cdn.velmorth.com/avatars/manish.webp",
    "country_code": "IN",
    "timezone": "Asia/Kolkata",
    "interface_language": "en",
    "native_languages": ["en", "hi"],
    "is_premium": true,
    "premium_expires_at": "2026-12-31T23:59:59Z",
    "onboarding_complete": true,
    "selected_title": "Language Scholar",
    "created_at": "2025-01-15T08:00:00Z"
  }
}
```

### PATCH /api/v1/users/me
```json
// Request (partial update supported)
{
  "display_name": "Manish S.",
  "bio": "Spanish learner 🇪🇸 | B1 level",
  "profile_visibility": "public"
}

// Response 200
{ "data": { /* updated user object */ } }
```

### GET /api/v1/users/me/learning-dna
```json
// Response 200
{
  "data": {
    "version": 23,
    "primary_cognitive_style": "visual",
    "secondary_cognitive_style": "reading_writing",
    "memory_profile": {
      "acquisition_speed": 0.72,
      "consolidation_speed": 0.58,
      "optimal_session_minutes": 20,
      "max_new_items_per_session": 8,
      "review_to_new_ratio": 0.65
    },
    "motivation_profile": {
      "primary_motivator": "achievement",
      "challenge_tolerance": 0.75,
      "gamification_responsiveness": 0.88
    },
    "performance_profile": {
      "accuracy_by_skill": {
        "vocabulary": 0.82,
        "grammar": 0.64,
        "listening": 0.78,
        "reading": 0.85,
        "speaking": 0.59
      },
      "accuracy_trend": "improving"
    },
    "schedule_profile": {
      "peak_performance_hours": [8, 9, 10, 20, 21],
      "optimal_session_start_hour": 8
    },
    "error_profile": {
      "common_error_types": [
        {"type": "subjunctive_vs_indicative", "frequency": 23},
        {"type": "ser_vs_estar", "frequency": 18}
      ]
    },
    "last_updated": "2025-06-08T12:00:00Z"
  }
}
```

---

## 10.4 COURSE AND LESSON APIs

### GET /api/v1/courses
```json
// Query params: ?target_language=es&instruction_language=en&page=1&limit=20

// Response 200
{
  "data": {
    "courses": [
      {
        "id": "course-uuid",
        "target_language": "es",
        "title": "Spanish for English Speakers",
        "description": "Master Spanish from beginner to advanced...",
        "learner_count": 4521034,
        "units_count": 24,
        "lessons_count": 312,
        "proficiency_range": "A0-C2",
        "estimated_hours": 450
      }
    ],
    "total": 1,
    "page": 1,
    "limit": 20
  }
}
```

### GET /api/v1/courses/:courseId/progress
```json
// Response 200
{
  "data": {
    "course_id": "course-uuid",
    "enrollment": {
      "enrolled_at": "2025-01-15T08:00:00Z",
      "last_active_at": "2025-06-08T09:30:00Z",
      "overall_progress": 0.342,
      "lessons_completed": 107,
      "items_mastered": 843,
      "total_study_minutes": 1240,
      "estimated_cefr_level": "B1"
    },
    "current_unit": {
      "id": "unit-uuid",
      "title": "Subjunctive Mood",
      "sequence_order": 14,
      "user_progress": 0.45
    },
    "next_lesson": {
      "id": "lesson-uuid",
      "title": "Present Subjunctive: Wishes and Desires",
      "estimated_duration_minutes": 12,
      "items_count": 18,
      "is_new": true,
      "xp_available": 75
    },
    "due_review_count": 23
  }
}
```

### POST /api/v1/lessons/sessions
**Description**: Start a new lesson session (returns session config with items)
```json
// Request
{
  "lesson_id": "lesson-uuid",  // null for AI-generated adaptive session
  "session_type": "standard",  // "standard", "review", "practice", "ai_adaptive"
  "time_available_minutes": 20
}

// Response 201
{
  "data": {
    "session_id": "session-uuid",
    "lesson_id": "lesson-uuid",
    "lesson_title": "Present Subjunctive: Wishes and Desires",
    "items": [
      {
        "id": "item-uuid",
        "exercise_type": "multiple_choice",
        "item_order": 1,
        "content": {
          "question": "Choose the correct subjunctive form:",
          "context": "Quiero que tú... (hablar) más despacio.",
          "options": [
            {"id": "a", "text": "hablas"},
            {"id": "b", "text": "hablen"},
            {"id": "c", "text": "hables"},
            {"id": "d", "text": "hablaste"}
          ],
          "correct_answer_id": "c"
        },
        "audio_url": "https://cdn.velmorth.com/audio/item-uuid.mp3",
        "hint": "After 'querer que', use subjunctive. -ar verbs: -e, -es, -e..."
      },
      {
        "id": "item-uuid-2",
        "exercise_type": "speak",
        "item_order": 2,
        "content": {
          "target_phrase": "Espero que llegues a tiempo",
          "translation": "I hope you arrive on time",
          "phonetic": "esˈpeɾo ke ˈʝeɣes a ˈtjempo",
          "recording_max_seconds": 10
        },
        "audio_url": "https://cdn.velmorth.com/audio/reference-uuid.mp3"
      }
      // ... 13 more items
    ],
    "total_items": 15,
    "hearts_available": 5,
    "hints_available": 3,
    "estimated_xp": 75,
    "session_config": {
      "show_translation_hints": true,
      "voice_enabled": true,
      "time_pressure": false
    }
  }
}
```

### POST /api/v1/lessons/sessions/:sessionId/answers
**Description**: Submit an answer to a lesson item
```json
// Request
{
  "item_id": "item-uuid",
  "answer": "c",  // For MC: option ID; for translate: text; for speak: base64 audio
  "response_time_ms": 3240,
  "hint_used": false
}

// Response 200
{
  "data": {
    "item_id": "item-uuid",
    "is_correct": true,
    "quality_score": 4,
    "correct_answer": "c",
    "xp_earned": 5,
    "hearts_remaining": 5,
    "explanation": {
      "title": "Correct! 🎉",
      "text": "After verbs of desire (querer, desear, esperar), use subjunctive. The 'tú' form of 'hablar' in present subjunctive is 'hables'.",
      "grammar_rule": "Subjunctive triggers: querer que, esperar que, desear que...",
      "example": "Quiero que tú hables más — 'hables' is subjunctive"
    },
    "memory_update": {
      "mastery_level": "learning",
      "mastery_score": 0.42,
      "next_review": "2025-06-10T08:00:00Z"
    },
    "session_progress": {
      "items_completed": 7,
      "items_total": 15,
      "xp_earned_so_far": 35,
      "accuracy_so_far": 0.857
    }
  }
}
```

### POST /api/v1/lessons/sessions/:sessionId/complete
```json
// Request (signal completion, get final results)
{}

// Response 200
{
  "data": {
    "session_id": "session-uuid",
    "lesson_id": "lesson-uuid",
    "results": {
      "score": 0.933,
      "accuracy": 0.933,
      "stars_earned": 3,
      "xp_earned": 98,
      "time_taken_seconds": 587,
      "items_total": 15,
      "items_correct": 14,
      "is_perfect": false,
      "hearts_lost": 0
    },
    "level_update": {
      "xp_before": 15420,
      "xp_after": 15518,
      "level_before": 18,
      "level_after": 18,
      "level_progress": 0.62
    },
    "streak_update": {
      "streak": 48,
      "streak_extended": true,
      "streak_xp_bonus": 20
    },
    "new_achievements": [
      {
        "id": "achievement-uuid",
        "name": "Subjunctive Survivor",
        "description": "Complete 5 lessons on the subjunctive mood",
        "xp_reward": 200,
        "badge_url": "https://cdn.velmorth.com/badges/subjunctive.png"
      }
    ],
    "ai_insight": "Excellent session! You've consistently used 'hables' correctly — this suggests the pattern is solidifying. Focus your next review on irregular subjunctive forms like 'sea', 'tenga', 'vaya'.",
    "next_lesson": {
      "id": "lesson-uuid-next",
      "title": "Irregular Subjunctive Verbs",
      "estimated_duration_minutes": 15
    },
    "confetti": true
  }
}
```

---

## 10.5 PROGRESS APIs

### GET /api/v1/progress/summary
```json
// Response 200
{
  "data": {
    "user_id": "user-uuid",
    "total_xp": 15518,
    "current_level": 18,
    "xp_to_next_level": 4482,
    "current_streak": 48,
    "longest_streak": 103,
    "study_stats": {
      "total_days_studied": 167,
      "total_minutes": 2840,
      "total_lessons": 312,
      "total_words_mastered": 847,
      "current_week_xp": 3240,
      "today_minutes": 18
    },
    "active_courses": [
      {
        "course_id": "course-uuid",
        "target_language": "es",
        "flag_emoji": "🇪🇸",
        "estimated_cefr_level": "B1",
        "progress": 0.342,
        "items_mastered": 843,
        "last_active": "2025-06-08T09:30:00Z"
      }
    ],
    "daily_goal": {
      "target_minutes": 20,
      "completed_minutes": 18,
      "progress": 0.9,
      "is_complete": false
    }
  }
}
```

### GET /api/v1/progress/analytics
```json
// Query params: ?course_id=uuid&period=30d

// Response 200
{
  "data": {
    "period_days": 30,
    "accuracy_trend": {
      "labels": ["Jun 1", "Jun 8", "Jun 15", "Jun 22"],
      "data": [0.71, 0.74, 0.78, 0.82]
    },
    "mastery_by_skill": {
      "vocabulary": 0.82,
      "grammar": 0.64,
      "listening": 0.78,
      "reading": 0.85,
      "speaking": 0.59
    },
    "learning_velocity": {
      "words_per_week": 45,
      "words_per_week_trend": "increasing"
    },
    "retention_rate": 0.78,
    "at_risk_items": 23,
    "study_patterns": {
      "most_active_day": "Tuesday",
      "most_active_hour": 8,
      "avg_session_length_minutes": 17.2
    },
    "predictions": {
      "b2_estimated_date": "2026-02-14",
      "confidence_days": 21,
      "on_track": true
    }
  }
}
```

---

## 10.6 AI APIs

### POST /api/v1/ai/tutor/messages
**Description**: Send a message to the AI Tutor
```json
// Request
{
  "session_id": "ai-session-uuid",
  "message": "I'm confused about when to use ser vs estar. Can you help?",
  "message_type": "text",  // "text", "voice" (for voice: message = base64 audio)
  "context": {
    "current_lesson_topic": "Spanish Copular Verbs",
    "recent_mistakes": ["ser_vs_estar"]
  }
}

// Response 200
{
  "data": {
    "message_id": "msg-uuid",
    "session_id": "ai-session-uuid",
    "response": {
      "text": "Great question! **Ser** and **estar** are both 'to be', but they work differently. Here's the key:\n\n**Ser** = permanent or inherent qualities:\n- Identity: *Soy estudiante* (I am a student)\n- Origin: *Soy de India* (I'm from India)\n- Characteristics: *La casa es grande* (The house is big)\n\n**Estar** = temporary states or positions:\n- Emotion: *Estoy cansado* (I'm tired)\n- Location: *Estoy en casa* (I'm at home)\n- Condition: *La sopa está fría* (The soup is cold)\n\n🎯 Quick rule: If you can imagine it changing easily, use **estar**!\n\nQuick quiz — which would you use: 'La playa __ hermosa' (the beach is beautiful)?",
      "target_language_phrases": [
        {"text": "Soy estudiante", "translation": "I am a student", "audio_url": "https://..."},
        {"text": "Estoy cansado", "translation": "I am tired", "audio_url": "https://..."}
      ],
      "tools_used": ["get_grammar_explanation", "generate_practice_exercise"],
      "exercise_inline": {
        "type": "quick_quiz",
        "question": "La playa ___ hermosa",
        "options": ["es", "está"],
        "correct": "es",
        "hint": "Permanent quality — beaches are inherently beautiful!"
      }
    },
    "audio_url": "https://cdn.velmorth.com/ai-audio/response-uuid.mp3",
    "created_at": "2025-06-08T09:35:00Z"
  }
}
```

### POST /api/v1/ai/voice/analyze
**Description**: Analyze pronunciation of recorded audio
```json
// Request (multipart/form-data)
// Fields:
//   audio: binary audio file (WebM, MP4, WAV, max 60 seconds)
//   target_phrase: "Espero que llegues a tiempo"
//   language: "es"

// Response 200
{
  "data": {
    "transcript": "espero que llegues a tiempo",
    "overall_score": 0.78,
    "grade": "B+",
    "phoneme_breakdown": {
      "es": {"user": "es", "expected": "es", "score": 0.95},
      "ˈpeɾo": {"user": "ˈpeɾo", "expected": "ˈpeɾo", "score": 0.88},
      "ˈʝeɣes": {"user": "ˈjeɣes", "expected": "ˈʝeɣes", "score": 0.62, "issue": "fricative_ll"},
      "ˈtjempo": {"user": "ˈtjembo", "expected": "ˈtjempo", "score": 0.70, "issue": "final_p"}
    },
    "prosody": {
      "stress_accuracy": 0.85,
      "intonation_accuracy": 0.80,
      "rhythm_accuracy": 0.75,
      "speech_rate_wpm": 95,
      "native_range_wpm": "120-160"
    },
    "feedback": "Really solid pronunciation! Your biggest opportunity is the Spanish 'll/y' sound in 'llegues' — in modern Spanish, it's a voiced fricative (like the 'zh' in 'measure'), not the English 'y'. Try putting your tongue behind your bottom teeth and creating friction. Your 'p' at the end of 'tiempo' is slightly closing too hard — in Spanish, it should be softer, almost unaspirated. Practice: say 'tiempo' slowly: tiem-po. Overall, great naturalness! 🎤",
    "areas_to_improve": [
      {"phoneme": "ʝ", "word": "llegues", "severity": "medium"},
      {"phoneme": "p", "word": "tiempo", "severity": "low"}
    ],
    "reference_audio_url": "https://cdn.velmorth.com/reference/espero-que-llegues.mp3"
  }
}
```

### GET /api/v1/ai/recommendations
```json
// Response 200
{
  "data": {
    "primary_recommendations": [
      {
        "type": "lesson",
        "id": "lesson-uuid",
        "title": "Irregular Subjunctive Verbs",
        "reason": "Logical next step after today's lesson",
        "estimated_minutes": 15,
        "xp_available": 80
      },
      {
        "type": "review",
        "items_due": 23,
        "reason": "23 items are due for review — 5 are highly at-risk of being forgotten",
        "estimated_minutes": 12,
        "xp_available": 55
      }
    ],
    "personalized_suggestions": [
      {
        "type": "practice_mode",
        "focus": "speaking",
        "reason": "Your speaking accuracy (59%) is your biggest growth opportunity",
        "action_url": "/practice/speaking"
      },
      {
        "type": "cultural_content",
        "title": "Spanish Football Culture",
        "reason": "You selected 'sports' as an interest in your profile",
        "estimated_minutes": 5
      }
    ]
  }
}
```

---

## 10.7 COMMUNITY / SOCIAL APIs

### GET /api/v1/leaderboard
```json
// Query params: ?period=weekly&language=es&limit=100

// Response 200
{
  "data": {
    "period": "weekly",
    "week_start": "2025-06-02",
    "week_end": "2025-06-08",
    "league": "gold",
    "cohort_size": 50,
    "user_entry": {
      "rank": 12,
      "user_id": "user-uuid",
      "display_name": "Manish S.",
      "avatar_url": "https://...",
      "xp": 3240,
      "streak": 48,
      "country_code": "IN",
      "rank_change": 3
    },
    "top_entries": [
      {
        "rank": 1,
        "user_id": "user-uuid-2",
        "display_name": "María L.",
        "avatar_url": "https://...",
        "xp": 8920,
        "streak": 145,
        "country_code": "ES",
        "rank_change": 0
      }
    ],
    "surrounding_entries": [
      // Ranks 10-14 for context around user's position
    ],
    "promotion_zone": { "rank_cutoff": 10, "label": "Top 10 promote to Platinum" },
    "demotion_zone": { "rank_cutoff": 45, "label": "Bottom 5 demote to Silver" }
  }
}
```

### GET /api/v1/guilds/:guildId
```json
// Response 200
{
  "data": {
    "id": "guild-uuid",
    "name": "Polyglots United",
    "description": "For serious language learners committed to daily study!",
    "emblem_url": "https://...",
    "language_focus": "es",
    "is_open": false,
    "member_count": 47,
    "max_members": 50,
    "guild_tier": "gold",
    "weekly_xp": 1823450,
    "total_xp": 45231890,
    "stats": {
      "rank_in_league": 8,
      "wins_this_season": 3,
      "current_boss_hp_contribution": 45231
    },
    "active_events": [
      {
        "type": "boss_battle",
        "name": "The Vocabulary Golem",
        "hp_current": 234567,
        "hp_max": 1000000,
        "ends_at": "2025-06-10T00:00:00Z"
      }
    ],
    "top_members": [
      {
        "user_id": "user-uuid",
        "display_name": "Ana García",
        "role": "leader",
        "weekly_xp": 12450,
        "avatar_url": "https://..."
      }
    ]
  }
}
```

---

## 10.8 NOTIFICATION APIs

### GET /api/v1/notifications
```json
// Query params: ?page=1&limit=20&unread_only=false

// Response 200
{
  "data": {
    "notifications": [
      {
        "id": "notif-uuid",
        "type": "achievement_unlocked",
        "title": "Achievement Unlocked! 🏆",
        "body": "You earned 'Burning Bright' — 30-day streak!",
        "data": {
          "achievement_id": "ach-uuid",
          "xp_earned": 500
        },
        "is_read": false,
        "created_at": "2025-06-08T08:00:00Z",
        "action_url": "/achievements/ach-uuid"
      },
      {
        "id": "notif-uuid-2",
        "type": "friend_activity",
        "title": "Priya completed Unit 5! 🎉",
        "body": "Give her a cheer!",
        "data": { "friend_id": "friend-uuid" },
        "is_read": true,
        "created_at": "2025-06-08T07:30:00Z"
      }
    ],
    "unread_count": 3,
    "total": 47,
    "page": 1,
    "limit": 20
  }
}
```

### POST /api/v1/notifications/:notifId/read
```json
// Response 200
{ "data": { "id": "notif-uuid", "is_read": true } }
```

---

## 10.9 PAYMENT APIs

### GET /api/v1/payments/plans
```json
// Response 200
{
  "data": {
    "plans": [
      {
        "id": "plan-uuid",
        "code": "premium_monthly",
        "name": "Premium Monthly",
        "price_usd": 9.99,
        "billing_period": "monthly",
        "features": {
          "unlimited_hearts": true,
          "ai_tutor_unlimited": true,
          "offline_lessons": true,
          "xp_multiplier": 1.2,
          "advanced_analytics": true,
          "no_ads": true,
          "streak_repair": true,
          "double_xp_uses_per_day": 2,
          "voice_practice_unlimited": true,
          "story_mode_full": true
        },
        "popular": false
      },
      {
        "id": "plan-uuid-2",
        "code": "premium_annual",
        "name": "Premium Annual",
        "price_usd": 79.99,
        "price_per_month_usd": 6.67,
        "savings_percent": 33,
        "billing_period": "annual",
        "features": { /* same as monthly */ },
        "popular": true,
        "badge": "BEST VALUE"
      }
    ]
  }
}
```

### POST /api/v1/payments/subscriptions
```json
// Request (web/Stripe)
{
  "plan_id": "plan-uuid",
  "payment_method_id": "pm_xxx",  // Stripe payment method
  "promo_code": "LEARN2025"
}

// Response 201
{
  "data": {
    "subscription_id": "sub-uuid",
    "status": "active",
    "plan": "Premium Annual",
    "current_period_end": "2026-06-08T23:59:59Z",
    "client_secret": "pi_xxx_secret_xxx"  // Stripe PaymentIntent for 3DS if needed
  }
}
```

---

## 10.10 WEBSOCKET API

### Connection
```
WSS: wss://api.velmorth.com/ws?token={access_token}

Handshake: JWT validated server-side on connection
Namespaces used via message type prefix
```

### Message Format
```json
{
  "type": "namespace.event_name",
  "data": { /* event-specific payload */ },
  "id": "msg-uuid",
  "timestamp": "2025-06-08T09:35:00.123Z"
}
```

### WebSocket Events (Client → Server)
```json
// Join lesson session room
{ "type": "lesson.join", "data": { "session_id": "session-uuid" } }

// Start AI Tutor conversation session
{ "type": "ai.session.start", "data": { "context": {} } }

// Live leaderboard subscription
{ "type": "leaderboard.subscribe", "data": { "period": "weekly", "language": "es" } }

// Guild raid participation
{ "type": "raid.join", "data": { "raid_id": "raid-uuid" } }
```

### WebSocket Events (Server → Client)
```json
// Real-time leaderboard update
{
  "type": "leaderboard.update",
  "data": {
    "user_rank": 12,
    "rank_change": -1,
    "user_xp": 3240,
    "top_3": [...]
  }
}

// Achievement unlocked (real-time)
{
  "type": "achievement.unlocked",
  "data": {
    "achievement": { "name": "...", "badge_url": "..." },
    "xp_earned": 200
  }
}

// Guild event update
{
  "type": "guild.boss.damage",
  "data": {
    "boss_id": "boss-uuid",
    "hp_before": 234567,
    "hp_after": 233900,
    "contributor": "Ana García",
    "damage_dealt": 667
  }
}

// Friend completed a lesson
{
  "type": "social.friend_activity",
  "data": {
    "friend_id": "friend-uuid",
    "display_name": "Priya K.",
    "activity": "completed_lesson",
    "lesson_title": "Present Tense",
    "xp_earned": 75
  }
}
```

---

## 10.11 GRAPHQL API (Admin + Analytics)

```graphql
# Schema overview for admin/analytics use

type Query {
  # Admin user queries
  user(id: ID!): AdminUser
  users(filter: UserFilter, pagination: Pagination): UserConnection
  
  # Analytics queries (aggregated)
  dailyActiveUsers(dateRange: DateRange!, language: String): TimeSeriesData
  lessonCompletionFunnel(courseId: ID!, dateRange: DateRange!): FunnelData
  retentionCohorts(startDate: Date!, cohortPeriod: CohortPeriod!): RetentionMatrix
  
  # Content management
  contentAnalytics(itemId: ID!): ContentAnalytics
  lowPerformingItems(courseId: ID!, threshold: Float): [ExerciseItem]
}

type Mutation {
  # Admin content management
  createCourse(input: CreateCourseInput!): Course
  updateLesson(id: ID!, input: UpdateLessonInput!): Lesson
  deactivateExerciseItem(id: ID!, reason: String!): ExerciseItem
  
  # Admin user management
  banUser(userId: ID!, reason: String!, expiresAt: DateTime): AdminUser
  grantPremium(userId: ID!, days: Int!, reason: String!): UserProfile
}

type Subscription {
  # Real-time admin monitoring
  liveUserCount: LiveMetrics
  errorRateAlert(threshold: Float!): AlertEvent
}
```

---

## 10.12 ERROR RESPONSE STANDARDS

```json
// Standard error format
{
  "errors": [
    {
      "code": "ERROR_CODE",            // Machine-readable error code
      "message": "Human-readable message",  // For display/logging
      "field": "email",               // Field-specific errors (optional)
      "details": { }                  // Additional context (optional)
    }
  ],
  "meta": {
    "request_id": "req_abc123",
    "timestamp": "2025-06-08T09:35:00Z"
  }
}

// Common error codes
UNAUTHORIZED              // 401: Not authenticated
FORBIDDEN                 // 403: Not authorized for this action
NOT_FOUND                 // 404: Resource not found
VALIDATION_ERROR          // 422: Request body/params failed validation
RATE_LIMIT_EXCEEDED       // 429: Too many requests
INTERNAL_ERROR            // 500: Unexpected server error
SERVICE_UNAVAILABLE       // 503: Service temporarily unavailable

// Feature-specific error codes
LESSON_SESSION_EXPIRED    // Session timed out (> 2 hours inactive)
INSUFFICIENT_GEMS         // Not enough gems for purchase
GUILD_FULL                // Guild has reached member limit
SUBSCRIPTION_REQUIRED     // Feature requires premium subscription
AUDIO_TOO_LONG            // Voice recording exceeds limit
LANGUAGE_NOT_SUPPORTED    // Requested language not available
```
