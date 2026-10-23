package com.freshcheck.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.ShoppingItem;
import com.freshcheck.app.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class ShoppingRepository {
    private static final String TAG = "ShoppingRepository";
    private final DatabaseHelper dbHelper;
    private final Context context;

    public ShoppingRepository(Context context) {
        this.context = context;
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insert(ShoppingItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = item.toContentValues();
        long id = db.insert(DatabaseHelper.TABLE_SHOPPING_ITEMS, null, values);
        item.setId(id);
        Log.d(TAG, "Inserted ShoppingItem with id: " + id);
        return id;
    }

    public int update(ShoppingItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = item.toContentValues();
        int rows = db.update(DatabaseHelper.TABLE_SHOPPING_ITEMS, values,
                DatabaseHelper.COL_SHOPPING_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
        Log.d(TAG, "Updated ShoppingItem id " + item.getId());
        return rows;
    }

    public int delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_SHOPPING_ITEMS,
                DatabaseHelper.COL_SHOPPING_ID + " = ?",
                new String[]{String.valueOf(id)});
        Log.d(TAG, "Deleted ShoppingItem id " + id);
        return rows;
    }

    public List<ShoppingItem> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ShoppingItem> list = new ArrayList<>();
        String orderBy = DatabaseHelper.COL_SHOPPING_IS_PURCHASED + " ASC, "
                + DatabaseHelper.COL_SHOPPING_ID + " DESC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_SHOPPING_ITEMS, null,
                null, null, null, null, orderBy);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToShoppingItem(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public boolean togglePurchased(long id, boolean isPurchased) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_SHOPPING_IS_PURCHASED, isPurchased ? 1 : 0);
        int rows = db.update(DatabaseHelper.TABLE_SHOPPING_ITEMS, cv,
                DatabaseHelper.COL_SHOPPING_ID + " = ?",
                new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public int clearPurchased() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_SHOPPING_ITEMS,
                DatabaseHelper.COL_SHOPPING_IS_PURCHASED + " = 1", null);
        Log.d(TAG, "Cleared " + rows + " purchased shopping items");
        return rows;
    }

    public boolean moveToInventory(long shoppingItemId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SHOPPING_ITEMS, null,
                DatabaseHelper.COL_SHOPPING_ID + " = ?",
                new String[]{String.valueOf(shoppingItemId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            ShoppingItem shoppingItem = cursorToShoppingItem(cursor);
            cursor.close();

            FoodItem foodItem = new FoodItem();
            foodItem.setName(shoppingItem.getName());
            foodItem.setQuantity(shoppingItem.getQuantity());
            foodItem.setUnit(shoppingItem.getUnit());
            foodItem.setCategory(shoppingItem.getCategory());
            foodItem.setPurchaseDate(DateUtils.getTodayString());
            foodItem.setExpiryDate(DateUtils.getFutureDateString(7)); // Default 1 week shelf life
            foodItem.setLocation("Refrigerator");
            foodItem.setNotes(shoppingItem.getNotes());

            FoodRepository foodRepository = new FoodRepository(context);
            long newId = foodRepository.insert(foodItem);

            // Remove from shopping list once moved
            delete(shoppingItemId);
            return newId > 0;
        }

        if (cursor != null) cursor.close();
        return false;
    }

    public String getFormattedShareText() {
        List<ShoppingItem> items = getAll();
        if (items.isEmpty()) return "FreshCheck Grocery List is empty.";

        StringBuilder sb = new StringBuilder("🛒 FreshCheck Grocery Shopping List:\n\n");
        for (ShoppingItem item : items) {
            String status = item.isPurchased() ? "[✓] " : "[ ] ";
            sb.append(status)
              .append(item.getName())
              .append(" - ")
              .append(item.getQuantity())
              .append(" ")
              .append(item.getUnit());
            if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                sb.append(" (").append(item.getNotes()).append(")");
            }
            sb.append("\n");
        }
        sb.append("\nOrganized with FreshCheck App");
        return sb.toString();
    }

    public static ShoppingItem cursorToShoppingItem(Cursor cursor) {
        ShoppingItem item = new ShoppingItem();
        item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SHOPPING_ID)));
        item.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SHOPPING_NAME)));
        item.setQuantity(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SHOPPING_QUANTITY)));
        item.setUnit(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SHOPPING_UNIT)));
        item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SHOPPING_CATEGORY)));
        item.setPurchased(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SHOPPING_IS_PURCHASED)) == 1);
        item.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SHOPPING_NOTES)));
        return item;
    }
}
