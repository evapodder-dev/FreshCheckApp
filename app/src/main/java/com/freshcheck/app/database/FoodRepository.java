package com.freshcheck.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.ConsumptionRecord;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.WasteRecord;
import com.freshcheck.app.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FoodRepository {
    private static final String TAG = "FoodRepository";

    public static final int SORT_EXPIRY_ASC = 0;
    public static final int SORT_EXPIRY_DESC = 1;
    public static final int SORT_NAME_ASC = 2;
    public static final int SORT_NAME_DESC = 3;
    public static final int SORT_RECENTLY_ADDED = 4;

    private final DatabaseHelper dbHelper;

    public FoodRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insert(FoodItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (item.getCreatedTimestamp() == 0) {
            item.setCreatedTimestamp(System.currentTimeMillis());
        }
        item.setUpdatedTimestamp(System.currentTimeMillis());

        ContentValues values = item.toContentValues();
        long id = db.insert(DatabaseHelper.TABLE_FOOD_ITEMS, null, values);
        item.setId(id);
        Log.d(TAG, "Inserted FoodItem with id: " + id);
        return id;
    }

    public int update(FoodItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        item.setUpdatedTimestamp(System.currentTimeMillis());
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
        return getAllActive(null);
    }

    public List<FoodItem> getAllActive(String householdId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<FoodItem> list = new ArrayList<>();
        String selection = DatabaseHelper.COL_FOOD_IS_CONSUMED + " = 0 AND "
                + DatabaseHelper.COL_FOOD_IS_WASTED + " = 0";
        String[] selectionArgs = null;

        if (householdId != null && !householdId.isEmpty()) {
            selection += " AND " + DatabaseHelper.COL_FOOD_HOUSEHOLD_ID + " = ?";
            selectionArgs = new String[]{householdId};
        } else if (householdId != null) {
            selection += " AND (" + DatabaseHelper.COL_FOOD_HOUSEHOLD_ID + " IS NULL OR "
                    + DatabaseHelper.COL_FOOD_HOUSEHOLD_ID + " = '' )";
        }

        String orderBy = DatabaseHelper.COL_FOOD_EXPIRY_DATE + " ASC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_FOOD_ITEMS, null,
                selection, selectionArgs, null, null, orderBy);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToFoodItem(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<FoodItem> getRecentlyAdded(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<FoodItem> list = new ArrayList<>();
        String selection = DatabaseHelper.COL_FOOD_IS_CONSUMED + " = 0 AND "
                + DatabaseHelper.COL_FOOD_IS_WASTED + " = 0";
        String orderBy = DatabaseHelper.COL_FOOD_ID + " DESC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_FOOD_ITEMS, null,
                selection, null, null, null, orderBy, String.valueOf(limit));

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToFoodItem(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<FoodItem> searchAndFilter(String query, String category, String statusFilter) {
        return searchAndFilter(query, category, statusFilter, "All", SORT_EXPIRY_ASC);
    }

    public List<FoodItem> searchAndFilter(String query, String category, String statusFilter,
                                         String locationFilter, int sortMode) {
        return searchAndFilter(query, category, statusFilter, locationFilter, sortMode, null);
    }

    public List<FoodItem> searchAndFilter(String query, String category, String statusFilter,
                                         String locationFilter, int sortMode, String householdId) {
        List<FoodItem> allActive = getAllActive(householdId);
        List<FoodItem> filtered = new ArrayList<>();

        String q = query != null ? query.trim().toLowerCase() : "";
        boolean filterCategory = category != null && !category.equalsIgnoreCase("All") && !category.trim().isEmpty();
        boolean filterStatus = statusFilter != null && !statusFilter.equalsIgnoreCase("All") && !statusFilter.trim().isEmpty();
        boolean filterLocation = locationFilter != null && !locationFilter.equalsIgnoreCase("All") && !locationFilter.trim().isEmpty();

        for (FoodItem item : allActive) {
            boolean matchesQuery = q.isEmpty()
                    || item.getName().toLowerCase().contains(q)
                    || item.getNotes().toLowerCase().contains(q)
                    || item.getLocation().toLowerCase().contains(q)
                    || item.getCategory().toLowerCase().contains(q);

            boolean matchesCategory = !filterCategory || item.getCategory().equalsIgnoreCase(category);

            boolean matchesLocation = !filterLocation || item.getLocation().equalsIgnoreCase(locationFilter);

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

            if (matchesQuery && matchesCategory && matchesStatus && matchesLocation) {
                filtered.add(item);
            }
        }

        // Apply Sorting
        switch (sortMode) {
            case SORT_EXPIRY_DESC:
                Collections.sort(filtered, new Comparator<FoodItem>() {
                    @Override
                    public int compare(FoodItem o1, FoodItem o2) {
                        return Long.compare(o2.getDaysRemaining(), o1.getDaysRemaining());
                    }
                });
                break;
            case SORT_NAME_ASC:
                Collections.sort(filtered, new Comparator<FoodItem>() {
                    @Override
                    public int compare(FoodItem o1, FoodItem o2) {
                        return o1.getName().compareToIgnoreCase(o2.getName());
                    }
                });
                break;
            case SORT_NAME_DESC:
                Collections.sort(filtered, new Comparator<FoodItem>() {
                    @Override
                    public int compare(FoodItem o1, FoodItem o2) {
                        return o2.getName().compareToIgnoreCase(o1.getName());
                    }
                });
                break;
            case SORT_RECENTLY_ADDED:
                Collections.sort(filtered, new Comparator<FoodItem>() {
                    @Override
                    public int compare(FoodItem o1, FoodItem o2) {
                        return Long.compare(o2.getId(), o1.getId());
                    }
                });
                break;
            case SORT_EXPIRY_ASC:
            default:
                Collections.sort(filtered, new Comparator<FoodItem>() {
                    @Override
                    public int compare(FoodItem o1, FoodItem o2) {
                        return Long.compare(o1.getDaysRemaining(), o2.getDaysRemaining());
                    }
                });
                break;
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
        FoodItem item = getById(id);
        if (item == null) return false;
        return consumePartial(id, item.getQuantity());
    }

    /**
     * Consumes partial or full quantity of an item.
     * Records an entry into consumption_records without losing item tracking.
     */
    public boolean consumePartial(long id, double consumedQuantity) {
        FoodItem item = getById(id);
        if (item == null || consumedQuantity <= 0) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            double remainingQty = item.getQuantity() - consumedQuantity;
            ContentValues cv = new ContentValues();
            if (remainingQty <= 0.001) {
                cv.put(DatabaseHelper.COL_FOOD_IS_CONSUMED, 1);
                cv.put(DatabaseHelper.COL_FOOD_QUANTITY, 0.0);
            } else {
                cv.put(DatabaseHelper.COL_FOOD_QUANTITY, remainingQty);
            }
            cv.put(DatabaseHelper.COL_FOOD_UPDATED_AT, System.currentTimeMillis());

            db.update(DatabaseHelper.TABLE_FOOD_ITEMS, cv,
                    DatabaseHelper.COL_FOOD_ID + " = ?",
                    new String[]{String.valueOf(id)});

            // Log consumption record
            ConsumptionRecord cr = new ConsumptionRecord(
                    0,
                    item.getId(),
                    item.getName(),
                    consumedQuantity,
                    item.getUnit(),
                    DateUtils.getTodayString(),
                    item.getCategory(),
                    item.getHouseholdId()
            );
            db.insert(DatabaseHelper.TABLE_CONSUMPTION_RECORDS, null, cr.toContentValues());

            db.setTransactionSuccessful();
            Log.d(TAG, "Item id " + id + " partially/fully consumed: " + consumedQuantity + " " + item.getUnit());
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean markWasted(long id, String reason, String notes) {
        FoodItem item = getById(id);
        if (item == null) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COL_FOOD_IS_WASTED, 1);
            cv.put(DatabaseHelper.COL_FOOD_UPDATED_AT, System.currentTimeMillis());
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
            record.setHouseholdId(item.getHouseholdId());

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
        int idx;

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_ID);
        if (idx != -1) item.setId(cursor.getLong(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_NAME);
        if (idx != -1) item.setName(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_QUANTITY);
        if (idx != -1) item.setQuantity(cursor.getDouble(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_UNIT);
        if (idx != -1) item.setUnit(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_CATEGORY);
        if (idx != -1) item.setCategory(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_PURCHASE_DATE);
        if (idx != -1) item.setPurchaseDate(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_EXPIRY_DATE);
        if (idx != -1) item.setExpiryDate(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_LOCATION);
        if (idx != -1) item.setLocation(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_COST);
        if (idx != -1) item.setEstimatedCost(cursor.getDouble(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_IS_CONSUMED);
        if (idx != -1) item.setConsumed(cursor.getInt(idx) == 1);

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_IS_WASTED);
        if (idx != -1) item.setWasted(cursor.getInt(idx) == 1);

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_NOTES);
        if (idx != -1) item.setNotes(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_FIREBASE_ID);
        if (idx != -1) item.setFirebaseId(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_IMAGE_URI);
        if (idx != -1) item.setImageUri(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_HOUSEHOLD_ID);
        if (idx != -1) item.setHouseholdId(cursor.getString(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_CREATED_AT);
        if (idx != -1) item.setCreatedTimestamp(cursor.getLong(idx));

        idx = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_UPDATED_AT);
        if (idx != -1) item.setUpdatedTimestamp(cursor.getLong(idx));

        return item;
    }
}