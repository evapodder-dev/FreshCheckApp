# FreshCheck Firebase & Authentication Setup Guide

FreshCheck contains an enterprise-grade **offline-first cloud architecture**:
1. **Firebase Authentication**: Sign in with **Google**, **Facebook**, **GitHub**, and **Guest / Anonymous**, plus seamless **Sign Out**.
2. **Firebase Realtime Database**: High-speed live tree synchronization for real-time pantry inventory.
3. **Cloud Firestore**: Document-based cloud database for scalable queries and multi-device backups.
4. **Offline Resilience**: When offline or before configuring credentials, the app operates 100% locally with SQLite, with demo authentication and persistent local state.

---

## 1. Firebase Authentication Setup

### A. Enable Sign-In Providers in Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/) -> Select your project -> **Authentication** -> **Sign-in method**.
2. **Google**:
   - Enable **Google**.
   - Select your project support email and save.
   - For Android Google Sign-In, add your debug SHA-1 fingerprint (found by running `./gradlew signingReport`) in Project Settings -> Your Android App.
3. **Facebook**:
   - Enable **Facebook**.
   - Enter your Facebook App ID and App Secret from the [Meta for Developers Portal](https://developers.facebook.com/).
   - Copy the OAuth redirect URI from Firebase and paste it into Facebook Login settings.
4. **GitHub**:
   - Enable **GitHub**.
   - Create an OAuth App on [GitHub Developer Settings](https://github.com/settings/developers).
   - Enter your GitHub Client ID and Client Secret in Firebase.
   - Set the Authorization callback URL to the URL provided by Firebase.
5. **Anonymous / Guest**:
   - Enable **Anonymous** for instant 1-tap testing without social credentials.

---

## 2. Firebase Realtime Database & Cloud Firestore Setup

### A. Realtime Database
1. In Firebase Console, go to **Build** -> **Realtime Database** -> **Create Database**.
2. Select database location (e.g., `United States (us-central1)`).
3. Set Security Rules:
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "inventory": {
      ".read": true,
      ".write": true
    }
  }
}
```

### B. Cloud Firestore
1. In Firebase Console, go to **Build** -> **Firestore Database** -> **Create Database**.
2. Set Security Rules for development:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/inventory/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /inventory/{document=**} {
      allow read, write: if true;
    }
  }
}
```

---

## 3. Place `google-services.json`
1. Download `google-services.json` from Firebase Project Settings.
2. Put it in `FreshCheckApp/app/google-services.json`.

---

## 4. How to Test in the FreshCheck App

1. Launch **FreshCheck** on your device or emulator.
2. Tap the **Account** icon (`ic_account`) in the top toolbar or the **Cloud Sync** button on the Dashboard.
3. In **Firebase Account & Auth**:
   - Tap **Sign in with Google**, **Facebook**, **GitHub**, or **Continue as Guest**.
   - Your profile details (Name, Email, UID, Provider) appear instantly in the Profile Card.
   - Tap **Sign Out** anytime to cleanly log out of the session.
4. In **Cloud Sync Center**:
   - Tap **Upload to Realtime DB** or **Download from Realtime DB**.
   - Tap **Upload to Cloud Firestore** or **Download from Cloud Firestore**.
   - Or tap **Sync All Cloud Databases** to synchronize across both Firebase databases simultaneously!
