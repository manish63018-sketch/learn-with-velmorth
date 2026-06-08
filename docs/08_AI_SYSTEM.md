# LEARN WITH VELMORTH — AI SYSTEM
## Section 8: Complete AI Architecture

---

## 8.1 AI SYSTEM OVERVIEW

Velmorth's AI system is not a single model — it is a **multi-agent AI ecosystem** where specialized AI agents collaborate to serve every dimension of a learner's needs. The system is built on LangGraph for stateful agent orchestration, with Gemini models at the core.

```
┌──────────────────────────────────────────────────────────────────────┐
│                       AI ORCHESTRATION LAYER                         │
│                    LangGraph State Machine                           │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │  AI Tutor    │  │  AI Coach    │  │  AI Mentor   │              │
│  │  (Teacher)   │  │  (Progress)  │  │  (Long-term) │              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
│         │                  │                  │                      │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐              │
│  │  Lesson Gen  │  │  Quiz Gen    │  │  Story Gen   │              │
│  │  (Content)   │  │  (Assessment)│  │  (Narrative) │              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
│         └──────────────────┴──────────────────┘                     │
│                            │                                         │
│  ┌─────────────────────────▼─────────────────────────────────────┐  │
│  │                  Shared Intelligence Services                  │  │
│  │  Recommendation Engine | Prediction Engine | Motivation Engine │  │
│  │  Voice Coach | Pronunciation Analyzer | Study Planner         │  │
│  └──────────────────────────────────────────────────────────────┘   │
│                            │                                         │
│  ┌─────────────────────────▼─────────────────────────────────────┐  │
│  │                     Model Layer                                │  │
│  │  Gemini 2.5 Pro (reasoning) | Gemini 2.0 Flash (speed)       │  │
│  │  Whisper v3 (STT) | Custom TF Models (classification)         │  │
│  └──────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 8.2 AI TUTOR — COMPLETE SPECIFICATION

### Role and Personality
The AI Tutor (named "Velmorth" in-app) is the primary conversational AI agent that acts as a teacher, language partner, and cultural guide. It has full access to the learner's complete profile, history, and knowledge state.

### Technical Architecture
```python
# AI Tutor uses LangGraph for stateful conversation management
from langgraph.graph import StateGraph, END
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain.memory import ConversationSummaryBufferMemory

class TutorAgentState(TypedDict):
    user_id: str
    learning_dna: dict
    knowledge_state: dict
    conversation_history: List[dict]
    current_topic: str
    session_goals: List[str]
    items_practiced_this_session: List[str]
    user_message: str
    assistant_response: str
    tools_called: List[str]
    metadata: dict

# Tools available to AI Tutor
TUTOR_TOOLS = [
    Tool("lookup_word_definition", "Look up definition, examples, and cultural notes for a word"),
    Tool("get_user_mastery_level", "Check user's current mastery of specific item"),
    Tool("generate_practice_exercise", "Create a customized exercise for the current topic"),
    Tool("get_grammar_explanation", "Retrieve detailed grammar rule explanation"),
    Tool("generate_mnemonic", "Create a memory aid for a difficult word or rule"),
    Tool("translate_text", "Translate text between languages"),
    Tool("search_example_sentences", "Find authentic example sentences from corpus"),
    Tool("get_cultural_note", "Retrieve cultural context for a word or phrase"),
    Tool("adjust_difficulty", "Signal to session manager to adjust difficulty level"),
    Tool("create_vocabulary_card", "Generate a visual vocabulary card with image"),
    Tool("book_ai_session", "Schedule a structured practice session"),
]

def build_tutor_graph():
    graph = StateGraph(TutorAgentState)
    
    # Nodes
    graph.add_node("understand_query", understand_user_intent)
    graph.add_node("gather_context", load_user_context)
    graph.add_node("select_response_strategy", plan_response)
    graph.add_node("call_tools", execute_tool_calls)
    graph.add_node("generate_response", generate_tutor_response)
    graph.add_node("validate_response", check_response_quality)
    graph.add_node("format_output", format_for_display)
    
    # Edges
    graph.add_edge("understand_query", "gather_context")
    graph.add_edge("gather_context", "select_response_strategy")
    graph.add_conditional_edges(
        "select_response_strategy",
        lambda s: "call_tools" if s["needs_tools"] else "generate_response"
    )
    graph.add_edge("call_tools", "generate_response")
    graph.add_edge("generate_response", "validate_response")
    graph.add_edge("validate_response", "format_output")
    graph.add_edge("format_output", END)
    
    return graph.compile()
```

### AI Tutor Response Generation
```python
async def generate_tutor_response(state: TutorAgentState) -> TutorAgentState:
    model = ChatGoogleGenerativeAI(model="gemini-2.5-pro", temperature=0.7)
    
    system_prompt = build_tutor_system_prompt(state)
    
    messages = [
        SystemMessage(content=system_prompt),
        *state["conversation_history"],
        HumanMessage(content=state["user_message"])
    ]
    
    response = await model.ainvoke(messages)
    
    state["assistant_response"] = response.content
    return state

def build_tutor_system_prompt(state: TutorAgentState) -> str:
    dna = state["learning_dna"]
    
    return f"""
You are Velmorth AI, an expert language teacher, mentor, and learning companion. 
You are having a conversation with {state['user_name']}, who is learning {state['target_language']}.

LEARNER PROFILE:
- Current Level: {state['current_cefr_level']} ({state['proficiency_description']})
- Native Language: {state['native_language']}
- Learning Style: {dna['cognitive_style']['primary_style']} learner
- Current Weak Areas: {', '.join(state['current_weaknesses'][:3])}
- Current Strengths: {', '.join(state['current_strengths'][:3])}
- Learning Goal: {state['learning_goal']}
- Study Pace: {dna['motivation_profile']['challenge_tolerance']} (0=easy-preferring, 1=challenge-loving)
- Preferred Explanation Style: {'detailed' if dna['cognitive_style']['elaboration_preference'] > 0.5 else 'concise'}

TEACHING GUIDELINES:
1. LANGUAGE CALIBRATION: 
   - Respond primarily in English unless practicing {state['target_language']}
   - When teaching vocabulary or grammar, always show the target language word/phrase in bold
   - Include phonetic pronunciation in IPA or simplified form for new words
   - Calibrate vocabulary complexity to {state['current_cefr_level']} level understanding

2. PEDAGOGICAL APPROACH:
   - Use Socratic method: ask questions that guide discovery rather than just giving answers
   - Connect new concepts to things the learner already knows (from their mastered items)
   - When correcting errors, acknowledge what was right before addressing what was wrong
   - Use analogies to the learner's native language ({state['native_language']}) when helpful
   - Provide memorable examples: use the learner's name, their stated interests, their goal

3. RESPONSE FORMATTING:
   - Keep responses concise: 3-6 sentences for conversational exchanges
   - For grammar explanations: use a clear structure (Rule → Example → Exception)
   - Use markdown formatting for vocabulary cards, grammar tables, example lists
   - End responses with an engaging question or practice suggestion when appropriate

4. EMOTIONAL INTELLIGENCE:
   - Detect frustration from message tone; respond with extra encouragement
   - Celebrate progress explicitly but not excessively
   - Never make the learner feel stupid for mistakes
   - Use their name naturally in conversation
   - Remember the last 10 exchanges to maintain conversational continuity

5. ACTIVE TEACHING BEHAVIORS:
   - Proactively notice and gently correct errors in target language the learner produces
   - Introduce 1-2 new vocabulary items naturally into each conversation
   - Reference their current lesson topics when relevant
   - Create mini-exercises inline when it would help (e.g., "Quick quiz: how would you say X?")

SESSION CONTEXT:
- Current topic: {state['current_topic']}
- Today's lesson focus: {state['todays_lesson_topic']}
- Items needing review: {', '.join(state['due_review_items'][:5])}
- Recent mistake pattern: {state['recent_mistake_pattern']}

IMPORTANT: You have access to tools. Use them proactively to:
- Look up words the learner asks about
- Create practice exercises when conversation warrants it
- Generate mnemonics for difficult words
- Pull example sentences from authentic sources

Current conversation topic thread: {state['current_topic']}
"""
```

---

## 8.3 AI COACH — COMPLETE SPECIFICATION

### Role
The AI Coach (distinct from the Tutor) operates at the macro level — not teaching individual lessons, but managing the learner's overall progress, motivation, and study strategy. The Coach reaches out proactively via notifications and check-in sessions.

```python
class AICoachAgent:
    """
    Proactive AI that monitors learning health and intervenes when needed.
    Runs as background service, not triggered by user request.
    """
    
    async def run_daily_health_check(self, user_id: str):
        """
        Called once per day per user. Analyzes state and decides if intervention needed.
        """
        health_report = await self.generate_health_report(user_id)
        
        interventions = []
        
        # Check 1: Streak at risk
        if health_report.streak_at_risk:
            interventions.append(self.create_streak_intervention(user_id, health_report))
        
        # Check 2: Plateau detected (no progress for 2 weeks)
        if health_report.learning_plateau_detected:
            interventions.append(self.create_plateau_intervention(user_id, health_report))
        
        # Check 3: Consistently missing study sessions
        if health_report.session_consistency < 0.5:  # Missing more than 50% of days
            interventions.append(self.create_habit_intervention(user_id, health_report))
        
        # Check 4: Difficulty mismatch (too hard or too easy for sustained period)
        if health_report.accuracy_trend == "persistently_low":
            interventions.append(self.create_difficulty_intervention(user_id, health_report))
        
        # Check 5: Goal mismatch (pace won't meet goal deadline)
        if health_report.on_track_for_goal == False:
            interventions.append(self.create_goal_adjustment_intervention(user_id, health_report))
        
        # Check 6: Positive milestones to celebrate
        if health_report.recent_milestones:
            interventions.append(self.create_celebration_message(user_id, health_report))
        
        # Execute highest priority intervention
        if interventions:
            priority_intervention = max(interventions, key=lambda x: x.priority)
            await priority_intervention.execute()
    
    async def generate_weekly_coach_report(self, user_id: str) -> CoachReport:
        """
        Every Sunday: Generates a personalized weekly review from the coach.
        """
        week_data = await load_week_data(user_id)
        dna = await load_learning_dna(user_id)
        
        report_prompt = f"""
Generate a personalized weekly learning report for {week_data.user_name}.

WEEK DATA:
- Days studied: {week_data.days_studied}/7
- Total XP: {week_data.total_xp}
- Lessons completed: {week_data.lessons_completed}
- New words mastered: {week_data.words_mastered}
- Accuracy: {week_data.avg_accuracy:.1%}
- Strongest moment: {week_data.best_session_description}
- Weakest moment: {week_data.worst_session_description}
- Streak status: {week_data.streak_status}

Write a personalized, warm but data-driven coach message that:
1. Opens with specific acknowledgment of what they did this week (reference exact numbers)
2. Celebrates the #1 win of the week
3. Gently identifies the #1 improvement area with a specific actionable suggestion
4. Provides next week's recommended focus (based on their weak areas)
5. Sets a specific, measurable goal for next week
6. Closes with a motivational sentence that references their specific learning goal

Tone: Like a personal trainer who genuinely cares about them — warm, encouraging, specific, honest.
Length: 4-6 paragraphs.
"""
        return await self.model.ainvoke(report_prompt)
```

---

## 8.4 AI LESSON GENERATOR

### Dynamic Lesson Content Generation
```python
class LessonGenerator:
    """
    Generates infinite calibrated lesson content on demand.
    """
    
    async def generate_lesson(self, lesson_params: LessonGenerationParams) -> GeneratedLesson:
        """
        Generates a complete lesson with all exercise items.
        """
        # Build generation context
        context = {
            "language": lesson_params.target_language,
            "cefr_level": lesson_params.target_cefr_level,
            "topic": lesson_params.topic,  # e.g., "Past tense irregular verbs"
            "exercise_types": lesson_params.required_exercise_types,
            "vocabulary_pool": lesson_params.available_vocabulary,
            "grammar_focus": lesson_params.grammar_points,
            "cultural_context": lesson_params.cultural_theme,
            "learner_native_lang": lesson_params.native_language,
            "learning_dna": lesson_params.user_dna,
        }
        
        # Generate exercises in parallel for speed
        tasks = [
            self.generate_multiple_choice_batch(context, count=3),
            self.generate_translation_items(context, count=4),
            self.generate_listening_items(context, count=2),
            self.generate_word_arrangement_items(context, count=2),
            self.generate_speaking_items(context, count=2),
            self.generate_story_item(context, count=1),
        ]
        
        results = await asyncio.gather(*tasks)
        items = flatten(results)
        
        # Quality validation
        validated_items = await self.validate_all_items(items, context)
        
        # Sort by optimal teaching order
        ordered_items = self.optimal_item_ordering(validated_items, context)
        
        return GeneratedLesson(
            id=generate_uuid(),
            topic=lesson_params.topic,
            items=ordered_items,
            metadata=self.build_lesson_metadata(context),
            generated_at=datetime.now(),
            model_version=self.model_version,
        )
    
    async def generate_translation_items(self, context: dict, count: int) -> List[ExerciseItem]:
        prompt = f"""
Generate {count} translation exercise items for learning {context['language']}.

Requirements:
- CEFR Level: {context['cefr_level']}
- Topic: {context['topic']}
- Native language: {context['learner_native_lang']}
- Cultural context: {context['cultural_context']}
- Grammar focus: {context['grammar_focus']}

For each item, generate:
1. source_text: A natural sentence in the native language ({context['learner_native_lang']})
2. target_answer: The correct translation in {context['language']}
3. alternative_correct_answers: List of other acceptable translations (same meaning, different words)
4. key_grammar_point: Which grammar concept this practices
5. cultural_note: Brief note if there's cultural significance
6. difficulty_rating: 1-5 (where 3=target_level, <3=easier, >3=harder)
7. hint: What to focus on (shown if user clicks hint)

IMPORTANT:
- Sentences should be natural, useful real-world phrases (not textbook examples)
- Avoid clichés like "The pen is on the table"
- Include proper nouns from target culture (names, places, brands) naturally
- Vary sentence length and complexity within the level
- If grammar_focus includes {context['grammar_focus']}, make sure at least {count-1} items use it

Return as JSON array.
"""
        response = await self.fast_model.ainvoke(prompt)
        return parse_exercise_items(response.content, exercise_type="translation")
```

---

## 8.5 AI QUIZ GENERATOR

```python
class QuizGenerator:
    
    async def generate_adaptive_quiz(
        self, 
        user_id: str, 
        topic: str, 
        question_count: int = 10
    ) -> AdaptiveQuiz:
        """
        Generates a quiz that adapts in real-time to performance.
        """
        user_state = await load_user_state(user_id)
        
        # Initial question pool (generate 3x needed for adaptive selection)
        initial_pool = await self.generate_question_pool(
            topic=topic,
            cefr_level=user_state.current_level,
            count=question_count * 3,
            difficulty_spread="gaussian"  # Most at target level, some easier/harder
        )
        
        # Adaptive selection algorithm
        selected_questions = []
        current_ability_estimate = user_state.topic_ability.get(topic, 0.5)
        
        for i in range(question_count):
            # Item Response Theory (IRT) - 3PL model
            # Select item with max information at current ability estimate
            best_item = self.irt_item_selector(
                pool=initial_pool,
                current_ability=current_ability_estimate,
                already_selected=[q.id for q in selected_questions]
            )
            selected_questions.append(best_item)
            
            # After each question (in adaptive simulation), update ability estimate
            # (In real quiz, this happens after each real answer via WebSocket)
        
        return AdaptiveQuiz(
            questions=selected_questions,
            adaptive_mode=True,
            time_limit_seconds=question_count * 45,  # 45 seconds per question
            topic=topic,
        )
    
    def irt_item_selector(self, pool, current_ability, already_selected):
        """
        Selects the most informative item using Item Response Theory.
        Information = b * P(correct) * P(incorrect)
        where b = item discrimination, P = probability of correct response
        """
        max_info = 0
        best_item = None
        
        for item in pool:
            if item.id in already_selected:
                continue
            
            # 3PL IRT probability
            p_correct = item.c + (1 - item.c) / (1 + math.exp(-item.a * (current_ability - item.b)))
            
            # Fisher information
            info = item.a ** 2 * (p_correct - item.c) ** 2 / ((1 - item.c) ** 2) * \
                   (1 - p_correct) / p_correct
            
            if info > max_info:
                max_info = info
                best_item = item
        
        return best_item
```

---

## 8.6 AI STORY GENERATOR

### Narrative Immersion Content
```python
class StoryGenerator:
    """
    Creates serialized language-learning stories where the user is the protagonist.
    Each story is personalized to the learner's interests, level, and progress.
    """
    
    async def generate_story_chapter(
        self,
        user_id: str,
        story_arc: StoryArc,
        chapter_number: int
    ) -> StoryChapter:
        
        user_profile = await load_user_profile(user_id)
        prev_chapter = await load_chapter(story_arc.id, chapter_number - 1)
        
        # User's choices from previous chapter affect this one
        user_choices = await load_user_story_choices(user_id, story_arc.id)
        
        prompt = f"""
You are creating Chapter {chapter_number} of a language-learning story for {user_profile.name}.

STORY ARC: {story_arc.title}
SETTING: {story_arc.setting}
GENRE: {story_arc.genre}
TARGET LANGUAGE: {user_profile.target_language} ({user_profile.current_cefr_level} level)

PREVIOUS CHAPTER SUMMARY: {prev_chapter.summary if prev_chapter else "Beginning of story"}
USER'S PREVIOUS CHOICES: {format_choices(user_choices)}

USER INTERESTS (weave into story naturally): {', '.join(user_profile.interests)}

VOCABULARY TO TEACH IN THIS CHAPTER:
{format_vocabulary_list(story_arc.chapter_vocabulary[chapter_number])}

GRAMMAR POINTS TO ILLUSTRATE:
{story_arc.chapter_grammar[chapter_number]}

GENERATE:
1. chapter_text: Narrative text (600-800 words)
   - Written at {user_profile.current_cefr_level} reading level
   - Target language words/phrases appear naturally in context (marked with **bold**)
   - Story is engaging, has stakes and emotion
   - Characters speak in authentic dialogue
   - User ({user_profile.name}) is the protagonist, make decisions that matter
   
2. vocabulary_in_context: List of target language items used, with timestamp position

3. comprehension_questions: 3 questions about the chapter content
   - Tests: understanding + language recall
   
4. choice_point: End of chapter decision point
   Options A, B, C (each leads to different Chapter {chapter_number + 1} variant)
   Make choices meaningful, not trivial
   
5. chapter_summary: 50-word summary (for next chapter context)

6. learning_notes: 3-5 key language takeaways from this chapter

Format as JSON.
"""
        response = await self.creative_model.ainvoke(prompt)
        return parse_story_chapter(response.content)
```

---

## 8.7 AI CONVERSATION ENGINE

### Real-Time Language Practice
```python
class ConversationEngine:
    """
    Manages live language practice conversations.
    Simulates a native speaker in various roles and scenarios.
    """
    
    SCENARIO_CATALOG = {
        "coffee_shop": {
            "en": "You are a barista at a busy coffee shop in {city}.",
            "difficulty_range": ["A1", "A2"],
            "key_vocabulary": ["order", "size", "payment", "to-go", "dine-in"],
        },
        "job_interview": {
            "en": "You are an interviewer at a tech company in {city}.",
            "difficulty_range": ["B2", "C1"],
            "key_vocabulary": ["experience", "responsibility", "challenge", "achievement"],
        },
        "apartment_hunting": {
            "en": "You are a landlord showing an apartment to a potential tenant.",
            "difficulty_range": ["A2", "B1"],
            "key_vocabulary": ["rent", "deposit", "utilities", "neighborhood", "lease"],
        },
        "doctor_appointment": {
            "en": "You are a doctor at a clinic.",
            "difficulty_range": ["B1", "B2"],
            "key_vocabulary": ["symptoms", "prescription", "appointment", "diagnosis"],
        },
        "free_conversation": {
            "en": "You are {user_name}'s language exchange partner, a native {language} speaker.",
            "difficulty_range": ["A1", "C2"],
            "key_vocabulary": [],
        },
    }
    
    async def process_conversation_turn(
        self,
        session: ConversationSession,
        user_utterance: str
    ) -> ConversationResponse:
        
        # 1. Transcribe if voice input
        if session.input_mode == "voice":
            user_utterance = await self.voice_service.transcribe(user_utterance)
        
        # 2. Error detection
        errors = await self.detect_language_errors(
            text=user_utterance,
            expected_level=session.user_level,
            native_language=session.native_language
        )
        
        # 3. Generate response in character (scenario role)
        scenario = self.SCENARIO_CATALOG[session.scenario]
        
        response_prompt = f"""
You are playing a role in a language practice conversation.

ROLE: {scenario['en'].format(city=session.city, user_name=session.user_name, language=session.target_language)}
LANGUAGE: Respond in {session.target_language} only (occasionally mix in {session.native_language} for key vocabulary explanations)
LEARNER LEVEL: {session.user_level} — calibrate your response vocabulary and grammar to be slightly above their level (i+1)

USER SAID: "{user_utterance}"

ERRORS DETECTED IN USER'S MESSAGE: {format_errors(errors)}

YOUR RESPONSE MUST:
1. Stay in character and respond naturally to what they said
2. If they made errors: subtly use the correct form in your response (recasting), do NOT explicitly correct unless asked
3. If they used a good phrase or structure: naturally echo/expand on it (positive reinforcement)
4. Advance the conversation scenario naturally
5. Include 1-2 new vocabulary items naturally (bold them)
6. End with a question or prompt that invites them to respond
7. Keep response to 2-4 sentences (natural conversation pace)

RESPONSE FORMAT:
{{
  "response_text": "...",
  "target_language_only": "...",  // Same response, target language only (for display)
  "translation": "...",  // English translation (shown on request)
  "vocabulary_introduced": ["word1", "word2"],
  "grammar_demonstrated": "...",
  "error_correction_approach": "recast|explicit|ignore",
  "hint_for_next_response": "..."  // Suggestion for what they could say next
}}
"""
        response_json = await self.model.ainvoke(response_prompt)
        parsed = parse_json(response_json.content)
        
        # Generate audio for the response
        audio_url = await self.tts_service.synthesize(
            text=parsed["target_language_only"],
            language=session.target_language,
            voice=session.selected_voice,
            speed=session.preferred_speech_rate
        )
        
        # Update conversation session state
        await self.update_session(session, user_utterance, parsed, errors)
        
        return ConversationResponse(
            text=parsed["response_text"],
            target_language_text=parsed["target_language_only"],
            audio_url=audio_url,
            translation=parsed["translation"],
            errors_detected=errors,
            vocabulary_highlighted=parsed["vocabulary_introduced"],
            grammar_note=parsed["grammar_demonstrated"],
        )
```

---

## 8.8 AI VOICE COACH

```python
class VoiceCoach:
    """
    Analyzes pronunciation and provides detailed, actionable feedback.
    """
    
    async def analyze_pronunciation(
        self,
        audio_bytes: bytes,
        target_phrase: str,
        language: str,
        user_native_language: str
    ) -> PronunciationAnalysis:
        
        # Step 1: Transcribe with phoneme alignment
        transcript = await whisper_transcribe_with_alignment(audio_bytes, language)
        
        # Step 2: Get reference pronunciation (native speaker model)
        reference_phonemes = await self.get_reference_phonemes(target_phrase, language)
        
        # Step 3: Phoneme-level comparison
        user_phonemes = transcript.aligned_phonemes
        phoneme_scores = self.compare_phoneme_sequences(user_phonemes, reference_phonemes)
        
        # Step 4: Prosody analysis
        prosody = await self.analyze_prosody(audio_bytes, language)
        
        # Step 5: Overall scoring
        phoneme_accuracy = mean(phoneme_scores.values())
        
        score = (
            0.50 * phoneme_accuracy +
            0.20 * prosody.stress_accuracy +
            0.15 * prosody.intonation_accuracy +
            0.15 * prosody.rhythm_accuracy
        )
        
        # Step 6: Generate targeted feedback
        feedback = await self.generate_pronunciation_feedback(
            target_phrase=target_phrase,
            transcript=transcript.text,
            phoneme_scores=phoneme_scores,
            prosody=prosody,
            user_native_language=user_native_language,
            language=language,
            overall_score=score
        )
        
        return PronunciationAnalysis(
            transcript=transcript.text,
            overall_score=score,
            grade=score_to_grade(score),  # A+, A, B, C, D, F
            phoneme_breakdown=phoneme_scores,
            prosody_analysis=prosody,
            feedback=feedback,
            areas_to_improve=identify_top_issues(phoneme_scores, prosody),
            model_audio_url=reference_audio_url(target_phrase, language),
        )
    
    async def generate_pronunciation_feedback(self, **kwargs) -> str:
        prompt = f"""
You are an expert phonetics teacher providing pronunciation feedback.

TARGET PHRASE: "{kwargs['target_phrase']}"
STUDENT SAID: "{kwargs['transcript']}"
OVERALL SCORE: {kwargs['overall_score']:.1%}
STUDENT'S NATIVE LANGUAGE: {kwargs['user_native_language']}

PHONEME ISSUES DETECTED:
{format_phoneme_issues(kwargs['phoneme_scores'])}

PROSODY ISSUES:
- Stress: {kwargs['prosody'].stress_score:.1%} — {kwargs['prosody'].stress_notes}
- Intonation: {kwargs['prosody'].intonation_score:.1%} — {kwargs['prosody'].intonation_notes}
- Rhythm: {kwargs['prosody'].rhythm_score:.1%} — {kwargs['prosody'].rhythm_notes}

Generate feedback that:
1. Opens with one genuine positive observation (find something right even if minor)
2. Identifies the #1 most impactful issue to fix (not the most errors, the most important)
3. Gives a specific, practical exercise or technique to fix that issue
4. References their native language ({kwargs['user_native_language']}) when relevant
   (e.g., "In English, the /r/ sound is different — your tongue shouldn't touch the roof...")
5. Closes with encouraging call-to-action ("Try it again focusing on...")

Length: 3-5 sentences. Warm, specific, actionable. Never discouraging.
"""
        response = await self.model.ainvoke(prompt)
        return response.content
```

---

## 8.9 AI RECOMMENDATION ENGINE

```python
class RecommendationEngine:
    """
    Determines what content to show each learner, at every moment.
    Uses a hybrid collaborative + content-based filtering approach.
    """
    
    async def recommend_next_content(
        self,
        user_id: str,
        context: RecommendationContext
    ) -> ContentRecommendations:
        
        user_embedding = await self.get_user_embedding(user_id)
        content_candidates = await self.generate_candidates(user_id, context)
        
        # Score each candidate
        scored_candidates = []
        for candidate in content_candidates:
            
            score = await self.score_candidate(
                user_embedding=user_embedding,
                candidate=candidate,
                context=context
            )
            scored_candidates.append((candidate, score))
        
        # Apply diversity filter (ensure variety in content types)
        diverse_results = self.apply_diversity_filter(scored_candidates, top_n=10)
        
        # Apply business rules
        filtered_results = self.apply_business_rules(diverse_results, user_id)
        # Business rules: don't recommend paid content to free users without conversion nudge
        # Don't recommend same content twice in a row, etc.
        
        return ContentRecommendations(
            primary=diverse_results[:3],
            secondary=diverse_results[3:6],
            discovery=diverse_results[6:],  # New/exploratory
        )
    
    async def score_candidate(self, user_embedding, candidate, context) -> float:
        # Multi-factor scoring
        
        # 1. Collaborative filter score (what similar learners found valuable)
        similar_users = await self.find_similar_users(user_embedding, top_k=100)
        collab_score = self.collaborative_score(candidate, similar_users)
        
        # 2. Content-based score (how well it matches learner's DNA)
        content_score = self.content_based_score(candidate, context.user_dna)
        
        # 3. Urgency score (SRS due-ness)
        urgency_score = self.calculate_urgency(candidate, context.srs_state)
        
        # 4. Novelty score (hasn't been seen recently)
        novelty_score = 1.0 - self.recency_penalty(candidate, context.recent_history)
        
        # 5. Learning value score (will this move the needle?)
        value_score = self.learning_value(candidate, context.knowledge_state)
        
        # Weighted combination
        weights = {"collaborative": 0.20, "content": 0.25, "urgency": 0.30, 
                   "novelty": 0.10, "value": 0.15}
        
        final_score = (
            weights["collaborative"] * collab_score +
            weights["content"] * content_score +
            weights["urgency"] * urgency_score +
            weights["novelty"] * novelty_score +
            weights["value"] * value_score
        )
        
        return final_score
```

---

## 8.10 AI STUDY PLANNER

```python
class StudyPlanner:
    
    async def generate_study_plan(
        self,
        user_id: str,
        goal: StudyGoal
    ) -> StudyPlan:
        """
        Generates a complete, day-by-day study plan to reach the target goal.
        """
        user_state = await load_user_state(user_id)
        dna = await load_learning_dna(user_id)
        prediction = await self.predictor.predict_mastery_date(user_id, goal.target_level)
        
        # Calculate required daily effort
        available_days = (goal.target_date - date.today()).days
        items_per_day = (prediction.items_remaining / available_days) * 1.2  # 20% buffer
        minutes_per_day = items_per_day * dna.memory_profile.avg_time_per_item / 60
        
        # Generate weekly schedule
        plan_prompt = f"""
Create a personalized weekly study schedule for a language learner.

LEARNER PROFILE:
- Name: {user_state.name}
- Target Language: {user_state.target_language}
- Current Level: {user_state.cefr_level}
- Target Level: {goal.target_level}
- Target Date: {goal.target_date}
- Available Minutes Per Day: {minutes_per_day:.0f} minutes
- Learning Goal: {goal.description}
- Preferred Study Times: {dna.schedule_profile.preferred_times}
- Days Per Week They Can Study: {dna.schedule_profile.available_days_per_week}
- Weak Areas: {', '.join(user_state.top_weaknesses)}
- Strong Areas: {', '.join(user_state.top_strengths)}

GENERATE A WEEKLY RECURRING PLAN with:
1. Day-by-day breakdown (what to study each day)
2. Session composition (how many new words, grammar, review, practice)
3. Special focus days (e.g., "Grammar Wednesday", "Speaking Saturday")
4. Rest days (important for memory consolidation)
5. Monthly milestone checkpoints
6. Contingency recommendations (what to do if behind schedule)

IMPORTANT:
- Be realistic about the learner's available time
- Include variety to prevent burnout
- Prioritize weak areas without completely ignoring strengths
- The plan should feel motivating, not overwhelming
- Include specific activities for each day type

Format as structured JSON with day-by-day breakdown.
"""
        response = await self.model.ainvoke(plan_prompt)
        plan_data = parse_json(response.content)
        
        return StudyPlan(
            user_id=user_id,
            goal=goal,
            weekly_schedule=plan_data["schedule"],
            milestones=plan_data["milestones"],
            estimated_completion=prediction.predicted_date,
            daily_minutes=minutes_per_day,
            created_at=datetime.now(),
        )
```

---

## 8.11 AI MOTIVATION ENGINE

```python
class MotivationEngine:
    """
    Generates personalized motivational content at the right moment.
    Motivated by BJ Fogg's Tiny Habits + Positive Psychology research.
    """
    
    MOTIVATION_TRIGGERS = {
        "streak_at_risk": {"priority": 10, "window_hours": 4},
        "streak_broken": {"priority": 9, "window_hours": 24},
        "plateau_detected": {"priority": 8, "window_hours": 72},
        "goal_milestone": {"priority": 7, "window_hours": 1},
        "weekly_low_engagement": {"priority": 6, "window_hours": 168},
        "daily_reminder": {"priority": 3, "window_hours": 24},
        "celebration": {"priority": 7, "window_hours": 1},
    }
    
    async def generate_motivation_message(
        self,
        user_id: str,
        trigger: str,
        context: dict
    ) -> MotivationMessage:
        
        user = await load_user(user_id)
        dna = await load_learning_dna(user_id)
        
        # Select motivation style based on DNA
        style = self.select_motivation_style(dna.motivation_profile)
        
        prompt = f"""
Generate a highly personalized motivational message for a language learner.

TRIGGER: {trigger}
CONTEXT: {format_context(context)}

LEARNER: {user.name}
LANGUAGE THEY'RE LEARNING: {user.target_language}
LEARNING GOAL: {user.primary_goal}
CURRENT STREAK: {user.current_streak} days
CURRENT LEVEL: {user.cefr_level}
RECENT ACHIEVEMENT: {user.most_recent_achievement}
TIME STUDYING: {user.total_study_days} days total
MOTIVATION STYLE: {style}

MOTIVATION STYLE DEFINITIONS:
- achievement_focused: Reference progress metrics, milestones, improvement percentages
- social_focused: Reference community, friends, guild, not being left behind
- story_focused: Create a narrative, use metaphors, make them feel like the hero
- scientific_focused: Reference learning science, brain science, evidence-based insights
- challenger: Challenge their identity as a capable person, invoke pride

GENERATE:
1. Push notification text (max 100 chars) — for notification preview
2. Full message (3-5 sentences) — for in-app display
3. Call-to-action button text (max 20 chars)
4. Tone: {style}

The message must:
- Reference their specific situation (not generic)
- Use their first name once naturally
- Reference the specific language they're learning
- Connect to their stated learning goal
- Feel human-written, not AI-generated
- For streak_broken: Be empathetic first, then motivating (never shame)
- For plateau: Normalize the challenge, offer a specific solution

Format as JSON.
"""
        response = await self.creative_model.ainvoke(prompt)
        return parse_motivation_message(response.content)
```

---

## 8.12 AI LEARNING TWIN

### Concept
The AI Learning Twin is a digital representation of the learner's knowledge state — a simulated version of "them" that the AI can use to predict how they will perform on future content, what they are likely to forget, and what they are ready to learn next.

```python
class LearningTwin:
    """
    A predictive model of a specific learner's knowledge state.
    Updated after every session. Used for proactive content selection.
    """
    
    def __init__(self, user_id: str):
        self.user_id = user_id
        self.knowledge_vector: np.ndarray  # Embedding of current knowledge state
        self.learning_velocity: float      # Items per day
        self.forgetting_curve_params: dict # Per-item-type forgetting rates
        self.error_propensity: dict        # Probability of error by item type
        
    def predict_performance(self, item: LearningItem, future_datetime: datetime) -> float:
        """
        Predicts probability of correct recall at a specific future moment.
        Uses learner-specific forgetting curve.
        """
        memory_obj = load_memory_object(self.user_id, item.id)
        
        if not memory_obj.is_introduced:
            return 0.0  # Haven't learned it yet
        
        days_since_review = (future_datetime - memory_obj.last_reviewed).days
        
        # Use learner's personalized decay rate from their profile
        stability = memory_obj.stability * self.forgetting_curve_params.get(item.type, 1.0)
        
        # Ebbinghaus-extended forgetting curve with stability
        retrievability = math.exp(-days_since_review / stability)
        
        # Adjust for item-type error propensity
        error_adj = 1 - self.error_propensity.get(item.type, 0.0)
        
        return retrievability * error_adj
    
    def identify_at_risk_items(self, horizon_days: int = 7) -> List[AtRiskItem]:
        """
        Identifies all items that are predicted to drop below threshold within horizon.
        """
        all_introduced = load_all_introduced_items(self.user_id)
        future_date = datetime.now() + timedelta(days=horizon_days)
        
        at_risk = []
        for item in all_introduced:
            predicted_retrieval = self.predict_performance(item, future_date)
            if predicted_retrieval < 0.70:  # Below 70% = at risk
                at_risk.append(AtRiskItem(
                    item=item,
                    predicted_retrievability=predicted_retrieval,
                    predicted_days_to_drop_below_70=self.days_to_threshold(item, 0.70),
                ))
        
        at_risk.sort(key=lambda x: x.predicted_retrievability)
        return at_risk
```

---

## 8.13 AI PROGRESS PREDICTOR

```python
class ProgressPredictor:
    """
    Machine learning model for predicting learning outcomes.
    Uses gradient boosting with engineered features from user history.
    """
    
    def predict_lesson_outcome(
        self, 
        user_id: str, 
        lesson: Lesson
    ) -> LessonOutcomePrediction:
        """
        Before a lesson starts: predicts expected accuracy, XP, and duration.
        Used to pre-personalize difficulty and motivational messaging.
        """
        features = self.engineer_features(user_id, lesson)
        
        # XGBoost model (trained on 100M+ historical lesson outcomes)
        predicted_accuracy = self.accuracy_model.predict(features)
        predicted_xp = self.xp_model.predict(features)
        predicted_duration = self.duration_model.predict(features)
        
        confidence = self.calculate_prediction_confidence(features, user_id)
        
        return LessonOutcomePrediction(
            expected_accuracy=predicted_accuracy,
            expected_xp=predicted_xp,
            expected_duration_minutes=predicted_duration,
            confidence=confidence,
            difficulty_recommendation=self.recommend_difficulty(predicted_accuracy),
        )
    
    def engineer_features(self, user_id: str, lesson: Lesson) -> np.ndarray:
        """
        Engineers 47 features for prediction model.
        """
        user = load_user(user_id)
        dna = load_learning_dna(user_id)
        recent_sessions = load_recent_sessions(user_id, days=14)
        
        return np.array([
            # User features (15)
            user.total_lessons_completed,
            user.current_streak,
            user.cefr_level_numeric,
            user.days_since_joined,
            user.daily_study_consistency,
            user.avg_accuracy_14d,
            user.accuracy_trend_slope,  # Improving or declining?
            user.avg_session_length_minutes,
            user.time_of_day_factor,    # Morning/evening performance diff
            user.days_since_last_study,
            dna.memory_profile.acquisition_speed,
            dna.memory_profile.decay_rate,
            dna.performance_profile.accuracy_by_type.get(lesson.primary_type, 0.7),
            user.last_session_accuracy,
            user.last_session_xp,
            
            # Lesson features (15)
            lesson.cefr_level_numeric,
            lesson.primary_exercise_type_numeric,
            lesson.vocabulary_item_count,
            lesson.grammar_item_count,
            lesson.review_item_count,
            lesson.new_item_count,
            lesson.avg_item_difficulty,
            lesson.topic_familiarity_score,  # How well user knows adjacent topics
            lesson.is_first_time,
            lesson.contains_confusable_pairs,
            lesson.speaking_exercise_count,
            lesson.estimated_duration,
            lesson.interference_risk_score,
            lesson.days_since_topic_practice,
            lesson.mastery_prerequisite_score,
            
            # Context features (10)
            day_of_week_encoding,
            hour_of_day,
            is_weekend,
            current_streak,
            days_to_goal,
            is_post_streak_broken,  # First session after breaking streak
            guild_event_active,
            active_power_ups,
            weather_pressure_factor,  # Optional: weather API integration
            battery_level_proxy,      # Session time patterns suggest device usage
            
            # Interaction features (7)
            accuracy_x_difficulty,
            streak_x_motivation,
            # ... derived interactions
        ])
```
