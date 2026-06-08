# LEARN WITH VELMORTH — SYSTEM ARCHITECTURE
## Section 2: Complete System Architecture

---

## 2.1 ARCHITECTURAL OVERVIEW

Learn With Velmorth is built on a **cloud-native, event-driven, microservices architecture** designed to serve 100 million concurrent users with sub-100ms API response times, 99.99% uptime SLA, and zero-downtime deployments. The architecture is organized into 7 primary layers, each independently scalable, observable, and deployable.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                                │
│  Flutter Mobile (iOS/Android) │ Web (Next.js) │ TV/Watch/Desktop    │
└─────────────────────┬───────────────────────────────────────────────┘
                      │ HTTPS/WSS/gRPC
┌─────────────────────▼───────────────────────────────────────────────┐
│                    API GATEWAY LAYER                                 │
│  Kong Gateway │ Rate Limiter │ Auth Middleware │ Load Balancer       │
│  SSL Termination │ Request Routing │ Circuit Breaker                 │
└─────────────────────┬───────────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────────┐
│                  MICROSERVICES LAYER                                 │
│                                                                     │
│  Auth  │ User  │ Course │ Lesson │ Progress │ AI    │ Community     │
│  Svc   │ Svc   │ Svc    │ Svc    │ Svc      │ Svc   │ Svc          │
│                                                                     │
│  Game  │ Notif │ Voice  │ Media  │ Search   │ Pay   │ Analytics    │
│  Svc   │ Svc   │ Svc    │ Svc    │ Svc      │ Svc   │ Svc          │
└─────────────────────┬───────────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────────┐
│                   INTELLIGENCE LAYER                                 │
│  Learning Engine │ Memory Engine │ AI Tutor │ Recommendation Engine │
│  NLP Pipeline │ Voice AI │ Content Generator │ Prediction Engine    │
└─────────────────────┬───────────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────────┐
│                     DATA LAYER                                      │
│  PostgreSQL │ Redis │ Elasticsearch │ ClickHouse │ Apache Kafka     │
│  MinIO (Object Storage) │ Neo4j (Knowledge Graph)                   │
└─────────────────────┬───────────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────────┐
│               INFRASTRUCTURE LAYER                                  │
│  Kubernetes (GKE) │ Terraform │ NGINX │ Istio Service Mesh         │
│  Prometheus │ Grafana │ Jaeger │ PagerDuty                          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2.2 FRONTEND LAYER

### Technology Stack
- **Primary Client**: Flutter 3.x (Dart) — single codebase for iOS, Android, Web, Desktop
- **Web SSR**: Next.js 14 (for marketing site, SEO, and web app portal)
- **State Management**: Riverpod 2.0 + StateNotifier
- **Local Storage**: Hive (encrypted) + SQLite (via drift)
- **Local AI**: TensorFlow Lite (on-device inference for offline features)
- **Push Notifications**: Firebase Cloud Messaging (FCM) + APNS
- **Real-time**: WebSocket client (via socket_io_client)
- **Offline Sync**: Custom CRDT-based sync layer

### Client-to-Gateway Communication
All client requests are routed through a unified API Gateway. The client maintains:
1. **Primary Connection**: HTTPS REST for standard CRUD operations
2. **WebSocket Connection**: Persistent WSS connection for real-time features (live lessons, leaderboards, AI chat)
3. **gRPC Stream**: For high-frequency, low-latency data (voice analysis, real-time pronunciation feedback)
4. **Background Sync Queue**: For offline-mode data that syncs when connectivity restores

---

## 2.3 API GATEWAY LAYER

### Kong API Gateway Configuration
- **Deployment**: Kong on Kubernetes, 3 replicas per region
- **Plugins Active**: JWT Auth, Rate Limiting, CORS, Request Transform, Response Cache, Bot Detection, IP Restriction
- **Rate Limits by Tier**:
  - Anonymous: 10 req/min
  - Free Tier: 120 req/min
  - Premium: 600 req/min
  - Enterprise API: 10,000 req/min
- **Circuit Breaker**: Activated when downstream service error rate exceeds 5% in 10 seconds; opens circuit for 30 seconds
- **Load Balancing Algorithm**: Least-connections with health-check weighted routing
- **SSL Termination**: TLS 1.3 only; certificates via Let's Encrypt with auto-renewal; HSTS enforced

### Request Routing Rules
```
/api/v1/auth/*           → Auth Service (port 3001)
/api/v1/users/*          → User Service (port 3002)
/api/v1/courses/*        → Course Service (port 3003)
/api/v1/lessons/*        → Lesson Service (port 3004)
/api/v1/progress/*       → Progress Service (port 3005)
/api/v1/ai/*             → AI Orchestration Service (port 3006)
/api/v1/community/*      → Community Service (port 3007)
/api/v1/gamification/*   → Gamification Service (port 3008)
/api/v1/notifications/*  → Notification Service (port 3009)
/api/v1/voice/*          → Voice Service (port 3010)
/api/v1/analytics/*      → Analytics Service (port 3011)
/api/v1/payments/*       → Payment Service (port 3012)
/api/v1/search/*         → Search Service (port 3013)
/ws/*                    → WebSocket Service (port 3014)
/grpc/*                  → gRPC Gateway (port 50051)
```

---

## 2.4 MICROSERVICES LAYER — COMPLETE SERVICE CATALOG

### Service 01: Auth Service (velmorth-auth)
**Responsibility**: All authentication and authorization
**Tech Stack**: NestJS + Passport.js + PostgreSQL + Redis
**Capabilities**:
- JWT access token issuance (15-min TTL) and refresh token management (30-day TTL, rotated on use)
- OAuth 2.0 flows for Google, Apple, Facebook, GitHub login
- TOTP-based 2FA with backup codes
- Email OTP verification
- Biometric authentication delegation (device-side; server validates binding)
- Session management across multiple devices (max 5 active sessions)
- Token blacklisting via Redis SET with TTL
- Suspicious login detection (new device, new country, impossible travel)
- PKCE flow for mobile OAuth

**Internal Communication**: Publishes auth events to Kafka topic `user.auth.events`

---

### Service 02: User Service (velmorth-users)
**Responsibility**: User profile management and Learning DNA
**Tech Stack**: NestJS + PostgreSQL + Redis + MinIO
**Capabilities**:
- CRUD for user profiles, preferences, settings
- Learning DNA profile creation, storage, and real-time updating
- Avatar management (stored in MinIO, CDN-served)
- User relationship graph (friends, study partners, mentors)
- Privacy settings and data export (GDPR compliance)
- Account deletion cascade
- Multi-language profile support

---

### Service 03: Course Service (velmorth-courses)
**Responsibility**: Course catalog, curriculum management, content serving
**Tech Stack**: NestJS + PostgreSQL + Redis + Elasticsearch
**Capabilities**:
- Language course catalog management (100+ languages)
- Curriculum tree management (Units → Chapters → Lessons → Items)
- Content versioning (learners never experience mid-lesson content changes)
- A/B test content variant management
- Content localization and metadata
- Course search and discovery

---

### Service 04: Lesson Service (velmorth-lessons)
**Responsibility**: Dynamic lesson generation and delivery
**Tech Stack**: NestJS + PostgreSQL + Redis + AI Service (gRPC)
**Capabilities**:
- Lesson session creation and management
- Real-time item selection (pulls from AI Recommendation Engine)
- Answer validation (multi-modal: text, voice, choice, arrangement)
- Hint system management
- Lesson state persistence (resume mid-lesson across device restarts)
- XP calculation and emission
- Lesson completion event publishing

---

### Service 05: Progress Service (velmorth-progress)
**Responsibility**: Learning progress tracking and analytics
**Tech Stack**: NestJS + PostgreSQL + ClickHouse + Redis
**Capabilities**:
- Real-time progress tracking per course, unit, lesson, item
- Mastery score calculation and updates
- Streak management (calculation, grace period, freezes, repair)
- XP ledger and level calculation
- Learning velocity calculation
- Weak/strong topic identification
- Progress history and time-series data
- Retention curve modeling per learner

---

### Service 06: AI Orchestration Service (velmorth-ai)
**Responsibility**: Unified AI service routing and orchestration
**Tech Stack**: Python FastAPI + LangChain + LangGraph + Redis
**Sub-components**:
- **Tutor Agent**: Conversational AI with full learning context
- **Lesson Generator**: Generates calibrated lesson content on demand
- **Quiz Generator**: Creates adaptive assessments
- **Story Generator**: Produces narrative immersion content
- **Voice Coach**: Processes audio; returns phoneme-level feedback
- **Recommendation Engine**: Item and content selection
- **Study Planner**: Generates personalized study schedules
- **Progress Predictor**: ML model inference for mastery timeline
- **Motivation Engine**: Context-aware motivational message generation
- **Content Moderator**: Filters user-generated content

**Model Stack**:
- **Gemini 2.5 Pro**: Primary reasoning, lesson generation, conversation
- **Gemini 2.0 Flash**: Fast inference for real-time recommendation
- **Whisper Large v3**: Speech-to-text transcription
- **Custom Pronunciation Model**: Fine-tuned on phoneme classification
- **TensorFlow Serving**: Custom ML models (forgetting curve, mastery prediction)
- **Sentence-BERT**: Semantic similarity for answer validation

---

### Service 07: Community Service (velmorth-community)
**Responsibility**: Social features, guilds, forums, mentorship
**Tech Stack**: NestJS + PostgreSQL + Redis + Elasticsearch
**Capabilities**:
- Guild creation, management, and membership
- Forum threads, comments, reactions
- Study partner matching algorithm
- Mentor/mentee pairing
- Community challenges and events
- Moderation tools and report system
- User-generated content validation pipeline
- Real-time notifications via WebSocket

---

### Service 08: Gamification Service (velmorth-gamification)
**Responsibility**: XP, levels, achievements, challenges, virtual economy
**Tech Stack**: NestJS + PostgreSQL + Redis + RabbitMQ
**Capabilities**:
- XP event processing and ledger management
- Level calculation and unlock triggering
- Achievement condition evaluation and award
- Badge management
- Daily/Weekly mission generation and tracking
- Boss Battle event management
- Season Pass progression
- Virtual currency (Velmorth Gems) ledger
- Leaderboard calculation (Redis Sorted Sets)
- Rank tier management

---

### Service 09: Notification Service (velmorth-notifications)
**Responsibility**: Multi-channel notification delivery
**Tech Stack**: NestJS + RabbitMQ + Redis + FCM + APNS + SendGrid + Twilio
**Capabilities**:
- Push notification dispatch (FCM + APNS)
- Email notifications (SendGrid with custom templates)
- SMS notifications (Twilio, optional)
- In-app notification center
- Notification preference management
- Optimal time delivery (per user, based on historical engagement data)
- Notification A/B testing
- Delivery receipt tracking

---

### Service 10: Voice Service (velmorth-voice)
**Responsibility**: All voice/audio processing
**Tech Stack**: Python FastAPI + Whisper + Custom Phoneme Models + FFmpeg
**Capabilities**:
- Audio ingestion (WebM/MP4/M4A/WAV)
- Speech-to-text transcription (Whisper Large v3)
- Pronunciation accuracy scoring (phoneme-level comparison)
- Prosody analysis (rhythm, stress, intonation)
- Fluency measurement (speech rate, pausing, filled pauses)
- Text-to-speech synthesis (neural voices per language, 40+ voices)
- Audio content generation for lessons
- Real-time voice session streaming (WebRTC)

---

### Service 11: Analytics Service (velmorth-analytics)
**Responsibility**: All event tracking, aggregation, and dashboard data
**Tech Stack**: NestJS + ClickHouse + Apache Kafka + Grafana
**Capabilities**:
- Event ingestion pipeline (10M+ events/day)
- Real-time aggregation via Kafka Streams
- ClickHouse materialized views for sub-second query responses
- Funnel analysis
- Cohort analysis
- A/B test result calculation
- Revenue analytics
- Learning outcome analytics
- Admin dashboard data API
- User-facing analytics API

---

### Service 12: Payment Service (velmorth-payments)
**Responsibility**: Subscription management and payment processing
**Tech Stack**: NestJS + Stripe + App Store Connect + Google Play Billing + PostgreSQL
**Capabilities**:
- Subscription lifecycle management (create, upgrade, downgrade, cancel)
- Multi-provider payment support (Stripe for web, native IAP for mobile)
- Webhook processing from payment providers
- Invoice generation and management
- Promo code and coupon system
- Revenue recognition and ledger
- Refund processing
- Family plan seat management
- Enterprise billing and invoicing
- Tax calculation (Stripe Tax)

---

### Service 13: Search Service (velmorth-search)
**Responsibility**: Full-text and semantic search
**Tech Stack**: NestJS + Elasticsearch 8.x + Sentence-BERT
**Capabilities**:
- Content search (lessons, courses, vocabulary items)
- User search (find friends, study partners)
- Community search (forums, posts)
- Semantic search (meaning-based, not just keyword)
- Search autocomplete and suggestions
- Search analytics (failed searches → content gap identification)

---

### Service 14: Media Service (velmorth-media)
**Responsibility**: Media asset management and delivery
**Tech Stack**: NestJS + MinIO + CloudFront CDN + FFmpeg
**Capabilities**:
- Image upload and optimization (WebP conversion, responsive sizing)
- Audio file management (lesson audio, user recordings)
- Video content management (cultural videos, teacher explanations)
- CDN integration for global low-latency delivery
- Streaming audio/video support
- Avatar and badge image management

---

## 2.5 AI LAYER — COMPLETE INTELLIGENCE ARCHITECTURE

```
┌────────────────────────────────────────────────────────────────────┐
│                    AI ORCHESTRATION LAYER                          │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              LangGraph Orchestrator                          │   │
│  │  Manages stateful AI agent workflows with tool calling       │   │
│  └────────┬──────────┬──────────┬────────────┬─────────────────┘   │
│           │          │          │            │                      │
│    ┌──────▼──┐ ┌─────▼───┐ ┌───▼──────┐ ┌──▼──────────┐          │
│    │ Tutor   │ │ Lesson  │ │  Voice   │ │ Recommend.  │          │
│    │ Agent   │ │ Gen.    │ │  Coach   │ │ Engine      │          │
│    └──────┬──┘ └─────┬───┘ └───┬──────┘ └──┬──────────┘          │
│           └──────────┴──────────┴───────────┘                      │
│                              │                                      │
│    ┌─────────────────────────▼────────────────────────────────┐    │
│    │                  Model Router                             │    │
│    │  Routes to: Gemini 2.5 Pro | Gemini Flash | Fine-tuned  │    │
│    └─────────────────────────┬────────────────────────────────┘    │
│                              │                                      │
│    ┌─────────────────────────▼────────────────────────────────┐    │
│    │              Context Store (Redis + PostgreSQL)           │    │
│    │  Learning DNA │ Session History │ Knowledge State         │    │
│    └──────────────────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────────────────────┘
```

### AI Processing Pipeline
1. Client sends request (with session context) to AI Orchestration Service
2. LangGraph Orchestrator determines agent type and assembles context
3. Context Store fetches: Learning DNA, last 20 interactions, knowledge graph state, current lesson metadata
4. Model Router selects optimal model based on task complexity, cost, and latency requirements
5. Model generates response with tool calls as needed (database queries, content retrieval, external APIs)
6. Response is post-processed (safety filter, quality check, localization)
7. Response cached in Redis (per learner + context hash) for 5 minutes
8. Response returned to client with metadata (confidence, model used, latency)
9. Interaction logged to ClickHouse for model fine-tuning pipeline

---

## 2.6 ANALYTICS LAYER

### Real-Time Analytics Pipeline
```
App Events → Kafka Topics → Stream Processors → ClickHouse → Grafana/Custom Dashboard
              (100+ topics)   (Kafka Streams)    (OLAP DB)    (Visualization)
```

### Event Categories
- **Learning Events**: lesson_started, item_answered, lesson_completed, item_mastered
- **Engagement Events**: app_open, session_started, screen_viewed, feature_used
- **Social Events**: friend_added, guild_joined, challenge_accepted
- **Commerce Events**: subscription_started, payment_failed, trial_converted
- **System Events**: error_occurred, latency_spike, model_inference_time

### ClickHouse Schema Design
- Partitioned by `toYYYYMMDD(event_time)` for time-series queries
- Pre-aggregated materialized views for: DAU/MAU, retention cohorts, learning velocity
- TTL policies: raw events kept 90 days, aggregates kept 3 years

---

## 2.7 GAMIFICATION LAYER

The gamification layer operates as an independent service but is deeply integrated with every other service through an event-driven model:

```
Any Service → Publishes Event → Kafka Topic: gamification.events
                                      ↓
                            Gamification Consumer
                                      ↓
                    ┌─────────────────┴─────────────────┐
                    │  Event Type Router                 │
                    └──┬──────┬──────┬──────┬───────────┘
                       │      │      │      │
                    XP Proc. Achiev. Mission Streak
                    Engine   Engine  Engine  Engine
```

All gamification state is stored in:
- **PostgreSQL**: Source of truth for all balances and awards
- **Redis**: Real-time leaderboard (Sorted Sets), current session XP, active challenges

---

## 2.8 LEARNING ENGINE

The Learning Engine is the intellectual core of Velmorth. It runs as a set of coordinated services within the AI layer:

```
┌───────────────────────────────────────────────────────────────────┐
│                      LEARNING ENGINE                              │
│                                                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐  │
│  │   Memory Engine  │  │  Mastery Engine  │  │ Weakness Detect│  │
│  │  (SRS Scheduler) │  │  (Level Gates)   │  │ (Error Pattern)│  │
│  └────────┬─────────┘  └────────┬─────────┘  └───────┬────────┘  │
│           │                     │                     │           │
│  ┌────────▼─────────────────────▼─────────────────────▼────────┐  │
│  │              Knowledge Graph (Neo4j)                         │  │
│  │  Nodes: Concepts, Words, Rules, Examples                     │  │
│  │  Edges: prerequisite_of, related_to, confusable_with         │  │
│  │  Per-User Layer: mastery_score, last_reviewed, error_count   │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                │                                   │
│  ┌─────────────────────────────▼────────────────────────────────┐  │
│  │             Personalization Engine                            │  │
│  │  Selects next items based on: SRS schedule, mastery gaps,    │  │
│  │  current session goals, user energy level, time available    │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘
```

---

## 2.9 RECOMMENDATION ENGINE

The Recommendation Engine determines what content each learner sees next, at every moment:

### Input Signals
1. Current Learning DNA profile (cognitive style, preferred difficulty)
2. Knowledge graph state (mastered nodes, gap nodes, at-risk nodes)
3. SRS schedule (items due for review, urgency scores)
4. Session context (session goal, time remaining, energy level)
5. Historical performance (recent accuracy, error types, completion rates)
6. Social signals (what learners with similar profiles found effective)
7. Curriculum sequence requirements (prerequisite graph traversal)

### Algorithm
1. **Candidate Generation**: Query knowledge graph for 50 candidate items (mix of new and review)
2. **Scoring**: Each candidate scored on: urgency (SRS due-ness), learning value, difficulty match, variety
3. **Diversity Filter**: Ensures no 2 consecutive items test the same skill type
4. **A/B Injection**: 5% of sessions inject experimental content for model improvement
5. **Output**: Ordered list of 15-25 items for the session

---

## 2.10 VOICE ENGINE

### Architecture
```
Client Audio Input (WebRTC/WebM)
    ↓
Voice Service (Python FastAPI)
    ↓
Audio Pre-processing (FFmpeg normalization, noise reduction)
    ↓
┌───────────────┬────────────────┬─────────────────────┐
│  STT Engine   │ Phoneme Analyzer│ Prosody Analyzer    │
│  (Whisper v3) │ (Custom CNN)    │ (Librosa + custom)  │
└───────┬───────┴────────┬────────┴──────────┬──────────┘
        │                │                   │
        ▼                ▼                   ▼
  Transcript       Phoneme Scores      Prosody Scores
        │                │                   │
        └────────────────┴───────────────────┘
                         │
                 Score Aggregator
                         │
              Feedback Generator (LLM)
                         │
              Client: Transcript + Score + Feedback
```

### Pronunciation Scoring Components
- **Phoneme Accuracy**: Each phoneme scored 0-100 against reference speaker
- **Word Stress**: Correct syllable stress in multi-syllable words
- **Sentence Rhythm**: Appropriate weak/strong syllable pattern
- **Intonation**: Rising/falling patterns matching language norms
- **Connected Speech**: Linking, elision, assimilation patterns
- **Speech Rate**: Words per minute vs. native range
- **Overall Fluency Score**: Weighted composite of all components

---

## 2.11 REAL-TIME SERVICES

### WebSocket Architecture
- **Server**: NestJS WebSocket Gateway with Socket.io
- **Scaling**: Redis Pub/Sub for cross-server message routing (multiple server instances)
- **Authentication**: JWT validated on handshake; connection rejected if invalid
- **Namespaces**:
  - `/lesson` — Real-time lesson session state
  - `/leaderboard` — Live leaderboard updates
  - `/chat` — AI Tutor conversation
  - `/raid` — Live guild raid events
  - `/notifications` — Real-time notification delivery

### Event Flow for Real-Time Lesson
```
Client → WS Connect → Auth Handshake → Join lesson:{sessionId} room
Client sends: ANSWER_SUBMITTED {itemId, answer, timestamp}
Server validates → computes score → publishes to Redis
Redis Pub/Sub → broadcasts to all subscribers (handles multi-server)
Client receives: ANSWER_RESULT {correct, score, explanation, nextItem}
```

---

## 2.12 NOTIFICATION SERVICES

### Optimal Send Time Algorithm
For each user, we maintain a `notification_engagement_profile`:
- Hour-of-day engagement scores (0-23 hours, based on historical open rates)
- Day-of-week engagement scores
- Last active time
- Notification fatigue score (suppressed if user is overwhelmed)

When dispatching a "nudge" notification:
1. Query user's engagement profile
2. If user is currently active: suppress (they don't need a nudge)
3. If best send time is within next 2 hours: schedule for exact time
4. If best send time is more than 2 hours away: add to delayed queue with scheduled dispatch

### Notification Types and Templates
- **Streak at Risk**: "Your {N}-day streak ends in 2 hours. Jump in for 5 minutes!"
- **Friend Activity**: "{Friend} just beat your score on Unit 3. Time to reclaim your spot!"
- **Guild Mission**: "Your guild needs you — only 3 hours left on today's mission!"
- **Perfect Recall Moment**: "Your brain is ready to lock in 'subjunctive mood' permanently. Review now!"
- **Achievement Unlocked**: Celebration notification with animation
- **New Content**: "New story available: 'The Last Conversation' — your level, your language"
- **Study Reminder**: Personalized based on committed schedule

---

## 2.13 AUTHENTICATION SERVICES

### Auth Flow (Complete)
```
Mobile App → POST /auth/google-login {idToken}
    ↓
Auth Service validates Google ID Token via Google tokeninfo API
    ↓
Check if user exists (by Google sub ID)
    ├── Existing user: fetch profile, generate tokens
    └── New user: create account, trigger onboarding flow
    ↓
Generate: access_token (JWT, 15min), refresh_token (UUID, stored in DB, 30 days)
    ↓
Return: {access_token, refresh_token, user_profile, onboarding_required}
    ↓
Client stores: access_token in memory, refresh_token in secure storage
    ↓
On 401 response: auto-refresh using refresh_token → new access_token
```

---

## 2.14 CLOUD INFRASTRUCTURE

### Google Cloud Platform Primary Architecture

**Regions**: 
- Primary: us-central1 (Iowa) — main processing
- Secondary: europe-west1 (Belgium) — European users + DR
- Tertiary: asia-southeast1 (Singapore) — APAC users

**Core Services Used**:
- **GKE Autopilot**: Kubernetes cluster management (per-region)
- **Cloud SQL (PostgreSQL 16)**: Primary transactional database with read replicas
- **Cloud Memorystore (Redis)**: Managed Redis clusters
- **Cloud Pub/Sub**: Kafka-compatible messaging (Confluent Cloud for advanced features)
- **Cloud Storage**: Object storage (MinIO on GKE for cross-cloud portability)
- **Cloud CDN + Cloud Load Balancing**: Global content delivery
- **Cloud Armor**: DDoS protection and WAF
- **Cloud KMS**: Encryption key management
- **Secret Manager**: Secrets and configuration management
- **Cloud Run**: Serverless container execution for batch AI workloads
- **Vertex AI**: Custom ML model training and serving
- **BigQuery**: Data warehouse for analytics exports

### Network Architecture
```
Internet → Cloud Armor (DDoS/WAF) → Global Load Balancer
    → Regional Load Balancers (3 regions)
    → GKE Ingress (NGINX Ingress Controller)
    → Kong API Gateway Pods
    → Service Mesh (Istio) → Microservice Pods
```

### Kubernetes Cluster Configuration (per region)
- **Node Pools**:
  - `general-pool`: n2-standard-8 (8 CPU, 32GB RAM), min 3, max 50 nodes
  - `ai-pool`: n2-highcpu-16 (16 CPU, 32GB RAM), min 2, max 20 nodes
  - `memory-pool`: n2-highmem-8 (8 CPU, 64GB RAM), min 2, max 10 nodes (PostgreSQL, Redis)
- **Cluster Autoscaler**: Enabled, scale-up cooldown 60s, scale-down cooldown 300s
- **Pod Disruption Budgets**: Minimum 1 pod available per service during maintenance
- **Resource Quotas**: Per-namespace CPU and memory limits enforced

---

## 2.15 MICROSERVICES COMMUNICATION PATTERNS

### Synchronous Communication (Request/Response)
- **Protocol**: REST (HTTP/2) via service mesh for inter-service calls
- **Format**: JSON with Protobuf for high-frequency calls
- **Discovery**: Kubernetes DNS + Istio service registry
- **Retry Policy**: 3 retries with exponential backoff (100ms, 200ms, 400ms)
- **Timeout Policy**: 3 seconds for standard calls, 30 seconds for AI inference

### Asynchronous Communication (Event-Driven)
- **Message Broker**: Apache Kafka (Confluent Cloud) + RabbitMQ for local task queuing
- **Topic Naming Convention**: `{service}.{entity}.{event}` (e.g., `lessons.session.completed`)
- **Consumer Groups**: Each service has its own consumer group per topic
- **Message Ordering**: Keyed by userId for per-user event ordering
- **Retention**: 7 days for all topics; 30 days for critical business events
- **Dead Letter Queue**: All failed messages after 3 retries → DLQ for manual inspection

### Service Mesh (Istio)
- **mTLS**: All inter-service communication encrypted with mutual TLS automatically
- **Traffic Management**: Weighted routing for canary deployments (95/5 split)
- **Observability**: Automatic distributed tracing via Jaeger injection
- **Circuit Breaking**: Envoy sidecar enforces circuit breakers per service

---

## 2.16 EVENT-DRIVEN ARCHITECTURE

### Kafka Topic Catalog (Complete)
```
user.registered                  → [User Svc, Notif Svc, AI Svc, Analytics]
user.profile.updated             → [User Svc, AI Svc]
user.deleted                     → [All services - cascade delete]
auth.login.success               → [Analytics, Gamification]
auth.login.failed                → [Auth Svc, Analytics, Security]
lesson.session.started           → [Analytics, Gamification]
lesson.item.answered             → [Progress Svc, Analytics, Gamification, Memory Engine]
lesson.session.completed         → [Progress Svc, Analytics, Gamification, Notif Svc]
progress.mastery.achieved        → [Gamification, Notif Svc, Analytics, AI Svc]
progress.streak.updated          → [Gamification, Notif Svc]
progress.streak.broken           → [Gamification, Notif Svc, AI Coach]
gamification.xp.earned           → [Progress Svc, Leaderboard, Analytics]
gamification.level.up            → [Notif Svc, Analytics, AI Svc]
gamification.achievement.unlocked→ [Notif Svc, Analytics]
community.guild.event.started    → [All guild members via Notif Svc]
ai.lesson.generated              → [Lesson Svc, Analytics]
payment.subscription.created     → [User Svc, Analytics, Notif Svc]
payment.subscription.cancelled   → [User Svc, Analytics, Notif Svc]
voice.pronunciation.analyzed     → [Progress Svc, Analytics, AI Coach]
```

---

## 2.17 CACHING ARCHITECTURE

### Redis Caching Strategy (Multi-Layer)

**Layer 1: Application-Level Cache (Redis Cluster)**
```
Key Pattern                          TTL     Purpose
user:{userId}:profile               15min   User profile data
user:{userId}:learning_dna          1hr     Learning DNA (slow-changing)
user:{userId}:progress_summary      5min    Dashboard data
user:{userId}:active_session        30min   Current lesson session state
course:{courseId}:metadata          24hr    Course structure (rarely changes)
lesson:{lessonId}:content           1hr     Lesson content items
leaderboard:{period}:{language}     1min    Leaderboard rankings (sorted set)
achievement:{userId}:list           15min   User achievements
auth:refresh:{token}:valid          30days  Refresh token validity
rate_limit:{userId}:{endpoint}      1min    Rate limit counters
ai:context:{userId}:recent          5min    Recent AI conversation context
```

**Layer 2: CDN Cache (CloudFront)**
- Static assets: 365-day TTL with content-hashed URLs
- API responses for public content (course catalog): 1-hour TTL
- Media files (audio, images): 30-day TTL

**Layer 3: In-App Cache (Flutter)**
- Lesson content downloaded ahead: 3 lessons pre-cached
- User profile: 1-hour in-memory TTL
- Static assets: Persistent app cache
- Offline mode data: SQLite cache

**Cache Invalidation Strategy**:
- **Write-through**: On any write, update both DB and cache simultaneously
- **Event-driven invalidation**: Kafka consumer listens for data change events and purges affected keys
- **TTL expiry**: Safety net for cache consistency

---

## 2.18 SECURITY ARCHITECTURE

### Defense-in-Depth Model
```
Layer 1: Network - Cloud Armor WAF, DDoS protection, IP allowlisting for admin
Layer 2: Transport - TLS 1.3 everywhere, HSTS, certificate pinning on mobile
Layer 3: Application - JWT auth, RBAC, input validation, OWASP top 10 mitigations
Layer 4: Data - AES-256 at rest, field-level encryption for PII, column masking
Layer 5: Identity - MFA, OAuth PKCE, session management, suspicious login detection
Layer 6: API - Rate limiting, payload size limits, query depth limiting (GraphQL)
Layer 7: Infrastructure - Pod security policies, network policies, secrets management
Layer 8: Monitoring - SIEM integration, anomaly detection, automated alerting
```

### Encryption Standards
- **Data at Rest**: AES-256 (Google Cloud KMS managed keys)
- **Data in Transit**: TLS 1.3 (minimum), HSTS with 1-year max-age
- **Passwords**: bcrypt (cost factor 12) — though social login preferred
- **PII Fields**: Application-level encryption for email, phone, name (Cloud KMS)
- **JWT Signing**: RS256 (RSA 2048-bit) for access tokens; HS512 for refresh tokens

---

## 2.19 MONITORING ARCHITECTURE

### Observability Stack (Three Pillars)

**Metrics (Prometheus + Grafana)**
- Node-level metrics: CPU, memory, disk, network (via node_exporter)
- Application metrics: Request rate, error rate, latency (P50, P95, P99)
- Business metrics: DAU, lesson completions, XP earned, subscription conversions
- AI metrics: Inference latency, model accuracy, token costs
- Custom dashboards: One per microservice + Executive dashboard
- Alerting: PagerDuty integration, escalation policies per severity

**Logs (Structured Logging → Cloud Logging → BigQuery)**
- All services log structured JSON to stdout
- Log levels: DEBUG (dev only), INFO (all envs), WARN, ERROR, FATAL
- Correlation IDs: Every request tagged with X-Correlation-ID (propagated through service calls)
- Log retention: 30 days hot (Cloud Logging), 1 year cold (Cloud Storage)

**Traces (Jaeger / Cloud Trace)**
- OpenTelemetry SDK in all services
- Distributed trace for every API request across all microservices
- Sampling: 100% for errors, 10% for normal traffic, 1% for high-volume endpoints
- Trace storage: 7 days, queryable by correlation ID, user ID, endpoint

### SLO Definitions
```
Service          SLO Metric                 Target   Error Budget
API Gateway      Request success rate       99.95%   21.9 min/month
Auth Service     Login success rate         99.99%   4.4 min/month
Lesson Service   Lesson load time < 500ms   99.5%    3.6 hr/month
AI Tutor         Response time < 2s         99%      7.3 hr/month
Voice Service    Analysis time < 3s         98%      14.6 hr/month
Database         Query time < 100ms (P99)   99.9%    43.8 min/month
```

---

## 2.20 DISASTER RECOVERY ARCHITECTURE

### Recovery Objectives
- **RTO** (Recovery Time Objective): 15 minutes for API services, 1 hour for full system
- **RPO** (Recovery Point Objective): 5 minutes for transactional data, 1 hour for analytics

### Backup Strategy
- **PostgreSQL**: Continuous WAL streaming to Cloud Storage; daily full snapshots; 30-day retention
- **Redis**: RDB snapshots every 15 minutes + AOF; stored in Cloud Storage
- **MinIO**: Cross-region replication to secondary storage bucket
- **Kafka**: Multi-region topic replication (Confluent Cloud MRC)
- **Elasticsearch**: Daily snapshots to Cloud Storage

### Failover Architecture
- **Active-Passive** for databases: Automated failover via Cloud SQL (< 60 seconds)
- **Active-Active** for application tier: Traffic routed to healthy regions automatically via Global Load Balancer
- **Chaos Engineering**: Monthly automated chaos experiments using Chaos Monkey to validate resilience

### DR Runbooks
All DR scenarios documented with step-by-step runbooks:
- Region-level outage: Traffic fails over to secondary region in < 5 minutes
- Database corruption: Point-in-time recovery to last known good state
- Cache stampede: Gradual cache warming procedure, circuit breaker activation
- AI service degradation: Fallback to pre-generated lesson pool (static content mode)

---

## 2.21 SCALING ARCHITECTURE

### Horizontal Scaling (Pod Level)
- **HPA** (Horizontal Pod Autoscaler): CPU > 70% or custom metric (requests/second)
- **VPA** (Vertical Pod Autoscaler): Recommends right-sizing based on historical usage
- **Cluster Autoscaler**: Adds nodes when pod scheduling fails; removes underutilized nodes

### Vertical Scaling (Database Level)
- Read replicas for all read-heavy services (Progress, Analytics, Community)
- **PgBouncer** connection pooling: max 10,000 connections pooled to 100 PostgreSQL connections
- **Elasticsearch**: Auto-sharding, allocation awareness across availability zones
- **ClickHouse**: Distributed table engine with manual sharding by userId hash

### Traffic Patterns and Peak Handling
- **Daily peaks**: 8-10 AM and 7-9 PM local time per region; auto-scaled 2 hours before predicted peak
- **Event peaks**: Seasonal events, global challenges; pre-scaled 24 hours in advance
- **Viral growth spikes**: Circuit breakers prevent cascade failure; graceful degradation activates (reduce AI features first, then social, then analytics; core learning always available)

### Capacity Planning
Target: 100M users, 10M DAU, 1M concurrent
- API Gateway: 100,000 req/second peak → 50 pods × 2,000 req/s/pod
- WebSocket: 1M concurrent connections → Redis Pub/Sub cluster, 20 WS servers × 50K conn each
- Database: 100K queries/second → 5 read replicas + connection pooling
- AI Inference: 50K concurrent AI sessions → GPU node pool (NVIDIA T4) with 10 replicas

---

## 2.22 GLOBAL DEPLOYMENT ARCHITECTURE

### Multi-Region Strategy
```
Global DNS (Cloud DNS with latency-based routing)
    ↓
Global Load Balancer (Anycast IP)
    ↓
Regional Anycast Edge (Cloud CDN / Cloud Armor)
    ↓
Regional Clusters:
    us-central1: 40% traffic (Americas)
    europe-west1: 35% traffic (EMEA)
    asia-southeast1: 25% traffic (APAC)
    ↓
Regional PostgreSQL (primary + 2 read replicas each)
    ↓
Global PostgreSQL replication (async, < 100ms replication lag)
```

### Data Residency Compliance
- **EU Users**: All PII data stored exclusively in europe-west1 (GDPR)
- **Brazil Users**: Data sovereignty via São Paulo region expansion
- **India Users**: Data stored in asia-south1 when required by future regulations
- **China Users**: Separate deployment via local partnership (when applicable)

### Content Delivery Optimization
- Language-specific content pre-warmed at edge for top 10 languages
- Audio files served from nearest PoP with < 50ms latency
- Dynamic content personalized at origin with 5-second browser cache
- Static app bundles: 30-day CDN cache, version-locked URLs
