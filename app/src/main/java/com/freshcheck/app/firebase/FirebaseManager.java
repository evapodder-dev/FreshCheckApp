package com.freshcheck.app.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.database.HouseholdRepository;
import com.freshcheck.app.database.ShoppingRepository;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.HouseholdGroup;
import com.freshcheck.app.model.HouseholdMember;
import com.freshcheck.app.model.ShoppingItem;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Senior Architecture Firebase Cloud & Synchronization Engine:
 * - Realtime Database: Real-time mess collaboration, shared grocery list live listeners, .info/connected monitoring.
 * - Cloud Firestore: Scalable pantry inventory persistence, multi-user mess membership, and activity logs.
 * - Cross-Device Synchronization: Automatic propagation of grocery additions, purchases, deletions, and pantry updates.
 * - Dynamic Status Monitoring: CONNECTED, CONNECTING, OFFLINE, SYNCING, SYNC_FAILED.
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    // Storage / database nodes
    public static final String NODE_USERS = "users";
    public static final String NODE_INVENTORY = "inventory";
    public static final String NODE_MESSES = "messes";
    public static final String NODE_GROCERIES = "groceries";
    public static final String NODE_PANTRY = "pantry";
    public static final String NODE_MEMBERS = "members";
    public static final String NODE_INVITE_CODES = "invite_codes";
    public static final String NODE_ACTIVITIES = "activities";

    private static final String PREF_SYNC = "freshcheck_cloud_sync_prefs";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_timestamp";
    private static final String KEY_PENDING_UPLOADS = "pending_uploads_count";
    private static final String KEY_LAST_SYNC_ERROR = "last_sync_error";

    public enum ConnectionState {
        CONNECTED("Connected", "#3D946B"),
        CONNECTING("Connecting...", "#E6A23C"),
        OFFLINE("Offline", "#78847B"),
        SYNCING("Syncing...", "#1E88E5"),
        SYNC_FAILED("Sync Failed", "#D9534F");

        private final String label;
        private final String hexColor;

        ConnectionState(String label, String hexColor) {
            this.label = label;
            this.hexColor = hexColor;
        }

        public String getLabel() { return label; }
        public String getHexColor() { return hexColor; }
    }

    public interface SyncCallback {
        void onProgress(String message);
        void onSuccess(String message, int itemsSynced);
        void onError(String errorMessage);
    }

    public interface ConnectionListener {
        void onConnectionStateChanged(ConnectionState rtdbState, ConnectionState firestoreState, ConnectionState overallState);
    }

    public interface CloudMessCallback {
        void onSuccess(HouseholdGroup group, String inviteCode);
        void onError(String errorMessage);
    }

    public interface CloudJoinCallback {
        void onSuccess(HouseholdGroup group);
        void onError(String errorMessage);
    }

    public interface OnSharedGroceryChangeListener {
        void onGroceryChanged();
    }

    private static FirebaseManager instance;
    private final Context context;
    private final Handler mainHandler;

    private FirebaseDatabase realtimeDatabase;
    private FirebaseFirestore firestore;

    private ConnectionState rtdbState = ConnectionState.OFFLINE;
    private ConnectionState firestoreState = ConnectionState.OFFLINE;
    private final List<ConnectionListener> connectionListeners = new ArrayList<>();

    private ValueEventListener rtdbConnectedListener;
    private DatabaseReference groceriesRef;
    private ChildEventListener groceriesChildListener;
    private ListenerRegistration firestorePantryListener;
    private OnSharedGroceryChangeListener sharedGroceryChangeListener;

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

    public synchronized void initFirebase() {
        try {
            FirebaseConfigHelper.initialize(context);

            if (!FirebaseApp.getApps(context).isEmpty()) {
                // Initialize Realtime Database
                try {
                    String databaseUrl = FirebaseConfigHelper.getDatabaseUrl(context);
                    if (databaseUrl != null && !databaseUrl.isEmpty()) {
                        realtimeDatabase = FirebaseDatabase.getInstance(databaseUrl);
                    } else {
                        realtimeDatabase = FirebaseDatabase.getInstance();
                    }
                    setupRtdbConnectionMonitor();
                    Log.i(TAG, "Firebase Realtime Database initialized: " + realtimeDatabase.getReference().toString());
                } catch (Exception e) {
                    Log.w(TAG, "Realtime DB initialization warning: " + e.getMessage());
                    rtdbState = ConnectionState.OFFLINE;
                }

                // Initialize Cloud Firestore
                try {
                    firestore = FirebaseFirestore.getInstance();
                    Log.i(TAG, "Firebase Cloud Firestore initialized successfully");
                    // Perform initial ping
                    pingFirestore(null);
                } catch (Exception e) {
                    Log.w(TAG, "Cloud Firestore initialization warning: " + e.getMessage());
                    firestoreState = ConnectionState.OFFLINE;
                }
            } else {
                Log.w(TAG, "FirebaseApp not initialized. Operating in local-only offline mode.");
                rtdbState = ConnectionState.OFFLINE;
                firestoreState = ConnectionState.OFFLINE;
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal error initializing FirebaseManager: " + e.getMessage(), e);
            rtdbState = ConnectionState.OFFLINE;
            firestoreState = ConnectionState.OFFLINE;
        }
        notifyConnectionStateChanged();
    }

    // ==========================================
    // 1. CONNECTION MONITORING (Requirement 7)
    // ==========================================

    private void setupRtdbConnectionMonitor() {
        if (realtimeDatabase == null) return;

        try {
            DatabaseReference connectedRef = realtimeDatabase.getReference(".info/connected");
            if (rtdbConnectedListener != null) {
                connectedRef.removeEventListener(rtdbConnectedListener);
            }

            rtdbConnectedListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    rtdbState = connected ? ConnectionState.CONNECTED : ConnectionState.CONNECTING;
                    Log.d(TAG, "RTDB connection state changed: " + rtdbState.getLabel());
                    notifyConnectionStateChanged();

                    if (connected) {
                        // Connection restored: sync pending changes automatically
                        syncPendingChanges();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "RTDB connection monitor cancelled: " + error.getMessage());
                    rtdbState = ConnectionState.SYNC_FAILED;
                    saveLastSyncError("RTDB Error: " + error.getMessage());
                    notifyConnectionStateChanged();
                }
            };
            connectedRef.addValueEventListener(rtdbConnectedListener);
        } catch (Exception e) {
            Log.w(TAG, "Failed to attach .info/connected listener: " + e.getMessage());
        }
    }

    public void pingFirestore(@Nullable final SyncCallback callback) {
        if (firestore == null) {
            firestoreState = ConnectionState.OFFLINE;
            notifyConnectionStateChanged();
            if (callback != null) callback.onError("Firestore is not initialized.");
            return;
        }

        firestoreState = ConnectionState.CONNECTING;
        notifyConnectionStateChanged();

        // Query server directly to verify real network connection
        firestore.collection("health_check").document("ping")
                .get(Source.SERVER)
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        firestoreState = ConnectionState.CONNECTED;
                        notifyConnectionStateChanged();
                        if (callback != null) callback.onSuccess("Cloud Firestore connected & verified live.", 1);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // If error is permission-denied, server IS reachable and rules rejected, meaning connected!
                        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                        if (msg.contains("permission-denied") || msg.contains("not-found")) {
                            firestoreState = ConnectionState.CONNECTED;
                            notifyConnectionStateChanged();
                            if (callback != null) callback.onSuccess("Cloud Firestore connected (Security Rules Active)", 1);
                        } else {
                            firestoreState = ConnectionState.OFFLINE;
                            saveLastSyncError("Firestore: " + e.getMessage());
                            notifyConnectionStateChanged();
                            if (callback != null) callback.onError("Firestore unreachable: " + e.getMessage());
                        }
                    }
                });
    }

    public ConnectionState getRtdbState() { return rtdbState; }
    public ConnectionState getFirestoreState() { return firestoreState; }

    public ConnectionState getOverallCloudState() {
        if (rtdbState == ConnectionState.CONNECTED && firestoreState == ConnectionState.CONNECTED) {
            return ConnectionState.CONNECTED;
        }
        if (rtdbState == ConnectionState.CONNECTED || firestoreState == ConnectionState.CONNECTED) {
            return ConnectionState.CONNECTED;
        }
        if (rtdbState == ConnectionState.SYNCING || firestoreState == ConnectionState.SYNCING) {
            return ConnectionState.SYNCING;
        }
        if (rtdbState == ConnectionState.CONNECTING || firestoreState == ConnectionState.CONNECTING) {
            return ConnectionState.CONNECTING;
        }
        if (rtdbState == ConnectionState.SYNC_FAILED || firestoreState == ConnectionState.SYNC_FAILED) {
            return ConnectionState.SYNC_FAILED;
        }
        return ConnectionState.OFFLINE;
    }

    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null && !connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
            listener.onConnectionStateChanged(rtdbState, firestoreState, getOverallCloudState());
        }
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    private void notifyConnectionStateChanged() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                ConnectionState overall = getOverallCloudState();
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnectionStateChanged(rtdbState, firestoreState, overall);
                }
            }
        });
    }

    public boolean isConfigured() {
        return (realtimeDatabase != null || firestore != null);
    }

    public boolean isRealtimeDatabaseAvailable() {
        return realtimeDatabase != null;
    }

    public boolean isFirestoreAvailable() {
        return firestore != null;
    }

    // ==========================================
    // 2. MESS & INVITE CODE CLOUD SYNC (Req 5)
    // ==========================================

    /**
     * Stores newly created mess in Realtime Database and Cloud Firestore.
     * Generates cloud invite code mapping for instant cross-device joining.
     */
    public void createMessInCloud(final HouseholdGroup group, final String adminUid,
                                  final String adminName, final CloudMessCallback callback) {
        if (group == null || group.getHouseholdId() == null) {
            if (callback != null) callback.onError("Invalid mess parameters.");
            return;
        }

        final String messId = group.getHouseholdId();
        final String inviteCode = group.getInviteCode().trim().toUpperCase();

        Log.i(TAG, "Creating mess in Firebase Cloud: " + messId + " (Code: " + inviteCode + ")");

        Map<String, Object> messData = new HashMap<>();
        messData.put("householdId", messId);
        messData.put("name", group.getName());
        messData.put("inviteCode", inviteCode);
        messData.put("adminUid", adminUid);
        messData.put("adminName", adminName);
        messData.put("createdAt", group.getCreatedAt());
        messData.put("memberCount", 1);

        Map<String, Object> adminMember = new HashMap<>();
        adminMember.put("userUid", adminUid);
        adminMember.put("userName", adminName);
        adminMember.put("role", "admin");
        adminMember.put("joinedAt", System.currentTimeMillis());

        Map<String, Object> membersMap = new HashMap<>();
        membersMap.put(adminUid, adminMember);
        messData.put("members", membersMap);

        // 1. Write to Realtime Database
        if (realtimeDatabase != null) {
            DatabaseReference messRef = realtimeDatabase.getReference(NODE_MESSES).child(messId);
            messRef.setValue(messData);

            // Write invite code reference for fast O(1) cloud lookup
            Map<String, Object> codeData = new HashMap<>();
            codeData.put("householdId", messId);
            codeData.put("name", group.getName());
            codeData.put("adminUid", adminUid);
            codeData.put("inviteCode", inviteCode);
            codeData.put("createdAt", System.currentTimeMillis());
            realtimeDatabase.getReference(NODE_INVITE_CODES).child(inviteCode).setValue(codeData);
        }

        // 2. Write to Cloud Firestore
        if (firestore != null) {
            List<String> memberUids = new ArrayList<>();
            memberUids.add(adminUid);
            messData.put("memberUids", memberUids);

            firestore.collection(NODE_MESSES).document(messId)
                    .set(messData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Mess written to Cloud Firestore: " + messId))
                    .addOnFailureListener(e -> Log.w(TAG, "Firestore mess write note: " + e.getMessage()));

            Map<String, Object> codeMap = new HashMap<>();
            codeMap.put("householdId", messId);
            codeMap.put("name", group.getName());
            codeMap.put("adminUid", adminUid);
            firestore.collection(NODE_INVITE_CODES).document(inviteCode).set(codeMap);
        }

        recordLastSyncSuccess();
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(group, inviteCode));
        }
    }

    /**
     * Validates an invite code against actual Firebase Cloud data.
     * Joins the user to the shared mess and attaches real-time listeners.
     */
    public void joinMessInCloud(final String inviteCode, final String userUid,
                                final String userName, final CloudJoinCallback callback) {
        final String cleanCode = inviteCode.trim().toUpperCase();
        Log.i(TAG, "Validating invite code in Firebase Cloud: " + cleanCode);

        if (realtimeDatabase == null && firestore == null) {
            if (callback != null) callback.onError("Firebase Cloud is unavailable. Check internet connection.");
            return;
        }

        // Step 1: Check Realtime Database for invite code
        if (realtimeDatabase != null) {
            realtimeDatabase.getReference(NODE_INVITE_CODES).child(cleanCode)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String messId = snapshot.child("householdId").getValue(String.class);
                                String messName = snapshot.child("name").getValue(String.class);
                                if (messName == null) messName = "Shared Mess (" + cleanCode + ")";

                                completeCloudJoin(messId, messName, cleanCode, userUid, userName, callback);
                            } else {
                                // Fallback: Try Firestore before rejecting
                                checkFirestoreInviteCode(cleanCode, userUid, userName, callback);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            checkFirestoreInviteCode(cleanCode, userUid, userName, callback);
                        }
                    });
        } else {
            checkFirestoreInviteCode(cleanCode, userUid, userName, callback);
        }
    }

    private void checkFirestoreInviteCode(final String cleanCode, final String userUid,
                                          final String userName, final CloudJoinCallback callback) {
        if (firestore == null) {
            if (callback != null) callback.onError("Invalid invite code: '" + cleanCode + "' was not found in Firebase Cloud.");
            return;
        }

        firestore.collection(NODE_INVITE_CODES).document(cleanCode)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (doc != null && doc.exists()) {
                            String messId = doc.getString("householdId");
                            String messName = doc.getString("name");
                            if (messName == null) messName = "Shared Mess (" + cleanCode + ")";
                            completeCloudJoin(messId, messName, cleanCode, userUid, userName, callback);
                        } else {
                            if (callback != null) {
                                callback.onError("Invalid invite code: '" + cleanCode + "' does not exist in Firebase Cloud.");
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (callback != null) {
                            callback.onError("Failed to validate invite code in cloud: " + e.getMessage());
                        }
                    }
                });
    }

    private void completeCloudJoin(final String messId, final String messName, final String cleanCode,
                                   final String userUid, final String userName, final CloudJoinCallback callback) {
        Log.i(TAG, "Code validated successfully! Adding user " + userName + " to cloud mess " + messId);

        // Add member in RTDB
        if (realtimeDatabase != null) {
            Map<String, Object> memberData = new HashMap<>();
            memberData.put("userUid", userUid);
            memberData.put("userName", userName);
            memberData.put("role", "member");
            memberData.put("joinedAt", System.currentTimeMillis());

            realtimeDatabase.getReference(NODE_MESSES).child(messId).child(NODE_MEMBERS).child(userUid).setValue(memberData);

            // Log activity
            Map<String, Object> act = new HashMap<>();
            act.put("userName", userName);
            act.put("action", "Joined the mess via invite code " + cleanCode);
            act.put("timestamp", System.currentTimeMillis());
            realtimeDatabase.getReference(NODE_MESSES).child(messId).child(NODE_ACTIVITIES).push().setValue(act);
        }

        // Add member in Firestore
        if (firestore != null) {
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("userUid", userUid);
            memberMap.put("userName", userName);
            memberMap.put("role", "member");
            memberMap.put("joinedAt", System.currentTimeMillis());

            firestore.collection(NODE_MESSES).document(messId)
                    .collection(NODE_MEMBERS).document(userUid).set(memberMap);

            firestore.collection(NODE_MESSES).document(messId)
                    .update("memberUids", com.google.firebase.firestore.FieldValue.arrayUnion(userUid));
        }

        // Persist locally in SQLite
        HouseholdRepository repo = new HouseholdRepository(context);
        HouseholdGroup group = repo.getById(messId);
        if (group == null) {
            group = new HouseholdGroup(0, messId, messName, cleanCode, userUid, System.currentTimeMillis(), 2);
            repo.insertGroupDirect(group);
        }
        repo.addMember(messId, userUid, userName, "member");
        repo.setActiveHousehold(messId, messName);

        // Immediately start listening for shared mess groceries & pantry items!
        attachSharedGroceryListener(messId, sharedGroceryChangeListener);

        recordLastSyncSuccess();
        if (callback != null) {
            final HouseholdGroup finalGroup = group;
            mainHandler.post(() -> callback.onSuccess(finalGroup));
        }
    }

    // ==========================================
    // 3. SHARED GROCERY LIST REAL-TIME SYNC (Req 6)
    // ==========================================

    /**
     * Attaches a real-time listener to the shared mess grocery list.
     * When any member on any device adds, edits, purchases, or deletes an item,
     * this listener immediately updates the local SQLite database and notifies the UI.
     */
    public void attachSharedGroceryListener(final String messId, @Nullable final OnSharedGroceryChangeListener listener) {
        this.sharedGroceryChangeListener = listener;
        if (realtimeDatabase == null || messId == null || messId.isEmpty()) return;

        if (groceriesRef != null && groceriesChildListener != null) {
            groceriesRef.removeEventListener(groceriesChildListener);
        }

        groceriesRef = realtimeDatabase.getReference(NODE_MESSES).child(messId).child(NODE_GROCERIES);
        groceriesChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                handleGroceryItemFromCloud(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                handleGroceryItemFromCloud(snapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                handleGroceryItemRemovedFromCloud(snapshot);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Shared grocery listener cancelled: " + error.getMessage());
            }
        };
        groceriesRef.addChildEventListener(groceriesChildListener);
        Log.i(TAG, "Attached Realtime Grocery Listener for mess: " + messId);
    }

    private void handleGroceryItemFromCloud(DataSnapshot snapshot) {
        try {
            String firebaseId = snapshot.getKey();
            if (firebaseId == null) return;

            String name = snapshot.child("name").getValue(String.class);
            if (name == null || name.trim().isEmpty()) return;

            Double qty = snapshot.child("quantity").getValue(Double.class);
            String unit = snapshot.child("unit").getValue(String.class);
            String cat = snapshot.child("category").getValue(String.class);
            Boolean isPurchased = snapshot.child("isPurchased").getValue(Boolean.class);
            Double cost = snapshot.child("estimatedCost").getValue(Double.class);
            String notes = snapshot.child("notes").getValue(String.class);
            String householdId = snapshot.child("householdId").getValue(String.class);

            ShoppingRepository repo = new ShoppingRepository(context);
            List<ShoppingItem> localItems = repo.getAll();
            ShoppingItem existing = null;

            for (ShoppingItem item : localItems) {
                if (firebaseId.equals(item.getFirebaseId()) || name.equalsIgnoreCase(item.getName())) {
                    existing = item;
                    break;
                }
            }

            if (existing != null) {
                existing.setName(name);
                existing.setQuantity(qty != null ? qty : 1.0);
                existing.setUnit(unit != null ? unit : "items");
                existing.setCategory(cat != null ? cat : "Vegetables");
                existing.setPurchased(Boolean.TRUE.equals(isPurchased));
                existing.setEstimatedCost(cost != null ? cost : 0.0);
                existing.setNotes(notes != null ? notes : "");
                existing.setFirebaseId(firebaseId);
                existing.setHouseholdId(householdId != null ? householdId : "");
                repo.update(existing);
            } else {
                ShoppingItem newItem = new ShoppingItem(0, name, qty != null ? qty : 1.0,
                        unit != null ? unit : "items", cat != null ? cat : "Vegetables",
                        Boolean.TRUE.equals(isPurchased), cost != null ? cost : 0.0,
                        notes != null ? notes : "", householdId != null ? householdId : "");
                newItem.setFirebaseId(firebaseId);
                repo.insert(newItem);
            }

            recordLastSyncSuccess();
            if (sharedGroceryChangeListener != null) {
                mainHandler.post(() -> sharedGroceryChangeListener.onGroceryChanged());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling cloud grocery item: " + e.getMessage());
        }
    }

    private void handleGroceryItemRemovedFromCloud(DataSnapshot snapshot) {
        try {
            String firebaseId = snapshot.getKey();
            String name = snapshot.child("name").getValue(String.class);

            ShoppingRepository repo = new ShoppingRepository(context);
            List<ShoppingItem> localItems = repo.getAll();
            for (ShoppingItem item : localItems) {
                if (firebaseId != null && firebaseId.equals(item.getFirebaseId())) {
                    repo.delete(item.getId());
                    break;
                } else if (name != null && name.equalsIgnoreCase(item.getName())) {
                    repo.delete(item.getId());
                    break;
                }
            }

            if (sharedGroceryChangeListener != null) {
                mainHandler.post(() -> sharedGroceryChangeListener.onGroceryChanged());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling grocery deletion: " + e.getMessage());
        }
    }

    /**
     * Uploads/updates a shared grocery item to Realtime Database.
     * Automatically assigns a unique cloud push key.
     */
    public void syncSharedGroceryItem(final ShoppingItem item, @Nullable final SyncCallback callback) {
        if (item == null) return;

        HouseholdRepository householdRepo = new HouseholdRepository(context);
        String activeHouseholdId = householdRepo.getActiveHouseholdId();

        if (realtimeDatabase == null || activeHouseholdId == null || activeHouseholdId.isEmpty()) {
            // Offline/personal: record pending sync
            incrementPendingUploads();
            if (callback != null) callback.onError("Saved locally. Realtime DB offline.");
            return;
        }

        DatabaseReference targetRef = realtimeDatabase.getReference(NODE_MESSES)
                .child(activeHouseholdId).child(NODE_GROCERIES);

        String key = item.getFirebaseId();
        if (key == null || key.isEmpty()) {
            key = targetRef.push().getKey();
            item.setFirebaseId(key);
            // Update local SQLite with assigned cloud key
            new ShoppingRepository(context).update(item);
        }

        item.setHouseholdId(activeHouseholdId);
        Map<String, Object> map = item.toFirebaseMap();
        map.put("updatedAt", System.currentTimeMillis());
        map.put("updatedBy", FirebaseAuthManager.getInstance(context).getUserDisplayName());

        targetRef.child(key).setValue(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error != null) {
                    incrementPendingUploads();
                    saveLastSyncError("Grocery upload: " + error.getMessage());
                    if (callback != null) callback.onError("Failed to upload grocery: " + error.getMessage());
                } else {
                    recordLastSyncSuccess();
                    if (callback != null) callback.onSuccess("Grocery item synchronized to cloud mess.", 1);
                }
            }
        });
    }

    /**
     * Updates purchased status of an item in the shared cloud grocery list.
     */
    public void updateSharedGroceryItemPurchased(final ShoppingItem item, final boolean isPurchased) {
        if (item == null) return;
        HouseholdRepository householdRepo = new HouseholdRepository(context);
        String activeHouseholdId = householdRepo.getActiveHouseholdId();

        if (realtimeDatabase != null && activeHouseholdId != null && !activeHouseholdId.isEmpty()) {
            String key = item.getFirebaseId();
            if (key != null && !key.isEmpty()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("isPurchased", isPurchased);
                updates.put("updatedAt", System.currentTimeMillis());
                updates.put("updatedBy", FirebaseAuthManager.getInstance(context).getUserDisplayName());

                realtimeDatabase.getReference(NODE_MESSES)
                        .child(activeHouseholdId).child(NODE_GROCERIES).child(key)
                        .updateChildren(updates);
            }
        }
    }

    /**
     * Deletes an item from the shared cloud grocery list.
     */
    public void deleteSharedGroceryItem(final ShoppingItem item) {
        if (item == null) return;
        HouseholdRepository householdRepo = new HouseholdRepository(context);
        String activeHouseholdId = householdRepo.getActiveHouseholdId();

        if (realtimeDatabase != null && activeHouseholdId != null && !activeHouseholdId.isEmpty()) {
            String key = item.getFirebaseId();
            if (key != null && !key.isEmpty()) {
                realtimeDatabase.getReference(NODE_MESSES)
                        .child(activeHouseholdId).child(NODE_GROCERIES).child(key).removeValue();
            }
        }
    }

    // ==========================================
    // 4. INVENTORY UPLOAD & DOWNLOAD (Req 3 & 4)
    // ==========================================

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

    public void uploadToRealtimeDatabase(final List<FoodItem> items, final SyncCallback callback) {
        if (callback != null) callback.onProgress("Connecting to Firebase Realtime Database...");
        rtdbState = ConnectionState.SYNCING;
        notifyConnectionStateChanged();

        final DatabaseReference ref = getUserRealtimeRef();
        if (ref == null) {
            rtdbState = ConnectionState.OFFLINE;
            notifyConnectionStateChanged();
            postError(callback, "Realtime Database is in Offline / Local mode.");
            return;
        }

        if (items == null || items.isEmpty()) {
            rtdbState = ConnectionState.CONNECTED;
            notifyConnectionStateChanged();
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
                    rtdbState = ConnectionState.SYNC_FAILED;
                    saveLastSyncError("RTDB Upload: " + databaseError.getMessage());
                    notifyConnectionStateChanged();
                    postError(callback, "Realtime DB upload failed: " + databaseError.getMessage());
                } else {
                    rtdbState = ConnectionState.CONNECTED;
                    recordLastSyncSuccess();
                    notifyConnectionStateChanged();

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
        rtdbState = ConnectionState.SYNCING;
        notifyConnectionStateChanged();

        final DatabaseReference ref = getUserRealtimeRef();
        if (ref == null) {
            rtdbState = ConnectionState.OFFLINE;
            notifyConnectionStateChanged();
            postError(callback, "Realtime Database is in Offline / Local mode.");
            return;
        }

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rtdbState = ConnectionState.CONNECTED;
                notifyConnectionStateChanged();

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

                recordLastSyncSuccess();
                postSuccess(callback, "Downloaded " + count + " items from Realtime Database.", count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                rtdbState = ConnectionState.SYNC_FAILED;
                saveLastSyncError("RTDB Download: " + error.getMessage());
                notifyConnectionStateChanged();
                postError(callback, "Failed to download from Realtime DB: " + error.getMessage());
            }
        });
    }

    public void uploadToFirestore(final List<FoodItem> items, final SyncCallback callback) {
        if (callback != null) callback.onProgress("Connecting to Cloud Firestore...");
        firestoreState = ConnectionState.SYNCING;
        notifyConnectionStateChanged();

        final CollectionReference collection = getUserFirestoreCollection();
        if (collection == null) {
            firestoreState = ConnectionState.OFFLINE;
            notifyConnectionStateChanged();
            postError(callback, "Cloud Firestore is in Offline / Local mode.");
            return;
        }

        if (items == null || items.isEmpty()) {
            firestoreState = ConnectionState.CONNECTED;
            notifyConnectionStateChanged();
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
                        firestoreState = ConnectionState.CONNECTED;
                        recordLastSyncSuccess();
                        notifyConnectionStateChanged();

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
                        firestoreState = ConnectionState.SYNC_FAILED;
                        saveLastSyncError("Firestore Upload: " + e.getMessage());
                        notifyConnectionStateChanged();
                        postError(callback, "Cloud Firestore upload failed: " + e.getMessage());
                    }
                });
    }

    public void downloadFromFirestore(final SyncCallback callback) {
        if (callback != null) callback.onProgress("Connecting to Cloud Firestore...");
        firestoreState = ConnectionState.SYNCING;
        notifyConnectionStateChanged();

        final CollectionReference collection = getUserFirestoreCollection();
        if (collection == null) {
            firestoreState = ConnectionState.OFFLINE;
            notifyConnectionStateChanged();
            postError(callback, "Cloud Firestore is in Offline / Local mode.");
            return;
        }

        collection.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshots) {
                        firestoreState = ConnectionState.CONNECTED;
                        recordLastSyncSuccess();
                        notifyConnectionStateChanged();

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
                        firestoreState = ConnectionState.SYNC_FAILED;
                        saveLastSyncError("Firestore Download: " + e.getMessage());
                        notifyConnectionStateChanged();
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
                uploadToFirestore(items, new SyncCallback() {
                    @Override
                    public void onProgress(String msg) {
                        if (callback != null) callback.onProgress(msg);
                    }

                    @Override
                    public void onSuccess(String msg, int count) {
                        clearPendingUploads();
                        postSuccess(callback, "Synced " + count + " items to Realtime DB & Cloud Firestore!", count);
                    }

                    @Override
                    public void onError(String error) {
                        postSuccess(callback, "Realtime DB synced, Firestore reported: " + error, itemsSynced);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                uploadToFirestore(items, callback);
            }
        });
    }

    // ==========================================
    // 5. PENDING SYNC & METRICS (Req 6 & 7)
    // ==========================================

    public void syncPendingChanges() {
        int pending = getPendingUploadsCount();
        if (pending > 0) {
            Log.i(TAG, "Auto-syncing " + pending + " pending changes to cloud...");
            FoodRepository repo = new FoodRepository(context);
            List<FoodItem> items = repo.getAllActive();
            uploadToRealtimeDatabase(items, null);
            clearPendingUploads();
        }
    }

    public void recordLastSyncSuccess() {
        String formatted = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(new Date());
        context.getSharedPreferences(PREF_SYNC, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_SYNC_TIME, formatted)
                .putString(KEY_LAST_SYNC_ERROR, "")
                .apply();
        clearPendingUploads();
    }

    public String getLastSyncTimeFormatted() {
        return context.getSharedPreferences(PREF_SYNC, Context.MODE_PRIVATE)
                .getString(KEY_LAST_SYNC_TIME, "Never synced");
    }

    public void saveLastSyncError(String error) {
        context.getSharedPreferences(PREF_SYNC, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_SYNC_ERROR, error)
                .apply();
    }

    public String getLastSyncError() {
        return context.getSharedPreferences(PREF_SYNC, Context.MODE_PRIVATE)
                .getString(KEY_LAST_SYNC_ERROR, "");
    }

    public int getPendingUploadsCount() {
        return context.getSharedPreferences(PREF_SYNC, Context.MODE_PRIVATE)
                .getInt(KEY_PENDING_UPLOADS, 0);
    }

    public void incrementPendingUploads() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SYNC, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_PENDING_UPLOADS, 0);
        prefs.edit().putInt(KEY_PENDING_UPLOADS, current + 1).apply();
    }

    public void clearPendingUploads() {
        context.getSharedPreferences(PREF_SYNC, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_PENDING_UPLOADS, 0)
                .apply();
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
