package com.freshcheck.app.model;

import android.content.ContentValues;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class HouseholdGroup implements Serializable {
    private long id;
    private String householdId; // e.g. "house_abc123"
    private String name;
    private String inviteCode; // e.g. "MESS-9X2A"
    private String adminUid;
    private long createdAt;
    private int memberCount;

    public HouseholdGroup() {
        this.householdId = "";
        this.name = "";
        this.inviteCode = "";
        this.adminUid = "";
        this.createdAt = System.currentTimeMillis();
        this.memberCount = 1;
    }

    public HouseholdGroup(long id, String householdId, String name, String inviteCode,
                          String adminUid, long createdAt, int memberCount) {
        this.id = id;
        this.householdId = householdId;
        this.name = name;
        this.inviteCode = inviteCode;
        this.adminUid = adminUid;
        this.createdAt = createdAt;
        this.memberCount = memberCount;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getHouseholdId() { return householdId != null ? householdId : ""; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getInviteCode() { return inviteCode != null ? inviteCode : ""; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getAdminUid() { return adminUid != null ? adminUid : ""; }
    public void setAdminUid(String adminUid) { this.adminUid = adminUid; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("household_id", householdId);
        cv.put("name", name);
        cv.put("invite_code", inviteCode);
        cv.put("admin_uid", adminUid);
        cv.put("created_at", createdAt);
        cv.put("member_count", memberCount);
        return cv;
    }

    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("householdId", householdId);
        map.put("name", name);
        map.put("inviteCode", inviteCode);
        map.put("adminUid", adminUid);
        map.put("createdAt", createdAt);
        map.put("memberCount", memberCount);
        return map;
    }
}