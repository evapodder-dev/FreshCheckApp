package com.freshcheck.app.model;

import android.content.ContentValues;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class HouseholdMember implements Serializable {
    private long id;
    private String householdId;
    private String userUid;
    private String userName;
    private String role; // "admin" or "member"
    private long joinedAt;

    public HouseholdMember() {
        this.householdId = "";
        this.userUid = "";
        this.userName = "";
        this.role = "member";
        this.joinedAt = System.currentTimeMillis();
    }

    public HouseholdMember(long id, String householdId, String userUid, String userName,
                           String role, long joinedAt) {
        this.id = id;
        this.householdId = householdId;
        this.userUid = userUid;
        this.userName = userName;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getHouseholdId() { return householdId; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }

    public String getUserUid() { return userUid; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    public String getUserName() { return userName != null ? userName : "Member"; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRole() { return role != null ? role : "member"; }
    public void setRole(String role) { this.role = role; }

    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("household_id", householdId);
        cv.put("user_uid", userUid);
        cv.put("user_name", userName);
        cv.put("role", role);
        cv.put("joined_at", joinedAt);
        return cv;
    }

    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("householdId", householdId);
        map.put("userUid", userUid);
        map.put("userName", userName);
        map.put("role", role);
        map.put("joinedAt", joinedAt);
        return map;
    }
}