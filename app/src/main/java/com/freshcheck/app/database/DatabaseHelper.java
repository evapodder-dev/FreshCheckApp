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
    public static final int DATABASE_VERSION = 1;

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

    // Table: shopping_items
    public static final String TABLE_SHOPPING_ITEMS = "shopping_items";
    public static final String COL_SHOPPING_ID = "_id";
    public static final String COL_SHOPPING_NAME = "name";
    public static final String COL_SHOPPING_QUANTITY = "quantity";
    public static final String COL_SHOPPING_UNIT = "unit";
    public static final String COL_SHOPPING_CATEGORY = "category";
    public static final String COL_SHOPPING_IS_PURCHASED = "is_purchased";
    public static final String COL_SHOPPING_NOTES = "notes";

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
        Log.d(TAG, "Creating FreshCheck database tables...");

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
                + COL_FOOD_FIREBASE_ID + " TEXT"
                + ");";

        String createShoppingTable = "CREATE TABLE " + TABLE_SHOPPING_ITEMS + " ("
                + COL_SHOPPING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_SHOPPING_NAME + " TEXT NOT NULL, "
                + COL_SHOPPING_QUANTITY + " REAL NOT NULL, "
                + COL_SHOPPING_UNIT + " TEXT NOT NULL, "
                + COL_SHOPPING_CATEGORY + " TEXT NOT NULL, "
                + COL_SHOPPING_IS_PURCHASED + " INTEGER DEFAULT 0, "
                + COL_SHOPPING_NOTES + " TEXT"
                + ");";

        String createWasteTable = "CREATE TABLE " + TABLE_WASTE_RECORDS + " ("
                + COL_WASTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_WASTE_FOOD_NAME + " TEXT NOT NULL, "
                + COL_WASTE_QUANTITY + " REAL NOT NULL, "
                + COL_WASTE_UNIT + " TEXT NOT NULL, "
                + COL_WASTE_DATE + " TEXT NOT NULL, "
                + COL_WASTE_REASON + " TEXT NOT NULL, "
                + COL_WASTE_COST + " REAL DEFAULT 0.0, "
                + COL_WASTE_NOTES + " TEXT"
                + ");";

        db.execSQL(createFoodTable);
        db.execSQL(createShoppingTable);
        db.execSQL(createWasteTable);

        // Seed initial demo data for immediate rich evaluation
        seedInitialData(db);
    }

    private void seedInitialData(SQLiteDatabase db) {
        Log.d(TAG, "Seeding initial inventory and shopping data...");

        // Food Items: Seed items across statuses (Expires Today, Expiring Soon, Fresh, Expired)
        insertInitialFood(db, "Fresh Milk 2%", 1.0, "liters", "Dairy & Eggs",
                DateUtils.getFutureDateString(-5), DateUtils.getTodayString(), "Refrigerator", 3.49, "Use for cereal or cooking today");

        insertInitialFood(db, "Baby Spinach", 200.0, "g", "Produce (Fruits & Veg)",
                DateUtils.getFutureDateString(-3), DateUtils.getFutureDateString(1), "Refrigerator", 2.99, "Crisp leaves, use in salad or smoothie");

        insertInitialFood(db, "Greek Yogurt", 2.0, "packs", "Dairy & Eggs",
                DateUtils.getFutureDateString(-4), DateUtils.getFutureDateString(2), "Refrigerator", 4.50, "Strawberry flavor");

        insertInitialFood(db, "Sourdough Bread", 1.0, "items", "Bakery & Bread",
                DateUtils.getFutureDateString(-2), DateUtils.getFutureDateString(3), "Countertop", 3.75, "Great for morning toast");

        insertInitialFood(db, "Chicken Breast", 500.0, "g", "Meat & Poultry",
                DateUtils.getFutureDateString(-1), DateUtils.getFutureDateString(4), "Refrigerator", 6.20, "Keep chilled or freeze");

        insertInitialFood(db, "Organic Eggs", 12.0, "items", "Dairy & Eggs",
                DateUtils.getFutureDateString(-2), DateUtils.getFutureDateString(10), "Refrigerator", 4.10, "Grade A brown eggs");

        insertInitialFood(db, "Brown Rice", 1.0, "kg", "Pantry & Dry Goods",
                DateUtils.getFutureDateString(-10), DateUtils.getFutureDateString(90), "Pantry", 2.25, "Long grain rice");

        insertInitialFood(db, "Old Tomatoes", 3.0, "items", "Produce (Fruits & Veg)",
                DateUtils.getFutureDateString(-12), DateUtils.getFutureDateString(-1), "Refrigerator", 1.95, "Past prime, consider composting");

        // Seed Shopping Items
        insertInitialShopping(db, "Olive Oil", 1.0, "bottles", "Pantry & Dry Goods", false, "Extra virgin");
        insertInitialShopping(db, "Avocados", 4.0, "items", "Produce (Fruits & Veg)", false, "Medium ripe");
        insertInitialShopping(db, "Rolled Oats", 1.0, "kg", "Pantry & Dry Goods", true, "Quick cooking");

        // Seed Sample Waste Record
        ContentValues waste = new ContentValues();
        waste.put(COL_WASTE_FOOD_NAME, "Cheddar Cheese");
        waste.put(COL_WASTE_QUANTITY, 200.0);
        waste.put(COL_WASTE_UNIT, "g");
        waste.put(COL_WASTE_DATE, DateUtils.getFutureDateString(-2));
        waste.put(COL_WASTE_REASON, "Spoiled / Moldy");
        waste.put(COL_WASTE_COST, 3.80);
        waste.put(COL_WASTE_NOTES, "Forgotten behind salad bin");
        db.insert(TABLE_WASTE_RECORDS, null, waste);
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
        db.insert(TABLE_FOOD_ITEMS, null, cv);
    }

    private void insertInitialShopping(SQLiteDatabase db, String name, double qty, String unit,
                                      String category, boolean isPurchased, String notes) {
        ContentValues cv = new ContentValues();
        cv.put(COL_SHOPPING_NAME, name);
        cv.put(COL_SHOPPING_QUANTITY, qty);
        cv.put(COL_SHOPPING_UNIT, unit);
        cv.put(COL_SHOPPING_CATEGORY, category);
        cv.put(COL_SHOPPING_IS_PURCHASED, isPurchased ? 1 : 0);
        cv.put(COL_SHOPPING_NOTES, notes);
        db.insert(TABLE_SHOPPING_ITEMS, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOOD_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHOPPING_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WASTE_RECORDS);
        onCreate(db);
    }
}
