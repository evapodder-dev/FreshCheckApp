package com.freshcheck.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.WasteRecord;
import com.freshcheck.app.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FoodRepository {
    private static final String TAG = "FoodRepository";
    private final DatabaseHelper dbHelper;

    public FoodRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insert(FoodItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = item.toContentValues();
        long id = db.insert(DatabaseHelper.TABLE_FOOD_ITEMS, null, values);
        item.setId(id);
        Log.d(TAG, "Inserted FoodItem with id: " + id);
        return id;
    }

    public int update(FoodItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = item.toContentValues();
        int rows = db.update(DatabaseHelper.TABLE_FOOD_ITEMS, values,
                DatabaseHelper.COL_FOOD_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
        Log.d(TAG, "Updated FoodItem id " + item.getId() + ", rows affected: " + rows);
        return rows;
    }

    public int delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_FOOD_ITEMS,
                DatabaseHelper.COL_FOOD_ID + " = ?",
                new String[]{String.valueOf(id)});
        Log.d(TAG, "Deleted FoodItem id " + id + ", rows affected: " + rows);
        return rows;
    }

    public FoodItem getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_FOOD_ITEMS, null,
                DatabaseHelper.COL_FOOD_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        FoodItem item = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                item = cursorToFoodItem(cursor);
            }
            cursor.close();
        }
        return item;
    }

    public List<FoodItem> getAllActive() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<FoodItem> list = new ArrayList<>();
        String selection = DatabaseHelper.COL_FOOD_IS_CONSUMED + " = 0 AND "
                + DatabaseHelper.COL_FOOD_IS_WASTED + " = 0";
        String orderBy = DatabaseHelper.COL_FOOD_EXPIRY_DATE + " ASC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_FOOD_ITEMS, null,
                selection, null, null, null, orderBy);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToFoodItem(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<FoodItem> searchAndFilter(String query, String category, String statusFilter) {
        List<FoodItem> allActive = getAllActive();
        List<FoodItem> filtered = new ArrayList<>();

        String q = query != null ? query.trim().toLowerCase() : "";
        boolean filterCategory = category != null && !category.equalsIgnoreCase("All") && !category.trim().isEmpty();
        boolean filterStatus = statusFilter != null && !statusFilter.equalsIgnoreCase("All") && !statusFilter.trim().isEmpty();

        for (FoodItem item : allActive) {
            boolean matchesQuery = q.isEmpty()
                    || item.getName().toLowerCase().contains(q)
                    || item.getNotes().toLowerCase().contains(q)
                    || item.getLocation().toLowerCase().contains(q);

            boolean matchesCategory = !filterCategory || item.getCategory().equalsIgnoreCase(category);

            boolean matchesStatus = true;
            if (filterStatus) {
                if (statusFilter.equalsIgnoreCase("Fresh")) {
                    matchesStatus = CategoryConstants.STATUS_FRESH.equals(item.getStatus());
                } else if (statusFilter.equalsIgnoreCase("Expiring Soon") || statusFilter.equalsIgnoreCase("Expiring")) {
                    matchesStatus = CategoryConstants.STATUS_EXPIRING_SOON.equals(item.getStatus())
                            || CategoryConstants.STATUS_EXPIRES_TODAY.equals(item.getStatus());
                } else if (statusFilter.equalsIgnoreCase("Expired")) {
                    matchesStatus = CategoryConstants.STATUS_EXPIRED.equals(item.getStatus());
                }
            }

            if (matchesQuery && matchesCategory && matchesStatus) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public List<FoodItem> getUseFirstItems() {
        List<FoodItem> active = getAllActive();
        List<FoodItem> useFirst = new ArrayList<>();
        for (FoodItem item : active) {
            if (item.isExpiringOrExpired()) {
                useFirst.add(item);
            }
        }

        Collections.sort(useFirst, new Comparator<FoodItem>() {
            @Override
            public int compare(FoodItem o1, FoodItem o2) {
                return Long.compare(o1.getDaysRemaining(), o2.getDaysRemaining());
            }
        });
        return useFirst;
    }

    public int getTotalActiveCount() {
        return getAllActive().size();
    }

    public int getFreshCount() {
        int count = 0;
        for (FoodItem item : getAllActive()) {
            if (CategoryConstants.STATUS_FRESH.equals(item.getStatus())) count++;
        }
        return count;
    }

    public int getExpiringSoonCount() {
        int count = 0;
        for (FoodItem item : getAllActive()) {
            if (CategoryConstants.STATUS_EXPIRING_SOON.equals(item.getStatus())) count++;
        }
        return count;
    }

    public int getExpiresTodayCount() {
        int count = 0;
        for (FoodItem item : getAllActive()) {
            if (CategoryConstants.STATUS_EXPIRES_TODAY.equals(item.getStatus())) count++;
        }
        return count;
    }

    public int getExpiredCount() {
        int count = 0;
        for (FoodItem item : getAllActive()) {
            if (CategoryConstants.STATUS_EXPIRED.equals(item.getStatus())) count++;
        }
        return count;
    }

    public boolean markConsumed(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_FOOD_IS_CONSUMED, 1);
        int rows = db.update(DatabaseHelper.TABLE_FOOD_ITEMS, cv,
                DatabaseHelper.COL_FOOD_ID + " = ?",
                new String[]{String.valueOf(id)});
        Log.d(TAG, "Food item id " + id + " marked consumed: " + (rows > 0));
        return rows > 0;
    }

    public boolean markWasted(long id, String reason, String notes) {
        FoodItem item = getById(id);
        if (item == null) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COL_FOOD_IS_WASTED, 1);
            db.update(DatabaseHelper.TABLE_FOOD_ITEMS, cv,
                    DatabaseHelper.COL_FOOD_ID + " = ?",
                    new String[]{String.valueOf(id)});

            WasteRecord record = new WasteRecord();
            record.setFoodName(item.getName());
            record.setQuantity(item.getQuantity());
            record.setUnit(item.getUnit());
            record.setWasteDate(DateUtils.getTodayString());
            record.setReason(reason != null && !reason.isEmpty() ? reason : "Past Expiry Date");
            record.setEstimatedCost(item.getEstimatedCost());
            record.setNotes(notes != null ? notes : item.getNotes());

            db.insert(DatabaseHelper.TABLE_WASTE_RECORDS, null, record.toContentValues());

            db.setTransactionSuccessful();
            Log.d(TAG, "Food item id " + id + " recorded as waste successfully");
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public static FoodItem cursorToFoodItem(Cursor cursor) {
        FoodItem item = new FoodItem();
        item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_ID)));
        item.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_NAME)));
        item.setQuantity(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_QUANTITY)));
        item.setUnit(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_UNIT)));
        item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_CATEGORY)));
        item.setPurchaseDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_PURCHASE_DATE)));
        item.setExpiryDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_EXPIRY_DATE)));
        item.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_LOCATION)));
        item.setEstimatedCost(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_COST)));
        item.setConsumed(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_IS_CONSUMED)) == 1);
        item.setWasted(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_IS_WASTED)) == 1);
        item.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_NOTES)));

        int fbIdx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_FIREBASE_ID);
        if (fbIdx != -1) {
            item.setFirebaseId(cursor.getString(fbIdx));
        }
        return item;
    }
}
