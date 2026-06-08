# LEARN WITH VELMORTH — LEARNING ENGINE
## Section 6: Complete Learning Engine Design

---

## 6.1 ADAPTIVE LEARNING ENGINE OVERVIEW

The Velmorth Learning Engine is the most sophisticated component of the entire system. It is a collection of interconnected algorithmic engines that work together to create a personalized, scientifically-optimal learning experience for each user. No two users ever see the same learning path.

### Architecture
```
┌─────────────────────────────────────────────────────────────────┐
│                    LEARNING ENGINE CORE                         │
│                                                                 │
│  Input signals ──→ Knowledge Graph ──→ Item Selector           │
│       │                  │                   │                  │
│       │           Memory Engine         Difficulty Engine       │
│       │                  │                   │                  │
│  Mastery Engine ←─ Weakness Detector ←── Answer Processor      │
│       │                                       │                 │
│  Personalization Engine ←─ Learning DNA ←─────┘               │
│       │                                                         │
│  Next Item Generator ──→ Content Delivery Layer                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6.2 MEMORY ENGINE — ENHANCED SPACED REPETITION

### Foundation: Modified SM-18 Algorithm

The Memory Engine tracks every individual learning item (vocabulary word, grammar rule, phoneme, cultural fact) as a **Memory Object** with the following state:

```python
class MemoryObject:
    item_id: str
    user_id: str
    
    # SRS Core State
    easiness_factor: float  # E-factor: 1.3 to 3.0 (default 2.5)
    interval: int           # Days until next review
    repetitions: int        # Number of successful reviews in sequence
    
    # Velmorth Enhanced Fields
    emotional_encoding_strength: float  # 0.0-1.0 (high-emotion sessions encode better)
    contextual_diversity: float         # 0.0-1.0 (practiced in many contexts = more stable)
    interference_coefficient: float     # Risk of confusion with similar items
    modality_coverage: dict             # {visual: bool, auditory: bool, production: bool}
    
    # History
    review_history: List[ReviewRecord]  # All past reviews
    first_learned: datetime
    last_reviewed: datetime
    next_review: datetime
    
    # Forgetting Curve Parameters (per-item, per-user)
    stability: float         # S: how long this specific memory lasts (days)
    retrievability: float    # R: current probability of successful recall (0.0-1.0)
    difficulty: float        # D: how inherently hard this item is (0.0-1.0)
```

### Scheduling Algorithm

```python
def calculate_next_review(memory_obj: MemoryObject, response_quality: int) -> datetime:
    """
    response_quality: 0=forgot completely, 1=wrong, 2=wrong but remember, 
                      3=correct with difficulty, 4=correct, 5=perfect instant recall
    """
    
    # Step 1: Update easiness factor
    ef = memory_obj.easiness_factor
    new_ef = ef + (0.1 - (5 - response_quality) * (0.08 + (5 - response_quality) * 0.02))
    new_ef = max(1.3, new_ef)  # Never below 1.3
    
    # Step 2: Determine new interval
    if response_quality < 3:
        # Failed review: reset to beginning but factor in how close they were
        new_interval = 1 if response_quality == 0 else 1
        new_repetitions = 0
    else:
        # Successful review
        if memory_obj.repetitions == 0:
            new_interval = 1
        elif memory_obj.repetitions == 1:
            new_interval = 6
        else:
            new_interval = round(memory_obj.interval * new_ef)
        new_repetitions = memory_obj.repetitions + 1
    
    # Step 3: Apply Velmorth enhancements
    
    # Emotional encoding bonus: Strong emotion → longer initial interval
    emotional_multiplier = 1 + (memory_obj.emotional_encoding_strength * 0.2)
    
    # Contextual diversity bonus: Practiced in many contexts → more stable
    diversity_multiplier = 1 + (memory_obj.contextual_diversity * 0.15)
    
    # Interference penalty: Easily confused items reviewed sooner
    interference_penalty = 1 - (memory_obj.interference_coefficient * 0.10)
    
    # Modality coverage bonus: Multi-modal practice → more stable
    modality_coverage_score = sum(memory_obj.modality_coverage.values()) / 3
    modality_multiplier = 1 + (modality_coverage_score * 0.10)
    
    # Optimal time of day adjustment
    optimal_hour = get_user_optimal_review_hour(memory_obj.user_id)
    adjusted_interval = new_interval * emotional_multiplier * diversity_multiplier * \
                        interference_penalty * modality_multiplier
    
    # Schedule at user's optimal time on the review day
    review_date = datetime.now() + timedelta(days=adjusted_interval)
    review_datetime = review_date.replace(hour=optimal_hour, minute=0)
    
    return review_datetime, new_ef, new_interval, new_repetitions
```

### FSRS Integration (2024 Research)
In parallel with SM-18, Velmorth also models memory using the **Free Spaced Repetition Scheduler (FSRS 5.0)** — the state-of-the-art algorithm. The system runs both and uses ensemble prediction:

```python
def ensemble_next_review(sm18_result, fsrs_result, user_profile):
    """
    Blend SM-18 and FSRS predictions based on user history length
    """
    if user_profile.total_reviews < 100:
        # New user: trust FSRS (better initial estimates)
        weight_fsrs = 0.8
        weight_sm18 = 0.2
    elif user_profile.total_reviews < 1000:
        weight_fsrs = 0.6
        weight_sm18 = 0.4
    else:
        # Experienced user: equal trust (both have good data)
        weight_fsrs = 0.5
        weight_sm18 = 0.5
    
    blended_interval = (fsrs_result.interval * weight_fsrs + 
                       sm18_result.interval * weight_sm18)
    return round(blended_interval)
```

---

## 6.3 KNOWLEDGE GRAPH

### Technology: Neo4j Graph Database
Every language is modeled as a **Knowledge Graph** — a network of interconnected concepts, vocabulary, grammar rules, phonemes, and cultural notes.

### Node Types
```cypher
// Vocabulary Node
(:Word {
  id: UUID,
  text: String,
  language: String,
  translation: String,
  frequency_rank: Integer,    // COCA/comparable corpus rank
  cefr_level: String,         // A1, A2, B1, B2, C1, C2
  part_of_speech: String,
  semantic_field: String,     // e.g., "food", "emotions", "travel"
  formal_register: Boolean,
  audio_url: String,
  example_sentences: List<String>,
  image_url: String,
  mnemonic: String            // AI-generated memory aid
})

// Grammar Rule Node
(:GrammarRule {
  id: UUID,
  name: String,               // e.g., "Spanish Subjunctive Present"
  language: String,
  cefr_level: String,
  explanation: String,
  short_explanation: String,
  examples: List<String>,
  common_errors: List<String>,
  related_rules: List<UUID>
})

// Phoneme Node
(:Phoneme {
  id: UUID,
  ipa_symbol: String,
  language: String,
  description: String,
  similar_sounds: List<String>,
  native_language_mapping: Map<String, String>  // Maps to phonemes in other langs
})

// Cultural Note Node
(:CulturalNote {
  id: UUID,
  title: String,
  language: String,
  country: String,
  category: String,           // "social_customs", "food", "history", "etiquette"
  content: String,
  importance: String          // "essential", "useful", "interesting"
})

// User-Knowledge Node (Per-User State)
(:UserKnowledgeState {
  user_id: UUID,
  item_id: UUID,
  item_type: String,
  mastery_score: Float,       // 0.0-1.0
  srs_state: JSON,            // Serialized MemoryObject
  times_practiced: Integer,
  times_correct: Integer,
  last_practiced: DateTime,
  next_review: DateTime,
  is_introduced: Boolean
})
```

### Edge Types
```cypher
// Prerequisite relationship
(:GrammarRule)-[:REQUIRES]->(:GrammarRule)
// "Present Perfect requires Present Simple to be learned first"

// Semantic relationship  
(:Word)-[:SYNONYM_OF]->(:Word)
(:Word)-[:ANTONYM_OF]->(:Word)
(:Word)-[:MEMBER_OF]->(:SemanticField)
(:Word)-[:COLLOCATION_WITH {strength: Float}]->(:Word)

// Confusability relationship (critical for interference-aware scheduling)
(:Word)-[:EASILY_CONFUSED_WITH {reason: String}]->(:Word)
// e.g., "ser" and "estar" in Spanish

// Usage relationship
(:GrammarRule)-[:USED_WITH]->(:Word)
(:Word)-[:EXEMPLIFIES]->(:GrammarRule)

// Cultural context
(:Word)-[:HAS_CULTURAL_CONTEXT]->(:CulturalNote)

// Phoneme relationship
(:Word)-[:CONTAINS_PHONEME]->(:Phoneme)
(:Phoneme)-[:SIMILAR_TO {difficulty_for: String}]->(:Phoneme)

// Curriculum relationship
(:LessonNode)-[:TEACHES]->(:Word|GrammarRule|Phoneme|CulturalNote)
(:LessonNode)-[:NEXT]->(:LessonNode)
(:LessonNode)-[:REQUIRES]->(:LessonNode)
```

### Knowledge Graph Query Examples
```cypher
// Find all vocabulary a user needs to review today
MATCH (u:User {id: $userId})-[:HAS_STATE]->(ks:UserKnowledgeState)-[:FOR]->(w:Word)
WHERE ks.next_review <= datetime() AND ks.is_introduced = true
ORDER BY ks.next_review ASC
LIMIT 50
RETURN w, ks

// Find vocabulary gaps (words at user's CEFR level not yet introduced)
MATCH (w:Word {language: $lang, cefr_level: $userCefrLevel})
WHERE NOT EXISTS {
  MATCH (u:User {id: $userId})-[:HAS_STATE]->(ks:UserKnowledgeState {is_introduced: true})-[:FOR]->(w)
}
RETURN w ORDER BY w.frequency_rank LIMIT 20

// Find items likely to interfere with upcoming review
MATCH (u:User {id: $userId})-[:HAS_STATE]->(ks1:UserKnowledgeState)-[:FOR]->(w1:Word)
MATCH (w1)-[:EASILY_CONFUSED_WITH]->(w2:Word)
MATCH (u)-[:HAS_STATE]->(ks2:UserKnowledgeState)-[:FOR]->(w2)
WHERE ks1.next_review <= datetime() + duration('P7D')
  AND ks2.next_review <= datetime() + duration('P7D')
RETURN w1, w2, ks1.next_review, ks2.next_review
// Use this to schedule them APART to prevent interference
```

---

## 6.4 LEARNING DNA SYSTEM

### What is Learning DNA?
Learning DNA is Velmorth's proprietary cognitive profile that captures the unique learning characteristics of each user. It is created during onboarding and continuously refined with every session.

### DNA Components (Complete)

```python
class LearningDNA:
    user_id: str
    version: int
    last_updated: datetime
    
    # Cognitive Style Profile
    cognitive_style: CognitiveStyleProfile
    
    # Memory Profile
    memory_profile: MemoryProfile
    
    # Motivation Profile
    motivation_profile: MotivationProfile
    
    # Performance Profile
    performance_profile: PerformanceProfile
    
    # Schedule Profile
    schedule_profile: ScheduleProfile
    
    # Error Pattern Profile
    error_profile: ErrorProfile

class CognitiveStyleProfile:
    primary_style: str          # "visual", "auditory", "kinesthetic", "reading_writing"
    secondary_style: str
    learning_modality_weights: dict  # {"visual": 0.4, "auditory": 0.35, "kinesthetic": 0.25}
    context_dependency: float        # 0.0=abstract_OK, 1.0=needs_context_always
    chunk_size_preference: int       # Items per chunk: 3-7
    elaboration_preference: float    # 0.0=brief, 1.0=wants_full_explanation
    
class MemoryProfile:
    acquisition_speed: float         # 0.0-1.0 (how fast they learn new items)
    consolidation_speed: float       # 0.0-1.0 (how fast memories stabilize)
    decay_rate: float                # How quickly they forget (multiplier on SRS intervals)
    retroactive_interference_sensitivity: float  # How much new learning interferes with old
    sleep_benefit_factor: float      # Improvement from sleep (post-sleep review performance)
    optimal_session_length: int      # Minutes (5-90)
    max_new_items_per_session: int   # 3-15
    review_to_new_ratio: float       # Optimal: e.g., 0.7 = 70% review, 30% new
    
class MotivationProfile:
    primary_motivator: str           # "achievement", "social", "streak", "story", "challenge"
    gamification_responsiveness: float  # 0.0=doesn't care, 1.0=highly responsive
    competitive_preference: float    # 0.0=solo, 1.0=loves competition
    social_accountability_preference: float
    challenge_tolerance: float       # 0.0=prefers easy, 1.0=loves hard
    novelty_preference: float        # How much they like variety vs. routine
    reward_sensitivity: str          # "variable_ratio", "fixed_ratio", "immediate"
    
class PerformanceProfile:
    accuracy_by_type: dict           # {"multiple_choice": 0.85, "translation": 0.62, ...}
    accuracy_by_skill: dict          # {"vocabulary": 0.80, "grammar": 0.65, ...}
    accuracy_trend: str              # "improving", "stable", "declining"
    speed_percentile: float          # Response speed vs. cohort
    consistency_score: float         # How consistent (low variance) in performance
    
class ScheduleProfile:
    peak_performance_hours: List[int]  # Hours of day with best accuracy
    optimal_session_start: int          # Best hour to start studying
    session_length_sweet_spot: int      # Minutes before attention dips
    days_of_week_engagement: dict       # {"monday": 0.8, "tuesday": 0.9, ...}
    
class ErrorProfile:
    common_error_types: List[ErrorPattern]
    persistent_confusions: List[Tuple[str, str]]  # e.g., [("ser", "estar")]
    improvement_velocity: dict  # {error_type: trend}
    pronunciation_weak_phonemes: List[str]  # IPA phonemes they consistently miss
```

### DNA Update Algorithm
```python
def update_learning_dna(dna: LearningDNA, session: LessonSession) -> LearningDNA:
    """
    Updates DNA after each session using exponential moving average
    New value = 0.85 * old_value + 0.15 * session_observation
    (0.85 gives old data 85% weight — prevents rapid oscillation)
    """
    alpha = 0.15  # Learning rate for DNA updates
    
    # Update memory profile
    session_acquisition = calculate_acquisition_speed(session.new_items, session.correct_new)
    dna.memory_profile.acquisition_speed = (
        (1 - alpha) * dna.memory_profile.acquisition_speed + 
        alpha * session_acquisition
    )
    
    # Update performance profile
    for exercise_type, accuracy in session.accuracy_by_type.items():
        current = dna.performance_profile.accuracy_by_type.get(exercise_type, 0.5)
        dna.performance_profile.accuracy_by_type[exercise_type] = (
            (1 - alpha) * current + alpha * accuracy
        )
    
    # Update schedule profile (session start time → peak performance correlation)
    hour = session.start_time.hour
    session_accuracy = session.overall_accuracy
    dna.schedule_profile.peak_performance_hours = update_hour_weights(
        dna.schedule_profile.peak_performance_hours, hour, session_accuracy, alpha
    )
    
    # Update error profile
    new_errors = extract_error_patterns(session.wrong_answers)
    dna.error_profile.common_error_types = merge_error_patterns(
        dna.error_profile.common_error_types, new_errors, alpha
    )
    
    dna.version += 1
    dna.last_updated = datetime.now()
    return dna
```

---

## 6.5 WEAKNESS DETECTION ENGINE

```python
class WeaknessDetector:
    
    def detect_weaknesses(self, user_id: str, time_window_days: int = 30) -> WeaknessReport:
        # Fetch recent performance data
        sessions = self.get_sessions(user_id, days=time_window_days)
        answers = self.get_answers(user_id, days=time_window_days)
        
        weaknesses = []
        
        # 1. Skill-level weaknesses
        accuracy_by_skill = aggregate_accuracy_by_skill(answers)
        for skill, accuracy in accuracy_by_skill.items():
            if accuracy < 0.65:  # Below 65% accuracy = weakness
                weaknesses.append(Weakness(
                    type=WeaknessType.SKILL,
                    identifier=skill,
                    severity=self.calculate_severity(accuracy),
                    trend=self.get_trend(answers, skill),
                    recommended_intervention=self.get_intervention(skill, accuracy)
                ))
        
        # 2. Specific vocabulary persistently missed
        persistent_misses = find_items_missed_3plus_times(answers)
        for item in persistent_misses:
            weaknesses.append(Weakness(
                type=WeaknessType.VOCABULARY_ITEM,
                identifier=item.id,
                severity=WeaknessSeverity.HIGH,
                miss_count=item.miss_count,
                recommended_intervention="mnemonic_creation"
            ))
        
        # 3. Grammar rule patterns
        grammar_errors = extract_grammar_error_patterns(answers)
        for pattern in grammar_errors:
            if pattern.frequency > 3 and pattern.recency < 7:  # Recent + frequent
                weaknesses.append(Weakness(
                    type=WeaknessType.GRAMMAR_RULE,
                    identifier=pattern.rule_id,
                    pattern_description=pattern.description
                ))
        
        # 4. Confusion pairs (commonly swapped items)
        confusion_pairs = find_confusion_pairs(answers, threshold=3)
        for pair in confusion_pairs:
            weaknesses.append(Weakness(
                type=WeaknessType.CONFUSION_PAIR,
                items=pair,
                intervention="minimal_pair_drilling"
            ))
        
        # 5. Pronunciation weaknesses (from voice sessions)
        phoneme_scores = aggregate_phoneme_scores(user_id)
        for phoneme, score in phoneme_scores.items():
            if score < 0.60:
                weaknesses.append(Weakness(
                    type=WeaknessType.PRONUNCIATION,
                    identifier=phoneme,
                    score=score
                ))
        
        # Prioritize weaknesses
        weaknesses.sort(key=lambda w: (w.severity * w.frequency * w.recency_weight))
        
        return WeaknessReport(
            user_id=user_id,
            generated_at=datetime.now(),
            weaknesses=weaknesses[:10],  # Top 10 most critical
            summary=self.generate_summary(weaknesses)
        )
```

---

## 6.6 MASTERY ENGINE

### Mastery Definition
An item is considered **mastered** when a learner demonstrates stable, fluent recall across:
1. Multiple review sessions over an extended time period (≥ 3 months of intervals)
2. Multiple modalities (can recognize, produce, use in context)
3. Varied contexts (tested in formal, informal, written, spoken forms)
4. Under cognitive load (time pressure, interleaved with similar items)

### Mastery Score Calculation
```python
def calculate_mastery_score(memory_obj: MemoryObject) -> float:
    """
    Mastery score: 0.0 (not started) to 1.0 (fully mastered)
    """
    if not memory_obj.is_introduced:
        return 0.0
    
    # Component 1: Interval stability (40% weight)
    # How long can the memory survive without review?
    # 21+ days interval = strong memory
    interval_score = min(memory_obj.interval / 21, 1.0)
    
    # Component 2: Accuracy history (30% weight)
    # Recent success rate over last 5 reviews
    recent_reviews = memory_obj.review_history[-5:]
    if recent_reviews:
        accuracy = sum(1 for r in recent_reviews if r.quality >= 4) / len(recent_reviews)
    else:
        accuracy = 0.0
    
    # Component 3: Modality coverage (15% weight)
    # Has it been practiced visually, aurally, and productively?
    modalities_covered = sum(memory_obj.modality_coverage.values())
    modality_score = modalities_covered / 3
    
    # Component 4: Contextual diversity (15% weight)
    # Practiced in how many different semantic contexts?
    context_score = min(memory_obj.contextual_diversity, 1.0)
    
    mastery = (
        0.40 * interval_score +
        0.30 * accuracy +
        0.15 * modality_score +
        0.15 * context_score
    )
    
    return mastery

# Mastery thresholds
MASTERY_LEVELS = {
    "not_started": (0.0, 0.05),
    "introduced": (0.05, 0.30),
    "learning": (0.30, 0.60),
    "consolidating": (0.60, 0.80),
    "mastered": (0.80, 0.95),
    "long_term": (0.95, 1.00)
}
```

---

## 6.7 PERSONALIZATION ENGINE

### Content Selection Algorithm
```python
def select_session_content(user_id: str, session_params: SessionParams) -> SessionContent:
    """
    Selects optimal 15-25 items for a learning session.
    """
    dna = load_learning_dna(user_id)
    
    # Step 1: Determine session composition ratio
    # Based on DNA: optimal review-to-new ratio
    total_items = session_params.target_items  # e.g., 20
    new_item_count = round(total_items * (1 - dna.memory_profile.review_to_new_ratio))
    review_count = total_items - new_item_count
    
    # Step 2: Select review items
    due_reviews = query_srs_due_items(user_id, limit=review_count * 3)
    
    # Apply interference-aware filtering
    review_items = select_with_interference_avoidance(due_reviews, review_count)
    
    # Step 3: Select new items
    available_new = query_unintroduced_items(
        user_id,
        cefr_level=dna.performance_profile.current_cefr_level,
        prerequisite_satisfied=True,  # Only items whose prerequisites are mastered
        limit=new_item_count * 2  # Get 2x needed for filtering
    )
    
    # Filter by DNA preferences
    if dna.cognitive_style.context_dependency > 0.7:
        # Prefers contextual items — select from active narrative thread
        new_items = prioritize_contextual_items(available_new, user_id)[:new_item_count]
    else:
        # OK with abstract items — select by frequency rank (most useful first)
        new_items = sort_by_frequency_rank(available_new)[:new_item_count]
    
    # Step 4: Determine exercise type mix
    exercise_mix = calculate_exercise_mix(dna, session_params)
    # e.g., {"multiple_choice": 3, "translation": 4, "speak": 3, "arrange": 3, ...}
    
    # Step 5: Adjust difficulty
    difficulty_target = calculate_target_difficulty(dna)
    # Target 80% accuracy (challenge calibration)
    
    # Step 6: Build session sequence
    session_items = interleave_items(review_items + new_items, exercise_mix)
    
    return SessionContent(
        items=session_items,
        difficulty_target=difficulty_target,
        estimated_duration_minutes=estimate_duration(session_items, dna),
        session_focus=determine_session_focus(review_items, new_items, dna)
    )
```

---

## 6.8 DIFFICULTY ADJUSTMENT ENGINE

### Dynamic Difficulty Model
The Difficulty Adjustment Engine ensures learners are always operating in their **optimal challenge zone** — hard enough to feel engaged but not so hard as to cause frustration.

```python
class DifficultyAdjuster:
    
    TARGET_ACCURACY = 0.80  # Aim for 80% correct (productive struggle zone)
    FRUSTRATION_THRESHOLD = 0.50  # Below 50% = frustration, reduce difficulty
    BOREDOM_THRESHOLD = 0.95     # Above 95% = boredom, increase difficulty
    
    def adjust_during_session(self, session_state: SessionState) -> DifficultyAdjustment:
        recent_accuracy = self.get_recent_accuracy(session_state, last_n=5)
        current_response_time = session_state.avg_response_time_recent
        
        if recent_accuracy < self.FRUSTRATION_THRESHOLD:
            # Learner is frustrated
            action = DifficultyAction.REDUCE
            magnitude = (self.FRUSTRATION_THRESHOLD - recent_accuracy) * 2
            reason = "frustration_detected"
            
            # Also: inject easier review items (high mastery score)
            # And: add an encouraging AI message
            # And: optionally unlock a hint
            
        elif recent_accuracy > self.BOREDOM_THRESHOLD:
            # Learner is bored — not being challenged enough
            action = DifficultyAction.INCREASE
            magnitude = (recent_accuracy - self.BOREDOM_THRESHOLD) * 2
            reason = "boredom_detected"
            
            # Also: introduce new item instead of next review
            # And: switch to harder exercise type (production instead of recognition)
            
        else:
            # In optimal zone
            action = DifficultyAction.MAINTAIN
            magnitude = 0.0
            reason = "optimal_zone"
        
        # Response time adjustment
        if current_response_time > 15:  # seconds
            # Struggling despite correct answers — slightly reduce difficulty
            action = DifficultyAction.SUBTLE_REDUCE
        
        return DifficultyAdjustment(action=action, magnitude=magnitude, reason=reason)
    
    def calculate_item_difficulty(self, item: LearningItem, user: UserProfile) -> float:
        """
        Item difficulty relative to this specific user (0.0 = trivially easy, 1.0 = extremely hard)
        """
        # Base difficulty from CEFR level gap
        cefr_levels = ["A1", "A2", "B1", "B2", "C1", "C2"]
        user_level_idx = cefr_levels.index(user.current_cefr_level)
        item_level_idx = cefr_levels.index(item.cefr_level)
        cefr_gap = item_level_idx - user_level_idx
        
        base_difficulty = 0.5 + (cefr_gap * 0.15)
        
        # Modifiers
        frequency_modifier = 1 - (item.frequency_rank / 50000)  # Rarer = harder
        
        # Cross-linguistic difficulty (user's native language similarity)
        linguistic_distance = get_linguistic_distance(user.native_language, item.language)
        
        # User's historical accuracy on this item type
        type_accuracy = user.dna.performance_profile.accuracy_by_type.get(item.type, 0.7)
        type_modifier = 1 - type_accuracy
        
        return np.clip(base_difficulty + frequency_modifier * 0.2 + type_modifier * 0.3, 0, 1)
```

---

## 6.9 REVIEW ENGINE

### Intelligent Review Session Generation
The Review Engine creates dedicated review sessions optimized for **maximum retention benefit**:

```python
class ReviewEngine:
    
    def generate_review_session(self, user_id: str, max_items: int = 20) -> ReviewSession:
        # Priority 1: Overdue items (missed their scheduled review)
        overdue = self.get_overdue_items(user_id)
        # Sort by: (days_overdue * urgency_weight + mastery_score_drop_risk)
        
        # Priority 2: Due today
        due_today = self.get_due_today(user_id)
        
        # Priority 3: At-risk items (mastery score declining trend)
        at_risk = self.get_at_risk_items(user_id, threshold=0.70)
        
        # Priority 4: Pre-emptive review (items due tomorrow but learner studying now)
        upcoming = self.get_due_tomorrow(user_id)
        
        # Combine with priority weighting
        candidates = (
            [(item, 3) for item in overdue] +
            [(item, 2) for item in due_today] +
            [(item, 1.5) for item in at_risk] +
            [(item, 1) for item in upcoming]
        )
        
        # Remove duplicates, keep highest priority
        candidates = deduplicate_by_priority(candidates)
        
        # Apply interference avoidance
        final_items = self.apply_interference_filtering(candidates, max_items)
        
        # Order: interleave to maximize retrieval practice effect
        ordered_items = self.optimal_ordering(final_items)
        
        return ReviewSession(
            user_id=user_id,
            items=ordered_items,
            session_type="scheduled_review",
            estimated_duration=estimate_duration(ordered_items)
        )
    
    def apply_interference_filtering(self, candidates, limit):
        """
        Ensure easily confused items don't appear too close together in session.
        """
        selected = []
        recently_seen_confusable = set()
        
        for item, priority in candidates:
            if len(selected) >= limit:
                break
            
            # Check if any confusable item was seen in last 3 items
            item_confusables = get_confusable_items(item.id)
            if not item_confusables.intersection(recently_seen_confusable):
                selected.append(item)
                # Update recently seen window (slide by 3)
                recently_seen_confusable = set([i.id for i in selected[-3:]])
        
        return selected
```

---

## 6.10 LEARNING PREDICTION ENGINE

### Mastery Timeline Prediction
```python
class LearningPredictor:
    
    def predict_mastery_date(self, user_id: str, target_level: str) -> MasteryPrediction:
        """
        Predicts when a user will reach target CEFR level based on:
        1. Current knowledge state (mastery across all items)
        2. Learning velocity (items mastered per week)
        3. Consistency score (how often they actually study)
        4. DNA-based acquisition/consolidation rates
        """
        dna = load_learning_dna(user_id)
        knowledge_state = load_knowledge_state(user_id)
        
        # Items required for target level
        required_items = count_items_at_level(target_level, dna.target_language)
        currently_mastered = count_mastered_items(user_id, up_to_level=target_level)
        items_remaining = required_items - currently_mastered
        
        # Calculate current learning velocity (items mastered per week, 4-week rolling avg)
        velocity = calculate_mastery_velocity(user_id, weeks=4)
        # velocity = items fully mastered per week at current pace
        
        # Adjust velocity by consistency
        consistency = calculate_study_consistency(user_id, days=30)
        adjusted_velocity = velocity * consistency
        
        # Account for increasing difficulty (later-level items take longer)
        level_difficulty_multiplier = get_cefr_level_difficulty(target_level)
        realistic_velocity = adjusted_velocity / level_difficulty_multiplier
        
        if realistic_velocity <= 0:
            return MasteryPrediction(confidence=0.0, error="insufficient_data")
        
        weeks_remaining = items_remaining / realistic_velocity
        predicted_date = datetime.now() + timedelta(weeks=weeks_remaining)
        
        # Confidence interval
        # Based on: consistency variance, historical prediction accuracy
        confidence_interval_days = calculate_confidence_interval(
            user_id, weeks_remaining, consistency
        )
        
        # Alternative scenarios
        if_daily_study = predicted_date - timedelta(weeks=weeks_remaining * (1 - consistency))
        if_premium = predicted_date - timedelta(weeks=weeks_remaining * 0.25)  # Premium faster
        
        return MasteryPrediction(
            user_id=user_id,
            target_level=target_level,
            predicted_date=predicted_date,
            confidence_interval=confidence_interval_days,
            items_remaining=items_remaining,
            current_velocity=realistic_velocity,
            alternative_scenarios={
                "if_daily_study": if_daily_study,
                "if_premium_ai_coaching": if_premium,
            }
        )
```
