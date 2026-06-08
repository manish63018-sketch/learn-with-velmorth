# LEARN WITH VELMORTH — COMPLETE USER FLOW
## Section 5: Every Screen and Transition

---

## 5.1 SCREEN MAP OVERVIEW

```
[SPLASH] ──────────────────────────────────────────────────────────
     │
     ├──→ [Not Authenticated] ──→ [LOGIN SCREEN]
     │                                  │
     │                         ┌────────┴────────┐
     │                    [Google Login]    [Apple Login]
     │                         │              │
     │                         └──────┬───────┘
     │                                │
     │                    ┌───────────▼───────────────┐
     │                    │        ONBOARDING          │
     │                    │  1. Language Selection     │
     │                    │  2. Proficiency Assessment │
     │                    │  3. Goal Calibration       │
     │                    │  4. Learning Style Quiz    │
     │                    │  5. DNA Reveal             │
     │                    │  6. Study Plan             │
     │                    └───────────┬───────────────┘
     │                                │
     └──→ [Authenticated + Complete] ─┘
                                      │
                          ┌───────────▼──────────────────┐
                          │         MAIN SHELL           │
                          │  [Bottom Nav: 5 tabs]        │
                          └──┬──────┬──────┬──────┬──────┘
                             │      │      │      │
                         [HOME] [LEARN] [AI] [SOCIAL] [PROFILE]
```

---

## 5.2 SCREEN 1: SPLASH SCREEN

**Route**: `/splash`
**Duration**: 2.0 - 2.5 seconds (while bootstrapping)
**Purpose**: Brand imprint + session restoration + initialization

### Visual Design
- Background: `gradient-cosmic` (dark mode always, regardless of system setting)
- Center: Velmorth neural node logo (SVG, 80×80px)
- Animation Sequence:
  1. 0ms: Black screen
  2. 100ms: Logo fades in (opacity 0→1, 600ms ease-in)
  3. 700ms: Logo scale pulses (1.0→1.05→1.0, 400ms)
  4. 1100ms: "VELMORTH" wordmark slides up from below (Y: +20→0, opacity 0→1, 500ms ease-out)
  5. 1600ms: Tagline "Learn Beyond Limits" fades in (400ms)
  6. 2000ms: Loading indicator (3-dot pulse, bottom 20% of screen)
  7. 2500ms: Transition begins

### Background Tasks During Splash
- Check for valid auth tokens in secure storage
- Refresh access token if expired (silent)
- Fetch remote feature flags
- Initialize Hive database
- Check connectivity status
- Download latest app configuration

### Transitions
- To Login: Slide up and reveal (screen moves up, login screen revealed from below)
- To Dashboard: Logo animates to top-left corner position, content reveals from bottom

---

## 5.3 SCREEN 2: LOGIN SCREEN

**Route**: `/auth/login`
**Purpose**: Account creation and sign-in

### Visual Design
- Background: `gradient-cosmic` (full screen)
- Upper 45%: Hero illustration — Velmorth scholar avatar surrounded by floating language symbols (animating gently)
- Lower 55%: White (light) / dark card sheet that slides up from bottom

### Content Structure
```
[Hero Illustration with floating language symbols]
[VELMORTH wordmark — headline-md]
[Tagline: "Your AI-powered path to fluency" — body-md, secondary]

[Card Sheet — slides up on mount, 400ms spring]
  [Google Sign-In Button — full width, official branding]
  [Apple Sign-In Button — full width, official branding]
  [Divider: "or"]
  [Email/Password Form — toggleable, collapsed by default]
    [Email input field]
    [Password input field + show/hide toggle]
    [Forgot password? link]
    [Sign In button]
  [Toggle: "Don't have an account? Sign Up"]
  [Privacy Policy + Terms links — label-sm, secondary]
```

### Animations
- Language symbols: Each one floats independently (randomized amplitude 5-15px, period 2-4s)
- Card sheet: Slides up from -100% to 0 with spring physics on mount
- Form toggle: Smooth height animation (AnimatedSize widget)
- Error states: Red border + shake animation

### Screen Transitions
- Sign-in success (new user): Cross-fade to Onboarding screen 1
- Sign-in success (existing user): Hero logo contracts to top-left, dashboard expands from center

---

## 5.4 SCREEN 3: ONBOARDING — LANGUAGE SELECTION

**Route**: `/onboarding/language`
**Step**: 1 of 6

### Visual Design
- White/light background
- Top: Animated world map (low-poly style, countries glow when language selected)
- Progress indicator: 6 dots at top, step 1 highlighted

### Content
```
[Animated world map — 60% of screen]
[Title: "What do you want to learn?" — headline-lg]
[Search bar: "Search languages..."]
[Popular languages grid — 2 columns]:
  Each card: Country flag SVG + Language name + Native name
  Cards: Spanish, French, German, Japanese, Mandarin, Arabic, Portuguese, Korean, Italian, Hindi
[Show all 100+ languages toggle]
[Continue button — disabled until selection made]
```

### Behavior
- Country/language selection: Card scales to 1.05 + primary border appears + checkmark icon
- World map: Selected country's region glows with gradient (pulse animation)
- Multiple language selection supported (premium feature hint if more than 1)
- Search: Real-time filter, no API call needed (all 100 languages loaded locally)

---

## 5.5 SCREEN 4: ONBOARDING — PROFICIENCY ASSESSMENT

**Route**: `/onboarding/assessment`
**Step**: 2 of 6
**Duration**: 5-8 minutes

### Visual Design
- Top: Estimated level indicator (A0 → C2 spectrum, current position highlighted)
- Question area: 70% of screen
- Answer area: Bottom 30%

### Assessment Flow (Adaptive)
```
Question 1: "Do you know any [language]?"
  Options: Complete beginner | Know a little | Intermediate | Advanced | Near fluent
  
If "Complete beginner": Skip remaining, go to Goal Selection
If "Know a little": Start with A1 vocabulary recognition
If "Intermediate or above": Branch into A2/B1 questions

[Adaptive branching continues — 8-15 questions maximum]

Each question type cycles through:
  - Vocabulary recognition (see word → choose meaning)
  - Listening comprehension (hear phrase → choose meaning)
  - Reading (read sentence → answer question)
  - Translation (beginner-friendly for higher levels)
  - Grammar identification (intermediate+)

Real-time level meter: Updates after each answer
  "Our AI thinks you're at A1... A2... B1..."

Final screen: "Assessment Complete"
  Shows: Estimated level badge (A2, B1, etc.)
  "Your AI studied your patterns. You're stronger at vocabulary than grammar."
  [Start from my level] or [Start from the beginning]
```

### Gamification
- Timer per question (but no penalty for time — just data collection)
- Encouraging micro-copy after each question ("Nice! Keep going")
- Progress bar shows assessment completion

---

## 5.6 SCREEN 5: ONBOARDING — GOAL CALIBRATION

**Route**: `/onboarding/goals`
**Step**: 3 of 6

### Content
```
[Title: "What's your goal?" — headline-lg]
[Subtitle: "We'll build your perfect plan around it" — body-md]

[Goal Cards — scrollable horizontal or vertical grid]:
  ✈️ Travel & Adventure — "Navigate the world with confidence"
  💼 Career & Business — "Unlock global opportunities"
  🎓 Study & Academia — "Excel in school and exams"
  ❤️ Love & Relationships — "Connect with someone special"
  🎭 Culture & Arts — "Appreciate films, books, and music"
  🌍 Immigration — "Build a new life in a new country"
  🧠 Brain Training — "Keep your mind sharp"
  ✨ Just For Fun — "Because you want to"

[Multiple selection allowed]

[Sub-questions that appear after goal selection]:
  "When do you want to reach conversational fluency?"
    → Options: 3 months, 6 months, 1 year, I'll go at my pace
  
  "How much time can you study each day?"
    → Slider: 5 min / 10 min / 20 min / 30 min / 45 min / 1 hour+
  
  "What time do you usually study?"
    → Time picker with morning / afternoon / evening / night presets
```

### Visual
- Goal cards: 180×100px, gradient backgrounds unique to each goal
- Selected state: Card scales, checkmark appears, border glows
- Sub-questions: Slide in from right after primary selection

---

## 5.7 SCREEN 6: ONBOARDING — LEARNING STYLE QUIZ

**Route**: `/onboarding/learning-style`
**Step**: 4 of 6

### Content
```
8 illustrated scenario questions:

Q1: "You're learning a new word. What helps most?"
  A) 🖼️ Seeing a picture of it
  B) 🎵 Hearing it in a song
  C) ✍️ Writing it 10 times
  D) 📖 Reading it in a story

Q2: "When you're frustrated, what helps you most?"
  A) 💡 A clear explanation of why I was wrong
  B) 🎮 A fun mini-game to lighten the mood
  C) ⏭️ Skip it and come back later
  D) 🤗 Encouragement to try again

Q3: "How do you prefer challenges?"
  A) 🔥 Hard! Push me to my limits
  B) 📈 Gradual — just slightly above my level
  C) 😌 Easy — I like to build confidence
  D) 🎲 Surprise me — vary it!

Q4: "What's your ideal session length?"
  A) ⚡ 5-10 min quick bursts
  B) 📚 20-30 min focused sessions
  C) 🏃 45-60 min deep dives
  D) 🌊 Unlimited — I flow until done

[Questions 5-8 continue similarly — covering: social vs. solo preference, 
  visual vs. auditory, story-based vs. structured, competitive vs. collaborative]
```

### Visual
- Each question: Full-screen with large illustrated background
- Answer options: 4 large cards with icons
- Progress: Smooth progress bar
- Selected answer: Immediate spring-scale confirmation
- Swipe navigation: Swipe left/right between questions

---

## 5.8 SCREEN 7: ONBOARDING — LEARNING DNA REVEAL

**Route**: `/onboarding/dna-reveal`
**Step**: 5 of 6
**This is the "WOW" moment of onboarding**

### Sequence
```
Phase 1: AI Analysis (3 seconds)
  - Animated neural network visualization (lines connecting, nodes lighting up)
  - Text: "Velmorth AI is mapping your Learning DNA..."
  - Progress indicator: "Analyzing assessment patterns... ✓"
  - "Mapping cognitive style... ✓"
  - "Calibrating difficulty engine... ✓"
  - "Building your unique profile... ✓"

Phase 2: DNA Reveal (dramatic)
  - Screen transforms: Neural network morphs into a DNA double helix shape
  - DNA helix rotates slowly, glowing with primary gradient
  - Title animation: "YOUR LEARNING DNA" letter by letter
  
Phase 3: DNA Components Revealed (stagger 400ms each)
  Component reveals with icon + title + description:
  
  🧠 Cognitive Style: "Contextual Visual Learner"
     "You learn best when language is embedded in meaningful stories and images."
  
  ⚡ Learning Speed: "Rapid Acquirer / Deep Consolidator"
     "You pick up new material quickly but need thorough review for lasting memory."
  
  🎯 Challenge Tolerance: "Ambitious Learner"  
     "You thrive when pushed slightly beyond your comfort zone."
  
  🌅 Peak Time: "Morning Learner"
     "Your brain encodes language fastest between 7-10 AM."
  
  💪 Strengths: "Vocabulary & Listening"
     "Your pattern recognition for words is exceptional."
  
  🔧 Growth Areas: "Grammar Application & Speaking"
     "We'll build specific exercises to strengthen these."
  
  🎮 Learning Style: "Story-Driven Achiever"
     "Narrative context and reward progression keep you most engaged."

Phase 4: DNA Card
  - Full-screen shareable card with DNA visualization
  - User's name + all DNA attributes
  - "Share your Learning DNA" button
  - Subtle: "No two learners are the same" — building uniqueness feeling
```

---

## 5.9 SCREEN 8: ONBOARDING — STUDY PLAN REVEAL

**Route**: `/onboarding/study-plan`
**Step**: 6 of 6

### Content
```
[Title: "Your Personalized 90-Day Journey" — display-sm]

[Timeline visualization — scrollable]:
  Week 1-2: Foundation Builder
    → Topics: Greetings, Basic vocabulary, Present tense
    → Daily: 15 min | 3 lessons per day
    → Milestone: Order food in [language] ✓
  
  Week 3-6: Confidence Builder
    → Topics: Past tense, Questions, Numbers, Directions
    → Daily: 20 min | 4 lessons per day  
    → Milestone: Have a 5-minute conversation ✓
  
  Week 7-12: Fluency Sprint
    → Topics: Subjunctive, Complex vocabulary, Business language
    → Daily: 25 min | 5 lessons per day
    → Milestone: A2 Certificate 🏆

[AI Study Schedule]:
  "Your AI has set reminders for:"
  Mon-Fri: 8:30 AM (15 min morning session)
  Sat-Sun: 10:00 AM (30 min weekend deep dive)
  [Customize schedule button]

[Commit to your streak]:
  "Choose your starting commitment:"
  [5 min/day]  [10 min/day]  [20 min/day]  [30 min/day]
  Animated: fire icon glowing next to selected option

[Begin Learning button — primary, full width]
  Tap → Confetti burst + transition to dashboard
```

---

## 5.10 SCREEN 9: DASHBOARD (HOME TAB)

**Route**: `/dashboard`
**Purpose**: Daily learning hub and progress overview

### Layout (Mobile)
```
[Top Bar]
  Left: Velmorth logo (small) + "Good morning, [Name] 👋"
  Right: Gem count + notification bell + streak flame

[Hero Section — Daily Goal Card]
  Background: gradient-aurora
  Content: 
    Progress ring (circular) — daily minutes completed / goal
    "Today's Goal: 20 minutes"
    Remaining: "8 more minutes"
    [Continue Learning → button]
  
  Streak badge overlaid: "🔥 47 Day Streak"

[Quick Actions Row — horizontal scroll]:
  [📚 Daily Lesson] [🔄 Review] [🎤 Speak] [🤖 AI Tutor] [⚔️ Challenge]

[Progress Summary — Cards row]:
  Words Learned: 847
  Current Level: B1
  Mastery: 73%
  Weekly XP: 3,240

[Course Map — main feature]:
  Visual path of nodes (Duolingo-style but more beautiful)
  Current position: pulsing glow indicator
  Completed: solid, color-coded
  Available: normal
  Locked: grayed with lock icon
  Side quests: branch paths
  Lore locations: story-unlock gates

[AI Recommendations]:
  "Your AI noticed you struggled with subjunctive verbs last week"
  [Review Subjunctive — 5 min] button

[Activity Feed — from community]:
  "Maria completed Unit 5 — give her a 🎉"
  "Your guild 'Polyglots United' is 200 XP from daily goal"

[Leaderboard Teaser]:
  Mini leaderboard — top 5 this week
  User's rank highlighted
  [View Full Leaderboard] link

[Motivational AI Message]:
  AI-personalized daily message
  Today: "You've been consistent for 47 days. The neural pathways for Spanish 
  grammar are solidifying in your long-term memory. Keep going — you're 
  building something permanent."
```

### Dashboard Animations
- Page load: Stagger animation — each section slides up 60ms apart
- Progress ring: Circular animation from 0 to current value (1 second, ease-out)
- XP counter: Counts up from previous session value
- Course map: Nodes animate in sequentially on first load
- Streak flame: CSS-like flicker animation using Lottie

---

## 5.11 SCREEN 10: LESSON SCREEN

**Route**: `/lesson/:sessionId`
**Purpose**: Core learning experience

### Layout Structure
```
[Status Bar]
  Left: X (exit) button
  Center: Progress bar (segmented — 1 segment per exercise item)
  Right: ❤️❤️❤️ (hearts/lives)

[Exercise Area — 65% of screen]
  Dynamic based on exercise type:
  
  --- MULTIPLE CHOICE ---
  [Question prompt]
  [Optional: audio play button]
  [4 answer choices — vertical list]
  
  --- TRANSLATE ---
  [Source sentence (native language)]
  [Optional: tap words for hints]
  [Answer input field OR word bank]
  [Word bank: tap words to construct translation]
  
  --- SPEAK ---
  [Target phrase displayed]
  [Big microphone button — tap to record]
  [Recording waveform visualization]
  [Pronunciation score: A / B / C / retry]
  
  --- LISTEN & TYPE ---
  [Play audio button — large, centered]
  [Speed control: 0.75x / 1.0x / 1.25x]
  [Text input field]
  
  --- ARRANGE WORDS ---
  [Source sentence]
  [Answer slots — top half]
  [Word bank — bottom half]
  [Drag words up or tap to place]
  
  --- MATCH PAIRS ---
  [Left column: words in native]
  [Right column: translations, scrambled]
  [Draw connecting lines]
  [Color-coded matches: each pair unique color]

[Answer Overlay — slides from bottom]
  Correct: Green overlay
    "CORRECT! 🎉"
    [Explanation of grammar rule (if applicable)]
    [CONTINUE button]
  
  Wrong: Red overlay
    "ALMOST! The correct answer is..."
    [Show correct answer with highlight]
    [Explanation]
    [CONTINUE button]
  
  Hint: Yellow overlay (partial)
    [Hint text]
    [CONTINUE button]

[Bottom Area]
  [HINT button — left] (costs 1 hint token)
  [CHECK button — right, primary, disabled until answer selected]
```

### Lesson Session Management
- Session state persisted: User can close app and resume exactly where left off
- Auto-save: Every 30 seconds + on each answer submission
- Pause/Resume: Back button shows "Pause session?" dialog
- Heart system: Start with 5 hearts; lose 1 per wrong answer; replenish with time (1/hour) or gems
- Unlimited hearts: Premium feature

### Exercise Type Distribution per Session
```
Standard 15-item session:
  3× Multiple Choice (vocabulary/meaning)
  3× Translation (native → target or target → native, alternating)
  2× Listen & Type
  2× Arrange Words
  2× Speak
  2× Match Pairs
  1× Story Choice (narrative contextual item)
Order: Interleaved (never 2 same type in row)
```

---

## 5.12 SCREEN 11: LESSON RESULT SCREEN

**Route**: `/lesson/result/:sessionId`
**Purpose**: Celebration, stats, learning insights

### Animation Sequence
```
Phase 1: Score Reveal (0-1 second)
  - Star rating: Stars animate in one by one (scale 0→1.3→1.0 + sparkle)
  - 1 star: Any completion
  - 2 stars: < 3 mistakes
  - 3 stars: Perfect or 1 mistake
  
Phase 2: Stats Reveal (1-2 seconds)
  - Stagger: XP earned, Accuracy %, Time taken
  - XP: Counts up from 0 to earned amount
  - Streak update (if session maintained)
  
Phase 3: Insights (2-3 seconds)
  - AI-generated insight about the session
  - "You mastered 3 new words today! 'Subjuntivo' was your strongest new item."
  - Weakness flag: "You struggled with ser vs. estar — we'll review this tomorrow."

Phase 4: Actions
  - [Continue] — next lesson in sequence
  - [Review Mistakes] — see all wrong answers with explanations
  - [Share Result] — shareable card
  - [Dashboard] — return to home
```

### Confetti
- Perfect lesson: Full-screen confetti (Lottie animation, 3 seconds)
- Normal lesson: Smaller confetti burst from score area

---

## 5.13 SCREEN 12: AI TUTOR SCREEN

**Route**: `/ai-tutor`
**Purpose**: Conversational AI learning companion

### Layout
```
[Top Bar]
  AI Tutor avatar (animated, blinking)
  "Velmorth AI" title
  Mode selector chip: [Chat] [Practice] [Explain] [Quiz]
  
[Conversation Area — scrollable]
  Chat bubbles:
    AI messages: Left-aligned, gradient background, with avatar icon
    User messages: Right-aligned, primary color background
    
  Special message types:
    Exercise bubble: Inline exercise inside chat (answer in chat)
    Audio bubble: Playable audio message from AI
    Vocabulary card: Flip card animation for new words
    Grammar note: Expandable explanation card
    Cultural note: Story card with illustration

[Input Area]
  [Microphone button — hold to speak]
  [Text input field: "Ask anything..."]
  [Send button]
  [Quick reply chips — AI-suggested responses]

[Context Panel — expandable from right]
  Current topic being discussed
  Words/grammar introduced this session
  Quick reference grammar tables
```

### AI Tutor Modes

**Chat Mode**: Free conversation with AI. Language practice through dialogue.

**Practice Mode**: AI generates targeted exercises based on Learning DNA weak spots.

**Explain Mode**: Ask the AI to explain any grammar rule, vocabulary, or concept in depth.

**Quiz Mode**: AI gives a timed quiz on recent topics, grades performance, provides explanations.

### Conversation Context
- AI maintains full session history
- AI has access to learner's Learning DNA, recent lesson performance, weak areas
- AI adjusts response language complexity to match learner's level
- AI tracks vocabulary introduced in conversation, prevents overwhelming the learner
- AI naturally introduces grammar structures appropriate to level

---

## 5.14 SCREEN 13: LEADERBOARD

**Route**: `/leaderboard`

### Tabs
```
[Weekly] [Monthly] [All Time] [Friends] [Guild]

Weekly Leaderboard (default):
  [My Rank Card — prominent, full-width]
    Rank: #24 of 45,231 learners
    XP This Week: 3,240
    Change: ↑ 8 positions from last week
  
  [Top 10 List]:
    Each row: Rank number + Avatar + Name + XP + Country flag
    #1-3: Special crown/medal icons
    User's row: Highlighted in primary color
    
    Rank animations:
      Position changed since last view: Row slides in from new position
      New achievement: Badge floats over avatar
  
  [Ranks Below User — 5 rows]
    Motivational: "You're only 200 XP ahead of #25"
  
  [Rival Feature]:
    "Your Rival This Week: @spanish_student_2024"
    Their XP vs yours in real-time
```

### Leaderboard Gamification
- Promotion zones: Top 5 get promoted to next tier league
- Demotion zones: Bottom 3 get demoted (Duolingo-style)
- League tiers: Bronze → Silver → Gold → Platinum → Diamond → Obsidian → Legendary
- Weekly reset: Sunday midnight UTC

---

## 5.15 SCREEN 14: ACHIEVEMENTS

**Route**: `/achievements`

### Layout
```
[Header]: Achievement score (e.g., "312 / 500 achievements unlocked")
[XP From Achievements]: Total XP earned from achievements

[Filter Tabs]: [All] [Unlocked] [In Progress] [Locked]

[Categories — horizontal scroll]:
  🎯 Learning Milestones | ⚡ Speed Records | 🏆 Mastery | 
  👥 Social | 🔥 Streaks | 🌍 Exploration | 🎭 Story | 🏅 Special

[Achievement Grid — 2 columns]:
  Each achievement card:
    - Badge icon (full color if unlocked, grayscale if locked)
    - Name
    - Description
    - XP reward
    - Progress bar (if in progress)
    - Unlock date (if unlocked)
  
  Locked achievements: Blurred description (mystery) OR shown as teaser
  Unlocked: Glowing gold border + unlock animation replay button

[Featured Achievement — pinned top]
  Currently in progress — shows exactly what's needed to unlock
```

---

## 5.16 SCREEN 15: GUILD SCREEN

**Route**: `/community/guild`

### Layout
```
[Guild Header Card — gradient background in guild colors]
  Guild emblem (custom or template)
  Guild name + rank badge
  Members: 24/50 | Weekly XP: 45,200 | League: Gold

[Member List — scrollable]
  Each member: Avatar + Name + Weekly XP contribution + Role (Leader/Officer/Member)
  Your row highlighted

[Guild Missions — cards]:
  🗡️ Weekly Quest: "Complete 500 collective lessons" — 73% complete
  ⚔️ Boss Battle: "Defeat the Grammar Dragon" — 3 days remaining
  🌪️ Raid: "Spanish Weekend Raid" — Starts in 6 hours

[Guild Chat — mini feed]:
  Last 5 messages
  [Open Full Chat] button

[Guild Shop]:
  Buy guild cosmetics with collective gems

[Guild Wars]:
  Current opponent guild
  Your guild XP vs opponent XP
  Battle ends: 48h countdown

[Actions]:
  [Contribute XP] — Start learning to add to guild total
  [Invite Member] — Share guild code
```

---

## 5.17 SCREEN 16: PROFILE SCREEN

**Route**: `/profile`

### Layout
```
[Hero Section]:
  Avatar (animated if premium avatar)
  Name + Username
  Country flag + Language badges
  Bio (140 char)
  [Edit Profile] button

[Stats Grid]:
  Total XP | Current Level | Day Streak | Languages
  Words Known | Lessons Done | Perfect Lessons | Achievements

[Language Progress]:
  Each language: Flag + Current Level + Progress bar
  CEFR estimate: A2, B1, etc.

[Achievement Showcase]:
  3 pinned achievements (user selects)
  [View All 312] link

[Activity Heatmap]:
  365-day GitHub-style contribution graph
  Color = XP earned that day
  Hover: tooltip with date + XP

[Badge Wall]:
  All earned badges in scrollable grid

[Social]:
  Followers: 234 | Following: 189
  [View Friends] link

[Guild Membership]:
  Guild card preview

[Settings shortcuts]:
  Notification settings
  Account settings  
  Premium status
```

---

## 5.18 SCREEN 17: ANALYTICS SCREEN

**Route**: `/analytics`

### Content
```
[My Learning Analytics — learner-facing dashboard]

[Summary Cards — top]:
  This Month: Total time, XP earned, Accuracy rate, Words added

[Learning Velocity Chart]:
  Line chart: Words mastered per week (8-week view)
  AI interpretation: "Your learning speed is in the top 15% of similar learners"

[Accuracy Breakdown]:
  Radar chart: Vocabulary / Grammar / Listening / Reading / Speaking
  Strongest: "Vocabulary (87%)"
  Growth area: "Grammar (62%)"

[Retention Report]:
  "Your long-term retention rate: 78%"
  "Items you'll likely forget this week: 23"
  [Start Review Session] button

[Session Patterns]:
  Bar chart: Sessions by day of week + time of day heatmap
  "You study most consistently on Tuesdays at 8 AM"
  "Your longest sessions are on weekends — consider a weekend challenge"

[Struggle Words]:
  Top 10 most missed vocabulary items
  Each: word, miss count, last practiced
  [Review All] button

[AI Prediction]:
  "At your current pace, you'll reach B2 in approximately 4 months and 12 days"
  "To reach B2 in 3 months, you'd need to study 8 more minutes per day"
  [Adjust my goal] button

[Comparative Insight]:
  "Compared to learners with your same starting level at the same time:"
  Your progress vs. cohort average (chart)
```

---

## 5.19 SCREEN 18: SETTINGS SCREEN

**Route**: `/settings`

### Content
```
[Profile Settings]
  Name, Email, Profile picture, Bio, Country

[Learning Preferences]
  Interface language
  Translation language
  Daily goal (minutes)
  Study reminder time
  Streak protection (grace period on/off)
  Exercise types (toggle on/off specific types)

[AI Tutor Settings]
  Tutor personality: [Strict Teacher] [Friendly Coach] [Casual Friend]
  Feedback verbosity: [Concise] [Detailed] [Extra Detailed]
  Language of explanations: [English] [Target language] [Both]

[Accessibility]
  Font size
  High contrast mode
  Reduced motion
  Dyslexia font mode
  Color blind mode
  Screen reader optimization

[Notifications]
  Master toggle
  Streak reminder: [Time picker]
  Daily lesson reminder: [Time picker]
  Friend activity
  Guild notifications
  Achievement unlocks
  Marketing (opt-in)

[Privacy & Security]
  Two-factor authentication
  Connected accounts (Google, Apple)
  Download my data (GDPR)
  Delete account

[Premium]
  Current plan display
  Manage subscription (→ store)
  Restore purchases

[About]
  Version, Terms, Privacy Policy, Licenses, Contact support
```

---

## 5.20 SCREEN 19: SHOP / STORE

**Route**: `/shop`

### Sections
```
[Gems Balance] + [Subscription Status]

[Featured] — promotional banner

[Power-Ups Tab]:
  Heart Refill: 500 gems (5 hearts immediately)
  Streak Freeze: 800 gems (protects streak for 1 missed day)
  Double XP: 1,000 gems (2x XP for 30 minutes)
  Unlimited Hearts: 1,200 gems (1 day)
  Hint Tokens: 200 gems (5 hints)

[Cosmetics Tab]:
  Avatar borders (Bronze/Silver/Gold/Animated)
  Profile themes
  Custom lesson mascots
  Guild badge upgrades
  Special streak effects

[Premium Tab]:
  Premium Monthly: $9.99/month — feature list
  Premium Annual: $79.99/year (save 33%) — BEST VALUE badge
  Family Plan: $14.99/month (up to 6 members)
  [Comparison table: Free vs Premium]

[Bundles]:
  Starter Pack: Gems + streak freeze + 1 week premium ($2.99)
  Power Pack: Lots of gems + 1 month premium ($12.99)

[Gem Packs]:
  100 gems: $0.99
  550 gems (+10%): $4.99
  1,200 gems (+20%): $9.99
  2,800 gems (+40%): $19.99
  6,000 gems (+60%): $39.99
```
