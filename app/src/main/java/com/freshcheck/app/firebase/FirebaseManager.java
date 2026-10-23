package com.freshcheck.app.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.model.FoodItem;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Firebase Realtime Database cloud synchronization.
 * Supports offline-first architecture: if Firebase is not yet
 * configured with google-services.json, operations fail gracefully
 * with descriptive status messages without crashing the application.
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final String NODE_INVENTORY = "inventory";
    private static FirebaseManager instance;

    public interface SyncCallback {
        void onProgress(String message);
        void onSuccess(String message, int itemsSynced);
        void onError(String errorMessage);
    }

    private final Context context;
    private FirebaseDatabase database;
    private DatabaseReference inventoryRef;
    private boolean isAvailable = false;
    private final Handler mainHandler;

    private FirebaseManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initFirebase();
    }

    public static synchronized FirebaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseManager(context);
        }
        return instance;
    }

    private void initFirebase() {
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) {
                database = FirebaseDatabase.getInstance();
                database.setPersistenceEnabled(true);
                inventoryRef = database.getReference(NODE_INVENTORY);
                isAvailable = true;
                Log.d(TAG, "Firebase Realtime Database initialized successfully");
            } else {
                Log.i(TAG, "FirebaseApp not initialized. Operating in local-only offline mode.");
                isAvailable = false;
            }
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization notice: " + e.getMessage());
            isAvailable = false;
        }
    }

    public boolean isConfigured() {
        return isAvailable && inventoryRef != null;
    }

    public void uploadInventory(final List<FoodItem> items, final SyncCallback callback) {
        if (callback != null) callback.onProgress("Checking Firebase connection...");

        if (!isConfigured()) {
            postError(callback, "Firebase is in Local / Offline mode. Add google-services.json to your project to enable live cloud sync.");
            return;
        }

        if (items == null || items.isEmpty()) {
            postSuccess(callback, "Inventory is empty, nothing to upload.", 0);
            return;
        }

        if (callback != null) callback.onProgress("Uploading " + items.size() + " items to Firebase...");

        Map<String, Object> batchUpload = new HashMap<>();
        for (FoodItem item : items) {
            String key = item.getFirebaseId();
            if (key == null || key.isEmpty()) {
                key = inventoryRef.push().getKey();
                item.setFirebaseId(key);
            }
            batchUpload.put(key, item.toFirebaseMap());
        }

        inventoryRef.updateChildren(batchUpload, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    postError(callback, "Upload failed: " + databaseError.getMessage());
                } else {
                    // Update local items with newly generated Firebase IDs
                    FoodRepository repo = new FoodRepository(context);
                    for (FoodItem item : items) {
                        repo.update(item);
                    }
                    postSuccess(callback, "Successfully uploaded " + items.size() + " items to cloud.", items.size());
                }
            }
        });
    }

    public void downloadInventory(final SyncCallback callback) {
        if (callback != null) callback.onProgress("Connecting to Firebase Realtime Database...");

        if (!isConfigured()) {
            postError(callback, "Firebase is in Local / Offline mode. Add google-services.json to your project to enable cloud sync.");
            return;
        }

        inventoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    postSuccess(callback, "No remote inventory found on Firebase.", 0);
                    return;
                }

                FoodRepository repo = new FoodRepository(context);
                int count = 0;

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    try {
                        String key = itemSnapshot.getKey();
                        String name = itemSnapshot.child("name").getValue(String.class);
                        if (name == null) continue;

                        Double qty = itemSnapshot.child("quantity").getValue(Double.class);
                        String unit = itemSnapshot.child("unit").getValue(String.class);
                        String cat = itemSnapshot.child("category").getValue(String.class);
                        String purchaseDate = itemSnapshot.child("purchaseDate").getValue(String.class);
                        String expiryDate = itemSnapshot.child("expiryDate").getValue(String.class);
                        String location = itemSnapshot.child("location").getValue(String.class);
                        Double cost = itemSnapshot.child("estimatedCost").getValue(Double.class);
                        String notes = itemSnapshot.child("notes").getValue(String.class);

                        FoodItem item = new FoodItem();
                        item.setFirebaseId(key);
                        item.setName(name);
                        item.setQuantity(qty != null ? qty : 1.0);
                        item.setUnit(unit != null ? unit : "items");
                        item.setCategory(cat != null ? cat : "Other");
                        item.setPurchaseDate(purchaseDate != null ? purchaseDate : "");
                        item.setExpiryDate(expiryDate != null ? expiryDate : "");
                        item.setLocation(location != null ? location : "Refrigerator");
                        item.setEstimatedCost(cost != null ? cost : 0.0);
                        item.setNotes(notes != null ? notes : "");

                        repo.insert(item);
                        count++;
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing item snapshot: " + e.getMessage());
                    }
                }

                postSuccess(callback, "Downloaded and synced " + count + " items from Firebase.", count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                postError(callback, "Failed to download from cloud: " + error.getMessage());
            }
        });
    }

    private void postSuccess(final SyncCallback callback, final String message, final int count) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onSuccess(message, count);
            }
        });
    }

    private void postError(final SyncCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onError(message);
            }
        });
    }
}
