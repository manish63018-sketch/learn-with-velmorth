# 🌿 Learn With Velmorth

> A beautiful, offline-first Android language learning app with a forest theme, companion mascot, and gamified lessons.

---

## 📸 Screenshots

<!-- Add screenshots here after first build -->
<!-- ![Home Screen](screenshots/home.png) -->
<!-- ![Lesson Player](screenshots/lesson.png) -->
<!-- ![Review Garden](screenshots/review.png) -->

---

## ✨ Features

- 🌿 **Forest-themed UI** — Deep green Material 3 design with warm cream tones
- 🦦 **Velmorth Mascot** — Interactive companion with moods (Happy, Excited, Hungry, Sleepy)
- 📚 **Lesson Path** — Chapter-based language lessons with vocabulary & grammar
- 🎯 **Quiz Mode** — Multiple choice, fill-in, and audio recognition questions
- 🌸 **Review Garden** — Spaced repetition flashcard system
- 🍃 **Leaf Economy** — Earn leaves for completing lessons, spend in the shop
- 🔥 **Daily Streaks** — Motivation tracking with streak protection
- 🎤 **AI Speaker** — Pronunciation practice (Premium feature)
- 💎 **Premium Paywall** — Unlock advanced content and features
- 👤 **Profile & Stats** — XP, level, badges, and learning history
- ⚙️ **Settings** — Theme, notifications, language preferences
- 📴 **Offline-first** — Room database, works without internet

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Navigation | Navigation3 (alpha) |
| DI | Hilt |
| Local DB | Room |
| Async | Kotlin Coroutines + Flow |
| Image Loading | Coil |
| Animations | Lottie + Compose Animations |
| Preferences | DataStore |
| Background | WorkManager |
| Fonts | Google Fonts (Playfair Display, Nunito) |

---

## 🏗️ Project Structure

```
LearnWithVelmorth/
├── app/src/main/java/com/example/learnwithvelmorth/
│   ├── MainActivity.kt
│   ├── VelmorthApplication.kt
│   ├── Navigation.kt          ← NavDisplay + bottom bar
│   ├── NavigationKeys.kt      ← Type-safe nav keys
│   ├── data/
│   │   ├── local/
│   │   │   ├── db/            ← Room database
│   │   │   ├── dao/           ← DAOs
│   │   │   └── entities/      ← Room entities
│   │   └── repository/        ← Repository implementations
│   ├── domain/
│   │   ├── model/             ← Domain models
│   │   └── repository/        ← Repository interfaces
│   ├── di/                    ← Hilt modules
│   ├── theme/                 ← Colors, Typography, Shapes
│   └── ui/
│       ├── components/        ← Shared UI components
│       └── screens/           ← 12 screens
│           ├── splash/
│           ├── onboarding/
│           ├── home/
│           ├── lessons/
│           ├── lessonplayer/
│           ├── quiz/
│           ├── review/
│           ├── aispeaker/
│           ├── shop/
│           ├── premium/
│           ├── profile/
│           └── settings/
└── app/src/main/assets/
    └── db/lessons_seed.json   ← Seed data
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17+ (bundled with Android Studio)
- Android SDK 36
- Min SDK: Android 7.0 (API 24)

### Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/YOUR_USERNAME/learn-with-velmorth.git
   cd learn-with-velmorth
   ```

2. **Set up API keys**
   ```bash
   # Copy the example file
   cp apikey.properties.example apikey.properties
   # Edit apikey.properties and add your real keys
   ```

3. **Open in Android Studio**
   - File → Open → select the project folder
   - Let Gradle sync complete

4. **Run the app**
   - Connect a device or start an emulator
   - Press ▶ Run

> **Note:** `local.properties`, `apikey.properties`, and `google-services.json` are in `.gitignore` and must never be committed.

---

## 🔐 Security

This project follows secure credential management:

| File | Status | Reason |
|---|---|---|
| `local.properties` | 🚫 gitignored | Contains local SDK path |
| `apikey.properties` | 🚫 gitignored | Contains real API keys |
| `google-services.json` | 🚫 gitignored | Firebase config with keys |
| `serviceAccountKey.json` | 🚫 gitignored | Firebase admin secret |
| `*.jks` / `*.keystore` | 🚫 gitignored | Release signing key |
| `apikey.properties.example` | ✅ committed | Placeholder template only |

**If you accidentally commit a secret:**
1. Immediately revoke/rotate the key in its dashboard
2. Remove it from git history: `git filter-branch` or BFG Repo Cleaner
3. Force push the cleaned history

---

## 🌱 Roadmap

- [x] Forest theme + Material 3 design system
- [x] 12 screens (Splash → Settings)
- [x] Room database with seed data
- [x] Navigation3 with bottom nav
- [x] Hilt dependency injection
- [x] Velmorth mascot with mood system
- [ ] Firebase Authentication
- [ ] Cloud sync (Firestore)
- [ ] Real AI Speaker (Gemini API)
- [ ] Push notifications (streak reminders)
- [ ] Play Store release

---

## 🤝 Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit with clear messages: `git commit -m "Add: XP reward animation"`
4. Push: `git push origin feature/your-feature`
5. Open a Pull Request

### Commit Message Format
```
Add: short description of what was added
Fix: short description of what was fixed
Update: short description of what was changed
Remove: short description of what was removed
```

---

## 📄 License

```
MIT License — see LICENSE file for details
```

---

## 👤 Author

**Velmorth Team**  
Built with 🌿 using Jetpack Compose
