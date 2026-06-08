# LEARN WITH VELMORTH — ADVANCED GAMIFICATION
## Section 7: Complete Gamification Architecture

---

## 7.1 GAMIFICATION PHILOSOPHY

Velmorth's gamification is not a thin layer of points and badges bolted onto a learning app. It is a **deeply integrated engagement ecosystem** designed by applying game design principles from the most addictive mobile games (Clash of Clans, Pokémon GO, Candy Crush) combined with behavioral psychology research (BJ Fogg's Behavior Model, Self-Determination Theory, Operant Conditioning).

**Core principles**:
1. **Every action must feel meaningful**: XP is not just a number — it represents real knowledge gained
2. **Loss aversion over gain motivation**: Fear of losing a streak is more powerful than hope of gaining XP
3. **Variable reward schedules**: Unpredictable rewards are more addictive than predictable ones
4. **Social competition with privacy**: Compete with anonymous cohorts + opt-in friend competition
5. **Progress visibility**: Always show how close the user is to the next milestone
6. **Multiple progression axes**: Even on a bad day, you can progress on SOMETHING

---

## 7.2 XP SYSTEM

### XP Earning Rules (Complete)
```
LESSON XP:
  Complete a lesson (standard):          +50 XP base
  Perfect lesson (no mistakes):          +100 XP (+50 bonus)
  Speed bonus (completed quickly):       +10-30 XP (proportional)
  Difficulty bonus (+1 CEFR level):     +25 XP per level gap
  First completion of lesson:            +20 XP (novelty bonus)
  Lesson review session:                 +30 XP base
  
STREAK XP:
  Maintaining daily streak (each day):   +10 × streak_milestone_multiplier XP
  7-day streak milestone:               +150 XP bonus
  30-day streak milestone:              +500 XP bonus
  100-day streak milestone:             +2,000 XP bonus
  365-day streak milestone:             +10,000 XP bonus
  
CHALLENGE XP:
  Daily Mission (each):                  +50-200 XP (varies)
  Weekly Mission (each):                 +200-500 XP (varies)
  Boss Battle victory:                   +500-2,000 XP (tiered)
  Guild Raid contribution:               +1,000-5,000 XP (based on contribution)
  
MASTERY XP:
  Item introduced for first time:        +5 XP
  Item reaches "learning" level:         +10 XP
  Item reaches "consolidating" level:    +25 XP
  Item reaches "mastered" level:         +50 XP
  Item reaches "long_term" level:        +100 XP
  
SOCIAL XP:
  Writing a helpful community post:      +20 XP (up to 3×/day)
  Helping a guild member:                +15 XP
  Accepting and completing a challenge:  +30 XP
  
AI ENGAGEMENT XP:
  AI Tutor conversation (per 5 min):    +20 XP (up to 3×/day)
  Voice practice session:               +40 XP base
  Perfect pronunciation score:          +30 XP bonus
  
MULTIPLIERS (stack):
  Double XP Power-Up (active):          ×2.0
  Weekend Warrior (weekend sessions):    ×1.5
  Guild Bonus (active guild event):      ×1.2
  Premium Member:                        ×1.2
  Streak Multiplier (30+ day streak):    ×1.1
  
  Maximum multiplier: ×3.0 (capped)
```

### XP Ledger Architecture
```sql
-- Every XP event is recorded immutably
CREATE TABLE xp_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    amount INTEGER NOT NULL,          -- Can be negative (cheating penalty)
    source_type VARCHAR(50) NOT NULL, -- 'lesson', 'achievement', 'streak', etc.
    source_id UUID,                   -- Reference to source entity
    multipliers JSONB,                -- Applied multipliers at time of award
    net_amount INTEGER NOT NULL,      -- After multipliers
    created_at TIMESTAMPTZ DEFAULT now(),
    season_id INTEGER                 -- Which season this belongs to
);

-- Aggregated XP view (maintained by triggers + periodic recalculation)
CREATE TABLE user_xp_summary (
    user_id UUID PRIMARY KEY,
    total_xp BIGINT DEFAULT 0,
    current_level INTEGER DEFAULT 1,
    xp_to_next_level INTEGER,
    current_season_xp INTEGER DEFAULT 0,
    weekly_xp INTEGER DEFAULT 0,
    daily_xp INTEGER DEFAULT 0,
    last_recalculated TIMESTAMPTZ
);
```

---

## 7.3 LEVEL SYSTEM

### 100-Level Progression System
```
TIER 1: WANDERER (Levels 1-10)
  Level 1:  "First Step"          (0 XP)
  Level 2:  "Curious Learner"     (300 XP)
  Level 3:  "Word Collector"      (700 XP)
  Level 4:  "Phrase Builder"      (1,400 XP)
  Level 5:  "Pattern Seeker"      (2,500 XP)
  Level 6:  "Sentence Former"     (4,000 XP)
  Level 7:  "Story Follower"      (6,000 XP)
  Level 8:  "Context Grasper"     (8,500 XP)
  Level 9:  "Grammar Apprentice"  (11,500 XP)
  Level 10: "Language Initiate"   (15,000 XP) ← TIER MILESTONE: Bronze Badge
  Reward: "Wanderer" avatar border unlocked

TIER 2: STUDENT (Levels 11-25)
  Level 11-25: Increasing XP gates (15k → 75k total XP)
  Level 25: "Language Scholar" ← TIER MILESTONE: Silver Badge
  Reward: Silver avatar border + Scholar title

TIER 3: ADEPT (Levels 26-50)
  Levels 26-50: 75k → 350k total XP
  Level 50: "Language Adept" ← TIER MILESTONE: Gold Badge
  Reward: Gold avatar border + Animated flame effect + Adept title

TIER 4: MASTER (Levels 51-75)
  Levels 51-75: 350k → 1.2M total XP
  Level 75: "Language Master" ← TIER MILESTONE: Diamond Badge
  Reward: Diamond avatar border + Unique animated avatar + Master title

TIER 5: LEGEND (Levels 76-100)
  Levels 76-100: 1.2M → 5M total XP
  Level 100: "Velmorth Legend" ← ULTIMATE MILESTONE
  Reward: Custom legend border + "Legend" crown + Exclusive avatar + 5,000 gems

Level XP Formula:
  xp_for_level(n) = 50 * n^1.8 + 200 * n - 250
  This creates a natural acceleration that feels fair (not exponential grind)
```

### Level-Up Rewards (Every 5 Levels)
| Milestone | Reward |
|---|---|
| Level 5 | +500 gems + Streak freeze |
| Level 10 | +1000 gems + Bronze avatar border + "Initiate" title |
| Level 15 | +1500 gems + 3-day premium trial |
| Level 20 | +2000 gems + New mascot pet |
| Level 25 | +3000 gems + Silver avatar border + "Scholar" title |
| Level 30 | +5000 gems + Special animated badge |
| Level 50 | +10000 gems + Gold border + Custom pet skin |
| Level 75 | +20000 gems + Diamond border + Unique avatar |
| Level 100 | +50000 gems + Legend crown + Exclusive features for life |

---

## 7.4 RANK / LEAGUE SYSTEM

### Weekly League System
```
LEAGUES (7 tiers):
  Bronze League    — Default for new users
  Silver League    — Top 50% of Bronze this week
  Gold League      — Top 30% of Silver
  Platinum League  — Top 20% of Gold
  Diamond League   — Top 10% of Platinum
  Obsidian League  — Top 5% of Diamond
  Legendary League — Top 1% of all (global elite)

LEAGUE MECHANICS:
  League resets: Every Sunday midnight UTC
  Promotion: Top N% get promoted (as above)
  Demotion: Bottom 10% of each league get demoted (except Bronze — no demotion from Bronze)
  Size: Each league cohort = 50 users (matched by language + level range)
  
LEAGUE XP BONUSES:
  Bronze: No bonus
  Silver: +5% XP from all sources  
  Gold: +10% XP
  Platinum: +15% XP
  Diamond: +20% XP + Exclusive Diamond badge
  Obsidian: +25% XP + Exclusive avatar frame + Obsidian title
  Legendary: +30% XP + "Legendary" prefix + Access to Legendary-only challenges

PROMOTION CELEBRATION:
  Animated promotion card with old league → new league transformation
  Social share option: "I just promoted to Diamond League! 💎"
```

---

## 7.5 ACHIEVEMENT SYSTEM

### Complete Achievement Catalog (150+ Achievements)

#### Category: Streaks
```
🔥 First Flame          — Complete your first day streak (Day 1)
🔥 Smoldering           — 7-day streak
🔥 Burning Bright       — 30-day streak (+500 gems)
🔥 Unstoppable Force    — 100-day streak (+2,000 gems)
🔥 Eternal Flame        — 365-day streak (+10,000 gems + exclusive badge)
🔥 Phoenix              — Rebuild a streak after losing a 30+ day streak
🔥 Marathon Runner      — Total: 500 days of streaks (lifetime, not consecutive)
```

#### Category: Learning Milestones
```
📚 First Words          — Learn 10 vocabulary items
📚 Growing Vocabulary   — Learn 100 words
📚 Word Hoarder         — Learn 500 words
📚 Vocabulary Master    — Learn 1,000 words
📚 Walking Dictionary   — Learn 5,000 words
📚 Fluency Dawn         — Reach A2 level (verified)
📚 Halfway to Fluent    — Reach B1 level
📚 Conversational       — Reach B2 level
📚 Near Native          — Reach C1 level
📚 Mastery Achieved     — Reach C2 level
📚 Polyglot             — Reach B1 in 3 different languages
```

#### Category: Accuracy Records
```
⭐ Sharp Shooter        — 10 perfect lessons in a row
⭐ Sniper               — 95%+ accuracy for 7 days
⭐ Flawless             — 100% accuracy in 50 total lessons
⭐ Speed Runner         — Complete a lesson in under 2 minutes
⭐ Lightning            — Answer 10 questions in under 30 seconds
⭐ Iron Mind            — Never use a hint for 30 days
```

#### Category: Social
```
👥 First Friend         — Add your first friend
👥 Squad Goals          — Join a guild
👥 Loyal Member         — Stay in same guild for 30 days
👥 Guild Hero           — Top contributor in guild for a week
👥 Challenge Champion   — Win 10 challenges against friends
👥 Mentor              — Complete 10 mentoring sessions
👥 Community Pillar     — 100 helpful community posts (rated 4+ stars)
```

#### Category: Exploration
```
🌍 Worldly             — Learn 5 different languages (any level)
🌍 Cultural Adventurer  — Complete cultural notes in 3 different language courses
🌍 Story Reader         — Complete 10 AI Story Mode chapters
🌍 Lore Keeper          — Unlock all story locations in 1 course
🌍 Night Owl           — Complete a lesson after midnight
🌍 Early Bird           — Complete a lesson before 6 AM
🌍 Weekend Warrior      — Complete lessons every Saturday and Sunday for 8 weeks
```

#### Category: Special / Hidden
```
🎯 Perfectionist        — Hidden — Get 100% on 100 consecutive items
🎯 Linguist Royale      — Hidden — Finish in Top 3 of a Legendary League
🎯 Dragon Slayer        — Hidden — Beat the hardest Boss Battle
🎯 Shadow Learner       — Hidden — Complete 50 lessons at midnight or later
🎯 The Legend Returns   — Hidden — Rebuild a 100+ day streak after losing it
🎯 Velmorth Champion    — Hidden — Master 2,500 vocabulary items
```

---

## 7.6 GUILDS SYSTEM

### Guild Architecture
```
GUILD STRUCTURE:
  Max 50 members per guild
  Roles: Leader (1), Officers (up to 5), Members (rest)
  Each guild has: Name, Custom emblem, Description, Language focus (optional)
  
GUILD CREATION:
  Cost: 1,000 gems (prevents spam guilds)
  Requires: Level 10+
  
GUILD TIERS (by collective weekly XP):
  Starter: < 100,000 weekly XP
  Bronze: 100,000 - 500,000 XP
  Silver: 500,000 - 1,500,000 XP
  Gold: 1,500,000 - 5,000,000 XP
  Platinum: 5,000,000 - 15,000,000 XP
  Elite: 15,000,000+ XP
  
GUILD BENEFITS BY TIER:
  Bronze: +5% XP for all members, access to Bronze raids
  Silver: +10% XP, Silver raid access, guild cosmetic unlocks
  Gold: +15% XP, Gold raids, animated guild banner
  Platinum: +20% XP, exclusive Platinum raids, custom guild avatar
  Elite: +25% XP, all raids, unique Elite badge, featured in discover
  
GUILD WEEKLY ACTIVITY:
  Day 1-5: Collect guild XP (sum of all members' XP)
  Day 6: Boss Battle (collective challenge, all contribute)
  Day 7: Results + Rank update + Rewards distributed
  
GUILD WARS:
  Duration: 72 hours
  Format: Your guild vs. another guild (similar tier)
  Metric: Collective XP earned during war period
  Reward: Winner gets 2,000 guild gems (distributed) + War badge
  
GUILD SHOP:
  Guild Bank: Shared gem pool (members can donate)
  Purchasable: Guild emblem upgrades, animated banners, member XP boosts
```

---

## 7.7 BOSS BATTLES

### Boss Battle System
Boss Battles are timed, high-stakes collective challenges that the entire Velmorth community (or individual guilds) fights together. Each "Boss" represents a challenging language concept or skill.

```
BOSS ANATOMY:
  Boss Name: "The Grammar Dragon of Subjunctive Peril"
  Language: Spanish
  Skill Focus: Present Subjunctive
  HP: 1,000,000 (scales with participant count)
  Duration: 48 hours
  
DAMAGE SYSTEM:
  Each correct answer in a relevant lesson: -1 HP
  Streak bonus damage: ×1.5 HP per correct answer (active streak)
  Perfect lesson: -50 HP (mega damage)
  Wrong answer: +5 HP (heals the boss)
  Speed kill: -2 HP per second faster than average
  
BOSS PHASES:
  Phase 1 (100%-60% HP): Standard battles
  Phase 2 (60%-30% HP): Boss attacks! Wrong answers cost 2 hearts
  Phase 3 (30%-0% HP): "Berserk Mode" — time pressure added to every question
  
REWARDS:
  All participants: Participation badge + 500 XP
  Top 10% contributors: Boss badge + 2,000 XP + 500 gems
  Top 1% contributors: Legendary boss badge + 5,000 XP + 2,000 gems
  Guild that contributes most: Guild trophy + bonus gems
  
BOSS SCHEDULE:
  Global Boss: Every 2 weeks (language rotation)
  Guild Boss: Weekly (guild-specific, smaller HP pool)
  Special Event Boss: Seasonal (Halloween, New Year, etc.)
  
BOSS TYPES:
  Grammar Dragon: Tests specific grammar rules
  Vocabulary Golem: Tests high-frequency vocabulary
  Pronunciation Hydra: Voice exercises; each head = a difficult phoneme
  Culture Sphinx: Cultural knowledge questions
  Speed Demon: Time-pressure challenges
```

---

## 7.8 RAID SYSTEM

### Language Raids
Raids are collaborative real-time events where groups of learners work together to achieve a goal within a fixed time window.

```
RAID TYPES:

1. WEEKEND RAID (Recurring)
   Trigger: Every Saturday 00:00 UTC
   Duration: 48 hours
   Format: Global participation grouped by language
   Goal: Collectively reach a XP target
   Reward: Bonus XP multiplier (×1.5) for the weekend + exclusive raid badge
   
2. VOCABULARY BLITZ
   Duration: 24 hours
   Format: Learn as many words as possible collectively
   Goal: 1,000,000 new vocabulary items mastered globally
   Theme: Specific topic (e.g., "The Great Food Raid — Learn Food Vocabulary")
   
3. SPEAKING SURGE  
   Duration: 8 hours
   Format: Complete speaking exercises
   Leaderboard: Individual speaking exercises completed
   Reward: Voice Coach badge + gems
   
4. GUILD RAID
   Duration: 24 hours
   Format: Guild-specific target
   Goal: Each member completes their individual quota
   If goal met: All members get +500 gems + Guild XP bonus
   
5. CULTURAL EXPEDITION (Monthly)
   Theme: Deep dive into a specific culture/country
   Activities: Cultural notes, cultural vocabulary, idiom mastery
   Duration: 7 days
   Reward: Country badge + Cultural Ambassador title
   
RAID LEADERBOARD:
  Real-time leaderboard showing top contributors
  Personal contribution meter
  Guild contribution comparison
  Time remaining countdown (high urgency)
```

---

## 7.9 SEASON PASS

### Season Structure (12 weeks per season)
```
SEASON THEME EXAMPLES:
  Season 1: "The Grand Expedition" — Explore the world of Romance languages
  Season 2: "Digital Nomad" — Business and technology vocabulary
  Season 3: "Ancient Echoes" — Historical and classical language
  Season 4: "The Polyglot Games" — Multi-language Olympics

FREE TRACK (all users):
  Season XP milestones (every 100 XP): Minor rewards
  Milestone 1: +50 gems
  Milestone 5: Seasonal badge
  Milestone 10: Streak freeze
  Milestone 20: +200 gems
  Milestone 30: Seasonal avatar border
  Milestone 50: Season title
  
PREMIUM TRACK ($9.99/season or included in Premium subscription):
  All free rewards PLUS:
  Premium-only cosmetic rewards every 2 milestones
  Exclusive season mascot pet
  2× season XP for all activities
  Early access to next season content
  Premium-only guild raid participation
  Season-exclusive avatar frame (animated)
  
SEASON FINALE:
  Final week: 2× season XP for all activities
  Final day: 4× season XP (all-hands)
  Season champion rewards: Top 100 globally get exclusive "Season Champion" badge
  
SEASON XP SOURCES:
  Normal XP: Converts 1:1 to season XP
  Challenges completed: +50 season XP
  Boss battle participation: +200 season XP
  Raid completion: +500 season XP
  Perfect week: +100 season XP
```

---

## 7.10 DAILY AND WEEKLY MISSIONS

### Daily Mission Generator
```python
class MissionGenerator:
    
    def generate_daily_missions(self, user_id: str, date: date) -> List[Mission]:
        dna = load_learning_dna(user_id)
        user_stats = load_user_stats(user_id)
        
        missions = []
        
        # Mission 1: Core Learning (always present, calibrated to DNA)
        missions.append(Mission(
            type=MissionType.LESSON_COMPLETION,
            target=3,  # 3 lessons today
            xp_reward=150,
            gem_reward=0,
            description="Complete 3 lessons today",
            icon="📚",
            difficulty=MissionDifficulty.MEDIUM
        ))
        
        # Mission 2: Skill-specific (targets current weak area)
        weak_skill = dna.error_profile.current_weakest_skill
        missions.append(Mission(
            type=MissionType.SKILL_PRACTICE,
            target_skill=weak_skill,
            target=5,  # 5 correct answers in weak skill
            xp_reward=200,
            gem_reward=50,
            description=f"Nail 5 {weak_skill} questions",
            icon=get_skill_icon(weak_skill),
            difficulty=MissionDifficulty.HARD
        ))
        
        # Mission 3: Surprise mission (variable ratio — different each day)
        surprise_type = random.choice([
            MissionType.PERFECT_LESSON,       # "Complete a perfect lesson (no mistakes)"
            MissionType.SPEED_CHALLENGE,      # "Answer 10 questions in under 3 minutes"
            MissionType.VOICE_PRACTICE,       # "Complete 2 speaking exercises"
            MissionType.AI_TUTOR_SESSION,     # "Have a 5-minute AI Tutor conversation"
            MissionType.STREAK_EXTENSION,     # "Review vocabulary 3 days in a row"
            MissionType.VOCABULARY_MILESTONE, # "Learn 20 new words today"
            MissionType.COMMUNITY,            # "Encourage 3 friends"
            MissionType.GUILD_CONTRIBUTION,   # "Contribute 500 XP to your guild"
        ])
        missions.append(self.build_surprise_mission(surprise_type, user_stats))
        
        return missions
    
    def generate_weekly_missions(self, user_id: str, week_start: date) -> List[Mission]:
        # 5 weekly missions, harder targets, bigger rewards
        # Unlocked one per day (to prevent all-Sunday grind)
        return [
            Mission("Complete 20 lessons this week", xp=1000, gems=200),
            Mission("Achieve 80%+ accuracy for 5 consecutive lessons", xp=800, gems=150),
            Mission("Maintain your streak all 7 days", xp=500, gems=100),
            Mission("Complete a Boss Battle", xp=600, gems=200),
            Mission("Win a challenge against a friend", xp=400, gems=100),
        ]
```

---

## 7.11 AVATAR AND PET SYSTEM

### Avatar Evolution
```
BASE AVATAR:
  User selects from 20 base avatar designs at signup (diverse, inclusive)
  Each avatar has a unique art style: human, fantasy, geometric, animal, etc.

AVATAR CUSTOMIZATION LAYERS:
  1. Base character (20 options, more unlockable)
  2. Avatar border (level-based + gem-purchasable + seasonal)
  3. Background (unlockable through achievements)
  4. Effect (animated — exclusive for Diamond+)
  5. Accessories (purchasable with gems)

AVATAR EVOLUTION (DNA-linked):
  As learner's level increases, their avatar subtly evolves
  Level 1-10: Base form
  Level 11-25: Avatar gains subtle glow
  Level 26-50: Animated breathing effect
  Level 51-75: Floating light particles
  Level 76-100: Full legendary animated aura

PET SYSTEM:
  Each user has a companion pet (AI character that learns alongside them)
  Pet name: Customizable
  Pet species: Unlockable (starts with "Young Scholar Owl")
  
  Pet progression:
  Level 1: Tiny, curious, basic animations
  Level 10: Grows, more expressive, new emotes
  Level 25: Adolescent size, can perform tricks
  Level 50: Adult, full personality, wears language-themed accessories
  Level 100: Legendary mythical form (unique to user)
  
  Pet mechanics:
  - Pet earns XP alongside owner (same activities)
  - Neglect (no study): Pet shows sad animations → motivation mechanic
  - Daily care: Feed pet a "knowledge crystal" (costs 1 minute of studying)
  - Pet sends encouraging notifications when idle: "Your owl misses learning with you!"
  - Rare pet species: Drop from Boss Battles and special events
```

---

## 7.12 STREAK SYSTEM

### Streak Architecture
```python
class StreakEngine:
    GRACE_PERIOD_HOURS = 4      # 4 extra hours on missed day (8 PM → midnight extension)
    MAX_FREEZES = 5             # Maximum active streak freezes a user can hold
    FREEZE_DURATION_DAYS = 1    # 1 freeze = 1 day protection
    
    def process_day_completion(self, user_id: str) -> StreakUpdate:
        user = load_user(user_id)
        today = date.today()
        
        # Has user studied enough today? (minimum: daily goal or 5 minutes, whichever is higher)
        today_minutes = get_study_minutes_today(user_id, today)
        daily_goal = user.daily_goal_minutes
        
        if today_minutes >= max(5, daily_goal):
            if user.streak_last_updated == today - timedelta(days=1):
                # Consecutive day — extend streak
                new_streak = user.current_streak + 1
                self.check_streak_milestones(user_id, new_streak)
            elif user.streak_last_updated == today:
                # Already updated today — no change
                new_streak = user.current_streak
            else:
                # Gap of more than 1 day
                if self.check_freeze_active(user_id, user.streak_last_updated, today):
                    # Freeze protected this day
                    new_streak = user.current_streak + 1
                    self.consume_freeze(user_id)
                else:
                    # Streak broken
                    old_streak = user.current_streak
                    new_streak = 1  # Reset to 1 (today counts)
                    self.handle_streak_broken(user_id, old_streak)
        else:
            # Did not complete daily goal today
            # Check grace period (if within 4 hours of midnight, allow)
            if self.is_in_grace_period():
                return StreakUpdate(status="grace_period", streak=user.current_streak)
            new_streak = user.current_streak  # Unchanged (processed at midnight)
        
        # Update database
        self.update_streak(user_id, new_streak, today)
        
        return StreakUpdate(
            status="success",
            streak=new_streak,
            xp_awarded=self.calculate_streak_xp(new_streak),
        )
    
    def handle_streak_broken(self, user_id: str, broken_streak: int):
        # Send emotional support notification
        # If streak was large (30+), offer streak repair (paid)
        # Create "Phoenix" achievement path
        # Trigger AI Coach motivational message
        if broken_streak >= 30:
            offer_streak_repair(user_id, broken_streak)  # Cost: 500-2000 gems
            send_ai_motivation_message(user_id, event="streak_broken", streak=broken_streak)
        
        # If this is a recurring pattern, AI Coach creates intervention
        if self.count_broken_streaks(user_id, days=90) >= 3:
            trigger_ai_coach_intervention(user_id, issue="streak_maintenance")
```

### Streak Visual System
```
Streak Display:
  0: No streak indicator
  1-6: Small flame icon, counter
  7-29: Medium flame, subtle glow, different color per tier
  30-99: Large flame, animated, golden glow
  100+: Epic flame with particle effects, unique color (electric blue)
  365+: Legendary "Eternal Flame" — unique mythical animation

Streak Heat Map:
  Profile shows 365-day GitHub-style grid
  Color intensity = study duration that day
  Patterns: users can spot their own consistency visually
```

---

## 7.13 SOCIAL COMPETITION SYSTEM

### Friend Challenges
```
CHALLENGE TYPES:
  XP Race: Who earns more XP in 24/48/72 hours
  Accuracy Duel: Compete on the same lesson; who scores higher
  Vocabulary Speed Test: Match vocabulary against each other in real-time (live PvP)
  Pronunciation Battle: Both attempt same phrase; AI judges; winner gets XP
  
CHALLENGE FLOW:
  User A sends challenge → User B receives notification
  User B accepts → Challenge begins (simultaneous or sequential)
  Result: Winner gets +200 XP + bragging card + small gem reward
  
RIVAL SYSTEM:
  Algorithm assigns a "Rival" each week (similar level, slightly ahead in XP)
  Rival creates urgency: "You're 450 XP behind your rival with 2 days left"
  
SOCIAL FEED:
  Friends' activity shown on dashboard
  Completions, level-ups, achievements, streak milestones
  Reactions: 🎉 👏 🔥 💪 (one-tap encouragement)
  Sending reactions gives small XP bonus (up to 10× daily)
```

---

## 7.14 REWARD ECONOMY AND VIRTUAL CURRENCY

### Velmorth Gems (Premium Currency)
```
EARNING GEMS (Free):
  Daily login bonus: 10 gems
  Weekly mission completion: 50-200 gems
  Achievement unlock: 10-2,000 gems
  Guild rewards: Variable
  Level-up milestones: 200-50,000 gems
  Season pass rewards: Variable
  Perfect lesson bonus: 5 gems
  Referring a friend: 100 gems per referral
  7-day streak: 50 gems
  30-day streak: 500 gems
  
SPENDING GEMS:
  Heart refill: 500 gems
  Streak freeze: 800 gems  
  Double XP (30 min): 1,000 gems
  Streak repair: 500-2,000 gems (proportional to streak length)
  Avatar items: 200-5,000 gems
  Pet accessories: 100-1,000 gems
  Guild creation: 1,000 gems
  
GEM ECONOMY DESIGN GOALS:
  Free users can earn ~300-500 gems/week through consistent study
  Premium features (heart refills, freezes) achievable through play within 2-3 weeks
  Cosmetic items: balanced between earnable and purchasable
  Never pay-to-win: Gems cannot buy additional learning content or unfair XP advantages
  
ECONOMY SAFEGUARDS:
  Daily gem earning cap: 200 gems (prevents farming bots)
  Gem transactions logged for audit
  Anti-inflation: Regular gem earning rate review
  Gem balances visible in-app; no dark patterns hiding balances
```

---

## 7.15 EVENTS SYSTEM

### Recurring Events Calendar
```
DAILY:
  Daily Challenge Chest: Complete 3 daily missions → open chest (gems + XP)
  
WEEKLY:
  Monday: New weekly missions unlock
  Wednesday: Guild mid-week bonus (×1.5 guild XP for 4 hours)
  Friday: "Friday Fluency" — double XP on speaking exercises, 6-8 PM local time
  Sunday: League reset + promotion/demotion notifications + weekly rewards
  
MONTHLY:
  Cultural Expedition: 7-day themed event (see Raids section)
  Monthly Boss Battle: Harder boss than weekly, better rewards
  
SEASONAL (4 per year):
  Spring Event: "Language Spring" — Cherry blossoms theme, Japanese/Korean content
  Summer Event: "Global Summer" — Travel vocabulary, worldwide cultural content
  Autumn Event: "Harvest of Words" — Literary vocabulary, classical texts
  Winter Event: "Holiday Polyglot" — Festive vocabulary across all languages
  
  Seasonal mechanics:
  - Limited-time cosmetics that expire
  - Seasonal leaderboard (starts at season start)
  - 2× XP on seasonal vocabulary topics
  - Exclusive seasonal pet forms
  - Limited-time story chapters in seasonal themes
  
SPECIAL EVENTS:
  World Language Day (February 21): Global celebration event
  Earth Day: Environmental vocabulary + plant-a-tree partnership
  International Education Day: Learning-related content + free premium trial offers
```
