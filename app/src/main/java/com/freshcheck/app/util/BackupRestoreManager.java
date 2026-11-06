package com.freshcheck.app.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.database.ShoppingRepository;
import com.freshcheck.app.database.WasteRepository;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.ShoppingItem;
import com.freshcheck.app.model.WasteRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupRestoreManager {
    private static final String TAG = "BackupRestoreManager";

    public static File exportInventoryToCsv(Context context, FoodRepository repo) {
        List<FoodItem> items = repo.getAllActive();
        File file = new File(context.getCacheDir(), "FreshCheck_Inventory_" + System.currentTimeMillis() + ".csv");

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            writer.write("Name,Category,Quantity,Unit,PurchaseDate,ExpiryDate,Location,EstimatedCost,Status,Notes\n");
            for (FoodItem item : items) {
                writer.write(String.format("\"%s\",\"%s\",%.2f,\"%s\",\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\"\n",
                        escapeCsv(item.getName()),
                        escapeCsv(item.getCategory()),
                        item.getQuantity(),
                        escapeCsv(item.getUnit()),
                        escapeCsv(item.getPurchaseDate()),
                        escapeCsv(item.getExpiryDate()),
                        escapeCsv(item.getLocation()),
                        item.getEstimatedCost(),
                        item.getStatusLabel(),
                        escapeCsv(item.getNotes())
                ));
            }
            writer.flush();
            Log.d(TAG, "Exported " + items.size() + " items to CSV: " + file.getAbsolutePath());
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting inventory to CSV: " + e.getMessage());
            return null;
        }
    }

    public static File exportWasteReportToCsv(Context context, WasteRepository repo) {
        List<WasteRecord> records = repo.getAll();
        File file = new File(context.getCacheDir(), "FreshCheck_Waste_Report_" + System.currentTimeMillis() + ".csv");

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            writer.write("FoodName,Quantity,Unit,WasteDate,Reason,EstimatedCost,Notes\n");
            for (WasteRecord r : records) {
                writer.write(String.format("\"%s\",%.2f,\"%s\",\"%s\",\"%s\",%.2f,\"%s\"\n",
                        escapeCsv(r.getFoodName()),
                        r.getQuantity(),
                        escapeCsv(r.getUnit()),
                        escapeCsv(r.getWasteDate()),
                        escapeCsv(r.getReason()),
                        r.getEstimatedCost(),
                        escapeCsv(r.getNotes())
                ));
            }
            writer.flush();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting waste report to CSV: " + e.getMessage());
            return null;
        }
    }

    public static File createFullJsonBackup(Context context, FoodRepository foodRepo,
                                           ShoppingRepository shoppingRepo, WasteRepository wasteRepo) {
        File file = new File(context.getCacheDir(), "FreshCheck_Backup_" + System.currentTimeMillis() + ".json");
        Map<String, Object> backupData = new HashMap<>();
        backupData.put("version", 2);
        backupData.put("timestamp", System.currentTimeMillis());
        backupData.put("foodItems", foodRepo.getAllActive());
        backupData.put("shoppingItems", shoppingRepo.getAll());
        backupData.put("wasteRecords", wasteRepo.getAll());

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(backupData, writer);
            writer.flush();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON backup: " + e.getMessage());
            return null;
        }
    }

    public static void shareFile(Context context, File file, String mimeType, String title) {
        if (file == null || !file.exists()) return;
        try {
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, title));
        } catch (Exception e) {
            // Fallback
            Intent fallback = new Intent(Intent.ACTION_SEND);
            fallback.setType("text/plain");
            fallback.putExtra(Intent.EXTRA_TEXT, "FreshCheck Data Export generated on " + DateUtils.getTodayString());
            context.startActivity(Intent.createChooser(fallback, title));
        }
    }

    private static String escapeCsv(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }
}