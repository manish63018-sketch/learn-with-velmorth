# 🌿 Learn With Velmorth — Firestore Database Setup Guide

This document provides step-by-step instructions to configure and initialize your Google Firebase Cloud Firestore database for the **Learn With Velmorth** Android app.

---

## 📅 Step 1: Create a Firebase Project & Enable Firestore
1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add Project** and follow the prompts to create your project (e.g., `learn-with-velmorth`).
3. In the left-hand navigation menu, click on **Build** > **Firestore Database**.
4. Click **Create Database**.
5. Select a database location closest to your target users (e.g., `nam5 (us-central)` or `asia-south1` for India).
6. Start in **Production Mode** (recommended) or **Test Mode**. If starting in Production Mode, configure the Security Rules as described in Step 4 below.

---

## 🗃️ Step 2: Collection Schema Definitions

Configure the following four root collections inside your Firestore database:

### 1. `users` (Root Collection)
Main collection tracking user details, streaks, leaves, and XP thresholds.
* **Document ID**: `{uid}` (Matches the User's Firebase Authentication UID)
* **Fields**:
  * `uid`: `String` (User ID matching auth)
  * `name`: `String` (Display Name)
  * `username`: `String` (Unique alphanumeric handle, e.g. `ravi_learner`)
  * `email`: `String` (Registered email address)
  * `profileImage`: `String` (Gallery URI or Firebase Storage URL, empty by default)
  * `photoUrl`: `String` (Alias for profileImage for backwards compatibility)
  * `xp`: `Integer` (Current learning XP balance, default `0`)
  * `level`: `Integer` (Calculated learning level, default `1`)
  * `streak`: `Integer` (Daily active learning streak count, default `0`)
  * `leafBalance`: `Integer` (Current leaf currency balance, default `5`)
  * `isPremium`: `Boolean` (Subscription flag, default `false`)
  * `darkMode`: `Boolean` (System appearance setting, default `false`)
  * `notificationsEnabled`: `Boolean` (Daily reminder flag, default `true`)
  * `createdAt`: `Timestamp` (Account registration timestamp)
  * `lastActive`: `Timestamp` (Automatically updated on every app login or sync activity)

---

### 2. `lessons` (Root Collection)
Contains the language syllabus data. Useful for cloud seeding and offline-first dynamic updates.
* **Document ID**: `{lessonId}` (e.g. `ja_u01_l01_hello_basic`)
* **Fields**:
  * `lessonId`: `String` (Unique lesson ID)
  * `title`: `String` (Lesson Title)
  * `language`: `String` (e.g. `japanese`)
  * `difficulty`: `String` (e.g. `beginner-1`)
  * `xpReward`: `Integer` (XP rewarded on completion, e.g., `10`)
  * `isPremium`: `Boolean` (Locks lesson for free tier users)
  * `content`: `Map`
    * `vocabulary`: `Array` of `Map` objects (holding `vocab_id`, `kanji`, `romaji`, `meaning_en`, `meaning_hi`)
    * `grammar`: `Map` (holding `grammar_id`, `title`, `structure`, `explanation_en`)
    * `exercises`: `Array` of `Map` (holding quiz options, correct indices, and prompts)

---

### 3. `progress` (Root Collection)
Tracks lesson completions per user. Enables real-time statistics queries and multi-device progress restorations.
* **Document ID**: `{uid}_{lessonId}` (Unique composite key)
* **Fields**:
  * `uid`: `String` (User UID)
  * `lessonId`: `String` (Completed Lesson ID)
  * `score`: `Integer` (Quiz accuracy score percentage, e.g., `100`)
  * `completed`: `Boolean` (Status flag, default `true`)
  * `completedAt`: `Timestamp` (Time of lesson completion)

---

### 4. `settings` (Root Collection)
Holds device preference overlays linked to account settings.
* **Document ID**: `{uid}` (Matches User UID)
* **Fields**:
  * `uid`: `String` (User UID)
  * `notifications`: `Boolean` (Daily streak reminders toggle)
  * `theme`: `String` (`"light"` | `"dark"` | `"system"`)
  * `language`: `String` (Currently selected learning language)
  * `updatedAt`: `Timestamp` (Time of last settings update)

---

## 🔒 Step 3: Configure Firestore Security Rules
To guard user information and prevent unauthorized writes, apply the following **Security Rules** inside the **Rules** tab of your Firestore Database console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // User Profile Document Access
    match /users/{uid} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == uid;
      
      // Allow progress and srs subcollections reads/writes
      match /{allSubcollections=**} {
        allow read, write: if request.auth != null && request.auth.uid == uid;
      }
    }
    
    // Username Reservation Checking
    match /usernames/{username} {
      allow read: if true; // Public check for availability during signup
      allow create: if request.auth != null && request.resource.data.uid == request.auth.uid;
      allow update, delete: if request.auth != null && resource.data.uid == request.auth.uid;
    }
    
    // Flat Lesson Completion Tracking
    match /progress/{compositeId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null && request.resource.data.uid == request.auth.uid;
      allow delete: if request.auth != null && resource.data.uid == request.auth.uid;
    }
    
    // Settings Preferences Syncing
    match /settings/{uid} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == uid;
    }
    
    // Lessons Syllabus - Read-only for authenticated clients
    match /lessons/{lessonId} {
      allow read: if request.auth != null;
      allow write: if false; // Only manageable via Admin SDK or Firebase Console
    }
  }
}
```

---

## ⚡ Step 4: Configure Indexes
For fast queries, ensure the following composite indexes are created in the **Indexes** tab:
1. **`progress` Collection Index**:
   * Field 1: `uid` (Ascending)
   * Field 2: `completedAt` (Descending)
   * Scope: Single Field / Collection Group
2. **`users` Collection Index**:
   * Field 1: `xp` (Descending)
   * Field 2: `streak` (Descending)
   * Scope: Single Field (For building high-performance leaderboards)

---

## 🌱 Step 5: How the App Seeds Content (Seeding Script Template)
You can seed the `lessons` collection in cloud Firestore by importing your local assets. Below is a simple Node.js Admin SDK seeding snippet:

```javascript
const admin = require('firebase-admin');
const fs = require('fs');

// Initialize Admin SDK with service account
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function seedLessons() {
  const fileContent = fs.readFileSync('./japanese.json', 'utf8');
  const courseData = JSON.parse(fileContent);
  
  const units = courseData.units;
  for (let unit of units) {
    for (let lesson of unit.lessons) {
      console.log(`Seeding lesson: ${lesson.lesson_id}`);
      await db.collection('lessons').document(lesson.lesson_id).set({
        lessonId: lesson.lesson_id,
        title: lesson.lesson_title,
        language: 'japanese',
        difficulty: lesson.difficulty || 'beginner-1',
        content: lesson
      });
    }
  }
  console.log('Syllabus seeding complete! 🎉');
}

seedLessons();
```
*(Save this script as `seed.js` under the root workspace folder, install `firebase-admin`, configure your serviceAccountKey, and execute using `node seed.js` to populate Firestore instantly).*
