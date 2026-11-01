package com.freshcheck.app.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.model.FoodItem;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles dual Cloud Synchronization:
 * 1. Firebase Realtime Database
 * 2. Cloud Firestore
 *
 * Scopes data per authenticated user (UID) when signed in,
 * with offline-first local SQLite fallback.
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final String NODE_INVENTORY = "inventory";
    private static final String NODE_USERS = "users";

    private static FirebaseManager instance;

    public interface SyncCallback {
        void onProgress(String message);
        void onSuccess(String message, int itemsSynced);
        void onError(String errorMessage);
    }

    private final Context context;
    private FirebaseDatabase realtimeDatabase;
    private FirebaseFirestore firestore;
    private boolean isRealtimeReady = false;
    private boolean isFirestoreReady = false;
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
                // Initialize Realtime Database
                try {
                    realtimeDatabase = FirebaseDatabase.getInstance();
                    realtimeDatabase.setPersistenceEnabled(true);
                    isRealtimeReady = true;
                    Log.d(TAG, "Firebase Realtime Database initialized");
                } catch (Exception e) {
                    Log.w(TAG, "Realtime DB init warning: " + e.getMessage());
                    isRealtimeReady = (realtimeDatabase != null);
                }

                // Initialize Cloud Firestore
                try {
                    firestore = FirebaseFirestore.getInstance();
                    isFirestoreReady = true;
                    Log.d(TAG, "Cloud Firestore initialized");
                } catch (Exception e) {
                    Log.w(TAG, "Cloud Firestore init warning: " + e.getMessage());
                    isFirestoreReady = false;
                }
            } else {
                Log.i(TAG, "FirebaseApp not initialized. Operating in local-only offline mode.");
                isRealtimeReady = false;
                isFirestoreReady = false;
            }
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization notice: " + e.getMessage());
            isRealtimeReady = false;
            isFirestoreReady = false;
        }
    }

    public boolean isConfigured() {
        return isRealtimeReady || isFirestoreReady;
    }

    public boolean isRealtimeDatabaseAvailable() {
        return isRealtimeReady && realtimeDatabase != null;
    }

    public boolean isFirestoreAvailable() {
        return isFirestoreReady && firestore != null;
    }

    private DatabaseReference getUserRealtimeRef() {
        if (!isRealtimeDatabaseAvailable()) return null;
        FirebaseAuthManager auth = FirebaseAuthManager.getInstance(context);
        String uid = auth.getUserId();
        if (auth.isUserLoggedIn() && uid != null && !uid.isEmpty()) {
            return realtimeDatabase.getReference(NODE_USERS).child(uid).child(NODE_INVENTORY);
        }
        return realtimeDatabase.getReference(NODE_INVENTORY);
    }

    private CollectionReference getUserFirestoreCollection() {
        if (!isFirestoreAvailable()) return null;
        FirebaseAuthManager auth = FirebaseAuthManager.getInstance(context);
        String uid = auth.getUserId();
        if (auth.isUserLoggedIn() && uid != null && !uid.isEmpty()) {
            return firestore.collection(NODE_USERS).document(uid).collection(NODE_INVENTORY);
        }
        return firestore.collection(NODE_INVENTORY);
    }

    // Default upload delegates to Realtime Database
    public void uploadInventory(final List<FoodItem> items, final SyncCallback callback) {
        uploadToRealtimeDatabase(items, callback);
    }

    // Default download delegates to Realtime Database
    public void downloadInventory(final SyncCallback callback) {
        downloadFromRealtimeDatabase(callback);
    }

    // ==========================================
    // 1. FIREBASE REALTIME DATABASE METHODS
    // ==========================================

    public void uploadToRealtimeDatabase(final List<FoodItem> items, final SyncCallback callback) {
        if (callback != null) callback.onProgress("Connecting to Firebase Realtime Database...");

        final DatabaseReference ref = getUserRealtimeRef();
        if (ref == null) {
            postError(callback, "Realtime Database is in Offline / Local mode. Configure google-services.json to sync live.");
            return;
        }

        if (items == null || items.isEmpty()) {
            postSuccess(callback, "Inventory is empty, nothing to upload.", 0);
            return;
        }

        if (callback != null) callback.onProgress("Uploading " + items.size() + " items to Realtime Database...");

        Map<String, Object> batchUpload = new HashMap<>();
        for (FoodItem item : items) {
            String key = item.getFirebaseId();
            if (key == null || key.isEmpty()) {
                key = ref.push().getKey();
                item.setFirebaseId(key);
            }
            batchUpload.put(key, item.toFirebaseMap());
        }

        ref.updateChildren(batchUpload, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    postError(callback, "Realtime DB upload failed: " + databaseError.getMessage());
                } else {
                    FoodRepository repo = new FoodRepository(context);
                    for (FoodItem item : items) {
                        repo.update(item);
                    }
                    postSuccess(callback, "Uploaded " + items.size() + " items to Realtime Database.", items.size());
                }
            }
        });
    }

    public void downloadFromRealtimeDatabase(final SyncCallback callback) {
        if (callback != null) callback.onProgress("Connecting to Firebase Realtime Database...");

        final DatabaseReference ref = getUserRealtimeRef();
        if (ref == null) {
            postError(callback, "Realtime Database is in Offline / Local mode.");
            return;
        }

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    postSuccess(callback, "No remote inventory found on Realtime Database.", 0);
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
                        Log.e(TAG, "Error parsing RTDB item: " + e.getMessage());
                    }
                }

                postSuccess(callback, "Downloaded " + count + " items from Realtime Database.", count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                postError(callback, "Failed to download from Realtime DB: " + error.getMessage());
            }
        });
    }

    // ==========================================
    // 2. CLOUD FIRESTORE METHODS
    // ==========================================

    public void uploadToFirestore(final List<FoodItem> items, final SyncCallback callback) {
        if (callback != null) callback.onProgress("Connecting to Cloud Firestore...");

        final CollectionReference collection = getUserFirestoreCollection();
        if (collection == null) {
            postError(callback, "Cloud Firestore is in Offline / Local mode.");
            return;
        }

        if (items == null || items.isEmpty()) {
            postSuccess(callback, "Inventory is empty, nothing to upload.", 0);
            return;
        }

        if (callback != null) callback.onProgress("Uploading " + items.size() + " items to Cloud Firestore...");

        WriteBatch batch = firestore.batch();
        for (FoodItem item : items) {
            String docId = item.getFirebaseId();
            if (docId == null || docId.isEmpty()) {
                docId = collection.document().getId();
                item.setFirebaseId(docId);
            }
            batch.set(collection.document(docId), item.toFirebaseMap());
        }

        batch.commit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        FoodRepository repo = new FoodRepository(context);
                        for (FoodItem item : items) {
                            repo.update(item);
                        }
                        postSuccess(callback, "Uploaded " + items.size() + " items to Cloud Firestore.", items.size());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        postError(callback, "Cloud Firestore upload failed: " + e.getMessage());
                    }
                });
    }

    public void downloadFromFirestore(final SyncCallback callback) {
        if (callback != null) callback.onProgress("Connecting to Cloud Firestore...");

        final CollectionReference collection = getUserFirestoreCollection();
        if (collection == null) {
            postError(callback, "Cloud Firestore is in Offline / Local mode.");
            return;
        }

        collection.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshots) {
                        if (snapshots == null || snapshots.isEmpty()) {
                            postSuccess(callback, "No remote inventory found on Cloud Firestore.", 0);
                            return;
                        }

                        FoodRepository repo = new FoodRepository(context);
                        int count = 0;

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            try {
                                String key = doc.getId();
                                String name = doc.getString("name");
                                if (name == null) continue;

                                Double qty = doc.getDouble("quantity");
                                String unit = doc.getString("unit");
                                String cat = doc.getString("category");
                                String purchaseDate = doc.getString("purchaseDate");
                                String expiryDate = doc.getString("expiryDate");
                                String location = doc.getString("location");
                                Double cost = doc.getDouble("estimatedCost");
                                String notes = doc.getString("notes");

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
                                Log.e(TAG, "Error parsing Firestore doc: " + e.getMessage());
                            }
                        }

                        postSuccess(callback, "Downloaded " + count + " items from Cloud Firestore.", count);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        postError(callback, "Failed to download from Cloud Firestore: " + e.getMessage());
                    }
                });
    }

    public void syncAll(final List<FoodItem> items, final SyncCallback callback) {
        if (callback != null) callback.onProgress("Synchronizing with all Cloud Firebase services...");
        uploadToRealtimeDatabase(items, new SyncCallback() {
            @Override
            public void onProgress(String message) {
                if (callback != null) callback.onProgress(message);
            }

            @Override
            public void onSuccess(String message, int itemsSynced) {
                // Next sync to Firestore
                uploadToFirestore(items, new SyncCallback() {
                    @Override
                    public void onProgress(String msg) {
                        if (callback != null) callback.onProgress(msg);
                    }

                    @Override
                    public void onSuccess(String msg, int count) {
                        postSuccess(callback, "Synced " + count + " items to Realtime DB & Cloud Firestore!", count);
                    }

                    @Override
                    public void onError(String error) {
                        postSuccess(callback, "Realtime DB synced, but Firestore had: " + error, itemsSynced);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Try Firestore even if Realtime DB had error
                uploadToFirestore(items, callback);
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