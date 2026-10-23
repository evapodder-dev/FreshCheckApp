# Firebase Realtime Database Setup Guide for FreshCheck

FreshCheck is designed with an **offline-first architecture**. All core features (dashboard freshness health, inventory management, expiry notifications, recipe suggestions, shopping list, and waste tracking) run 100% locally out of the box using SQLite and WorkManager.

To enable **cloud synchronization** across devices using Firebase Realtime Database, follow the simple steps below.

---

## 1. Create a Firebase Project

1. Navigate to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or create a project) and name it (e.g., `FreshCheck`).
3. Google Analytics is optional; choose your preference and click **Create project**.

---

## 2. Register Android Application

1. In your Firebase Project Overview, click the **Android** icon (`+ Add app`).
2. Enter the exact package name:
   ```
   com.freshcheck.app
   ```
3. Enter an optional App nickname (e.g., `FreshCheck Android`).
4. Click **Register app**.

---

## 3. Download and Place `google-services.json`

1. Download the generated `google-services.json` configuration file from Firebase.
2. Copy this file into your project's `app/` directory:
   ```
   FreshCheckApp/app/google-services.json
   ```
   *(Note: A template file is already located in `app/google-services.json` for reference).*

---

## 4. Enable Google Services Gradle Plugin (Optional / Standard)

If you wish to apply the Google Services Gradle plugin:
1. In `FreshCheckApp/build.gradle`, add:
   ```groovy
   plugins {
       id 'com.android.application' version '9.4.1' apply false
       id 'com.google.gms.google-services' version '4.4.2' apply false
   }
   ```
2. In `FreshCheckApp/app/build.gradle`, add to plugins block:
   ```groovy
   plugins {
       id 'com.android.application'
       id 'com.google.gms.google-services'
   }
   ```

---

## 5. Enable Firebase Realtime Database

1. In the Firebase Console left menu, navigate to **Build** -> **Realtime Database**.
2. Click **Create Database**.
3. Choose your database location (e.g., `United States (us-central1)`).
4. For security rules during development and academic evaluation, select **Start in test mode**:
   ```json
   {
     "rules": {
       ".read": true,
       ".write": true
     }
   }
   ```
5. Click **Enable**.

---

## 6. Test Cloud Sync in the App

1. Open **FreshCheck** on your device or emulator.
2. Tap the **Cloud Sync** icon in the top toolbar or the button on the Home Dashboard.
3. Tap **Upload Inventory to Firebase** — your local items will upload to the `/inventory` node.
4. You can immediately see the live records in your Firebase Console under the **Data** tab!
5. To test restoration or sync on another device, tap **Download & Merge from Firebase**.

---

## Offline Behavior & Graceful Fallback

If Firebase is not configured or the device is offline:
- The app detects offline state and displays a status card: `"Firebase Offline / Local Persistence Active"`.
- Core features remain 100% operational with SQLite local database.
- No crashes or ANRs occur.
