# FreshCheck — Smart Food Management & Waste Reduction App

**FreshCheck** is a full-featured, modern Android Studio application built with **Java** and **XML**. It helps students, families, and individuals track food inventory, calculate expiry countdowns, reduce food waste, find recipes utilizing expiring ingredients, and organize grocery shopping.

The project incorporates all foundational and advanced Android syllabus requirements, including Multi-Activity lifecycles, SQLite local storage, custom ContentProviders, ContentResolver queries, Started Services, BroadcastReceivers, WorkManager background workers, explicit and implicit Intents, custom Canvas Views, and Firebase Realtime Database cloud synchronization.

---

## 📱 Features Overview

### 1. Home Dashboard
* **Dynamic Statistics**: Hardcode-free calculation of Total Active Items, Items Expiring Soon (1–3 days), Items Expiring Today (0 days), and Expired Items (< 0 days).
* **Interactive Freshness Donut Gauge**: Custom View (`ExpiryStatusDonutView`) dynamically rendering real-time food freshness percentage and status distribution.
* **Use First Priority Feed**: Instant access to urgent items with one-tap "Consumed" and "Waste" action buttons.
* **Quick Access Actions**: Instant navigation to Add Food, Start Inventory Audit Service, Cloud Sync, and ContentProvider Demo.

### 2. Food Inventory Management
* **Comprehensive CRUD**: Add, view, edit, and delete food items with confirmation dialogs.
* **Custom Food Cards**: MaterialCardView items displaying food name, quantity, unit, category, storage location, relative expiry countdown, and status badges.
* **Real-Time Search**: Instant filtering by item name, notes, or storage location.
* **Category & Status Filtering**: Filter items by Category (Produce, Dairy, Meat, Seafood, Bakery, Pantry, Frozen, Beverages, Other) and Status (All, Fresh, Expiring Soon, Expired).

### 3. Expiry Tracking & Calculations
* Calculates days remaining by comparing midnight-normalized dates (`Calendar` & `TimeUnit`).
* 4 Distinct Visual Statuses:
  * **Fresh** (> 3 days) — Green badge
  * **Expiring Soon** (1 to 3 days) — Orange badge
  * **Expires Today** (0 days) — Deep Orange badge
  * **Expired** (< 0 days) — Red badge

### 4. Background Expiry Notifications & Permissions
* **Android 13+ Runtime Permission**: Handles `POST_NOTIFICATIONS` runtime requests gracefully.
* **High-Priority Notification Channel**: Custom alerts with action intents leading directly to the inventory screen.
* **WorkManager Integration**: `ExpiryCheckWorker` runs background checks every 24 hours.
* **Boot Persistence**: `BootReceiver` restores background workers after device restarts.

### 5. Priority "Use First" Workflow
* Items expiring within 3 days are automatically prioritized.
* **One-Tap Consumed**: Celebratory feedback, removes item from active inventory without penalizing waste statistics.
* **Record Waste**: Moves item to waste history, tracking discard reason, financial loss, and notes.

### 6. Smart Recipe Suggestions (Offline & Online)
* **Inventory-Driven Suggestions**: Prioritizes recipes matching ingredients currently available and expiring in your pantry.
* **12+ Curated Local Recipes**: Pre-packaged breakfast, lunch, dinner, and snack recipes functioning 100% offline.
* **Live HTTP Requests & JSON Parsing**: `RecipeApiService` queries TheMealDB public REST API via `HttpURLConnection` on an `ExecutorService` background thread.
* **Implicit Intents**: Direct options to share recipes via messaging apps or search video tutorials on YouTube/browser.

### 7. Grocery Shopping List
* Quick-add bar for items with quantity, unit, and category.
* Interactive checkboxes for marking items as purchased with strike-through styling.
* **Convert to Inventory**: One-click action to transition purchased shopping items into active pantry food items!
* **Implicit Intent Sharing**: Shares formatted grocery list to SMS, WhatsApp, Email, or Notes.

### 8. Food Waste Tracking & Financial Loss Analytics
* Logs wasted items with date, quantity, reason (Spoiled, Past Expiry, Overcooked, Leftovers, Other), and estimated monetary value ($).
* **Custom Canvas View (`WasteBreakdownChartView`)**: Custom-painted bar chart visualizing financial loss and percentage breakdown by waste reason.
* Cumulative statistics tracking total wasted items and total financial loss.

### 9. Firebase Realtime Database & Cloud Firestore Multi-Cloud Sync
* **Dual Database Support**: Full synchronization with both **Firebase Realtime Database** and **Cloud Firestore**.
* **User-Scoped Cloud Storage**: Automatically scopes inventory per authenticated user (`/users/{uid}/inventory`).
* **Offline-First Resilience**: Works offline seamlessly using SQLite local storage when not connected to the cloud.

### 10. Firebase Authentication & Social Sign-In
* **Google Sign-In**: One-tap sign in using Google Play Services OAuth credential flow.
* **Facebook OAuth Sign-In**: Seamless authentication with Facebook account.
* **GitHub OAuth Sign-In**: Developer-friendly authentication with GitHub OAuth.
* **Guest / Anonymous Sign-In**: Instant trial without requiring social accounts.
* **Sign Out Option**: Clean session logout and credential revocation anytime.
* **Dedicated Account Screen (`AuthActivity`)**: User profile badge, provider icon, display name, email, UID, and session management.
* Cloud sync for uploading and downloading inventory to `/inventory`.
* Full setup instructions provided in `FIREBASE_SETUP.md`.

---

## 🎓 Syllabus Coverage & Implementation Map

| Syllabus Topic | Components Implemented | Class / File Reference |
| :--- | :--- | :--- |
| **Project Architecture & Gradle** | Standard Gradle structure, AGP 9.4+, Java 11, dependencies | `build.gradle`, `app/build.gradle`, `settings.gradle` |
| **Manifest & Permissions** | Permissions (`INTERNET`, `POST_NOTIFICATIONS`, `BOOT_COMPLETED`), 5 Activities, 1 Service, 3 Receivers, 1 Provider | `app/src/main/AndroidManifest.xml` |
| **Multiple Activities** | 5 Activities with explicit intents & extras | `MainActivity`, `AddEditFoodActivity`, `RecipeDetailActivity`, `ProviderDemoActivity`, `CloudSyncActivity` |
| **Activity Lifecycles** | Lifecycle callbacks with Logcat output (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`) | All 5 Activity classes |
| **XML Layouts & Material UI** | ConstraintLayout, CoordinatorLayout, NestedScrollView, MaterialCardView, Chips | `res/layout/*.xml`, `res/values/themes.xml`, `colors.xml` |
| **RecyclerView & Adapters** | 4 Adapters with custom ViewHolders & card layouts | `FoodAdapter`, `UseFirstAdapter`, `ShoppingAdapter`, `WasteAdapter`, `RecipeAdapter` |
| **Custom Views (Canvas & Paint)** | 2 Custom Views implementing `onMeasure()` and `onDraw()` | `ExpiryStatusDonutView.java`, `WasteBreakdownChartView.java` |
| **Local Database (SQLite)** | `SQLiteOpenHelper`, DAOs/Repositories, CRUD operations | `DatabaseHelper.java`, `FoodRepository.java`, `ShoppingRepository.java`, `WasteRepository.java` |
| **ContentProvider** | Custom Provider with `UriMatcher`, MIME types, CRUD | `FoodContentProvider.java` (`content://com.freshcheck.app.provider/food_items`) |
| **ContentResolver** | ContentResolver querying & inserting with raw Cursor inspection | `ProviderDemoActivity.java` |
| **Started Service** | Background inventory audit service running on thread pool | `InventoryAuditService.java` |
| **BroadcastReceivers** | Alarm receiver, audit completion receiver, device boot receiver | `ExpiryAlarmReceiver.java`, `InventoryAuditReceiver.java`, `BootReceiver.java` |
| **Background WorkManager** | Periodic background worker for expiry inspections | `ExpiryCheckWorker.java`, `WorkManagerScheduler.java` |
| **Explicit Intents** | Navigating between activities with bundles and object serialization | `MainActivity`, `AddEditFoodActivity`, `RecipeDetailActivity` |
| **Implicit Intents** | Sharing shopping list, sharing recipes (`ACTION_SEND`), opening video tutorials (`ACTION_VIEW`) | `ShoppingFragment.java`, `RecipeDetailActivity.java` |
| **Networking & JSON Parsing** | `HttpURLConnection`, `JSONObject`, `JSONArray`, background executor | `RecipeApiService.java` (TheMealDB REST API) |
| **Cloud Database (Firebase)** | Firebase Realtime Database backup/restore with offline fallback | `FirebaseManager.java`, `CloudSyncActivity.java` |

---

## 🛠️ How to Open, Build, and Run in Android Studio

1. **Open Android Studio** (Hedgehog, Iguana, Jellyfish, Koala, Ladybug, or newer).
2. Select **File > Open...** and choose the `FreshCheckApp` directory.
3. Allow Android Studio to complete Gradle sync.
4. Select an Android Emulator or connected physical device (running Android 7.0 / API 24 or higher).
5. Click the green **Run 'app'** button (`Shift + F10`).

---

## 🧪 Testing Checklist

- [ ] **Home Dashboard**: Confirm that counter numbers reflect items in database.
- [ ] **Donut Chart**: Observe donut chart segments change as food statuses update.
- [ ] **Add Food**: Tap `+ Add Food`, select dates via DatePicker, pick category and location, and save.
- [ ] **Inventory Filter**: Test searching for "Milk" and switching status chips ("Expiring Soon", "Expired").
- [ ] **Use First Priority**: On dashboard, tap "Consumed" to mark an item eaten or "Waste" to record discard.
- [ ] **Recipe Suggestions**: Open Recipes tab. Observe local recipes matching inventory ingredients. Tap "Online API" to fetch live recipes from TheMealDB.
- [ ] **Shopping List**: Add groceries, check items off, and tap "Share" to test the implicit Intent.
- [ ] **ContentProvider Demo**: From toolbar menu, select **ContentProvider Demo**. Tap "Query via ContentResolver" to view live Cursor records.
- [ ] **Inventory Audit Service**: From toolbar menu, tap the audit icon to run the background service and observe completion toast/notification.
- [ ] **Waste Analytics**: Open Waste tab to inspect total financial loss and the custom `WasteBreakdownChartView`.
