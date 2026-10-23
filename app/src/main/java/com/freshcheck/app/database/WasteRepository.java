package com.freshcheck.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.freshcheck.app.model.WasteRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WasteRepository {
    private static final String TAG = "WasteRepository";
    private final DatabaseHelper dbHelper;

    public WasteRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insert(WasteRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = record.toContentValues();
        long id = db.insert(DatabaseHelper.TABLE_WASTE_RECORDS, null, values);
        record.setId(id);
        Log.d(TAG, "Inserted WasteRecord id: " + id);
        return id;
    }

    public int delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_WASTE_RECORDS,
                DatabaseHelper.COL_WASTE_ID + " = ?",
                new String[]{String.valueOf(id)});
        Log.d(TAG, "Deleted WasteRecord id: " + id);
        return rows;
    }

    public List<WasteRecord> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<WasteRecord> list = new ArrayList<>();
        String orderBy = DatabaseHelper.COL_WASTE_DATE + " DESC, " + DatabaseHelper.COL_WASTE_ID + " DESC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_WASTE_RECORDS, null,
                null, null, null, null, orderBy);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToWasteRecord(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public int getTotalItemsWasted() {
        return getAll().size();
    }

    public double getTotalFinancialLoss() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double total = 0.0;
        String query = "SELECT SUM(" + DatabaseHelper.COL_WASTE_COST + ") FROM " + DatabaseHelper.TABLE_WASTE_RECORDS;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
            cursor.close();
        }
        return total;
    }

    public Map<String, Float> getWasteCostByReason() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, Float> map = new HashMap<>();
        String query = "SELECT " + DatabaseHelper.COL_WASTE_REASON + ", SUM(" + DatabaseHelper.COL_WASTE_COST + ") "
                + "FROM " + DatabaseHelper.TABLE_WASTE_RECORDS + " GROUP BY " + DatabaseHelper.COL_WASTE_REASON;

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String reason = cursor.getString(0);
                float cost = cursor.getFloat(1);
                map.put(reason, cost);
            }
            cursor.close();
        }
        return map;
    }

    public Map<String, Integer> getWasteCountByReason() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, Integer> map = new HashMap<>();
        String query = "SELECT " + DatabaseHelper.COL_WASTE_REASON + ", COUNT(*) "
                + "FROM " + DatabaseHelper.TABLE_WASTE_RECORDS + " GROUP BY " + DatabaseHelper.COL_WASTE_REASON;

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String reason = cursor.getString(0);
                int count = cursor.getInt(1);
                map.put(reason, count);
            }
            cursor.close();
        }
        return map;
    }

    public static WasteRecord cursorToWasteRecord(Cursor cursor) {
        WasteRecord record = new WasteRecord();
        record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WASTE_ID)));
        record.setFoodName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WASTE_FOOD_NAME)));
        record.setQuantity(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WASTE_QUANTITY)));
        record.setUnit(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WASTE_UNIT)));
        record.setWasteDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WASTE_DATE)));
        record.setReason(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WASTE_REASON)));
        record.setEstimatedCost(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WASTE_COST)));
        record.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WASTE_NOTES)));
        return record;
    }
}
