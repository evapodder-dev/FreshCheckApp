package com.freshcheck.app.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freshcheck.app.database.DatabaseHelper;

/**
 * Custom ContentProvider exposing FreshCheck food inventory data
 * to ContentResolvers, external consumers, and demo activities.
 */
public class FoodContentProvider extends ContentProvider {
    private static final String TAG = "FoodContentProvider";

    public static final String AUTHORITY = "com.freshcheck.app.provider";
    public static final String PATH_FOOD_ITEMS = "food_items";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_FOOD_ITEMS);

    private static final int CODE_FOOD_ITEMS = 100;
    private static final int CODE_FOOD_ITEM_ID = 101;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, PATH_FOOD_ITEMS, CODE_FOOD_ITEMS);
        uriMatcher.addURI(AUTHORITY, PATH_FOOD_ITEMS + "/#", CODE_FOOD_ITEM_ID);
    }

    private DatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "FoodContentProvider initialized");
        dbHelper = DatabaseHelper.getInstance(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        int match = uriMatcher.match(uri);
        switch (match) {
            case CODE_FOOD_ITEMS:
                cursor = db.query(DatabaseHelper.TABLE_FOOD_ITEMS, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case CODE_FOOD_ITEM_ID:
                long id = ContentUris.parseId(uri);
                String idSelection = DatabaseHelper.COL_FOOD_ID + " = ?";
                String[] idArgs = new String[]{String.valueOf(id)};
                cursor = db.query(DatabaseHelper.TABLE_FOOD_ITEMS, projection,
                        idSelection, idArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CODE_FOOD_ITEMS:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + PATH_FOOD_ITEMS;
            case CODE_FOOD_ITEM_ID:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + PATH_FOOD_ITEMS;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != CODE_FOOD_ITEMS) {
            throw new IllegalArgumentException("Unsupported URI for insert: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(DatabaseHelper.TABLE_FOOD_ITEMS, null, values);

        if (id > 0) {
            Uri itemUri = ContentUris.withAppendedId(CONTENT_URI, id);
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(itemUri, null);
            }
            return itemUri;
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted;

        switch (uriMatcher.match(uri)) {
            case CODE_FOOD_ITEMS:
                rowsDeleted = db.delete(DatabaseHelper.TABLE_FOOD_ITEMS, selection, selectionArgs);
                break;
            case CODE_FOOD_ITEM_ID:
                long id = ContentUris.parseId(uri);
                rowsDeleted = db.delete(DatabaseHelper.TABLE_FOOD_ITEMS,
                        DatabaseHelper.COL_FOOD_ID + " = ?",
                        new String[]{String.valueOf(id)});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsDeleted > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated;

        switch (uriMatcher.match(uri)) {
            case CODE_FOOD_ITEMS:
                rowsUpdated = db.update(DatabaseHelper.TABLE_FOOD_ITEMS, values, selection, selectionArgs);
                break;
            case CODE_FOOD_ITEM_ID:
                long id = ContentUris.parseId(uri);
                rowsUpdated = db.update(DatabaseHelper.TABLE_FOOD_ITEMS, values,
                        DatabaseHelper.COL_FOOD_ID + " = ?",
                        new String[]{String.valueOf(id)});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsUpdated > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}
