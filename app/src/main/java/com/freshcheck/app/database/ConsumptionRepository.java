package com.freshcheck.app.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.freshcheck.app.model.ConsumptionRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsumptionRepository {
    private final DatabaseHelper dbHelper;

    public ConsumptionRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public List<ConsumptionRecord> getAll(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ConsumptionRecord> list = new ArrayList<>();
        String orderBy = DatabaseHelper.COL_CONSUMPTION_ID + " DESC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_CONSUMPTION_RECORDS, null,
                null, null, null, null, orderBy, String.valueOf(limit));

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToConsumption(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public int getTotalConsumedCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_CONSUMPTION_RECORDS, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public List<Map.Entry<String, Integer>> getFrequentlyConsumed(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, Integer> freqMap = new HashMap<>();
        String query = "SELECT " + DatabaseHelper.COL_CONSUMPTION_FOOD_NAME + ", COUNT(*) as cnt "
                + "FROM " + DatabaseHelper.TABLE_CONSUMPTION_RECORDS
                + " GROUP BY " + DatabaseHelper.COL_CONSUMPTION_FOOD_NAME
                + " ORDER BY cnt DESC LIMIT " + limit;

        Cursor cursor = db.rawQuery(query, null);
        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                int count = cursor.getInt(1);
                freqMap.put(name, count);
            }
            cursor.close();
            result.addAll(freqMap.entrySet());
        }
        return result;
    }

    public Map<String, Integer> getConsumptionByCategory() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, Integer> map = new HashMap<>();
        String query = "SELECT " + DatabaseHelper.COL_CONSUMPTION_CATEGORY + ", COUNT(*) "
                + "FROM " + DatabaseHelper.TABLE_CONSUMPTION_RECORDS
                + " GROUP BY " + DatabaseHelper.COL_CONSUMPTION_CATEGORY;

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String cat = cursor.getString(0);
                if (cat == null || cat.isEmpty()) cat = "Others";
                int count = cursor.getInt(1);
                map.put(cat, count);
            }
            cursor.close();
        }
        return map;
    }

    private ConsumptionRecord cursorToConsumption(Cursor c) {
        return new ConsumptionRecord(
                c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONSUMPTION_ID)),
                c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONSUMPTION_FOOD_ID)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONSUMPTION_FOOD_NAME)),
                c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONSUMPTION_QUANTITY)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONSUMPTION_UNIT)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONSUMPTION_DATE)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONSUMPTION_CATEGORY)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONSUMPTION_HOUSEHOLD_ID))
        );
    }
}