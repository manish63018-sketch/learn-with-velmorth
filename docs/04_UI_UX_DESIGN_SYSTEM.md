# LEARN WITH VELMORTH — UI/UX DESIGN SYSTEM
## Section 4: Complete Design Language

---

## 4.1 BRAND IDENTITY

### Brand Name & Meaning
**Velmorth** — A coined word combining "Vel" (Latin: swift, from "velox") and "Morth" (archaic: depth of knowledge). Together: *"Swift mastery of deep knowledge."* The name sounds ancient and wise yet modern and technological — a deliberate fusion of heritage learning and AI innovation.

### Brand Personality
- **Intelligent**: Feels like talking to the world's best tutor — knowledgeable, precise, clear
- **Warm**: Never cold or robotic; always encouraging, personal, humanistic
- **Ambitious**: Pushes learners beyond comfort; believes in their potential before they do
- **Playful**: Celebrates wins with genuine joy; gamification feels natural, not forced
- **Trustworthy**: Scientific rigor; every claim backed by cognitive science research

### Visual Philosophy
Velmorth's visual language is called **Luminary Design** — inspired by the feeling of a light turning on in your mind. Key visual metaphors:
- **Neural connections** — curved lines connecting nodes, representing knowledge linking to knowledge
- **Gradient light** — soft glows and gradients suggesting illumination and insight
- **Crystal clarity** — clean layouts with generous whitespace; nothing is cluttered
- **Cosmic depth** — dark mode inspired by deep space; learning as cosmic exploration
- **Organic motion** — all animations feel alive, natural, breathing — like a living system

### Logo
The Velmorth logo is a stylized **neural node** — three curved lines emanating from a central glowing point, forming a shape that simultaneously suggests a brain neuron, a compass rose, and an open book. The node pulses with a soft gradient animation in the app.

---

## 4.2 COMPLETE COLOR PALETTE

### Brand Colors (HEX + RGB + HSL + Usage)

#### Primary — Electric Indigo
- Light: `#5B4FD4` | rgb(91, 79, 212) | hsl(245, 61%, 57%)
- Dark: `#7C71FF` | rgb(124, 113, 255) | hsl(246, 100%, 72%)
- **Usage**: Primary buttons, active states, progress bars, key UI elements
- **Meaning**: Intelligence, depth, trust, premium quality

#### Primary Container — Indigo Mist
- Light: `#E8E5FF` | rgb(232, 229, 255) | hsl(246, 100%, 95%)
- Dark: `#2D2866` | rgb(45, 40, 102) | hsl(246, 43%, 28%)
- **Usage**: Selected chip backgrounds, input focus rings, subtle highlights

#### Secondary — Teal Mint
- Light: `#00C9A7` | rgb(0, 201, 167) | hsl(170, 100%, 39%)
- Dark: `#00C9A7` | rgb(0, 201, 167) | hsl(170, 100%, 39%)
- **Usage**: Correct answer indicators, success states, streak highlights, XP accents
- **Meaning**: Growth, progress, life, positive reinforcement

#### Secondary Container — Mint Haze
- Light: `#CCFFF6` | rgb(204, 255, 246) | hsl(170, 100%, 90%)
- Dark: `#003A2E` | rgb(0, 58, 46) | hsl(170, 100%, 11%)

#### Tertiary — Coral Fire
- Light: `#FF6B6B` | rgb(255, 107, 107) | hsl(0, 100%, 71%)
- Dark: `#FF8E8E` | rgb(255, 142, 142) | hsl(0, 100%, 78%)
- **Usage**: Streak fire icon, energetic actions, urgent notifications, hearts
- **Meaning**: Energy, urgency, passion, challenge

#### Accent Gold — Achievement Glow
- `#FFD700` | rgb(255, 215, 0) | hsl(51, 100%, 50%)
- Dark variation: `#FFC200` | rgb(255, 194, 0) | hsl(46, 100%, 50%)
- **Usage**: Achievement stars, gold tier badges, premium features, XP numbers

#### Success — Emerald
- `#22C55E` | rgb(34, 197, 94) | hsl(142, 71%, 45%)
- **Usage**: Correct answer banners, completion states, positive trends
- Container: `#DCFCE7` | rgb(220, 252, 231) | hsl(141, 78%, 93%)

#### Warning — Amber
- `#F59E0B` | rgb(245, 158, 11) | hsl(38, 92%, 50%)
- **Usage**: Streak at risk, moderate accuracy warnings, attention alerts
- Container: `#FEF3C7` | rgb(254, 243, 199) | hsl(48, 96%, 89%)

#### Error — Crimson
- `#E63946` | rgb(230, 57, 70) | hsl(356, 79%, 56%)
- **Usage**: Wrong answer banners, error states, critical alerts
- Container: `#FFE2E4` | rgb(255, 226, 228) | hsl(356, 100%, 94%)

#### Neutral Scale
- `#1A1A2E` — Text Primary (Light mode) | Deep Midnight
- `#4A4A6A` — Text Secondary (Light mode) | Muted Violet
- `#8B8BAB` — Text Tertiary / Placeholder (Light mode) | Cool Grey
- `#C4C4DC` — Border (Light mode)
- `#F5F5FF` — Background (Light mode) | Lavender White
- `#FFFFFF` — Surface (Light mode) | Pure White
- `#FAFAFE` — Card (Light mode) | Near White
- `#EEEDF8` — Surface Variant (Light mode)

#### Dark Mode Neutrals
- `#EAEAFF` — Text Primary (Dark mode) | Ghost White
- `#A8A8C8` — Text Secondary (Dark mode)
- `#6A6A8A` — Text Tertiary (Dark mode)
- `#2E2D42` — Border (Dark mode)
- `#0F0E1A` — Background (Dark mode) | Deep Space
- `#1A1928` — Surface (Dark mode) | Dark Matter
- `#1F1E30` — Card (Dark mode) | Void Card
- `#252440` — Surface Variant (Dark mode)

#### Gradient Definitions
```
gradient-primary: linear-gradient(135deg, #5B4FD4 0%, #8B6FF0 50%, #C084FC 100%)
gradient-secondary: linear-gradient(135deg, #00C9A7 0%, #00B4D8 100%)
gradient-sunset: linear-gradient(135deg, #FF6B6B 0%, #FF8E53 50%, #FFD700 100%)
gradient-cosmic: linear-gradient(180deg, #0F0E1A 0%, #1A1264 50%, #2D1B69 100%)
gradient-aurora: linear-gradient(135deg, #5B4FD4 0%, #00C9A7 100%)
gradient-card-glow: radial-gradient(ellipse at top, rgba(91,79,212,0.15) 0%, transparent 70%)
gradient-xp: linear-gradient(90deg, #FFD700 0%, #FF9500 100%)
gradient-streak: linear-gradient(135deg, #FF6B6B 0%, #FF9F43 100%)
```

---

## 4.3 TYPOGRAPHY HIERARCHY

### Font Stack
- **Display Font**: Nunito (Google Fonts) — Used for large headings, XP counts, scores; has that friendly + bold character
- **Body Font**: Inter (Google Fonts) — Used for all UI text; exceptional readability at small sizes
- **Monospace**: JetBrains Mono — Used for code snippets in programming language courses

### Type Scale (Complete)
| Token | Font | Size | Weight | Line Height | Letter Spacing | Usage |
|---|---|---|---|---|---|---|
| display-xl | Nunito | 64px | 800 | 1.1 | -1px | App launch headline |
| display-lg | Nunito | 48px | 800 | 1.15 | -0.5px | Major milestones |
| display-md | Nunito | 40px | 700 | 1.2 | -0.3px | Level up screen |
| display-sm | Nunito | 32px | 700 | 1.2 | -0.2px | Section headers |
| headline-lg | Inter | 28px | 700 | 1.25 | -0.2px | Screen titles |
| headline-md | Inter | 24px | 600 | 1.3 | -0.1px | Card headers |
| headline-sm | Inter | 20px | 600 | 1.35 | 0 | Section subtitles |
| title-lg | Inter | 18px | 600 | 1.4 | 0 | List headers |
| title-md | Inter | 16px | 600 | 1.4 | 0.1px | Card titles |
| title-sm | Inter | 14px | 600 | 1.4 | 0.1px | Labels, chips |
| body-lg | Inter | 16px | 400 | 1.6 | 0.15px | Main content |
| body-md | Inter | 14px | 400 | 1.6 | 0.25px | Secondary content |
| body-sm | Inter | 12px | 400 | 1.5 | 0.4px | Captions |
| label-lg | Inter | 14px | 500 | 1.4 | 0.1px | Button labels |
| label-md | Inter | 12px | 500 | 1.4 | 0.5px | Chip labels |
| label-sm | Inter | 11px | 500 | 1.4 | 0.5px | Micro labels |
| xp-counter | Nunito | 28px | 800 | 1 | 0 | XP displays |
| score | Nunito | 48px | 800 | 1 | -0.5px | Score reveals |

---

## 4.4 SPACING SYSTEM

Based on an 8px base grid:

| Token | Value | Usage |
|---|---|---|
| space-1 | 4px | Icon-text gap, chip internal |
| space-2 | 8px | Small gaps, icon padding |
| space-3 | 12px | Input internal padding |
| space-4 | 16px | Card padding, standard gap |
| space-5 | 20px | Screen horizontal margin |
| space-6 | 24px | Section gap (small) |
| space-8 | 32px | Section gap (standard) |
| space-10 | 40px | Section gap (large) |
| space-12 | 48px | Hero spacing |
| space-16 | 64px | Major section separation |
| space-20 | 80px | Page-level spacing |
| space-24 | 96px | Extra large |

---

## 4.5 ELEVATION SYSTEM

```
Level 0: 0dp elevation — Background, flat surfaces
Level 1: 1dp — Cards (subtle float)
Level 2: 3dp — Cards with interaction, contained buttons
Level 3: 6dp — FAB resting, navigation bar
Level 4: 8dp — Menus, tooltips
Level 5: 12dp — Dialogs, bottom sheets resting
Level 6: 16dp — Modals, full-screen overlays
Level 7: 24dp — Notifications, snackbars

Shadow Definitions:
level-1: 0 1px 2px rgba(0,0,0,0.08), 0 1px 3px rgba(0,0,0,0.06)
level-2: 0 2px 8px rgba(0,0,0,0.10), 0 4px 6px rgba(0,0,0,0.06)
level-3: 0 4px 16px rgba(0,0,0,0.12), 0 8px 12px rgba(0,0,0,0.07)
level-4: 0 8px 24px rgba(0,0,0,0.14), 0 12px 18px rgba(0,0,0,0.08)
level-5: 0 16px 32px rgba(0,0,0,0.18), 0 24px 36px rgba(0,0,0,0.10)

Glow Shadows (for special elements):
primary-glow: 0 0 20px rgba(91,79,212,0.35), 0 0 60px rgba(91,79,212,0.15)
success-glow: 0 0 20px rgba(0,201,167,0.35)
streak-glow:  0 0 20px rgba(255,107,107,0.40), 0 0 50px rgba(255,159,67,0.20)
gold-glow:    0 0 20px rgba(255,215,0,0.45), 0 0 50px rgba(255,215,0,0.20)
```

---

## 4.6 COMPONENT SYSTEM

### Primary Button
```
State       Background           Text        Border    Shadow
Default     gradient-primary     #FFFFFF     none      level-2 + primary-glow-subtle
Hover       gradient-primary+5%  #FFFFFF     none      level-3 + primary-glow
Pressed     gradient-primary-8%  #FFFFFF     none      none
Disabled    #C4C4DC             #8B8BAB     none      none
Loading     gradient-primary     spinner     none      level-2

Dimensions:
Height: 54px (mobile) / 48px (web)
Border Radius: 14px
Font: label-lg, weight 700
Padding: 18px horizontal
Animation: scale(0.97) on press, spring back on release (150ms)
```

### Secondary Button
```
State       Background    Text         Border
Default     transparent   primary      1.5px solid primary
Hover       primary/10%   primary      1.5px solid primary
Pressed     primary/15%   primary      1.5px solid primary-dark
```

### Ghost Button (Tertiary)
```
Default: transparent background, primary text, no border
Hover: primary/8% background
Used for: Cancel actions, low-emphasis actions
```

### Destructive Button
```
Default: error color background, white text
Used for: Delete account, remove item
```

---

### Cards
```
VelmorthCard (Standard):
Background: surface / dark: card-dark
Border Radius: 16px
Padding: 16px
Shadow: level-1
Border: none (light) / 1px solid #2E2D42 (dark)
Animation: scale(1.01) on hover + shadow increase

VelmorthCard (Elevated):
Background: surface
Shadow: level-2
On hover: level-3 + gradient-card-glow background

VelmorthCard (Highlighted):
Background: gradient from primaryContainer to surface
Border: 1.5px solid primary/30%
Shadow: level-2 + primary-glow

LessonCard:
Height: 120px
Left accent stripe: 4px solid (course color)
Contains: lesson icon, title, XP value, progress indicator
Swipe-to-action supported (iOS HIG)

AchievementCard:
Aspect ratio: 1:1
Glowing border when locked: dashed, muted
Glowing border when unlocked: solid gold-glow
3D tilt effect on hover (Transform perspective)

GuildCard:
Background: gradient (guild color scheme)
Contains: guild emblem, member count, weekly XP, rank badge
```

---

### Dialogs
```
VelmorthDialog:
Width: min(400px, screen-width - 48px)
Border Radius: 24px
Padding: 24px
Background: surface (blurred backdrop)
Backdrop blur: 20px blur behind
Backdrop color: rgba(0,0,0,0.5)
Enter animation: scale(0.9)→(1.0) + fade in, 300ms spring
Exit animation: scale(1.0)→(0.95) + fade out, 200ms ease

Content structure:
- Icon (optional, 48px, color-coded)
- Title: headline-md
- Body: body-md, text-secondary
- Divider (optional)
- Actions row: max 2 buttons (secondary + primary)

ConfirmationDialog: Red warning icon + destructive primary button
SuccessDialog: Success icon + Lottie confetti animation
InfoDialog: Info icon + close button only
```

---

### Bottom Sheets
```
Standard Bottom Sheet:
Border Radius top-left/right: 28px
Handle: 4px × 32px, rounded, color: border/60%
Background: surface
Max height: 90% of screen
Drag to dismiss: enabled
Dimmed backdrop: rgba(0,0,0,0.4)

Expanded Bottom Sheet (full-screen modal):
Border Radius: 0 (full screen)
Has close button in top-right
Used for: lesson summary, detailed analytics, full achievement view

Snap Points: 40% (default), 70% (expanded), 100% (full)
```

---

### Inputs
```
VelmorthTextField:
Height: 54px
Border: 1.5px solid border
Border Radius: 12px
Focus Border: 2px solid primary
Error Border: 2px solid error
Background: surfaceContainerLow
Padding: 16px horizontal
Label: floats above on focus (Material behavior)
Helper text: body-sm, text-tertiary
Error text: body-sm, error color
Leading icon: 24px, text-secondary
Trailing icon: 24px (clear/show-password/etc.)
Animation: border color transition 150ms, label float 200ms

Search Field:
Leading icon: search icon (persistent)
Background: surfaceContainer
Border: none (blends with surface)
Shadow: level-1
Instant results dropdown: level-4 shadow

OTP Input:
6 boxes, 48×54px each
4px gap between boxes
Focus highlight: primary border + primary/10% background
Auto-advance on digit entry
```

---

## 4.7 MICROINTERACTIONS

### Complete Microinteraction Inventory

**1. Answer Submission**
- Tap answer → haptic feedback (light impact)
- Answer selected → scale 1.0→1.02→1.0 in 100ms (confirmation)
- Submit button tap → haptic (medium impact) + button scale 0.97→1.0

**2. Correct Answer**
- Green flash overlay from bottom (80px height, 400ms slide)
- Checkmark icon bounces in (scale 0→1.2→1.0, 300ms spring)
- Sound effect: ascending ding
- "CORRECT!" text appears with stagger delay
- XP particles burst from answer (Lottie)
- Progress bar animates forward

**3. Wrong Answer**
- Red shake animation on selected answer (horizontal: ±8px, 3 cycles, 300ms)
- Haptic: error pattern (2 short pulses)
- Heart pops and deflates (Lottie, -1 heart)
- Correct answer revealed with green glow
- Sound effect: low thud

**4. Streak Fire**
- Streak counter increments: number bounces + fire emoji pulses
- New streak milestone (7, 14, 30, etc.): Full-screen celebration Lottie + confetti

**5. XP Gain**
- "+50 XP" floats up and fades from top of screen
- XP bar fills with spring physics animation
- Level-up: bar overflows → screen flash → Level N+1 animation

**6. Button States**
- Hover (web): scale 1.01 + shadow increase, 150ms
- Press: scale 0.97, 80ms; release: spring back, 150ms
- Loading: spinner replaces text, no width change

**7. Navigation**
- Tab switch: content slides in from direction of tab (left/right)
- Screen push: slide from right, 350ms decelerate
- Screen pop: slide to right, 300ms accelerate
- Modal appear: fade + scale from 0.92→1.0

**8. Pull-to-Refresh**
- Custom Velmorth mascot head bounces into view on pull
- Mascot blinks and smiles when refresh triggers

**9. Toggle/Switch**
- Thumb slides with spring physics
- Track color transitions with 200ms animation
- Haptic: light tap on toggle

**10. Achievement Unlock**
- Badge flies in from top with spin (360°) + bounce land
- Radial glow pulses from badge
- Screen edges flash gold
- Distinct "achievement unlocked" sound

**11. Lesson Completion**
- Stars earn one by one (stagger 300ms between each)
- Each star materializes with scale 0→1.3→1.0 + sparkle particles
- Final score counter animates up from 0 to score value

**12. Drag and Drop (Arrange Words exercise)**
- Picked word: scale 1.05, shadow level-4, slight rotation tilt
- Drop zone: border pulses with dashed animation
- Successful drop: soft snap + color fill transition
- Invalid drop: spring back + shake

---

## 4.8 DARK MODE

### Dark Mode Design Philosophy
Dark mode in Velmorth is not simply an inverted light mode — it's a completely crafted **cosmic learning environment** that enhances focus, reduces eye strain during evening study, and creates a distinct "focus mode" psychological feeling.

**Key dark mode design decisions**:
1. Background is `#0F0E1A` — deep space indigo-black, not pure black (avoids harsh contrast)
2. Cards have very subtle indigo tint (`#1F1E30`) — visible depth without harsh borders
3. All gradients shift to more luminous, glowing versions in dark mode
4. Glow effects are more prominent (they have more visual impact on dark backgrounds)
5. Primary color shifts from `#5B4FD4` to `#7C71FF` — lighter for contrast compliance
6. Surface hierarchy: background → surface → card → elevation (each step ~8% lighter)
7. Text hierarchy maintained: `#EAEAFF` → `#A8A8C8` → `#6A6A8A` (3 levels)

### Automatic Dark Mode
- Follows system setting by default
- User can override in settings
- Transition animation: 400ms cross-fade when switching modes
- OLED optimization mode: Background becomes true `#000000` (saves battery on OLED)

---

## 4.9 ACCESSIBILITY STANDARDS

### WCAG 2.1 AA Compliance Checklist

**Visual**:
- All text/background color combinations: 4.5:1 contrast ratio minimum ✓
- Large text (18px+ normal / 14px+ bold): 3:1 minimum ✓
- Focus indicators: 3px solid primary outline, visible on all interactive elements ✓
- Color not used as sole means of conveying information (icons + patterns supplement color) ✓
- Scalable text: UI reflows correctly at 200% text scale ✓
- No content lost when text spacing is increased ✓

**Interaction**:
- All interactive elements: minimum 48×48dp touch target ✓
- No gestures that can't be replicated by tap ✓
- Drag-and-drop exercises have keyboard/switch-accessible alternative ✓
- Timed exercises have "disable timer" accessibility option ✓
- All voice exercises can be completed in text mode ✓

**Screen Readers**:
- All images have meaningful alt text ✓
- Custom widgets expose correct accessibility role ✓
- Dynamic content updates announced (live regions) ✓
- Error messages associated with their form field ✓
- Reading order matches visual order ✓

**Motion**:
- All animations respect `prefers-reduced-motion` system setting ✓
- No content flashes more than 3 times per second ✓
- Parallax and float animations disabled in reduced-motion mode ✓

**Additional Velmorth Accessibility Features**:
- Dyslexia-friendly font mode (OpenDyslexic font option)
- High contrast mode (5:1 contrast ratio minimum)
- Color blind mode (Deuteranopia/Protanopia/Tritanopia palettes)
- Large text mode (global font scale +30%)
- Screen reader narration of lesson content
- Audio descriptions for all visual lesson content
