# FreshCheck — Smart Food Management & Waste Reduction Application

**FreshCheck** is an advanced, production-grade Android application built with **Java** and **Material Design 3 (XML)**. Designed for households, university students, and mess residents, FreshCheck helps monitor food inventory, calculate dynamic expiry countdowns, eliminate food waste, intelligently suggest recipes based on expiring ingredients, manage shared mess pantry supplies, track monthly grocery budgets in Bangladeshi Taka (৳), scan receipts with ML Kit OCR, and synchronize data across devices with Firebase.

FreshCheck follows a strict **Offline-First Architecture**: all core features function seamlessly without an internet connection using local SQLite database storage. When network connectivity is available, data synchronizes with Firebase Cloud Firestore and Realtime Database.

---

## 🚀 Key Modules & Capabilities

### 1. Interactive Dashboard & Expiry Metrics
* **Hardcode-Free Expiry Calculation**: Dynamically computes Total Active Items, Fresh Food (>3 days), Expiring Soon (1–3 days), Expires Today (0 days), and Expired (<0 days).
* **Interactive Freshness Donut Chart**: Custom View (`ExpiryStatusDonutView`) dynamically rendering real-time food freshness percentages and status distributions via Android Canvas and Paint.
* **Use First Priority Section**: Automatically prioritizes food items approaching their expiration dates with immediate one-tap partial or full consumption and waste logging.
* **Scope Selector**: Effortlessly switch between Personal Pantry and Shared Household/Mess scope directly from the dashboard header.
* **Recently Added Stream**: Dynamic audit trail of newly recorded pantry supplies.

### 2. Comprehensive Food Inventory Management
* **Full CRUD Operations**: Create, Read, Update, and Delete food records with verification dialogs.
* **Dynamic Status Classification**:
  * **Fresh**: > 3 days remaining.
  * **Expiring Soon**: 1–3 days remaining.
  * **Expires Today**: 0 days remaining.
  * **Expired**: Negative days remaining.
* **Multi-Criteria Filtering & Sorting**: Filter by Category (Vegetables, Fruits, Dairy, Meat, Fish, Bakery, Grains, Beverages, Snacks, Others), Storage Location (Refrigerator, Freezer, Pantry, Kitchen Shelf, Drawer, Other), and Sort by Expiry Date (Earliest/Latest), Alphabetical (A-Z/Z-A), or Recently Added.
* **Detailed Inspection Modal**: View complete metadata, notes, photo thumbnail, price in Taka (৳), and storage zone.

### 3. Shared Household & Mess Inventory
* **Mess/Household Creation**: Create a shared household or mess and automatically generate a unique join code (e.g., `MESS-8X2K`).
* **Invitation System**: Join any shared household by typing the invite code.
* **Role-Based Membership**: Supports Admin and Member roles.
* **Shared Real-Time Feeds**: Multi-user inventory, shared grocery lists, and persistent member activity audit logs.
* **Guest Compatibility**: Guest and anonymous users can use the personal inventory freely without joining a mess.

### 4. Smart Meal Planner
* **Weekly Calendar (Monday–Sunday)**: Plan Breakfast, Lunch, Dinner, and Snack slots for every day of the week.
* **Ingredient Availability Status**: Evaluates whether planned meal ingredients are currently in stock or expiring.
* **Automated Grocery Generation**: One-tap "Add Missing Ingredients to Grocery List" scans weekly meals against the pantry and automatically populates the grocery list with missing items.

### 5. Food Image Scanner (ML Kit)
* **On-Device Vision Recognition**: Uses Google ML Kit Image Labeling via Camera and Gallery.
* **Heuristic Food Taxonomy**: Maps detected labels to food categories (Fruits, Dairy, Meat, Bakery, Vegetables, etc.).
* **Safety Confirmation Protocol**: Never invents expiration dates; presents detected labels in an interactive dialog for user verification before persisting.

### 6. Grocery Receipt OCR Scanner (ML Kit)
* **Text Recognition**: Uses Google ML Kit Text Recognition to parse grocery slips and receipts.
* **Intelligent Item & Price Extraction**: Regex-based heuristic parser separates product descriptions and prices in Taka (`৳`).
* **Direct Import**: Imports recognized receipt lines directly into the active grocery shopping list.

### 7. Budget & Financial Savings Tracker
* **Currency in Bangladeshi Taka (৳)**: Default currency across all financial modules.
* **Monthly Budget Tracking**: Set monthly grocery spending targets with an interactive visual progress gauge.
* **Financial Loss vs. Net Savings**: Real-time calculation of Grocery Expenses, Recorded Food Waste Loss, and Estimated Net Savings.

### 8. Partial Consumption & Restocking Analytics
* **Partial Consumption**: Allows consuming fractional portions (e.g., consuming 0.5 kg of 1.0 kg) while updating remaining stock and recording consumption history.
* **Usage Frequency Analytics**: Identifies frequently consumed staples versus rarely used items to optimize restocking recommendations.

### 9. Smart Recipe Suggestions & Ingredient Match Algorithm
* **Intelligent Recipe Matcher**: Compares recipe ingredients against available pantry stock, calculating exact Match Percentage (`90% Match`), available ingredients, and missing ingredients.
* **Expiring Ingredient Priority**: Recipes that utilize ingredients nearing expiration are awarded top priority badges (`🔥 Uses Expiring Ingredients`).
* **12+ Curated Offline Recipes**: Structured into Breakfast, Lunch, Dinner, and Snacks.
* **TheMealDB REST API**: Fetches live online recipes with cooking instructions and video links, backed by graceful offline fallbacks.

### 10. Smart Grocery Shopping List
* **Quick-Add Interface**: Add grocery items with quantity, unit, and category.
* **Purchase Strike-Through**: Check off items as purchased in real time.
* **Verified Inventory Transfer**: One-tap transfer from grocery list to pantry inventory with mandatory expiration date confirmation (no invented expiry dates).
* **Implicit Intent Sharing**: Share grocery lists via SMS, WhatsApp, Email, or Notes.

### 11. Food Waste Challenge & Gamification
* **Food Saver Score (0–100)**: Evaluates pantry utilization, consumption ratio, and waste prevention.
* **Achievement Badges**: Unlock tiers such as "Zero-Waste Master", "Pantry Champion", "Smart Shopper", and "Budget Guardian".
* **Weekly Waste Challenges**: Interactive checklist of actionable habits to reduce spoilage.

### 12. Smart Food Storage Guide
* **Curated Category Advice**: Storage tips for Vegetables, Fruits, Dairy, Meat, Fish, and Bakery items.
* **Refrigerator Temperature Zones**: Interactive breakdown of optimal placement (Upper Shelf, Middle Shelf, Crisper Drawer, Door Shelves).
* **Food Safety Disclaimer**: Clear distinction between general storage tips and guaranteed food safety.

### 13. Data Export & Backup
* **CSV Inventory Export**: Export active inventory to standard CSV format.
* **CSV Waste Report**: Export historical waste records for external analysis.
* **JSON Backup & Restore**: Full local snapshot export and validation-checked restore.

### 14. Firebase Multi-Cloud Sync & Social Authentication
* **Dual Cloud Support**: Synchronizes with both **Firebase Cloud Firestore** and **Firebase Realtime Database**.
* **Authentication Providers**: Google Sign-In, Facebook OAuth, GitHub OAuth, and Anonymous/Guest Sign-In.
* **User-Scoped Security**: Isolate and secure inventory data per authenticated user (`/users/{uid}/inventory`).

---

## 🏛️ Architecture & Android Syllabus Coverage

| Syllabus Topic | Implemented Component | Class / File Reference |
| :--- | :--- | :--- |
| **Architectural Pattern** | Offline-First Repository Pattern with Multi-Activity & Fragments | `com.freshcheck.app.*` |
| **Local Database (SQLite)** | 9 Relational Tables with Indexes, Transactions & Migration | `DatabaseHelper.java` (Version 2) |
| **Data Repositories** | Decoupled Data Access Objects | `FoodRepository`, `HouseholdRepository`, `MealPlanRepository`, `ShoppingRepository`, `WasteRepository`, `ConsumptionRepository`, `BudgetRepository` |
| **Machine Learning (ML Kit)** | On-Device Vision Labeling & Text OCR | `FoodScannerActivity`, `ReceiptScannerActivity` |
| **ContentProvider** | Custom Provider with MIME types & URI Matcher | `FoodContentProvider.java` (`content://com.freshcheck.app.provider/food_items`) |
| **ContentResolver** | ContentResolver inspection & CRUD demonstration | `ProviderDemoActivity.java` |
| **Background WorkManager** | Periodic 24h background expiry audit | `ExpiryCheckWorker.java`, `WorkManagerScheduler.java` |
| **Started Services** | Asynchronous inventory audit service | `InventoryAuditService.java` |
| **BroadcastReceivers** | Alarm triggers, audit broadcast, boot rescheduling | `ExpiryAlarmReceiver`, `InventoryAuditReceiver`, `BootReceiver` |
| **Custom Canvas Views** | Custom-painted views implementing `onMeasure()` and `onDraw()` | `ExpiryStatusDonutView.java`, `WasteBreakdownChartView.java` |
| **Implicit Intents** | System share sheet (`ACTION_SEND`), video tutorial link (`ACTION_VIEW`) | `ShoppingFragment`, `RecipeDetailActivity`, `BackupRestoreManager` |
| **Network & REST API** | HTTP requests via `HttpURLConnection` & JSON parsing | `RecipeApiService.java` (TheMealDB API) |
| **Cloud Synchronization** | Cloud Firestore & Realtime Database sync | `FirebaseManager.java`, `CloudSyncActivity.java` |
| **Authentication** | Google, Facebook, GitHub, Anonymous, Sign Out | `AuthActivity.java`, `FirebaseAuth` |

---

## 🛠️ Database Schema (SQLite Version 2)

FreshCheck utilizes 9 indexed SQLite tables:
1. `food_items`: Pantry inventory records with expiration dates, quantities, categories, storage locations, photos, and household scoping.
2. `shopping_items`: Grocery shopping items with purchase states and estimated costs.
3. `waste_records`: Discarded food records with loss values in Taka (৳), discard reasons, and notes.
4. `consumption_records`: Partial and full consumption events with quantities and timestamps.
5. `meal_plans`: 7-day calendar plans mapped to recipes and meal slots (Breakfast, Lunch, Dinner, Snack).
6. `households`: Shared household/mess groups with unique `MESS-XXXX` join codes.
7. `household_members`: Association table linking users to households with role definitions (Admin/Member).
8. `activity_logs`: Chronological audit trail of household events.
9. `budget_settings`: Monthly grocery budget allocations in Bangladeshi Taka (৳).

---

## 📱 How to Build and Run

### Prerequisites
* **Android Studio** Ladybug or newer
* **JDK 17 or bundled Android Studio JBR (Java 25)**
* **Android SDK**: Min SDK 24 (Android 7.0), Target SDK 34 (Android 14)

### Building via Command Line
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd FreshCheckApp
.\gradlew.bat assembleDebug
```
The compiled debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
Developed for the university-level Advanced Android Development project. All rights reserved.
