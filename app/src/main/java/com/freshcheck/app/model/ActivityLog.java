package com.freshcheck.app.model;

import android.content.ContentValues;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ActivityLog implements Serializable {
    private long id;
    private String householdId;
    private String userUid;
    private String userName;
    private String action;
    private long timestamp;

    public ActivityLog() {
        this.householdId = "";
        this.userUid = "";
        this.userName = "";
        this.action = "";
        this.timestamp = System.currentTimeMillis();
    }

    public ActivityLog(long id, String householdId, String userUid, String userName,
                       String action, long timestamp) {
        this.id = id;
        this.householdId = householdId;
        this.userUid = userUid;
        this.userName = userName;
        this.action = action;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getHouseholdId() { return householdId; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }

    public String getUserUid() { return userUid; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    public String getUserName() { return userName != null ? userName : "User"; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAction() { return action != null ? action : ""; }
    public void setAction(String action) { this.action = action; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("household_id", householdId);
        cv.put("user_uid", userUid);
        cv.put("user_name", userName);
        cv.put("action", action);
        cv.put("timestamp", timestamp);
        return cv;
    }

    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("householdId", householdId);
        map.put("userUid", userUid);
        map.put("userName", userName);
        map.put("action", action);
        map.put("timestamp", timestamp);
        return map;
    }
}