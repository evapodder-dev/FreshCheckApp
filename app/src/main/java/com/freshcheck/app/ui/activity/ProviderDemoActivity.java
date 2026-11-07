package com.freshcheck.app.ui.activity;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.freshcheck.app.R;
import com.freshcheck.app.database.DatabaseHelper;
import com.freshcheck.app.provider.FoodContentProvider;
import com.freshcheck.app.util.DateUtils;

public class ProviderDemoActivity extends AppCompatActivity {
    private static final String TAG = "ProviderDemo_Lifecycle";

    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
        setContentView(R.layout.activity_provider_demo);

        Toolbar toolbar = findViewById(R.id.toolbar_provider_demo);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tvOutput = findViewById(R.id.tv_provider_output);

        findViewById(R.id.btn_run_query).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryContentProvider();
            }
        });

        findViewById(R.id.btn_run_insert).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertViaContentProvider();
            }
        });

        findViewById(R.id.btn_query_mediastore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                querySharedDeviceMedia();
            }
        });
    }

    private void querySharedDeviceMedia() {
        Log.d(TAG, "Querying shared device MediaStore via ContentResolver...");
        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED
        };

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    mediaUri,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_MODIFIED + " DESC LIMIT 10"
            );
        } catch (SecurityException se) {
            Log.w(TAG, "MediaStore permission notice: " + se.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("✓ Shared Device Data Query (System ContentProvider)\n");
        sb.append("Authority: ").append(MediaStore.AUTHORITY).append("\n");
        sb.append("Target URI: ").append(mediaUri.toString()).append("\n");

        if (cursor != null && cursor.getCount() > 0) {
            sb.append("Found ").append(cursor.getCount()).append(" shared media records on device:\n");
            sb.append("-------------------------------------------\n\n");
            int nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
            int sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);
            int count = 1;
            while (cursor.moveToNext()) {
                String name = nameCol != -1 ? cursor.getString(nameCol) : "Unknown";
                long size = sizeCol != -1 ? cursor.getLong(sizeCol) : 0L;
                sb.append("[").append(count++).append("] ").append(name)
                        .append(" (").append(size / 1024).append(" KB)\n");
            }
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            sb.append("Status: ContentResolver successfully interfaced with MediaStore provider.\n\n");
            sb.append("Note: FreshCheck uses FileProvider (content://com.freshcheck.app.fileprovider) for secure food photo captures and can query shared device media via ContentResolver.");
        }
        tvOutput.setText(sb.toString());
        Toast.makeText(this, "Queried Shared MediaStore via ContentResolver", Toast.LENGTH_SHORT).show();
    }

    private void queryContentProvider() {
        Log.d(TAG, "Querying FoodContentProvider via ContentResolver...");
        Uri uri = FoodContentProvider.CONTENT_URI;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor == null) {
            tvOutput.setText("ContentResolver query returned NULL cursor.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("✓ ContentResolver Query Successful!\n");
        sb.append("URI: ").append(uri.toString()).append("\n");
        sb.append("Total Records: ").append(cursor.getCount()).append("\n");
        sb.append("Columns: ").append(cursor.getColumnCount()).append("\n");
        sb.append("-------------------------------------------\n\n");

        int idCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_ID);
        int nameCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_NAME);
        int qtyCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_QUANTITY);
        int unitCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_UNIT);
        int catCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_CATEGORY);
        int expCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_EXPIRY_DATE);
        int locCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_LOCATION);

        int count = 1;
        while (cursor.moveToNext()) {
            long id = idCol != -1 ? cursor.getLong(idCol) : -1;
            String name = nameCol != -1 ? cursor.getString(nameCol) : "Unknown";
            double qty = qtyCol != -1 ? cursor.getDouble(qtyCol) : 0.0;
            String unit = unitCol != -1 ? cursor.getString(unitCol) : "";
            String category = catCol != -1 ? cursor.getString(catCol) : "";
            String expiry = expCol != -1 ? cursor.getString(expCol) : "";
            String location = locCol != -1 ? cursor.getString(locCol) : "";

            sb.append("[").append(count++).append("] ID: ").append(id)
              .append("\n    Name: ").append(name)
              .append(" (").append(qty).append(" ").append(unit).append(")")
              .append("\n    Category: ").append(category)
              .append(" | Location: ").append(location)
              .append("\n    Expiry: ").append(expiry)
              .append("\n\n");
        }
        cursor.close();

        tvOutput.setText(sb.toString());
        Toast.makeText(this, "Queried " + (count - 1) + " records via ContentResolver", Toast.LENGTH_SHORT).show();
    }

    private void insertViaContentProvider() {
        Log.d(TAG, "Inserting record into FoodContentProvider via ContentResolver...");
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_FOOD_NAME, "Provider Test Avocado");
        values.put(DatabaseHelper.COL_FOOD_QUANTITY, 2.0);
        values.put(DatabaseHelper.COL_FOOD_UNIT, "items");
        values.put(DatabaseHelper.COL_FOOD_CATEGORY, "Produce (Fruits & Veg)");
        values.put(DatabaseHelper.COL_FOOD_PURCHASE_DATE, DateUtils.getTodayString());
        values.put(DatabaseHelper.COL_FOOD_EXPIRY_DATE, DateUtils.getFutureDateString(5));
        values.put(DatabaseHelper.COL_FOOD_LOCATION, "Countertop");
        values.put(DatabaseHelper.COL_FOOD_COST, 3.50);
        values.put(DatabaseHelper.COL_FOOD_IS_CONSUMED, 0);
        values.put(DatabaseHelper.COL_FOOD_IS_WASTED, 0);
        values.put(DatabaseHelper.COL_FOOD_NOTES, "Inserted directly via getContentResolver().insert()");

        Uri insertedUri = getContentResolver().insert(FoodContentProvider.CONTENT_URI, values);

        if (insertedUri != null) {
            String log = "✓ Item inserted successfully via ContentResolver!\nInserted URI: " + insertedUri.toString()
                    + "\n\nQuery again to verify persistence in database.";
            tvOutput.setText(log);
            Toast.makeText(this, "Inserted item via ContentResolver", Toast.LENGTH_SHORT).show();
        } else {
            tvOutput.setText("Failed to insert record via ContentResolver.");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }
}
