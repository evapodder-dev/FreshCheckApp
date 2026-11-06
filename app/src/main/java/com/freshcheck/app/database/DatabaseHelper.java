package com.freshcheck.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.freshcheck.app.util.DateUtils;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    public static final String DATABASE_NAME = "freshcheck.db";
    public static final int DATABASE_VERSION = 2;

    // Table: food_items
    public static final String TABLE_FOOD_ITEMS = "food_items";
    public static final String COL_FOOD_ID = "_id";
    public static final String COL_FOOD_NAME = "name";
    public static final String COL_FOOD_QUANTITY = "quantity";
    public static final String COL_FOOD_UNIT = "unit";
    public static final String COL_FOOD_CATEGORY = "category";
    public static final String COL_FOOD_PURCHASE_DATE = "purchase_date";
    public static final String COL_FOOD_EXPIRY_DATE = "expiry_date";
    public static final String COL_FOOD_LOCATION = "location";
    public static final String COL_FOOD_COST = "estimated_cost";
    public static final String COL_FOOD_IS_CONSUMED = "is_consumed";
    public static final String COL_FOOD_IS_WASTED = "is_wasted";
    public static final String COL_FOOD_NOTES = "notes";
    public static final String COL_FOOD_FIREBASE_ID = "firebase_id";
    public static final String COL_FOOD_IMAGE_URI = "image_uri";
    public static final String COL_FOOD_HOUSEHOLD_ID = "household_id";
    public static final String COL_FOOD_CREATED_AT = "created_at";
    public static final String COL_FOOD_UPDATED_AT = "updated_at";

    // Table: shopping_items
    public static final String TABLE_SHOPPING_ITEMS = "shopping_items";
    public static final String COL_SHOPPING_ID = "_id";
    public static final String COL_SHOPPING_NAME = "name";
    public static final String COL_SHOPPING_QUANTITY = "quantity";
    public static final String COL_SHOPPING_UNIT = "unit";
    public static final String COL_SHOPPING_CATEGORY = "category";
    public static final String COL_SHOPPING_IS_PURCHASED = "is_purchased";
    public static final String COL_SHOPPING_COST = "estimated_cost";
    public static final String COL_SHOPPING_NOTES = "notes";
    public static final String COL_SHOPPING_HOUSEHOLD_ID = "household_id";

    // Table: waste_records
    public static final String TABLE_WASTE_RECORDS = "waste_records";
    public static final String COL_WASTE_ID = "_id";
    public static final String COL_WASTE_FOOD_NAME = "food_name";
    public static final String COL_WASTE_QUANTITY = "quantity";
    public static final String COL_WASTE_UNIT = "unit";
    public static final String COL_WASTE_DATE = "waste_date";
    public static final String COL_WASTE_REASON = "reason";
    public static final String COL_WASTE_COST = "estimated_cost";
    public static final String COL_WASTE_NOTES = "notes";
    public static final String COL_WASTE_HOUSEHOLD_ID = "household_id";

    // Table: consumption_records
    public static final String TABLE_CONSUMPTION_RECORDS = "consumption_records";
    public static final String COL_CONSUMPTION_ID = "_id";
    public static final String COL_CONSUMPTION_FOOD_ID = "food_id";
    public static final String COL_CONSUMPTION_FOOD_NAME = "food_name";
    public static final String COL_CONSUMPTION_QUANTITY = "quantity";
    public static final String COL_CONSUMPTION_UNIT = "unit";
    public static final String COL_CONSUMPTION_DATE = "consumed_date";
    public static final String COL_CONSUMPTION_CATEGORY = "category";
    public static final String COL_CONSUMPTION_HOUSEHOLD_ID = "household_id";

    // Table: meal_plans
    public static final String TABLE_MEAL_PLANS = "meal_plans";
    public static final String COL_MEAL_ID = "_id";
    public static final String COL_MEAL_DATE = "plan_date";
    public static final String COL_MEAL_TYPE = "meal_type";
    public static final String COL_MEAL_RECIPE_ID = "recipe_id";
    public static final String COL_MEAL_RECIPE_TITLE = "recipe_title";
    public static final String COL_MEAL_NOTES = "notes";
    public static final String COL_MEAL_HOUSEHOLD_ID = "household_id";
    public static final String COL_MEAL_IS_COMPLETED = "is_completed";

    // Table: households
    public static final String TABLE_HOUSEHOLDS = "households";
    public static final String COL_HOUSEHOLD_ID = "household_id";
    public static final String COL_HOUSEHOLD_NAME = "name";
    public static final String COL_HOUSEHOLD_INVITE_CODE = "invite_code";
    public static final String COL_HOUSEHOLD_ADMIN_UID = "admin_uid";
    public static final String COL_HOUSEHOLD_CREATED_AT = "created_at";
    public static final String COL_HOUSEHOLD_MEMBER_COUNT = "member_count";

    // Table: household_members
    public static final String TABLE_HOUSEHOLD_MEMBERS = "household_members";
    public static final String COL_MEMBER_ID = "_id";
    public static final String COL_MEMBER_HOUSEHOLD_ID = "household_id";
    public static final String COL_MEMBER_USER_UID = "user_uid";
    public static final String COL_MEMBER_USER_NAME = "user_name";
    public static final String COL_MEMBER_ROLE = "role";
    public static final String COL_MEMBER_JOINED_AT = "joined_at";

    // Table: activity_logs
    public static final String TABLE_ACTIVITY_LOGS = "activity_logs";
    public static final String COL_ACTIVITY_ID = "_id";
    public static final String COL_ACTIVITY_HOUSEHOLD_ID = "household_id";
    public static final String COL_ACTIVITY_USER_UID = "user_uid";
    public static final String COL_ACTIVITY_USER_NAME = "user_name";
    public static final String COL_ACTIVITY_ACTION = "action";
    public static final String COL_ACTIVITY_TIMESTAMP = "timestamp";

    // Table: budget_settings
    public static final String TABLE_BUDGET_SETTINGS = "budget_settings";
    public static final String COL_BUDGET_ID = "_id";
    public static final String COL_BUDGET_MONTH = "month_key"; // e.g. "2026-11"
    public static final String COL_BUDGET_AMOUNT = "monthly_budget";
    public static final String COL_BUDGET_CURRENCY = "currency_symbol";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating FreshCheck database tables (v" + DATABASE_VERSION + ")...");

        String createFoodTable = "CREATE TABLE " + TABLE_FOOD_ITEMS + " ("
                + COL_FOOD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_FOOD_NAME + " TEXT NOT NULL, "
                + COL_FOOD_QUANTITY + " REAL NOT NULL, "
                + COL_FOOD_UNIT + " TEXT NOT NULL, "
                + COL_FOOD_CATEGORY + " TEXT NOT NULL, "
                + COL_FOOD_PURCHASE_DATE + " TEXT NOT NULL, "
                + COL_FOOD_EXPIRY_DATE + " TEXT NOT NULL, "
                + COL_FOOD_LOCATION + " TEXT NOT NULL, "
                + COL_FOOD_COST + " REAL DEFAULT 0.0, "
                + COL_FOOD_IS_CONSUMED + " INTEGER DEFAULT 0, "
                + COL_FOOD_IS_WASTED + " INTEGER DEFAULT 0, "
                + COL_FOOD_NOTES + " TEXT, "
                + COL_FOOD_FIREBASE_ID + " TEXT, "
                + COL_FOOD_IMAGE_URI + " TEXT, "
                + COL_FOOD_HOUSEHOLD_ID + " TEXT, "
                + COL_FOOD_CREATED_AT + " INTEGER DEFAULT 0, "
                + COL_FOOD_UPDATED_AT + " INTEGER DEFAULT 0"
                + ");";

        String createShoppingTable = "CREATE TABLE " + TABLE_SHOPPING_ITEMS + " ("
                + COL_SHOPPING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_SHOPPING_NAME + " TEXT NOT NULL, "
                + COL_SHOPPING_QUANTITY + " REAL NOT NULL, "
                + COL_SHOPPING_UNIT + " TEXT NOT NULL, "
                + COL_SHOPPING_CATEGORY + " TEXT NOT NULL, "
                + COL_SHOPPING_IS_PURCHASED + " INTEGER DEFAULT 0, "
                + COL_SHOPPING_COST + " REAL DEFAULT 0.0, "
                + COL_SHOPPING_NOTES + " TEXT, "
                + COL_SHOPPING_HOUSEHOLD_ID + " TEXT"
                + ");";

        String createWasteTable = "CREATE TABLE " + TABLE_WASTE_RECORDS + " ("
                + COL_WASTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_WASTE_FOOD_NAME + " TEXT NOT NULL, "
                + COL_WASTE_QUANTITY + " REAL NOT NULL, "
                + COL_WASTE_UNIT + " TEXT NOT NULL, "
                + COL_WASTE_DATE + " TEXT NOT NULL, "
                + COL_WASTE_REASON + " TEXT NOT NULL, "
                + COL_WASTE_COST + " REAL DEFAULT 0.0, "
                + COL_WASTE_NOTES + " TEXT, "
                + COL_WASTE_HOUSEHOLD_ID + " TEXT"
                + ");";

        String createConsumptionTable = "CREATE TABLE " + TABLE_CONSUMPTION_RECORDS + " ("
                + COL_CONSUMPTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CONSUMPTION_FOOD_ID + " INTEGER, "
                + COL_CONSUMPTION_FOOD_NAME + " TEXT NOT NULL, "
                + COL_CONSUMPTION_QUANTITY + " REAL NOT NULL, "
                + COL_CONSUMPTION_UNIT + " TEXT NOT NULL, "
                + COL_CONSUMPTION_DATE + " TEXT NOT NULL, "
                + COL_CONSUMPTION_CATEGORY + " TEXT, "
                + COL_CONSUMPTION_HOUSEHOLD_ID + " TEXT"
                + ");";

        String createMealPlanTable = "CREATE TABLE " + TABLE_MEAL_PLANS + " ("
                + COL_MEAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_MEAL_DATE + " TEXT NOT NULL, "
                + COL_MEAL_TYPE + " TEXT NOT NULL, "
                + COL_MEAL_RECIPE_ID + " TEXT, "
                + COL_MEAL_RECIPE_TITLE + " TEXT NOT NULL, "
                + COL_MEAL_NOTES + " TEXT, "
                + COL_MEAL_HOUSEHOLD_ID + " TEXT, "
                + COL_MEAL_IS_COMPLETED + " INTEGER DEFAULT 0"
                + ");";

        String createHouseholdTable = "CREATE TABLE " + TABLE_HOUSEHOLDS + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_HOUSEHOLD_ID + " TEXT UNIQUE NOT NULL, "
                + COL_HOUSEHOLD_NAME + " TEXT NOT NULL, "
                + COL_HOUSEHOLD_INVITE_CODE + " TEXT NOT NULL, "
                + COL_HOUSEHOLD_ADMIN_UID + " TEXT NOT NULL, "
                + COL_HOUSEHOLD_CREATED_AT + " INTEGER, "
                + COL_HOUSEHOLD_MEMBER_COUNT + " INTEGER DEFAULT 1"
                + ");";

        String createMemberTable = "CREATE TABLE " + TABLE_HOUSEHOLD_MEMBERS + " ("
                + COL_MEMBER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_MEMBER_HOUSEHOLD_ID + " TEXT NOT NULL, "
                + COL_MEMBER_USER_UID + " TEXT NOT NULL, "
                + COL_MEMBER_USER_NAME + " TEXT NOT NULL, "
                + COL_MEMBER_ROLE + " TEXT NOT NULL, "
                + COL_MEMBER_JOINED_AT + " INTEGER"
                + ");";

        String createActivityTable = "CREATE TABLE " + TABLE_ACTIVITY_LOGS + " ("
                + COL_ACTIVITY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ACTIVITY_HOUSEHOLD_ID + " TEXT, "
                + COL_ACTIVITY_USER_UID + " TEXT, "
                + COL_ACTIVITY_USER_NAME + " TEXT NOT NULL, "
                + COL_ACTIVITY_ACTION + " TEXT NOT NULL, "
                + COL_ACTIVITY_TIMESTAMP + " INTEGER"
                + ");";

        String createBudgetTable = "CREATE TABLE " + TABLE_BUDGET_SETTINGS + " ("
                + COL_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_BUDGET_MONTH + " TEXT UNIQUE NOT NULL, "
                + COL_BUDGET_AMOUNT + " REAL DEFAULT 5000.0, "
                + COL_BUDGET_CURRENCY + " TEXT DEFAULT '৳'"
                + ");";

        db.execSQL(createFoodTable);
        db.execSQL(createShoppingTable);
        db.execSQL(createWasteTable);
        db.execSQL(createConsumptionTable);
        db.execSQL(createMealPlanTable);
        db.execSQL(createHouseholdTable);
        db.execSQL(createMemberTable);
        db.execSQL(createActivityTable);
        db.execSQL(createBudgetTable);

        // Indexes for performance
        db.execSQL("CREATE INDEX idx_food_expiry ON " + TABLE_FOOD_ITEMS + " (" + COL_FOOD_EXPIRY_DATE + ");");
        db.execSQL("CREATE INDEX idx_food_household ON " + TABLE_FOOD_ITEMS + " (" + COL_FOOD_HOUSEHOLD_ID + ");");
        db.execSQL("CREATE INDEX idx_meal_date ON " + TABLE_MEAL_PLANS + " (" + COL_MEAL_DATE + ");");

        // Seed initial demo data for rich evaluation
        seedInitialData(db);
    }

    private void seedInitialData(SQLiteDatabase db) {
        Log.d(TAG, "Seeding initial inventory, shopping, consumption, and meal plan data...");

        // Food Items (Categories matching specification)
        insertInitialFood(db, "Fresh Milk 2%", 1.0, "liters", "Dairy",
                DateUtils.getFutureDateString(-5), DateUtils.getTodayString(), "Refrigerator", 110.0, "For cereal or tea today");

        insertInitialFood(db, "Baby Spinach", 200.0, "g", "Vegetables",
                DateUtils.getFutureDateString(-3), DateUtils.getFutureDateString(1), "Refrigerator", 45.0, "Crisp leaves, use in salad or curry");

        insertInitialFood(db, "Greek Yogurt", 2.0, "packs", "Dairy",
                DateUtils.getFutureDateString(-4), DateUtils.getFutureDateString(2), "Refrigerator", 160.0, "Strawberry flavor");

        insertInitialFood(db, "Sourdough Bread", 1.0, "items", "Bakery",
                DateUtils.getFutureDateString(-2), DateUtils.getFutureDateString(3), "Pantry", 85.0, "Great for morning toast");

        insertInitialFood(db, "Chicken Breast", 500.0, "g", "Meat",
                DateUtils.getFutureDateString(-1), DateUtils.getFutureDateString(4), "Refrigerator", 240.0, "Boneless, keep chilled");

        insertInitialFood(db, "Organic Eggs", 12.0, "items", "Dairy",
                DateUtils.getFutureDateString(-2), DateUtils.getFutureDateString(10), "Refrigerator", 150.0, "Grade A brown eggs");

        insertInitialFood(db, "Miniket Rice", 5.0, "kg", "Grains",
                DateUtils.getFutureDateString(-10), DateUtils.getFutureDateString(120), "Pantry", 360.0, "Premium polished rice");

        insertInitialFood(db, "Overripe Tomatoes", 3.0, "items", "Vegetables",
                DateUtils.getFutureDateString(-12), DateUtils.getFutureDateString(-1), "Refrigerator", 30.0, "Past prime, consider composting or soup");

        // Seed Shopping Items
        insertInitialShopping(db, "Mustard Oil", 1.0, "liters", "Others", false, 180.0, "Cold-pressed pure oil");
        insertInitialShopping(db, "Green Apples", 1.0, "kg", "Fruits", false, 240.0, "Fresh sweet apples");
        insertInitialShopping(db, "Rolled Oats", 500.0, "g", "Grains", true, 195.0, "Breakfast quick cooking");

        // Seed Waste Record
        ContentValues waste = new ContentValues();
        waste.put(COL_WASTE_FOOD_NAME, "Cheddar Cheese");
        waste.put(COL_WASTE_QUANTITY, 200.0);
        waste.put(COL_WASTE_UNIT, "g");
        waste.put(COL_WASTE_DATE, DateUtils.getFutureDateString(-2));
        waste.put(COL_WASTE_REASON, "Spoiled / Moldy");
        waste.put(COL_WASTE_COST, 220.0);
        waste.put(COL_WASTE_NOTES, "Forgotten behind vegetable drawer");
        waste.put(COL_WASTE_HOUSEHOLD_ID, "");
        db.insert(TABLE_WASTE_RECORDS, null, waste);

        // Seed Consumption Records
        ContentValues c1 = new ContentValues();
        c1.put(COL_CONSUMPTION_FOOD_ID, 1);
        c1.put(COL_CONSUMPTION_FOOD_NAME, "Bananas");
        c1.put(COL_CONSUMPTION_QUANTITY, 4.0);
        c1.put(COL_CONSUMPTION_UNIT, "items");
        c1.put(COL_CONSUMPTION_DATE, DateUtils.getFutureDateString(-1));
        c1.put(COL_CONSUMPTION_CATEGORY, "Fruits");
        c1.put(COL_CONSUMPTION_HOUSEHOLD_ID, "");
        db.insert(TABLE_CONSUMPTION_RECORDS, null, c1);

        // Seed Sample Meal Plan
        ContentValues m1 = new ContentValues();
        m1.put(COL_MEAL_DATE, DateUtils.getTodayString());
        m1.put(COL_MEAL_TYPE, "Breakfast");
        m1.put(COL_MEAL_RECIPE_ID, "local_1");
        m1.put(COL_MEAL_RECIPE_TITLE, "Golden Sourdough French Toast");
        m1.put(COL_MEAL_NOTES, "Uses expiring Sourdough Bread & Milk");
        m1.put(COL_MEAL_HOUSEHOLD_ID, "");
        m1.put(COL_MEAL_IS_COMPLETED, 0);
        db.insert(TABLE_MEAL_PLANS, null, m1);

        // Seed Default Monthly Budget in Taka (৳)
        ContentValues b1 = new ContentValues();
        b1.put(COL_BUDGET_MONTH, DateUtils.getTodayString().substring(0, 7)); // e.g. "2026-11"
        b1.put(COL_BUDGET_AMOUNT, 8000.0);
        b1.put(COL_BUDGET_CURRENCY, "৳");
        db.insert(TABLE_BUDGET_SETTINGS, null, b1);
    }

    private void insertInitialFood(SQLiteDatabase db, String name, double qty, String unit,
                                  String category, String purchaseDate, String expiryDate,
                                  String location, double cost, String notes) {
        ContentValues cv = new ContentValues();
        cv.put(COL_FOOD_NAME, name);
        cv.put(COL_FOOD_QUANTITY, qty);
        cv.put(COL_FOOD_UNIT, unit);
        cv.put(COL_FOOD_CATEGORY, category);
        cv.put(COL_FOOD_PURCHASE_DATE, purchaseDate);
        cv.put(COL_FOOD_EXPIRY_DATE, expiryDate);
        cv.put(COL_FOOD_LOCATION, location);
        cv.put(COL_FOOD_COST, cost);
        cv.put(COL_FOOD_IS_CONSUMED, 0);
        cv.put(COL_FOOD_IS_WASTED, 0);
        cv.put(COL_FOOD_NOTES, notes);
        cv.put(COL_FOOD_IMAGE_URI, "");
        cv.put(COL_FOOD_HOUSEHOLD_ID, "");
        cv.put(COL_FOOD_CREATED_AT, System.currentTimeMillis());
        cv.put(COL_FOOD_UPDATED_AT, System.currentTimeMillis());
        db.insert(TABLE_FOOD_ITEMS, null, cv);
    }

    private void insertInitialShopping(SQLiteDatabase db, String name, double qty, String unit,
                                      String category, boolean isPurchased, double cost, String notes) {
        ContentValues cv = new ContentValues();
        cv.put(COL_SHOPPING_NAME, name);
        cv.put(COL_SHOPPING_QUANTITY, qty);
        cv.put(COL_SHOPPING_UNIT, unit);
        cv.put(COL_SHOPPING_CATEGORY, category);
        cv.put(COL_SHOPPING_IS_PURCHASED, isPurchased ? 1 : 0);
        cv.put(COL_SHOPPING_COST, cost);
        cv.put(COL_SHOPPING_NOTES, notes);
        cv.put(COL_SHOPPING_HOUSEHOLD_ID, "");
        db.insert(TABLE_SHOPPING_ITEMS, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Migrating database from version " + oldVersion + " to " + newVersion);
        if (oldVersion < 2) {
            safeAlterTable(db, TABLE_FOOD_ITEMS, COL_FOOD_IMAGE_URI, "TEXT");
            safeAlterTable(db, TABLE_FOOD_ITEMS, COL_FOOD_HOUSEHOLD_ID, "TEXT");
            safeAlterTable(db, TABLE_FOOD_ITEMS, COL_FOOD_CREATED_AT, "INTEGER DEFAULT 0");
            safeAlterTable(db, TABLE_FOOD_ITEMS, COL_FOOD_UPDATED_AT, "INTEGER DEFAULT 0");

            safeAlterTable(db, TABLE_SHOPPING_ITEMS, COL_SHOPPING_COST, "REAL DEFAULT 0.0");
            safeAlterTable(db, TABLE_SHOPPING_ITEMS, COL_SHOPPING_HOUSEHOLD_ID, "TEXT");

            safeAlterTable(db, TABLE_WASTE_RECORDS, COL_WASTE_HOUSEHOLD_ID, "TEXT");

            safeExecuteSql(db, "CREATE TABLE IF NOT EXISTS " + TABLE_CONSUMPTION_RECORDS + " ("
                    + COL_CONSUMPTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_CONSUMPTION_FOOD_ID + " INTEGER, "
                    + COL_CONSUMPTION_FOOD_NAME + " TEXT NOT NULL, "
                    + COL_CONSUMPTION_QUANTITY + " REAL NOT NULL, "
                    + COL_CONSUMPTION_UNIT + " TEXT NOT NULL, "
                    + COL_CONSUMPTION_DATE + " TEXT NOT NULL, "
                    + COL_CONSUMPTION_CATEGORY + " TEXT, "
                    + COL_CONSUMPTION_HOUSEHOLD_ID + " TEXT);");

            safeExecuteSql(db, "CREATE TABLE IF NOT EXISTS " + TABLE_MEAL_PLANS + " ("
                    + COL_MEAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_MEAL_DATE + " TEXT NOT NULL, "
                    + COL_MEAL_TYPE + " TEXT NOT NULL, "
                    + COL_MEAL_RECIPE_ID + " TEXT, "
                    + COL_MEAL_RECIPE_TITLE + " TEXT NOT NULL, "
                    + COL_MEAL_NOTES + " TEXT, "
                    + COL_MEAL_HOUSEHOLD_ID + " TEXT, "
                    + COL_MEAL_IS_COMPLETED + " INTEGER DEFAULT 0);");

            safeExecuteSql(db, "CREATE TABLE IF NOT EXISTS " + TABLE_HOUSEHOLDS + " ("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_HOUSEHOLD_ID + " TEXT UNIQUE NOT NULL, "
                    + COL_HOUSEHOLD_NAME + " TEXT NOT NULL, "
                    + COL_HOUSEHOLD_INVITE_CODE + " TEXT NOT NULL, "
                    + COL_HOUSEHOLD_ADMIN_UID + " TEXT NOT NULL, "
                    + COL_HOUSEHOLD_CREATED_AT + " INTEGER, "
                    + COL_HOUSEHOLD_MEMBER_COUNT + " INTEGER DEFAULT 1);");

            safeExecuteSql(db, "CREATE TABLE IF NOT EXISTS " + TABLE_HOUSEHOLD_MEMBERS + " ("
                    + COL_MEMBER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_MEMBER_HOUSEHOLD_ID + " TEXT NOT NULL, "
                    + COL_MEMBER_USER_UID + " TEXT NOT NULL, "
                    + COL_MEMBER_USER_NAME + " TEXT NOT NULL, "
                    + COL_MEMBER_ROLE + " TEXT NOT NULL, "
                    + COL_MEMBER_JOINED_AT + " INTEGER);");

            safeExecuteSql(db, "CREATE TABLE IF NOT EXISTS " + TABLE_ACTIVITY_LOGS + " ("
                    + COL_ACTIVITY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_ACTIVITY_HOUSEHOLD_ID + " TEXT, "
                    + COL_ACTIVITY_USER_UID + " TEXT, "
                    + COL_ACTIVITY_USER_NAME + " TEXT NOT NULL, "
                    + COL_ACTIVITY_ACTION + " TEXT NOT NULL, "
                    + COL_ACTIVITY_TIMESTAMP + " INTEGER);");

            safeExecuteSql(db, "CREATE TABLE IF NOT EXISTS " + TABLE_BUDGET_SETTINGS + " ("
                    + COL_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_BUDGET_MONTH + " TEXT UNIQUE NOT NULL, "
                    + COL_BUDGET_AMOUNT + " REAL DEFAULT 5000.0, "
                    + COL_BUDGET_CURRENCY + " TEXT DEFAULT '৳');");

            Log.i(TAG, "Database upgrade migration completed successfully");
        }
    }

    private void safeAlterTable(SQLiteDatabase db, String table, String column, String type) {
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type + ";");
        } catch (Exception e) {
            Log.d(TAG, "Column " + column + " already exists or alter skipped: " + e.getMessage());
        }
    }

    private void safeExecuteSql(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (Exception e) {
            Log.w(TAG, "safeExecuteSql error: " + e.getMessage());
        }
    }
}