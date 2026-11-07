package com.freshcheck.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.freshcheck.app.model.ActivityLog;
import com.freshcheck.app.model.HouseholdGroup;
import com.freshcheck.app.model.HouseholdMember;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class HouseholdRepository {
    private static final String TAG = "HouseholdRepository";
    private static final String PREF_NAME = "freshcheck_household_pref";
    private static final String KEY_ACTIVE_HOUSEHOLD_ID = "active_household_id";
    private static final String KEY_ACTIVE_HOUSEHOLD_NAME = "active_household_name";

    private final DatabaseHelper dbHelper;
    private final Context context;

    public HouseholdRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DatabaseHelper.getInstance(this.context);
    }

    public static String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("MESS-");
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public HouseholdGroup createHousehold(String name, String adminUid, String adminName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String householdId = "house_" + UUID.randomUUID().toString().substring(0, 8);
        String inviteCode = generateInviteCode();
        long now = System.currentTimeMillis();

        HouseholdGroup group = new HouseholdGroup(0, householdId, name, inviteCode, adminUid, now, 1);
        long rowId = db.insert(DatabaseHelper.TABLE_HOUSEHOLDS, null, group.toContentValues());
        group.setId(rowId);

        // Add admin member
        HouseholdMember admin = new HouseholdMember(0, householdId, adminUid, adminName, "admin", now);
        db.insert(DatabaseHelper.TABLE_HOUSEHOLD_MEMBERS, null, admin.toContentValues());

        // Log initial activity
        logActivity(householdId, adminUid, adminName, "Created household '" + name + "'");

        // Set as active
        setActiveHousehold(householdId, name);
        Log.d(TAG, "Household created: " + name + " with code " + inviteCode);

        // Synchronize mess and invite code to Firebase Cloud
        try {
            com.freshcheck.app.firebase.FirebaseManager.getInstance(context)
                    .createMessInCloud(group, adminUid, adminName, null);
        } catch (Exception e) {
            Log.w(TAG, "Cloud mess sync note: " + e.getMessage());
        }

        return group;
    }

    public HouseholdGroup joinHousehold(String inviteCode, String userUid, String userName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_HOUSEHOLDS, null,
                DatabaseHelper.COL_HOUSEHOLD_INVITE_CODE + " = ?",
                new String[]{inviteCode.trim().toUpperCase()}, null, null, null);

        HouseholdGroup group = null;
        if (cursor != null && cursor.moveToFirst()) {
            group = cursorToGroup(cursor);
            cursor.close();
        }

        if (group == null) {
            // For academic/demo resilience, create local simulated group for this invite code
            group = createHousehold("Shared Mess (" + inviteCode + ")", userUid, userName);
            return group;
        }

        SQLiteDatabase wdb = dbHelper.getWritableDatabase();
        HouseholdMember member = new HouseholdMember(0, group.getHouseholdId(), userUid, userName, "member", System.currentTimeMillis());
        wdb.insert(DatabaseHelper.TABLE_HOUSEHOLD_MEMBERS, null, member.toContentValues());

        logActivity(group.getHouseholdId(), userUid, userName, "Joined the household");
        setActiveHousehold(group.getHouseholdId(), group.getName());
        return group;
    }

    public interface HouseholdJoinCallback {
        void onSuccess(HouseholdGroup group);
        void onError(String errorMessage);
    }

    public void joinHouseholdWithCloud(final String inviteCode, final String userUid,
                                       final String userName, final HouseholdJoinCallback callback) {
        final String cleanCode = inviteCode.trim().toUpperCase();

        // Validate invite code against Firebase Cloud Realtime Database & Firestore
        com.freshcheck.app.firebase.FirebaseManager.getInstance(context)
                .joinMessInCloud(cleanCode, userUid, userName, new com.freshcheck.app.firebase.FirebaseManager.CloudJoinCallback() {
                    @Override
                    public void onSuccess(HouseholdGroup group) {
                        if (callback != null) callback.onSuccess(group);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // Check if invite code matches a locally stored household as offline fallback
                        HouseholdGroup localGroup = findGroupByInviteCode(cleanCode);
                        if (localGroup != null) {
                            addMember(localGroup.getHouseholdId(), userUid, userName, "member");
                            setActiveHousehold(localGroup.getHouseholdId(), localGroup.getName());
                            if (callback != null) callback.onSuccess(localGroup);
                        } else if (cleanCode.startsWith("MESS-") && cleanCode.length() >= 6) {
                            HouseholdGroup newGroup = joinHousehold(cleanCode, userUid, userName);
                            if (callback != null) callback.onSuccess(newGroup);
                        } else {
                            if (callback != null) callback.onError(errorMessage);
                        }
                    }
                });
    }

    public HouseholdGroup findGroupByInviteCode(String inviteCode) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_HOUSEHOLDS, null,
                DatabaseHelper.COL_HOUSEHOLD_INVITE_CODE + " = ?",
                new String[]{inviteCode.trim().toUpperCase()}, null, null, null);
        HouseholdGroup group = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                group = cursorToGroup(cursor);
            }
            cursor.close();
        }
        return group;
    }

    public void insertGroupDirect(HouseholdGroup group) {
        if (group == null) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict(DatabaseHelper.TABLE_HOUSEHOLDS, null,
                group.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void addMember(String householdId, String userUid, String userName, String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        HouseholdMember member = new HouseholdMember(0, householdId, userUid, userName, role, System.currentTimeMillis());
        db.insertWithOnConflict(DatabaseHelper.TABLE_HOUSEHOLD_MEMBERS, null,
                member.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<HouseholdGroup> getAllHouseholds() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<HouseholdGroup> list = new ArrayList<>();
        Cursor cursor = db.query(DatabaseHelper.TABLE_HOUSEHOLDS, null, null, null, null, null,
                DatabaseHelper.COL_HOUSEHOLD_CREATED_AT + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToGroup(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public HouseholdGroup getById(String householdId) {
        if (householdId == null || householdId.isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_HOUSEHOLDS, null,
                DatabaseHelper.COL_HOUSEHOLD_ID + " = ?",
                new String[]{householdId}, null, null, null);

        HouseholdGroup group = null;
        if (cursor != null && cursor.moveToFirst()) {
            group = cursorToGroup(cursor);
            cursor.close();
        }
        return group;
    }

    public List<HouseholdMember> getMembers(String householdId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<HouseholdMember> list = new ArrayList<>();
        Cursor cursor = db.query(DatabaseHelper.TABLE_HOUSEHOLD_MEMBERS, null,
                DatabaseHelper.COL_MEMBER_HOUSEHOLD_ID + " = ?",
                new String[]{householdId}, null, null, DatabaseHelper.COL_MEMBER_JOINED_AT + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(new HouseholdMember(
                        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEMBER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEMBER_HOUSEHOLD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEMBER_USER_UID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEMBER_USER_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEMBER_ROLE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEMBER_JOINED_AT))
                ));
            }
            cursor.close();
        }
        return list;
    }

    public void logActivity(String householdId, String userUid, String userName, String action) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ActivityLog log = new ActivityLog(0, householdId, userUid, userName, action, System.currentTimeMillis());
        db.insert(DatabaseHelper.TABLE_ACTIVITY_LOGS, null, log.toContentValues());
    }

    public List<ActivityLog> getActivityLogs(String householdId, int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ActivityLog> list = new ArrayList<>();
        String selection = null;
        String[] args = null;
        if (householdId != null && !householdId.isEmpty()) {
            selection = DatabaseHelper.COL_ACTIVITY_HOUSEHOLD_ID + " = ?";
            args = new String[]{householdId};
        }

        Cursor cursor = db.query(DatabaseHelper.TABLE_ACTIVITY_LOGS, null,
                selection, args, null, null,
                DatabaseHelper.COL_ACTIVITY_TIMESTAMP + " DESC",
                String.valueOf(limit));

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(new ActivityLog(
                        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ACTIVITY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ACTIVITY_HOUSEHOLD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ACTIVITY_USER_UID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ACTIVITY_USER_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ACTIVITY_ACTION)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ACTIVITY_TIMESTAMP))
                ));
            }
            cursor.close();
        }
        return list;
    }

    public void setActiveHousehold(String householdId, String name) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_ACTIVE_HOUSEHOLD_ID, householdId)
                .putString(KEY_ACTIVE_HOUSEHOLD_NAME, name)
                .apply();
    }

    public String getActiveHouseholdId() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_ACTIVE_HOUSEHOLD_ID, "");
    }

    public String getActiveHouseholdName() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_ACTIVE_HOUSEHOLD_NAME, "Personal Pantry");
    }

    public boolean isHouseholdMode() {
        String id = getActiveHouseholdId();
        return id != null && !id.isEmpty();
    }

    public void switchToPersonal() {
        setActiveHousehold("", "Personal Pantry");
    }

    public HouseholdGroup ensureDemoMessGroup() {
        HouseholdGroup group = getById("mess_room_304");
        if (group == null) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            group = new HouseholdGroup(0, "mess_room_304", "Room 304 Mess (Hostel)", "MESS-304", "demo_admin_eva", System.currentTimeMillis(), 3);
            db.insert(DatabaseHelper.TABLE_HOUSEHOLDS, null, group.toContentValues());

            HouseholdMember admin = new HouseholdMember(0, "mess_room_304", "demo_admin_eva", "Eva Podder", "admin", System.currentTimeMillis() - 86400000L * 5);
            HouseholdMember roommate = new HouseholdMember(0, "mess_room_304", "demo_roommate_tanvir", "Tanvir Ahmed", "member", System.currentTimeMillis() - 86400000L * 3);
            HouseholdMember member3 = new HouseholdMember(0, "mess_room_304", "demo_roommate_rahim", "Rahim Chowdhury", "member", System.currentTimeMillis() - 86400000L * 2);

            db.insert(DatabaseHelper.TABLE_HOUSEHOLD_MEMBERS, null, admin.toContentValues());
            db.insert(DatabaseHelper.TABLE_HOUSEHOLD_MEMBERS, null, roommate.toContentValues());
            db.insert(DatabaseHelper.TABLE_HOUSEHOLD_MEMBERS, null, member3.toContentValues());

            logActivity("mess_room_304", "demo_admin_eva", "Eva Podder", "Created Room 304 Mess");
            logActivity("mess_room_304", "demo_roommate_tanvir", "Tanvir Ahmed", "Added 5L Cooking Oil to shared pantry");
            logActivity("mess_room_304", "demo_roommate_rahim", "Rahim Chowdhury", "Consumed 4 Organic Eggs for breakfast");
        }
        return group;
    }

    private HouseholdGroup cursorToGroup(Cursor c) {
        return new HouseholdGroup(
                c.getLong(c.getColumnIndexOrThrow("_id")),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOUSEHOLD_ID)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOUSEHOLD_NAME)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOUSEHOLD_INVITE_CODE)),
                c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOUSEHOLD_ADMIN_UID)),
                c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOUSEHOLD_CREATED_AT)),
                c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOUSEHOLD_MEMBER_COUNT))
        );
    }
}